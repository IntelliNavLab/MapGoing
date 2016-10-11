/*
 * 文件名：MyApplication
 * 描    述：全局类
 * 作    者：陈警
 * 时    间：2016-06-01日写完，2016-10-08日整理
 * 版    权：©CopyRight 2012级武汉大学测绘学院——陈警
 */
package com.graduate.mapgoing;

import android.app.Application;

public class MyApplication extends Application {
	// 系统误差全局变量，手抖校正参数变量
	public static float VACCX = (float) 0.702, VACCY = (float) 0.532,
			VACCZ = (float) 0.667;
	public static float AccX = (float) -0.005, AccY = (float) -0.002,
			AccZ = (float) -0.371, handVVaccX = 0, handVVaccY = 0,
			handVVaccZ = 0, handaccX = 0, handaccY = 0, handaccZ = 0;

	@Override
	public void onCreate() {
		super.onCreate();
	}

}