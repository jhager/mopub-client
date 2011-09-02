/*
 * Copyright (c) 2011, MoPub Inc.
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

import com.mopub.ormma.OrmmaView;
import com.mopub.ormma.OrmmaView.OrmmaViewListener;

import android.view.Gravity;
import android.widget.FrameLayout;

public class OrmmaAdapter extends BaseAdapter implements OrmmaViewListener {
    
    private MoPubView mMoPubView;
    private OrmmaView mOrmmaView;
    private String mParams;
    private boolean mPreviousAutorefreshSetting;
    
    public void init(MoPubView view, String params) {
        mMoPubView = view;
        mParams = params;
        mPreviousAutorefreshSetting = false;
    }
    
    @Override
    public void loadAd() {
        if (mMoPubView == null) return;

        mOrmmaView = new OrmmaView(mMoPubView.getActivity());
        mOrmmaView.setListener(this);
        mOrmmaView.loadHtmlData(mParams);
        
        mMoPubView.removeAllViews();
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, 
                FrameLayout.LayoutParams.FILL_PARENT);
        layoutParams.gravity = Gravity.CENTER;
        mMoPubView.addView(mOrmmaView, layoutParams);
        
        mMoPubView.nativeAdLoaded();
        mMoPubView.trackNativeImpression();
    }

    @Override
    public void invalidate() {
        mMoPubView = null;
    }
    
    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public boolean onReady() {
        return true;
    }

    @Override
    public boolean onResize() {
        if (mMoPubView != null) {
            mPreviousAutorefreshSetting = mMoPubView.getAutorefreshEnabled();
            mMoPubView.setAutorefreshEnabled(false);
            mMoPubView.registerClick();
        }
        return false;
    }

    @Override
    public boolean onExpand() {
        if (mMoPubView != null) {
            mPreviousAutorefreshSetting = mMoPubView.getAutorefreshEnabled();
            mMoPubView.setAutorefreshEnabled(false);
            mMoPubView.registerClick();
        }
        return false;
    }

    @Override
    public boolean onExpandClose() {
        if (mMoPubView != null) {
            mMoPubView.setAutorefreshEnabled(mPreviousAutorefreshSetting);
            mMoPubView.adClosed();
        }
        return false;
    }

    @Override
    public boolean onResizeClose() {
        if (mMoPubView != null) {
            mMoPubView.setAutorefreshEnabled(mPreviousAutorefreshSetting);
            mMoPubView.adClosed();
        }
        return false;
    }

    @Override
    public boolean onEventFired() {
        return false;
    }
/*
    @Override
    public void handleRequest(String url) {
    }*/

    @Override
    public boolean onFailure() {
        if (mMoPubView != null) mMoPubView.loadFailUrl();
        return false;
    }
}
