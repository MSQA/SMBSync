package com.sentaroh.android.SMBSync;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class SMBSyncReceiver extends BroadcastReceiver{

//	private boolean defaultSettingEnableScheduler=false;
	private static WakeLock mWakeLock=null;

//	private SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy/MM/dd HH:mm",Locale.getDefault());
	@SuppressLint("Wakelock")
	@Override
	final public void onReceive(Context context, Intent arg1) {
		if (mWakeLock==null) mWakeLock=
   	    		((PowerManager)context.getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK    					
    				| PowerManager.ON_AFTER_RELEASE, "TaskAutomation-Receiver");
		if (!mWakeLock.isHeld()) mWakeLock.acquire(100);
//		mWakeLock.acquire(100);
		
//		initSettingParms();
		String action=arg1.getAction();
		if (action!=null) {
			Intent in = new Intent(context, SMBSyncScheduler.class);
//			if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
//				CommonUtilities.clearSavedWifiSsid(context);
//			}
			in.setAction(action);
			context.startService(in);
		}
	};
	
//	private void initSettingParms() {
//		SharedPreferences prefs = context.getSharedPreferences(DEFAULT_PREFS_FILENAME,
//        		Context.MODE_PRIVATE|Context.MODE_MULTI_PROCESS);
//		defaultSettingEnableScheduler=prefs.getBoolean(context.getString(R.string.settings_main_enable_scheduler), true);
//	};

}
