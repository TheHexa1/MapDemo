package com.erichamion.freelance.oakglen.map;

import android.speech.tts.TextToSpeech;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.ref.WeakReference;

/**
 * Created by Eric Ray on 7/15/2016.
 */
class SafeMapUiHandler implements MapUiHandler {
    private final WeakReference<MapUiHandler> mHandlerRef;

    public SafeMapUiHandler(@NonNull MapUiHandler handler) {
        mHandlerRef = new WeakReference<>(handler);
    }

    @Override
    public void onMapLoadingStatusFinished() {
        MapUiHandler handler = mHandlerRef.get();
        if (handler != null) handler.onMapLoadingStatusFinished();
    }

    @Override
    public void onMapModeChanged(int newMode) {
        MapUiHandler handler = mHandlerRef.get();
        if (handler != null) handler.onMapModeChanged(newMode);
    }

    @Override
    public void publishAdvice(@Nullable String adviceImage, @Nullable String adviceText) {
        MapUiHandler handler = mHandlerRef.get();
        if (handler != null) handler.publishAdvice(adviceImage, adviceText);
    }

    @Override
    public void publishAdvice(@DrawableRes int adviceImage, @Nullable String adviceText) {
        MapUiHandler handler = mHandlerRef.get();
        if (handler != null) handler.publishAdvice(adviceImage, adviceText);
    }

    @Override
    public void publishAdvice(@Nullable String adviceImage, int adviceText) {
        MapUiHandler handler = mHandlerRef.get();
        if (handler != null) handler.publishAdvice(adviceImage, adviceText);
    }

    @Override
    public void publishAdvice(@DrawableRes int adviceImage, int adviceText) {
        MapUiHandler handler = mHandlerRef.get();
        if (handler != null) handler.publishAdvice(adviceImage, adviceText);
    }

    @Override
    public void onRouteReady() {
        MapUiHandler handler = mHandlerRef.get();
        if (handler != null) handler.onRouteReady();
    }

    @Override
    public void onRouteCalculationFailed(@NonNull String reason) {
        MapUiHandler handler = mHandlerRef.get();
        if (handler != null) handler.onRouteCalculationFailed(reason);
    }

    @Nullable
    @Override
    public TextToSpeech getTts(boolean initializeIfEmpty) {
        MapUiHandler handler = mHandlerRef.get();
        return (handler == null) ? null : handler.getTts(initializeIfEmpty);
    }

    @Override
    public void releaseTts() {
        MapUiHandler handler = mHandlerRef.get();
        if (handler != null) handler.releaseTts();
    }

    @Override
    public boolean onDestinationReached() {
        MapUiHandler handler = mHandlerRef.get();
        return handler != null && handler.onDestinationReached();
    }
}
