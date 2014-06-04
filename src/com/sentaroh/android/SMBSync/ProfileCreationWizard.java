package com.sentaroh.android.SMBSync;

import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_ACTIVE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_GROUP_DEFAULT;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_LOCAL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_REMOTE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_SYNC;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_COPY;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MIRROR;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MOVE;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.sentaroh.android.Utilities.LocalMountPoint;
import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.NotifyEvent.NotifyEventListener;
import com.sentaroh.android.Utilities.Dialog.CommonDialog;
import com.sentaroh.android.Utilities.Widget.CustomSpinnerAdapter;

public class ProfileCreationWizard {
	class WizardData {
		public ArrayList<Dialog> dialog_list=new ArrayList<Dialog>();

		public WizardProfileData[] prof_node=new WizardProfileData[] {
			new WizardProfileData(), new WizardProfileData()
		};
		
		public String sync_name="";
		public String master_name="";
		public String master_type="";
		public String target_name="";
		public String target_type="";
		public int mirror_type=0;
		public int sync_direction=0;
		public ArrayList<String> file_filter=new ArrayList<String>();
		public ArrayList<String> dir_filter=new ArrayList<String>();
		public boolean process_master_file_dir=true;
		public boolean confirm_required=true;
		public boolean use_system_last_mod=false;
		public boolean not_use_last_mod_remote=false;
	};
	class WizardProfileData {
		public String node_type="";
		
		public String local_dir_name="";
		public String local_mount_point_name="";
		
		public String remote_host_name="";
		public String remote_host_ipaddr="";
		public String remote_user_name="";
		public String remote_user_pass="";
		public String remote_share_name="";
		public String remote_dir_name="";
		public String remote_port="";
		public boolean remote_use_hostname=true;
		public boolean remote_use_user_pass=true;
	};
	
	private WizardData mWizData=null;
	private SMBSyncUtil util=null;
	private Context mContext=null;
	private ProfileMaintenance profMaint=null;
	private AdapterProfileList profileAdapter=null;
	private GlobalParameters glblParms=null;
	private CommonDialog mCommonDlg=null;
	
	public ProfileCreationWizard(GlobalParameters gp, Context c, SMBSyncUtil su, 
			ProfileMaintenance pm, CommonDialog cd, AdapterProfileList pa) {
		glblParms=gp;
		util=su;
		mContext=c;
		profMaint=pm;
		mCommonDlg=cd;
		profileAdapter=pa;
	};
	
	public void wizardMain() {
		mWizData=new WizardData();
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		mWizData.dialog_list.add(dialog);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.sync_wizard_dlg);
		
