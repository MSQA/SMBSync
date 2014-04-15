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

import static com.sentaroh.android.SMBSync.Constants.DEBUG_ENABLE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_ON;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_SPEC_AP;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.sentaroh.android.Utilities.DateUtil;
import com.sentaroh.android.Utilities.MiscUtil;

@SuppressLint("SimpleDateFormat")
public class SMBSyncUtil {
	
	private final static String DEBUG_TAG = "SMBSync";
	
	private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
	
	private GlobalParameters glblParms=null;
	
	private Context mContext=null;
	
	public SMBSyncUtil (Context c, String lid, GlobalParameters gp) {
		mContext=c;
		glblParms=gp;
		setLogIdentifier(lid);
	};
	
	public boolean setActivityIsForeground(boolean d) {
		glblParms.activityIsForeground=d;
		return d;
	};

	public boolean isDebuggable() {
        PackageManager manager = mContext.getPackageManager();
        ApplicationInfo appInfo = null;
        try {
            appInfo = manager.getApplicationInfo(mContext.getPackageName(), 0);
        } catch (NameNotFoundException e) {
            return false;
        }
        if ((appInfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE)
            return true;
        return false;
    };
	
	public boolean isActivityForeground() {return glblParms.activityIsForeground;};
	
	public boolean isRemoteDisable() {
		boolean ret=false;
		boolean ws=isWifiActive();
		if (glblParms.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF)) {
			ret=false;
		} else {
			if (ws) ret=false;
			else ret=true;
		}
		
		if (DEBUG_ENABLE) addDebugLogMsg(2,"I","isRemoteDisable settingWifiOption="+glblParms.settingWifiOption+
				", WifiConnected="+ws+", result="+ret);
		
		return ret;
	};
	
