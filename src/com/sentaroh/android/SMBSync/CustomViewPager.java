package com.sentaroh.android.SMBSync;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;

public class CustomViewPager extends ViewPager{

	public CustomViewPager(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public CustomViewPager(Context context) {
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