		final LinearLayout ll_overall=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_overall);
		final LinearLayout ll_local=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_local);
		final LinearLayout ll_remote=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_remote);
		final LinearLayout ll_sync=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_sync);

		final TextView dlg_title=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_title);
		dlg_title.setText(mContext.getString(R.string.msgs_sync_wizard_title)+" - "+
				mContext.getString(R.string.msgs_sync_wizard_title_type_overall));
		
		final Button btn_ok=(Button)dialog.findViewById(R.id.sync_wizard_dlg_ok);
		final Button btn_back=(Button)dialog.findViewById(R.id.sync_wizard_dlg_back);
		final Button btn_cancel=(Button)dialog.findViewById(R.id.sync_wizard_dlg_cancel);
		final Button btn_next=(Button)dialog.findViewById(R.id.sync_wizard_dlg_next);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_msg);

		final CheckBox cb_all_files=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_cb_all_files);
		final CheckBox cb_audio=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_audio);
		final CheckBox cb_image=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_image);
		final CheckBox cb_video=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_video);
		final LinearLayout ll_sel_files=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_process_files_list);
		cb_all_files.setChecked(true);
		ll_sel_files.setVisibility(LinearLayout.GONE);
		cb_all_files.setEnabled(false);
		setProcessedFileSelectCheckBoxEnabled(dialog,false);
		setProcessedFileSelectCheckBoxChecked(dialog, false);
		cb_all_files.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				if (isChecked) {
					dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
					btn_next.setEnabled(true);
					setProcessedFileSelectCheckBoxEnabled(dialog,false);
					setProcessedFileSelectCheckBoxChecked(dialog, false);
					ll_sel_files.setVisibility(LinearLayout.GONE);
				} else {
					ll_sel_files.setVisibility(LinearLayout.VISIBLE);
					setProcessedFileSelectCheckBoxEnabled(dialog,true);
					setProcessedFileSelectCheckBoxChecked(dialog,false);
					if (isProcessedFileSelected(dialog)) {
						btn_next.setEnabled(true);
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
					} else {
						btn_next.setEnabled(false);
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_sync_select_processed_file));						
					}
				}
			}
		});
		
		cb_audio.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				checkFileTypeCB(dialog, isChecked, btn_next, dlg_msg);
			}
		});
		cb_image.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				checkFileTypeCB(dialog, isChecked, btn_next, dlg_msg);
			}
		});
		cb_video.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				checkFileTypeCB(dialog, isChecked, btn_next, dlg_msg);
			}
		});
		
		mWizData.file_filter.clear();
		dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_sync_direction));
		
		btn_ok.setVisibility(Button.GONE);
		btn_back.setVisibility(Button.GONE);

		final EditText et_prof_name = (EditText) dialog.findViewById(R.id.sync_wizard_dlg_name);

		btn_next.setEnabled(false);

		final Spinner spinner_sync_direction=(Spinner) dialog.findViewById(R.id.sync_wizard_dlg_sync_direction);
		setSyncDirectionSpinner(spinner_sync_direction, "");

		ll_overall.setVisibility(LinearLayout.VISIBLE);
		ll_local.setVisibility(LinearLayout.GONE);
		ll_remote.setVisibility(LinearLayout.GONE);
		ll_sync.setVisibility(LinearLayout.GONE);

		spinner_sync_direction.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
				if (pos==1) {
					mWizData.prof_node[0].node_type="R";
					mWizData.prof_node[1].node_type="L";
					mWizData.master_type="R";
					mWizData.target_type="L";
					et_prof_name.setEnabled(true);
					if (et_prof_name.getText().length()==0) 
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_profname));
					else
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
				} else if (pos==2) {
					mWizData.prof_node[0].node_type="L";
					mWizData.prof_node[1].node_type="R";
					mWizData.master_type="L";
					mWizData.target_type="R";
					et_prof_name.setEnabled(true);
					if (et_prof_name.getText().length()==0) 
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_profname));
					else
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
				} else if (pos==3) {
					mWizData.prof_node[0].node_type="L";
					mWizData.prof_node[1].node_type="L";
					mWizData.master_type="L";
					mWizData.target_type="L";
					et_prof_name.setEnabled(true);
					if (et_prof_name.getText().length()==0) 
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_profname));
					else
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
				} else {
					et_prof_name.setEnabled(false);
					dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_sync_direction));
				}
			}
			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
		});
		
		btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				for (int i=0;i<mWizData.dialog_list.size();i++)
					mWizData.dialog_list.get(i).dismiss();
			}
		});
		dialog.setOnCancelListener(new OnCancelListener(){
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		btn_next.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
//				mWizData.prof_name=et_prof_name.getText().toString();
				mWizData.file_filter.clear();
				if (!cb_all_files.isChecked()) {
					if (cb_audio.isChecked()) {
						mWizData.file_filter.add("I*.aac");
						mWizData.file_filter.add("I*.aif");
						mWizData.file_filter.add("I*.aifc");
						mWizData.file_filter.add("I*.aiff");
						mWizData.file_filter.add("I*.kar");
						mWizData.file_filter.add("I*.m3u");
						mWizData.file_filter.add("I*.m4a");
						mWizData.file_filter.add("I*.mid");
						mWizData.file_filter.add("I*.midi");
						mWizData.file_filter.add("I*.mp2");
						mWizData.file_filter.add("I*.mp3");
						mWizData.file_filter.add("I*.mpga");
						mWizData.file_filter.add("I*.ra");
						mWizData.file_filter.add("I*.ram");
						mWizData.file_filter.add("I*.wav");
						mWizData.file_filter.add("I*.wma");
					}
					if (cb_image.isChecked()) {
						mWizData.file_filter.add("I*.bmp");
						mWizData.file_filter.add("I*.cgm");
						mWizData.file_filter.add("I*.djv");
						mWizData.file_filter.add("I*.djvu");
						mWizData.file_filter.add("I*.gif");
						mWizData.file_filter.add("I*.ico");
						mWizData.file_filter.add("I*.ief");
						mWizData.file_filter.add("I*.jpe");
						mWizData.file_filter.add("I*.jpeg");
						mWizData.file_filter.add("I*.jpg");
						mWizData.file_filter.add("I*.pbm");
						mWizData.file_filter.add("I*.pgm");
						mWizData.file_filter.add("I*.png");
						mWizData.file_filter.add("I*.pnm");
						mWizData.file_filter.add("I*.ppm");
						mWizData.file_filter.add("I*.ras");
						mWizData.file_filter.add("I*.rgb");
						mWizData.file_filter.add("I*.svg");
						mWizData.file_filter.add("I*.tif");
						mWizData.file_filter.add("I*.tiff");
						mWizData.file_filter.add("I*.wbmp");
						mWizData.file_filter.add("I*.xbm");
						mWizData.file_filter.add("I*.xpm");
						mWizData.file_filter.add("I*.xwd");
					}
					if (cb_video.isChecked()) {
						mWizData.file_filter.add("I*.avi");
						mWizData.file_filter.add("I*.m4u");
						mWizData.file_filter.add("I*.mov");
						mWizData.file_filter.add("I*.movie");
						mWizData.file_filter.add("I*.mpe");
						mWizData.file_filter.add("I*.mpeg");
						mWizData.file_filter.add("I*.mpg");
						mWizData.file_filter.add("I*.mxu");
						mWizData.file_filter.add("I*.qt");
						mWizData.file_filter.add("I*.wmv");
					}
					Collections.sort(mWizData.file_filter,new Comparator<String>(){
						@Override
						public int compare(String lhs, String rhs) {
							return lhs.compareToIgnoreCase(rhs);
						}
					});
				}
				if (mWizData.prof_node[0].node_type.equals("L")) buildLocalProfile(0);
				else buildRemoteProfile(0);
			}
		});
		
		et_prof_name.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable text) {
				if (text.length()>0) {
					mWizData.sync_name="AG-S-"+text.toString();
					mWizData.master_name="AGM-"+mWizData.master_type+"-"+
							text.toString();
					mWizData.target_name="AGT-"+mWizData.target_type+"-"+
							text.toString();
					
					if (!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_SYNC, mWizData.sync_name) &&
							!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT, mWizData.master_type, mWizData.master_name) &&
							!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT, mWizData.target_type, mWizData.target_name)) {
						cb_all_files.setEnabled(true);
						setProcessedFileSelectCheckBoxEnabled(dialog,true);
						setProcessedFileSelectCheckBoxChecked(dialog, false);
						if (isProcessedFileSelected(dialog)) {
							btn_next.setEnabled(true);
							dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
						} else {
							btn_next.setEnabled(false);
							dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_sync_select_processed_file));
						}
 					} else {
 						cb_all_files.setEnabled(false);
 						setProcessedFileSelectCheckBoxEnabled(dialog,false);
 						setProcessedFileSelectCheckBoxChecked(dialog, false);
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_profname_invalid));
					}
				} else {
					dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_profname));
					btn_next.setEnabled(false);
					cb_all_files.setEnabled(false);
					setProcessedFileSelectCheckBoxEnabled(dialog,false);
					setProcessedFileSelectCheckBoxChecked(dialog, false);
				}
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		dialog.show();
	};

	private void checkFileTypeCB(Dialog dialog,
			boolean isChecked, Button btn_next, TextView dlg_msg) {
		if (isChecked) {
			btn_next.setEnabled(true);
			dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
		} else {
			if (isProcessedFileSelected(dialog)) {
				btn_next.setEnabled(true);
				dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
			} else {
				btn_next.setEnabled(false);
				dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_sync_select_processed_file));
			}
		}

	};
	
	private void setProcessedFileSelectCheckBoxEnabled(Dialog dialog, boolean enabled) {
		final CheckBox cb_audio=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_audio);
		final CheckBox cb_image=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_image);
		final CheckBox cb_video=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_video);
		cb_audio.setEnabled(enabled);
		cb_image.setEnabled(enabled);
		cb_video.setEnabled(enabled);
	};
	
	private void setProcessedFileSelectCheckBoxChecked(Dialog dialog, boolean checked) {
		final CheckBox cb_audio=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_audio);
		final CheckBox cb_image=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_image);
		final CheckBox cb_video=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_video);
		cb_audio.setChecked(checked);
		cb_image.setChecked(checked);
		cb_video.setChecked(checked);
	};
	
	private boolean isProcessedFileSelected(Dialog dialog) {
		final CheckBox cb_audio=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_audio);
		final CheckBox cb_image=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_image);
		final CheckBox cb_video=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_overall_cb_video);
		final CheckBox cb_all_files=(CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_cb_all_files);
		boolean result=false;
		if (cb_all_files.isChecked()) {
			result=true;
		} else {
			if (cb_audio.isChecked() || cb_image.isChecked() || cb_video.isChecked()) {
				result=true;
			}
		}
		return result;
	};
	
	private void buildLocalProfile(final int node_pos) {
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		mWizData.dialog_list.add(dialog);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.sync_wizard_dlg);
		
		final LinearLayout ll_overall=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_overall);
		final LinearLayout ll_local=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_local);
		final LinearLayout ll_remote=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_remote);
		final LinearLayout ll_sync=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_sync);

		final TextView dlg_title=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_title);
		String n_type="";
		if (node_pos==0) n_type=mContext.getString(R.string.msgs_sync_wizard_title_type_master);
		else n_type=mContext.getString(R.string.msgs_sync_wizard_title_type_target);
		dlg_title.setText(mContext.getString(R.string.msgs_sync_wizard_title)+" - "+n_type);
		
		final Button btn_ok=(Button)dialog.findViewById(R.id.sync_wizard_dlg_ok);
		final Button btn_back=(Button)dialog.findViewById(R.id.sync_wizard_dlg_back);
		final Button btn_cancel=(Button)dialog.findViewById(R.id.sync_wizard_dlg_cancel);
		final Button btn_next=(Button)dialog.findViewById(R.id.sync_wizard_dlg_next);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_msg);
		
		final Button btn_local_dir=(Button) dialog.findViewById(R.id.sync_wizard_dlg_local_dir_btn);

		final EditText et_remote_prof=(EditText) dialog.findViewById(R.id.sync_wizard_dlg_local_prof_name);
		et_remote_prof.selectAll();
		if (node_pos==0) et_remote_prof.setText(mWizData.master_name);
		else et_remote_prof.setText(mWizData.target_name);

		final EditText et_local_dir=(EditText) dialog.findViewById(R.id.sync_wizard_dlg_local_dir);
		
		et_local_dir.selectAll();
		et_local_dir.setText("");
		
		final Spinner spinnerLmp=(Spinner) dialog.findViewById(R.id.sync_wizard_dlg_local_spinner_lmp);
		
		btn_ok.setVisibility(Button.GONE);

		dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_local_dir));
		
		ll_overall.setVisibility(LinearLayout.GONE);
		ll_local.setVisibility(LinearLayout.VISIBLE);
		ll_remote.setVisibility(LinearLayout.GONE);
		ll_sync.setVisibility(LinearLayout.GONE);

		profMaint.setLocalMountPointSpinner(spinnerLmp, LocalMountPoint.getExternalStorageDir());
