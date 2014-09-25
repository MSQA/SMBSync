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
import static com.sentaroh.android.SMBSync.SchedulerConstants.*;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Locale;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.ClipboardManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

import com.sentaroh.android.Utilities.CallBackListener;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenuItem.CustomContextMenuOnCleanupListener;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenuItem.CustomContextMenuOnClickListener;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;

@SuppressWarnings("deprecation")
@SuppressLint({"SimpleDateFormat" })
public class SMBSyncMain extends FragmentActivity {
	
	private final static int ATERM_WAIT_TIME = 30;
	
	private boolean isTaskTermination = false; // kill is disabled(enable is kill by onDestroy)
	
	@SuppressWarnings("unused")
	private boolean isApplicationFirstTimeRunning=false;
	
	private String packageVersionName="Not found"; 

	private String currentViewType ="P";

	private TabHost tabHost;
	private Context mContext;
	
	private static GlobalParameters mGp=null;
	private ProfileUtility profUtil=null;
	
	private static SMBSyncUtil util=null;
	private CustomContextMenu ccMenu = null;
	
	private boolean 
			isExtraSpecAutoStart=false,
			isExtraSpecAutoTerm=false,
			isExtraSpecBgExec=false;
	private String[] extraValueSyncProfList=null;
	private boolean extraValueAutoStart=false,
			extraValueAutoTerm=false,
			extraValueBgExec=false;
	
	private int restartStatus=0;
	
	private ServiceConnection mSvcConnection=null;
	private ThreadCtrl tcService=null;
    private CommonDialog commonDlg=null;
    private static Handler mUiHandler=new Handler();
	private WifiManager.WifiLock mWifiLock=null;

	private Locale mCurrentLocal=null;
	
	@Override  
	protected void onSaveInstanceState(Bundle outState) {  
		super.onSaveInstanceState(outState);
		util.addDebugLogMsg(1, "I", "onSaveInstanceState entered.");
		outState.putString("currentViewType", currentViewType);
	};  

	@Override  
	protected void onRestoreInstanceState(Bundle savedInstanceState) {  
		super.onRestoreInstanceState(savedInstanceState);
		util.addDebugLogMsg(1, "I", "onRestoreInstanceState entered.");
		restartStatus=2;
		currentViewType=savedInstanceState.getString("currentViewType");
	};
 
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
//		StrictMode.enableDefaults();
//		setTheme(android.R.style.Theme_Dialog);
//		setTheme(android.R.style.Theme_Holo_Light);
//		setTheme(android.R.style.Theme_Light);
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);

		mCurrentLocal=getResources().getConfiguration().locale;
		
		setContentView(R.layout.main);
		mContext=this;
		mGp=(GlobalParameters) getApplication();
		mGp.enableMainUi=true;
		mGp.uiHandler=new Handler();
		mGp.SMBSync_External_Root_Dir=LocalMountPoint.getExternalStorageDir();
		
        startService(new Intent(mContext, SMBSyncService.class));

		mDimScreenWakelock=((PowerManager)getSystemService(Context.POWER_SERVICE))
	    			.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
//	    				 PowerManager.PARTIAL_WAKE_LOCK
	    				| PowerManager.ACQUIRE_CAUSES_WAKEUP
//	   	    				| PowerManager.ON_AFTER_RELEASE
	    				, "SMBSync-ScreenOn");
		
		mWifiLock=((WifiManager)mContext.getSystemService(Context.WIFI_SERVICE))
				.createWifiLock(WifiManager.WIFI_MODE_FULL, "SMBSync-wifi");

		if (tcService==null) tcService=new ThreadCtrl();

//		if (Build.VERSION.SDK_INT>=14)
//			this.getActionBar().setHomeButtonEnabled(false);
		
		if (util==null) util=new SMBSyncUtil(this.getApplicationContext(),"Main", mGp);
		util.setActivityIsForeground(true);

		if (ccMenu ==null) ccMenu = new CustomContextMenu(getResources(),getSupportFragmentManager());
		commonDlg=new CommonDialog(mContext, getSupportFragmentManager());

		ArrayList<MsgListItem> tml =new ArrayList<MsgListItem>();
		mGp.msgListAdapter = new AdapterMessageList(this,R.layout.msg_list_item_view,tml);

		ArrayList<ProfileListItem> tml1 =new ArrayList<ProfileListItem>();
		mGp.profileAdapter=new AdapterProfileList(this, R.layout.profile_list_item_view, tml1,
						mGp.SMBSync_External_Root_Dir);
		currentViewType="P";

		mGp.syncHistoryList=util.loadHistoryList();
		mGp.syncHistoryAdapter=new AdapterSyncHistory(mContext, R.layout.sync_history_list_item_view, 
				mGp.syncHistoryList);
		
		createTabView() ;
		loadMsgString();
		initSettingsParms();
		applySettingParms();
		
		checkExternalStorage();
		mGp.SMBSync_Internal_Root_Dir=getFilesDir().toString();
		
		util.openLogFile();
		
		initAdapterAndView();
		
		util.addDebugLogMsg(1,"I","onCreate entered, "+"resartStatus="+restartStatus+
					", isActivityForeground="+util.isActivityForeground());
		
		initJcifsOption();
		
		getApplVersionName();

		if (profUtil==null) 
			profUtil=new ProfileUtility(util,this, commonDlg,ccMenu, mGp,getSupportFragmentManager());
		
		SchedulerMain.setTimer(mContext, SCHEDULER_INTENT_SET_TIMER_IF_NOT_SET);
