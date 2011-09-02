/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma.controller;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.mopub.ormma.Defines;
import com.mopub.ormma.OrmmaView;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;
import android.widget.Toast;

public class OrmmaUtilityController extends OrmmaController {
    
    private static final String LOGTAG = "OrmmaUtilityController";
    
    private OrmmaAssetController mAssetController;
    private OrmmaDisplayController mDisplayController;
    private OrmmaLocationController mLocationController;
    private OrmmaNetworkController mNetworkController;
    private OrmmaSensorController mSensorController;
    
    public OrmmaUtilityController(OrmmaView ormmaView, Context context) {
        super(ormmaView, context);
        
        mAssetController = new OrmmaAssetController(ormmaView, context);
        mDisplayController = new OrmmaDisplayController(ormmaView, context);
        mLocationController = new OrmmaLocationController(ormmaView, context);
        mNetworkController = new OrmmaNetworkController(ormmaView, context);
        mSensorController = new OrmmaSensorController(ormmaView, context);
        
        /* Bind controller objects to JavaScript. */
        ormmaView.addJavascriptInterface(mAssetController, "ORMMAAssetControllerBridge");
        ormmaView.addJavascriptInterface(mDisplayController, "ORMMADisplayControllerBridge");
        ormmaView.addJavascriptInterface(mLocationController, "ORMMALocationControllerBridge");
        ormmaView.addJavascriptInterface(mNetworkController, "ORMMANetworkControllerBridge");
        ormmaView.addJavascriptInterface(mSensorController, "ORMMASensorControllerBridge");
    }

    public void init(float density) {
        String injection = "window.ormmaview.fireChangeEvent({ state: \'default\', "+
                "network: \'" + mNetworkController.getNetwork() + "\', " +
                "size: " + mDisplayController.getSize() + ", " +
                "maxSize: " + mDisplayController.getMaxSize() + ", " +
                "screenSize: " + mDisplayController.getScreenSize() + ", " +
                "defaultPosition: { x:" + (int) (mOrmmaView.getLeft() / density) + 
                    ", y:" + (int) (mOrmmaView.getTop() / density) + 
                    ", width: " + (int) (mOrmmaView.getWidth() / density) + 
                    ", height: " + (int) (mOrmmaView.getHeight() / density) + "}, " +
                "orientation: " + mDisplayController.getOrientation() + ", " +
                getSupportsString() +
                "});";
        Log.d(LOGTAG, "init: injection: " + injection);
        mOrmmaView.injectJavaScript(injection);
    }
    
    private String getSupportsString() {
        String supports = "supports: ['level-1', 'level-2', 'screen', 'orientation', 'network', " +
                "'video', 'audio', 'map', 'email'";
        
        // Append capabilities based on manifest permissions.
        if (mLocationController.allowsLocationServices() && manifestAllowsLocationServices()) {
            supports += ", 'location'";
        }
        
        if (manifestAllowsSms()) supports += ", 'sms'";
        if (manifestAllowsPhoneCalls()) supports += ", 'phone'";
        if (manifestAllowsCalendarAccess()) supports += ", 'calendar'";
        
        supports += "]";
        Log.d(LOGTAG, "getSupportsString: " + supports);
        return supports;
    }
    
    private boolean manifestAllowsLocationServices() {
        return ((mContext.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) || 
                (mContext.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED));
    }
    
    private boolean manifestAllowsSms() {
        return (mContext.checkCallingOrSelfPermission(android.Manifest.permission.SEND_SMS) == 
            PackageManager.PERMISSION_GRANTED);
    }
    
    private boolean manifestAllowsPhoneCalls() {
        return (mContext.checkCallingOrSelfPermission(android.Manifest.permission.CALL_PHONE) == 
            PackageManager.PERMISSION_GRANTED);
    }
    
