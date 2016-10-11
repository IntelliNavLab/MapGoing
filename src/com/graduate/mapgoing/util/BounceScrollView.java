/*
 * 文件名：BounceScrollView
 * 描    述：上下拉弹簧效果
 * 作    者：陈警
 * 时    间：2016-06-01日写完，2016-10-08日整理
 * 版    权：©CopyRight 2012级武汉大学测绘学院——陈警
 */
package com.graduate.mapgoing.util;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.TranslateAnimation;
import android.widget.ScrollView;

public class BounceScrollView extends ScrollView {
	private View inner;
	private float y;
	private Rect normal = new Rect();
	private boolean isCount = false;
	public BounceScrollView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	protected void onFinishInflate() {
		if (getChildCount() > 0) {
			inner = getChildAt(0);
		}
	}

	public boolean onTouchEvent(MotionEvent ev) {
		if (inner != null) {
			commOnTouchEvent(ev);
		}
		return super.onTouchEvent(ev);
	}

	public void commOnTouchEvent(MotionEvent ev) {
		int action = ev.getAction();
		switch (action) {
		case MotionEvent.ACTION_DOWN:

			break;
		case MotionEvent.ACTION_UP:
			if (isNeedAnimation()) {
				animation();
				isCount = false;
			}
			break;
		case MotionEvent.ACTION_MOVE:
			final float preY = y;
			float nowY = ev.getY();
			int deltaY = (int) (preY - nowY);
			if (!isCount) {
				deltaY = 0;
			}
			y = nowY;
			if (isNeedMove()) {
				if (normal.isEmpty()) {
					normal.set(inner.getLeft(), inner.getTop(),
							inner.getRight(), inner.getBottom());
				}
				inner.layout(inner.getLeft(), inner.getTop() - deltaY / 2,
						inner.getRight(), inner.getBottom() - deltaY / 2);
			}
			isCount = true;
			break;
		default:
			break;
		}
	}
	
	public void animation(){
		TranslateAnimation ta = new TranslateAnimation(0, 0,inner.getTop(),normal.top);
		ta.setDuration(300);
		inner.startAnimation(ta);		
		inner.layout(normal.left, normal.top, normal.right, normal.bottom);
		normal.setEmpty();
	}	
	
	public boolean isNeedAnimation(){
		return !normal.isEmpty();
	}	
	
	public boolean isNeedMove(){
		int offset = inner.getMeasuredHeight() - getHeight();
		int scrollY = getScrollY();
		if (scrollY == 0 || scrollY == offset ) {
			return true;
		}
		return false;
	}

}
