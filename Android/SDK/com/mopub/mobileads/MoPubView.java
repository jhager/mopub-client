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
import android.view.Gravity;
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

	private AdView	mAdView = null;
	private Object	mAdSenseAdapter = null;
	private OnAdLoadedListener  mOnAdLoadedListener = null;
	private OnAdFailedListener  mOnAdFailedListener = null;
	private OnAdClosedListener  mOnAdClosedListener = null;

	public MoPubView(Context context) {
		super(context);
		init(context, null);
	}

	public MoPubView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init(context, attrs);
	}

	private void init(Context context, AttributeSet attrs) {
		mAdView = new AdView(context, this);
		final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				320, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		addView(mAdView, layoutParams);
	}

	public void loadAd() {
		mAdView.loadAd();
	}

	public void loadFailUrl() {
		removeAllViews();
		final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
				320, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
		addView(mAdView, layoutParams);
		mAdView.loadFailUrl();
	}

	public void loadAdSense(String params) {
		removeAllViews();
		try {
			Class<?> adapterClass = (Class<?>) Class.forName("com.mopub.mobileads.AdSenseAdapter");
			Class<?>[] parameterTypes = new Class[1];
			parameterTypes[0] = MoPubView.class;
			parameterTypes[1] = String.class;

			Constructor<?> constructor = adapterClass.getConstructor(parameterTypes);

			Object[] args = new Object[1];
			args[0] = this;
			args[1] = params;

			mAdSenseAdapter = constructor.newInstance(args);
			
			Method loadAdMethod = adapterClass.getMethod("loadAd", (Class[]) null);
			loadAdMethod.invoke(mAdSenseAdapter, (Object[]) null);
		} catch (Throwable e) {
			Log.d("MoPub", "adsense failed, trying next"); 
			// If the adapter didn't load, try the next ad in the auction 
			loadFailUrl(); 
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

	public void adLoaded() {
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