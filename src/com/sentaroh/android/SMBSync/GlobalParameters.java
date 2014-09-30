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

import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP;

import java.io.PrintWriter;
import java.util.ArrayList;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.support.v4.app.NotificationCompat.BigTextStyle;
import android.support.v4.app.NotificationCompat.Builder;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class GlobalParameters extends Application{
	
	public Context svcContext=null;
	
	public PrintWriter logWriter=null;
	public int logLineCount=0;
	public String currentLogFilePath="";
	
	public boolean activityIsForeground=true;
	
	public boolean enableMainUi=true;
	
	public boolean mirrorThreadActive=false;
	
	public boolean supressAutoStart=false;
	
	public Handler uiHandler=null;
	
	public TextView mainViewScheduleInfo=null;
	
	public AdapterProfileList profileAdapter=null;
	public ListView profileListView=null;

	public AdapterMessageList msgListAdapter=null;
	public ListView msgListView=null;
	
	public String SMBSync_External_Root_Dir="/mnt/sdcard";
	public boolean externalStorageIsMounted=false;
	public String SMBSync_Internal_Root_Dir="";
	
	public String profilePassword="";
	public final String profilePasswordPrefix="*SMBSync*";
	
	public NotificationManager notificationManager;
	public int notificationIcon=R.drawable.ic_48_smbsync_wait;
	public Notification notification;
	public Builder notificationBuilder;
	public BigTextStyle notificationBigTextStyle;
	public Intent notificationIntent;
	public PendingIntent notificationPendingIntent;
	public String notificationLastShowedMessage=null, notificationLastShowedTitle="";
	public String notificationAppName="";
//	public boolean notiifcationEnabled=false;
	
//	public ISvcCallback callBackStub=null;
	
	public ArrayList<MirrorIoParmList> mirrorIoParms=null;
	
	public ListView syncHistoryListView=null;
	public AdapterSyncHistory syncHistoryAdapter=null;
	public ArrayList<SyncHistoryListItem> syncHistoryList=null;
	
	//Message view
	public boolean freezeMessageViewScroll=false;
	
	public boolean sampleProfileCreateRequired=false;
	
	//Parameters from Settings menu
	public int debugLevel=0;
	public boolean settingAutoStart=false,
			settingAutoTerm=false, settingErrorOption=false,
			settingDebugMsgDisplay=false,
			settingMediaFiles,
			settingScanExternalStorage,
			settingExitClean,
			settingBackgroundExecution=false;
	public String settingScreenOnOption=KEEP_SCREEN_ON_WHEN_SCREEN_UNLOCKED;
	public static final String KEEP_SCREEN_ON_DISABLED="0";
	public static final String KEEP_SCREEN_ON_ALWAYS="1";
	public static final String KEEP_SCREEN_ON_WHEN_SCREEN_UNLOCKED="2";
	public boolean settingWifiLockRequired=true;
	public String settingWifiOption=SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP;
//	public boolean settingAltUiEnabled=true;
	public boolean settingShowSyncDetailMessage=true;
	public boolean settingShowRemotePortOption=true;
	public String settingLogOption="0";
	public String settingLogMsgDir="/SMBSync/";
	
	public String settingBgTermNotifyMsg="0";
	public static final String BG_TERM_NOTIFY_MSG_ALWAYS = "0";
	public static final String BG_TERM_NOTIFY_MSG_ERROR = "1";
	public static final String BG_TERM_NOTIFY_MSG_NO = "2";

	public String settingLogMsgFilename="";
	public int settiingLogGeneration=1;
	public boolean settingExportedProfileEncryptRequired=true;
//	public boolean settingInternalProfileEncryptRequired=true;
	
	public static final String VIBRATE_WHEN_SYNC_ENDED_NONE="0";
	public static final String VIBRATE_WHEN_SYNC_ENDED_ALWAYS="1";
	public static final String VIBRATE_WHEN_SYNC_ENDED_SUCCESS="2";
	public static final String VIBRATE_WHEN_SYNC_ENDED_ERROR="3";
	public String settingVibrateWhenSyncEnded=VIBRATE_WHEN_SYNC_ENDED_ALWAYS;
	
	public static final String PB_RINGTONE_WHEN_SYNC_ENDED_NONE="0";
	public static final String PB_RINGTONE_WHEN_SYNC_ENDED_ALWAYS="1";
	public static final String PB_RINGTONE_WHEN_SYNC_ENDED_SUCCESS="2";
	public static final String PB_RINGTONE_WHEN_SYNC_ENDED_ERROR="3";
	public String settingRingtoneWhenSyncEnded=PB_RINGTONE_WHEN_SYNC_ENDED_ALWAYS;
	
	public boolean settingRemoteFileCopyByRename=false;
	public boolean settingLocalFileCopyByRename=true;
	
    public LinearLayout confirmView=null;
    public TextView confirmTitle=null;
    public TextView confirmMsg=null;
    public Button confirmCancel=null;
    public OnClickListener confirmCancelListener=null;
    public Button confirmYes=null;
    public OnClickListener confirmYesListener=null;
    public Button confirmNo=null;
    public OnClickListener confirmNoListener=null;
    public Button confirmYesAll=null;
    public OnClickListener confirmYesAllListener=null;
    public Button confirmNoAll=null;
    public OnClickListener confirmNoAllListener=null;

    public LinearLayout progressBarView=null;
    public TextView progressBarMsg=null;
    public ProgressBar progressBarPb=null;
    public Button progressBarCancel=null;
    public OnClickListener progressBarCancelListener=null;
    public Button progressBarImmed=null;
    public OnClickListener progressBarImmedListener=null;

    public LinearLayout progressSpinView=null;
    public TextView progressSpinSyncprof=null;
    public TextView progressSpinFilePath=null;
    public TextView progressSpinStatus=null;
    public Button progressSpinCancel=null;
    public OnClickListener progressSpinCancelListener=null;


}
