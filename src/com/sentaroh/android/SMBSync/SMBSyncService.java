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

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.ThreadCtrl;

@SuppressLint("Wakelock")
public class SMBSyncService extends Service {
	private GlobalParameters mGp=null;
	
	private SMBSyncUtil mUtil=null;
	
	private ThreadCtrl tcMirror=null, tcConfirm=null;
	
	private WakeLock mPartialWakeLock=null;
	
	private ISvcCallback callBackStub=null;

	private WifiManager mWifiMgr=null;
	
	private WifiReceiver mWifiReceiver=new WifiReceiver();
	
	@Override
	public void onCreate() {
		super.onCreate();
		mGp=(GlobalParameters) getApplication();
		mGp.svcContext=this.getApplicationContext();
		NotificationUtil.initNotification(mGp);
		mUtil=new SMBSyncUtil(getApplicationContext(), "Service", mGp);
		
		mUtil.addDebugLogMsg(1,"I","onCreate entered");

		mWifiMgr=(WifiManager)getSystemService(Context.WIFI_SERVICE);
		
		initWifiStatus();
		
		IntentFilter int_filter = new IntentFilter();
  		int_filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
  		int_filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
  		int_filter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);
		int_filter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);
		int_filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		int_filter.addAction(WifiManager.NETWORK_IDS_CHANGED_ACTION);
		registerReceiver(mWifiReceiver, int_filter);

		mPartialWakeLock=((PowerManager)getSystemService(Context.POWER_SERVICE))
    			.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK
	    				| PowerManager.ACQUIRE_CAUSES_WAKEUP
//	   	    				| PowerManager.ON_AFTER_RELEASE
	    				, "SMBSync-Service");

//		PackageInfo packageInfo;
//		try {
//			packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
//			int flags = packageInfo.applicationInfo.flags;
//			mDebugEnabled = (flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0;
//		} catch (NameNotFoundException e) {
//			e.printStackTrace();
//		}           
		tcMirror=new ThreadCtrl(); 
		tcConfirm=new ThreadCtrl();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		mUtil.addDebugLogMsg(1,"I","onStartCommand entered");
		return START_STICKY;
	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		mUtil.addDebugLogMsg(1,"I","onBind entered,action="+arg0.getAction());
//		if (arg0.getAction().equals("MessageConnection")) 
			return mSvcClientStub;
//		else return svcInterface;
	};
	
	@Override
	public boolean onUnbind(Intent intent) {
		mUtil.addDebugLogMsg(1,"I","onUnbind entered");
		return super.onUnbind(intent);
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mUtil.addDebugLogMsg(1,"I","onDestroy entered");
		unregisterReceiver(mWifiReceiver);
		mUtil.closeLogFile();
		android.os.Process.killProcess(android.os.Process.myPid());
//		glblParms.logWriter.close();
	};
    
    final private ISvcClient.Stub mSvcClientStub = new ISvcClient.Stub() {
		@Override
		public void setCallBack(ISvcCallback callback)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlSetCallBack entered");
			callBackStub=callback;
		}

		@Override
		public void removeCallBack(ISvcCallback callback)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlRemoveCallBack entered");
			callBackStub=null;
		}

		@Override
		public void aidlStartThread() throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlStartThread entered");
			startThread();
		}

		@Override
		public void aidlCancelThread() throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlCancelThread entered");
			synchronized(tcMirror) {
				tcMirror.setDisabled();
				tcMirror.notify();
			}
		}

		@Override
		public void aidlStartForeground() throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlStartForeground entered");
//			NotificationUtil.setNotificationEnabled(glblParms);
			startForeground(R.string.app_name,mGp.notification);
		}

		@Override
		public void aidlStopForeground(boolean clear_notification)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlStopForeground entered, clear="+clear_notification);
