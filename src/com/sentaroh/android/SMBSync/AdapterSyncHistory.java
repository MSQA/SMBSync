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

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class AdapterSyncHistory extends ArrayAdapter<SyncHistoryListItem> {
	private Context c;
	private int id;
	private ArrayList<SyncHistoryListItem> items;
	
	public AdapterSyncHistory(Context context, int textViewResourceId,
			ArrayList<SyncHistoryListItem> objects) {
		super(context, textViewResourceId, objects);
		c=context;
		id=textViewResourceId;
		items=objects;
	}

	@Override
	public SyncHistoryListItem getItem(int i) {return items.get(i);}
	@Override
	public int getCount() {return items.size();}
	
	public void remove(int pos) {items.remove(pos);}
	@Override
	public void remove(SyncHistoryListItem p) {items.remove(p);}
	
	public ArrayList<SyncHistoryListItem> getSyncHistoryList() {return items;}
	
	public void setSyncHistoryList(ArrayList<SyncHistoryListItem> p) {items=p;}
	
	@Override
    public View getView(final int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
    		holder.cb_sel=(CheckBox)v.findViewById(R.id.sync_history_list_view_cb);
    		holder.tv_date=(TextView)v.findViewById(R.id.sync_history_list_view_date);
    		holder.tv_time=(TextView)v.findViewById(R.id.sync_history_list_view_time);
    		holder.tv_prof=(TextView)v.findViewById(R.id.sync_history_list_view_prof);
    		holder.tv_status=(TextView)v.findViewById(R.id.sync_history_list_view_status);
    		holder.tv_cnt_copied=(TextView)v.findViewById(R.id.sync_history_list_view_count_copied);
    		holder.tv_cnt_deleted=(TextView)v.findViewById(R.id.sync_history_list_view_count_deleted);
    		holder.tv_cnt_ignored=(TextView)v.findViewById(R.id.sync_history_list_view_count_ignored);
    		holder.tv_error=(TextView)v.findViewById(R.id.sync_history_list_view_error_text);
    		holder.ll_count=(LinearLayout)v.findViewById(R.id.sync_history_list_view_count);
    		holder.ll_main=(LinearLayout)v.findViewById(R.id.sync_history_list_view);
            
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final SyncHistoryListItem o = getItem(position);

        if (o != null ) {
        	if (!o.sync_prof.equals("No history")) {
            	holder.tv_date.setVisibility(TextView.VISIBLE);
            	holder.tv_time.setVisibility(TextView.VISIBLE);
            	holder.tv_status.setVisibility(TextView.VISIBLE);
            	holder.ll_count.setVisibility(TextView.VISIBLE);
             	holder.cb_sel.setVisibility(TextView.VISIBLE);

             	holder.tv_date.setText(o.sync_date);
            	holder.tv_time.setText(o.sync_time);
            	holder.tv_prof.setText(o.sync_prof);
            	String st_text="";
            	if (o.sync_status==SyncHistoryListItem.SYNC_STATUS_SUCCESS) {
            		st_text=c.getString(R.string.msgs_sync_history_status_success);
            		holder.tv_status.setTextColor(Color.LTGRAY);
            	} else if (o.sync_status==SyncHistoryListItem.SYNC_STATUS_ERROR) {
            		st_text=c.getString(R.string.msgs_sync_history_status_fail);
            		holder.tv_status.setTextColor(Color.RED);
            	} else if (o.sync_status==SyncHistoryListItem.SYNC_STATUS_CANCELLED) {
            		st_text=c.getString(R.string.msgs_sync_history_status_cancel);
            		holder.tv_status.setTextColor(Color.YELLOW);
            	}
            	holder.tv_status.setText(st_text);
            	holder.tv_cnt_copied.setText(Integer.toString(o.sync_result_no_of_copied));
            	holder.tv_cnt_deleted.setText(Integer.toString(o.sync_result_no_of_deleted));
            	holder.tv_cnt_ignored.setText(Integer.toString(o.sync_result_no_of_ignored));
            	
            	if (o.sync_error_text!=null && !o.sync_error_text.equals("")) {
           			holder.tv_error.setVisibility(TextView.VISIBLE);
           			holder.tv_error.setText(o.sync_error_text);
           		} else {
           			holder.tv_error.setVisibility(TextView.GONE);
           		}
           		
             	holder.cb_sel.setOnCheckedChangeListener(new OnCheckedChangeListener() {
     				@Override
     				public void onCheckedChanged(CompoundButton buttonView,
    						boolean isChecked) {
//     					o.isChecked=isChecked;
     					items.get(position).isChecked=isChecked;
     				}}
             	);
             	holder.cb_sel.setChecked(items.get(position).isChecked);
        	} else {
        		holder.tv_prof.setText(o.sync_prof);

        		holder.tv_date.setVisibility(TextView.GONE);
            	holder.tv_time.setVisibility(TextView.GONE);
            	holder.tv_status.setVisibility(TextView.GONE);
            	holder.ll_count.setVisibility(TextView.GONE);
       			holder.tv_error.setVisibility(TextView.GONE);
             	holder.cb_sel.setVisibility(TextView.GONE);
        	}

        }
   		return v;
	};
	
	static class ViewHolder {
		CheckBox cb_sel;
		TextView tv_date,tv_time,tv_prof, tv_status,tv_cnt_copied, tv_cnt_deleted, tv_cnt_ignored;
		TextView tv_error;
		LinearLayout ll_count, ll_main;
	}

}
class SyncHistoryListItem {
	public boolean isChecked=false;
	
	public String sync_date=null;
	public String sync_time=null;
	public String sync_prof=null;
	public int sync_status=0;
	public final static int SYNC_STATUS_SUCCESS=0;
	public final static int SYNC_STATUS_CANCELLED=1;
	public final static int SYNC_STATUS_ERROR=2;
	
	public int sync_result_no_of_copied=0;
	public int sync_result_no_of_deleted=0;
	public int sync_result_no_of_ignored=0;
	
	public String sync_error_text="";
	
	public String sync_log_file_path="";
	public boolean isLogFileAvailable=false;
	
	public String sync_result_file_path="";
}