//		if (Build.VERSION.SDK_INT >= 19) {
//		    File[] extDirs = getExternalFilesDirs(Environment.DIRECTORY_DOWNLOADS);
//		    for (int i=0;i<extDirs.length;i++)
//				try {
//					Log.v("", "i="+i+", path="+extDirs[i].getPath()+
//							", absolute path="+extDirs[i].getAbsolutePath()+
//							", canonical path="+extDirs[i].getCanonicalPath()+
//							", "+extDirs[extDirs.length-1].toString());
//				} catch (IOException e) {
//					e.printStackTrace();
//				}
//		}
	};
	
	@Override
	protected void onStart() {
		super.onStart();
		
			util.addDebugLogMsg(1,"I","onStart entered, "+"resartStatus="+restartStatus+
					", isActivityForeground="+util.isActivityForeground());

		util.setActivityIsForeground(true);
	};

	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		util.addDebugLogMsg(1,"I","onNewIntent entered, "+"resartStatus="+restartStatus);
		SchedulerMain.setSchedulerInfo(mGp, mContext,null);
		if (restartStatus!=2) {
			if (mGp.mirrorThreadActive) {
				if (isAutoStartRequested(intent)) {
					commonDlg.showCommonDialog(false, "W", "", 
							mContext.getString(R.string.msgs_application_already_started), null);
					util.addLogMsg("W",mContext.getString(R.string.msgs_application_already_started));
				}
			} else {
				if (isAutoStartRequested(intent)) {
					if (!mGp.supressAutoStart) {
	 					util.addDebugLogMsg(1,"I","onNewIntent Auto start data found.");
						isExtraSpecAutoStart=isExtraSpecAutoTerm=isExtraSpecBgExec=false;
						checkAutoStart(intent);
					} else {
						tabHost.setCurrentTab(1);
						util.addLogMsg("W",
								mContext.getString(R.string.msgs_main_auto_start_ignored_by_other_msg));
					}
				}
			}
		} else {
			mPendingRequestIntent=intent;
		}
	};
	
	private Intent mPendingRequestIntent=null;
	
	@Override
	protected void onResume() {
		super.onResume();
		util.addDebugLogMsg(1,"I","onResume entered, "+"resartStatus="+restartStatus+
					", isActivityForeground="+util.isActivityForeground());
		util.setActivityIsForeground(true);
		
		if (restartStatus==1) {
			SchedulerMain.setSchedulerInfo(mGp, mContext,null);
			if (!mGp.freezeMessageViewScroll) {
				mGp.uiHandler.post(new Runnable(){
					@Override
					public void run() {
						mGp.msgListView.setSelection(mGp.msgListAdapter.getCount()-1);
					}
				});
//				setScreenOn();
			}
		} else {
			setUiEnabled();
			NotifyEvent svc_ntfy=new NotifyEvent(mContext);
			svc_ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					svcStartForeground();
					setCallbackListener();
					
					if (restartStatus==0) {
						startupWarning();
						util.addLogMsg("I",msgs_smbsync_main_start+" Version "+packageVersionName );
						showNotificationMsg(msgs_smbsync_main_start+" Version "+packageVersionName );
					} else if (restartStatus==2) {
						restoreTaskData();
						if (currentViewType.equals("M")) tabHost.setCurrentTab(1);
						util.addLogMsg("I",msgs_smbsync_main_restart+" Version "+packageVersionName);
						showNotificationMsg(msgs_smbsync_main_restart+" Version "+packageVersionName);
					}
					listSMBSyncOption();

					deleteTaskData();
					
					setMsglistViewListener();
					setProfilelistItemClickListener();
					setProfilelistLongClickListener();
					setMsglistLongClickListener();
					
					setHistoryViewItemClickListener();
					setHistoryViewLongClickListener();

					SchedulerMain.setSchedulerInfo(mGp, mContext,null);
					
					Intent intent=null;
					if (restartStatus==2) intent=mPendingRequestIntent;
					else intent=getIntent();

					if (enableProfileConfirmCopyDeleteIfRequired()) {
						if (isAutoStartRequested(intent) || mGp.settingAutoStart) {
							isExtraSpecAutoStart=isExtraSpecAutoTerm=isExtraSpecBgExec=false;
							checkAutoStart(intent);
						} else {
							if (mGp.profileAdapter.getItem(0).getType().equals("")) {
								ProfileCreationWizard sw=new ProfileCreationWizard(mGp, mContext, 
											util, profUtil, commonDlg, mGp.profileAdapter);
								sw.wizardMain();
							} else {
								if (LocalFileLastModified.isLastModifiedWasUsed(mGp.profileAdapter))
									checkLastModifiedListValidity();
							}
						}
					}
					
					restartStatus=1;
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {}
			});
			openService(svc_ntfy);
		}
	};

	@Override
	protected void onRestart() {
		super.onRestart();
		
		util.addDebugLogMsg(1,"I","onRestart entered, "+"resartStatus="+restartStatus+
					", isActivityForeground="+util.isActivityForeground());
	};

	@Override
	protected void onPause() {
		super.onPause();
		util.setActivityIsForeground(false);
		
		util.addDebugLogMsg(1,"I","onPause entered "+
					",currentView="+currentViewType+
				", getChangingConfigurations="+getChangingConfigurations()+
				", isActivityForeground="+util.isActivityForeground());
//		if (!isTaskTermination && mSvcClient!=null) {
//		}
		saveTaskData();
	};

	@Override
	protected void onStop() {
		super.onStop();
		
		util.addDebugLogMsg(1,"I","onStop entered, "+
					", isActivityForeground="+util.isActivityForeground());
		util.setActivityIsForeground(false);
//		saveTaskData();
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		util.setActivityIsForeground(false);
		
		util.addDebugLogMsg(1,"I","onDestroy entered, " +
					"isActivityForeground="+util.isActivityForeground()+
					", isFinishing="+isFinishing());
		clearScreenOn();
		relWifiLock();
		unsetCallbackListener();
		deleteTaskData();
		closeService();
		util.flushLogFile();
		//move to service
//		if (mGp.settingExitClean) {
//			System.gc();
//			android.os.Process.killProcess(android.os.Process.myPid());
//			Handler hndl=new Handler();
//			hndl.postDelayed(new Runnable() {
//				@Override
//				public void run() {
//					android.os.Process.killProcess(android.os.Process.myPid());
//				}
//			}, 20);	
//		}
	};
	
	class ViewSaveArea {
		public int current_tab_pos=0;
		public int prof_list_view_pos_x=0,prof_list_view_pos_y=0; 
		public int msg_list_view_pos_x=0,msg_list_view_pos_y=0;
		public int sync_list_view_pos_x=0,sync_list_view_pos_y=0;
		
		public boolean prog_bar_showed=false, prog_spin_showed=false, confirm_showed=false;
		public String prog_prof="", prog_fp="", prog_msg="", prog_sched_info="";
		
		
		public String confirm_title="", confirm_msg="";
		public String progress_bar_msg="";
		public int progress_bar_progress=0, progress_bar_max=0;
		
		public ButtonViewContent confirm_cancel=new ButtonViewContent();
		public ButtonViewContent confirm_yes=new ButtonViewContent();
		public ButtonViewContent confirm_yes_all=new ButtonViewContent();
		public ButtonViewContent confirm_no=new ButtonViewContent();
		public ButtonViewContent confirm_no_all=new ButtonViewContent();
		public ButtonViewContent prog_bar_cancel=new ButtonViewContent();
		public ButtonViewContent prog_bar_immed=new ButtonViewContent();
		public ButtonViewContent prog_spin_cancel=new ButtonViewContent();
	};
	
	class ButtonViewContent {
		public String button_text="";
		public boolean button_visible=true, button_enabled=true, button_clickable=true;
	};
	
	private void saveButtonStatus(Button btn, ButtonViewContent sv) {
		sv.button_text=btn.getText().toString();
		sv.button_clickable=btn.isClickable();
		sv.button_enabled=btn.isEnabled();
		sv.button_visible=btn.isShown();
	};

	private void restoreButtonStatus(Button btn, ButtonViewContent sv, OnClickListener ocl) {
		btn.setText(sv.button_text);
		btn.setClickable(sv.button_clickable);
		btn.setEnabled(sv.button_enabled);
//		if (sv.button_visible) btn.setVisibility(Button.VISIBLE);
//		else btn.setVisibility(Button.GONE);
		btn.setOnClickListener(ocl);
	};

	@Override
	public void onConfigurationChanged(final Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    if (util!=null) {
	    	util.addDebugLogMsg(1,"I","onConfigurationChanged Entered, " ,
	    			"New orientation="+newConfig.orientation+
	    			", Current language=",mCurrentLocal.getLanguage(),
	    			", New language=",newConfig.locale.getLanguage());
	    }
	    if (newConfig.locale.getLanguage().equals("ja")) {
	    	if (!mCurrentLocal.getLanguage().equals("ja")) {//to ja
	    		changeLanguageCode(newConfig);
	    	}
	    } else {
	    	if (mCurrentLocal.getLanguage().equals("ja")) {//to en（Default)
	    		changeLanguageCode(newConfig);
	    	}
	    }
	    final ViewSaveArea vsa=new ViewSaveArea();
	    saveViewContent(vsa);
	    
	    setContentView(R.layout.main);
	    
		createTabView() ;
		initAdapterAndView();
		
		setMsglistViewListener();
		setProfilelistItemClickListener();
		setProfilelistLongClickListener();
		setMsglistLongClickListener();
		
		setHistoryViewItemClickListener();
		setHistoryViewLongClickListener();

		restoreViewContent(vsa);
		
		if (isUiEnabled()) setUiEnabled();
		else setUiDisabled();
		
//		refreshOptionMenu();
	};

	private void saveViewContent(ViewSaveArea vsa) {
	    vsa.current_tab_pos=tabHost.getCurrentTab();
	    
	    vsa.prof_list_view_pos_x=mGp.profileListView.getFirstVisiblePosition();
	    if (mGp.profileListView.getChildAt(0)!=null) vsa.prof_list_view_pos_y=mGp.profileListView.getChildAt(0).getTop();
	    vsa.msg_list_view_pos_x=mGp.msgListView.getFirstVisiblePosition();
	    if (mGp.msgListView.getChildAt(0)!=null) vsa.msg_list_view_pos_y=mGp.msgListView.getChildAt(0).getTop();
	    vsa.sync_list_view_pos_x=mGp.syncHistoryListView.getFirstVisiblePosition();
	    if (mGp.syncHistoryListView.getChildAt(0)!=null) vsa.sync_list_view_pos_y=mGp.syncHistoryListView.getChildAt(0).getTop();
	    
		vsa.prog_prof=mGp.progressSpinSyncprof.getText().toString();
		vsa.prog_fp=mGp.progressSpinFilePath.getText().toString();
		vsa.prog_msg=mGp.progressSpinStatus.getText().toString();
		vsa.progress_bar_progress=mGp.progressBarPb.getProgress();
		vsa.progress_bar_max=mGp.progressBarPb.getMax();
		vsa.prog_sched_info=mGp.mainViewScheduleInfo.getText().toString();

		vsa.prog_bar_showed=mGp.progressBarView.isShown();
		vsa.confirm_showed=mGp.confirmView.isShown();
		vsa.prog_spin_showed=mGp.progressSpinView.isShown();
		
		saveButtonStatus(mGp.confirmCancel,vsa.confirm_cancel);
		saveButtonStatus(mGp.confirmYes,vsa.confirm_yes);
		saveButtonStatus(mGp.confirmYesAll,vsa.confirm_yes_all);
		saveButtonStatus(mGp.confirmNo,vsa.confirm_no);
		saveButtonStatus(mGp.confirmNoAll,vsa.confirm_no_all);
		saveButtonStatus(mGp.progressBarCancel,vsa.prog_bar_cancel);
		saveButtonStatus(mGp.progressSpinCancel,vsa.prog_spin_cancel);
		saveButtonStatus(mGp.progressBarImmed,vsa.prog_bar_immed);
		
		vsa.confirm_title=mGp.confirmTitle.getText().toString();
		vsa.confirm_msg=mGp.confirmMsg.getText().toString();

		vsa.progress_bar_msg=mGp.progressBarMsg.getText().toString();

	};
	
	private void restoreViewContent(ViewSaveArea vsa) {
		tabHost.setCurrentTab(vsa.current_tab_pos);
		mGp.profileListView.setSelectionFromTop(vsa.prof_list_view_pos_x, vsa.prof_list_view_pos_y);
		mGp.msgListView.setSelectionFromTop(vsa.msg_list_view_pos_x, vsa.msg_list_view_pos_y);
		mGp.syncHistoryListView.setSelectionFromTop(vsa.sync_list_view_pos_x, vsa.sync_list_view_pos_y);

		mGp.confirmTitle.setText(vsa.confirm_title);
		mGp.confirmMsg.setText(vsa.confirm_msg);

		restoreButtonStatus(mGp.confirmCancel,vsa.confirm_cancel,mGp.confirmCancelListener);
		restoreButtonStatus(mGp.confirmYes,vsa.confirm_yes,mGp.confirmYesListener);
		restoreButtonStatus(mGp.confirmYesAll,vsa.confirm_yes_all,mGp.confirmYesAllListener);
		restoreButtonStatus(mGp.confirmNo,vsa.confirm_no, mGp.confirmNoListener);
		restoreButtonStatus(mGp.confirmNoAll,vsa.confirm_no_all, mGp.confirmNoAllListener);
		restoreButtonStatus(mGp.progressBarCancel,vsa.prog_bar_cancel, mGp.progressBarCancelListener);
		restoreButtonStatus(mGp.progressSpinCancel,vsa.prog_spin_cancel,mGp.progressSpinCancelListener);
		restoreButtonStatus(mGp.progressBarImmed,vsa.prog_bar_immed,mGp.progressBarImmedListener);

		mGp.progressBarMsg.setText(vsa.progress_bar_msg);
		mGp.progressBarPb.setMax(vsa.progress_bar_max);
		mGp.progressBarPb.setProgress(vsa.progress_bar_progress);
		
		mGp.progressSpinSyncprof.setText(vsa.prog_prof);
		mGp.progressSpinFilePath.setText(vsa.prog_fp);
		mGp.progressSpinStatus.setText(vsa.prog_msg);
		mGp.mainViewScheduleInfo.setText(vsa.prog_sched_info);

		if (vsa.prog_bar_showed) {
			mGp.progressBarView.bringToFront();
			mGp.progressBarView.setBackgroundColor(Color.BLACK);
			mGp.progressBarView.setVisibility(LinearLayout.VISIBLE);
		} else mGp.progressBarView.setVisibility(LinearLayout.GONE);

		if (vsa.prog_spin_showed) {
			mGp.progressSpinView.bringToFront();
			mGp.progressSpinView.setBackgroundColor(Color.BLACK);
			mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
		} else mGp.progressSpinView.setVisibility(LinearLayout.GONE);
		
		if (vsa.confirm_showed) {
			mGp.confirmView.setBackgroundColor(Color.BLACK);
			mGp.confirmView.setVisibility(LinearLayout.VISIBLE);
			mGp.confirmView.bringToFront();
		} else {
			mGp.confirmView.setVisibility(LinearLayout.GONE);
		}

	};
	
	private void changeLanguageCode(final Configuration newConfig) {
		util.addLogMsg("I",getString(R.string.msgs_smbsync_main_language_changed));
	    loadMsgString();
//	    refreshOptionMenu();
//		mTabChildviewProf.setTabTitle(getString(R.string.msgs_tab_name_prof));
//		mTabChildviewMsg.setTabTitle(getString(R.string.msgs_tab_name_msg));
//		mTabChildviewHist.setTabTitle(getString(R.string.msgs_tab_name_history));
		profUtil.loadMsgString();
		mCurrentLocal=newConfig.locale;
		
		commonDlg.showCommonDialog(false, "W", "", 
				getString(R.string.msgs_smbsync_main_language_changed), null);
		
	};
	
	private void svcStartForeground() {
		if (mSvcClient!=null) {
			try {
				mSvcClient.aidlStartForeground();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	private void svcStopForeground(boolean clear) {
		if (mSvcClient!=null) {
			try {
				mSvcClient.aidlStopForeground(clear);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	private boolean enableProfileConfirmCopyDeleteIfRequired() {
		boolean result=false;
		if (mGp.profileAdapter.getItem(0).getType().equals("")) result=true;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (prefs.getString(SMBSYNC_PROFILE_CONFIRM_COPY_DELETE, SMBSYNC_PROFILE_CONFIRM_COPY_DELETE_REQUIRED)
				.equals(SMBSYNC_PROFILE_CONFIRM_COPY_DELETE_REQUIRED)) {
			prefs.edit().putString(SMBSYNC_PROFILE_CONFIRM_COPY_DELETE, SMBSYNC_PROFILE_CONFIRM_COPY_DELETE_NOT_REQUIRED).commit();
			String c_prof="";
			String c_sep="";
			for(int i=0;i<mGp.profileAdapter.getCount();i++) {
				ProfileListItem pfli=mGp.profileAdapter.getItem(i);
				if (pfli.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
					if (!pfli.isConfirmRequired())  {
						pfli.setConfirmRequired(true);
						c_prof+=c_sep+pfli.getName();
						c_sep=", ";
					}
				}
			}
			if (!c_prof.equals("")) {
				profUtil.saveProfileToFile(false, "", "", mGp.profileAdapter, false);
				String m_txt=mContext.getString(R.string.msgs_set_profile_confirm_delete);
				mGp.supressAutoStart=true;
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						mGp.supressAutoStart=false;
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						mGp.supressAutoStart=false;
					}
					
				});
				util.addLogMsg("W",m_txt+"\n"+c_prof);
				commonDlg.showCommonDialog(false, "W", m_txt+"\n"+c_prof, "", ntfy);
				result=false;
			} else {
				result=true;
			}
		} else {
			result=true;
		}
		return result;
	};
	
	private void initAdapterAndView() {
		mGp.msgListView.setFastScrollEnabled(true);
		
		mGp.msgListView.setAdapter(mGp.msgListAdapter);
		mGp.msgListView.setDrawingCacheEnabled(true);
		mGp.msgListView.setClickable(true);
		mGp.msgListView.setFocusable(true);
		mGp.msgListView.setFocusableInTouchMode(true);
		mGp.msgListView.setSelected(true);
		setFastScrollListener(mGp.msgListView);
		
		mGp.profileListView.setAdapter(mGp.profileAdapter);
		
		mGp.syncHistoryListView.setAdapter(mGp.syncHistoryAdapter);
		mGp.syncHistoryAdapter.notifyDataSetChanged();
		mGp.syncHistoryListView.setClickable(true);
		mGp.syncHistoryListView.setFocusable(true);
//		mGp.syncHistoryListView.setFastScrollEnabled(true);
		mGp.syncHistoryListView.setFocusableInTouchMode(true);
//		mGp.syncHistoryListView.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_INSET);
//		setFastScrollListener(mGp.syncHistoryListView);
	};
	
	private void setFastScrollListener(final ListView lv) {
		if (Build.VERSION.SDK_INT == 19) {
			lv.setFastScrollAlwaysVisible(true);
//			mUiHandler.postDelayed(new Runnable(){
//				@Override
//				public void run() {
//					lv.smoothScrollToPosition(0);
//					lv.setSelection(0);
//					lv.setFastScrollAlwaysVisible(false);
//				}
//			}, 500);
//		    lv.setOnScrollListener(new OnScrollListener() {
//		        private static final int DELAY = 2000;
//		        private AbsListView view;
//		        private int scrollState=0;
//
//		        @Override
//		        public void onScrollStateChanged(AbsListView view, int scrollState) {
//		        	if (view.isFastScrollEnabled()) {
//		        	}
//		        	this.scrollState=scrollState;
//		            if (scrollState != SCROLL_STATE_IDLE) {
//		                view.setFastScrollAlwaysVisible(true);
//		                handler.removeCallbacks(runnable);
//		            } else {
//		                this.view = view;
//		                handler.postDelayed(runnable, DELAY);
//		            }
//		        }
//
//		        @Override
//		        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//		        }
//
//		        private Handler handler = new Handler();
//		        private Runnable runnable = new Runnable() {
//			        @Override
//			        public void run() {
//			        	if (scrollState==SCROLL_STATE_IDLE) {
//				            view.setFastScrollAlwaysVisible(false);
//				            view = null;
//			        	}
//			        }
//		        };
//		    });
		}
	};
	
	private void getApplVersionName() {
		try {
		    String packegeName = getPackageName();
		    PackageInfo packageInfo = getPackageManager().getPackageInfo(packegeName, PackageManager.GET_META_DATA);
		    packageVersionName=packageInfo.versionName;
		} catch (NameNotFoundException e) {
			
				util.addDebugLogMsg(1,"I", "SMBSync package can not be found");        
		}
	};
	
	private CustomTabContentView mTabChildviewProf=null, 
			mTabChildviewMsg=null, mTabChildviewHist=null;
	private void createTabView() {
		tabHost=(TabHost)findViewById(android.R.id.tabhost);
		tabHost.setup();

		mTabChildviewProf = 
			new CustomTabContentView(this,getString(R.string.msgs_tab_name_prof));
		TabHost.TabSpec tabSpec=
			tabHost.newTabSpec("prof").setIndicator(mTabChildviewProf).setContent(R.id.profile_view);
		tabHost.addTab(tabSpec);
		
		mTabChildviewMsg = 
			new CustomTabContentView(this,getString(R.string.msgs_tab_name_msg));
		tabSpec=
			tabHost.newTabSpec("msg").setIndicator(mTabChildviewMsg).setContent(R.id.message_view);
		tabHost.addTab(tabSpec);

		mTabChildviewHist = 
				new CustomTabContentView(this,getString(R.string.msgs_tab_name_history));
		tabSpec=
			tabHost.newTabSpec("hst").setIndicator(mTabChildviewHist).setContent(R.id.history_view);
		tabHost.addTab(tabSpec);

		if (restartStatus==0) tabHost.setCurrentTab(0);
		tabHost.setOnTabChangedListener(new OnTabChange());
		
		mGp.msgListView = (ListView) findViewById(R.id.message_view_list);
		mGp.profileListView =(ListView) findViewById(R.id.profile_view_list);
		mGp.syncHistoryListView=(ListView)findViewById(R.id.history_view_list);
		
		mGp.mainViewScheduleInfo=(TextView)findViewById(R.id.schedule_info);
		
		mGp.confirmView=(LinearLayout)findViewById(R.id.profile_confirm);
		mGp.confirmTitle=(TextView)findViewById(R.id.copy_delete_confirm_title);
		mGp.confirmMsg=(TextView)findViewById(R.id.copy_delete_confirm_msg);
		mGp.confirmCancel=(Button)findViewById(R.id.copy_delete_confirm_task_cancel);
		mGp.confirmYes=(Button)findViewById(R.id.copy_delete_confirm_yes);
	    mGp.confirmNo=(Button)findViewById(R.id.copy_delete_confirm_no);
	    mGp.confirmYesAll=(Button)findViewById(R.id.copy_delete_confirm_yesall);
	    mGp.confirmNoAll=(Button)findViewById(R.id.copy_delete_confirm_noall);

	    mGp.progressBarView=(LinearLayout)findViewById(R.id.profile_progress_bar);
	    mGp.progressBarMsg=(TextView)findViewById(R.id.profile_progress_bar_msg);
	    mGp.progressBarPb = (ProgressBar)findViewById(R.id.profile_progress_bar_progress);

	    mGp.progressBarCancel=(Button)findViewById(R.id.profile_progress_bar_btn_cancel);
	    mGp.progressBarImmed=(Button)findViewById(R.id.profile_progress_bar_btn_immediate);

	    mGp.progressSpinView=(LinearLayout)findViewById(R.id.profile_progress_spin);

	    mGp.progressSpinSyncprof=(TextView)findViewById(R.id.profile_progress_spin_syncprof);
	    mGp.progressSpinFilePath=(TextView)findViewById(R.id.profile_progress_spin_filepath);
	    mGp.progressSpinStatus=(TextView)findViewById(R.id.profile_progress_spin_status);
	    mGp.progressSpinCancel=(Button)findViewById(R.id.profile_progress_spin_btn_cancel);

	};
	
	class OnTabChange implements OnTabChangeListener {
		@Override
		public void onTabChanged(String tabId){
			util.addDebugLogMsg(2,"I","onTabchanged entered. tab="+tabId+",v="+currentViewType);
			if (tabId.equals("prof")) {
				currentViewType="P";
//				glblParms.profileListView.setSelection(posglblParms.profileListView);
			} else if (tabId.equals("msg")) {
				currentViewType="M";
			} else if (tabId.equals("hst")) {
				currentViewType="H";
			}
			util.addDebugLogMsg(2,"I","onTabchanged exited. tab="+tabId+",v="+currentViewType);
		};
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		util.addDebugLogMsg(1,"I","onCreateOptionsMenu entered");
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.menu_top, menu);
		return true;//super.onCreateOptionsMenu(menu);
	};
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		util.addDebugLogMsg(1,"I","onPrepareOptionsMenu entered, isUiEnabled()="+isUiEnabled());
//		menu.findItem(R.id.menu_top_scheduler).setVisible(false);
		if (isUiEnabled()) {
			menu.findItem(R.id.menu_top_sync).setEnabled(true);
			menu.findItem(R.id.menu_top_browse_log).setEnabled(true);
			menu.findItem(R.id.menu_top_export).setEnabled(true);
			menu.findItem(R.id.menu_top_import).setEnabled(true);
			menu.findItem(R.id.menu_top_last_mod_list).setEnabled(true);
			menu.findItem(R.id.menu_top_about).setEnabled(true);
			menu.findItem(R.id.menu_top_settings).setEnabled(true);
			menu.findItem(R.id.menu_top_log_management).setEnabled(true);
			menu.findItem(R.id.menu_top_scheduler).setVisible(true);
			if (!mGp.externalStorageIsMounted) {
				menu.findItem(R.id.menu_top_sync).setEnabled(false);
				menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
				menu.findItem(R.id.menu_top_export).setEnabled(false);
				menu.findItem(R.id.menu_top_import).setEnabled(false);
				menu.findItem(R.id.menu_top_log_management).setEnabled(false);
			}
			if (mGp.logWriter==null)
				menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
			if (!LocalFileLastModified.isLastModifiedWasUsed(mGp.profileAdapter))
				menu.findItem(R.id.menu_top_last_mod_list).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_top_sync).setEnabled(false);
			
			menu.findItem(R.id.menu_top_browse_log).setEnabled(true);
			if (!mGp.externalStorageIsMounted) {
				menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
			}
			if (mGp.logWriter==null) {
				menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
			}
//			Log.v("","ena="+menu.findItem(R.id.menu_top_browse_log).isEnabled());
			
			menu.findItem(R.id.menu_top_export).setEnabled(false);
			menu.findItem(R.id.menu_top_import).setEnabled(false);
			menu.findItem(R.id.menu_top_last_mod_list).setEnabled(false);
			menu.findItem(R.id.menu_top_about).setEnabled(false);
			menu.findItem(R.id.menu_top_settings).setEnabled(false);
			menu.findItem(R.id.menu_top_log_management).setEnabled(false);
			menu.findItem(R.id.menu_top_scheduler).setVisible(false);
		}
        return super.onPrepareOptionsMenu(menu);
	};
	
	private long mToastNextIssuedTimeSyncOption=0l;
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_top_sync:
				if (!util.isRemoteDisable()) {
					if (profUtil.getActiveSyncProfileCount(mGp.profileAdapter)>0) {
						syncActiveProfile();
					} else {
						NotifyEvent ntfy=new NotifyEvent(mContext);
						ntfy.setListener(new NotifyEventListener(){
							@Override
							public void positiveResponse(Context c, Object[] o) {
								ProfileCreationWizard sw=new ProfileCreationWizard(mGp, mContext, 
										util, profUtil, commonDlg, mGp.profileAdapter);
								sw.wizardMain();
							}
							@Override
							public void negativeResponse(Context c, Object[] o) {}
						});
						commonDlg.showCommonDialog(false, "W", "", 
								mContext.getString(R.string.msgs_sync_can_not_sync_no_valid_profile), ntfy);
					}
				} else {
					if (mToastNextIssuedTimeSyncOption<System.currentTimeMillis()) {
						Toast.makeText(mContext, 
							mContext.getString(R.string.msgs_sync_can_not_sync_wlan_option_not_satisfied), 
							Toast.LENGTH_SHORT)
							.show();
						mToastNextIssuedTimeSyncOption=System.currentTimeMillis()+2000;
					}
				}
				return true;
			case R.id.menu_top_browse_log:
				invokeLogFileBrowser();
				return true;
			case R.id.menu_top_last_mod_list:
				LocalFileLastModified lflm=
					new LocalFileLastModified(mContext,mGp.profileAdapter,util,commonDlg);
				lflm.maintLastModListDlg();
				return true;
			case R.id.menu_top_export:
				profUtil.exportProfileDlg(mGp.SMBSync_External_Root_Dir,"/SMBSync","profile.txt");
				return true;
			case R.id.menu_top_import:
				importProfileAndParms();
				return true;
			case R.id.menu_top_log_management:
				invokeLogManagement();
				return true;
			case R.id.menu_top_scheduler:
				SchedulerMain sm=new SchedulerMain(util, mContext, commonDlg, ccMenu, mGp);
				sm.initDialog();
				return true;
			case R.id.menu_top_about:
				aboutSMBSync();
				return true;			
			case R.id.menu_top_settings:
				invokeSettingsActivity();
				return true;			
			case R.id.menu_top_kill:
				killTerminateApplication();
				return true;
		}
		if (isUiEnabled()) {
		}
		return false;
	};

	private void invokeLogManagement() {
		util.flushLogFile();
		LogFileManagementFragment lfm=
				LogFileManagementFragment.newInstance(getString(R.string.msgs_log_management_title));
		lfm.showDialog(getSupportFragmentManager(), lfm, mGp);
	};
	
	private void checkLastModifiedListValidity() {
		final ArrayList<LocalFileLastModifiedMaintListItem> maint_list=
				new ArrayList<LocalFileLastModifiedMaintListItem>();
		final Handler hndl=new Handler();
		Thread th=new Thread(new Runnable(){
			@Override
			public void run() {
				LocalFileLastModified.createLastModifiedMaintList(
						mContext, mGp.profileAdapter,maint_list);
				hndl.post(new Runnable() {
					@Override
					public void run() {
						checkMixedMountPoint(maint_list);
					}
				});
			}
		});
		th.start();
	};

	private ProfileListItem getProfileListItem(String type, String name) {
		ProfileListItem pli=null;
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			if (mGp.profileAdapter.getItem(i).getType().equals(type) &&
					mGp.profileAdapter.getItem(i).getName().equals(name)) {
				pli=mGp.profileAdapter.getItem(i);
				break;
			}
		}
		return pli;
	};
	
	private void checkMixedMountPoint(final ArrayList<LocalFileLastModifiedMaintListItem> maint_list) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.getBoolean(SMBSYNC_SUPPRESS_WARNING_MIXED_MP, false)) {
			boolean mixed_mp=false;
			String mp_name=null;
			for (int i=0;i<mGp.profileAdapter.getCount();i++) {
				ProfileListItem s_pli=mGp.profileAdapter.getItem(i);
				if (s_pli.getType().equals(SMBSYNC_PROF_TYPE_SYNC) && s_pli.isActive()) {
					ProfileListItem o_pli=null;
					if (s_pli.getMasterType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						o_pli=getProfileListItem(SMBSYNC_PROF_TYPE_LOCAL,s_pli.getMasterName());
					} else {
						o_pli=getProfileListItem(SMBSYNC_PROF_TYPE_LOCAL,s_pli.getTargetName());
					}
					if (o_pli!=null) {
						if (mp_name!=null) {
//							Log.v("","mp_name="+o_pli.getLocalMountPoint());
							if (!o_pli.getLocalMountPoint().equals(mp_name)) {
								mixed_mp=true;
								break;
							}
						} else {
//							Log.v("","mp_name init ="+mp_name);
							mp_name=o_pli.getLocalMountPoint();
						}
					}
				}
			}
//			Log.v("","mixed_mp="+mixed_mp);
			if (!mixed_mp) {
				checkLastModifiedCorrupted(maint_list);
			} else {
				final Dialog dialog = new Dialog(mContext);//, android.R.style.Theme_Black);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setContentView(R.layout.mixed_mount_point_dialog);
				TextView title=(TextView)dialog.findViewById(R.id.mixed_mount_point_dialog_title);
				title.setText(mContext.getString(R.string.msgs_common_dialog_warning));
				title.setTextColor(Color.YELLOW);
				
				((TextView)dialog.findViewById(R.id.mixed_mount_point_dialog_subtitle))
				.setText(mContext.getString(R.string.msgs_local_file_modified_maint_mixed_old_new_title));
				
				((TextView)dialog.findViewById(R.id.mixed_mount_point_dialog_msg))
				.setText(mContext.getString(R.string.msgs_local_file_modified_maint_mixed_old_new_msg));
				
				final Button btnOk = (Button) dialog.findViewById(R.id.common_dialog_btn_ok);
				final CheckedTextView ctvSuppr= (CheckedTextView) dialog.findViewById(R.id.mixed_mount_point_dialog_ctv_suppress);
				SMBSyncUtil.setCheckedTextView(ctvSuppr);
				
				CommonDialog.setDlgBoxSizeCompact(dialog);
				ctvSuppr.setChecked(false);
				// OKボタンの指定
				btnOk.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						dialog.dismiss();
						if (ctvSuppr.isChecked()) {
							SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
							prefs.edit().putBoolean(SMBSYNC_SUPPRESS_WARNING_MIXED_MP, true).commit();
						}
						checkLastModifiedCorrupted(maint_list);
					}
				});
				// Cancelリスナーの指定
				dialog.setOnCancelListener(new Dialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						btnOk.performClick();
					}
				});
