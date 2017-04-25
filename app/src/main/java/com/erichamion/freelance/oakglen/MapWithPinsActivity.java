package com.erichamion.freelance.oakglen;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.ToggleButton;

import com.erichamion.freelance.oakglen.map.MapUiHandler;
import com.erichamion.freelance.oakglen.map.SafeMapUiHandler;
import com.skobbler.ngx.SKCoordinate;
import com.skobbler.ngx.SKMaps;
import com.skobbler.ngx.map.SKAnimationSettings;
import com.skobbler.ngx.map.SKAnnotation;
import com.skobbler.ngx.map.SKAnnotationView;
import com.skobbler.ngx.map.SKCoordinateRegion;
import com.skobbler.ngx.map.SKMapCustomPOI;
import com.skobbler.ngx.map.SKMapPOI;
import com.skobbler.ngx.map.SKMapSurfaceListener;
import com.skobbler.ngx.map.SKMapSurfaceView;
import com.skobbler.ngx.map.SKMapViewHolder;
import com.skobbler.ngx.map.SKPOICluster;
import com.skobbler.ngx.map.SKScreenPoint;
import com.skobbler.ngx.positioner.SKCurrentPositionListener;
import com.skobbler.ngx.positioner.SKCurrentPositionProvider;
import com.skobbler.ngx.positioner.SKPosition;
import com.skobbler.ngx.util.SKLogging;

import java.util.HashMap;
import java.util.Map;


public class MapWithPinsActivity extends AppCompatActivity implements SKMapSurfaceListener,SKCurrentPositionListener, MapUiHandler {

    private SKMapViewHolder mapViewHolder;
    private SKMapSurfaceView mapView;

    private String mMapResourcesPath;
    private SKCurrentPositionProvider mPositionProvider;
    private SensorManager mSensorManager;
    private SafeMapUiHandler mUiHandler;
    private LocationManager mLocationManager;
    private SKPosition skPosition;
    private Map<Integer, SKCoordinate> skCoordinateMap;

