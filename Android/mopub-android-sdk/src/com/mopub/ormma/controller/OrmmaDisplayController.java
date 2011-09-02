/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma.controller;

import org.json.JSONException;
import org.json.JSONObject;

import com.mopub.ormma.OrmmaView;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.webkit.URLUtil;

public class OrmmaDisplayController extends OrmmaController {
    
    private static final String LOGTAG = "OrmmaDisplayController";
    
    // Used to get information about screen size, density, and orientation.
    private Display mDisplay;
    
    // Scaling factor for a density-independent pixel (1.0 for 160dpi screen, 0.75 for 120dpi, etc).
    private float mScaleFactor;
    
    // Has the creative set its own maximum size?
    private boolean mMaxSizeSet = false;
    
    // Maximum size and width, if set by the creative.
    private int mMaxWidth = -1;
    private int mMaxHeight = -1;
    
    // Receiver that listens for Intent.ACTION_CONFIGURATION_CHANGED.
    private OrmmaConfigurationBroadcastReceiver mConfigurationBroadcastReceiver;
    
    public OrmmaDisplayController(OrmmaView ormmaView, Context context) {
        super(ormmaView, context);
        
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        mDisplay = wm.getDefaultDisplay();
        
        DisplayMetrics dm = new DisplayMetrics();
        mDisplay.getMetrics(dm);
        mScaleFactor = dm.density;
        
        mConfigurationBroadcastReceiver = new OrmmaConfigurationBroadcastReceiver();
    }
    
    public void resize(int width, int height) {
        if (canResizeToFit(width, height)) {
            mOrmmaView.resize((int) (mScaleFactor * width), (int) (mScaleFactor * height));
        } else mOrmmaView.raiseError("Tried to resize beyond the maximum size.", "resize");
    }
    
    private boolean canResizeToFit(int width, int height) {
        return (mMaxWidth > 0 && width <= mMaxWidth && mMaxHeight > 0 && height <= mMaxHeight);
    }
    
    public void open(String url, boolean back, boolean forward, boolean refresh) {
        if (URLUtil.isValidUrl(url)) mOrmmaView.open(url, back, forward, refresh);
        else mOrmmaView.raiseError("Tried to open an invalid URL.", "open");
    }
    
    public void openMap(String url, boolean fullscreen) {
        if (URLUtil.isValidUrl(url)) mOrmmaView.openMap(url, fullscreen);
        else mOrmmaView.raiseError("Tried to open a map with an invalid URL.", "openMap");
    }
    
    public void playAudio(String url, boolean autoPlay, boolean controls, boolean loop, 
            boolean position, String startStyle, String stopStyle) {
        if (URLUtil.isValidUrl(url)) {
            mOrmmaView.playAudio(url, autoPlay, controls, loop, position, startStyle, stopStyle);
        } else mOrmmaView.raiseError("Tried to play audio with an invalid URL.", "playAudio");
    }
    
    public void playVideo(String url, boolean audioMuted, boolean autoPlay, boolean controls,
            boolean loop, int[] position, String startStyle, String stopStyle) {
        Dimensions d = null;
        if (position[0] != -1) {
            d = new Dimensions();
            d.x = position[0];
            d.y = position[1];
            d.width = position[2];
            d.height = position[3];
            d = getDeviceDimensions(d);
        }
        
        if (URLUtil.isValidUrl(url)) {
            mOrmmaView.playVideo(url, audioMuted, autoPlay, controls, loop, d, startStyle,
                    stopStyle);
        } else mOrmmaView.raiseError("Tried to play video with an invalid URL.", "playVideo");
    }
    
    private Dimensions getDeviceDimensions(Dimensions d) {
        d.width *= mScaleFactor;
        d.height *= mScaleFactor;
        d.x *= mScaleFactor;
        d.y *= mScaleFactor;
        
        if (d.height < 0) d.height = mOrmmaView.getHeight();
        if (d.width < 0) d.width = mOrmmaView.getWidth();
        
        int locationInWindow[] = new int[2];
        mOrmmaView.getLocationInWindow(locationInWindow);
        if (d.x < 0) d.x = locationInWindow[0];
        if (d.y < 0) d.y = locationInWindow[1];
        
        return d;
    }
    
