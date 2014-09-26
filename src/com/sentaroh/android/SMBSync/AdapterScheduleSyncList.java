package com.sentaroh.android.SMBSync;

import static com.sentaroh.android.SMBSync.Constants.*;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class AdapterScheduleSyncList extends ArrayAdapter<String>{
	private int layout_id=0;
	private Context context=null;
	private int text_color=0;
	public AdapterScheduleSyncList(Context c, int textViewResourceId) {
		super(c, textViewResourceId);
		layout_id=textViewResourceId;
		context=c;
	}
	
	@Override
	public View getView(final int position, View convertView, final ViewGroup parent) {
		final ViewHolder holder;
		final String o = getItem(position);
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(layout_id, null);
            holder=new ViewHolder();
            holder.tv_name=(TextView)v.findViewById(android.R.id.text1);
            text_color=holder.tv_name.getCurrentTextColor();
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        if (o != null) {
        	holder.tv_name.setText(o.substring(1));
        	if (o.substring(0, 1).equals(SMBSYNC_PROF_ACTIVE)) {
        		holder.tv_name.setTextColor(text_color);
        	} else {
        		holder.tv_name.setTextColor(Color.DKGRAY);
        	}
        }
        return v;
	
	}
	class ViewHolder {
		TextView tv_name;
	};

}
