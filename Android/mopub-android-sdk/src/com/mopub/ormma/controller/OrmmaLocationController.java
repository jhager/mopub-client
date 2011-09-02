/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma.controller;

import java.util.Iterator;
import java.util.List;

import com.mopub.ormma.OrmmaView;


import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;

public class OrmmaLocationController extends OrmmaController {
    
    private static final String LOGTAG = "OrmmaLocationController";
    
    private LocationManager mLocationManager;
    private OrmmaLocationListener mGpsLocListener;
    private OrmmaLocationListener mNetworkLocListener;
    private int mLocListenerCount;
    private boolean mAllowsLocationServices = false;
    
    public OrmmaLocationController(OrmmaView ormmaView, Context context) {
        super(ormmaView, context);
        
        try {
            mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (mLocationManager.getProvider(LocationManager.GPS_PROVIDER) != null) {
                mGpsLocListener = new OrmmaLocationListener(context, LocationManager.GPS_PROVIDER);
            } else if (mLocationManager.getProvider(LocationManager.NETWORK_PROVIDER) != null) {
                mNetworkLocListener = new OrmmaLocationListener(context, LocationManager.NETWORK_PROVIDER);
            }
            mAllowsLocationServices = true;
        } catch (SecurityException e) {
            // If getProvider fails for either provider (or both), we'll just have to live with
            // not receiving location events from that provider.
            Log.e(LOGTAG, e.getMessage());
        }
    }
    
    public void setAllowsLocationServices(boolean allows) {
        mAllowsLocationServices = allows;
    }
    
    public boolean allowsLocationServices() {
        return mAllowsLocationServices;
    }
    
    public String getLocation() {
        if (!mAllowsLocationServices) return null;
        
        List<String> providers = mLocationManager.getProviders(true);
        Iterator<String> provider = providers.iterator();
        Location lastLocation = null;
        
        while (provider.hasNext()) {
            lastLocation = mLocationManager.getLastKnownLocation(provider.next());
            if (lastLocation != null) break;
        }
        
        return (lastLocation != null) ? formatLocation(lastLocation) : null;
    }
    
    private static String formatLocation(Location loc) {
        return "{ lat: " + loc.getLatitude() + 
                ", lon: " + loc.getLongitude() +
                ", acc: " + loc.getAccuracy() + "}";
    }
    
    public void startLocationListener() {
        if (mLocListenerCount == 0) {
            if (mGpsLocListener != null) mGpsLocListener.startListening();
            if (mNetworkLocListener != null) mNetworkLocListener.startListening();
        }
        mLocListenerCount++;
    }
    
    public void stopLocationListener() {
        mLocListenerCount--;
        if (mLocListenerCount == 0) {
            if (mGpsLocListener != null) mGpsLocListener.stopListening();
            if (mNetworkLocListener != null) mNetworkLocListener.stopListening();
        }
    }
    
    /**
     * Fires the "locationChange" JavaScript event. This method should be called automatically as
     * long as {@link #startLocationListener()} has been called.
     * 
     * @param loc The new location
     */
    private void success(Location loc) {
        String script = "window.ormmaview.fireChangeEvent({ location: "+ formatLocation(loc) + "})";
        mOrmmaView.injectJavaScript(script);
    }
    
    /**
     * Fires the "error" JavaScript event.
     */
    private void fail() {
        mOrmmaView.injectJavaScript("window.ormmaview.fireErrorEvent(\"Location cannot be identified\", \"OrmmaLocationController\")");
    }
    
    @Override
    public void stopAllListeners() {
        mLocListenerCount = 0;
        if (mGpsLocListener != null) mGpsLocListener.stopListening();
        if (mNetworkLocListener != null) mNetworkLocListener.stopListening();
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public class OrmmaLocationListener implements LocationListener {
        
        private String mProvider;
        
        public OrmmaLocationListener(Context context, String provider) {
            mProvider = provider;
        }
        
        @Override
        public void onProviderDisabled(String provider) {
            OrmmaLocationController.this.fail();
        }
        
        @Override
        public void onProviderEnabled(String provider) {
            
        }
        
        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            if (status == LocationProvider.OUT_OF_SERVICE) OrmmaLocationController.this.fail();
        }
        
        @Override
        public void onLocationChanged(Location location) {
            OrmmaLocationController.this.success(location);
        }
        
        public void stopListening() {
            if (mLocationManager != null) mLocationManager.removeUpdates(this);
        }
        
        public void startListening() {
            if (mLocationManager != null) {
                // arguments: provider, minTime, minDistance, listener
                mLocationManager.requestLocationUpdates(mProvider, 0, 0, this);
            }
        }
    }
}