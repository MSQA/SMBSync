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

import static com.sentaroh.android.SMBSync.Constants.SMBSYNC_PROF_TYPE_REMOTE;
import android.content.Context;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class SpinnerAdapterProfileSelection extends ArrayAdapter<String> {
	
//	private int mResourceId;
	private Context mContext;
	@SuppressWarnings("unused")
	private int mTextColor=0;
	@SuppressWarnings("unused")
	private int mTextSize=0;

	public void setTextColor(int color) {mTextColor=color;}
	public void setTextSize(int size_sp) {mTextSize=size_sp;}
	
	public SpinnerAdapterProfileSelection(Context c, int textViewResourceId) {
		super(c, textViewResourceId);
//		mResourceId=textViewResourceId;
		mContext=c;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
        TextView view;
        if (convertView == null) {
//        	LayoutInflater vi = (LayoutInflater)mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//          view = (TextView)vi.inflate(mResourceId, null);
          view=(TextView)super.getView(position,convertView,parent);
        } else {
            view = (TextView)convertView;
        }
        String type=getItem(position).substring(0, 1);
        String name=getItem(position).substring(2);
        view.setText(name);
        view.setGravity(Gravity.CENTER_VERTICAL);
        view.setCompoundDrawablePadding(10);
        if (type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
            view.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(android.R.drawable.arrow_down_float),
            		null,
            		mContext.getResources().getDrawable(R.drawable.ic_32_server), 
            		null);
        } else {
            view.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(android.R.drawable.arrow_down_float),
            		null,
            		mContext.getResources().getDrawable(R.drawable.ic_32_mobile), 
            		null);
        }

//        if (text_color!=0) view.setTextColor(text_color);
//        if (text_size!=0) view.setTextSize(text_size);

        return view;
	}
	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView text;
        text=(TextView)super.getDropDownView(position, convertView, parent);
//        text.setBackgroundColor(Color.LTGRAY);
        String type=getItem(position).substring(0, 1);
        String name=getItem(position).substring(2);
        text.setText(name);
        if (Build.VERSION.SDK_INT>=11) {
            if (type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
            	text.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(android.R.drawable.btn_radio),
                		null,
                		mContext.getResources().getDrawable(R.drawable.ic_32_server), 
                		null);
            } else {
            	text.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(android.R.drawable.btn_radio),
                		null,
                		mContext.getResources().getDrawable(R.drawable.ic_32_mobile), 
                		null);
            }
        } else {
            if (type.equals(SMBSYNC_PROF_TYPE_REMOTE)) {
            	text.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.ic_32_server),
            			null,
                		null,
                		 
                		null);
            } else {
            	text.setCompoundDrawablesWithIntrinsicBounds(mContext.getResources().getDrawable(R.drawable.ic_32_mobile),
            			null,
                		null,null);
            }
        }

//        if (text_color!=0) text.setTextColor(text_color);
//        if (text_size!=0) text.setTextSize(text_size);
        
        return text;
	}
	
}
