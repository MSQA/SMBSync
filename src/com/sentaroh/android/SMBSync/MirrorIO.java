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

import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_CONFIRM_FOR_COPY;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_CONFIRM_FOR_DELETE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_CONFIRM_RESP_NOALL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_CONFIRM_RESP_YESALL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_LOCAL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_REMOTE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_COPY;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MIRROR;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MOVE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_SYNC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.Thread.UncaughtExceptionHandler;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;
import android.os.Build;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import com.sentaroh.android.Utilities.DateUtil;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.ThreadCtrl;

public class MirrorIO implements Runnable {
	
	private int mirrorIoBufferSize =65536*2; //64K x 2
	private byte[] mirrorIoBuffer;

	private boolean syncLocalToLocal=false;

	private String syncMasterProfType="";  	// R:remote to local, L:local to remote
	private String syncType="";	    		// M:mirror, C:copy, X:move, S:SYNC
	private String syncProfName="";
	private Pattern fileFilterInclude,fileFilterExclude;
	private Pattern dirFilterInclude,dirFilterExclude;
	private ArrayList<Pattern[]> dirIncludeFilterList=new ArrayList<Pattern[]>();
	private ArrayList<Pattern> dirExcludeFilterList=new ArrayList<Pattern>();
	
	private String mirrorIoRootDir="";
	
	private String syncRemoteAddr;
	private String syncRemoteShare;
	private String syncRemoteDir;
	private String syncLocalDir;
	private String syncMasterLocalDir;
	private String syncTargetLocalDir;
	private String syncRemoteUserid;
	private String syncRemotePassword;
	private String remoteUrl; 
	private String localUrl;
	private String remoteMasterDir;
	private NtlmPasswordAuthentication ntlmPasswordAuth =null;;
	
//	private String SMBSync_External_Root_Dir;
	
	private ArrayList<MirrorIoParmList> syncList;

//	private final int SMBSYNC_ERROR_ASYNC_KILL = -10;

	private long totalTransferByte = 0, totalTransferTime=0;
	private int copyCount, deleteCount, ignoreCount;
	private int totalCopyCnt=0, totalDeleteCnt=0, totalIgnoreCnt=0, 
				totalWarningMsgCnt=0, totalErrorMsgCnt=0;
	
	private ThreadCtrl tcMirror=null;
	private ThreadCtrl tcConfirm=null;
	
	private boolean isExceptionOccured=false;
	
	private NotifyEvent notifyEvent;
	
	private String settingsSmbRcvBufSize="",settingsSmbSndBufSize="",
			settingsSmbListSize="",settingsSmbMaxBuffers="",settingsIoBuffers="",
					settingsSmbTcpNodelay="", settingsSmbLogLevel="",
					settingsSmbLmCompatibility="0",
					settingsSmbUseExtendedSecurity="false"; 
	
	private String settingsMediaStoreUseLastModTime="0";
	private boolean settingsMediaFiles,
		defaultSettingScanExternalStorage;

	private boolean syncMasterDirFileProcess=true, 
			syncProfileConfirmRequired=false, syncProfileUseJavaLastModified=true;
	private boolean syncProfileNotUseLastModifiedForRemote=false;
	
	private ArrayList<String> mediaStoreImageList = new ArrayList<String>();
	private ArrayList<String> mediaStoreAudioList = new ArrayList<String>();
	private ArrayList<String> mediaStoreVideoList = new ArrayList<String>();
//	private ArrayList<String> mediaStoreFilesList = new ArrayList<String>();
	
	private boolean isMediaStoreChangeWarningIssued=false;
	private int timeDifferenceLimit=3;
	
	private int timeZone;

	private ArrayList<String> copiedFileList;
	
//	private ArrayList<String> mHistoryCopiedList,mHistoryDeletedList,mHistoryIgnoredList;
	
	private boolean isSyncParmError;
	
	private MediaScannerConnection mediaScanner ;
	
	private ArrayList<FileLastModifiedEntryItem> currentFileLastModifiedList=null;
	private ArrayList<FileLastModifiedEntryItem> newFileLastModifiedList=null;
	private String loadedLocalMountPoint="";
	private ArrayList<LocalFileLastModifiedListCacheItem>mLocalFileLastModifiedCache=
			new ArrayList<LocalFileLastModifiedListCacheItem>();
	
	private ArrayList<SyncHistoryListItem> syncHistoryList=null;
	
	private GlobalParameters glblParms=null;
	
	private SMBSyncUtil mUtil=null;
	
	public MirrorIO(GlobalParameters gwa, NotifyEvent ne, ThreadCtrl ac, ThreadCtrl tw ) {
		glblParms=gwa;
		notifyEvent=ne;
		tcConfirm=tw;
		syncList = glblParms.mirrorIoParms;
		loadMsgString(glblParms);
		tcMirror=ac; //new ThreadCtrl();
		
		mUtil=new SMBSyncUtil(glblParms.svcContext, settingsMediaStoreUseLastModTime, gwa);
		mUtil.setLogIdentifier("MirrorIO");
		
//		SMBSync_External_Root_Dir = LocalMountPoint.getExternalStorageDir();
		
		setJcifsOption();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(glblParms.svcContext);
		settingsMediaStoreUseLastModTime=
			prefs.getString(glblParms.svcContext.getString(R.string.settings_media_store_last_mod_time),"0");
		String td=
			prefs.getString(glblParms.svcContext.getString(R.string.settings_file_diff_time_seconds), "3");
		timeDifferenceLimit=Integer.parseInt(td)*1000;

		buildMediaStoreDirList();
		
		TimeZone tz=TimeZone.getDefault();
		timeZone = tz.getRawOffset();
		
		settingsMediaFiles =
				prefs.getBoolean(glblParms.svcContext.getString(R.string.settings_media_scanner_non_media_files_scan),false);
		defaultSettingScanExternalStorage=
				prefs.getBoolean(glblParms.svcContext.getString(R.string.settings_media_scanner_scan_extstg),false);

		if (glblParms.debugLevel>=1) {
			addDebugLogMsg(1,"I",
					"defautSettingsMediaStore=",settingsMediaStoreUseLastModTime);
			addDebugLogMsg(1,"I",
					"settings_file_diff_time_seconds="+timeDifferenceLimit);
			addDebugLogMsg(1,"I","time zone="+timeZone);
			
			addDebugLogMsg(1,"I",
					"defaultSettingMediaFiles="+settingsMediaFiles+
					", defaultSettingScanExternalStorage="+defaultSettingScanExternalStorage);
		}

		mediaScanner = new MediaScannerConnection(glblParms.svcContext,
				new MediaScannerConnectionClient() {
			@Override
			public void onMediaScannerConnected() {
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"I","MediaScanner connected.");
			};
			@Override
			public void onScanCompleted(final String fp, final Uri uri) {
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"I","MediaScanner scan completed. fn=",
							fp,", Uri="+uri);
//				checkMediaScannerReult(fp,uri);
			};
		});
		mediaScanner.connect();
		currentFileLastModifiedList=new ArrayList<FileLastModifiedEntryItem>();
		newFileLastModifiedList=new ArrayList<FileLastModifiedEntryItem>();
		
		syncHistoryList=mUtil.loadHistoryList();
	};

	@SuppressWarnings("unused")
	final private boolean checkMediaScannerReult(String fp, Uri uri) {
		File lf=new File(fp);
		long lf_lm=lf.lastModified()/1000;
		boolean result=false;
        ContentResolver resolver = glblParms.svcContext.getContentResolver();
        Cursor ci = resolver.query(uri,
        		new String[] {MediaStore.MediaColumns.DATA,
        						MediaStore.MediaColumns.DATE_MODIFIED},
        		MediaStore.MediaColumns.DATA+"=?",new String[]{fp},
        		MediaStore.MediaColumns.DATA);
        boolean checked=false;
        while( ci.moveToNext() ){
        	checked=true;
        	long lm=ci.getLong(ci.getColumnIndex(MediaStore.MediaColumns.DATE_MODIFIED));
        	long ms_lm=0;
        	if (Build.VERSION.SDK_INT<=10) {//Android 3.0以下のバグ回避
	        	if (lm>=1000000000000L) ms_lm=lm/1000;
	        	else ms_lm=lm;
        	} else ms_lm=lm;
//        	Log.v("","lm="+lm+", lmx="+ms_lm);
        	if (ms_lm!=lf_lm) {
        		addLogMsg("W",fp,String
        			.format(msgs_mirror_prof_ms_different_file_last_mod,
        			DateUtil.convDateTimeTo_YearMonthDayHourMinSec(lf_lm*1000),
        			DateUtil.convDateTimeTo_YearMonthDayHourMinSec(ms_lm*1000)));
        	} else result=true;
        }
        if (!checked) {
        	addLogMsg("W",fp,msgs_mirror_prof_ms_read_error);
//        	Log.v("","count="+ci.getCount()+", isFirst="+ci.isFirst());
//        	Log.v("","uri="+uri); 
        }
        ci.close();
        return result;
	};

	final private boolean waitMediaScanner(boolean ds) {
    	boolean result=false;
    	try {
    		int limit=0;
    		while(true) {
    			if (mediaScanner.isConnected()==ds) {
    				result=true;
    				break;
    			}
    			synchronized(mediaScanner) {
    				mediaScanner.wait(50);
    			}
    			limit++;
    			if (limit>200) {
    				result=false;
    				if (glblParms.debugLevel>=1) 
    					addDebugLogMsg(1,"E","MediaScannerConnection wait timeout occured.");
    				break;
    			}
    		}
		} catch (InterruptedException e) {
			addLogMsg("E","","MediaScannerConnection wait error:"+e.getMessage());
			addLogMsg("W","","InterruptedException occured");
			printStackTraceElement(e.getStackTrace());
			result=false;
		}
		return result;
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
    			tcMirror.setThreadResultError();
    			String end_msg=msgs_mirror_task_result_error_ended+"\n"+
    					ex.toString()+st_msg;
        		tcMirror.setThreadMessage(end_msg);
        		addLogMsg("E","",end_msg);
        		NotificationUtil.showOngoingNotificationMsg(glblParms,end_msg);
        		notifyThreadTerminate();
        		tcMirror.setDisable();
                // re-throw critical exception further to the os (important)
//                defaultUEH.uncaughtException(thread, ex);
            }
    };
    
	@Override
	final public void run() {
		addLogMsg("I","",msgs_mirror_task_started);

		defaultUEH = Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler(unCaughtExceptionHandler);
        
		NotificationUtil.showOngoingNotificationMsg(glblParms,msgs_mirror_task_started);
		waitMediaScanner(true);//wait for media scanner service connection
		copiedFileList = new ArrayList<String>();
		totalCopyCnt=totalDeleteCnt=totalIgnoreCnt=totalWarningMsgCnt=totalErrorMsgCnt=0;
		boolean error_occured_but_ignored=false;
		for (int i = 0; i < syncList.size(); i++) {
			totalTransferByte = totalTransferTime= 0;
			isExceptionOccured=isSyncParmError=false;
			copyCount=deleteCount=ignoreCount=0;
//			mHistoryCopiedList=new ArrayList<String>();
//			mHistoryDeletedList=new ArrayList<String>();
//			mHistoryIgnoredList=new ArrayList<String>();
			isMediaStoreChangeWarningIssued=false;
			if (tcMirror.isEnable()) { // async process was enabled
				syncProfName = syncList.get(i).getProfname();
				addMsgToProgDlg(false,"I","",msgs_mirror_prof_started);
				addLogMsg("I","",msgs_mirror_prof_started);
				initSyncParm(syncList.get(i));
				if (!isSyncParmError) {
					loadLocalFileLastModifiedList(syncList.get(i).getLocalMountPoint());
					setJcifsProperties();
					//sync 開始
					if (LocalMountPoint.isMountPointAvailable(localUrl)) {
						if (syncType.equals(SMBSYNC_SYNC_TYPE_MIRROR)) { // mirror
							copiedFileList.clear();
							doSyncMirror(syncList.get(i));
						} else if (syncType.equals(SMBSYNC_SYNC_TYPE_COPY)) { // copy
							copiedFileList.clear();
							doSyncCopy(syncList.get(i));
						} else if (syncType.equals(SMBSYNC_SYNC_TYPE_MOVE)) { // move
							copiedFileList.clear();
							doSyncMove(syncList.get(i));
						} else if (syncType.equals(SMBSYNC_SYNC_TYPE_SYNC)) { // sync
							// sync process
							copiedFileList.clear();
						} else {
							// invalid mirror type was specified
							addLogMsg("E","",
									" "+syncType+" "+msgs_mirror_prof_invalid_mirror_type);
							tcMirror.setThreadMessage(syncType+" "+msgs_mirror_prof_invalid_mirror_type);
							isSyncParmError=true;
						}
					} else {
						// invalid mirror type was specified
						addLogMsg("E",localUrl,msgs_mirror_prof_sync_local_mount_point_unavailable);
						tcMirror.setThreadMessage(msgs_mirror_prof_sync_local_mount_point_unavailable+","+localUrl);
						isSyncParmError=true;
					}
					saveLocalFileLastModifiedList(syncList.get(i).getLocalMountPoint());
				}
				//sync 終了
				//
				totalCopyCnt+=copyCount;
				totalDeleteCnt+=deleteCount;
				totalIgnoreCnt+=ignoreCount;
				if (tcMirror.isEnable()) {
					if (!isSyncParmError && !isExceptionOccured) {
						addLogMsg("I","",String.format(msgs_mirror_prof_no_of_copy,
										copyCount , deleteCount, ignoreCount));
						if (copyCount>0) {
							if (glblParms.debugLevel>=1) 
								addDebugLogMsg(1,"I","TotalByte="+totalTransferByte+
										",Time="+totalTransferTime);
							addLogMsg("I","",
									msgs_mirror_prof_avg_rate+" " + 
									calTransferRate(totalTransferByte,totalTransferTime));
						}
						addLogMsg("I","",msgs_mirror_prof_success_end);
						addHistoryList(SyncHistoryListItem.SYNC_STATUS_SUCCESS,copyCount,deleteCount,ignoreCount,"");
						mUtil.saveHistoryList(syncHistoryList);
					} else { 
						addLogMsg("E","",msgs_mirror_prof_was_failed);
						addHistoryList(SyncHistoryListItem.SYNC_STATUS_ERROR,copyCount,deleteCount,ignoreCount,tcMirror.getThreadMessage());
						mUtil.saveHistoryList(syncHistoryList);
						tcMirror.setExtraDataInt(1);//Indicate error occured
						if (!glblParms.settingErrorOption) {
							break;
						} else {
							error_occured_but_ignored=true;
						}
					}
				} else {
					addLogMsg("W","",msgs_mirror_prof_was_cancelled);
					addHistoryList(SyncHistoryListItem.SYNC_STATUS_CANCELLED,copyCount,deleteCount,ignoreCount,"");
					mUtil.saveHistoryList(syncHistoryList);
					isSyncParmError=true;
					break;
				}
			}
		}
		syncProfName="";
		mediaScanner.disconnect();
		waitMediaScanner(false);//wait for media scanner disconnection

		addLogMsg("I","",msgs_mirror_task_ended);
		NotificationUtil.showOngoingNotificationMsg(glblParms,msgs_mirror_task_ended);

		addLogMsg("I","",String.format(msgs_mirror_task_result_stats, 
				totalCopyCnt, totalDeleteCnt, totalIgnoreCnt,totalWarningMsgCnt,totalErrorMsgCnt));

		String end_msg="";
		if (!tcMirror.isEnable()) {
			tcMirror.setThreadResultCancelled();
			end_msg=msgs_mirror_task_result_cancel;
		} else if (isSyncParmError || isExceptionOccured || error_occured_but_ignored) {
			tcMirror.setThreadResultError();
			if (error_occured_but_ignored) {
				tcMirror.setThreadMessage(msgs_mirror_task_result_error_ignored);
				end_msg=msgs_mirror_task_result_error_ignored;
			} else {
				end_msg=msgs_mirror_task_result_error_ended;
			}
		} else {
			tcMirror.setThreadResultSuccess();
			end_msg=msgs_mirror_task_result_ok;
		}
		tcMirror.setThreadMessage(end_msg);
		addLogMsg("I","",end_msg);
		NotificationUtil.showOngoingNotificationMsg(glblParms,end_msg);
		notifyThreadTerminate();
		tcMirror.setDisable();
	};

	final private void addHistoryList(int status, int copy_cnt, int del_cnt, int ignore_cnt,
			String error_msg) {
		String date_time=DateUtil.convDateTimeTo_YearMonthDayHourMinSec(System.currentTimeMillis());
		String date=date_time.substring(0,10);
		String time=date_time.substring(11);
		SyncHistoryListItem hli=new SyncHistoryListItem();
		hli.sync_date=date;
		hli.sync_time=time;
		hli.sync_prof=syncProfName;
		hli.sync_status=status;
		hli.sync_result_no_of_copied=copy_cnt;
		hli.sync_result_no_of_deleted=del_cnt;
		hli.sync_result_no_of_ignored=ignore_cnt;
		hli.sync_error_text=error_msg;
		hli.sync_log_file_path=glblParms.currentLogFilePath;
//		hli.sync_copied_file=new String[mHistoryCopiedList.size()];
//		for (int i=0;i<mHistoryCopiedList.size();i++) hli.sync_copied_file[i]=mHistoryCopiedList.get(i);
//		
//		hli.sync_deleted_file=new String[mHistoryDeletedList.size()];
//		for (int i=0;i<mHistoryDeletedList.size();i++) hli.sync_deleted_file[i]=mHistoryDeletedList.get(i);
//		
//		hli.sync_ignored_file=new String[mHistoryIgnoredList.size()];
//		for (int i=0;i<mHistoryIgnoredList.size();i++) hli.sync_ignored_file[i]=mHistoryIgnoredList.get(i);
		
		mUtil.addHistoryList(syncHistoryList, hli);
	};
	
	final private void initSyncParm(MirrorIoParmList mipl) {
		syncMasterProfType = mipl.getMasterType();
		String syncTargetProfType = mipl.getTargetType();
		syncType =mipl.getMirrorType();
//		syncRemoteAddr = mipl.getRemoteAddr();
		syncRemoteShare = mipl.getRemoteShare();
		syncRemoteDir = mipl.getRemoteDir();
		syncLocalDir = mipl.getLocalDir();
		syncRemoteUserid = mipl.getRemoteUserid();
		syncRemotePassword = mipl.getRemotePass();
		syncMasterDirFileProcess=mipl.isMasterDirFileProcessed();
		syncProfileConfirmRequired=mipl.isConfirmRequired();
//		Log.v("","addr="+mipl.getRemoteAddr()+", host="+mipl.getHostName());
				
//		if (mipl.getHostName().equals("")) syncRemoteAddr = mipl.getRemoteAddr();
//		else syncRemoteAddr = resolveHostName(mipl.getHostName());
		if (mipl.getHostName().equals("")) syncRemoteAddr = mipl.getRemoteAddr();
		else syncRemoteAddr = mipl.getHostName();
		
		if (mipl.isForceLastModifiedUseSmbsync()) syncProfileUseJavaLastModified=false;
		else {
			syncProfileUseJavaLastModified=
					isSetLastModifiedFunctional(mipl.getLocalMountPoint());
		}
		syncProfileNotUseLastModifiedForRemote=mipl.isNotUseLastModifiedForRemote();
		
		if (syncRemoteDir.equals("")) {
			remoteUrl= "smb://" + syncRemoteAddr + "/"+syncRemoteShare+syncRemoteDir;
		}else {
			remoteUrl= "smb://" + syncRemoteAddr + "/"+syncRemoteShare+"/"+syncRemoteDir;
		}
		remoteMasterDir=remoteUrl;
//		if (syncLocalDir.equals("")) {
//			localUrl=SMBSync_External_Root_Dir+syncLocalDir;
//		} else {
//			localUrl=SMBSync_External_Root_Dir+"/"+syncLocalDir;
//		}
		localUrl=syncLocalDir;
		
//		compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
		
		
		if (syncMasterProfType.equals("L") && syncTargetProfType.equals("L")) {
			syncLocalToLocal=true;
			localUrl=syncLocalDir=mipl.getTargetLocalMountPoint();
			syncMasterLocalDir=mipl.getMasterLocalMountPoint()+"/"+mipl.getMasterLocalDir();
			syncTargetLocalDir=mipl.getTargetLocalMountPoint()+"/"+mipl.getTargetLocalDir();
		} else syncLocalToLocal=false;

// Check local directory access		
		if (syncMasterProfType.equals("L") && syncTargetProfType.equals("L")) {
			File lf=new File(mipl.getMasterLocalMountPoint());
			boolean ex=lf.exists();
			boolean cr=lf.canRead();
			if (!ex || (ex && !cr)) {
				addLogMsg("E",mipl.getMasterLocalMountPoint(),msgs_mirror_master_local_mount_point_not_readable);
				isSyncParmError=true;
			}
			lf=new File(mipl.getTargetLocalMountPoint());
			ex=lf.exists();
			boolean cw=lf.canWrite();
			if (!ex || (ex && !cw)) {
				addLogMsg("E",mipl.getTargetLocalMountPoint(),msgs_mirror_target_local_mount_point_not_writable);
				isSyncParmError=true;
			}
		} else {
			if (syncMasterProfType.equals("L")) {
				File lf=new File(mipl.getLocalMountPoint());
				boolean ex=lf.exists();
				boolean cr=lf.canRead();
				if (!ex || (ex && !cr)) {
					addLogMsg("E",mipl.getLocalMountPoint(),msgs_mirror_master_local_mount_point_not_readable);
					isSyncParmError=true;
				}
			} else {
				File lf=new File(mipl.getLocalMountPoint());
				boolean ex=lf.exists();
				boolean cw=lf.canWrite();
				if (!ex || (ex && !cw)) {
					addLogMsg("E",mipl.getLocalMountPoint(),msgs_mirror_target_local_mount_point_not_writable);
					isSyncParmError=true;
				}
			}
		}
		
		if (glblParms.debugLevel>=1) {
			addDebugLogMsg(1,"I","Sync parameters: " +
				"syncLocalToLocal="+syncLocalToLocal+
				", syncMasterProfType="+syncMasterProfType+
				", syncTargetProfType="+syncTargetProfType+
				", syncType="+ syncType + ", syncProfName=" + syncProfName
				+ ", syncRemoteShare=" + syncRemoteShare
				+ ", syncRemoteDir=" + syncRemoteDir
				+ ", syncLocalDir=" + syncLocalDir
				+ ", syncMasterLocalDir=" + syncMasterLocalDir
				+ ", syncTargetLocalDir=" + syncTargetLocalDir
				+ ", syncRemoteUserid=" + syncRemoteUserid
				+ ", syncMasterDirFileProcess="+syncMasterDirFileProcess
				+ ", syncProfileConfirmRequired="+syncProfileConfirmRequired
				+ ", syncProfileUseJavaLastModified="+syncProfileUseJavaLastModified
				+ ", syncProfileNotUseLastModifiedForRemote="+syncProfileNotUseLastModifiedForRemote
				+ ", fileFilter=" + mipl.getFileFilter()
				+ ", dirFilter=" + mipl.getDirFilter());
 			addDebugLogMsg(9,"I","syncRemotePassword=" + syncRemotePassword);
 			addDebugLogMsg(1,"I","remoteUrl="+remoteUrl+", localUrl="+localUrl);
		}
	};
	
