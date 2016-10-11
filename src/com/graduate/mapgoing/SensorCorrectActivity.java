/*
 * 文件名：SensorCorrectActivity
 * 描    述：采集传感器数据，计算系统误差校正参数，计算手掌抖动校正参数
 * 作    者：陈警
 * 时    间：2016-06-01日写完，2016-10-08日整理
 * 版    权：©CopyRight 2012级武汉大学测绘学院——陈警
 */
package com.graduate.mapgoing;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.graduate.mapgoing.R.id;

public class SensorCorrectActivity extends Activity {
	// UI控件
	private TextView zuoyou, qianhou, accX, accY, accZ, share;
	private TextView back, resultx, resulty, resultz, zhidao;
	private Button jiaozheng;
	private Button endCollect;
	private EditText dataname;
	private RadioGroup state;

	// 传感器相关变量
	private SensorManager LinearAcceleration;
	private SensorManager Orientation;
	private Sensor linearAcceleration;
	private Sensor orientation;
	private float G = SensorManager.STANDARD_GRAVITY;
	private float accx = 0, accy = 0, accz = 0;
	private float yow = 0, pitch = 0, roll = 0;

	// 计时器
	private Timer julitimer = null;
	private TimerTask julitask = null;
	private Handler julihandle = null;
	// 数据存储
	private FileHelper helper;
	String ACCXValue = "", ACCYValue = "", ACCZValue = "";
	private static Long longcount = (long) 0;
	private List<Float> ACCX = new ArrayList<Float>();
	private List<Float> ACCY = new ArrayList<Float>();
	private List<Float> ACCZ = new ArrayList<Float>();
	private List<Float> ACCXYZ = new ArrayList<Float>();
	private boolean isstart = false;
	private int statestate = 1;
	private MyApplication app;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.jiaozheng);
		app = (MyApplication) getApplication();
		zuoyou = (TextView) findViewById(R.id.zuoyou);
		qianhou = (TextView) findViewById(R.id.qianhou);
		accX = (TextView) findViewById(R.id.x);
		accY = (TextView) findViewById(R.id.y);
		accZ = (TextView) findViewById(R.id.z);
		share = (TextView) findViewById(R.id.wancheng);
		back = (TextView) findViewById(R.id.jiaofanhui);
		resultx = (TextView) findViewById(R.id.resultaccx);
		resulty = (TextView) findViewById(R.id.resultaccy);
		resultz = (TextView) findViewById(R.id.resultaccz);
		zhidao = (TextView) findViewById(R.id.zhidao);
		state = (RadioGroup) findViewById(id.state);
		jiaozheng = (Button) findViewById(id.jiaozheng);
		endCollect = (Button) findViewById(id.jieshucaiji);
		dataname = (EditText) findViewById(id.dataname);

		LinearAcceleration = (SensorManager) getSystemService(SENSOR_SERVICE);
		linearAcceleration = LinearAcceleration.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		Orientation = (SensorManager) getSystemService(SENSOR_SERVICE);
		orientation = Orientation.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		longcount = (long) 0;
		helper = new FileHelper(getApplicationContext());
		startsensor();
		state.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup radio, int arg1) {
				int id = radio.getCheckedRadioButtonId();
				if (id == R.id.jingzhi) {
					statestate = 1;
					zhidao.setText("    请尽可能将手机水平静止放置（保持左右倾角，前后倾角均为0°），"
							+ "然后点击“开始校正”按钮，保持静止10s自动完成校正，点击“校正完成”返回");
					longcount = (long) 0;
				}
				if (id == R.id.shouwo) {
					statestate = 2;
					zhidao.setText("    请将手机像往常一样握在手中，水平静止放置（保持左右倾角，前后倾角均为0°），"
							+ "然后点击“开始校正”按钮，保持静止10s自动完成校正，点击“校正完成”返回");
					longcount = (long) 0;
				}
			}
		});
		back.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
		share.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				finish();
			}
		});
		jiaozheng.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				if (isstart == false) {
					startTimer();
					isstart = true;
				} else {
					isstart = false;
					ACCX.clear();
					ACCY.clear();
					ACCZ.clear();
					ACCXYZ.clear();
					longcount = (long) 0;
				}
			}
		});
		endCollect.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				sendMessage(1);
			}
		});

		julihandle = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					jiaozheng.setText("请保持" + longcount + "秒");
					jiaozheng.setEnabled(false);
					break;
				case 1:
					jiaozheng.setText("开  始  校  正");
					toast("校正成功");
					writeData();
					saveData();
					jiaozheng.setEnabled(true);
					stopTimer();
					break;
				default:
					break;
				}
			}
		};
	}
