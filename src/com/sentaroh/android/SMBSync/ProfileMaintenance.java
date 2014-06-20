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
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROFILE_FILE_NAME_V1;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROFILE_FILE_NAME_V2;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROFILE_FILE_NAME_V3;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROFILE_FILE_NAME_V4;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROFILE_FILE_NAME_V5;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_ACTIVE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_DEC;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_ENC;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_FILTER_EXCLUDE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_FILTER_INCLUDE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_GROUP_DEFAULT;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_INACTIVE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_LOCAL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_REMOTE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_SETTINGS;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_SYNC;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_VER1;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_VER2;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_VER3;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_VER4;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_VER5;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SETTINGS_TYPE_BOOLEAN;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SETTINGS_TYPE_INT;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SETTINGS_TYPE_STRING;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_COPY;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MIRROR;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MOVE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;

import com.sentaroh.android.Utilities.Base64Compat;
import com.sentaroh.android.Utilities.EncryptUtil;
import com.sentaroh.android.Utilities.EncryptUtil.CipherParms;
import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.NetworkUtil;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.ThreadCtrl;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenu;
import com.sentaroh.android.Utilities.ContextMenu.CustomContextMenuItem.CustomContextMenuOnClickListener;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Dialog.DialogBackKeyListener;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistAdapter;
import com.sentaroh.android.Utilities.TreeFilelist.TreeFilelistItem;
import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;

public class ProfileMaintenance {

	private boolean errorCreateProfileList;

	private CustomContextMenu ccMenu=null;
	private String smbUser,smbPass;
	
	private Context mContext;
	
	private SMBSyncUtil util;
	
	private HashMap<Integer, String[]> 
		importedSettingParmList=new HashMap<Integer, String[]>();
	
	private CommonDialog commonDlg=null;
	
	private GlobalParameters mGp=null;

	private String mProfilePassword="";
	private final String mProfilePasswordPrefix="*SMBSync*";
	
	ProfileMaintenance (SMBSyncUtil mu, Context c,  
			CommonDialog cd, CustomContextMenu ccm, GlobalParameters gp) {
		mContext=c;
		
		mGp=gp;
		
		util=mu;
		
		loadMsgString();
		
		commonDlg=cd;
		
		ccMenu=ccm;
		
		AdapterProfileList tpfa=createProfileList(false,"");
		mGp.profileAdapter.setAllItem(tpfa.getAllItem());
		mGp.profileAdapter.notifyDataSetChanged();
	};

	public void importProfileDlg(final String lurl, final String ldir, 
			String file_name, final NotifyEvent p_ntfy) {
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
    			final String fpath=(String)o[0];
    			
    			NotifyEvent ntfy_pswd=new NotifyEvent(mContext);
    			ntfy_pswd.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						mProfilePassword=(String)o[0];
		    			final AdapterProfileList tfl = createProfileList(true,fpath);
		    			ProfileListItem pli=tfl.getItem(0);
		    			if (errorCreateProfileList) {
			    			commonDlg.showCommonDialog(false,"E",
			    					String.format(msgs_import_prof_fail,fpath),"",null);
		    			} else {
		    				if (tfl.getCount()==1 && pli.getType().equals("")) {
		    	    			commonDlg.showCommonDialog(false,"E",
		    	    					String.format(msgs_import_prof_fail_no_valid_item,fpath),"",null);
		    				} else {
		    					selectImportProfileItem(tfl, p_ntfy);
		    				}
		    			}
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
    			});
    			if (isProfileWasEncrypted(fpath)) {
    				promptPasswordForImport(fpath,ntfy_pswd);
    			} else ntfy_pswd.notifyToListener(true, new Object[]{""});
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
			}
		});
		commonDlg.fileOnlySelectWithoutCreate(
				lurl,ldir,file_name,msgs_select_import_file,ntfy);
	};
	
	private boolean isProfileWasEncrypted(String fpath) {
		boolean result=false;
		File lf=new File(fpath);
		if (lf.exists() && lf.canRead()) {
			try {
				BufferedReader br;
				br = new BufferedReader(new FileReader(fpath),8192);
				String pl = br.readLine();
				if (pl!=null) {
					if (pl.startsWith(SMBSYNC_PROF_VER1) || pl.startsWith(SMBSYNC_PROF_VER2)) {
						//NOtencrypted
					} else if (pl.startsWith(SMBSYNC_PROF_VER3)) {
						if (pl.startsWith(SMBSYNC_PROF_VER3+SMBSYNC_PROF_ENC)) result=true;
					} else if (pl.startsWith(SMBSYNC_PROF_VER4)) {
						if (pl.startsWith(SMBSYNC_PROF_VER4+SMBSYNC_PROF_ENC)) result=true;
					} else if (pl.startsWith(SMBSYNC_PROF_VER5)) {
						if (pl.startsWith(SMBSYNC_PROF_VER5+SMBSYNC_PROF_ENC)) result=true;
					}
				}
				br.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	};
	
	public void promptPasswordForImport(final String fpath,  
			final NotifyEvent ntfy_pswd) {
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.password_input_dlg);
		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.password_input_msg);
		final CheckBox cb_protect = (CheckBox) dialog.findViewById(R.id.password_input_protect);
		final Button btn_ok = (Button) dialog.findViewById(R.id.password_input_ok_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.password_input_cancel_btn);
		final EditText et_password=(EditText) dialog.findViewById(R.id.password_input_password);
		final EditText et_confirm=(EditText) dialog.findViewById(R.id.password_input_password_confirm);
		et_confirm.setVisibility(EditText.GONE);
		btn_ok.setText(mContext.getString(R.string.msgs_export_import_pswd_btn_ok));
		cb_protect.setVisibility(CheckBox.GONE);
		
		dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_password_required));
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		btn_ok.setEnabled(false);
		et_password.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				if (arg0.length()>0) btn_ok.setEnabled(true);
				else btn_ok.setEnabled(false);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		//OK button
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String passwd=et_password.getText().toString();
				BufferedReader br;
				String pl;
				boolean pswd_invalid=true;
				try {
					br = new BufferedReader(new FileReader(fpath),8192);
					pl = br.readLine();
					if (pl!=null) {
						String enc_str="";
						if (pl.startsWith(SMBSYNC_PROF_VER3+SMBSYNC_PROF_ENC)) {
							enc_str=pl.replace(SMBSYNC_PROF_VER3+SMBSYNC_PROF_ENC, "");
						} else if (pl.startsWith(SMBSYNC_PROF_VER4+SMBSYNC_PROF_ENC)) {
							enc_str=pl.replace(SMBSYNC_PROF_VER4+SMBSYNC_PROF_ENC, "");
						} else if (pl.startsWith(SMBSYNC_PROF_VER5+SMBSYNC_PROF_ENC)) {
							enc_str=pl.replace(SMBSYNC_PROF_VER5+SMBSYNC_PROF_ENC, "");
						}
						if (!enc_str.equals("")) {
							CipherParms cp=EncryptUtil.initDecryptEnv(
									mProfilePasswordPrefix+passwd);
							byte[] enc_array=Base64Compat.decode(enc_str, Base64Compat.NO_WRAP);
							String dec_str=EncryptUtil.decrypt(enc_array, cp);
							if (!SMBSYNC_PROF_ENC.equals(dec_str)) {
								dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_invalid_password));
							} else {
								pswd_invalid=false;
							}
						}
					}
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				if (!pswd_invalid) {
					dialog.dismiss();
					ntfy_pswd.notifyToListener(true, new Object[] {passwd});
				}
			}
		});
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				ntfy_pswd.notifyToListener(false, null);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setCancelable(false);
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
		dialog.show();

	};

	public void promptPasswordForExport(final String fpath,  
			final NotifyEvent ntfy_pswd) {
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.password_input_dlg);
		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.password_input_msg);
		final CheckBox cb_protect = (CheckBox) dialog.findViewById(R.id.password_input_protect);
		final Button btn_ok = (Button) dialog.findViewById(R.id.password_input_ok_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.password_input_cancel_btn);
		final EditText et_password=(EditText) dialog.findViewById(R.id.password_input_password);
		final EditText et_confirm=(EditText) dialog.findViewById(R.id.password_input_password_confirm);
		
		dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_specify_password));
		
		CommonDialog.setDlgBoxSizeCompact(dialog);

		cb_protect.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				setPasswordFieldVisibility(isChecked, et_password,
						et_confirm, btn_ok, dlg_msg);
			}
		});
		cb_protect.setChecked(mGp.settingExportedProfileEncryptRequired);
		setPasswordFieldVisibility(mGp.settingExportedProfileEncryptRequired,
				et_password, et_confirm, btn_ok, dlg_msg);

		et_password.setEnabled(true);
		et_confirm.setEnabled(false);
		et_password.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				btn_ok.setEnabled(false);
				setPasswordPromptOkButton(et_password, et_confirm, 
						btn_ok, dlg_msg);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		et_confirm.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				btn_ok.setEnabled(false);
				setPasswordPromptOkButton(et_password, et_confirm, 
						btn_ok, dlg_msg);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		//OK button
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String passwd=et_password.getText().toString();
				if ((cb_protect.isChecked() && !mGp.settingExportedProfileEncryptRequired) ||
						(!cb_protect.isChecked() && mGp.settingExportedProfileEncryptRequired)) {
					mGp.settingExportedProfileEncryptRequired=cb_protect.isChecked();
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
					prefs.edit().putBoolean(mContext.getString(R.string.settings_exported_profile_encryption), 
							cb_protect.isChecked()).commit();
				}
				if (!cb_protect.isChecked()) {
					dialog.dismiss();
					ntfy_pswd.notifyToListener(true, new Object[] {""});
				} else {
					if (!passwd.equals(et_confirm.getText().toString())) {
						//Unmatch
						dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
					} else {
						dialog.dismiss();
						ntfy_pswd.notifyToListener(true, new Object[] {passwd});
					}
				}
			}
		});
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				ntfy_pswd.notifyToListener(false, null);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setCancelable(false);
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
		dialog.show();

	};

	private void setPasswordFieldVisibility(boolean isChecked, EditText et_password,
			EditText et_confirm, Button btn_ok, TextView dlg_msg) {
		if (isChecked) {
			et_password.setVisibility(EditText.VISIBLE);
			et_confirm.setVisibility(EditText.VISIBLE);
			setPasswordPromptOkButton(et_password, et_confirm, 
					btn_ok, dlg_msg);
		} else {
			dlg_msg.setText("");
			et_password.setVisibility(EditText.GONE);
			et_confirm.setVisibility(EditText.GONE);
		}
	};
	
	private void setPasswordPromptOkButton(EditText et_passwd, EditText et_confirm, 
			Button btn_ok, TextView dlg_msg) {
		String password=et_passwd.getText().toString();
		String confirm=et_confirm.getText().toString();
		if (password.length()>0 && et_confirm.getText().length()==0) {
			dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
			et_confirm.setEnabled(true);
		} else if (password.length()>0 && et_confirm.getText().length()>0) {
			et_confirm.setEnabled(true);
			if (!password.equals(confirm)) {
				btn_ok.setEnabled(false);
				dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
			} else {
				btn_ok.setEnabled(true);
				dlg_msg.setText("");
			}
		} else if (password.length()==0 && confirm.length()==0) {
			btn_ok.setEnabled(true);
			dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_specify_password));
			et_passwd.setEnabled(true);
			et_confirm.setEnabled(false);
		} else if (password.length()==0 && confirm.length()>0) {
			dlg_msg.setText(mContext.getString(R.string.msgs_export_import_pswd_unmatched_confirm_pswd));
		}

	};
	
	private void selectImportProfileItem(final AdapterProfileList tfl, final NotifyEvent p_ntfy) {
		ArrayList<ExportImportProfileListItem> eipl=new ArrayList<ExportImportProfileListItem>();

		for (int i=0;i<tfl.getCount();i++) {
			ProfileListItem pl=tfl.getItem(i);
			ExportImportProfileListItem eipli=new ExportImportProfileListItem();
			eipli.isChecked=true;
			eipli.item_type=pl.getType();
			eipli.item_name=pl.getName();
			eipl.add(eipli);
		}
//		Collections.sort(eipl, new Comparator<ExportImportProfileListItem>(){
//			@Override
//			public int compare(ExportImportProfileListItem arg0,
//					ExportImportProfileListItem arg1) {
//				if (arg0.item_name.equals(arg1.item_name)) return arg0.item_type.compareToIgnoreCase(arg1.item_type);
//				return arg0.item_name.compareToIgnoreCase(arg1.item_name);
//			}
//		});
//		ExportImportProfileListItem eipli=new ExportImportProfileListItem();
//		eipli.isChecked=true;
//		eipli.item_type="*";
//		eipli.item_name=mContext.getString(R.string.msgs_export_import_profile_setting_parms);
//		eipl.add(eipli);
		
		final Dialog dialog=new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.export_import_profile_dlg);
		dialog.setCanceledOnTouchOutside(false);
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		
		TextView tv_title=(TextView)dialog.findViewById(R.id.export_import_profile_title);
		tv_title.setText(mContext.getString(R.string.msgs_export_import_profile_title));
		TextView tv_msg=(TextView)dialog.findViewById(R.id.export_import_profile_msg);
		tv_msg.setVisibility(LinearLayout.GONE);
		LinearLayout ll_filelist=(LinearLayout)dialog.findViewById(R.id.export_import_profile_file_list);
		ll_filelist.setVisibility(LinearLayout.GONE);
		final Button ok_btn=(Button)dialog.findViewById(R.id.export_import_profile_ok_btn);
		Button cancel_btn=(Button)dialog.findViewById(R.id.export_import_profile_cancel_btn);
		
		final RadioGroup rg_select=(RadioGroup)dialog.findViewById(R.id.export_import_profile_list_rg_item_select);
		final RadioButton rb_select_all=(RadioButton)dialog.findViewById(R.id.export_import_profile_list_rb_select_all);
		final RadioButton rb_unselect_all=(RadioButton)dialog.findViewById(R.id.export_import_profile_list_rb_unselect_all);
		final CheckBox cb_reset_profile=(CheckBox)dialog.findViewById(R.id.export_import_profile_list_cb_reset_profile);
		final CheckBox cb_import_settings=(CheckBox)dialog.findViewById(R.id.export_import_profile_list_cb_import_settings);
		cb_import_settings.setChecked(true);
		
		final AdapterExportImportProfileList imp_list_adapt=
				new AdapterExportImportProfileList(mContext, R.layout.export_import_profile_list_item, eipl);
		
		ListView lv=(ListView)dialog.findViewById(R.id.export_import_profile_listview);
		lv.setAdapter(imp_list_adapt);
		
		lv.setOnItemClickListener(new OnItemClickListener(){
			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int pos,
					long arg3) {
				  imp_list_adapt.getItem(pos).isChecked=!imp_list_adapt.getItem(pos).isChecked;
				  imp_list_adapt.notifyDataSetChanged();
				  if (imp_list_adapt.isItemSelected()) {
					  ok_btn.setEnabled(true);
				  } else {
					  ok_btn.setEnabled(false);
				  }
			}
		});
		
		lv.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				ccMenu.addMenuItem(
						mContext.getString(R.string.msgs_export_import_profile_select_all),R.drawable.blank)
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
				  @Override
				  final public void onClick(CharSequence menuTitle) {
					  for (int i=0;i<imp_list_adapt.getCount();i++)
						  imp_list_adapt.getItem(i).isChecked=true;
					  imp_list_adapt.notifyDataSetChanged();
					  ok_btn.setEnabled(true);
				  	}
			  	});
				ccMenu.addMenuItem(
						mContext.getString(R.string.msgs_export_import_profile_unselect_all),R.drawable.blank)
			  	.setOnClickListener(new CustomContextMenuOnClickListener() {
				  @Override
				  final public void onClick(CharSequence menuTitle) {
					  for (int i=0;i<imp_list_adapt.getCount();i++)
						  imp_list_adapt.getItem(i).isChecked=false;
					  imp_list_adapt.notifyDataSetChanged();
					  ok_btn.setEnabled(false);
				  	}
			  	});
				ccMenu.createMenu();
				return false;
			}
		});
		
		rg_select.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(RadioGroup arg0, int arg1) {
				if (arg1==R.id.export_import_profile_list_rb_select_all) {
					for (int i=0;i<imp_list_adapt.getCount();i++)
						  imp_list_adapt.getItem(i).isChecked=true;
					rb_select_all.setChecked(false);
				} else if (arg1==R.id.export_import_profile_list_rb_unselect_all) {
					for (int i=0;i<imp_list_adapt.getCount();i++)
						  imp_list_adapt.getItem(i).isChecked=false;
					rb_unselect_all.setChecked(false);
				}
				imp_list_adapt.notifyDataSetChanged();
			}
		});
		
		NotifyEvent ntfy_cb_listener=new NotifyEvent(mContext);
		ntfy_cb_listener.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				  if (imp_list_adapt.isItemSelected()) {
					  ok_btn.setEnabled(true);
				  } else {
					  if (cb_import_settings.isChecked()) ok_btn.setEnabled(true);
					  else ok_btn.setEnabled(false);
				  }
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		imp_list_adapt.setCheckButtonListener(ntfy_cb_listener);
		
		
		ok_btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (cb_reset_profile.isChecked()) mGp.profileAdapter.clear();
				importSelectedProfileItem(imp_list_adapt,tfl,
						cb_import_settings.isChecked(),p_ntfy);
				dialog.dismiss();
			}
		});
		cancel_btn.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				dialog.dismiss();
			}
		});
		
		dialog.show();
		
	};
	
	private void importSelectedProfileItem(
			final AdapterExportImportProfileList imp_list_adapt,
			final AdapterProfileList tfl, final boolean import_settings,
			final NotifyEvent p_ntfy) {
		String repl_list="";
		for (int i=0;i<imp_list_adapt.getCount();i++) {
			ExportImportProfileListItem eipli=imp_list_adapt.getItem(i);
			if (eipli.isChecked &&
					getProfile(eipli.item_name, mGp.profileAdapter)!=null) {
				repl_list+=eipli.item_name+"\n";
			}
		}
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
				String imp_list="";
				for (int i=0;i<tfl.getCount();i++) {
					ProfileListItem ipfli=tfl.getItem(i);
					ExportImportProfileListItem eipli=imp_list_adapt.getItem(i);
					if (eipli.isChecked ) {
						imp_list+=ipfli.getName()+"\n";
//						Log.v("","name1="+ipfli.getName()+
//								", result="+getProfile(ipfli.getName(), mGp.profileAdapter));
						ProfileListItem mpfli=getProfile(ipfli.getName(), mGp.profileAdapter);
						if (mpfli!=null) {
							mGp.profileAdapter.remove(mpfli);
							mGp.profileAdapter.add(ipfli);
						} else {
							mGp.profileAdapter.add(ipfli);
						}
					}
				}
//				ExportImportProfileListItem eipli=imp_list_adapt.getItem(imp_list_adapt.getCount()-1);
				if (import_settings) {
					restoreImportedSettingParms();
					imp_list+=mContext.getString(R.string.msgs_export_import_profile_setting_parms)+"\n";
				}
				if (imp_list.length()>0) imp_list+=" ";
				mGp.profileAdapter.sort();
				mGp.profileListView.setSelection(0);
				saveProfileToFile(false,"","",mGp.profileAdapter,false);
				commonDlg.showCommonDialog(false,"I",
						mContext.getString(R.string.msgs_export_import_profile_import_success),
						imp_list,null); 
				if (import_settings) p_ntfy.notifyToListener(true, null);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {
			}
		});
		if (!repl_list.equals("")) {
			//Confirm
			commonDlg.showCommonDialog(true,"W",
					mContext.getString(R.string.msgs_export_import_profile_confirm_override),
					repl_list,ntfy); 
		} else {
			ntfy.notifyToListener(true, null);
		}
		
	};

	private void restoreImportedSettingParms() {
		final HashMap<Integer, String[]> spl=importedSettingParmList;
		
		if (spl.size()==0) {
			util.addDebugLogMsg(2,"I","Import setting parms can not be not found.");
			return;
		}
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		
		if (spl.size()>=0) {
			for (int i=0;i<spl.size();i++) {
				if (spl.get(i)[1].equals(SMBSYNC_SETTINGS_TYPE_STRING)) {
					prefs.edit().putString(spl.get(i)[0],spl.get(i)[2]).commit();
					util.addDebugLogMsg(2,"I","Imported Settings="+
							spl.get(i)[0]+"="+spl.get(i)[2]);
				} else if (spl.get(i)[1].equals(SMBSYNC_SETTINGS_TYPE_BOOLEAN)) {
					boolean b_val = false;
					if (spl.get(i)[2].equals("false")) b_val = false;
					else b_val = true;
					prefs.edit().putBoolean(spl.get(i)[0],b_val).commit();
					util.addDebugLogMsg(2,"I","Imported Settings="+
							spl.get(i)[0]+"="+spl.get(i)[2]);
				} else if (spl.get(i)[1].equals(SMBSYNC_SETTINGS_TYPE_INT)) {
					int i_val = 0;
					i_val = Integer.parseInt(spl.get(i)[2]);;
					prefs.edit().putInt(spl.get(i)[0],i_val).commit();
					util.addDebugLogMsg(2,"I","Imported Settings="+
							spl.get(i)[0]+"="+spl.get(i)[2]);
				}
			}
//			applySettingParms();
		}
	};

	public void exportProfileDlg(final String lurl, final String ldir, final String ifn) {

		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
    			final String fpath=(String)o[0];
    			NotifyEvent ntfy_pswd=new NotifyEvent(mContext);
    			ntfy_pswd.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						mProfilePassword=(String)o[0];
						boolean encrypt_required=false;
						if (!mProfilePassword.equals("")) encrypt_required=true; 
		    			String fd=fpath.substring(0,fpath.lastIndexOf("/"));
		    			String fn=fpath.replace(fd+"/","");
		    			exportProfileToFile(fd,fn,encrypt_required);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
					}
    			});
    			promptPasswordForExport(fpath,ntfy_pswd);
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});
		commonDlg.fileOnlySelectWithCreate(
				lurl,ldir,ifn,msgs_select_export_file,ntfy);
	};	
			
	public void exportProfileToFile(final String profile_dir, 
			final String profile_filename, final boolean encrypt_required) {
		
		File lf = new File(profile_dir+"/"+profile_filename);
		if (lf.exists()) {
			NotifyEvent ntfy=new NotifyEvent(mContext);
			ntfy.setListener(new NotifyEventListener() {
				@Override
				public void positiveResponse(Context c,Object[] o) {
	    			String fp =profile_dir+"/"+profile_filename;
	    			String fd =profile_dir;
	    			
					if (saveProfileToFile(true,fd,fp,mGp.profileAdapter,encrypt_required)) {
						commonDlg.showCommonDialog(false,"I",
								String.format(msgs_export_prof_success,fp),"",null);
						util.addDebugLogMsg(1,"I","Profile was exported. fn="+fp);						
					} else {
						commonDlg.showCommonDialog(false,"E",
								String.format(msgs_export_prof_fail,fp),"",null);
					}
				}
	
				@Override
				public void negativeResponse(Context c,Object[] o) {}
			});
			commonDlg.showCommonDialog(true,"W",msgs_export_prof_title,
					profile_dir+"/"+profile_filename+" "+msgs_override,ntfy);
		} else {
			String fp =profile_dir+"/"+profile_filename;
			String fd =profile_dir;
			if (saveProfileToFile(true,fd,fp,mGp.profileAdapter,encrypt_required)) {
				commonDlg.showCommonDialog(false,"I",
						String.format(msgs_export_prof_success,fp),"",null);
				util.addDebugLogMsg(1,"I","Profile was exported. fn="+fp);						
			} else {
				commonDlg.showCommonDialog(false,"E",
						String.format(msgs_export_prof_fail,fp),"",null);
			}
		}
	};
	
	public void setProfileToChecked(boolean chk, AdapterProfileList pa, 
			ProfileListItem item, int no) {
		
		if (chk) {
			item.setChecked(true);
			pa.replace(item,no);
		} else {
			item.setChecked(false);
			pa.replace(item,no);
		}
	};
	
	public void setProfileToActive() {
		ProfileListItem item ;

		int pos=mGp.profileListView.getFirstVisiblePosition();
		int posTop=mGp.profileListView.getChildAt(0).getTop();
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			item = mGp.profileAdapter.getItem(i);
			if (item.isChecked()) {
				if (!item.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
					item.setActive(SMBSYNC_PROF_ACTIVE);
					item.setChecked(false);
					mGp.profileAdapter.replace(item,i); 
				} else {
					if (isSyncProfileDisabled(item)) {
						//Not activated
						commonDlg.showCommonDialog(false, "W", 
								mContext.getString(R.string.msgs_prof_active_not_activated), "", null);
					} else {
						item.setActive(SMBSYNC_PROF_ACTIVE);
						item.setChecked(false);
						mGp.profileAdapter.replace(item,i); 
					}
				}
			} 
		}

//		resolveSyncProfileRelation();

		saveProfileToFile(false,"","",mGp.profileAdapter,false);
		mGp.profileAdapter.setNotifyOnChange(true);
		mGp.profileListView.setSelectionFromTop(pos,posTop);			
	};
	
	public void setProfileToInactive() {
		ProfileListItem item ;

		int pos=mGp.profileListView.getFirstVisiblePosition();
		int posTop=mGp.profileListView.getChildAt(0).getTop();
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			item = mGp.profileAdapter.getItem(i);
			if (item.isChecked()) {
				item.setActive(SMBSYNC_PROF_INACTIVE);
				item.setChecked(false);
				mGp.profileAdapter.replace(item,i);
			}		
		}
		
		resolveSyncProfileRelation();
		
		saveProfileToFile(false,"","",mGp.profileAdapter,false);
		mGp.profileAdapter.setNotifyOnChange(true);
		mGp.profileListView.setSelectionFromTop(pos,posTop);
	};
	
	public void deleteProfile() {
		final int[] dpnum = new int[mGp.profileAdapter.getCount()];
		String dpmsg="";
		
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			if (mGp.profileAdapter.getItem(i).isChecked()) {
				dpmsg=dpmsg+mGp.profileAdapter.getItem(i).getName()+"\n";
				dpnum[i]=i;
			} else dpnum[i]=-1;
		}

		NotifyEvent ntfy=new NotifyEvent(mContext);
		// set commonDlg.showCommonDialog response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				ArrayList<ProfileListItem> dpItemList = new ArrayList<ProfileListItem>();
				int pos=mGp.profileListView.getFirstVisiblePosition();
				for (int i=0;i<dpnum.length;i++) {
					if (dpnum[i]!=-1)
						dpItemList.add(mGp.profileAdapter.getItem(dpnum[i]));
				}
				for (int i=0;i<dpItemList.size();i++) mGp.profileAdapter.remove(dpItemList.get(i));
				
				resolveSyncProfileRelation();
				
				saveProfileToFile(false,"","",mGp.profileAdapter,false);
				
				if (mGp.profileAdapter.getCount()<=0) {
					mGp.profileAdapter.add(new ProfileListItem("","",
							mContext.getString(R.string.msgs_no_profile_entry),
							"","",null,false));
				}
