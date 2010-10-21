/*
 * Copyright (c) 2010, MoPub Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'MoPub Inc.' nor the names of its contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.mopub.mobileads;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.content.Context;
import android.location.Location;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.FrameLayout;

public class MoPubView extends FrameLayout {

	public interface OnAdLoadedListener {
		public void OnAdLoaded(MoPubView m);
	}

	public interface OnAdFailedListener {
		public void OnAdFailed(MoPubView m);
	}

	public interface OnAdClosedListener {
		public void OnAdClosed(MoPubView m);
	}

	public static String HOST = "ads.mopub.com";
	public static String AD_HANDLER = "/m/ad";

	private AdView	mAdView;
	private Object	mAdSenseAdapter;
	private OnAdLoadedListener  mOnAdLoadedListener;
	private OnAdFailedListener  mOnAdFailedListener;
	private OnAdClosedListener  mOnAdClosedListener;

	public MoPubView(Context context) {
		this(context, null);
	}

	public MoPubView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// The AdView doesn't need to be in the view hierarchy until an ad is loaded
		mAdView = new AdView(context, this);
	}

	public void loadAd() {
		mAdView.loadAd();
	}

	public void loadFailUrl() {
		mAdView.loadFailUrl();
	}

	public void loadAdSense(String params) {
		try {
			Class.forName("com.google.ads.GoogleAdView");
		} catch (ClassNotFoundException e) {
			Log.d("MoPub", "Couldn't find AdSense SDK. Trying next ad...");
			loadFailUrl(); 		
			return;
		}

		try {
			Class<?> adapterClass;
			adapterClass = (Class<?>) Class.forName("com.mopub.mobileads.AdSenseAdapter");

			Class<?>[] parameterTypes = new Class[2];
			parameterTypes[0] = MoPubView.class;
			parameterTypes[1] = String.class;

			Constructor<?> constructor = adapterClass.getConstructor(parameterTypes);

			Object[] args = new Object[2];
			args[0] = this;
			args[1] = params;

			mAdSenseAdapter = constructor.newInstance(args);

			Method loadAdMethod = adapterClass.getMethod("loadAd", (Class[]) null);
			loadAdMethod.invoke(mAdSenseAdapter, (Object[]) null);
		} catch (ClassNotFoundException e) {
			Log.d("MoPub", "Couldn't find AdSenseAdapter class.  Trying next ad..."); 
			loadFailUrl(); 
			return;
		} catch (Exception e) {
			Log.d("MoPub", "Couldn't create AdSenseAdapter class.  Trying next ad...");
			loadFailUrl();
			return;
		}
	}

	public void registerClick() { 
		mAdView.registerClick(); 
	} 

	public void setAdUnitId(String adUnitId) {
		mAdView.setAdUnitId(adUnitId);
	}

	public void setKeywords(String keywords) {
		mAdView.setKeywords(keywords);
	}

	public String getKeywords() { 
		return mAdView.getKeywords(); 
	}

	public void setLocation(Location location) {
		mAdView.setLocation(location);
	}

	public Location getLocation() { 
		return mAdView.getLocation(); 
	} 

	public void setTimeout(int milliseconds) {
		mAdView.setTimeout(milliseconds);
	}

	public int getAdWidth() {
		return mAdView.getAdWidth();
	}

	public int getAdHeight() {
		return mAdView.getAdHeight();
	}

	public void adLoaded() {
		Log.d("MoPub","adLoaded");
		if (mOnAdLoadedListener != null)
			mOnAdLoadedListener.OnAdLoaded(this);
	}

	public void adFailed() {
		if (mOnAdFailedListener != null)
			mOnAdFailedListener.OnAdFailed(this);
	}

	public void adClosed() {
		if (mOnAdClosedListener != null)
			mOnAdClosedListener.OnAdClosed(this);
	}

	public void setOnAdLoadedListener(OnAdLoadedListener listener) {
		mOnAdLoadedListener = listener;
	}

	public void setOnAdFailedListener(OnAdFailedListener listener) {
		mOnAdFailedListener = listener;
	}

	public void setOnAdClosedListener(OnAdClosedListener listener) {
		mOnAdClosedListener = listener;
	}
}