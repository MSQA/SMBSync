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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.sentaroh.android.Utilities.Widget.CustomTextView;

class MsgListItem implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String msgCat, msgBody, msgDate, msgTime,msgIssuer;
	
	public MsgListItem(String cat, String mdate, String mtime, 
			String issuer, String msg)
	{
		msgCat=cat;
		msgBody= msg;
		msgDate=mdate;
		msgTime=mtime;
		msgIssuer=issuer;
	}
	public String getCat()
	{
		return msgCat;
	}
	public String getMdate()
	{
		return msgDate;
	}
	public String getMtime()
	{
		return msgTime;
	}
	public String getIssuer()
	{
		return msgIssuer;
	}
	public String getMsg()
	{
		return msgBody;
	}
	public String toString() {
		return msgCat+" "+msgDate+" "+msgTime+" "+
				(msgIssuer+"          ").substring(0,9)+msgBody;
	}
}


public class AdapterMessageList extends ArrayAdapter<MsgListItem> {

	private Context c;
	private int id;
	private ArrayList<MsgListItem>items;
	private boolean msgDataChanged=false;
	
	public AdapterMessageList(Context context, int textViewResourceId,
			ArrayList<MsgListItem> objects) {
		super(context, textViewResourceId, objects);
		c = context;
		id = textViewResourceId;
		items = objects;
	}
	
	final public void remove(int i) {
		items.remove(i);
		msgDataChanged=true;
	}

	@Override
	final public void add(MsgListItem mli) {
		items.add(mli);
		msgDataChanged=true;
		notifyDataSetChanged();
	}
	
	final public boolean resetDataChanged() {
		boolean tmp=msgDataChanged;
		msgDataChanged=false;
		return tmp;
	};
	
	@Override
	final public MsgListItem getItem(int i) {
		 return items.get(i);
	}
	
	final public ArrayList<MsgListItem> getAllItem() {return items;}
	
	final public void setAllItem(List<MsgListItem> p) {
		items.clear();
		if (p!=null) items.addAll(p);
		notifyDataSetChanged();
	}
	
//	@Override
//	public boolean isEnabled(int idx) {
//		 return getItem(idx).getActive().equals("A");
//	}

	@Override
	final public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		
        View v = convertView;
        if (v == null) {
            LayoutInflater vi = (LayoutInflater)c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(id, null);
            holder=new ViewHolder();
//            holder.tv_row_cat= (TextView) v.findViewById(R.id.msg_list_view_item_cat);
            holder.tv_row_msg= (CustomTextView) v.findViewById(R.id.msg_list_view_item_msg);
            holder.tv_row_time= (TextView) v.findViewById(R.id.msg_list_view_item_time);
            
    		Typeface tf=Typeface.create(Typeface.MONOSPACE, Typeface.NORMAL);
        	holder.tv_row_msg.setTypeface(tf);
        	holder.tv_row_msg.setLineBreak(CustomTextView.LINE_BREAK_NO_WORD_WRAP);

            holder.config=v.getResources().getConfiguration();
            v.setTag(holder);
        } else {
        	holder= (ViewHolder)v.getTag();
        }
        MsgListItem o = getItem(position);
        if (o != null) {
//       		wsz_w=activity.getWindow()
//    					.getWindowManager().getDefaultDisplay().getWidth();
//   			wsz_h=activity.getWindow()
//    					.getWindowManager().getDefaultDisplay().getHeight();
//    		
//    		if (wsz_w>=700) 
        		holder.tv_row_time.setVisibility(TextView.VISIBLE);
//        	else holder.tv_row_time.setVisibility(TextView.GONE);

        	if (o.getCat().equals("W")) {
        		holder.tv_row_time.setTextColor(Color.YELLOW);
        		holder.tv_row_msg.setTextColor(Color.YELLOW);
            	holder.tv_row_time.setText(o.getMtime());
            	holder.tv_row_msg.setText(o.getMsg());
        	} else if (o.getCat().equals("E")) {
        		holder.tv_row_time.setTextColor(Color.RED);
        		holder.tv_row_msg.setTextColor(Color.RED);
        		holder.tv_row_time.setText(o.getMtime());
            	holder.tv_row_msg.setText(o.getMsg());
        	} else {
        		holder.tv_row_time.setTextColor(Color.WHITE);
        		holder.tv_row_msg.setTextColor(Color.WHITE);
        		holder.tv_row_time.setText(o.getMtime());

            	holder.tv_row_msg.setText(o.getMsg());
        	}
       	}
        return v;
	};
	
	class ViewHolder {
//		TextView tv_row_cat;
		TextView tv_row_time;
		CustomTextView tv_row_msg;
		Configuration config;
	}
}