//				glblParms.profileListView.setAdapter(glblParms.profileAdapter);
				mGp.profileAdapter.setNotifyOnChange(true);
				mGp.profileListView.setSelection(pos);
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});
		commonDlg.showCommonDialog(true,"W",msgs_delete_following_profile,dpmsg,ntfy);
	};
	
	private void resolveSyncProfileRelation() {
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			ProfileListItem item=mGp.profileAdapter.getItem(i);
			if (item.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
				if (isSyncProfileDisabled(item)) {
//					glblParms.profileAdapter.remove(glblParms.profileAdapter.getItem(i));
					mGp.profileAdapter.replace(item,i);
				}

			}
		}
	};

	private boolean isSyncProfileDisabled(ProfileListItem item) {
		boolean result=false;
		if (getProfileType(item.getMasterName(),mGp.profileAdapter).equals("")) {
			item.setMasterType("");
			item.setMasterName("");
			item.setActive(SMBSYNC_PROF_INACTIVE);
			result=true;
		} else {
			if (!isProfileActive(SMBSYNC_PROF_GROUP_DEFAULT,
					item.getMasterType(), item.getMasterName())) {
				item.setActive(SMBSYNC_PROF_INACTIVE);
				result=true;
			}
		}
		if (getProfileType(item.getTargetName(),mGp.profileAdapter).equals("")) {
			item.setTargetType("");
			item.setTargetName("");
			item.setActive(SMBSYNC_PROF_INACTIVE);
			result=true;
		} else {
			if (!isProfileActive(SMBSYNC_PROF_GROUP_DEFAULT,
					item.getTargetType(), item.getTargetName())) {
				item.setActive(SMBSYNC_PROF_INACTIVE);
				result=true;
			}
		}
		return result;
	};
	
	public void addLocalProfile(boolean add_op, String prof_name, String prof_act,
			String prof_lmp, String prof_dir, String dialog_msg) {
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.edit_profile_local);
		final TextView dlg_title=(TextView) dialog.findViewById(R.id.local_profile_dlg_title);
		if (add_op) dlg_title.setText(msgs_add_local_profile);
		else dlg_title.setText(msgs_copy_local_profile);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.local_profile_dlg_msg);
		dlg_msg.setText(dialog_msg);
		final CheckBox tg = (CheckBox) dialog.findViewById(R.id.local_profile_active);
		final EditText editdir = (EditText) dialog.findViewById(R.id.local_profile_dir);
		editdir.setText(prof_dir);
		final EditText editname = (EditText) dialog.findViewById(R.id.local_profile_name);
		editname.setText(prof_name);

		final Spinner spinner=
				(Spinner) dialog.findViewById(R.id.local_profile_lmp_btn);
		spinner.setVisibility(Spinner.VISIBLE);

		setLocalMountPointSpinner(spinner, prof_lmp);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		if (prof_act.equals("A")) tg.setChecked(true);
		else tg.setChecked(false);
		
		// GET_btn1ボタンの指定
		Button btnGet1 = (Button) dialog.findViewById(R.id.local_profile_get_btn1);
		if (!mGp.externalStorageIsMounted) btnGet1.setEnabled(false);
		btnGet1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				
				String url=(String)spinner.getSelectedItem();
				String p_dir=editdir.getText().toString();

				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						editdir.setText((String)arg1[0]);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText("");
					}
					
				});
				selectLocalDirDlg(url,"",p_dir,ntfy);
			}
		});
		// CANCELボタンの指定
		final Button btn_cancel = (Button) dialog.findViewById(R.id.local_profile_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		
		final Button btn_ok = (Button) dialog.findViewById(R.id.local_profile_ok);
		btn_ok.setEnabled(false);
		dlg_msg.setText(msgs_audit_msgs_profilename2);
		
		editname.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (!isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_LOCAL, s.toString())) {
					String audit_msg="";
					if (hasInvalidChar(s.toString(),new String[]{"\t"})) {
						audit_msg=String.format(msgs_audit_msgs_profilename1,detectedInvalidCharMsg);
						btn_ok.setEnabled(false);
					} else if (s.toString().length()==0) {
						audit_msg=msgs_audit_msgs_profilename2;
						editname.requestFocus();
						btn_ok.setEnabled(false);
					} else {
						btn_ok.setEnabled(true);
					}
					dlg_msg.setText(audit_msg);
				} else {
					dlg_msg.setText(msgs_duplicate_profile);
					btn_ok.setEnabled(false);
				}
			}
		});
		editdir.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				String audit_msg="";
				if (hasInvalidChar(s.toString(),new String[]{"\t"})) {
					audit_msg=String.format(msgs_audit_msgs_local_dir,detectedInvalidCharMsg);
				}
				dlg_msg.setText(audit_msg);
			}
		});

		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String prof_name, prof_dir, prof_act, prof_lmp;
				boolean audit_error = false;
				String audit_msg="";
				prof_lmp=mGp.SMBSync_External_Root_Dir;
				prof_dir = editdir.getText().toString();
				prof_name = editname.getText().toString();
				if (hasInvalidChar(prof_dir,new String[]{"\t"})) {
					audit_error=true;
					prof_dir=removeInvalidChar(prof_dir);
					audit_msg=String.format(msgs_audit_msgs_local_dir,detectedInvalidCharMsg);
					editdir.setText(prof_dir);
				}
				if (!audit_error) {
					if (hasInvalidChar(prof_name,new String[]{"\t"})) {
						audit_error=true;
						prof_dir=removeInvalidChar(prof_name);
						audit_msg=String.format(msgs_audit_msgs_profilename1,detectedInvalidCharMsg);
						editname.setText(prof_name);
					} else if (prof_name.length()==0) {
						audit_error=true;
						audit_msg=msgs_audit_msgs_profilename2;
						editname.requestFocus();
					}
				}
				if (tg.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
					else prof_act = SMBSYNC_PROF_INACTIVE;
				
				if (audit_error) {
					((TextView) dialog.findViewById(R.id.local_profile_dlg_msg))
						.setText(audit_msg);
					return;
				} else {
					if (!isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_LOCAL, prof_name)) {
						dialog.dismiss();
//						int pos=glblParms.profileListView.getFirstVisiblePosition();
//						int posTop=glblParms.profileListView.getChildAt(0).getTop();
						if (mGp.profileAdapter.getItem(0).getType().equals(""))
							mGp.profileAdapter.remove(0);
						updateLocalProfileAdapter(true,prof_name,prof_act,
								prof_lmp, prof_dir,false,0);
						mGp.profileAdapter.sort();
						saveProfileToFile(false,"","",mGp.profileAdapter,false);
//						AdapterProfileList tfl= createProfileList(false,"");
//						replaceglblParms.profileAdapterContent(tfl);
//						glblParms.profileListView.setSelectionFromTop(pos,posTop);
					} else {
						((TextView) dialog.findViewById(R.id.local_profile_dlg_msg))
							.setText(msgs_duplicate_profile);
						return;
					}
//					glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
				}
			}
		}); 
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
	};

//	public void replaceglblParms.profileAdapterContentX(AdapterProfileList tfl) {
//		glblParms.profileAdapter.clear();
//		ProfileListItem item ;
//		for (int i=0;i<tfl.getCount();i++) {
//			item=tfl.getItem(i);
//			glblParms.profileAdapter.add(item);
//		}
//		glblParms.profileAdapter.setNotifyOnChange(true);
//	};
	
	public void addRemoteProfile(boolean add_op,String prof_name, String prof_act,
			String prof_addr, String prof_user, String prof_pass,
			String prof_share, final String prof_dir, String prof_host, 
			String prof_port, String dialog_msg) {

		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.edit_profile_remote);
		final TextView dlg_title=(TextView) dialog.findViewById(R.id.remote_profile_dlg_title);
		if (add_op) dlg_title.setText(msgs_add_remote_profile);
		else dlg_title.setText(msgs_copy_remote_profile);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.remote_profile_dlg_msg);
		dlg_msg.setText(dialog_msg);

		final CheckBox tg = (CheckBox) dialog.findViewById(R.id.remote_profile_active);
		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);
		final EditText editdir = (EditText) dialog.findViewById(R.id.remote_profile_dir);
		final EditText editname = (EditText) dialog.findViewById(R.id.remote_profile_name);

		final EditText edithost = (EditText) dialog.findViewById(R.id.remote_profile_hostname);
		final CheckBox cb_use_hostname = (CheckBox) dialog.findViewById(R.id.remote_profile_use_computer_name);
		final CheckBox cb_use_user_pass = (CheckBox) dialog.findViewById(R.id.remote_profile_use_user_pass);
		
		final Button btnAddr = (Button) dialog.findViewById(R.id.remote_profile_get_addr_btn);
		final Button btnListShare = (Button) dialog.findViewById(R.id.remote_profile_get_btn1);
		final Button btnListDir = (Button) dialog.findViewById(R.id.remote_profile_get_btn2);

		if (prof_host.equals("")) {
			cb_use_hostname.setChecked(false);
			editaddr.setVisibility(EditText.VISIBLE);
			edithost.setVisibility(EditText.GONE);
		} else {
			cb_use_hostname.setChecked(true);
			editaddr.setVisibility(EditText.GONE);
			edithost.setVisibility(EditText.VISIBLE);
		}
		editaddr.setText(prof_addr);
		edithost.setText(prof_host);
		if (prof_user.equals("") && prof_pass.equals("")) {
			cb_use_user_pass.setChecked(false);
			edituser.setEnabled(false);
			editpass.setEnabled(false);
		} else {
			cb_use_user_pass.setChecked(true);
			edituser.setEnabled(true);
			editpass.setEnabled(true);
		}

		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.remote_profile_use_port_number);
		final EditText editport = (EditText) dialog.findViewById(R.id.remote_profile_port);
		if (!prof_port.equals("")) {
			cb_use_port_number.setChecked(true);
			editport.setText(prof_port);
		} else {
			editport.setText("");
			editport.setEnabled(false);
		}
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		editname.setText(prof_name);
		editaddr.setText(prof_addr);
		edithost.setText(prof_host);
		edituser.setText(prof_user);
		editpass.setText(prof_pass);
		editshare.setText(prof_share);
		editdir.setText(prof_dir);
		if (prof_act.equals("A")) tg.setChecked(true);
		else tg.setChecked(false);
		
		// IpAddressScanボタンの指定
		if (util.isRemoteDisable()) btnAddr.setEnabled(false);
		btnAddr.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ipAddressScanButtonDlg(dialog);
			}
		});
		
		if (util.isRemoteDisable()) btnListShare.setEnabled(false);
		btnListShare.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				invokeSelectRemoteShareDlg(dialog);
			}
		});
		
		if (util.isRemoteDisable()) btnListDir.setEnabled(false);
		btnListDir.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				selectRemoteDirectory(dialog);
			}
		});
		
		final Button btnLogon = (Button) dialog.findViewById(R.id.remote_profile_logon);
		btnLogon.setEnabled(false);
		btnLogon.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String user=null, pass=null;
				if (cb_use_user_pass.isChecked()) {
					if (edituser.getText().length()>0) user=edituser.getText().toString();
					if (editpass.getText().length()>0) pass=editpass.getText().toString();
				}
				String port="";
				if (cb_use_port_number.isChecked()) port=editport.getText().toString();
				if (cb_use_hostname.isChecked()) {
					logonToRemoteDlg(edithost.getText().toString(),
							"", port, user,pass,null);
				} else {
					String t_addr=editaddr.getText().toString();
					String s_addr=t_addr;
					if (t_addr.indexOf(":")>=0) s_addr=t_addr.substring(0,t_addr.indexOf(":")) ;
					logonToRemoteDlg("",s_addr,
							port, user,pass,null);
				}
			}
		});

		// CANCELボタンの指定
		final Button btn_cancel = (Button) dialog.findViewById(R.id.remote_profile_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});

		final Button btn_ok = (Button) dialog.findViewById(R.id.remote_profile_ok);
		btn_ok.setEnabled(false);
		dlg_msg.setText(msgs_audit_msgs_profilename2);
		
		setRemoteProfileCommonListener(dialog);
		
		editname.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (!isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_REMOTE, s.toString())) {
					String audit_msg="";
					if (hasInvalidChar(s.toString(),new String[]{"\t"})) {
						audit_msg=String.format(msgs_audit_msgs_profilename1,detectedInvalidCharMsg);
						btn_ok.setEnabled(false);
					} else if (s.toString().length()==0) {
						audit_msg=msgs_audit_msgs_profilename2;
						editname.requestFocus();
						btn_ok.setEnabled(false);
					} else {
						btn_ok.setEnabled(true);
					}
					dlg_msg.setText(audit_msg);
				} else {
					dlg_msg.setText(msgs_duplicate_profile);
					btn_ok.setEnabled(false);
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		
		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String prof_name, prof_user, prof_pass, prof_share, prof_dir, prof_act;
				String prof_addr="", prof_host="";
				
				prof_addr = editaddr.getText().toString();

				prof_host = edithost.getText().toString();
				prof_user = edituser.getText().toString();
				prof_pass = editpass.getText().toString();
				prof_share = editshare.getText().toString();
				prof_dir = editdir.getText().toString();
				prof_name = editname.getText().toString();

				if (!cb_use_user_pass.isChecked()) {
					prof_user="";
					prof_pass="";
				}
				
				if (tg.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
					else prof_act = SMBSYNC_PROF_INACTIVE;
//				String e_msg=auditRemoteProfileField(dialog);
//				if (e_msg.length()!=0) {
//					((TextView) dialog.findViewById(R.id.remote_profile_dlg_msg))
//						.setText(e_msg);
//					return;
//				} else {
					if (!isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_REMOTE, prof_name)) {
						dialog.dismiss();
						if (cb_use_hostname.isChecked()) prof_addr="";
						else prof_host="";
						String remote_port="";
						if (cb_use_port_number.isChecked()) remote_port=editport.getText().toString();
//						int pos=glblParms.profileListView.getFirstVisiblePosition();
//						int posTop=glblParms.profileListView.getChildAt(0).getTop();
						if (mGp.profileAdapter.getItem(0).getType().equals(""))
							mGp.profileAdapter.remove(0);
						updateRemoteProfileAdapter(true, prof_name, prof_act,prof_dir,
								prof_user,prof_pass,prof_share,prof_addr,prof_host,
								remote_port, false,0);
						mGp.profileAdapter.sort();
						mGp.profileAdapter.notifyDataSetChanged();
						saveProfileToFile(false,"","",mGp.profileAdapter,false);
//						AdapterProfileList tfl= createProfileList(false,"");
//						replaceglblParms.profileAdapterContent(tfl);
//						glblParms.profileListView.setSelectionFromTop(pos,posTop);
					} else {
						((TextView) dialog.findViewById(R.id.remote_profile_dlg_msg))
						.setText(msgs_duplicate_profile);
					}
//				}
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();

	};

	public void logonToRemoteDlg(final String host, final String addr, final String port, 
			final String user, final String pass, final NotifyEvent p_ntfy) {
		final ThreadCtrl tc=new ThreadCtrl();
		tc.setEnable();
		tc.setThreadResultSuccess();
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.progress_spin_dlg);
		((TextView)dialog.findViewById(R.id.progress_spin_dlg_title))
			.setText(R.string.msgs_progress_spin_dlg_test_logon);
		((TextView)dialog.findViewById(R.id.progress_spin_dlg_msg))
			.setText("");
		((TextView)dialog.findViewById(R.id.progress_spin_dlg_msg))
			.setVisibility(TextView.GONE);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.progress_spin_dlg_btn_cancel);
		btn_cancel.setText(R.string.msgs_progress_spin_dlg_test_logon_cancel);
		
//		(dialog.context.findViewById(R.id.progress_spin_dlg)).setVisibility(TextView.GONE);
//		(dialog.context.findViewById(R.id.progress_spin_dlg)).setEnabled(false);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tc.setDisable();//disableAsyncTask();
				btn_cancel.setText(mContext.getString(R.string.msgs_progress_dlg_canceling));
				btn_cancel.setEnabled(false);
				util.addDebugLogMsg(1,"W","Logon is cancelled.");
			}
		});
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
//		dialog.show(); showDelayedProgDlgで表示

		Thread th=new Thread() {
			@Override
			public void run() {
				util.addDebugLogMsg(1,"I","Test logon started, host="+host+", addr="+addr+
						", port="+port+", user="+user);
				NtlmPasswordAuthentication auth=new NtlmPasswordAuthentication(null, user, pass);
				
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						dialog.dismiss();
						String err_msg=(String)o[0];
						if (tc.isEnable()) {
							if (err_msg!=null) {
								commonDlg.showCommonDialog(false, "E", 
										mContext.getString(R.string.msgs_remote_profile_dlg_logon_error)
										, err_msg, null);
								if (p_ntfy!=null) p_ntfy.notifyToListener(false, null);
							} else {
								commonDlg.showCommonDialog(false, "I", "", 
									mContext.getString(R.string.msgs_remote_profile_dlg_logon_success), null);
								if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
							}
						} else {
							commonDlg.showCommonDialog(false, "I", "", 
									mContext.getString(R.string.msgs_remote_profile_dlg_logon_cancel), null);
								if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
						}
					}

					@Override
					public void negativeResponse(Context c, Object[] o) {}					
				});
				
				if (host.equals("")) {
					boolean reachable=false;
					if (port.equals("")) {
						if (NetworkUtil.isIpAddressAndPortConnected(addr,139,3500) ||
								NetworkUtil.isIpAddressAndPortConnected(addr,445,3500)) {
							reachable=true;
						}
					} else {
						reachable=NetworkUtil.isIpAddressAndPortConnected(addr,
								Integer.parseInt(port),3500);
					}
					if (reachable) {
						testAuth(auth,addr,port,ntfy);
					} else {
						util.addDebugLogMsg(1,"I","Test logon failed, remote server not connected");
						String unreachble_msg="";
						if (port.equals("")) {
							unreachble_msg=String.format(mContext.getString(R.string.msgs_mirror_remote_addr_not_connected)
									,addr);
						} else {
							unreachble_msg=String.format(mContext.getString(R.string.msgs_mirror_remote_addr_not_connected_with_port)
									,addr,port);
						}
						ntfy.notifyToListener(true, new Object[]{unreachble_msg});
					}
				} else {
					if (NetworkUtil.getSmbHostIpAddressFromName(host)!=null) testAuth(auth,host,port,ntfy);
					else {
						util.addDebugLogMsg(1,"I","Test logon failed, remote server not connected");
						String unreachble_msg="";
						unreachble_msg=mContext.getString(R.string.msgs_mirror_remote_name_not_found)+host;
						ntfy.notifyToListener(true, new Object[]{unreachble_msg});
					}
				}
			}
		};
		th.start();
		dialog.show();
	};
	
	@SuppressWarnings("unused")
	private void testAuth(final NtlmPasswordAuthentication auth, 
			final String host, String port, final NotifyEvent ntfy) {
		final UncaughtExceptionHandler defaultUEH = 
				Thread.currentThread().getUncaughtExceptionHandler();
        Thread.currentThread().setUncaughtExceptionHandler( new Thread.UncaughtExceptionHandler() {
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
    			String end_msg=ex.toString()+st_msg;
    			ntfy.notifyToListener(true, new Object[] {end_msg});
                // re-throw critical exception further to the os (important)
//                defaultUEH.uncaughtException(thread, ex);
            }
        });

		String err_msg=null;
		SmbFile sf=null;
		SmbFile[] lf=null;
		String url="";
		if (port.equals("")) {
			url="smb://"+host+"/IPC$/";
		} else {
			url="smb://"+host+":"+port+"/IPC$/";
		}
//		Log.v("","url="+url);
		try {
			sf=new SmbFile(url,auth);
			sf.connect();
//			sf.getSecurity();
//			lf=sf.listFiles();
//			if (lf!=null) {
//				for (int i=0;i<lf.length;i++) {
//					Log.v("","name="+lf[i].getName()+", share="+lf[i].getShare());
//				}
//			}
//			String aa=null;
//			aa.length();
			util.addDebugLogMsg(1,"I","Test logon completed, host="+host+
					", port="+port+", user="+auth.getUsername());
		} catch(SmbException e) {
//			if (e.getNtStatus()==NtStatus.NT_STATUS_LOGON_FAILURE ||
//					e.getNtStatus()==NtStatus.NT_STATUS_ACCOUNT_RESTRICTION ||
//					e.getNtStatus()==NtStatus.NT_STATUS_INVALID_LOGON_HOURS ||
//					e.getNtStatus()==NtStatus.NT_STATUS_INVALID_WORKSTATION ||
//					e.getNtStatus()==NtStatus.NT_STATUS_PASSWORD_EXPIRED ||
//					e.getNtStatus()==NtStatus.NT_STATUS_ACCOUNT_DISABLED) {
//			}
			String[] e_msg=NetworkUtil.analyzeNtStatusCode(e, mContext, url, auth.getUsername());
			err_msg=e_msg[0];
			util.addDebugLogMsg(1,"I","Test logon failed."+"\n"+err_msg);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Thread.currentThread().setUncaughtExceptionHandler(defaultUEH);
		ntfy.notifyToListener(true, new Object[] {err_msg});
	};
	
	private void setRemoteProfileCommonListener(final Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.remote_profile_dlg_msg);

		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
		final EditText edithost = (EditText) dialog.findViewById(R.id.remote_profile_hostname);

		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);
		final EditText editdir = (EditText) dialog.findViewById(R.id.remote_profile_dir);

		final CheckBox cb_use_hostname = (CheckBox) dialog.findViewById(R.id.remote_profile_use_computer_name);
		final CheckBox cb_use_user_pass = (CheckBox) dialog.findViewById(R.id.remote_profile_use_user_pass);

		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.remote_profile_use_port_number);
		final EditText editport = (EditText) dialog.findViewById(R.id.remote_profile_port);

		
//		final Button btn_logon = (Button) dialog.findViewById(R.id.remote_profile_logon);
//		final Button btn_ok = (Button) dialog.findViewById(R.id.remote_profile_ok);
		final LinearLayout ll_port = (LinearLayout) dialog.findViewById(R.id.remote_profile_port_option);
		if (mGp.settingShowRemotePortOption) {
			ll_port.setVisibility(LinearLayout.VISIBLE);
		} else {
			ll_port.setVisibility(LinearLayout.GONE);
		}
		
		cb_use_hostname.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				if (isChecked) {
					editaddr.setVisibility(EditText.GONE);
					edithost.setVisibility(EditText.VISIBLE);
				} else {
					editaddr.setVisibility(EditText.VISIBLE);
					edithost.setVisibility(EditText.GONE);
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});

		cb_use_user_pass.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				if (isChecked) {
					edituser.setEnabled(true);
					editpass.setEnabled(true);
				} else {
					edituser.setEnabled(false);
					editpass.setEnabled(false);
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		editaddr.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(msgs_audit_msgs_address1,detectedInvalidCharMsg));
					editaddr.setText(new_val);
					editaddr.requestFocus();
				} else if (s.length()==0) {
					if (!cb_use_hostname.isChecked() && editaddr.getText().length()==0) {
						dlg_msg.setText(msgs_audit_msgs_address2);
						editaddr.requestFocus();
					} else {
						if (cb_use_hostname.isChecked() && edithost.getText().length()==0) {
							dlg_msg.setText(msgs_audit_msgs_address2);
							edithost.requestFocus();
						}
					}
				}
				setRemoteProfileOkBtnEnabled(dialog); 
			}
		});
		edithost.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(msgs_audit_msgs_address1,detectedInvalidCharMsg));
					edithost.setText(new_val);
					edithost.requestFocus();
				} else if (s.length()==0) {
					if (!cb_use_hostname.isChecked() && editaddr.getText().length()==0) {
						dlg_msg.setText(msgs_audit_msgs_address2);
						editaddr.requestFocus();
					} else {
						if (cb_use_hostname.isChecked() && edithost.getText().length()==0) {
							dlg_msg.setText(msgs_audit_msgs_address2);
							edithost.requestFocus();
						}
					}
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		edituser.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(msgs_audit_msgs_username1,detectedInvalidCharMsg));
					editaddr.setText(new_val);
					editaddr.requestFocus();
				} else if (s.length()==0) {
					if (edituser.getText().length()==0 && editpass.getText().length()==0) {
						dlg_msg.setText(String.format(msgs_audit_msgs_user_or_pass_missing,detectedInvalidCharMsg));
						edituser.requestFocus();
					}
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		editpass.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(msgs_audit_msgs_password1,detectedInvalidCharMsg));
					editaddr.setText(new_val);
					editaddr.requestFocus();
				} else if (s.length()==0) {
					if (edituser.getText().length()==0 && editpass.getText().length()==0) {
						dlg_msg.setText(String.format(msgs_audit_msgs_user_or_pass_missing,detectedInvalidCharMsg));
						edituser.requestFocus();
					}
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		editshare.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(msgs_audit_msgs_share1,detectedInvalidCharMsg));
					editshare.setText(new_val);
					editshare.requestFocus();
				} else if (s.toString().length()==0) {
					dlg_msg.setText(msgs_audit_msgs_share2);
					editshare.requestFocus();
				}
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
		editdir.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (hasInvalidChar(s.toString(),new String[]{"\t"})) {
					String new_val=removeInvalidChar(s.toString());
					dlg_msg.setText(String.format(msgs_audit_msgs_dir1,detectedInvalidCharMsg));
					editdir.setText(new_val);
				}
			}
		});
		
		cb_use_port_number.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) editport.setEnabled(true);
				else editport.setEnabled(false);
				setRemoteProfileOkBtnEnabled(dialog);
			}
			
		});
		editport.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				setRemoteProfileOkBtnEnabled(dialog);
			}
		});
	};
	
	private boolean auditIpAddressValue(String in_addr) {
		boolean result=NetworkUtil.isValidIpAddress(in_addr);
		return result;
	};

	
	private void setRemoteProfileOkBtnEnabled(Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.remote_profile_dlg_msg);

		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
		final EditText edithost = (EditText) dialog.findViewById(R.id.remote_profile_hostname);

		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);

		final CheckBox cb_use_hostname = (CheckBox) dialog.findViewById(R.id.remote_profile_use_computer_name);
		final CheckBox cb_use_user_pass = (CheckBox) dialog.findViewById(R.id.remote_profile_use_user_pass);

		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.remote_profile_use_port_number);
		final EditText editport = (EditText) dialog.findViewById(R.id.remote_profile_port);
		
		final Button btn_logon = (Button) dialog.findViewById(R.id.remote_profile_logon);
		final Button btn_ok = (Button) dialog.findViewById(R.id.remote_profile_ok);
		btn_ok.setEnabled(false);
		btn_logon.setEnabled(false);
		if (cb_use_hostname.isChecked()) {
			if (edithost.getText().length()>0) {
				dlg_msg.setText("");
			} else {
				dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostname_not_spec));
				return;
			}
		} else {
			if (auditIpAddressValue(editaddr.getText().toString())) {
				dlg_msg.setText("");
			} else {
				dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostaddr_not_spec));
				return;
			}
		}

		if (cb_use_user_pass.isChecked()) {
			if (edituser.getText().length()>0 || editpass.getText().length()>0) {
				dlg_msg.setText("");
			} else {
				dlg_msg.setText(msgs_audit_msgs_user_or_pass_missing);
				return;
			}
		}
		
		if (cb_use_port_number.isChecked()) {
			if (editport.getText().length()>0) {
				dlg_msg.setText("");
			} else {
				dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostport_not_spec));
				btn_logon.setEnabled(false);
				return;
			}
		}
		if (cb_use_user_pass.isChecked() && !util.isRemoteDisable()) btn_logon.setEnabled(true);
		else btn_logon.setEnabled(false);

		if (editshare.getText().length()==0) {
			dlg_msg.setText(msgs_audit_share_not_spec);
			return;
		}
		btn_ok.setEnabled(true);	
	};

	public void setSyncOptionSpinner(Spinner spinnerSyncOption, String prof_syncopt) {
//		final Spinner spinnerSyncOption=(Spinner)dialog.findViewById(R.id.sync_profile_sync_option);
		final CustomSpinnerAdapter adapterSyncOption=new CustomSpinnerAdapter(mContext, R.layout.custom_simple_spinner_item);
		adapterSyncOption.setTextColor(Color.BLACK);
		adapterSyncOption.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerSyncOption.setPrompt(mContext.getString(R.string.msgs_sync_profile_dlg_syncopt_prompt));
		spinnerSyncOption.setAdapter(adapterSyncOption);
		adapterSyncOption.add(mContext.getString(R.string.msgs_sync_profile_dlg_mirror));
		adapterSyncOption.add(mContext.getString(R.string.msgs_sync_profile_dlg_copy));
		adapterSyncOption.add(mContext.getString(R.string.msgs_sync_profile_dlg_move));
		
		if (prof_syncopt.equals(SMBSYNC_SYNC_TYPE_MIRROR)) spinnerSyncOption.setSelection(0);
		else if (prof_syncopt.equals(SMBSYNC_SYNC_TYPE_COPY)) spinnerSyncOption.setSelection(1);
		else if (prof_syncopt.equals(SMBSYNC_SYNC_TYPE_MOVE)) spinnerSyncOption.setSelection(2);
		
		adapterSyncOption.notifyDataSetChanged();
	};

	public void setSyncMasterProfileSpinner(Spinner spinner_master, String prof_master) {
		final SpinnerAdapterProfileSelection adapter_spinner=new SpinnerAdapterProfileSelection(mContext, R.layout.custom_simple_spinner_item);
		adapter_spinner.setTextColor(Color.BLACK);
		adapter_spinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_master.setPrompt(msgs_select_master_profile);
		spinner_master.setAdapter(adapter_spinner);
		int pos=0,cnt=-1;
		
		for (ProfileListItem pli:mGp.profileAdapter.getAllItem()) {
			if (pli.getType().equals(SMBSYNC_PROF_TYPE_REMOTE) || 
					pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
				cnt++;
//				Log.v("","master added="+pli.getName());
				adapter_spinner.add(pli.getType()+" "+pli.getName());
				if (prof_master.equals(pli.getName())) pos=cnt;
			}
		}
//		Log.v("","set master master="+prof_master+", pos="+pos);
		spinner_master.setSelection(pos);
		adapter_spinner.notifyDataSetChanged();
	};

	public void setSyncTargetProfileSpinner(Spinner spinner_target, String prof_master, String prof_target) {
		final SpinnerAdapterProfileSelection adapter_spinner=new SpinnerAdapterProfileSelection(mContext, R.layout.custom_simple_spinner_item);
		adapter_spinner.setTextColor(Color.BLACK);
		adapter_spinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner_target.setPrompt(msgs_select_target_profile);
		spinner_target.setAdapter(adapter_spinner);

		if (!prof_master.equals("")) {
			ProfileListItem m_pli=getProfile(SMBSYNC_PROF_GROUP_DEFAULT, prof_master);
			String mst_type="";
			if (m_pli!=null) mst_type=m_pli.getType();
			
			int pos=0, cnt=-1;
			
			for (ProfileListItem pli:mGp.profileAdapter.getAllItem()) {
				if (mst_type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
					if (pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						cnt++;
						adapter_spinner.add(pli.getType()+" "+pli.getName());
						if (prof_target.equals(pli.getName())) pos=cnt;
					}
				} else if (mst_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
					if (pli.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
						cnt++;
						adapter_spinner.add(pli.getType()+" "+pli.getName());
						if (prof_target.equals(pli.getName())) pos=cnt;
					} else {
						if (!prof_master.equals(pli.getName())) {
							String m_path=m_pli.getLocalMountPoint()+"/"+m_pli.getDir();
							String t_path=pli.getLocalMountPoint()+"/"+pli.getDir();
							if (!m_path.equals(t_path)) {
								cnt++;
								adapter_spinner.add(pli.getType()+" "+pli.getName());
								if (prof_target.equals(pli.getName())) pos=cnt;
							}
						}
					}
				}
			}
			spinner_target.setSelection(pos);
		} else {
			if (!prof_target.equals("")) {
				ProfileListItem t_pli=getProfile(SMBSYNC_PROF_GROUP_DEFAULT, prof_target);
				String tgt_type="";
				if (t_pli!=null) tgt_type=t_pli.getType();
				int pos=0, cnt=-1;
				
				for (ProfileListItem pli:mGp.profileAdapter.getAllItem()) {
					if (tgt_type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
						if (pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							cnt++;
							adapter_spinner.add(pli.getType()+" "+pli.getName());
							if (prof_target.equals(pli.getName())) pos=cnt;
						}
					} else if (tgt_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						if (pli.getType().equals(SMBSYNC_PROF_TYPE_REMOTE) || 
								pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							if (!prof_master.equals(pli.getName())) {
								cnt++;
								adapter_spinner.add(pli.getType()+" "+pli.getName());
								if (prof_target.equals(pli.getName())) pos=cnt;
							}
						}
					}
				}
				spinner_target.setSelection(pos);
			} else {
				int pos=0, cnt=-1;
				for (ProfileListItem pli:mGp.profileAdapter.getAllItem()) {
					if (pli.getType().equals(SMBSYNC_PROF_TYPE_REMOTE) || 
							pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						if (!prof_master.equals(pli.getName())) {
							cnt++;
							adapter_spinner.add(pli.getType()+" "+pli.getName());
							if (prof_target.equals(pli.getName())) pos=cnt;
						}
					}
				}
				spinner_target.setSelection(pos);
			}
		}
		adapter_spinner.notifyDataSetChanged();
//		Log.v("","master="+prof_master+", target="+prof_target);
//		Log.v("","set sp_t="+spinner_target.getSelectedItem());
	};

	public void addSyncProfile(boolean add_op, String prof_name, String prof_act,
			String prof_master, String prof_target, String prof_syncopt,
			final ArrayList<String> ff,final ArrayList<String> df,String dialog_msg) {
		final ArrayList<String>prof_file_filter;
		final ArrayList<String>prof_dir_filter;
		if (ff==null) prof_file_filter=new ArrayList<String>();
			else  prof_file_filter=ff;
		if (df==null) prof_dir_filter=new ArrayList<String>();
			else prof_dir_filter=df;

		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.edit_profile_sync);
		final TextView dlg_title=(TextView) dialog.findViewById(R.id.sync_profile_dlg_title);
		if (add_op) dlg_title.setText(msgs_add_sync_profile);
		else dlg_title.setText(msgs_copy_sync_profile);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.sync_profile_dlg_msg);
		dlg_msg.setText(dialog_msg);
		final TextView dlg_file_filter=(TextView) dialog.findViewById(R.id.sync_profile_file_filter);
		dlg_file_filter.setText(R.string.msgs_filter_list_dlg_not_specified);
		final TextView dlg_dir_filter=(TextView) dialog.findViewById(R.id.sync_profile_dir_filter);
		dlg_dir_filter.setText(R.string.msgs_filter_list_dlg_not_specified);
		final EditText editname = (EditText)dialog.findViewById(R.id.sync_profile_name);
		editname.setText(prof_name);

		final Spinner spinnerSyncOption=(Spinner)dialog.findViewById(R.id.sync_profile_sync_option);
		setSyncOptionSpinner(spinnerSyncOption, prof_syncopt); 

		final CheckBox cbmpd = (CheckBox)dialog.findViewById(R.id.sync_profile_master_dir_cb);
		cbmpd.setChecked(true);
