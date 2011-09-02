package com.mopub.ormma;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.CookieSyncManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;
import android.widget.Toast;

import com.mopub.mobileads.R;

public class OrmmaBrowser extends Activity {
    
    public static final String URL_EXTRA = "extra_url";
    public static final String SHOW_BACK_EXTRA = "open_show_back";
    public static final String SHOW_FORWARD_EXTRA = "open_show_forward";
    public static final String SHOW_REFRESH_EXTRA = "open_show_refresh";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        getWindow().setFeatureInt(Window.FEATURE_PROGRESS, Window.PROGRESS_VISIBILITY_ON);
        
        setContentView(R.layout.ormma_browser);
        
        Intent intent = getIntent();
        initializeWebView(intent);
        initializeButtons(intent);
        enableCookies();
    }
    
    private void initializeWebView(Intent intent) {
        WebView webView = (WebView) findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.loadUrl(intent.getStringExtra(URL_EXTRA));
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onReceivedError(WebView view, int errorCode, String description, 
                    String failingUrl) {
                Activity a = (Activity) view.getContext();
                Toast.makeText(a, "ORMMA error: " + description, Toast.LENGTH_SHORT).show();
            }
            
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                return true;
            }
            
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                ImageButton forwardButton = (ImageButton) findViewById(R.id.browserForwardButton);
                forwardButton.setImageResource(R.drawable.unrightarrow);
            }
            
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                ImageButton forwardButton = (ImageButton) findViewById(R.id.browserForwardButton);
                if (view.canGoForward()) forwardButton.setImageResource(R.drawable.rightarrow);
                else forwardButton.setImageResource(R.drawable.unrightarrow);
            }
        });
        
        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int progress) {
                Activity a = (Activity) view.getContext();
                a.setTitle("Loading...");
                a.setProgress(progress * 100);
                if (progress == 100) a.setTitle(view.getUrl());
            }
        });
    }
    
    private void initializeButtons(Intent intent) {
        ImageButton backButton = (ImageButton) findViewById(R.id.browserBackButton);
        if (!intent.getBooleanExtra(SHOW_BACK_EXTRA, true)) {
            backButton.setVisibility(ViewGroup.INVISIBLE);
        }
        backButton.setBackgroundColor(Color.TRANSPARENT);
        backButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = (WebView) findViewById(R.id.webView);
                if (webView.canGoBack()) webView.goBack();
                else OrmmaBrowser.this.finish();
            }
        });
        
        ImageButton forwardButton = (ImageButton) findViewById(R.id.browserForwardButton);
        if (!intent.getBooleanExtra(SHOW_FORWARD_EXTRA, true)) {
            forwardButton.setVisibility(ViewGroup.INVISIBLE);
        }
        forwardButton.setBackgroundColor(Color.TRANSPARENT);
        forwardButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = (WebView) findViewById(R.id.webView);
                if (webView.canGoForward()) webView.goForward();
            }
        });
        
        ImageButton refreshButton = (ImageButton) findViewById(R.id.browserRefreshButton);
        if (!intent.getBooleanExtra(SHOW_REFRESH_EXTRA, true)) {
            refreshButton.setVisibility(ViewGroup.INVISIBLE);
        }
        refreshButton.setBackgroundColor(Color.TRANSPARENT);
        refreshButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                WebView webView = (WebView) findViewById(R.id.webView);
                webView.reload();
            }
        });
        
        ImageButton closeButton = (ImageButton) findViewById(R.id.browserCloseButton);
        closeButton.setBackgroundColor(Color.TRANSPARENT);
        closeButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                OrmmaBrowser.this.finish();
            }
        });
    }
    
    private void enableCookies() {
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().startSync();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        CookieSyncManager.getInstance().stopSync();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        CookieSyncManager.getInstance().startSync();
    }
}
