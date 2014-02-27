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

import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.util.ArrayList;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.sentaroh.android.Utilities.NotifyEventCompletion;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistItem;

public class ReadRemoteFilelist implements Runnable  {
	private ThreadCtrl getFLCtrl=null;
	
	private ArrayList<TreeFilelistItem> remoteFileList;
	private String remoteUrl, remoteDir;
	private NtlmPasswordAuthentication ntlmPasswordAuth;
	
	private NotifyEventCompletion notifyEvent;
	
	private boolean readDirOnly=false;
	private boolean readSubDirCnt=true;
	
	private SMBSyncUtil util=null;
	
	private Context mContext=null;
	
	public ReadRemoteFilelist(Context c, ThreadCtrl ac, String ru, String rd, 
			ArrayList<TreeFilelistItem> fl,String user, String pass, 
			NotifyEventCompletion ne,boolean dironly, boolean dc, GlobalParameters gp) {
		mContext=c;
		util=new SMBSyncUtil(mContext, "FileList", gp);
		remoteFileList=fl;
		remoteUrl=ru;
		remoteDir=rd;
		getFLCtrl=ac; //new ThreadCtrl();
		notifyEvent=ne;
		
		readDirOnly=dironly;
		readSubDirCnt=dc;
		
		if (DEBUG_ENABLE) {
			util.addDebugLogMsg(1,"I","getFileList constructed. user="+user+
					", url="+ru+", dir="+rd);
			util.addDebugLogMsg(9,"I","getFileList constructed. pass="+pass);
		}
		setJcifsProperties(user,pass);
	}
	
	@Override
	public void run() {
		defaultUEH = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler(unCaughtExceptionHandler);

		getFLCtrl.setThreadResultSuccess();
		getFLCtrl.setThreadMessage("");
		
		if (DEBUG_ENABLE) util.addDebugLogMsg(1,"I","getFileList started");
		
		remoteFileList.clear();
		try {		
			SmbFile remoteFile = new SmbFile(remoteUrl+remoteDir,ntlmPasswordAuth);
			SmbFile[] fl = remoteFile.listFiles();

			for (int i=0;i<fl.length;i++){
				if (getFLCtrl.isEnable()) {
					String fn=fl[i].getName();
					if (fn.endsWith("/")) fn=fn.substring(0,fn.length()-1);
					String fp=fl[i].getPath();
					if (fp.endsWith("/")) fp=fp.substring(0,fp.lastIndexOf("/"));
					fp=fp.substring(remoteUrl.length()+1,fp.length());
					if (fp.lastIndexOf("/")>0) {
						fp="/"+fp.substring(0,fp.lastIndexOf("/")+1);
					} else fp="/";
					int dirct=0;
					if (readSubDirCnt && fl[i].canRead() && 
							fl[i].isDirectory() && !fn.equals("IPC$") && 
							!fn.equals(".android_secure") &&
							!fn.equals("System Volume Information")) {
						SmbFile tdf=new SmbFile(fl[i].getPath(),ntlmPasswordAuth);
						SmbFile[] tfl=tdf.listFiles();
						
						if (readDirOnly) {
							for (int j=0;j<tfl.length;j++)
								if (tfl[j].isDirectory()) dirct++;
						} else {
							dirct=tfl.length;
						}
					}
					TreeFilelistItem fi=new TreeFilelistItem (
							fn,
							"",
							fl[i].isDirectory(),
							fl[i].length(),
							fl[i].lastModified(),
							false,
							fl[i].canRead(),
							fl[i].canWrite(),
							fl[i].isHidden(),
							fp,0);
					fi.setSubDirItemCount(dirct);
					if (readDirOnly) {
						if (fi.isDir()) remoteFileList.add(fi);
					} else remoteFileList.add(fi);
					if (DEBUG_ENABLE) 
						util.addDebugLogMsg(2,"I","filelist detail="+fn+",isDir=" +
							fl[i].isDirectory()+", canRead="+fl[i].canRead()+
							", canWrite="+fl[i].canWrite()+",fp="+fp+", dircnt="+dirct);
				} else {
					getFLCtrl.setThreadResultCancelled();
					if (DEBUG_ENABLE) util.addDebugLogMsg(1,"W","Cancelled by main task.");
					break;
				}
			}
			
		} catch (SmbException e) {
			e.printStackTrace();
			util.addDebugLogMsg(1,"E",e.toString());
			getFLCtrl.setThreadResultError();
			if (e.getRootCause()!=null) getFLCtrl.setThreadMessage(e.getRootCause()+" "+e.getMessage());
			else getFLCtrl.setThreadMessage(e.getMessage());
			getFLCtrl.setDisable();
		} catch (MalformedURLException e) {
			e.printStackTrace();
			util.addDebugLogMsg(1,"E",e.toString());
			getFLCtrl.setThreadResultError();
			getFLCtrl.setThreadMessage(e.getMessage());
			getFLCtrl.setDisable();
		}
			
		if (DEBUG_ENABLE) util.addDebugLogMsg(1,"I","getFileList ended.");
		getFLCtrl.setDisable();
		notifyEvent.notifyToListener(true, null);
	};
	
