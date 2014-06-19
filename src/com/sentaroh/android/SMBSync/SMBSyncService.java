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
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.RemoteException;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.ThreadCtrl;

@SuppressLint("Wakelock")
public class SMBSyncService extends Service {
	private GlobalParameters glblParms=null;
	
	private SMBSyncUtil mUtil=null;
	
	private ThreadCtrl tcMirror=null, tcConfirm=null;
	
	private WakeLock mWakeLock=null;

	@Override
	public void onCreate() {
		super.onCreate();
		glblParms=(GlobalParameters) getApplication();
		glblParms.svcContext=this.getApplicationContext();
		NotificationUtil.initNotification(glblParms);
		mUtil=new SMBSyncUtil(getApplicationContext(), "Service", glblParms);

		
		mWakeLock=((PowerManager)getSystemService(Context.POWER_SERVICE))
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
		mUtil.addDebugLogMsg(1,"I","onCreate entered");
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
		mUtil.closeLogFile();
		android.os.Process.killProcess(android.os.Process.myPid());
//		glblParms.logWriter.close();
	};
    
    final private ISvcClient.Stub mSvcClientStub = new ISvcClient.Stub() {
		@Override
		public void setCallBack(ISvcCallback callback)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlSetCallBack entered");
			glblParms.callBackStub=callback;
		}

		@Override
		public void removeCallBack(ISvcCallback callback)
				throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlRemoveCallBack entered");
			glblParms.callBackStub=null;
		}

		@Override
		public void aidlStartThread() throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlStartThread entered");
			startThread();
		}

		@Override
		public void aidlCancelThread() throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlCancelThread entered");
			tcMirror.setDisable();
		}

		@Override
		public void aidlStartForeground() throws RemoteException {
			mUtil.addDebugLogMsg(1, "I", "aidlStartForeground entered");
//			NotificationUtil.setNotificationEnabled(glblParms);
			startForeground(R.string.app_name,glblParms.notification);
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
			NotificationUtil.showOngoingNotificationMsg(glblParms, prof, fp, msg);
		}
		@Override
		public void aidlAcqWakeLock() throws RemoteException {
			if (mWakeLock.isHeld()) {
				mUtil.addDebugLogMsg(1, "I", "aidlAcqWakeLock WakeLock not acquired, already held.");
			} else {
				mWakeLock.acquire();
				mUtil.addDebugLogMsg(1, "I", "aidlAcqWakeLock WakeLock acquired.");
			}
		}
		@Override
		public void aidlRelWakeLock() throws RemoteException {
			if (!mWakeLock.isHeld()) {
				mUtil.addDebugLogMsg(1, "I", "aidlRelWakeLock WakeLock not released, not held.");
			} else {
				mWakeLock.release();
				mUtil.addDebugLogMsg(1, "I", "aidlRelWakeLock WakeLock released.");
			}
		}
    };

	@SuppressLint("Wakelock")
	private void startThread() {
//		final Handler hndl=new Handler();
		NotificationUtil.setNotificationIcon(glblParms, R.drawable.ic_48_smbsync_run_anim);
		tcConfirm.initThreadCtrl();
		tcMirror.initThreadCtrl();
		tcMirror.setEnable();//enableAsyncTask();
		tcConfirm.setEnable();//enableAsyncTask();
		NotifyEvent ntfy = new NotifyEvent(this);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c, Object[] o) {
				NotificationUtil.setNotificationIcon(glblParms, R.drawable.ic_48_smbsync_wait);
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
					glblParms.callBackStub.cbThreadEnded(result_code, result_msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			} 
		});
		
		Thread tm = new Thread(new MirrorIO(glblParms, ntfy, tcMirror, tcConfirm)); 
		tm.setPriority(Thread.MIN_PRIORITY);
		tm.start();
	};

}