	public boolean isWifiActive() { 
		boolean ret=false;
		WifiManager mWifi = 
				(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
//		if (mWifi.getConnectionInfo().getSupplicantState()==
//				SupplicantState.COMPLETED)
		String ssid="";
		if (mWifi.isWifiEnabled()) {
			if (glblParms.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_ON)) {
				ret=true;
			} else if (glblParms.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP)) {
				ssid=mWifi.getConnectionInfo().getSSID();
				if (ssid!=null && 
						!ssid.equals("0x") &&
						!ssid.equals("")) ret=true;
			} else if (glblParms.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_SPEC_AP)) {
				ret=true;
			}
		}
		if (DEBUG_ENABLE) addDebugLogMsg(2,"I","isWifiActive WifiEnabled="+mWifi.isWifiEnabled()+
				", settingWifiOption="+glblParms.settingWifiOption+
				", SSID="+ssid+", result="+ret);
		return ret;
	};
	
	public static String getLocalIpAddress() {
		String result="";
	    try {
	        for (Enumeration<NetworkInterface> en = 
	        		NetworkInterface.getNetworkInterfaces();
	        		en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = 
	            		intf.getInetAddresses(); 
	            		enumIpAddr.hasMoreElements();) {
	            	InetAddress inetAddress = enumIpAddr.nextElement();
//	                if (!inetAddress.isLoopbackAddress() && !(inetAddress.toString().indexOf(":")>=0)) {
//	                    return inetAddress.getHostAddress().toString();
//	                }
//	            	Log.v("","ip="+inetAddress.getHostAddress());
	            	if (inetAddress.isSiteLocalAddress()) {
	                    result=inetAddress.getHostAddress();
	                    break;
	            	}
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e(DEBUG_TAG, ex.toString());
	        result="192.168.0.1";
	    }
//		Log.v("","getLocalIpAddress result="+result);
	    if (result.equals("")) result="192.168.0.1";
	    return result;
	};

	public static String getIfIpAddress() {
		String result="";
	    try {
	        for (Enumeration<NetworkInterface> en = 
	        		NetworkInterface.getNetworkInterfaces();
	        		en.hasMoreElements();) {
	            NetworkInterface intf = en.nextElement();
	            for (Enumeration<InetAddress> enumIpAddr = 
	            		intf.getInetAddresses(); 
	            		enumIpAddr.hasMoreElements();) {
	            	InetAddress inetAddress = enumIpAddr.nextElement();
//	            	Log.v("","ip="+inetAddress.getHostAddress());
	            	if (!inetAddress.isLoopbackAddress() &&
	            			(inetAddress.getHostAddress().startsWith("0") || 
	            					inetAddress.getHostAddress().startsWith("1") || 
	            					inetAddress.getHostAddress().startsWith("2"))) {
	                    result=inetAddress.getHostAddress();
	                    break;
	            	}
	            }
	        }
	    } catch (SocketException ex) {
	        Log.e(DEBUG_TAG, ex.toString());
	        result="192.168.0.1";
	    }
//		Log.v("","getIfIpAddress result="+result);
	    if (result.equals("")) result="192.168.0.1";
	    return result;
	};

	private StringBuilder mSbForaddMsgToProgDlg = new StringBuilder(256);
	final public void addMsgToProgDlg(boolean log, String log_cat, String syncProfName, 
			String fp, String log_msg) {
		String msgflag="";
		if (log) msgflag="1"; 	// flag=1 both, arg2=0 dialog only, arg2=2 msgview only
		else msgflag="0";
		String dt=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		sendMsgToActivity(log_cat,msgflag,syncProfName,dt.substring(0,10),
				dt.substring(11),mLogId,"M",fp,log_msg);
		if (!msgflag.equals("0")) 
			writeLogMsgToFile(glblParms.logWriter,log_cat,syncProfName, fp,log_msg);
		if (glblParms.debugLevel>0) { 
			if (glblParms.debugLevel>0 && log) 
				Log.v(DEBUG_TAG,
					buildLogCatString(mSbForaddMsgToProgDlg,
							log_cat,mLogId,syncProfName,fp,log_msg));
		}
	};

	private StringBuilder mSbForWriteLog = new StringBuilder(256);
	final private void writeLogMsgToFile(PrintWriter pw, String cat, String prof, 
			String fp, String msg) {
		if (pw!=null) { 
			synchronized(pw) {
				String dt=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
				mSbForWriteLog.setLength(0);
				mSbForWriteLog.append(cat).append(" ")
					.append(dt.substring(0,10)).append(" ")
					.append(dt.substring(11)).append(" ")
					.append(mLogId);
				if (!prof.equals("")) {
					mSbForWriteLog.append(prof).append(" ");
				}
				if (!fp.equals("")) {
					mSbForWriteLog.append(fp).append(" ");
				}
				mSbForWriteLog.append(msg);
				try {
					pw.println(mSbForWriteLog);
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
//			if (logFileFlushCnt>1000) {
//				flushLogFile();
//				logFileFlushCnt=0;
//			} else logFileFlushCnt++;
		}
	};
	
	private StringBuilder mSbForsendMsgToActivity=new StringBuilder(256);
	final public void sendMsgToActivity(final String log_cat, final String msgflag, final String sync_prof,
			final String date, final String time, final String tag, final String debug_flag, final String fp, final String msg_text) {
		glblParms.uiHandler.post(new Runnable(){
			@Override
			public void run() {
				if (msgflag.equals("0")) {
					glblParms.mainViewProgressProf.setText(sync_prof);
					glblParms.mainViewProgressFilepath.setText(fp);
					glblParms.mainViewProgressMessage.setText(msg_text);
					NotificationUtil.showOngoingNotificationMsg(glblParms,sync_prof,fp,msg_text);
				} else { //
					if (msgflag.equals("1")) {
						glblParms.mainViewProgressProf.setText(sync_prof);
						glblParms.mainViewProgressFilepath.setText(fp);
						glblParms.mainViewProgressMessage.setText(msg_text);
						NotificationUtil.showOngoingNotificationMsg(glblParms,sync_prof,fp,msg_text);
					}  
					if (debug_flag.equals("M") || 
							(debug_flag.equals("D")&&glblParms.settingDebugMsgDisplay)) {
						mSbForsendMsgToActivity.setLength(0);
						if (!sync_prof.equals("")) mSbForsendMsgToActivity.append(sync_prof).append(" ");
						if (!fp.equals("")) mSbForsendMsgToActivity.append(fp).append(" ");
						mSbForsendMsgToActivity.append(msg_text);
						addMsgToMsglistAdapter(glblParms,
								new MsgListItem(log_cat,date,time,tag,
										mSbForsendMsgToActivity.toString()));
					}
				}
			}
		});
	};

	private StringBuilder mSbForAddLogMsg=new StringBuilder(256);
	final public void addLogMsg(String log_cat, String sync_prof, String fp, String log_msg) {
		String dt=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		// flag=1 both, arg2=0 dialog only, arg2=2 msgview only
		sendMsgToActivity(log_cat,"2",sync_prof,dt.substring(0,10), dt.substring(11),
				mLogId,"M",fp,log_msg);
		writeLogMsgToFile(glblParms.logWriter,"M "+log_cat, sync_prof, fp, log_msg);
		if (glblParms.debugLevel>0)
			Log.v(DEBUG_TAG,
					buildLogCatString(mSbForAddLogMsg, log_cat,mLogId,sync_prof,fp,log_msg));
	};

	final public void addLogMsg(String cat, String logmsg) {
		addMsgToMsglistAdapter(glblParms,
			  		 new MsgListItem(cat,sdfDate.format(System.currentTimeMillis()),
					 sdfTime.format(System.currentTimeMillis()),"MAIN",logmsg));
		if (glblParms.logWriter!=null) {
			synchronized(glblParms.logWriter) {
				glblParms.logWriter.println("M "+cat+" "+
					sdfDate.format(System.currentTimeMillis())+" "+
					sdfTime.format(System.currentTimeMillis())+" "+
					("MAIN"+"          ").substring(0,13)+logmsg);
//				glblParms.logWriter.flush();
			}
		}
		if (DEBUG_ENABLE ) {
			if (glblParms.debugLevel>0) Log.v(DEBUG_TAG,cat+" "+mLogId+logmsg);
		}
	};
	
	final static public void addMsgToMsglistAdapter(
			final GlobalParameters gp, MsgListItem mli) {
		if (gp.msgListAdapter!=null) {
			if (gp.msgListAdapter.getCount()>5000) { 
				for (int i=0;i<1000;i++) gp.msgListAdapter.remove(0);
			}
			gp.msgListAdapter.add(mli);
			if (!gp.freezeMessageViewScroll && gp.activityIsForeground) {
				gp.msgListView.setSelection(gp.msgListView.getCount()-1);
			}
//			gp.msgListAdapter.notifyDataSetChanged();
		}
	};

	final public void addMsgToMsglistAdapter(MsgListItem mli) {
		addMsgToMsglistAdapter(glblParms,mli);
	};

	private String mLogId="Util       ";
	final public void setLogIdentifier(String lid) {
		mLogId=(lid+"                        ").substring(0,13);
	};
	
	final static private String buildLogCatString(
			StringBuilder sb,
			String cat, String lid, String prof, String fp, String msg) {
		sb.setLength(0);
		if (!cat.equals("")) {
			sb.append(cat).append(" ");
		}
		sb.append(lid);
		if (!prof.equals("")) {
			sb.append(prof).append(" ");
		}
		if (!fp.equals("")) {
			sb.append(fp).append(" ");
		}
		sb.append(msg);
		return sb.toString();
	};

	private StringBuilder mSbForaddDebugLogMsg1=new StringBuilder(256);
	final public void addDebugLogMsg(
			int lvl, String log_cat, String syncProfName, String...log_msg) {
		if (glblParms.debugLevel>=lvl) {
			mSbForaddDebugLogMsg1.setLength(0);
			for (int i=0;i<log_msg.length;i++) mSbForaddDebugLogMsg1.append(log_msg[i]);
			if (glblParms.logWriter!=null || glblParms.settingDebugMsgDisplay) {
				if (glblParms.settingDebugMsgDisplay) {
//					// flag=1 both, arg2=0 dialog only, arg2=2 msgview only
					String dt=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
					sendMsgToActivity(log_cat,"2",syncProfName,dt.substring(0,10),
							dt.substring(11),mLogId,"D","", 
							mSbForaddDebugLogMsg1.toString());
				}
				writeLogMsgToFile(glblParms.logWriter,"D "+log_cat,syncProfName, "", 
						mSbForaddDebugLogMsg1.toString());
			}			
			if (DEBUG_ENABLE) 
				Log.v(DEBUG_TAG,
					buildLogCatString(mSbForaddDebugLogMsg1,log_cat,mLogId,syncProfName,"",
							mSbForaddDebugLogMsg1.toString()));
		}
	};

	private StringBuilder mSbForaddDebugLogMsg2=new StringBuilder(256);
	final public void addDebugLogMsg(
			int lvl, String cat, String logmsg) {
		if (glblParms.debugLevel>=lvl ) {
//		    Calendar cd = Calendar.getInstance();
			if (glblParms.settingDebugMsgDisplay) {
				addMsgToMsglistAdapter(glblParms,
				    		 new MsgListItem(cat,sdfDate.format(System.currentTimeMillis()),
								sdfTime.format(System.currentTimeMillis()),mLogId,logmsg));
			}
			if (glblParms.logWriter!=null) {
				synchronized(glblParms.logWriter) {
					String dt=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
					mSbForaddDebugLogMsg2.setLength(0);
					mSbForaddDebugLogMsg2.append("D ")
						.append(cat)
						.append(" ")
						.append(dt.substring(0,10))
						.append(" ")
						.append(dt.substring(11))
						.append(" ")
						.append(mLogId)
						.append(logmsg);
					glblParms.logWriter.println(mSbForaddDebugLogMsg2);
//					glblParms.logWriter.flush();
				}
			}
			if (DEBUG_ENABLE) Log.v(DEBUG_TAG,cat+" "+mLogId+logmsg);
		}
	};

	public void flushLogFile() {
		if (glblParms.logWriter!=null) 
			synchronized(glblParms.logWriter) {
				glblParms.logWriter.flush();
			}
	};
	
	@SuppressLint("SdCardPath")
	public void openLogFile() {
		if (DEBUG_ENABLE) addDebugLogMsg(2,"I","open log file entered. esm="+glblParms.externalStorageIsMounted);
		if (glblParms.settingLogOption.equals("0") || glblParms.logWriter!=null ||
				!glblParms.externalStorageIsMounted) {
			if (glblParms.externalStorageIsMounted) manageLogFileGeneration();
			return;
		}
		manageLogFileGeneration();

		String t_fd="",fp="";
		t_fd=glblParms.settingLogMsgDir;
		if (t_fd.lastIndexOf("/")==(t_fd.length()-1)) {//last is "/"
			fp=t_fd+glblParms.settingLogMsgFilename;
		} else fp=t_fd+"/"+glblParms.settingLogMsgFilename;
//		Log.v("","fd="+t_fd+", fp="+fp);
		File lf=new File(t_fd);
		if(!lf.exists()) lf.mkdirs();
		
		try {
			BufferedWriter bw;
			FileWriter fw ;
			fw=new FileWriter(fp,true);
			bw = new BufferedWriter(fw,4096*32);
			glblParms.logWriter = new PrintWriter(bw);
			glblParms.currentLogFilePath=fp;
			if (glblParms.debugLevel>=2) {
				Calendar cd = Calendar.getInstance();
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String logmsg="D "+"I "+df.format(cd.getTime()) + " LOGT      " + "Log file opened.";
				glblParms.logWriter.println(logmsg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	private void manageLogFileGeneration() {
		ArrayList<LogFileManagemntListItem>lfm_list=createLogFileList(glblParms);
//		Log.v("","setting="+glblParms.settiingLogGeneration+", listsize="+lfm_list.size());
		Collections.sort(lfm_list,new Comparator<LogFileManagemntListItem>(){
			@Override
			public int compare(LogFileManagemntListItem arg0,
					LogFileManagemntListItem arg1) {
				int result=0;
				long comp=arg0.log_file_last_modified-arg1.log_file_last_modified;
				if (comp==0) result=0;
				else if(comp<0) result=-1;
				else if(comp>0) result=1;
				return result;
			}
		});
		ArrayList<LogFileManagemntListItem>lfm_del_list= new ArrayList<LogFileManagemntListItem>();
		for (LogFileManagemntListItem lfmli:lfm_list) {
			if (lfmli.log_file_generation>=glblParms.settiingLogGeneration) {
				addDebugLogMsg(1,"I","Log file was deleted, name="+lfmli.log_file_path);
				File lf=new File(lfm_list.get(0).log_file_path);
				lf.delete();
				lfm_del_list.add(lfmli);
			}
		}
		for (LogFileManagemntListItem del_lfmli:lfm_del_list) {
			lfm_list.remove(del_lfmli);
		}
	};
	
	public void closeLogFile() {
		if (DEBUG_ENABLE) addDebugLogMsg(2,"I","close log file entered. esm="+glblParms.externalStorageIsMounted);
		if (glblParms.logWriter!=null && glblParms.externalStorageIsMounted) {
			synchronized(glblParms.logWriter) {
				if (glblParms.debugLevel>=2) {
					Calendar cd = Calendar.getInstance();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String logmsg="D "+"I "+df.format(cd.getTime()) + " LOGT      " + "Log file closed.";
					glblParms.logWriter.println(logmsg);
				}
				glblParms.logWriter.close();
				glblParms.logWriter=null;
				glblParms.currentLogFilePath="";
			}
		}
	};
	
	public boolean deleteLogFile() {
		boolean result=false;
		if (DEBUG_ENABLE) 
			addDebugLogMsg(2,"I","delete log file entered. esm="+glblParms.externalStorageIsMounted);
		if (glblParms.logWriter==null && glblParms.externalStorageIsMounted) {
			File lf = new File(glblParms.settingLogMsgDir+glblParms.settingLogMsgFilename);
			result=lf.delete();
		} else result=false;
//		Log.v("","file="+glblParms.settingLogMsgDir+glblParms.settingLogMsgFilename+", result="+result);
		return result;
	};

    static public ArrayList<LogFileManagemntListItem> createLogFileList(GlobalParameters glblParms) {
    	ArrayList<LogFileManagemntListItem> lfm_fl=new ArrayList<LogFileManagemntListItem>();
    	
    	File lf=new File(glblParms.settingLogMsgDir);
    	File[] file_list=lf.listFiles();
    	if (file_list!=null) {
    		for (int i=0;i<file_list.length;i++) {
    			if (file_list[i].getName().startsWith("SMBSync_log")) {
    				if (file_list[i].getName().startsWith("SMBSync_log_20")) {
        		    	LogFileManagemntListItem t=new LogFileManagemntListItem();
        		    	t.log_file_name=file_list[i].getName();
        		    	t.log_file_path=file_list[i].getPath();
        		    	t.log_file_size=MiscUtil.convertFileSize(file_list[i].length());
        		    	t.log_file_last_modified=file_list[i].lastModified();
        		    	String lm_date=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(file_list[i].lastModified());
        		    	if (file_list[i].getPath().equals(glblParms.currentLogFilePath))
        		    		t.isCurrentLogFile=true;
        		    	t.log_file_last_modified_date=lm_date.substring(0,10);
        		    	t.log_file_last_modified_time=lm_date.substring(11);
        		    	lfm_fl.add(t);
//    				} else if (file_list[i].getName().equals("SMBSync_log.txt")){
//        		    	LogFileManagemntListItem t=new LogFileManagemntListItem();
//        		    	t.log_file_name=file_list[i].getName();
//        		    	t.log_file_path=file_list[i].getPath();
//        		    	t.log_file_size=convertFileSize(file_list[i].length());
//        		    	t.log_file_last_modified=file_list[i].lastModified();
//        		    	if (file_list[i].getPath().equals(glblParms.currentLogFilePath))
//        		    		t.isCurrentLogFile=true;
//        		    	String lm_date=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(file_list[i].lastModified());
//        		    	t.log_file_last_modified_date=lm_date.substring(0,10);
//        		    	t.log_file_last_modified_time=lm_date.substring(11);
//        		    	lfm_fl.add(t);
    				}
    			}
    		}
    		Collections.sort(lfm_fl,new Comparator<LogFileManagemntListItem>(){
				@Override
				public int compare(LogFileManagemntListItem arg0,
						LogFileManagemntListItem arg1) {
					int result=0;
					long comp=arg1.log_file_last_modified-arg0.log_file_last_modified;
					if (comp==0) result=0;
					else if(comp<0) result=-1;
					else if(comp>0) result=1;
					return result;
				}
    			
    		});
    	}
    	if (lfm_fl.size()==0) {
    		LogFileManagemntListItem t=new LogFileManagemntListItem();
    		lfm_fl.add(t);
    	} else {
    		String c_lfm_date="";
    		int gen=-1;
    		for (LogFileManagemntListItem lfmli:lfm_fl) {
    			String n_lfm_date="";
    			String lfm_date_time=lfmli.log_file_name
    					.replace("SMBSync_log_", "")
    					.replace(".txt","");
    			if (lfm_date_time.indexOf("_")>=0) {
    				n_lfm_date=lfm_date_time
    						.substring(0,lfm_date_time.indexOf("_"));
    			} else {
    				n_lfm_date=lfm_date_time;
    			}
    			if (!c_lfm_date.equals(n_lfm_date)) {
    				gen++;
    				c_lfm_date=n_lfm_date;
    			} 
    			lfmli.log_file_generation=gen;
    		}
    	}
    	return lfm_fl;
    };
    
	@SuppressLint("SdCardPath")
	public ArrayList<SyncHistoryListItem> loadHistoryList() {
//		Log.v("","load hist started");
		ArrayList<SyncHistoryListItem> hl=new ArrayList<SyncHistoryListItem>();
		try {
			String dir=glblParms.SMBSync_External_Root_Dir+"/SMBSync";
			File lf=new File(dir+"/history.txt");
			if (lf.exists()) {
				FileReader fw=new FileReader(lf);
				BufferedReader br=new BufferedReader(fw,4096*16);
				String line="";
				String[] l_array=null;
			    while ((line = br.readLine()) != null) {
			    	l_array=line.split("\u0001");
			    	if (l_array!=null && l_array.length>=11) {
			    		SyncHistoryListItem hli=new SyncHistoryListItem();
			    		try {
				    		hli.sync_date=l_array[0];
				    		hli.sync_time=l_array[1];
				    		hli.sync_prof=l_array[2];
				    		hli.sync_status=Integer.valueOf(l_array[3]);
				    		hli.sync_result_no_of_copied=Integer.valueOf(l_array[4]);
				    		hli.sync_result_no_of_deleted=Integer.valueOf(l_array[5]);
				    		hli.sync_result_no_of_ignored=Integer.valueOf(l_array[6]);
				    		hli.sync_error_text=l_array[7];
				    		hli.sync_copied_file=string2Array(l_array[8]);
				    		hli.sync_deleted_file=string2Array(l_array[9]);
				    		hli.sync_ignored_file=string2Array(l_array[10]);
				    		if (l_array.length>=12) {
				    			hli.sync_log_file_path=l_array[11];
					    		if (!hli.sync_log_file_path.equals("")) {
									File tf=new File(hli.sync_log_file_path);
									if (tf.exists()) hli.isLogFileAvailable=true;
					    		}
				    		}
				    		hl.add(hli);
			    		} catch(Exception e) {
			    			addLogMsg("W","History list can not loaded");
			    			e.printStackTrace();
			    		}
			    	} 
				}
				br.close();
				if (hl.size()>1) {
					Collections.sort(hl,new Comparator<SyncHistoryListItem>(){
						@Override
						public int compare(SyncHistoryListItem lhs, SyncHistoryListItem rhs) {
							if (rhs.sync_date.equals(lhs.sync_date)) {
								if (rhs.sync_time.equals(lhs.sync_time)) {
									return lhs.sync_prof.compareToIgnoreCase(rhs.sync_prof);
								} else return rhs.sync_time.compareTo(lhs.sync_time) ;
							} else return rhs.sync_date.compareTo(lhs.sync_date) ;
						}
					});
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (hl.size()==0) {
			SyncHistoryListItem hli=new SyncHistoryListItem();
			hli.sync_prof="No history";
			hl.add(hli);
		}
//		Log.v("","load hist ended");
		return hl;
	};

	@SuppressLint("SdCardPath")
	final public void saveHistoryList(ArrayList<SyncHistoryListItem> hl) {
//		Log.v("","save hist started");
		try {
			String dir=glblParms.SMBSync_External_Root_Dir+"/SMBSync";
			File lf=new File(dir);
			lf.mkdirs();
			lf=new File(dir+"/history.txt");
			FileWriter fw=new FileWriter(lf);
			BufferedWriter bw=new BufferedWriter(fw,4096*16);
			int max=500;
			StringBuilder sb_buf=new StringBuilder(1024*512);
			SyncHistoryListItem shli=null;
			String cpy_str, del_str, ign_str;
			for (int i=0;i<hl.size();i++) {
				if (!hl.get(i).sync_prof.equals("No history")) {
					if (i<max) {
						shli=hl.get(i);
						cpy_str=array2String(sb_buf,shli.sync_copied_file);
						del_str=array2String(sb_buf,shli.sync_deleted_file);
						ign_str=array2String(sb_buf,shli.sync_ignored_file);
						sb_buf.setLength(0);
						sb_buf.append(shli.sync_date).append("\u0001")
							.append(shli.sync_time).append("\u0001")
							.append(shli.sync_prof).append("\u0001")
							.append(shli.sync_status).append("\u0001")
							.append(shli.sync_result_no_of_copied).append("\u0001")
							.append(shli.sync_result_no_of_deleted).append("\u0001")
							.append(shli.sync_result_no_of_ignored).append("\u0001")
							.append(shli.sync_error_text).append("\u0001")
							.append(cpy_str).append("\u0001")
							.append(del_str).append("\u0001")
							.append(ign_str).append("\u0001") 
							.append(shli.sync_log_file_path)
							.append("\n");
								
						bw.append(sb_buf.toString());
					}
				}
			}
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		Log.v("","save hist ended");
	};
	
	final static private String array2String(StringBuilder sb_buf,String[] sa) {
		sb_buf.setLength(0);
		if (sa!=null) {
			sb_buf
				.append(Integer.toString(sa.length))
				.append("\u0002");
			for (int i=0;i<sa.length;i++) {
				sb_buf.append("\u0003")
					.append(sa[i])
					.append("\u0002");
			}
		} else {
			sb_buf.append(Integer.toString(0));
		}
		return sb_buf.toString();
	};

	final static private String[] string2Array(String str) {
		String[]t_array=str.split("\u0002");
		String[] result=null;
		if (!t_array[0].equals("0")) {
			result=new String[Integer.parseInt(t_array[0])];
			for (int i=0;i<result.length;i++) {
				result[i]=t_array[i+1].replace("\u0003", "");
			}
		} 
		return result;
	};
	
	public void addHistoryList(ArrayList<SyncHistoryListItem> hl, SyncHistoryListItem item) {
		hl.add(0,item);
	};
	public void removeHistoryList(ArrayList<SyncHistoryListItem> hl, int pos) {
		hl.remove(pos);
	};
	public void removeHistoryList(ArrayList<SyncHistoryListItem> hl, SyncHistoryListItem item) {
		hl.remove(item);
	};
	
}