/**
 * 根据不同状态保存数据到txt文件
 */
	protected void writeData() {
		if (statestate == 1) {
			String txtx = "WalkX" + dataname.getText() + ".txt";
			String txty = "WalkY" + dataname.getText() + ".txt";
			String txtz = "WalkZ" + dataname.getText() + ".txt";
			helper.writeSDFile(ACCXValue, txtx);
			helper.writeSDFile(ACCYValue, txty);
			helper.writeSDFile(ACCZValue, txtz);
			toast("记录成功" + "步行数据采集.txt");
			ACCXValue = "";
			ACCYValue = "";
			ACCZValue = "";
		}
		if (statestate == 2) {
			String txtx = "HandErrorX.txt";
			String txty = "HandErrorY.txt";
			String txtz = "HandErrorZ.txt";
			helper.writeSDFile(ACCXValue, txtx);
			helper.writeSDFile(ACCYValue, txty);
			helper.writeSDFile(ACCZValue, txtz);
			toast("记录成功" + "手抖误差.txt");
			ACCXValue = "";
			ACCYValue = "";
			ACCZValue = "";
		}
		String name = dataname.getText() + "合加速度_" + longcount + ".txt";
		helper.writeSDFile(ACCXYZValue, name);
		ACCXYZValue = "";
		toast("记录成功:" + name);
	}
/**
 * 消息提示函数
 */
	private void toast(String text) {
		Toast.makeText(getApplicationContext(), text, 0).show();
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
					longcount++;
				}
			};
		}
		if (julitimer != null && julitask != null)
			julitimer.schedule(julitask, 100, 1000);
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
 * 定时发送任务消息
 */
	public void sendMessage(int id) {
		if (julihandle != null) {
			Message message = Message.obtain(julihandle, id);
			julihandle.sendMessage(message);
		}
	}
/**
 * 分情况保存校正参数
 */
	private void saveData() {
		final SharedPreferences setting = getSharedPreferences("jiaozheng", 0);
		if (statestate == 1) {
			float eaccx = mean(ACCX);
			float eaccy = mean(ACCY);
			float eaccz = mean(ACCZ);
			if (Math.abs(eaccx - 0.3) > 0 || Math.abs(eaccy - 0.3) > 0) {
				Toast.makeText(SensorCorrectActivity.this, "检测静止加速度太大，请重新校正",
						Toast.LENGTH_SHORT).show();
			} else {
				float vaccx = variance(ACCX, eaccx);
				float vaccy = variance(ACCY, eaccy);
				float vaccz = variance(ACCZ, eaccz);
				app.VACCX = vaccx;
				app.VACCY = vaccy;
				app.VACCZ = vaccz;
				String accxstr = String.format("%.3f", eaccx);
				String accystr = String.format("%.3f", eaccy);
				String acczstr = String.format("%.3f", eaccz);
				String accxv = String.format("%.3f", Math.sqrt(vaccx));
				String accyv = String.format("%.3f", Math.sqrt(vaccy));
				String acczv = String.format("%.3f", Math.sqrt(vaccz));
				String vx = "X轴加速度随机误差均值：" + accxstr + "，方差：" + accxv;
				String vy = "Y轴加速度随机误差均值：" + accystr + "，方差：" + accyv;
				String vz = "Z轴加速度随机误差均值：" + acczstr + "，方差：" + acczv;
				resultx.setText(vx);
				resulty.setText(vy);
				resultz.setText(vz);
				app.AccX = eaccx;
				app.AccY = eaccy;
				app.AccZ = eaccz;

				setting.edit().putFloat("AccX", eaccx).commit();
				setting.edit().putFloat("AccY", eaccy).commit();
				setting.edit().putFloat("AccZ", eaccz).commit();
				setting.edit().putFloat("VACCX", vaccx).commit();
				setting.edit().putFloat("VACC", vaccy).commit();
				setting.edit().putFloat("VACCZ", vaccz).commit();
				Toast.makeText(SensorCorrectActivity.this, "校正成功\n数据已保存",
						Toast.LENGTH_SHORT).show();
			}
		}
		if (statestate == 2) {
			float eaccx = mean(ACCX);
			float eaccy = mean(ACCY);
			float eaccz = mean(ACCZ);
			if (Math.abs(eaccx - 0.3) > 0 || Math.abs(eaccy - 0.3) > 0) {
				Toast.makeText(SensorCorrectActivity.this, "检测手握状态加速度太大，请重新校正",
						Toast.LENGTH_SHORT).show();
			} else {
				float vaccx = variance(ACCX, eaccx);
				float vaccy = variance(ACCY, eaccy);
				float vaccz = variance(ACCZ, eaccz);
				app.handVVaccX = vaccx;
				app.handVVaccY = vaccy;
				app.handVVaccZ = vaccz;
				String accxstr = String.format("%.3f", eaccx);
				String accystr = String.format("%.3f", eaccy);
				String acczstr = String.format("%.3f", eaccz);
				String accxv = String.format("%.3f", Math.sqrt(vaccx));
				String accyv = String.format("%.3f", Math.sqrt(vaccy));
				String acczv = String.format("%.3f", Math.sqrt(vaccz));
				String vx = "X轴加速度随机误差均值：" + accxstr + "，方差：" + accxv;
				String vy = "Y轴加速度随机误差均值：" + accystr + "，方差：" + accyv;
				String vz = "Z轴加速度随机误差均值：" + acczstr + "，方差：" + acczv;
				resultx.setText(vx);
				resulty.setText(vy);
				resultz.setText(vz);
				app.handaccX = eaccx;
				app.handaccX = eaccy;
				app.handaccX = eaccz;

				setting.edit().putFloat("handaccX", eaccx).commit();
				setting.edit().putFloat("handaccY", eaccy).commit();
				setting.edit().putFloat("handaccZ", eaccz).commit();
				setting.edit().putFloat("handVVaccX", vaccx).commit();
				setting.edit().putFloat("handVVaccY", vaccy).commit();
				setting.edit().putFloat("handVVaccZ", vaccz).commit();
				Toast.makeText(SensorCorrectActivity.this, "校正成功\n数据已保存",
				Toast.LENGTH_SHORT).show();
			}
		}
	}
