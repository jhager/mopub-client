/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.android.maps.MapView;
import com.mopub.ormma.controller.OrmmaUtilityController;
import com.mopub.ormma.controller.OrmmaController.Dimensions;
import com.mopub.ormma.controller.OrmmaController.PlayerProperties;
import com.mopub.ormma.controller.OrmmaController.Properties;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.WindowManager;
import android.webkit.JsResult;
import android.webkit.URLUtil;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.Toast;

public class OrmmaView extends WebView implements OnGlobalLayoutListener {
    
    private static final String LOGTAG = "OrmmaView";
    
    public enum ViewState { DEFAULT, RESIZED, EXPANDED, HIDDEN, LEFT_BEHIND, OPENED; }
    
    private static final int MESSAGE_RESIZE = 1000;
    private static final int MESSAGE_CLOSE = 1001;
    private static final int MESSAGE_HIDE = 1002;
    private static final int MESSAGE_SHOW = 1003;
    private static final int MESSAGE_EXPAND = 1004;
    private static final int MESSAGE_SEND_EXPAND_CLOSE = 1005;
    private static final int MESSAGE_OPEN = 1006;
    private static final int MESSAGE_PLAY_VIDEO = 1007;
    private static final int MESSAGE_PLAY_AUDIO = 1008;
    private static final int MESSAGE_RAISE_ERROR = 1009;
    
    public static final String DIMENSIONS = "expand_dimensions";
    public static final String PLAYER_PROPERTIES = "player_properties";
    public static final String EXPAND_URL = "expand_url";
    public static final String ACTION_KEY = "action";
    private static final String EXPAND_PROPERTIES = "expand_properties";
    private static final String RESIZE_WIDTH = "resize_width";
    private static final String RESIZE_HEIGHT = "resize_height";
    private static final String CURRENT_FILE = "ormma_current";
    private static final String AD_PATH = "AD_PATH";
    private static final String ERROR_MESSAGE = "message";
    private static final String ERROR_ACTION = "action";
    
    protected static final int PLACEHOLDER_ID = 100;
    protected static final int BACKGROUND_ID = 101;
    public static final int ORMMA_ID = 102;
    
    public enum ACTION { PLAY_AUDIO, PLAY_VIDEO }
    
    private static OrmmaPlayer sOrmmaPlayer;
    
    public interface OrmmaViewListener {
        abstract boolean onReady();
        abstract boolean onResize();
        abstract boolean onExpand();
        abstract boolean onExpandClose();
        abstract boolean onResizeClose();
        abstract boolean onEventFired();
        abstract boolean onFailure();
    }
    private OrmmaViewListener mListener;
    
    private WebViewClient mWebViewClient;
    private WebChromeClient mWebChromeClient;
    private Handler mHandler;
    private float mDensity;
    private int mContentViewHeight;
    private boolean mKeyboardVisible;
    private int mDefaultHeight;
    private int mDefaultWidth;
    private int mInitialLayoutHeight;
    private int mInitialLayoutWidth;
    private int mViewIndexInParent;
    private ViewState mViewState = ViewState.DEFAULT;
    private boolean mHasInitialLayoutParams;
    private OrmmaUtilityController mUtilityController;
    private String mLocalFilePath;
    private String mOrmmaPath;
    private String mBridgePath;
    private String mMapsApiKey;
    
    public OrmmaView(Context context) {
        super(context);
        initialize();
    }
    
