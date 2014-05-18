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

import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_BG_TERM_NOTIFY_MSG_ALWAYS;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_BG_TERM_NOTIFY_MSG_NO;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_CONFIRM_FOR_COPY;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_CONFIRM_RESP_NO;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_CONFIRM_RESP_NOALL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_CONFIRM_RESP_YES;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_CONFIRM_RESP_YESALL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_EXTRA_PARM_AUTO_START;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_EXTRA_PARM_AUTO_TERM;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_EXTRA_PARM_SYNC_PROFILE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PB_RINGTONE_NOTIFICATION_ALWAYS;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PB_RINGTONE_NOTIFICATION_ERROR;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PB_RINGTONE_NOTIFICATION_SUCCESS;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_ACTIVE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_LOCAL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_REMOTE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_SYNC;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SERIALIZABLE_FILE_NAME;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SUPPRESS_WARNING_MIXED_MP;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_ADAPTER_OFF;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ALWAYS;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ERROR;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_SUCCESS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
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
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
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
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenuItem.CustomContextMenuOnClickListener;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;

@SuppressWarnings("deprecation")
@SuppressLint({"SimpleDateFormat" })
public class SMBSyncMain extends FragmentActivity {
	
	private final static int ATERM_WAIT_TIME = 30;
	
	private boolean isTaskTermination = false; // kill is disabled(enable is kill by onDestroy)
	
	private AdapterProfileList profileAdapter=null;
	private ListView profileListView;

	private AdapterSyncHistory historyAdapter=null;
	private ListView historyListView;

	private String packageVersionName="Not found"; 

	private String currentViewType ="P";

	private TabHost tabHost;
	private Context mContext;
	
	private static GlobalParameters glblParms=null;
	private ProfileMaintenance profMaint=null;
	
	private static SMBSyncUtil util=null;
	private CustomContextMenu ccMenu = null;
	
	public boolean extraDataSpecifiedSyncProfile=false,
			extraDataSpecifiedAutoStart=false,extraDataSpecifiedAutoTerm=false,
			extraDataSpecifiedExecuteMinimum=false;
	
	private int restartStatus=0;
	private boolean enableMainUi=true;
	
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
//		setTheme(android.R.style.Theme_Dialog);
//		setTheme(android.R.style.Theme_Holo_Light);
//		setTheme(android.R.style.Theme_Light);
		super.onCreate(savedInstanceState);
//		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		mCurrentLocal=getResources().getConfiguration().locale;
		
		setContentView(R.layout.main);
		mContext=this;
		glblParms=(GlobalParameters) getApplication();
		glblParms.uiHandler=new Handler();
		glblParms.SMBSync_External_Root_Dir=LocalMountPoint.getExternalStorageDir();

