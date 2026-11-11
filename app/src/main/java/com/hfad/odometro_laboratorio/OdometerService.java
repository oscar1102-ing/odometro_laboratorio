package com.hfad.odometro_laboratorio;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.content.ContextCompat;

/** Bound service: calcula la distancia recorrida (metros) usando el GPS. */
public class OdometerService extends Service {

    public static final String PERMISSION_STRING =
            android.Manifest.permission.ACCESS_FINE_LOCATION;

    private final IBinder binder = new OdometerBinder();
    private LocationManager locManager;
    private LocationListener listener;

    private static double distanceInMeters = 0;
    private static Location lastLocation = null;
    private static Location currentLocation = null;
    private boolean tracking = false;

    public class OdometerBinder extends Binder {
        OdometerService getOdometer() {
            return OdometerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        locManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                if (lastLocation != null) {
                    distanceInMeters += location.distanceTo(lastLocation);
                }
                lastLocation = location;
                currentLocation = location;
            }

            @Override public void onProviderDisabled(String provider) {}
            @Override public void onProviderEnabled(String provider) {}
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {}
        };
    }

    /**
     * Inicia las actualizaciones de ubicación fijando el GPS_PROVIDER explícitamente.
     */
    public boolean startTracking(long seconds, float meters) {
        if (ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        stopTracking();

        // Forzar el proveedor GPS del emulador en lugar de getBestProvider
        String provider = LocationManager.GPS_PROVIDER;
        if (locManager == null || !locManager.isProviderEnabled(provider)) {
            provider = LocationManager.NETWORK_PROVIDER;
        }

        // Obtener la posición actual de partida
        Location last = locManager.getLastKnownLocation(provider);
        if (last != null) {
            currentLocation = last;
            lastLocation = last;
        }

        try {
            // Escuchar actualizaciones garantizando el Looper principal
            locManager.requestLocationUpdates(
                    provider,
                    seconds * 1000L,
                    meters,
                    listener,
                    Looper.getMainLooper()
            );
            tracking = true;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public void stopTracking() {
        if (tracking && locManager != null && listener != null
                && ContextCompat.checkSelfPermission(this, PERMISSION_STRING)
                == PackageManager.PERMISSION_GRANTED) {
            locManager.removeUpdates(listener);
        }
        tracking = false;
    }

    public void resetDistance() {
        distanceInMeters = 0;
        lastLocation = currentLocation;
    }

    public boolean isTracking() { return tracking; }

    public double getDistance() { return distanceInMeters; }

    public Location getCurrentLocation() { return currentLocation; }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopTracking();
        locManager = null;
        listener = null;
    }
}