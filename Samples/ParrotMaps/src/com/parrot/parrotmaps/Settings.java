/*
 * Copyright (c) 2011, Parrot
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 *   * Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   * Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   * Neither the name of "Parrot SA" nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, 
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR 
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR 
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
 * PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.parrot.parrotmaps;

import android.content.Context;
import android.content.SharedPreferences;

import com.parrot.parrotmaps.DisplayAbstract.MAP_MODE;
import com.parrot.parrotmaps.DisplayAbstract.RESULT_MODE;

public class Settings {

    //! Name of file for ParrotMaps settings file
    private static final String SETTINGS_FILE_NAME = "ParrotMapsSettings";

    //! Wikipedia layer setting id
    private static final String SETTINGS_WIKIPEDIA_LAYER_ID = "WikipediaLayer";

    //! Photos layer setting id
    private static final String SETTINGS_PHOTOS_LAYER_ID = "PhotosLayer";

    //! Traffic layer setting id
    private static final String SETTINGS_TRAFFIC_LAYER_ID = "TrafficLayer";

    //! Results display mode setting id
    private static final String SETTINGS_RESULTS_DISPLAY_MODE_ID = "ResultsDisplayMode";

    //! Map mode setting id
    private static final String SETTINGS_MAP_MODE_ID = "MapMode";

    //! TTS activation id
    private static final String TTS_ACTIVATION_ID = "TTSActivation";
    
    //! Default value of Wikipedia layer setting
    final static boolean DEFAULT_WIKIPEDIA_LAYER_SETTING = false;
    
    //! Default value of Photos layer setting
    final static boolean DEFAULT_PHOTOS_LAYER_SETTING = false;
    
    //! Default value of Traffic layer setting
    final static boolean DEFAULT_TRAFFIC_LAYER_SETTING = true;
    
    //! Default value of 'results list and map' setting
    final static String DEFAULT_RESULTS_DISPLAY_MODE = RESULT_MODE.MAP.name();
    
    //! Default value of map mode setting
    final static String DEFAULT_MAP_MODE_SETTING = MAP_MODE.MAP.name();
    
    /**
     * Get Wikipedia layer setting.
     * @param ctx Application environment
     * @return true if layer is activated, false otherwise.
     */ 
    public static boolean getWikipediaLayerSetting(Context ctx)
    {
        return getBooleanSetting( ctx, SETTINGS_WIKIPEDIA_LAYER_ID, DEFAULT_WIKIPEDIA_LAYER_SETTING);
    }
    
    /**
     * Save Wikipedia layer setting.
     * @param ctx Application environment
     * @param activated true if layer is activated, false otherwise.
     */
    public static void setWikipediaLayerSetting(Context ctx, boolean activated)
    {
    	setBooleanSetting( ctx, SETTINGS_WIKIPEDIA_LAYER_ID, activated);
    }
    
    /**
     * Get Photos layer setting.
     * @param ctx Application environment
     * @return true if layer is activated, false otherwise.
     */ 
    public static boolean getPhotosLayerSetting(Context ctx)
    {
        return getBooleanSetting( ctx, SETTINGS_PHOTOS_LAYER_ID, DEFAULT_PHOTOS_LAYER_SETTING);
    }
    
    /**
     * Save Photos layer setting.
     * @param ctx Application environment
     * @param activated true if layer is activated, false otherwise.
     */
    public static void setPhotosLayerSetting(Context ctx, boolean activated)
    {
    	setBooleanSetting( ctx, SETTINGS_PHOTOS_LAYER_ID, activated);
    }
    
    /**
     * Get Traffic layer setting.
     * @param ctx Application environment
     * @return true if layer is activated, false otherwise.
     */ 
    public static boolean getTrafficLayerSetting(Context ctx)
    {
        return getBooleanSetting( ctx, SETTINGS_TRAFFIC_LAYER_ID, DEFAULT_TRAFFIC_LAYER_SETTING);
    }
    
    /**
     * Save Traffic layer setting.
     * @param ctx Application environment
     * @param activated true if layer is activated, false otherwise.
     */
    public static void setTrafficLayerSetting(Context ctx, boolean activated)
    {
    	setBooleanSetting( ctx, SETTINGS_TRAFFIC_LAYER_ID, activated);
    }
    
    /**
     * Get results display mode setting.
     * @param ctx Application environment
     * @return true if option is activated, false otherwise.
     */ 
    public static RESULT_MODE getResultsDisplayModeSetting(Context ctx)
    {
    	RESULT_MODE mode = RESULT_MODE.MAP; 
    	String modeName = getStringSetting( ctx, SETTINGS_RESULTS_DISPLAY_MODE_ID, DEFAULT_RESULTS_DISPLAY_MODE);
    	mode = Enum.valueOf(RESULT_MODE.class, modeName);
    	return mode;
    }
    
    /**
     * Save result display mod setting.
     * @param ctx Application environment
     * @param mode The mode.
     */
    public static void setResultsDisplayModeSetting(Context ctx, RESULT_MODE mode)
    {
    	setStringSetting( ctx, SETTINGS_RESULTS_DISPLAY_MODE_ID, mode.name());
    }
    
    /**
     * Get map mode setting.
     * @param ctx Application environment
     * @return true if option is activated, false otherwise.
     */ 
    public static MAP_MODE getMapModeSetting(Context ctx)
    {
    	MAP_MODE mode = MAP_MODE.MAP;
    	String modeName = getStringSetting( ctx, SETTINGS_MAP_MODE_ID, DEFAULT_MAP_MODE_SETTING);
    	mode = Enum.valueOf(MAP_MODE.class, modeName);
    	return mode;
    }
    
    /**
     * Save map mode setting.
     * @param ctx Application environment
     * @param mode The mode.
     */
    public static void setMapModeSetting(Context ctx, MAP_MODE mode)
    {
    	setStringSetting( ctx, SETTINGS_MAP_MODE_ID, mode.name());
    }
    
    /**
     * Save TTS activation setting.
     * @param ctx Application environment
     * @param activated true if TTS is activated, false otherwise.
     */
    public static void setTTSActivationSetting(Context ctx, boolean activated)
    {
    	setBooleanSetting( ctx, TTS_ACTIVATION_ID, activated);
    }
    
    /**
     * Get TTS activation setting.
     * @param ctx Application environment
     * @return true if TTS is activated, false otherwise.
     */ 
    public static boolean getTTSActivationSetting(Context ctx)
    {
        return getBooleanSetting( ctx, TTS_ACTIVATION_ID, ctx.getResources().getBoolean(R.bool.DEFAULT_TTS_ACTIVATION_SETTING));
    }

    /**
     * Get the value of a setting.
     * @param ctx Application environment
     * @param preference_id Id the preference to get.
     * @param default_value Default value returned by this function.
     * @return The value of the setting.
     */
    private static boolean getBooleanSetting( Context ctx,
    										  String preference_id,
    										  boolean default_value )
    {
        boolean value = default_value;
        if( null != ctx )
        {
            SharedPreferences preferences = ctx.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
            if( null != preferences )
            {
                value = preferences.getBoolean(preference_id, default_value);
            }
            else
            {
                // Nothing to do
            }
        }
        else
        {
            // Nothing to do
        }
        return value;
    }

    
    /**
     * Save a setting.
     * @param ctx Application environment
     * @param preference_id Id the preference to save.
     * @param value Saved value.
     */
    private static void setBooleanSetting( Context ctx,
    									   String preference_id,
    									   boolean value )
    {
        SharedPreferences preferences = ctx.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
        if( null != preferences )
        {
            SharedPreferences.Editor editor = preferences.edit();
            if( null != editor )
            {
                editor.putBoolean(preference_id, value);
                editor.commit();
            }
            else
            {
                // Nothing to do
            }
        }
        else
        {
            // Nothing to do
        }
    }

    /**
     * Get the value of a setting.
     * @param ctx Application environment
     * @param preference_id Id the preference to get.
     * @param default_value Default value returned by this function.
     * @return The value of the setting.
     */
    private static String getStringSetting( Context ctx,
                                            String preference_id,
                                            String default_value )
    {
        String value = default_value;
        if( null != ctx )
        {
            SharedPreferences preferences = ctx.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
            if( null != preferences )
            {
                value = preferences.getString(preference_id, default_value);
            }
            else
            {
                // Nothing to do
            }
        }
        else
        {
            // Nothing to do
        }
        return value;
    }

    
    /**
     * Save a setting.
     * @param ctx Application environment
     * @param preference_id Id the preference to save.
     * @param value Saved value.
     */
    private static void setStringSetting( Context ctx,
                                          String preference_id,
                                          String value )
    {
        SharedPreferences preferences = ctx.getSharedPreferences(SETTINGS_FILE_NAME, Context.MODE_PRIVATE);
        if( null != preferences )
        {
            SharedPreferences.Editor editor = preferences.edit();
            if( null != editor )
            {
                editor.putString(preference_id, value);
                editor.commit();
            }
            else
            {
                // Nothing to do
            }
        }
        else
        {
            // Nothing to do
        }
    }
}