//	private String resolveHostName(String hn) {
//		String ipAddress=hn;
//		try {
//			NbtAddress nbtAddress = NbtAddress.getByName(hn);
//			InetAddress address = nbtAddress.getInetAddress();
//			ipAddress= address.getHostAddress();
//		} catch (UnknownHostException e) {
//			e.printStackTrace();
//		}
//		return ipAddress;
//	}
	
	final private void doSyncMirror(MirrorIoParmList mipl) {
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I","doSyncMirror entered ",
				"errorStatus="+checkErrorStatus(),
				", isExceptionOccured="+isExceptionOccured);
		if (syncLocalToLocal) {
			// Mirror local -> Local
			mirrorIoRootDir=syncMasterLocalDir;
//			isSamePhyscalDirectory(syncMasterLocalDir,syncTargetLocalDir);
			if (!isExceptionOccured) {
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorCopyLocalToLocal(false, syncMasterLocalDir,
						syncTargetLocalDir, syncTargetLocalDir, copiedFileList);
				if (!isExceptionOccured && !isSyncParmError) {
					mirrorDeleteLocalToLocalFile(syncMasterLocalDir, syncTargetLocalDir);
				}
			}
		} else {
			if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_REMOTE)) { 
				// Mirror remote -> local
				mirrorIoRootDir=remoteUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorCopyRemoteToLocal(false, remoteUrl, localUrl,copiedFileList);
				if (!isExceptionOccured && !isSyncParmError)
					mirrorDeleteLocalFile(remoteUrl, localUrl);
			} else if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_LOCAL)) { 
				// Mirror local -> remote
				mirrorIoRootDir=localUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorCopyLocalToRemote(false, localUrl,remoteUrl, copiedFileList);
				if (!isExceptionOccured && !isSyncParmError) {
					mirrorDeleteRemoteFile(localUrl, remoteUrl);
				}
			} else { 
				addLogMsg("E","","invalid master profile type specified:"+syncMasterProfType);
				tcMirror.setThreadMessage("invalid master profile type specified:"+syncMasterProfType);
				isSyncParmError=true;
			}
		}
	};

	final private void doSyncCopy(MirrorIoParmList mipl) {
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I","doSyncCopy entered ",
				"errorStatus="+checkErrorStatus(),
				", isExceptionOccured="+isExceptionOccured);
		if (syncLocalToLocal) {
			// Mirror local -> Local
			mirrorIoRootDir=syncMasterLocalDir;
//			isSamePhyscalDirectory(syncMasterLocalDir,syncTargetLocalDir);
			if (!isExceptionOccured) {
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorCopyLocalToLocal(false, syncMasterLocalDir,
						syncTargetLocalDir, syncTargetLocalDir, copiedFileList);
			}
		} else {
			if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_REMOTE)) { 
				// Copy remote -> local
				mirrorIoRootDir=remoteUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorCopyRemoteToLocal(false, remoteUrl, localUrl,copiedFileList);
			} else if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_LOCAL)) { 
				// copy local -> remote
				mirrorIoRootDir=localUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorCopyLocalToRemote(false, localUrl,remoteUrl, copiedFileList);
			} else {
				addLogMsg("E","","invalid master profile type specified:"+syncMasterProfType);
				tcMirror.setThreadMessage("invalid master profile type specified:"+syncMasterProfType);
				isSyncParmError=true;
			}
		}
	};
	
	final private void doSyncMove(MirrorIoParmList mipl) {
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I","doSyncMove entered ",
				"errorStatus="+checkErrorStatus(),
				", isExceptionOccured="+isExceptionOccured);
		if (syncLocalToLocal) {
			// Mirror local -> Local
			mirrorIoRootDir=syncMasterLocalDir;
//			isSamePhyscalDirectory(syncMasterLocalDir,syncTargetLocalDir);
			if (!isExceptionOccured) {
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorMoveLocalToLocal(false, syncMasterLocalDir,
						syncTargetLocalDir, syncTargetLocalDir, copiedFileList);
				if (copyCount>=0 && !isExceptionOccured && !isSyncParmError) {
					Collections.sort(copiedFileList);
					for (int j=copiedFileList.size()-1;j>=0;j--) {
						if (localUrl.equals(copiedFileList.get(j)))
							break;
						if (isLocalDirEmpty(copiedFileList.get(j))) 
							deleteLocalItem(true,copiedFileList.get(j));
					}
				}
			}
		} else {
			if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_REMOTE)) { 
				// Move remote -> local
				mirrorIoRootDir=remoteUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorMoveRemoteToLocal(false, remoteUrl, localUrl,copiedFileList);
				if (copyCount>=0 && !isExceptionOccured && !isSyncParmError) {
					Collections.sort(copiedFileList);
					for (int j=copiedFileList.size()-1;j>=0;j--) {
						if ((remoteUrl+"/").equals(copiedFileList.get(j)))
							break;
						if (isRemoteDirEmpty(copiedFileList.get(j))) 
							deleteRemoteItem(true,copiedFileList.get(j));
					}
				} 
			} else if (syncMasterProfType.equals(SMBSYNC_PROF_TYPE_LOCAL)) { 
				// Move local -> remote
				mirrorIoRootDir=localUrl;
				compileFilter(mipl.getFileFilter(),mipl.getDirFilter());
				mirrorMoveLocalToRemote(false, localUrl,remoteUrl, copiedFileList);
				if (copyCount>=0 && !isExceptionOccured && !isSyncParmError) {
					Collections.sort(copiedFileList);
					for (int j=copiedFileList.size()-1;j>=0;j--) {
						if (localUrl.equals(copiedFileList.get(j)))
							break;
						if (isLocalDirEmpty(copiedFileList.get(j))) 
							deleteLocalItem(true,copiedFileList.get(j));
					}
				}
			} else {
				addLogMsg("E","","invalid master profile type specified:"+syncMasterProfType);
				tcMirror.setThreadMessage("invalid master profile type specified:"+syncMasterProfType);
				isSyncParmError=true;
			}
		}
	};

//	final private void isSamePhyscalDirectory(String master_fp, String target_fp) {
//		File mf=new File(master_fp+"/smbsync_tmp_file"); 
//		File tf=new File(target_fp+"/smbsync_tmp_file");
//		boolean result=false;
//		try {
//			String mfcp=mf.getCanonicalPath();
//			String tfcp=tf.getCanonicalPath();
////			Log.v("","isSamePhyscalDirectory mfcp="+mfcp);
////			Log.v("","isSamePhyscalDirectory tfcp="+tfcp);
//			if (mfcp.equals(tfcp)) result=true;
//		} catch (IOException e) {
//			e.printStackTrace();
//			result=true;
//		}
//		if (result) addLogMsg("E","","isSamePhyscalDirectory Exception detected. "
//				+ "Tried to write in the same directory the same file.");
//		isExceptionOccured=result;
//	};
	
	final private void buildMediaStoreDirList() {
		String [] proj = new String[] {MediaStore.MediaColumns.DATA};
    	ContentResolver resolver = glblParms.svcContext.getContentResolver();
    	//build image
        Cursor ci = resolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI ,
        		proj ,null ,null ,"_data");
        String c_m_d="";
        while( ci.moveToNext() ){
        	String data=ci.getString( ci.getColumnIndex(MediaStore.MediaColumns.DATA ));
        	if (data.lastIndexOf("/")>=1) {
	        	String t_dir=data.substring(0,data.lastIndexOf("/"));
	        	if  (!c_m_d.equals(t_dir)) {
	        		mediaStoreImageList.add(t_dir);
	        		c_m_d=t_dir;
	        	}
        	}
        }
        ci.close();
    	//build audio
        Cursor ca = resolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI ,
        		proj ,null ,null ,"_data");
        c_m_d="";
        while( ca.moveToNext() ){
        	String data=ca.getString( ca.getColumnIndex(MediaStore.MediaColumns.DATA ));
        	if (data.lastIndexOf("/")>=1) {
	        	String t_dir=data.substring(0,data.lastIndexOf("/"));
	        	if  (!c_m_d.equals(t_dir)) {
	        		mediaStoreAudioList.add(t_dir);
	        		c_m_d=t_dir;
	        	}
        	}
        }
        ca.close();
    	//build video
        Cursor cv = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        		proj ,null ,null ,"_data");
        c_m_d="";
        while( cv.moveToNext() ){
        	String data=cv.getString( cv.getColumnIndex(MediaStore.MediaColumns.DATA ));
        	if (data.lastIndexOf("/")>=1) {
	        	String t_dir=data.substring(0,data.lastIndexOf("/"));
	        	if  (!c_m_d.equals(t_dir)) {
	        		mediaStoreVideoList.add(t_dir);
	        		c_m_d=t_dir;
	        	}
        	}
        }
        cv.close();

        //build Files(Document)
