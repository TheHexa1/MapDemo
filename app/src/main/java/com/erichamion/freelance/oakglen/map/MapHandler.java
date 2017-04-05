package com.erichamion.freelance.oakglen.map;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.SystemClock;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;

import com.erichamion.freelance.oakglen.R;
import com.erichamion.freelance.oakglen.Util;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSettings;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKPolyline;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.navigation.SKAdvisorSettings;
import com.skobbler.ngx.navigation.SKNavigationListener;
import com.skobbler.ngx.navigation.SKNavigationManager;
import com.skobbler.ngx.navigation.SKNavigationSettings;
import com.skobbler.ngx.navigation.SKNavigationState;
import com.skobbler.ngx.positioner.SKCurrentPositionListener;
import com.skobbler.ngx.positioner.SKCurrentPositionProvider;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.routing.SKRouteInfo;
import com.skobbler.ngx.routing.SKRouteJsonAnswer;
import com.skobbler.ngx.routing.SKRouteListener;
import com.skobbler.ngx.routing.SKRouteManager;
import com.skobbler.ngx.routing.SKRouteSettings;
import com.skobbler.ngx.sdktools.navigationui.SKToolsAdvicePlayer;
import com.skobbler.ngx.util.SKGeoUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Created by Eric Ray on 7/15/2016.
 */
public class MapHandler implements SKCurrentPositionListener, SKRouteListener, SensorEventListener, SKMapSurfaceListener, SKNavigationListener {
    public static final int MODE_NOLOCATION = 0;
    public static final int MODE_FREE = 1;
    public static final int MODE_NAV = 2;
    public static final int MODE_POST_NAV_ROAM = 3;
    public static final int MODE_WALKING_NAV = 4;

    private static final int MAP_ANIMATION_TIME = 300;
    private static final int MAP_PADDING_HORIZ = 32;
    private static final int MAP_PADDING_VERT = 32;
    private static final int MAP_UPDATE_FREQUENCY_MILLIS = 200;
    private static final float ZOOM_INTERVAL = 0.5f;

    // Roughly 2 feet.
    private static final double LOCATION_SENSITIVITY = 2.0 / 3.0;

    // Orientation in degrees
    private static final float ORIENTATION_SENSITIVITY = 10f;

    // Approximately 10 feet
    private static final double ARRIVAL_RADIUS = 3.05;

    private SKMapViewHolder mMapView;
    private SKMapSurfaceView mMapSurfaceView;
    private SKCurrentPositionProvider mPositionProvider;
    private SKNavigationManager mNavigationManager;
    private SKCoordinate mDest;
    private SKPosition mCurrentPosition;
    private SKPosition mLastUpdatedPosition;
    private float mLastUpdatedOrientation = Float.NEGATIVE_INFINITY;
    private int mMapMode = MODE_NOLOCATION;
    private String mMapResourcesPath;
    private boolean mHasRoute = false;
    private SKNavigationSettings mNavigationSettings;
    private boolean mIsOrientationSensorStarted = false;
    private double mDensityRatio;
    private long mLastLocationUpdateTime = 0;
    private long mLastHeadingUpdateTime = 0;
    private SKPolyline mPointerLine;


    private SensorManager mSensorManager;
    private final SafeMapUiHandler mUiHandler;
    private LocationManager mLocationManager;
    private boolean mShouldReportWhenReached;