//		cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_enable));
//		cbmpd.setOnCheckedChangeListener(new OnCheckedChangeListener(){
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView,
//					boolean isChecked) {
//				if (isChecked) cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_enable));
//				else cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_disable));
//			}
//		});
		if (prof_dir_filter.size()!=0) cbmpd.setEnabled(true);
		else cbmpd.setEnabled(false);
		
		final CheckBox cbConf = (CheckBox)dialog.findViewById(R.id.sync_profile_confirm);
		cbConf.setChecked(true);
		final CheckBox cbLastMod = (CheckBox)dialog.findViewById(R.id.sync_profile_last_modified);
		cbLastMod.setChecked(false);
		final CheckBox cbNotUseLastModRem = (CheckBox)dialog.findViewById(R.id.sync_profile_not_use_last_modified_remote_file_for_diff);
		cbNotUseLastModRem.setChecked(false);
		CommonDialog.setDlgBoxSizeCompact(dialog);

		final CheckBox tg = (CheckBox)dialog.findViewById(R.id.sync_profile_active);
		if (prof_act.equals(SMBSYNC_PROF_ACTIVE)) tg.setChecked(true);
			else tg.setChecked(false);

		
		final Spinner spinner_master=(Spinner)dialog.findViewById(R.id.sync_profile_master_spinner);
		final Spinner spinner_target=(Spinner)dialog.findViewById(R.id.sync_profile_target_spinner);
		setSyncMasterProfileSpinner(spinner_master,"");
		if (spinner_master.getCount()>0) {
			setSyncTargetProfileSpinner(spinner_target,spinner_master.getSelectedItem().toString().substring(2),"");
		} else {
			setSyncTargetProfileSpinner(spinner_target,"","");
		}
		
		final Button swap_master_target = (Button)dialog.findViewById(R.id.sync_profile_edit_change_master_and_target);
		if (spinner_master.getCount()>0 && spinner_target.getCount()>0) swap_master_target.setEnabled(true);
		else swap_master_target.setEnabled(false);
		swap_master_target .setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String mst=spinner_target.getSelectedItem().toString().substring(2);
				String tgt=spinner_master.getSelectedItem().toString().substring(2);;
				setSyncMasterProfileSpinner(spinner_master,mst);
				setSyncTargetProfileSpinner(spinner_target,mst,tgt);
				setMasterProfileEditButtonListener(dialog, mst);
				setTargetProfileEditButtonListener(dialog, tgt);
			}
		});

//		Log.v("","add main sp_m="+spinner_master.getSelectedItem()+", sp_t="+spinner_target.getSelectedItem());
		
		final ImageButton ib_edit_master = (ImageButton)dialog.findViewById(R.id.sync_profile_edit_master);
		final ImageButton ib_edit_target = (ImageButton)dialog.findViewById(R.id.sync_profile_edit_target);
		
		spinner_master.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String c_mst="", c_tgt="";
				if (spinner_target.getSelectedItem()!=null) 
					c_tgt=spinner_target.getSelectedItem().toString().substring(2);
				if (spinner_master.getSelectedItem()!=null) 
					c_mst=spinner_master.getSelectedItem().toString().substring(2);
				setSyncTargetProfileSpinner(spinner_target,c_mst,c_tgt);
//				Log.v("","c_mst="+c_mst+", c_tgt="+c_tgt);
				setMasterProfileEditButtonListener(dialog, c_mst);
				setTargetProfileEditButtonListener(dialog, c_tgt);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		ib_edit_master.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (spinner_master.getCount()>0) {
					String m_name=spinner_master.getSelectedItem().toString().substring(2);
					int num=-1;
					ProfileListItem m_pli=getProfile(SMBSYNC_PROF_GROUP_DEFAULT,m_name);
					if (m_pli!=null) {
						for (int i=0;i<mGp.profileAdapter.getCount();i++) {
							if (mGp.profileAdapter.getItem(i).getName().equals(m_name)) {
								num=i;
								break;
							}
						}
						if (m_pli.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
							editProfileRemote(m_name, SMBSYNC_PROF_TYPE_REMOTE,
									num, m_pli.getActive(), 
									m_pli.getAddr(), m_pli.getUser(), m_pli.getPass(),
									m_pli.getShare(), m_pli.getDir(),m_pli.getHostname(),
									m_pli.getPort(), "");
						} else if (m_pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							editProfileLocal(m_name, SMBSYNC_PROF_TYPE_LOCAL,
									num, m_pli.getActive(), m_pli.getLocalMountPoint(), 
									m_pli.getDir(), "");
						}
					}
				}
			}
		});

		ib_edit_target.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (spinner_target.getCount()>0) {
					String t_name=spinner_target.getSelectedItem().toString().substring(2);;
					int num=-1;
					ProfileListItem m_pli=getProfile(SMBSYNC_PROF_GROUP_DEFAULT, t_name);
					if (m_pli!=null) {
						for (int i=0;i<mGp.profileAdapter.getCount();i++) {
							if (mGp.profileAdapter.getItem(i).getName().equals(t_name)) {
								num=i;
								break;
							}
						}
						if (m_pli.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
							editProfileRemote(t_name, m_pli.getType(),
									num, m_pli.getActive(), 
									m_pli.getAddr(), m_pli.getUser(), m_pli.getPass(),
									m_pli.getShare(), m_pli.getDir(),m_pli.getHostname(),
									m_pli.getPort(), "");
						} else if (m_pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							editProfileLocal(t_name, SMBSYNC_PROF_TYPE_LOCAL,
									num, m_pli.getActive(), m_pli.getLocalMountPoint(), 
									m_pli.getDir(), "");
						}
					}
				}
			}
		});

		final Button btn_ok = (Button) dialog.findViewById(R.id.sync_profile_ok);
		btn_ok.setEnabled(false);
		dlg_msg.setText(msgs_audit_msgs_profilename2);
		editname.addTextChangedListener(new TextWatcher(){
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,	int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {}
			@Override
			public void afterTextChanged(Editable s) {
				if (!isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_SYNC, s.toString())) {
					String e_msg=auditSyncProfileField(dialog);
					if (e_msg.length()!=0) {
						dlg_msg.setText(e_msg);
						btn_ok.setEnabled(false);
					} else {
						dlg_msg.setText("");
						btn_ok.setEnabled(true);
					} 
				} else {
					dlg_msg.setText(msgs_duplicate_profile);
					btn_ok.setEnabled(false);
				}
			}
		});
		
		// file filterボタンの指定
		Button file_filter_btn = (Button) dialog.findViewById(R.id.sync_profile_file_filter_btn);
		file_filter_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				invokeEditFileFilterDlg(dialog, prof_file_filter);
			}
		});
		// directory filterボタンの指定
		Button dir_filter_btn = (Button) dialog.findViewById(R.id.sync_profile_dir_filter_btn);
		if (spinner_master.getCount()>0) dir_filter_btn.setEnabled(true);
		else dir_filter_btn.setEnabled(false);
		dir_filter_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				invokeEditDirFilterDlg(dialog, prof_dir_filter);
			}
		});
		
		// CANCELボタンの指定
		final Button btn_cancel = (Button) dialog.findViewById(R.id.sync_profile_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});

		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (saveSyncProfile(dialog, prof_file_filter, prof_dir_filter,0)) 
					dialog.dismiss();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();

	};
	
	private boolean saveSyncProfile(Dialog dialog,
			final ArrayList<String> prof_file_filter, 
			final ArrayList<String> prof_dir_filter,
			int prof_pos) {
		
//		final TextView dlg_title=(TextView) dialog.findViewById(R.id.sync_profile_dlg_title);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.sync_profile_dlg_msg);
//		final TextView dlg_file_filter=(TextView) dialog.findViewById(R.id.sync_profile_file_filter);
//		final TextView dlg_dir_filter=(TextView) dialog.findViewById(R.id.sync_profile_dir_filter);
		final EditText editname = (EditText)dialog.findViewById(R.id.sync_profile_name);
		final Spinner spinnerSyncOption=(Spinner)dialog.findViewById(R.id.sync_profile_sync_option);
		final CheckBox cbmpd = (CheckBox)dialog.findViewById(R.id.sync_profile_master_dir_cb);
		final CheckBox cbConf = (CheckBox)dialog.findViewById(R.id.sync_profile_confirm);
		final CheckBox cbLastMod = (CheckBox)dialog.findViewById(R.id.sync_profile_last_modified);
		final CheckBox cbNotUseLastModRem = (CheckBox)dialog.findViewById(R.id.sync_profile_not_use_last_modified_remote_file_for_diff);
		final CheckBox tg = (CheckBox)dialog.findViewById(R.id.sync_profile_active);
		final Spinner spinner_master=(Spinner)dialog.findViewById(R.id.sync_profile_master_spinner);
		final Spinner spinner_target=(Spinner)dialog.findViewById(R.id.sync_profile_target_spinner);
//		final Button swap_master_target = (Button)dialog.findViewById(R.id.sync_profile_edit_change_master_and_target);
//		final ImageButton ib_edit_master = (ImageButton)dialog.findViewById(R.id.sync_profile_edit_master);
//		final ImageButton ib_edit_target = (ImageButton)dialog.findViewById(R.id.sync_profile_edit_target);
//		final Button btn_ok = (Button) dialog.findViewById(R.id.sync_profile_ok);
//		Button file_filter_btn = (Button) dialog.findViewById(R.id.sync_profile_file_filter_btn);
//		Button dir_filter_btn = (Button) dialog.findViewById(R.id.sync_profile_dir_filter_btn);
//		final Button btn_cancel = (Button) dialog.findViewById(R.id.sync_profile_cancel);
		
		String prof_name, prof_act, prof_master, prof_target, prof_syncopt = null;
		boolean profile_saved = true;
		String audit_msg="";
		
		prof_master = spinner_master.getSelectedItem().toString().substring(2);
		prof_target = spinner_target.getSelectedItem().toString().substring(2);
		prof_name = editname.getText().toString();

		String e_msg=auditSyncProfileField(dialog);
		if (e_msg.length()!=0) {
			audit_msg=e_msg;
			profile_saved=false;
		} else if (e_msg.length()==0 && prof_master.equals(prof_target)) {
				profile_saved=false;
				audit_msg=msgs_audit_msgs_master_target;
		} 

		if (tg.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
			else prof_act = SMBSYNC_PROF_INACTIVE;

		int syncopt=spinnerSyncOption.getSelectedItemPosition();
		if (syncopt==0) prof_syncopt = SMBSYNC_SYNC_TYPE_MIRROR;
		else if (syncopt==1) prof_syncopt = SMBSYNC_SYNC_TYPE_COPY;
		else if (syncopt==2) prof_syncopt = SMBSYNC_SYNC_TYPE_MOVE;
		else prof_syncopt = SMBSYNC_SYNC_TYPE_COPY;

		Boolean prof_mpd=true;
		if (!cbmpd.isChecked())prof_mpd=false; 

		if (!profile_saved) {
			dlg_msg.setText(audit_msg);
		} else {
			if (mGp.profileAdapter.getItem(0).getType().equals(""))
				mGp.profileAdapter.remove(0);
			String m_typ=getProfileType(prof_master,mGp.profileAdapter);
			String t_typ=getProfileType(prof_target,mGp.profileAdapter);
			updateSyncProfileAdapter(prof_name, prof_act,
					prof_syncopt, m_typ,prof_master, t_typ,prof_target,
					prof_file_filter,prof_dir_filter,prof_mpd,
					cbConf.isChecked(),cbLastMod.isChecked(),
					cbNotUseLastModRem.isChecked(),false,prof_pos);
			mGp.profileAdapter.sort();
			mGp.profileAdapter.notifyDataSetChanged();
			saveProfileToFile(false,"","",mGp.profileAdapter,false);
		}
		return profile_saved;
	};
	
	public void editProfileLocal(String prof_name, final String prof_type,
			final int prof_num, String prof_act, 
			final String prof_lmp, String prof_dir, String dialog_msg) {

		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		dialog.setContentView(R.layout.edit_profile_local);
		final TextView dlg_title=(TextView) dialog.findViewById(R.id.local_profile_dlg_title);
		dlg_title.setText(msgs_edit_local_profile);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.local_profile_dlg_msg);
		dlg_msg.setText(dialog_msg);
		final EditText editdir = (EditText) dialog.findViewById(R.id.local_profile_dir);
		editdir.setText(prof_dir);
		final EditText editname = (EditText) dialog.findViewById(R.id.local_profile_name);
		editname.setText(prof_name);
		editname.setTextColor(Color.LTGRAY);
		editname.setEnabled(false);
		final CheckBox tg = (CheckBox) dialog.findViewById(R.id.local_profile_active);
		if (prof_act.equals(SMBSYNC_PROF_ACTIVE)) tg.setChecked(true);
			else tg.setChecked(false);
		
		final Spinner spinner=
				(Spinner) dialog.findViewById(R.id.local_profile_lmp_btn);
		spinner.setVisibility(Spinner.VISIBLE);
		
		setLocalMountPointSpinner(spinner, prof_lmp);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		
		// GET_btn1ボタンの指定
		Button btnGet1 = (Button) dialog.findViewById(R.id.local_profile_get_btn1);
		if (!mGp.externalStorageIsMounted) btnGet1.setEnabled(false);
		btnGet1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
//				String url=SMBSync_External_Root_Dir;
				String url=(String)spinner.getSelectedItem();
				String p_dir=editdir.getText().toString();
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						editdir.setText((String)arg1[0]);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText("");
					}
					
				});
				selectLocalDirDlg(url,"",p_dir,ntfy);
			}
		});
		
		// CANCELボタンの指定
		final Button btn_cancel = (Button) dialog.findViewById(R.id.local_profile_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX, currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		Button btn_ok = (Button) dialog.findViewById(R.id.local_profile_ok);
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String prof_name, prof_dir, prof_act, prof_lmp;
				boolean audit_error = false;
				String audit_msg="";
				prof_lmp=(String)spinner.getSelectedItem();
				prof_dir = editdir.getText().toString();
				prof_name = editname.getText().toString();

				if (hasInvalidChar(prof_dir,new String[]{"\t"})) {
					audit_error=true;
					prof_dir=removeInvalidChar(prof_dir);
					audit_msg=String.format(msgs_audit_msgs_local_dir,detectedInvalidCharMsg);
					editdir.setText(prof_dir);
				}
				if (!audit_error) {
					if (hasInvalidChar(prof_name,new String[]{"\t"})) {
						audit_error=true;
						prof_name=removeInvalidChar(prof_name);
						audit_msg=String.format(msgs_audit_msgs_profilename1,detectedInvalidCharMsg);
						editname.setText(prof_name);
					}  else if (prof_name.length()==0) {
						audit_error=true;
						audit_msg=msgs_audit_msgs_sync1;
					}
				}
				if (tg.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
					else prof_act = SMBSYNC_PROF_INACTIVE;

				if (audit_error) {
					((TextView) dialog.findViewById(R.id.local_profile_dlg_msg))
						.setText(audit_msg);
					return;
				} else {
					ProfileListItem item = mGp.profileAdapter.getItem(prof_num);
					boolean lmp_changed=false;
					if (!LocalFileLastModified.isSetLastModifiedFunctional(prof_lmp)) {
						if (!item.getLocalMountPoint().equals(prof_lmp)) lmp_changed=true;
					}
					if (prof_name.equals(item.getName()) ||
							(!prof_name.equals(item.getName()) &&
							 !isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,
									 SMBSYNC_PROF_TYPE_LOCAL,prof_name))) {
						final String t_prof_name=prof_name;
						final String t_prof_act=prof_act;
						final String t_prof_lmp=prof_lmp;
						final String t_prof_dir=prof_dir;
						NotifyEvent ntfy=new NotifyEvent(null);
						ntfy.setListener(new NotifyEventListener(){
							@Override
							public void positiveResponse(Context c,Object[] o) {
								dialog.dismiss();
								updateLocalProfileAdapter(false,t_prof_name,t_prof_act,
										t_prof_lmp, t_prof_dir,false,prof_num);
								resolveSyncProfileRelation();
								saveProfileToFile(false,"","",mGp.profileAdapter,false);
								mGp.profileAdapter.notifyDataSetChanged();
//								AdapterProfileList tfl= createProfileList(false,"");
//								replaceglblParms.profileAdapterContent(tfl);
							}
							@Override
							public void negativeResponse(Context c, Object[] o) {}
						});
						if (lmp_changed) {
							commonDlg.showCommonDialog(true, "W", 
									mContext.getString(R.string.msgs_local_profile_dlg_local_mount_changed_confirm_title),
									mContext.getString(R.string.msgs_local_profile_dlg_local_mount_changed_confirm_msg),
									ntfy);
						} else {
							ntfy.notifyToListener(true, null);
						}
					} else {
						((TextView) dialog.findViewById(R.id.local_profile_dlg_msg))
						.setText(msgs_duplicate_profile);
					}
				}
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
	};

	public void editProfileRemote(String prof_name, final String prof_type,
			final int prof_num, String prof_act, 
			String prof_addr, String prof_user, String prof_pass,
			String prof_share, String prof_dir,String prof_host, 
			String prof_port, String dialog_msg) {
		
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		dialog.setContentView(R.layout.edit_profile_remote);
		final TextView dlg_title=(TextView) dialog.findViewById(R.id.remote_profile_dlg_title);
		dlg_title.setText(msgs_edit_remote_profile);
		
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.remote_profile_dlg_msg);
		dlg_msg.setText(dialog_msg);

		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
		if (prof_addr.length()!=0) editaddr.setText(prof_addr);
		final EditText edithost = (EditText) dialog.findViewById(R.id.remote_profile_hostname);
		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
		if (prof_user.length()!=0) edituser.setText(prof_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
		if (prof_pass.length()!=0) editpass.setText(prof_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);
		if (prof_share.length()!=0) editshare.setText(prof_share);
		final EditText editdir = (EditText) dialog.findViewById(R.id.remote_profile_dir);
		if (prof_dir.length()!=0) editdir.setText(prof_dir);
		final EditText editname = (EditText) dialog.findViewById(R.id.remote_profile_name);
		editname.setText(prof_name);
		editname.setTextColor(Color.LTGRAY);
		editname.setEnabled(false);
		
		final CheckBox cb_use_hostname = (CheckBox) dialog.findViewById(R.id.remote_profile_use_computer_name);
		final CheckBox cb_use_user_pass = (CheckBox) dialog.findViewById(R.id.remote_profile_use_user_pass);
		
		if (prof_host.equals("")) {
			cb_use_hostname.setChecked(false);
			editaddr.setVisibility(EditText.VISIBLE);
			edithost.setVisibility(EditText.GONE);
		} else {
			cb_use_hostname.setChecked(true);
			editaddr.setVisibility(EditText.GONE);
			edithost.setVisibility(EditText.VISIBLE);
		}
		editaddr.setText(prof_addr);
		edithost.setText(prof_host);
		
		if (prof_user.equals("") && prof_pass.equals("")) {
			cb_use_user_pass.setChecked(false);
			edituser.setEnabled(false);
			editpass.setEnabled(false);
		} else {
			cb_use_user_pass.setChecked(true);
			edituser.setEnabled(true);
			editpass.setEnabled(true);
		}
		
		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.remote_profile_use_port_number);
		final EditText editport = (EditText) dialog.findViewById(R.id.remote_profile_port);
		if (!prof_port.equals("")) {
			cb_use_port_number.setChecked(true);
			editport.setText(prof_port);
		} else {
			editport.setText("");
			editport.setEnabled(false);
		}
		
		final Button btnLogon = (Button) dialog.findViewById(R.id.remote_profile_logon);
		btnLogon.setEnabled(cb_use_user_pass.isChecked());
		btnLogon.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String user=null, pass=null;
				if (cb_use_user_pass.isChecked()) {
					if (edituser.getText().length()>0) user=edituser.getText().toString();
					if (editpass.getText().length()>0) pass=editpass.getText().toString();
				}
				String port="";
				if (cb_use_port_number.isChecked()) port=editport.getText().toString();
				if (cb_use_hostname.isChecked()) {
					logonToRemoteDlg(edithost.getText().toString(),
							"", port, user,pass,null);
				} else {
					String t_addr=editaddr.getText().toString();
					String s_addr=t_addr;
					if (t_addr.indexOf(":")>=0) s_addr=t_addr.substring(0,t_addr.indexOf(":")) ;
					logonToRemoteDlg("",s_addr, port, user,pass,null);
				}
			}
		});

		CommonDialog.setDlgBoxSizeCompact(dialog);

		final Button btn_ok = (Button) dialog.findViewById(R.id.remote_profile_ok);
		setRemoteProfileCommonListener(dialog);
		
		final CheckBox tg = (CheckBox) dialog.findViewById(R.id.remote_profile_active);
		if (prof_act.equals(SMBSYNC_PROF_ACTIVE)) tg.setChecked(true);
			else tg.setChecked(false);
		
		// addressボタンの指定
		Button btnAddr = (Button) dialog.findViewById(R.id.remote_profile_get_addr_btn);
		if (util.isRemoteDisable()) btnAddr.setEnabled(false);
		btnAddr.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ipAddressScanButtonDlg(dialog);
			}
		});
		
		// RemoteShareボタンの指定
		Button btnGet1 = (Button) dialog.findViewById(R.id.remote_profile_get_btn1);
		if (util.isRemoteDisable()) btnGet1.setEnabled(false);
		btnGet1.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				invokeSelectRemoteShareDlg(dialog);
			}
		});
		// RemoteDirectoryボタンの指定
		final Button btnGet2 = (Button) dialog.findViewById(R.id.remote_profile_get_btn2);
		if (util.isRemoteDisable()) btnGet2.setEnabled(false);
		btnGet2.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				selectRemoteDirectory(dialog);
			}
		});

		// CANCELボタンの指定
		final Button btn_cancel = (Button) dialog.findViewById(R.id.remote_profile_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String prof_name, remote_user, remote_pass, remote_share, 
				prof_dir, prof_act = null;
				String remote_host="",remote_addr="";

				remote_host=edithost.getText().toString();
				remote_addr=editaddr.getText().toString();
				
				remote_user = edituser.getText().toString();
				remote_pass = editpass.getText().toString();
				remote_share = editshare.getText().toString();
				prof_dir = editdir.getText().toString();
				prof_name = editname.getText().toString();
				
				if (!cb_use_user_pass.isChecked()) {
					remote_user="";
					remote_pass="";
				}
				
				if (tg.isChecked()) prof_act = SMBSYNC_PROF_ACTIVE;
					else prof_act = SMBSYNC_PROF_INACTIVE;
//				String e_msg=auditRemoteProfileField(dialog);
//				if (e_msg.length()!=0) {
//					((TextView) dialog.findViewById(R.id.remote_profile_dlg_msg))
//					.setText(e_msg);
//					return;
//				} else {
					dialog.dismiss();
					ProfileListItem item = mGp.profileAdapter.getItem(prof_num);
					if (prof_name.equals(item.getName())||
							(!prof_name.equals(item.getName()) &&
							 !isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,
									 SMBSYNC_PROF_TYPE_REMOTE, prof_name))) {
						dialog.dismiss();
						if (cb_use_hostname.isChecked()) remote_addr="";
						else remote_host="";
						String remote_port="";
						if (cb_use_port_number.isChecked()) 
							remote_port=editport.getText().toString();
//						int pos=glblParms.profileListView.getFirstVisiblePosition();
//						int posTop=glblParms.profileListView.getChildAt(0).getTop();
						updateRemoteProfileAdapter(false,prof_name,prof_act,prof_dir,
								remote_user, remote_pass,remote_share,
								remote_addr,remote_host,remote_port,
								false,prof_num);
						resolveSyncProfileRelation();
						saveProfileToFile(false,"","",mGp.profileAdapter,false);
						mGp.profileAdapter.notifyDataSetChanged();
//						AdapterProfileList tfl= createProfileList(false,"");
//						replaceglblParms.profileAdapterContent(tfl);
//						glblParms.profileListView.setSelectionFromTop(pos,posTop);
					} else {
						((TextView) dialog.findViewById(R.id.remote_profile_dlg_msg))
						.setText(msgs_duplicate_profile);
					}
