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
import android.widget.ListView;
import android.widget.TextView;

public class GlobalParameters extends Application{
	
	public Context svcContext=null;
	
	public PrintWriter logWriter=null;
	public String currentLogFilePath="";
	
	public boolean activityIsForeground=true;
	
	public Handler uiHandler=null;
	public TextView mainViewProgressProf=null;
	public TextView mainViewProgressFilepath=null;
	public TextView mainViewProgressMessage=null;
	public AdapterMessageList msgListAdapter=null;
	public ListView msgListView=null;
	
	public String SMBSync_External_Root_Dir="/mnt/sdcard";
	public boolean externalStorageIsMounted=false;
	public String SMBSync_Internal_Root_Dir="";
	
	public NotificationManager notificationManager;
	public boolean isNotificationWasShowed=false;
	public int notificationIcon=R.drawable.ic_48_smbsync_wait;
	public Notification notification;
	public Builder notificationBuilder;
	public BigTextStyle notificationBigTextStyle;
	public Intent notificationIntent;
	public PendingIntent notificationPendingIntent;
	public String notificationLastShowedMessage=null, notificationLastShowedTitle="";
	public String notificationAppName="";
//	public boolean notiifcationEnabled=false;
	
	public ISvcCallback callBackStub=null;
	
	public ArrayList<MirrorIoParmList> mirrorIoParms=null;
	
	//Message view
	public boolean freezeMessageViewScroll=false;
	
	public boolean sampleProfileCreateRequired=false;
	
	//Parameters from Settings menu
	public int debugLevel=0;
	public String settingUsername, settingPassword,settingAddr;
	public boolean settingAutoStart=false,
			settingAutoTerm=false, settingErrorOption=false,
			settingDebugMsgDisplay=false,
			settingMediaFiles,
			settingScanExternalStorage,
			settingExitClean,
			settingBackgroundExecution=false;
	public boolean settingScreenOnEnabled=true;
	public boolean settingWifiLockRequired=true;
	public String settingWifiOption=SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP;
	public boolean settingAltUiEnabled=true;
	public String settingLogOption="0";
	public String settingLogMsgDir="/SMBSync/";
	public String settingBgTermNotification="0";
	public String settingLogMsgFilename="SMBSync_log_yyyy-mm-dd.txt";
	public int settiingLogGeneration=1;
	public boolean settingExportedProfileEncryptRequired=true;
	
}