//			if (clear_notification) NotificationUtil.setNotificationDisabled(glblParms);
			stopForeground(clear_notification);
		}

		@Override
		public void aidlConfirmResponse(int confirmed)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlConfirmResponse entered, confirmed="+confirmed);
			synchronized(tcConfirm) {
				tcConfirm.setExtraDataInt(confirmed);
				tcConfirm.notify();
			}
		}

		@Override
		public void aidlStopService() throws RemoteException {
			stopSelf();
		}

		@Override
		public void aidlShowNotificationMsg(String prof, String fp, String msg)
				throws RemoteException {
			NotificationUtil.showOngoingMsg(mGp, prof, fp, msg);
		}
		
		@Override
		public void aidlSetNotificationIcon(int icon_res)
				throws RemoteException {
//			Log.v("","icon="+icon_res);
//			Thread.currentThread().dumpStack();
			NotificationUtil.setNotificationIcon(mGp, icon_res);
		}
		
		@Override
		public void aidlAcqWakeLock() throws RemoteException {
			if (mPartialWakeLock.isHeld()) {
				mUtil.addDebugLogMsg(1, "I", "aidlAcqWakeLock WakeLock not acquired, already held.");
			} else {
				mPartialWakeLock.acquire();
				mUtil.addDebugLogMsg(1, "I", "aidlAcqWakeLock WakeLock acquired.");
			}
		}
		@Override
		public void aidlRelWakeLock() throws RemoteException {
			if (!mPartialWakeLock.isHeld()) {
				mUtil.addDebugLogMsg(1, "I", "aidlRelWakeLock WakeLock not released, not held.");
			} else {
				mPartialWakeLock.release();
				mUtil.addDebugLogMsg(1, "I", "aidlRelWakeLock WakeLock released.");
			}
		}
    };

	@SuppressLint("Wakelock")
	private void startThread() {
//		final Handler hndl=new Handler();
		NotificationUtil.setNotificationIcon(mGp, R.drawable.ic_48_smbsync_run_anim);
		tcConfirm.initThreadCtrl();
		tcMirror.initThreadCtrl();
		tcMirror.setEnabled();//enableAsyncTask();
		tcConfirm.setEnabled();//enableAsyncTask();
		NotifyEvent ntfy = new NotifyEvent(this);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c, Object[] o) {
				NotificationUtil.setNotificationIcon(mGp, R.drawable.ic_48_smbsync_wait);
				String result_code="", result_msg="";
				if (tcMirror.isThreadResultSuccess()) {
					result_code="OK";
					result_msg=tcMirror.getThreadMessage();
				} else if (tcMirror.isThreadResultCancelled()) {
					result_code="CANCELLED";
					result_msg=tcMirror.getThreadMessage();
				} else if (tcMirror.isThreadResultError()) {
					result_code="ERROR";
					result_msg=tcMirror.getThreadMessage();
				}
				try {
					callBackStub.cbThreadEnded(result_code, result_msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {} 
		});
		
		Thread tm = new Thread(new MirrorIO(mGp, ntfy, tcMirror, tcConfirm, callBackStub)); 
		tm.setPriority(Thread.MIN_PRIORITY);
		tm.start();
	};
	
	private void initWifiStatus() {
		mGp.wifiIsActive=mWifiMgr.isWifiEnabled();
		if (mGp.wifiIsActive) {
			if (mWifiMgr.getConnectionInfo().getSSID()!=null) mGp.wifiSsid=mWifiMgr.getConnectionInfo().getSSID();
			else mGp.wifiSsid="";
		}
		mUtil.addDebugLogMsg(1,"I","Wi-Fi Status, Active="+mGp.wifiIsActive+", SSID="+mGp.wifiSsid);
	};
	
    final private class WifiReceiver  extends BroadcastReceiver {
		@Override
		final public void onReceive(Context c, Intent in) {
			String tssid=mWifiMgr.getConnectionInfo().getSSID();
			String wssid="";
			String ss=mWifiMgr.getConnectionInfo().getSupplicantState().toString();
			if (tssid==null || tssid.equals("<unknown ssid>")) wssid="";
			else wssid=tssid.replaceAll("\"", "");
			if (wssid.equals("0x")) wssid="";
			
			boolean new_wifi_enabled=mWifiMgr.isWifiEnabled();
			if (!new_wifi_enabled && mGp.wifiIsActive ) {
				mUtil.addDebugLogMsg(1,"I","WIFI receiver, WIFI Off");
				mGp.wifiSsid="";
				mGp.wifiIsActive=false;
				try {
					if (callBackStub!=null) callBackStub.cbWifiStatusChanged("On", "");
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			} else {
				if (ss.equals("COMPLETED") ||ss.equals("ASSOCIATING") || ss.equals("ASSOCIATED")) {
					if (mGp.wifiSsid.equals("") && !wssid.equals("")) {
						mUtil.addDebugLogMsg(1,"I","WIFI receiver, Connected WIFI Access point ssid="+wssid);
						mGp.wifiSsid=wssid;
						mGp.wifiIsActive=true;
						try {
							if (callBackStub!=null) callBackStub.cbWifiStatusChanged("Connected", mGp.wifiSsid);
						} catch (RemoteException e) {
							e.printStackTrace();
						}
					}
				} else if (ss.equals("INACTIVE") ||
						ss.equals("DISCONNECTED") ||
						ss.equals("UNINITIALIZED") ||
						ss.equals("INTERFACE_DISABLED") ||
						ss.equals("SCANNING")) {
					if (mGp.wifiIsActive) {
						if (!mGp.wifiSsid.equals("")) {
							mUtil.addDebugLogMsg(1,"I","WIFI receiver, Disconnected WIFI Access point ssid="+mGp.wifiSsid);
							mGp.wifiSsid="";
							mGp.wifiIsActive=true;
							try {
								if (callBackStub!=null) callBackStub.cbWifiStatusChanged("Disconnected", "");
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					} else {
						if (new_wifi_enabled) {
							mUtil.addDebugLogMsg(1,"I","WIFI receiver, WIFI On");
							mGp.wifiSsid="";
							mGp.wifiIsActive=true;
							try {
								if (callBackStub!=null) callBackStub.cbWifiStatusChanged("On", "");
							} catch (RemoteException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		}
    };

	
}