//				}
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
	};
	
	public void editProfileSync(String prof_name, final String prof_type,
				final int prof_num, String prof_act, 
				final String prof_master, final String prof_target, String prof_syncopt,
				ArrayList<String> prof_file_filter, 
				ArrayList<String> prof_dir_filter,boolean prof_mpd, 
				boolean prof_conf, boolean prof_ujlm, boolean nulm_remote, String dialog_msg) {
	
		String f_fl="", d_fl="";
		if (prof_file_filter!=null) {
			String cn="";
			for (int i=0;i<prof_file_filter.size();i++) {
				f_fl+=cn+prof_file_filter.get(i).substring(1,prof_file_filter.get(i).length());
				cn=",";
			}
		} 
		if (f_fl.length()==0) f_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
		if (prof_dir_filter!=null) {
			String cn="";
			for (int i=0;i<prof_dir_filter.size();i++) {
				d_fl+=cn+prof_dir_filter.get(i).substring(1,prof_dir_filter.get(i).length());
				cn=",";
			}
		}
		if (d_fl.length()==0)  d_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);

		final ArrayList<String> n_file_filter= new ArrayList<String>();
		n_file_filter.addAll(prof_file_filter);
		final ArrayList<String> n_dir_filter=new ArrayList<String>(); 
		n_dir_filter.addAll(prof_dir_filter);

		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);//,android.R.style.Theme_Light);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		dialog.setContentView(R.layout.edit_profile_sync);
		final TextView dlg_title=(TextView) dialog.findViewById(R.id.sync_profile_dlg_title);
		dlg_title.setText(msgs_edit_sync_profile);
		
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.sync_profile_dlg_msg);
		dlg_msg.setText(dialog_msg);
		final TextView dlg_file_filter=(TextView) dialog.findViewById(R.id.sync_profile_file_filter);
		dlg_file_filter.setText(f_fl);
		final TextView dlg_dir_filter=(TextView) dialog.findViewById(R.id.sync_profile_dir_filter);
		dlg_dir_filter.setText(d_fl);

		final EditText editname = (EditText) dialog.findViewById(R.id.sync_profile_name);
		editname.setText(prof_name);
		editname.setTextColor(Color.LTGRAY);
		editname.setEnabled(false);
		final CheckBox cbmpd = (CheckBox)dialog.findViewById(R.id.sync_profile_master_dir_cb);
		final CheckBox tg = (CheckBox)dialog.findViewById(R.id.sync_profile_active);
		final CheckBox cbConf = (CheckBox)dialog.findViewById(R.id.sync_profile_confirm);
		final CheckBox cbLastMod = (CheckBox)dialog.findViewById(R.id.sync_profile_last_modified);
		cbConf.setChecked(prof_conf);
		cbLastMod.setChecked(prof_ujlm);
		
		final ImageButton ib_edit_master=(ImageButton) dialog.findViewById(R.id.sync_profile_edit_master);
		final ImageButton ib_edit_target=(ImageButton) dialog.findViewById(R.id.sync_profile_edit_target);

		final Spinner spinnerSyncOption=(Spinner)dialog.findViewById(R.id.sync_profile_sync_option);
		setSyncOptionSpinner(spinnerSyncOption, prof_syncopt); 

		CommonDialog.setDlgBoxSizeCompact(dialog);

		if (prof_act.equals(SMBSYNC_PROF_ACTIVE)) tg.setChecked(true);
			else tg.setChecked(false);
		if (prof_dir_filter.size()!=0) {
			cbmpd.setEnabled(true);
			cbmpd.setChecked(true);
		} else cbmpd.setEnabled(false);

		final CheckBox cbNotUseLastModRem = (CheckBox)dialog.findViewById(R.id.sync_profile_not_use_last_modified_remote_file_for_diff);
		cbNotUseLastModRem.setChecked(nulm_remote);

//		if (cbmpd.isChecked()) cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_enable));
//		else cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_disable));
//		cbmpd.setOnCheckedChangeListener(new OnCheckedChangeListener(){
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView,
//					boolean isChecked) {
//				if (isChecked) cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_enable));
//				else cbmpd.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_disable));
//			}
//		});

		final Spinner spinner_master=(Spinner)dialog.findViewById(R.id.sync_profile_master_spinner);
		final Spinner spinner_target=(Spinner)dialog.findViewById(R.id.sync_profile_target_spinner);
		setSyncMasterProfileSpinner(spinner_master,prof_master);
		setSyncTargetProfileSpinner(spinner_target,prof_master,prof_target);
		
		spinner_master.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				String c_mst="", c_tgt="";
				if (spinner_target.getSelectedItem()!=null) 
					c_tgt=spinner_target.getSelectedItem().toString().substring(2);
				if (spinner_master.getSelectedItem()!=null) 
					c_mst=spinner_master.getSelectedItem().toString().substring(2);
				setSyncTargetProfileSpinner(spinner_target,c_mst,c_tgt);
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});

		setMasterProfileEditButtonListener(dialog, prof_master);
		setTargetProfileEditButtonListener(dialog, prof_target);
		
		ib_edit_master.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String m_name=spinner_master.getSelectedItem().toString().substring(2);
				int num=-1;
				ProfileListItem m_pli=getProfile(SMBSYNC_PROF_GROUP_DEFAULT,m_name);
				if (m_pli!=null) {
					for (int i=0;i<mGp.profileAdapter.getCount();i++) {
						if (mGp.profileAdapter.getItem(i).getName().equals(m_name)) {
							num=i;
							break;
						}
					}
					if (m_pli.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
						editProfileRemote(m_name, m_pli.getType(),
								num, m_pli.getActive(), 
								m_pli.getAddr(), m_pli.getUser(), m_pli.getPass(),
								m_pli.getShare(), m_pli.getDir(),m_pli.getHostname(),
								m_pli.getPort(), "");
					} else if (m_pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						editProfileLocal(m_name, prof_type,
								num, m_pli.getActive(), m_pli.getLocalMountPoint(), 
								m_pli.getDir(), "");
					}
				}
			}
			
		});

		final Button swap_master_target = (Button)dialog.findViewById(R.id.sync_profile_edit_change_master_and_target);
		if (spinner_master.getCount()>0 && spinner_target.getCount()>0) swap_master_target.setEnabled(true);
		else swap_master_target.setEnabled(false);
		swap_master_target .setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String mst=spinner_target.getSelectedItem().toString().substring(2);
				String tgt=spinner_master.getSelectedItem().toString().substring(2);;
				setSyncMasterProfileSpinner(spinner_master,mst);
				setSyncTargetProfileSpinner(spinner_target,mst,tgt);
				setMasterProfileEditButtonListener(dialog, mst);
				setTargetProfileEditButtonListener(dialog, tgt);
			}
		});

		ib_edit_target.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				String t_name=spinner_target.getSelectedItem().toString().substring(2);;
				int num=-1;
				ProfileListItem m_pli=getProfile(SMBSYNC_PROF_GROUP_DEFAULT, t_name);
				if (m_pli!=null) {
					for (int i=0;i<mGp.profileAdapter.getCount();i++) {
						if (mGp.profileAdapter.getItem(i).getName().equals(t_name)) {
							num=i;
							break;
						}
					}
					if (m_pli.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
						editProfileRemote(t_name, m_pli.getType(),
								num, m_pli.getActive(), 
								m_pli.getAddr(), m_pli.getUser(), m_pli.getPass(),
								m_pli.getShare(), m_pli.getDir(),m_pli.getHostname(), 
								m_pli.getPort(), "");
					} else if (m_pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
						editProfileLocal(t_name, prof_type,
								num, m_pli.getActive(), m_pli.getLocalMountPoint(), 
								m_pli.getDir(), "");
					}
				}
			}
			
		});

		
		// file filterボタンの指定
		Button file_filter_btn = (Button) dialog.findViewById(R.id.sync_profile_file_filter_btn);
		file_filter_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				invokeEditFileFilterDlg(dialog, n_file_filter);
			}
		});
		// directory filterボタンの指定
		Button dir_filter_btn = (Button) dialog.findViewById(R.id.sync_profile_dir_filter_btn);
		if (spinner_master.getCount()>0) dir_filter_btn.setEnabled(true);
		else dir_filter_btn.setEnabled(false);
		dir_filter_btn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				invokeEditDirFilterDlg(dialog, n_dir_filter);
			}
		});
		// Master Dir processボタンの指定
		if (prof_dir_filter.size()!=0) cbmpd.setEnabled(true);
			else cbmpd.setEnabled(false);
		if (prof_mpd) cbmpd.setChecked(true);
			else cbmpd.setChecked(false);

		// CANCELボタンの指定
		final Button btn_cancel = (Button) dialog.findViewById(R.id.sync_profile_cancel);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		Button btn_ok = (Button) dialog.findViewById(R.id.sync_profile_ok);
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				if (saveSyncProfile(dialog, n_file_filter, n_dir_filter,prof_num)) 
					dialog.dismiss();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
	
	};

	public void copyProfile(ProfileListItem pli) {
		if (pli.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
			addLocalProfile(false,pli.getName(),pli.getActive(),
					pli.getLocalMountPoint(),pli.getDir(),"");
		} else if (pli.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
			addRemoteProfile(false, pli.getName(),pli.getActive(),
					pli.getAddr(),pli.getUser(),pli.getPass(),
					pli.getShare(),pli.getDir(),pli.getHostname(),
					pli.getPort(), "");
		} else if (pli.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
			addSyncProfile(false, pli.getName(),pli.getActive(),
					pli.getMasterName(),pli.getTargetName(),
					pli.getSyncType(),pli.getFileFilter(),pli.getDirFilter(),"");
		}
	};
	
	public void renameProfile(final ProfileListItem pli) {
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.single_item_input_dlg);
		final TextView dlg_title = (TextView) dialog.findViewById(R.id.single_item_input_title);		
//		final TextView dlg_msg = (TextView) dialog.findViewById(R.id.single_item_input_msg);
		final TextView dlg_cmp = (TextView) dialog.findViewById(R.id.single_item_input_name);
		final Button btn_ok = (Button) dialog.findViewById(R.id.single_item_input_ok_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.single_item_input_cancel_btn);
		final EditText etInput=(EditText) dialog.findViewById(R.id.single_item_input_dir);
		
		dlg_title.setText(mContext.getString(R.string.msgs_rename_profile));
		
		dlg_cmp.setVisibility(TextView.GONE);
		CommonDialog.setDlgBoxSizeCompact(dialog);
		etInput.setText(pli.getName());
		btn_ok.setEnabled(false);
		etInput.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				if (!arg0.equals(pli.getName())) btn_ok.setEnabled(true);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});
		
		//OK button
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				String new_name=etInput.getText().toString();
//				int pos=glblParms.profileListView.getFirstVisiblePosition();
//				int posTop=glblParms.profileListView.getChildAt(0).getTop();
				if (!pli.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
					for (int i=0;i<mGp.profileAdapter.getCount();i++) {
						ProfileListItem tpli=mGp.profileAdapter.getItem(i);
						boolean update_required=false;
						if (tpli.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
							if (tpli.getMasterName().equals(pli.getName())) {
								tpli.setMasterName(new_name);
								update_required=true;
							}
							if (tpli.getTargetName().equals(pli.getName())) {
								tpli.setTargetName(new_name);
								update_required=true;
							}
							if (update_required) mGp.profileAdapter.replace(tpli, i);
						}
					}
				}
				mGp.profileAdapter.remove(pli);
				pli.setName(new_name);
				mGp.profileAdapter.add(pli);

				resolveSyncProfileRelation();

				mGp.profileAdapter.sort();
				mGp.profileAdapter.notifyDataSetChanged();

				saveProfileToFile(false,"","",mGp.profileAdapter,false);
//				AdapterProfileList tfl= createProfileList(false,"");
//				replaceglblParms.profileAdapterContent(tfl);
//				glblParms.profileAdapter.setNotifyOnChange(true);
//				glblParms.profileListView.setSelectionFromTop(pos,posTop);
			}
		});
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setCancelable(false);
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
		dialog.show();

	};

	private void ipAddressScanButtonDlg(Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.remote_profile_dlg_msg);
		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
		final EditText edithost = (EditText) dialog.findViewById(R.id.remote_profile_hostname);
		final CheckBox cb_use_hostname = (CheckBox) dialog.findViewById(R.id.remote_profile_use_computer_name);
		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.remote_profile_use_port_number);
		final EditText editport = (EditText) dialog.findViewById(R.id.remote_profile_port);
		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				if (((String)arg1[0]).equals("A")) {
					cb_use_hostname.setChecked(false);
					editaddr.setText((String)arg1[1]);
				} else {
					cb_use_hostname.setChecked(true);
					edithost.setText((String)arg1[1]);
				}
			}

			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {
				dlg_msg.setText("");
			}
			
		});
		String port_num="";
		if (cb_use_port_number.isChecked()) port_num=editport.getText().toString();
		scanRemoteNetworkDlg(ntfy,port_num,false);
	};

	private void invokeSelectRemoteShareDlg(Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.remote_profile_dlg_msg);

		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);
		final EditText edithost = (EditText) dialog.findViewById(R.id.remote_profile_hostname);
		final CheckBox cb_use_hostname = (CheckBox) dialog.findViewById(R.id.remote_profile_use_computer_name);
		final CheckBox cb_use_userpass = (CheckBox) dialog.findViewById(R.id.remote_profile_use_user_pass);
		final EditText editport = (EditText) dialog.findViewById(R.id.remote_profile_port);
		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.remote_profile_use_port_number);
		String remote_addr, remote_user="", remote_pass="",remote_host;
		
		remote_addr = editaddr.getText().toString();
		remote_host = edithost.getText().toString();
		if (cb_use_userpass.isChecked()) {
			remote_user = edituser.getText().toString();
			remote_pass = editpass.getText().toString();
		}

		if (remote_addr.length()<1 && remote_host.length()<1) { 
			if (cb_use_hostname.isChecked()) dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostname_not_spec));
			else dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostaddr_not_spec));
			return;
		}
		if (hasInvalidChar(remote_pass,new String[]{"\t"})) {
			remote_pass=removeInvalidChar(remote_pass);
			dlg_msg.setText(String.format(msgs_audit_msgs_password1,detectedInvalidCharMsg));
			editpass.setText(remote_pass);
			editpass.requestFocus();
			return;
		}
		if (hasInvalidChar(remote_user,new String[]{"\t"})) {
			remote_user=removeInvalidChar(remote_user);
			dlg_msg.setText(String.format(msgs_audit_msgs_username1,detectedInvalidCharMsg));
			edituser.setText(remote_user);
			edituser.requestFocus();
			return;
		}

		setSmbUserPass(remote_user,remote_pass);
		String t_url="";
		if (cb_use_hostname.isChecked()) t_url=remote_host;
		else t_url=remote_addr;
		String h_port="";
		if (cb_use_port_number.isChecked()) {
			if (editport.getText().length()>0) h_port=":"+editport.getText().toString();
			else {
				dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostport_not_spec));
				return;
			}
		}
		String remurl="smb://"+t_url+h_port+"/";
		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				editshare.setText((String)arg1[0]);
			}

			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {
				if (arg1!=null) dlg_msg.setText((String)arg1[0]);
				else dlg_msg.setText("");
			}
			
		});
		selectRemoteShareDlg(remurl,"", ntfy);
	};

	public void setSmbUserPass(String user, String pass) {
		smbUser=user;
		smbPass=pass;
	};
	
	private void selectRemoteDirectory(Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.remote_profile_dlg_msg);

		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
		final EditText edithost = (EditText) dialog.findViewById(R.id.remote_profile_hostname);
		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);
		final EditText editdir = (EditText) dialog.findViewById(R.id.remote_profile_dir);
		final CheckBox cb_use_hostname = (CheckBox) dialog.findViewById(R.id.remote_profile_use_computer_name);
		final CheckBox cb_use_userpass = (CheckBox) dialog.findViewById(R.id.remote_profile_use_user_pass);
		final EditText editport = (EditText) dialog.findViewById(R.id.remote_profile_port);
		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.remote_profile_use_port_number);
		String remote_addr, remote_user="", remote_pass="",remote_share,remote_host;
		remote_addr = editaddr.getText().toString();
		remote_host = edithost.getText().toString();
		if (cb_use_userpass.isChecked()) {
			remote_user = edituser.getText().toString();
			remote_pass = editpass.getText().toString();
		}

		remote_share = editshare.getText().toString();

		if (remote_addr.length()<1 && remote_host.length()<1) {
			if (cb_use_hostname.isChecked()) dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostname_not_spec));
			else dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostaddr_not_spec));
			return;
		}
		if (remote_share.length()<1) {
			dlg_msg.setText(msgs_audit_share_not_spec);
			return;
		}
		if (hasInvalidChar(remote_pass,new String[]{"\t"})) {
			remote_pass=removeInvalidChar(remote_pass);
			dlg_msg.setText(String.format(msgs_audit_msgs_password1,detectedInvalidCharMsg));
			editpass.setText(remote_pass);
			editpass.requestFocus();
			return;
		}
		if (hasInvalidChar(remote_user,new String[]{"\t"})) {
			remote_user=removeInvalidChar(remote_user);
			dlg_msg.setText(String.format(msgs_audit_msgs_username1,detectedInvalidCharMsg));
			edituser.setText(remote_user);
			edituser.requestFocus();
			return;
		}
		String p_dir = editdir.getText().toString();
		
		setSmbUserPass(remote_user,remote_pass);
		String t_url="";
		if (cb_use_hostname.isChecked()) t_url=remote_host;
		else t_url=remote_addr;
		String h_port="";
		if (cb_use_port_number.isChecked()) {
			if (editport.getText().length()>0) h_port=":"+editport.getText().toString();
			else {
				dlg_msg.setText(mContext.getString(R.string.msgs_audit_hostport_not_spec));
				return;
			}
		}
		String remurl="smb://"+t_url+h_port+"/"+remote_share+"/";
		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				editdir.setText((String)arg1[0]);
			}

			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {
				if (arg1!=null) dlg_msg.setText((String)arg1[0]);
				else dlg_msg.setText("");
			}
			
		});
		setRemoteDir(remurl, "",p_dir, ntfy);
	};
	
	
	private void setMasterProfileEditButtonListener(Dialog dialog, final String prof_name) {
		final ImageButton ib_edit_master=(ImageButton) dialog.findViewById(R.id.sync_profile_edit_master);
		final ProfileListItem pli=getProfile(SMBSYNC_PROF_GROUP_DEFAULT, prof_name);
		if (pli==null) ib_edit_master.setEnabled(false);//setVisibility(ImageButton.GONE);
		else {
			ib_edit_master.setEnabled(true);//.setVisibility(ImageButton.VISIBLE);
			
		}

	};

	private void setTargetProfileEditButtonListener(Dialog dialog, final String prof_name) {
		final ImageButton ib_edit_target=(ImageButton) dialog.findViewById(R.id.sync_profile_edit_target);
		final ProfileListItem pli=getProfile(SMBSYNC_PROF_GROUP_DEFAULT, prof_name);
		if (pli==null) ib_edit_target.setEnabled(false);//.setVisibility(ImageButton.GONE);
		else {
			ib_edit_target.setEnabled(true);//.setVisibility(ImageButton.VISIBLE);
		}

	};

	private ProfileListItem getProfile(String group, String name) {
		ProfileListItem pli=null;
		for (int i=0;i<mGp.profileAdapter.getCount();i++) {
			if (mGp.profileAdapter.getItem(i).getGroup().equals(SMBSYNC_PROF_GROUP_DEFAULT) &&
					mGp.profileAdapter.getItem(i).getName().equals(name)) {
				pli=mGp.profileAdapter.getItem(i);
			}
		}
		return pli;
		
	};
	
	public int getActiveSyncProfileCount(AdapterProfileList pa) {
		int result=0;
		for (int i=0;i<pa.getCount();i++) {
			if (pa.getItem(i).getActive().equals(SMBSYNC_PROF_ACTIVE) && 
					pa.getItem(i).getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
				result++;
			}
		}
		return result;
	}
	
	private void invokeEditFileFilterDlg(Dialog dialog,
			final ArrayList<String> n_file_filter) {
		final TextView dlg_file_filter=(TextView) dialog.findViewById(R.id.sync_profile_file_filter);

		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				String f_fl="";
				if (n_file_filter!=null) {
					String cn="";
					for (int i=0;i<n_file_filter.size();i++) {
						f_fl+=cn+n_file_filter.get(i).substring(1,n_file_filter.get(i).length());
						cn=",";
					}
				}
				if (f_fl.length()==0) f_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
				dlg_file_filter.setText(f_fl);
			}

			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {}
			
		});
		editFileFilterDlg(n_file_filter,ntfy);

	};
	
	private void invokeEditDirFilterDlg(Dialog dialog,
			final ArrayList<String> n_dir_filter) {
		final CheckBox cbmpd = (CheckBox)dialog.findViewById(R.id.sync_profile_master_dir_cb);
		final TextView dlg_dir_filter=(TextView) dialog.findViewById(R.id.sync_profile_dir_filter);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.sync_profile_dlg_msg);

		NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				String d_fl="";
				if (n_dir_filter!=null) {
					String cn="";
					for (int i=0;i<n_dir_filter.size();i++) {
						d_fl+=cn+n_dir_filter.get(i).substring(1,n_dir_filter.get(i).length());
						cn=",";
					}
				}
				if (d_fl.length()==0)  d_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
				dlg_dir_filter.setText(d_fl);
				if (n_dir_filter.size()!=0) cbmpd.setEnabled(true);
				else cbmpd.setEnabled(false);
			}
			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {}
		});

		Spinner spinner_master=(Spinner)dialog.findViewById(R.id.sync_profile_master_spinner);
//		Spinner spinner_target=(Spinner)dialog.findViewById(R.id.sync_profile_target_spinner);
//		String m_prof_type=spinner_master.getSelectedItem().toString().substring(0,1);
		String m_prof_name=spinner_master.getSelectedItem().toString().substring(2);
		if (getProfileType(m_prof_name,mGp.profileAdapter)
				.equals("")) {
			if (m_prof_name.length()==0) {
				dlg_msg.setText(msgs_audit_msgs_master2);
			} else {
				dlg_msg.setText(String.format(
					mContext.getString(R.string.msgs_filter_list_dlg_master_prof_not_found), m_prof_name));
			}
			return;
		}
		editDirFilterDlg(mGp.profileAdapter,m_prof_name,n_dir_filter,ntfy);
	};
	
	public void setLocalMountPointSpinner(Spinner spinner, String prof_lmp) {
//		Local mount pointの設定
		AdapterLocalMountPoint adapter = 
        		new AdapterLocalMountPoint(mContext,
//        				android.R.layout.simple_spinner_item);
        				R.layout.custom_simple_spinner_item);
        adapter.setTextColor(Color.BLACK);
        adapter.setDropDownViewResource(
        		android.R.layout.simple_spinner_dropdown_item);
        spinner.setPrompt(
        		mContext.getString(R.string.msgs_local_profile_dlg_local_mount_point));
        spinner.setAdapter(adapter);

        int a_no=0;
        ArrayList<String>ml=LocalMountPoint.buildLocalMountPointList();
        if (ml.size()==0) {
        	adapter.add(prof_lmp);
        	spinner.setEnabled(false);
        } else {
        	spinner.setEnabled(true);
        	boolean found=false;
	        for (int i=0;i<ml.size();i++) { 
					adapter.add(ml.get(i));
					if (ml.get(i).equals(prof_lmp)) {
				        spinner.setSelection(a_no);
				        found=true;
					}
					a_no++;
			}
	        if (!found) {
	        	adapter.add(prof_lmp);
	        	spinner.setSelection(a_no+1);
	        }
        }
	};
	
	public void setSyncDialogIcon(String prof, ImageView iv) {
		if (prof==null || prof.length()==0) {
			iv.setImageResource(R.drawable.blank);
		} else {
			if (getProfileType(prof,mGp.profileAdapter).equals("R")) {
				iv.setImageResource(R.drawable.ic_32_server);
			} else {
				iv.setImageResource(R.drawable.ic_32_mobile);
			}
		}
	};
	
	public void editFileFilterDlg(final ArrayList<String>file_filter, final NotifyEvent p_ntfy) {
		ArrayList<FilterListItem> filterList=new ArrayList<FilterListItem>() ;
		final AdapterFilterList filterAdapter;
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.filter_list_dlg);
		
		Button dirbtn=(Button) dialog.findViewById(R.id.filter_select_edit_dir_btn);
		dirbtn.setVisibility(Button.GONE);

		filterAdapter = new AdapterFilterList(mContext,
				R.layout.filter_list_item_view,filterList);
		ListView lv=(ListView) dialog.findViewById(R.id.filter_select_edit_listview);
		
		for (int i=0; i<file_filter.size();i++) {
			String inc=file_filter.get(i).substring(0,1);
			String filter=file_filter.get(i).substring(1,file_filter.get(i).length());
			boolean b_inc=false;
			if (inc.equals(SMBSYNC_PROF_FILTER_INCLUDE)) b_inc=true;
			filterAdapter.add(new FilterListItem(filter,b_inc) );
		}
		if (filterAdapter.getCount()==0) filterAdapter.add(
				new FilterListItem(mContext.getString(R.string.msgs_filter_list_no_filter),false) );
		lv.setAdapter(filterAdapter);
//		filterAdapter.getFileFilter().filter("D");
//		lv.setTextFilterEnabled(false);
//		lv.setDivider(new ColorDrawable(Color.WHITE));
		final TextView dlg_title=(TextView) dialog.findViewById(R.id.filter_select_edit_title);
		dlg_title.setText(mContext.getString(R.string.msgs_filter_list_dlg_file_filter));
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.filter_select_edit_msg);
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
//		CommonDialog.setDlgBoxSizeCompact(dialog);

		final EditText et_filter=(EditText)dialog.findViewById(R.id.filter_select_edit_new_filter);
		final Button addBtn = (Button) dialog.findViewById(R.id.filter_select_edit_add_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_select_edit_cancel_btn);
		final Button btn_ok = (Button) dialog.findViewById(R.id.filter_select_edit_ok_btn);
		
        lv.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
        		FilterListItem fli = filterAdapter.getItem(idx);
        		if (fli.getFilter().startsWith("---") || fli.isDeleted()) return;
                // リストアイテムを選択したときの処理
        		editDirFilter(idx,filterAdapter,fli,fli.getFilter());
            }
        });	 

		// Addボタンの指定
		addBtn.setEnabled(false);
		et_filter.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length()!=0) {
					if (isFilterExists(s.toString().trim(),filterAdapter)) {
						String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
						dlg_msg.setText(String.format(mtxt, s.toString().trim()));
						addBtn.setEnabled(false);
						btn_ok.setEnabled(true);
					} else {
						dlg_msg.setText("");
						addBtn.setEnabled(true);
						btn_ok.setEnabled(false);
					}
				} else {
					addBtn.setEnabled(false);
					btn_ok.setEnabled(true);
				}
