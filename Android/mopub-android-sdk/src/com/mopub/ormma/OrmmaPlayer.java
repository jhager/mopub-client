/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma;

import com.mopub.ormma.controller.OrmmaController.PlayerProperties;

import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.TextView;
import android.widget.VideoView;

public class OrmmaPlayer extends VideoView implements OnCompletionListener, OnErrorListener, 
        OnPreparedListener {
    
    private static String sTransientText = "Loading...";
    
    public interface OrmmaPlayerListener {
        public void onComplete();
        public void onPrepared();
        public void onError();
    }
    
    private PlayerProperties mPlayerProperties;
    private RelativeLayout mTransientLayout;
    private AudioManager mAudioManager;
    private OrmmaPlayerListener mListener;
    private String mContentUrl;
    private int mVolumeBeforeMuting;
    private boolean mReleased = false;

    public OrmmaPlayer(Context context) {
        super(context);
        
        setOnCompletionListener(this);
        setOnErrorListener(this);
        setOnPreparedListener(this);
        
        mPlayerProperties = new PlayerProperties();
        mAudioManager = (AudioManager) getContext().getSystemService(Context.AUDIO_SERVICE);
    }
    
    public void setPlayData(PlayerProperties properties, String url) {
        if (properties != null) mPlayerProperties = properties;
        mContentUrl = url.trim();
    }
    
    public void setListener(OrmmaPlayerListener listener) {
        mListener = listener;
    }
    
    public void playVideo() {
        if (mPlayerProperties.doMute()) mutePlayer();
        loadPlayerContent();
        startPlaying();
    }
    
    public void playAudio() {
        loadPlayerContent();
    }
    
    public void mutePlayer() {
        mVolumeBeforeMuting = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_PLAY_SOUND);
    }
    
    public void unmutePlayer() {
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, mVolumeBeforeMuting, 
                AudioManager.FLAG_PLAY_SOUND);
    }
    
    private void loadPlayerContent() {
        Uri uri = Uri.parse(mContentUrl);
        setVideoURI(uri);
        startPlaying();
    }
    
    private void startPlaying() {
        displayPlayerControls();
        addTransientMessage();
        if (mPlayerProperties.isAutoPlay()) start();
    }
    
    private void displayPlayerControls() {
        if (mPlayerProperties.showControl()) {
            MediaController controller = new MediaController(getContext());
            setMediaController(controller);
            controller.setAnchorView(this);
            controller.requestFocus();
        }
    }
    
    private void addTransientMessage() {
        if (mPlayerProperties.inline) return;
        
        Context context = getContext();
        mTransientLayout = new RelativeLayout(context);
        mTransientLayout.setLayoutParams(getLayoutParams());
        
        TextView transientView = new TextView(context);
        transientView.setText(sTransientText);
        transientView.setTextColor(Color.WHITE);
        
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        lp.addRule(RelativeLayout.CENTER_IN_PARENT);
        
        mTransientLayout.addView(transientView, lp);
        ViewGroup parent = (ViewGroup) getParent();
        parent.addView(mTransientLayout);
    }
    
    private void clearTransientMessage() {
        if (mTransientLayout != null) {
            ViewGroup parent = (ViewGroup) getParent();
            parent.removeView(mTransientLayout);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        clearTransientMessage();
        if (mListener != null) mListener.onPrepared();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        //clearTransientMessage();
        removePlayerFromParent();
        if (mListener != null) mListener.onError();
        return false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        if (mPlayerProperties.doLoop()) start();
        else if (mPlayerProperties.exitOnComplete() || mPlayerProperties.inline) releasePlayer();
    }
    
    private void removePlayerFromParent() {
        ViewGroup parent = (ViewGroup) getParent();
        if (parent != null) parent.removeView(this);
    }
    
    public void releasePlayer() {
        if (mReleased) return;
        
        mReleased = true;
        
        stopPlayback();
        removePlayerFromParent();
        
        if (mPlayerProperties.doMute()) unmutePlayer();
        if (mListener != null) mListener.onComplete();
    }
}