//		et_local_dir.setText(mWizData.prof_node[node_pos].local_dir_name);
		if (node_pos==1 &&
				mWizData.master_type.equals("L") && mWizData.target_type.equals("L") &&
				mWizData.prof_node[0].local_dir_name.equals("")) {
			//remove same mount point
			AdapterLocalMountPoint adapter=(AdapterLocalMountPoint) spinnerLmp.getAdapter();
			String lfm_mpn=mWizData.prof_node[0].local_mount_point_name;
			File lfm=new File(lfm_mpn);
			try {
				String lfm_cp=lfm.getCanonicalPath();
				for (int i=spinnerLmp.getCount()-1;i>=0;i--) {
					String lmp_name=spinnerLmp.getItemAtPosition(i).toString();
					File lft=new File(lmp_name);
					String lft_cp=lft.getCanonicalPath();
					if (lfm_cp.equals(lft_cp)) {
						adapter.remove(lmp_name);
					}
				}
				if (adapter.getCount()==0) {
					adapter.add(lfm_mpn);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		spinnerLmp.setOnItemSelectedListener(new OnItemSelectedListener(){
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1,
					int pos, long arg3) {
//				Log.v("","pos="+node_pos+", master="+mWizData.master_type+", target="+mWizData.target_type);
				if (node_pos==1 &&
						mWizData.master_type.equals("L") && mWizData.target_type.equals("L")) {
					String lfm_path=mWizData.prof_node[0].local_mount_point_name+"/"+mWizData.prof_node[0].local_dir_name;
					String lft_path=spinnerLmp.getSelectedItem().toString()+"/"+et_local_dir.getText().toString();
					if (lfm_path.equals(lft_path)) {
						btn_next.setEnabled(false);
					} else {
						btn_next.setEnabled(false);
						if (!lfm_path.equals(lft_path)) {
							File lfm=new File(lfm_path);
							File lft=new File(lft_path);
							try {
//								Log.v("","lfm_cp="+lfm.getCanonicalPath());
//								Log.v("","lft_cp="+lft.getCanonicalPath());
								if (!lfm.getCanonicalPath().equals(lft.getCanonicalPath()))
									btn_next.setEnabled(true);
							} catch (IOException e) {
								e.printStackTrace();
							}
						} else {
							btn_next.setEnabled(false);
						}
					}
				} else {
					btn_next.setEnabled(true);
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {}
			
		});
		
		// LocalDirボタンの指定
		if (!glblParms.externalStorageIsMounted) btn_local_dir.setEnabled(false);
		btn_local_dir.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String url=(String)spinnerLmp.getSelectedItem();
				String p_dir=et_local_dir.getText().toString();
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						et_local_dir.setText((String)arg1[0]);
					}
					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText("");
					}
				});
				profMaint.setLocalDir(url,"",p_dir,ntfy);
			}
		});
		
		if (mWizData.master_type.equals("L") && mWizData.target_type.equals("L")) {
			if (node_pos==1) {
				btn_next.setEnabled(false);
			}
		}

		et_local_dir.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable text) {
				if (node_pos==0) {
					if (text.length()>0) btn_next.setEnabled(true);
				} else {
					if (mWizData.prof_node[0].local_mount_point_name.equals(spinnerLmp.getSelectedItem().toString())) {
						if (!mWizData.prof_node[0].local_dir_name.equals(et_local_dir.getText().toString())) {
							btn_next.setEnabled(true);
						} else {
							btn_next.setEnabled(false);
						}
					} else {
						btn_next.setEnabled(true);
					}
				}
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		et_remote_prof.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable text) {
				if (node_pos==0) {
					if (text.length()>0) {
						if (text.toString().equals(mWizData.sync_name)) btn_next.setEnabled(false);
						else {
							if (!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_SYNC, text.toString()) &&
									!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_LOCAL, text.toString()) &&
									!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_REMOTE, text.toString()) ) {
								btn_next.setEnabled(true);
							} else btn_next.setEnabled(false);
						}
					} else btn_next.setEnabled(false);
				} else {
					if (text.length()>0) {
						if (text.toString().equals(mWizData.sync_name)) btn_next.setEnabled(false);
						else {
							if (text.toString().equals(mWizData.master_name)) btn_next.setEnabled(false);
							else {
								if (!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_SYNC, text.toString()) &&
										!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_LOCAL, text.toString()) &&
										!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_REMOTE, text.toString()) ) {
									btn_next.setEnabled(true);
								} else btn_next.setEnabled(false);
							}
						}
					} else btn_next.setEnabled(false);
				}
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				for (int i=0;i<mWizData.dialog_list.size();i++)
					mWizData.dialog_list.get(i).dismiss();
			}
		});
		dialog.setOnCancelListener(new OnCancelListener(){
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		btn_back.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				mWizData.dialog_list.remove(dialog);
				dialog.dismiss();
			}
		});
		btn_next.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (node_pos==0) mWizData.master_name=et_remote_prof.getText().toString();
				else mWizData.target_name=et_remote_prof.getText().toString();

				mWizData.prof_node[node_pos].local_dir_name=et_local_dir.getText().toString();
				mWizData.prof_node[node_pos].local_mount_point_name=spinnerLmp.getSelectedItem().toString();
				if (node_pos==1) buildSyncProfile();
				else if (node_pos==0) {
					if (mWizData.prof_node[node_pos+1].node_type.equals("L")) buildLocalProfile(node_pos+1);
					else buildRemoteProfile(node_pos+1);
				}
			}
		});
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		dialog.show();
	};

	boolean remote_user_pass_verified=false;
	private void buildRemoteProfile(final int node_pos) {
		remote_user_pass_verified=false;
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		mWizData.dialog_list.add(dialog);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.sync_wizard_dlg);
		
		final LinearLayout ll_overall=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_overall);
		final LinearLayout ll_local=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_local);
		final LinearLayout ll_remote=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_remote);
		final LinearLayout ll_sync=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_sync);

		final TextView dlg_title=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_title);
		String n_type="";
		if (node_pos==0) n_type=mContext.getString(R.string.msgs_sync_wizard_title_type_master);
		else n_type=mContext.getString(R.string.msgs_sync_wizard_title_type_target);
		dlg_title.setText(mContext.getString(R.string.msgs_sync_wizard_title)+" - "+n_type);
		
		final Button btn_ok=(Button)dialog.findViewById(R.id.sync_wizard_dlg_ok);
		final Button btn_back=(Button)dialog.findViewById(R.id.sync_wizard_dlg_back);
		final Button btn_cancel=(Button)dialog.findViewById(R.id.sync_wizard_dlg_cancel);
		final Button btn_next=(Button)dialog.findViewById(R.id.sync_wizard_dlg_next);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_msg);
		
		btn_ok.setVisibility(Button.GONE);

		ll_overall.setVisibility(LinearLayout.GONE);
		ll_local.setVisibility(LinearLayout.GONE);
		ll_remote.setVisibility(LinearLayout.VISIBLE);
		ll_sync.setVisibility(LinearLayout.GONE);

		dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_server));
		
		final CheckBox cb_use_hostname=(CheckBox)dialog.findViewById((R.id.sync_wizard_dlg_remote_use_computer_name));
		final Button btn_scan_network=(Button)dialog.findViewById((R.id.sync_wizard_dlg_remote_get_addr_btn));
		final EditText et_remote_addr=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_addr));
		final EditText et_remote_hostname=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_hostname));
		final CheckBox cb_use_user_pass=(CheckBox)dialog.findViewById((R.id.sync_wizard_dlg_remote_use_user_pass));
		final EditText et_remote_user=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_user));
		final EditText et_remote_pass=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_password));
		final Button btn_remote_share=(Button)dialog.findViewById((R.id.sync_wizard_dlg_remote_share_btn));
		final EditText et_remote_share=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_share));
		final Button btn_remote_dir=(Button)dialog.findViewById((R.id.sync_wizard_dlg_remote_dir_btn));
		final EditText et_remote_dir=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_dir));

		final EditText et_remote_prof=(EditText) dialog.findViewById(R.id.sync_wizard_dlg_remote_prof_name);
		et_remote_prof.selectAll();
		if (node_pos==0) et_remote_prof.setText(mWizData.master_name);
		else et_remote_prof.setText(mWizData.target_name);

		et_remote_addr.setText(mWizData.prof_node[node_pos].remote_host_ipaddr);
		et_remote_hostname.setText(mWizData.prof_node[node_pos].remote_host_name);
		et_remote_user.setText(mWizData.prof_node[node_pos].remote_user_name);
		et_remote_pass.setText(mWizData.prof_node[node_pos].remote_user_pass);
		et_remote_share.setText(mWizData.prof_node[node_pos].remote_share_name);
		et_remote_dir.setText(mWizData.prof_node[node_pos].remote_dir_name);

		cb_use_user_pass.setEnabled(false);
		et_remote_user.setEnabled(false);
		et_remote_pass.setEnabled(false);
		btn_remote_share.setEnabled(false);
		et_remote_share.setEnabled(false);
		btn_remote_dir.setEnabled(false);
		et_remote_dir.setEnabled(false);
		btn_next.setEnabled(false);

		cb_use_hostname.setChecked(mWizData.prof_node[node_pos].remote_use_hostname);
		et_remote_addr.setVisibility(EditText.GONE);
		et_remote_hostname.setVisibility(EditText.VISIBLE);
		cb_use_hostname.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				if (isChecked) {
					et_remote_addr.setVisibility(EditText.GONE);
					et_remote_hostname.setVisibility(EditText.VISIBLE);
				} else {
					et_remote_addr.setVisibility(EditText.VISIBLE);
					et_remote_hostname.setVisibility(EditText.GONE);
				}
				setRemoteProfileViewVisibility(dialog);
			}
		});
		
		et_remote_hostname.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				setRemoteProfileViewVisibility(dialog);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});
		et_remote_addr.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				setRemoteProfileViewVisibility(dialog);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});

		et_remote_prof.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable text) {
				if (node_pos==0) {
					if (text.length()>0) {
						if (text.toString().equals(mWizData.sync_name)) btn_next.setEnabled(false);
						else {
							if (!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_SYNC, text.toString()) &&
									!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_LOCAL, text.toString()) &&
									!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_REMOTE, text.toString()) ) {
								btn_next.setEnabled(true);
							} else btn_next.setEnabled(false);
						}
					} else btn_next.setEnabled(false);
				} else {
					if (text.length()>0) {
						if (text.toString().equals(mWizData.sync_name)) btn_next.setEnabled(false);
						else {
							if (text.toString().equals(mWizData.master_name)) btn_next.setEnabled(false);
							else {
								if (!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_SYNC, text.toString()) &&
										!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_LOCAL, text.toString()) &&
										!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_REMOTE, text.toString()) ) {
									btn_next.setEnabled(true);
								} else btn_next.setEnabled(false);
							}
						}
					} else btn_next.setEnabled(false);
				}
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		cb_use_user_pass.setChecked(mWizData.prof_node[node_pos].remote_use_user_pass);
		et_remote_user.setEnabled(true);
		et_remote_pass.setEnabled(true);
		cb_use_user_pass.setOnCheckedChangeListener(new OnCheckedChangeListener(){
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean isChecked) {
				setRemoteProfileViewVisibility(dialog);
			}
		});
		
		et_remote_user.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				remote_user_pass_verified=false;
				setRemoteProfileViewVisibility(dialog);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});

		et_remote_pass.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				remote_user_pass_verified=false;
				setRemoteProfileViewVisibility(dialog);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});
		
		final Button btnLogon = (Button) dialog.findViewById(R.id.sync_wizard_dlg_remote_logon);
		btnLogon.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				String user=null, pass=null;
				if (cb_use_user_pass.isChecked()) {
					if (et_remote_user.getText().length()>0) user=et_remote_user.getText().toString();
					if (et_remote_pass.getText().length()>0) pass=et_remote_pass.getText().toString();
				}
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						remote_user_pass_verified=true;
						setRemoteProfileViewVisibility(dialog);
					}
					@Override
					public void negativeResponse(Context c, Object[] o) {
						remote_user_pass_verified=false;
						setRemoteProfileViewVisibility(dialog);
					}
				});
				if (cb_use_hostname.isChecked()) {
					profMaint.processLogonToRemote(et_remote_hostname.getText().toString(),
							"","",user,pass,ntfy);
				} else {
					profMaint.processLogonToRemote("",
							et_remote_addr.getText().toString(),"",user,pass,ntfy);
				}
			}
		});


		// address scanボタンの指定
		if (util.isRemoteDisable()) btn_scan_network.setEnabled(false);
		btn_scan_network.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						if (((String)arg1[0]).equals("A")) {
							cb_use_hostname.setChecked(false);
							et_remote_addr.setText((String)arg1[1]);
						} else {
							cb_use_hostname.setChecked(true);
							et_remote_hostname.setText((String)arg1[1]);
						}
					}
					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						dlg_msg.setText("");
					}
				});
				profMaint.scanRemoteNetworkDlg(ntfy,"");
			}
		});
		
		// RemoteShareボタンの指定
		if (util.isRemoteDisable()) btn_remote_share.setEnabled(false);
		btn_remote_share.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				profMaint.setSmbUserPass(et_remote_user.getText().toString(),et_remote_pass.getText().toString());
				String t_url="";
				if (cb_use_hostname.isChecked()) t_url=et_remote_hostname.getText().toString();
				else t_url=et_remote_addr.getText().toString();
				String remurl="smb://"+t_url+"/";
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						et_remote_share.setText((String)arg1[0]);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						if (arg1!=null) dlg_msg.setText((String)arg1[0]);
						else dlg_msg.setText("");
					}
					
				});
				profMaint.setRemoteShare(remurl,"", ntfy);
			}
		});
		// RemoteDirectoryボタンの指定
		if (util.isRemoteDisable()) btn_remote_dir.setEnabled(false);
		btn_remote_dir.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				profMaint.setSmbUserPass(et_remote_user.getText().toString(),et_remote_pass.getText().toString());
				String t_url="";
				if (cb_use_hostname.isChecked()) t_url=et_remote_hostname.getText().toString();
				else t_url=et_remote_addr.getText().toString();
				String remurl="smb://"+t_url+"/"+
						et_remote_share.getText().toString()+"/";
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						et_remote_dir.setText((String)arg1[0]);
					}
					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {
						if (arg1!=null) dlg_msg.setText((String)arg1[0]);
						else dlg_msg.setText("");
					}
				});
				profMaint.setRemoteDir(remurl, "","", ntfy);
			}
		});

		et_remote_share.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				setRemoteProfileViewVisibility(dialog);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});

		et_remote_dir.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable arg0) {
				setRemoteProfileViewVisibility(dialog);
			}

			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence s, int start, int before,int count) {}
		});

		setRemoteProfileViewVisibility(dialog);
		
		btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				for (int i=0;i<mWizData.dialog_list.size();i++)
					mWizData.dialog_list.get(i).dismiss();
			}
		});
		dialog.setOnCancelListener(new OnCancelListener(){
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		btn_back.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				mWizData.dialog_list.remove(dialog);
				dialog.dismiss();
			}
		});
		btn_next.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				if (node_pos==0) mWizData.master_name=et_remote_prof.getText().toString();
				else mWizData.target_name=et_remote_prof.getText().toString();

				mWizData.prof_node[node_pos].remote_dir_name=et_remote_dir.getText().toString();
				mWizData.prof_node[node_pos].remote_host_ipaddr=et_remote_addr.getText().toString();
				mWizData.prof_node[node_pos].remote_host_name=et_remote_hostname.getText().toString();
				mWizData.prof_node[node_pos].remote_share_name=et_remote_share.getText().toString();
				mWizData.prof_node[node_pos].remote_use_hostname=cb_use_hostname.isChecked();
				mWizData.prof_node[node_pos].remote_use_user_pass=cb_use_user_pass.isChecked();
				mWizData.prof_node[node_pos].remote_user_name=et_remote_user.getText().toString();
				mWizData.prof_node[node_pos].remote_user_pass=et_remote_pass.getText().toString();
				if (node_pos==1) buildSyncProfile();
				else if (node_pos==0) {
					if (mWizData.prof_node[node_pos+1].node_type.equals("L")) buildLocalProfile(node_pos+1);
					else buildRemoteProfile(node_pos+1);
				}
			}
		});
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		dialog.show();
	};
	
	private void setRemoteProfileViewVisibility(Dialog dialog) {
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_msg);
		final CheckBox cb_use_hostname=(CheckBox)dialog.findViewById((R.id.sync_wizard_dlg_remote_use_computer_name));
