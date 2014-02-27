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

import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_LOCAL;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_REMOTE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_SYNC;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_COPY;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MIRROR;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_MOVE;
import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_SYNC_TYPE_SYNC;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AdapterProfileList extends ArrayAdapter<ProfileListItem> {

		private Context c;
		private int id;
		private ArrayList<ProfileListItem>items;
		@SuppressWarnings("unused")
		private String SMBSync_External_Root_Dir;
		
		public AdapterProfileList(Context context, int textViewResourceId,
				ArrayList<ProfileListItem> objects, String esd) {
			super(context, textViewResourceId, objects);
			c = context;
			id = textViewResourceId;
			items = objects;
			SMBSync_External_Root_Dir=esd;
		}
		public ProfileListItem getItem(int i) {
			 return items.get(i);
		}
		public  void remove(int i) {
			items.remove(i);
			notifyDataSetChanged();
		}
		public  void replace(ProfileListItem pli, int i) {
			items.set(i,pli);
			notifyDataSetChanged();
		}
		
		public ArrayList<ProfileListItem> getAllItem() {return items;}
		
		public void setAllItem(ArrayList<ProfileListItem> p) {
			items.clear();
			if (p!=null) items.addAll(p);
			notifyDataSetChanged();
		}

		public void sort() {
			Collections.sort(items, new Comparator<ProfileListItem>(){
				@Override
				public int compare(ProfileListItem litem, ProfileListItem ritem) {
					String l_t,l_n,l_g;
					String r_t,r_n,r_g;
					
					l_g=litem.getGroup();
					if (litem.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) l_t="0";
					else if (litem.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) l_t="1";
					else l_t="2";
					l_n=litem.getName();
					
					r_g=ritem.getGroup();
					if (ritem.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) r_t="0";
					else if (ritem.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) r_t="1";
					else r_t="2";
					r_n=ritem.getName();
					
					if (!l_g.equalsIgnoreCase(r_g)) return l_g.compareToIgnoreCase(r_g);
					else if (!l_t.equalsIgnoreCase(r_t)) return l_t.compareToIgnoreCase(r_t);
					else if (!l_n.equalsIgnoreCase(r_n)) return l_n.compareToIgnoreCase(r_n);
					return 0;
				}
			});
		};
		
//		@Override
//		public boolean isEnabled(int idx) {
//			 return getItem(idx).getActive().equals("A");
//		}

		@Override
	    final public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			
            View v = convertView;
            if (v == null) {
                LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                v = vi.inflate(id, null);
                holder=new ViewHolder();
                holder.iv_row_icon= (ImageView) v.findViewById(R.id.profile_list_icon);
                holder.tv_row_name= (TextView) v.findViewById(R.id.profile_list_name);
                holder.tv_row_active= (TextView) v.findViewById(R.id.profile_list_active);
                holder.cbv_row_cb1=(CheckBox) v.findViewById(R.id.profile_list_checkbox1);
                holder.tv_active_active=
                		c.getString(R.string.msgs_sync_list_array_activ_activ);
                holder.tv_active_inact=
                		c.getString(R.string.msgs_sync_list_array_activ_inact);
                
                holder.tv_row_master= (TextView) v.findViewById(R.id.profile_list_master_name);
                holder.tv_row_target= (TextView) v.findViewById(R.id.profile_list_target_name);
                holder.tv_row_master_const= (TextView) v.findViewById(R.id.profile_list_master_const);
                holder.tv_row_target_const= (TextView) v.findViewById(R.id.profile_list_target_const);
                holder.tv_row_synctype= (TextView) v.findViewById(R.id.profile_list_synctype);
                holder.iv_row_image_master= (ImageView) v.findViewById(R.id.profile_list_image_master);
                holder.iv_row_image_target= (ImageView) v.findViewById(R.id.profile_list_image_target);
                holder.tv_mtype_mirror=c.getString(R.string.msgs_sync_list_array_mtype_mirr);
                holder.tv_mtype_copy=c.getString(R.string.msgs_sync_list_array_mtype_copy);
                holder.tv_mtype_move=c.getString(R.string.msgs_sync_list_array_mtype_move);
                
                holder.ll_sync=(LinearLayout) v.findViewById(R.id.profile_list_sync_layout);
                holder.ll_entry=(LinearLayout) v.findViewById(R.id.profile_list_entry_layout);
                
                holder.tv_dir_name=(TextView) v.findViewById(R.id.profile_list_dir_name);
                
                v.setTag(holder);
            } else {
            	holder= (ViewHolder)v.getTag();
            }
            final ProfileListItem o = getItem(position);
            if (o != null) {
            	if (o.getType().equals(SMBSYNC_PROF_TYPE_SYNC)) 
            		holder.iv_row_icon.setImageResource(R.drawable.ic_32_sync);
            	else if (o.getType().equals(SMBSYNC_PROF_TYPE_REMOTE)) 
            		holder.iv_row_icon.setImageResource(R.drawable.ic_32_server);
            	else if (o.getType().equals(SMBSYNC_PROF_TYPE_LOCAL)) 
            		holder.iv_row_icon.setImageResource(R.drawable.ic_32_mobile);
            		
            	String act="";
            	if (o.getActive().equals("A")) act=holder.tv_active_active;
            	else act=holder.tv_active_inact;
            	holder.tv_row_active.setText(act);
            	holder.tv_row_name.setText(o.getName());
                
                if (!getItem(position).getActive().equals("A")) {
             	   //v.setVisibility(View.INVISIBLE);
//             	   v.setBackgroundColor(Color.argb(32, 255, 255, 255));
//             	   v.setBackgroundColor(Color.DKGRAY);
             	   holder.tv_row_name.setTextColor(Color.DKGRAY);
             	   holder.tv_row_active.setTextColor(Color.DKGRAY);
                } else {
//             	   v.setBackgroundColor(Color.BLACK);
             	   holder.tv_row_name.setTextColor(Color.WHITE);
             	   holder.tv_row_active.setTextColor(Color.WHITE);
                }
               
                if (o.getType().equals("S")) {//Sync profile
                	holder.tv_dir_name.setVisibility(LinearLayout.GONE);
                	holder.ll_sync.setVisibility(LinearLayout.VISIBLE);
                	holder.iv_row_icon.setVisibility(LinearLayout.VISIBLE);
                    holder.tv_row_active.setVisibility(LinearLayout.VISIBLE);
                    holder.cbv_row_cb1.setVisibility(LinearLayout.VISIBLE);
                	
                	String synctp="";
                    
                    if (o.getSyncType().equals(SMBSYNC_SYNC_TYPE_SYNC)) synctp="SYNC";
                    else if (o.getSyncType().equals(SMBSYNC_SYNC_TYPE_MIRROR)) synctp=holder.tv_mtype_mirror;
                    else if (o.getSyncType().equals(SMBSYNC_SYNC_TYPE_MOVE)) synctp=holder.tv_mtype_move;
                    else if (o.getSyncType().equals(SMBSYNC_SYNC_TYPE_COPY)) synctp=holder.tv_mtype_copy;
                    else synctp="ERR";
                	
                    if (o.getMasterType().equals(SMBSYNC_PROF_TYPE_REMOTE)) 
                 	   holder.iv_row_image_master.setImageResource(R.drawable.ic_16_server);
                    else if (o.getMasterType().equals(SMBSYNC_PROF_TYPE_LOCAL)) 
                 	   holder.iv_row_image_master.setImageResource(R.drawable.ic_16_mobile);
                    
                    if (o.getTargetType().equals(SMBSYNC_PROF_TYPE_REMOTE)) 
                 	   holder.iv_row_image_target.setImageResource(R.drawable.ic_16_server);
                    else if (o.getTargetType().equals(SMBSYNC_PROF_TYPE_LOCAL)) 
                 	   holder.iv_row_image_target.setImageResource(R.drawable.ic_16_mobile);
                    
                    holder.tv_row_master.setText(o.getMasterName());
                    holder.tv_row_target.setText(o.getTargetName());
                    holder.tv_row_synctype.setText(synctp);
                    
                    if (!getItem(position).getActive().equals("A")) {
                    	holder.tv_row_master.setTextColor(Color.DKGRAY);
                    	holder.tv_row_master_const.setTextColor(Color.DKGRAY);
                    	holder.tv_row_target.setTextColor(Color.DKGRAY);
                    	holder.tv_row_target_const.setTextColor(Color.DKGRAY);
                    	holder.tv_row_synctype.setTextColor(Color.DKGRAY);
                    } else {
                    	holder.tv_row_master.setTextColor(Color.WHITE);
                    	holder.tv_row_master_const.setTextColor(Color.WHITE);
                    	holder.tv_row_target.setTextColor(Color.WHITE);
                    	holder.tv_row_target_const.setTextColor(Color.WHITE);
                    	holder.tv_row_synctype.setTextColor(Color.WHITE);
                    }
                } else if (o.getType().equals("R") || o.getType().equals("L")) {//Remote or Local profile
                	holder.tv_dir_name.setVisibility(LinearLayout.VISIBLE);
                	holder.ll_sync.setVisibility(LinearLayout.GONE);
                	holder.iv_row_icon.setVisibility(LinearLayout.VISIBLE);
                    holder.tv_row_active.setVisibility(LinearLayout.VISIBLE);
                    holder.cbv_row_cb1.setVisibility(LinearLayout.VISIBLE);
                    if (o.getType().equals("L")) {
                    	holder.tv_dir_name.setText(o.getLocalMountPoint()+"/"+o.getDir());
                    } else {
                    	holder.tv_dir_name.setText("/"+o.getShare()+"/"+o.getDir());
                    }
                	
                	if (!getItem(position).getActive().equals("A")) {
                    	holder.tv_dir_name.setTextColor(Color.DKGRAY);
                    } else {
                    	holder.tv_dir_name.setTextColor(Color.WHITE);
                    }
                } else {
                	holder.tv_dir_name.setVisibility(LinearLayout.GONE);
                	holder.ll_sync.setVisibility(LinearLayout.GONE);
                	holder.iv_row_icon.setVisibility(LinearLayout.GONE);
                    holder.tv_row_active.setVisibility(LinearLayout.GONE);
                    holder.cbv_row_cb1.setVisibility(LinearLayout.GONE);
                }
                
                
                final int p = position;
             // 必ずsetChecked前にリスナを登録(convertView != null の場合は既に別行用のリスナが登録されている！)
             	holder.cbv_row_cb1.setOnCheckedChangeListener(new OnCheckedChangeListener() {
     				@Override
     				public void onCheckedChanged(CompoundButton buttonView,
 						boolean isChecked) {
     					o.setChecked(isChecked);
     					items.set(p, o);
     					}
     				});
             	holder.cbv_row_cb1.setChecked(items.get(position).isChecked());
           	}
            return v;
		};
		
		class ViewHolder {
			TextView tv_row_name,tv_row_active;
			ImageView iv_row_icon;
			CheckBox cbv_row_cb1;
			String tv_active_active,tv_active_inact;
			
			TextView tv_row_synctype, tv_row_master, tv_row_target;
			TextView tv_row_master_const, tv_row_target_const;
			ImageView iv_row_image_master,iv_row_image_target;
			String tv_mtype_mirror,tv_mtype_move,tv_mtype_copy;
			
			TextView tv_dir_name, tv_dir_const;
			
			LinearLayout ll_sync, ll_entry;
		}
}

