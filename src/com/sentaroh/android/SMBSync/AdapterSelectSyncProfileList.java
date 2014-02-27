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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class AdapterSelectSyncProfileList extends ArrayAdapter<String> {
	private Context c;
	private int id;
	private ArrayList<String> items;
	
	public AdapterSelectSyncProfileList(Context context, int textViewResourceId,
			ArrayList<String> objects) {
		super(context, textViewResourceId, objects);
		c=context;
		id=textViewResourceId;
		items=objects;
	}

	public String getItem(int i) {return items.get(i);}
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		final ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
            holder.tv_row_profilename= 
            		(TextView) v.findViewById(R.id.sync_profile_list_item_profilename);
            holder.iv_row_icon= 
            		(ImageView) v.findViewById(R.id.sync_profile_list_item_icon);
            holder.no_profile_entry=c.getString(R.string.msgs_no_profile);
            
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final String o = getItem(position);

        if (o != null ) {
        	if (!o.equals(holder.no_profile_entry)) {
        		holder.iv_row_icon.setVisibility(View.VISIBLE);
        		if ((o.substring(0,1)).equals("R")) 
        			 holder.iv_row_icon.setImageResource(R.drawable.ic_32_server) ;
        		else holder.iv_row_icon.setImageResource(R.drawable.ic_32_mobile) ;
        	} else holder.iv_row_icon.setVisibility(View.GONE);
       		holder.tv_row_profilename.setText(o.substring(2,o.length()));
       		
        }
   		return v;
	};
	
	static class ViewHolder {
		TextView tv_row_profilename;
		ImageView iv_row_icon;
//		Button btn_row_delbtn;
//		EditText et_filter;
//		RadioButton rb_inc,rb_exc;
//		LinearLayout ll_entry;
		String no_profile_entry;
		
	}

}
