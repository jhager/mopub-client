/*  Copyright (c) 2011 The ORMMA.org project authors. All Rights Reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */
 
package com.mopub.ormma.controller;

import java.lang.reflect.Field;

import org.json.JSONException;
import org.json.JSONObject;

import com.mopub.ormma.OrmmaView;

import android.content.Context;
import android.graphics.Color;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

public abstract class OrmmaController {
    
    private static final String LOGTAG = "OrmmaController";
    
    private static final String STRING_TYPE = "class java.lang.String";
    private static final String INT_TYPE = "int";
    private static final String BOOLEAN_TYPE = "boolean";
    private static final String FLOAT_TYPE = "float";
    private static final String NAVIGATION_TYPE = "class com.ormma.NavigationStringEnum";
    private static final String TRANSITION_TYPE = "class com.ormma.TransitionStringEnum";
    
    public static final String FULL_SCREEN = "fullscreen";
    public static final String EXIT = "exit";
    public static final String STYLE_NORMAL = "normal";
    
    protected OrmmaView mOrmmaView;
    protected Context mContext;
    
    public OrmmaController(OrmmaView ormmaView, Context context) {
        mOrmmaView = ormmaView;
        mContext = context;
    }
    
    public abstract void stopAllListeners();
    
    protected static Object getFromJSON(JSONObject json, Class<?> c) 
        throws IllegalArgumentException {
        if (json == null) throw new IllegalArgumentException("JSONObject cannot be null.");
        if (c == null) throw new IllegalArgumentException("Class to get from JSON cannot be null.");
        
        Object obj;
        try {
            obj = c.newInstance();
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Failed to access constructor of class " + 
                    c.toString());
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Failed to construct object of class " + 
                    c.toString());
        }
        Field[] fields = c.getDeclaredFields();
        
        for (int i = 0; i < fields.length; i++) {
            Field f = fields[i];
            String fieldName = f.getName();
            String jsonName = fieldName.replace('_', '-');
            
            Class<?> type = f.getType();
            String typeStr = type.toString();
            
            try {
                if (typeStr.equals(INT_TYPE)) {
                    String value = json.getString(jsonName);
                    f.set(obj, jsonStringToInt(value.toLowerCase()));
                } else if (typeStr.equals(STRING_TYPE)) {
                    String value = json.getString(jsonName);
                    f.set(obj, value);
                } else if (typeStr.equals(BOOLEAN_TYPE)) {
                    boolean value = json.getBoolean(jsonName);
                    f.set(obj, value);
                } else if (typeStr.equals(FLOAT_TYPE)) {
                    String value = json.getString(jsonName);
                    f.set(obj, Float.parseFloat(value));
                } else if (typeStr.equals(NAVIGATION_TYPE)) {
                    String value = json.getString(jsonName);
                    f.set(obj, NavigationStringEnum.fromString(value));
                } else if (typeStr.equals(TRANSITION_TYPE)) {
                    String value = json.getString(jsonName);
                    f.set(obj, TransitionStringEnum.fromString(value));
                }
            } catch (IllegalAccessException e) {
                throw new IllegalArgumentException("Error setting fields for object of class " + 
                        c.toString());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Error setting fields for object of class " + 
                        c.toString());
            } catch (JSONException e) {
                Log.e(LOGTAG, "Tried to get the JSON value mapped by " + jsonName + ", but it " +
                        "didn't exist.");
            }
        }
        
        return obj;
    }
    
    /* Converts JSON value strings to ints. Often used to parse hex-encoded color values. */
    private static int jsonStringToInt(String str) {
        if (!str.startsWith("#")) return Integer.parseInt(str);
        else {
            // This will be a hex string, either prefixed with "0x" or not.
            try {
                if (str.startsWith("#0x")) return Integer.decode(str.substring(1)).intValue();
                else return Integer.parseInt(str.substring(1), 16);
            } catch (NumberFormatException e) {
                return Color.WHITE;
            }
        }
    }

    public static class ReflectedParcelable implements Parcelable {

        public static final Parcelable.Creator<ReflectedParcelable> CREATOR = 
            new Parcelable.Creator<ReflectedParcelable>() {
                public ReflectedParcelable createFromParcel(Parcel in) {
                    return new ReflectedParcelable(in);
                }

                public ReflectedParcelable[] newArray(int size) {
                    return new ReflectedParcelable[size];
                }
        };
        
        public ReflectedParcelable() {
            
        }
        
        protected ReflectedParcelable(Parcel in) {
            Class<?> c = this.getClass();
            Field[] fields = c.getDeclaredFields();
            
            /* Iterate over each field of this object and examine its type. Once the type is known,
             * we read that type from the Parcel and set the field. */
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                Class<?> type = f.getType();
                
                try {
                    // TODO: We could handle enums generically.
                    if (type.equals(NavigationStringEnum.class)) {
                        f.set(this, NavigationStringEnum.fromString(in.readString()));
                    } else if (type.equals(TransitionStringEnum.class)) {
                        f.set(this, TransitionStringEnum.fromString(in.readString()));
                    } else {
                        Object dt = f.get(this);
                        
                        // The static Parcelable.Creator is the only field we shouldn't set.
                        if (!(dt instanceof Parcelable.Creator<?>)) f.set(this, in.readValue(null));
                    }
                } catch (IllegalAccessException e) {
                    Log.e(LOGTAG, "Error: Could not create object from parcel. " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    Log.e(LOGTAG, "Error: Could not create object from parcel. " + e.getMessage());
                }
            }
        }
        
        @Override
        public int describeContents() {
            return 0;
        }

        /* Iterate over each field of this object and examine its type. Once the type is known,
         * we'll know how to write the field out to the Parcel. */
        @Override
        public void writeToParcel(Parcel out, int flags) {
            Class<?> c = this.getClass();
            Field[] fields = c.getDeclaredFields();
            
            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                Class<?> type = f.getType();
                
                try {
                    // TODO: We could handle enums generically.
                    if (type.equals(NavigationStringEnum.class)) {
                        NavigationStringEnum nse = (NavigationStringEnum) f.get(this);
                        out.writeString(nse.getText());
                    } else if (type.equals(TransitionStringEnum.class)) {
                        TransitionStringEnum tse = (TransitionStringEnum) f.get(this);
                        out.writeString(tse.getText());
                    } else {
                        Object dt = f.get(this);
                        
                        // The static Parcelable.Creator is the only field we shouldn't write out.
                        if (!(dt instanceof Parcelable.Creator<?>)) out.writeValue(dt);
                    }
                } catch (IllegalAccessException e) {
                    Log.e(LOGTAG, "Error: Could not write to parcel. " + e.getMessage());
                } catch (IllegalArgumentException e) {
                    Log.e(LOGTAG, "Error: Could not write to parcel. " + e.getMessage());
                }
            }
        }
    }
    
    public static class Dimensions extends ReflectedParcelable {
        
        public int x, y, width, height;
        
        public static final Parcelable.Creator<Dimensions> CREATOR = new Parcelable.Creator<Dimensions>() {
            public Dimensions createFromParcel(Parcel in) {
                return new Dimensions(in);
            }

            public Dimensions[] newArray(int size) {
                return new Dimensions[size];
            }
        };
        
        protected Dimensions() {
            x = -1;
            y = -1;
            width = -1;
            height = -1;
        }

        public Dimensions(Parcel in) {
            super(in);
        }
    }
    
    public static class PlayerProperties extends ReflectedParcelable {
        
        public boolean autoPlay, showControl, doLoop, audioMuted, inline;
        public String stopStyle, startStyle;
        
        public static final Parcelable.Creator<PlayerProperties> CREATOR = new Parcelable.Creator<PlayerProperties>() {
            public PlayerProperties createFromParcel(Parcel in) {
                return new PlayerProperties(in);
            }

            public PlayerProperties[] newArray(int size) {
                return new PlayerProperties[size];
            }
        };
        
        public PlayerProperties() {
            autoPlay = true;
            showControl = true;
            doLoop = false;
            audioMuted = false;
            startStyle = STYLE_NORMAL;
            stopStyle = STYLE_NORMAL;
            inline = false;
        }
        
        public PlayerProperties(Parcel in) {
            super(in);
        }

        public void setProperties(boolean audioMuted, boolean autoPlay, boolean controls,
                boolean inline, boolean loop, String startStyle, String stopStyle) {
            this.autoPlay = autoPlay;
            this.showControl = controls;
            this.doLoop = loop;
            this.audioMuted = audioMuted;
            this.startStyle = startStyle;
            this.stopStyle = stopStyle;
            this.inline = inline;
        }
        
        public void muteAudio() {
            audioMuted = true;
        }
        
        public boolean isAutoPlay() {
            return (autoPlay == true);
        }
        
        public boolean showControl() {
            return showControl;
        }
        
        public boolean doLoop() {
            return doLoop;
        }
        
        public boolean doMute() {
            return audioMuted;
        }
        
        public boolean exitOnComplete() {
            return stopStyle.equalsIgnoreCase(EXIT);
        }
        
        public boolean isFullScreen() {
            return startStyle.equalsIgnoreCase(FULL_SCREEN);
        }       
    }
    
    public static class Properties extends ReflectedParcelable {
        
        public boolean useBackground;
        public int backgroundColor;
        public float backgroundOpacity;
        
        public static final Parcelable.Creator<Properties> CREATOR = new Parcelable.Creator<Properties>() {
            public Properties createFromParcel(Parcel in) {
                return new Properties(in);
            }

            public Properties[] newArray(int size) {
                return new Properties[size];
            }
        };
        
        protected Properties() {
            useBackground = false;
            backgroundColor = 0;
            backgroundOpacity = 0;
        }

        public Properties(Parcel in) {
            super(in);
        }
    }
    
    public enum NavigationStringEnum {
        NONE("none"), 
        CLOSE("close"), 
        BACK("back"), 
        FORWARD("forward"), 
        REFRESH("refresh");

        private String text;
        
        NavigationStringEnum(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }
        
        public static NavigationStringEnum fromString(String text) {
            if (text != null) {
                for (NavigationStringEnum b : NavigationStringEnum.values()) {
                    if (text.equalsIgnoreCase(b.text)) return b;
                }
            }
            return null;
        }
    }
    
    public enum TransitionStringEnum {
        DEFAULT("default"), 
        DISSOLVE("dissolve"), 
        FADE("fade"), 
        ROLL("roll"), 
        SLIDE("slide"), 
        ZOOM("zoom"), 
        NONE("none");

        private String text;

        TransitionStringEnum(String text) {
            this.text = text;
        }

        public String getText() {
            return this.text;
        }
        
        public static TransitionStringEnum fromString(String text) {
            if (text != null) {
                for (TransitionStringEnum b : TransitionStringEnum.values()) {
                    if (text.equalsIgnoreCase(b.text)) return b;
                }
            }
            return null;
        }
    }
}