//        if(Build.VERSION.SDK_INT >= 11) {
//       	Cursor cd = resolver.query(MediaStore.Files.getContentUri("external"),
//        				proj ,null ,null ,"_data");
//	        while( cd.moveToNext() ){
//	        	String data=cd.getString( cd.getColumnIndex(MediaStore.MediaColumns.DATA));
//	        	if (data.lastIndexOf("/")>=1) {
//		        	String t_dir=data.substring(0,data.lastIndexOf("/"));
//		        	if  (!findMediaStoreDirList(t_dir,mediaStoreFilesList)) {
//		        		mediaStoreFilesList.add(t_dir);
//		        	}
//	        	}
//	        }
//	        cd.close();
//        }

		if (glblParms.debugLevel>=1) { 
        	for (int i=0;i<mediaStoreImageList.size();i++) 
        		addDebugLogMsg(1,"I","mediaStoreImageList="+mediaStoreImageList.get(i));
        	for (int i=0;i<mediaStoreAudioList.size();i++) 
        		addDebugLogMsg(1,"I","mediaStoreAudioList="+mediaStoreAudioList.get(i));
        	for (int i=0;i<mediaStoreVideoList.size();i++) 
        		addDebugLogMsg(1,"I","mediaStoreVideoList="+mediaStoreVideoList.get(i));
//        	for (int i=0;i<mediaStoreFilesList.size();i++) 
//        		sendDebugLogMsg(1,"I","mediaStoreFilesList="+mediaStoreFilesList.get(i));
        }
	};
	
//	private boolean findMediaStoreDirList(String dir, ArrayList<String> al) {
//		boolean found=false;
//		if (mediaStoreImageList.size()!=0) {
//			if (Collections.binarySearch(al,dir)>=0) 
//				found=true;
//		}
//		return found;
//	};

	final private void scanMediaStoreLibrary(String fp) {
//		defaultSettingScanExternalStorage
		if (LocalMountPoint.isExternalMountPoint(fp) && !defaultSettingScanExternalStorage) {
			if (glblParms.debugLevel>=1) 
				addDebugLogMsg(1,"I",
					"scanMediaStoreLibrary scan external storage disabled, " ,
					"MediaScanner not invoked. Path=",fp);
			return;
		}
		if (!mediaScanner.isConnected()) {
			if (glblParms.debugLevel>=1) 
				addLogMsg("W",fp,
					"mediaScanner not connected, MediaScanner not invoked.");
			return;
		}
		String mt=isMediaFile(fp);
		if (mt==null) mt="";
		if (Build.VERSION.SDK_INT<11) { //Android 2.1/2.2/2.3
			if (!isNoMediaPath(fp)) {  
				if	(mt.startsWith("audio") || mt.startsWith("video") ||
						 mt.startsWith("image") ) { 
					File lf=new File(fp);
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"scanMediaStoreLibrary MediaScanner invoked. fn=",fp,
							", lastModified="+lf.lastModified()+
							", date=",
							DateUtil.convDateTimeTo_YearMonthDayHourMinSec(lf.lastModified()));
					mediaScanner.scanFile(fp, mt);
				} else {
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"scanMediaStoreLibrary Mime type was not audio/image/video, " ,
							"MediaScanner not invoked. mime type=",mt);
				}
			} else {
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"I",
							"scanMediaStoreLibrary hidden directory or .nomedia found, " ,
							"MediaScanner not invoked.");
			}
		} else {//Android 3.0以上
			if	(mt.startsWith("audio") || mt.startsWith("video") ||
					 mt.startsWith("image") ) {
				if (!isNoMediaPath(fp)) {
					File lf=new File(fp);
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"scanMediaStoreLibrary MediaScanner invoked. fn=",fp,
							", lastModified="+lf.lastModified(),
							", date=",
							DateUtil.convDateTimeTo_YearMonthDayHourMinSec(lf.lastModified()));
					mediaScanner.scanFile(fp, mt);
				} else {
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"scanMediaStoreLibrary hidden directory or .nomedia found, ",
							"MediaScanner not invoked.");
				}
			} else {
				if (settingsMediaFiles) {
					File lf=new File(fp);
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"scanMediaStoreLibrary MediaScanner invoked. fn=",fp,
							", lastModified="+lf.lastModified(),
							", date=",DateUtil.convDateTimeTo_YearMonthDayHourMinSec(lf.lastModified()));
					mediaScanner.scanFile(fp, mt);
				} else {
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"scanMediaStoreLibrary scan MediaFiles disabled, " ,
							"MediaScanner not invoked.");
				}
			}
		}
	};

	static final private String isMediaFile(String fp) {
		String mt=null;
		String fid="";
		if (fp.lastIndexOf(".")>0) {
			fid=fp.substring(fp.lastIndexOf(".")+1,fp.length());
			fid=fid.toLowerCase(Locale.getDefault());
		}
		mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
		return mt;
	};
	
	static final private boolean isNoMediaPath(String path) {
        if (path == null) return false;

        if (path.indexOf("/.") >= 0) return true;

        int offset = 1;
        while (offset >= 0  && offset<path.lastIndexOf("/")) {
            int slashIndex = path.indexOf('/', offset);
            if (slashIndex > offset) {
                slashIndex++; // move past slash
                File file = new File(path.substring(0, slashIndex) + ".nomedia");
//              Log.v("","off="+offset+", si="+slashIndex+", p="+file.getPath());                
                if (file.exists()) {
                    return true;
                }
            }
            offset = slashIndex;
        }
        return false;
    }

//	private void scanMediaStoreLibrary(String fp) {
//		String fid="";
//		if (fp.lastIndexOf(".")>0) {
//			fid=fp.substring(fp.lastIndexOf(".")+1,fp.length());
//			fid=fid.toLowerCase();
//		}
//		String mt=MimeTypeMap.getSingleton().getMimeTypeFromExtension(fid);
//		if (mt!=null) {
//			sendDebugLogMsg(1,"I",
//					"scanMediaStoreLibrary scan was invoked. mt="+mt+", fn="+fp);
//			mediaScanner.scanFile(fp, mt);
//		} else {
//			sendDebugLogMsg(1,"I","scanMediaStoreLibrary scan was not invoked. fn="+fp);
//		}
//	};
	
	//API8