//				dialog.setOnKeyListener(new DialogOnKeyListener(mContext));
//				dialog.setCancelable(false);
				dialog.show();
			}
		}
	};
	
	private void checkLastModifiedCorrupted(ArrayList<LocalFileLastModifiedMaintListItem> maint_list) {
		String m_line="";
		if (maint_list.size()!=0) {
			for (int i=0;i<maint_list.size();i++) {
				if (maint_list.get(i).getStatus().equals(mContext.getString(R.string.msgs_local_file_modified_maint_status_corrupted))) {
					m_line+=maint_list.get(i).getLocalMountPoint()+"\n";
				}
			}
		}
		if (!m_line.equals("")) {
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					LocalFileLastModified lflm=
							new LocalFileLastModified(mContext,mGp.profileAdapter,util,commonDlg);
					lflm.maintLastModListDlg();
				}
				@Override
				public void negativeResponse(Context c, Object[] o) {}
			});
			commonDlg.showCommonDialog( true, "W", 
					mContext.getString(R.string.msgs_local_file_modified_maint_validation_title), 
					mContext.getString(R.string.msgs_local_file_modified_maint_validation_msg)+
					"\n"+m_line,ntfy);
		}
	};
	
	private void importProfileAndParms() {
		NotifyEvent ntfy=new NotifyEvent(this);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				applySettingParms();
				checkJcifsOptionChanged();
				SchedulerMain.setTimer(mContext, SCHEDULER_INTENT_SET_TIMER);
			}

			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		profUtil.importProfileDlg(mGp.SMBSync_External_Root_Dir,"/SMBSync","profile.txt", ntfy);
	};

	private CallBackListener onKeyCallBackListener=null;
	private void setOnKeyCallBackListener(CallBackListener cbl) {
		onKeyCallBackListener=cbl;
	};
	private void unsetOnKeyCallBackListener() {
		onKeyCallBackListener=null;
	};
	
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		util.addDebugLogMsg(9,"i","main onKeyDown enterd, kc="+keyCode);
		switch (keyCode) {
			case KeyEvent.KEYCODE_BACK:
				if (onKeyCallBackListener!=null) {
					if (!onKeyCallBackListener.onCallBack(mContext, event, null))
						terminateApplication();
				} else {
					terminateApplication();
				}
				return true;
				// break;
			default:
				return super.onKeyDown(keyCode, event);
				// break;
		}
	};
	
	private void startupWarning() { 
		if (!mGp.externalStorageIsMounted) {
    		util.addLogMsg("W",getString(R.string.msgs_no_external_storage));
    		commonDlg.showCommonDialog(false,"W",
    				getString(R.string.msgs_no_external_storage),"",null);
		}

	};
	
	private void aboutSMBSync() {
		
		// common カスタムダイアログの生成
		final Dialog dialog = new Dialog(this);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.about_dialog);
		((TextView)dialog.findViewById(R.id.about_dialog_title)).setText(
			getString(R.string.msgs_dlg_title_about)+"(Ver "+packageVersionName+")");
		final WebView func_view=(WebView)dialog.findViewById(R.id.about_dialog_function);
//		func_view.loadDataWithBaseURL("file:///android_asset/",
//				getString(R.string.msgs_dlg_title_about_func_desc),"text/html","UTF-8","");
		func_view.loadUrl("file:///android_asset/"+
				getString(R.string.msgs_dlg_title_about_func_desc));
		func_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		func_view.getSettings().setBuiltInZoomControls(true);
		
		final WebView change_view=
				(WebView)dialog.findViewById(R.id.about_dialog_change_history);
		change_view.loadUrl("file:///android_asset/"+
				getString(R.string.msgs_dlg_title_about_change_desc));
		change_view.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY);
		change_view.getSettings().setBuiltInZoomControls(true);
		
		final Button btnFunc = (Button) dialog.findViewById(R.id.about_dialog_btn_show_func);
		final Button btnChange = (Button) dialog.findViewById(R.id.about_dialog_btn_show_change);		
		final Button btnOk = (Button) dialog.findViewById(R.id.about_dialog_btn_ok);
		
		func_view.setVisibility(TextView.VISIBLE);
		change_view.setVisibility(TextView.GONE);
		btnChange.setBackgroundResource(R.drawable.button_bg_color_selector);
		btnFunc.setBackgroundResource(R.drawable.button_bg_color_selector);
		btnChange.setTextColor(Color.DKGRAY);
		btnFunc.setTextColor(Color.GREEN);
		btnFunc.setEnabled(false);
		
//		btnOk.setTextColor(Color.DKGRAY);
//		btnOk.setTextColor(Color.GREEN);
//		btnOk.setBackgroundResource(R.drawable.button_back_ground_color_selector);

		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		
		// funcボタンの指定
		btnFunc.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				change_view.setVisibility(TextView.GONE);
				func_view.setVisibility(TextView.VISIBLE);
				CommonDialog.setDlgBoxSizeLimit(dialog,true);
//				func_view.setEnabled(true);
//				change_view.setEnabled(false);
				btnFunc.setTextColor(Color.GREEN);
				btnChange.setTextColor(Color.DKGRAY);
				btnChange.setEnabled(true);
				btnFunc.setEnabled(false);
			}
		});
		
		// changeボタンの指定
		btnChange.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				change_view.setVisibility(TextView.VISIBLE);
				func_view.setVisibility(TextView.GONE);
				CommonDialog.setDlgBoxSizeLimit(dialog,true);
//				func_view.setEnabled(true);
//				change_view.setEnabled(false);
				btnChange.setTextColor(Color.GREEN);
				btnFunc.setTextColor(Color.DKGRAY);
				btnChange.setEnabled(false);
				btnFunc.setEnabled(true);
			}
		});
		
		// OKボタンの指定
		btnOk.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btnOk.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(mContext));
//		dialog.setCancelable(false);
		dialog.show();
				
	};

	@SuppressLint("SdCardPath")
	private void checkExternalStorage() {
    	// get file state  
    	String status   = Environment.getExternalStorageState();  

		if (!status.equals(Environment.MEDIA_MOUNTED)) {  
    	  // media is not mounted
			if (Build.VERSION.SDK_INT==7) mGp.SMBSync_External_Root_Dir="/sdcard";
			else mGp.SMBSync_External_Root_Dir="/mnt/sdcard";
    		mGp.externalStorageIsMounted=false;
    	} else  {  
        	// get file path  
    		mGp.SMBSync_External_Root_Dir = LocalMountPoint.getExternalStorageDir();
    		mGp.externalStorageIsMounted=true;
    	}
	};

	private void terminateApplication() {
		if (tabHost.getCurrentTab()==0) {//
			if (ProfileUtility.isAnyProfileSelected(mGp.profileAdapter,SMBSYNC_PROF_GROUP_DEFAULT)) {
				resetAllCheckedItem();
				return;
			}
		} else if (tabHost.getCurrentTab()==1) {
		} else if (tabHost.getCurrentTab()==2) {
			if (isHistoryItemSelected()) {
				setHistoryItemUnselectAll();
				return;
			}
		}
		util.addLogMsg("I",msgs_smbsync_main_end);
		isTaskTermination = true; // exit cleanly
		finish();
	};
	
	private void killTerminateApplication() {
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
//				terminateApplication();
				deleteTaskData();
				util.flushLogFile();
				android.os.Process.killProcess(android.os.Process.myPid());
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});
		commonDlg.showCommonDialog(true,"W",
				mContext.getString(R.string.msgs_kill_application),"",ntfy);
	};
	
	private void initSettingsParms() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (prefs.getString(getString(R.string.settings_log_dir), "-1").equals("-1")) {
			Editor pe=prefs.edit();
			
			isApplicationFirstTimeRunning=true;
			mGp.sampleProfileCreateRequired=true;
			
			pe.putString(getString(R.string.settings_log_dir), mGp.SMBSync_External_Root_Dir+"/SMBSync/");
			pe.putString(getString(R.string.settings_network_wifi_option), SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP);
			pe.putString(getString(R.string.settings_file_diff_time_seconds), "3");
			pe.putString(getString(R.string.settings_media_store_last_mod_time), "0");

			pe.putBoolean(getString(R.string.settings_media_scanner_non_media_files_scan), true);
			pe.putBoolean(getString(R.string.settings_media_scanner_scan_extstg), true);

			pe.putBoolean(getString(R.string.settings_exit_clean), true);
			
			pe.putString(getString(R.string.settings_smb_lm_compatibility),"0");
			pe.putBoolean(getString(R.string.settings_smb_use_extended_security),false);
			pe.putString(getString(R.string.settings_smb_log_level),"0");
			pe.putString(getString(R.string.settings_smb_rcv_buf_size),"66576");
			pe.putString(getString(R.string.settings_smb_snd_buf_size),"66576");
			pe.putString(getString(R.string.settings_smb_listSize),"65535");
			pe.putString(getString(R.string.settings_smb_maxBuffers),"100");
			pe.putString(getString(R.string.settings_smb_tcp_nodelay),"true");
			pe.putString(getString(R.string.settings_io_buffers),"8");
			
			pe.putString(SMBSYNC_PROFILE_CONFIRM_COPY_DELETE, SMBSYNC_PROFILE_CONFIRM_COPY_DELETE_NOT_REQUIRED);
			
			pe.commit();
		}

		if (prefs.getString(getString(R.string.settings_keep_screen_on), "").equals("")) {
			prefs.edit().putString(getString(R.string.settings_keep_screen_on), GlobalParameters.KEEP_SCREEN_ON_WHEN_SCREEN_UNLOCKED)
				.commit();
		}
		
		if (prefs.getString(getString(R.string.settings_smb_perform_class), "-1").equals("-1")) {
			prefs.edit().putString(getString(R.string.settings_smb_perform_class), 
					"0").commit();
		}

		if (prefs.getString(getString(R.string.settings_network_wifi_option), "-1").equals("-1")) {
			if (prefs.getBoolean("settings_wifi_only", false)) {
				prefs.edit().putString(getString(R.string.settings_network_wifi_option), 
						SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP).commit();
			} else {
				prefs.edit().putString(getString(R.string.settings_network_wifi_option), 
						SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF).commit();
			}
		}

		
//		if (prefs.getString(getString(R.string.settings_smb_listSize), "").length()==0) {
//			prefs.edit().putString(getString(R.string.settings_smb_listSize), 
//					"1300").commit();
//		}
//		if (prefs.getString(getString(R.string.settings_smb_maxBuffers), "").length()==0) {
//			prefs.edit().putString(getString(R.string.settings_smb_maxBuffers), 
//					"16").commit();
//		}
	};
	
	private void applySettingParms() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

		mGp.debugLevel=
			Integer.parseInt(prefs.getString(getString(R.string.settings_log_level), "0"));

		util.addDebugLogMsg(1, "I", "applySettingParms entered");

		mGp.settiingLogGeneration=Integer.valueOf(
				prefs.getString(getString(R.string.settings_log_generation), "10"));
		
		String t_dir =
				prefs.getString(getString(R.string.settings_log_dir),
						mGp.SMBSync_External_Root_Dir+"/SMBSync/");
		if (t_dir.equals("")) {
			t_dir=mGp.SMBSync_External_Root_Dir+"/SMBSync/";
			prefs.edit().putString(getString(R.string.settings_log_dir),
					mGp.SMBSync_External_Root_Dir+"/SMBSync/").commit();
		} else {
			if (!t_dir.endsWith("/")) {
				t_dir+="/";
				prefs.edit().putString(getString(R.string.settings_log_dir),t_dir).commit();
			} 
		}
		
		if (!mGp.settingLogMsgDir.equals(t_dir)) {// option was changed
			mGp.settingLogMsgDir=t_dir;
			if (!mGp.settingLogOption.equals("0")) {
				util.closeLogFile();
				util.openLogFile();
			}
		}
		
		mGp.settingAutoStart=
				prefs.getBoolean(getString(R.string.settings_auto_start), false);
		mGp.settingDebugMsgDisplay=
				prefs.getBoolean(getString(R.string.settings_debug_msg_diplay), false);
		
		String p_opt=mGp.settingLogOption;
		mGp.settingLogOption=
				prefs.getString(getString(R.string.settings_log_option), "0");

		if (!mGp.settingLogOption.equals(p_opt)) {
			if (mGp.settingLogOption.equals("0")) util.closeLogFile();
			else util.openLogFile();
		}
//		Log.v("","p="+p_opt+", n="+glblParms.settingLogOption);
//		if (!glblParms.settingLogOption.equals(t_lo) ) {// option was changed
//			commonDlg.showCommonDialog(false,"W",msgs_setting_log_opt_chg,"",null);
//		}
		mGp.settingAutoTerm=
				prefs.getBoolean(getString(R.string.settings_auto_term), false);
		mGp.settingWifiOption=
				prefs.getString(getString(R.string.settings_network_wifi_option), 
						SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP);
		mGp.settingErrorOption=
				prefs.getBoolean(getString(R.string.settings_error_option), false);
		mGp.settingBackgroundExecution=
				prefs.getBoolean(getString(R.string.settings_backgroound_execution), false);
		mGp.settingBgTermNotifyMsg=
				prefs.getString(getString(R.string.settings_background_termination_notification), "0");

		mGp.settingVibrateWhenSyncEnded=
				prefs.getString(getString(R.string.settings_vibrate_when_sync_ended), "0");
		mGp.settingRingtoneWhenSyncEnded=
				prefs.getString(getString(R.string.settings_playback_ringtone_when_sync_ended), "0");

		mGp.settingScreenOnOption=
				prefs.getString(getString(R.string.settings_keep_screen_on), GlobalParameters.KEEP_SCREEN_ON_WHEN_SCREEN_UNLOCKED);

		mGp.settingWifiLockRequired=
				prefs.getBoolean(getString(R.string.settings_wifi_lock), false);

		mGp.settingRemoteFileCopyByRename=
				prefs.getBoolean(getString(R.string.settings_remote_file_copy_by_rename), false);
		mGp.settingLocalFileCopyByRename=
				prefs.getBoolean(getString(R.string.settings_local_file_copy_by_rename), false);
		
//		mGp.settingAltUiEnabled=
//				prefs.getBoolean(getString(R.string.settings_ui_alternate_ui), false);

		mGp.settingMediaFiles=
				prefs.getBoolean(getString(R.string.settings_media_scanner_non_media_files_scan), true);
		mGp.settingScanExternalStorage=
				prefs.getBoolean(getString(R.string.settings_media_scanner_scan_extstg), true);
		
		mGp.settingExitClean=
				prefs.getBoolean(getString(R.string.settings_exit_clean), true);
		mGp.settingExportedProfileEncryptRequired=
				prefs.getBoolean(getString(R.string.settings_exported_profile_encryption), true);
		
		if (!mGp.settingAutoStart) mGp.settingAutoTerm=false;
		
//		if (isJcifsOptionChanged() && restartStatus!=0) {
//			commonDlg.showCommonDialog(false,"W",
//					"",mContext.getString(R.string.msgs_smbsync_main_settings_jcifs_changed_restart),null);
//		}
		
