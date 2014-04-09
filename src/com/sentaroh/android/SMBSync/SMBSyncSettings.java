package com.sentaroh.android.SMBSync;

/*
The MIT License (MIT)
Copyright (c) 2011-2013 Sentaroh

Permission is hereby granted, free of charge, to any person obtaining a copy of 
this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights to use,
copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
and to permit persons to whom the Software is furnished to do so, subject to 
the following conditions:

The above copyright notice and this permission notice shall be included in all copies or 
substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE 
LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, 
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
OTHER DEALINGS IN THE SOFTWARE.

*/

import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_NOTIFICATION_MESSAGE_ALWAYS;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_NOTIFICATION_MESSAGE_ERROR;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_NOTIFICATION_MESSAGE_NO;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_ON;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_SPEC_AP;

import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.util.Log;

import com.sentaroh.android.Utilities.LocalMountPoint;

@SuppressLint("NewApi")
public class SMBSyncSettings extends PreferenceActivity{
	private static boolean DEBUG_ENABLE=false;
	private static final String APPLICATION_TAG="SMBSync";
	private static Context mContext=null;
	private static PreferenceActivity mPrefAct=null;
	private static PreferenceFragment mPrefFrag=null;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsActivity onCreate entered");
        if (Build.VERSION.SDK_INT>=11) return;
        
		addPreferencesFromResource(R.xml.settings);

		mPrefAct=this;
		mContext=getApplicationContext();
		
    	// get file state  
    	String status   = Environment.getExternalStorageState();  
    	boolean ext_strg=false;
    	if (!status.equals(Environment.MEDIA_MOUNTED)) ext_strg=false;
    	else ext_strg=true;

    	if (!ext_strg) {
    		findPreference(getString(R.string.settings_log_option).toString())
    			.setEnabled(false);
    		findPreference(getString(R.string.settings_log_dir).toString())
    			.setEnabled(false);
    	}
    	
    	if (Build.VERSION.SDK_INT<11) {
    		findPreference(getString(R.string.settings_media_scanner_non_media_files_scan).toString())
    			.setEnabled(false);
    	}

		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(this);

		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_network_wifi_option));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_auto_start));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_auto_term));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_backgroound_execution));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_error_option));
		
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_debug_msg_diplay));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_log_option));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_log_generation));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_log_dir));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_log_level));

		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_ui_alternate_ui));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_ui_keep_screen_on));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_suppress_warning_mixed_mp));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_background_termination_notification));

    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_media_scanner_non_media_files_scan));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_media_scanner_scan_extstg));
    	
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_file_diff_time_seconds));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_media_store_last_mod_time));

		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_default_user));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_default_pass));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_default_addr));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_smb_use_extended_security));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_smb_lm_compatibility));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_smb_perform_class));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_smb_log_level));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_smb_rcv_buf_size));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_smb_snd_buf_size));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_smb_listSize));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_smb_maxBuffers));
		initSettingValueBeforeHc(shared_pref,getString(R.string.settings_smb_tcp_nodelay));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_io_buffers));
    	
    	shared_pref.edit().putBoolean(getString(R.string.settings_exit_clean),true).commit();
		findPreference(getString(R.string.settings_exit_clean).toString()).setEnabled(false);
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_exit_clean));
    	initSettingValueBeforeHc(shared_pref,getString(R.string.settings_exported_profile_encryption));
    	
	};

	private static void initSettingValueBeforeHc(SharedPreferences shared_pref, String key_string) {
//		Log.v("","key="+key_string);
		initSettingValue(mPrefAct.findPreference(key_string),shared_pref,key_string);
	};

	private static void initSettingValueAfterHc(SharedPreferences shared_pref, String key_string) {
		initSettingValue(mPrefFrag.findPreference(key_string),shared_pref,key_string);
	};
	
	private static void initSettingValue(Preference pref_key, 
			SharedPreferences shared_pref, String key_string) {
		
		if (!checkSyncSettings(pref_key,shared_pref, key_string,mContext)) 
			if (!checkUiSettings(pref_key,shared_pref, key_string,mContext))
	    	if (!checkLogSettings(pref_key,shared_pref, key_string,mContext))
	    	if (!checkMediaScannerSettings(pref_key,shared_pref, key_string,mContext))
	    	if (!checkDifferentialSettings(pref_key,shared_pref, key_string,mContext))
		    if (!checkSmbSettings(pref_key,shared_pref, key_string,mContext))
		    if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))				    	
		    	checkOtherSettings(pref_key,shared_pref, key_string,mContext);
	};
		
 
    @Override
    public void onStart(){
        super.onStart();
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsActivity onStart entered");
    };
 
    @Override
    public void onResume(){
        super.onResume();
        if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsActivity onResume entered");
		setTitle(R.string.settings_main_title);
        if (Build.VERSION.SDK_INT<=10) {
    	    mPrefAct.getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerBeforeHc);  
        } else {
//    	    mPrefFrag.getPreferenceScreen().getSharedPreferences()
//    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);  
        }
    };
 
    @Override
    public void onBuildHeaders(List<Header> target) {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsActivity onBuildHeaders entered");
        loadHeadersFromResource(R.xml.settings_frag, target);
    };

