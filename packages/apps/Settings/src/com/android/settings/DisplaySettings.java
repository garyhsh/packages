/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings;

import static android.provider.Settings.System.SCREEN_OFF_TIMEOUT;

import android.app.ActivityManagerNative;
import android.app.admin.DevicePolicyManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;

import com.android.internal.view.RotationPolicy;
import android.view.IWindowManager;
import android.view.WindowManager;
import com.android.settings.DreamSettings;

import java.util.ArrayList;

public class DisplaySettings extends SettingsPreferenceFragment implements
        Preference.OnPreferenceChangeListener {
    private static final String TAG = "DisplaySettings";

    /** If there is no setting in the provider, use this. */
    private static final int FALLBACK_SCREEN_TIMEOUT_VALUE = 30000;

    private static final String KEY_SCREEN_TIMEOUT = "screen_timeout";
    private static final String KEY_ACCELEROMETER = "accelerometer";
    private static final String KEY_FONT_SIZE = "font_size";
    private static final String KEY_NOTIFICATION_PULSE = "notification_pulse";
    private static final String KEY_SCREEN_SAVER = "screensaver";
    private static final String KEY_ACCELEROMETER_COORDINATE = "accelerometer_coornadite";
    private static final String KEY_SCREEN_ADAPTION = "screen_adaption";
	private static final String KEY_BRIGHT_SYSTEM = "bright_system";
	private static final String KEY_BRIGHT_SYSTEM_DEMO = "bright_demo_mode";
	private static final String KEY_BTIGHTNESS_LIGHT = "brightness_light";
	private static final String KEY_BTIGHTNESS_LIGHT_DEMO = "backlight_demo_mode";
	private static final String KEY_BRIGHTNESS_LIGHT_CLOSE_HDMI_PLUGGED = "close_backlight_hdmi_plugged";
    private CheckBoxPreference mAccelerometer;
    private ListPreference mFontSizePref;
    private CheckBoxPreference mNotificationPulse;
    private ListPreference mAccelerometerCoordinate;
	private CheckBoxPreference mBrightSystem,mBrightSystemDemo;
	private CheckBoxPreference mBrightnessLight,mBrightnessLightDemo,mBrightCloseHdmiPlugged;

    private final Configuration mCurConfig = new Configuration();
    
    private ListPreference mScreenTimeoutPreference;
    private Preference mScreenSaverPreference;
    private Preference mScreenAdaption;
    private final RotationPolicy.RotationPolicyListener mRotationPolicyListener =
            new RotationPolicy.RotationPolicyListener() {
        @Override
        public void onChange() {
            updateAccelerometerRotationCheckbox();
        }
    };
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ContentResolver resolver = getActivity().getContentResolver();

        addPreferencesFromResource(R.xml.display_settings);

        mAccelerometer = (CheckBoxPreference) findPreference(KEY_ACCELEROMETER);
        mAccelerometer.setPersistent(false);
        if (RotationPolicy.isRotationLockToggleSupported(getActivity())) {
            // If rotation lock is supported, then we do not provide this option in
            // Display settings.  However, is still available in Accessibility settings.
            getPreferenceScreen().removePreference(mAccelerometer);
        }

        mScreenSaverPreference = findPreference(KEY_SCREEN_SAVER);
        if (mScreenSaverPreference != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_enableDreams) == false) {
            getPreferenceScreen().removePreference(mScreenSaverPreference);
        }
        
        mScreenTimeoutPreference = (ListPreference) findPreference(KEY_SCREEN_TIMEOUT);
        final long currentTimeout = Settings.System.getLong(resolver, SCREEN_OFF_TIMEOUT,
                FALLBACK_SCREEN_TIMEOUT_VALUE);
        mScreenTimeoutPreference.setValue(String.valueOf(currentTimeout));
        mScreenTimeoutPreference.setOnPreferenceChangeListener(this);
        disableUnusableTimeouts(mScreenTimeoutPreference);
        updateTimeoutPreferenceDescription(currentTimeout);

        mFontSizePref = (ListPreference) findPreference(KEY_FONT_SIZE);
        mFontSizePref.setOnPreferenceChangeListener(this);
        mNotificationPulse = (CheckBoxPreference) findPreference(KEY_NOTIFICATION_PULSE);
        if (mNotificationPulse != null
                && getResources().getBoolean(
                        com.android.internal.R.bool.config_intrusiveNotificationLed) == false) {
            getPreferenceScreen().removePreference(mNotificationPulse);
        } else {
            try {
                mNotificationPulse.setChecked(Settings.System.getInt(resolver,
                        Settings.System.NOTIFICATION_LIGHT_PULSE) == 1);
                mNotificationPulse.setOnPreferenceChangeListener(this);
            } catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.NOTIFICATION_LIGHT_PULSE + " not found");
            }
        }
        mScreenAdaption = (Preference)findPreference(KEY_SCREEN_ADAPTION);
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        android.view.Display display = wm.getDefaultDisplay();
        int width     = display.getWidth();
        int height    = display.getHeight();
        Log.d(TAG,"rate1 = " + (width * 3.0f / (height * 5.0f)) + 
                 " rate2 = " + (width * 5.0f / (height * 3.0f)));
        if(((width * 3.0f / (height * 5.0f) == 1.0f) ||
           (width * 5.0f / (height * 3.0f) == 1.0f)) && mScreenAdaption!=null){
            getPreferenceScreen().removePreference(mScreenAdaption) ;   
        }
        mAccelerometerCoordinate = (ListPreference) findPreference(KEY_ACCELEROMETER_COORDINATE);
        if(mAccelerometerCoordinate != null){
            mAccelerometerCoordinate.setOnPreferenceChangeListener(this);
            String value = Settings.System.getString(getContentResolver(),
                    Settings.System.ACCELEROMETER_COORDINATE);
            mAccelerometerCoordinate.setValue(value);
            updateAccelerometerCoordinateSummary(value);
        }
		mBrightSystem = (CheckBoxPreference)findPreference(KEY_BRIGHT_SYSTEM);
		mBrightSystemDemo = (CheckBoxPreference)findPreference(KEY_BRIGHT_SYSTEM_DEMO);
		boolean demoEnabled;
		if(mBrightSystem != null){
			try{
			   demoEnabled =(Settings.System.getInt(resolver,
                        Settings.System.BRIGHT_SYSTEM_MODE)&0x01) > 0;
			   mBrightSystem.setChecked(demoEnabled);
               mBrightSystem.setOnPreferenceChangeListener(this);
			   if(mBrightSystemDemo != null&&demoEnabled){
			      try{
			         mBrightSystemDemo.setChecked((Settings.System.getInt(resolver,
                        Settings.System.BRIGHT_SYSTEM_MODE)&0x02)> 0);
                     mBrightSystemDemo.setOnPreferenceChangeListener(this);
			      }catch (SettingNotFoundException snfe) {
                     Log.e(TAG, Settings.System.BRIGHT_SYSTEM_MODE + " not found");
                  }
		       }else if(mBrightSystemDemo == null){
		          getPreferenceScreen().removePreference(mBrightSystemDemo);
		       }else {
                  mBrightSystemDemo.setEnabled(demoEnabled);
			   }
			}catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.BRIGHT_SYSTEM_MODE + " not found");
            }
		}else{
		    getPreferenceScreen().removePreference(mBrightSystem);
		}

		mBrightnessLight = (CheckBoxPreference)findPreference(KEY_BTIGHTNESS_LIGHT);
		mBrightnessLightDemo = (CheckBoxPreference)findPreference(KEY_BTIGHTNESS_LIGHT_DEMO);
		mBrightCloseHdmiPlugged = (CheckBoxPreference)findPreference(KEY_BRIGHTNESS_LIGHT_CLOSE_HDMI_PLUGGED);
		if(mBrightnessLight != null){
			try{
				demoEnabled = (Settings.System.getInt(resolver,
                        Settings.System.BRIGHTNESS_LIGHT_MODE)&0x01)> 0;
				mBrightnessLight.setChecked(demoEnabled);
                mBrightnessLight.setOnPreferenceChangeListener(this);

				if(mBrightnessLightDemo != null&& demoEnabled){
			       try{
				      mBrightnessLightDemo.setChecked((Settings.System.getInt(resolver,
                         Settings.System.BRIGHTNESS_LIGHT_MODE)&0x02) > 0);
                      mBrightnessLightDemo.setOnPreferenceChangeListener(this);
			       }catch (SettingNotFoundException snfe) {
                      Log.e(TAG, Settings.System.BRIGHTNESS_LIGHT_MODE + " not found");
                   }
		        }else if(mBrightnessLightDemo == null){
		           getPreferenceScreen().removePreference(mBrightnessLightDemo);
		        }else{
                   mBrightnessLightDemo.setEnabled(demoEnabled);
				}
			}catch (SettingNotFoundException snfe) {
                Log.e(TAG, Settings.System.BRIGHTNESS_LIGHT_MODE + " not found");
            }
		}else{
		    getPreferenceScreen().removePreference(mBrightnessLight);
		}
		if(mBrightCloseHdmiPlugged != null){
			try{
				boolean brightCloseHdmiPlugged = (Settings.System.getInt(resolver,
                    Settings.System.BRIGHTNESS_LIGHT_CLOSE_HDMI_PLUGGED)&0x01)> 0;
                mBrightCloseHdmiPlugged.setChecked(brightCloseHdmiPlugged);
                mBrightCloseHdmiPlugged.setOnPreferenceChangeListener(this);
                Log.d(TAG,"mBrightCloseHdmiPlugged = " + mBrightCloseHdmiPlugged);
			}catch(SettingNotFoundException snfe){
				Log.e(TAG,"Settings.System.BRIGHTNESS_LIGHT_CLOSE_HDMI_PLUGGED" + "not found");
			}
		}else{
			getPreferenceScreen().removePreference(mBrightCloseHdmiPlugged);
		}

    }

    private void updateTimeoutPreferenceDescription(long currentTimeout) {
        ListPreference preference = mScreenTimeoutPreference;
        String summary;
        if (currentTimeout < 0) {
            // Unsupported value
            summary = "";
        } else {
            final CharSequence[] entries = preference.getEntries();
            final CharSequence[] values = preference.getEntryValues();
            int best = 0;
            for (int i = 0; i < values.length; i++) {
                long timeout = Long.parseLong(values[i].toString());
                if (currentTimeout >= timeout) {
                    best = i;
                }
            }
            summary = preference.getContext().getString(R.string.screen_timeout_summary,
                    entries[best]);
        }
        preference.setSummary(summary);
    }

    private void disableUnusableTimeouts(ListPreference screenTimeoutPreference) {
        final DevicePolicyManager dpm =
                (DevicePolicyManager) getActivity().getSystemService(
                Context.DEVICE_POLICY_SERVICE);
        final long maxTimeout = dpm != null ? dpm.getMaximumTimeToLock(null) : 0;
        if (maxTimeout == 0) {
            return; // policy not enforced
        }
        final CharSequence[] entries = screenTimeoutPreference.getEntries();
        final CharSequence[] values = screenTimeoutPreference.getEntryValues();
        ArrayList<CharSequence> revisedEntries = new ArrayList<CharSequence>();
        ArrayList<CharSequence> revisedValues = new ArrayList<CharSequence>();
        for (int i = 0; i < values.length; i++) {
            long timeout = Long.parseLong(values[i].toString());
            if (timeout <= maxTimeout) {
                revisedEntries.add(entries[i]);
                revisedValues.add(values[i]);
            }
        }
        if (revisedEntries.size() != entries.length || revisedValues.size() != values.length) {
            screenTimeoutPreference.setEntries(
                    revisedEntries.toArray(new CharSequence[revisedEntries.size()]));
            screenTimeoutPreference.setEntryValues(
                    revisedValues.toArray(new CharSequence[revisedValues.size()]));
            final int userPreference = Integer.parseInt(screenTimeoutPreference.getValue());
            if (userPreference <= maxTimeout) {
                screenTimeoutPreference.setValue(String.valueOf(userPreference));
            } else {
                // There will be no highlighted selection since nothing in the list matches
                // maxTimeout. The user can still select anything less than maxTimeout.
                // TODO: maybe append maxTimeout to the list and mark selected.
            }
        }
        screenTimeoutPreference.setEnabled(revisedEntries.size() > 0);
    }

    int floatToIndex(float val) {
        String[] indices = getResources().getStringArray(R.array.entryvalues_font_size);
        float lastVal = Float.parseFloat(indices[0]);
        for (int i=1; i<indices.length; i++) {
            float thisVal = Float.parseFloat(indices[i]);
            if (val < (lastVal + (thisVal-lastVal)*.5f)) {
                return i-1;
            }
            lastVal = thisVal;
        }
        return indices.length-1;
    }
    
    public void readFontSizePreference(ListPreference pref) {
        try {
            mCurConfig.updateFrom(ActivityManagerNative.getDefault().getConfiguration());
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to retrieve font size");
        }

        // mark the appropriate item in the preferences list
        int index = floatToIndex(mCurConfig.fontScale);
        pref.setValueIndex(index);

        // report the current size in the summary text
        final Resources res = getResources();
        String[] fontSizeNames = res.getStringArray(R.array.entries_font_size);
        pref.setSummary(String.format(res.getString(R.string.summary_font_size),
                fontSizeNames[index]));
    }
    
    @Override
    public void onResume() {
        super.onResume();

        updateState();

        RotationPolicy.registerRotationPolicyListener(getActivity(),
                mRotationPolicyListener);
    }

    @Override
    public void onPause() {
        super.onPause();

        RotationPolicy.unregisterRotationPolicyListener(getActivity(),
                mRotationPolicyListener);
    }

    private void updateState() {
        updateAccelerometerRotationCheckbox();
        readFontSizePreference(mFontSizePref);
        updateScreenSaverSummary();
        
		if(mAccelerometerCoordinate != null){
            updateAccelerometerCoordinateSummary(mAccelerometerCoordinate.getValue());
        }
	}
	
    private void updateScreenSaverSummary() {
        mScreenSaverPreference.setSummary(
            DreamSettings.isScreenSaverEnabled(mScreenSaverPreference.getContext())
                ? R.string.screensaver_settings_summary_on
                : R.string.screensaver_settings_summary_off);
    }

    private void updateAccelerometerRotationCheckbox() {
        if (getActivity() == null) return;

        mAccelerometer.setChecked(!RotationPolicy.isRotationLocked(getActivity()));
    }

    private void updateAccelerometerCoordinateSummary(Object value){       
        CharSequence[] summaries = getResources().getTextArray(R.array.accelerometer_summaries);
        CharSequence[] values = mAccelerometerCoordinate.getEntryValues();
        for (int i=0; i<values.length; i++) {
            if (values[i].equals(value)) {
                mAccelerometerCoordinate.setSummary(summaries[i]);
                break;
            }
        }
    }

    public void writeFontSizePreference(Object objValue) {
        try {
            mCurConfig.fontScale = Float.parseFloat(objValue.toString());
            ActivityManagerNative.getDefault().updatePersistentConfiguration(mCurConfig);
        } catch (RemoteException e) {
            Log.w(TAG, "Unable to save font size");
        }
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        boolean value;
		int value2;
		try{
            if (preference == mAccelerometer) {
                RotationPolicy.setRotationLockForAccessibility(
                    getActivity(), !mAccelerometer.isChecked());
            } else if (preference == mNotificationPulse) {
                value = mNotificationPulse.isChecked();
                Settings.System.putInt(getContentResolver(), Settings.System.NOTIFICATION_LIGHT_PULSE,
                    value ? 1 : 0);
                return true;
            } else if (preference == mBrightSystem){
                value = mBrightSystem.isChecked();
			    value2 = Settings.System.getInt(getContentResolver(),
				     Settings.System.BRIGHT_SYSTEM_MODE);
			    Settings.System.putInt(getContentResolver(),Settings.System.BRIGHT_SYSTEM_MODE,
				     value ? value2|0x01 : value2&0x02);
				mBrightSystemDemo.setEnabled(value);
		    } else if (preference == mBrightSystemDemo){
		        value = mBrightSystemDemo.isChecked();
			    value2 = Settings.System.getInt(getContentResolver(),
				     Settings.System.BRIGHT_SYSTEM_MODE);
			    Settings.System.putInt(getContentResolver(),Settings.System.BRIGHT_SYSTEM_MODE,
				     value ? value2|0x02 : value2&0x01);
		    } else if (preference == mBrightnessLight){
                value = mBrightnessLight.isChecked();
			    value2 = Settings.System.getInt(getContentResolver(),
				     Settings.System.BRIGHTNESS_LIGHT_MODE);
			    Settings.System.putInt(getContentResolver(),Settings.System.BRIGHTNESS_LIGHT_MODE,
				     value ? value2|0x01 : value2&0x02); 
				mBrightnessLightDemo.setEnabled(value);
		    } else if (preference == mBrightnessLightDemo){
                value = mBrightnessLightDemo.isChecked();
			    value2 = Settings.System.getInt(getContentResolver(),
				     Settings.System.BRIGHTNESS_LIGHT_MODE);
			    Settings.System.putInt(getContentResolver(),Settings.System.BRIGHTNESS_LIGHT_MODE,
				     value ? value2|0x02 : value2&0x01);
		    }else if(preference == mBrightCloseHdmiPlugged){
		    	value = mBrightCloseHdmiPlugged.isChecked();
		    	Settings.System.putInt(getContentResolver(),Settings.System.BRIGHTNESS_LIGHT_CLOSE_HDMI_PLUGGED,
					     value ? 1 : 0);
		    }
		}catch (SettingNotFoundException e){
		    Log.e(TAG, Settings.System.BRIGHTNESS_LIGHT_MODE+ " or "+
		         Settings.System.BRIGHT_SYSTEM_MODE + " not found");

		}
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    public boolean onPreferenceChange(Preference preference, Object objValue) {
        final String key = preference.getKey();
        if (KEY_SCREEN_TIMEOUT.equals(key)) {
            int value = Integer.parseInt((String) objValue);
            try {
                Settings.System.putInt(getContentResolver(), SCREEN_OFF_TIMEOUT, value);
                updateTimeoutPreferenceDescription(value);
            } catch (NumberFormatException e) {
                Log.e(TAG, "could not persist screen timeout setting", e);
            }
        }
        if (KEY_FONT_SIZE.equals(key)) {
            writeFontSizePreference(objValue);
        }if (KEY_ACCELEROMETER_COORDINATE.equals(key))
        {
            String value = String.valueOf(objValue);
            try {
                Settings.System.putString(getContentResolver(), 
                        Settings.System.ACCELEROMETER_COORDINATE, value);
                updateAccelerometerCoordinateSummary(objValue);
            }catch (NumberFormatException e) {
                Log.e(TAG, "could not persist key accelerometer coordinate setting", e);
            }
        }

        return true;
    }
}