    private boolean manifestAllowsCalendarAccess() {
        return ((mContext.checkCallingOrSelfPermission(android.Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED) &&
                (mContext.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED));
    }

    public void setMaxSize(int w, int h) {
        mDisplayController.setMaxSize(w, h);
    }
    
    public String writeInputStreamToDisk(InputStream is, String filename, 
            boolean useHashedDirectory) throws IllegalArgumentException, IOException {
        return mAssetController.writeInputStreamToDisk(is, filename, useHashedDirectory);
    }
    
    public String writeHtmlToDisk(String html, String filename, boolean useHashedDirectory) 
            throws IllegalArgumentException, IOException {
        return mAssetController.writeHtmlToDisk(html, filename, useHashedDirectory);
    }
    
    public String copyRawResourceToAdDirectory(int resourceId, String dstFileName) 
            throws IOException {
        return mAssetController.copyRawResourceToAdDirectory(resourceId, dstFileName);
    }
    
    @Override
    public void stopAllListeners() {
        mAssetController.stopAllListeners();
        mDisplayController.stopAllListeners();
        mLocationController.stopAllListeners();
        mNetworkController.stopAllListeners();
        mSensorController.stopAllListeners();
    }
    
    public void deleteCachedAds() {
        mAssetController.deleteCachedAds();
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////
    // "Bridge" methods, called in ormma_bridge.js

    public void showAlert(String message) {
        Log.e(LOGTAG, message);
    }
    
    /* Activate a device event listener (e.g. location, accelerometer, etc.). */
    public void activate(String event) {
        Log.d(LOGTAG, "activate: " + event);
        if (event.equalsIgnoreCase(Defines.Events.NETWORK_CHANGE)) {
            mNetworkController.startNetworkListener();
        } else if (event.equalsIgnoreCase(Defines.Events.LOCATION_CHANGE) && 
                mLocationController.allowsLocationServices()) {
            mLocationController.startLocationListener();
        } else if (event.equalsIgnoreCase(Defines.Events.SHAKE)) {
            mSensorController.startShakeListener();
        } else if (event.equalsIgnoreCase(Defines.Events.TILT_CHANGE)) {
            mSensorController.startTiltListener();
        } else if (event.equalsIgnoreCase(Defines.Events.HEADING_CHANGE)) {
            mSensorController.startHeadingListener();
        } else if (event.equalsIgnoreCase(Defines.Events.ORIENTATION_CHANGE)) {
            mDisplayController.startConfigurationListener();
        }
    }
    
    /* Deactivate a device event listener. */
    public void deactivate(String event) {
        Log.d(LOGTAG, "deactivate: " + event);
        if (event.equalsIgnoreCase(Defines.Events.NETWORK_CHANGE)) {
            mNetworkController.stopNetworkListener();
        } else if (event.equalsIgnoreCase(Defines.Events.LOCATION_CHANGE)) {
            mLocationController.stopLocationListener();
        } else if (event.equalsIgnoreCase(Defines.Events.SHAKE)) {
            mSensorController.stopShakeListener();
        } else if (event.equalsIgnoreCase(Defines.Events.TILT_CHANGE)) {
            mSensorController.stopTiltListener();
        } else if (event.equalsIgnoreCase(Defines.Events.HEADING_CHANGE)) {
            mSensorController.stopHeadingListener();
        } else if (event.equalsIgnoreCase(Defines.Events.ORIENTATION_CHANGE)) {
            mDisplayController.stopConfigurationListener();
        }
    }
    
    /* Create a calendar event. */
    public void createEvent(final String date, final String title, final String body) {
        Log.d(LOGTAG, "createEvent: date: " + date + " title: " + title + " body: " + body);
        final ContentResolver cr = mContext.getContentResolver();
        Cursor cursor;
        final String[] cols = new String[] { "_id", "displayName", "_sync_account" };
        
        if (Integer.parseInt(Build.VERSION.SDK) == 8) // 2.2 or higher
            cursor = cr.query(Uri.parse("content://com.android.calendar/calendars"),
                    cols, null, null, null);
        else
            cursor = cr.query(Uri.parse("content://calendar/calendars"), 
                    cols, null, null, null);
        
        if (cursor == null || (cursor != null && !cursor.moveToFirst()) ) {
            // No CalendarID found
            Toast.makeText(mContext, "No calendar account found", Toast.LENGTH_LONG).show();
            if(cursor != null)
                cursor.close();
            return;
        }
            
        if(cursor.getCount() == 1){
            addCalendarEvent(cursor.getInt(0), date, title, body);
        }
        else{
            final List<Map<String, String>> entries = new ArrayList<Map<String,String>>();

            for (int i = 0; i < cursor.getCount(); i++) {
                Map<String,String> entry = new HashMap<String, String>();
                entry.put("ID", cursor.getString(0));
                entry.put("NAME", cursor.getString(1));
                entry.put("EMAILID", cursor.getString(2));
                entries.add(entry);
                cursor.moveToNext();
            }

            AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
            builder.setTitle("Choose Calendar to save event to");
            ListAdapter adapter = new SimpleAdapter(mContext, 
                    entries, 
                    android.R.layout.two_line_list_item,
                    new String[] {"NAME", "EMAILID"},
                    new int[] {android.R.id.text1, android.R.id.text2});
            
            
            builder.setSingleChoiceItems(adapter, -1, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Map<String, String> entry = entries.get(which);
                    addCalendarEvent(Integer.parseInt(entry.get("ID")), date, title, body);
                    dialog.cancel();
                }

            });

            builder.create().show();
        }
        cursor.close();
    }
    
