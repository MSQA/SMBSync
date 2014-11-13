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

import static com.sentaroh.android.SMBSync.Constants.*;
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
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.CheckedTextView;

import com.sentaroh.android.Utilities.DateUtil;
import com.sentaroh.android.Utilities.MiscUtil;
import com.sentaroh.android.Utilities.NetworkUtil;

@SuppressLint("SimpleDateFormat")
public class SMBSyncUtil {
	
	private SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd");
	private SimpleDateFormat sdfTime = new SimpleDateFormat("HH:mm:ss");
	
	private GlobalParameters mGp=null;
	
	private Context mContext=null;
	
	public SMBSyncUtil (Context c, String lid, GlobalParameters gp) {
		mContext=c;
		mGp=gp;
		setLogIdentifier(lid);
	};
	
	public boolean setActivityIsForeground(boolean d) {
		mGp.activityIsForeground=d;
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
	
	public boolean isActivityForeground() {return mGp.activityIsForeground;};
	
	public boolean isRemoteDisable() {
		boolean ret=false;
		boolean ws=isWifiActive();
		if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF)) {
			ret=false;
		} else {
			if (ws) ret=false;
			else ret=true;
		}
		
		addDebugLogMsg(2,"I","isRemoteDisable settingWifiOption="+mGp.settingWifiOption+
				", WifiConnected="+ws+", result="+ret);
		
		return ret;
	};
	