//	private void scanMediaStoreLibrary(String fp) {
//		String[] scanfile = new String[] {fp};
//		sendDebugLogMsg(1,"I","scanMediaStoreLibrary scan invoked fn="+fp);
//		MediaScannerConnection.scanFile(globalWorkArea.appContext,scanfile, null,null);
//	};
	
	@SuppressLint("NewApi")
	final private int deleteMediaStoreItem(String fp) {
		int dc_image=0, dc_audio=0, dc_video=0, dc_files=0;
		if (isMediaStoreDir(fp)) {
	    	ContentResolver cr = glblParms.svcContext.getContentResolver();
	    	dc_image=cr.delete(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
	          		MediaStore.Images.Media.DATA + "=?", new String[]{fp} );
	       	dc_audio=cr.delete(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
	           		MediaStore.Audio.Media.DATA + "=?", new String[]{fp} );
	       	dc_video=cr.delete(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
	           		MediaStore.Video.Media.DATA + "=?", new String[]{fp} );
	        if(Build.VERSION.SDK_INT >= 11) {
	        	dc_files=cr.delete(MediaStore.Files.getContentUri("external"), 
	          		MediaStore.Files.FileColumns.DATA + "=?", new String[]{fp} );
	        }
			if (glblParms.debugLevel>=1) 
	       		addDebugLogMsg(1,"I","deleMediaStoreItem fn=",fp,
	       				", delete count image="+dc_image,
	       				", audio="+dc_audio,", video="+dc_video,", files="+dc_files);
		} else {
			if (glblParms.debugLevel>=1) 
	       		addDebugLogMsg(1,"I","deleMediaStoreItem not MediaStore library. fn=",fp);
		}
		
		return dc_image+dc_audio+dc_video+dc_files;
	};
	
	static final private String calTransferRate(long tb, long tt) {
	    String tfs = null;
	    BigDecimal bd_tr;
	    
	    if (tb==0) return "0Bytes/sec";
	    
	    if (tb>(1024)) {//KB
		    BigDecimal dfs1 = new BigDecimal(tb*1.000);
		    BigDecimal dfs2 = new BigDecimal(1024*1.000);
		    BigDecimal dfs3 = new BigDecimal("0.000000");
		    dfs3=dfs1.divide(dfs2);
			BigDecimal dft1 = new BigDecimal(tt*1.000);
		    BigDecimal dft2 = new BigDecimal(1000.000);
		    BigDecimal dft3 = new BigDecimal("0.000000");
		    dft3=dft1.divide(dft2);
			bd_tr=dfs3.divide(dft3,2,BigDecimal.ROUND_HALF_UP);
			tfs=bd_tr+"KBytes/sec";
		} else {
		    BigDecimal dfs1 = new BigDecimal(tb*1.000);
		    BigDecimal dfs2 = new BigDecimal(1024*1.000);
		    BigDecimal dfs3 = new BigDecimal("0.000000");
		    dfs3=dfs1.divide(dfs2);
			BigDecimal dft1 = new BigDecimal(tt*1.000);
		    BigDecimal dft2 = new BigDecimal(1000.000);
		    BigDecimal dft3 = new BigDecimal("0.000000");
		    dft3=dft1.divide(dft2);
			bd_tr=dfs3.divide(dft3,2,BigDecimal.ROUND_HALF_UP);
			tfs=bd_tr+"Bytes/sec";
		}
		
		return tfs;
	}

	final private void setJcifsOption() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(glblParms.svcContext);
		String cp=
				prefs.getString(glblParms.svcContext.getString(R.string.settings_smb_perform_class), "");
		
		if (cp.equals("0")) {//Minimum
			settingsSmbLogLevel="0";
			settingsSmbRcvBufSize="16644";
			settingsSmbSndBufSize="16644";
			settingsSmbListSize="130170";
			settingsSmbMaxBuffers="";
			settingsIoBuffers="4";
			settingsSmbTcpNodelay="false";
		} else if (cp.equals("1")) {//Medium
			settingsSmbLogLevel="0";
			settingsSmbRcvBufSize="33288";
			settingsSmbSndBufSize="33288";
			settingsSmbListSize="130170";
			settingsSmbMaxBuffers="100";
			settingsIoBuffers="4";
			settingsSmbTcpNodelay="false";
		} else if (cp.equals("2")) {//Large
			settingsSmbLogLevel="0";
			settingsSmbRcvBufSize="66576";
			settingsSmbSndBufSize="66576";
			settingsSmbListSize="130170";
			settingsSmbMaxBuffers="100";
			settingsIoBuffers="8";
			settingsSmbTcpNodelay="false";
		} else {
			settingsSmbLogLevel=
					prefs.getString(glblParms.svcContext.getString(R.string.settings_smb_log_level), "0");
			if (settingsSmbLogLevel.length()==0) settingsSmbLogLevel="0";
			
			settingsSmbRcvBufSize=
					prefs.getString(glblParms.svcContext.getString(R.string.settings_smb_rcv_buf_size),"66576");
			settingsSmbSndBufSize=
					prefs.getString(glblParms.svcContext.getString(R.string.settings_smb_snd_buf_size),"66576");
			settingsSmbListSize=
					prefs.getString(glblParms.svcContext.getString(R.string.settings_smb_listSize), "");
			settingsSmbMaxBuffers=
					prefs.getString(glblParms.svcContext.getString(R.string.settings_smb_maxBuffers), "100");
			settingsIoBuffers=
					prefs.getString(glblParms.svcContext.getString(R.string.settings_io_buffers), "8");
			settingsSmbTcpNodelay=
					prefs.getString(glblParms.svcContext.getString(R.string.settings_smb_tcp_nodelay),"false");
		}
			
		settingsSmbLmCompatibility=
			prefs.getString(glblParms.svcContext.getString(R.string.settings_smb_lm_compatibility),"0");
		boolean ues=
				prefs.getBoolean(glblParms.svcContext.getString(R.string.settings_smb_use_extended_security),false);
		if (ues) settingsSmbUseExtendedSecurity="true";
		else settingsSmbUseExtendedSecurity="false";

		mirrorIoBufferSize=Integer.parseInt(settingsIoBuffers)*65536;
		
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I","JCIFS Option : rcv_buf_size="+settingsSmbRcvBufSize+", "+
				"snd_buf_size="+settingsSmbSndBufSize+", "+
				"listSize="+settingsSmbListSize+", "+
				"maxBuffres="+settingsSmbMaxBuffers+", "+
				"iobuffers="+mirrorIoBufferSize+", "+
				"tcpNodelay="+settingsSmbTcpNodelay+", "+
				"logLevel="+settingsSmbLogLevel+", "+
				"lmCompatibility="+settingsSmbLmCompatibility+", "+
				"useExtendedSecurity="+settingsSmbUseExtendedSecurity);
		mirrorIoBuffer = new byte[mirrorIoBufferSize];
	};
	
	final private void setJcifsProperties() {

//		System.setProperty("jcifs.util.loglevel", settingsSmbLogLevel);
//		System.setProperty("jcifs.smb.lmCompatibility", "0");
//		System.setProperty("jcifs.smb.client.useExtendedSecurity", "false");
//		Auth errorの回避 
		jcifs.Config.setProperty( "jcifs.netbios.retryTimeout", "3000");
		
		System.setProperty("jcifs.util.loglevel", settingsSmbLogLevel);
		System.setProperty("jcifs.smb.lmCompatibility", settingsSmbLmCompatibility);
		System.setProperty("jcifs.smb.client.useExtendedSecurity", settingsSmbUseExtendedSecurity);
		String tuser=null,tpass=null;
		if (syncRemoteUserid.length()!=0) tuser=syncRemoteUserid;
		if (syncRemotePassword.length()!=0) tpass=syncRemotePassword;
		ntlmPasswordAuth = 
				new NtlmPasswordAuthentication(null, tuser, tpass);
 
		System.setProperty("jcifs.smb.client.tcpNoDelay",settingsSmbTcpNodelay);
        
		if (!settingsSmbRcvBufSize.equals(""))
			System.setProperty("jcifs.smb.client.rcv_buf_size", settingsSmbRcvBufSize);//60416 120832
		if (!settingsSmbSndBufSize.equals(""))
			System.setProperty("jcifs.smb.client.snd_buf_size", settingsSmbSndBufSize);//16644 120832
        
		if (!settingsSmbListSize.equals(""))
			System.setProperty("jcifs.smb.client.listSize",settingsSmbListSize); //65536 1300
		if (!settingsSmbMaxBuffers.equals(""))
			System.setProperty("jcifs.smb.maxBuffers",settingsSmbMaxBuffers);//16 100
//		jcifs.Config.registerSmbURLHandler();
		
//		System.setProperty("jcifs.netbios.client.writeSize","65536");//1500
	};

	
	final private int checkErrorStatus() {
		int rc=0;
		if (!tcMirror.isEnable()) rc=-10;
		if (isExceptionOccured) rc=-1;
//		if (glblParms.debugLevel>=3) 
//			addDebugLogMsg(3,"I","checkErrorStatus status="+rc);
		return rc;
	};
	
	final private int mirrorCopyLocalToRemote(boolean allcopy, String masterUrl,
			String targetUrl, ArrayList<String> copiedFileList) {
		SmbFile hf = null;
		File lf;
		
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","mirrorCopyLocalToRemote master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		String tmp_target="";
		
		try {
			lf = new File(masterUrl);
			if (lf.exists()) {
				if (lf.isDirectory()) { // Directory copy
					if (lf.canRead() && 
							isDirectoryToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))) {
						File[] children = lf.listFiles();
						for (File element : children) {
							String tmp = element.getName();
							if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
							mirrorCopyLocalToRemote(allcopy, masterUrl + "/"
									+ tmp,targetUrl + "/" + tmp, copiedFileList);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
					} 
				} else { // file copy
					if (isDirFiltered(masterUrl.replace(localUrl, "")) &&
							isFileFiltered(masterUrl)) {
						createRemoteDir(targetUrl,null,masterUrl);
						copiedFileList.add(masterUrl);
						lf = new File(masterUrl);
						hf = new SmbFile(targetUrl,ntlmPasswordAuth);
						String t_fp=masterUrl.replace(localUrl, "");
						if (isFileChangedForLocalToRemote(masterUrl,lf,hf,allcopy)) { 
							// copy was done
							if (confirmCopy(targetUrl)) {
								long file_byte=lf.length();
								String t_fn=lf.getName().replace("/","");

								if (glblParms.settingRemoteFileCopyByRename) {
									tmp_target=makeTempFilePath(targetUrl);
								}
								copyFileLocalToRemote(lf,hf,file_byte,t_fn,t_fp,tmp_target);
								if (checkErrorStatus()!=0) {
									return checkErrorStatus();
								}
								try {
									if (!syncProfileNotUseLastModifiedForRemote)
										hf.setLastModified(lf.lastModified());
								} catch(SmbException e) {
									addLogMsg("W",targetUrl,
											glblParms.svcContext.getString(R.string.msgs_mirror_prof_remote_file_set_last_modified_failed));
									addDebugLogMsg(1,"W",targetUrl,
											"Remote file setLastModified() failed, reason="+ e.getMessage());
								}
								copyCount++;
							} else {
								addLogMsg("W",targetUrl,msgs_mirror_confirm_copy_cancel);
							}
						} else {
//							updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
//									targetUrl,hf.getLastModified());
						}
						if (!tcMirror.isEnable()) return -10;
						if (isExceptionOccured) return -1;
					}
				}
			} else {
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"W","Local file ", masterUrl,
							" was not copied, because file/dir not existed.");
				addLogMsg("I",masterUrl,msgs_mirror_prof_master_not_found );
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				return -1;
				
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		} catch (SmbException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		} catch (UnknownHostException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		} catch (FileNotFoundException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		} catch (IOException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		}
		return 0;
	};

	private void deleteRemoteTempFile(String tmp_target) {
		SmbFile hf_tmp=null;
		try {
			if (!tmp_target.equals("")) {
				hf_tmp=new SmbFile(tmp_target, ntlmPasswordAuth);
				if (hf_tmp.exists()) hf_tmp.delete();
			}
		} catch (SmbException e1) {
		} catch (MalformedURLException e) {
		}
		hf_tmp=null;
	};

	private void deleteLocalTempFile(String tmp_target) {
		if (!tmp_target.equals("")) {
			File lf_tmp=new File(tmp_target);
			if (lf_tmp.exists()) lf_tmp.delete();
		}
	};

	private void printStackTraceElement(StackTraceElement[] ste) {
		for (int i=0;i<ste.length;i++) {
			addLogMsg("E","",ste[i].toString());	
		}
	}
	
	final private int mirrorDeleteRemoteFile(String masterUrl, String targetUrl) {
		SmbFile hf;
		File lf;

		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(2,"I","mirrorDeleteRemoteFile master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		try {
			hf = new SmbFile(targetUrl,ntlmPasswordAuth);
			String t_dir="";
			if (hf.isDirectory()) t_dir=hf.getPath();
				else t_dir=hf.getParent();
			if (!isDirExcluded(t_dir.replace(remoteUrl, "")) &&
					isDirectoryToBeProcessed(t_dir.replace(remoteUrl, ""))){ 
				if (hf.isDirectory()) { // Directory Delete
					lf = new File(masterUrl);
					if (lf.exists()) {
						hf = new SmbFile(targetUrl + "/",ntlmPasswordAuth);
						SmbFile[] children = hf.listFiles();
						for (SmbFile element : children) {
							String tmp = element.getName();
							if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
							mirrorDeleteRemoteFile(masterUrl + "/"+tmp,targetUrl+"/"+tmp);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
					} else {
						// local Dir was not found,delete remote dir
						if (confirmDelete(targetUrl)) {
							deleteRemoteItem(true,targetUrl);
//							mHistoryDeletedList.add(targetUrl);
						} else {
							addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				} else { // file Delete
					lf = new File(masterUrl);
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(3,"I","Local file exists="+lf.exists());
					if (!lf.exists()) {
						String m_dir=targetUrl.replace(remoteMasterDir+"/","");
						if (!(m_dir.indexOf("/")<0 && !syncMasterDirFileProcess)) { 
							if (confirmDelete(targetUrl)) {
								deleteRemoteItem(true,targetUrl);
//								mHistoryDeletedList.add(targetUrl);
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","","mirrorDeleteRemoteFile From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			return -1;
		} catch (SmbException e) {
			addLogMsg("E","","mirrorDeleteRemoteFile From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			return -1;
		}
		return deleteCount;
	};

	final private int mirrorCopyLocalToLocal(boolean allcopy, String masterUrl,
			String targetUrl, String target_fp_base, ArrayList<String> copiedFileList) {
		File tf;
		File mf;
		
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","mirrorCopyLocalToLocal master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();
		String tmp_target="";
		try {
			mf = new File(masterUrl);
			if (mf.exists()) {
				if (mf.isDirectory()) { // Directory copy
					if (mf.canRead() && 
							isDirectoryToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))) {
						File[] children = mf.listFiles();
						for (File element : children) {
							String tmp = element.getName();
							if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
							String n_master=(masterUrl + "/"+ tmp).replaceAll("//", "/");
							String n_target=(targetUrl + "/" + tmp).replaceAll("//", "/");
							
							if (!n_master.equals(target_fp_base)) {
								mirrorCopyLocalToLocal(allcopy, n_master, n_target, target_fp_base,
										copiedFileList);
								if (checkErrorStatus()!=0) return checkErrorStatus();
							} else {
								addLogMsg("W","",
										String.format(msgs_mirror_same_directory_ignored,n_master));
							}
						}
					} 
				} else { // file copy
					if (isDirFiltered(masterUrl.replace(localUrl, "")) &&
							isFileFiltered(masterUrl)) {
						createLocalDir(targetUrl,null,masterUrl);
						copiedFileList.add(masterUrl);
						mf = new File(masterUrl);
						tf = new File(targetUrl);
						String t_fp=masterUrl.replace(localUrl, "");
						if (isFileChanged(targetUrl,tf,mf,allcopy)) {							
							// copy was done
							if (confirmCopy(targetUrl)) {
								long file_byte=mf.length();
								String t_fn=mf.getName().replace("/","");

								if (glblParms.settingLocalFileCopyByRename) {
									tmp_target=makeTempFilePath(targetUrl);
								}
								copyFileLocalToLocal(mf,tf,file_byte,t_fn,t_fp, tmp_target);
								if (checkErrorStatus()!=0) return checkErrorStatus();
								tf.setLastModified(mf.lastModified());
								updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
										targetUrl,mf.lastModified());
								copyCount++;
							} else {
								addLogMsg("W",targetUrl,msgs_mirror_confirm_copy_cancel);
							}
						}
						if (!tcMirror.isEnable()) return -10;
						if (isExceptionOccured) return -1;
					}
				}
			} else {
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"W","Local file ", masterUrl,
							" was not copied, because file/dir not existed.");
				addLogMsg("I",masterUrl,msgs_mirror_prof_master_not_found );
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				return -1;
				
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (SmbException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (UnknownHostException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (FileNotFoundException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (IOException e) {
			addLogMsg("E","","mirrorCopyLocalToRemote From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		}
		return 0;
	};

	final private int mirrorDeleteLocalToLocalFile(String masterUrl, String targetUrl) {
		File hf;
		File lf;

		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(2,"I","mirrorDeleteRemoteFile master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		hf = new File(targetUrl);
		String t_dir="";
		if (hf.isDirectory()) t_dir=hf.getPath();
			else t_dir=hf.getParent();
		if (!isDirExcluded(t_dir.replace(remoteUrl, "")) &&
				isDirectoryToBeProcessed(t_dir.replace(remoteUrl, ""))){ 
			if (hf.isDirectory()) { // Directory Delete
				lf = new File(masterUrl);
				if (lf.exists()) {
					hf = new File(targetUrl + "/");
					File[] children = hf.listFiles();
					for (File element : children) {
						String tmp = element.getName();
						if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
						mirrorDeleteLocalToLocalFile(masterUrl + "/"+tmp,targetUrl+"/"+tmp);
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				} else {
					// local Dir was not found,delete remote dir
					if (confirmDelete(targetUrl)) {
						deleteLocalItem(true,targetUrl);
//							mHistoryDeletedList.add(targetUrl);
					} else {
						addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
					}
					if (checkErrorStatus()!=0) return checkErrorStatus();
				}
			} else { // file Delete
				lf = new File(masterUrl);
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(3,"I","Local file exists="+lf.exists());
				if (!lf.exists()) {
					String m_dir=targetUrl.replace(remoteMasterDir+"/","");
					if (!(m_dir.indexOf("/")<0 && !syncMasterDirFileProcess)) { 
						if (confirmDelete(targetUrl)) {
							deleteLocalItem(true,targetUrl);
//								mHistoryDeletedList.add(targetUrl);
						} else {
							addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
						}
					}
					if (checkErrorStatus()!=0) return checkErrorStatus();
				}
			}
		}
		return deleteCount;
	};

	final private int mirrorCopyRemoteToLocal(boolean allcopy, String masterUrl,
			String targetUrl, ArrayList<String> copiedFileList) {
		SmbFile hf;
		File lf;

		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","mirrorCopyRemoteToLocal from=", masterUrl, ", to=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();
		String tmp_target="";
		try {
			hf = new SmbFile(masterUrl,ntlmPasswordAuth);
			if (hf.exists()) {
				if (hf.isDirectory()) { // Directory copy
					if (hf.canRead() && isDirectoryToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))) {
						hf = new SmbFile(masterUrl + "/",ntlmPasswordAuth);
						try {
							SmbFile[] children = hf.listFiles();
							for (SmbFile element : children) {
								String tmp = element.getName();
								if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
								mirrorCopyRemoteToLocal(allcopy, masterUrl + "/"+ tmp,
										targetUrl + "/"+ tmp, copiedFileList);
								if (checkErrorStatus()!=0) return checkErrorStatus();
							}
						} catch (SmbException e) {
							addLogMsg("W","","SmbException occured during SmbFile#listFiles(), name="+masterUrl+
										", jcifs error="+e.getMessage());
						}
					}
				} else { // file copy
					if (isDirFiltered(masterUrl.replace(remoteUrl, "")) &&
							isFileFiltered(masterUrl)) {
						createLocalDir(targetUrl,null,masterUrl);
						copiedFileList.add(masterUrl);
						lf = new File(targetUrl);
						String t_fp=masterUrl.replace("smb://"+syncRemoteAddr, "");
						if (isFileChanged(t_fp,lf,hf,allcopy)) {
							// copy 
							if (confirmCopy(targetUrl)) {
								long file_byte=hf.length();
								String t_fn =hf.getName().replace("/", "");
								if (glblParms.settingLocalFileCopyByRename) {
									tmp_target=makeTempFilePath(targetUrl);
								}
								copyFileRemoteToLocal(hf,lf,file_byte,t_fn,t_fp, tmp_target);
								updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
										targetUrl,hf.getLastModified());
								if (checkErrorStatus()!=0) return checkErrorStatus();
//								mHistoryCopiedList.add(targetUrl);
								if (syncProfileUseJavaLastModified) {
									if (!lf.setLastModified(hf.lastModified())) {
										addLogMsg("W",targetUrl,
											glblParms.svcContext.getString(R.string.msgs_mirror_prof_local_file_set_last_modified_failed));
									}
								}
//								if (isMediaStoreDir(lf.getParent()))
								scanMediaStoreLibrary(targetUrl);
								copyCount++;
							} else {
								copiedFileList.remove(targetUrl);
								addLogMsg("W",targetUrl,msgs_mirror_confirm_delete_cancel);
							}
						} else {
							updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
									targetUrl,hf.getLastModified());
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();					}
				}
			} else {
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"E","remote file ", masterUrl,
							" was not copied, because file/dir not found");
				addMsgToProgDlg(true,"E",masterUrl,msgs_mirror_prof_master_not_found);
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				return -1;
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (SmbException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (UnknownHostException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (FileNotFoundException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (IOException e) {
			addLogMsg("E","","mirrorCopyRemoteToLocal From="+masterUrl+", To="+targetUrl);
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		}
		return 0;
	};
	
	private int confirmCopyResult=0, confirmDeleteResult=0;
	final private boolean confirmDelete(String url) {
		boolean result=true;
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","confirmDelete entered url=",url);

		if (syncProfileConfirmRequired) {
			if (confirmDeleteResult!=SMBSYNC_CONFIRM_RESP_YESALL && 
					confirmDeleteResult!=SMBSYNC_CONFIRM_RESP_NOALL) {
				try {
					tcConfirm.initThreadCtrl();
					glblParms.callBackStub.cbShowConfirm(url, SMBSYNC_CONFIRM_FOR_DELETE);
					synchronized(tcConfirm) {
						tcConfirm.wait();//Posted by SMBSyncService#aidlConfirmResponse()
					}
					confirmDeleteResult=tcConfirm.getExtraDataInt();
					if (confirmDeleteResult>0) result=true;
					else result=false;
				} catch (RemoteException e) {
					addLogMsg("E","","RemoteException occured");
					printStackTraceElement(e.getStackTrace());
				} catch (InterruptedException e) {
					addLogMsg("E","","InterruptedException occured");
					printStackTraceElement(e.getStackTrace());
				}
			} else {
				if (confirmDeleteResult==SMBSYNC_CONFIRM_RESP_YESALL) result=true;
				else result=false;
			}
		}
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","confirmDelete result="+result,
				", confirmResult="+confirmDeleteResult,", syncProfileConfirmRequired="+syncProfileConfirmRequired);

		return result;
	};

	final private boolean confirmCopy(String url) throws MalformedURLException, SmbException {
		boolean result=true;
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","confirmCopy entered url=",url);
		if (syncProfileConfirmRequired) {
			if (confirmCopyResult!=SMBSYNC_CONFIRM_RESP_YESALL && 
					confirmCopyResult!=SMBSYNC_CONFIRM_RESP_NOALL) {
				boolean file_exists=false;
				if (url.startsWith("smb://")) {
					SmbFile rf=new SmbFile(url+"/",ntlmPasswordAuth);
					file_exists=rf.exists();
				} else {
					File lf=new File(url);
					file_exists=lf.exists();
				}
				if (file_exists) {
					try {
						tcConfirm.initThreadCtrl();
						glblParms.callBackStub.cbShowConfirm(url, SMBSYNC_CONFIRM_FOR_COPY);
						synchronized(tcConfirm) {
							tcConfirm.wait();//Posted by SMBSyncService#aidlConfirmResponse()
						}
						confirmCopyResult=tcConfirm.getExtraDataInt();
						if (confirmCopyResult>0) result=true;
						else result=false;
					} catch (RemoteException e) {
						addLogMsg("E","","RemoteException occured");
						printStackTraceElement(e.getStackTrace());
					} catch (InterruptedException e) {
						addLogMsg("E","","InterruptedException occured");
						printStackTraceElement(e.getStackTrace());
					}
				}
			} else {
				if (confirmCopyResult==SMBSYNC_CONFIRM_RESP_YESALL) result=true;
				else result=false;
			}
		}
		
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","confirmCopy result="+result
				+", confirmResult="+confirmCopyResult+", syncProfileConfirmRequired="+syncProfileConfirmRequired);

		return result;
	};
	
	final private int mirrorDeleteLocalFile(String masterUrl, String targetUrl) {
		SmbFile hf;
		File lf;

		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","mirrorDeleteLocalFile master=", masterUrl,
						", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		try {
			lf = new File(targetUrl);
			String t_dir="";
			if (lf.isDirectory()) t_dir=lf.getPath();
				else t_dir=lf.getParent();
			if (!isDirExcluded(t_dir.replace(localUrl, "")+"/") &&
					isDirectoryToBeProcessed(t_dir.replace(localUrl, "")+"/")){ 
				if (lf.isDirectory()) { // Directory Delete
					lf = new File(targetUrl + "/");
					hf = new SmbFile(masterUrl,ntlmPasswordAuth);
					if (hf.exists()) {
						File[] children = lf.listFiles();
						for (File element : children) {
							String tmp = element.getName();
							if (tmp.lastIndexOf("/")>0) 
								tmp=tmp.substring(0,tmp.lastIndexOf("/"));
							mirrorDeleteLocalFile(masterUrl + "/"
									+ tmp,targetUrl + "/" + tmp);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
					} else {
						// remote Dir was not found, delete local dir
						if (confirmDelete(targetUrl)) {
							deleteLocalItem(true,targetUrl); 
//							mHistoryDeletedList.add(targetUrl);
						} else {
							addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				} else { // file Delete
					hf = new SmbFile(masterUrl,ntlmPasswordAuth);
					if (glblParms.debugLevel>=3) 
						addDebugLogMsg(3,"I","Remote file exists="+hf.exists());
					if (!hf.exists() ) {
						String m_dir=targetUrl.replace(syncLocalDir+"/","");
						if (!(m_dir.indexOf("/")<0 && !syncMasterDirFileProcess)) { 
							if (confirmDelete(targetUrl)) {
								deleteLocalItem(true,targetUrl);
//								mHistoryDeletedList.add(targetUrl);
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			return -1;
		} catch (SmbException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			return -1;
		}
		return deleteCount;
	};

//	private boolean isRootDirFile(String fp) {
//		
//		String t_fp=fp.replace("/", "");
//		Log.v("","tu="+fp+". td="+t_fp);
//		if (t_fp.indexOf("/")>0) return false;
//		else return true;
//		
//	};
	
	final private int mirrorMoveRemoteToLocal(boolean allcopy, String masterUrl,
			String targetUrl, ArrayList<String> moved_dirs) {
		SmbFile hf;
		File lf;
		
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","mirrorMoveFileToLocal_Copy from=", masterUrl, ", to=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();
		String tmp_target="";
		try {
			hf = new SmbFile(masterUrl,ntlmPasswordAuth);
			if (hf.exists()) {
				if (hf.isDirectory()) { // Directory copy
					if (hf.canRead() && 
							isDirectoryToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))) {
						hf = new SmbFile(masterUrl + "/",ntlmPasswordAuth);
						try {
							SmbFile[] children = hf.listFiles();
							for (SmbFile element : children) {
								String tmp = element.getName();
								if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
								mirrorMoveRemoteToLocal(allcopy, masterUrl + "/"+ tmp,
										targetUrl + "/"+ tmp,moved_dirs);
								if (checkErrorStatus()!=0) return checkErrorStatus();
							}
						} catch (SmbException e) {
							addLogMsg("W","","SmbException occured during SmbFile#listFiles(), name="+masterUrl+
									", jcifs error="+e.getMessage());
						}
					}
				} else { // file copy
					if (isDirFiltered(masterUrl.replace(remoteUrl, "")) &&
							isFileFiltered(masterUrl)) {
						createLocalDir(targetUrl,moved_dirs,masterUrl);
						lf = new File(targetUrl);
						String t_fn=hf.getName().replace("/", "");
						String t_fp=masterUrl.replace("smb://"+syncRemoteAddr, "");
						if (isFileChanged(t_fp,lf,hf,allcopy)) {
							// copy
							if (confirmCopy(targetUrl)) {
								long file_byte=hf.length();
								if (glblParms.settingLocalFileCopyByRename) {
									tmp_target=makeTempFilePath(targetUrl);
								}
								copyFileRemoteToLocal(hf,lf,file_byte,t_fn,t_fp, tmp_target);
								updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
										targetUrl,hf.lastModified());
								if (checkErrorStatus()!=0) return checkErrorStatus();
//								mHistoryCopiedList.add(targetUrl);
								if (!lf.setLastModified(hf.lastModified())) {
									if (glblParms.debugLevel>=1) 
										addDebugLogMsg(1,"E","setLastModified() was failed. File name=", targetUrl);
								}
//								if (isMediaStoreDir(lf.getParent()))
								scanMediaStoreLibrary(targetUrl);
								copyCount++;
								
								// delete master file
								if (confirmDelete(masterUrl)) {
									addMovedDirList(moved_dirs,hf.getParent());
									deleteRemoteItem(true,masterUrl);
//									mHistoryDeletedList.add(masterUrl);
									addMsgToProgDlg(false,"I",t_fn,msgs_mirror_prof_file_deleted);
								} else {
									addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
								}
							} else {
								addLogMsg("W",targetUrl,msgs_mirror_confirm_copy_cancel);
							}
						} else {
							updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
									targetUrl,hf.lastModified());
							if (confirmDelete(masterUrl)) {
								// delete master file
								addMovedDirList(moved_dirs,hf.getParent());
								deleteRemoteItem(true,masterUrl);
//								mHistoryDeletedList.add(masterUrl);
								addMsgToProgDlg(false,"I",t_fn,msgs_mirror_prof_file_deleted);
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			} else {
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"E","remote file ", masterUrl,
							" was not copied, because file/dir not found");
				addMsgToProgDlg(true,"E",masterUrl,msgs_mirror_prof_master_not_found);
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				return -1;
				
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (SmbException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (UnknownHostException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (FileNotFoundException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (IOException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		}
		return 0;
	};

	private String makeTempFilePath(String  targetUrl) {
		String tmp_wu="";
		String last_sep="";
		if (targetUrl.endsWith("/")) {
			tmp_wu=targetUrl.substring(0,(targetUrl.length()-1));
			last_sep="/";
		} else tmp_wu=targetUrl;
		String target_dir=tmp_wu.substring(0,tmp_wu.lastIndexOf("/"));
		target_dir=target_dir.substring(0,target_dir.lastIndexOf("/"))+"/";
		String target_fn=tmp_wu.replace(target_dir, "");
		target_fn=target_fn.substring(0,(target_fn.length()-1));
		String tmp_target=target_dir+"SMBSync.work.tmp"+last_sep;
//		Log.v("","tmp="+tmp_target+", to="+targetUrl);
		return tmp_target;
	};
	
	final private int mirrorMoveLocalToRemote(boolean allcopy, String masterUrl,
			String targetUrl, ArrayList<String> moved_dirs) {
		SmbFile hf=null;
		File lf;
		
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(2,"I","mirrorMoveLocalToRemote master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();

		String tmp_target="";
		
		try {
			lf = new File(masterUrl);
			if (lf.exists()) {
				if (lf.isDirectory()) { // Directory copy
					if (lf.canRead() && 
							isDirectoryToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))) {
						File[] children = lf.listFiles();
						for (File element : children) {
							String tmp = element.getName();
							if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
							mirrorMoveLocalToRemote(allcopy, masterUrl + "/"
									+ tmp,targetUrl + "/" + tmp, moved_dirs);
							if (checkErrorStatus()!=0) return checkErrorStatus();
						}
					}
				} else { // file copy
					if (isDirFiltered(targetUrl.replace(localUrl, "")) &&
							isFileFiltered(masterUrl)) {
						createRemoteDir(targetUrl,moved_dirs,masterUrl);
						lf = new File(masterUrl);
						hf = new SmbFile(targetUrl,ntlmPasswordAuth);
						String t_fn=lf.getName().replace("/","");
//						String t_fp=masterUrl.replace(SMBSync_External_Root_Dir, "");
						String t_fp=masterUrl.replace(localUrl, "");
						if (isFileChangedForLocalToRemote(masterUrl,lf,hf,allcopy)) {
							// copy done
							if (confirmCopy(targetUrl)) {
								long file_byte=lf.length();
								
								if (glblParms.settingRemoteFileCopyByRename) {
									tmp_target=makeTempFilePath(targetUrl);
								} 
								copyFileLocalToRemote(lf,hf,file_byte,t_fn,t_fp, tmp_target);
								if (checkErrorStatus()!=0) {
									return checkErrorStatus();
								}
								try {
									if (!syncProfileNotUseLastModifiedForRemote)
										hf.setLastModified(lf.lastModified());
								} catch(SmbException e) {
									addLogMsg("W",targetUrl,
											glblParms.svcContext.getString(R.string.msgs_mirror_prof_remote_file_set_last_modified_failed));
									addDebugLogMsg(1,"W",targetUrl,
											"Remote file setLastModified() failed, reason="+ e.getMessage());
								}

//								updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
//										masterUrl,hf.getLastModified());
								copyCount++;
								
								if (confirmDelete(masterUrl)) {
									// delete master file
									addMovedDirList(moved_dirs,lf.getParent());
									deleteLocalItem(true,masterUrl);
//									mHistoryDeletedList.add(masterUrl);
									addMsgToProgDlg(false,"I",t_fn,msgs_mirror_prof_file_deleted);
								} else {
									addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
								}
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_copy_cancel);								
							}
						} else {
							if (confirmDelete(masterUrl)) {
								// delete master file
								addMovedDirList(moved_dirs,lf.getParent());
								deleteLocalItem(true,masterUrl);
//								mHistoryDeletedList.add(masterUrl);
								addMsgToProgDlg(false,"I",t_fn,msgs_mirror_prof_file_deleted);
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			} else {
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"E","Local file ", masterUrl,
							" was not copied, because file/dir not existed.");
				addLogMsg("I",masterUrl,msgs_mirror_prof_master_not_found);
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				return -1;
				
			}
		} catch (MalformedURLException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		} catch (SmbException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		} catch (UnknownHostException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		} catch (FileNotFoundException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		} catch (IOException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteRemoteTempFile(tmp_target);
			return -1;
		}
		return copyCount;
	};

	final private int mirrorMoveLocalToLocal(boolean allcopy, String masterUrl,
			String targetUrl, String target_fp_base, ArrayList<String> moved_dirs) {
		File tf;
		File mf;
		
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(2,"I","mirrorMoveLocalToLocal master=", masterUrl,
				", target=", targetUrl);
		if (checkErrorStatus()!=0) return checkErrorStatus();
		String tmp_target="";
		try {
			mf = new File(masterUrl);
			if (mf.exists()) {
				if (mf.isDirectory()) { // Directory copy
					if (mf.canRead() && 
							isDirectoryToBeProcessed(masterUrl.replace(mirrorIoRootDir, ""))) {
						File[] children = mf.listFiles();
						for (File element : children) {
							String tmp = element.getName();
							if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
							String n_master=(masterUrl + "/"+ tmp).replaceAll("//", "/");
							String n_target=(targetUrl + "/" + tmp).replaceAll("//", "/");;
//							Log.v("","n_master="+n_master+", n_target="+n_target);
//							Log.v("","target_fp_base="+target_fp_base);
							if (!n_master.equals(target_fp_base)) {
								mirrorMoveLocalToLocal(allcopy, n_master, n_target, target_fp_base,
										moved_dirs);
								if (checkErrorStatus()!=0) return checkErrorStatus();
							} else {
//								isExceptionOccured=true;
								addLogMsg("W","",
										String.format(msgs_mirror_same_directory_ignored,n_master));
//								break;
							}
						}
					}
				} else { // file copy
					if (isDirFiltered(targetUrl.replace(localUrl, "")) &&
							isFileFiltered(masterUrl)) {
						createLocalDir(targetUrl,moved_dirs,masterUrl);
						mf = new File(masterUrl);
						tf = new File(targetUrl);
						String t_fn=mf.getName().replace("/","");
//						String t_fp=masterUrl.replace(SMBSync_External_Root_Dir, "");
						String t_fp=masterUrl.replace(localUrl, "");
//						if (isFileChanged(masterUrl,mf,tf,allcopy)) {
						if (isFileChanged(targetUrl,tf,mf,allcopy)) {							
							// copy done
							if (confirmCopy(targetUrl)) {
								long file_byte=mf.length();
								if (glblParms.settingLocalFileCopyByRename) {
									tmp_target=makeTempFilePath(targetUrl);
								}
								copyFileLocalToLocal(mf,tf,file_byte,t_fn,t_fp,tmp_target);
								if (checkErrorStatus()!=0) return checkErrorStatus();
//								mHistoryCopiedList.add(targetUrl);
								tf.setLastModified(mf.lastModified());
								updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
										targetUrl,mf.lastModified());
								copyCount++;
								
								if (confirmDelete(masterUrl)) {
									// delete master file
									addMovedDirList(moved_dirs,mf.getParent());
									deleteLocalItem(true,masterUrl);
//									mHistoryDeletedList.add(masterUrl);
									addMsgToProgDlg(false,"I",t_fn,msgs_mirror_prof_file_deleted);
								} else {
									addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
								}
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_copy_cancel);								
							}
						} else {
							if (confirmDelete(masterUrl)) {
								// delete master file
								addMovedDirList(moved_dirs,mf.getParent());
								deleteLocalItem(true,masterUrl);
//								mHistoryDeletedList.add(masterUrl);
								addMsgToProgDlg(false,"I",t_fn,msgs_mirror_prof_file_deleted);
							} else {
								addLogMsg("W",masterUrl,msgs_mirror_confirm_delete_cancel);
							}
						}
						if (checkErrorStatus()!=0) return checkErrorStatus();
					}
				}
			} else {
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"E","Local file ", masterUrl,
							" was not copied, because file/dir not existed.");
				addLogMsg("I",masterUrl,msgs_mirror_prof_master_not_found);
				tcMirror.setThreadMessage(msgs_mirror_prof_master_not_found+","+masterUrl);
				return -1;
				
			}
		} catch (MalformedURLException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (SmbException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (UnknownHostException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (FileNotFoundException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		} catch (IOException e) {
			printStackTraceElement(e.getStackTrace());
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","From="+masterUrl+", To="+targetUrl);
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
			return -1;
		}
		return copyCount;
	};

	final private boolean createRemoteDir(String targetUrl,
			ArrayList<String> moved_dirs,String masterUrl) 
					throws MalformedURLException, SmbException {
		boolean result=false;
		String target_dir="",master_dir="";
		if (targetUrl.lastIndexOf("/")<=0) return false;
		else target_dir=targetUrl.substring(0,targetUrl.lastIndexOf("/"));
		if (masterUrl.lastIndexOf("/")<=0) return false;
		else master_dir=masterUrl.substring(0,masterUrl.lastIndexOf("/"));

		SmbFile hf = new SmbFile(target_dir + "/",ntlmPasswordAuth);
		if (!hf.exists()) {
			hf.mkdirs();
			if (moved_dirs!=null) addMovedDirList(moved_dirs,master_dir);
			addLogMsg("I",target_dir,msgs_mirror_prof_dir_create);
		}
		return result;
	};
	
	final private boolean createLocalDir(String targetUrl,
			ArrayList<String> moved_dirs,String masterUrl) {
		boolean result=false;
		String target_dir="",master_dir="";
		if (targetUrl.lastIndexOf("/")<=0) return false;
		else target_dir=targetUrl.substring(0,targetUrl.lastIndexOf("/"));
		if (masterUrl.lastIndexOf("/")<=0) return false;
		else master_dir=masterUrl.substring(0,masterUrl.lastIndexOf("/"));
		
		File lf = new File(target_dir);
		if (!lf.exists()) {
			lf.mkdirs();
			if (moved_dirs!=null) addMovedDirList(moved_dirs,master_dir + "/");
			addLogMsg("I",target_dir,msgs_mirror_prof_dir_create);
			result=true;
		}
		return result;
	};

	final private int copyFileLocalToRemote(File in_file, SmbFile out_file, 
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		long fileReadBytes = 0;
		long readBeginTime = System.currentTimeMillis();
		int bufferReadBytes=0;
		boolean out_file_exits=out_file.exists();
		SmbFile tmp_out=null;
		SmbFileOutputStream out=null;
		if (!tmp_target.equals("")) {
			tmp_out=new SmbFile(tmp_target, ntlmPasswordAuth);
			out=new SmbFileOutputStream(tmp_out);
		} else {
			out=new SmbFileOutputStream(out_file);
		}
		FileInputStream in=new FileInputStream(in_file);
//		BufferedInputStream in=new BufferedInputStream(fis,4096*512);
//		BufferedOutputStream out=new BufferedOutputStream(fos,4096*512);
		while ((bufferReadBytes = in.read(mirrorIoBuffer)) > 0) {
			out.write(mirrorIoBuffer, 0, bufferReadBytes);
			fileReadBytes += bufferReadBytes;
			if (file_byte>fileReadBytes) {
				addMsgToProgDlg(false,"I",t_fn,
					String.format(msgs_mirror_prof_file_copying,(fileReadBytes*100)/file_byte));
			}
			if (!tcMirror.isEnable()) {
				in.close();
				out.flush();
				out.close();
				if (tmp_out!=null && tmp_out.exists()) tmp_out.delete();
				return -10;
			}
		}
		long readElapsedTime = System.currentTimeMillis() - readBeginTime;
		in.close();
		out.flush();
		out.close();
		
		if (tmp_out!=null) {
			if (out_file.exists()) out_file.delete();
			tmp_out.renameTo(out_file);
		}
		
		String tmsg="";
		if (out_file_exits) tmsg=msgs_mirror_prof_file_replaced;
		else tmsg=msgs_mirror_prof_file_copied;
		addMsgToProgDlg(false,"I",t_fn,tmsg);
		if (glblParms.settingShowSyncDetailMessage) addLogMsg("I",t_fp,tmsg);

		totalTransferByte+=fileReadBytes;
		totalTransferTime+=readElapsedTime;
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I",t_fp+" "+fileReadBytes + " bytes transfered in ",
				readElapsedTime+" mili seconds at "+calTransferRate(fileReadBytes,readElapsedTime));
		return 0;
	};

	final private int copyFileLocalToLocal(File in_file, File out_file,
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		long fileReadBytes = 0;
		long readBeginTime = System.currentTimeMillis();
		int bufferReadBytes=0;
		boolean out_file_exits=out_file.exists();
		FileInputStream in=new FileInputStream(in_file);
		FileOutputStream out=null;
		File tmp_out=null;
		if (!tmp_target.equals("")) {
			tmp_out=new File(tmp_target);	
			out=new FileOutputStream(tmp_out);
		} else {
			out=new FileOutputStream(out_file);
		}
		
//		BufferedInputStream in=new BufferedInputStream(fis,4096*512);
//		BufferedOutputStream out=new BufferedOutputStream(fos,4096*512);
		while ((bufferReadBytes = in.read(mirrorIoBuffer)) > 0) {
			out.write(mirrorIoBuffer, 0, bufferReadBytes);
			fileReadBytes += bufferReadBytes;
			if (file_byte>fileReadBytes) {
				addMsgToProgDlg(false,"I",t_fn,
					String.format(msgs_mirror_prof_file_copying,(fileReadBytes*100)/file_byte));
			}
			if (!tcMirror.isEnable()) {
				in.close();
				out.flush();
				out.close();
				if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
				return -10;
			}
		}
		long readElapsedTime = System.currentTimeMillis() - readBeginTime;
		if (readElapsedTime==0) readElapsedTime=1;
		in.close();
		out.flush();
		out.close();
		
		if (tmp_out!=null) {
			if (out_file.exists()) out_file.delete();
			tmp_out.renameTo(out_file);
		}

		String tmsg="";
		if (out_file_exits) tmsg=msgs_mirror_prof_file_replaced;
		else tmsg=msgs_mirror_prof_file_copied;
		addMsgToProgDlg(false,"I",t_fn,tmsg);
		if (glblParms.settingShowSyncDetailMessage) addLogMsg("I",t_fp,tmsg);
		totalTransferByte+=fileReadBytes;
		totalTransferTime+=readElapsedTime;
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I",t_fp+" "+fileReadBytes + " bytes transfered in ",
				readElapsedTime+" mili seconds at "+calTransferRate(fileReadBytes,readElapsedTime));
		return 0;
	};

	final private int copyFileRemoteToLocal(SmbFile in_file, File out_file, 
			long file_byte, String t_fn, String t_fp, String tmp_target) throws IOException {
		long readBeginTime = System.currentTimeMillis();
		long fileReadBytes = 0;
		int bufferReadBytes=0;
		boolean out_file_exits=out_file.exists();
		SmbFileInputStream in=new SmbFileInputStream(in_file);
		FileOutputStream out=null;
		File tmp_out=null;
		if (!tmp_target.equals("")) {
			tmp_out=new File(tmp_target);	
			out=new FileOutputStream(tmp_out);
		} else {
			out=new FileOutputStream(out_file);
		}
		
//		BufferedInputStream in=new BufferedInputStream(fis,4096*512);
//		BufferedOutputStream out=new BufferedOutputStream(fos,4096*512);
		while ((bufferReadBytes = in.read(mirrorIoBuffer)) > 0) {
			out.write(mirrorIoBuffer, 0, bufferReadBytes);
			fileReadBytes += bufferReadBytes;
			if (file_byte>fileReadBytes) {
				addMsgToProgDlg(false,"I",t_fn,
					String.format(msgs_mirror_prof_file_copying,(fileReadBytes*100)/file_byte));
			}
			if (!tcMirror.isEnable()) {
				in.close();
				out.flush();
				out.close();
				if (!tmp_target.equals("")) deleteLocalTempFile(tmp_target);
				return -10;
			}
		}
		long readElapsedTime = System.currentTimeMillis() - readBeginTime;
		in.close();
		out.flush();
		out.close();
		
		if (tmp_out!=null) {
			if (out_file.exists()) out_file.delete();
			tmp_out.renameTo(out_file);
		}

		String tmsg="";
		if (out_file_exits) tmsg=msgs_mirror_prof_file_replaced;
		else tmsg=msgs_mirror_prof_file_copied;
		addMsgToProgDlg(false,"I",t_fn,tmsg);
		if (glblParms.settingShowSyncDetailMessage) addLogMsg("I",t_fp,tmsg);

		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I",t_fp+" "+fileReadBytes + " bytes transfered in "
					+ readElapsedTime + " mili seconds at "+ 
					calTransferRate(fileReadBytes,readElapsedTime));
		totalTransferByte+=fileReadBytes;
		totalTransferTime+=readElapsedTime;

		return 0;
	};

	static final private void addMovedDirList(ArrayList<String> moved_dirs,String path) {
		if (moved_dirs.size()!=0) {
			boolean found=false;
			for (int i=0;i<moved_dirs.size();i++) {
				if (moved_dirs.get(i).equals(path)) found=true;
			}
			if (!found) {
				moved_dirs.add(path);
			}
		} else {
			moved_dirs.add(path);
		}
	};
	
	final private void compileFilter(ArrayList<String> ff, ArrayList<String> df) {
		int flags = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
		String ffinc="", ffexc="",dfinc="",dfexc="";
		if (ff.size()!=0) {
			String prefix, filter, cni="", cne="";
			for (int j=0;j<ff.size();j++) {
				prefix=ff.get(j).substring(0,1);
				filter=ff.get(j).substring(1,ff.get(j).length());
				
				if (prefix.equals("I")) {
					ffinc = ffinc+cni+convertRegExp(filter);
					cni="|";
				} else {
					ffexc = ffexc+cne+convertRegExp(filter);
					cne="|";
				}
			}
		}
		dirIncludeFilterList.clear();
		dirExcludeFilterList.clear();
		if (df.size()!=0) {
			String prefix, filter, cni="", cne="";
			for (int j=0;j<df.size();j++) {
				prefix=df.get(j).substring(0,1);
//				filter=mirrorIoRootDir+
				filter="/"+df.get(j).substring(1,df.get(j).length())+"/";
				createDirFilterList(prefix,filter);
				if (prefix.equals("I")) {
					dfinc = dfinc+cni+convertRegExp(filter);
					cni="|";
				} else {
					dfexc = dfexc+cne+convertRegExp(filter);
					cne="|";
				}
			}
		}

		fileFilterInclude = fileFilterExclude = null;
		dirFilterInclude = dirFilterExclude = null;
		if (ffinc.length() != 0) fileFilterInclude = Pattern.compile("(" + ffinc + ")", flags);
		if (ffexc.length() != 0) fileFilterExclude = Pattern.compile("(" + ffexc + ")", flags);
		if (dfinc.length() != 0) dirFilterInclude = Pattern.compile("(" + dfinc + ")", flags);
		if (dfexc.length() != 0) dirFilterExclude = Pattern.compile("(" + dfexc + ")", flags);

	};
	
	final private void createDirFilterList(String prefix, String filter) {
		int flags = Pattern.CASE_INSENSITIVE | Pattern.MULTILINE;
		String[] filter_array=filter.split("/");
		Pattern[] pattern_array=new Pattern[filter_array.length];
		
		for (int k=0;k<filter_array.length;k++) 
			pattern_array[k] = 
				Pattern.compile("^"+convertRegExp(filter_array[k]), flags);
		
		boolean[] pattern_notreg=new boolean[pattern_array.length];
		
		for (int k=0;k<pattern_array.length;k++){ 
			if (pattern_array[k].toString().equals(filter_array[k])) 
				 pattern_notreg[k]=true;  //non regular expression
			else pattern_notreg[k]=false; //regular expression
		}
		
		if (prefix.equals("I")) {
			dirIncludeFilterList.add(pattern_array);
		} else {
//			Log.v("","filter="+filter+", conv="+convertRegExp(filter)+
//					", comp="+Pattern.compile(convertRegExp(filter), flags));
			dirExcludeFilterList.add(
					Pattern.compile(convertRegExp(filter), flags));
		}
	};
	
	final private boolean isFileChanged(String fp, File lf, SmbFile hf, boolean ac) 
			throws SmbException {
		long hf_time=0, hf_length=0;
		boolean hf_exists = hf.exists();
		
		if (hf_exists) {
			hf_time=hf.lastModified();
			hf_length=hf.length();
		}
		return isFileChangedDetailCompare(fp, lf, 
				hf_exists, hf_time, hf_length, ac);
	};

	final private boolean isFileChanged(String fp, File mf, File tf, boolean ac) 
			throws SmbException {
		long tf_time=0, tf_length=0;
		boolean tf_exists = tf.exists();
		
		if (tf_exists) {
			tf_time=tf.lastModified();
			tf_length=tf.length();
		}
		return isFileChangedDetailCompare(fp, mf, 
				tf_exists, tf_time, tf_length, ac);
	};

	final private boolean isFileChangedDetailCompare(String fp, File lf, 
			boolean hf_exists, long hf_time, long hf_length, 
			boolean ac) 
			throws SmbException {
		boolean diff=false;
		long lf_time=0, lf_length=0;
		boolean lf_exists = lf.exists();
		boolean exists_diff=false;
		
		if (lf_exists) {
			lf_time=lf.lastModified();
			lf_length=lf.length();
		}
		long time_diff=Math.abs((hf_time-lf_time));
		long length_diff=Math.abs((hf_length-lf_length));
		long time_diff_tz1=Math.abs(hf_time-(lf_time-timeZone));
//		long diff_tz_2=Math.abs(hf_time-(lf_time-(timeZone*2)));

		if (hf_exists!=lf_exists) exists_diff=true;
		if (exists_diff || length_diff>0 || ac) {
			if (!syncProfileUseJavaLastModified) {//Use lastModified
				if (lf_exists) {
					updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
							lf.getPath(), hf_time); 
				} else {
					boolean updated=
							updateLocalFileLastModifiedList(currentFileLastModifiedList,newFileLastModifiedList,
									lf.getPath(), hf_time);
					if (!updated)
						addLastModifiedItem(currentFileLastModifiedList,newFileLastModifiedList,
								lf.getPath(), lf_time,hf_time); 
				}
			}
			diff=true;
		} else {//Check lastModified()
			if (syncProfileUseJavaLastModified) {//Use lastModified
				if (time_diff>timeDifferenceLimit) { //LastModified was changed
					if (isMediaStoreDir(lf.getParent())) {//MediaStore Directory
						if (settingsMediaStoreUseLastModTime.equals("0")) diff=true;// 
						else {
							if (settingsMediaStoreUseLastModTime.equals("1")) {
							//Check lastModified and TimeZone
								if (time_diff_tz1>timeDifferenceLimit) {
									diff=true;
									if (glblParms.debugLevel>=1) 
										addDebugLogMsg(1,"W",
												"TimeZone does not matched. different(ms) time="+
												time_diff);
								} else diff=false;
							} else {// Ignore lastModified time
								diff=false;
//								mHistoryIgnoredList.add(fp);
								ignoreCount++;
								if (!isMediaStoreChangeWarningIssued) {
									addLogMsg("W","",msgs_mirror_prof_file_bypass_media_store_change);
									isMediaStoreChangeWarningIssued=true;
								}
								if (glblParms.debugLevel>=1) 
									addDebugLogMsg(1,"W",
										"Was ignored the difference between the last ",
										"update time of local and remote files.",
										" time_diff="+time_diff+", fn=",fp);
							}
						}
					} else {//not MediaStore file, lastModified was changed ->do copy
						diff=true;
					}
				} else diff=false;
			} else {//Use Filelist
				String lfp=lf.getPath();
				diff=isLocalFileLastModifiedWasDifferent(
						currentFileLastModifiedList,
						newFileLastModifiedList,
						lfp, lf_time,hf_time); 
//				Log.v("","lfp="+lfp+", lf_time="+lf_time+", hf_time="+hf_time);
			}
		}
		if (glblParms.debugLevel>=3) { 
			addDebugLogMsg(3,"I","isFileChangedDetailCompare");
			if (hf_exists) addDebugLogMsg(3,"I","Remote file length="+hf_length+
						", last modified(ms)="+hf_time+
						", date="+DateUtil.convDateTimeTo_YearMonthDayHourMinSec((hf_time/1000)*1000));
			else addDebugLogMsg(3,"I","Remote file was not exists");
			if (lf_exists) addDebugLogMsg(3,"I","Local  file length="+lf_length+
					", last modified(ms)="+lf_time+
					", date="+DateUtil.convDateTimeTo_YearMonthDayHourMinSec((lf_time/1000)*1000));
			else addDebugLogMsg(3,"I","Local  file was not exists");
			addDebugLogMsg(3,"I","allcopy="+ac+",exists_diff="+exists_diff+
					",time_diff="+time_diff+", time_zone_diff="+time_diff_tz1+
					",length_diff="+length_diff+", diff="+diff);
		}
		return diff;
	};

	final private boolean isFileChangedForLocalToRemote(
			String fp, File lf, SmbFile hf, boolean ac) 
			throws SmbException {
		boolean diff=false;
		long hf_time=0, hf_length=0;
		boolean hf_exists = hf.exists();
		
		if (hf_exists) {
			hf_time=hf.lastModified();
			hf_length=hf.length();
		}
		long lf_time=0, lf_length=0;
		boolean lf_exists = lf.exists();
		boolean exists_diff=false;
		
		if (lf_exists) {
			lf_time=lf.lastModified();
			lf_length=lf.length();
		}
		long time_diff=Math.abs((hf_time-lf_time));
		long length_diff=Math.abs((hf_length-lf_length));
		long time_diff_tz1=Math.abs(hf_time-(lf_time-timeZone));
//		long diff_tz_2=Math.abs(hf_time-(lf_time-(timeZone*2)));

		if (hf_exists!=lf_exists) exists_diff=true;
		if (exists_diff || length_diff>0 || ac) {
			diff=true;
		} else {//Check lastModified()
			if (!syncProfileNotUseLastModifiedForRemote) {
				if (time_diff>timeDifferenceLimit) { //LastModified was changed
					diff=true;
				} else diff=false;
			}
		}
		if (glblParms.debugLevel>=3) { 
			addDebugLogMsg(3,"I","isFileChangedForLocalToRemote");
			if (hf_exists) addDebugLogMsg(3,"I","Remote file length="+hf_length+
						", last modified(ms)="+hf_time+
						", date="+DateUtil.convDateTimeTo_YearMonthDayHourMinSec((hf_time/1000)*1000));
			else addDebugLogMsg(3,"I","Remote file was not exists");
			if (lf_exists) addDebugLogMsg(3,"I","Local  file length="+lf_length+
					", last modified(ms)="+lf_time+
					", date="+DateUtil.convDateTimeTo_YearMonthDayHourMinSec((lf_time/1000)*1000));
			else addDebugLogMsg(3,"I","Local  file was not exists");
			addDebugLogMsg(3,"I","allcopy="+ac+",exists_diff="+exists_diff+
					",time_diff="+time_diff+", time_zone_diff="+time_diff_tz1+
					",length_diff="+length_diff+", diff="+diff);
		}
		return diff;
	};

	final private boolean isLocalFileLastModifiedWasDifferent(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String fp, long l_lm, long r_lm) {
		boolean result=LocalFileLastModified.isCurrentListWasDifferent(
				curr_last_modified_list, new_last_modified_list,
				fp,l_lm,r_lm,timeDifferenceLimit);
		if (glblParms.debugLevel>=3) 
			addDebugLogMsg(3,"I","isLocalFileLastModifiedWasDifferent result="+result+", item="+fp);
		return result;
	};
	
	final private void deleteLocalFileLastModifiedEntry(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String fp){
		LocalFileLastModified.deleteLastModifiedItem(
				curr_last_modified_list, new_last_modified_list, fp);
		if (glblParms.debugLevel>=3) 
			addDebugLogMsg(3,"I","deleteLocalFileLastModifiedEntry entry="+fp);

	};
	
	final private boolean updateLocalFileLastModifiedList(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String targetUrl, long r_lm) {
		return LocalFileLastModified.updateLastModifiedList(
				curr_last_modified_list, new_last_modified_list, targetUrl, r_lm);
	};
	
	final private void addLastModifiedItem(
			ArrayList<FileLastModifiedEntryItem> curr_last_modified_list,
			ArrayList<FileLastModifiedEntryItem> new_last_modified_list,
			String targetUrl, long l_lm, long r_lm) {
		LocalFileLastModified.addLastModifiedItem(
				curr_last_modified_list, new_last_modified_list, targetUrl, l_lm, r_lm);
		if (glblParms.debugLevel>=3) 
			addDebugLogMsg(3,"I","addLastModifiedItem entry="+targetUrl);
	};
	
	final private boolean isSetLastModifiedFunctional(String lmp) {
		boolean result=
				LocalFileLastModified.isSetLastModifiedFunctional(lmp);
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I","isSetLastModifiedFunctional result="+result+", lmp="+lmp);
		return result;
	};

	final private void saveLocalFileLastModifiedList(String lmp) {
		if (syncProfileUseJavaLastModified) return;
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I","saveLocalFileLastModifiedList mp="+lmp+", curr size="+
					currentFileLastModifiedList.size()+", add size="+newFileLastModifiedList.size());
		LocalFileLastModified.saveLastModifiedList(
				lmp, currentFileLastModifiedList, newFileLastModifiedList);
	};
	
	final private void saveLocalFileLastModifiedListCache(String lmp) {
		if (lmp.equals("")) return; 
		boolean found=false;
		int idx=0;
		for (int i=0;i<mLocalFileLastModifiedCache.size();i++) {
			if (mLocalFileLastModifiedCache.get(i).mount_point_name.equals(lmp)) {
				found=true;
				idx=i;
				break;
			}
		}
		LocalFileLastModifiedListCacheItem cli=null;
		if (found) {
			cli=mLocalFileLastModifiedCache.get(idx);
			cli.cureent_list.clear();
			cli.cureent_list.addAll(currentFileLastModifiedList);
			cli.new_list.clear();
			cli.new_list.addAll(newFileLastModifiedList);
			cli.mount_point_name=lmp;
		} else {
			cli=new LocalFileLastModifiedListCacheItem();
			cli.mount_point_name=lmp;
			cli.cureent_list.addAll(currentFileLastModifiedList);
			cli.new_list.addAll(newFileLastModifiedList);
			mLocalFileLastModifiedCache.add(cli);
		}
	}
	final private void loadLocalFileLastModifiedList(String lmp) {
		if (syncProfileUseJavaLastModified) return;
		if (!loadedLocalMountPoint.equals(lmp)) {
			saveLocalFileLastModifiedListCache(loadedLocalMountPoint);
			boolean hit=false;
			for (int i=0;i<mLocalFileLastModifiedCache.size();i++) {
				if (mLocalFileLastModifiedCache.get(i).mount_point_name.equals(lmp)) {
					hit=true;
					currentFileLastModifiedList.clear();
					currentFileLastModifiedList.addAll(mLocalFileLastModifiedCache.get(i).cureent_list);
					newFileLastModifiedList.clear();
					newFileLastModifiedList.addAll(mLocalFileLastModifiedCache.get(i).new_list);
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(1,"I","loadLocalFileLastModifiedList cache hit. mp="+lmp);
					break;
				}
			}
			if (!hit) {
				LocalFileLastModified.loadLastModifiedList(
					lmp, currentFileLastModifiedList, newFileLastModifiedList);
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"I","loadLocalFileLastModifiedList list loaded. mp="+lmp);
			}
			loadedLocalMountPoint=lmp;
			if (glblParms.debugLevel>=1) 
				addDebugLogMsg(1,"I","loadLocalFileLastModifiedList mp="+lmp+", list size current="+
						currentFileLastModifiedList.size()+", added="+newFileLastModifiedList.size());		
		} else {
			if (glblParms.debugLevel>=1) 
				addDebugLogMsg(1,"I","loadLocalFileLastModifiedList already loaded. mp="+lmp);
		}
	};
	
	final private boolean isMediaStoreDir(String path) {
		boolean found=false;
		if(Build.VERSION.SDK_INT >= 11) {
			//android 3.1 以上は常にMediaScanを行う
			found=true;
		} else {
			if (mediaStoreImageList.size()!=0) {
				if (Collections.binarySearch(mediaStoreImageList, path)>=0) 
					found=true;
			}
			if (!found && mediaStoreAudioList.size()!=0) {
				if (Collections.binarySearch(mediaStoreAudioList, path)>=0) 
					found=true;
			}
			if (!found && mediaStoreVideoList.size()!=0) {
				if (Collections.binarySearch(mediaStoreVideoList, path)>=0) 
					found=true;
			}
		}
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I","isMediaStoreDir="+found+",dir="+path);
		return found;
	};

	final private boolean isFileFiltered(String url) {
		boolean filtered = false;
		Matcher mt;
		
		if (!syncMasterDirFileProcess) {//「root直下のファイルは処理しないオプション」が有郊
			String tmp_d=url.replace(mirrorIoRootDir+"/", "");
			if (tmp_d.indexOf("/")<0) {
				//root直下なので処理しない
				if (glblParms.debugLevel>=3) 
					addDebugLogMsg(3,"I","Filefilter not filtered, " +
							"because Master Dir not processed was effective");
				return false;
			}
		}
		
		String temp_fid = url.substring(url.lastIndexOf("/") + 1, url.length());
		if (fileFilterInclude == null) {
			// nothing filter
			filtered = true;
		} else {
			mt = fileFilterInclude.matcher(temp_fid);
			if (mt.find()) filtered = true;
			if (glblParms.debugLevel>=3) 
				addDebugLogMsg(3,"I","Filefilter Include result:"+filtered);
		}
		if (fileFilterExclude==null) {
			//nop
		} else {
			mt = fileFilterExclude.matcher(temp_fid);
			if (mt.find()) filtered=false;
			if (glblParms.debugLevel>=3) 
				addDebugLogMsg(3,"I","Filefilter Exclude result:"+filtered);
		}
		if (glblParms.debugLevel>=3) 
			addDebugLogMsg(3,"I","Filefilter result:"+filtered);
		return filtered;
	};

	final private boolean isDirFiltered(String url) {
		boolean filtered = false;
		Matcher mt;
		
//		String tmp_d=url.replace(mirrorIoRootDir+"/","");
//		String tmp_d=url;
		if (url.equals(mirrorIoRootDir)) {
			//not filtered
			filtered = true;
		} else {
			if (dirFilterInclude == null) {
				// nothing filter
				filtered = true;
			} else {
				mt = dirFilterInclude.matcher(url);
				if (mt.find()) {
					filtered = true;
				}
				if (glblParms.debugLevel>=2) 
					addDebugLogMsg(2,"I","Dirfilter Include result:"+filtered);
			}
			if (dirFilterExclude==null) {
				//nop
			} else {
				mt = dirFilterExclude.matcher(url);
				if (mt.find()) {
					filtered=false;
				}
				if (glblParms.debugLevel>=2) 
					addDebugLogMsg(2,"I","Dirfilter Exclude result:"+filtered);
			}
			if (glblParms.debugLevel>=2) 
				addDebugLogMsg(2,"I","Dirfilter result:"+filtered);
		}
		return filtered;
	};

	final private boolean isDirExcluded(String fp) {
		boolean result=false;
		
		Matcher mt;
		
		if (dirFilterExclude==null) {
			//nop
		} else {
			mt = dirFilterExclude.matcher(fp);
			if (mt.find()) {
				result=true;
			}
		}
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","isDirExcluded result:"+result);
		
		return result;
	}

	final private boolean isDirectoryToBeProcessed(String dir) {
		boolean inc=false, exc=false, result=false;
		
		String filter_dir;
		if (dir.length()!=0) {
			if (dir.endsWith("/")) filter_dir=dir;
			else filter_dir=dir+"/";
			String[] dir_array=filter_dir.split("/");
			if (dirIncludeFilterList.size()==0) inc=true;
			else {
				for (int i=0;i<dirIncludeFilterList.size();i++) {
					Pattern[] pattern_array=dirIncludeFilterList.get(i);
					boolean found=true;
					for (int j=0;j<Math.min(dir_array.length,pattern_array.length);j++) {
						Matcher mt = pattern_array[j].matcher(dir_array[j]);
						if (dir_array[j].length()!=0) {
							if (!mt.find()) {
								found=false;
//								sendDebugLogMsg(0,"I","dir_array="+dir_array[j]+
//										", pattern="+pattern_array[j]);
								break;
							} 
						}
					}
					if (found) {
						inc=true;
						break;
					}
				}
			}
			if (dirExcludeFilterList.size()==0) exc=false;
			else {
				exc=false;
				for (int i=0;i<dirExcludeFilterList.size();i++) {
					Pattern filter_pattern=dirExcludeFilterList.get(i);
					Matcher mt = filter_pattern.matcher(filter_dir);
					if (mt.find()) {
						exc=true;
						break;
					}
					if (exc) break;
				}
			}
			
			if (exc) result=false;
				else if (inc) result=true;
					else result=false;
		} else {
			result=true;
			inc=exc=false;
		}
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","isDirectoryToBeProcessed"+
				" include="+inc+", exclude="+exc+", result="+result+", fp="+dir);
		return result;
	}

	final private String convertRegExp(String filter) {

		if (filter==null || filter.equals("")) return "";
		
		// 正規表現に変換
		String out = "";

		for (int i = 0; i < filter.length(); i++) {
			String temp = filter.substring(i, i + 1);
			if (temp.equals(";")) {// 区切り文字
				if ((i + 1) > filter.length()) {
					// 終了
					break;
				} else {
					out = out + "|";
				}
			} else if (temp.equals("\\")) {
				out = out + "\\\\";
			} else if (temp.equals("*")) {
				out = out + ".*";
			} else if (temp.equals(".")) {
				out = out + "\\.";
			} else if (temp.equals("?")) {
				out = out + ".";
			} else if (temp.equals("+")) {
				out = out + "\\+";
			} else if (temp.equals("{")) {
				out = out + "\\{";
			} else if (temp.equals("}")) {
				out = out + "\\}";
			} else if (temp.equals("(")) {
				out = out + "\\(";
			} else if (temp.equals(")")) {
				out = out + "\\)";
			} else if (temp.equals("[")) {
				out = out + "\\[";
			} else if (temp.equals("]")) {
				out = out + "\\]";
			} else if (temp.equals("^")) {
				out = out + "\\^";
			} else if (temp.equals("$")) {
				out = out + "\\$";
			} else if (temp.equals("[")) {
				out = out + "\\[";
			} else
				out = out + temp;
		}
		if (glblParms.debugLevel>=2) 
			addDebugLogMsg(2,"I","convertRegExp out="+out+", in="+filter);
		return out;

	};

	final private void notifyThreadTerminate() {
		mUtil.flushLogFile();
		notifyEvent.notifyToListener(true, null);
	};

	final private int deleteRemoteItem(boolean deldir, String url) {
		SmbFile sf;

		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(2,"I","deleteRemoteItem=" + url);

		try {
			sf = new SmbFile(url,ntlmPasswordAuth);
			if (deldir) { 
				deleteRemoteFile("", sf); //delete specified dir
			} else { 
				deleteCount=deleteRemoteFile(sf.getPath(), sf); //not delete specified dir
			}
			

		} catch (MalformedURLException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","url="+url);
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			return -1;
		}
		return deleteCount;
	};

	final private int deleteRemoteFile(String rootpath, SmbFile hf) {
		try {
			if (hf.isDirectory()) {// ディレクトリの場合
				// ディレクトリにあるすべてのファイルを処理する
				String[] children = (new SmbFile(hf.getPath()+"/",ntlmPasswordAuth)).list();
				for (int i = 0; i < children.length; i++) {
					deleteRemoteFile(rootpath, 
						(new SmbFile(hf.getPath()+"/"+children[i],ntlmPasswordAuth)));
					if (checkErrorStatus()!=0) return checkErrorStatus();
				}
			}
			// 削除
			if (rootpath.equals(hf.getPath())) {
				//root dirなので削除しない
			} else {
				String t_dir=hf.getPath();
				String t_fn=hf.getName().replaceAll("/", "");
				SmbFile hfd = new SmbFile(t_dir+"/",ntlmPasswordAuth);
				boolean td=hfd.isDirectory();
				hfd.delete();
				String t_prf="smb://"+syncRemoteAddr;
				if (td) {
					addMsgToProgDlg(false,"I",t_fn,msgs_mirror_prof_dir_deleted);
					if (glblParms.settingShowSyncDetailMessage) 
						addLogMsg("I",t_dir.replace(t_prf, ""),msgs_mirror_prof_dir_deleted);
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"Remote directory was deleted:"+hfd.getPath().substring(0,hfd.getPath().length()-1));
				}
				else{ 
					addMsgToProgDlg(false,"I",t_fn,msgs_mirror_prof_file_deleted);
					if (glblParms.settingShowSyncDetailMessage) 
						addLogMsg("I",t_dir.replace(t_prf, ""),msgs_mirror_prof_file_deleted);
					if (glblParms.debugLevel>=1) 
						addDebugLogMsg(1,"I",
							"Remote file was deleted:"+hfd.getPath().substring(0,hfd.getPath().length()-1));
				}
				deleteCount++;
				if (checkErrorStatus()!=0) return checkErrorStatus();
			}
		} catch (MalformedURLException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			return -1;
		} catch (SmbException e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			return -1;
		}
		return deleteCount;
	};

	final private int deleteLocalItem(boolean deldir, String url) {
		File sf;

		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(2,"I","deleteLocalItem=" + url);

		try {
			sf = new File(url);
			if (deldir) 
				deleteLocalFile("", sf); // delete specified dir
			else 
				deleteLocalFile(sf.getPath(), sf); //not delete specified dir

		} catch (Exception e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			addLogMsg("E","","url="+url);
			printStackTraceElement(e.getStackTrace());
			isExceptionOccured=true;
			tcMirror.setThreadMessage(e.getMessage());
			return -1;
		}
		return 0;
	};

	final private int deleteLocalFile(String rootpath, File lf) {
		if (lf.isDirectory()) {// ディレクトリの場合
			String[] children = lf.list();// ディレクトリにあるすべてのファイルを処理する
			for (int i = 0; i < children.length; i++) {
				deleteLocalFile(rootpath,(new File(lf, children[i])));
				if (checkErrorStatus()!=0) return checkErrorStatus();
			}
		}
		// 削除
		if (rootpath.equals(lf.getPath())) {
			//root dirなので削除しない
		} else {
			boolean td=lf.isDirectory();
			lf.delete();
			deleteLocalFileLastModifiedEntry(
					currentFileLastModifiedList,newFileLastModifiedList,lf.getPath());
			deleteCount++;
			if (td) {
				addMsgToProgDlg(false,"I",lf.getName(),msgs_mirror_prof_dir_deleted);
				if (glblParms.settingShowSyncDetailMessage)  
					addLogMsg("I",lf.getPath().replace(localUrl,""),msgs_mirror_prof_dir_deleted);
//								lf.getPath().replace(SMBSync_External_Root_Dir,"")));
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"I",
						"Local directory was deleted:"+lf.getPath());

			} else {
//				Log.v("","Dir="+lf.getParent()+", path="+lf.getPath());
				deleteMediaStoreItem(lf.getPath());
				addMsgToProgDlg(false,"I",lf.getName(),msgs_mirror_prof_file_deleted);
				if (glblParms.settingShowSyncDetailMessage)  
					addLogMsg("I",lf.getPath().replace(localUrl,""),msgs_mirror_prof_file_deleted);
//								lf.getPath().replace(SMBSync_External_Root_Dir,"")));
				if (glblParms.debugLevel>=1) 
					addDebugLogMsg(1,"I",
						"Local file was deleted:"+lf.getPath());

			}
			if (checkErrorStatus()!=0) return checkErrorStatus();
			return 0;
		}
		
		return 0;
	};
	
	final private boolean isRemoteDirEmpty(String url) {
		SmbFile hf;
		boolean dirEmpty=false;

		try {
			hf = new SmbFile(url,ntlmPasswordAuth);
			String[] list=hf.list();
			if (list.length==0) dirEmpty=true; 
		} catch (Exception e) {
			addLogMsg("E","",e.getMessage());//e.toString());
			isExceptionOccured=true;
			printStackTraceElement(e.getStackTrace());
			tcMirror.setThreadMessage(e.getMessage());
			return false;
		}
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I","isRemoteDirEmpty=" + url+", empty="+dirEmpty);
		return dirEmpty;
	};

	final private boolean isLocalDirEmpty(String url) {
		File lf;
		boolean dirEmpty=false;

		try {
			lf = new File(url);
			String[] list=lf.list();
			if (list==null) dirEmpty=true; 
			else if (list!=null && list.length==0) dirEmpty=true;
		} catch (Exception e) {
			isExceptionOccured=true;
			addLogMsg("E","",e.getMessage());//e.toString());
			printStackTraceElement(e.getStackTrace());
			tcMirror.setThreadMessage(e.getMessage());
			return false;
		}
		if (glblParms.debugLevel>=1) 
			addDebugLogMsg(1,"I","isLocalDirEmpty=" + url+", empty="+dirEmpty);
		return dirEmpty;
	};
	
	final private void addMsgToProgDlg(boolean log, String log_cat, String fp, String log_msg) {
		mUtil.addMsgToProgDlg(log, log_cat, syncProfName, fp, log_msg);
	};

	final private void addDebugLogMsg(int lvl, String log_cat, String...log_msg) {
		mUtil.addDebugLogMsg(lvl, log_cat, syncProfName, log_msg);
	};
	
	final private void addLogMsg(String log_cat, String fp, String log_msg) {
		mUtil.addLogMsg(log_cat, syncProfName, fp, log_msg);
		if (log_cat.equals("W")) totalWarningMsgCnt++;
		else if (log_cat.equals("E")) totalErrorMsgCnt++;
	};

	
	static private String	msgs_mirror_prof_file_deleted	;
	static private String	msgs_mirror_prof_started	;
	static private String	msgs_mirror_prof_invalid_mirror_type	;
	static private String	msgs_mirror_prof_was_failed	;
	static private String	msgs_mirror_prof_no_of_copy	;
	static private String	msgs_mirror_prof_avg_rate	;
	static private String	msgs_mirror_prof_success_end	;
	static private String	msgs_mirror_prof_was_cancelled	;
	static private String	msgs_mirror_prof_file_copied	;
	static private String	msgs_mirror_prof_file_replaced	;
	static private String	msgs_mirror_prof_master_not_found;
	static private String	msgs_mirror_prof_dir_deleted	;
	static private String	msgs_mirror_prof_dir_create	;
	static private String  msgs_mirror_prof_file_copying;
	static private String  msgs_mirror_prof_file_bypass_media_store_change;
	static private String  msgs_mirror_task_result_stats;
	static private String  msgs_mirror_task_started;
	static private String  msgs_mirror_task_ended;
	static private String  msgs_mirror_prof_ms_different_file_last_mod;
	static private String  msgs_mirror_prof_ms_read_error;
	static private String  msgs_mirror_prof_sync_local_mount_point_unavailable;
	static private String msgs_mirror_confirm_copy_cancel;
	static private String msgs_mirror_confirm_delete_cancel;
	static private String msgs_mirror_task_result_error_ignored;
	static private String msgs_mirror_task_result_error_ended;
	static private String msgs_mirror_task_result_ok;
	static private String msgs_mirror_task_result_cancel;
	static private String msgs_mirror_master_local_mount_point_not_readable;
	static private String msgs_mirror_target_local_mount_point_not_writable;
	static private String msgs_mirror_same_directory_ignored;
	
	static private void loadMsgString(GlobalParameters glblParms) {

		msgs_mirror_task_result_ok=glblParms.svcContext.getString(R.string.msgs_mirror_task_result_ok);
		msgs_mirror_task_result_cancel=glblParms.svcContext.getString(R.string.msgs_mirror_task_result_cancel);
		msgs_mirror_task_result_error_ignored=glblParms.svcContext.getString(R.string.msgs_mirror_task_result_error_ignored);
		msgs_mirror_task_result_error_ended=glblParms.svcContext.getString(R.string.msgs_mirror_task_result_error_ended);
		msgs_mirror_confirm_copy_cancel=glblParms.svcContext.getString(R.string.msgs_mirror_confirm_copy_cancel);
		msgs_mirror_confirm_delete_cancel=glblParms.svcContext.getString(R.string.msgs_mirror_confirm_delete_cancel);
		
		msgs_mirror_prof_sync_local_mount_point_unavailable=glblParms.svcContext.getString(R.string.msgs_mirror_prof_sync_local_mount_point_unavailable);
        msgs_mirror_prof_ms_different_file_last_mod=glblParms.svcContext.getString(R.string.msgs_mirror_prof_ms_different_file_last_mod);
        msgs_mirror_prof_ms_read_error=glblParms.svcContext.getString(R.string.msgs_mirror_prof_ms_read_error);

		msgs_mirror_task_started=glblParms.svcContext.getString(R.string.msgs_mirror_task_started);
		msgs_mirror_task_ended=glblParms.svcContext.getString(R.string.msgs_mirror_task_ended);
		msgs_mirror_task_result_stats=glblParms.svcContext.getString(R.string.msgs_mirror_task_result_stats);
		msgs_mirror_prof_file_bypass_media_store_change=glblParms.svcContext.getString(R.string.msgs_mirror_prof_file_bypass_media_store_change);
		msgs_mirror_prof_file_deleted=glblParms.svcContext.getString(R.string.msgs_mirror_prof_file_deleted);
		msgs_mirror_prof_started=glblParms.svcContext.getString(R.string.msgs_mirror_prof_started);
		msgs_mirror_prof_invalid_mirror_type=glblParms.svcContext.getString(R.string.msgs_mirror_prof_invalid_mirror_type);
		msgs_mirror_prof_was_failed=glblParms.svcContext.getString(R.string.msgs_mirror_prof_was_failed);
		msgs_mirror_prof_no_of_copy=glblParms.svcContext.getString(R.string.msgs_mirror_prof_no_of_copy);
		msgs_mirror_prof_avg_rate=glblParms.svcContext.getString(R.string.msgs_mirror_prof_avg_rate);
		msgs_mirror_prof_success_end=glblParms.svcContext.getString(R.string.msgs_mirror_prof_success_end);
		msgs_mirror_prof_was_cancelled=glblParms.svcContext.getString(R.string.msgs_mirror_prof_was_cancelled);
		msgs_mirror_prof_file_copied=glblParms.svcContext.getString(R.string.msgs_mirror_prof_file_copied);
		msgs_mirror_prof_file_replaced=glblParms.svcContext.getString(R.string.msgs_mirror_prof_file_replaced);
		msgs_mirror_prof_master_not_found=glblParms.svcContext.getString(R.string.msgs_mirror_prof_master_not_found);
		msgs_mirror_prof_dir_deleted=glblParms.svcContext.getString(R.string.msgs_mirror_prof_dir_deleted);
		msgs_mirror_prof_dir_create=glblParms.svcContext.getString(R.string.msgs_mirror_prof_dir_create);
		msgs_mirror_prof_file_copying=glblParms.svcContext.getString(R.string.msgs_mirror_prof_file_copying);
		
		msgs_mirror_master_local_mount_point_not_readable=glblParms.svcContext.getString(R.string.msgs_mirror_master_local_mount_point_not_readable);
		msgs_mirror_target_local_mount_point_not_writable=glblParms.svcContext.getString(R.string.msgs_mirror_target_local_mount_point_not_writable);
		
		msgs_mirror_same_directory_ignored=glblParms.svcContext.getString(R.string.msgs_mirror_same_directory_ignored);
		
		return ;
				
	}
}
class MirrorIoParmList {
	private String mp_profname=""; 
	private String mp_master_type="";
	private String mp_target_type="";
	private String mp_target_name="";
	private String mp_mirror_type="";
	private String mp_remote_addr="";
	private String mp_remote_host="";
	private String mp_remote_share="";
	private String mp_remote_dir="";
	private String mp_local_dir="";
	private String mp_local_mount_point="";
	private String mp_master_local_dir="";
	private String mp_master_local_mount_point="";
	private String mp_target_local_dir="";
	private String mp_target_local_mount_point="";
	private String mp_remote_userid="";
	private String mp_remote_pass="";
	private ArrayList<String> mp_file_filter=new ArrayList<String>();
	private ArrayList<String> mp_dir_filter=new ArrayList<String>();
	private boolean mp_master_dir_proc =true;
	private boolean mp_confirm_required =true;
	private boolean mp_force_last_modified_use_smbsync =true;
	private boolean mp_not_use_last_modified_for_remote =false;
	
	public MirrorIoParmList (
			String profname,
			String master_type,
			String target_type,
			String target,
			String type,
			String addr,
			String host,
			String share,
			String r_dir,
			String lmp,
			String l_dir, 
			String user,
			String pass,
			ArrayList<String> ff,
			ArrayList<String> df,
			boolean mdp,
			boolean conf,
			boolean ujlm,
			boolean nulm_remote) {

		mp_profname=profname;
		mp_master_type=master_type;
		mp_target_type=target_type;
		mp_target_name=target;
		mp_mirror_type=type;
		mp_remote_addr=addr;
		mp_remote_host=host;
		mp_remote_share=share;
		mp_remote_dir=r_dir;
		mp_local_mount_point=lmp;
		mp_local_dir=l_dir;
		mp_remote_userid=user;
		mp_remote_pass=pass;
		mp_file_filter=ff;
		mp_dir_filter=df;
		mp_master_dir_proc=mdp;
		mp_confirm_required=conf;
		mp_force_last_modified_use_smbsync=ujlm;
		mp_not_use_last_modified_for_remote=nulm_remote;
	}

	public String getProfname() { return mp_profname;}
	public String getMasterType() { return mp_master_type;}
	public String getTargetType() { return mp_target_type;}
	public String getTargetName() { return mp_target_name;}
	public String getMirrorType() { return mp_mirror_type;}
	public String getRemoteAddr() { return mp_remote_addr;}
	public String getHostName()   { return mp_remote_host;}
	public String getRemoteShare() { return mp_remote_share;}
	public String getRemoteDir() { return mp_remote_dir;}
	public String getLocalDir() { return mp_local_dir;}
	public String getLocalMountPoint() { return mp_local_mount_point;}
	public String getMasterLocalDir() { return mp_master_local_dir;}
	public String getMasterLocalMountPoint() { return mp_master_local_mount_point;}
	public String getTargetLocalDir() { return mp_target_local_dir;}
	public String getTargetLocalMountPoint() { return mp_target_local_mount_point;}
	public String getRemoteUserid() { return mp_remote_userid;}
	public String getRemotePass() { return mp_remote_pass;}
	public ArrayList<String> getFileFilter() { return mp_file_filter;}
	public ArrayList<String> getDirFilter() { return mp_dir_filter;}
	public boolean isMasterDirFileProcessed() {return mp_master_dir_proc;}
	public boolean isConfirmRequired() {return mp_confirm_required;}
	public boolean isForceLastModifiedUseSmbsync() {return mp_force_last_modified_use_smbsync;}
	public boolean isNotUseLastModifiedForRemote() {return mp_not_use_last_modified_for_remote;}
	public void setNotUseLastModifiedForRemote(boolean p) {mp_not_use_last_modified_for_remote=p;}
	
	public void setProfname(String p) { mp_profname=p;}
	public void setMasterType(String p) { mp_master_type=p;}
	public void setTargetType(String p) { mp_target_type=p;}
	public void setTargetName(String p) { mp_target_name=p;}
	public void setMirrorType(String p) { mp_mirror_type=p;}
	public void setRemoteAddr(String p) { mp_remote_addr=p;}
	public void setHostName(String p)   { mp_remote_host=p;}
	public void setRemoteShare(String p) { mp_remote_share=p;}
	public void setRemoteDir(String p) { mp_remote_dir=p;}
	public void setLocalDir(String p) { mp_local_dir=p;}
	public void setLocalMountPoint(String p) { mp_local_mount_point=p;}
	public void setMasterLocalDir(String p) { mp_master_local_dir=p;}
	public void setMasterLocalMountPoint(String p) { mp_master_local_mount_point=p;}
	public void setTargetLocalDir(String p) { mp_target_local_dir=p;}
	public void setTargetLocalMountPoint(String p) { mp_target_local_mount_point=p;}
	public void setRemoteUserid(String p) { mp_remote_userid=p;}
	public void setRemotePass(String p) { mp_remote_pass=p;}
	public void setFileFilter(ArrayList<String> p) { mp_file_filter=p;}
	public void setDirFilter(ArrayList<String> p) { mp_dir_filter=p;}
	public void setMasterDirFileProcessed(boolean p) {mp_master_dir_proc=p;}
	public void setConfirmRequired(boolean p) {mp_confirm_required=p;}
	public void setForceLastModifiedUseSmbsync(boolean p) {mp_force_last_modified_use_smbsync=p;}
}

class LocalFileLastModifiedListCacheItem {
	String mount_point_name;
	ArrayList<FileLastModifiedEntryItem> cureent_list=
			new ArrayList<FileLastModifiedEntryItem>();
	ArrayList<FileLastModifiedEntryItem> new_list=
			new ArrayList<FileLastModifiedEntryItem>();
}