	 // Default uncaught exception handler variable
    private UncaughtExceptionHandler defaultUEH;
    
 // handler listener
    private Thread.UncaughtExceptionHandler unCaughtExceptionHandler =
        new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable ex) {
            	Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);
            	ex.printStackTrace();
            	StackTraceElement[] st=ex.getStackTrace();
            	String st_msg="";
            	for (int i=0;i<st.length;i++) {
            		st_msg+="\n at "+st[i].getClassName()+"."+
            				st[i].getMethodName()+"("+st[i].getFileName()+
            				":"+st[i].getLineNumber()+")";
            	}
            	getFLCtrl.setThreadResultError();
    			String end_msg=ex.toString()+st_msg;
    			getFLCtrl.setThreadMessage(end_msg);
    			getFLCtrl.setDisable();
    			notifyEvent.notifyToListener(true, null);
                // re-throw critical exception further to the os (important)
//                defaultUEH.uncaughtException(thread, ex);
            }
    };

	private void setJcifsProperties(String user, String pass) {

//		jcifs.Config.setProperty("jcifs.util.loglevel", "9");
//		System.setProperty("jcifs.util.loglevel", "5");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String settingsSmbLogLevel=
				prefs.getString(mContext.getString(R.string.settings_smb_log_level), "0");

		String settingsSmbLmCompatibility=
			prefs.getString(mContext.getString(R.string.settings_smb_lm_compatibility),"0");
		boolean ues=
			prefs.getBoolean(mContext.getString(R.string.settings_smb_use_extended_security),false);
		String settingsSmbUseExtendedSecurity="true";
		if (ues) settingsSmbUseExtendedSecurity="true";
		else settingsSmbUseExtendedSecurity="false";

		System.setProperty("jcifs.util.loglevel", settingsSmbLogLevel);
		System.setProperty("jcifs.smb.lmCompatibility", settingsSmbLmCompatibility);
		System.setProperty("jcifs.smb.client.useExtendedSecurity", settingsSmbUseExtendedSecurity);

//		System.setProperty("jcifs.smb.client.tcpNoDelay","false");
//		System.setProperty("jcifs.smb.client.rcv_buf_size", "16644");
//		System.setProperty("jcifs.smb.client.snd_buf_size", "16644");
//		jcifs.Config.registerSmbURLHandler();
		
//		Samba server(Android)
		String tuser=null,tpass=null;
		if (user.length()!=0) tuser=user;
		if (pass.length()!=0) tpass=pass;
		ntlmPasswordAuth = 
				new NtlmPasswordAuthentication(null, tuser, tpass);

	};
}