class ProfileListItem implements Serializable,Comparable<ProfileListItem>{
	private static final long serialVersionUID = 1L;
	private String profileGroup="";
	private String profileType="";
	private String profileName="";
	private String profileActive="";
	private boolean profileChk=false;
	private String profileDir="";
	private String profileShare="";
	private String profileAddr="";
	private String profileHostname="";
	private String profileUser="";
	private String profilePass="";
	private String profileSyncType="";
	private String profileMasterType="";
	private String profileMasterName="";
	private String profileTargetType="";
	private String profileTargetName="";
	private String profileLocalMountPoint="";
	private boolean profileMasterDirFileProcess=true;
	private boolean profileConfirm=true;
	private boolean profileForceLastModifiedUseSmbsync=true;
	private ArrayList<String> profileFileFilter =new ArrayList<String>();
	private ArrayList<String> profileDirFilter =new ArrayList<String>();
	
	
	// constructor for local profile
	public ProfileListItem(String pfg, String pft,String pfn, 
			String pfa, String pf_mp, String pf_dir, boolean ic)
	{
		profileGroup=pfg;
		profileType = pft;
		profileName = pfn;
		profileActive=pfa;
		profileLocalMountPoint=pf_mp;
		profileDir=pf_dir;
		profileChk = ic;
	};
	// constructor for remote profile
	public ProfileListItem(String pfg, String pft,String pfn, String pfa, 
			String pf_user,String pf_pass,String pf_addr, String pf_hostname, 
			String pf_share, String pf_dir, boolean ic)
	{
		profileGroup=pfg;
		profileType = pft;
		profileName = pfn;
		profileActive=pfa;
		profileDir=pf_dir;
		profileUser=pf_user;
		profilePass=pf_pass;
		profileShare=pf_share;
		profileAddr=pf_addr;
		profileHostname=pf_hostname;
		profileChk = ic;
	};
	// constructor for sync profile
	public ProfileListItem(String pfg, String pft,String pfn, String pfa,
			String pf_synctype,String pf_master_type,String pf_master_name,
			String pf_target_type,String pf_target_name,
			ArrayList<String> ff, ArrayList<String> df, boolean pd, boolean cnf, 
			boolean jlm, boolean ic)
	{
		profileGroup=pfg;
		profileType = pft;
		profileName = pfn;
		profileActive=pfa;
		profileSyncType=pf_synctype;
		profileMasterType=pf_master_type;
		profileMasterName=pf_master_name;
		profileTargetType=pf_target_type;
		profileTargetName=pf_target_name;
		profileFileFilter=ff;
		profileDirFilter=df;
		profileMasterDirFileProcess=pd;
		profileConfirm=cnf;
		profileForceLastModifiedUseSmbsync=jlm;
		profileChk = ic;
		
	};

