package com.sentaroh.android.SMBSync;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class AboutViewPager extends ViewPager{
	public AboutViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public AboutViewPager(Context context) {
		super(context);
		init();
	}
	
	private void init() {
//		setPageTransformer(false, new ViewPager.PageTransformer() {
//		    @Override
//		    public void transformPage(View page, float position) {
//		    	final float normalizedposition = Math.abs(Math.abs(position) - 1);
//
//		        page.setScaleX(normalizedposition / 2 + 0.5f);
//		        page.setScaleY(normalizedposition / 2 + 0.5f);
//		    } 
//		});
	}

}