//		refreshOptionMenu();
//		
	};

	private boolean isCheckWifiOffRequired=false;
	
	private void checkAutoStart(Intent in) {
		loadExtraDataParms(in);
		util.addDebugLogMsg(1,"I","Extra data AutoStart="+extraValueAutoStart+
				", AutoTerm="+extraValueAutoTerm+
				", Background="+extraValueBgExec+
				", SyncProfile="+extraValueSyncProfList);
		if (mGp.settingAutoStart ||(isExtraSpecAutoStart && extraValueAutoStart)) {
			if (isExtraSpecAutoStart && extraValueAutoStart) isCheckWifiOffRequired=true;
			else isCheckWifiOffRequired=false;
			if (mGp.settingLogOption.equals("0")) {
				mGp.settingLogOption="1";
				util.addLogMsg("I",getString(R.string.msgs_smbsync_main_settings_force_log_enabled));
			}
			if ((!mGp.externalStorageIsMounted || util.isRemoteDisable())) {
				String m_txt="";
				if (!mGp.externalStorageIsMounted) m_txt=mContext.getString(R.string.msgs_astart_abort_external_storage_not_mounted);
				if (util.isRemoteDisable()) m_txt=mContext.getString(R.string.msgs_astart_abort_wifi_option_not_satisfied);
				commonDlg.showCommonDialog(false, "W", "", m_txt, null);
				util.addLogMsg("W",m_txt);
				setWifiOff();
			} else {
				markAutoStartProfileList(extraValueSyncProfList);
				if (profUtil.getActiveSyncProfileCount(mGp.profileAdapter)>0) {
					setScreenOn();
					mGp.mirrorThreadActive=true;
					autoStartDlg();
					if ((isExtraSpecBgExec && extraValueBgExec)) {
						setScreenSwitchToHome();
					} else if (!isExtraSpecBgExec && mGp.settingBackgroundExecution) {
						setScreenSwitchToHome();
					}
				} else {
					setWifiOff();
				}
			}
		}
	};
	
	private void setWifiOff() {
		if (isCheckWifiOffRequired) {
			SchedulerParms sp=new SchedulerParms();
			SchedulerUtil.loadScheduleData(sp, mContext);
			if (sp.syncWifiOnBeforeSyncStart && sp.syncWifiOffAfterSyncEnd) {
				SchedulerMain.setTimer(mContext, SCHEDULER_INTENT_WIFI_OFF);
			}
		}
		isCheckWifiOffRequired=false;
	};

	@SuppressLint("DefaultLocale")
	private boolean isAutoStartRequested(Intent in) {
		boolean result=false;
		if (in!=null) {
			Bundle bundle=in.getExtras();
			if (bundle!=null) {
				if (bundle.containsKey(SMBSYNC_EXTRA_PARM_AUTO_START)) {
					result=true;
				} else if (bundle.containsKey(SMBSYNC_EXTRA_PARM_AUTO_TERM)) {
					result=true;
				} else if (bundle.containsKey(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION)) {
					result=true;
				} else if (bundle.containsKey(SMBSYNC_EXTRA_PARM_STARTUP_PARMS)) {
					result=true;
				} else if (bundle.containsKey(SMBSYNC_EXTRA_PARM_SYNC_PROFILE)) {
					result=true;
				}
			}
		}
		util.addDebugLogMsg(1,"I","isAutoStartRequested result="+result);
		return result;
	};
	
	@SuppressLint("DefaultLocale")
	private void loadExtraDataParms(Intent in) {
		Bundle bundle=in.getExtras();
		if (bundle!=null) {
			tabHost.setCurrentTab(1);
			String sid="External Application";
			if (bundle.containsKey(SMBSYNC_SCHEDULER_ID)) {
				if (bundle.get(SMBSYNC_SCHEDULER_ID).getClass().getSimpleName().equals("String")) {
					sid=bundle.getString(SMBSYNC_SCHEDULER_ID);
				}
			}
			util.addLogMsg("I",
					String.format(mContext.getString(R.string.msgs_extra_data_startup_by),sid));
			if (bundle.containsKey(SMBSYNC_EXTRA_PARM_STARTUP_PARMS)) {
				if (bundle.get(SMBSYNC_EXTRA_PARM_STARTUP_PARMS).getClass().getSimpleName().equals("String")) {
					String op=bundle.getString(SMBSYNC_EXTRA_PARM_STARTUP_PARMS).replaceAll("\"", "").replaceAll("\'", "");
//					Log.v("","op="+op);
					boolean[] opa=new boolean[]{false,false,false};
					boolean error=false;
					extraValueSyncProfList=null;
					String[] op_str_array=op.split(",");
					if (op_str_array.length<3) {
						error=true;
						util.addLogMsg("W",String.format(
								mContext.getString(R.string.msgs_extra_data_startup_parms_length_error),op));
					} else {
						for (int i=0;i<3;i++) {
							if (op_str_array[i].toLowerCase().equals("false")) {
								opa[i]=false;
							} else if (op_str_array[i].toLowerCase().equals("true")){
								opa[i]=true;
							} else {
								error=true;
								util.addLogMsg("W",String.format(
										mContext.getString(R.string.msgs_extra_data_startup_parms_value_error),op_str_array[i]));
								break;
							} 
						}
						if (op_str_array.length>3) {
							extraValueSyncProfList=new String[op_str_array.length-3];
							for (int i=0;i<op_str_array.length-3;i++) 
								extraValueSyncProfList[i]=op_str_array[i+3];
						}
					}
					if (!error) {
						isExtraSpecAutoStart=isExtraSpecAutoTerm=isExtraSpecBgExec=true;
						extraValueAutoStart=opa[0];
						util.addLogMsg("I","AutoStart="+extraValueAutoStart);
						if (isExtraSpecAutoStart && extraValueAutoStart) {
							extraValueAutoTerm=opa[1];
							util.addLogMsg("I","AutoTerm="+extraValueAutoTerm);
							extraValueBgExec=opa[2];
							util.addLogMsg("I","Background="+extraValueBgExec);
						} else {
							util.addLogMsg("W",String.format(
									mContext.getString(R.string.msgs_extra_data_ignored_auto_start_not_specified),"AutoTerm"));
							util.addLogMsg("W",String.format(
									mContext.getString(R.string.msgs_extra_data_ignored_auto_start_not_specified),"Background"));
						}
						
						if (bundle.containsKey(SMBSYNC_EXTRA_PARM_AUTO_START)) {
							util.addLogMsg("W",String.format(
									mContext.getString(R.string.msgs_extra_data_ignored_startup_parms_specified),SMBSYNC_EXTRA_PARM_AUTO_START));
						}
						if (bundle.containsKey(SMBSYNC_EXTRA_PARM_AUTO_TERM)) {
							util.addLogMsg("W",String.format(
									mContext.getString(R.string.msgs_extra_data_ignored_startup_parms_specified),SMBSYNC_EXTRA_PARM_AUTO_TERM));
						}
						if (bundle.containsKey(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION)) {
							util.addLogMsg("W",String.format(
									mContext.getString(R.string.msgs_extra_data_ignored_startup_parms_specified),SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION));
						}
						if (bundle.containsKey(SMBSYNC_EXTRA_PARM_SYNC_PROFILE)) {
							util.addLogMsg("W",String.format(
									mContext.getString(R.string.msgs_extra_data_ignored_startup_parms_specified),SMBSYNC_EXTRA_PARM_SYNC_PROFILE));
						}
					}
				} else {
					util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_startup_parms_type_error));
				}
			} else {
				if (bundle.containsKey(SMBSYNC_EXTRA_PARM_AUTO_START)) {
					if (bundle.get(SMBSYNC_EXTRA_PARM_AUTO_START).getClass().getSimpleName().equals("Boolean")) {
						isExtraSpecAutoStart=true;
						extraValueAutoStart=bundle.getBoolean(SMBSYNC_EXTRA_PARM_AUTO_START);
						util.addLogMsg("I","AutoStart="+extraValueAutoStart);
					} else {
						util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_auto_start_not_boolean));
					}
				}
				if (bundle.containsKey(SMBSYNC_EXTRA_PARM_AUTO_TERM)) {
					if (bundle.get(SMBSYNC_EXTRA_PARM_AUTO_TERM).getClass().getSimpleName().equals("Boolean")) {
						if (isExtraSpecAutoStart) {
							isExtraSpecAutoTerm=true;
							extraValueAutoTerm=bundle.getBoolean(SMBSYNC_EXTRA_PARM_AUTO_TERM);
							util.addLogMsg("I","AutoTerm="+extraValueAutoTerm);
						} else {
							util.addLogMsg("W",String.format(
									mContext.getString(R.string.msgs_extra_data_ignored_auto_start_not_specified),"AutoTerm"));
						}
					} else {
						util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_auto_term_not_boolean));
					}
				}
				if (bundle.containsKey(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION)) {
					if (bundle.get(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION).getClass().getSimpleName().equals("Boolean")) {
						if (isExtraSpecAutoStart) {
							isExtraSpecBgExec=true;
							extraValueBgExec=bundle.getBoolean(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION);
							util.addLogMsg("I","Background="+extraValueBgExec);
						} else {
							util.addLogMsg("W",String.format(
									mContext.getString(R.string.msgs_extra_data_ignored_auto_start_not_specified),"Background"));
						}

					} else {
						util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_back_ground_not_boolean));
					}
				}
				if (bundle.containsKey(SMBSYNC_EXTRA_PARM_SYNC_PROFILE)) {
					if (isExtraSpecAutoStart) {
						if (bundle.get(SMBSYNC_EXTRA_PARM_SYNC_PROFILE).getClass().getSimpleName().equals("String[]")) {
							extraValueSyncProfList=bundle.getStringArray(SMBSYNC_EXTRA_PARM_SYNC_PROFILE);
						} else {
							util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_sync_profile_type_error));
						}
					} else {
						util.addLogMsg("W",String.format(
								mContext.getString(R.string.msgs_extra_data_ignored_auto_start_not_specified),"SyncProfile"));
					}
				}
				if (isExtraSpecAutoStart) {
					if (!isExtraSpecAutoTerm) {
						isExtraSpecAutoTerm=true;
						extraValueAutoTerm=false;
						util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_assumed_auto_term_disabled));
						util.addLogMsg("I","AutoTerm="+extraValueAutoTerm);
					}
					if (!isExtraSpecBgExec) {
						isExtraSpecBgExec=true;
						extraValueBgExec=false;
						util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_assumed_bg_exec_disabled));
						util.addLogMsg("I","Background="+extraValueBgExec);
					}
				}
			}
		}
	};

	private boolean markAutoStartProfileList(String[] sync_profile) {
		boolean prof_selected=false;
		if (sync_profile!=null && sync_profile.length!=0) {
			for (int i=0;i<sync_profile.length;i++) {
				if (!sync_profile[i].equals("")) {
					util.addLogMsg("I",mContext.getString(R.string.msgs_extra_data_sync_profile));
//					util.addLogMsg("I", "  "+sync_profile[i]);
					break;
				}
			}
			for (int pidx=0;pidx<mGp.profileAdapter.getCount();pidx++) mGp.profileAdapter.getItem(pidx).setChecked(false);
			for (int sidx=0;sidx<sync_profile.length;sidx++) {
				if (!sync_profile[sidx].equals("")) {
					boolean selected=false;
					for (int pidx=0;pidx<mGp.profileAdapter.getCount();pidx++) {
						if (mGp.profileAdapter.getItem(pidx).getType().equals(SMBSYNC_PROF_TYPE_SYNC) && 
								mGp.profileAdapter.getItem(pidx).getName().equals(sync_profile[sidx])) {
							selected=true;
							if (mGp.profileAdapter.getItem(pidx).isActive()) {
								mGp.profileAdapter.getItem(pidx).setChecked(true);
								util.addLogMsg("I","  "+mContext.getString(R.string.msgs_extra_data_profile_selected)+sync_profile[sidx]);
								prof_selected=true;
							} else {
								util.addLogMsg("W","  "+mContext.getString(R.string.msgs_extra_data_profile_disabled)+sync_profile[sidx]);								
							}
						}
					}
					if (!selected)
							util.addLogMsg("W","  "+mContext.getString(R.string.msgs_extra_data_profile_not_exists)+sync_profile[sidx]);								
				}
			}
		}
		if (!prof_selected) {
			if (extraValueAutoStart)
				util.addLogMsg("I",mContext.getString(R.string.msgs_extra_data_no_profile_selected));
		}
		return prof_selected;
	};
	
	private void listSMBSyncOption() {
		util.addDebugLogMsg(1,"I","SMBSync option :"+
				"debugLevel="+mGp.debugLevel+
				", settingAutoStart="+mGp.settingAutoStart+
				", settingAutoTerm="+mGp.settingAutoTerm+
				", settingErrorOption="+mGp.settingErrorOption+
				", settingBackgroundExecution="+mGp.settingBackgroundExecution+
				", settingScreenOnOption="+mGp.settingScreenOnOption+
				", settingWifiLockRequired="+mGp.settingWifiLockRequired+
				", settingWifiOption="+mGp.settingWifiOption+

//				", settingAltUiEnabled="+mGp.settingAltUiEnabled+
				", settingDebugMsgDisplay="+mGp.settingDebugMsgDisplay+
				", settingMediaFiles="+mGp.settingMediaFiles+
				", settingScanExternalStorage="+mGp.settingScanExternalStorage+
				", settingBgTermNotifyMsg="+mGp.settingBgTermNotifyMsg+
				", settingVibrateWhenSyncEnded="+mGp.settingVibrateWhenSyncEnded+
				", settingRingtoneWhenSyncEnded="+mGp.settingRingtoneWhenSyncEnded+
				
				", settingExitClean="+mGp.settingExitClean+
				
				", settingShowSyncDetailMessage="+mGp.settingShowSyncDetailMessage+
				", settingLogOption="+mGp.settingLogOption+
				", settingLogMsgDir="+mGp.settingLogMsgDir+
				", settingLogMsgFilename="+mGp.settingLogMsgFilename+
				", settiingLogGeneration="+mGp.settiingLogGeneration+
				
				", settingExportedProfileEncryptRequired="+mGp.settingExportedProfileEncryptRequired+
				
				", settingRemoteFileCopyByRename="+mGp.settingRemoteFileCopyByRename
				); 
	};

	@SuppressLint("SdCardPath")
	private void invokeLogFileBrowser() {
		util.addDebugLogMsg(1,"I","Invoke log file browser.");
		util.flushLogFile();
//		enableBrowseLogFileMenu=false;
		if (mGp.logWriter!=null) {
			String t_fd="",fp="";
			t_fd=mGp.settingLogMsgDir;
			if (t_fd.lastIndexOf("/")==(t_fd.length()-1)) {//last is "/"
				fp=t_fd+mGp.settingLogMsgFilename;
			} else fp=t_fd+"/"+mGp.settingLogMsgFilename;

			Intent intent = new Intent(android.content.Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.parse("file://"+fp), "text/plain");
			startActivityForResult(intent,1);
		}
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
//				enableBrowseLogFileMenu=true;
			}
		}, 1000);
	};
	
	private void invokeSettingsActivity() {
		util.addDebugLogMsg(1,"I","Invoke Settings.");
		Intent intent = new Intent(this, SMBSyncSettings.class);
		startActivityForResult(intent,0);
	};
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (!mGp.settingLogOption.equals("0")) util.openLogFile();
		if (requestCode==0) {
			util.addDebugLogMsg(1,"I","Return from Settings.");
			util.setActivityIsForeground(true);
			applySettingParms();
			checkJcifsOptionChanged();
			listSMBSyncOption();
			enableProfileConfirmCopyDeleteIfRequired();
		} else if (requestCode==1) {
			util.addDebugLogMsg(1,"I","Return from browse log file.");
			util.setActivityIsForeground(true);
		}
	};

	private boolean setScreenSwitchToHome() {
//		moveTaskToBack(true);
		Handler hndl=new Handler();
		hndl.postDelayed(new Runnable(){
			@Override
			public void run() {
				Intent in=new Intent();
				in.setAction(Intent.ACTION_MAIN);
				in.addCategory(Intent.CATEGORY_HOME);
				startActivity(in);
			}
		}, 100);
		return true;
	};

	private void setHistoryViewItemClickListener() {
		mGp.syncHistoryListView.setEnabled(true);
		mGp.syncHistoryListView
			.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mGp.syncHistoryListView.setEnabled(false);
				SyncHistoryListItem item = mGp.syncHistoryList.get(position);
//				if (mGp.settingAltUiEnabled) {
//				} else {
//					item.isChecked=!item.isChecked;
//					mGp.syncHistoryListView.setEnabled(true);
//				}
				if (isHistoryItemSelected()) {
					item.isChecked=!item.isChecked;
					mGp.syncHistoryListView.setEnabled(true);
				} else {
					if (!item.sync_result_file_path.equals("")) {
						Intent intent = 
								new Intent(android.content.Intent.ACTION_VIEW);
						intent.setDataAndType(
								Uri.parse("file://"+item.sync_result_file_path),
								"text/plain");
						startActivityForResult(intent,1);
					}
					mUiHandler.postDelayed(new Runnable(){
						@Override
						public void run() {
							mGp.syncHistoryListView.setEnabled(true);
						}
					},1000);
				}
				mGp.syncHistoryAdapter.notifyDataSetChanged();
			}
		});
	};

	private boolean isHistoryItemSelected() {
		boolean result=false;
		for (int i=0;i<mGp.syncHistoryList.size();i++) 
			if (mGp.syncHistoryList.get(i).isChecked) {
				result=true;
				break;
			}
		return result;
	}
	
	private void setHistoryViewLongClickListener() {
		mGp.syncHistoryListView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				createHistoryContextMenu(arg1, arg2);
				return true;
			}
		});
	};

	private void createHistoryContextMenu(View view, int idx) {
		int prev_selected_cnt=0;
		for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) {
			if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
				prev_selected_cnt++;
			}
		}
		if (prev_selected_cnt==0) {//Not selected
			setHistoryItemChecked(idx,true);
			mGp.syncHistoryAdapter.notifyDataSetChanged();
			createHistoryContextMenu_Single(idx);
		} else if (prev_selected_cnt==1) {//Previous selected was single
			for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) {
				if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
					if (i!=idx) {
						setHistoryItemChecked(i,false);
						setHistoryItemChecked(idx,true);
						mGp.syncHistoryAdapter.notifyDataSetChanged();
					}
				}
			}
			createHistoryContextMenu_Single(idx);
		} else {
			boolean already_selected=false;
			for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) {
				if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
					if (i==idx) {
						already_selected=true;
						break;
					}
				}
			}
			if (already_selected) {
				createHistoryContextMenu_Multiple(idx);
			} else {
				setHistoryItemUnselectAll();
				setHistoryItemChecked(idx,true);
				mGp.syncHistoryAdapter.notifyDataSetChanged();
				createHistoryContextMenu_Single(idx);
			}
		}
	};
	
	private void createHistoryContextMenu_Multiple(int idx) { 

		ccMenu.addMenuItem(msgs_move_to_top,R.drawable.menu_top)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				mGp.syncHistoryListView.setSelection(0);
			}
		});
		
		ccMenu.addMenuItem(msgs_move_to_bottom,R.drawable.menu_bottom)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					mGp.syncHistoryListView.setSelection(mGp.syncHistoryAdapter.getCount()-1);
				}
		});
		
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_delete,R.drawable.menu_trash)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				confirmDeleteHistory();
			}
		});
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_unselectall)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				setHistoryItemUnselectAll();
			}
		});
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_selectall)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				setHistoryItemSelectAll();
			}
		});

		ccMenu.addMenuItem(msgs_sync_history_ccmeu_copy_clipboard)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				 ClipboardManager cm = 
					      (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				 StringBuilder out= new StringBuilder(256);
				 for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++){
					 if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
						 SyncHistoryListItem hli=mGp.syncHistoryAdapter.getItem(i);
						 out.append(hli.sync_date).append(" ");
						 out.append(hli.sync_time).append(" ");
						 out.append(hli.sync_prof).append("\n");
		            	if (hli.sync_status==SyncHistoryListItem.SYNC_STATUS_SUCCESS) {
		            		out.append(mContext.getString(R.string.msgs_sync_history_status_success)).append("\n");
		            	} else if (hli.sync_status==SyncHistoryListItem.SYNC_STATUS_ERROR) {
		            		out.append(mContext.getString(R.string.msgs_sync_history_status_fail)).append("\n");
		            	} else if (hli.sync_status==SyncHistoryListItem.SYNC_STATUS_CANCELLED) {
		            		out.append(mContext.getString(R.string.msgs_sync_history_status_cancel)).append("\n");
		            	}
		            	out.append(mContext.getString(R.string.msgs_sync_history_count_copied))
		            		.append(Integer.toString(hli.sync_result_no_of_copied)).append(" ");
		            	out.append(mContext.getString(R.string.msgs_sync_history_count_deleted))
		        			.append(Integer.toString(hli.sync_result_no_of_deleted)).append(" ");
		            	out.append(mContext.getString(R.string.msgs_sync_history_count_ignored))
		        			.append(Integer.toString(hli.sync_result_no_of_ignored)).append(" ");
		            	out.append("\n").append(hli.sync_error_text);
					 }
				 }
				 if (out.length()>0) cm.setText(out);
			}
		});
		ccMenu.createMenu();
	};

	private void setHistoryItemUnselectAll() {
		for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) mGp.syncHistoryAdapter.getItem(i).isChecked=false;
		mGp.syncHistoryAdapter.notifyDataSetChanged();
	}

	private void setHistoryItemSelectAll() {
		for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) mGp.syncHistoryAdapter.getItem(i).isChecked=true;
		mGp.syncHistoryAdapter.notifyDataSetChanged();
	}

	private void setHistoryItemChecked(int pos, boolean p) {
		mGp.syncHistoryAdapter.getItem(pos).isChecked=p;
	};


	private void confirmDeleteHistory() {
		String conf_list="";
		boolean del_all_history=false;
		int del_cnt=0;
		for (int i=0;i<mGp.syncHistoryAdapter.getCount();i++) {
			if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
				del_cnt++;
				conf_list+="\n"+mGp.syncHistoryAdapter.getItem(i).sync_date+" "+
						mGp.syncHistoryAdapter.getItem(i).sync_time+" "+
						mGp.syncHistoryAdapter.getItem(i).sync_prof+" ";
			}
		}
		if (del_cnt==mGp.syncHistoryAdapter.getCount()) del_all_history=true;
		NotifyEvent ntfy=new NotifyEvent(this);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				for (int i=mGp.syncHistoryAdapter.getCount()-1;i>=0;i--) {
					if (mGp.syncHistoryAdapter.getItem(i).isChecked) {
						String result_fp=mGp.syncHistoryAdapter.getItem(i).sync_result_file_path;
						if (!result_fp.equals("")) {
							File lf=new File(result_fp);
							if (lf.exists()) lf.delete();
						}
						mGp.syncHistoryAdapter.remove(mGp.syncHistoryAdapter.getItem(i));
					}
				}
				util.saveHistoryList(mGp.syncHistoryAdapter.getSyncHistoryList());
