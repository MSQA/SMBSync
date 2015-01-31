package com.sentaroh.android.SMBSync;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.CompoundButton.OnCheckedChangeListener;

import com.sentaroh.android.Utilities.NotifyEvent;
import com.sentaroh.android.Utilities.Widget.CustomTextView;

public class AdapterMountPointList  extends BaseAdapter{

	private ArrayList<MountPointListItem>local_mount_point_list=null;
	private int textViewResourceId=0;
	private Context c;
	
	private NotifyEvent mCheckBoxClickListener=null;
	
	private boolean themeIsLight=false;
	
	public AdapterMountPointList(Context context, int textViewResourceId,
			ArrayList<MountPointListItem> objects, boolean themeIsLight, NotifyEvent ntfy) {
		c=context;
		local_mount_point_list=objects;
		mCheckBoxClickListener=ntfy;
		this.textViewResourceId=textViewResourceId;
		if (isAnyItemSelected()) setShowCheckBox(true);
		this.themeIsLight=themeIsLight;
		sort();
	}
	
	public void replaceDataList(ArrayList<MountPointListItem> dl) {
		local_mount_point_list=dl;
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		return local_mount_point_list.size();
	}

	@Override
	public MountPointListItem getItem(int pos) {
		return local_mount_point_list.get(pos);
	}

	@Override
	public long getItemId(int arg0) {
		return arg0;
	}
	
	public void add(MountPointListItem lmpli) {
		local_mount_point_list.add(lmpli);
	}

	public void remove(MountPointListItem lmpli) {
		local_mount_point_list.remove(lmpli);
	}

	public void sort() {
		sort(local_mount_point_list);
	};
	
	public static void sort(ArrayList<MountPointListItem>mpl) {
		Collections.sort(mpl, new Comparator<MountPointListItem>(){
			@Override
			public int compare(MountPointListItem lhs,
					MountPointListItem rhs) {
				String l_type="U", r_type="U";
				if (lhs.isSystemDefined) l_type="S";
				if (rhs.isSystemDefined) r_type="S";
				String l_key=l_type+lhs.local_mount_point_path;
				String r_key=r_type+rhs.local_mount_point_path;
				return l_key.compareToIgnoreCase(r_key);
			}
		});
	}

	private boolean mShowCheckBox=false;
	public void setShowCheckBox(boolean p) {mShowCheckBox=p;}
	public boolean isShowCheckBox() {return mShowCheckBox;}
	
	public boolean isEmptyAdapter() {
		boolean result=true;
		if (local_mount_point_list!=null) {
			if (local_mount_point_list.size()>0) {
				if (local_mount_point_list.get(0).local_mount_point_path!=null) result=false;
			}
		}
		return result;
	};
	
	public boolean isAnyItemSelected() {
		boolean result=false;
		if (local_mount_point_list!=null) {
			for(int i=0;i<local_mount_point_list.size();i++) {
				if (local_mount_point_list.get(i).isChecked && !local_mount_point_list.get(i).isSystemDefined) {
					result=true;
					break;
				}
			}
		}
		return result;
	};

	public int getItemSelectedCount() {
		int result=0;
		if (local_mount_point_list!=null) {
			for(int i=0;i<local_mount_point_list.size();i++) {
				if (local_mount_point_list.get(i).isChecked) {
					result++;
				}
			}
		}
		return result;
	};

	public void setAllItemChecked(boolean p) {
		if (local_mount_point_list!=null) {
			for(int i=0;i<local_mount_point_list.size();i++) {
				if (local_mount_point_list.get(i).isSystemDefined) local_mount_point_list.get(i).isChecked=false;
				else local_mount_point_list.get(i).isChecked=p;
			}
		}
		notifyDataSetChanged();
	};
	
	private ColorStateList cs_list=null;
	private Drawable ll_default=null;
	
	@Override
    final public View getView(final int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(textViewResourceId, null);
            holder=new ViewHolder();
            holder.cb_select=(CheckBox)v.findViewById(R.id.edit_lmp_list_item_checkbox);
            holder.tv_lmp_path=(CustomTextView)v.findViewById(R.id.edit_lmp_list_item_lmp_path);
            holder.tv_lmp_type=(TextView)v.findViewById(R.id.edit_lmp_list_item_lmp_type);
            
            holder.ll_view=(LinearLayout)v.findViewById(R.id.edit_lmp_list_item_lmp_view);
            
            if (ll_default!=null) ll_default=holder.ll_view.getBackground();
            if (cs_list==null) cs_list=holder.tv_lmp_type.getTextColors();
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final MountPointListItem o = getItem(position);
        if (o.local_mount_point_path!=null) {
    		holder.tv_lmp_path.setText(o.local_mount_point_path);
    		holder.tv_lmp_type.setVisibility(TextView.VISIBLE);
    		holder.tv_lmp_path.setTextColor(cs_list.getDefaultColor());
    		holder.tv_lmp_type.setTextColor(cs_list);
    		if (o.isSystemDefined) {
    			if (themeIsLight) holder.tv_lmp_path.setTextColor(Color.BLACK);
    			else holder.tv_lmp_path.setTextColor(Color.GRAY);
    			if (themeIsLight) holder.tv_lmp_type.setTextColor(Color.BLACK);
    			else holder.tv_lmp_type.setTextColor(Color.GRAY);
    			holder.tv_lmp_type.setText("S");
    			holder.cb_select.setEnabled(false);
    			holder.cb_select.setVisibility(CheckBox.INVISIBLE);
    		} else {
    			holder.tv_lmp_type.setText("U");
    			holder.cb_select.setEnabled(true);
        		if (mShowCheckBox) holder.cb_select.setVisibility(CheckBox.VISIBLE);
        		else holder.cb_select.setVisibility(CheckBox.INVISIBLE);
    		}
         	holder.cb_select.setOnCheckedChangeListener(new OnCheckedChangeListener() {
    			@Override
    			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//    				o.isChecked=isChecked;
    				Log.v("","c="+isChecked+", s="+mShowCheckBox);
    				getItem(position).isChecked=isChecked;
    				if (mCheckBoxClickListener!=null && mShowCheckBox) 
    					mCheckBoxClickListener.notifyToListener(true, new Object[]{isChecked});
    			}
    		});
         	holder.cb_select.setChecked(getItem(position).isChecked);
        } else {
    		holder.tv_lmp_path.setText(c.getString(R.string.msgs_edit_lmp_no_lmp_entry));
    		holder.tv_lmp_type.setVisibility(TextView.INVISIBLE);
    		holder.cb_select.setVisibility(TextView.GONE);
        }
        return v;
	};

	static class ViewHolder {
		LinearLayout ll_view;
		CheckBox cb_select;
		TextView tv_lmp_type;
		CustomTextView tv_lmp_path;
	}
}
class MountPointListItem {
	public boolean isChecked=false;
	public boolean isAvailable=false;
	public boolean isSystemDefined=false;
	public String local_mount_point_path=null;
}