/**
 * 计算方差
 */
	private float variance(List<Float> acc, float eacc) {
		float s = 0;
		for (float value : acc) {
			s = s + (value - eacc) * (value - eacc);
		}
		float result = s / (acc.size());
		result = (float) Math.sqrt(result);
		return result;
	}
/**
 * 计算平均值
 */
	private float mean(List<Float> acc) {
		float s = 0;
		for (float a : acc) {
			s = s + a;
		}
		float result = s / (acc.size());
		return result;
	}
/**
 * 开启传感器
 */
	private void startsensor() {
		LinearAcceleration.registerListener(myListener3, linearAcceleration,
				SensorManager.SENSOR_DELAY_NORMAL);
		Orientation.registerListener(myListener4, orientation,
				SensorManager.SENSOR_DELAY_NORMAL);
	}
/**
 * 结束传感器
 */
	private void endsensor() {
		LinearAcceleration.unregisterListener(myListener3);
		Orientation.unregisterListener(myListener4);
	}
/**
 * 传感器数据监听，移动平均滤波，保存采集变量
 */
	List<Float> yidongpingjunX = new ArrayList<Float>();
	List<Float> yidongpingjunY = new ArrayList<Float>();
	List<Float> yidongpingjunZ = new ArrayList<Float>();
	int K = 15;
	String ACCXYZValue = "";
	Long lasttime = (long) 0, nowtime = (long) 0;
	private SensorEventListener myListener3 = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] v = event.values;
			nowtime = System.currentTimeMillis();
//			jiaozheng.setText(nowtime - lasttime + "");//真正采集时间确定，并非真正地15HZ，采集间隔是0.18s一次5.5HZ
			lasttime = nowtime;
			accx = v[0];
			accy = v[1];
			accz = v[2];
			ACCXValue = ACCXValue + accx + "\n";
			ACCYValue = ACCYValue + accy + "\n";
			ACCZValue = ACCZValue + accz + "\n";

			String accxstr = String.format("%.6f", accx);
			String accystr = String.format("%.6f", accy);
			String acczstr = String.format("%.6f", accz);
			accX.setText("" + accxstr);
			accY.setText("" + accystr);
			accZ.setText("" + acczstr);

			yidongpingjunX.add(v[0]);
			yidongpingjunY.add(v[1]);
			yidongpingjunZ.add(v[2]);
			if (yidongpingjunX.size() > K) {
				double ssX = 0, ssY = 0, ssZ = 0;
				for (int i = 0; i < yidongpingjunX.size(); i++) {
					ssX = ssX + yidongpingjunX.get(i);
					ssY = ssY + yidongpingjunY.get(i);
					ssZ = ssZ + yidongpingjunZ.get(i);
				}
				v[0] = (float) (ssX / yidongpingjunX.size());
				v[1] = (float) ssY / yidongpingjunY.size();
				v[2] = (float) ssZ / yidongpingjunZ.size();
				yidongpingjunX.remove(0);
				yidongpingjunY.remove(0);
				yidongpingjunZ.remove(0);
			} else {
			}
			// 采集合加速度值
			double heacc = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
			ACCXYZ.add((float) heacc);
			ACCXYZValue = ACCXYZValue + (float) heacc + "\n";
			ACCX.add(accx);
			ACCY.add(accy);
			ACCZ.add(accz);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};

	@Override
	protected void onResume() {
		startsensor();
		super.onResume();
	}

	@Override
	protected void onPause() {
		endsensor();
		super.onPause();
	}
/**
 * 方向传感器，显示手机设备倾斜角度，便于将手机保持水平，前后左右倾角为0时，手机水平
 */
	private SensorEventListener myListener4 = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] values = event.values;
			yow = values[0];
			pitch = values[1];
			roll = values[2];
			qianhou.setText("前后倾角:" + pitch);
			zuoyou.setText("左右倾角:" + roll);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};

}