//				mGp.syncHistoryAdapter.setSyncHistoryList(util.loadHistoryList());
				mGp.syncHistoryAdapter.notifyDataSetChanged();
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		String subtitle="",msgtext="";
		if (del_all_history) {
//			subtitle=getString(R.string.msgs_sync_history_del_conf_subtitle);
			msgtext=getString(R.string.msgs_sync_history_del_conf_all_history);
		} else {
//			subtitle=getString(R.string.msgs_sync_history_del_conf_subtitle);
			msgtext=getString(R.string.msgs_sync_history_del_conf_selected_history)+
					conf_list;
		}
		
		commonDlg.showCommonDialog(true, "W",subtitle, msgtext, ntfy);
	};
	
	private void createHistoryContextMenu_Single(final int cin) { 

		ccMenu.addMenuItem(msgs_move_to_top,R.drawable.menu_top)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				mGp.syncHistoryListView.setSelection(0);
			}
		});
		
		ccMenu.addMenuItem(msgs_move_to_bottom,R.drawable.menu_bottom)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					mGp.syncHistoryListView.setSelection(mGp.syncHistoryAdapter.getCount()-1);
				}
		});

		final SyncHistoryListItem item = mGp.syncHistoryAdapter.getItem(cin);
		
		boolean log_enabled=false;
		if (!item.sync_result_file_path.equals("")) log_enabled=true;
		ccMenu.addMenuItem(log_enabled,
				getString(R.string.msgs_sync_history_ccmeu_show_log),R.drawable.ic_64_browse_text)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				Intent intent = 
						new Intent(android.content.Intent.ACTION_VIEW);
				intent.setDataAndType(
						Uri.parse("file://"+item.sync_result_file_path),
						"text/plain");
				startActivityForResult(intent,1);
			}
		});
		
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_delete,R.drawable.menu_trash)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					confirmDeleteHistory();
				}
		});
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_unselectall)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				setHistoryItemUnselectAll();
			}
		});
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_selectall)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				setHistoryItemSelectAll();
			}
		});
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_copy_clipboard)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				 ClipboardManager cm = 
					      (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				 SyncHistoryListItem hli=mGp.syncHistoryAdapter.getItem(cin);
				 
				 StringBuilder out= new StringBuilder(256);
				 out.append(hli.sync_date).append(" ");
				 out.append(hli.sync_time).append(" ");
				 out.append(hli.sync_prof).append("\n");
            	if (hli.sync_status==SyncHistoryListItem.SYNC_STATUS_SUCCESS) {
            		out.append(mContext.getString(R.string.msgs_sync_history_status_success)).append("\n");
            	} else if (hli.sync_status==SyncHistoryListItem.SYNC_STATUS_ERROR) {
            		out.append(mContext.getString(R.string.msgs_sync_history_status_fail)).append("\n");
            	} else if (hli.sync_status==SyncHistoryListItem.SYNC_STATUS_CANCELLED) {
            		out.append(mContext.getString(R.string.msgs_sync_history_status_cancel)).append("\n");
            	}
            	out.append(mContext.getString(R.string.msgs_sync_history_count_copied))
            		.append(Integer.toString(hli.sync_result_no_of_copied)).append(" ");
            	out.append(mContext.getString(R.string.msgs_sync_history_count_deleted))
        			.append(Integer.toString(hli.sync_result_no_of_deleted)).append(" ");
            	out.append(mContext.getString(R.string.msgs_sync_history_count_ignored))
        			.append(Integer.toString(hli.sync_result_no_of_ignored)).append(" ");
            	out.append("\n").append(hli.sync_error_text);
            	cm.setText(out);
			}
		});

		ccMenu.createMenu();
	};

//	private boolean mProfileListItemClickEnabled=true;
	private void setProfilelistItemClickListener() {
//		mProfileListItemClickEnabled=true;
		mGp.profileListView.setEnabled(true);
		mGp.profileListView
			.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				mGp.profileListView.setEnabled(false);
				ProfileListItem item = mGp.profileAdapter.getItem(position);
//				if (mGp.settingAltUiEnabled) {
//				} else {
//					item.setChecked(!item.isChecked());
//					mGp.profileListView.setEnabled(true);
//				}
				if (!ProfileUtility.isAnyProfileSelected(mGp.profileAdapter,SMBSYNC_PROF_GROUP_DEFAULT)) {
					editProfile(item.getName(),item.getType(),item.getActive(),position);
					mUiHandler.postDelayed(new Runnable(){
						@Override
						public void run() {
							mGp.profileListView.setEnabled(true);
						}
					},1000);
				} else {
					item.setChecked(!item.isChecked());
					mGp.profileListView.setEnabled(true);
				}
				mGp.profileAdapter.notifyDataSetChanged();
			}
		});
	};
	
	private void setProfilelistLongClickListener() {
		mGp.profileListView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				createProfileContextMenu(arg1, arg2);
				return true;
			}
		});
	};
	
	private void setMsglistViewListener() {
		CheckBox cb_freeze_scroll=(CheckBox)findViewById(R.id.message_view_scroll_freeze);
		cb_freeze_scroll.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				mGp.freezeMessageViewScroll=isChecked;
			}
		});
	}
	
	private void setMsglistLongClickListener() {
		mGp.msgListView
			.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int arg2, long arg3) {
				createMsglistContextMenu(arg1, arg2);
				return true;
			}
		});
	};
	
	private void createProfileContextMenu(View view, int idx) {
		int prev_selected_cnt=0;
		boolean sync=false;
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			if (mGp.profileAdapter.getItem(i).isChecked()) {
				prev_selected_cnt++;
			}
		}
		if (prev_selected_cnt==0) {//Not selected
			ProfileUtility.setProfileToChecked(true, mGp.profileAdapter, idx);
			mGp.profileAdapter.notifyDataSetChanged();
			if (mGp.profileAdapter.getItem(idx).getType().equals(SMBSYNC_PROF_TYPE_SYNC)) sync=true;
			createProfileContextMenu_Single(idx, sync);
		} else if (prev_selected_cnt==1) {//Previous selected was single
			for (int i=0;i<mGp.profileAdapter.getCount();i++) {
				if (mGp.profileAdapter.getItem(i).isChecked()) {
					if (i!=idx) {
						ProfileUtility.setProfileToChecked(false, mGp.profileAdapter, i);
						ProfileUtility.setProfileToChecked(true, mGp.profileAdapter, idx);
						mGp.profileAdapter.notifyDataSetChanged();
					}
				}
			}
			if (mGp.profileAdapter.getItem(idx).getType().equals(SMBSYNC_PROF_TYPE_SYNC)) sync=true;
			createProfileContextMenu_Single(idx, sync);
		} else {
			boolean already_selected=false;
			for (int i=0;i<mGp.profileAdapter.getCount();i++) {
				if (mGp.profileAdapter.getItem(i).isChecked()) {
					if (i==idx) {
						already_selected=true;
						break;
					}
				}
			}
			if (already_selected) {
				for (int i=0;i<mGp.profileAdapter.getCount();i++) {
					if (mGp.profileAdapter.getItem(i).isChecked()) 
						if (mGp.profileAdapter.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
							sync=true;
							break;
						}
				}
				createProfileContextMenu_Multiple(idx,sync);
			} else {
				resetAllCheckedItem();
				ProfileUtility.setProfileToChecked(true, mGp.profileAdapter, idx);
				mGp.profileAdapter.notifyDataSetChanged();
				if (mGp.profileAdapter.getItem(idx).getType().equals(SMBSYNC_PROF_TYPE_SYNC)) sync=true;
				createProfileContextMenu_Single(idx, sync);
			}
		}
	};
	
	private void createProfileContextMenu_Multiple(int idx,boolean sync) { 

		boolean sync_enabled=false;
		if (mGp.externalStorageIsMounted && !util.isRemoteDisable()&&sync) sync_enabled=true; 
		ccMenu.addMenuItem(sync_enabled, msgs_prof_cont_mult_sync,R.drawable.ic_32_sync)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				syncSelectedProfile();
//				resetAllCheckedItem();
			}
		});

		ccMenu.addMenuItem(msgs_prof_cont_mult_act,R.drawable.menu_active)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profUtil.setProfileToActive(mGp);
//					resetAllCheckedItem();
				}
		});

		ccMenu.addMenuItem(msgs_prof_cont_mult_inact,R.drawable.menu_inactive)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profUtil.setProfileToInactive();
//					resetAllCheckedItem();
				}
		});

		ccMenu.addMenuItem(msgs_prof_cont_mult_delete,R.drawable.menu_trash)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profUtil.deleteProfile();
//					resetAllCheckedItem();
				}
		});

		ccMenu.addMenuItem(msgs_prof_cont_select_all)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				for (int i=0;i<mGp.profileAdapter.getCount();i++) {
					ProfileUtility.setProfileToChecked(true, mGp.profileAdapter, i);
				}
				mGp.profileAdapter.notifyDataSetChanged();
			}
		});
		ccMenu.addMenuItem(msgs_prof_cont_unselect_all)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				resetAllCheckedItem();
			}
		});

		ccMenu.createMenu();
		
	};

	private void createProfileContextMenu_Single(final int cin, boolean sync) { 

		final ProfileListItem item = mGp.profileAdapter.getItem(cin);
		final String i_type = item.getType();
		final String i_act = item.getActive();
		final String i_name = item.getName();
		
		if (!i_type.equals("")) {
			boolean sync_enabled=false, inact_enabled=false, act_enabled=false;
			if (i_act.equals(SMBSYNC_PROF_ACTIVE)) {
				if (mGp.externalStorageIsMounted && !util.isRemoteDisable() ) {
					if (sync) {
						sync_enabled=true;
					}
				}
				inact_enabled=true;

			} else {
				act_enabled=true;
			}
			ccMenu.addMenuItem(sync_enabled, 
					String.format(msgs_sync_profile,i_name),R.drawable.ic_32_sync)
					.setOnClickListener(new CustomContextMenuOnClickListener() {
						@Override
						public void onClick(CharSequence menuTitle) {
							syncSelectedProfile();
//							resetAllCheckedItem();
						}
			});
			if (inact_enabled) {
				ccMenu.addMenuItem(
						String.format(msgs_prof_cont_sngl_inact,i_name),R.drawable.menu_inactive)
					.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						profUtil.setProfileToInactive();
//						resetAllCheckedItem();
					}
				});
			}
			if (act_enabled) {
				ccMenu.addMenuItem(act_enabled,
						String.format(msgs_prof_cont_sngl_act,i_name),R.drawable.menu_active)
					.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						profUtil.setProfileToActive(mGp);
//						resetAllCheckedItem();
					}
				});
			}
			
			ccMenu.addMenuItem(String.format(msgs_prof_cont_sngl_edit,i_name),R.drawable.menu_edit)
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
				  @Override
				  public void onClick(CharSequence menuTitle) {
					  editProfile(i_name, i_type,i_act, cin);
//					  resetAllCheckedItem();
				  }
			});
				
		    ccMenu.addMenuItem(String.format(msgs_prof_cont_sngl_delete,i_name),R.drawable.menu_trash)
				.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						profUtil.deleteProfile();
//						resetAllCheckedItem();
					}
			});
		};
		
		ccMenu.addMenuItem(String.format(msgs_prof_cont_sngl_wizard,i_name),R.drawable.ic_64_wizard)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				ProfileCreationWizard sw=new ProfileCreationWizard(mGp, mContext, 
						util, profUtil, commonDlg, mGp.profileAdapter);
				sw.wizardMain();
//				resetAllCheckedItem();
			}
	  	});

		ccMenu.addMenuItem(msgs_prof_cont_add_local,R.drawable.menu_add)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					ProfileMaintLocalFragment pmsp=ProfileMaintLocalFragment.newInstance();
					pmsp.showDialog(getSupportFragmentManager(), pmsp, "ADD", new ProfileListItem(), 
							0, profUtil, util, commonDlg);
//					resetAllCheckedItem();
				}
		});

		ccMenu.addMenuItem(msgs_prof_cont_add_remote,R.drawable.menu_add)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					String c_ip=SMBSyncUtil.getLocalIpAddress();
					ProfileListItem pfli=new ProfileListItem();
					pfli.setAddr(c_ip);
					
					ProfileMaintRemoteFragment pmsp=ProfileMaintRemoteFragment.newInstance();
					pmsp.showDialog(getSupportFragmentManager(), pmsp, "ADD", pfli, 
							0, profUtil, util, commonDlg);
//					resetAllCheckedItem();
				}
		});

//		boolean isRemoteExists=false, isLocalExists=false;
//		for (int i=0;i<glblParms.profileAdapter.getCount();i++) {
//			if (glblParms.profileAdapter.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
//				isRemoteExists=true;
//			} 
//			if (glblParms.profileAdapter.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
//				isLocalExists=true;
//			} 
//			if (isRemoteExists && isLocalExists) break;
//		}
//		if (isRemoteExists && isLocalExists) {
//		}
		ccMenu.addMenuItem(msgs_prof_cont_add_sync,R.drawable.menu_add)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				ProfileMaintSyncFragment pmsp=ProfileMaintSyncFragment.newInstance();
				pmsp.showDialog(getSupportFragmentManager(), pmsp, "ADD", new ProfileListItem(), 
						profUtil, util, commonDlg);
//				resetAllCheckedItem();
			}
		});

		if (!i_type.equals("")) {
			ccMenu.addMenuItem(msgs_prof_cont_copy,R.drawable.menu_copy)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profUtil.copyProfile(item);
//					resetAllCheckedItem();
				}
			});

			ccMenu.addMenuItem(msgs_prof_cont_rename,R.drawable.menu_rename)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profUtil.renameProfile(item);