//		final Button btn_scan_network=(Button)dialog.findViewById((R.id.sync_wizard_dlg_remote_get_addr_btn));
		final EditText et_remote_addr=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_addr));
		final EditText et_remote_hostname=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_hostname));
		final CheckBox cb_use_user_pass=(CheckBox)dialog.findViewById((R.id.sync_wizard_dlg_remote_use_user_pass));
		final EditText et_remote_user=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_user));
		final EditText et_remote_pass=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_password));
		final Button btn_remote_share=(Button)dialog.findViewById((R.id.sync_wizard_dlg_remote_share_btn));
		final EditText et_remote_share=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_share));
		final Button btn_remote_dir=(Button)dialog.findViewById((R.id.sync_wizard_dlg_remote_dir_btn));
		final EditText et_remote_dir=(EditText)dialog.findViewById((R.id.sync_wizard_dlg_remote_dir));
		final Button btn_next=(Button)dialog.findViewById(R.id.sync_wizard_dlg_next);
		final Button btnLogon = (Button) dialog.findViewById(R.id.sync_wizard_dlg_remote_logon);
		
		boolean nw=false;
		if (cb_use_hostname.isChecked()) {
			if (et_remote_hostname.getText().length()>0) nw=true;
		} else {
			if (et_remote_addr.getText().length()>0) {
				String[] addr=et_remote_addr.getText().toString().split("\\.");
				if (addr.length==4) {
					boolean error=false;
					for (int i=0;i<4;i++) {
						try {
							int num=Integer.parseInt(addr[i]);
							if (num<0 || num>255) {
								error=true;
								break;
							}
						} catch(NumberFormatException e) {
							error=true;
							break;
						}
					}
					if (!error) nw=true;
				}
			}
		}
