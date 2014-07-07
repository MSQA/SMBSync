package com.sentaroh.android.SMBSync;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

public class SchedulerReceiver extends BroadcastReceiver{

	private static WakeLock mWakeLock=null;

	@SuppressLint("Wakelock")
	@Override
	final public void onReceive(Context context, Intent arg1) {
		if (mWakeLock==null) mWakeLock=
   	    		((PowerManager)context.getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK    					
    				| PowerManager.ON_AFTER_RELEASE, "TaskAutomation-Receiver");
		if (!mWakeLock.isHeld()) mWakeLock.acquire(100);
//		mWakeLock.acquire(100);
		
		String action=arg1.getAction();
		if (action!=null) {
			Intent in = new Intent(context, SchedulerService.class);
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
//				CommonUtilities.clearSavedBluetoothConnected(context);
//				CommonUtilities.clearSavedWifiSsid(context);
			}
			in.setAction(action);
			context.startService(in);
		}
	};
}