    private void addCalendarEvent(int callId, final String date, final String title, final String body) {
        final ContentResolver cr = mContext.getContentResolver();
        long dtStart = Long.parseLong(date);
        long dtEnd = dtStart + 60 * 1000 * 60;
        ContentValues cv = new ContentValues();
        cv.put("calendar_id", callId);
        cv.put("title", title);
        cv.put("description", body);
        cv.put("dtstart", dtStart);
        cv.put("hasAlarm", 1);
        cv.put("dtend", dtEnd);

        Uri newEvent;
        if (Integer.parseInt(Build.VERSION.SDK) == 8)
            newEvent = cr.insert(Uri.parse("content://com.android.calendar/events"), cv);
        else
            newEvent = cr.insert(Uri.parse("content://calendar/events"), cv);

        if (newEvent != null) {
            long id = Long.parseLong(newEvent.getLastPathSegment());
            ContentValues values = new ContentValues();
            values.put("event_id", id);
            values.put("method", 1);
            values.put("minutes", 15); // 15 minutes
            if (Integer.parseInt(Build.VERSION.SDK) == 8)
                cr.insert(Uri.parse("content://com.android.calendar/reminders"), values);
            else
                cr.insert(Uri.parse("content://calendar/reminders"), values);
        }

        Toast.makeText(mContext, "Event added to calendar", Toast.LENGTH_SHORT).show();
    }
    
    public void makeCall(String phoneNumber) {
        String url = TextUtils.isEmpty(phoneNumber) ? null : "tel:" + phoneNumber;
        if (url == null) mOrmmaView.raiseError("Tried to call an invalid phone number.", "makeCall");
        else {
            Intent i = new Intent(Intent.ACTION_CALL, Uri.parse(url));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(i);
        }
    }
    
    public void sendMail(String recipient, String subject, String body) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("plain/text");
        i.putExtra(android.content.Intent.EXTRA_EMAIL, new String[] { recipient });
        i.putExtra(android.content.Intent.EXTRA_SUBJECT, subject);
        i.putExtra(android.content.Intent.EXTRA_TEXT, body);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }
    
    public void sendSMS(String recipient, String body) {
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.putExtra("address", recipient);
        i.putExtra("sms_body", body);
        i.setType("vnd.android-dir/mms-sms");
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }
}
