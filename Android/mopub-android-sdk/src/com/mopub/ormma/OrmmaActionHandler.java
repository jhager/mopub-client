/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.mopub.ormma.OrmmaView.ACTION;
import com.mopub.ormma.controller.OrmmaController.Dimensions;
import com.mopub.ormma.controller.OrmmaController.PlayerProperties;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

public class OrmmaActionHandler extends Activity {

    private HashMap<ACTION, Object> mActionMap = new HashMap<ACTION, Object>();
    private RelativeLayout mLayout;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Bundle data = getIntent().getExtras();
        
        mLayout = new RelativeLayout(this);
        mLayout.setLayoutParams(
                new ViewGroup.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT));
        
        setContentView(mLayout);
        doAction(data);
    }
    
    /*
     * Starts playing either audio or video, depending on the data passed in.
     */
    private void doAction(Bundle data) {
        String actionData = data.getString(OrmmaView.ACTION_KEY);
        if (actionData == null) return;
        
        ACTION actionType = ACTION.valueOf(actionData);
        switch (actionType) {
            case PLAY_AUDIO: {
                OrmmaPlayer player = initPlayer(data, actionType);
                player.playAudio();
                break;
            }
            case PLAY_VIDEO: {
                OrmmaPlayer player = initPlayer(data, actionType);
                player.playVideo();
                break;
            }
            default: break;
        }
    }
    
    private OrmmaPlayer initPlayer(Bundle data, ACTION actionType) {
        PlayerProperties playerProperties = 
            (PlayerProperties) data.getParcelable(OrmmaView.PLAYER_PROPERTIES);
        Dimensions dimensions = (Dimensions) data.getParcelable(OrmmaView.DIMENSIONS);
        
        OrmmaPlayer player = new OrmmaPlayer(this);
        player.setPlayData(playerProperties, data.getString(OrmmaView.EXPAND_URL));
        
        RelativeLayout.LayoutParams lp;
        if (dimensions == null) {
            lp = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);
            lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        } else {
            lp = new RelativeLayout.LayoutParams(dimensions.width, dimensions.height);
            lp.topMargin = dimensions.y;
            lp.leftMargin = dimensions.x;
        }
        
        player.setLayoutParams(lp);
        mLayout.addView(player);
        
        mActionMap.put(actionType, player);
        setPlayerListener(player);
        
        return player;
    }
    
    private void setPlayerListener(OrmmaPlayer player) {
        player.setListener(new OrmmaPlayer.OrmmaPlayerListener() {

            @Override
            public void onComplete() {
                finish();
            }

            @Override
            public void onPrepared() {
            }

            @Override
            public void onError() {
                finish();
            }
        });
    }
    
    @Override
    protected void onStop() {
        Iterator<Entry<ACTION, Object>> it = mActionMap.entrySet().iterator();
        
        while (it.hasNext()) {
            Map.Entry<ACTION, Object> entry = it.next();
            switch (entry.getKey()) {
                case PLAY_AUDIO:
                case PLAY_VIDEO: {
                    OrmmaPlayer player = (OrmmaPlayer) entry.getValue();
                    player.releasePlayer();
                    it.remove(); // MPADD
                    break;
                }
                default: break;
            }
        }
        
        super.onStop();
    }
}
