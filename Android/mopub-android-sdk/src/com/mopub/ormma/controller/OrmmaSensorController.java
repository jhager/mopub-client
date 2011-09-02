/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma.controller;

import java.util.List;

import com.mopub.ormma.OrmmaView;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class OrmmaSensorController extends OrmmaController {
    
    private static final String LOGTAG = "OrmmaSensorController";
    
    private AccelListener mAccelListener;
    private float mLastX = 0;
    private float mLastY = 0;
    private float mLastZ = 0;
    
    public OrmmaSensorController(OrmmaView ormmaView, Context context) {
        super(ormmaView, context);
        mAccelListener = new AccelListener(context);
    }
    
    public void startTiltListener() {
        mAccelListener.startTrackingTilt();
    }
    
    public void startShakeListener() {
        mAccelListener.startTrackingShake();
    }

    public void stopTiltListener() {
        mAccelListener.stopTrackingTilt();
    }

    public void stopShakeListener() {
        mAccelListener.stopTrackingShake();
    }

    public void startHeadingListener() {
        mAccelListener.startTrackingHeading();
    }

    public void stopHeadingListener() {
        mAccelListener.stopTrackingHeading();
    }

    public String getTilt() {
        String tilt = "{ x : \"" + mLastX + "\", y : \"" + mLastY + "\", z : \"" + mLastZ + "\"}";
        Log.d(LOGTAG, "getTilt: " + tilt);
        return tilt;
    }

    public float getHeading() {
        Log.d(LOGTAG, "getHeading: " + mAccelListener.getHeading());
        return mAccelListener.getHeading();
    }
    
    @Override
    public void stopAllListeners() {
        mAccelListener.stopAllListeners();
    }
    
    private void onShake() {
        String script = "window.ormmaview.fireShakeEvent()";
        Log.d(LOGTAG, script);
        mOrmmaView.injectJavaScript(script);
    }

    private void onTilt(float x, float y, float z) {
        mLastX = x;
        mLastY = y;
        mLastZ = z;
        
        String script = "window.ormmaview.fireChangeEvent({ tilt: "+ getTilt() + "})";
        Log.d(LOGTAG, script);
        mOrmmaView.injectJavaScript(script);
    }
    
    private void onHeadingChange(float f) {
        String script = "window.ormmaview.fireChangeEvent({ heading: " + (int) (f * (180 / Math.PI)) + "});";
        Log.d(LOGTAG, script );
        mOrmmaView.injectJavaScript(script);
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////

    public class AccelListener implements SensorEventListener {
        
        private static final int FORCE_THRESHOLD = 1000;
        private static final int TIME_THRESHOLD = 100;
        private static final int SHAKE_TIMEOUT = 500;
        private static final int SHAKE_DURATION = 2000;
        private static final int SHAKE_COUNT = 2;

        String mKey;

        int registeredTiltListeners = 0;
        int registeredShakeListeners = 0;
        int registeredHeadingListeners = 0;

        private SensorManager mSensorManager;
        private int mSensorDelay = SensorManager.SENSOR_DELAY_NORMAL;
        private long mLastForce;
        private int mShakeCount;
        private long mLastTime;
        private long mLastShake;
        private float[] mMagVals;
        private float[] mAccVals = { 0, 0, 0 };
        private boolean bMagReady;
        private boolean bAccReady;
        private float[] mLastAccVals = { 0, 0, 0 };
        private float[] mActualOrientation = { -1, -1, -1 };

        public AccelListener(Context context) {
            mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        }

        public void setSensorDelay(int delay) {
            mSensorDelay = delay;
            if ((registeredTiltListeners > 0) || (registeredShakeListeners > 0)) {
                stop();
                start();
            }
        }

        public void startTrackingTilt() {
            if (registeredTiltListeners == 0) start();
            registeredTiltListeners++;
        }

        public void stopTrackingTilt() {
            registeredTiltListeners--;
            if (registeredTiltListeners == 0) stop();
        }

        public void startTrackingShake() {
            if (registeredShakeListeners == 0) {
                setSensorDelay(SensorManager.SENSOR_DELAY_GAME);
                start();
            }
            registeredShakeListeners++;
        }

        public void stopTrackingShake() {
            registeredShakeListeners--;
            if (registeredShakeListeners == 0) {
                setSensorDelay(SensorManager.SENSOR_DELAY_NORMAL);
                stop();
            }
        }

        public void startTrackingHeading() {
            if (registeredHeadingListeners == 0) startMagneticFieldSensor();
            registeredHeadingListeners++;
        }

        private void startMagneticFieldSensor() {
            List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
            if (list.size() > 0) {
                mSensorManager.registerListener(this, list.get(0), mSensorDelay);
                start();
            } else {
                Log.e(LOGTAG, "Tried to use the magnetic field sensor, but it failed.");
            }
        }

        public void stopTrackingHeading() {
            registeredHeadingListeners--;
            if (registeredHeadingListeners == 0) stop();
        }

        private void start() {
            List<Sensor> list = mSensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
            if (list.size() > 0) {
                mSensorManager.registerListener(this, list.get(0), mSensorDelay);
            } else {
                Log.e(LOGTAG, "Tried to use the accelerometer sensor, but it failed.");
            }
        }

        public void stop() {
            if ((registeredHeadingListeners == 0) && 
                    (registeredShakeListeners == 0) && 
                    (registeredTiltListeners == 0)) {
                mSensorManager.unregisterListener(this);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    mMagVals = event.values.clone();
                    bMagReady = true;
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    mLastAccVals = mAccVals;
                    mAccVals = event.values.clone();
                    bAccReady = true;
                    break;
            }
            
            if (mMagVals != null && mAccVals != null && bAccReady && bMagReady) {
                bAccReady = false;
                bMagReady = false;
                float[] R = new float[9];
                float[] I = new float[9];
                SensorManager.getRotationMatrix(R, I, mAccVals, mMagVals);

                mActualOrientation = new float[3];

                SensorManager.getOrientation(R, mActualOrientation);
                OrmmaSensorController.this.onHeadingChange(mActualOrientation[0]);
            }
            
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                long now = System.currentTimeMillis();

                if ((now - mLastForce) > SHAKE_TIMEOUT) mShakeCount = 0;

                if ((now - mLastTime) > TIME_THRESHOLD) {
                    long diff = now - mLastTime;
                    float speed = Math.abs(mAccVals[SensorManager.DATA_X] +
                            mAccVals[SensorManager.DATA_Y] +
                            mAccVals[SensorManager.DATA_Z] - 
                            mLastAccVals[SensorManager.DATA_X] -
                            mLastAccVals[SensorManager.DATA_Y] - 
                            mLastAccVals[SensorManager.DATA_Z])
                            / diff * 10000;
                            
                    if (speed > FORCE_THRESHOLD) {
                        if ((++mShakeCount >= SHAKE_COUNT) && (now - mLastShake > SHAKE_DURATION)) {
                            mLastShake = now;
                            mShakeCount = 0;
                            OrmmaSensorController.this.onShake();
                        }
                        mLastForce = now;
                    }
                    
                    mLastTime = now;
                    OrmmaSensorController.this.onTilt(
                            mAccVals[SensorManager.DATA_X],
                            mAccVals[SensorManager.DATA_Y],
                            mAccVals[SensorManager.DATA_Z]);
                }
            }
        }

        public float getHeading() {
            return mActualOrientation[0];
        }
        
        public void stopAllListeners() {
            registeredTiltListeners = 0;
            registeredShakeListeners = 0;
            registeredHeadingListeners = 0;
            stop();
        }
    }
}
