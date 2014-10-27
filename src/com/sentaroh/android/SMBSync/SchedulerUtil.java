package com.sentaroh.android.SMBSync;

import static com.sentaroh.android.SMBSync.SchedulerConstants.*;

import java.util.Calendar;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class SchedulerUtil {

    final static public void loadScheduleData(SchedulerParms sp, Context c) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	sp.debugLevel=Integer.parseInt(prefs.getString(c.getString(R.string.settings_log_level), "0"));
    	sp.scheduleEnabled=prefs.getBoolean(SCHEDULER_SCHEDULE_ENABLED_KEY, false);
    	sp.scheduleType=prefs.getString(SCHEDULER_SCHEDULE_TYPE_KEY, SCHEDULER_SCHEDULE_TYPE_EVERY_DAY);
    	sp.scheduleHours=prefs.getString(SCHEDULER_SCHEDULE_HOURS_KEY, "00");
    	sp.scheduleMinutes=prefs.getString(SCHEDULER_SCHEDULE_MINUTES_KEY, "00");
    	sp.scheduleDayOfTheWeek=prefs.getString(SCHEDULER_SCHEDULE_DAY_OF_THE_WEEK_KEY, "0000000");
    	
    	sp.syncProfile=prefs.getString(SCHEDULER_SYNC_PROFILE_KEY, "");
    	sp.syncOptionAutostart=prefs.getBoolean(SCHEDULER_SYNC_OPTION_AUTOSTART_KEY, false);
    	sp.syncOptionAutoterm=prefs.getBoolean(SCHEDULER_SYNC_OPTION_AUTOTERM_KEY, false);
    	sp.syncOptionBgExec=prefs.getBoolean(SCHEDULER_SYNC_OPTION_BGEXEC_KEY, false);

    	sp.syncWifiOnBeforeSyncStart=prefs.getBoolean(SCHEDULER_SYNC_WIFI_ON_BEFORE_SYNC_START_KEY, false);
    	sp.syncWifiOffAfterSyncEnd=prefs.getBoolean(SCHEDULER_SYNC_WIFI_OFF_AFTER_SYNC_END_KEY, false);

    	sp.syncDelayedSecondForWifiOn=Integer.parseInt(
    			prefs.getString(SCHEDULER_SYNC_DELAYED_TIME_FOR_WIFI_ON_KEY, 
    					SCHEDULER_SYNC_DELAYED_TIME_FOR_WIFI_ON_DEFAULT_VALUE));
    	
    };

    final static public void saveScheduleData(SchedulerParms sp, Context c) {
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
    	prefs.edit().putBoolean(SCHEDULER_SCHEDULE_ENABLED_KEY, sp.scheduleEnabled).commit();
    	prefs.edit().putString(SCHEDULER_SCHEDULE_TYPE_KEY, sp.scheduleType).commit();
    	prefs.edit().putString(SCHEDULER_SCHEDULE_HOURS_KEY, sp.scheduleHours).commit();
    	prefs.edit().putString(SCHEDULER_SCHEDULE_MINUTES_KEY, sp.scheduleMinutes).commit();
    	prefs.edit().putString(SCHEDULER_SCHEDULE_DAY_OF_THE_WEEK_KEY, sp.scheduleDayOfTheWeek).commit();
    	
    	prefs.edit().putString(SCHEDULER_SYNC_PROFILE_KEY, sp.syncProfile).commit();
    	prefs.edit().putBoolean(SCHEDULER_SYNC_OPTION_AUTOSTART_KEY, sp.syncOptionAutostart).commit();
    	prefs.edit().putBoolean(SCHEDULER_SYNC_OPTION_AUTOTERM_KEY, sp.syncOptionAutoterm).commit();
    	prefs.edit().putBoolean(SCHEDULER_SYNC_OPTION_BGEXEC_KEY, sp.syncOptionBgExec).commit();

    	prefs.edit().putBoolean(SCHEDULER_SYNC_WIFI_ON_BEFORE_SYNC_START_KEY, sp.syncWifiOnBeforeSyncStart).commit();
    	prefs.edit().putBoolean(SCHEDULER_SYNC_WIFI_OFF_AFTER_SYNC_END_KEY, sp.syncWifiOffAfterSyncEnd).commit();
    	
    	prefs.edit().putString(SCHEDULER_SYNC_DELAYED_TIME_FOR_WIFI_ON_KEY, 
    			String.valueOf(sp.syncDelayedSecondForWifiOn)).commit();
    };

    final static public long getNextSchedule(SchedulerParms sp) {
    	Calendar cal=Calendar.getInstance();
    	cal.setTimeInMillis(System.currentTimeMillis());
    	long result=0;
		int s_hrs=Integer.parseInt(sp.scheduleHours);
		int s_min=Integer.parseInt(sp.scheduleMinutes);
		int c_year=cal.get(Calendar.YEAR);
		int c_month=cal.get(Calendar.MONTH);
		int c_day=cal.get(Calendar.DAY_OF_MONTH);
		int c_dw=cal.get(Calendar.DAY_OF_WEEK)-1;
		int c_hr=cal.get(Calendar.HOUR_OF_DAY);
		int c_mm=cal.get(Calendar.MINUTE);
		if (sp.scheduleType.equals(SCHEDULER_SCHEDULE_TYPE_EVERY_HOURS)) {
    		if (c_mm>=s_min) {
        		cal.set(c_year, c_month, c_day, c_hr, 0, 0);
        		result=cal.getTimeInMillis()+(60*1000*60)+(60*1000*s_min);
    		} else {
        		cal.set(c_year, c_month, c_day, c_hr, 0, 0);
        		result=cal.getTimeInMillis()+(60*1000*s_min);
    		}
//    		cal.set(c_year, c_month, c_day, c_hr, c_mm, 0);
//    		result=cal.getTimeInMillis()+(60*1000);
    	} else if (sp.scheduleType.equals(SCHEDULER_SCHEDULE_TYPE_EVERY_DAY)) {
    		cal.set(c_year, c_month, c_day, s_hrs, 0, 0);
    		if ((c_hr*100+c_mm)>=(s_hrs*100+s_min)) result=cal.getTimeInMillis()+(60*1000*60*24)+(60*1000*s_min);
    		else result=cal.getTimeInMillis()+(60*1000*s_min);
    	} else if (sp.scheduleType.equals(SCHEDULER_SCHEDULE_TYPE_DAY_OF_THE_WEEK)) {
    		boolean[] dwa=new boolean[]{false,false,false,false,false,false,false};
    		for (int i=0;i<sp.scheduleDayOfTheWeek.length();i++) {
    			String dw_s=sp.scheduleDayOfTheWeek.substring(i, i+1);
    			if (dw_s.equals("1")) dwa[i]=true;
//    			Log.v("","i="+i+", de_s="+dw_s+", dwa="+dwa[i]);
    		}
        	int s_hhmm=Integer.parseInt(sp.scheduleHours)*100+s_min;
        	int c_hhmm=c_hr*100+c_mm;
//        	Log.v("","c_hhmm="+c_hhmm+", s_hhmm="+s_hhmm+", c_dw="+c_dw);
        	int s_dw=0;
        	if (c_hhmm>=s_hhmm) {
        		if (c_dw==6) {
        			c_dw=0;
        			s_dw=1;
        			for (int i=c_dw;i<7;i++) {
        				if (dwa[i]) {
        					break;
        				}
        				s_dw++;
        			}
//        			Log.v("","c1 s_dw="+s_dw);
        		} else {
        			c_dw++;
        			s_dw=1;
        			boolean found=false;
        			for (int i=c_dw;i<7;i++) {
        				if (dwa[i]) {
//        					Log.v("","c2 s_dw="+s_dw+", i="+i);
        					found=true;
        					break;
        				}
        				s_dw++;
        			}
        			if (!found) {
            			for (int i=0;i<c_dw;i++) {
            				if (dwa[i]) {
//            					Log.v("","c3 s_dw="+s_dw+", i="+i);
            					found=true;
            					break;
            				}
            				s_dw++;
            			}
        			}
        		}
        	} else {
    			s_dw=0;
    			boolean found=false;
//				Log.v("","c_dw="+c_dw);
    			for (int i=c_dw;i<7;i++) {
    				if (dwa[i]) {
//    					Log.v("","c4 s_dw="+s_dw);
    					found=true;
    					break;
    				}
    				s_dw++;
    			}
    			if (!found) {
        			for (int i=0;i<c_dw;i++) {
        				if (dwa[i]) {
//        					Log.v("","c5 s_dw="+s_dw);
        					found=true;
        					break;
        				}
        				s_dw++;
        			}
    			}
        	}
//    		Log.v("","s_dw="+s_dw);
    		cal.set(c_year, c_month, c_day, s_hrs, 0, 0);
    		result=cal.getTimeInMillis()+s_dw*(60*1000*60*24)+(60*1000*s_min);
    	}
    	return result;
    };
}

class SchedulerParms {
	public int debugLevel=0;
    public boolean scheduleEnabled=false;
    public String scheduleType="";
    public String scheduleHours="";
    public String scheduleMinutes="";
    public String scheduleDayOfTheWeek="";
    public String syncProfile="";
    public boolean syncOptionAutostart=false;
    public boolean syncOptionAutoterm=false;
    public boolean syncOptionBgExec=false;
    
    public boolean syncWifiOnBeforeSyncStart=false;
    public boolean syncWifiOffAfterSyncEnd=false;
    public int syncDelayedSecondForWifiOn=10;
}