    public MapHandler(@NonNull Context context, @NonNull MapUiHandler uiHandler) {
        SharedPreferences prefs = context.getSharedPreferences(Util.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
        mMapResourcesPath = prefs.getString(Util.PREFKEY_MAPRESOURCESPATH, "");
        Util.initMapLibrary(context, mMapResourcesPath);

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mPositionProvider = new SKCurrentPositionProvider(context);
        mPositionProvider.setCurrentPositionListener(this);
        mUiHandler = new SafeMapUiHandler(uiHandler);
    }

    public void init(double densityRatio, double destLatitude, double destLongitude, boolean reportWhenReached, @NonNull SKMapViewHolder mapView) {
        mDensityRatio = densityRatio;

        mDest = new SKCoordinate();
        mDest.setLatitude(destLatitude);
        mDest.setLongitude(destLongitude);

        mMapView = mapView;
        mMapView.setMapSurfaceListener(this);

        mShouldReportWhenReached = reportWhenReached;

        startLocationUpdates();
    }

    public void onStart() {
        if (mNavigationManager != null && mMapMode == MODE_NAV && mNavigationSettings != null) {
            startLocationUpdates();
            mNavigationManager.startNavigation(mNavigationSettings);
        }

    }

    public void onResume() {
        mMapView.onResume();

        if (mMapMode == MODE_POST_NAV_ROAM) {
            startOrientationSensor();
        }
    }

    public void onPause() {
        mMapView.onPause();

        if (mMapMode == MODE_POST_NAV_ROAM) {
            stopOrientationSensor();
        }
    }

    public void onStop() {

        if (mNavigationManager != null && mMapMode == MODE_NAV) {
            mNavigationManager.stopNavigation();
        }
        if (mPositionProvider != null) mPositionProvider.stopLocationUpdates();
    }

    public void onDestroy() {
        mUiHandler.releaseTts();
    }

    public boolean tryExit(Activity activityContext) {
        // To be accessible from the OnClickListeners, shouldExit must
        // be final. But a final boolean can't be changed. So use an
        // array (with length 1). The array is final, but the value
        // inside of it can be changed.
        final boolean[] shouldExit = {false};
        if (mMapMode == MODE_NAV || mMapMode == MODE_WALKING_NAV) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activityContext)
                    .setTitle("Exit Navigation")
                    .setMessage("Are you sure you want to stop navigation?")
                    .setCancelable(true)
                    .setNegativeButton("No", null); // shouldExit[0] remains false

            if (mMapMode == MODE_NAV) {
                dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitNavigation(false);
                        // shouldExit[0] remains false;
                    }
                });
            } else {
                dialogBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        exitWalkingNavigation();
                        // shouldExit[0] remains false;
                    }
                });
            }
            dialogBuilder.show();
        } else {
            shouldExit[0] = true;
        }

        return shouldExit[0];
    }

    @Override
    public void onCurrentPositionUpdate(SKPosition skPosition) {
        if (mMapSurfaceView == null) return;

        mCurrentPosition = skPosition;
        if (mCurrentPosition == null) return;


        // This needs reported as frequently as possible for navigation
        // purposes, so report before checking the time.
        mMapSurfaceView.reportNewGPSPosition(skPosition);

        // If applicable, report if the user has reached the destination.
        if (mShouldReportWhenReached &&
                SKGeoUtils.calculateAirDistanceBetweenCoordinates(skPosition.getCoordinate(), mDest) < ARRIVAL_RADIUS) {
            mShouldReportWhenReached = mUiHandler.onDestinationReached();
        }

        boolean mustSetViewingRegion = (mMapMode == MODE_WALKING_NAV);
        if (mMapMode == MODE_NOLOCATION && !mHasRoute) {
            mustSetViewingRegion = true;
            mMapMode = MODE_FREE;
            calculateRoute();
        }

        // Only update the map visibly if enough time has passed and if the minimum
        // distance has been moved.
        long time = SystemClock.elapsedRealtime();
        double distanceMoved = mLastUpdatedPosition == null ?
                LOCATION_SENSITIVITY * 2 :
                SKGeoUtils.calculateAirDistanceBetweenCoordinates(mCurrentPosition.getCoordinate(),
                        mLastUpdatedPosition.getCoordinate());
        if (mustSetViewingRegion && distanceMoved > LOCATION_SENSITIVITY && time - mLastLocationUpdateTime > MAP_UPDATE_FREQUENCY_MILLIS) {
            setViewingRegion(true);
            mLastUpdatedPosition = mCurrentPosition;
            mLastLocationUpdateTime = time;

            if (mMapMode == MODE_WALKING_NAV) {
                updatePointerLine(mCurrentPosition.getCoordinate(), mDest);
                double distanceMeters = SKGeoUtils.calculateAirDistanceBetweenCoordinates(mCurrentPosition.getCoordinate(), mDest);
                int distanceFeet = Math.round((float) (distanceMeters * SKGeoUtils.METERSTOFEET));
                mUiHandler.publishAdvice(null, String.format(Locale.US, "About %d feet", distanceFeet));
            }
        }

    }

    private void startOrientationSensor() {
        if (mIsOrientationSensorStarted) return;

        //noinspection deprecation
        Sensor orientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        mSensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI);
        mIsOrientationSensorStarted = true;
    }

    private void stopOrientationSensor() {
        if (!mIsOrientationSensorStarted) return;

        mSensorManager.unregisterListener(this);
        mIsOrientationSensorStarted = false;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long time = SystemClock.elapsedRealtime();
        if (time - mLastHeadingUpdateTime < MAP_UPDATE_FREQUENCY_MILLIS) return;

        //noinspection deprecation
        if (event.sensor.getType() != Sensor.TYPE_ORIENTATION) return;

        mMapSurfaceView.reportNewHeading(Math.abs(event.values[0]));

        if (mMapMode == MODE_WALKING_NAV && Math.abs(event.values[0] - mLastUpdatedOrientation) > ORIENTATION_SENSITIVITY) {
            setViewingRegion(true);
            mLastUpdatedOrientation = event.values[0];
        }

        mLastHeadingUpdateTime = time;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do nothing
    }

    private void setViewingRegion(boolean smooth) {
        // Don't do anything if we're not ready
        if (mMapSurfaceView == null) return;

        if (mMapMode == MODE_NAV || mMapMode == MODE_POST_NAV_ROAM) return;

        if (mMapMode == MODE_FREE || mMapMode == MODE_WALKING_NAV) {
            //mMapSurfaceView.setMapHeading(0f, smooth);
            if (mMapMode == MODE_FREE) {
                if (smooth) {
                    mMapSurfaceView.rotateTheMapToNorthSmooth(MAP_ANIMATION_TIME);
                } else {
                    mMapSurfaceView.rotateTheMapToNorth();
                }
            }

            if (mCurrentPosition == null) {
                if (smooth) {
                    mMapSurfaceView.centerMapOnPositionSmooth(mDest, MAP_ANIMATION_TIME);
                } else {
                    mMapSurfaceView.centerMapOnPosition(mDest);
                }
            } else {

//            double topLat = Math.max(mCurrentPosition.getCoordinate().getLatitude(), mDest.getLatitude());
//            double bottomLat = Math.min(mCurrentPosition.getCoordinate().getLatitude(), mDest.getLatitude());
//            double rightLong = Math.max(mCurrentPosition.getCoordinate().getLongitude(), mDest.getLongitude());
//            double leftLong = Math.min(mCurrentPosition.getCoordinate().getLongitude(), mDest.getLongitude());

                if (mMapMode == MODE_FREE) {
                    SKCoordinate centerCoord = new SKCoordinate();
                    centerCoord.setLatitude((mCurrentPosition.getCoordinate().getLatitude() + mDest.getLatitude()) / 2);
                    centerCoord.setLongitude((mCurrentPosition.getCoordinate().getLongitude() + mDest.getLongitude()) / 2);
                    double diameter = SKGeoUtils.calculateAirDistanceBetweenCoordinates(mCurrentPosition.getCoordinate(), mDest);

                    float zoomLevel = getZoomLevelForDiameter(diameter, centerCoord
                    );
                    //mMapSurfaceView.changeMapVisibleRegion(new SKCoordinateRegion(centerCoord, zoomLevel), smooth);
                    if (smooth) {
                        mMapSurfaceView.centerMapOnPositionSmooth(centerCoord, MAP_ANIMATION_TIME / 2);
                        mMapSurfaceView.setZoomSmooth(zoomLevel, MAP_ANIMATION_TIME / 2);
                    } else {
                        mMapSurfaceView.centerMapOnPosition(centerCoord);
                        mMapSurfaceView.setZoom(zoomLevel);
                    }
                } else {
                    // Use the SKMapSurfaceView's idea of the current location in case
                    // it is out of sync with mCurrentPosition.
                    SKCoordinate mapCenter = mMapSurfaceView.getCurrentMapRegion().getCenter();
                    double radius = SKGeoUtils.calculateAirDistanceBetweenCoordinates(mapCenter, mDest);
                    float zoomLevel = getZoomLevelForDiameter(radius * 2, mapCenter
                    );
                    if (smooth) {
                        //mMapSurfaceView.centerMapOnCurrentPositionSmooth(zoomLevel, MAP_ANIMATION_TIME);
                        mMapSurfaceView.setZoomSmooth(zoomLevel, MAP_ANIMATION_TIME);
                    } else {
                        //mMapSurfaceView.centerMapOnCurrentPosition();
                        mMapSurfaceView.setZoom(zoomLevel);
                    }
                }


            }
        }
    }

    private float getZoomLevelForDiameter(double diameter, SKCoordinate center) {
        /*
        * According to http://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Resolution_and_Scale
        *    resolution (in meters/pixel) = 156543.03 meters/pixel * cos(latitude) / (2 ^ zoomlevel)
        *
        * This implies:
        *    distance (in meters) = 156543.03 meters/pixel * screen_distance (in pixels) * cos(latitude) / (2 ^ zoomlevel)
        *
        * We want the minimum screen dimension (either height or width) to cover the specified diameter:
        *    diameter (meters) = 156543.03 meters/pixel * screen_measure (pixels) * cos(latitude) / (2 ^ zoomlevel)
        *
        * Rearranging the equation:
        *    2 ^ zoomlevel = 156543.03 meters/pixel * screen_measure (pixels) * cos(latitude) / diameter (meters)
        *
        * Finally:
        *    zoomlevel = log2(156543.03 meters/pixel * screen_measure (pixels) * cos(latitude) / diameter (meters))
        *
        */

        int viewWidthPx = mMapView.getWidth() - MAP_PADDING_HORIZ;
        int viewHeightPx = mMapView.getHeight() - MAP_PADDING_VERT;
        int screenMeasureDp = (int) Math.ceil(Math.min(viewWidthPx, viewHeightPx) / mDensityRatio);
        //int screenMeasure = Math.min(viewWidthPx, viewHeightPx);

        float result = (float) (Math.log(156543.03 * screenMeasureDp * Math.cos(Math.toRadians(center.getLatitude())) / diameter) / Math.log(2.0));

        int steps = (int) (result / ZOOM_INTERVAL);
        result = steps * ZOOM_INTERVAL;

        return result;

    }

    private void removePointerLine() {
        if (mPointerLine == null) return;
        mMapSurfaceView.clearOverlay(mPointerLine.getIdentifier());
        mPointerLine = null;
    }

    private void updatePointerLine(SKCoordinate source, SKCoordinate dest) {
        removePointerLine();
        mPointerLine = new SKPolyline();
        List<SKCoordinate> nodes = new ArrayList<>(2);
        nodes.add(source);
        nodes.add(dest);
        mPointerLine.setNodes(nodes);
        mPointerLine.setColor(new float[]{0, 0, 0, 0});
        mPointerLine.setOutlineDottedPixelsSolid(10);
        mPointerLine.setOutlineDottedPixelsSkip(5);
        mPointerLine.setOutlineColor(new float[] {0, 0, 0, 0.5f});
        mPointerLine.setOutlineSize(2);
        mPointerLine.setIdentifier(175);
        mMapSurfaceView.addPolyline(mPointerLine);
    }

    private void calculateRoute() {
        SKRouteManager.getInstance().clearAllRoutesFromCache();
        SKRouteSettings routeSettings = new SKRouteSettings();
        routeSettings.setStartCoordinate(mCurrentPosition.getCoordinate());
        routeSettings.setDestinationCoordinate(mDest);
        routeSettings.setNoOfRoutes(1);
        routeSettings.setRouteMode(SKRouteSettings.SKRouteMode.CAR_FASTEST);
        //routeSettings.setRouteMode(SKRouteSettings.SKRouteMode.PEDESTRIAN);
        routeSettings.setRouteExposed(true);

        SKRouteManager.getInstance().setRouteListener(this);
        SKRouteManager.getInstance().calculateRoute(routeSettings);

    }

    private void setUpMap() {
        mUiHandler.onMapLoadingStatusFinished();

        mMapSurfaceView = mMapView.getMapSurfaceView();
        applyMapSettings(mMapSurfaceView);
        mMapSurfaceView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.NONE);

        SKAnnotation destAnnotation = new SKAnnotation(100);
        destAnnotation.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_MARKER);
        destAnnotation.setLocation(mDest);
        destAnnotation.setMininumZoomLevel(SKMapSurfaceView.MAXIMUM_ZOOM_LEVEL);

        mMapSurfaceView.addAnnotation(destAnnotation, SKAnimationSettings.ANIMATION_POP_OUT);

        setViewingRegion(false);

    }

    private void applyMapSettings(SKMapSurfaceView mapSurfaceView) {
        mapSurfaceView.getMapSettings().setMapRotationEnabled(true);
        mapSurfaceView.getMapSettings().setMapZoomingEnabled(true);
        mapSurfaceView.getMapSettings().setMapPanningEnabled(true);
        mapSurfaceView.getMapSettings().setZoomWithAnchorEnabled(true);
        mapSurfaceView.getMapSettings().setInertiaRotatingEnabled(true);
        mapSurfaceView.getMapSettings().setInertiaZoomingEnabled(true);
        mapSurfaceView.getMapSettings().setInertiaPanningEnabled(true);

    }

    private boolean[] hasLocationProviders(String... params) throws SecurityException {
        boolean[] result = new boolean[params.length];
        for (int i = 0; i < params.length; i++) {
            String providerToTest = params[i];
            result[i] = mLocationManager.getProvider(providerToTest) != null;
        }

        return result;
    }

    public void exitNavigation(boolean followPosition) {
        mNavigationManager.stopNavigation();
        mMapMode = MODE_FREE;

        if (followPosition) {
            mMapSurfaceView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.POSITION_PLUS_HEADING);
            mMapMode = MODE_POST_NAV_ROAM;
            startOrientationSensor();
        } else {
            mMapSurfaceView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.NONE);
            mMapMode = MODE_FREE;
        }

        mUiHandler.onMapModeChanged(mMapMode);
    }

    public void startWalkingNavigation() {
        mMapMode = MODE_WALKING_NAV;
        mUiHandler.publishAdvice(null, R.string.walking_distance);

        mMapSurfaceView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.POSITION_PLUS_HEADING);
        mMapSurfaceView.showHeadingIndicator(true);
        mMapView.setScaleViewEnabled(true);
        updatePointerLine(mMapSurfaceView.getCurrentMapRegion().getCenter(), mDest);
        startOrientationSensor();

        // Force location/orientation updates as soon as we get the sensor information
        mLastUpdatedOrientation = Float.NEGATIVE_INFINITY;
        mLastHeadingUpdateTime = 0;
        mLastUpdatedPosition = null;
        mLastLocationUpdateTime = 0;

        mUiHandler.onMapModeChanged(mMapMode);
    }

    public void exitWalkingNavigation() {
        // TODO: Add a new mode MODE_CALCULATING to use while calculating route

        mMapView.setScaleViewEnabled(false);
        removePointerLine();

        mMapSurfaceView.getMapSettings().setFollowerMode(SKMapSettings.SKMapFollowerMode.NONE);
        mMapMode = MODE_FREE;
        SKRouteManager.getInstance().clearCurrentRoute();
        mHasRoute = false;

        mUiHandler.onMapModeChanged(mMapMode);
        mUiHandler.publishAdvice(null, R.string.calculating_route);

        calculateRoute();
    }


    /****************************
     * BEGIN IMPLEMENTATION:
     * SKRouteListener
     ****************************/

    @Override
    public void onRouteCalculationCompleted(SKRouteInfo skRouteInfo) {
        SKRouteManager.getInstance().setCurrentRouteByUniqueId(skRouteInfo.getRouteID());
        if (mMapMode == MODE_NAV || mMapMode == MODE_POST_NAV_ROAM || mMapMode == MODE_WALKING_NAV) return;

        mMapSurfaceView.setMapHeading(0, true);
        SKRouteManager.getInstance().zoomToRoute(1.03f, 1.03f, MAP_PADDING_VERT, MAP_PADDING_VERT, MAP_PADDING_HORIZ, MAP_PADDING_HORIZ);

        mHasRoute = true;

        mUiHandler.publishAdvice(null, null);
        mUiHandler.onRouteReady();

    }

    @Override
    public void onRouteCalculationFailed(SKRoutingErrorCode skRoutingErrorCode) {
        Snackbar.make(mMapView, "Could not calculate route", Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onAllRoutesCompleted() {
        // Do nothing
    }

    @Override
    public void onServerLikeRouteCalculationCompleted(SKRouteJsonAnswer skRouteJsonAnswer) {
        // Do nothing
    }

    @Override
    public void onOnlineRouteComputationHanging(int i) {
        // Do nothing, at least for now
    }

    /****************************
     * END IMPLEMENTATION:
     * SKRouteListener
     ****************************/

    private SKAdvisorSettings.SKAdvisorType setAdvisorSettings() {
        SKAdvisorSettings.SKAdvisorType advisorType = SKAdvisorSettings.SKAdvisorType.TEXT_TO_SPEECH;
        final SKAdvisorSettings advisorSettings = new SKAdvisorSettings();
        advisorSettings.setLanguage(SKAdvisorSettings.SKAdvisorLanguage.LANGUAGE_EN_US);
        advisorSettings.setAdvisorConfigPath(mMapResourcesPath + "/Advisor");
        advisorSettings.setResourcePath(mMapResourcesPath + "/Advisor/Languages");
        advisorSettings.setAdvisorVoice("en");
        advisorSettings.setAdvisorType(advisorType);
        SKRouteManager.getInstance().setAudioAdvisorSettings(advisorSettings);
        return advisorType;
    }

    public void launchNavigation() {
        // Set up the advisor settings
        SKAdvisorSettings.SKAdvisorType advisorType = setAdvisorSettings();

        if (advisorType == SKAdvisorSettings.SKAdvisorType.TEXT_TO_SPEECH) {
            // Make sure TTS is initialized, but we don't need to do anything with
            // it right now.
            mUiHandler.getTts(true);
        } else {
            mUiHandler.releaseTts();
        }

        // get navigation settings object
        mNavigationSettings = new SKNavigationSettings();
        // set the desired navigation settings
        SKNavigationSettings.SKNavigationType navType = SKNavigationSettings.SKNavigationType.REAL;
        mNavigationSettings.setNavigationType(navType);
        mNavigationSettings.setPositionerVerticalAlignment(-0.25f);
        mNavigationSettings.setShowRealGPSPositions(false);
        mNavigationSettings.setDistanceUnit(SKMaps.SKDistanceUnitType.DISTANCE_UNIT_MILES_FEET);
        mNavigationSettings.setSplitRouteEnabled(true);
        // get the navigation manager object
        mNavigationManager = SKNavigationManager.getInstance();
        mNavigationManager.setMapView(mMapSurfaceView);
        // set listener for navigation events
        mNavigationManager.setNavigationListener(this);

        // start navigating using the settings
        mNavigationManager.startNavigation(mNavigationSettings);
        mMapMode = MODE_NAV;

        // Set up the UI for navigation mode
        mUiHandler.onMapModeChanged(mMapMode);
    }

    private void startLocationUpdates() {
        boolean[] locationProviders = hasLocationProviders(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER);
        mPositionProvider.requestLocationUpdates(locationProviders[0], locationProviders[1], locationProviders[2]);
    }



    /****************************
     * BEGIN IMPLEMENTATION:
     * SKMapSurfaceListener
     ****************************/

    @Override
    public void onActionPan() {
    }

    @Override
    public void onActionZoom() {
    }

    @Override
    public void onSurfaceCreated(SKMapViewHolder skMapViewHolder) {
        setUpMap();
    }

    @Override
    public void onMapRegionChanged(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onMapRegionChangeStarted(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onMapRegionChangeEnded(SKCoordinateRegion skCoordinateRegion) {

    }

    @Override
    public void onDoubleTap(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onSingleTap(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onRotateMap() {

    }

    @Override
    public void onLongPress(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onInternetConnectionNeeded() {

    }

    @Override
    public void onMapActionDown(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onMapActionUp(SKScreenPoint skScreenPoint) {

    }

    @Override
    public void onPOIClusterSelected(SKPOICluster skpoiCluster) {

    }

    @Override
    public void onMapPOISelected(SKMapPOI skMapPOI) {

    }

    @Override
    public void onAnnotationSelected(SKAnnotation skAnnotation) {

    }

    @Override
    public void onCustomPOISelected(SKMapCustomPOI skMapCustomPOI) {

    }

    @Override
    public void onCompassSelected() {

    }

    @Override
    public void onCurrentPositionSelected() {

    }

    @Override
    public void onObjectSelected(int i) {

    }

    @Override
    public void onInternationalisationCalled(int i) {

    }

    //        @Override
    @SuppressWarnings({"unused", "EmptyMethod"})
    public void onDebugInfo(double v, float v1, double v2) {
        // Android Studio is confused about whether this is part of the interface.
        // If the function is left in, with the @Override annotation, the project
        // won't build ("Error:(335, 9) error: method does not override or implement
        // a method from a supertype"). If it's left out entirely, the project builds
        // but the IDE GUI gives an error, which is distracting.
        // So I'm leaving this in to satisfy the GUI, but removing the @Override
        // annotation to satisfy the build system, even though it really is an overridden
        // function (it's even in the documentation at
        // http://developer.skobbler.com/docs/android/index.html?com/skobbler/ngx/map/SKMapSurfaceListener.html
        // ... although, interestingly, apparently the same documentation (both are version 2.5)
        // does NOT show this function at
        // http://developer.skobbler.com/docs/android/2.5.0/index.html?com/skobbler/ngx/map/SKMapSurfaceListener.html

    }

    @Override
    public void onBoundingBoxImageRendered(int i) {

    }

    @Override
    public void onGLInitializationError(String s) {

    }

    /****************************
     * END IMPLEMENTATION:
     * SKMapSurfaceListener
     ****************************/



    /****************************
     * BEGIN IMPLEMENTATION:
     * SKNavigationListener
     ****************************/
    @Override
    public void onDestinationReached() {
        exitNavigation(true);
    }

    @Override
    public void onSignalNewAdviceWithInstruction(String s) {
        speak(s);
    }

    @Override
    public void onSignalNewAdviceWithAudioFiles(String[] audioFiles, boolean specialSoundFile) {
        SKToolsAdvicePlayer.getInstance().playAdvice(audioFiles, SKToolsAdvicePlayer.PRIORITY_NAVIGATION);
    }

    @Override
    public void onSpeedExceededWithAudioFiles(String[] strings, boolean b) {
        SKToolsAdvicePlayer.getInstance().playAdvice(strings, SKToolsAdvicePlayer.PRIORITY_SPEED_WARNING);
    }

    @Override
    public void onSpeedExceededWithInstruction(String s, boolean b) {
        if (b) speak(s);
    }

    @Override
    public void onUpdateNavigationState(SKNavigationState skNavigationState) {
        // Do nothing?
    }

    @Override
    public void onReRoutingStarted() {
        // Do nothing?
    }

    @Override
    public void onFreeDriveUpdated(String s, String s1, SKNavigationState.SKStreetType skStreetType, double v, double v1) {

    }

    @Override
    public void onViaPointReached(int i) {

    }

    @Override
    public void onVisualAdviceChanged(boolean b, boolean b1, SKNavigationState skNavigationState) {
        if (b || b1) {
            String exitNum = skNavigationState.getCurrentAdviceExitNumber();
            String intro = (exitNum != null && !exitNum.equals("")) ? "Exit " + exitNum + "for " : "";

            mUiHandler.publishAdvice(skNavigationState.getCurrentAdviceVisualAdviceFile(),
                    intro + skNavigationState.getCurrentAdviceNextStreetName());
        }
    }

    @Override
    public void onTunnelEvent(boolean b) {

    }

    private void speak(String s) {
        TextToSpeech tts = mUiHandler.getTts(true);
        if (tts != null) {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(s, TextToSpeech.QUEUE_ADD, null, null);
            } else {
                //noinspection deprecation
                tts.speak(s, TextToSpeech.QUEUE_ADD, null);
            }
        }
    }

    /****************************
     * END IMPLEMENTATION:
     * SKNavigationListener
     ****************************/


}