    public void expand(String dimensions, String url, String properties) {
            Dimensions d;
            try {
                d = (Dimensions) getFromJSON(new JSONObject(dimensions), Dimensions.class);
                mOrmmaView.expand(getDeviceDimensions(d), url, 
                        (Properties) getFromJSON(new JSONObject(properties), Properties.class));
            } catch (IllegalArgumentException e) {
                Log.e(LOGTAG, "Failed to expand: getFromJSON had bad arguments.");
            } catch (JSONException e) {
                Log.e(LOGTAG, "Failed to expand: invalid JSON objects.");
            }
    }
    
    public void close() {
        mOrmmaView.close();
    }
    
    public void hide() {
        mOrmmaView.hide();
    }
    
    public void show() {
        mOrmmaView.show();
    }
    
    public boolean isVisible() {
        return (mOrmmaView.getVisibility() == View.VISIBLE);
    }
    
    public String dimensions() {
        return "{ \"top\" :" + (int) (mOrmmaView.getTop() / mScaleFactor) + "," + "\"left\" :"
                + (int) (mOrmmaView.getLeft() / mScaleFactor) + "," + "\"bottom\" :"
                + (int) (mOrmmaView.getBottom() / mScaleFactor) + "," + "\"right\" :"
                + (int) (mOrmmaView.getRight() / mScaleFactor) + "}";
    }

    public int getOrientation() {
        int orientation = mDisplay.getOrientation();
        int ret = -1;
        switch (orientation) {
            case Surface.ROTATION_0: ret = 0; break;
            case Surface.ROTATION_90: ret = 90; break;
            case Surface.ROTATION_180: ret = 180; break;
            case Surface.ROTATION_270: ret = 270; break;
        }
        Log.d(LOGTAG, "getOrientation: " +  ret);
        return ret;
    }

    public String getScreenSize() {
        DisplayMetrics dm = new DisplayMetrics();
        mDisplay.getMetrics(dm);

        return "{ width: " + (int) (dm.widthPixels / dm.density) + ", " + "height: "
                + (int) (dm.heightPixels / dm.density) + "}";
    }
    
    public String getSize() {
        return mOrmmaView.getSize();
    }
    
    public String getMaxSize() {
        if (mMaxSizeSet) return "{ width: " + mMaxWidth + ", " + "height: " + mMaxHeight + "}";
        else return getScreenSize();
    }
    
    public void setMaxSize(int w, int h) {
        mMaxSizeSet = true;
        mMaxWidth = w;
        mMaxHeight = h;
    }
    
    /**
     * Fires the "orientationChange" JavaScript event. This method will be called automatically 
     * as long as {@link #startConfigurationListener()} has been called.
     * 
     * @param orientation The new orientation (degrees clockwise from portrait)
     */
    private void onOrientationChanged(int orientation) {
        String script = "window.ormmaview.fireChangeEvent({ orientation: " + orientation + "});";
        Log.d(LOGTAG, script);
        mOrmmaView.injectJavaScript(script);
    }
    
    public void startConfigurationListener() {
        if (mConfigurationBroadcastReceiver == null) {
            mConfigurationBroadcastReceiver = new OrmmaConfigurationBroadcastReceiver();
        }
        mContext.registerReceiver(mConfigurationBroadcastReceiver, 
                new IntentFilter(Intent.ACTION_CONFIGURATION_CHANGED));
    }
    
    public void stopConfigurationListener() {
        try {
            mContext.unregisterReceiver(mConfigurationBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(LOGTAG, "Tried to stop configuration listener, but it was already stopped.");
        }
    }
    
    @Override
    public void stopAllListeners() {
        stopConfigurationListener();
        mConfigurationBroadcastReceiver = null;
    }
    
    private class OrmmaConfigurationBroadcastReceiver extends BroadcastReceiver {
        
        private int mLastOrientation;
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_CONFIGURATION_CHANGED)) {
                // Check whether orientation was a part of this configuration change.
                int orientation = OrmmaDisplayController.this.getOrientation();
                if (orientation != mLastOrientation) {
                    mLastOrientation = orientation;
                    OrmmaDisplayController.this.onOrientationChanged(mLastOrientation);
                }
            }
        }
    };
}