//					resetAllCheckedItem();
				}
			});
			ccMenu.addMenuItem(msgs_prof_cont_select_all)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					for (int i=0;i<mGp.profileAdapter.getCount();i++) {
						ProfileUtility.setProfileToChecked(true, mGp.profileAdapter, i);
					}
					mGp.profileAdapter.notifyDataSetChanged();
				}
			});
			ccMenu.addMenuItem(msgs_prof_cont_unselect_all)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					resetAllCheckedItem();
				}
			});
		}
		// set onClick listener for mContext menu
		ccMenu.createMenu(new CustomContextMenuOnCleanupListener(){
			@Override
			public void onCleanup() {
//				Log.v("","onCleanup entered");
//				resetAllCheckedItem();
			}
		});
	};
	
	private void resetAllCheckedItem() {
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			ProfileUtility.setProfileToChecked(false, mGp.profileAdapter, i);
		}
		mGp.profileAdapter.notifyDataSetChanged();
	};
	
	private void createMsglistContextMenu(View view, int idx) {

		ccMenu.addMenuItem(msgs_move_to_top,R.drawable.menu_top)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					mGp.msgListView.setSelection(0);
				}
		});
		
		ccMenu.addMenuItem(msgs_move_to_bottom,R.drawable.menu_bottom)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					mGp.msgListView.setSelection(mGp.msgListView.getCount()-1);
				}
		});

		ccMenu.addMenuItem(msgs_clear_log_message,R.drawable.menu_clear)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					mGp.msgListView.setSelection(0);
					mGp.msgListAdapter.clear();
					util.addLogMsg("W",getString(R.string.msgs_log_msg_cleared));
				}
		});

		ccMenu.createMenu();
	};
	
	private void editProfile(String prof_name, String prof_type,
			String prof_act, int prof_num) {
		ProfileListItem item = mGp.profileAdapter.getItem(prof_num);
		if (prof_type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
			  ProfileMaintRemoteFragment pmp=ProfileMaintRemoteFragment.newInstance();
			  pmp.showDialog(getSupportFragmentManager(), pmp, "EDIT", item, 
					  prof_num, profUtil, util, commonDlg);
		} else if (prof_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
			  ProfileMaintLocalFragment pmp=ProfileMaintLocalFragment.newInstance();
			  pmp.showDialog(getSupportFragmentManager(), pmp, "EDIT", item, 
					  prof_num, profUtil, util, commonDlg);
		} else if (prof_type.equals(SMBSYNC_PROF_TYPE_SYNC)) {
			  ProfileMaintSyncFragment pmsp=ProfileMaintSyncFragment.newInstance();
			  pmsp.showDialog(getSupportFragmentManager(), pmsp, "EDIT", item, 
						profUtil, util, commonDlg);
		}
	};

	private void syncSelectedProfile() {
		ProfileListItem item ;
		ArrayList<MirrorIoParmList> alp = new ArrayList<MirrorIoParmList>();
		for (int i=0;i<mGp.profileAdapter.getCount();i++){
			item=mGp.profileAdapter.getItem(i);
			if (item.isChecked()&&item.isActive()) {
				if (item.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
					MirrorIoParmList alp_item =buildSyncParameter(item);
					if (alp_item!=null) alp.add(alp_item);			
				}
			}
		}
		
		if (alp.isEmpty()) {
			util.addLogMsg("E",msgs_sync_select_prof_no_active_profile);
			commonDlg.showCommonDialog(false, "E", "", msgs_sync_select_prof_no_active_profile, null);
		} else {
			tabHost.setCurrentTab(1);
			startMirrorTask(alp);
		}
	};
		
	private void syncActiveProfile() {
		ArrayList<MirrorIoParmList> alp = new ArrayList<MirrorIoParmList>();
		ProfileListItem item;

		for (int i=0;i< mGp.profileAdapter.getCount();i++) {
			item = mGp.profileAdapter.getItem(i);
			if (item.isActive() && item.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
				MirrorIoParmList alp_item =buildSyncParameter(item);
				if (alp_item!=null) alp.add(alp_item);			
			}
		}

		if (alp.isEmpty()) {
			util.addLogMsg("E",msgs_active_sync_prof_not_found);
			commonDlg.showCommonDialog(false, "E", "", msgs_active_sync_prof_not_found, null);
		} else {
			tabHost.setCurrentTab(1);
			startMirrorTask(alp);
		}
		
	};
	
	private void setUiEnabled() {
		util.addDebugLogMsg(1,"I","setUiEnabled entered");
		mGp.enableMainUi=true;
		TabWidget tw=(TabWidget)findViewById(android.R.id.tabs);
		tw.setEnabled(true);
		if (Build.VERSION.SDK_INT>=11) tw.setAlpha(1.0f);
//		mTabChildviewProf.setEnabled(true); 
//		mTabChildviewProf.setViewAlpha(1.0f);
//		mTabChildviewMsg.setEnabled(true);
//		mTabChildviewMsg.setViewAlpha(1.0f);
//		mTabChildviewHist.setEnabled(true);
//		mTabChildviewHist.setViewAlpha(1.0f);
		
		unsetOnKeyCallBackListener();
		
		refreshOptionMenu();
	};
	
	private void setUiDisabled() {
		util.addDebugLogMsg(1,"I","setUiDisabled entered");
		mGp.enableMainUi=false;
		TabWidget tw=(TabWidget)findViewById(android.R.id.tabs);
		tw.setEnabled(false);
		if (Build.VERSION.SDK_INT>=11) tw.setAlpha(0.4f);
//		mTabChildviewProf.setEnabled(false); 
//		mTabChildviewProf.setAlpha(0.4f);
//		mTabChildviewMsg.setEnabled(false);
//		mTabChildviewMsg.setViewAlpha(0.4f);
//		mTabChildviewHist.setEnabled(false);
//		mTabChildviewHist.setViewAlpha(0.4f);
		
		setOnKeyCallBackListener(new CallBackListener() {
			private long next_issued_time=0;
			@Override
			public boolean onCallBack(Context c, Object o1, Object[] o2) {
				if (next_issued_time<System.currentTimeMillis()) {
					Toast.makeText(mContext, 
						mContext.getString(R.string.msgs_dlg_hardkey_back_button), 
						Toast.LENGTH_SHORT)
						.show();
					next_issued_time=System.currentTimeMillis()+2000;
				}
				return true;
			}
		});

		refreshOptionMenu();
	};
	
	private boolean isUiEnabled() {
		return mGp.enableMainUi;
	};
	
	@SuppressLint("NewApi")
	final private void refreshOptionMenu() {
		util.addDebugLogMsg(1,"I","refreshOptionMenu entered");
		if (Build.VERSION.SDK_INT>=11)
			this.invalidateOptionsMenu();
	};

	private void startMirrorTask(ArrayList<MirrorIoParmList> alp) {
		mGp.progressSpinView.setVisibility(LinearLayout.VISIBLE);
		mGp.progressSpinView.setBackgroundColor(Color.BLACK);
		mGp.progressSpinView.bringToFront();

		mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_spin_dlg_sync_cancel));
		mGp.progressSpinCancel.setEnabled(true);
		// CANCELボタンの指定
		mGp.progressSpinCancelListener=new View.OnClickListener() {
			public void onClick(View v) {
				try {
					mSvcClient.aidlCancelThread();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				mGp.progressSpinCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
				mGp.progressSpinCancel.setEnabled(false);
				mGp.settingAutoTerm=false;
			}
		};
		mGp.progressSpinCancel.setOnClickListener(mGp.progressSpinCancelListener);
		
		mGp.msgListView.setFastScrollEnabled(false);

		mGp.mirrorIoParms=alp;

		setUiDisabled();
		mGp.mirrorThreadActive=true;
		try {
			mSvcClient.aidlStartThread();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		setScreenOn();
		acqWifiLock();

	};

	private WakeLock mDimScreenWakelock=null;
//	private void setScreenOn(int timeout) {
//		if (mGp.settingScreenOnEnabled) {
//			if (Build.VERSION.SDK_INT>=17) {
//				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) ;
//					util.addDebugLogMsg(1,"I","setScreenOn set KEEP_SCREEN_ON");
//			} else {
//				if (!mScreenOnWakelock.isHeld()) {
//			    	if (timeout==0) mScreenOnWakelock.acquire();
//			    	else mScreenOnWakelock.acquire(timeout);
//					util.addDebugLogMsg(1,"I","Wakelock acquired");
//				} else {
//					util.addDebugLogMsg(1,"I","Wakelock not acquired, because Wakelock already acquired");
//				}
//			}
//		}
//	};
//	
//	private void clearScreenOn() {
//		if (mGp.settingScreenOnEnabled) {
//			if (Build.VERSION.SDK_INT>=17) {
//				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) ;
//				util.addDebugLogMsg(1,"I","clearScreenOn clear KEEP_SCREEN_ON");
//			} else {
//				if (mScreenOnWakelock.isHeld()) {
//					util.addDebugLogMsg(1,"I","Wakelock released");
//					mScreenOnWakelock.release();
//				} else {
//					util.addDebugLogMsg(1,"I","Wakelock not relased, because Wakelock not acquired");
//				}
//			}
//		}
//	};

	private void acqWifiLock() {
		if (mGp.settingWifiLockRequired) {
			if (!mWifiLock.isHeld()) {
				mWifiLock.acquire();
				util.addDebugLogMsg(1,"I","WifiLock acquired");
			} else {
				util.addDebugLogMsg(1,"I","WifiLock not acquired, because WifiLock already acquired");
			}
		}
	};
	
	private void relWifiLock() {
		if (mGp.settingWifiLockRequired) {
			if (mWifiLock.isHeld()) {
				mWifiLock.release();
				util.addDebugLogMsg(1,"I","WifiLock released");
			} else {
				util.addDebugLogMsg(1,"I","WifiLock not released, because WifiLock not acquired");
			}
		}
	};
	
	private void setScreenOn() {
		try {
			mSvcClient.aidlAcqWakeLock();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (!mGp.settingScreenOnOption.equals(GlobalParameters.KEEP_SCREEN_ON_DISABLED)) {
			if (mGp.settingScreenOnOption.equals(GlobalParameters.KEEP_SCREEN_ON_ALWAYS) ||
					!isKeyguardEffective(mContext)) {
				if (!mDimScreenWakelock.isHeld()) {
			    	mDimScreenWakelock.acquire();
					util.addDebugLogMsg(1,"I","Dim screen wakelock acquired");
				} else {
					util.addDebugLogMsg(1,"I","Dim screen wakelock not acquired, because Wakelock already acquired");
				}
			} else {
				util.addDebugLogMsg(1,"I","Dim screen wakelock not acquired, because screen is alread locked");
			}
		}
	};
	
	static final private boolean isKeyguardEffective(Context mContext) {
        KeyguardManager keyguardMgr=
        		(KeyguardManager)mContext.getSystemService(Context.KEYGUARD_SERVICE);
    	boolean result=keyguardMgr.inKeyguardRestrictedInputMode();
    	return result;
    };

	private void clearScreenOn() {
		try {
			if (mSvcClient!=null) mSvcClient.aidlRelWakeLock();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		if (mDimScreenWakelock.isHeld()) {
			util.addDebugLogMsg(1,"I","Dim screen wakelock released");
			mDimScreenWakelock.release();
		} else {
			util.addDebugLogMsg(1,"I","Dim screen wakelock not relased, because Wakelock not acquired");
		}
//		if (mGp.settingScreenOnEnabled) {
//		}
	};

	private void mirrorTaskEnded(String result_code, String result_msg) {
		setUiEnabled();

		final LinearLayout ll_spin=(LinearLayout)findViewById(R.id.profile_progress_spin);
		ll_spin.setVisibility(LinearLayout.GONE);
		
//		mGp.syncHistoryAdapter=new AdapterSyncHistory(mContext, R.layout.sync_history_list_item_view, 
//				mGp.syncHistoryList);
//		mGp.syncHistoryListView.setAdapter(mGp.syncHistoryAdapter);
//		setHistoryViewItemClickListener();
		mGp.syncHistoryAdapter.setSyncHistoryList(mGp.syncHistoryList);
		mGp.syncHistoryAdapter.notifyDataSetChanged();
		
//		playBackDefaultNotification();
//		vibrateDefaultPattern();

		mGp.msgListView.setFastScrollEnabled(true);
		util.flushLogFile();
		setWifiOff();
		if ((!isExtraSpecAutoTerm && mGp.settingAutoStart && mGp.settingAutoTerm) || 
				(isExtraSpecAutoTerm && extraValueAutoTerm)) {
			if (mGp.settingErrorOption) {
				showMirrorThreadResult(result_code,result_msg);
				autoTerminateDlg(result_code, result_msg);
			} else {
				if (result_code.equals("OK")) {
					showMirrorThreadResult(result_code,result_msg);
					autoTerminateDlg(result_code, result_msg);
				} else {
					mGp.settingAutoTerm=false;
					showMirrorThreadResult(result_code,result_msg);
					if (!util.isActivityForeground()) saveTaskData();
					util.rotateLogFile();
					mGp.mirrorThreadActive=false;
				}		
			}
		} else {
			showMirrorThreadResult(result_code,result_msg);
			util.rotateLogFile();
			if (!util.isActivityForeground()) saveTaskData();
			mGp.mirrorThreadActive=false;
		}
	};
	
	private void playBackDefaultNotification() {
//		Thread th=new Thread(){
//			@Override
//			public void run() {
//				Uri uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
//				if (uri!=null) {
////					Ringtone rt=RingtoneManager.getRingtone(mContext, uri);
////					rt.play();
////					SystemClock.sleep(1000);
////					rt.stop();
//					MediaPlayer player = MediaPlayer.create(mContext, uri);
//					if (player!=null) {
//						int dur=player.getDuration();
//						player.start();
//						SystemClock.sleep(dur+10);
//						player.stop();
//						player.reset();
//						player.release();
//					}
//				}
//			}
//		};
//		th.start();
		Uri uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
		if (uri!=null) {
			MediaPlayer player = MediaPlayer.create(mContext, uri);
			if (player!=null) {
				int dur=player.getDuration();
				player.start();
				SystemClock.sleep(dur+10);
				player.stop();
				player.reset();
				player.release();
			}
		}
	};

	private void vibrateDefaultPattern() {
		Thread th=new Thread(){
			@Override
			public void run() {
				Vibrator vibrator = (Vibrator)mContext.getSystemService(Context.VIBRATOR_SERVICE);
				vibrator.vibrate(new long[]{0,200,400,200,400,200},-1);
			}
		};
		th.start();
    };
    
	private void showMirrorThreadResult(final String code, final String text) {
//		if (!mGp.settingAutoTerm)
//			if (util.isActivityForeground())
//				commonDlg.showCommonDialog(false,"I",text,"",null);
		if (mGp.settingBgTermNotifyMsg.equals(SMBSYNC_BG_TERM_NOTIFY_MSG_ALWAYS))
			showNotificationMsg(text);
//		Log.v("","code="+code+", pb="+glblParms.settingRingtoneWhenSyncEnded+", vib="+glblParms.settingVibrateWhenSyncEnded);
		if (code.equals("OK")) {
			if (mGp.settingVibrateWhenSyncEnded.equals(SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ALWAYS) ||
				mGp.settingVibrateWhenSyncEnded.equals(SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_SUCCESS)) {
				vibrateDefaultPattern();
			}
			if (mGp.settingRingtoneWhenSyncEnded.equals(SMBSYNC_PB_RINGTONE_NOTIFICATION_ALWAYS) ||
				mGp.settingRingtoneWhenSyncEnded.equals(SMBSYNC_PB_RINGTONE_NOTIFICATION_SUCCESS)) {
				playBackDefaultNotification();
			}
		} else {
			if (mGp.settingVibrateWhenSyncEnded.equals(SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ALWAYS) ||
				mGp.settingVibrateWhenSyncEnded.equals(SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ERROR)) {
				vibrateDefaultPattern();
			}
			if (mGp.settingRingtoneWhenSyncEnded.equals(SMBSYNC_PB_RINGTONE_NOTIFICATION_ALWAYS) ||
				mGp.settingRingtoneWhenSyncEnded.equals(SMBSYNC_PB_RINGTONE_NOTIFICATION_ERROR)) {
				playBackDefaultNotification();
			}
		}
	};

	private ISvcCallback mSvcCallbackStub=new ISvcCallback.Stub() {
		@Override
		public void cbThreadEnded(final String result_code, final String result_msg)
				throws RemoteException {
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					mirrorTaskEnded(result_code, result_msg);
					clearScreenOn();
					relWifiLock();
//					setScreenOn(500);
				}
			});
		}

		@Override
		public void cbShowConfirm(final String fp, final String copy_or_delete)
				throws RemoteException {
			mUiHandler.post(new Runnable(){
				@Override
				public void run() {
					showConfirmDialog(fp, copy_or_delete);
				}
			});
		}

		@Override
		public void cbSendMessage(final String cat, final String flag,
				final String sync_prof, final String date, final String time, 
				final String tag,final String debug_or_msg, final String fp, final String msg_text)
				throws RemoteException {
		}
    };

	private ISvcClient mSvcClient=null;
	
	private void openService(final NotifyEvent p_ntfy) {
 		util.addDebugLogMsg(1,"I","openService entered");
        mSvcConnection = new ServiceConnection(){
    		public void onServiceConnected(ComponentName arg0, IBinder service) {
    	    	util.addDebugLogMsg(1,"I","onServiceConnected entered");
    	    	mSvcClient=ISvcClient.Stub.asInterface(service);
   	    		p_ntfy.notifyToListener(true, null);
    		}
    		public void onServiceDisconnected(ComponentName name) {
    			mSvcConnection = null;
   				util.addDebugLogMsg(1,"I","onServiceDisconnected entered");
    	    	mSvcClient=null;
    	    	synchronized(tcService) {
        	    	tcService.notify();
    	    	}
    		}
        };
    	
		Intent intmsg = new Intent(mContext, SMBSyncService.class);
		intmsg.setAction("MessageConnection");
        bindService(intmsg, mSvcConnection, BIND_AUTO_CREATE);
	};

	private void closeService() {
    	
		util.addDebugLogMsg(1,"I","closeService entered");

    	if (mSvcConnection!=null) {
    		try {
				if (mSvcClient!=null) mSvcClient.aidlStopService();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
    		mSvcClient=null;
    		unbindService(mSvcConnection);
	    	mSvcConnection=null;
//	    	Log.v("","close service");
    	}
        Intent intent = new Intent(this, SMBSyncService.class);
        stopService(intent);
	};
	
	final private void setCallbackListener() {
		util.addDebugLogMsg(1, "I", "setCallbackListener entered");
		try{
			mSvcClient.setCallBack(mSvcCallbackStub);
		} catch (RemoteException e){
			e.printStackTrace();
			util.addDebugLogMsg(0,"E", "setCallbackListener error :"+e.toString());
		}
	};

	final private void unsetCallbackListener() {
		if (mSvcClient!=null) {
			try{
				mSvcClient.removeCallBack(mSvcCallbackStub);
			} catch (RemoteException e){
				e.printStackTrace();
				util.addDebugLogMsg(0,"E", "unsetCallbackListener error :"+e.toString());
			}
		}
	};

	private void showConfirmDialog(String fp, String method) {
    	
			util.addDebugLogMsg(1,"I","showConfirmDialog entered");
		final NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				try {
					mSvcClient.aidlConfirmResponse((Integer)o[0]);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
				try {
					mSvcClient.aidlConfirmResponse((Integer)o[0]);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		
		mGp.confirmView.setVisibility(LinearLayout.VISIBLE);
		mGp.confirmView.setBackgroundColor(Color.BLACK);
		mGp.confirmView.bringToFront();
		mGp.confirmTitle.setText(mContext.getString(R.string.msgs_common_dialog_warning));
		mGp.confirmTitle.setTextColor(Color.YELLOW);
		String msg_text="";
		if (method.equals(SMBSYNC_CONFIRM_FOR_COPY)) {
			msg_text=String.format(getString(R.string.msgs_mirror_confirm_copy_confirm),fp);
		} else {
			msg_text=String.format(getString(R.string.msgs_mirror_confirm_delete_confirm),fp);
		}
		mGp.confirmMsg.setText(msg_text);
		
		showNotificationMsg(msg_text);
		
		if (method.equals(SMBSYNC_CONFIRM_FOR_COPY)) mGp.confirmMsg.setText(
				String.format(getString(R.string.msgs_mirror_confirm_copy_confirm),fp));
		else mGp.confirmMsg.setText(String.format(getString(R.string.msgs_mirror_confirm_delete_confirm),fp));
		
		// Yesボタンの指定
		mGp.confirmYesListener=new View.OnClickListener() {
			public void onClick(View v) {
				mGp.confirmView.setVisibility(LinearLayout.GONE);
				ntfy.notifyToListener(true,new Object[]{SMBSYNC_CONFIRM_RESP_YES});
			}
		};
		mGp.confirmYes.setOnClickListener(mGp.confirmYesListener);
		// YesAllボタンの指定
		mGp.confirmYesAllListener=new View.OnClickListener() {
			public void onClick(View v) {
				mGp.confirmView.setVisibility(LinearLayout.GONE);
				ntfy.notifyToListener(true,new Object[]{SMBSYNC_CONFIRM_RESP_YESALL});
			}
		};
		mGp.confirmYesAll.setOnClickListener(mGp.confirmYesAllListener);
		// Noボタンの指定
		mGp.confirmNoListener=new View.OnClickListener() {
			public void onClick(View v) {
				mGp.confirmView.setVisibility(LinearLayout.GONE);
				ntfy.notifyToListener(false,new Object[]{SMBSYNC_CONFIRM_RESP_NO});
			}
		};
		mGp.confirmNo.setOnClickListener(mGp.confirmNoListener);
		// NoAllボタンの指定
		mGp.confirmNoAllListener=new View.OnClickListener() {
			public void onClick(View v) {
				mGp.confirmView.setVisibility(LinearLayout.GONE);
				ntfy.notifyToListener(false,new Object[]{SMBSYNC_CONFIRM_RESP_NOALL});
			}
		};
		mGp.confirmNoAll.setOnClickListener(mGp.confirmNoAllListener);
		// Task cancelボタンの指定
		mGp.confirmCancelListener=new View.OnClickListener() {
			public void onClick(View v) {
				mGp.confirmView.setVisibility(LinearLayout.GONE);
//				try {
//					mSvcClient.aidlCancelThread();
//				} catch (RemoteException e) {
//					e.printStackTrace();
//				}
				ntfy.notifyToListener(false,new Object[]{SMBSYNC_CONFIRM_RESP_NOALL});
				mGp.progressSpinCancel.performClick();
			}
		};
		mGp.confirmCancel.setOnClickListener(mGp.confirmCancelListener);
	};

	private void autoStartDlg() {
		final ThreadCtrl threadCtl=new ThreadCtrl();
		threadCtl.setEnabled();//enableAsyncTask();

		mGp.progressBarView.setVisibility(LinearLayout.VISIBLE);
		mGp.progressBarView.setBackgroundColor(Color.BLACK);
		mGp.progressBarView.bringToFront();

		mGp.progressBarMsg.setText(getString(R.string.msgs_progress_bar_dlg_astart_starting));
		mGp.progressBarCancel.setText(getString(R.string.msgs_progress_bar_dlg_astart_cancel));
		mGp.progressBarImmed.setText(getString(R.string.msgs_progress_bar_dlg_astart_immediate));
		
		// CANCELボタンの指定
		mGp.progressBarCancel.setEnabled(true);
		mGp.progressBarCancelListener=new View.OnClickListener() {
			public void onClick(View v) {
				mGp.settingAutoTerm=false;
				mGp.progressBarCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
				mGp.progressBarCancel.setEnabled(false);
				threadCtl.setDisabled();//disableAsyncTask();
				util.addLogMsg("W",getString(R.string.msgs_astart_canceling));
				showNotificationMsg(getString(R.string.msgs_astart_canceling));
			}
		};
		mGp.progressBarCancel.setOnClickListener(mGp.progressBarCancelListener);
		
		final NotifyEvent at_ne=new NotifyEvent(mContext);
		at_ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				util.addDebugLogMsg(1,"I","Auto timer was expired.");
//				showNotificationMsg(getString(R.string.msgs_astart_expired));
				if (mGp.settingAutoStart || (isExtraSpecAutoStart && extraValueAutoStart)){
					util.addDebugLogMsg(1,"I","Auto sync was invoked.");
					boolean sel_prof=false;
					for (int i=0;i<mGp.profileAdapter.getCount();i++) {
						if (mGp.profileAdapter.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_SYNC) && 
								mGp.profileAdapter.getItem(i).isChecked()) {
							sel_prof=true;
						}
					}
					if (sel_prof) syncSelectedProfile();
					else syncActiveProfile();
				}
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
				util.addLogMsg("W",getString(R.string.msgs_astart_cancelled));
				showNotificationMsg(getString(R.string.msgs_astart_cancelled));
				mGp.mirrorThreadActive=false;
				clearScreenOn();
			}
		});

		// Immediateボタンの指定
		mGp.progressBarImmed.setEnabled(true);
		mGp.progressBarImmedListener=new View.OnClickListener() {
			public void onClick(View v) {
				mRequestAutoTimerExpired=true;
				threadCtl.setDisabled();//disableAsyncTask();
			}
		};
		mGp.progressBarImmed.setOnClickListener(mGp.progressBarImmedListener);

		showNotificationMsg(getString(R.string.msgs_astart_started));
		util.addLogMsg("I",getString(R.string.msgs_astart_started));
		tabHost.setCurrentTab(1);
		if (extraValueAutoStart) {
			mGp.progressBarView.setVisibility(LinearLayout.GONE);
			at_ne.notifyToListener(true, null);
		} else {
			mRequestAutoTimerExpired=false;
			autoTimer(threadCtl, at_ne,getString(R.string.msgs_astart_after));
		}
	};

	private void autoTerminateDlg(final String result_code, final String result_msg) {
		final ThreadCtrl threadCtl=new ThreadCtrl();
		threadCtl.setEnabled();//enableAsyncTask();

		mGp.progressBarView.setVisibility(LinearLayout.VISIBLE);
		mGp.progressBarView.setBackgroundColor(Color.BLACK);
		mGp.progressBarView.bringToFront();

		mGp.progressBarMsg.setText("");
		mGp.progressBarCancel.setText(getString(R.string.msgs_progress_bar_dlg_aterm_cancel));
		mGp.progressBarImmed.setText(getString(R.string.msgs_progress_bar_dlg_aterm_immediate));

		// CANCELボタンの指定
		mGp.progressBarCancel.setEnabled(true);
		mGp.progressBarCancelListener=new View.OnClickListener() {
			public void onClick(View v) {
				mGp.settingAutoTerm=false;
				mGp.progressBarCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
				mGp.progressBarCancel.setEnabled(false);
				threadCtl.setDisabled();//disableAsyncTask();
				util.addLogMsg("W",getString(R.string.msgs_aterm_canceling));
				showNotificationMsg(getString(R.string.msgs_aterm_canceling));
			}
		};
		mGp.progressBarCancel.setOnClickListener(mGp.progressBarCancelListener);
		
		final NotifyEvent at_ne=new NotifyEvent(mContext);
		at_ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				util.addLogMsg("I",getString(R.string.msgs_aterm_expired));
				if (mGp.settingAutoTerm || (isExtraSpecAutoStart && extraValueAutoStart)){
					svcStopForeground(true);
					//Wait until stopForeground() completion 
					Handler hndl=new Handler();
					hndl.post(new Runnable(){
						@Override
						public void run() {
							util.addDebugLogMsg(1,"I","Auto termination was invoked.");
							if (!mGp.settingBgTermNotifyMsg.equals(SMBSYNC_BG_TERM_NOTIFY_MSG_NO)) {
//								Log.v("","result code="+result_code+", result_msg="+result_msg);
								if (mGp.settingBgTermNotifyMsg.equals(SMBSYNC_BG_TERM_NOTIFY_MSG_ALWAYS)) 
									NotificationUtil.showNoticeMsg(mContext,mGp,result_msg);
								else {
									if (!result_code.equals("OK")) {
										NotificationUtil.showNoticeMsg(mContext,mGp,result_msg);
									} else {
										NotificationUtil.clearNotification(mGp);
									}
								}
							} else {
								NotificationUtil.clearNotification(mGp);
							}
//							saveTaskData();
							util.flushLogFile();
							terminateApplication();
						}
					});
				}
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
				util.addLogMsg("W",getString(R.string.msgs_aterm_cancelled));
				showNotificationMsg(getString(R.string.msgs_aterm_cancelled));
				util.rotateLogFile();
				mGp.mirrorThreadActive=false;
			}
		});
		
		// Immediateボタンの指定
		mGp.progressBarImmed.setEnabled(true);
		mGp.progressBarImmedListener=new View.OnClickListener() {
			public void onClick(View v) {
				mRequestAutoTimerExpired=true;
				threadCtl.setDisabled();//disableAsyncTask();
			}
		};
		mGp.progressBarImmed.setOnClickListener(mGp.progressBarImmedListener);

