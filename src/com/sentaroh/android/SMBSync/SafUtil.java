package com.sentaroh.android.SMBSync;

//下記サイトの情報を参考にし作成
//How to use the new SD-Card access API presented for Lollipop?
//http://stackoverflow.com/questions/26744842/how-to-use-the-new-sd-card-access-api-presented-for-lollipop

import java.io.File;
import java.util.ArrayList;

import com.sentaroh.android.Utilities.LocalMountPoint;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;


@SuppressLint("SdCardPath")
public class SafUtil {
	private static final String SAF_EXTERNAL_SDCARD_TREE_URI_KEY="external_sdcard_tree_uri_key";
	private static final boolean DEBUG_ENABLED=false;
	public static void initWorkArea(Context c, SafWorkArea swa) {
//		prefs = PreferenceManager.getDefaultSharedPreferences(c);
		swa.pkg_name=c.getPackageName();
		swa.app_spec_dir="/android/data/"+swa.pkg_name;
		String uri_string=getSafExternalSdcardRootTreeUri(c);
		if (!uri_string.equals(""))
			swa.rootDocumentFile=DCFile.fromTreeUri(c, Uri.parse(uri_string));
		buildSafExternalSdcardDirList(c, swa.external_sdcard_dir_list);
	};

	private static void buildSafExternalSdcardDirList(Context c, ArrayList<String> al) {
		File[] fl=ContextCompat.getExternalFilesDirs(c, null);
		String ld=LocalMountPoint.getExternalStorageDir();
		al.clear();
		if (Build.VERSION.SDK_INT>=21) {
			if (fl!=null) {
				for(File f:fl) {
					if (f!=null && f.getPath()!=null && !f.getPath().startsWith(ld)) {
						String esd=f.getPath().substring(0, f.getPath().indexOf("/Android/data"));
						if (DEBUG_ENABLED) Log.v("SafUtil","buildSafExternalSdcardDirList dir="+esd);
						al.add(esd);
					}
				}
			}
		}
	};
	
	public static String getSafExternalSdcardRootTreeUri(Context c) {
		long b_time=System.currentTimeMillis();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		String uri_string=prefs.getString(SAF_EXTERNAL_SDCARD_TREE_URI_KEY, "");
		if (DEBUG_ENABLED) Log.v("SafUtil","getSafExternalSdcardRootTreeUri elapsed="+(System.currentTimeMillis()-b_time));
		return uri_string;
	}
	
	public static boolean hasSafExternalSdcard(Context c) {
		boolean result=false;
//		File lf=new File("/storage/sdcard1");
//		if (lf.exists() && lf.canWrite()) result=true;
//		else {
//			lf=new File("/sdcard1");
//			if (lf.exists() && lf.canWrite()) result=true;
//		}
		ArrayList<String>al=new ArrayList<String>();
		buildSafExternalSdcardDirList(c, al);
		if (al.size()>0) result=true;
		if (DEBUG_ENABLED) Log.v("SafUtil","hasSafExternalSdcard result="+result);		
		return result;
	};

	public static boolean isSafExternalSdcardTreeUri(Context c, Uri tree_uri) {
		long b_time=System.currentTimeMillis();
		boolean result=false;
		DCFile document=DCFile.fromTreeUri(c, tree_uri);
		if (document.getName().startsWith("sdcard1")) result=true;
		if (DEBUG_ENABLED) Log.v("SafUtil","isSafExternalSdcardTreeUri elapsed="+(System.currentTimeMillis()-b_time));
		return result;
	};

	public static boolean isSafExternalSdcardPath(Context c, SafWorkArea swa, String fpath) {
		boolean result=false;
		if (Build.VERSION.SDK_INT>=21) {
			if (fpath.startsWith("/sdcard1")) {
				if (!fpath.startsWith("/sdcard1"+swa.app_spec_dir)) result=true;
			} else {
				for(String esd:swa.external_sdcard_dir_list) {
					if (fpath.startsWith(esd)) {
						if (!fpath.startsWith(esd+swa.app_spec_dir)) {
							result=true;
							break;
						}
					}
				}
			}
		}
		return result;
	};
	
