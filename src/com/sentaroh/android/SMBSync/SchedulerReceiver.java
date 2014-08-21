package com.sentaroh.android.SMBSync;

import static com.sentaroh.android.SMBSync.Constants.*;
import static com.sentaroh.android.SMBSync.SchedulerConstants.*;

import com.sentaroh.android.Utilities.DateUtil;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class SchedulerReceiver extends BroadcastReceiver{

	private static WakeLock mWakeLock=null;
	
	private static Context mContext =null;
	
	private static SchedulerParms mSched=null;
	
	@SuppressLint("Wakelock")
	@Override
	final public void onReceive(Context context, Intent arg1) {
		if (mWakeLock==null) mWakeLock=
   	    		((PowerManager)context.getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK    					
    				| PowerManager.ON_AFTER_RELEASE, "SMBSync-Receiver");
		if (!mWakeLock.isHeld()) mWakeLock.acquire(1000);
		if (mSched==null) mSched=new SchedulerParms();
		mContext=context;
		String action=arg1.getAction();
		if (mSched.debugLevel>0) addDebugMsg(1,"I", "Receiver action="+action);
//		mWakeLock.acquire(100);
		loadScheduleData();
		if (action!=null) {
			if (action.equals(Intent.ACTION_BOOT_COMPLETED) || 
					action.equals(Intent.ACTION_DATE_CHANGED) || 
					action.equals(Intent.ACTION_TIMEZONE_CHANGED) || 
					action.equals(Intent.ACTION_TIME_CHANGED) ||
					action.equals(Intent.ACTION_PACKAGE_REPLACED)) {
				setTimer();
			} else if (action.equals(SCHEDULER_SET_TIMER)) {
				setTimer();
			} else if (action.equals(SCHEDULER_SET_TIMER_IF_NOT_SET)) {
				if (!isTimerScheduled()) setTimer();
			} else if (action.equals(SCHEDULER_TIMER_EXPIRED)) {
				startSync();
				setTimer();
			}
		}
	};
	
	static private void loadScheduleData() {
		SchedulerUtil.loadScheduleData(mSched, mContext);
		if (mSched.debugLevel>0) 
			addDebugMsg(1,"I", "loadScheduleData type="+mSched.scheduleType+
				", hours="+mSched.scheduleHours+
				", minutes="+mSched.scheduleMinutes+
				", dw="+mSched.scheduleDayOfTheWeek+
				", sync_prof="+mSched.syncProfile+
				", auto_start="+mSched.syncOptionAutostart+
				", auto_term="+mSched.syncOptionAutoterm+
				", bg_exec="+mSched.syncOptionBgExec);
	};

	static private void startSync() {
    	Intent in=new Intent(mContext,SMBSyncMain.class);
    	in.putExtra(SMBSYNC_SCHEDULER_ID,"SMBSync Scheduler");
    	String[] prof=mSched.syncProfile.split(",");
    	in.putExtra(SMBSYNC_EXTRA_PARM_SYNC_PROFILE, prof);
    	in.putExtra(SMBSYNC_EXTRA_PARM_AUTO_START, true);
    	in.putExtra(SMBSYNC_EXTRA_PARM_AUTO_TERM, mSched.syncOptionAutoterm);
    	in.putExtra(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION, mSched.syncOptionBgExec);
    	in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	mContext.startActivity(in);
	};
	
    static private void setTimer() {
    	if (mSched.debugLevel>0) addDebugMsg(1,"I", "setTimer entered");
    	cancelTimer();
		if (mSched.scheduleEmabled) {
			long time=SchedulerUtil.getNextSchedule(mSched);
			if (mSched.debugLevel>0) 
				addDebugMsg(1,"I", "getNextSchedule result="+DateUtil.convDateTimeTo_YearMonthDayHourMinSec(time));
			Intent in = new Intent();
			in.setAction(SCHEDULER_TIMER_EXPIRED);
			PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, in, PendingIntent.FLAG_UPDATE_CURRENT);
		    AlarmManager am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
		    am.set(AlarmManager.RTC_WAKEUP, time, pi);
		}
    };
    
	private static boolean isTimerScheduled() {
		Intent iw = new Intent();
		iw.setAction(SCHEDULER_TIMER_EXPIRED);
	    PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, iw, PendingIntent.FLAG_NO_CREATE);
	    if(pi == null) {
	        return false;
	    }else {
	        return true;
	    }
	};
    
    static private void cancelTimer() {
    	if (mSched.debugLevel>0) addDebugMsg(1,"I", "cancelTimer entered");
		Intent in = new Intent();
		in.setAction(SCHEDULER_TIMER_EXPIRED);
		PendingIntent pi = PendingIntent.getBroadcast(mContext, 0, in, PendingIntent.FLAG_CANCEL_CURRENT);
	    AlarmManager am = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
	    am.cancel(pi);
    };
    
	private static String log_id="SCHEDULER    ";
	  
	final static public void addDebugMsg(int lvl, String cat, String... msg) {
			StringBuilder print_msg=new StringBuilder("D ");
			print_msg.append(cat);
			StringBuilder log_msg=new StringBuilder(512);
			for (int i=0;i<msg.length;i++) log_msg.append(msg[i]);
			Log.v(APPLICATION_TAG,cat+" "+log_id+log_msg.toString());
	};

}