//				et_filter.setText(s);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});
		addBtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dlg_msg.setText("");
				String newfilter=et_filter.getText().toString().trim();
				et_filter.setText("");
				if (filterAdapter.getItem(0).getFilter().startsWith("---"))
						filterAdapter.remove(filterAdapter.getItem(0));
				filterAdapter.add(new FilterListItem(newfilter,true) );
				filterAdapter.setNotifyOnChange(true);
				filterAdapter.sort(new Comparator<FilterListItem>() {
					@Override
					public int compare(FilterListItem lhs,
							FilterListItem rhs) {
						return lhs.getFilter().compareToIgnoreCase(rhs.getFilter());
					};
				});
				btn_ok.setEnabled(true);
			}
		});

		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				file_filter.clear();
				if (filterAdapter.getCount()>0) {
					for (int i=0;i<filterAdapter.getCount();i++) {
						if (!filterAdapter.getItem(i).isDeleted() &&
								!filterAdapter.getItem(i).getFilter().startsWith("---")) {
							String inc=SMBSYNC_PROF_FILTER_EXCLUDE;
							if (filterAdapter.getItem(i).getInc()) inc=SMBSYNC_PROF_FILTER_INCLUDE;
							file_filter.add(inc+filterAdapter.getItem(i).getFilter());
						}
							
					}
				}
				p_ntfy.notifyToListener(true, null);
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
		
	};
	
	public void editDirFilterDlg(final AdapterProfileList prof_dapter,
			final String prof_master, final ArrayList<String>dir_filter, 
			final NotifyEvent p_ntfy) {
		ArrayList<FilterListItem> filterList=new ArrayList<FilterListItem>() ;
		final AdapterFilterList filterAdapter;
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.filter_list_dlg);
		
		filterAdapter = new AdapterFilterList(mContext,
				R.layout.filter_list_item_view,filterList);
		final ListView lv=
				(ListView) dialog.findViewById(R.id.filter_select_edit_listview);
		
		for (int i=0; i<dir_filter.size();i++) {
			String inc=dir_filter.get(i).substring(0,1);
			String filter=dir_filter.get(i).substring(1,dir_filter.get(i).length());
			boolean b_inc=false;
			if (inc.equals(SMBSYNC_PROF_FILTER_INCLUDE)) b_inc=true;
			filterAdapter.add(new FilterListItem(filter,b_inc) );
		}
		if (filterAdapter.getCount()==0) filterAdapter.add(
				new FilterListItem(mContext.getString(R.string.msgs_filter_list_no_filter),false) );
		lv.setAdapter(filterAdapter);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);
		
		final TextView dlg_title=(TextView) dialog.findViewById(R.id.filter_select_edit_title);
		dlg_title.setText(mContext.getString(R.string.msgs_filter_list_dlg_dir_filter));
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.filter_select_edit_msg);
		final Button dirbtn = (Button) dialog.findViewById(R.id.filter_select_edit_dir_btn);
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);

		final EditText et_filter=(EditText)dialog.findViewById(R.id.filter_select_edit_new_filter);
		final Button addbtn = (Button) dialog.findViewById(R.id.filter_select_edit_add_btn);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_select_edit_cancel_btn);
		final Button btn_ok = (Button) dialog.findViewById(R.id.filter_select_edit_ok_btn);

        lv.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
        		FilterListItem fli = filterAdapter.getItem(idx);
        		if (fli.getFilter().startsWith("---") || fli.isDeleted()) return;
                // リストアイテムを選択したときの処理
        		editDirFilter(idx,filterAdapter,fli,fli.getFilter());
            }
        });	 

		// Addボタンの指定
		et_filter.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable s) {
				if (s.length()!=0) {
					if (isFilterExists(s.toString().trim(),filterAdapter)) {
						String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
						dlg_msg.setText(String.format(mtxt, s.toString().trim()));
						addbtn.setEnabled(false);
						dirbtn.setEnabled(true);
						btn_ok.setEnabled(true);
					} else {
						dlg_msg.setText("");
						addbtn.setEnabled(true);
						dirbtn.setEnabled(false);
						btn_ok.setEnabled(false);
					}
				} else {
					addbtn.setEnabled(false);
					dirbtn.setEnabled(true);
					btn_ok.setEnabled(true);
				}
//				et_filter.setText(s);
			}
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,int after) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});
		addbtn.setEnabled(false);
		addbtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dlg_msg.setText("");
				String newfilter=et_filter.getText().toString();
				if (isFilterExists(newfilter,filterAdapter)) {
					String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
					dlg_msg.setText(String.format(mtxt, newfilter));
					return;
				}
				dlg_msg.setText("");
				et_filter.setText("");
				if (filterAdapter.getItem(0).getFilter().startsWith("---"))
					filterAdapter.remove(filterAdapter.getItem(0));
				filterAdapter.add(new FilterListItem(newfilter,true) );
				filterAdapter.setNotifyOnChange(true);
				filterAdapter.sort(new Comparator<FilterListItem>() {
					@Override
					public int compare(FilterListItem lhs,
							FilterListItem rhs) {
						return lhs.getFilter().compareToIgnoreCase(rhs.getFilter());
					};
				});
				dirbtn.setEnabled(true);
				btn_ok.setEnabled(true);
			}
		});
		// Directoryボタンの指定
		if (getProfileType(prof_master,prof_dapter).equals("L")) {
			if (!mGp.externalStorageIsMounted) dirbtn.setEnabled(false);
		} else if (getProfileType(prof_master,prof_dapter).equals("R")) {
			if (util.isRemoteDisable()) dirbtn.setEnabled(false);
		} else dirbtn.setEnabled(false);
		dirbtn.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText("");
					}
					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						if (arg1!=null) dlg_msg.setText((String)arg1[0]);
						else dlg_msg.setText("");
					}
				});
				listDirFilter(prof_dapter, prof_master,dir_filter,filterAdapter,ntfy);
			}
		});

		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
				dir_filter.clear();
				if (filterAdapter.getCount()>0) {
					for (int i=0;i<filterAdapter.getCount();i++) {
						if (!filterAdapter.getItem(i).isDeleted() &&
								!filterAdapter.getItem(i).getFilter().startsWith("---")) {
							String inc=SMBSYNC_PROF_FILTER_EXCLUDE;
							if (filterAdapter.getItem(i).getInc()) inc=SMBSYNC_PROF_FILTER_INCLUDE;
							dir_filter.add(inc+filterAdapter.getItem(i).getFilter());
						}
							
					}
				}
				p_ntfy.notifyToListener(true, null);
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
		
	};

	private void editDirFilter(final int edit_idx, final AdapterFilterList fa, 
			final FilterListItem fli, final String filter) {
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.filter_edit_dlg);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		final EditText et_filter=(EditText)dialog.findViewById(R.id.filter_edit_dlg_filter);
		et_filter.setText(filter);
		// CANCELボタンの指定
		final Button btn_cancel = (Button) dialog.findViewById(R.id.filter_edit_dlg_cancel_btn);
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				dialog.dismiss();
//				glblParms.profileListView.setSelectionFromTop(currentViewPosX,currentViewPosY);
			}
		});
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		// OKボタンの指定
		Button btn_ok = (Button) dialog.findViewById(R.id.filter_edit_dlg_ok_btn);
		btn_ok.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				TextView dlg_msg =(TextView)dialog.findViewById(R.id.filter_edit_dlg_msg);
				
				String newfilter=et_filter.getText().toString();
				if (!filter.equals(newfilter)) {
					if (isFilterExists(newfilter,fa)) {
						String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
						dlg_msg.setText(String.format(mtxt,newfilter));
						return;
					}
				}
				dialog.dismiss();

				fa.remove(fli);
				fa.insert(fli, edit_idx);
				fli.setFilter(newfilter);
				
				et_filter.setText("");
				
				fa.setNotifyOnChange(true);
				fa.sort(new Comparator<FilterListItem>() {
					@Override
					public int compare(FilterListItem lhs,
							FilterListItem rhs) {
						return lhs.getFilter().compareToIgnoreCase(rhs.getFilter());
					};
				});
//				p_ntfy.notifyToListener(true, null);
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
		dialog.show();
		
	};

	private boolean isFilterExists(String nf, AdapterFilterList fa) {
		if (fa.getCount()==0) return false;
		for (int i=0;i<fa.getCount();i++) {
			if (!fa.getItem(i).isDeleted())
				if (fa.getItem(i).getFilter().equals(nf)) return true;
		}
		return false;
	};
	
	private void listDirFilter(AdapterProfileList t_prof,
			String prof_master, final ArrayList<String>dir_filter, 
			AdapterFilterList fla, final NotifyEvent p_ntfy) {
		if (getProfileType(prof_master,t_prof).equals("L")) {
			listDirFilterLocal(t_prof, prof_master, dir_filter, fla, p_ntfy);
		} else {
			listDirFilterRemote(t_prof, prof_master, dir_filter, fla, p_ntfy);
		}
	};

	private void listDirFilterLocal(AdapterProfileList t_prof,
			String prof_master, final ArrayList<String>dir_filter, 
			final AdapterFilterList fla, final NotifyEvent p_ntfy) {
		ProfileListItem t_i=null;
		
		for (int i=0;i<t_prof.getCount();i++) 
			if (t_prof.getItem(i).getName().equals(prof_master)) 
				t_i=t_prof.getItem(i);
		if (t_i==null) p_ntfy.notifyToListener(false, new Object[]{ 
			String.format(mContext.getString(
				R.string.msgs_filter_list_dlg_master_prof_not_found),prof_master)});
		
		final ProfileListItem item=t_i;
		final String cdir=item.getDir();
		
    	//カスタムダイアログの生成
        final Dialog dialog=new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
    	dialog.setContentView(R.layout.item_select_list_dlg);
		((TextView)dialog.findViewById(R.id.item_select_list_dlg_title))
			.setText(mContext.getString(R.string.msgs_filter_list_dlg_add_dir_filter));
		((TextView)dialog.findViewById(R.id.item_select_list_dlg_subtitle))
    		.setText(msgs_current_dir+" "+item.getLocalMountPoint()+"/"+cdir);
        final TextView dlg_msg=(TextView)dialog.findViewById(R.id.item_select_list_dlg_msg);
	    final Button btn_ok=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);
        dlg_msg.setVisibility(TextView.VISIBLE);

//        if (rows.size()<=2) 
//        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//        	.setVisibility(TextView.VISIBLE);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);
		
        final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
        final TreeFilelistAdapter tfa= 
        		new TreeFilelistAdapter(mContext, false,false);
        lv.setAdapter(tfa);
        ArrayList<TreeFilelistItem> tfl =
        		createLocalFilelist(true,item.getLocalMountPoint(),"/"+cdir);
        if (tfl.size()<1) tfl.add(new TreeFilelistItem(msgs_dir_empty));
        tfa.setDataList(tfl);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);
        lv.setFastScrollEnabled(true);
        
        lv.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
	    		final int pos=tfa.getItem(idx);
	    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
				if (tfi.getName().startsWith("---")) return;
				expandHideLocalDirTree(true,item.getLocalMountPoint(), pos,tfi,tfa);
            }
        });
		lv.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					final int position, long arg3) {
	  			final int t_pos=tfa.getItem(position);
	  			if (tfa.getDataItem(t_pos).isChecked()) { 
		  			ccMenu.addMenuItem(mContext.getString(R.string.msgs_file_select_unselect_this_entry)
							+" "+tfa.getDataItem(t_pos).getPath()+
							tfa.getDataItem(t_pos).getName())
					.setOnClickListener(new CustomContextMenuOnClickListener() {
				  		@Override
						public void onClick(CharSequence menuTitle) {
				    		final TreeFilelistItem tfi=tfa.getDataItem(t_pos);
							if (tfi.getName().startsWith("---")) return;
							tfa.setDataItemIsUnselected(t_pos);
							if (tfa.isDataItemIsSelected()) btn_ok.setEnabled(true);
							else btn_ok.setEnabled(false);
						}
				  	});
	  			} else {
		  			ccMenu.addMenuItem(mContext.getString(R.string.msgs_file_select_select_this_entry)
							+" "+tfa.getDataItem(t_pos).getPath()+
							tfa.getDataItem(t_pos).getName())
					.setOnClickListener(new CustomContextMenuOnClickListener() {
				  		@Override
						public void onClick(CharSequence menuTitle) {
				    		final TreeFilelistItem tfi=tfa.getDataItem(t_pos);
							if (tfi.getName().startsWith("---")) return;
							tfa.setDataItemIsSelected(t_pos);
							btn_ok.setEnabled(true);
						}
				  	});
	  			}
				ccMenu.createMenu();
				return false;
			}
		});

	    //OKボタンの指定
	    btn_ok.setEnabled(false);
        NotifyEvent ntfy=new NotifyEvent(mContext);
		//Listen setRemoteShare response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context arg0, Object[] arg1) {
				btn_ok.setEnabled(true);
			}
			@Override
			public void negativeResponse(Context arg0, Object[] arg1) {
				boolean checked=false;
				for (int i=0;i<tfa.getDataItemCount();i++) {
					if (tfa.getDataItem(i).isChecked()) {
						checked=true;
						break;
					}
				}
				if (checked) btn_ok.setEnabled(true);
				else btn_ok.setEnabled(false);
			}
		});
		tfa.setCbCheckListener(ntfy);
		
	    btn_ok.setText(mContext.getString(R.string.msgs_filter_list_dlg_add));
	    btn_ok.setVisibility(Button.VISIBLE);
	    btn_ok.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	        	if (!addDirFilter(true,tfa,fla,"/"+cdir+"/",dlg_msg)) return;
	        	addDirFilter(false,tfa,fla,"/"+cdir+"/",dlg_msg);
	            dialog.dismiss();
	            p_ntfy.notifyToListener(true,null );
	        }
	    });

        //CANCELボタンの指定
        final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
        btn_cancel.setText(mContext.getString(R.string.msgs_filter_list_dlg_close));
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                p_ntfy.notifyToListener(true, null);
            }
        });
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//        dialog.setOnKeyListener(new DialogOnKeyListener(context));
//        dialog.setCancelable(false);
        dialog.show();
		
		return ;
 
	};
	
	private void listDirFilterRemote(AdapterProfileList t_prof,
			String prof_master, final ArrayList<String>dir_filter, 
			final AdapterFilterList fla, final NotifyEvent p_ntfy) {
		ProfileListItem item=null;
		
		for (int i=0;i<t_prof.getCount();i++) 
			if (t_prof.getItem(i).getName().equals(prof_master)) 
				item=t_prof.getItem(i);
		if (item==null) p_ntfy.notifyToListener(false, new Object[]{
			String.format(mContext.getString(
					R.string.msgs_filter_list_dlg_master_prof_not_found),prof_master)});

		setSmbUserPass(item.getUser(),item.getPass());
		String t_remurl="";
		if (item.getHostname().equals("")) t_remurl=item.getAddr();
		else t_remurl=item.getHostname();
		String h_port="";
		if (!item.getPass().equals("")) h_port=":"+item.getPort();
		final String remurl="smb://"+t_remurl+h_port+"/"+item.getShare();
		final String remdir="/"+item.getDir()+"/";

		NotifyEvent ntfy=new NotifyEvent(mContext);
		// set thread response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				final ArrayList<TreeFilelistItem> rows = new ArrayList<TreeFilelistItem>();
				@SuppressWarnings("unchecked")
				ArrayList<TreeFilelistItem> rfl=(ArrayList<TreeFilelistItem>)o[0];
				
				for (int i=0;i<rfl.size();i++){
					if (rfl.get(i).isDir() && rfl.get(i).canRead()) rows.add(rfl.get(i));
				}
				Collections.sort(rows);
				if (rows.size()<1) rows.add(new TreeFilelistItem(msgs_dir_empty));
				//カスタムダイアログの生成
				final Dialog dialog=new Dialog(mContext);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setCanceledOnTouchOutside(false);
				dialog.setContentView(R.layout.item_select_list_dlg);
				((TextView)dialog.findViewById(R.id.item_select_list_dlg_title))
					.setText(mContext.getString(R.string.msgs_filter_list_dlg_add_dir_filter));
				((TextView)dialog.findViewById(R.id.item_select_list_dlg_subtitle))
		    		.setText(msgs_current_dir+" "+remurl+remdir);
		        final TextView dlg_msg=(TextView)dialog.findViewById(R.id.item_select_list_dlg_msg);
			    final Button btn_ok=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);
		        dlg_msg.setVisibility(TextView.VISIBLE);
				
//		        if (rows.size()<=2) 
//		        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//		        	.setVisibility(TextView.VISIBLE);
				
				CommonDialog.setDlgBoxSizeLimit(dialog, true);
				
				final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
			    final TreeFilelistAdapter tfa= 
			    		new TreeFilelistAdapter(mContext,false,false);
				tfa.setDataList(rows);
			    lv.setAdapter(tfa);
			    lv.setScrollingCacheEnabled(false);
			    lv.setScrollbarFadingEnabled(false);
			    lv.setFastScrollEnabled(true);
				
				lv.setOnItemClickListener(new OnItemClickListener(){
					public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
			            // リストアイテムを選択したときの処理
			    		final int pos=tfa.getItem(idx);
			    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
						if (tfi.getName().startsWith("---")) return;
						expandHideRemoteDirTree(remurl, pos,tfi,tfa);
					}
				});	 
				lv.setOnItemLongClickListener(new OnItemLongClickListener(){
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
							final int position, long arg3) {
			  			final int t_pos=tfa.getItem(position);
			  			if (tfa.getDataItem(t_pos).isChecked()) {
				  			ccMenu.addMenuItem(mContext.getString(R.string.msgs_file_select_unselect_this_entry)
									+" "+tfa.getDataItem(t_pos).getPath()+
									tfa.getDataItem(t_pos).getName())
							.setOnClickListener(new CustomContextMenuOnClickListener() {
						  		@Override
								public void onClick(CharSequence menuTitle) {
						    		final TreeFilelistItem tfi=tfa.getDataItem(t_pos);
									if (tfi.getName().startsWith("---")) return;
									tfa.setDataItemIsUnselected(t_pos);
									if (tfa.isDataItemIsSelected()) btn_ok.setEnabled(true);
									else btn_ok.setEnabled(false);
								}
						  	});
			  			} else {
				  			ccMenu.addMenuItem(mContext.getString(R.string.msgs_file_select_select_this_entry)
									+" "+tfa.getDataItem(t_pos).getPath()+
									tfa.getDataItem(t_pos).getName())
							.setOnClickListener(new CustomContextMenuOnClickListener() {
						  		@Override
								public void onClick(CharSequence menuTitle) {
						    		final TreeFilelistItem tfi=tfa.getDataItem(t_pos);
									if (tfi.getName().startsWith("---")) return;
									tfa.setDataItemIsSelected(t_pos);
									btn_ok.setEnabled(true);
								}
						  	});
			  			}
						ccMenu.createMenu();
						return false;
					}
				});

				//OKボタンの指定
			    btn_ok.setEnabled(false);
		        NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						btn_ok.setEnabled(true);
					}
					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						boolean checked=false;
						for (int i=0;i<tfa.getDataItemCount();i++) {
							if (tfa.getDataItem(i).isChecked()) {
								checked=true;
								break;
							}
						}
						if (checked) btn_ok.setEnabled(true);
						else btn_ok.setEnabled(false);
					}
				});
				tfa.setCbCheckListener(ntfy);

			    btn_ok.setText(mContext.getString(R.string.msgs_filter_list_dlg_add));
			    btn_ok.setVisibility(Button.VISIBLE);
			    btn_ok.setOnClickListener(new View.OnClickListener() {
			        public void onClick(View v) {
			        	if (!addDirFilter(true,tfa,fla,remdir,dlg_msg)) return;
			        	addDirFilter(false,tfa,fla,remdir,dlg_msg);
			            dialog.dismiss();
			            p_ntfy.notifyToListener(true,null );

			        }
			    });
				//CANCELボタンの指定
				final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
				btn_cancel.setText(mContext.getString(R.string.msgs_filter_list_dlg_close));
				btn_cancel.setOnClickListener(new View.OnClickListener() {
				    public void onClick(View v) {
				        dialog.dismiss();
				        p_ntfy.notifyToListener(true, null);
				    }
				});
				// Cancelリスナーの指定
				dialog.setOnCancelListener(new Dialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						btn_cancel.performClick();
					}
				});
//				dialog.setOnKeyListener(new DialogOnKeyListener(context));
//				dialog.setCancelable(false);
				dialog.show();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				p_ntfy.notifyToListener(false, o);
			}
		});
		createRemoteFileList(remurl,remdir,ntfy,true);
		
	};

	private boolean addDirFilter(boolean check_only, TreeFilelistAdapter tfa, 
			AdapterFilterList fla, String cdir, TextView dlg_msg) {
        String sel="", add_msg="";
        //check duplicate entry
        for (int i=0;i<tfa.getCount();i++) {
        	if (tfa.getDataItem(i).isChecked()) {
        		if (tfa.getDataItem(i).getPath().length()==1) 
            		sel=tfa.getDataItem(i).getName();
        		else sel=tfa.getDataItem(i).getPath()+
        				tfa.getDataItem(i).getName();
        		sel=sel.replaceFirst(cdir, "");
        		if (isFilterExists(sel,fla)) {
        			String mtxt=mContext.getString(R.string.msgs_filter_list_duplicate_filter_specified);
					dlg_msg.setText(String.format(mtxt,sel));
					return false;
				}
        		if (!check_only) {
    				fla.add(new FilterListItem(sel,true) );
    				if (add_msg.length()==0) add_msg=sel;
    				else add_msg=add_msg+","+sel;
        		}
        	}
        }
        if (!check_only) {
			fla.setNotifyOnChange(true);
			fla.sort(new Comparator<FilterListItem>() {
				@Override
				public int compare(FilterListItem lhs,
						FilterListItem rhs) {
					return lhs.getFilter().compareToIgnoreCase(rhs.getFilter());
				};
			});
			dlg_msg.setText(String.format(mContext.getString(R.string.msgs_filter_list_dlg_filter_added), 
					add_msg));
        }        
		return true;
	};
	
	private String auditSyncProfileField(Dialog dialog) {
		String prof_name, prof_master="", prof_target="";
		boolean audit_error=false;
		String audit_msg="";
		Spinner spinner_master=(Spinner)dialog.findViewById(R.id.sync_profile_master_spinner);
		Spinner spinner_target=(Spinner)dialog.findViewById(R.id.sync_profile_target_spinner);
		EditText editname = (EditText) dialog.findViewById(R.id.sync_profile_name);
		CheckBox tg = (CheckBox)dialog.findViewById(R.id.sync_profile_active);
		
		if (spinner_master.getCount()>0) 
			prof_master = spinner_master.getSelectedItem().toString().substring(2);
		if (spinner_target.getCount()>0) 
			prof_target = spinner_target.getSelectedItem().toString().substring(2);
		prof_name = editname.getText().toString();

		if (hasInvalidChar(prof_name,new String[]{"\t"})) {
			audit_error=true;
			prof_name=removeInvalidChar(prof_name);
			audit_msg=String.format(msgs_audit_msgs_profilename1,detectedInvalidCharMsg);
			editname.setText(prof_name);
			editname.requestFocus();
		}  else if (prof_name.length()==0) {
			audit_error=true;
			audit_msg=msgs_audit_msgs_profilename2;
			editname.requestFocus();
		}

		if (!audit_error) {
			if (hasInvalidChar(prof_master,new String[]{"\t"})) {
				audit_error=true;
				prof_master=removeInvalidChar(prof_master);
				audit_msg=String.format(msgs_audit_msgs_master1,detectedInvalidCharMsg);
			} else if (prof_master.length()==0) {
				audit_error=true;
				audit_msg=msgs_audit_msgs_master2;
			} else if (!isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,
					 SMBSYNC_PROF_TYPE_SYNC, prof_master)) {
				audit_msg= String.format(
						mContext.getString(R.string.msgs_master_profile_not_found), 
						prof_master);
				audit_error=true;
			} else if (tg.isChecked() && !isProfileActive(SMBSYNC_PROF_GROUP_DEFAULT,
					getProfileType(prof_master,mGp.profileAdapter), prof_master)) {
				audit_msg= 
						mContext.getString(R.string.msgs_prof_active_not_activated);
				audit_error=true;
			}
		}
		
		if (!audit_error) {
			if (hasInvalidChar(prof_target,new String[]{"\t"})) {
				audit_error=true;
				prof_target=removeInvalidChar(prof_target);
				audit_msg=String.format(msgs_audit_msgs_target1,detectedInvalidCharMsg);
			} else if (prof_target.length()==0) {
				audit_error=true;
				audit_msg=msgs_audit_msgs_target2;
			} else if (!isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,
					 SMBSYNC_PROF_TYPE_SYNC, prof_target)) {
				audit_msg= String.format(
						mContext.getString(R.string.msgs_target_profile_not_found), 
						prof_target);
				audit_error=true;
			} else if (tg.isChecked() && !isProfileActive(SMBSYNC_PROF_GROUP_DEFAULT,
					getProfileType(prof_target,mGp.profileAdapter), prof_target)) {
				audit_msg= 
						mContext.getString(R.string.msgs_prof_active_not_activated);
				audit_error=true;
			}

		}
		if (!audit_error) {
			String m_typ=getProfileType(prof_master,mGp.profileAdapter);
			String t_typ=getProfileType(prof_target,mGp.profileAdapter);
			if (m_typ.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
				if (t_typ.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
					audit_error=true;
					audit_msg=mContext.getString(R.string.msgs_invalid_profile_combination);
				}
			}
		}

		return audit_msg;
	}
	
	