	public String getGroup()	{return profileGroup;}
	public String getName()		{return profileName;}
	public String getType()		{return profileType;}
	public String getActive()	{return profileActive;}
	public String getUser()		{return profileUser;}
	public String getPass()		{return profilePass;}
	public String getShare()	{return profileShare;}
	public String getDir()		{return profileDir;}
	public String getAddr()		{return profileAddr;}
	public String getHostname()	{return profileHostname;}
	public String getSyncType()	{return profileSyncType;}
	public String getMasterType(){return profileMasterType;}
	public String getMasterName(){return profileMasterName;}
	public String getTargetType(){return profileTargetType;}
	public String getTargetName(){return profileTargetName;}
	public ArrayList<String> getFileFilter()	{return profileFileFilter;}
	public ArrayList<String> getDirFilter()	{return profileDirFilter;}
	public boolean isMasterDirFileProcess()	{return profileMasterDirFileProcess;}
	public boolean isConfirmRequired()	{return profileConfirm;}
	public boolean isForceLastModifiedUseSmbsync()	{return profileForceLastModifiedUseSmbsync;}
	public boolean isChecked()		{return profileChk;}
	
	public void setGroup(String p)		{profileGroup=p;}
	public void setName(String p)		{profileName=p;}
	public void setType(String p)		{profileType=p;}
	public void setActive(String p)	    {profileActive=p;}
	public void setUser(String p)		{profileUser=p;}
	public void setPass(String p)		{profilePass=p;}
	public void setShare(String p)	    {profileShare=p;}
	public void setDir(String p)		{profileDir=p;}
	public void setAddr(String p)		{profileAddr=p;}
	public void setHostname(String p)	{profileHostname=p;}
	public void setSyncType(String p)	{profileSyncType=p;}
	public void setMasterType(String p) {profileMasterType=p;}
	public void setMasterName(String p) {profileMasterName=p;}
	public void setTargetType(String p) {profileTargetType=p;}
	public void setTargetName(String p) {profileTargetName=p;}
	public void setFileFilter(ArrayList<String> p){profileFileFilter=p;}
	public void setDirFilter(ArrayList<String> p){profileDirFilter=p;}
	public void setMasterDirFileProcess(boolean p) {profileMasterDirFileProcess=p;}
	public void setConfirmRequired(boolean p) {profileConfirm=p;}
	public void setForceLastModifiedUseSmbsync(boolean p) {profileForceLastModifiedUseSmbsync=p;}
	public void setChecked(boolean p)		{profileChk=p;}
	public void setLocalMountPoint(String p) {profileLocalMountPoint=p;}
	public String getLocalMountPoint() {return profileLocalMountPoint;}

	@SuppressLint("DefaultLocale")
	@Override
	public int compareTo(ProfileListItem o) {
		if(this.profileName != null)
			return this.profileName.toLowerCase(Locale.getDefault()).compareTo(o.getName().toLowerCase()) ; 
//				return this.filename.toLowerCase().compareTo(o.getName().toLowerCase()) * (-1);
		else 
			throw new IllegalArgumentException();
	}
}