		mScreenOnWakelock=((PowerManager)getSystemService(Context.POWER_SERVICE))
	    			.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK
	    				| PowerManager.ACQUIRE_CAUSES_WAKEUP
//	   	    				| PowerManager.ON_AFTER_RELEASE
	    				, "SMBSync-ScreenOn");
		
		WifiManager mWifi = 
				(WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		mWifiLock=mWifi.createWifiLock(WifiManager.WIFI_MODE_FULL, "SMBSync-wifi");

		if (tcService==null) tcService=new ThreadCtrl();

//		if (Build.VERSION.SDK_INT>=14)
//			this.getActionBar().setHomeButtonEnabled(false);
		
		if (util==null)
			util=new SMBSyncUtil(this.getApplicationContext(),"Main", glblParms);
		util.setActivityIsForeground(true);

		if (ccMenu ==null)
			ccMenu = new CustomContextMenu(getResources(),getSupportFragmentManager());
		commonDlg=new CommonDialog(mContext, getSupportFragmentManager());
		
		createTabView() ;
		loadMsgString();
		initSettingsParms();
		applySettingParms();
		
		checkExternalStorage();
		glblParms.SMBSync_Internal_Root_Dir=getFilesDir().toString();
		
		util.openLogFile();
		
		initAdapterAndView();

		
			util.addDebugLogMsg(1,"I","onCreate entered, "+"resartStatus="+restartStatus+
					", isActivityForeground="+util.isActivityForeground());
		
		getApplVersionName();

		if (profMaint==null) 
			profMaint=new ProfileMaintenance(util,this, profileAdapter, 
					profileListView, commonDlg,ccMenu, glblParms);
	};

	@Override
	protected void onStart() {
		super.onStart();
		
			util.addDebugLogMsg(1,"I","onStart entered, "+"resartStatus="+restartStatus+
					", isActivityForeground="+util.isActivityForeground());

		util.setActivityIsForeground(true);
	};

	@Override
	protected void onResume() {
		super.onResume();
		util.addDebugLogMsg(1,"I","onResume entered, "+"resartStatus="+restartStatus+
					", isActivityForeground="+util.isActivityForeground());
		util.setActivityIsForeground(true);
		if (restartStatus==1) {
			if (!glblParms.freezeMessageViewScroll) {
				glblParms.uiHandler.post(new Runnable(){
					@Override
					public void run() {
						glblParms.msgListView.setSelection(glblParms.msgListAdapter.getCount()-1);
					}
				});
			}
		} else {
			setUiEnabled();
			NotifyEvent svc_ntfy=new NotifyEvent(mContext);
			svc_ntfy.setListener(new NotifyEventListener(){
				@Override
				public void positiveResponse(Context c, Object[] o) {
					svcStartForeground();
					setCallbackListener();
					
					if (restartStatus==0) startupWarning();
					
					profMaint.replaceProfileAdapterContent(profMaint.createProfileList(false,""));
					
					if (restartStatus==0) {
						util.addLogMsg("I",msgs_smbsync_main_start+" Version "+packageVersionName );
						showNotificationMsg(msgs_smbsync_main_start+" Version "+packageVersionName );
						loadExtraDataParms();
						listSMBSyncOption();
						if (glblParms.settingAutoStart && 
							glblParms.externalStorageIsMounted && !util.isRemoteDisable()) {
							autoStartDlg();
							if (glblParms.settingBackgroundExecution) {
								setScreenSwitchToHome();
							}
						} else {
							NotifyEvent ntfy_nr=new NotifyEvent(mContext);
							ntfy_nr.setListener(new NotifyEventListener(){
								@Override
								public void positiveResponse(Context c,Object[] o) {
									if (LocalFileLastModified.isLastModifiedWasUsed(profileAdapter))
										checkLastModifiedListValidity();
								}
								@Override
								public void negativeResponse(Context c,Object[] o) {}
							});
							
							if (glblParms.settingAutoStart && 
								(!glblParms.externalStorageIsMounted || util.isRemoteDisable())) {
								String m_txt="";
								if (!glblParms.externalStorageIsMounted) m_txt=c.getString(R.string.msgs_astart_abort_external_storage_not_mounted);
								if (util.isRemoteDisable()) m_txt=c.getString(R.string.msgs_astart_abort_wifi_option_not_satisfied);
								commonDlg.showCommonDialog(false, "W", "", m_txt, ntfy_nr);
								util.addLogMsg("W",m_txt);
							} else {
								ntfy_nr.notifyToListener(true, null);
							}
						}
					} else if (restartStatus==2) {
						restoreTaskData();
						if (currentViewType.equals("M")) tabHost.setCurrentTab(1);
						util.addLogMsg("I",msgs_smbsync_main_restart+" Version "+packageVersionName);
						showNotificationMsg(msgs_smbsync_main_restart+" Version "+packageVersionName);
					}

					restartStatus=1;
					deleteTaskData();
					
					setMsglistViewListener();
					setProfilelistItemClickListener();
					setProfilelistLongClickListener();
					setMsglistLongClickListener();
					
					setHistoryViewItemClickListener();
					setHistoryViewLongClickListener();
					
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
		
			util.addDebugLogMsg(1,"I","onStop entered, " +
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
		if (glblParms.settingExitClean) {
			System.gc();
			Handler hndl=new Handler();
			hndl.postDelayed(new Runnable() {
				@Override
				public void run() {
					android.os.Process.killProcess(android.os.Process.myPid());
				}
			}, 500);	
		}
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
	};

	private void changeLanguageCode(final Configuration newConfig) {
		util.addLogMsg("I",getString(R.string.msgs_smbsync_main_language_changed));
	    loadMsgString();
	    refreshOptionMenu();
		mTabChildviewProf.setTabTitle(getString(R.string.msgs_tab_name_prof));
		mTabChildviewMsg.setTabTitle(getString(R.string.msgs_tab_name_msg));
		mTabChildviewHist.setTabTitle(getString(R.string.msgs_tab_name_history));
		profMaint.loadMsgString();
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

	private void initAdapterAndView() {
		glblParms.msgListView = (ListView) findViewById(R.id.message_view_list);
		glblParms.msgListView.setFastScrollEnabled(true);
		profileListView =(ListView) findViewById(R.id.profile_view_list);
		if (glblParms.msgListAdapter==null) {
			ArrayList<MsgListItem> tml =new ArrayList<MsgListItem>();
			glblParms.msgListAdapter = new AdapterMessageList(this,R.layout.msg_list_item_view,tml);
		}
		glblParms.msgListView.setAdapter(glblParms.msgListAdapter);
		glblParms.msgListView.setDrawingCacheEnabled(true);
		glblParms.msgListView.setClickable(true);
		glblParms.msgListView.setFocusable(true);
		glblParms.msgListView.setFocusableInTouchMode(true);
		glblParms.msgListView.setSelected(true);
		setFastScrollListener(glblParms.msgListView);
		
		if (profileAdapter==null) {
			ArrayList<ProfileListItem> tml1 =new ArrayList<ProfileListItem>();
			profileAdapter=
					new AdapterProfileList(this, R.layout.profile_list_item_view, tml1,
							glblParms.SMBSync_External_Root_Dir);
			currentViewType="P";
		}
		profileListView.setAdapter(profileAdapter);
		
		historyListView=(ListView)findViewById(R.id.history_view_list);
		historyAdapter=new AdapterSyncHistory(mContext, R.layout.sync_history_list_item_view, 
				util.loadHistoryList());
		historyListView.setAdapter(historyAdapter);
		historyAdapter.notifyDataSetChanged();
		historyListView.setClickable(true);
		historyListView.setFocusable(true);
		historyListView.setFastScrollEnabled(true);
		historyListView.setFocusableInTouchMode(true);
		setFastScrollListener(historyListView);

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
		
		glblParms.mainViewProgressProf=(TextView)findViewById(R.id.profile_progress_spin_syncprof);
		glblParms.mainViewProgressFilepath=(TextView)findViewById(R.id.profile_progress_spin_filepath);
		glblParms.mainViewProgressMessage=(TextView)findViewById(R.id.profile_progress_spin_status);

	};
	
	class OnTabChange implements OnTabChangeListener {
		@Override
		public void onTabChanged(String tabId){
			
				util.addDebugLogMsg(2,"I","onTabchanged entered. tab="+tabId+
						",v="+currentViewType);
			
			if (tabId.equals("prof")) {
				currentViewType="P";
//				profileListView.setSelection(posProfileListView);
			} else if (tabId.equals("msg")) {
				currentViewType="M";
			} else if (tabId.equals("hst")) {
				currentViewType="H";
			}
			
			
				util.addDebugLogMsg(2,"I","onTabchanged exited. tab="+tabId+
						",v="+currentViewType);
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
		if (isUiEnabled()) {
			menu.findItem(R.id.menu_top_sync).setEnabled(true);
			menu.findItem(R.id.menu_top_browse_log).setEnabled(true);
			menu.findItem(R.id.menu_top_export).setEnabled(true);
			menu.findItem(R.id.menu_top_import).setEnabled(true);
			menu.findItem(R.id.menu_top_last_mod_list).setEnabled(true);
			menu.findItem(R.id.menu_top_about).setEnabled(true);
			menu.findItem(R.id.menu_top_settings).setEnabled(true);
			menu.findItem(R.id.menu_top_log_management).setEnabled(true);
			if (!glblParms.externalStorageIsMounted) {
				menu.findItem(R.id.menu_top_sync).setEnabled(false);
				menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
				menu.findItem(R.id.menu_top_export).setEnabled(false);
				menu.findItem(R.id.menu_top_import).setEnabled(false);
				menu.findItem(R.id.menu_top_log_management).setEnabled(false);
			}
			if (glblParms.logWriter==null)
				menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
			if (!LocalFileLastModified.isLastModifiedWasUsed(profileAdapter))
				menu.findItem(R.id.menu_top_last_mod_list).setEnabled(false);
		} else {
			menu.findItem(R.id.menu_top_sync).setEnabled(false);
			
			menu.findItem(R.id.menu_top_browse_log).setEnabled(true);
			if (!glblParms.externalStorageIsMounted) {
				menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
			}
			if (glblParms.logWriter==null) {
				menu.findItem(R.id.menu_top_browse_log).setEnabled(false);
			}
//			Log.v("","ena="+menu.findItem(R.id.menu_top_browse_log).isEnabled());
			
			menu.findItem(R.id.menu_top_export).setEnabled(false);
			menu.findItem(R.id.menu_top_import).setEnabled(false);
			menu.findItem(R.id.menu_top_last_mod_list).setEnabled(false);
			menu.findItem(R.id.menu_top_about).setEnabled(false);
			menu.findItem(R.id.menu_top_settings).setEnabled(false);
			menu.findItem(R.id.menu_top_log_management).setEnabled(false);
		}
        return super.onPrepareOptionsMenu(menu);
	};
	
	private long mToastNextIssuedTimeSyncOption=0l;
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.menu_top_sync:
				if (!util.isRemoteDisable()) {
					syncActiveProfile();
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
					new LocalFileLastModified(mContext,profileAdapter,util,commonDlg);
				lflm.maintLastModListDlg();
				return true;
			case R.id.menu_top_export:
				profMaint.exportProfileDlg(glblParms.SMBSync_External_Root_Dir,"/SMBSync","profile.txt");
				return true;
			case R.id.menu_top_import:
				importProfileAndParms();
				return true;
			case R.id.menu_top_log_management:
				invokeLogManagement();
				return true;
			case R.id.menu_top_about:
				aboutSMBSync();
				return true;			
			case R.id.menu_top_settings:
				invokeSettingsActivity();
				return true;			
//			case R.id.menu_top_quit:
//				terminateApplication();
//				return true;
		}
		if (isUiEnabled()) {
		}
		return false;
	};

	private void invokeLogManagement() {
		util.flushLogFile();
		LogFileManagementFragment lfm=
				LogFileManagementFragment.newInstance(getString(R.string.msgs_log_management_title));
		lfm.showDialog(getSupportFragmentManager(), lfm, glblParms);
	};
	
	private void checkLastModifiedListValidity() {
		final ArrayList<LocalFileLastModifiedMaintListItem> maint_list=
				new ArrayList<LocalFileLastModifiedMaintListItem>();
		final NotifyEvent th_ntfy=new NotifyEvent(mContext);
		th_ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				checkMixedMountPoint(maint_list);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
		});
		final Handler hndl=new Handler();
		Thread th=new Thread(new Runnable(){
			@Override
			public void run() {
				LocalFileLastModified.createLastModifiedMaintList(
						mContext, profileAdapter,maint_list);
				hndl.post(new Runnable() {
					@Override
					public void run() {
						th_ntfy.notifyToListener(true, null);
					}
				});
			}
		});
		th.start();
	};

	private ProfileListItem getProfileListItem(String type, String name) {
		ProfileListItem pli=null;
		for (int i=0;i<profileAdapter.getCount();i++) {
			if (profileAdapter.getItem(i).getType().equals(type) &&
					profileAdapter.getItem(i).getName().equals(name)) {
				pli=profileAdapter.getItem(i);
				break;
			}
		}
		return pli;
	}
	
	private void checkMixedMountPoint(final ArrayList<LocalFileLastModifiedMaintListItem> maint_list) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		if (!prefs.getBoolean(SMBSYNC_SUPPRESS_WARNING_MIXED_MP, false)) {
			boolean mixed_mp=false;
			String mp_name=null;
			for (int i=0;i<profileAdapter.getCount();i++) {
				ProfileListItem s_pli=profileAdapter.getItem(i);
				if (s_pli.getType().equals(SMBSYNC_PROF_TYPE_SYNC) && s_pli.getActive().equals(SMBSYNC_PROF_ACTIVE)) {
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
				final CheckBox cbSuppr= (CheckBox) dialog.findViewById(R.id.mixed_mount_point_dialog_suppress);
				
				CommonDialog.setDlgBoxSizeCompact(dialog);
				cbSuppr.setChecked(false);
				// OKボタンの指定
				btnOk.setOnClickListener(new View.OnClickListener() {
					public void onClick(View v) {
						dialog.dismiss();
						if (cbSuppr.isChecked()) {
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
							new LocalFileLastModified(mContext,profileAdapter,util,commonDlg);
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
			}

			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		profMaint.importProfileDlg(glblParms.SMBSync_External_Root_Dir,"/SMBSync","profile.txt", ntfy);
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
						confirmTerminateApplication();
				} else {
					confirmTerminateApplication();
				}
				return true;
				// break;
			default:
				return super.onKeyDown(keyCode, event);
				// break;
		}
	};
	
	private void startupWarning() {
		if (!glblParms.externalStorageIsMounted) {
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
			if (Build.VERSION.SDK_INT==7) glblParms.SMBSync_External_Root_Dir="/sdcard";
			else glblParms.SMBSync_External_Root_Dir="/mnt/sdcard";
    		glblParms.externalStorageIsMounted=false;
    	} else  {  
        	// get file path  
    		glblParms.SMBSync_External_Root_Dir = LocalMountPoint.getExternalStorageDir();
    		glblParms.externalStorageIsMounted=true;
    	}
	};

	private void terminateApplication() {
//		stopMirrorService();
		util.addLogMsg("I",msgs_smbsync_main_end);
//		saveLogMsgToFile();
//		closeLogFile();
		isTaskTermination = true; // exit cleanly
//		moveTaskToBack(true);
		finish();
	};
	
	private void confirmTerminateApplication() {
		
//		NotifyEvent ntfy=new NotifyEvent(mContext);
//		ntfy.setListener(new NotifyEventListener() {
//			@Override
//			public void positiveResponse(Context c,Object[] o) {
//				terminateApplication();
//			}
//			@Override
//			public void negativeResponse(Context c,Object[] o) {}
//		});
//		commonDlg.showCommonDialog(true,"W",msgs_terminate_application,"",ntfy);
		terminateApplication();

		return;
	};
	
	private void initSettingsParms() {
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		
		if (prefs.getString(getString(R.string.settings_log_dir), "-1").equals("-1")) {
			
			glblParms.sampleProfileCreateRequired=true;
			
			prefs.edit().putString(getString(R.string.settings_log_dir),
					glblParms.SMBSync_External_Root_Dir+"/SMBSync/").commit();
			prefs.edit().putString(getString(R.string.settings_network_wifi_option),
					SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP).commit();
			prefs.edit().putString(getString(R.string.settings_file_diff_time_seconds), 
					"3").commit();
			prefs.edit().putString(getString(R.string.settings_media_store_last_mod_time), 
					"0").commit();

			prefs.edit().putBoolean(getString(R.string.settings_media_scanner_non_media_files_scan), 
					true).commit();
			prefs.edit().putBoolean(getString(R.string.settings_media_scanner_scan_extstg), 
					true).commit();

			prefs.edit().putBoolean(getString(R.string.settings_exit_clean), 
					true).commit();
			String c_ip=SMBSyncUtil.getLocalIpAddress();
			if (c_ip.indexOf(":")>0) {//IP V6
				prefs.edit().putString(getString(R.string.settings_default_addr),
						c_ip).commit();
			} else {//IP V4
//				Log.v("","c_ip="+c_ip);
				prefs.edit().putString(getString(R.string.settings_default_addr), 
						c_ip.substring(0,c_ip.lastIndexOf("."))+".xxx").commit();				
			}

			prefs.edit().putString(getString(R.string.settings_smb_lm_compatibility),"0").commit();
			prefs.edit().putBoolean(getString(R.string.settings_smb_use_extended_security),false).commit();
			prefs.edit().putString(getString(R.string.settings_smb_log_level),"0").commit();
			prefs.edit().putString(getString(R.string.settings_smb_rcv_buf_size),"66576").commit();
			prefs.edit().putString(getString(R.string.settings_smb_snd_buf_size),"66576").commit();
			prefs.edit().putString(getString(R.string.settings_smb_listSize),"65535").commit();
			prefs.edit().putString(getString(R.string.settings_smb_maxBuffers),"100").commit();
			prefs.edit().putString(getString(R.string.settings_smb_tcp_nodelay),"true").commit();
			prefs.edit().putString(getString(R.string.settings_io_buffers),"8").commit();
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

		glblParms.debugLevel=
			Integer.parseInt(prefs.getString(getString(R.string.settings_log_level), "0"));

		util.addDebugLogMsg(1, "I", "applySettingParms entered");

		glblParms.settiingLogGeneration=Integer.valueOf(
				prefs.getString(getString(R.string.settings_log_generation), "10"));
		
		String t_dir =
				prefs.getString(getString(R.string.settings_log_dir),
						glblParms.SMBSync_External_Root_Dir+"/SMBSync/");
		if (t_dir.equals("")) {
			t_dir=glblParms.SMBSync_External_Root_Dir+"/SMBSync/";
			prefs.edit().putString(getString(R.string.settings_log_dir),
					glblParms.SMBSync_External_Root_Dir+"/SMBSync/").commit();
		} else {
			if (!t_dir.endsWith("/")) {
				t_dir+="/";
				prefs.edit().putString(getString(R.string.settings_log_dir),t_dir).commit();
			} 
		}
		
		if (!glblParms.settingLogMsgDir.equals(t_dir)) {// option was changed
			glblParms.settingLogMsgDir=t_dir;
			if (!glblParms.settingLogOption.equals("0")) {
				util.closeLogFile();
				util.openLogFile();
			}
		}
		
		glblParms.settingUsername=
				prefs.getString(getString(R.string.settings_default_user), "");
		glblParms.settingPassword=
				prefs.getString(getString(R.string.settings_default_pass), "");
		glblParms.settingAddr=
				prefs.getString(getString(R.string.settings_default_addr), "");
		glblParms.settingAutoStart=
				prefs.getBoolean(getString(R.string.settings_auto_start), false);
		glblParms.settingDebugMsgDisplay=
				prefs.getBoolean(getString(R.string.settings_debug_msg_diplay), false);
		
		String p_opt=glblParms.settingLogOption;
		glblParms.settingLogOption=
				prefs.getString(getString(R.string.settings_log_option), "0");
		SimpleDateFormat df=null;
		if (glblParms.settingLogFileCreatedByStartupTime) df = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
		else df = new SimpleDateFormat("yyyy-MM-dd");
		glblParms.settingLogMsgFilename="SMBSync_log_"+df.format(System.currentTimeMillis())+".txt";

		if (!glblParms.settingLogOption.equals(p_opt)) {
			if (glblParms.settingLogOption.equals("0")) util.closeLogFile();
			else util.openLogFile();
		}
//		Log.v("","p="+p_opt+", n="+glblParms.settingLogOption);
//		if (!glblParms.settingLogOption.equals(t_lo) ) {// option was changed
//			commonDlg.showCommonDialog(false,"W",msgs_setting_log_opt_chg,"",null);
//		}
		if (glblParms.settingAutoStart) {
			glblParms.settingLogOption="1"; //Force log enabled
		}

		glblParms.settingAutoTerm=
				prefs.getBoolean(getString(R.string.settings_auto_term), false);
		glblParms.settingWifiOption=
				prefs.getString(getString(R.string.settings_network_wifi_option), 
						SMBSYNC_SYNC_WIFI_OPTION_CONNECTED_ANY_AP);
		glblParms.settingErrorOption=
				prefs.getBoolean(getString(R.string.settings_error_option), false);
		glblParms.settingBackgroundExecution=
				prefs.getBoolean(getString(R.string.settings_backgroound_execution), false);
		glblParms.settingBgTermNotifyMsg=
				prefs.getString(getString(R.string.settings_background_termination_notification), "0");

		glblParms.settingVibrateWhenSyncEnded=
				prefs.getString(getString(R.string.settings_vibrate_when_sync_ended), "0");
		glblParms.settingRingtoneWhenSyncEnded=
				prefs.getString(getString(R.string.settings_playback_ringtone_when_sync_ended), "0");

		glblParms.settingScreenOnEnabled=
				prefs.getBoolean(getString(R.string.settings_ui_keep_screen_on), false);
		
		glblParms.settingWifiLockRequired=
				prefs.getBoolean(getString(R.string.settings_wifi_lock), false);
		
		glblParms.settingAltUiEnabled=
				prefs.getBoolean(getString(R.string.settings_ui_alternate_ui), false);

		glblParms.settingMediaFiles=
				prefs.getBoolean(getString(R.string.settings_media_scanner_non_media_files_scan), true);
		glblParms.settingScanExternalStorage=
				prefs.getBoolean(getString(R.string.settings_media_scanner_scan_extstg), true);
		
		glblParms.settingExitClean=
				prefs.getBoolean(getString(R.string.settings_exit_clean), true);
		glblParms.settingExportedProfileEncryptRequired=
				prefs.getBoolean(getString(R.string.settings_exported_profile_encryption), true);
		
		if (!glblParms.settingAutoStart) glblParms.settingAutoTerm=false;
		
//		refreshOptionMenu();
//		
	};

	private void loadExtraDataParms() {
		Intent in=getIntent();
		Bundle bundle=in.getExtras();
		if (bundle!=null) {
			util.addLogMsg("I",mContext.getString(R.string.msgs_extra_data_was_found));
			if (bundle.containsKey(SMBSYNC_EXTRA_PARM_AUTO_START)) {
				if (bundle.get(SMBSYNC_EXTRA_PARM_AUTO_START).getClass().getSimpleName().equals("Boolean")) {
					extraDataSpecifiedAutoStart=true;
					glblParms.settingAutoStart=bundle.getBoolean(SMBSYNC_EXTRA_PARM_AUTO_START);
					util.addLogMsg("I"," AutoStart="+glblParms.settingAutoStart);
				} else {
					util.addLogMsg("W"," AutoStart must be boolean, ignored extra data");
				}
			}
			if (bundle.containsKey(SMBSYNC_EXTRA_PARM_AUTO_TERM)) {
				if (bundle.get(SMBSYNC_EXTRA_PARM_AUTO_TERM).getClass().getSimpleName().equals("Boolean")) {
					if (extraDataSpecifiedAutoStart && glblParms.settingAutoStart) {
						extraDataSpecifiedAutoTerm=true;
						glblParms.settingAutoTerm=bundle.getBoolean(SMBSYNC_EXTRA_PARM_AUTO_TERM);
						util.addLogMsg("I"," AutoTerm="+glblParms.settingAutoTerm);
					} else {
						util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_ignored)+"AutoTerm");
					}
				} else {
					util.addLogMsg("W"," AutoTerm must be boolean, ignored extra data");
				}
			}
			if (bundle.containsKey(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION)) {
				if (bundle.get(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION).getClass().getSimpleName().equals("Boolean")) {
					extraDataSpecifiedExecuteMinimum=
							bundle.getBoolean(SMBSYNC_EXTRA_PARM_BACKGROUND_EXECUTION);
					glblParms.settingBackgroundExecution=extraDataSpecifiedExecuteMinimum;
					util.addLogMsg("I"," Background="+glblParms.settingBackgroundExecution);
				} else {
					util.addLogMsg("W"," Background must be boolean, ignored extra data");
				}
			}
			if (bundle.containsKey(SMBSYNC_EXTRA_PARM_SYNC_PROFILE)) {
				if (extraDataSpecifiedAutoStart && glblParms.settingAutoStart) {
					if (bundle.get(SMBSYNC_EXTRA_PARM_SYNC_PROFILE).getClass().getSimpleName().equals("String[]")) {
						String[] sync_profile=bundle.getStringArray(SMBSYNC_EXTRA_PARM_SYNC_PROFILE);
						if (sync_profile.length!=0) {
							util.addLogMsg("I",mContext.getString(R.string.msgs_extra_data_sync_profile));
							for (int sidx=0;sidx<sync_profile.length;sidx++) {
								boolean selected=false;
								for (int pidx=0;pidx<profileAdapter.getCount();pidx++) {
									if (profileAdapter.getItem(pidx).getType().equals(SMBSYNC_PROF_TYPE_SYNC) && 
											profileAdapter.getItem(pidx).getName().equals(sync_profile[sidx])) {
										selected=true;
										if (profileAdapter.getItem(pidx).getActive().equals(SMBSYNC_PROF_ACTIVE)) {
											profileAdapter.getItem(pidx).setChecked(true);
											util.addLogMsg("I",mContext.getString(R.string.msgs_extra_data_profile_selected)+sync_profile[sidx]);
											extraDataSpecifiedSyncProfile=true;
										} else {
											util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_profile_disabled)+sync_profile[sidx]);								
										}
									}
								}
								if (!selected) 
									util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_profile_not_exists)+sync_profile[sidx]);								
							}
						} else {
							util.addLogMsg("W"," No profile name was specified, ignored extra data");
						}
					} else {
						util.addLogMsg("W"," SyncProfile must be string array(String[]), ignored extra data");
					}
				} else {
					util.addLogMsg("W",mContext.getString(R.string.msgs_extra_data_ignored)+"SyncProfile");
				}
			}
			if (extraDataSpecifiedAutoStart && glblParms.settingAutoStart && !extraDataSpecifiedSyncProfile)
				util.addLogMsg("I",mContext.getString(R.string.msgs_extra_data_no_profile_selected));
		}
	};
	
	private void listSMBSyncOption() {
		util.addDebugLogMsg(1,"I","SMBSync option :"+
				"settings_log_level="+glblParms.debugLevel+
				",settings_default_user="+glblParms.settingUsername+
				",settings_default_addr="+glblParms.settingAddr+
				",settings_auto_start="+glblParms.settingAutoStart+
				",settings_auto_term="+glblParms.settingAutoTerm+
				",settings_error_option="+glblParms.settingErrorOption+
				",settings_background_execution="+glblParms.settingBackgroundExecution+
				",settings_background_termination_notification="+glblParms.settingBgTermNotifyMsg+
				",settings_log_option="+glblParms.settingLogOption+
				",settings_log_dir="+glblParms.settingLogMsgDir);
		if (glblParms.debugLevel==9)
			util.addDebugLogMsg(1,"I","settings_default_pass="+
					glblParms.settingPassword);
		if (glblParms.settingAutoStart) util.addLogMsg("I",
				getString(R.string.msgs_smbsync_main_settings_force_log_enabled));

	};

	@SuppressLint("SdCardPath")
	private void invokeLogFileBrowser() {
		util.addDebugLogMsg(1,"I","Invoke log file browser.");
		util.flushLogFile();
//		enableBrowseLogFileMenu=false;
		if (glblParms.logWriter!=null) {
			String t_fd="",fp="";
			t_fd=glblParms.settingLogMsgDir;
			if (t_fd.lastIndexOf("/")==(t_fd.length()-1)) {//last is "/"
				fp=t_fd+glblParms.settingLogMsgFilename;
			} else fp=t_fd+"/"+glblParms.settingLogMsgFilename;

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
		if (!glblParms.settingLogOption.equals("0")) util.openLogFile();
		if (requestCode==0) {
			util.addDebugLogMsg(1,"I","Return from Settings.");
			util.setActivityIsForeground(true);
			applySettingParms();
			listSMBSyncOption();
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
		
		historyListView
			.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				SyncHistoryListItem item = historyAdapter.getItem(position);
				if (glblParms.settingAltUiEnabled) {
					if (isHistoryItemSelected()) item.isChecked=!item.isChecked;
					else {
						if (item.isLogFileAvailable) {
							Intent intent = 
									new Intent(android.content.Intent.ACTION_VIEW);
							intent.setDataAndType(
									Uri.parse("file://"+item.sync_log_file_path),
									"text/plain");
							startActivityForResult(intent,1);
						}
					}
				} else item.isChecked=!item.isChecked;
				historyAdapter.notifyDataSetChanged();
			}
		});
	};

	private boolean isHistoryItemSelected() {
		boolean result=false;
		
		for (int i=0;i<historyAdapter.getCount();i++) 
			if (historyAdapter.getItem(i).isChecked) {
				result=true;
				break;
			}
		return result;
	}
	
	private void setHistoryViewLongClickListener() {
		historyListView
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
		SyncHistoryListItem item;
		int j=0;
		for (int i=0;i<historyAdapter.getCount();i++) {
			if (historyAdapter.getItem(i).isChecked) j++;
		}
		if (j<=1) {
			for (int i=0;i<historyAdapter.getCount();i++) {
				item = historyAdapter.getItem(i);
				if (idx==i) {
					historyAdapter.getItem(i).isChecked=true;
					j=i;//set new index no
				} else {
					if (item.isChecked) {
						historyAdapter.getItem(i).isChecked=false;
					}
				}
			}
			historyAdapter.notifyDataSetChanged();
			createHistoryContextMenu_Single(j);
		} else {
			historyAdapter.notifyDataSetChanged();
			createHistoryContextMenu_Multiple(idx);
		}
	};
	
	private void createHistoryContextMenu_Multiple(int idx) { 

		ccMenu.addMenuItem(msgs_move_to_top,R.drawable.menu_top)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				historyListView.setSelection(0);
			}
		});
		
		ccMenu.addMenuItem(msgs_move_to_bottom,R.drawable.menu_bottom)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					historyListView.setSelection(historyAdapter.getCount()-1);
				}
		});
		
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_delete,R.drawable.menu_delete)
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
				for (int i=0;i<historyAdapter.getCount();i++) historyAdapter.getItem(i).isChecked=false;
				historyAdapter.notifyDataSetChanged();
			}
		});
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_selectall)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				for (int i=0;i<historyAdapter.getCount();i++) historyAdapter.getItem(i).isChecked=true;
				historyAdapter.notifyDataSetChanged();
			}
		});

		ccMenu.addMenuItem(msgs_sync_history_ccmeu_copy_clipboard)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				 ClipboardManager cm = 
					      (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				 StringBuilder out= new StringBuilder(256);
				 for (int i=0;i<historyAdapter.getCount();i++){
					 if (historyAdapter.getItem(i).isChecked) {
						 SyncHistoryListItem hli=historyAdapter.getItem(i);
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

	private void confirmDeleteHistory() {
		String conf_list="";
		boolean del_all_history=false;
		int del_cnt=0;
		for (int i=0;i<historyAdapter.getCount();i++) {
			if (historyAdapter.getItem(i).isChecked) {
				del_cnt++;
				conf_list+="\n"+historyAdapter.getItem(i).sync_date+" "+
						historyAdapter.getItem(i).sync_time+" "+
						historyAdapter.getItem(i).sync_prof+" ";
			}
		}
		if (del_cnt==historyAdapter.getCount()) del_all_history=true;
		NotifyEvent ntfy=new NotifyEvent(this);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				for (int i=historyAdapter.getCount()-1;i>=0;i--) {
					if (historyAdapter.getItem(i).isChecked) 
						historyAdapter.remove(historyAdapter.getItem(i));
				}
				util.saveHistoryList(historyAdapter.getSyncHistoryList());
				historyAdapter.setSyncHistoryList(util.loadHistoryList());
				historyAdapter.notifyDataSetChanged();
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
				historyListView.setSelection(0);
			}
		});
		
		ccMenu.addMenuItem(msgs_move_to_bottom,R.drawable.menu_bottom)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					historyListView.setSelection(historyAdapter.getCount()-1);
				}
		});

		final SyncHistoryListItem item = historyAdapter.getItem(cin);
		if (item.isLogFileAvailable) {
			ccMenu.addMenuItem(msgs_sync_history_ccmeu_browse,R.drawable.ic_64_browse_text)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					Intent intent = 
							new Intent(android.content.Intent.ACTION_VIEW);
					intent.setDataAndType(
							Uri.parse("file://"+item.sync_log_file_path),
							"text/plain");
					startActivityForResult(intent,1);
				}
			});
		}
		
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_delete,R.drawable.menu_delete)
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
				for (int i=0;i<historyAdapter.getCount();i++) historyAdapter.getItem(i).isChecked=false;
				historyAdapter.notifyDataSetChanged();
			}
		});
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_selectall)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				for (int i=0;i<historyAdapter.getCount();i++) historyAdapter.getItem(i).isChecked=true;
				historyAdapter.notifyDataSetChanged();
			}
		});
		ccMenu.addMenuItem(msgs_sync_history_ccmeu_copy_clipboard)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				 ClipboardManager cm = 
					      (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
				 SyncHistoryListItem hli=historyAdapter.getItem(cin);
				 
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

	private void setProfilelistItemClickListener() {
		
		profileListView
			.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				ProfileListItem item = profileAdapter.getItem(position);
//				util.sendDebugLogMsg(1,"I","Clicked :" + item.getName());
//				editProfile(item.getName(),item.getType(),item.getActive(),position);
				if (glblParms.settingAltUiEnabled) {
					if (!isProfileItemSelected()) editProfile(item.getName(),item.getType(),item.getActive(),position);
					else item.setChecked(!item.isChecked());
				} else item.setChecked(!item.isChecked());
				profileAdapter.notifyDataSetChanged();
			}
		});
	};
	
	private boolean isProfileItemSelected() {
		boolean result=false;
		
		for (int i=0;i<profileAdapter.getCount();i++) 
			if (profileAdapter.getItem(i).isChecked()) {
				result=true;
				break;
			}
		
		return result;
	}
	
	private void setProfilelistLongClickListener() {
		profileListView
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
				glblParms.freezeMessageViewScroll=isChecked;
			}
		});
	}
	
	private void setMsglistLongClickListener() {
		glblParms.msgListView
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
		ProfileListItem item;
//		int pos=profileListView.getFirstVisiblePosition();		
		int j=0;
		boolean sync=false;
		for (int i=0;i<profileAdapter.getCount();i++) {
			if (profileAdapter.getItem(i).isChecked()) j++;
		}
		if (j<=1) {
			for (int i=0;i<profileAdapter.getCount();i++) {
				item = profileAdapter.getItem(i);
				if (idx==i) {
					profMaint.setProfileChecked(true, profileAdapter, item, i);
					j=i;//set new index no
					if (profileAdapter.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_SYNC))
						sync=true;
				} else {
					if (item.isChecked()) {
						profMaint.setProfileChecked(false, profileAdapter, item, i);
					}
				}
			}
			profileAdapter.notifyDataSetChanged();
//			profileListView.setAdapter(profileAdapter);
//			profileListView.setSelection(pos);
			
			createProfileContextMenu_Single(j, sync);
		}
		else {
			sync=false;
			for (int i=0;i<profileAdapter.getCount();i++) {
				if (profileAdapter.getItem(i).isChecked()) 
					if (profileAdapter.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_SYNC))
						sync=true;
			}
			createProfileContextMenu_Multiple(idx,sync);
		}
	};
	
	private void createProfileContextMenu_Multiple(int idx,boolean sync) { 

		if (glblParms.externalStorageIsMounted && !util.isRemoteDisable()&&sync) {
			ccMenu.addMenuItem(msgs_prof_cont_mult_sync,R.drawable.ic_32_sync)
				.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						syncSelectedProfile();
						resetAllCheckedItem();
					}
				});
		}

		ccMenu.addMenuItem(msgs_prof_cont_mult_act,R.drawable.menu_active)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profMaint.setProfileToActive();
					resetAllCheckedItem();
				}
		});

		ccMenu.addMenuItem(msgs_prof_cont_mult_inact,R.drawable.menu_inactive)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profMaint.setProfileToInactive();
					resetAllCheckedItem();
				}
		});

		ccMenu.addMenuItem(msgs_prof_cont_mult_delete,R.drawable.menu_delete)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profMaint.deleteProfile();
					resetAllCheckedItem();
				}
		});

		ccMenu.addMenuItem(msgs_prof_cont_select_all)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				for (int i=0;i<profileAdapter.getCount();i++) {
					ProfileListItem item = profileAdapter.getItem(i);
					profMaint.setProfileChecked(true, profileAdapter, item, i);
				}
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

		final ProfileListItem item = profileAdapter.getItem(cin);
		final String i_type = item.getType();
		final String i_act = item.getActive();
		final String i_name = item.getName();
		
		if (!i_type.equals("")) {
			if (i_act.equals(SMBSYNC_PROF_ACTIVE)) {
				if (glblParms.externalStorageIsMounted && !util.isRemoteDisable() ) {
					if (sync) {
						ccMenu.addMenuItem(
							String.format(msgs_sync_profile,i_name),R.drawable.ic_32_sync)
							.setOnClickListener(new CustomContextMenuOnClickListener() {
								@Override
								public void onClick(CharSequence menuTitle) {
									syncSelectedProfile();
									resetAllCheckedItem();
								}
						});
					}
				}
				ccMenu.addMenuItem(String.format(msgs_prof_cont_sngl_inact,i_name),R.drawable.menu_inactive)
					.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						profMaint.setProfileToInactive();
						resetAllCheckedItem();
					}
				});

			} else {
				ccMenu.addMenuItem(String.format(msgs_prof_cont_sngl_act,i_name),R.drawable.menu_active)
					.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						profMaint.setProfileToActive();
						resetAllCheckedItem();
					}
				});

			}
			
			ccMenu.addMenuItem(String.format(msgs_prof_cont_sngl_edit,i_name),R.drawable.menu_edit)
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
				  @Override
				  public void onClick(CharSequence menuTitle) {
					  editProfile(i_name, i_type,i_act, cin);
					  resetAllCheckedItem();
				  }
			});
				
		    ccMenu.addMenuItem(String.format(msgs_prof_cont_sngl_delete,i_name),R.drawable.menu_delete)
				.setOnClickListener(new CustomContextMenuOnClickListener() {
					@Override
					public void onClick(CharSequence menuTitle) {
						profMaint.deleteProfile();
						resetAllCheckedItem();
					}
			});


		};
		ccMenu.addMenuItem(msgs_prof_cont_add_local,R.drawable.menu_add)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profMaint.addLocalProfile(true,"",SMBSYNC_PROF_ACTIVE,
							glblParms.SMBSync_External_Root_Dir,"","");
					resetAllCheckedItem();
				}
		});

		ccMenu.addMenuItem(msgs_prof_cont_add_remote,R.drawable.menu_add)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profMaint.addRemoteProfile(true, "",SMBSYNC_PROF_ACTIVE,
							glblParms.settingAddr,glblParms.settingUsername,
							glblParms.settingPassword,"","","","");
					resetAllCheckedItem();
				}
		});

		boolean isRemoteExists=false, isLocalExists=false;
		for (int i=0;i<profileAdapter.getCount();i++) {
			if (profileAdapter.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
				isRemoteExists=true;
			} 
			if (profileAdapter.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
				isLocalExists=true;
			} 
			if (isRemoteExists && isLocalExists) break;
		}
		if (isRemoteExists && isLocalExists) {
			ccMenu.addMenuItem(msgs_prof_cont_add_sync,R.drawable.menu_add)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					profMaint.addSyncProfile(true, "",SMBSYNC_PROF_ACTIVE,"","","",null, null,"");
					resetAllCheckedItem();
				}
		});
		}

		ccMenu.addMenuItem(msgs_prof_cont_copy,R.drawable.menu_copy)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				profMaint.copyProfile(item);
				resetAllCheckedItem();
			}
		});

		ccMenu.addMenuItem(msgs_prof_cont_rename,R.drawable.menu_rename)
		.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				profMaint.renameProfile(item);
				resetAllCheckedItem();
			}
		});

		ccMenu.addMenuItem(String.format(msgs_prof_cont_sngl_wizard,i_name),R.drawable.ic_64_wizard)
	  	.setOnClickListener(new CustomContextMenuOnClickListener() {
			@Override
			public void onClick(CharSequence menuTitle) {
				ProfileCreationWizard sw=new ProfileCreationWizard(glblParms, mContext, 
						util, profMaint, commonDlg, profileAdapter);
				sw.wizardMain();
				resetAllCheckedItem();
			}
	  	});

		if (!i_type.equals("")) {
			ccMenu.addMenuItem(msgs_prof_cont_select_all)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					for (int i=0;i<profileAdapter.getCount();i++) {
						ProfileListItem item = profileAdapter.getItem(i);
						profMaint.setProfileChecked(true, profileAdapter, item, i);
					}
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
		ccMenu.createMenu();
	};
	
	private void resetAllCheckedItem() {
		for (int i=0;i<profileAdapter.getCount();i++) {
			ProfileListItem item = profileAdapter.getItem(i);
			profMaint.setProfileChecked(false, profileAdapter, item, i);
		}
	};
	
	private void createMsglistContextMenu(View view, int idx) {

		ccMenu.addMenuItem(msgs_move_to_top,R.drawable.menu_top)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					glblParms.msgListView.setSelection(0);
				}
		});
		
		ccMenu.addMenuItem(msgs_move_to_bottom,R.drawable.menu_bottom)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					glblParms.msgListView.setSelection(glblParms.msgListView.getCount()-1);
				}
		});

		ccMenu.addMenuItem(msgs_clear_log_message,R.drawable.menu_clear)
			.setOnClickListener(new CustomContextMenuOnClickListener() {
				@Override
				public void onClick(CharSequence menuTitle) {
					glblParms.msgListView.setSelection(0);
					glblParms.msgListAdapter.clear();
					util.addLogMsg("W",getString(R.string.msgs_log_msg_cleared));
				}
		});

		ccMenu.createMenu();
	};
	
	private void editProfile(String prof_name, String prof_type,
			String prof_act, int prof_num) {
		ProfileListItem item = profileAdapter.getItem(prof_num);
		if (prof_type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
			profMaint.editProfileRemote(prof_name, prof_type, prof_num, prof_act,
					item.getAddr(),item.getUser(),item.getPass(),item.getShare(),
					item.getDir(),item.getHostname(),"");
		} else if (prof_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
			profMaint.editProfileLocal(prof_name, prof_type, prof_num, prof_act,
					item.getLocalMountPoint(),item.getDir(),"");
		} else if (prof_type.equals(SMBSYNC_PROF_TYPE_SYNC)) {
			profMaint.editProfileSync(prof_name, prof_type, prof_num, prof_act, 
					item.getMasterName(),item.getTargetName(),item.getSyncType(),
					item.getFileFilter(),item.getDirFilter(),
					item.isMasterDirFileProcess(),item.isConfirmRequired(),
					item.isForceLastModifiedUseSmbsync(),"");
		}

	};

	private void syncSelectedProfile() {
		ProfileListItem item ;
		ArrayList<MirrorIoParmList> alp = new ArrayList<MirrorIoParmList>();
		for (int i=0;i<profileAdapter.getCount();i++){
			item=profileAdapter.getItem(i);
			if (item.isChecked()&&item.getActive().equals(SMBSYNC_PROF_ACTIVE)) {
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

		for (int i=0;i< profileAdapter.getCount();i++) {
			item = profileAdapter.getItem(i);
			if (item.getActive().equals(SMBSYNC_PROF_ACTIVE) &&
					item.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
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
		enableMainUi=true;
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
		enableMainUi=false;
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
		return enableMainUi;
	};
	
	@SuppressLint("NewApi")
	final private void refreshOptionMenu() {
		util.addDebugLogMsg(1,"I","refreshOptionMenu entered");
		if (Build.VERSION.SDK_INT>=11)
			this.invalidateOptionsMenu();
	};

	private void startMirrorTask(ArrayList<MirrorIoParmList> alp) {
		final LinearLayout ll_spin=(LinearLayout)findViewById(R.id.profile_progress_spin);
		final Button btnCancel = (Button)findViewById(R.id.profile_progress_spin_btn_cancel);
		ll_spin.setVisibility(LinearLayout.VISIBLE);
		ll_spin.setBackgroundColor(Color.BLACK);
		ll_spin.bringToFront();

		btnCancel.setText(getString(R.string.msgs_progress_spin_dlg_sync_cancel));
		btnCancel.setEnabled(true);
		// CANCELボタンの指定
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				try {
					mSvcClient.aidlCancelThread();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				btnCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
				btnCancel.setEnabled(false);
				glblParms.settingAutoTerm=false;
			}
		});
		
		glblParms.msgListView.setFastScrollEnabled(false);

		glblParms.mirrorIoParms=alp;

		setUiDisabled();
		try {
			mSvcClient.aidlStartThread();
		} catch (RemoteException e) {
			e.printStackTrace();
		}
		setScreenOn(0);
		acqWifiLock();

	};

	private WakeLock mScreenOnWakelock=null;
//	private void setScreenOn() {
//		if (glblParms.settingScreenOnEnabled) {
//			if (Build.VERSION.SDK_INT>=17) {
//				getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) ;
//				
//					util.addDebugLogMsg(1,"I","setScreenOn set KEEP_SCREEN_ON");
//			} else {
//				if (!mScreenOnWakelock.isHeld()) {
//			    	
//						util.addDebugLogMsg(1,"I","setScreenOn Wakelock acquired");
//					mScreenOnWakelock.acquire();
//				} else {
//					
//						util.addDebugLogMsg(1,"I","setScreenOn Wakelock already acquired");
//				}
//			}
//		}
//	};
//	
//	private void clearScreenOn() {
//		if (glblParms.settingScreenOnEnabled) {
//			if (Build.VERSION.SDK_INT>=17) {
//				getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) ;
//				
//					util.addDebugLogMsg(1,"I","clearScreenOn clear KEEP_SCREEN_ON");
//			} else {
//				if (mScreenOnWakelock.isHeld()) {
//			    	
//						util.addDebugLogMsg(1,"I","clearScreenOn Wakelock released");
//					mScreenOnWakelock.release();
//				} else {
//			    	
//						util.addDebugLogMsg(1,"I","clearScreenOn Wakelock already released");
//				}
//			}
//		}
//	};

	private void acqWifiLock() {
		if (glblParms.settingWifiLockRequired) {
			if (!mWifiLock.isHeld()) {
				mWifiLock.acquire();
				util.addDebugLogMsg(1,"I","WifiLock acquired");
			} else {
				util.addDebugLogMsg(1,"I","Wifilock not acquired, because Wifilock already acquired");
			}
		}
	};
	
	private void relWifiLock() {
		if (glblParms.settingWifiLockRequired) {
			if (mWifiLock.isHeld()) {
				mWifiLock.release();
				util.addDebugLogMsg(1,"I","WifiLock released");
			} else {
				util.addDebugLogMsg(1,"I","Wifilock not releas, because Wifilock not acquired");
			}
		}
	};
	
	private void setScreenOn(int timeout) {
		if (glblParms.settingScreenOnEnabled) {
			if (!mScreenOnWakelock.isHeld()) {
		    	if (timeout==0) mScreenOnWakelock.acquire();
		    	else mScreenOnWakelock.acquire(timeout);
				util.addDebugLogMsg(1,"I","Wakelock acquired");
			} else {
				
					util.addDebugLogMsg(1,"I","Wakelock not acquired, because Wakelock already acquired");
			}
		}
	};
	
	private void clearScreenOn() {
		if (glblParms.settingScreenOnEnabled) {
			if (mScreenOnWakelock.isHeld()) {
				util.addDebugLogMsg(1,"I","Wakelock released");
				mScreenOnWakelock.release();
			} else {
				util.addDebugLogMsg(1,"I","Wakelock not relased, because Wakelock not acquired");
			}
		}
	};

	private void mirrorTaskEnded(String result_code, String result_msg) {
		setUiEnabled();

		final LinearLayout ll_spin=(LinearLayout)findViewById(R.id.profile_progress_spin);
		ll_spin.setVisibility(LinearLayout.GONE);
		
		historyAdapter=new AdapterSyncHistory(mContext, R.layout.sync_history_list_item_view, 
				util.loadHistoryList());
		historyListView.setAdapter(historyAdapter);
		historyAdapter.notifyDataSetChanged();
		
//		playBackDefaultNotification();
//		vibrateDefaultPattern();

		glblParms.msgListView.setFastScrollEnabled(true);
		util.flushLogFile();
		if (glblParms.settingAutoStart && glblParms.settingAutoTerm) {
			if (glblParms.settingErrorOption) {
				showMirrorThreadResult(result_code,result_msg);
				autoTerminateDlg(result_code, result_msg);
			} else {
				if (result_code.equals("OK")) {
					showMirrorThreadResult(result_code,result_msg);
					autoTerminateDlg(result_code, result_msg);
				}
				else {
					glblParms.settingAutoTerm=false;
					showMirrorThreadResult(result_code,result_msg);
				}		
			}
		} else {
			showMirrorThreadResult(result_code,result_msg);
			saveTaskData();
		}
	};
	
	private void playBackDefaultNotification() {
		Thread th=new Thread(){
			@Override
			public void run() {
				Uri uri=RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
				if (uri!=null) {
//					Ringtone rt=RingtoneManager.getRingtone(mContext, uri);
//					rt.play();
//					SystemClock.sleep(1000);
//					rt.stop();
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
			}
		};
		th.start();
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
		if (!glblParms.settingAutoTerm)
			if (util.isActivityForeground())
				commonDlg.showCommonDialog(false,"I",text,"",null);
		if (glblParms.settingBgTermNotifyMsg.equals(SMBSYNC_BG_TERM_NOTIFY_MSG_ALWAYS))
			showNotificationMsg(text);
//		Log.v("","code="+code+", pb="+glblParms.settingRingtoneWhenSyncEnded+", vib="+glblParms.settingVibrateWhenSyncEnded);
		if (code.equals("OK")) {
			if (glblParms.settingVibrateWhenSyncEnded.equals(SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ALWAYS) ||
				glblParms.settingVibrateWhenSyncEnded.equals(SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_SUCCESS)) {
				vibrateDefaultPattern();
			}
			if (glblParms.settingRingtoneWhenSyncEnded.equals(SMBSYNC_PB_RINGTONE_NOTIFICATION_ALWAYS) ||
				glblParms.settingRingtoneWhenSyncEnded.equals(SMBSYNC_PB_RINGTONE_NOTIFICATION_SUCCESS)) {
				playBackDefaultNotification();
			}
		} else {
			if (glblParms.settingVibrateWhenSyncEnded.equals(SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ALWAYS) ||
				glblParms.settingVibrateWhenSyncEnded.equals(SMBSYNC_VIBRATE_WHEN_SYNC_ENDED_ERROR)) {
				vibrateDefaultPattern();
			}
			if (glblParms.settingRingtoneWhenSyncEnded.equals(SMBSYNC_PB_RINGTONE_NOTIFICATION_ALWAYS) ||
				glblParms.settingRingtoneWhenSyncEnded.equals(SMBSYNC_PB_RINGTONE_NOTIFICATION_ERROR)) {
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
				mSvcClient.aidlStopService();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
    		mSvcClient=null;
    		unbindService(mSvcConnection);
	    	mSvcConnection=null;
//	    	Log.v("","close service");
    	}
//        Intent intent = new Intent(this, SMBSyncService.class);
//        stopService(intent);
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
		
		final LinearLayout ll_confirm=(LinearLayout)findViewById(R.id.profile_confirm);
		ll_confirm.setVisibility(LinearLayout.VISIBLE);
		ll_confirm.setBackgroundColor(Color.BLACK);
		ll_confirm.bringToFront();
		TextView dlg_title=(TextView)findViewById(R.id.copy_delete_confirm_title);
		TextView dlg_msg=(TextView)findViewById(R.id.copy_delete_confirm_msg);
		dlg_title.setText(mContext.getString(R.string.msgs_common_dialog_warning));
		dlg_title.setTextColor(Color.YELLOW);
		String msg_text="";
		if (method.equals(SMBSYNC_CONFIRM_FOR_COPY)) {
			msg_text=String.format(getString(R.string.msgs_mirror_confirm_copy_confirm),fp);
		} else {
			msg_text=String.format(getString(R.string.msgs_mirror_confirm_delete_confirm),fp);
		}
		dlg_msg.setText(msg_text);
		
		showNotificationMsg(msg_text);
		
		if (method.equals(SMBSYNC_CONFIRM_FOR_COPY)) dlg_msg.setText(
				String.format(getString(R.string.msgs_mirror_confirm_copy_confirm),fp));
		else dlg_msg.setText(String.format(getString(R.string.msgs_mirror_confirm_delete_confirm),fp));
		
		Button btnYes = (Button)findViewById(R.id.copy_delete_confirm_yes);
		Button btnYesAll = (Button)findViewById(R.id.copy_delete_confirm_yesall);
		final Button btnNo = (Button)findViewById(R.id.copy_delete_confirm_no);
		Button btnNoAll = (Button)findViewById(R.id.copy_delete_confirm_noall);
		Button btnTaskCancel = (Button)findViewById(R.id.copy_delete_confirm_task_cancel);
		
		// Yesボタンの指定
		btnYes.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ll_confirm.setVisibility(LinearLayout.GONE);
				ntfy.notifyToListener(true,new Object[]{SMBSYNC_CONFIRM_RESP_YES});
			}
		});
		// YesAllボタンの指定
		btnYesAll.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ll_confirm.setVisibility(LinearLayout.GONE);
				ntfy.notifyToListener(true,new Object[]{SMBSYNC_CONFIRM_RESP_YESALL});
			}
		});
		// Noボタンの指定
		btnNo.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ll_confirm.setVisibility(LinearLayout.GONE);
				ntfy.notifyToListener(false,new Object[]{SMBSYNC_CONFIRM_RESP_NO});
			}
		});
		// NoAllボタンの指定
		btnNoAll.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ll_confirm.setVisibility(LinearLayout.GONE);
				ntfy.notifyToListener(false,new Object[]{SMBSYNC_CONFIRM_RESP_NOALL});
			}
		});
		// Task cancelボタンの指定
		btnTaskCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ll_confirm.setVisibility(LinearLayout.GONE);
				try {
					mSvcClient.aidlCancelThread();
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				ntfy.notifyToListener(false,new Object[]{SMBSYNC_CONFIRM_RESP_NOALL});
			}
		});
	};

	private void autoStartDlg() {
		final ThreadCtrl threadCtl=new ThreadCtrl();
		threadCtl.setEnable();//enableAsyncTask();

		final LinearLayout ll_bar=(LinearLayout)findViewById(R.id.profile_progress_bar);
		ll_bar.setVisibility(LinearLayout.VISIBLE);
		ll_bar.setBackgroundColor(Color.BLACK);
		ll_bar.bringToFront();

		final TextView title = (TextView) findViewById(R.id.profile_progress_bar_msg);
		title.setText(getString(R.string.msgs_progress_bar_dlg_astart_starting));
		final Button btnCancel = (Button) findViewById(R.id.profile_progress_bar_btn_cancel);
		btnCancel.setText(getString(R.string.msgs_progress_bar_dlg_astart_cancel));
		
		// CANCELボタンの指定
		btnCancel.setEnabled(true);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				glblParms.settingAutoTerm=false;
				btnCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
				btnCancel.setEnabled(false);
				threadCtl.setDisable();//disableAsyncTask();
				util.addLogMsg("W",getString(R.string.msgs_astart_canceling));
				showNotificationMsg(getString(R.string.msgs_astart_canceling));
			}
		});
		
		final NotifyEvent at_ne=new NotifyEvent(mContext);
		at_ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				util.addLogMsg("I",getString(R.string.msgs_astart_expired));
				showNotificationMsg(getString(R.string.msgs_astart_expired));
				if (glblParms.settingAutoStart){
					
						util.addDebugLogMsg(1,"I","Auto synchronization was invoked.");
//					saveTaskData();
					boolean sel_prof=false;
					for (int i=0;i<profileAdapter.getCount();i++) {
						if (profileAdapter.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_SYNC) && 
								profileAdapter.getItem(i).isChecked()) {
							sel_prof=true;
							break;
						}
					}
					if (sel_prof && extraDataSpecifiedSyncProfile) syncSelectedProfile();
					else syncActiveProfile();
				}
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
				util.addLogMsg("W",getString(R.string.msgs_astart_cancelled));
				showNotificationMsg(getString(R.string.msgs_astart_cancelled));
			}
		});
		
		showNotificationMsg(getString(R.string.msgs_astart_started));
		util.addLogMsg("I",getString(R.string.msgs_astart_started));
		tabHost.setCurrentTab(1);
		if (extraDataSpecifiedAutoStart) {
			at_ne.notifyToListener(true, null);
		} else {
			autoTimer(threadCtl, at_ne,getString(R.string.msgs_astart_after));
		}
	};

	private void autoTerminateDlg(final String result_code, final String result_msg) {
		final ThreadCtrl threadCtl=new ThreadCtrl();
		threadCtl.setEnable();//enableAsyncTask();

		final LinearLayout ll_bar=(LinearLayout)findViewById(R.id.profile_progress_bar);
		ll_bar.setVisibility(LinearLayout.VISIBLE);
		ll_bar.setBackgroundColor(Color.BLACK);
		ll_bar.bringToFront();

		final TextView title = (TextView) findViewById(R.id.profile_progress_bar_msg);
		title.setText("");
		final Button btnCancel = (Button) findViewById(R.id.profile_progress_bar_btn_cancel);

		btnCancel.setText(getString(R.string.msgs_progress_bar_dlg_aterm_cancel));
		// CANCELボタンの指定
		btnCancel.setEnabled(true);
		btnCancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				glblParms.settingAutoTerm=false;
				btnCancel.setText(getString(R.string.msgs_progress_dlg_canceling));
				btnCancel.setEnabled(false);
				threadCtl.setDisable();//disableAsyncTask();
				util.addLogMsg("W",getString(R.string.msgs_aterm_canceling));
				showNotificationMsg(getString(R.string.msgs_aterm_canceling));
			}
		});
		
		final NotifyEvent at_ne=new NotifyEvent(mContext);
		at_ne.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				util.addLogMsg("I",getString(R.string.msgs_aterm_expired));
				if (glblParms.settingAutoTerm){
					svcStopForeground(true);

					//Wait until stopForeground() completion 
					Handler hndl=new Handler();
					hndl.post(new Runnable(){
						@Override
						public void run() {
							
								util.addDebugLogMsg(1,"I","Auto termination was invoked.");
							if (!glblParms.settingBgTermNotifyMsg.equals(SMBSYNC_BG_TERM_NOTIFY_MSG_NO)) {
//								Log.v("","result code="+result_code+", result_msg="+result_msg);
								if (glblParms.settingBgTermNotifyMsg.equals(SMBSYNC_BG_TERM_NOTIFY_MSG_ALWAYS)) 
									NotificationUtil.showNoticeNotificationMsg(mContext,glblParms,result_msg);
								else {
									if (!result_code.equals("OK")) {
										NotificationUtil.showNoticeNotificationMsg(mContext,glblParms,result_msg);
									} else {
										NotificationUtil.clearNotification(glblParms);
									}
								}
							} else {
								NotificationUtil.clearNotification(glblParms);
							}
							saveTaskData();
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
			}
		});
		util.addLogMsg("I",getString(R.string.msgs_aterm_started));
		if (extraDataSpecifiedAutoTerm) {
			util.addLogMsg("I", getString(R.string.msgs_aterm_back_ground_term));
			at_ne.notifyToListener(true, null);
		} else if (!util.isActivityForeground()) {
			util.addLogMsg("I", getString(R.string.msgs_aterm_back_ground_term));
			at_ne.notifyToListener(true, null);
		} else {
			autoTimer(threadCtl, at_ne,
					getString(R.string.msgs_aterm_terminate_after));
		}
	};

	private void autoTimer( 
			final ThreadCtrl threadCtl, final NotifyEvent at_ne, final String msg) {
		setUiDisabled();
		final Handler handler=new Handler();
       	new Thread(new Runnable() {
			@Override
			public void run() {//non UI thread
				ProgressBar pb = (ProgressBar) 
						findViewById(R.id.profile_progress_bar_progress);
				pb.setMax(ATERM_WAIT_TIME);
				final TextView pm =
						(TextView)findViewById(R.id.profile_progress_bar_msg);
				for (int i=0; i<ATERM_WAIT_TIME;i++) {
					try {
						if(threadCtl.isEnable()) {
							final int ix=i;
							handler.post(new Runnable() {
								//UI thread
								@Override
								public void run() {
									String t_msg=String.format(msg, (ATERM_WAIT_TIME-ix));
									pm.setText(t_msg);
									showNotificationMsg(t_msg);
							}});
							// non UI thread
							pb.setProgress(i);
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
						if(threadCtl.isEnable())at_ne.notifyToListener(true, null);
						else at_ne.notifyToListener(false, null);
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

		String master_local_dir="", master_local_mp="";
		String target_local_dir="", target_local_mp="";
		
		mp_profname = item.getName();
		mp_master_name = item.getMasterName();
		mp_target_name = item.getTargetName();
		mirror_prof_type = item.getSyncType();

		boolean build_success_master = false;
		boolean build_success_target = false;
		
		for (int j = 0; j <= profileAdapter.getCount() - 1; j++) {
			ProfileListItem item_master = profileAdapter.getItem(j);
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
					remote_prof_share = item_master.getShare();
					remote_prof_userid = item_master.getUser();
					remote_prof_pass = item_master.getPass();
					remote_prof_dir = item_master.getDir();
					mirror_prof_master_type = "R";// Mirror remote -> local
				}
				build_success_master=true;
			}
		}

		for (int k = 0; k <= profileAdapter.getCount() - 1; k++) {
			ProfileListItem item_target = profileAdapter.getItem(k);
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
					","+mirror_prof_type+","+remote_prof_addr+","+remote_host_name+","+remote_prof_share+
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
					item.isForceLastModifiedUseSmbsync());
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

	private void saveTaskData() {
		util.addDebugLogMsg(2,"I", "saveRestartData entered");
		
		if (!isTaskTermination) {
			if (!isTaskDataExisted() || glblParms.msgListAdapter.resetDataChanged())  {
				ActivityDataHolder data = new ActivityDataHolder();
				data.ml=glblParms.msgListAdapter.getAllItem();
				data.pl=profileAdapter.getAllItem();
				try {
				    FileOutputStream fos = openFileOutput(SMBSYNC_SERIALIZABLE_FILE_NAME, MODE_PRIVATE);
				    ObjectOutputStream oos = new ObjectOutputStream(fos);
				    oos.writeObject(data);
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
		    	new File(glblParms.SMBSync_Internal_Root_Dir+"/"+SMBSYNC_SERIALIZABLE_FILE_NAME);
	    if (lf.exists()) {
			try {
	//		    FileInputStream fis = openFileInput(SMBSYNC_SERIALIZABLE_FILE_NAME);
			    FileInputStream fis = new FileInputStream(lf); 
			    ObjectInputStream ois = new ObjectInputStream(fis);
			    ActivityDataHolder data = (ActivityDataHolder) ois.readObject();
			    ois.close();
			    lf.delete();
			    
			    ArrayList<MsgListItem> o_ml=new ArrayList<MsgListItem>(); 
				for (int i=0;i<glblParms.msgListAdapter.getCount();i++)
					o_ml.add(glblParms.msgListAdapter.getItem(i));
			    
				glblParms.msgListAdapter.clear();

				glblParms.msgListAdapter.setAllItem(data.ml);

				for (int i=0;i<o_ml.size();i++) glblParms.msgListAdapter.add(o_ml.get(i));

				glblParms.msgListAdapter.notifyDataSetChanged();
				glblParms.msgListAdapter.resetDataChanged();
				
				profileAdapter.clear();
				profileAdapter.setAllItem(data.pl);
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
		    	new File(glblParms.SMBSync_Internal_Root_Dir+"/"+SMBSYNC_SERIALIZABLE_FILE_NAME);
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
    
    private static String msgs_sync_history_ccmeu_browse;
    private static String msgs_sync_history_ccmeu_delete;
    private static String msgs_sync_history_ccmeu_unselectall;
    private static String msgs_sync_history_ccmeu_selectall;
    private static String msgs_sync_history_ccmeu_copy_clipboard;
    
    private static String msgs_prof_cont_sngl_wizard="";
    
//    private static String msgs_dlg_hardkey_back_button;
    
	private void loadMsgString() {
		
		msgs_prof_cont_sngl_wizard=getString(R.string.msgs_prof_cont_sngl_wizard);
		
		msgs_sync_history_ccmeu_browse=getString(R.string.msgs_sync_history_ccmeu_browse);
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