//	private String auditRemoteProfileField(Dialog dialog) {
//		String prof_name, prof_addr, prof_user, prof_pass, prof_share, prof_dir, prof_host;
//		boolean audit_error=false;
//		String audit_msg="";
//		final EditText editaddr = (EditText) dialog.findViewById(R.id.remote_profile_addr);
//		final EditText edituser = (EditText) dialog.findViewById(R.id.remote_profile_user);
//		final EditText editpass = (EditText) dialog.findViewById(R.id.remote_profile_pass);
//		final EditText editshare = (EditText) dialog.findViewById(R.id.remote_profile_share);
//		final EditText editdir = (EditText) dialog.findViewById(R.id.remote_profile_dir);
//		final EditText editname = (EditText) dialog.findViewById(R.id.remote_profile_name);
//		final EditText edithost = (EditText) dialog.findViewById(R.id.remote_profile_hostname);
//		final CheckBox cb_use_hostname = (CheckBox) dialog.findViewById(R.id.remote_profile_use_computer_name);
//		final CheckBox cb_use_user_pass = (CheckBox) dialog.findViewById(R.id.remote_profile_use_user_pass);
//		
//		prof_addr = editaddr.getText().toString();
//		prof_host = edithost.getText().toString();
//		prof_user = edituser.getText().toString();
//		prof_pass = editpass.getText().toString();
//		prof_share = editshare.getText().toString();
//		prof_dir = editdir.getText().toString();
//		prof_name = editname.getText().toString();
//		if (hasInvalidChar(prof_name,new String[]{"\t"})) {
//			audit_error=true;
//			prof_name=removeInvalidChar(prof_name);
//			audit_msg=String.format(msgs_audit_msgs_profilename1,detectedInvalidCharMsg);
//			editname.setText(prof_name);
//			editdir.requestFocus();
//		} else if (prof_name.length()==0) {
//			audit_error=true;
//			audit_msg=msgs_audit_msgs_profilename2;
//			editname.requestFocus();
//		}
//		if (!audit_error) {
//			if (hasInvalidChar(prof_addr,new String[]{"\t"})) {
//				audit_error=true;
//				prof_addr=removeInvalidChar(prof_addr);
//				audit_msg=String.format(msgs_audit_msgs_address1,detectedInvalidCharMsg);
//				editaddr.setText(prof_addr);
//				editaddr.requestFocus();
//			} else {
//				if (!cb_use_hostname.isChecked() && prof_addr.length()==0) {
//					audit_error=true;
//					audit_msg=msgs_audit_msgs_address2;
//					editaddr.requestFocus();
//				} else {
//					if (cb_use_hostname.isChecked() && prof_host.length()==0) {
//						audit_error=true;
//						audit_msg=msgs_audit_msgs_address2;
//						editaddr.requestFocus();
//					} else {
//						audit_error=false;
//					}
//				}
//			}
//		}
//		if (!audit_error) {
//			if (cb_use_user_pass.isChecked()) {
//				if (hasInvalidChar(prof_user,new String[]{"\t"})) {
//					audit_error=true;
//					prof_user=removeInvalidChar(prof_user);
//					audit_msg=String.format(msgs_audit_msgs_username1,detectedInvalidCharMsg);
//					edituser.setText(prof_user);
//					edituser.requestFocus();
//				} else {
//					if (prof_user.equals("") && prof_pass.equals("")) {
//						audit_error=true;
//						audit_msg=String.format(msgs_audit_msgs_user_or_pass_missing,detectedInvalidCharMsg);
//						edituser.requestFocus();
//					} else {
//						audit_error=false;
//					}
//				}
//			}
//		}
//		if (!audit_error) {
//			if (cb_use_user_pass.isChecked()) {
//				if (hasInvalidChar(prof_pass,new String[]{"\t"})) {
//					audit_error=true;
//					prof_pass=removeInvalidChar(prof_pass);
//					audit_msg=String.format(msgs_audit_msgs_password1,detectedInvalidCharMsg);
//					editpass.setText(prof_pass);
//					editpass.requestFocus();
//				} else {
//					if (prof_user.equals("") && prof_pass.equals("")) {
//						audit_error=true;
//						audit_msg=String.format(msgs_audit_msgs_user_or_pass_missing,detectedInvalidCharMsg);
//						editpass.requestFocus();
//					} else {
//						audit_error=false;
//					}
//				}
//			}
//		}
//		if (!audit_error) {					
//			if (hasInvalidChar(prof_share,new String[]{"\t"})) {
//				audit_error=true;
//				prof_share=removeInvalidChar(prof_share);
//				audit_msg=String.format(msgs_audit_msgs_share1,detectedInvalidCharMsg);
//				editshare.setText(prof_share);
//				editshare.requestFocus();
//			} else if (prof_share.length()==0) {
//				audit_error=true;
//				audit_msg=msgs_audit_msgs_share2;
//				editshare.requestFocus();
//			}
//		}
//		if (!audit_error) {
//			if (hasInvalidChar(prof_dir,new String[]{"\t"})) {
//				audit_error=true;
//				prof_dir=removeInvalidChar(prof_dir);
//				audit_msg=String.format(msgs_audit_msgs_dir1,detectedInvalidCharMsg);
//				editdir.setText(prof_dir);
//				editdir.requestFocus();
//			}
//		}
//		return audit_msg;
//	};

	private String detectedInvalidChar="",detectedInvalidCharMsg="";
	private boolean hasInvalidChar(String in_text,String[] invchar) {
		for (int i=0;i<invchar.length;i++) {
			if (in_text.indexOf(invchar[i])>=0) {
				if (invchar[i].equals("\t")) {
					detectedInvalidCharMsg="TAB";
					detectedInvalidChar="\t";
				} else {
					detectedInvalidCharMsg=detectedInvalidChar=invchar[i];
				}
				return true;
			}
			
		}
		return false ;
	};

	private String removeInvalidChar(String in){
		if (detectedInvalidChar==null || detectedInvalidChar.length()==0) return in;
		String out="";
		for (int i=0;i<in.length();i++) {
			if (in.substring(i,i+1).equals(detectedInvalidChar)) {
				//ignore
			} else {
				out=out+in.substring(i,i+1);
			}
		}
		return out;
	}
	public boolean isProfileExists(String prof_grp,String prof_type, 
			String prof_name) {
		boolean dup = false;

		for (int i = 0; i <= mGp.profileAdapter.getCount() - 1; i++) {
			ProfileListItem item = mGp.profileAdapter.getItem(i);
			String prof_chk=item.getGroup()+item.getName();
			if (prof_chk.equals(prof_grp+prof_name)) {
				dup = true;
				break;
			}
		}
		return dup;
	};

	private boolean isProfileActive(String prof_grp,String prof_type, 
			String prof_name) {
		boolean active = false;

		for (int i = 0; i <= mGp.profileAdapter.getCount() - 1; i++) {
			String item_key=mGp.profileAdapter.getItem(i).getGroup()+
					mGp.profileAdapter.getItem(i).getType()+
					mGp.profileAdapter.getItem(i).getName();
			if (item_key.equals(prof_grp+prof_type+prof_name)) {
				if (mGp.profileAdapter.getItem(i).getActive()
						.equals(SMBSYNC_PROF_INACTIVE))
					active=false;
				else active=true;
			}
		}
		return active;
	};

	public void scanRemoteNetworkDlg(final NotifyEvent p_ntfy, 
			String port_number, boolean scan_start) {
		//カスタムダイアログの生成
	    final Dialog dialog=new Dialog(mContext);
	    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
	    dialog.setCanceledOnTouchOutside(false);
	    dialog.setContentView(R.layout.scan_remote_ntwk_dlg);
	    final Button btn_scan=(Button)dialog.findViewById(R.id.scan_remote_ntwk_btn_ok);
	    final Button btn_cancel=(Button)dialog.findViewById(R.id.scan_remote_ntwk_btn_cancel);
	    final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_msg);
	    final TextView tv_result = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_scan_result_title);
	    tvmsg.setText(mContext.getString(R.string.msgs_scan_ip_address_press_scan_btn));
	    tv_result.setVisibility(TextView.GONE);
	    
		final String from=SMBSyncUtil.getLocalIpAddress();
		String subnet=from.substring(0,from.lastIndexOf("."));
		String subnet_o1, subnet_o2,subnet_o3;
		subnet_o1=subnet.substring(0,subnet.indexOf("."));
		subnet_o2=subnet.substring(subnet.indexOf(".")+1,subnet.lastIndexOf("."));
		subnet_o3=subnet.substring(subnet.lastIndexOf(".")+1,subnet.length());
		final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o1);
		final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o2);
		final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o3);
		final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o4);
		final EditText eaEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_end_address_o4);
		baEt1.setText(subnet_o1);
		baEt2.setText(subnet_o2);
		baEt3.setText(subnet_o3);
		baEt4.setText("1");
		baEt4.setSelection(1);
		eaEt4.setText("254");
		baEt4.requestFocus();
		
		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.scan_remote_ntwk_use_port);
		final EditText et_port_number = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_port_number);

		final LinearLayout ll_port=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_port_option);
		if (mGp.settingShowRemotePortOption) {
			ll_port.setVisibility(LinearLayout.VISIBLE);
		} else {
			ll_port.setVisibility(LinearLayout.GONE);
		}
		
		
	    CommonDialog.setDlgBoxSizeLimit(dialog, true);
	    
	    if (port_number.equals("")) {
		    et_port_number.setEnabled(false);
		    cb_use_port_number.setChecked(false);
	    } else {
		    et_port_number.setEnabled(true);
		    et_port_number.setText(port_number);
		    cb_use_port_number.setChecked(true);
	    }
	    cb_use_port_number.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				et_port_number.setEnabled(isChecked);
			}
	    });
	    
	    final NotifyEvent ntfy_lv_click=new NotifyEvent(mContext);
	    ntfy_lv_click.setListener(new NotifyEventListener(){
			@Override
			public void positiveResponse(Context c, Object[] o) {
	            dialog.dismiss();
				p_ntfy.notifyToListener(true,o);
			}
			@Override
			public void negativeResponse(Context c, Object[] o) {}
	    });
	    
		final ArrayList<ScanAddressResultListItem> ipAddressList = new ArrayList<ScanAddressResultListItem>();
//		ScanAddressResultListItem li=new ScanAddressResultListItem();
//		li.server_name=mContext.getString(R.string.msgs_ip_address_no_address);
//		ipAddressList.add(li);
	    final ListView lv = (ListView) dialog.findViewById(R.id.scan_remote_ntwk_scan_result_list);
	    final AdapterScanAddressResultList adap=new AdapterScanAddressResultList
		    	(mContext, R.layout.scan_address_result_list_item, ipAddressList, ntfy_lv_click);
	    lv.setAdapter(adap);
	    lv.setScrollingCacheEnabled(false);
	    lv.setScrollbarFadingEnabled(false);
	    
	    //SCANボタンの指定
	    btn_scan.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            ipAddressList.clear();
	            NotifyEvent ntfy=new NotifyEvent(mContext);
	    		ntfy.setListener(new NotifyEventListener() {
	    			@Override
	    			public void positiveResponse(Context c,Object[] o) {
	    				if (ipAddressList.size()<1) {
	    					tvmsg.setText(mContext.getString(R.string.msgs_scan_ip_address_not_detected));
	    					tv_result.setVisibility(TextView.GONE);
	    				} else {
	    					tvmsg.setText(mContext.getString(R.string.msgs_scan_ip_address_select_detected_host));
	    					tv_result.setVisibility(TextView.VISIBLE);
	    				}
//	    				adap.clear();
//	    				for (int i=0;i<ipAddressList.size();i++) 
//	    					adap.add(ipAddressList.get(i));
	    			}
	    			@Override
	    			public void negativeResponse(Context c,Object[] o) {}

	    		});
				if (auditScanAddressRangeValue(dialog)) {
					tv_result.setVisibility(TextView.GONE);
					String ba1=baEt1.getText().toString();
					String ba2=baEt2.getText().toString();
					String ba3=baEt3.getText().toString();
					String ba4=baEt4.getText().toString();
					String ea4=eaEt4.getText().toString();
					String subnet=ba1+"."+ba2+"."+ba3;
					int begin_addr = Integer.parseInt(ba4);
					int end_addr = Integer.parseInt(ea4);
					scanRemoteNetwork(dialog,lv,adap,ipAddressList,
							subnet, begin_addr, end_addr, ntfy);
				} else {
					//error
				}
	        }
	    });

	    //CANCELボタンの指定
	    btn_cancel.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            dialog.dismiss();
	            p_ntfy.notifyToListener(false, null);
	        }
	    });
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
	    dialog.show();

	    if (scan_start) btn_scan.performClick();
	};

	private int mScanCompleteCount=0, mScanAddrCount=0;
	private String mLockScanCompleteCount="";
	private void scanRemoteNetwork(
			final Dialog dialog,
			final ListView lv_ipaddr,
			final AdapterScanAddressResultList adap,
			final ArrayList<ScanAddressResultListItem> ipAddressList,
			final String subnet, final int begin_addr, final int end_addr,
			final NotifyEvent p_ntfy) {
		final Handler handler=new Handler();
		final ThreadCtrl tc=new ThreadCtrl();
		final LinearLayout ll_addr=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_scan_address);
		final LinearLayout ll_prog=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_progress);
		final TextView tvmsg=(TextView) dialog.findViewById(R.id.scan_remote_ntwk_progress_msg);
		final Button btn_scan = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_ok);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_cancel);
		final Button scan_cancel = (Button) dialog.findViewById(R.id.scan_remote_ntwk_progress_cancel);
		
		final CheckBox cb_use_port_number = (CheckBox) dialog.findViewById(R.id.scan_remote_ntwk_use_port);
		final EditText et_port_number = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_port_number);

		tvmsg.setText("");
		scan_cancel.setText(R.string.msgs_progress_spin_dlg_addr_cancel);
		ll_addr.setVisibility(LinearLayout.GONE);
		ll_prog.setVisibility(LinearLayout.VISIBLE);
		btn_scan.setEnabled(false);
		btn_cancel.setEnabled(false);
		adap.setButtonEnabled(false);
		scan_cancel.setEnabled(true);
	    dialog.setOnKeyListener(new DialogBackKeyListener(mContext));
	    dialog.setCancelable(false);
		// CANCELボタンの指定
		scan_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				scan_cancel.setText(mContext.getString(R.string.msgs_progress_dlg_canceling));
				scan_cancel.setEnabled(false);
				util.addDebugLogMsg(1,"W","IP Address list creation was cancelled");
				tc.setDisable();
			}
		});
		if (util.isActivityForeground()) dialog.show();
		
		util.addDebugLogMsg(1,"I","Scan IP address ransge is "+subnet+ "."+begin_addr+" - "+end_addr);
		
		final String scan_prog=mContext.getString(R.string.msgs_ip_address_scan_progress);
		String p_txt=String.format(scan_prog,0);
		tvmsg.setText(p_txt);

       	new Thread(new Runnable() {
			@Override
			public void run() {//non UI thread
				mScanCompleteCount=0;
				mScanAddrCount=end_addr-begin_addr+1;
				int scan_thread=30;
				String scan_port="";
				if (cb_use_port_number.isChecked()) scan_port=et_port_number.getText().toString();
				for (int i=begin_addr; i<=end_addr;i+=scan_thread) {
					if (!tc.isEnable()) break;
					boolean scan_end=false;
					for (int j=i;j<(i+scan_thread);j++) {
						if (j<=end_addr) {
							startRemoteNetworkScanThread(handler, tc, dialog, p_ntfy,
									lv_ipaddr, adap, tvmsg, subnet+"."+j,ipAddressList, scan_port);
						} else {
							scan_end=true;
						}
					}
					if (!scan_end) {
						for (int wc=0;wc<210;wc++) {
							if (!tc.isEnable()) break;
							SystemClock.sleep(30);
						}
					}
				}
				if (!tc.isEnable()) {
					handler.post(new Runnable() {// UI thread
						@Override
						public void run() {
							closeScanRemoteNetworkProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
						}
					});
				} else {
					handler.postDelayed(new Runnable() {// UI thread
						@Override
						public void run() {
							closeScanRemoteNetworkProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
						}
					},10000);
				}
			}
		})
       	.start();
	};

	private void closeScanRemoteNetworkProgressDlg(
			final Dialog dialog,
			final NotifyEvent p_ntfy,
			final ListView lv_ipaddr,
			final AdapterScanAddressResultList adap,
			final TextView tvmsg) {
		final LinearLayout ll_addr=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_scan_address);
		final LinearLayout ll_prog=(LinearLayout) dialog.findViewById(R.id.scan_remote_ntwk_progress);
		final Button btn_scan = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_ok);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.scan_remote_ntwk_btn_cancel);
		ll_addr.setVisibility(LinearLayout.VISIBLE);
		ll_prog.setVisibility(LinearLayout.GONE);
		btn_scan.setEnabled(true);
		btn_cancel.setEnabled(true);
		adap.setButtonEnabled(true);
	    dialog.setOnKeyListener(null);
	    dialog.setCancelable(true);
		if (p_ntfy!=null) p_ntfy.notifyToListener(true, null);
		
	};
	
	private void startRemoteNetworkScanThread(final Handler handler,
			final ThreadCtrl tc,
			final Dialog dialog,
			final NotifyEvent p_ntfy,
			final ListView lv_ipaddr,
			final AdapterScanAddressResultList adap,
			final TextView tvmsg,
			final String addr,
			final ArrayList<ScanAddressResultListItem> ipAddressList,
			final String scan_port) {
		final String scan_prog=mContext.getString(R.string.msgs_ip_address_scan_progress);
		Thread th=new Thread(new Runnable() {
			@Override
			public void run() {
				if (isIpAddrReachable(addr,scan_port)) {
					synchronized(mLockScanCompleteCount) {
						mScanCompleteCount++;
						String srv_name=getSmbHostName(addr);
						ScanAddressResultListItem li=new ScanAddressResultListItem();
						li.server_address=addr;
						li.server_name=srv_name;
						ipAddressList.add(li);
						Collections.sort(ipAddressList, new Comparator<ScanAddressResultListItem>(){
							@Override
							public int compare(ScanAddressResultListItem lhs,
									ScanAddressResultListItem rhs) {
								return lhs.server_address.compareTo(rhs.server_address);
							}
						});
					}
				} else {
					synchronized(mLockScanCompleteCount) {
						mScanCompleteCount++;
					}
				}
				handler.post(new Runnable() {// UI thread
					@Override
					public void run() {
						synchronized(mLockScanCompleteCount) {
							lv_ipaddr.setSelection(lv_ipaddr.getCount());
							adap.notifyDataSetChanged();
							String p_txt=String.format(scan_prog, 
									(mScanCompleteCount*100)/mScanAddrCount);
							tvmsg.setText(p_txt);
							
							if (mScanCompleteCount>=mScanAddrCount) {
								closeScanRemoteNetworkProgressDlg(dialog, p_ntfy, lv_ipaddr, adap, tvmsg);
							}
						}
					}
				});
			}
       	});
       	th.start();
	};

	private boolean isIpAddrReachable(String address, String scan_port) {
		boolean reachable=false;
//		reachable=NetworkUtil.ping(address);
		if (scan_port.equals("")) {
			if (!NetworkUtil.isIpAddressAndPortConnected(address,139,3000)) {
				reachable=NetworkUtil.isIpAddressAndPortConnected(address,445,3000);
			} else reachable=true;
		} else {
			reachable=NetworkUtil.isIpAddressAndPortConnected(address,
					Integer.parseInt(scan_port),3000);
		}
		util.addDebugLogMsg(2,"I","isIpAddrReachable Address="+address+
				", port="+scan_port+", reachable="+reachable);
		return reachable;
	};

//	@SuppressWarnings("unused")
//	private boolean isNbtAddressActive(String address) {
//		boolean result=NetworkUtil.isNbtAddressActive(address);
//    	util.addDebugLogMsg(1,"I","isSmbHost Address="+address+", result="+result);
//		return result;
//	};
	
	private String getSmbHostName(String address) {
		String srv_name=NetworkUtil.getSmbHostNameFromAddress(address);
       	util.addDebugLogMsg(1,"I","getSmbHostName Address="+address+", name="+srv_name);
    	return srv_name;
 	};

	private boolean auditScanAddressRangeValue(Dialog dialog) {
		boolean result=false;
		final EditText baEt1 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o1);
		final EditText baEt2 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o2);
		final EditText baEt3 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o3);
		final EditText baEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_begin_address_o4);
		final EditText eaEt4 = (EditText) dialog.findViewById(R.id.scan_remote_ntwk_end_address_o4);
		final TextView tvmsg = (TextView) dialog.findViewById(R.id.scan_remote_ntwk_msg);

		String ba1=baEt1.getText().toString();
		String ba2=baEt2.getText().toString();
		String ba3=baEt3.getText().toString();
		String ba4=baEt4.getText().toString();
		String ea4=eaEt4.getText().toString();
		
    	tvmsg.setText("");
		if (ba1.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt1.requestFocus();
			return false;
		} else if (ba2.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt2.requestFocus();
			return false;
		} else if (ba3.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt3.requestFocus();
			return false;
		} else if (ba4.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_notspecified));
			baEt4.requestFocus();
			return false;
		} else if (ea4.equals("")) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_end_notspecified));
			eaEt4.requestFocus();
			return false;
		}
		int iba1 = Integer.parseInt(ba1);
		if (iba1>255) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
			baEt1.requestFocus();
			return false;
		}
		int iba2 = Integer.parseInt(ba2);
		if (iba2>255) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
			baEt2.requestFocus();
			return false;
		}
		int iba3 = Integer.parseInt(ba3);
		if (iba3>255) {
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_addr_range_error));
			baEt3.requestFocus();
			return false;
		}
		int iba4 = Integer.parseInt(ba4);
		int iea4 = Integer.parseInt(ea4);
		if (iba4>0 && iba4<255) {
			if (iea4>0 && iea4<255) {
				if (iba4<=iea4) {
					result=true;
				} else {
					baEt4.requestFocus();
					tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_addr_gt_end_addr));
				}
			} else {
				eaEt4.requestFocus();
				tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_end_range_error));
			}
		} else {
			baEt4.requestFocus();
			tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_begin_range_error));
		}

		if (iba1==192&&iba2==168) {
			//class c private
		} else {
			if (iba1==10) {
				//class a private
			} else {
				if (iba1==172 && (iba2>=16&&iba2<=31)) {
					//class b private
				} else {
					//not private
					result=false;
					tvmsg.setText(mContext.getString(R.string.msgs_ip_address_range_dlg_not_private));
				}
			}
		}
		
		return result;
	};
	
//	private void setSyncMaterOrTagetProfile(
//			boolean mp, String base_prof_name, final NotifyEvent p_ntfy) {
//		final ArrayList<String> rows = new ArrayList<String>();
//		String prof_type=getProfileType(base_prof_name,glblParms.profileAdapter);
//		ProfileListItem base_pli=getProfile(base_prof_name, glblParms.profileAdapter);
//		for (int i = 0; i < glblParms.profileAdapter.getCount(); i++) {
//			ProfileListItem item = glblParms.profileAdapter.getItem(i);
//			if (!mp) {
//				if (!item.getType().equals(SMBSYNC_PROF_TYPE_SYNC) && 
//					item.getActive().equals(SMBSYNC_PROF_ACTIVE)) {
//					if (prof_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
//						if (item.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
//							if (!base_pli.getLocalMountPoint().equals(item.getLocalMountPoint()) || 
//									!base_pli.getDir().equals(item.getDir())) {
//								rows.add(item.getType()+" "+item.getName());
//							}
//						} else {
//							rows.add(item.getType()+" "+item.getName());
//						}
//					} else {
//						if (item.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
//							rows.add(item.getType()+" "+item.getName());
//						}
//					}
//				}
//			} else {
//				if (!item.getType().equals(SMBSYNC_PROF_TYPE_SYNC) && 
//						item.getActive().equals(SMBSYNC_PROF_ACTIVE)) {
//						rows.add(item.getType()+" "+item.getName());
//				}
//			}
//		}
//		if (rows.size()<1) rows.add(msgs_no_profile);
//    	//カスタムダイアログの生成
//        final Dialog dialog=new Dialog(mContext);
//        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//    	dialog.setContentView(R.layout.item_select_list_dlg);
//        ((TextView)dialog.findViewById(R.id.item_select_list_dlg_title))
//        	.setText(msgs_select_profile);
//        ((TextView)dialog.findViewById(R.id.item_select_list_dlg_subtitle))
//    	.setVisibility(TextView.GONE);
//        
//        CommonDialog.setDlgBoxSizeLimit(dialog, false);
//        
//        ListView lv = (ListView) dialog.findViewById(android.R.id.list);
//        lv.setAdapter(new AdapterSelectSyncProfileList(mContext, R.layout.sync_profile_list_item_view, rows));
//        lv.setScrollingCacheEnabled(false);
//        lv.setScrollbarFadingEnabled(false);
//        
//        lv.setOnItemClickListener(new OnItemClickListener(){
//        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
//        		if (rows.get(idx).startsWith("---")) return;
//	        	// リストアイテムを選択したときの処理
//                dialog.dismiss();
//				p_ntfy.notifyToListener(true, 
//						new Object[]{rows.get(idx).substring(2,rows.get(idx).length())});
//            }
//        });	 
//        //CANCELボタンの指定
//        final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
//        btn_cancel.setOnClickListener(new View.OnClickListener() {
//            public void onClick(View v) {
//                dialog.dismiss();
//                p_ntfy.notifyToListener(false, null);
//            }
//        });
//		// Cancelリスナーの指定
//		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
//			@Override
//			public void onCancel(DialogInterface arg0) {
//				btn_cancel.performClick();
//			}
//		});
//        dialog.show();
//		
//	};

	private ArrayList<TreeFilelistItem>  createLocalFilelist(boolean dironly, 
			String url, String dir) {
		
		ArrayList<TreeFilelistItem> tfl = new ArrayList<TreeFilelistItem>(); ;
		String tdir,fp;
		
		if (dir.equals("")) fp=tdir="/";
		else {
			tdir=dir;
			fp=dir+"/";
		}
		File lf = new File(url+tdir);
		final File[]  ff = lf.listFiles();
		TreeFilelistItem tfi=null;
		if (ff!=null) {
			for (int i=0;i<ff.length;i++){
				if (ff[i].canRead()) {
					int dirct=0;
					if (ff[i].isDirectory()) {
						File tlf=new File(url+tdir+"/"+ff[i].getName());
						File[] lfl=tlf.listFiles();
						if (lfl!=null) {
							for (int j=0;j<lfl.length;j++) {
								if (dironly) {
									if (lfl[j].isDirectory()) dirct++;
								} else dirct++;
							}
						}
					}
					tfi=new TreeFilelistItem(ff[i].getName(),
							""+", ", ff[i].isDirectory(), 0,0,false,
							ff[i].canRead(),ff[i].canWrite(),
							ff[i].isHidden(),fp,0);
					tfi.setSubDirItemCount(dirct);

					if (dironly) {
						if (ff[i].isDirectory()) tfl.add(tfi);
					} else tfl.add(tfi);
				}
			}
			Collections.sort(tfl);
		}
		return tfl;
	};
	
	
	public void selectLocalDirDlg(final String url,final String dir,
			String p_dir,final NotifyEvent p_ntfy) {
		
    	//カスタムダイアログの生成
        final Dialog dialog=new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setCanceledOnTouchOutside(false);
    	dialog.setContentView(R.layout.item_select_list_dlg);
        ((TextView)dialog.findViewById(R.id.item_select_list_dlg_title))
        	.setText(msgs_select_local_dir);
        ((TextView)dialog.findViewById(R.id.item_select_list_dlg_subtitle))
        	.setText(msgs_current_dir+url+dir);
	    final Button btn_ok=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);

//        if (rows.size()<=2) 
//        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//        	.setVisibility(TextView.VISIBLE);

        CommonDialog.setDlgBoxSizeLimit(dialog, true);
		
        ListView lv = (ListView) dialog.findViewById(android.R.id.list);
        final TreeFilelistAdapter tfa= 
        		new TreeFilelistAdapter(mContext,true,false);
        lv.setAdapter(tfa);
        ArrayList<TreeFilelistItem> tfl =createLocalFilelist(true,url,dir);
        if (tfl.size()<1) tfl.add(new TreeFilelistItem(msgs_dir_empty));
        tfa.setDataList(tfl);
        lv.setScrollingCacheEnabled(false);
        lv.setScrollbarFadingEnabled(false);
        lv.setFastScrollEnabled(true);

        if (p_dir.length()!=0)
        	for (int i=0;i<tfa.getDataItemCount();i++) {
        		if (tfa.getDataItem(i).getName().equals(p_dir)) 
        			lv.setSelection(i);
        	}
        lv.setOnItemClickListener(new OnItemClickListener(){
        	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
	    		final int pos=tfa.getItem(idx);
	    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
				if (tfi.getName().startsWith("---")) return;
				expandHideLocalDirTree(true,url, pos,tfi,tfa);
			}
        });
		lv.setOnItemLongClickListener(new OnItemLongClickListener(){
			@Override
			public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
					final int position, long arg3) {
	  			final int t_pos=tfa.getItem(position);
	  			if (tfa.getDataItem(t_pos).isChecked()) {
		  			ccMenu.addMenuItem(mContext.getString(R.string.msgs_file_select_select_this_entry)
							+" "+tfa.getDataItem(t_pos).getPath()+
							tfa.getDataItem(t_pos).getName())
					.setOnClickListener(new CustomContextMenuOnClickListener() {
				  		@Override
						public void onClick(CharSequence menuTitle) {
				    		final TreeFilelistItem tfi=tfa.getDataItem(t_pos);
							if (tfi.getName().startsWith("---")) return;
							tfa.setDataItemIsUnselected(t_pos);
							if (tfa.isDataItemIsSelected()) btn_ok.setEnabled(true);
							else btn_ok.setEnabled(false);
						}
				  	});
	  			} else {
		  			ccMenu.addMenuItem(mContext.getString(R.string.msgs_file_select_select_this_entry)
							+" "+tfa.getDataItem(t_pos).getPath()+
							tfa.getDataItem(t_pos).getName())
					.setOnClickListener(new CustomContextMenuOnClickListener() {
				  		@Override
						public void onClick(CharSequence menuTitle) {
				    		final TreeFilelistItem tfi=tfa.getDataItem(t_pos);
							if (tfi.getName().startsWith("---")) return;
							tfa.setDataItemIsSelected(t_pos);
							btn_ok.setEnabled(true);
						}
				  	});
	  			}
				ccMenu.createMenu();
				return false;
			}
		});
		NotifyEvent cb_ntfy=new NotifyEvent(mContext);
		cb_ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				if (o!=null) {
					int pos=(Integer)o[0];
					if (tfa.getDataItem(pos).isChecked()) btn_ok.setEnabled(true);
				}
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {
				btn_ok.setEnabled(false);
				for (int i=0;i<tfa.getDataItemCount();i++) {
					if (tfa.getDataItem(i).isChecked()) {
						btn_ok.setEnabled(true);
						break;
					}
				}
			}
		});
		tfa.setCbCheckListener(cb_ntfy);

	    //OKボタンの指定
		btn_ok.setEnabled(false);
	    btn_ok.setVisibility(Button.VISIBLE);
	    btn_ok.setOnClickListener(new View.OnClickListener() {
	        public void onClick(View v) {
	            String sel="";
	            for (int i=0;i<tfa.getCount();i++) {
	            	if (tfa.getDataItem(i).isChecked()  && 
	            			!tfa.getDataItem(i).getName().equals(msgs_dir_empty)) {
	            		if (tfa.getDataItem(i).getPath().length()==1) 
		            		sel=tfa.getDataItem(i).getName();
	            		else sel=tfa.getDataItem(i).getPath()
	            				.substring(1,tfa.getDataItem(i).getPath().length())+
	            				tfa.getDataItem(i).getName();
	            		break;
	            	}
	            }
	            if (sel.equals("")) {
	            	
	            }
	            dialog.dismiss();
	            p_ntfy.notifyToListener(true,new Object[]{sel} );
	        }
	    });

        //CANCELボタンの指定
        final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
        btn_cancel.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                dialog.dismiss();
                p_ntfy.notifyToListener(false, null);
            }
        });
		// Cancelリスナーの指定
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//        dialog.setOnKeyListener(new DialogOnKeyListener(context));
//        dialog.setCancelable(false);
        dialog.show();
		
		return ;
	};

	private void createRemoteFileList(String remurl,String remdir, 
			final NotifyEvent p_event, boolean readSubDirCnt) {
		final ArrayList<TreeFilelistItem> remoteFileList =
								new ArrayList<TreeFilelistItem>();
		final ThreadCtrl tc=new ThreadCtrl();
		tc.setEnable();
		tc.setThreadResultSuccess();
		
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setContentView(R.layout.progress_spin_dlg);
		((TextView)dialog.findViewById(R.id.progress_spin_dlg_title))
			.setText(R.string.msgs_progress_spin_dlg_filelist_getting);
		((TextView)dialog.findViewById(R.id.progress_spin_dlg_msg))
			.setText("");
		((TextView)dialog.findViewById(R.id.progress_spin_dlg_msg))
			.setVisibility(TextView.GONE);
		final Button btn_cancel = (Button) dialog.findViewById(R.id.progress_spin_dlg_btn_cancel);
		btn_cancel.setText(R.string.msgs_progress_spin_dlg_filelist_cancel);
		
//		(dialog.context.findViewById(R.id.progress_spin_dlg)).setVisibility(TextView.GONE);
//		(dialog.context.findViewById(R.id.progress_spin_dlg)).setEnabled(false);
		
		CommonDialog.setDlgBoxSizeCompact(dialog);
		// CANCELボタンの指定
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				tc.setDisable();//disableAsyncTask();
				btn_cancel.setText(mContext.getString(R.string.msgs_progress_dlg_canceling));
				btn_cancel.setEnabled(false);
				util.addDebugLogMsg(1,"W","Sharelist is cancelled.");
			}
		});
		dialog.setOnCancelListener(new Dialog.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
//		dialog.setOnKeyListener(new DialogOnKeyListener(context));
//		dialog.setCancelable(false);
//		dialog.show(); showDelayedProgDlgで表示

		final Handler hndl=new Handler();
		NotifyEvent ntfy=new NotifyEvent(mContext);
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				hndl.post(new Runnable(){
					@Override
					public void run() {
						dialog.dismiss();
						String err;
						util.addDebugLogMsg(1,"I","FileListThread result="+tc.getThreadResult()+","+
								"msg="+tc.getThreadMessage()+", enable="+
									tc.isEnable());
						if (tc.isThreadResultSuccess()) {
							p_event.notifyToListener(true, new Object[]{remoteFileList});
						} else {
							if (tc.isThreadResultCancelled()) err=msgs_filelist_cancel;
							else err=msgs_filelist_error+"\n"+tc.getThreadMessage();
							p_event.notifyToListener(false, new Object[]{err});
						}
					}
				});
			}
			@Override
			public void negativeResponse(Context c,Object[] o) {}
		});
		
		Thread tf = new Thread(new ReadRemoteFilelist(mContext, tc, remurl, remdir, remoteFileList,
				smbUser,smbPass, ntfy, true, readSubDirCnt, mGp));
		tf.start();
		
