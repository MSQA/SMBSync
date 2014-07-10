package com.sentaroh.android.SMBSync;

import static com.sentaroh.android.SMBSync.Constants.*;
import static com.sentaroh.android.SMBSync.SchedulerConstants.*;

import java.util.Calendar;

import com.sentaroh.android.Utilities.DateUtil;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.util.Log;

public class SchedulerReceiver extends BroadcastReceiver{

	private static WakeLock mWakeLock=null;
	
	private static String log_id="SCHEDULER    ";
	
	private static Context mContext =null;
	
	private static int debugLevel=1;

	@SuppressLint("Wakelock")
	@Override
	final public void onReceive(Context context, Intent arg1) {
		if (mWakeLock==null) mWakeLock=
   	    		((PowerManager)context.getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK    					
    				| PowerManager.ON_AFTER_RELEASE, "SMBSync-Receiver");
		if (!mWakeLock.isHeld()) mWakeLock.acquire(1000);
		mContext=context;
//		mWakeLock.acquire(100);
		loadScheduleData();
		
		String action=arg1.getAction();
//		Log.v("","action="+action);
		if (action!=null) {
			if (action.equals(Intent.ACTION_BOOT_COMPLETED)) {
				setTimer();
			} else if (action.equals(SCHEDULER_SET_TIMER)) {
				setTimer();
			} else if (action.equals(SCHEDULER_TIMER_EXPIRED)) {
				startSync();
				setTimer();
			}
		}
	};

	static private void startSync() {
    	Intent in=new Intent(mContext,SMBSyncMain.class);
    	in.putExtra(SMBSYNC_SCHEDULER_ID,"SMBSync Scheduler");
    	String[] prof=mSchedulerSyncProfile.split(",");
    	in.putExtra(SMBSYNC_EXTRA_PARM_SYNC_PROFILE, prof);
    	in.putExtra(SMBSYNC_EXTRA_PARM_AUTO_START, true);
    	in.putExtra(SMBSYNC_EXTRA_PARM_AUTO_TERM, mSchedulerSyncOptionAutoterm);
    	in.putExtra(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION, mSchedulerSyncOptionBgExec);
    	in.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    	mContext.startActivity(in);
	};
	
    static private void setTimer() {
    	if (debugLevel!=0) addDebugMsg(1,"I", "setTimer entered");
    	cancelTimer();
		if (mSchedulerScheduleEmabled) {
	    	long time=getNextSchedule();
			Intent iw = new Intent();
			iw.setAction(SCHEDULER_TIMER_EXPIRED);
			iw.putExtra("date_time",time);
			PendingIntent piw = PendingIntent.getBroadcast(mContext, 0, iw,
					PendingIntent.FLAG_UPDATE_CURRENT);
		    AlarmManager amw = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
		    amw.set(AlarmManager.RTC_WAKEUP, time, piw);
		}
    };
    
    static private void cancelTimer() {
    	if (debugLevel!=0) addDebugMsg(1,"I", "cancelTimer entered");
		Intent iw = new Intent();
		iw.setAction(SCHEDULER_TIMER_EXPIRED);
		PendingIntent piw = PendingIntent.getBroadcast(mContext, 0, iw,
				PendingIntent.FLAG_CANCEL_CURRENT);
	    AlarmManager amw = (AlarmManager)mContext.getSystemService(Context.ALARM_SERVICE);
	    amw.cancel(piw);
    };
    
    private static boolean mSchedulerScheduleEmabled=false;
    private static String mSchedulerScheduleType="";
    private static String mSchedulerScheduleHours="";
    private static String mSchedulerScheduleMinutes="";
    private static String mSchedulerScheduleDayOfTheWeek="";
    private static String mSchedulerSyncProfile="";
    private static boolean mSchedulerSyncOptionAutostart=false;
    private static boolean mSchedulerSyncOptionAutoterm=false;
    private static boolean mSchedulerSyncOptionBgExec=false;
    
    static private void loadScheduleData() {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
    	debugLevel=Integer.parseInt(prefs.getString(mContext.getString(R.string.settings_log_level), "0"));
    	mSchedulerScheduleEmabled=prefs.getBoolean(SCHEDULER_SCHEDULE_ENABLED_KEY, false);
    	mSchedulerScheduleType=prefs.getString(SCHEDULER_SCHEDULE_TYPE_KEY, SCHEDULER_SCHEDULE_TYPE_EVERY_DAY);
    	mSchedulerScheduleHours=prefs.getString(SCHEDULER_SCHEDULE_HOURS_KEY, "00");
    	mSchedulerScheduleMinutes=prefs.getString(SCHEDULER_SCHEDULE_MINUTES_KEY, "00");
    	mSchedulerScheduleDayOfTheWeek=prefs.getString(SCHEDULER_SCHEDULE_TYPE_DAY_OF_THE_WEEK, "0000000");
    	
    	mSchedulerSyncProfile=prefs.getString(SCHEDULER_SYNC_PROFILE_KEY, "");
    	mSchedulerSyncOptionAutostart=prefs.getBoolean(SCHEDULER_SYNC_OPTION_AUTOSTART_KEY, false);
    	mSchedulerSyncOptionAutoterm=prefs.getBoolean(SCHEDULER_SYNC_OPTION_AUTOTERM_KEY, false);
    	mSchedulerSyncOptionBgExec=prefs.getBoolean(SCHEDULER_SYNC_OPTION_BGEXEC_KEY, false);
    	
    	if (debugLevel!=0) 
    		addDebugMsg(1,"I", "loadScheduleData type="+mSchedulerScheduleType+
    				", hours="+mSchedulerScheduleHours+
    				", minutes="+mSchedulerScheduleMinutes+
    				", dw="+mSchedulerScheduleDayOfTheWeek+
    				", sync_prof="+mSchedulerSyncProfile+
    				", auto_start="+mSchedulerSyncOptionAutostart+
    				", auto_term="+mSchedulerSyncOptionAutoterm+
    				", bg_exec="+mSchedulerSyncOptionBgExec
    				);
    };
    
