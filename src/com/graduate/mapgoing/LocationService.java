/*
 * 文件名：LocationService
 * 描    述：后台定位服务，不断广播发送定位结果数据给MainActivity，解决MainActivity因返回桌面不能持续定位问题
 * 作    者：陈警
 * 时    间：2016-06-01日写完，2016-10-08日整理
 * 版    权：©CopyRight 2012级武汉大学测绘学院——陈警
 */
package com.graduate.mapgoing;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.LocationSource.OnLocationChangedListener;
import com.amap.api.maps2d.model.LatLng;

public class LocationService extends Service {
	// 定位相关变量
	private OnLocationChangedListener mListener;
	private AMapLocationListener aListener = null;
	private AMapLocationClient locationclient;
	private AMapLocationClientOption mLocationOption = null;

	private Intent intent = new Intent();
	private double weidu = 0, jingdu = 0;
	private float accuracy = 0;
	private boolean isGPSOK = false;

	@Override
	public void onCreate() {
		locationclient = new AMapLocationClient(getApplicationContext());
		toast("服务开启");
		startlocation();
		super.onCreate();
	}

	/**
	 * 消息弹出函数
	 * 
	 * @param text
	 *            消息文本
	 */
	private void toast(String text) {
		Toast.makeText(getApplicationContext(), text, 0).show();
	}

	/**
	 * 打开服务执行命令
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * 定位参数设置
	 */
	private void setLocationparams() {
		mLocationOption = new AMapLocationClientOption();
		mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
		mLocationOption.setNeedAddress(true);
		mLocationOption.setOnceLocation(false);
		mLocationOption.setWifiActiveScan(true);
		mLocationOption.setMockEnable(false);
		mLocationOption.setInterval(500);
		locationclient.setLocationOption(mLocationOption);
	}

	/**
	 * 启动定位
	 */
	public void startlocation() {
		setLocationparams();
		aListener = new AMapLocationListener() {
			@Override
			public void onLocationChanged(AMapLocation amapLocation) {
				if (amapLocation != null && (amapLocation).getErrorCode() == 0) {
					jingdu = amapLocation.getLongitude();
					weidu = amapLocation.getLatitude();
					accuracy = amapLocation.getAccuracy();
					// 广播发送定位结果，并根据是否有GPS，发送不同类型 　
					if (amapLocation.getLocationType() == AMapLocation.LOCATION_TYPE_GPS) {
						isGPSOK = true;
					} else {
						isGPSOK = false;
					}
					Bundle bundle = new Bundle();
					intent.setAction("location");
					bundle.putDouble("weidu", weidu);
					bundle.putDouble("jingdu", jingdu);
					bundle.putDouble("accuracy", accuracy);
					bundle.putBoolean("isGPSOK", isGPSOK);
					intent.putExtras(bundle);
					sendBroadcast(intent);
				}
			}
		};
		locationclient.setLocationListener(aListener);
		locationclient.startLocation();
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