//		showDelayedProgDlg(200,dialog, tc);
		dialog.show();
	};
	
	@SuppressWarnings("unused")
	private void showDelayedProgDlgX(final int wt, final Dialog dialog, final ThreadCtrl tc) {
    	final Handler handler=new Handler();

       	new Thread(new Runnable() {
			@Override
			public void run() {//Non UI thread
				try { 
					Thread.sleep(wt);
				} catch (InterruptedException e) 
					{e.printStackTrace();}
				
				handler.post(new Runnable() {
					@Override
					public void run() {// UI thread
						if (tc.isEnable()) if (dialog!=null) dialog.show();
					}
				});
			}
		})
       	.start();
	}
	
	public void selectRemoteShareDlg(final String remurl, String remdir,
			final NotifyEvent p_ntfy) { 
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		// set thread response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				final ArrayList<String> rows = new ArrayList<String>();
				@SuppressWarnings("unchecked")
				ArrayList<TreeFilelistItem> rfl=(ArrayList<TreeFilelistItem>)o[0];
				
				for (int i=0;i<rfl.size();i++){
					if (rfl.get(i).isDir() && rfl.get(i).canRead() && 
							!rfl.get(i).getName().startsWith("IPC$"))
						rows.add(rfl.get(i).getName().replaceAll("/", ""));
				}
				if (rows.size()<1) rows.add(msgs_dir_empty);
				Collections.sort(rows, String.CASE_INSENSITIVE_ORDER);
				//カスタムダイアログの生成
				final Dialog dialog=new Dialog(mContext);
				dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				dialog.setCanceledOnTouchOutside(false);
				dialog.setContentView(R.layout.item_select_list_dlg);
				((TextView)dialog.findViewById(R.id.item_select_list_dlg_title))
					.setText(msgs_select_remote_share);
				((TextView)dialog.findViewById(R.id.item_select_list_dlg_subtitle))
					.setVisibility(TextView.GONE);
				
//		        if (rows.size()<=2) 
//		        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//		        	.setVisibility(TextView.VISIBLE);
				
				CommonDialog.setDlgBoxSizeLimit(dialog, false);
				
				final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
				lv.setAdapter(new ArrayAdapter<String>(mContext,
							R.layout.simple_list_item_1o, rows));
				lv.setScrollingCacheEnabled(false);
				lv.setScrollbarFadingEnabled(false);
				
				lv.setOnItemClickListener(new OnItemClickListener(){
					public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
						if (rows.get(idx).startsWith("---")) return;
				    	dialog.dismiss();
				    	// リストアイテムを選択したときの処理
			    		String tmp =(String)lv.getItemAtPosition(idx);
			    		if (tmp.lastIndexOf("/")>0) tmp=tmp.substring(0,tmp.lastIndexOf("/"));
			    		p_ntfy.notifyToListener(true, new Object[]{tmp});
					}
				});	 
				//CANCELボタンの指定
				final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
				btn_cancel.setOnClickListener(new View.OnClickListener() {
				    public void onClick(View v) {
				        dialog.dismiss();
			    		p_ntfy.notifyToListener(false, null);
				    }
				});
				// Cancelリスナーの指定
				dialog.setOnCancelListener(new Dialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						btn_cancel.performClick();
					}
				});
//				dialog.setOnKeyListener(new DialogOnKeyListener(context));
//				dialog.setCancelable(false);
				dialog.show();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				p_ntfy.notifyToListener(false, o);
			}
		});
		createRemoteFileList(remurl,remdir, ntfy,false);

	};
        
	public void setRemoteDir(final String remurl, 
			final String curdir, final String p_dir, final NotifyEvent p_ntfy) { 
		final ArrayList<TreeFilelistItem> rows = new ArrayList<TreeFilelistItem>();
		
		NotifyEvent ntfy=new NotifyEvent(mContext);
		// set thread response 
		ntfy.setListener(new NotifyEventListener() {
			@Override
			public void positiveResponse(Context c,Object[] o) {
				@SuppressWarnings("unchecked")
				ArrayList<TreeFilelistItem> rfl = (ArrayList<TreeFilelistItem>)o[0];
				for (int i=0;i<rfl.size();i++){
					if (rfl.get(i).isDir() && rfl.get(i).canRead()) rows.add(rfl.get(i));
				}
				Collections.sort(rows);
				if (rows.size()<1) rows.add(new TreeFilelistItem(msgs_dir_empty));
				//カスタムダイアログの生成
			    final Dialog dialog=new Dialog(mContext);
			    dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
			    dialog.setCanceledOnTouchOutside(false);
				dialog.setContentView(R.layout.item_select_list_dlg);
			    ((TextView)dialog.findViewById(R.id.item_select_list_dlg_title))
			    	.setText(msgs_select_remote_dir);
			    
			    ((TextView)dialog.findViewById(R.id.item_select_list_dlg_subtitle))
			    	.setText(msgs_current_dir+"/"+remurl);
//			    if (rows.size()<1) {
//			    	TextView dlg_msg=(TextView)dialog.findViewById(R.id.item_select_list_dlg_msg);
//			    	dlg_msg.setText(msgs_dir_empty);
//			    	dlg_msg.setVisibility(TextView.VISIBLE);
//			    }
			    final Button btn_ok=(Button)dialog.findViewById(R.id.item_select_list_dlg_ok_btn);
//		        if (rows.size()<=2) 
//		        	((TextView)dialog.findViewById(R.id.item_select_list_dlg_spacer))
//		        	.setVisibility(TextView.VISIBLE);
			    
			    CommonDialog.setDlgBoxSizeLimit(dialog, true);
				
			    final ListView lv = (ListView) dialog.findViewById(android.R.id.list);
			    final TreeFilelistAdapter tfa= 
			    		new TreeFilelistAdapter(mContext,true,false);
//				tfa.setNotifyOnChange(true);
				tfa.setDataList(rows);
			    lv.setAdapter(tfa);
			    lv.setScrollingCacheEnabled(false);
			    lv.setScrollbarFadingEnabled(false);
			    lv.setFastScrollEnabled(true);
			    
		        if (p_dir.length()!=0)
		        	for (int i=0;i<tfa.getDataItemCount();i++) {
		        		if (tfa.getDataItem(i).getName().equals(p_dir)) 
		        			lv.setSelection(i);
		        	}
			    
			    lv.setOnItemClickListener(new OnItemClickListener(){
			    	public void onItemClick(AdapterView<?> items, View view, int idx, long id) {
			            // リストアイテムを選択したときの処理
			    		final int pos=tfa.getItem(idx);
			    		final TreeFilelistItem tfi=tfa.getDataItem(pos);
						if (tfi.getName().startsWith("---")) return;
						expandHideRemoteDirTree(remurl, pos,tfi,tfa);
			        }
			    });	
				lv.setOnItemLongClickListener(new OnItemLongClickListener(){
					@Override
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
							final int position, long arg3) {
			  			final int t_pos=tfa.getItem(position);
			  			if (tfa.getDataItem(t_pos).isChecked()) {
				  			ccMenu.addMenuItem(mContext.getString(R.string.msgs_file_select_unselect_this_entry)
									+" "+tfa.getDataItem(t_pos).getPath()+
									tfa.getDataItem(t_pos).getName())
							.setOnClickListener(new CustomContextMenuOnClickListener() {
						  		@Override
								public void onClick(CharSequence menuTitle) {
						    		final TreeFilelistItem tfi=tfa.getDataItem(t_pos);
									if (tfi.getName().startsWith("---")) return;
									tfa.setDataItemIsUnselected(t_pos);
									btn_ok.setEnabled(false);
								}
						  	});
			  			} else {
				  			ccMenu.addMenuItem(mContext.getString(R.string.msgs_file_select_select_this_entry)
									+" "+tfa.getDataItem(t_pos).getPath()+
									tfa.getDataItem(t_pos).getName())
							.setOnClickListener(new CustomContextMenuOnClickListener() {
						  		@Override
								public void onClick(CharSequence menuTitle) {
						    		final TreeFilelistItem tfi=tfa.getDataItem(t_pos);
									if (tfi.getName().startsWith("---")) return;
									tfa.setDataItemIsSelected(t_pos);
									btn_ok.setEnabled(true);
								}
						  	});
			  			}
						ccMenu.createMenu();
						return false;
					}
				});
				NotifyEvent cb_ntfy=new NotifyEvent(mContext);
				// set file list thread response listener 
				cb_ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context c,Object[] o) {
						if (o!=null) {
							int pos=(Integer)o[0];
							if (tfa.getDataItem(pos).isChecked()) btn_ok.setEnabled(true);
						}
					}
					@Override
					public void negativeResponse(Context c,Object[] o) {
						btn_ok.setEnabled(false);
						for (int i=0;i<tfa.getDataItemCount();i++) {
							if (tfa.getDataItem(i).isChecked()) {
								btn_ok.setEnabled(true);
								break;
							}
						}
					}
				});
				tfa.setCbCheckListener(cb_ntfy);

			    //OKボタンの指定
				btn_ok.setEnabled(false);
			    btn_ok.setVisibility(Button.VISIBLE);
			    btn_ok.setOnClickListener(new View.OnClickListener() {
			        public void onClick(View v) {
			            String sel="";
			            for (int i=0;i<tfa.getCount();i++) {
			            	if (tfa.getDataItem(i).isChecked() && 
			            			!tfa.getDataItem(i).getName().equals(msgs_dir_empty)) {
			            		if (tfa.getDataItem(i).getPath().length()==1) 
				            		sel=tfa.getDataItem(i).getName();
			            		else sel=tfa.getDataItem(i).getPath()
			            				.substring(1,tfa.getDataItem(i).getPath().length())+
			            				tfa.getDataItem(i).getName();
			            		break;
			            	}
			            }
			            dialog.dismiss();
			            p_ntfy.notifyToListener(true,new Object[]{sel} );
			        }
			    });
			    //CANCELボタンの指定
			    final Button btn_cancel=(Button)dialog.findViewById(R.id.item_select_list_dlg_cancel_btn);
			    btn_cancel.setOnClickListener(new View.OnClickListener() {
			        public void onClick(View v) {
			            dialog.dismiss();
			            p_ntfy.notifyToListener(false, null);
			        }
			    });
				// Cancelリスナーの指定
				dialog.setOnCancelListener(new Dialog.OnCancelListener() {
					@Override
					public void onCancel(DialogInterface arg0) {
						btn_cancel.performClick();
					}
				});

//			    dialog.setOnKeyListener(new DialogOnKeyListener(context));
//			    dialog.setCancelable(false);
			    dialog.show();
			}

			@Override
			public void negativeResponse(Context c,Object[] o) {
				p_ntfy.notifyToListener(false, o);
			}
		});
		createRemoteFileList(remurl,curdir,ntfy,true);
        return ;
	};

	private void expandHideRemoteDirTree(String remurl, final int pos, 
			final TreeFilelistItem tfi, final TreeFilelistAdapter tfa) {
		if (tfi.getSubDirItemCount()==0) return;
		if(tfi.isChildListExpanded()) {
			tfa.hideChildItem(tfi,pos);
		} else {
			if (tfi.isSubDirLoaded()) 
				tfa.reshowChildItem(tfi,pos);
			else {
				if (tfi.isSubDirLoaded()) 
					tfa.reshowChildItem(tfi,pos);
				else {
					NotifyEvent ne=new NotifyEvent(mContext);
					ne.setListener(new NotifyEventListener() {
						@SuppressWarnings("unchecked")
						@Override
						public void positiveResponse(Context c,Object[] o) {
							tfa.addChildItem(tfi,(ArrayList<TreeFilelistItem>)o[0],pos);
						}
						@Override
						public void negativeResponse(Context c,Object[] o) {}
					});
					createRemoteFileList(remurl,tfi.getPath()+tfi.getName()+"/",ne,true);
				}
			}
		}
	};
	private void expandHideLocalDirTree(boolean dironly,String lclurl, final int pos, 
			final TreeFilelistItem tfi, final TreeFilelistAdapter tfa) {
		if (tfi.getSubDirItemCount()==0) return;
		if(tfi.isChildListExpanded()) {
			tfa.hideChildItem(tfi,pos);
		} else {
			if (tfi.isSubDirLoaded()) 
				tfa.reshowChildItem(tfi,pos);
			else {
				if (tfi.isSubDirLoaded()) tfa.reshowChildItem(tfi,pos);
				else {
					ArrayList<TreeFilelistItem> ntfl =
							createLocalFilelist(dironly,lclurl,tfi.getPath()+tfi.getName());
					tfa.addChildItem(tfi,ntfl,pos);
				}
			}
		}
	};
	
	public AdapterProfileList createProfileList(boolean sdcard, String fp) {
		AdapterProfileList pfl;
		
		errorCreateProfileList=false;
		
		ArrayList<ProfileListItem> sync = new ArrayList<ProfileListItem>();
		ArrayList<ProfileListItem> rem = new ArrayList<ProfileListItem>();
		ArrayList<ProfileListItem> lcl = new ArrayList<ProfileListItem>();
		
		importedSettingParmList.clear();

		if (sdcard) {
			File sf = new File(fp);
			if (sf.exists()) {
				CipherParms cp=null;
				boolean prof_encrypted=isProfileWasEncrypted(fp);
				if (prof_encrypted) {
					cp=EncryptUtil.initDecryptEnv(
							mProfilePasswordPrefix+mProfilePassword);
				}
				try {
					BufferedReader br;
					br = new BufferedReader(new FileReader(fp),8192);
					String pl;
					while ((pl = br.readLine()) != null) {
						if (pl.startsWith(SMBSYNC_PROF_VER1) || pl.startsWith(SMBSYNC_PROF_VER2)) {
							addProfileList(pl, sync, rem, lcl);
						} else if (pl.startsWith(SMBSYNC_PROF_VER3) || 
								pl.startsWith(SMBSYNC_PROF_VER4) ||
								pl.startsWith(SMBSYNC_PROF_VER5)) {
							String prof_pre="";
							if (pl.startsWith(SMBSYNC_PROF_VER3)) prof_pre=SMBSYNC_PROF_VER3;
							else if (pl.startsWith(SMBSYNC_PROF_VER4)) prof_pre=SMBSYNC_PROF_VER4;
							else if (pl.startsWith(SMBSYNC_PROF_VER5)) prof_pre=SMBSYNC_PROF_VER5;
							if (!pl.startsWith(prof_pre+SMBSYNC_PROF_ENC) &&
									!pl.startsWith(prof_pre+SMBSYNC_PROF_DEC)) {
								if (prof_encrypted) {
									String enc_str=pl.replace(prof_pre, "");
									byte[] enc_array=Base64Compat.decode(enc_str, Base64Compat.NO_WRAP);
									String dec_str=EncryptUtil.decrypt(enc_array, cp);
									addProfileList(prof_pre+dec_str, sync, rem, lcl);
								} else {
									addProfileList(pl, sync, rem, lcl);
								}
							}
						}
					}
					br.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
					errorCreateProfileList=true;
					util.addLogMsg("E",String.format(msgs_create_profile_error,fp));
					util.addLogMsg("E",e.toString());
				} catch (IOException e) {
					e.printStackTrace();
					errorCreateProfileList=true;
					util.addLogMsg("E",String.format(msgs_create_profile_error,fp));
					util.addLogMsg("E",e.toString());
				}
			} else {
				errorCreateProfileList=true;
				util.addLogMsg("E",String.format(msgs_create_profile_not_found,fp));
			}

		} else {
			BufferedReader br;
			String pf = SMBSYNC_PROFILE_FILE_NAME_V0; 
			try {
				File lf1= new File(mGp.SMBSync_Internal_Root_Dir+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V1);
				File lf2= new File(mGp.SMBSync_Internal_Root_Dir+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V2);
				File lf3= new File(mGp.SMBSync_Internal_Root_Dir+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V3);
				File lf4= new File(mGp.SMBSync_Internal_Root_Dir+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V4);
				File lf5= new File(mGp.SMBSync_Internal_Root_Dir+"/"+
						SMBSYNC_PROFILE_FILE_NAME_V5);
				if (lf5.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V5;
				else if (lf4.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V4;
				else if (lf3.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V3;
				else if (lf2.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V2; 
				else if (lf1.exists()) pf=SMBSYNC_PROFILE_FILE_NAME_V1;
				else pf=SMBSYNC_PROFILE_FILE_NAME_V0;
				
				File lf= new File(mGp.SMBSync_Internal_Root_Dir+"/"+pf);
				
				if (lf.exists()) {
					br = new BufferedReader(
							new FileReader(mGp.SMBSync_Internal_Root_Dir+"/"+pf),8192); 
	//				InputStream in = context.openFileInput(SMBSYNC_PROFILE_FILE_NAME);
	//				BufferedReader br = new BufferedReader(new InputStreamReader(
	//						in, "UTF-8"));
					String pl;
					while ((pl = br.readLine()) != null) {
						addProfileList(pl, sync, rem, lcl);
					}
					br.close();
				} else {
					util.addDebugLogMsg(1, "W", 
							"profile not found, empty profile list created. fn="+
									mGp.SMBSync_Internal_Root_Dir+"/"+pf);
				}
			} catch (IOException e) {
				e.printStackTrace();
				util.addLogMsg("E",String.format(msgs_create_profile_error,pf));
				util.addLogMsg("E",e.toString());
				errorCreateProfileList=true;
			}
		}

		Collections.sort(sync);
		Collections.sort(rem);
		Collections.sort(lcl);
		sync.addAll(rem);
		sync.addAll(lcl); 

		pfl = new AdapterProfileList(mContext, R.layout.profile_list_item_view, sync,"");
		for (int i=0;i<pfl.getCount();i++) {
			ProfileListItem item = pfl.getItem(i);
			if (item.getMasterType().equals("")) {
				item.setMasterType(getProfileType(item.getMasterName(), pfl));
				item.setTargetType(getProfileType(item.getTargetName(), pfl));
				pfl.replace(item, i);
			}
		}

		if (pfl.getCount() == 0) {
			if (BUILD_FOR_AMAZON) {
				//アマゾン用はサンプルプロファイルを作成しない
				pfl.add(new ProfileListItem("","",
						mContext.getString(R.string.msgs_no_profile_entry),
						"","",null,false));
			} else {
				if (mGp.sampleProfileCreateRequired) {
					createSampleProfile(pfl);
					saveProfileToFile(false,"","",pfl,false);
					mGp.sampleProfileCreateRequired=false;
				} else {
					pfl.add(new ProfileListItem("","",
							mContext.getString(R.string.msgs_no_profile_entry),
							"","",null,false));
				}
			}
		}
		return pfl;
	};

	private void createSampleProfile(AdapterProfileList pfl) {
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_SYNC,"S-SAMP-DOWNLOAD", SMBSYNC_PROF_ACTIVE,
				SMBSYNC_SYNC_TYPE_MIRROR,"R","R-SAMP-DOWNLOAD",
				"L","L-SAMP-DOWNLOAD",new ArrayList<String>(), 
				new ArrayList<String>(), true, true,false,false,false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_LOCAL,"L-SAMP-DOWNLOAD", SMBSYNC_PROF_ACTIVE, 
				mGp.SMBSync_External_Root_Dir,"SAMPLEDIR", false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_REMOTE,"R-SAMP-DOWNLOAD", SMBSYNC_PROF_ACTIVE, 
				"TESTUSER","PSWD","192.168.0.2","","","SHARE", "RDIR", 
				false));
		ArrayList<String> ff1=new ArrayList<String>();
		ArrayList<String> df1=new ArrayList<String>();
		ArrayList<String> ff2=new ArrayList<String>();
		ArrayList<String> df2=new ArrayList<String>();
		df1.add("E.thumbnails");
		ff1.add("I*.jpg");
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_SYNC,"S-SAMP-UPLOAD-WITH-FILTER", SMBSYNC_PROF_ACTIVE,
				SMBSYNC_SYNC_TYPE_MIRROR,"L","L-SAMP-UPLOAD",
				"R","R-SAMP-UPLOAD",ff1,df1, true, true,false,false,false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_LOCAL,"L-SAMP-UPLOAD", SMBSYNC_PROF_ACTIVE, 
				mGp.SMBSync_External_Root_Dir,"DCIM", false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_REMOTE,"R-SAMP-UPLOAD", SMBSYNC_PROF_ACTIVE, 
				"TESTUSER","PSWD","192.168.0.2","","","SHARE", "DCIM", 
				false));
		
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_SYNC,"S-SAMP-LOCAL-LOCAL", SMBSYNC_PROF_ACTIVE,
				SMBSYNC_SYNC_TYPE_MIRROR,"L","L-SAMP-LOCAL",
				"L","L-SAMP-USBDISK",ff2,df2, true, true,false,false,false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_LOCAL,"L-SAMP-LOCAL", SMBSYNC_PROF_ACTIVE, 
				mGp.SMBSync_External_Root_Dir,"DCIM", false));
		pfl.add(new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_LOCAL,"L-SAMP-USBDISK", SMBSYNC_PROF_ACTIVE, 
				"/mnt/usbdisk","usb", false));
		pfl.sort();
	};
	
	public void addProfileList(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		String profVer="";
		if (pl.length()>7) profVer=pl.substring(0, 6);
		if (profVer.equals(SMBSYNC_PROF_VER1)) {
			if (pl.length()>10){
				addProfileListVer1(pl.substring(7,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl);
			}
		} else if (profVer.equals(SMBSYNC_PROF_VER2)) {
				if (pl.length()>10){
					addProfileListVer2(pl.substring(7,pl.length()),sync,rem,lcl);
					addImportSettingsParm(pl);
				}
		} else if (profVer.equals(SMBSYNC_PROF_VER3)) {
			if (pl.length()>10){
				addProfileListVer3(pl.substring(6,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl);
			}
		} else if (profVer.equals(SMBSYNC_PROF_VER4)) {
			if (pl.length()>10){
				addProfileListVer4(pl.substring(6,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl);
			}
		} else if (profVer.equals(SMBSYNC_PROF_VER5)) {
			if (pl.length()>10){
				addProfileListVer5(pl.substring(6,pl.length()),sync,rem,lcl);
				addImportSettingsParm(pl);
			}
		} else addProfileListVer0(pl, sync, rem, lcl);
	};
	
	public void addProfileListVer0(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {

		String prof_group=SMBSYNC_PROF_GROUP_DEFAULT;
		
		String[] tmp_pl=pl.split(",");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=tmp_pl[i];
			}
		}
		if (parm[0].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			rem.add(createRemoteProfilelistItem(
					prof_group,// group
					parm[1],//Name
					parm[2],//Active
					parm[7],//directory
					parm[5],//user
					parm[6],//pass
					parm[4],//share
					parm[3],//address
					"",		//hostname
					"",//port
					false));
		} else {
			if (parm[0].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				lcl.add(createLocalProfilelistItem(
						prof_group,// group
						parm[1],//Name
						parm[2],//Active
						parm[3],//Directory
						mGp.SMBSync_External_Root_Dir,
						false));
			} else if (parm[0].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (parm[6].length()!=0) ff.add("IF"+parm[6]);
				if (parm[7].length()!=0) ff.add("IF"+parm[7]);
				if (parm[8].length()!=0) ff.add("IF"+parm[8]);
				sync.add(createSyncProfilelistItem(
						prof_group,// group
						parm[1],//Name
						parm[ 2],//Active
						parm[ 5],//Sync type
						"",//Master type
						parm[ 3],//Master name
						"",//Target type
						parm[ 4],//Target name
						ff,//File Filter
						df,//Dir Filter
						true,
						false,
						true,
						false,
						false));
			}
		}
	};

	public void addProfileListVer1(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		if (pl.startsWith(SMBSYNC_PROF_VER1+","+"SETTINGS")) return; //ignore settings entry
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i]);
			}
		}
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			rem.add(createRemoteProfilelistItem(
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					parm[6],//address
					"",		//hostname
					"",//port
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.SMBSync_External_Root_Dir;
				lcl.add(createLocalProfilelistItem(
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				sync.add(createSyncProfilelistItem(
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						false,
						false));
			}
		}
	};

	public void addProfileListVer2(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		if (pl.startsWith(SMBSYNC_PROF_VER2+","+"SETTINGS")) return; //ignore settings entry
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i]);
			}
		}
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			rem.add(createRemoteProfilelistItem(
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					parm[6],//address
					parm[9],//hostname
					"",//port
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.SMBSync_External_Root_Dir;
				lcl.add(createLocalProfilelistItem(
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				sync.add(createSyncProfilelistItem(
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						false,
						false));
			}
		}
	};

	public void addProfileListVer3(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i].trim());
			}
		}
		if (parm[1].equals("SETTINGS")) return; //ignore settings entry
		
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			String h_addr="", h_name="";
			if (parm[6].length()>0) {
				if (parm[6].substring(0,1).compareTo("0")>=0 && parm[6].substring(0,1).compareTo("9")<=0) {
					h_addr=parm[6];
				} else {
					h_name=parm[6];
				}
			} else {
				h_addr="";
				h_name=parm[9];
			}
//			Log.v("","h_addr="+h_addr+", h_name="+h_name);
			rem.add(createRemoteProfilelistItem(
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					h_addr,//address
					h_name,//hostname
					"",//port
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.SMBSync_External_Root_Dir;
				lcl.add(createLocalProfilelistItem(
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				sync.add(createSyncProfilelistItem(
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						false,
						false));
			}
		}
	};

	public void addProfileListVer4(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i].trim());
			}
		}
		if (parm[1].equals("SETTINGS")) return; //ignore settings entry
		
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			String h_addr="", h_name="";
			if (parm[6].length()>0) {
				if (parm[6].substring(0,1).compareTo("0")>=0 && parm[6].substring(0,1).compareTo("9")<=0) {
					h_addr=parm[6];
				} else {
					h_name=parm[6];
				}
			} else {
				h_addr="";
				h_name=parm[9];
			}
