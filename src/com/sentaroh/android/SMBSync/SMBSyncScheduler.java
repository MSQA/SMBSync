package com.sentaroh.android.SMBSync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SMBSyncScheduler  extends Service {

	private GlobalParameters glblParms=null;
	
	@Override
	public void onCreate() {
		super.onCreate();
		glblParms=(GlobalParameters) getApplication();
		glblParms.svcContext=this.getApplicationContext();
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		return START_STICKY;
	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	};
	
	@Override
	public boolean onUnbind(Intent intent) {
		return super.onUnbind(intent);
	};
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		android.os.Process.killProcess(android.os.Process.myPid());
	};

}