	public static boolean isValidSafExternalSdcardRootTreeUri(Context c) {
		long b_time=System.currentTimeMillis();
		String uri_string=getSafExternalSdcardRootTreeUri(c);
		boolean result=true; 
		if (uri_string.equals("")) result=false;
		else {
			DCFile docf=DCFile.fromTreeUri(c, Uri.parse(uri_string));
			if (docf.getName()==null) result=false;
		}
		if (DEBUG_ENABLED) Log.v("SafUtil","isValidSafExternalSdcardRootTreeUri elapsed="+(System.currentTimeMillis()-b_time));
		return result;
	}
	@SuppressLint("NewApi")
	public static String saveSafExternalSdcardRootTreeUri(Context c, 
			String tree_uri_string) {
        String edit_string="";
        if (tree_uri_string.length()>(tree_uri_string.indexOf("%3A")+3)) {
        	edit_string=tree_uri_string.substring(0,(tree_uri_string.indexOf("%3A")+3));
        } else {
        	edit_string=tree_uri_string;
        }
//        Log.v("","edit_string="+edit_string);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(c);
		prefs.edit().putString(SAF_EXTERNAL_SDCARD_TREE_URI_KEY, edit_string).commit();
		
		c.getContentResolver().takePersistableUriPermission(Uri.parse(edit_string),
			      Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

		return edit_string;
	};
	
	public static String getFileNameFromPath(String fpath) {
		String result="";
		String[] st=fpath.split("/");
		if (st!=null) {
			if (st[st.length-1]!=null) result=st[st.length-1];
		}
		return result;
	}

	public static DCFile getSafDocumentFileByPath(Context c, SafWorkArea swa,  
			String target_path, boolean isDirectory) {
		long b_time=System.currentTimeMillis();
    	DCFile document=swa.rootDocumentFile;
    	
    	String relativePath = null;
    	String baseFolder="";
    	if (target_path.startsWith("/sdcard1")) baseFolder="/sdcard1/";
    	else if (target_path.startsWith("/storage/sdcard1")) baseFolder="/storage/sdcard1/";
    	else return null;
    	
    	relativePath=target_path.replace(baseFolder, "");
    	
    	
    	
    	if (DEBUG_ENABLED) Log.v("SafUtil","relativePath="+relativePath);
        String[] parts = relativePath.split("\\/");
        for (int i = 0; i < parts.length; i++) {
        	if (DEBUG_ENABLED) Log.v("SafUtil","parts="+parts[i]);
        	if (!parts[i].equals("")) {
                DCFile nextDocument = document.findFile(parts[i]);
//                DCFile nextDocument = findFile(document, parts[i]);
                if (nextDocument == null) {
                    if ((i < parts.length - 1) || isDirectory) {
                    	if (DEBUG_ENABLED) Log.v("SafUtil","dir created name="+parts[i]);
                   		nextDocument = document.createDirectory(parts[i]);
                    } else {
                    	if (DEBUG_ENABLED) Log.v("SafUtil","file created name="+parts[i]);
                        nextDocument = document.createFile("", parts[i]);
                    }
                }
                document = nextDocument;
        	}
        }
//    	c.getContentResolver().releasePersistableUriPermission(treeUri,
//                Intent.FLAG_GRANT_READ_URI_PERMISSION |
//                Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        if (DEBUG_ENABLED) Log.v("SafUtil","getSafDocumentFileByPath elapsed="+(System.currentTimeMillis()-b_time));
        return document;
	};
	
//	private static DCFile findFile(DCFile df, String name) {
//		DCFile result=null;
//		
//		DCFile[]lf=df.listFiles();
//		if (lf!=null) {
//			for(DCFile sdf:lf) {
//				if (sdf.canRead()) {
//					if (sdf.getName()!=null) {
//						if (sdf.getName().equals(name)) {
//							result=sdf;
//							break;
//						}
//					}
//				}
//			}
//		}
//		return result;
//	}

}

class SafWorkArea {
//	private SharedPreferences prefs=null;
	public String pkg_name="";
	public String app_spec_dir="";
	public DCFile rootDocumentFile=null;
	public ArrayList<String> external_sdcard_dir_list=new ArrayList<String>();
}