//	@SuppressLint("NewApi")
	public void initAppSpecificExternalDirectory(Context c) {
//		if (Build.VERSION.SDK_INT>=19) {
//			c.getExternalFilesDirs(null);
//		} else {
//		}
		ContextCompat.getExternalFilesDirs(c, null);
	};
	
	public boolean isWifiActive() { 
		boolean ret=false;
		WifiManager mWifi = 
				(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
//		if (mWifi.getConnectionInfo().getSupplicantState()==
//				SupplicantState.COMPLETED)
		String ssid="";
		if (mWifi.isWifiEnabled()) {
			if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_ON)) {
				ret=true;
			} else if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP)) {
				ssid=mWifi.getConnectionInfo().getSSID();
				if (ssid!=null && 
						!ssid.equals("0x") &&
						!ssid.equals("")) ret=true;
//				Log.v("","ssid="+ssid);
			} else if (mGp.settingWifiOption.equals(SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_SPEC_AP)) {
				ret=true;
			}
		}
		addDebugLogMsg(2,"I","isWifiActive WifiEnabled="+mWifi.isWifiEnabled()+
				", settingWifiOption="+mGp.settingWifiOption+
				", SSID="+ssid+", result="+ret);
		return ret;
	};
	
	public static boolean isSmbHostAddressConnected(String addr) {
		boolean result=false;
		if (NetworkUtil.isIpAddressAndPortConnected(addr,139,3500) || 
				NetworkUtil.isIpAddressAndPortConnected(addr,445,3500)) result=true;
		return result;
	};
	
	public static boolean isSmbHostAddressConnected(String addr, int port) {
		boolean result=false;
		result=NetworkUtil.isIpAddressAndPortConnected(addr,port,3500);
//		Log.v("","addr="+addr+", port="+port+", result="+result);
		return result;
	};

	public static String getLocalIpAddress() {
		String result="";
		boolean exit=false;
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
//	            	Log.v("","ip="+inetAddress.getHostAddress()+
//	            			", name="+intf.getName());
	            	if (inetAddress.isSiteLocalAddress()) {
	                    result=inetAddress.getHostAddress();
//	                    Log.v("","result="+result+", name="+intf.getName()+"-");
	                    if (intf.getName().equals("wlan0")) {
	                    	exit=true;
	                    	break;
	                    }
	            	}
	            }
	            if (exit) break;
	        }
	    } catch (SocketException ex) {
	        Log.e(APPLICATION_TAG, ex.toString());
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
	        Log.e(APPLICATION_TAG, ex.toString());
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
			writeLogMsgToFile(log_cat,syncProfName, fp,log_msg);
		if (mGp.debugLevel>0) { 
			if (mGp.debugLevel>0 && log) 
				Log.v(APPLICATION_TAG,
					buildLogCatString(mSbForaddMsgToProgDlg,
							log_cat,mLogId,syncProfName,fp,log_msg));
		}
	};

	private StringBuilder mSbForWriteLog = new StringBuilder(256);
	final private String writeLogMsgToFile(String cat, String prof, 
			String fp, String msg) {
		String result=null;
		if (mGp.logWriter!=null) { 
				result=formatLogMsg(cat,prof,fp,msg);
				try {
					writeLog(mGp,result);
				} catch(Exception e) {
					e.printStackTrace();
				}
//			if (logFileFlushCnt>1000) {
//				flushLogFile();
//				logFileFlushCnt=0;
//			} else logFileFlushCnt++;
		}
		return result;
	};

	final public String formatLogMsg(String cat, String prof, 
			String fp, String msg) {
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
		return mSbForWriteLog.toString();
	};
	
	private StringBuilder mSbForsendMsgToActivity=new StringBuilder(256);
	final public void sendMsgToActivity(final String log_cat, final String msgflag, final String sync_prof,
			final String date, final String time, final String tag, final String debug_flag, final String fp, final String msg_text) {
		mGp.activityUiHandler.post(new Runnable(){
			@Override
			public void run() {
				if (msgflag.equals("0")) {
					mGp.progressSpinSyncprof.setText(sync_prof);
					mGp.progressSpinFilePath.setText(fp);
					mGp.progressSpinStatus.setText(msg_text);
					NotificationUtil.showOngoingMsg(mGp,sync_prof,fp,msg_text);
				} else { //
					if (msgflag.equals("1")) {
						mGp.progressSpinSyncprof.setText(sync_prof);
						mGp.progressSpinFilePath.setText(fp);
						mGp.progressSpinStatus.setText(msg_text);
						NotificationUtil.showOngoingMsg(mGp,sync_prof,fp,msg_text);
					}  
					if (debug_flag.equals("M") || 
							(debug_flag.equals("D")&&mGp.settingDebugMsgDisplay)) {
						mSbForsendMsgToActivity.setLength(0);
						if (!sync_prof.equals("")) mSbForsendMsgToActivity.append(sync_prof).append(" ");
						if (!fp.equals("")) mSbForsendMsgToActivity.append(fp).append(" ");
						mSbForsendMsgToActivity.append(msg_text);
						addMsgToMsglistAdapter(mGp,
								new MsgListItem(log_cat,date,time,tag,
										mSbForsendMsgToActivity.toString()));
					}
				}
			}
		});
	};

	private StringBuilder mSbForAddLogMsg=new StringBuilder(256);
	final public String addLogMsg(String log_cat, String sync_prof, String fp, String log_msg) {
		String dt=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		// flag=1 both, arg2=0 dialog only, arg2=2 msgview only
		sendMsgToActivity(log_cat,"2",sync_prof,dt.substring(0,10), dt.substring(11),
				mLogId,"M",fp,log_msg);
		String wmsg=writeLogMsgToFile("M "+log_cat, sync_prof, fp, log_msg);
		if (mGp.debugLevel>0)
			Log.v(APPLICATION_TAG,
					buildLogCatString(mSbForAddLogMsg, log_cat,mLogId,sync_prof,fp,log_msg));
		return wmsg;
	};

	final public void addLogMsg(String cat, String logmsg) {
		addMsgToMsglistAdapter(mGp,
			  		 new MsgListItem(cat,
			  				 sdfDate.format(System.currentTimeMillis()),
			  				 sdfTime.format(System.currentTimeMillis()),
			  				 "MAIN",logmsg));
		if (mGp.logWriter!=null) {
				writeLog(mGp, "M "+cat+" "+
					sdfDate.format(System.currentTimeMillis())+" "+
					sdfTime.format(System.currentTimeMillis())+" "+
					("MAIN"+"          ").substring(0,13)+logmsg);
//				glblParms.logWriter.flush();
		}
		if (mGp.debugLevel>0) Log.v(APPLICATION_TAG,cat+" "+mLogId+logmsg);
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
		addMsgToMsglistAdapter(mGp,mli);
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
		if (mGp.debugLevel>=lvl) {
			mSbForaddDebugLogMsg1.setLength(0);
			for (int i=0;i<log_msg.length;i++) mSbForaddDebugLogMsg1.append(log_msg[i]);
			if (mGp.logWriter!=null || mGp.settingDebugMsgDisplay) {
				if (mGp.settingDebugMsgDisplay) {
//					// flag=1 both, arg2=0 dialog only, arg2=2 msg view only
					String dt=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
					sendMsgToActivity(log_cat,"2",syncProfName,dt.substring(0,10),
							dt.substring(11),mLogId,"D","", 
							mSbForaddDebugLogMsg1.toString());
				}
				writeLogMsgToFile("D "+log_cat,syncProfName, "", 
						mSbForaddDebugLogMsg1.toString());
			}			
			
				Log.v(APPLICATION_TAG,
					buildLogCatString(mSbForaddDebugLogMsg1,log_cat,mLogId,syncProfName,"",
							mSbForaddDebugLogMsg1.toString()));
		}
	};

	private StringBuilder mSbForaddDebugLogMsg2=new StringBuilder(256);
	final public void addDebugLogMsg(
			int lvl, String cat, String logmsg) {
		if (mGp.debugLevel>=lvl ) {
			if (mGp.settingDebugMsgDisplay) {
				addMsgToMsglistAdapter(mGp,
				    		 new MsgListItem(cat,sdfDate.format(System.currentTimeMillis()),
								sdfTime.format(System.currentTimeMillis()),mLogId,logmsg));
			}
			if (mGp.logWriter!=null) {
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
					writeLog(mGp,mSbForaddDebugLogMsg2.toString());
			}
			Log.v(APPLICATION_TAG,cat+" "+mLogId+logmsg);
		}
	};

	public void flushLogFile() {
		if (mGp.logWriter!=null) 
			synchronized(mGp.logWriter) {
				mGp.logWriter.flush();
			}
	};
	
	static public void writeLog(GlobalParameters gp, String msg) {
		if (gp.logWriter!=null) 
			synchronized(gp.logWriter) {
				gp.logWriter.println(msg);
				gp.logLineCount++;
				if (gp.logLineCount>100) {
					gp.logWriter.flush();
					gp.logLineCount=0;
				}
			}
	};
	
	final public void rotateLogFile() {
//		if (!mGp.settingLogOption.equals("0")) {
//			flushLogFile();
//			closeLogFile();
//			openLogFile();
//			addLogMsg("I", mContext.getString(R.string.msgs_log_management_log_file_switched));
//		}
	};

	@SuppressLint("SdCardPath")
	public void openLogFile() {
		addDebugLogMsg(2,"I","open log file entered. esm="+mGp.externalStorageIsMounted);
//		Log.v("","lo="+mGp.settingLogOption+", lw="+mGp.logWriter);
		if (!mGp.settingLogOption.equals("0") && mGp.logWriter==null) {
			SimpleDateFormat df=null;
			df = new SimpleDateFormat("yyyy-MM-dd");
			mGp.settingLogMsgFilename="SMBSync_log_"+df.format(System.currentTimeMillis())+".txt";
		}
		
		if (mGp.settingLogOption.equals("0") || mGp.logWriter!=null ||
				!mGp.externalStorageIsMounted) {
			if (mGp.externalStorageIsMounted) manageLogFileGeneration();
			return;
		}
		manageLogFileGeneration();

		String t_fd="",fp="";
		t_fd=mGp.settingLogMsgDir;
		if (t_fd.lastIndexOf("/")==(t_fd.length()-1)) {//last is "/"
			fp=t_fd+mGp.settingLogMsgFilename;
		} else fp=t_fd+"/"+mGp.settingLogMsgFilename;
		File lf=new File(t_fd);
		if(!lf.exists()) lf.mkdirs();
//		Log.v("","lfn="+fp);
		try {
			BufferedWriter bw;
			FileWriter fw ;
			fw=new FileWriter(fp,true);
			bw = new BufferedWriter(fw,4096*32);
			mGp.logWriter = new PrintWriter(bw,false);
			mGp.currentLogFilePath=fp;
			if (mGp.debugLevel>=2) {
				Calendar cd = Calendar.getInstance();
				SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
				String logmsg="D "+"I "+df.format(cd.getTime()) + " LOGT      " + "Log file opened.";
				mGp.logWriter.println(logmsg);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	};

	private void manageLogFileGeneration() {
		ArrayList<LogFileManagemntListItem>lfm_list=createLogFileList(mGp);
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
			if (lfmli.log_file_generation>=mGp.settiingLogGeneration) {
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
		addDebugLogMsg(2,"I","close log file entered. esm="+mGp.externalStorageIsMounted);
		if (mGp.logWriter!=null && mGp.externalStorageIsMounted) {
			synchronized(mGp.logWriter) {
				if (mGp.debugLevel>=2) {
					Calendar cd = Calendar.getInstance();
					SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
					String logmsg="D "+"I "+df.format(cd.getTime()) + " LOGT      " + "Log file closed.";
					mGp.logWriter.println(logmsg);
				}
				mGp.logWriter.close();
				mGp.logWriter=null;
				mGp.currentLogFilePath="";
			}
		}
	};
	
	public boolean deleteLogFile() {
		boolean result=false;
		addDebugLogMsg(2,"I","delete log file entered. esm="+mGp.externalStorageIsMounted);
		if (mGp.logWriter==null && mGp.externalStorageIsMounted) {
			File lf = new File(mGp.settingLogMsgDir+mGp.settingLogMsgFilename);
			result=lf.delete();
		} else result=false;
		return result;
	};
	
	static public void setCheckedTextView(final CheckedTextView ctv) {
		ctv.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				ctv.toggle();
			}
		});
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
//					int result=0;
//					long comp=arg1.log_file_last_modified-arg0.log_file_last_modified;
//					if (comp==0) result=0;
//					else if(comp<0) result=-1;
//					else if(comp>0) result=1;
//					return result;
					return arg1.log_file_name.compareToIgnoreCase(arg0.log_file_name);
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
//    			Log.v("","name="+lfmli.log_file_name+", gen="+lfmli.log_file_generation);
//    			Log.v("","lfm_date_time="+lfm_date_time+", lfm_date="+n_lfm_date);
    		}
    	}
    	return lfm_fl;
    };
    
	@SuppressLint("SdCardPath")
	public ArrayList<SyncHistoryListItem> loadHistoryList() {
//		Log.v("","load hist started");
		ArrayList<SyncHistoryListItem> hl=new ArrayList<SyncHistoryListItem>();
		try {
			String dir=mGp.SMBSync_External_Root_Dir+"/SMBSync";
			File lf=new File(dir+"/history.txt");
			if (lf.exists()) {
				FileReader fw=new FileReader(lf);
				BufferedReader br=new BufferedReader(fw,4096*16);
				String line="";
				String[] l_array=null;
			    while ((line = br.readLine()) != null) {
			    	l_array=line.split("\u0001");
//			    	Log.v("","line="+line);
//			    	Log.v("","em="+l_array[7]);
			    	if (l_array!=null && l_array.length>=11 && !l_array[2].equals("")) {
			    		SyncHistoryListItem hli=new SyncHistoryListItem();
			    		try {
				    		hli.sync_date=l_array[0];
				    		hli.sync_time=l_array[1];
				    		hli.sync_prof=l_array[2];
				    		hli.sync_status=Integer.valueOf(l_array[3]);
				    		hli.sync_result_no_of_copied=Integer.valueOf(l_array[4]);
				    		hli.sync_result_no_of_deleted=Integer.valueOf(l_array[5]);
				    		hli.sync_result_no_of_ignored=Integer.valueOf(l_array[6]);
				    		hli.sync_error_text=l_array[7].replaceAll("\u0002", "\n");
				    		if (!l_array[8].equals(" ")) hli.sync_result_no_of_retry=Integer.valueOf(l_array[8]);
//				    		hli.sync_deleted_file=string2Array(l_array[9]);
//				    		hli.sync_ignored_file=string2Array(l_array[10]);
				    		if (l_array.length>=12) {
				    			hli.sync_log_file_path=l_array[11];
					    		if (!hli.sync_log_file_path.equals("")) {
									File tf=new File(hli.sync_log_file_path);
									if (tf.exists()) hli.isLogFileAvailable=true;
					    		}
					    		if (l_array.length>=13) {
					    			hli.sync_result_file_path=l_array[12];
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
			hli.sync_prof="";
			hl.add(hli);
		}
//		Log.v("","load hist ended");
		return hl;
	};
	
	public void housekeepHistoryList() {
		Thread th=new Thread() {
			@Override
			public void run() {
				String dir=mGp.SMBSync_External_Root_Dir+"/SMBSync/result_log";
				File lf=new File(dir);
				if (lf.exists()) {
					File[] fl=lf.listFiles();
					if (fl!=null && fl.length>0) {
						for(int i=0;i<fl.length;i++) {
							String fp=fl[i].getPath();
							boolean found=false;
							for (int j=0;j<mGp.syncHistoryList.size();j++) {
								if (mGp.syncHistoryList.get(j).sync_result_file_path.equals(fp)) {
									found=true;
									break;
								}
							}
							if (!found) {
								File tlf=new File(fp);
								tlf.delete();
								addDebugLogMsg(1,"I","Sync history result log was delete because file name not registerd to SyncHistoryList, fp="+fp);							}
						}
					}
				}
			}
		};
		th.start();
	};

	final public String createSyncResultFilePath(String syncProfName) {
		String dir=mGp.SMBSync_External_Root_Dir+"/SMBSync/result_log";
		File tlf=new File(dir);
		if (!tlf.exists()) tlf.mkdirs();
		String dt=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		String fn="result_"+syncProfName+"_"+dt+".txt";
		String fp=dir+"/"+fn.replaceAll("/", "-").replaceAll(":", "").replaceAll(" ","_");
		return fp;
	};
	
	@SuppressLint("SdCardPath")
	final public void saveHistoryList(ArrayList<SyncHistoryListItem> hl) {
//		Log.v("","save hist started");
		try {
			String dir=mGp.SMBSync_External_Root_Dir+"/SMBSync";
			File lf=new File(dir);
			lf.mkdirs();
			lf=new File(dir+"/history.txt");
			FileWriter fw=new FileWriter(lf);
			BufferedWriter bw=new BufferedWriter(fw,4096*16);
			int max=500;
			StringBuilder sb_buf=new StringBuilder(1024*512);
			SyncHistoryListItem shli=null;
//			String cpy_str, del_str, ign_str;
			ArrayList<SyncHistoryListItem>del_list=new ArrayList<SyncHistoryListItem>();
			for (int i=0;i<hl.size();i++) {
//				Log.v("","i="+i+", n="+hl.get(i).sync_prof);
				if (!hl.get(i).sync_prof.equals("")) {
					shli=hl.get(i);
					if (i<max) {
//						cpy_str=array2String(sb_buf,shli.sync_copied_file);
//						del_str=array2String(sb_buf,shli.sync_deleted_file);
//						ign_str=array2String(sb_buf,shli.sync_ignored_file);
						String lfp="";
						if (shli.isLogFileAvailable) lfp=shli.sync_log_file_path;
						sb_buf.setLength(0);
						sb_buf.append(shli.sync_date).append("\u0001")
							.append(shli.sync_time).append("\u0001")
							.append(shli.sync_prof).append("\u0001")
							.append(shli.sync_status).append("\u0001")
							.append(shli.sync_result_no_of_copied).append("\u0001")
							.append(shli.sync_result_no_of_deleted).append("\u0001")
							.append(shli.sync_result_no_of_ignored).append("\u0001")
							.append(shli.sync_error_text.replaceAll("\n", "\u0002")).append("\u0001")
							.append(shli.sync_result_no_of_retry).append("\u0001") //retry count
							.append(" ").append("\u0001") //Dummy
							.append(" ").append("\u0001") //Dummy 
							.append(lfp).append("\u0001")
							.append(shli.sync_result_file_path)
							.append("\n");
								
						bw.append(sb_buf.toString());
					} else {
						del_list.add(shli);
						if (!shli.sync_result_file_path.equals("")) {
							File tlf=new File(shli.sync_result_file_path);
							if (tlf.exists()) tlf.delete();
						}
					}
				}
			}
			
			for (int i=0;i<del_list.size();i++) hl.remove(del_list.get(i));
			
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
//		Log.v("","save hist ended");
	};
	
//	final static private String array2String(StringBuilder sb_buf,String[] sa) {
//		sb_buf.setLength(0);
//		if (sa!=null) {
//			sb_buf
//				.append(Integer.toString(sa.length))
//				.append("\u0002");
//			for (int i=0;i<sa.length;i++) {
//				sb_buf.append("\u0003")
//					.append(sa[i])
//					.append("\u0002");
//			}
//		} else {
//			sb_buf.append(Integer.toString(0));
//		}
//		return sb_buf.toString();
//	};
//
//	final static private String[] string2Array(String str) {
//		String[]t_array=str.split("\u0002");
//		String[] result=null;
//		if (!t_array[0].equals("0")) {
//			result=new String[Integer.parseInt(t_array[0])];
//			for (int i=0;i<result.length;i++) {
//				result[i]=t_array[i+1].replace("\u0003", "");
//			}
//		} 
//		return result;
//	};
	
	public void addHistoryList(ArrayList<SyncHistoryListItem> hl, SyncHistoryListItem item) {
		if (hl.size()==1) {
			if (hl.get(0).sync_prof.equals("")) hl.remove(0);
		}
		hl.add(0,item);
	};
//	public void removeHistoryList(ArrayList<SyncHistoryListItem> hl, int pos) {
//		String result_fp=hl.get(pos).sync_result_file_path;
//		if (!result_fp.equals("")) {
//			File lf=new File(result_fp);
//			lf.delete();
//		}
//		hl.remove(pos);
//	};
//	public void removeHistoryList(ArrayList<SyncHistoryListItem> hl, SyncHistoryListItem item) {
//		String result_fp=item.sync_result_file_path;
//		if (!result_fp.equals("")) {
//			File lf=new File(result_fp);
//			lf.delete();
//		}
//		hl.remove(item);
//	};
	
}