    private void initialize() {
        setScrollContainer(false);
        setBackgroundColor(Color.TRANSPARENT);
        setVerticalScrollBarEnabled(false);
        setHorizontalScrollBarEnabled(false);
        getSettings().setJavaScriptEnabled(true);
        
        mHandler = new OrmmaMessageHandler();
        mContentViewHeight = getContentViewHeight();
        mUtilityController = new OrmmaUtilityController(this, this.getContext());
        addJavascriptInterface(mUtilityController, "ORMMAUtilityControllerBridge");
        
        initializeOrmmaScriptPaths();
        initializeScreenDensity();
        beginDetectingKeyboardStateChanges();
        
        mWebViewClient = new OrmmaWebViewClient();
        setWebViewClient(mWebViewClient);
        
        mWebChromeClient = new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                Log.d(LOGTAG, message);
                return false;
            }
        };
        setWebChromeClient(mWebChromeClient);
    }
    
    private int getContentViewHeight() {
        View contentView = getRootView().findViewById(android.R.id.content);
        return (contentView != null) ? contentView.getHeight() : -1;
    }
    
    private void initializeOrmmaScriptPaths() {
        try {
            if (mOrmmaPath == null) {
                mOrmmaPath = mUtilityController.copyRawResourceToAdDirectory(
                        com.mopub.mobileads.R.raw.ormma, "ormma.js");
            }
            if (mBridgePath == null) {
                mBridgePath = mUtilityController.copyRawResourceToAdDirectory(
                        com.mopub.mobileads.R.raw.ormma_bridge, "ormma_bridge.js");
            }
        } catch (IOException e) {
            Log.e(LOGTAG, "Error: could not write required ORMMA script files to disk.");
            if (mListener != null) mListener.onFailure();
        }
    }
    
    private void initializeScreenDensity() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager wm = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        mDensity = metrics.density;
    }
    
    private void beginDetectingKeyboardStateChanges() {
        getViewTreeObserver().addOnGlobalLayoutListener(this); // TODO Remove?
    }
    
    private class OrmmaWebViewClient extends WebViewClient {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, 
                String failingUrl) {
            Log.d(LOGTAG, "Error:" + description);
            super.onReceivedError(view, errorCode, description, failingUrl);
        }
        
        @Override
        public void onPageFinished(WebView view, String url) {
            mDefaultHeight = (int) (getHeight() / mDensity);
            mDefaultWidth = (int) (getWidth() / mDensity);
            mUtilityController.init(mDensity);
        }
        
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            Uri uri = Uri.parse(url);
            try {
                // TODO: For registered protocols.
                if (url.startsWith("mopub:")) return true;
                
                Intent i;
                
                // Handle special URIs using the appropriate Intents.
                if (url.startsWith("tel:")) i = new Intent(Intent.ACTION_DIAL, uri);
                if (url.startsWith("mailto:")) i = new Intent(Intent.ACTION_VIEW, uri);
                else {
                    i = new Intent();
                    i.setAction(Intent.ACTION_VIEW);
                    i.setData(uri);
                }
                
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                getContext().startActivity(i);
                return true;
            } catch (Exception e) {
                try {
                    launchActionViewWithIntentData(uri);
                    return true;
                } catch (Exception e2) {
                    return false;
                }
            }
        }
        
        @Override
        public void onLoadResource(WebView view, String url) {
            Log.d(LOGTAG, "lr:" + url);
        }
    }
    
    private void launchActionViewWithIntentData(Uri uri) {
        Intent i = new Intent();
        i.setAction(Intent.ACTION_VIEW);
        i.setData(uri);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
    }
    
    @Override
    public void clearView() {
        reset();
        super.clearView();
    }
    
    private void reset() {
        if (mViewState == ViewState.EXPANDED) handleCloseExpanded();
        else if (mViewState == ViewState.RESIZED) handleCloseResized();
        
        invalidate();
        mUtilityController.deleteCachedAds();
        mUtilityController.stopAllListeners();
        resetLayout();
    }
    
    @Override
    public void loadUrl(String url) {
        if (!URLUtil.isValidUrl(url)) return;
        
        try {
            URL u = new URL(url);
            InputStream is = u.openStream();
            loadInputStream(is);
            return;
        } catch (MalformedURLException e) {
            logExceptionAndNotifyListener(e);
            return;
        } catch (IOException e) {
            logExceptionAndNotifyListener(e);
            return;
        }
    }
    
    public void loadInputStream(InputStream is) {
        try {
            mLocalFilePath = mUtilityController.writeInputStreamToDisk(is, CURRENT_FILE, true);
        } catch (IllegalArgumentException e) {
            logExceptionAndNotifyListener(e);
            return;
        } catch (IOException e) {
            logExceptionAndNotifyListener(e);
            return;
        }
        String url = "file://" + mLocalFilePath + File.separator + CURRENT_FILE;
        super.loadUrl(url);
    }
    
    public void loadHtmlData(String data) {
        try {
            mLocalFilePath = mUtilityController.writeHtmlToDisk(data, CURRENT_FILE, true);
        } catch (IllegalArgumentException e) {
            logExceptionAndNotifyListener(e);
            return;
        } catch (IOException e) {
            logExceptionAndNotifyListener(e);
            return;
        }
        String url = "file://" + mLocalFilePath + File.separator + CURRENT_FILE;
        super.loadUrl(url);
    }
    
    public void loadFile(File f) {
        FileInputStream fis;
        try {
            fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
            logExceptionAndNotifyListener(e);;
            return;
        }
        
        loadInputStream(fis);
    }
    
    private void logExceptionAndNotifyListener(Exception e) {
        Log.e(LOGTAG, e.getMessage());
        if (mListener != null) mListener.onFailure();
    }
    
    public void injectJavaScript(String str) {
        if (str != null) super.loadUrl("javascript:" + str);
    }
    
    public void setListener(OrmmaViewListener listener) {
        mListener = listener;
    }
    
    public void removeListener() {
        mListener = null;
    }
    
    public String getOrmmaPath() {
        return mOrmmaPath;
    }
    
    public String getBridgePath() {
        return mBridgePath;
    }
    
    public void setMapsApiKey(String key) {
        mMapsApiKey = key;
    }

    public void setMaxSize(int w, int h) {
        mUtilityController.setMaxSize(w, h);
    }
    
    public boolean isExpanded() {
        return (mViewState == ViewState.EXPANDED);
    }
    
    public String getSize() {
        return "{ width: " + (int) (getWidth() / mDensity) + ", " +
                "height: " + (int) (getHeight() / mDensity) + "}";
    }
    
    public String getState() {
        return mViewState.toString().toLowerCase();
    }
    
    public void raiseError(String message, String action) {
        Message msg = mHandler.obtainMessage(MESSAGE_RAISE_ERROR);
        Bundle data = new Bundle();
        data.putString(ERROR_MESSAGE, message);
        data.putString(ERROR_ACTION, action);
        msg.setData(data);
        mHandler.sendMessage(msg);
    }
    
    public void resize(int width, int height) {
        Message msg = mHandler.obtainMessage(MESSAGE_RESIZE);
        
        Bundle data = new Bundle();
        data.putInt(RESIZE_WIDTH, width);
        data.putInt(RESIZE_HEIGHT, height);
        msg.setData(data);
        
        mHandler.sendMessage(msg);
    }
    
    public void close() {
        mHandler.sendEmptyMessage(MESSAGE_CLOSE);
    }
    
    public void hide() {
        mHandler.sendEmptyMessage(MESSAGE_HIDE);
    }
    
    public void show() {
        mHandler.sendEmptyMessage(MESSAGE_SHOW);
    }
    
    public void expand(Dimensions dimensions, String url, Properties properties) {
        Message msg = mHandler.obtainMessage(MESSAGE_EXPAND);
        
        Bundle data = new Bundle();
        data.putParcelable(DIMENSIONS, dimensions);
        data.putString(EXPAND_URL, url);
        data.putParcelable(EXPAND_PROPERTIES, properties);
        msg.setData(data);
        
        mHandler.sendMessage(msg);
    }
    
    public void open(String url, boolean back, boolean forward, boolean refresh) {
        Log.d(LOGTAG, "open: " + url);
        
        Intent i = new Intent(getContext(), OrmmaBrowser.class);
        i.putExtra(OrmmaBrowser.URL_EXTRA, url);
        i.putExtra(OrmmaBrowser.SHOW_BACK_EXTRA, back);
        i.putExtra(OrmmaBrowser.SHOW_FORWARD_EXTRA, forward);
        i.putExtra(OrmmaBrowser.SHOW_REFRESH_EXTRA, refresh);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(i);
    }
    
    public void openMap(String url, boolean fullscreen) {
        Log.d(LOGTAG, "openMap with url: " + url);
        
        url = url.trim();
        // TODO: Encode this URL?
        
        if (fullscreen) startMapsActivity(url);
        else displayEmbeddedMapView(url);
    }
    
    private void startMapsActivity(String url) {
        try {
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getContext().startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            Log.e(LOGTAG, "Failed to open the maps activity.");
        }
    }
    
    private void displayEmbeddedMapView(String url) {
        if (mMapsApiKey == null) {
            Toast.makeText(getContext(), "Error: no Google Maps API key provided for embedded map.",
                    Toast.LENGTH_LONG).show();
            return;
        }
        
        MapView mapView = new MapView(getContext(), mMapsApiKey);
        mapView.setBuiltInZoomControls(true);
        
        // MPADD
    }
    
    public void playAudio(String url, boolean autoPlay, boolean controls, boolean loop, 
            boolean position, String startStyle, String stopStyle) {
        PlayerProperties properties = new PlayerProperties();
        properties.setProperties(false, autoPlay, controls, position,loop, startStyle, stopStyle);

        Bundle data = new Bundle();
        data.putString(ACTION_KEY, ACTION.PLAY_AUDIO.toString());
        data.putString(EXPAND_URL, url);
        data.putParcelable(PLAYER_PROPERTIES, properties);

        if (properties.isFullScreen()) {
            try {
                Intent intent = new Intent(getContext(), OrmmaActionHandler.class);
                intent.putExtras(data);
                getContext().startActivity(intent);
            }
            catch (ActivityNotFoundException e){
                Log.e(LOGTAG, "Failed to open the OrmmaActionHandler activity.");
            }
        } else {
            Message msg = mHandler.obtainMessage(MESSAGE_PLAY_AUDIO);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }
    }
    
    public void playVideo(String url, boolean audioMuted, boolean autoPlay, boolean controls,
            boolean loop, Dimensions d, String startStyle, String stopStyle) {
        PlayerProperties properties = new PlayerProperties();
        properties.setProperties(audioMuted, autoPlay, controls, false, loop, startStyle,
                stopStyle);

        Bundle data = new Bundle();
        data.putString(EXPAND_URL, url);
        data.putString(ACTION_KEY, ACTION.PLAY_VIDEO.toString());
        data.putParcelable(PLAYER_PROPERTIES, properties);
        if (d != null) data.putParcelable(DIMENSIONS, d);

        if (properties.isFullScreen()) {
            try {
                Intent intent = new Intent(getContext(), OrmmaActionHandler.class);
                intent.putExtras(data);
                getContext().startActivity(intent);
            }
            catch (ActivityNotFoundException e) {
                Log.e(LOGTAG, "Failed to open the OrmmaActionHandler activity.");
            }
        } else if (d != null) {
            Message msg = mHandler.obtainMessage(MESSAGE_PLAY_VIDEO);
            msg.setData(data);
            mHandler.sendMessage(msg);
        }
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    
    private class OrmmaMessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Bundle data = msg.getData();
            switch (msg.what) {
                case MESSAGE_RESIZE: handleResize(data); break;
                case MESSAGE_CLOSE: handleClose(); break;
                case MESSAGE_HIDE: ormmaSetHidden(true); break;
                case MESSAGE_SHOW: ormmaSetHidden(false); break;
                case MESSAGE_EXPAND: handleExpand(data); break;
                case MESSAGE_SEND_EXPAND_CLOSE: handleSendExpandClose(); break;
                case MESSAGE_OPEN: handleOpen(); break;
                case MESSAGE_PLAY_VIDEO: handlePlayVideo(data); break;
                case MESSAGE_PLAY_AUDIO: handlePlayAudio(data); break;
                case MESSAGE_RAISE_ERROR: handleRaiseError(data); break;
                default: super.handleMessage(msg); break;
            }
        }
    }
    
    private void handleResize(Bundle data) {
        mViewState = ViewState.RESIZED;
        ViewGroup.LayoutParams lp = getLayoutParams();
        lp.height = data.getInt(RESIZE_HEIGHT, lp.height);
        lp.width = data.getInt(RESIZE_WIDTH, lp.width);
        String injection = "window.ormmaview.fireChangeEvent({ state: \'resized\',"
                + " size: { width: "
                + lp.width
                + ", "
                + "height: "
                + lp.height + "}});";
        injectJavaScript(injection);
        requestLayout();
        if (mListener != null) mListener.onResize();
    }
    
    private void handleClose() {
        switch (mViewState) {
            case RESIZED: handleCloseResized(); break;
            case EXPANDED: handleCloseExpanded(); break;
        }
    }
    
    private void handleCloseResized() {
        if (mListener != null) mListener.onResizeClose();
        String injection = "window.ormmaview.fireChangeEvent({ state: \'default\',"
                + " size: "
                + "{ width: "
                + mDefaultWidth
                + ", "
                + "height: "
                + mDefaultHeight + "}" + "});";
        Log.d(LOGTAG, "closeResized: injection: " + injection);
        injectJavaScript(injection);
        resetLayout();
    }
    
    private void resetLayout() {
        ViewGroup.LayoutParams lp = getLayoutParams();
        if (mHasInitialLayoutParams) {
            lp.height = mInitialLayoutHeight;
            lp.width = mInitialLayoutWidth;
        }
        setVisibility(VISIBLE);
        requestLayout();
    }
    
    private void handleCloseExpanded() {
        resetContents();

        String injection = "window.ormmaview.fireChangeEvent({ state: \'default\',"
                + " size: "
                + "{ width: "
                + mDefaultWidth
                + ", "
                + "height: "
                + mDefaultHeight + "}" + "});";
        Log.d(LOGTAG, "handleCloseExpanded: injection: " + injection);
        injectJavaScript(injection);

        mViewState = ViewState.DEFAULT;

        mHandler.sendEmptyMessage(MESSAGE_SEND_EXPAND_CLOSE);
        setVisibility(VISIBLE);
    }
    
    /* Revert to earlier ad state by replacing the placeholder view with this OrmmaView. */
    private void resetContents() {
        FrameLayout contentView = (FrameLayout) getRootView().findViewById(android.R.id.content);
        FrameLayout placeholder = (FrameLayout) getRootView().findViewById(PLACEHOLDER_ID);
        FrameLayout background = (FrameLayout) getRootView().findViewById(BACKGROUND_ID);
       
        ViewGroup parent = (ViewGroup) placeholder.getParent();
        
        background.removeView(this);
        contentView.removeView(background);
        
        resetLayout();
        
        parent.addView(this, mViewIndexInParent);
        parent.removeView(placeholder);
        parent.invalidate();
    }
    
    private void ormmaSetHidden(boolean hidden) {
        int visibility = (hidden) ? View.INVISIBLE : View.VISIBLE;
        String state = (hidden) ? "hidden" : "default";
        setVisibility(visibility);
        injectJavaScript("window.ormmaview.fireChangeEvent({ state: \'" + state + "\' });");
    }
    
    private void handleExpand(Bundle data) {
        Dimensions d = (Dimensions) data.getParcelable(DIMENSIONS);
        String url = data.getString(EXPAND_URL);
        Properties p = data.getParcelable(EXPAND_PROPERTIES);
        
        if (URLUtil.isValidUrl(url)) loadUrl(url);
        
        FrameLayout expandedContainer = expandedContentContainer(d);
        if (p.useBackground) {
            // Convert 24-bit color value and 8-bit opacity to 32-bit color.
            int color = p.backgroundColor | ((int) (p.backgroundOpacity * 0xFF) * 0x10000000);
            expandedContainer.setBackgroundColor(color);
        }
        
        String injection = "window.ormmaview.fireChangeEvent({ " + 
                "state: \'expanded\', " +
                "size: { " + 
                    "width: " + (int) (d.width / mDensity) + ", " + 
                    "height: " + (int) (d.height / mDensity) + 
                "}});";
        Log.d(LOGTAG, "handleExpand: injection: " + injection);
        injectJavaScript(injection);
        if (mListener != null) mListener.onExpand();
        mViewState = ViewState.EXPANDED;
    }
    
    private FrameLayout expandedContentContainer(Dimensions d) {
        FrameLayout contentView = (FrameLayout) getRootView().findViewById(android.R.id.content);
        ViewGroup parent = (ViewGroup) getParent();
        insertPlaceholderForViewInHierarchy(this, parent);
        
        FrameLayout bg = new FrameLayout(getContext());
        bg.setId(BACKGROUND_ID);
        bg.setPadding((int) d.x, (int) d.y, 0, 0);
        
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(
                (int) d.width, (int) d.height);
        fl.topMargin = (int) d.x;
        fl.leftMargin = (int) d.y;
        
        bg.addView(this, fl);
        
        contentView.addView(bg, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
            
        return bg;
    }
    
    private void insertPlaceholderForViewInHierarchy(View viewToRemove, ViewGroup parent) {
        if ((ViewGroup) viewToRemove.getParent() != parent) return;
            
        FrameLayout placeholder = new FrameLayout(getContext());
        placeholder.setId(PLACEHOLDER_ID);
        ViewGroup.LayoutParams lp = new ViewGroup.LayoutParams(getWidth(), getHeight());
        
        int index = 0;
        int count = parent.getChildCount();
        for (index = 0; index < count; index++) {
            if (parent.getChildAt(index) == viewToRemove) break;
        }
        
        mViewIndexInParent = index;
        parent.addView(placeholder, index, lp);
        parent.removeView(viewToRemove);
    }
    
    private void handleSendExpandClose() {
        if (mListener != null) mListener.onExpandClose();
    }
    
    private void handleOpen() {
        mViewState = ViewState.LEFT_BEHIND;
    }
    
    private void handlePlayAudio(Bundle data) {
        PlayerProperties properties = (PlayerProperties) data.getParcelable(PLAYER_PROPERTIES);
        String url = data.getString(EXPAND_URL);
        
        OrmmaPlayer audioPlayer = getPlayer();
        audioPlayer.setPlayData(properties, url);
        audioPlayer.setLayoutParams(new ViewGroup.LayoutParams(1, 1));
        
        ViewGroup parent = (ViewGroup) getParent();
        parent.addView(audioPlayer);
        audioPlayer.playAudio();
    }
    
    private void handlePlayVideo(Bundle data) {
        PlayerProperties properties = (PlayerProperties) data.getParcelable(PLAYER_PROPERTIES);
        Dimensions d = (Dimensions) data.getParcelable(DIMENSIONS);
        String url = data.getString(EXPAND_URL);
        
        OrmmaPlayer videoPlayer = getPlayer();
        videoPlayer.setPlayData(properties, url);
        videoPlayer.setListener(new OrmmaPlayer.OrmmaPlayerListener() {
            @Override
            public void onPrepared() {
            }
            
            @Override
            public void onError() {
                onComplete();
            }
            
            @Override
            public void onComplete() {
                FrameLayout videoContainer = 
                        (FrameLayout) getRootView().findViewById(BACKGROUND_ID);
                ViewGroup parent = (ViewGroup) videoContainer.getParent();
                parent.removeView(videoContainer);
                setVisibility(View.VISIBLE);
            }
        });
        
        addVideoPlayerToContentView(videoPlayer, d);
        setVisibility(View.INVISIBLE);
        videoPlayer.playVideo();
    }
    
    private OrmmaPlayer getPlayer() {
        if (sOrmmaPlayer != null) sOrmmaPlayer.releasePlayer();
        sOrmmaPlayer = new OrmmaPlayer(getContext());
        return sOrmmaPlayer;
    }
    
    private void addVideoPlayerToContentView(OrmmaPlayer videoPlayer, Dimensions d) {
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(
                (int) d.width, (int) d.height);
        fl.topMargin = (int) d.x;
        fl.leftMargin = (int) d.y;
        videoPlayer.setLayoutParams(fl);
        
        FrameLayout videoContainer = new FrameLayout(getContext());
        videoContainer.setId(BACKGROUND_ID);
        videoContainer.setPadding((int) d.x, (int) d.y, 0, 0);
        videoContainer.addView(videoPlayer);
        
        FrameLayout contentView = (FrameLayout) getRootView().findViewById(android.R.id.content);
        contentView.addView(videoContainer, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.FILL_PARENT, FrameLayout.LayoutParams.FILL_PARENT));
    }
    
    private void handleRaiseError(Bundle data) {
        String msg = data.getString(ERROR_MESSAGE);
        String action = data.getString(ERROR_ACTION);
        String injection = "window.ormmaview.fireErrorEvent(\"" + msg + "\", \"" + action +
                "\")";
		injectJavaScript(injection);
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    
    /*
     * Used to determine the initial height and width of the view.
     */
    @Override
    protected void onAttachedToWindow() {
        if (!mHasInitialLayoutParams) {
            ViewGroup.LayoutParams lp = getLayoutParams();
            mInitialLayoutHeight = lp.height;
            mInitialLayoutWidth = lp.width;
            mHasInitialLayoutParams = true;
        }
        super.onAttachedToWindow();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.webkit.WebView#saveState(android.os.Bundle)
     */
    @Override
    public WebBackForwardList saveState(Bundle outState) {
        outState.putString(AD_PATH, mLocalFilePath);
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.webkit.WebView#restoreState(android.os.Bundle)
     */
    @Override
    public WebBackForwardList restoreState(Bundle savedInstanceState) {
        mLocalFilePath = savedInstanceState.getString(AD_PATH);
        String url = "file://" + mLocalFilePath + File.separator + CURRENT_FILE;
        super.loadUrl(url);
        return null;
    }
    
    @Override
    protected void onDetachedFromWindow() {     
        mUtilityController.stopAllListeners();
        super.onDetachedFromWindow();
    }

    /*
     * Used to get the state of the soft keyboard.
     */
    @Override
    public void onGlobalLayout() {
        String injection = "";
        
        /* If the keyboard wasn't visible before, and the content view height has changed, then
         * we can assume the keyboard is now showing. */
        if (!mKeyboardVisible && mContentViewHeight >= 0 && getContentViewHeight() >= 0 &&
                (mContentViewHeight != getContentViewHeight())) {
            mKeyboardVisible = true;
            injection = "window.ormmaview.fireChangeEvent({ keyboardState: true });";
        } 
        
        // TODO: Confirm that the keyboard hidden triggers this.
        else if (mKeyboardVisible && mContentViewHeight >= 0 && getContentViewHeight() >= 0 &&
                (mContentViewHeight == getContentViewHeight())) {
            mKeyboardVisible = false;
            injection = "window.ormmaview.fireChangeEvent({ keyboardState: false });";
        }
        
        injectJavaScript(injection);
        
        if (mContentViewHeight < 0) mContentViewHeight = getContentViewHeight();
    }
}