//    @Override
//    public boolean isMultiPane () {
//    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsActivity isMultiPane entered");
//        return true;
//    };

    @Override
    public boolean onIsMultiPane () {
    	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsActivity onIsMultiPane entered");
        return true;
    };

	@Override  
	protected void onPause() {  
	    super.onPause();  
	    if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsActivity onPause entered");
        if (Build.VERSION.SDK_INT<=10) {
    	    mPrefAct.getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerBeforeHc);  
        } else {
//    	    mPrefFrag.getPreferenceScreen().getSharedPreferences()
//    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        }
	};

	@Override
	final public void onStop() {
		super.onStop();
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsActivity onStop entered");
	};

	@Override
	final public void onDestroy() {
		super.onDestroy();
		if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsActivity onDestroy entered");
	};

	private static SharedPreferences.OnSharedPreferenceChangeListener listenerBeforeHc =   
		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
		    public void onSharedPreferenceChanged(SharedPreferences shared_pref, 
		    		String key_string) {
		    	Preference pref_key=mPrefAct.findPreference(key_string);
				if (!checkSyncSettings(pref_key,shared_pref, key_string,mContext)) 
					if (!checkUiSettings(pref_key,shared_pref, key_string,mContext))
			    	if (!checkLogSettings(pref_key,shared_pref, key_string,mContext))
			    	if (!checkMediaScannerSettings(pref_key,shared_pref, key_string,mContext))
			    	if (!checkDifferentialSettings(pref_key,shared_pref, key_string,mContext))
				    if (!checkSmbSettings(pref_key,shared_pref, key_string,mContext))
				    if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))				    	
				    	checkOtherSettings(pref_key,shared_pref, key_string,mContext);
		    }
	};
	
	private static SharedPreferences.OnSharedPreferenceChangeListener listenerAfterHc =   
		    new SharedPreferences.OnSharedPreferenceChangeListener() {  
		    public void onSharedPreferenceChanged(SharedPreferences shared_pref, 
		    		String key_string) {
		    	Preference pref_key=mPrefFrag.findPreference(key_string);
				if (!checkSyncSettings(pref_key,shared_pref, key_string,mContext)) 
					if (!checkUiSettings(pref_key,shared_pref, key_string,mContext))
			    	if (!checkLogSettings(pref_key,shared_pref, key_string,mContext))
			    	if (!checkMediaScannerSettings(pref_key,shared_pref, key_string,mContext))
			    	if (!checkDifferentialSettings(pref_key,shared_pref, key_string,mContext))
				    if (!checkSmbSettings(pref_key,shared_pref, key_string,mContext))
				    if (!checkMiscSettings(pref_key,shared_pref, key_string,mContext))				    	
				    	checkOtherSettings(pref_key,shared_pref, key_string,mContext);
		    }
	};


	private static boolean checkSyncSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
    	if (key_string.equals(c.getString(R.string.settings_network_wifi_option))) {
    		isChecked=true;
    		String kv=shared_pref.getString(key_string,SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP);
			if (kv.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_ON)) {
				pref_key
					.setSummary(c.getString(R.string.settings_network_wifi_option_summary_adapter_on));
			} else if (kv.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF)){
				pref_key
					.setSummary(c.getString(R.string.settings_network_wifi_option_summary_adapter_off));
			} else if (kv.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP)){
				pref_key
					.setSummary(c.getString(R.string.settings_network_wifi_option_summary_connected_any_ap));
			} else if (kv.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_SPEC_AP)){
				pref_key
					.setSummary(c.getString(R.string.settings_network_wifi_option_summary_connected_spec_ap));
			}
    	} else if (key_string.equals(c.getString(R.string.settings_auto_start))) {
    		isChecked=true;
    		if (shared_pref.getBoolean(key_string, false)) {
    			pref_key
    				.setSummary(c.getString(R.string.settings_auto_start_summary_ena));
    		} else {
    			pref_key
					.setSummary(c.getString(R.string.settings_auto_start_summary_dis));
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_auto_term))) {
    		isChecked=true;
    		if (shared_pref.getBoolean(key_string, false)) {
    			pref_key
    				.setSummary(c.getString(R.string.settings_auto_term_summary_ena));
    		} else {
    			pref_key
    				.setSummary(c.getString(R.string.settings_auto_term_summary_dis));
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_backgroound_execution))) {
    		isChecked=true;
    		if (shared_pref.getBoolean(key_string, false)) {
    			pref_key
    				.setSummary(c.getString(R.string.settings_backgroound_execution_summary_ena));
    		} else {
    			pref_key
    				.setSummary(c.getString(R.string.settings_backgroound_execution_summary_dis));
    		}
		} else if (key_string.equals(c.getString(R.string.settings_error_option))) {
			isChecked=true;
			if (shared_pref.getBoolean(key_string, false)) {
				pref_key
					.setSummary(c.getString(R.string.settings_error_option_summary_ena));
			} else {
				pref_key
					.setSummary(c.getString(R.string.settings_error_option_summary_dis));
			}
		} else if (key_string.equals(c.getString(R.string.settings_ui_keep_screen_on))) {
			isChecked=true;
			if (shared_pref.getBoolean(key_string, false)) {
				pref_key
					.setSummary(c.getString(R.string.settings_ui_keep_screen_on_summary_ena));
			} else {
				pref_key
					.setSummary(c.getString(R.string.settings_ui_keep_screen_on_summary_dis));
			}
		} else if (key_string.equals(c.getString(R.string.settings_wifi_lock))) {
			isChecked=true;
			if (shared_pref.getBoolean(key_string, false)) {
				pref_key
					.setSummary(c.getString(R.string.settings_wifi_lock_summary_ena));
			} else {
				pref_key
					.setSummary(c.getString(R.string.settings_wifi_lock_summary_dis));
			}
    	}

    	return isChecked;
	};

	private static boolean checkUiSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_ui_alternate_ui))) {
				isChecked=true;
				if (shared_pref.getBoolean(key_string, false)) {
					pref_key
						.setSummary(c.getString(R.string.settings_ui_alternate_ui_summary_ena));
				} else {
					pref_key
						.setSummary(c.getString(R.string.settings_ui_alternate_ui_summary_dis));
				}
		} else if (key_string.equals(c.getString(R.string.settings_suppress_warning_mixed_mp))) {
			isChecked=true;
			if (shared_pref.getBoolean(key_string, false)) {
				pref_key
					.setSummary(c.getString(R.string.settings_suppress_warning_mixed_mp_summary_dis));
			} else {
				pref_key
					.setSummary(c.getString(R.string.settings_suppress_warning_mixed_mp_summary_ena));
			}
		} else if (key_string.equals(c.getString(R.string.settings_background_termination_notification))) {
			isChecked=true;
			if (shared_pref.getString(key_string,"0").equals(SMBSYNC_NOTIFICATION_MESSAGE_ALWAYS)) {
				pref_key
					.setSummary(c.getString(R.string.settings_background_termination_notification_summary_always));
			} else if (shared_pref.getString(key_string,"1").equals(SMBSYNC_NOTIFICATION_MESSAGE_ERROR)){
				pref_key
					.setSummary(c.getString(R.string.settings_background_termination_notification_summary_error));
			} else if (shared_pref.getString(key_string,"2").equals(SMBSYNC_NOTIFICATION_MESSAGE_NO)){
				pref_key
					.setSummary(c.getString(R.string.settings_background_termination_notification_summary_no));
			}
    	}

    	return isChecked;
	};

	private static boolean checkMiscSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_exit_clean))) {
			isChecked=true;
			if (shared_pref.getBoolean(key_string, true)) {
				pref_key
					.setSummary(c.getString(R.string.settings_exit_clean_summary_ena));
			} else {
				pref_key
					.setSummary(c.getString(R.string.settings_exit_clean_summary_dis));
			}
//    	} else if (key_string.equals(c.getString(R.string.settings_exported_profile_encryption))) {
//			isChecked=true;
//			if (shared_pref.getBoolean(key_string, true)) {
//				pref_key
//					.setSummary(c.getString(R.string.settings_exported_profile_encryption_summary_ena));
//			} else {
//				pref_key
//					.setSummary(c.getString(R.string.settings_exported_profile_encryption_summary_dis));
//			}
    	}

    	return isChecked;
	};


	private static boolean checkLogSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
    	if (key_string.equals(c.getString(R.string.settings_debug_msg_diplay))) {
    		isChecked=true;
    		if (shared_pref.getBoolean(key_string, false)) {
    			pref_key
    				.setSummary(c.getString(R.string.settings_debug_msg_diplay_summary_ena));
    		} else {
    			pref_key
    			.setSummary(c.getString(R.string.settings_debug_msg_diplay_summary_dis));
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_log_option))) {
    		isChecked=true;
    		if (shared_pref.getString(key_string, "0").equals("0")) {
				//option=0
    			if (Build.VERSION.SDK_INT>=11) {
    				mPrefFrag.findPreference(c.getString(R.string.settings_log_generation)).setEnabled(false);
    			} else {
    				mPrefAct.findPreference(c.getString(R.string.settings_log_generation)).setEnabled(false);
    			}
				pref_key
					.setSummary(c.getString(R.string.settings_log_option_summary_no));
			} else if (shared_pref.getString(key_string, "0").equals("1")) {
				//option=1
    			if (Build.VERSION.SDK_INT>=11) {
    				mPrefFrag.findPreference(c.getString(R.string.settings_log_generation)).setEnabled(true);
    			} else {
    				mPrefAct.findPreference(c.getString(R.string.settings_log_generation)).setEnabled(true);
    			}
				pref_key
					.setSummary(c.getString(R.string.settings_log_option_summary_date_time));
			}
    	} else if (key_string.equals(c.getString(R.string.settings_log_generation))) {
    		isChecked=true;
			pref_key
					.setSummary(String.format(c.getString(R.string.settings_log_generation_summary),
							shared_pref.getString(key_string, "10")));
    	}

    	return isChecked;
	};

	private static boolean checkMediaScannerSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
    	if (key_string.equals(c.getString(R.string.settings_media_scanner_non_media_files_scan))) {
    		isChecked=true;
    		if (shared_pref.getBoolean(key_string, true)) {
    			pref_key
    				.setSummary(c.getString(R.string.settings_media_scanner_non_media_files_scan_summary_ena));
    		} else {
    			pref_key
    			.setSummary(c.getString(R.string.settings_media_scanner_non_media_files_scan_summary_dis));
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_media_scanner_scan_extstg))) {
    		isChecked=true;
    		if (shared_pref.getBoolean(key_string, true)) {
    			pref_key
    				.setSummary(c.getString(R.string.settings_media_scanner_scan_extstg_summary_ena));
    		} else {
    			pref_key
    			.setSummary(c.getString(R.string.settings_media_scanner_scan_extstg_summary_dis));
    		}
    	}

    	return isChecked;
	};
	
	private static boolean checkDifferentialSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		
		if (key_string.equals(c.getString(R.string.settings_media_store_last_mod_time))) {
			isChecked=true;
    		String mso=shared_pref.getString(key_string, "0");
    		if (mso.equals("0"))
    			pref_key.setSummary(c.getString(R.string.settings_media_store_last_mod_time_0));
    		else if (mso.equals("1"))
    			pref_key.setSummary(c.getString(R.string.settings_media_store_last_mod_time_1));
    		else if (mso.equals("2"))
    			pref_key.setSummary(c.getString(R.string.settings_media_store_last_mod_time_2));
		}

    	return isChecked;
	};

	private static boolean checkSmbSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = false;
		if (key_string.equals(c.getString(R.string.settings_default_pass))) {
			isChecked=true;
        	if (shared_pref.getString(key_string, "").equals("")) {
        		pref_key
    				.setSummary(c.getString(R.string.settings_default_current_setting)+" ");
        	} else {
        		pref_key
    			.setSummary(c.getString(R.string.settings_default_current_setting)+"--------");
        	}
   		} else if (key_string.equals(c.getString(R.string.settings_smb_use_extended_security))) {
			isChecked=true;
    		if (shared_pref.getBoolean(key_string, false)) {
        		pref_key
    				.setSummary(c.getString(R.string.settings_smb_use_extended_security_summary_ena));
    		} else {
    			pref_key
				.setSummary(c.getString(R.string.settings_smb_use_extended_security_summary_dis));
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_smb_perform_class))) {
			isChecked=true;
        	if (shared_pref.getString(key_string, "").equals("0")) {
        		pref_key
    				.setSummary(c.getString(R.string.settings_smb_perform_class_0));
        	} else if (shared_pref.getString(key_string, "").equals("1")) {
        		pref_key
        		.setSummary(c.getString(R.string.settings_smb_perform_class_1));
        	} else if (shared_pref.getString(key_string, "").equals("2")) {
        		pref_key
        		.setSummary(c.getString(R.string.settings_smb_perform_class_2));
        	} else if (shared_pref.getString(key_string, "").equals("3")) {
        		pref_key
        		.setSummary(c.getString(R.string.settings_smb_perform_class_3));
        	} else {
        		pref_key
				.setSummary(c.getString(R.string.settings_smb_perform_class_0));
        	}
			if (Build.VERSION.SDK_INT>=11) {
	        	if (shared_pref.getString(key_string, "").equals("3")) 
	        		mPrefFrag.findPreference(c.getString(R.string.settings_smb_tuning)).setEnabled(true);
	        	else mPrefFrag.findPreference(c.getString(R.string.settings_smb_tuning)).setEnabled(false);
			} else {
	        	if (shared_pref.getString(key_string, "").equals("3")) 
	        		mPrefAct.findPreference(c.getString(R.string.settings_smb_tuning)).setEnabled(true);
	        	else mPrefAct.findPreference(c.getString(R.string.settings_smb_tuning)).setEnabled(false);
			}
    	} else if (key_string.equals(c.getString(R.string.settings_smb_log_level))) {
    		EditTextPreference etp=(EditTextPreference)pref_key;
    		etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    		if (shared_pref.getString(key_string, "").equals("")) {
    			shared_pref.edit().putString(key_string, "0").commit();
    			etp.setText("0");
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_smb_rcv_buf_size))) {
    		EditTextPreference etp=(EditTextPreference)pref_key;
    		etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    		if (shared_pref.getString(key_string, "").equals("")) {
    			shared_pref.edit().putString(key_string, "66576").commit();
    			etp.setText("66576");
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_smb_snd_buf_size))) {
    		EditTextPreference etp=(EditTextPreference)pref_key;
    		etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    		if (shared_pref.getString(key_string, "").equals("")) {
    			shared_pref.edit().putString(key_string, "66576").commit();
    			etp.setText("66576");
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_smb_listSize))) {
    		EditTextPreference etp=(EditTextPreference)pref_key;
    		etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
//    		if (shared_pref.getString(key_string, "").equals("")) {
//    			shared_pref.edit().putString(key_string, "1300").commit();
//    			etp.setText("1300"); //65535
//    		}
    	} else if (key_string.equals(c.getString(R.string.settings_smb_maxBuffers))) {
    		EditTextPreference etp=(EditTextPreference)pref_key;
    		etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    		if (shared_pref.getString(key_string, "").equals("")) {
    			shared_pref.edit().putString(key_string, "100").commit();
    			etp.setText("100");
    		}
    	} else if (key_string.equals(c.getString(R.string.settings_smb_tcp_nodelay))) {    		
    	} else if (key_string.equals(c.getString(R.string.settings_io_buffers))) {    		
    		EditTextPreference etp=(EditTextPreference)pref_key;
    		etp.getEditText().setInputType(InputType.TYPE_CLASS_NUMBER);
    		if (shared_pref.getString(key_string, "").equals("")) {
    			shared_pref.edit().putString(key_string, "8").commit();
    			etp.setText("8");
    		}
    	}

    	return isChecked;
	};

	private static boolean checkOtherSettings(Preference pref_key, 
			SharedPreferences shared_pref, String key_string, Context c) {
		boolean isChecked = true;
    	if (pref_key!=null) {
    		pref_key.setSummary(
	    		c.getString(R.string.settings_default_current_setting)+
	    		shared_pref.getString(key_string, "0"));
    	} else {
    		Log.v("SMBSyncSettings","key not found. key="+key_string);
    	}
    	return isChecked;
	};

 
    public static class SettingsSync extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentSync onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_sync);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_network_wifi_option));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_auto_start));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_auto_term));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_backgroound_execution));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_error_option));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_ui_keep_screen_on));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_wifi_lock));

        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentSync onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentSync onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };

    public static class SettingsDiff extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentDiff onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_diff);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_file_diff_time_seconds));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_media_store_last_mod_time));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentDiff onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentDiff onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
    
    public static class SettingsLog extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentLog onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_log);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
        	if (!LocalMountPoint.isExternalStorageAvailable()) {
        		findPreference(getString(R.string.settings_log_dir).toString())
        			.setEnabled(false);
        	}
    		
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_log_option));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_log_generation));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_log_dir));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_log_level));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentLog onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentLog onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
    
    public static class SettingsMedia extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMedia onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_media);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_media_scanner_non_media_files_scan));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_media_scanner_scan_extstg));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMedia onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMedia onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
    
    public static class SettingsMisc extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMisc onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_misc);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
        	shared_pref.edit().putBoolean(getString(R.string.settings_exit_clean),true).commit();
    		findPreference(getString(R.string.settings_exit_clean).toString()).setEnabled(false);
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_exit_clean));
//        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_exported_profile_encryption));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMisc onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentMisc onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
    
    public static class SettingsSmb extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentSmb onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_smb);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_default_user));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_default_pass));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_default_addr));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_smb_use_extended_security));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_smb_lm_compatibility));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_smb_perform_class));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_smb_log_level));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_smb_rcv_buf_size));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_smb_snd_buf_size));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_smb_listSize));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_smb_maxBuffers));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_smb_tcp_nodelay));
        	initSettingValueAfterHc(shared_pref,getString(R.string.settings_io_buffers));
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentSmb onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentSmb onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
    
    public static class SettingsUi extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
        	super.onCreate(savedInstanceState);
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentUi onCreate entered");
            
    		addPreferencesFromResource(R.xml.settings_frag_ui);

            mPrefFrag=this;
    		mContext=this.getActivity().getApplicationContext();

    		SharedPreferences shared_pref = PreferenceManager.getDefaultSharedPreferences(mContext);
    		
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_debug_msg_diplay));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_ui_alternate_ui));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_suppress_warning_mixed_mp));
    		initSettingValueAfterHc(shared_pref,getString(R.string.settings_background_termination_notification));
    		
        };
        
        @Override
        public void onStart() {
        	super.onStart();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentUi onStart entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.registerOnSharedPreferenceChangeListener(listenerAfterHc);
//    		getActivity().setTitle(R.string.settings_main_title);
        };
        @Override
        public void onStop() {
        	super.onStop();
        	if (DEBUG_ENABLE) Log.v(APPLICATION_TAG,"SettingsFragmentUi onStop entered");
    	    getPreferenceScreen().getSharedPreferences()
    			.unregisterOnSharedPreferenceChangeListener(listenerAfterHc);  
        };
    };
}