/*
 * 文件名：StepCounterService
 * 描    述：后台计步服务，监听手机传感器数据，根据谷歌计步算法，实现计算步数的功能，
 * 并把结果广播发送给MainActivity
 * 作    者：陈警
 * 时    间：2016-06-01日写完，2016-10-08日整理
 * 版    权：©CopyRight 2012级武汉大学测绘学院——陈警
 */
package com.graduate.mapgoing;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.widget.Toast;
   
 public class StepCounterService extends Service {
	private SensorManager mSensorManager;// 传感器服务 

	private Timer julitimer = null;
	private TimerTask julitask = null;
	private Handler julihandle = null;

	// 计步
	public static int CURRENT_SETP = 0;//计步累积量
 	public static float SENSITIVITY = (float) 0.4; // 计步灵敏度

	private float mLastValues[] = new float[3 * 2];
	private float mScale[] = new float[2];
	private float mYOffset;
	private static long end = 0;
	private static long start = 0;
	/**
	 * 最后加速度方向
	 */
	private float mLastDirections[] = new float[3 * 2];
	private float mLastExtremes[][] = { new float[3 * 2], new float[3 * 2] };
	private float mLastDiff[] = new float[3 * 2];
	private int mLastMatch = -1;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	private void toast(String text) {
		Toast.makeText(getApplicationContext(), text, 0).show();
	}
	/**
	 * 创建服务后初始化参数
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		//获取设置中的灵敏度
		SharedPreferences setting = getSharedPreferences("limit", 0);
		SENSITIVITY = setting.getFloat("limit", 5);		
		int h = 480;
		mYOffset = h * 0.5f;
		mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
		mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
		// 获取传感器的服务，初始化传感器， 注册传感器，注册监听器
		mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
		mSensorManager.registerListener(myListener,
				mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
				SensorManager.SENSOR_DELAY_FASTEST);		 
		julihandle = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					break;
				default:
					break;
				}
			}
		};
		startTimer();
		toast("开始计步");
	}
	/**
	 * 开始计时
	 */
	private void startTimer() {
		if (julitimer == null) {
			julitimer = new Timer();
		}
		if (julitask == null) {
			julitask = new TimerTask() {
				@Override
				public void run() {
					sendMessage(0);
				}
			};
		}
		if (julitimer != null && julitask != null)
			julitimer.schedule(julitask, 0, 50);
	}
	/**
	 * 停止计时
	 */
	private void stopTimer() {
		if (julitimer != null) {
			julitimer.cancel();
			julitimer = null;
		}
		if (julitask != null) {
			julitask.cancel();
			julitask = null;
		}
	}
	/**
	 * 定时发送消息
	 */
	public void sendMessage(int id) {
		if (julihandle != null) {
			Message message = Message.obtain(julihandle, id);
			julihandle.sendMessage(message);
		}
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mSensorManager.unregisterListener(myListener); 
	}
	/**
	 * 获取命令
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		try {
			Bundle extras = intent.getExtras();
			int action = extras.getInt("action");
			switch (action) {
			case 1:				
				StepCut();
				break;
			}
		} catch (Exception er) {
		}
		return super.onStartCommand(intent, flags, startId);
	}

	/**
	 * 传感器数据监听，并计步
	 */
	private SensorEventListener myListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			Sensor sensor = event.sensor;
			synchronized (this) {
				if (sensor.getType() == Sensor.TYPE_ORIENTATION) {
				} else {
					int j = (sensor.getType() == Sensor.TYPE_ACCELEROMETER) ? 1:0;
					if (j == 1) {
						float vSum = 0;
						for (int i = 0; i < 3; i++) {
							float v = mYOffset + event.values[i] * mScale[j];
							vSum += v;
						}
						int k = 0;
						float v = vSum / 3;
						SharedPreferences setting = getSharedPreferences(
								"limit", 0);
						SENSITIVITY = setting.getFloat("limit", 30);
						float direction = (v > mLastValues[k] ? 1
								: (v < mLastValues[k] ? -1 : 0));
						if (direction == -mLastDirections[k]) {
							// 方向改变
							int extType = (direction > 0 ? 0 : 1); // 最大值或者最小值？
							mLastExtremes[extType][k] = mLastValues[k];
							float diff = Math.abs(mLastExtremes[extType][k]
									- mLastExtremes[1 - extType][k]);
							//判断参数是否大于灵敏度
							if (diff > SENSITIVITY) {
								boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
								boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
								boolean isNotContra = (mLastMatch != 1 - extType);
								if (isAlmostAsLargeAsPrevious
										&& isPreviousLargeEnough && isNotContra) {
									end = System.currentTimeMillis();
									if (end - start > 500) {
										// 此时判断为走了一步
										Log.i("StepDetector", "CURRENT_SETP:" + CURRENT_SETP);
										CURRENT_SETP++;
										Intent intentstep = new Intent("step");
										intentstep.putExtra("stepcount", CURRENT_SETP);
										sendBroadcast(intentstep);
										mLastMatch = extType;
										start = end;
									}
								} else {
									mLastMatch = -1;
								}
							}
							mLastDiff[k] = diff;
						}
						mLastDirections[k] = direction;
						mLastValues[k] = v;
					}
				}
			}
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};
	/**
	 * 若发现当前计步有误，应减去一步
	 */
	public void StepCut() {
		CURRENT_SETP--;
	}
}