//		cb_use_user_pass.setEnabled(false);
//		et_remote_user.setEnabled(false);
//		et_remote_pass.setEnabled(false);
//		btn_remote_share.setEnabled(false);
//		et_remote_share.setEnabled(false);
//		btn_remote_dir.setEnabled(false);
//		et_remote_dir.setEnabled(false);
//		btn_next.setEnabled(false);
		
		if (nw) {
			cb_use_user_pass.setEnabled(true);
			if (cb_use_user_pass.isChecked()) {
				et_remote_user.setVisibility(CheckBox.VISIBLE);
				et_remote_pass.setVisibility(CheckBox.VISIBLE);
				btnLogon.setVisibility(Button.VISIBLE);
				if (et_remote_user.getText().length()>0) {
					if (et_remote_pass.getText().length()>0) {
						btn_remote_share.setEnabled(true);
						et_remote_share.setEnabled(true);
						if (et_remote_share.getText().length()>0) {
							btn_remote_dir.setEnabled(true);
							et_remote_dir.setEnabled(true);
						}
					}
				}
			} else {
				et_remote_user.setVisibility(CheckBox.GONE);
				et_remote_pass.setVisibility(CheckBox.GONE);
				btnLogon.setVisibility(Button.GONE);
				btn_remote_share.setEnabled(true);
				et_remote_share.setEnabled(true);
				if (et_remote_share.getText().length()>0) {
					btn_remote_dir.setEnabled(true);
					et_remote_dir.setEnabled(true);
				}
			}
			//set Next button
			if (cb_use_user_pass.isChecked()) {
				if (et_remote_user.getText().length()>0 && et_remote_pass.getText().length()>0) {
					if (remote_user_pass_verified) {
						if (et_remote_share.getText().length()>0) {
							btn_next.setEnabled(true);
						} else btn_next.setEnabled(false);
					} else {
						btn_next.setEnabled(false);
					}
				} else btn_next.setEnabled(false);
			} else {
				if (et_remote_share.getText().length()>0) {
					btn_next.setEnabled(true);
				} else btn_next.setEnabled(false);
			}
			//set Navigate message
			if (cb_use_user_pass.isChecked()) {
				if (et_remote_user.getText().length()==0 || et_remote_pass.getText().length()==0) {
					dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_user_pass));
				} else {
					if (!remote_user_pass_verified) {
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_logon_required));
					} else {
						if (et_remote_share.getText().length()==0) {
							dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_remote_share));
						} else {
							if (et_remote_dir.getText().length()==0) {
								dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_remote_dir));
							} else {
								dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
							}
						}
					}
				}
			} else {
				if (et_remote_share.getText().length()==0) {
					dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_remote_share));
				} else {
					if (et_remote_dir.getText().length()==0) {
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_remote_dir));
					} else {
						dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_press_next));
					}
				}
			}

		} else {
			cb_use_user_pass.setEnabled(false);
			dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_server));
		}
	};

	private void buildSyncProfile() {
		// カスタムダイアログの生成
		final Dialog dialog = new Dialog(mContext);
		mWizData.dialog_list.add(dialog);
		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
		dialog.setContentView(R.layout.sync_wizard_dlg);
		
		final LinearLayout ll_overall=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_overall);
		final LinearLayout ll_local=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_local);
		final LinearLayout ll_remote=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_remote);
		final LinearLayout ll_sync=(LinearLayout)dialog.findViewById(R.id.sync_wizard_dlg_sync);

		final TextView dlg_title=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_title);
		dlg_title.setText(mContext.getString(R.string.msgs_sync_wizard_title)+" - "+
				mContext.getString(R.string.msgs_sync_wizard_title_type_sync));
		
		final Button btn_ok=(Button)dialog.findViewById(R.id.sync_wizard_dlg_ok);
		final Button btn_back=(Button)dialog.findViewById(R.id.sync_wizard_dlg_back);
		final Button btn_cancel=(Button)dialog.findViewById(R.id.sync_wizard_dlg_cancel);
		final Button btn_next=(Button)dialog.findViewById(R.id.sync_wizard_dlg_next);
		final TextView dlg_msg=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_msg);
		dlg_msg.setText(mContext.getString(R.string.msgs_sync_wizard_specify_other_option));
		
		btn_next.setVisibility(Button.GONE);

		ll_overall.setVisibility(LinearLayout.GONE);
		ll_local.setVisibility(LinearLayout.GONE);
		ll_remote.setVisibility(LinearLayout.GONE);
		ll_sync.setVisibility(LinearLayout.VISIBLE);

		final Spinner spinner_sync=(Spinner) dialog.findViewById(R.id.sync_wizard_dlg_sync_option);
		final Button btn_file_filter=(Button) dialog.findViewById(R.id.sync_wizard_dlg_sync_file_filter_btn);
		final TextView tv_file_filter=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_sync_file_filter);
		final Button btn_dir_filter=(Button) dialog.findViewById(R.id.sync_wizard_dlg_sync_dir_filter_btn);
		final TextView tv_dir_filter=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_sync_dir_filter);
		final CheckBox cb_master_dir=(CheckBox) dialog.findViewById(R.id.sync_wizard_dlg_sync_master_dir_cb);
		final CheckBox cb_confirm=(CheckBox) dialog.findViewById(R.id.sync_wizard_dlg_sync_confirm);
		final CheckBox cb_last_modified=(CheckBox) dialog.findViewById(R.id.sync_wizard_dlg_sync_last_modified);
		final CheckBox cb_not_use_last_mod_remote = (CheckBox)dialog.findViewById(R.id.sync_wizard_dlg_not_use_last_modified_remote_file_for_diff);
		profMaint.setSyncOptionSpinner(spinner_sync, "");
		
		final EditText et_sync_prof=(EditText) dialog.findViewById(R.id.sync_wizard_dlg_sync_prof_name);
		et_sync_prof.selectAll();
		et_sync_prof.setText(mWizData.sync_name);

		et_sync_prof.addTextChangedListener(new TextWatcher(){
			@Override
			public void afterTextChanged(Editable text) {
				if (text.length()>0) {
					mWizData.sync_name=text.toString();
					if (text.toString().equals(mWizData.master_name)) btn_ok.setEnabled(false);
					else {
						if (text.toString().equals(mWizData.target_name)) btn_ok.setEnabled(false);
						else {
							if (!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_SYNC, text.toString()) &&
									!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_LOCAL, text.toString()) &&
									!profMaint.isProfileExists(SMBSYNC_PROF_GROUP_DEFAULT,SMBSYNC_PROF_TYPE_REMOTE, text.toString()) ) {
								btn_ok.setEnabled(true);
							} else btn_ok.setEnabled(false);
						}
					}
				} else btn_ok.setEnabled(false);
			}
			@Override
			public void beforeTextChanged(CharSequence arg0, int arg1,int arg2, int arg3) {}
			@Override
			public void onTextChanged(CharSequence arg0, int arg1, int arg2,int arg3) {}
		});

		cb_master_dir.setChecked(true);
