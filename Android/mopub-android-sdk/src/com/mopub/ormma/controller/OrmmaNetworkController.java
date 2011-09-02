/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma.controller;


import com.mopub.ormma.OrmmaView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

public class OrmmaNetworkController extends OrmmaController {
    
    private static final String LOGTAG = "OrmmaNetworkController";
    
    private ConnectivityManager mConnectivityManager;
    private int mNetworkListenerCount;
    private BroadcastReceiver mBroadcastReceiver;
    
    public OrmmaNetworkController(OrmmaView ormmaView, Context context) {
        super(ormmaView, context);
        
        mConnectivityManager = 
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }
    
    public String getNetwork() {
        NetworkInfo ni = mConnectivityManager.getActiveNetworkInfo();
        String networkType = "unknown";
        if (ni == null) networkType = "offline";
        else {
            switch (ni.getState()) {
                case UNKNOWN: networkType = "unknown"; break;
                case DISCONNECTED: networkType = "offline"; break;
                default:
                    int type = ni.getType();
                    if (type == ConnectivityManager.TYPE_MOBILE) networkType = "cell";
                    else if (type == ConnectivityManager.TYPE_WIFI) networkType = "wifi";
            }
        }
        Log.d(LOGTAG, "getNetwork: " + networkType);
        return networkType;
    }

    public void startNetworkListener() {
        if (mNetworkListenerCount == 0) {
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    if (action.equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                        OrmmaNetworkController.this.onConnectionChanged();
                    }
                }
            };
            mContext.registerReceiver(mBroadcastReceiver, 
                    new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
        }
        mNetworkListenerCount++;
    }
    
    public void stopNetworkListener() {
        mNetworkListenerCount--;
        if (mNetworkListenerCount == 0) {
            try {
                mContext.unregisterReceiver(mBroadcastReceiver);
            } catch (IllegalArgumentException e) {
                Log.d(LOGTAG, "Tried to stop network listener, but it was already stopped.");
            }
            mBroadcastReceiver = null;
        }
    }
    
    private void onConnectionChanged() {
        String script = "window.ormmaview.fireChangeEvent({ network: \'" + getNetwork() + "\'});";
        Log.d(LOGTAG, script);
        mOrmmaView.injectJavaScript(script);
    }
    
    @Override
    public void stopAllListeners() {
        mNetworkListenerCount = 0;
        try {
            mContext.unregisterReceiver(mBroadcastReceiver);
        } catch (IllegalArgumentException e) {
            Log.d(LOGTAG, "Tried to stop network listener, but it was already stopped.");
        }
    }
}
