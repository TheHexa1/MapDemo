package com.erichamion.freelance.oakglen.map;

import android.speech.tts.TextToSpeech;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * Created by Eric Ray on 7/15/2016.
 */
public interface MapUiHandler {
    void onMapLoadingStatusFinished();
    void onMapModeChanged(int newMode);
    void publishAdvice(@Nullable String adviceImage, @Nullable String adviceText);
    void publishAdvice(@DrawableRes int adviceImage, @Nullable String adviceText);
    void publishAdvice(@Nullable String adviceImage, int adviceText);
    void publishAdvice(@DrawableRes int adviceImage, int adviceText);
    void onRouteReady();
    void onRouteCalculationFailed(@NonNull String reason);
    @Nullable TextToSpeech getTts(boolean initializeIfEmpty);
    void releaseTts();
    boolean onDestinationReached();
}