    static private long getNextSchedule() {
    	Calendar cal=Calendar.getInstance();
    	cal.setTimeInMillis(System.currentTimeMillis());
    	long result=0;
		int s_hrs=Integer.parseInt(mSchedulerScheduleHours);
		int s_min=Integer.parseInt(mSchedulerScheduleMinutes);
		int c_year=cal.get(Calendar.YEAR);
		int c_month=cal.get(Calendar.MONTH);
		int c_day=cal.get(Calendar.DAY_OF_MONTH);
		int c_dw=cal.get(Calendar.DAY_OF_WEEK);
		int c_hr=cal.get(Calendar.HOUR_OF_DAY);
		int c_mm=cal.get(Calendar.MINUTE);
		if (mSchedulerScheduleType.equals(SCHEDULER_SCHEDULE_TYPE_EVERY_HOURS)) {
    		if (c_mm>=s_min) {
        		cal.set(c_year, c_month, c_day, c_hr, 0, 0);
        		result=cal.getTimeInMillis()+(60*1000*60)+(60*1000*s_min);
    		} else {
        		cal.set(c_year, c_month, c_day, c_hr, 0, 0);
        		result=cal.getTimeInMillis()+(60*1000*s_min);
    		}
//    		cal.set(c_year, c_month, c_day, c_hr, c_mm, 0);
//    		result=cal.getTimeInMillis()+(60*1000);
    	} else if (mSchedulerScheduleType.equals(SCHEDULER_SCHEDULE_TYPE_EVERY_DAY)) {
    		cal.set(c_year, c_month, c_day, s_hrs, 0, 0);
    		if ((c_hr*100+c_mm)>=(s_hrs*100+s_min)) result=cal.getTimeInMillis()+(60*1000*60*24)+(60*1000*s_min);
    		else result=cal.getTimeInMillis()+(60*1000*s_min);
    	} else if (mSchedulerScheduleType.equals(SCHEDULER_SCHEDULE_TYPE_DAY_OF_THE_WEEK)) {
    		boolean[] dwa=new boolean[]{false,false,false,false,false,false,false};
    		for (int i=0;i<mSchedulerScheduleDayOfTheWeek.length();i++) {
    			if (mSchedulerScheduleDayOfTheWeek.substring(i, i+1).equals("1")) dwa[i]=true;
    		}
        	int s_hhmm=Integer.parseInt(mSchedulerScheduleHours)*100+s_min;
        	int c_hhmm=c_hr*100+c_mm;
        	Log.v("","c_hhmm="+c_hhmm+", s_hhmm="+s_hhmm);
        	if (c_hhmm>=s_hhmm) c_dw++;
        	int s_dw=-1;
    		if (c_dw>=7) {//sat
    			if (dwa[6]) s_dw=1;
    		} else {
//    			Log.v("","c_dw="+c_dw);
        		if (c_dw>=7) {//sat
        			if (dwa[6]) s_dw=1;
        		} else {
        			for (int i=c_dw-1;i<7;i++) {
        				if (dwa[i]) {
            				s_dw=i-1;
//            				Log.v("","i1="+i);
            				break;
        				}
        			}
        			if (s_dw==-1) {
            			for (int i=0;i<c_dw-1;i++) {
            				if (dwa[i]) {
                				s_dw=i+(7-c_dw)+1;
//                				Log.v("","i2="+i);
                				break;
            				}
            			}
        			}
        		}
//        		Log.v("","s_dw="+s_dw);
    		}
    		cal.set(c_year, c_month, c_day, s_hrs, 0, 0);
    		result=cal.getTimeInMillis()+s_dw*(60*1000*60*24)+(60*1000*s_min);
    	}
    	if (debugLevel!=0) 
    		addDebugMsg(1,"I", "getNextSchedule result="+DateUtil.convDateTimeTo_YearMonthDayHourMinSec(result));
    	return result;
    };
    
  
	final static private void addDebugMsg(int lvl, String cat, String... msg) {
			StringBuilder print_msg=new StringBuilder("D ");
			print_msg.append(cat);
			StringBuilder log_msg=new StringBuilder(512);
			for (int i=0;i<msg.length;i++) log_msg.append(msg[i]);
			Log.v(APPLICATION_TAG,cat+" "+log_id+log_msg.toString());
	}
}