    private Intent mMapIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map_with_pins);

        mapViewHolder = (SKMapViewHolder) findViewById(R.id.view_group_map);

        skCoordinateMap = new HashMap<>();

        initMarkerLocations();

        init();

    }

    @Override
    protected void onResume() {
        super.onResume();
        mapViewHolder.onResume();

        SKLogging.writeLog("Init", "onResume - SKMaps initialization status=" + SKMaps.getInstance().isSKMapsInitialized(), SKLogging.LOG_DEBUG);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapViewHolder.onPause();

        SKLogging.writeLog("Init", "onPause - SKMaps initialization status=" + SKMaps.getInstance().isSKMapsInitialized(), SKLogging.LOG_DEBUG);
    }

    @Override
    public void onBackPressed() {
        Intent i = new Intent(this, HomeActivity.class);
        startActivity(i);
    }

    private void initMarkerLocations(){

        //location with names
        skCoordinateMap.put(0,new SKCoordinate(-116.943416667,34.031616667));
        skCoordinateMap.put(1,new SKCoordinate(-116.939667,34.032483));
        skCoordinateMap.put(2,new SKCoordinate(-116.939266667,34.0332));
        skCoordinateMap.put(3,new SKCoordinate(-116.935883333,34.038383333));
        skCoordinateMap.put(4,new SKCoordinate(-116.952533333,34.051466667));
        skCoordinateMap.put(5,new SKCoordinate(-116.9371,34.0385));
        skCoordinateMap.put(6,new SKCoordinate(-116.951566667,34.051133333));
        skCoordinateMap.put(7,new SKCoordinate(-116.941233333,34.040416667));
        skCoordinateMap.put(8,new SKCoordinate(-116.941366667,34.041016667));
        skCoordinateMap.put(9,new SKCoordinate(-116.9369,34.038966667));

        //location without names
        skCoordinateMap.put(10,new SKCoordinate(-116.941783333,34.0406));
        skCoordinateMap.put(11,new SKCoordinate(-116.941067,34.040067));
        skCoordinateMap.put(12,new SKCoordinate(-116.95305,34.051367));
        skCoordinateMap.put(13,new SKCoordinate(-116.942967,34.0327));
        skCoordinateMap.put(14,new SKCoordinate(-116.939266667,34.0331));
        skCoordinateMap.put(15,new SKCoordinate(-116.93605,34.038383));
        skCoordinateMap.put(16,new SKCoordinate(-116.937183,34.038817));
        skCoordinateMap.put(17,new SKCoordinate(-116.947816667,34.04415));
        skCoordinateMap.put(18,new SKCoordinate(-116.94775,34.044416667));
        skCoordinateMap.put(19,new SKCoordinate(-116.94535,34.045767));
        skCoordinateMap.put(20,new SKCoordinate(-116.953889,34.052)); //oak glen
        skCoordinateMap.put(21,new SKCoordinate(-117.591167,34.099717));

        //test coordinates 23.261377, 72.616499.. 23.228138, 72.632194
//        skCoordinateMap.put(0,new SKCoordinate(72.675048,23.227350));
//        skCoordinateMap.put(1,new SKCoordinate(72.632194,23.228138));
    }

    //initialize maps
    private void init(){
//        SharedPreferences prefs = getSharedPreferences(Util.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
//        mMapResourcesPath = prefs.getString(Util.PREFKEY_MAPRESOURCESPATH, "");
//        Util.initMapLibrary(this, mMapResourcesPath);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mPositionProvider = new SKCurrentPositionProvider(this);
        mPositionProvider.setCurrentPositionListener(this);
        mUiHandler = new SafeMapUiHandler(this);

        mapViewHolder.setMapSurfaceListener(this);

        startLocationUpdates();
    }

    /**
     * Customize the map view
     */
    private void applySettingsOnMapView() {
        mapView.getMapSettings().setMapRotationEnabled(true);
        mapView.getMapSettings().setMapZoomingEnabled(true);
        mapView.getMapSettings().setMapPanningEnabled(true);
        mapView.getMapSettings().setZoomWithAnchorEnabled(true);
        mapView.getMapSettings().setInertiaRotatingEnabled(true);
        mapView.getMapSettings().setInertiaZoomingEnabled(true);
        mapView.getMapSettings().setInertiaPanningEnabled(true);
    }

    /**
     * Draws annotations on map
     */
    private void prepareAnnotations() {

        // Add annotation using texture ID - from the json files.
        // get the annotation object
//        SKAnnotation annotationWithTextureId = new SKAnnotation(10);
//        // set annotation location
//        annotationWithTextureId.setLocation(new SKCoordinate(72.640991,23.2209618));
//        // set minimum zoom level at which the annotation should be visible
//        annotationWithTextureId.setMininumZoomLevel(5);
//        // set the annotation's type
//        annotationWithTextureId.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_MARKER);
//        // render annotation on map
//        mapView.addAnnotation(annotationWithTextureId, SKAnimationSettings.ANIMATION_POP_OUT);

        for(int i=0 ; i<skCoordinateMap.size(); i++) {
            // // add an annotation with a view
            SKAnnotation annotationFromView = new SKAnnotation(i);
            annotationFromView.setLocation(skCoordinateMap.get(i));
            annotationFromView.setMininumZoomLevel(5);
//            SKAnnotationView annotationView = new SKAnnotationView();
//            RelativeLayout customView =
//                    (RelativeLayout) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(
//                            R.layout.custom_marker, null, false);
            //  If width and height of the view  are not power of 2 the actual size of the image will be the next power of 2 of max(width,height).

//        annotationView.setView(findViewById(R.id.customMarker));
//            annotationView.setView(customView);
            // set the annotation's type
            annotationFromView.setAnnotationType(SKAnnotation.SK_ANNOTATION_TYPE_RED);
//            annotationFromView.setAnnotationView(annotationView);
            mapView.addAnnotation(annotationFromView, SKAnimationSettings.ANIMATION_POP_OUT);
        }

        if(skPosition != null) {
            mapView.setPositionAsCurrent(skPosition.getCoordinate(), 0, true);
        }

        // set map zoom level
        mapView.setZoom(13);

//        setViewingRegion(false);
        // center map on a position
//        mapView.animateToLocation(new SKCoordinate(37.7765, -122.4200), 0);
//        mapView.setPo

    }

    private void startLocationUpdates() {
        boolean[] locationProviders = hasLocationProviders(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER);
        mPositionProvider.requestLocationUpdates(locationProviders[0], locationProviders[1], locationProviders[2]);
    }

    private boolean[] hasLocationProviders(String... params) throws SecurityException {
        boolean[] result = new boolean[params.length];
        for (int i = 0; i < params.length; i++) {
            String providerToTest = params[i];
            result[i] = mLocationManager.getProvider(providerToTest) != null;
        }

        return result;
    }

    @Override
    public void onActionPan() {

    }

    @Override
    public void onActionZoom() {

    }

    @Override
    public void onSurfaceCreated(SKMapViewHolder skMapViewHolder) {

        mapView = mapViewHolder.getMapSurfaceView();
        applySettingsOnMapView();

        mapView.getCurrentGPSPosition(true);

        if(skPosition != null) {
            mapView.setPositionAsCurrent(skPosition.getCoordinate(), 0, true);
        }

//        mapView.setPositionAsCurrent(new SKCoordinate(-116.953889, 34.052), 0, true);

        prepareAnnotations();
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
    public void onAnnotationSelected(final SKAnnotation annotation) {

        String title = "Title Not Available";

        switch (annotation.getUniqueID()) {
            case 0:
                title = "Red Coats";
                break;
            case 1:
                title = "Gents";
                break;
            case 2:
                title = "BBQ Wheel";
                break;
            case 3:
                title = "Climbing Rock Face";
                break;
            case 4:
                title = "Squirrel and Saw";
                break;
            case 5:
                title = "School House";
                break;
            case 6:
                title = "Feed Station";
                break;
            case 7:
                title = "Concervency Sign";
                break;
            case 8:
                title = "Wilsher Peak Mountian Siloette";
                break;
            case 9:
                title = "Tennis Court Net";
                break;
            case 20:
                title = "Oak Glen";
                break;
        }

        askForNavigationDialog(annotation.getUniqueID(), title);
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

    @Override
    public void onBoundingBoxImageRendered(int i) {

    }

    @Override
    public void onGLInitializationError(String s) {

    }

    @Override
    public void onCurrentPositionUpdate(SKPosition skPosition) {
        this.skPosition = skPosition;

//        mapView.setPositionAsCurrent(skPosition.getCoordinate(), 0, true);
    }

    @Override
    public void onMapLoadingStatusFinished() {

    }

    @Override
    public void onMapModeChanged(int newMode) {

    }

    @Override
    public void publishAdvice(@Nullable String adviceImage, @Nullable String adviceText) {

    }

    @Override
    public void publishAdvice(@DrawableRes int adviceImage, @Nullable String adviceText) {

    }

    @Override
    public void publishAdvice(@Nullable String adviceImage, int adviceText) {

    }

    @Override
    public void publishAdvice(@DrawableRes int adviceImage, int adviceText) {

    }

    @Override
    public void onRouteReady() {

    }

    @Override
    public void onRouteCalculationFailed(@NonNull String reason) {

    }

    @Nullable
    @Override
    public TextToSpeech getTts(boolean initializeIfEmpty) {
        return null;
    }

    @Override
    public void releaseTts() {

    }

    @Override
    public boolean onDestinationReached() {
        return false;
    }

    private void askForNavigationDialog(final int marker_id, String title){
        android.support.v7.app.AlertDialog.Builder alertDialog = new android.support.v7.app.AlertDialog.Builder(this);

        // Setting Dialog Message
        alertDialog.setMessage("Do you want to start navigation to this location?");

        //title
        alertDialog.setTitle(title);

        // on pressing OK button
        alertDialog.setPositiveButton("START", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
//                dialog.cancel();
                mMapIntent = MapActivity.launchMap(MapWithPinsActivity.this,
                        skCoordinateMap.get(marker_id).getLatitude(),
                        skCoordinateMap.get(marker_id).getLongitude(),
                        "", R.drawable.cover_image_thumbnail, getString(R.string.app_name));
//                startActivity(mMapIntent);
            }
        });

        // on pressing OK button
        alertDialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        android.support.v7.app.AlertDialog alert = alertDialog.create();
        alert.show(); //show alert message
    }
}
