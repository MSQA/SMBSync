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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

import com.sentaroh.android.Utilities.NotifyEventCompletion;

class ExportImportProfileListItem {
	public boolean isChecked=false;
	public String item_type="";
	public String item_name="";
};

public class AdapterExportImportProfileList extends ArrayAdapter<ExportImportProfileListItem> {
	private Context c;
	private int id;
	private ArrayList<ExportImportProfileListItem>items;
	
	public AdapterExportImportProfileList(Context context, 
			int textViewResourceId, ArrayList<ExportImportProfileListItem> objects) {
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items=objects;
	};
	
	@Override
	final public int getCount() {
		return items.size();
	}

	public boolean isItemSelected() {
		boolean result=false;
		
		for (int i=0;i<items.size();i++) {
			if (items.get(i).isChecked) {
				result=true;
				break;
			}
		}
		
		return result;
	}
	
	private NotifyEventCompletion cb_ntfy=null;
	final public void setCheckButtonListener(NotifyEventCompletion ntfy) {
		cb_ntfy=ntfy;
	};
	
	@Override
	final public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
            holder.cb_item= (CheckBox) v.findViewById(R.id.export_import_profile_list_item_cb1);
            holder.tv_itemtype= (TextView) v.findViewById(R.id.export_import_profile_list_item_itemtype);
            holder.tv_itemname= (TextView) v.findViewById(R.id.export_import_profile_list_item_itemname);
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        final ExportImportProfileListItem o = items.get(position);
        if (o != null) {
        	holder.tv_itemname.setText(o.item_name);
        	holder.tv_itemtype.setText(o.item_type);
    		// 必ずsetChecked前にリスナを登録
            holder.cb_item.setOnCheckedChangeListener(new OnCheckedChangeListener() {
    			@Override
    			public void onCheckedChanged(CompoundButton buttonView,
    				boolean isChecked) {
    				o.isChecked=isChecked;
    				if (cb_ntfy!=null) cb_ntfy.notifyToListener(true, null);
    			}
    		});
        	holder.cb_item.setChecked(o.isChecked);
       	}
        return v;
	};

	class ViewHolder {
		TextView tv_itemname, tv_itemtype;
		CheckBox cb_item;
	}
}