//		Log.v("","e="+extraDataSpecifiedAutoTerm);
		util.addLogMsg("I",getString(R.string.msgs_aterm_started));
		if (extraValueAutoTerm) {
			util.addLogMsg("I", getString(R.string.msgs_aterm_back_ground_term));
			at_ne.notifyToListener(true, null);
		} else if (!util.isActivityForeground()) {
			util.addLogMsg("I", getString(R.string.msgs_aterm_back_ground_term));
			at_ne.notifyToListener(true, null);
		} else {
			mRequestAutoTimerExpired=false;
			autoTimer(threadCtl, at_ne, getString(R.string.msgs_aterm_terminate_after));
		}
	};

	@SuppressWarnings("unused")
	private void setNotificationIcon(int icon_res) {
		try {
			mSvcClient.aidlSetNotificationIcon(icon_res);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	};
	
	private boolean mRequestAutoTimerExpired=false;
	private void autoTimer( 
			final ThreadCtrl threadCtl, final NotifyEvent at_ne, final String msg) {
		setUiDisabled();
		final Handler handler=new Handler();
       	new Thread(new Runnable() {
			@Override
			public void run() {//non UI thread
				mGp.progressBarPb.setMax(ATERM_WAIT_TIME);
				for (int i=0; i<ATERM_WAIT_TIME;i++) {
					try {
						if(threadCtl.isEnabled()) {
							final int ix=i;
							handler.post(new Runnable() {
								//UI thread
								@Override
								public void run() {
									String t_msg=String.format(msg, (ATERM_WAIT_TIME-ix));
									mGp.progressBarMsg.setText(t_msg);
									showNotificationMsg(t_msg);
							}});
							// non UI thread
							mGp.progressBarPb.setProgress(i);
						} else {
							break;
						}
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				// dismiss progress bar dialog
				handler.post(new Runnable() {// UI thread
					@Override
					public void run() {
						LinearLayout ll_bar=(LinearLayout)findViewById(R.id.profile_progress_bar);
						ll_bar.setVisibility(LinearLayout.GONE);
						setUiEnabled();
						if (mRequestAutoTimerExpired) {//Immediate process requested
							at_ne.notifyToListener(true, null);
						} else {
							if(threadCtl.isEnabled())at_ne.notifyToListener(true, null);
							else at_ne.notifyToListener(false, null);
						}
					}
				});
			}
		})
       	.start();
	};
	
//	private void reshowOngoingNotificationMsg() {
//		try {
//			mSvcClient.aidlReshowNotificationMsg();
//		} catch (RemoteException e) {
//			e.printStackTrace();
//		}
//	}
	
	private void showNotificationMsg(String msg ) {
		try {
			mSvcClient.aidlShowNotificationMsg("","",msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	};

	@SuppressWarnings("unused")
	private void showNotificationMsg(String prof, String msg ) {
		try {
			mSvcClient.aidlShowNotificationMsg(prof,"",msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	};
	@SuppressWarnings("unused")
	private void showNotificationMsg(String prof, String fp, String msg ) {
		try {
			mSvcClient.aidlShowNotificationMsg(prof,fp,msg);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	};

	private MirrorIoParmList buildSyncParameter(ProfileListItem item) {
		MirrorIoParmList alp = null;
		String mirror_prof_master_type = "",mirror_prof_target_type = "", 
				remote_prof_addr = "", 
				remote_prof_share = "", remote_prof_dir = "", 
				local_prof_dir = "", remote_prof_userid = "", 
				remote_prof_pass = "", mirror_prof_type="";
		String mp_profname, mp_master_name, mp_target_name;
		String local_prof_lmp="";
		String remote_host_name="";
		String remote_host_port="";

		String master_local_dir="", master_local_mp="";
		String target_local_dir="", target_local_mp="";
		
		mp_profname = item.getName();
		mp_master_name = item.getMasterName();
		mp_target_name = item.getTargetName();
		mirror_prof_type = item.getSyncType();

		boolean build_success_master = false;
		boolean build_success_target = false;
		
		for (int j = 0; j <= mGp.profileAdapter.getCount() - 1; j++) {
			ProfileListItem item_master = mGp.profileAdapter.getItem(j);
			if (item_master.getName().equals(mp_master_name)) {
				if (item_master.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
					local_prof_dir = item_master.getDir();
					local_prof_lmp=item_master.getLocalMountPoint();
					master_local_dir=item_master.getDir();
					master_local_mp=item_master.getLocalMountPoint();
					mirror_prof_master_type = "L";// Mirror local -> remote
				} else {
					remote_prof_addr = item_master.getAddr();
					remote_host_name = item_master.getHostname();
					remote_host_port = item_master.getPort();
					remote_prof_share = item_master.getShare();
					remote_prof_userid = item_master.getUser();
					remote_prof_pass = item_master.getPass();
					remote_prof_dir = item_master.getDir();
					mirror_prof_master_type = "R";// Mirror remote -> local
				}
				build_success_master=true;
			}
		}

		for (int k = 0; k <= mGp.profileAdapter.getCount() - 1; k++) {
			ProfileListItem item_target = mGp.profileAdapter.getItem(k);
			if (item_target.getName().equals(mp_target_name)) {
				if (item_target.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
					local_prof_dir = item_target.getDir();
					local_prof_lmp=item_target.getLocalMountPoint();
					target_local_dir=item_target.getDir();
					target_local_mp=item_target.getLocalMountPoint();
					mirror_prof_target_type = "L";
				} else {
					remote_prof_addr = item_target.getAddr();
					remote_host_name = item_target.getHostname();
					remote_host_port = item_target.getPort();
					remote_prof_share = item_target.getShare();
					remote_prof_userid = item_target.getUser();
					remote_prof_pass = item_target.getPass();
					remote_prof_dir = item_target.getDir();
				}
				if (mirror_prof_master_type.equals("R") && mirror_prof_target_type.equals("R")) {
				} else {
					build_success_target=true;
				}
			}
		}
		util.addDebugLogMsg(9,"I","alp="+mp_profname+
				","+mirror_prof_master_type+
				","+mirror_prof_target_type+
					","+mirror_prof_type+","+remote_prof_addr+","+remote_host_name+
					","+remote_host_port+","+remote_prof_share+
					","+remote_prof_dir+","+remote_prof_userid+","+remote_prof_pass+
					","+local_prof_lmp+","+local_prof_dir+
					","+master_local_mp+","+master_local_dir+
					","+target_local_mp+","+target_local_dir+
					","+item.getFileFilter()+","+item.getDirFilter());
		if (build_success_master && build_success_target) {
					String t_dir="";
					if (local_prof_dir.equals("")) {
						t_dir=local_prof_lmp;
					} else {
						t_dir=local_prof_lmp+"/"+local_prof_dir;
					}
					alp = new MirrorIoParmList(
					mp_profname,
					mirror_prof_master_type,
					mirror_prof_target_type,
					mp_target_name,
					mirror_prof_type,
					remote_prof_addr,
					remote_host_name,
					remote_host_port,
					remote_prof_share,
					remote_prof_dir,
					local_prof_lmp,
					t_dir,
					remote_prof_userid,
					remote_prof_pass,
					item.getFileFilter(),
					item.getDirFilter(),
					item.isMasterDirFileProcess(),
					item.isConfirmRequired(),
					item.isForceLastModifiedUseSmbsync(),
					item.isNotUseLastModifiedForRemote(),
					Integer.parseInt(item.getRetryCount()),
					item.isSyncEmptyDirectory(),
					item.isSyncHiddenDirectory(),
					item.isSyncHiddenFile(),
					item.isSyncSubDirectory());
					if (mirror_prof_master_type.equals("L") && mirror_prof_target_type.equals("L")) {
						alp.setMasterLocalDir(master_local_dir);
						alp.setMasterLocalMountPoint(master_local_mp);
						alp.setTargetLocalDir(target_local_dir);
						alp.setTargetLocalMountPoint(target_local_mp);
					}
		} else {
			if (mirror_prof_master_type.equals("R") && mirror_prof_target_type.equals("R")) {
				util.addLogMsg("E",String.format(msgs_invalid_profile_combination,
						mp_profname));
			} else {
				if (!build_success_master)
					util.addLogMsg("E",String.format(msgs_master_profile_not_found,
						mp_profname));
				if (!build_success_target)
					util.addLogMsg("E",String.format(msgs_target_profile_not_found,
							mp_profname));
			}
		}
		return alp;
	};
	
	final private boolean checkJcifsOptionChanged() {
		boolean changed=false;
		
		String prevSmbLogLevel=settingsSmbLogLevel,	prevSmbRcvBufSize=settingsSmbRcvBufSize,
				prevSmbSndBufSize=settingsSmbSndBufSize, prevSmbListSize=settingsSmbListSize,
				prevSmbMaxBuffers=settingsSmbMaxBuffers, prevSmbTcpNodelay=settingsSmbTcpNodelay,
				prevSmbPerfClass=settingsSmbPerfClass,
				prevSmbLmCompatibility=settingsSmbLmCompatibility,
				prevSmbUseExtendedSecurity=settingsSmbUseExtendedSecurity;
		
		initJcifsOption();
		
		if (!settingsSmbLmCompatibility.equals(prevSmbLmCompatibility)) changed=true;
		else if (!settingsSmbUseExtendedSecurity.equals(prevSmbUseExtendedSecurity)) changed=true;
		
		if (!changed) {
			if (settingsSmbPerfClass.equals("0") || settingsSmbPerfClass.equals("1") ||
					settingsSmbPerfClass.equals("2")) {
				if (!settingsSmbPerfClass.equals(prevSmbPerfClass)) {
					changed=true;
//					Log.v("","perfClass");
				}
			} else {
				if (!settingsSmbLogLevel.equals(prevSmbLogLevel)) {
					changed=true;
//					Log.v("","logLevel");
				} else if (!settingsSmbRcvBufSize.equals(prevSmbRcvBufSize)) {
					changed=true;
//					Log.v("","rcvBuff");
				}
				else if (!settingsSmbSndBufSize.equals(prevSmbSndBufSize)) {
					changed=true;
//					Log.v("","sndBuff");
				}
				else if (!settingsSmbListSize.equals(prevSmbListSize)) {
					changed=true;
//					Log.v("","listSize");
				}
				else if (!settingsSmbMaxBuffers.equals(prevSmbMaxBuffers)) {
					changed=true;
//					Log.v("","maxBuff");
				}
				else if (!settingsSmbTcpNodelay.equals(prevSmbTcpNodelay)) {
					changed=true;
//					Log.v("","tcpNodelay");
				}
			}
		}
		if (changed) {
			commonDlg.showCommonDialog(false,"W",
					"",mContext.getString(R.string.msgs_smbsync_main_settings_jcifs_changed_restart),null);
		}

		return changed;
	};

	private String settingsSmbLogLevel="0",	settingsSmbRcvBufSize="16644",
			settingsSmbSndBufSize="16644",	settingsSmbListSize="130170",
			settingsSmbMaxBuffers="", settingsSmbTcpNodelay="false", 
			settingsSmbPerfClass="0",
			settingsSmbLmCompatibility="0",settingsSmbUseExtendedSecurity="true";

	final private void initJcifsOption() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		settingsSmbPerfClass=prefs.getString(mContext.getString(R.string.settings_smb_perform_class), "");
		if (settingsSmbPerfClass.equals("0")) {//Minimum
			settingsSmbLogLevel="0";
			settingsSmbRcvBufSize="16644";
			settingsSmbSndBufSize="16644";
			settingsSmbListSize="130170";
			settingsSmbMaxBuffers="";
			settingsSmbTcpNodelay="false";
		} else if (settingsSmbPerfClass.equals("1")) {//Medium
			settingsSmbLogLevel="0";
			settingsSmbRcvBufSize="33288";
			settingsSmbSndBufSize="33288";
			settingsSmbListSize="130170";
			settingsSmbMaxBuffers="100";
			settingsSmbTcpNodelay="false";
		} else if (settingsSmbPerfClass.equals("2")) {//Large
			settingsSmbLogLevel="0";
			settingsSmbRcvBufSize="66576";
			settingsSmbSndBufSize="66576";
			settingsSmbListSize="130170";
			settingsSmbMaxBuffers="100";
			settingsSmbTcpNodelay="false";
		} else {
			settingsSmbLogLevel=
					prefs.getString(mContext.getString(R.string.settings_smb_log_level), "0");
			if (settingsSmbLogLevel.length()==0) settingsSmbLogLevel="0";
			
			settingsSmbRcvBufSize=
					prefs.getString(mContext.getString(R.string.settings_smb_rcv_buf_size),"66576");
			settingsSmbSndBufSize=
					prefs.getString(mContext.getString(R.string.settings_smb_snd_buf_size),"66576");
			settingsSmbListSize=
					prefs.getString(mContext.getString(R.string.settings_smb_listSize), "");
			settingsSmbMaxBuffers=
					prefs.getString(mContext.getString(R.string.settings_smb_maxBuffers), "100");
			settingsSmbTcpNodelay=
					prefs.getString(mContext.getString(R.string.settings_smb_tcp_nodelay),"false");
		}
			
		settingsSmbLmCompatibility=
			prefs.getString(mContext.getString(R.string.settings_smb_lm_compatibility),"0");
		boolean ues=
				prefs.getBoolean(mContext.getString(R.string.settings_smb_use_extended_security),false);
		settingsSmbUseExtendedSecurity="";
		if (ues) settingsSmbUseExtendedSecurity="true";
		else settingsSmbUseExtendedSecurity="false";
		
		jcifs.Config.setProperty( "jcifs.netbios.retryTimeout", "3000");
		
		System.setProperty("jcifs.util.loglevel", settingsSmbLogLevel);
		System.setProperty("jcifs.smb.lmCompatibility", settingsSmbLmCompatibility);
		System.setProperty("jcifs.smb.client.useExtendedSecurity", settingsSmbUseExtendedSecurity);
		System.setProperty("jcifs.smb.client.tcpNoDelay",settingsSmbTcpNodelay);
        
		if (!settingsSmbRcvBufSize.equals(""))
			System.setProperty("jcifs.smb.client.rcv_buf_size", settingsSmbRcvBufSize);//60416 120832
		if (!settingsSmbSndBufSize.equals(""))
			System.setProperty("jcifs.smb.client.snd_buf_size", settingsSmbSndBufSize);//16644 120832
        
		if (!settingsSmbListSize.equals(""))
			System.setProperty("jcifs.smb.client.listSize",settingsSmbListSize); //65536 1300
		if (!settingsSmbMaxBuffers.equals(""))
			System.setProperty("jcifs.smb.maxBuffers",settingsSmbMaxBuffers);//16 100
		
		if (mGp.debugLevel>=1) 
			util.addDebugLogMsg(1,"I","JCIFS Option : rcv_buf_size="+settingsSmbRcvBufSize+", "+
				"snd_buf_size="+settingsSmbSndBufSize+", "+
				"listSize="+settingsSmbListSize+", "+
				"maxBuffres="+settingsSmbMaxBuffers+", "+
				"tcpNodelay="+settingsSmbTcpNodelay+", "+
				"logLevel="+settingsSmbLogLevel+", "+
				"lmCompatibility="+settingsSmbLmCompatibility+", "+
				"useExtendedSecurity="+settingsSmbUseExtendedSecurity);

	};
	
	private void saveTaskData() {
		util.addDebugLogMsg(2,"I", "saveRestartData entered");
		
		if (!isTaskTermination) {
			if (!isTaskDataExisted() || mGp.msgListAdapter.resetDataChanged())  {
				ActivityDataHolder data = new ActivityDataHolder();
				data.ml=mGp.msgListAdapter.getAllItem();
				data.pl=mGp.profileAdapter.getAllItem();
				try {
				    FileOutputStream fos = openFileOutput(SMBSYNC_SERIALIZABLE_FILE_NAME, MODE_PRIVATE);
				    BufferedOutputStream bos=new BufferedOutputStream(fos,4096*256);
				    ObjectOutputStream oos = new ObjectOutputStream(bos);
				    oos.writeObject(data);
				    oos.flush();
				    oos.close();
				    util.addDebugLogMsg(1,"I", "Restart data was saved.");
				} catch (Exception e) {
					e.printStackTrace();
				    util.addDebugLogMsg(1,"E", 
				    		"saveRestartData error, "+e.toString());
				}
			}
		} 
	};
	
	private void restoreTaskData() {
		util.addDebugLogMsg(2,"I", "restoreRestartData entered");
	    File lf =
		    	new File(mGp.SMBSync_Internal_Root_Dir+"/"+SMBSYNC_SERIALIZABLE_FILE_NAME);
	    if (lf.exists()) {
			try {
	//		    FileInputStream fis = openFileInput(SMBSYNC_SERIALIZABLE_FILE_NAME);
			    FileInputStream fis = new FileInputStream(lf); 
			    BufferedInputStream bis=new BufferedInputStream(fis,4096*256);
			    ObjectInputStream ois = new ObjectInputStream(bis);
			    ActivityDataHolder data = (ActivityDataHolder) ois.readObject();
			    ois.close();
			    lf.delete();
			    
			    ArrayList<MsgListItem> o_ml=new ArrayList<MsgListItem>(); 
				for (int i=0;i<mGp.msgListAdapter.getCount();i++)
					o_ml.add(mGp.msgListAdapter.getItem(i));
			    
				mGp.msgListAdapter.clear();

				mGp.msgListAdapter.setAllItem(data.ml);

				for (int i=0;i<o_ml.size();i++) mGp.msgListAdapter.add(o_ml.get(i));

				mGp.msgListAdapter.notifyDataSetChanged();
				mGp.msgListAdapter.resetDataChanged();
				
				mGp.profileAdapter.clear();
				mGp.profileAdapter.setAllItem(data.pl);
			    util.addDebugLogMsg(1,"I", "Restart data was restored.");
			} catch (Exception e) {
				e.printStackTrace();
			    util.addDebugLogMsg(1,"E", 
			    		"restoreRestartData error, "+e.toString());
			}
	    }
	};
	
	private boolean isTaskDataExisted() {
    	File lf =new File(getFilesDir()+"/"+SMBSYNC_SERIALIZABLE_FILE_NAME);
	    return lf.exists();
	};
	
	private void deleteTaskData() {
	    File lf =
		    	new File(mGp.SMBSync_Internal_Root_Dir+"/"+SMBSYNC_SERIALIZABLE_FILE_NAME);
	    if (lf.exists()) {
		    lf.delete();
		    util.addDebugLogMsg(1,"I", "RestartData was delete.");
	    }
	};
	
    @SuppressWarnings("unused")
	private static String msgs_prof_cont_mult;
    private static String msgs_prof_cont_sngl_act;
	private static String msgs_prof_cont_mult_act;
	private static String msgs_prof_cont_mult_inact;
	private static String msgs_prof_cont_mult_sync;
	private static String msgs_prof_cont_sngl_inact;
	private static String msgs_prof_cont_sngl_delete;
	private static String msgs_prof_cont_mult_delete;
	private static String msgs_prof_cont_sngl_edit;
	private static String msgs_prof_cont_select_all;
	private static String msgs_prof_cont_unselect_all;
	
	private static String msgs_active_sync_prof_not_found	;
	private static String msgs_prof_cont_add_local	;
	private static String msgs_prof_cont_add_remote	;
	private static String msgs_prof_cont_add_sync	;
	private static String msgs_clear_log_message	;
	private static String msgs_move_to_bottom	;
	private static String msgs_move_to_top	;
	private static String msgs_sync_profile	;
//	private static String msgs_terminate_application	;

	private static String msgs_master_profile_not_found;
	private static String msgs_target_profile_not_found;
	private static String msgs_invalid_profile_combination;
	
	private static String msgs_smbsync_main_start;
	private static String msgs_smbsync_main_restart;
	@SuppressWarnings("unused")
	private static String msgs_smbsync_main_restart_msglost;
	private static String msgs_smbsync_main_end;
	
//   	private static String msgs_setting_log_opt_chg;
//   	private static String msgs_setting_log_dir_chg;
   	
    private static String msgs_sync_select_prof_no_active_profile;
    
    private static String msgs_prof_cont_copy;
    private static String msgs_prof_cont_rename;
    
    private static String msgs_sync_history_ccmeu_delete;
    private static String msgs_sync_history_ccmeu_unselectall;
    private static String msgs_sync_history_ccmeu_selectall;
    private static String msgs_sync_history_ccmeu_copy_clipboard;
    
    private static String msgs_prof_cont_sngl_wizard="";
    
//    private static String msgs_dlg_hardkey_back_button;
    
	private void loadMsgString() {
		
		msgs_prof_cont_sngl_wizard=getString(R.string.msgs_prof_cont_sngl_wizard);
		
		msgs_sync_history_ccmeu_delete=getString(R.string.msgs_sync_history_ccmeu_delete);
		msgs_sync_history_ccmeu_unselectall=getString(R.string.msgs_sync_history_ccmeu_unselectall);
		msgs_sync_history_ccmeu_selectall=getString(R.string.msgs_sync_history_ccmeu_selectall);
		msgs_sync_history_ccmeu_copy_clipboard=getString(R.string.msgs_sync_history_ccmeu_copy_clipboard);
		
		msgs_prof_cont_copy=getString(R.string.msgs_prof_cont_copy);
		msgs_prof_cont_rename=getString(R.string.msgs_prof_cont_rename);
		
		msgs_sync_select_prof_no_active_profile=getString(R.string.msgs_sync_select_prof_no_active_profile);

		msgs_prof_cont_mult=getString(R.string.msgs_prof_cont_mult);
	    msgs_prof_cont_sngl_act=getString(R.string.msgs_prof_cont_sngl_act);
		msgs_prof_cont_mult_act=getString(R.string.msgs_prof_cont_mult_act);
		msgs_prof_cont_mult_inact=getString(R.string.msgs_prof_cont_mult_inact);
		msgs_prof_cont_mult_sync=getString(R.string.msgs_prof_cont_mult_sync);
		msgs_prof_cont_sngl_inact=getString(R.string.msgs_prof_cont_sngl_inact);
		msgs_prof_cont_sngl_delete=getString(R.string.msgs_prof_cont_sngl_delete);
		msgs_prof_cont_mult_delete=getString(R.string.msgs_prof_cont_mult_delete);
		msgs_prof_cont_sngl_edit=getString(R.string.msgs_prof_cont_sngl_edit);
		msgs_prof_cont_select_all=getString(R.string.msgs_prof_cont_select_all);
		msgs_prof_cont_unselect_all=getString(R.string.msgs_prof_cont_unselect_all);
		
//		msgs_setting_log_opt_chg =getString(R.string.msgs_setting_log_opt_chg);
//		msgs_setting_log_dir_chg =getString(R.string.msgs_setting_log_dir_chg);
		
	    msgs_smbsync_main_start=	getString(R.string.msgs_smbsync_main_start);
	    msgs_smbsync_main_restart=	getString(R.string.msgs_smbsync_main_restart);
	    msgs_smbsync_main_restart_msglost=	getString(R.string.msgs_smbsync_main_restart_msglost);
	    msgs_smbsync_main_end=	getString(R.string.msgs_smbsync_main_end);
		
		
		msgs_active_sync_prof_not_found	=	getString(R.string.msgs_active_sync_prof_not_found	);
		msgs_prof_cont_add_local			=	getString(R.string.msgs_prof_cont_add_local	);
		msgs_prof_cont_add_remote			=	getString(R.string.msgs_prof_cont_add_remote	);
		msgs_prof_cont_add_sync			=	getString(R.string.msgs_prof_cont_add_sync	);
		msgs_clear_log_message			=	getString(R.string.msgs_clear_log_message);
		msgs_move_to_bottom				=	getString(R.string.msgs_move_to_bottom	);
		msgs_move_to_top				=	getString(R.string.msgs_move_to_top	);
		msgs_sync_profile				=	getString(R.string.msgs_sync_profile	);
//		msgs_terminate_application		=	getString(R.string.msgs_terminate_application	);
		msgs_master_profile_not_found	=	getString(R.string.msgs_master_profile_not_found);
		msgs_target_profile_not_found	=	getString(R.string.msgs_target_profile_not_found);
		msgs_invalid_profile_combination=	getString(R.string.msgs_invalid_profile_combination);
	
	};
	
	public class CustomTabContentView extends FrameLayout {  
        LayoutInflater inflater = (LayoutInflater) getApplicationContext()  
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);  
      
        private View childview1=null;
        private TextView tv1=null;
        public CustomTabContentView(Context context) {  
            super(context);  
        }  
        @SuppressLint("InflateParams")
		public CustomTabContentView(Context context, String title) {  
            this(context);  
            childview1 = inflater.inflate(R.layout.tab_widget1, null);  
            tv1 = (TextView) childview1.findViewById(R.id.tab_widget1_textview);  
            tv1.setText(title);  
            addView(childview1);  
       }
       public void setTabTitle(String title) {  
            tv1.setText(title);  
       }  
       public void setViewAlpha(float alpha) {  
           tv1.setAlpha(alpha);  
      }  

    };
}
class ActivityDataHolder implements Serializable  {
	private static final long serialVersionUID = 1L;
	ArrayList<MsgListItem> ml=null;
	ArrayList<ProfileListItem> pl=null;
}