//			Log.v("","h_addr="+h_addr+", h_name="+h_name);
			rem.add(createRemoteProfilelistItem(
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					h_addr,//address
					h_name,//hostname
					"",//port
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.SMBSync_External_Root_Dir;
				lcl.add(createLocalProfilelistItem(
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false, nulm_remote=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				if (parm[12].equals("1")) nulm_remote=true;
				sync.add(createSyncProfilelistItem(
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						nulm_remote,
						false));
			}
		}
	};

	public void addProfileListVer5(String pl, ArrayList<ProfileListItem> sync,
			ArrayList<ProfileListItem> rem, ArrayList<ProfileListItem> lcl) {
		//Extract ArrayList<String> field
		String list1="",list2="", npl="";
		if (pl.indexOf("[")>=0) {
			// found first List
			list1=pl.substring(pl.indexOf("[")+1, pl.indexOf("]"));
			npl=pl.replace("["+list1+"]\t", "");
			if (npl.indexOf("[")>=0) {
				// found second List
				list2=npl.substring(npl.indexOf("[")+1, npl.indexOf("]"));
				npl=npl.replace("["+list2+"]\t", "");
			}
		} else npl=pl;
//		Log.v("","pl="+pl);
//		String prof_group = npl.substring(0,11).trim();
//		String tmp_ps=npl.substring(12,npl.length());

		String[] tmp_pl=npl.split("\t");// {"type","name","active",options...};
		String[] parm= new String[30];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=convertToSpecChar(tmp_pl[i].trim());
			}
//			Log.v("","i="+i+", "+parm[i]);
		}
		if (parm[1].equals("SETTINGS")) return; //ignore settings entry
		
		if (parm[1].equals(SMBSYNC_PROF_TYPE_REMOTE)) {//Remote
			String h_addr="", h_name="";
			if (parm[6].length()>0) {
				if (parm[6].substring(0,1).compareTo("0")>=0 && parm[6].substring(0,1).compareTo("9")<=0) {
					h_addr=parm[6];
				} else {
					h_name=parm[6];
				}
			} else {
				h_addr="";
				h_name=parm[9];
			}
//			Log.v("","h_addr="+h_addr+", h_name="+h_name);
			rem.add(createRemoteProfilelistItem(
					parm[0],//group
					parm[2],//Name
					parm[3],//Active
					parm[8],//directory
					parm[4],//user
					parm[5],//pass
					parm[7],//share
					h_addr,//address
					h_name,//hostname
					parm[10],//port
					false));

		} else {
			if (parm[1].equals(SMBSYNC_PROF_TYPE_LOCAL)) {//Local
				if (parm[5].equals("")) parm[5]=mGp.SMBSync_External_Root_Dir;
				lcl.add(createLocalProfilelistItem(
						parm[0],//group
						parm[2],//Name
						parm[3],//Active
						parm[4],//Directory
						parm[5],//Local mount point
						false));
			} else if (parm[1].equals(SMBSYNC_PROF_TYPE_SYNC)) {//Sync
				ArrayList<String> ff=new ArrayList<String>();
				ArrayList<String> df=new ArrayList<String>();
				if (list1.length()!=0) {
					String[] fp=list1.split("\t");
					for (int i=0;i<fp.length;i++) ff.add(convertToSpecChar(fp[i]));					
				} else ff.clear();
				if (list2.length()!=0) {
					String[] dp=list2.split("\t");
					for (int i=0;i<dp.length;i++) df.add(convertToSpecChar(dp[i]));
				} else df.clear();
				boolean mpd=true, conf=false, ujlm=false, nulm_remote=false;
				if (parm[9].equals("0")) mpd=false;
				if (parm[10].equals("1")) conf=true;
				if (parm[11].equals("1")) ujlm=true;
				if (parm[12].equals("1")) nulm_remote=true;
				sync.add(createSyncProfilelistItem(
						parm[0],//group
						parm[ 2],//Name
						parm[ 3],//Active
						parm[ 4],//Sync type
						parm[ 5],//Master type
						parm[ 6],//Master name
						parm[ 7],//Target type
						parm[ 8],//Target name
						ff,//File Filter
						df,//Dir Filter
						mpd,
						conf,
						ujlm,
						nulm_remote,
						false));
			}
		}
	};

	private String convertToSpecChar(String in) {
		if (in==null || in.length()==0) return "";
		boolean cont=true;
		String out=in;
		while (cont) {
			if (out.indexOf("\u0001")>=0) out=out.replace("\u0001","[") ;
			else cont=false;
		}

		cont=true;
		while (cont) {
			if (out.indexOf("\u0002")>=0) out=out.replace("\u0002","]") ;
			else cont=false;
		}
		
		return out;
	};
	
	private String convertToCodeChar(String in) {
		if (in==null || in.length()==0) return "";
		boolean cont=true;
		String out=in;
		while (cont) {
			if (out.indexOf("[")>=0) out=out.replace("[","\u0001") ;
			else cont=false;
		}

		cont=true;
		while (cont) {
			if (out.indexOf("]")>=0) out=out.replace("]","\u0002") ;
			else cont=false;
		}
		return out;
	};

	private void updateSyncProfileAdapter(String prof_name, 
			String prof_act,String prof_syncopt, String prof_master_typ,String prof_master,
			String prof_target_typ,String prof_target, 
			ArrayList<String> file_filter, ArrayList<String> dir_filter,
			boolean prof_mpd, boolean prof_conf, boolean prof_ujlm, boolean nulm_remote,
			boolean isChk, int pos) {
		String prof_group=SMBSYNC_PROF_GROUP_DEFAULT;
		boolean isExists=isProfileExists(prof_group,
				 SMBSYNC_PROF_TYPE_SYNC, prof_name);
		if (!isExists) {
			ProfileListItem pfli=createSyncProfilelistItem(prof_group,prof_name,prof_act,
					prof_syncopt,prof_master_typ,prof_master,prof_target_typ,prof_target,
					file_filter, dir_filter,prof_mpd,prof_conf,prof_ujlm,nulm_remote,isChk);
			mGp.profileAdapter.add(pfli);
		} else {
//			glblParms.profileAdapter.remove(glblParms.profileAdapter.getItem(pos));
			ProfileListItem pfli=createSyncProfilelistItem(prof_group,prof_name,prof_act,
					prof_syncopt,prof_master_typ,prof_master,prof_target_typ,prof_target,
					file_filter, dir_filter, prof_mpd,prof_conf,prof_ujlm,nulm_remote,isChk);
			mGp.profileAdapter.replace(pfli,pos);
		}
	};
	
	private ProfileListItem createSyncProfilelistItem(String prof_group,String prof_name, 
			String prof_act,String prof_syncopt, String prof_master_typ,String prof_master,
			String prof_target_typ,String prof_target, 
			ArrayList<String> ff, ArrayList<String> df,boolean prof_mpd, 
			boolean prof_conf, boolean prof_ujlm, boolean nulm_remote, boolean isChk) {
		return new ProfileListItem(prof_group,SMBSYNC_PROF_TYPE_SYNC,prof_name,prof_act,
				prof_syncopt,
				prof_master_typ,
				prof_master,
				prof_target_typ,
				prof_target,
				ff,
				df,
				prof_mpd,
				prof_conf,
				prof_ujlm,
				nulm_remote,
				isChk);
	};

	private void updateRemoteProfileAdapter(boolean isAdd, String prof_name, 
			String prof_act,String prof_dir, String prof_user, String prof_pass, 
			String prof_share, String prof_addr, String prof_host, String prof_port,
			boolean isChk,int pos) {
		String prof_group=SMBSYNC_PROF_GROUP_DEFAULT;
		if (isAdd) {
			mGp.profileAdapter.add(createRemoteProfilelistItem(prof_group, prof_name,prof_act,
					prof_dir,prof_user,prof_pass,prof_share,prof_addr,prof_host,prof_port,isChk));
		} else {
//			glblParms.profileAdapter.remove(glblParms.profileAdapter.getItem(pos));
			mGp.profileAdapter.replace(createRemoteProfilelistItem(prof_group, prof_name,prof_act,
					prof_dir,prof_user,prof_pass,prof_share,prof_addr,prof_host,prof_port,isChk),pos);
		}

	};
	private ProfileListItem createRemoteProfilelistItem(String prof_group, String prof_name, 
			String prof_act,String prof_dir, String prof_user, String prof_pass, 
			String prof_share, String prof_addr, String prof_host, String prof_port,boolean isChk) {
		return new ProfileListItem(prof_group,SMBSYNC_PROF_TYPE_REMOTE,prof_name,prof_act,
				prof_user,
				prof_pass,
				prof_addr,
				prof_host,
				prof_port,
				prof_share,
				prof_dir,
				
				isChk);
	};

	private void updateLocalProfileAdapter(boolean isAdd, String prof_name, 
			String prof_act, String prof_lmp, String prof_dir, 
			boolean isChk,int pos) {
		String prof_group=SMBSYNC_PROF_GROUP_DEFAULT;
		if (isAdd) {
			mGp.profileAdapter.add(createLocalProfilelistItem(prof_group,prof_name,
					prof_act, prof_dir, prof_lmp, isChk));
		} else {
			mGp.profileAdapter.replace(
					createLocalProfilelistItem(prof_group,prof_name,prof_act,
							prof_dir, prof_lmp, isChk),pos);
		}
	};

	private ProfileListItem createLocalProfilelistItem(String prof_group,
			String prof_name, String prof_act, String prof_dir, 
			String prof_lmp, boolean isChk) {
		return new ProfileListItem(prof_group,SMBSYNC_PROF_TYPE_LOCAL,
				prof_name,prof_act, prof_lmp, prof_dir,isChk);
	};

	public static String getProfileType(String pfn, AdapterProfileList pa) {
		for (int i=0;i<pa.getCount();i++)
			if (pa.getItem(i).getName().equals(pfn)) 
				return pa.getItem(i).getType(); 
		return "";
	};

	public static ProfileListItem getProfile(String pfn, AdapterProfileList pa) {
		for (int i=0;i<pa.getCount();i++)
			if (pa.getItem(i).getName().equals(pfn)) 
				return pa.getItem(i); 
		return null;
	};

	public boolean saveProfileToFile(boolean sdcard, String fd, String fp,
			AdapterProfileList pfl, boolean encrypt_required) {
		boolean result=true;
		String ofp="";
		PrintWriter pw;
		BufferedWriter bw=null;
		try {
			CipherParms cp=null;
			if (sdcard) {
				if (encrypt_required) {
					cp=EncryptUtil.initEncryptEnv(mProfilePasswordPrefix+mProfilePassword);
				}
				File lf=new File(fd);
				if (!lf.exists()) lf.mkdir();
				bw = new BufferedWriter(new FileWriter(fp),8192);
				pw = new PrintWriter(bw);
				ofp=fp;
				if (encrypt_required) {
					byte[] enc_array=EncryptUtil.encrypt(SMBSYNC_PROF_ENC, cp); 
					String enc_str = 
							Base64Compat.encodeToString(enc_array, Base64Compat.NO_WRAP);
//					MiscUtil.hexString("", enc_array, 0, enc_array.length);
					pw.println(SMBSYNC_PROF_VER5+SMBSYNC_PROF_ENC+enc_str);
				}
				else pw.println(SMBSYNC_PROF_VER5+SMBSYNC_PROF_DEC);
			} else {
//				OutputStream out = context.openFileOutput(SMBSYNC_PROFILE_FILE_NAME,
//						Context.MODE_PRIVATE);
//				pw = new PrintWriter(new OutputStreamWriter(out, "UTF-8"));
//				ofp=SMBSYNC_PROFILE_FILE_NAME;
				ofp=mGp.SMBSync_Internal_Root_Dir+"/"+SMBSYNC_PROFILE_FILE_NAME_V5;
				File lf=new File(mGp.SMBSync_Internal_Root_Dir);
				if (!lf.exists()) lf.mkdir();
				bw =new BufferedWriter(new FileWriter(ofp),8192);
				pw = new PrintWriter(bw);
			}

			if (pfl.getCount() > 0) {
				String pl;
				for (int i = 0; i < pfl.getCount(); i++) {
					ProfileListItem item = pfl.getItem(i);
					String pl_name=convertToCodeChar(item.getName());
					String pl_active=item.getActive();
					String pl_lmp=convertToCodeChar(item.getLocalMountPoint());
					String pl_dir=convertToCodeChar(item.getDir());
					String pl_user=convertToCodeChar(item.getUser());
					String pl_pass=convertToCodeChar(item.getPass());
					String pl_addr=convertToCodeChar(item.getAddr());
					String pl_host=convertToCodeChar(item.getHostname());
					String pl_share=convertToCodeChar(item.getShare());
					String pl_port=item.getPort();
					String pl_synctype=item.getSyncType();
					String pl_mastertype=item.getMasterType();
					String pl_targettype=item.getTargetType();
					String pl_mastername=convertToCodeChar(item.getMasterName());
					String pl_targetname=convertToCodeChar(item.getTargetName());
					if (!item.getType().equals("")) {
						pl = "";
						if (item.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) {
							pl =item.getGroup()+"\t"+
									SMBSYNC_PROF_TYPE_LOCAL+ "\t" + pl_name + "\t"
									+ pl_active + "\t" +pl_dir+"\t"+
									pl_lmp+"\t";
						} else if (item.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) {
							pl =item.getGroup()+"\t"+
									SMBSYNC_PROF_TYPE_REMOTE+ "\t" + pl_name + "\t"
									+ pl_active + "\t" +
									pl_user+"\t" +
									pl_pass+"\t" +
									pl_addr+ "\t" +
									pl_share+"\t" +
									pl_dir+"\t" +
									pl_host+ "\t"+
									pl_port+ "\t"
									;
						} else if (item.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) {
							String fl="", dl="";
							for (int j=0;j<item.getFileFilter().size();j++) {
								if (fl.length()!=0) fl+="\t"; 
								if (!item.getFileFilter().get(j).equals("")) 
									fl+=item.getFileFilter().get(j); 
							}
							fl=convertToCodeChar(fl);
							fl="["+fl+"]";
							for (int j=0;j<item.getDirFilter().size();j++) {
								if (dl.length()!=0) dl+="\t";
								if (!item.getDirFilter().get(j).equals("")) 
									dl+=item.getDirFilter().get(j); 
							}
							dl=convertToCodeChar(dl);
							dl="["+dl+"]";

							String mpd="1";
							if (!item.isMasterDirFileProcess()) mpd="0";
							String conf="1";
							if (!item.isConfirmRequired()) conf="0";
							String ujlm="1";
							if (!item.isForceLastModifiedUseSmbsync()) ujlm="0";
							String nulm_remote="0";
							if (item.isNotUseLastModifiedForRemote()) nulm_remote="1";
							pl =item.getGroup()+"\t"+
									SMBSYNC_PROF_TYPE_SYNC+ "\t" + pl_name + "\t"+
									pl_active + "\t" +
									pl_synctype+"\t" +
									pl_mastertype+"\t" +
									pl_mastername+"\t" +
									pl_targettype+"\t" +
									pl_targetname+"\t" +
									fl+"\t" +
									dl+"\t"+
									mpd+"\t"+
									conf+"\t"+
									ujlm+"\t"+
									nulm_remote;
						}
						util.addDebugLogMsg(9,"I","saveProfileToFile=" + pl);
						if (sdcard) {
							if (encrypt_required) {
								String enc = 
										Base64Compat.encodeToString(
											EncryptUtil.encrypt(pl, cp), 
											Base64Compat.NO_WRAP);
								pw.println(SMBSYNC_PROF_VER5+enc);
							} else {
								pw.println(SMBSYNC_PROF_VER5+pl);
							}
						} else {
							pw.println(SMBSYNC_PROF_VER5+pl);
						}
					}
				}
			}
			saveSettingsParmsToFile(pw,encrypt_required,cp);
			pw.close();
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			util.addLogMsg("E",String.format(msgs_save_to_profile_error,ofp));
			util.addLogMsg("E",e.toString());
			result=false;
		}
		
		return result;
	};
	
	private void addImportSettingsParm(String pl) {
		String tmp_ps=pl.substring(7,pl.length());
		String[] tmp_pl=tmp_ps.split("\t");// {"type","name","active",options...};
		String[] parm= new String[90];
		for (int i=0;i<30;i++) parm[i]="";
		for (int i=0;i<tmp_pl.length;i++) {
			if (tmp_pl[i]==null) parm[i]="";
			else {
				if (tmp_pl[i]==null) parm[i]="";
				else parm[i]=tmp_pl[i];
			}
		}
		if (parm[1].equals(SMBSYNC_PROF_TYPE_SETTINGS)) {
			int newkey=importedSettingParmList.size();
			String[] val = new String[]{parm[2],parm[3],parm[4]};
			importedSettingParmList.put(newkey, val);
		}
	};

	private void saveSettingsParmsToFileString(String group, PrintWriter pw, String dflt,
			boolean encrypt_required, final CipherParms cp, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String k_type, k_val;

		k_val=prefs.getString(key, dflt);
		k_type=SMBSYNC_SETTINGS_TYPE_STRING;
		String k_str=group+"\t"+
				SMBSYNC_PROF_TYPE_SETTINGS+"\t"+key+"\t"+k_type+"\t"+k_val;
		if (encrypt_required) {
			byte[] out=EncryptUtil.encrypt(k_str,cp);
			String enc = Base64Compat.encodeToString(
						out, 
						Base64Compat.NO_WRAP);
			pw.println(SMBSYNC_PROF_VER5+enc);
		} else {
			pw.println(SMBSYNC_PROF_VER5+k_str);
		}
	};
	
	@SuppressWarnings("unused")
	private void saveSettingsParmsToFileInt(String group, PrintWriter pw, int dflt,
			boolean encrypt_required, final CipherParms cp, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String k_type;
		int k_val;

		k_val=prefs.getInt(key, dflt);
		k_type=SMBSYNC_SETTINGS_TYPE_INT;
		String k_str=group+"\t"+
				SMBSYNC_PROF_TYPE_SETTINGS+"\t"+key+"\t"+k_type+"\t"+k_val;
		if (encrypt_required) {
			String enc = Base64Compat.encodeToString(
						EncryptUtil.encrypt(k_str,cp), 
						Base64Compat.NO_WRAP);
			pw.println(SMBSYNC_PROF_VER5+enc);
		} else {
			pw.println(SMBSYNC_PROF_VER5+k_str);
		}
	};
	private void saveSettingsParmsToFileBoolean(String group, PrintWriter pw, boolean dflt,
			boolean encrypt_required, final CipherParms cp, String key) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
		String k_type;
		boolean k_val;

		k_val=prefs.getBoolean(key, dflt);
		k_type=SMBSYNC_SETTINGS_TYPE_BOOLEAN;
		String k_str=group+"\t"+
				SMBSYNC_PROF_TYPE_SETTINGS+"\t"+key+"\t"+k_type+"\t"+k_val;
		if (encrypt_required) {
			String enc = Base64Compat.encodeToString(
						EncryptUtil.encrypt(k_str, cp), 
						Base64Compat.NO_WRAP);
			pw.println(SMBSYNC_PROF_VER5+enc);
		} else {
			pw.println(SMBSYNC_PROF_VER5+k_str);
		}
	};
	
	private void saveSettingsParmsToFile(PrintWriter pw, boolean encrypt_required,
			final CipherParms cp) {
		String group="Default";// 12Bytes
		saveSettingsParmsToFileString(group, pw, "0",    encrypt_required,cp,mContext.getString(R.string.settings_network_wifi_option));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_auto_start));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_auto_term));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_backgroound_execution));
		saveSettingsParmsToFileString(group, pw, "0",    encrypt_required,cp,mContext.getString(R.string.settings_background_termination_notification));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_error_option));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_ui_keep_screen_on));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_wifi_lock));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_remote_file_copy_by_rename));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_local_file_copy_by_rename));

		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_ui_keep_screen_on));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_ui_alternate_ui));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_debug_msg_diplay));
		saveSettingsParmsToFileString(group, pw, "0",    encrypt_required,cp,mContext.getString(R.string.settings_log_option));
		saveSettingsParmsToFileString(group, pw, "0",    encrypt_required,cp,mContext.getString(R.string.settings_log_level));
		saveSettingsParmsToFileString(group, pw, "",     encrypt_required,cp,mContext.getString(R.string.settings_log_dir));
		saveSettingsParmsToFileString(group, pw, "10",   encrypt_required,cp,mContext.getString(R.string.settings_log_generation));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_suppress_warning_mixed_mp));

		saveSettingsParmsToFileString(group, pw, "",     encrypt_required,cp,mContext.getString(R.string.settings_default_user));
		saveSettingsParmsToFileString(group, pw, "",     encrypt_required,cp,mContext.getString(R.string.settings_default_pass));
		saveSettingsParmsToFileString(group, pw, "",     encrypt_required,cp,mContext.getString(R.string.settings_default_addr));

		saveSettingsParmsToFileString(group, pw, "0",    encrypt_required,cp,mContext.getString(R.string.settings_media_store_last_mod_time));
		saveSettingsParmsToFileString(group, pw, "3",    encrypt_required,cp,mContext.getString(R.string.settings_file_diff_time_seconds));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_media_scanner_non_media_files_scan));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_media_scanner_scan_extstg));
		
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_exit_clean));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_exported_profile_encryption));
		
		saveSettingsParmsToFileString(group, pw, "0",    encrypt_required,cp,mContext.getString(R.string.settings_smb_lm_compatibility));
		saveSettingsParmsToFileBoolean(group, pw, false, encrypt_required,cp,mContext.getString(R.string.settings_smb_use_extended_security));
		
		saveSettingsParmsToFileString(group, pw, "0",    encrypt_required,cp,mContext.getString(R.string.settings_smb_perform_class));
		
		saveSettingsParmsToFileString(group, pw, "0",    encrypt_required,cp,mContext.getString(R.string.settings_smb_log_level));
		saveSettingsParmsToFileString(group, pw, "",     encrypt_required,cp,mContext.getString(R.string.settings_smb_rcv_buf_size));
		saveSettingsParmsToFileString(group, pw, "",     encrypt_required,cp,mContext.getString(R.string.settings_smb_snd_buf_size));
		saveSettingsParmsToFileString(group, pw, "",     encrypt_required,cp,mContext.getString(R.string.settings_smb_listSize));
		saveSettingsParmsToFileString(group, pw, "",     encrypt_required,cp,mContext.getString(R.string.settings_smb_maxBuffers));
		saveSettingsParmsToFileString(group, pw, "",     encrypt_required,cp,mContext.getString(R.string.settings_io_buffers));
		saveSettingsParmsToFileString(group, pw, "false",encrypt_required,cp,mContext.getString(R.string.settings_smb_tcp_nodelay));
	};

	
	private static String msgs_dir_empty	;
//	private static String msgs_no_profile	;
	private static String msgs_override	;
	private static String msgs_add_local_profile	;
	private static String msgs_add_remote_profile	;
	private static String msgs_add_sync_profile	;
	private static String msgs_copy_local_profile	;
	private static String msgs_copy_remote_profile	;
	private static String msgs_copy_sync_profile	;
	private static String msgs_audit_msgs_address1	;
	private static String msgs_audit_msgs_address2	;
	private static String msgs_current_dir	;
	private static String msgs_delete_following_profile	;
	private static String msgs_audit_msgs_dir1	;
	private static String msgs_duplicate_profile	;
	private static String msgs_edit_local_profile	;
	private static String msgs_edit_remote_profile	;
	private static String msgs_edit_sync_profile	;
	private static String msgs_export_prof_title	;
	private static String msgs_export_prof_success;
	private static String msgs_export_prof_fail;
	private static String msgs_import_prof_fail	;
	private static String msgs_import_prof_fail_no_valid_item;
	private static String msgs_audit_msgs_local_dir	;
	private static String msgs_audit_msgs_master1	;
	private static String msgs_audit_msgs_master2	;
	private static String msgs_audit_msgs_password1	;
	private static String msgs_audit_msgs_profilename1	;
	private static String msgs_audit_msgs_profilename2	;
	private static String msgs_select_export_file	;
	private static String msgs_select_import_file	;
	private static String msgs_select_local_dir	;
	private static String msgs_select_master_profile;
	private static String msgs_select_target_profile;
	private static String msgs_select_remote_share;
	private static String msgs_select_remote_dir;
	private static String msgs_audit_msgs_share1	;
	private static String msgs_audit_msgs_share2	;
	private static String msgs_audit_msgs_master_target	;
	private static String msgs_audit_msgs_sync1	;
	private static String msgs_audit_msgs_target1	;
	private static String msgs_audit_msgs_target2	;
	private static String msgs_audit_msgs_username1	;
	private static String msgs_audit_msgs_user_or_pass_missing	;

   	private static String msgs_filelist_error;
   	private static String msgs_filelist_cancel;
   	
    private static String msgs_create_profile_not_found;
    private static String msgs_create_profile_error;
    private static String msgs_save_to_profile_error;
    
    private static String msgs_audit_share_not_spec;
    
//    private static String msgs_dlg_hardkey_back_button;
    
	public void loadMsgString() {
		
		msgs_audit_share_not_spec=mContext.getString(R.string.msgs_audit_share_not_spec);
		
		msgs_create_profile_not_found=mContext.getString(R.string.msgs_create_profile_not_found);
	    msgs_create_profile_error=mContext.getString(R.string.msgs_create_profile_error);
	    msgs_save_to_profile_error=mContext.getString(R.string.msgs_save_to_profile_error);
		
		msgs_filelist_cancel=	mContext.getString(R.string.msgs_filelist_cancel);
		msgs_filelist_error=	mContext.getString(R.string.msgs_filelist_error);
		
		msgs_dir_empty					=	mContext.getString(R.string.msgs_dir_empty	);
//		msgs_no_profile					=	mContext.getString(R.string.msgs_no_profile	);
		msgs_override					=	mContext.getString(R.string.msgs_override	);
		msgs_add_local_profile			=	mContext.getString(R.string.msgs_add_local_profile	);
		msgs_add_remote_profile			=	mContext.getString(R.string.msgs_add_remote_profile	);
		msgs_add_sync_profile			=	mContext.getString(R.string.msgs_add_sync_profile	);
		msgs_copy_local_profile			=	mContext.getString(R.string.msgs_copy_local_profile	);
		msgs_copy_remote_profile			=	mContext.getString(R.string.msgs_copy_remote_profile	);
		msgs_copy_sync_profile			=	mContext.getString(R.string.msgs_copy_sync_profile	);
		msgs_audit_msgs_address1		=	mContext.getString(R.string.msgs_audit_msgs_address1	);
		msgs_audit_msgs_address2		=	mContext.getString(R.string.msgs_audit_msgs_address2	);
		msgs_current_dir				=	mContext.getString(R.string.msgs_current_dir	);
		msgs_delete_following_profile	=	mContext.getString(R.string.msgs_delete_following_profile	);
		msgs_audit_msgs_dir1			=	mContext.getString(R.string.msgs_audit_msgs_dir1	);
		msgs_duplicate_profile			=	mContext.getString(R.string.msgs_duplicate_profile	);
		msgs_edit_local_profile			=	mContext.getString(R.string.msgs_edit_local_profile	);
		msgs_edit_remote_profile		=	mContext.getString(R.string.msgs_edit_remote_profile	);
		msgs_edit_sync_profile			=	mContext.getString(R.string.msgs_edit_sync_profile	);
		msgs_export_prof_title			=	mContext.getString(R.string.msgs_export_prof_title	);
		msgs_export_prof_success		=	mContext.getString(R.string.msgs_export_prof_success);
		msgs_export_prof_fail		=	mContext.getString(R.string.msgs_export_prof_fail);
		msgs_import_prof_fail		=	mContext.getString(R.string.msgs_import_prof_fail	);
		msgs_import_prof_fail_no_valid_item=mContext.getString(R.string.msgs_import_prof_fail_no_valid_item);
		msgs_audit_msgs_local_dir		=	mContext.getString(R.string.msgs_audit_msgs_local_dir	);
		msgs_audit_msgs_master1			=	mContext.getString(R.string.msgs_audit_msgs_master1	);
		msgs_audit_msgs_master2			=	mContext.getString(R.string.msgs_audit_msgs_master2	);
		msgs_audit_msgs_password1		=	mContext.getString(R.string.msgs_audit_msgs_password1	);
		msgs_audit_msgs_profilename1	=	mContext.getString(R.string.msgs_audit_msgs_profilename1	);
		msgs_audit_msgs_profilename2	=	mContext.getString(R.string.msgs_audit_msgs_profilename2	);
		msgs_select_export_file			=	mContext.getString(R.string.msgs_select_export_file	);
		msgs_select_import_file			=	mContext.getString(R.string.msgs_select_import_file	);
		msgs_select_local_dir			=	mContext.getString(R.string.msgs_select_local_dir	);
		msgs_select_master_profile		=	mContext.getString(R.string.msgs_select_master_profile	);
		msgs_select_target_profile		=	mContext.getString(R.string.msgs_select_target_profile	);
		msgs_select_remote_share		=	mContext.getString(R.string.msgs_select_remote_share);
		msgs_select_remote_dir		=	mContext.getString(R.string.msgs_select_remote_dir);
		msgs_audit_msgs_share1			=	mContext.getString(R.string.msgs_audit_msgs_share1	);
		msgs_audit_msgs_share2			=	mContext.getString(R.string.msgs_audit_msgs_share2	);
		msgs_audit_msgs_master_target	=	mContext.getString(R.string.msgs_audit_msgs_master_target	);
		msgs_audit_msgs_sync1			=	mContext.getString(R.string.msgs_audit_msgs_sync1	);
		msgs_audit_msgs_target1			=	mContext.getString(R.string.msgs_audit_msgs_target1	);
		msgs_audit_msgs_target2			=	mContext.getString(R.string.msgs_audit_msgs_target2	);
		msgs_audit_msgs_username1		=	mContext.getString(R.string.msgs_audit_msgs_username1	);
		msgs_audit_msgs_user_or_pass_missing=	mContext.getString(R.string.msgs_audit_msgs_user_or_pass_missing);
	};

	public class FilterAdapterSort implements Comparator<String>{
        @Override
        public int compare(String s1, String s2){
            return s1.compareTo(s2);
        }
    }

}