//		cb_master_dir.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_enable));
//		cb_master_dir.setOnCheckedChangeListener(new OnCheckedChangeListener(){
//			@Override
//			public void onCheckedChanged(CompoundButton buttonView,
//					boolean isChecked) {
//				if (isChecked) cb_master_dir.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_enable));
//				else cb_master_dir.setText(mContext.getString(R.string.msgs_sync_profile_master_dir_cb_disable));
//			}
//		});
		
		cb_confirm.setChecked(true);
		cb_last_modified.setChecked(false);
		cb_not_use_last_mod_remote.setChecked(false);

		if (mWizData.file_filter.size()==0) tv_file_filter.setText(mContext.getString(R.string.msgs_filter_list_dlg_not_specified));
		else {
			String f_fl="";
			if (mWizData.file_filter!=null) {
				String cn="";
				for (int i=0;i<mWizData.file_filter.size();i++) {
					f_fl+=cn+mWizData.file_filter.get(i).substring(1,mWizData.file_filter.get(i).length());
					cn=",";
				}
			}
			if (f_fl.length()==0) f_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
			tv_file_filter.setText(f_fl);
		}

		// file filterボタンの指定
		btn_file_filter.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						String f_fl="";
						if (mWizData.file_filter!=null) {
							String cn="";
							for (int i=0;i<mWizData.file_filter.size();i++) {
								f_fl+=cn+mWizData.file_filter.get(i).substring(1,mWizData.file_filter.get(i).length());
								cn=",";
							}
						}
						if (f_fl.length()==0) f_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
						tv_file_filter.setText(f_fl);
					}

					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {}
					
				});
				profMaint.setFileFilter(mWizData.file_filter,ntfy);
			}
		});
		
		// directory filterボタンの指定
		btn_dir_filter.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				ArrayList<ProfileListItem>pl=new ArrayList<ProfileListItem>();
				AdapterProfileList t_prof=new AdapterProfileList(mContext, 
							R.layout.profile_list_item_view, pl, "");
				syncWizardCreateProfile(t_prof);
				NotifyEvent ntfy=new NotifyEvent(mContext);
				//Listen setRemoteShare response 
				ntfy.setListener(new NotifyEventListener() {
					@Override
					public void positiveResponse(Context arg0, Object[] arg1) {
						String d_fl="";
						if (mWizData.dir_filter!=null) {
							String cn="";
							for (int i=0;i<mWizData.dir_filter.size();i++) {
								d_fl+=cn+mWizData.dir_filter.get(i)
										.substring(1,mWizData.dir_filter.get(i).length());
								cn=",";
							}
						}
						if (d_fl.length()==0)  {
							d_fl=mContext.getString(R.string.msgs_filter_list_dlg_not_specified);
						}
						tv_dir_filter.setText(d_fl);
						if (mWizData.dir_filter.size()!=0) cb_master_dir.setEnabled(true);
						else cb_master_dir.setEnabled(false);
					}
					@Override
					public void negativeResponse(Context arg0, Object[] arg1) {}
				});
				profMaint.setDirFilter(t_prof,mWizData.master_name,
						mWizData.dir_filter,ntfy);
			}
		});
		
		btn_cancel.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				for (int i=0;i<mWizData.dialog_list.size();i++)
					mWizData.dialog_list.get(i).dismiss();
			}
		});
		dialog.setOnCancelListener(new OnCancelListener(){
			@Override
			public void onCancel(DialogInterface arg0) {
				btn_cancel.performClick();
			}
		});
		btn_back.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				mWizData.dialog_list.remove(dialog);
				dialog.dismiss();
			}
		});
		btn_ok.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View arg0) {
				NotifyEvent ntfy=new NotifyEvent(mContext);
				ntfy.setListener(new NotifyEventListener(){
					@Override
					public void positiveResponse(Context c, Object[] o) {
						mWizData.sync_name=et_sync_prof.getText().toString();
						
						for (int i=0;i<mWizData.dialog_list.size();i++)
							mWizData.dialog_list.get(i).dismiss();
						mWizData.confirm_required=cb_confirm.isChecked();
						mWizData.process_master_file_dir=cb_master_dir.isChecked();
						mWizData.use_system_last_mod=cb_last_modified.isChecked();	
						mWizData.not_use_last_mod_remote=cb_not_use_last_mod_remote.isChecked();
						mWizData.mirror_type=spinner_sync.getSelectedItemPosition();
						syncWizardCreateProfile(profileAdapter);
						profileAdapter.sort();
						profileAdapter.notifyDataSetChanged();
						profMaint.saveProfileToFile(false,"","",profileAdapter,false);
						
						mCommonDlg.showCommonDialog(false, "I", 
								mContext.getString(R.string.msgs_sync_wizard_confirm_profile_created), "", null);
					}

					@Override
					public void negativeResponse(Context c, Object[] o) {}
					
				});
				confirmProfileCreation(dialog, ntfy);
			}
		});
		
		CommonDialog.setDlgBoxSizeLimit(dialog,true);
		dialog.show();
	};

	private void confirmProfileCreation(Dialog dialog, NotifyEvent p_ntfy) {
		String sync_prof_name=mWizData.sync_name;
		String sd="";
		if (mWizData.master_type.equals(SMBSYNC_PROF_TYPE_REMOTE) && mWizData.target_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
			sd=mContext.getString(R.string.msgs_sync_wizard_sync_direction_remote_to_local);
		} else if (mWizData.master_type.equals(SMBSYNC_PROF_TYPE_LOCAL) && mWizData.target_type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
			sd=mContext.getString(R.string.msgs_sync_wizard_sync_direction_local_to_remote);
		} else if (mWizData.master_type.equals(SMBSYNC_PROF_TYPE_LOCAL) && mWizData.target_type.equals(SMBSYNC_PROF_TYPE_LOCAL)) {
			sd=mContext.getString(R.string.msgs_sync_wizard_sync_direction_local_to_local);
		}
		String sync_direction=sd;
		
		String master="", target="";
		if (mWizData.prof_node[0].node_type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
			master=listRemoteProfile(mWizData.prof_node[0]);
		} else {
			master=listLocalProfile(mWizData.prof_node[0]);
		}
		if (mWizData.prof_node[1].node_type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
			target=listRemoteProfile(mWizData.prof_node[1]);
		} else {
			target=listLocalProfile(mWizData.prof_node[1]);
		}
		String sync_master="", sync_target="";
		sync_master=String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_sync_master_name),mWizData.master_name,master);
		sync_target=String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_sync_target_name),mWizData.target_name,target);
		
		String so=SMBSYNC_SYNC_TYPE_COPY;
		if (mWizData.mirror_type==0) so=mContext.getString(R.string.msgs_sync_profile_dlg_mirror);
		else if (mWizData.mirror_type==1) so=mContext.getString(R.string.msgs_sync_profile_dlg_copy);
		else if (mWizData.mirror_type==2) so=mContext.getString(R.string.msgs_sync_profile_dlg_move);
		String sync_type=String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_sync_type),so);
		
		final TextView tv_file_filter=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_sync_file_filter);
		final TextView tv_dir_filter=(TextView) dialog.findViewById(R.id.sync_wizard_dlg_sync_dir_filter);
		String sync_file_filter=mContext.getString(R.string.msgs_sync_wizard_confirm_list_sync_file_filter)+
				":\n     "+tv_file_filter.getText().toString();
		String sync_dir_filter=mContext.getString(R.string.msgs_sync_wizard_confirm_list_sync_dir_filter)+
				":\n     "+tv_dir_filter.getText().toString();
		
		String sync_mpd=mContext.getString(R.string.msgs_sync_profile_master_dir_cb_enable)+":\n     ";
		if (mWizData.process_master_file_dir) sync_mpd+="YES";
		else sync_mpd+="NO";

		String sync_confirm=mContext.getString(R.string.msgs_sync_profile_confirm_required)+":\n     ";
		if (mWizData.confirm_required) sync_confirm+="YES";
		else sync_confirm+="NO";

		String sync_nulm_remote=mContext.getString(R.string.msgs_sync_profile_not_set_remote_file_last_modified)+":\n     ";
		if (mWizData.not_use_last_mod_remote) sync_nulm_remote+="YES";
		else sync_nulm_remote+="NO";

		String sync_last_mod=mContext.getString(R.string.msgs_sync_profile_last_modified_smbsync)+":\n     ";
		if (mWizData.use_system_last_mod) sync_last_mod+="YES";
		else sync_last_mod+="NO";
		
		String msg_text=sync_prof_name+"\n"+sync_direction+
				"\n"+sync_master+"\n"+sync_target+"\n"+sync_type+
				"\n"+sync_file_filter+
				"\n"+sync_dir_filter+
				"\n"+sync_mpd+
				"\n"+sync_confirm+
				"\n"+sync_nulm_remote+
				"\n"+sync_last_mod;
		
		mCommonDlg.showCommonDialog(true, "I", 
				mContext.getString(R.string.msgs_sync_wizard_confirm_title), msg_text, p_ntfy);
		
	};
	
	private String listRemoteProfile(WizardProfileData prof_node) {
		String result="";
		
		String server_id="";
		if (prof_node.remote_use_hostname) server_id=prof_node.remote_host_name;
		else server_id=prof_node.remote_host_ipaddr;
		String user_id=prof_node.remote_user_name;
		String share=prof_node.remote_share_name;
		String dir=prof_node.remote_dir_name;
		
		if (prof_node.remote_use_user_pass) {
			result=	"     "+String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_remote_serverid),server_id)+"\n"+
					"     "+String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_remote_userid),user_id)+"\n"+
					"     "+String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_remote_share),share)+"\n"+
					"     "+String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_remote_directory),dir);
		} else {
			result=	"     "+String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_remote_serverid),server_id)+"\n"+
					"     "+String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_remote_share),share)+"\n"+
					"     "+String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_remote_directory),dir);
		}
		return result;
	};
	
	private String listLocalProfile(WizardProfileData prof_node) {
		String result="";
		
		String lmp=prof_node.local_mount_point_name;
		String dir=prof_node.local_dir_name;
		
		result=	"     "+String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_local_mount_point),lmp)+"\n"+
				"     "+String.format(mContext.getString(R.string.msgs_sync_wizard_confirm_list_local_directory),dir);
		
		return result;
	};
	
	private void syncWizardCreateProfile(AdapterProfileList t_prof) {
		for (int i=0;i<2;i++) {
			String prof_name="";
			if (i==0) prof_name=mWizData.master_name; 
			else prof_name=mWizData.target_name;
			if (mWizData.prof_node[i].node_type.equals("L")) {
				ProfileListItem l_pli=new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
						SMBSYNC_PROF_TYPE_LOCAL,
						prof_name,
						SMBSYNC_PROF_ACTIVE,
						mWizData.prof_node[i].local_mount_point_name, 
						mWizData.prof_node[i].local_dir_name, false);
				t_prof.add(l_pli);
			} else {
				ProfileListItem r_pli=new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
						SMBSYNC_PROF_TYPE_REMOTE,
						prof_name,
						SMBSYNC_PROF_ACTIVE,
						mWizData.prof_node[i].remote_user_name,
						mWizData.prof_node[i].remote_user_pass,
						mWizData.prof_node[i].remote_host_ipaddr,
						mWizData.prof_node[i].remote_host_name,
						mWizData.prof_node[i].remote_port,
						mWizData.prof_node[i].remote_share_name,
						mWizData.prof_node[i].remote_dir_name,false);
				t_prof.add(r_pli);
			}
		}
		
		String so=SMBSYNC_SYNC_TYPE_COPY;
		if (mWizData.mirror_type==0) so=SMBSYNC_SYNC_TYPE_MIRROR;
		else if (mWizData.mirror_type==1) so=SMBSYNC_SYNC_TYPE_COPY;
		else if (mWizData.mirror_type==2) so=SMBSYNC_SYNC_TYPE_MOVE;
		
		ProfileListItem s_pli=new ProfileListItem(SMBSYNC_PROF_GROUP_DEFAULT, 
				SMBSYNC_PROF_TYPE_SYNC,
				mWizData.sync_name,
				SMBSYNC_PROF_ACTIVE,
				so,mWizData.master_type,mWizData.master_name,
				mWizData.target_type,mWizData.target_name,
				mWizData.file_filter,
				mWizData.dir_filter,
				mWizData.process_master_file_dir,
				mWizData.confirm_required,
				mWizData.use_system_last_mod,
				mWizData.not_use_last_mod_remote, false);
		t_prof.add(s_pli);
	};

	private void setSyncDirectionSpinner(Spinner spinnerSyncDirection, String prof_syncopt) {
//		final Spinner spinnerSyncDirection=(Spinner)dialog.findViewById(R.id.sync_profile_sync_option);
		final CustomSpinnerAdapter adapterSyncDirection=new CustomSpinnerAdapter(mContext, R.layout.custom_simple_spinner_item);
		adapterSyncDirection.setTextColor(Color.BLACK);
		adapterSyncDirection.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinnerSyncDirection.setPrompt(mContext.getString(R.string.msgs_sync_profile_dlg_syncopt_prompt));
		spinnerSyncDirection.setAdapter(adapterSyncDirection);
		adapterSyncDirection.add(mContext.getString(R.string.msgs_sync_wizard_sync_direction_not_selected));
		adapterSyncDirection.add(mContext.getString(R.string.msgs_sync_wizard_sync_direction_remote_to_local));
		adapterSyncDirection.add(mContext.getString(R.string.msgs_sync_wizard_sync_direction_local_to_remote));
		adapterSyncDirection.add(mContext.getString(R.string.msgs_sync_wizard_sync_direction_local_to_local));
		
		adapterSyncDirection.notifyDataSetChanged();
	};
}
