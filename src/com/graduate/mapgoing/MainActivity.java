/*
 * 文件名：MainActivity
 * 描    述：利用PDR/INS/GPS方法，集成手机多传感器定位算法实现代码
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.amap.api.location.AMapLocation;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.location.AMapLocationClientOption.AMapLocationMode;
import com.amap.api.location.AMapLocationListener;
import com.amap.api.maps2d.AMap;
import com.amap.api.maps2d.AMap.CancelableCallback;
import com.amap.api.maps2d.AMap.InfoWindowAdapter;
import com.amap.api.maps2d.AMap.OnCameraChangeListener;
import com.amap.api.maps2d.AMap.OnInfoWindowClickListener;
import com.amap.api.maps2d.AMap.OnMapClickListener;
import com.amap.api.maps2d.AMap.OnMapLoadedListener;
import com.amap.api.maps2d.AMap.OnMapLongClickListener;
import com.amap.api.maps2d.AMap.OnMapTouchListener;
import com.amap.api.maps2d.AMap.OnMarkerClickListener;
import com.amap.api.maps2d.AMapOptions;
import com.amap.api.maps2d.AMapUtils;
import com.amap.api.maps2d.CameraUpdate;
import com.amap.api.maps2d.CameraUpdateFactory;
import com.amap.api.maps2d.LocationSource;
import com.amap.api.maps2d.MapView;
import com.amap.api.maps2d.model.BitmapDescriptor;
import com.amap.api.maps2d.model.BitmapDescriptorFactory;
import com.amap.api.maps2d.model.CameraPosition;
import com.amap.api.maps2d.model.Circle;
import com.amap.api.maps2d.model.LatLng;
import com.amap.api.maps2d.model.Marker;
import com.amap.api.maps2d.model.MarkerOptions;
import com.amap.api.maps2d.model.MyLocationStyle;
import com.amap.api.maps2d.model.Polyline;
import com.amap.api.maps2d.model.PolylineOptions;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch.OnPoiSearchListener;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RouteSearch.OnRouteSearchListener;
import com.amap.api.services.route.WalkRouteResult;
import com.graduate.mapgoing.R.id;
import com.graduate.mapgoing.util.Matrix;

public class MainActivity extends Activity implements LocationSource,
		AMapLocationListener, OnMarkerClickListener, OnInfoWindowClickListener,
		OnMapLoadedListener, InfoWindowAdapter, OnMapClickListener,
		OnMapLongClickListener, OnMapTouchListener, OnRouteSearchListener,
		OnPoiSearchListener, OnCameraChangeListener {
	// UI控件
	private TextView sports, map, history;
	private TabHost tabHost;
	private ImageButton map_in, map_out, mylocate;
	private TextView stepcount;
	private Button jiaozheng, suoding;
	private TextView sportstate;
	private Button startstep;
	private static MyApplication app;

	// 定位模块
	private AMap aMap;
	private MapView mapView;
	private OnLocationChangedListener mListener;
	private AMapLocationClient mlocationClient;
	private AMapLocationClientOption mLocationOption;
	private BitmapDescriptor levellogo;
	private boolean isTimerStart = false;
	Marker MyMarker = null;

	// 绘制轨迹
	private PolylineOptions resultpoint = new PolylineOptions();
	private PolylineOptions walkpoint = new PolylineOptions();
	private PolylineOptions gpspoint = new PolylineOptions();
	private LatLng Mylocation = null;

	// 传感器
	private SensorManager Orientation;
	private Sensor orientation;
	private SensorManager Acceleration;
	private Sensor acceleration;
	private SensorManager Magnetic;
	private Sensor magnetic;
	private SensorManager LinearAcceleration;
	private Sensor linearAcceleration;
	private SensorManager Distance;
	private Sensor distance;
	private SensorManager Light;
	private Sensor light;
	private SensorManager Gyroscope;
	private Sensor gyroscope;
	private double GRAVITY = SensorManager.STANDARD_GRAVITY;
	private boolean isSensorStart = false;

	// 传感器数据存储
	private List<Float> ACCX = new ArrayList<Float>();
	private List<Float> ACCY = new ArrayList<Float>();
	private List<Float> ACCZ = new ArrayList<Float>();

	// 计步
	private long stepCount = 0;
	private Timer timer = null;
	private TimerTask task = null;
	private Handler handler = null;
	boolean isStartstep = false, isTrail = false;

	// 运动状态
	private static int WALK = 0;
	double WALK_line1 = 0, WALK_line2 = (double) 0.5;
	private static int RUN = 1;
	double RUN_line1 = (double) 0.5, RUN_line2 = 1;
	private static int CAR = 2;
	double CAR_line1 = 1, CAR_line2 = (double) 1.5;
	private static int BUS = 3;
	double BUS_line1 = (double) 1.5, BUS_line2 = 2;
	private static int RELAWAY = 4;
	double RELAWAY_line1 = 2, RELAWAY_line2 = (double) 2.5;
	private int state = -1;

	// 取出校正检测的值
	private static double corMeanX = 0, corVarX = app.VACCX, x0 = 0,
			Rx = app.VACCX;
	private double corMeanY = 0, corVarY = app.VACCY, y0 = 0, Ry = app.VACCY;
	private double corMeanZ = 0, corVarZ = app.VACCZ, z0 = 0, Rz = app.VACCZ;

	// 广播通信
	private BroadCast0 broadcast;
	private LatLng BroadMyLocation = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_main);
		findView();
		// 启动定位服务
		Intent loca = new Intent(MainActivity.this, LocationService.class);
		startService(loca);
		// 初始化
		helper = new FileHelper(getApplicationContext()); // 初始化文件读写
		app = (MyApplication) getApplication();// 初始化全局类
		mapView.onCreate(savedInstanceState);// 初始化地图
		// 初始化传感器
		Orientation = (SensorManager) getSystemService(SENSOR_SERVICE);// 姿态
		orientation = Orientation.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		Acceleration = (SensorManager) getSystemService(SENSOR_SERVICE);// 加速度
		acceleration = Acceleration.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		Magnetic = (SensorManager) getSystemService(SENSOR_SERVICE);// 磁场
		magnetic = Magnetic.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		LinearAcceleration = (SensorManager) getSystemService(SENSOR_SERVICE);// 线性加速度
		linearAcceleration = LinearAcceleration
				.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
		Distance = (SensorManager) getSystemService(SENSOR_SERVICE);// 距离
		distance = Distance.getDefaultSensor(Sensor.TYPE_PROXIMITY);
		Light = (SensorManager) getSystemService(SENSOR_SERVICE);// 光线
		light = Light.getDefaultSensor(Sensor.TYPE_LIGHT);
		Gyroscope = (SensorManager) getSystemService(SENSOR_SERVICE);// 陀螺仪
		gyroscope = Gyroscope.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		startSensor();// 开启传感器
		initMap();// 创建地图
		broadcast = new BroadCast0(); // 初始化广播，定义Action
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("step");
		intentFilter.addAction("location");
		registerReceiver(broadcast, intentFilter);

		// 初始化界面
		tabHost = (TabHost) findViewById(R.id.tabhost);
		tabHost.setup();
		tabHost.addTab(tabHost.newTabSpec("tab1").setIndicator("运动")
				.setContent(R.id.relamap));
		tabHost.addTab(tabHost.newTabSpec("tab2").setIndicator("历史")
				.setContent(R.id.view3));
		tabHost.addTab(tabHost.newTabSpec("tab3").setIndicator("我的")
				.setContent(R.id.view1));
		tabHost.setCurrentTab(0);
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				switch (msg.what) {
				case 0:
					stepcount.setText(stepCount + "步");
					value.setText(stepCount + "步");
					break;
				case 1:
					if (isTrail) {
						drawingPDRtrack();
					}
					if (Mylocation != null) {
						if (MyMarker != null) {
							MyMarker.remove();
						}
						MyMarker = aMap.addMarker(new MarkerOptions()
								.anchor(0.5f, 0.5f).position(Mylocation)
								.icon(levellogo).draggable(false));
					}
					break;
				case 2:
					break;
				default:
					break;
				}
			}
		};
		sports.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sports.setTextColor(Color.BLACK);
				sports.setBackgroundColor(Color.rgb(153, 221, 85));
				map.setTextColor(Color.BLACK);
				map.setBackgroundColor(Color.rgb(232, 232, 232));
				history.setTextColor(Color.BLACK);
				history.setBackgroundColor(Color.rgb(232, 232, 232));
				tabHost.setCurrentTab(0);
				mylocate.setVisibility(View.VISIBLE);
				map_out.setVisibility(View.VISIBLE);
				map_in.setVisibility(View.VISIBLE);
				startstep.setVisibility(View.VISIBLE);
			}
		});
		map.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sports.setTextColor(Color.BLACK);
				sports.setBackgroundColor(Color.rgb(232, 232, 232));
				map.setTextColor(Color.BLACK);
				map.setBackgroundColor(Color.rgb(153, 221, 85));
				history.setTextColor(Color.BLACK);
				history.setBackgroundColor(Color.rgb(232, 232, 232));
				tabHost.setCurrentTab(1);
				mylocate.setVisibility(View.GONE);
				map_out.setVisibility(View.GONE);
				map_in.setVisibility(View.GONE);
				startstep.setVisibility(View.GONE);
			}
		});
		history.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sports.setTextColor(Color.BLACK);
				sports.setBackgroundColor(Color.rgb(232, 232, 232));
				map.setTextColor(Color.BLACK);
				map.setBackgroundColor(Color.rgb(232, 232, 232));
				history.setTextColor(Color.BLACK);
				history.setBackgroundColor(Color.rgb(153, 221, 85));
				tabHost.setCurrentTab(2);
				mylocate.setVisibility(View.GONE);
				map_out.setVisibility(View.GONE);
				map_in.setVisibility(View.GONE);
				startstep.setVisibility(View.GONE);
			}
		});
		mylocate.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (Mylocation != null) {
					if (MyMarker != null) {
						MyMarker.remove();
					}
					MyMarker = aMap.addMarker(new MarkerOptions()
							.anchor(0.5f, 0.5f).position(Mylocation)
							.icon(levellogo).draggable(false));
				}
				if (Mylocation != null) {
					changeCamera(CameraUpdateFactory.changeLatLng(Mylocation),
							null);
				}
			}
		});
		mylocate.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					mylocate.setImageResource(R.drawable.location_pressed);
					break;
				case MotionEvent.ACTION_UP:
					mylocate.setImageResource(R.drawable.location_selected);
					break;
				}
				return false;
			}
		});
		map_in.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (aMap.getCameraPosition().zoom == aMap.getMaxZoomLevel()) {
					map_in.setImageResource(R.drawable.map_in1);
				} else if (aMap.getCameraPosition().zoom == aMap
						.getMinZoomLevel()) {
					map_out.setImageResource(R.drawable.map_out1);
				} else {
					map_in.setImageResource(R.drawable.map_in);
					map_out.setImageResource(R.drawable.map_out);
				}
				aMap.animateCamera(CameraUpdateFactory.zoomIn(), 300, null);
			}
		});
		map_out.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (aMap.getCameraPosition().zoom == aMap.getMaxZoomLevel()) {
					map_in.setImageResource(R.drawable.map_in1);
				} else if (aMap.getCameraPosition().zoom == aMap
						.getMinZoomLevel()) {
					map_out.setImageResource(R.drawable.map_out1);
				} else {
					map_in.setImageResource(R.drawable.map_in);
					map_out.setImageResource(R.drawable.map_out);
				}
				aMap.animateCamera(CameraUpdateFactory.zoomOut(), 300, null);
			}
		});
		map_out.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					map_out.setImageResource(R.drawable.map_out1);
					break;
				case MotionEvent.ACTION_UP:
					map_out.setImageResource(R.drawable.map_out);
					break;
				}
				return false;
			}
		});
		map_in.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				switch (event.getAction()) {
				case MotionEvent.ACTION_DOWN:
					map_in.setImageResource(R.drawable.map_in1);
					break;
				case MotionEvent.ACTION_UP:
					map_in.setImageResource(R.drawable.map_in);
					break;
				}
				return false;
			}
		});
		startstep.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent in = new Intent(MainActivity.this,
						StepCounterService.class);
				if (!isStartstep) {
					if (BroadMyLocation != null) {
						statestate = 1;
						isStartstep = true;
						startService(in);
						isTrail = true;
						startstep.setBackgroundResource(R.drawable.pause);
						toast("开始计步");
					}
				} else {
					startstep.setBackgroundResource(R.drawable.start);
					if (BroadMyLocation != null) {
						isStartstep = false;
						isTrail = false;
						stopService(in);
					}
				}
			}
		});
		startstep.setOnTouchListener(new View.OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (isStartstep) {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						startstep
								.setBackgroundResource(R.drawable.pause_pressed);
						break;
					case MotionEvent.ACTION_UP:
						startstep.setBackgroundResource(R.drawable.pause);
						break;
					}
				} else {
					switch (event.getAction()) {
					case MotionEvent.ACTION_DOWN:
						startstep
								.setBackgroundResource(R.drawable.start_pressed);
						break;
					case MotionEvent.ACTION_UP:
						startstep.setBackgroundResource(R.drawable.start);
						break;
					}
				}
				return false;
			}
		});
		jiaozheng.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				Intent jiao = new Intent(MainActivity.this,
						SensorCorrectActivity.class);
				startActivity(jiao);
			}
		});

		suoding.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				SharedPreferences setting = getSharedPreferences("limit", 0);
				setting.edit()
						.putFloat("limit",
								Float.valueOf(limit.getText().toString()))
						.commit();
				toast("计步阈值设置为："+limit.getText().toString());
			}
		});
		jisuan.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				String txt = rrtext.getText().toString();
				if (statestate == 1) {
					helper.writeSDFile(WalkPString, txt + "PDR步行融合误差P.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(traiString, txt + "PDR融合定位结果.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(walkString, txt + "PDR步行定位结果.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(gpsString, txt + "PDRGPS定位结果.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(fangweijiao, txt + "步行方位角.txt");
				}
				if (statestate == 2) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(insMatrixtring, txt + "惯性导航误差P.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(insTraiString, txt + "惯性导航融合定位结果.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(insTraiString, txt + "惯性导航定位结果.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(insGpsString, txt + "惯性导航GPS定位结果.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(insAzimuth, txt + "惯性导航方位角.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(insDS, txt + "惯性导航积分距离S.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(heACCXYZ, txt + "原始合加速度值.txt");
					try {
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(JingzhiV, txt + "静止滤波后速度.txt");
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					helper.writeSDFile(noJingzhiV, txt + "直接积分速度.txt");
				}
				toast("保存成功！");
			}
		});
		stategroup
				.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(RadioGroup group, int checkedId) {
						if (checkedId == R.id.walkratio) {
							sportstate.setText("步行状态");
							statestate = 1;
						}
						if (checkedId == R.id.carratio) {
							sportstate.setText("非步行状态");
							statestate = 2;
						}
					}
				});
		showronghe
				.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							for (Polyline poly : Rongheroute) {
								poly.setVisible(true);
							}
						} else {
							for (Polyline poly : Rongheroute) {
								poly.setVisible(false);
							}
						}
					}
				});
		showgps.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					for (Polyline poly : GPSroute) {
						poly.setVisible(true);
					}
				} else {
					for (Polyline poly : GPSroute) {
						poly.setVisible(false);
					}
				}
			}
		});
		showpdr.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView,
					boolean isChecked) {
				if (isChecked) {
					for (Polyline poly : Singleroute) {
						poly.setVisible(true);
					}
				} else {
					for (Polyline poly : Singleroute) {
						poly.setVisible(false);
					}
				}
			}
		});
	}

	long RR = 6378137;
	String XValue = "";
	FileHelper helper;
	TextView writetime, value, gpsstate;
	EditText txtname, rrtext;
	Button jisuan;
	EditText limit;
	RadioGroup stategroup;
	Switch showronghe, showgps, showpdr;
	TextView alldistance, kaluli, speedtext;

	/**
	 * 获取控件id和常量初始化
	 */
	private void findView() {
		stategroup = (RadioGroup) findViewById(id.radioGroup1);
		showronghe = (Switch) findViewById(id.showronghe);
		showgps = (Switch) findViewById(id.showgps);
		showpdr = (Switch) findViewById(id.showpdr);
		gpsstate = (TextView) findViewById(R.id.gpsstate);
		alldistance = (TextView) findViewById(R.id.alldistance);
		kaluli = (TextView) findViewById(R.id.kaluli);
		speedtext = (TextView) findViewById(R.id.speedtext);
		rrtext = (EditText) findViewById(id.rrtext);
		limit = (EditText) findViewById(id.editText1);
		value = (TextView) findViewById(R.id.value);
		jisuan = (Button) findViewById(id.jisuanjisuan);
		mapView = (MapView) findViewById(R.id.map);
		sports = (TextView) findViewById(R.id.sports);
		map = (TextView) findViewById(R.id.mapmap);
		history = (TextView) findViewById(R.id.history);
		stepcount = (TextView) findViewById(id.stepcount);
		startstep = (Button) findViewById(id.startstep);
		map_in = (ImageButton) findViewById(id.map_in);
		map_out = (ImageButton) findViewById(id.map_out);
		mylocate = (ImageButton) findViewById(id.mylocate);
		sportstate = (TextView) findViewById(R.id.sportstate);
		levellogo = BitmapDescriptorFactory.fromResource(R.drawable.maker1);
		jiaozheng = (Button) findViewById(id.jiaozheng);
		suoding = (Button) findViewById(id.suoding);
		SharedPreferences setting = getSharedPreferences("jiaozheng", 0);
		corMeanX = setting.getFloat("AccX", app.AccX);
		corMeanY = setting.getFloat("AccY", app.AccY);
		corMeanZ = setting.getFloat("AccZ", app.AccZ);
		Rx = setting.getFloat("VACCX", app.VACCX);
		Ry = setting.getFloat("VACCY", app.VACCY);
		Rz = setting.getFloat("VACCZ", app.VACCZ);
		corVarX = Rx;
		corVarY = Ry;
		corVarZ = Rz;
		x0 = 0;
		y0 = 0;
		z0 = 0;
	}

	/**
	 * 为传感器设置不同的监听
	 */
	private void startSensor() {
		Acceleration.registerListener(myListener1, acceleration,
				SensorManager.SENSOR_DELAY_NORMAL);
		Orientation.registerListener(myListener, orientation,
				SensorManager.SENSOR_DELAY_NORMAL);
		Magnetic.registerListener(myListener2, magnetic,
				SensorManager.SENSOR_DELAY_NORMAL);
		LinearAcceleration.registerListener(myListener3, linearAcceleration,
				SensorManager.SENSOR_DELAY_NORMAL);
		Distance.registerListener(JuLiListener, distance,
				SensorManager.SENSOR_DELAY_NORMAL);
		Light.registerListener(LightListener, light,
				SensorManager.SENSOR_DELAY_NORMAL);
		Gyroscope.registerListener(TuoLuoYiListener, gyroscope,
				SensorManager.SENSOR_DELAY_NORMAL);
		isSensorStart = true;
	}

	/**
	 * 传感器取消监听
	 */
	@SuppressWarnings("unused")
	private void endSensor() {
		Orientation.unregisterListener(myListener);
		Magnetic.unregisterListener(myListener2);
		LinearAcceleration.unregisterListener(myListener3);
		Distance.unregisterListener(JuLiListener);
		Light.unregisterListener(LightListener);
		Gyroscope.unregisterListener(TuoLuoYiListener);
		isSensorStart = false;
	}

	/**
	 * 距离传感器监听
	 */
	double distancevalue = 0, lightvalue = 0;
	double[] tuoluoyivalue = new double[3];
	private SensorEventListener JuLiListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] v = event.values;
			distancevalue = v[0];
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};
	/**
	 * 光线传感器监听
	 */
	private SensorEventListener LightListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] v = event.values;
			lightvalue = v[0];
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};
	/**
	 * 方向传感器监听
	 */
	private double Roll = 0, Yow = 0, Pitch = 0;
	List<Double> YowList = new ArrayList<Double>();
	List<Double> GuanYowList = new ArrayList<Double>();
	private SensorEventListener myListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] v = event.values;
			// sportstate.setText("姿态"+v[0]);
			Yow = v[0];
			Pitch = v[1];
			Roll = v[2];
			YowList.add(Yow);
			GuanYowList.add(Yow);
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};
	// 代码暂不加入陀螺仪和姿态角的卡尔曼滤波组合，因为陀螺仪数据需要改正成地面坐标系，这个TM哪篇文章都没说
	// 都TM默认为坐标系就是地面
	/**
	 * 陀螺仪传感器监听
	 */
	List<Float> TuoLuoYiList = new ArrayList<Float>();
	private SensorEventListener TuoLuoYiListener = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] v = event.values;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};
	List<Double> yidongpingjunX = new ArrayList<Double>();
	List<Double> yidongpingjunY = new ArrayList<Double>();
	List<Double> yidongpingjunZ = new ArrayList<Double>();
	int K = 5;
	String pingjunlvboFile = "", chuzhivalue = "";
	int isjingzhi = 15;
	List<Double> isjingzhiX = new ArrayList<Double>();
	List<Double> isjingzhiY = new ArrayList<Double>();
	List<Double> isjingzhiZ = new ArrayList<Double>();
	List<Double> WindowV = new ArrayList<Double>();
	double isjingzhiYuZhi = 0.2;
	/**
	 * 线性加速度传感器监听
	 */
	private SensorEventListener myListener3 = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] v = event.values;
			v[0] = (float) (v[0] - corMeanX);
			v[1] = (float) (v[1] - corMeanY);
			v[2] = (float) (v[2] - corMeanZ);
			chuzhivalue = chuzhivalue + v[0] + "\n";
			// 移动平均滤波算法
			yidongpingjunX.add((double) v[0]);
			yidongpingjunY.add((double) v[1]);
			yidongpingjunZ.add((double) v[2]);
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
			double heacc = Math.sqrt(v[0] * v[0] + v[1] * v[1] + v[2] * v[2]);
			WindowV.add(heacc);
			if (WindowV.size() > 30) {
				WindowV.remove(0);
			}
			pingjunlvboFile = pingjunlvboFile + v[0] + "\n";
			isjingzhiX.add(heacc);
			isjingzhiY.add((double) v[1]);
			isjingzhiZ.add((double) v[2]);
			// 静止检测算法
			ACCX.add(v[0]);
			ACCY.add(v[1]);
			ACCZ.add(v[2]);
			if (isjingzhiX.size() > isjingzhi) {
				double k = 0;
				for (int i = 0; i < isjingzhiX.size(); i++) {

				}
				for (int i = isjingzhiY.size() - 1; i > isjingzhiY.size()
						- isjingzhi; i--) {
					if (isjingzhiY.get(i) < isjingzhiYuZhi) {
						k++;
					}
				}
				if ((k / isjingzhi) > 0.7) {// 70%的数据小于阈值，认为是静止状态
					v[0] = 0;// 速度归零
					v[1] = 0;
					v[2] = 0;
					Vx = 0;// 加速度归零
					Vy = 0;
					Vz = 0;
				}
				isjingzhiX.remove(0);
				isjingzhiY.remove(0);
				isjingzhiZ.remove(0);
			}
			lineaccxyz = v;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};
	List<Float> yidongCiX = new ArrayList<Float>();
	List<Float> yidongCiY = new ArrayList<Float>();
	List<Float> yidongCiZ = new ArrayList<Float>();
	/**
	 * 磁场传感器监听
	 */
	private SensorEventListener myListener2 = new SensorEventListener() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] v = event.values;
			yidongCiX.add(v[0]);
			yidongCiY.add(v[1]);
			yidongCiZ.add(v[2]);
			if (yidongCiX.size() > K) {
				double ssX = 0, ssY = 0, ssZ = 0;
				for (int i = 0; i < yidongCiX.size(); i++) {
					ssX = ssX + yidongCiX.get(i);
					ssY = ssY + yidongCiY.get(i);
					ssZ = ssZ + yidongCiZ.get(i);
				}
				v[0] = (float) (ssX / yidongCiX.size());
				v[1] = (float) (ssY / yidongCiY.size());
				v[2] = (float) (ssZ / yidongCiZ.size());
				yidongCiX.remove(0);
				yidongCiY.remove(0);
				yidongCiZ.remove(0);
			} else {
			}
			cichangvalue = v;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};

	float[] accxyz = new float[3], Xvv = new float[3];
	float[] cichangvalue = new float[3], lineaccxyz = new float[3];
	double[] ground = new double[3];
	List<Float> yidongaccX = new ArrayList<Float>();
	List<Float> yidongaccY = new ArrayList<Float>();
	List<Float> yidongaccZ = new ArrayList<Float>();
	/**
	 * 加速度传感器监听
	 */
	private SensorEventListener myListener1 = new SensorEventListener() {

		@Override
		public void onSensorChanged(SensorEvent event) {
			float[] v = event.values;
			yidongaccX.add(v[0]);
			yidongaccY.add(v[1]);
			yidongaccZ.add(v[2]);
			if (yidongaccX.size() > K) {
				double ssX = 0, ssY = 0, ssZ = 0;
				for (int i = 0; i < yidongaccX.size(); i++) {
					ssX = ssX + yidongaccX.get(i);
					ssY = ssY + yidongaccY.get(i);
					ssZ = ssZ + yidongaccZ.get(i);
				}
				v[0] = (float) (ssX / yidongaccX.size());
				v[1] = (float) (ssY / yidongaccY.size());
				v[2] = (float) (ssZ / yidongaccZ.size());
				yidongaccX.remove(0);
				yidongaccY.remove(0);
				yidongaccZ.remove(0);
			} else {
			}
			if (Math.abs(v[0]) < 0.5) {
				v[0] = 0;
			}
			if (Math.abs(v[1]) < 0.5) {
				v[1] = 0;
			}
			if (Math.abs(v[2] - GRAVITY) < 0.5) {
				v[2] = (float) GRAVITY;
			}
			accxyz = v;
		}

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {

		}
	};
	double Vx = 0, Vy = 0, Vz = 0, VVV = 0, dt = 0.0666666666;
	double Sx = 0, Sy = 0, Sz = 0;
	LatLng MyNewLocation = null;
	float[] orientationValues = new float[3];
	String walkLength = "";

	/**
	 * 线性模型计算步长
	 * 
	 * @param F
	 *            步频
	 * @param VS
	 *            加速度方差
	 * @return 步长
	 */
	@SuppressWarnings("unused")
	private double stepLength(double F, double VS) {
		// L=A+B*F+C*SV
		double result = (double) (0.1 + 1 * F / 1000 + 0.8 * VS);
		double result1 = -0.9898 + 0.5913 * 1000 / F + 7.6336 * VS;
		if (result1 > 1.5) {
			result1 = 1.5;
		}
		walkLength = walkLength + nowsteptime + "    " + F + "    " + VS + "\n";
		return 1;// 因实验次数有限，线性模型误差较大，先采用固定步长模型，平均步长估计1m
	}

	double a = 6378137, b = 6356752.314245, f = 1 / 298.257223563;
	double e2 = 1 - (b * b) / (a * a);
	double NN = a;

	/**
	 * 近似大地测量正算
	 * 
	 * @param weidu
	 *            维度
	 * @param jingdu
	 *            经度
	 * @param S
	 *            距离
	 * @param afa
	 *            方位角
	 * @return 第二个点的经纬度
	 */
	private LatLng distanceLatLng(double weidu, double jingdu, double S,
			double afa) {
		double sinB2 = Math.pow(Math.sin(d2R(weidu)), 2);
		double W = Math.sqrt(1 - e2 * sinB2);
		double djingdu = (S * Math.sin(d2R(afa))) * W
				/ (a * Math.cos(d2R(weidu)));
		djingdu = r2D(djingdu);
		double dweidu = S * Math.cos(d2R(afa)) * W * W * W / (a * (1 - e2));
		dweidu = r2D(dweidu);
		LatLng result = new LatLng(weidu + dweidu, jingdu + djingdu);
		return result;
	}

	/**
	 * Vincenty方程，精度较高的正算公式
	 * 
	 * @param weidu
	 *            维度
	 * @param jingdu
	 *            经度
	 * @param S
	 *            距离
	 * @param afa
	 *            方位角
	 * @return 第二个点的经纬度
	 */
	@SuppressWarnings("unused")
	private LatLng Vincenty(double weidu, double jingdu, double S, double afa) {
		LatLng result = null;
		afa = d2R(afa);
		double cosafa1 = Math.cos(afa), sinafa1 = Math.sin(afa);
		double tanu1 = (double) ((1 - f) * Math.tan(afa)), cosu1 = (double) (1 / (Math
				.sqrt(1 + tanu1 * tanu1))), sinu1 = tanu1 * cosu1;
		double theta1 = Math.atan(tanu1 / cosafa1);
		double sina = cosu1 * Math.sin(afa), cos2a = 1 - sina * sina;
		double u2 = cos2a * (a * a - b * b) / b * b;
		double A = 1 + u2 / 16384
				* (4096 + u2 * (-768 + u2 * (320 - 175 * u2)));
		double B = u2 / 1024 * (256 + u2 * (-128 + u2 * (74 - 47 * u2)));
		double theta = S / (b * A), theta_ = 0;
		double cos2m = 0;
		do {
			cos2m = Math.cos(2 * theta1 + theta);
			double sinm = Math.sin(theta);
			double cosm = Math.cos(theta);
			double dtheta = B
					* sinm
					* (cos2m + B
							/ 4
							* (cosm * (-1 + 2 * cos2m * cos2m) - B / 6 * cos2m
									* (-3 + 4 * sinm * sinm)
									* (-3 + 4 * cos2m * cos2m)));
			theta_ = theta;
			theta = S / (b * A) + dtheta;
		} while (Math.abs(theta_ - theta) > 1e-12);
		double costhe = Math.cos(theta), sinthe = Math.sin(theta);
		double temp = sinu1 * sinthe - cosu1 * costhe * cosafa1;
		double atan1 = sinu1 * costhe + cosu1 * sinthe * cosafa1, atan2 = (1 - f)
				* Math.sqrt(sina * sina + temp * temp);
		double weidu2 = weidu + r2D(Math.atan(atan1 / atan2));
		double naim = Math.atan(sinthe * sinafa1
				/ (cosu1 * costhe - sinu1 * sinthe * cosafa1));
		double C = f / 16 * cos2a * (4 + f * (4 - 3 * cos2a));
		double L1 = naim
				- (1 - C)
				* f
				* sinafa1
				* (theta + C * sinthe
						* (cos2m + C * costhe * (-1 + 2 * cos2m * cos2m)));
		double jingdu2 = jingdu + r2D(L1);
		result = new LatLng(weidu2, jingdu2);
		return result;
	}

	/**
	 * 方差计算
	 * 
	 * @param acc
	 *            待求方差序列
	 * @return 方差值
	 */
	private double varianceValue(List<Double> acc) {
		double s = 0;
		double mean = meanValue(acc);
		for (double value : acc) {
			s = s + (value - mean) * (value - mean);
		}
		double result = s / (acc.size());
		result = (double) Math.sqrt(result);
		return result;
	}

	/**
	 * 平均值计算
	 * 
	 * @param acc
	 *            待求平均值序列
	 * @return 序列平均值
	 */
	private double meanValue(List<Double> acc) {
		double s = 0;
		for (double a : acc) {
			s = s + a;
		}
		double result = s / (acc.size());
		return result;
	}

	double trailDistance = 0, walkDistance = 0, gpsDistance = 0;

	/**
	 * 绘制PDR，GPS，以及融合轨迹
	 * 
	 * @return void
	 */
	private void drawingPDRtrack() {
		int length = resultpoint.getPoints().size();
		int length1 = walkpoint.getPoints().size();
		int length2 = gpspoint.getPoints().size();
		if (length >= 2) {
			LatLng lastpoint = resultpoint.getPoints().get(length - 2);
			LatLng nowpoint = resultpoint.getPoints().get(length - 1);
			int color = Color.RED;
			if (state == 0) {
				color = Color.RED;
			}
			Rongheroute.add(aMap.addPolyline(new PolylineOptions()
					.add(lastpoint, nowpoint).color(color).width(6)));

			trailDistance = trailDistance
					+ AMapUtils.calculateLineDistance(lastpoint, nowpoint);
		}
		if (length1 >= 2) {
			LatLng lastpoint = walkpoint.getPoints().get(length1 - 2);
			LatLng nowpoint = walkpoint.getPoints().get(length1 - 1);
			int color = Color.BLUE;
			Singleroute.add(aMap.addPolyline(new PolylineOptions()
					.add(lastpoint, nowpoint).color(color).width(3)));
		}
		if (length2 >= 2) {
			LatLng lastpoint = gpspoint.getPoints().get(length2 - 2);
			LatLng nowpoint = gpspoint.getPoints().get(length2 - 1);
			int color = Color.GREEN;
			GPSroute.add(aMap.addPolyline(new PolylineOptions()
					.add(lastpoint, nowpoint).color(color).width(3)));
		}
		String result = String.format("%.2f", trailDistance);
		allCalorie = allCalorie + Calorie(trailDistance);
		String resulta = String.format("%.2f", allCalorie);
		kaluli.setText("卡路里:" + resulta + "千卡");
		alldistance.setText("总距离:" + result + "m");
	}

	double allCalorie = 0;

	/**
	 * 卡路里计算
	 * 
	 * @param 里程
	 *            ，单位米m
	 * @return 卡路里值
	 */
	private double Calorie(double S) {
		double res = 0;
		res = 0.53 * 170 + 0.58 * 55 + 0.37 * 100 + 0.0314 * S - 145.03;
		return res;
	}

	Circle circle;
	double roomAccuracy = 0;
	LatLng LastMyLocation = null, GPSLocation = null;
	List<LatLng> MyLocationList = new ArrayList<LatLng>();
	Marker NetMarker = null;

	/**
	 * Activity定位监听
	 */
	@Override
	public void onLocationChanged(AMapLocation amapLocation) {
		if (mListener != null && amapLocation != null) {
			if (amapLocation != null && amapLocation.getErrorCode() == 0) {

			}
		}
	}

	/**
	 * 弧度转为度数
	 * 
	 * @param 弧度
	 * @return 度数 °
	 */
	private double r2D(double hu) {
		double result = (hu * 180 / Math.PI);
		return result;
	}

	/**
	 * 度数转为弧度
	 * 
	 * @param 度数
	 *            °
	 * @return 弧度R
	 */
	private double d2R(double du) {
		double hu = (du * Math.PI / 180);
		return hu;
	}

	final double PI = 3.141592654;

	/**
	 * 待定函数：由加速度传感器判断运动方向，并根据步长计算当前步经纬度， 而不是手机Y周轴指向
	 */
	@SuppressWarnings("unused")
	private void sensorLocate() {
		if (circle != null) {
			circle.remove();
		}
		if (ACCX.size() > 0) {
			// 取出一步之内的加速度平均值，判断开始方向
			new Thread(new Runnable() {
				@Override
				public void run() {
					double SX = 0, SY = 0, SZ = 0;
					for (int i = 0; i < ACCX.size(); i++) {
						SX = SX + ACCX.get(i);
						SY = SY + ACCY.get(i);
						SZ = SZ + ACCZ.get(i);
					}
					double LX = SX / ACCX.size(), LY = SY / ACCY.size(), LZ = SZ
							/ ACCZ.size();
					double[][] Accxyz = new double[3][1], AccXYZ = new double[3][1];
					Accxyz[0][0] = LX;
					Accxyz[1][0] = LY;
					Accxyz[2][0] = LZ;
					double Lastangle = Math.atan(Accxyz[0][0] / Accxyz[1][0]);
					double STEPLENGTH = 0.5;
					double Djingdu = (STEPLENGTH * Math.sin(Lastangle))
							/ (RR * Math.cos(Lastangle));
					double Dweidu = (STEPLENGTH * Math.cos(Lastangle)) / (RR);
					double lastjingdu = 0, lastweidu = 0;
					lastjingdu = Mylocation.longitude * 180 / PI;
					lastweidu = Mylocation.latitude * 180 / PI;
					if (Mylocation != null) {
						Mylocation = new LatLng(lastweidu + Dweidu, lastjingdu
								+ Djingdu);
						resultpoint.add(Mylocation);
					}
					sendMessage(1);
				}
			}).start();
		}
	}

	/**
	 * 近似大地测量方法计算纬度差
	 * 
	 * @param weidu
	 *            纬度
	 * @param S
	 *            距离，单位m
	 * @return 纬度之差
	 */
	@SuppressWarnings("unused")
	private double dLat(double weidu, double S) {
		double sinB2 = Math.pow(Math.sin(d2R(weidu)), 2);
		NN = a / (Math.sqrt(1 - e2 * sinB2));
		double dweidu = S / NN;
		dweidu = r2D(dweidu);
		return dweidu;
	}

	/**
	 * 近似大地测量方法计算经度差
	 * 
	 * @param weidu
	 *            纬度
	 * @param S
	 *            距离，单位m
	 * @return 经度之差
	 */
	@SuppressWarnings("unused")
	private double dLon(double weidu, double S) {
		double djingdu = S / (RR * Math.cos(d2R((double) weidu)));
		djingdu = r2D(djingdu);
		return djingdu;
	}

	/**
	 * 根据特征空间模型，判断当前用户运动状态，步行状态和非步行状态
	 */
	@SuppressWarnings("unused")
	private int checkState() {
		int result = 1;
		double VV = varianceValue(WindowV);
		if (VV > 0 && VV < 4) {
			result = 1;// 步行状态
		}
		if (VV > 4 && VV < 5) {
			result = 2;// 乘车状态
		}
		return result;
	}

	// PDR和GPS卡尔曼滤波融合各种变量
	double[][] XX = new double[4][1];
	private double BiaoZhunCha = 0;
	double allacc = 0;
	List<Double> AllAcc = new ArrayList<Double>();
	double Xk = 0, Yk = 0, Lk = 0;
	double[][] ZZ = new double[4][1], Kk = new double[4][4],
			XX_ = new double[4][1], ZZ_ = new double[4][1];
	double Qx = 0.1 * 4.5101e-005, Qy = 0.1 * 5.2099e-005, QL = (double) 0.5,
			Qp = (double) d2R(0.5);
	double[][] Qw = new double[][] { { Qx, 0, 0, 0 }, { 0, Qy, 0, 0 },
			{ 0, 0, QL, 0 }, { 0, 0, 0, Qp } };
	double[][] Rw = new double[][] { { Qx, 0, 0, 0 }, { 0, Qy, 0, 0 },
			{ 0, 0, QL, 0 }, { 0, 0, 0, Qp } };
	double[][] P = new double[][] { { Qx, 0, 0, 0 }, { 0, Qy, 0, 0 },
			{ 0, 0, QL, 0 }, { 0, 0, 0, Qp } };
	LatLng WalkLocation = null;
	String traiString = "", walkString = "", gpsString = "";
	String PDRGPSdistance = "", TraiGPSdistance = "";
	int intint = 0;
	double YowYow = 0;
	String WalkPString = "";
	double lastYowYow = -1;
	String fangweijiao = "";

	/**
	 * PDR和AGPS卡尔曼滤波融合
	 */
	private void PDR_AGPS() {
		double SYow = 0;
		for (double yow : YowList) {
			SYow = SYow + yow;
		}
		YowYow = SYow / YowList.size();
		double dYow = 1;
		if (lastYowYow > 0 && YowYow != 0) {
			dYow = (YowYow - lastYowYow);
		}
		if (dYow < 3) {
			dYow = 1;
		}
		lastYowYow = YowYow;
		YowList.clear();
		AllAcc.clear();
		ACCZ.clear();
		ACCX.clear();
		ACCY.clear();
		Lk = 0.9;
		double steptimetime = steptime / 1000;
		if (steptimetime > 0.2) {
			String result1 = String.format("%.2f", Lk / steptimetime);
			speedtext.setText(result1 + "m/s");
		}
		if (WalkLocation != null) {
			WalkLocation = distanceLatLng(WalkLocation.latitude,
					WalkLocation.longitude, Lk, YowYow);
		} else {
			WalkLocation = StartWalkingLocation;
		}
		// 卡尔曼滤波融合过程
		ZZ[0][0] = BroadMyLocation.latitude;
		ZZ[1][0] = BroadMyLocation.longitude;
		ZZ[2][0] = Lk;
		ZZ[3][0] = d2R(YowYow);
		double sinZZ = Math.sin(XX[3][0]), cosZZ = Math.cos(XX[3][0]);
		double dLat = distanceLatLng(BroadMyLocation.latitude,
				BroadMyLocation.longitude, XX[2][0] * cosZZ, 0).latitude
				- BroadMyLocation.latitude;
		double dLon = distanceLatLng(BroadMyLocation.latitude,
				BroadMyLocation.longitude, XX[2][0] * sinZZ, 90).longitude
				- BroadMyLocation.longitude;
		double sinfai = Math.sin(d2R(XX[0][0]));
		double cosfai = Math.cos(d2R(XX[0][0]));
		double Wk = Math.sqrt(1 - e2 * sinfai * sinfai);
		double Wk3 = Wk * Wk * Wk, ae2 = 1 / (a * (1 - e2)), rou = 180 / Math.PI;
		double m1 = 1 - rou * 3 * e2 * XX[2][0] * cosZZ * Wk * sinfai * cosfai
				/ (a * (1 - e2));
		double m2 = cosZZ * Wk3 * ae2;
		double m3 = -XX[2][0] * sinZZ * Wk3 * ae2;
		double n1 = (1 - e2) * XX[2][0] * sinZZ * sinfai
				/ (a * cosfai * cosfai * Wk);
		double n2 = sinZZ * Wk / (a * cosfai);
		double n3 = XX[2][0] * cosZZ * Wk / (a * cosfai);
		m2 = r2D(m2);
		m3 = r2D(m3);
		n1 = r2D(n1);
		n2 = r2D(n2);
		n3 = r2D(n3);
		double[][] phik = new double[][] { { m1, 0, m2, m3 },
				{ n1, 1, n2, n3 }, { 0, 0, 1, 0 }, { 0, 0, 0, 1 } };
		// 判断室内还是室外，是否有GPS信号
		if (isGPSOK == true) {// 采用相信GPS定位参数,不相信PDR定位
			Qx = 0.008 * 4.5101e-005;
			Qy = 0.008 * 5.2099e-005;
			QL = 0.5;
			Qp = d2R(0.5);
			Rw[0][0] = distanceLatLng(BroadMyLocation.latitude,
					BroadMyLocation.longitude, accuracy, 0).latitude
					- BroadMyLocation.latitude;
			Rw[1][1] = distanceLatLng(BroadMyLocation.latitude,
					BroadMyLocation.longitude, accuracy, 90).longitude
					- BroadMyLocation.longitude;
			Rw[0][0] = Rw[0][0] * Rw[0][0];
			Rw[1][1] = Rw[1][1] * Rw[1][1];
			Rw[2][2] = (double) 0.01;
			Rw[3][3] = d2R(0.01);
			Qw = new double[][] { { Qx * Qx * dYow, 0, 0, 0 },
					{ 0, Qy * Qy * dYow, 0, 0 }, { 0, 0, QL * QL * dYow, 0 },
					{ 0, 0, 0, Qp * Qp * dYow } };

		} else {// 采用相信PDR定位参数，不相信GPS定位
			Qx = 0.003 * 4.5101e-05;
			Qy = 0.003 * 5.2099e-005;
			QL = 0.01;
			Qp = 0.01;
			Rw[0][0] = 10
					* distanceLatLng(BroadMyLocation.latitude,
							BroadMyLocation.longitude, accuracy, 0).latitude
					- BroadMyLocation.latitude;
			Rw[1][1] = 10
					* distanceLatLng(BroadMyLocation.latitude,
							BroadMyLocation.longitude, accuracy, 90).longitude
					- BroadMyLocation.longitude;
			Rw[0][0] = Rw[0][0] * Rw[0][0];
			Rw[1][1] = Rw[1][1] * Rw[1][1];
			Rw[2][2] = (double) 0.01;
			Rw[3][3] = d2R(0.01);
			Qw = new double[][] { { Qx * Qx * dYow, 0, 0, 0 },
					{ 0, Qy * Qy * dYow, 0, 0 }, { 0, 0, QL * QL * dYow, 0 },
					{ 0, 0, 0, Qp * Qp * dYow } };

		}
		double[][] P_ = Matrix.plus(
				Matrix.multi(Matrix.multi(phik, P), Matrix.zhz(phik)), Qw);
		Kk = Matrix.multi(P_, Matrix.inv(Matrix.plus(P_, Rw)));
		double[][] kk = new double[4][4];
		kk = Kk;
		kk[0][0] = Math.sqrt(Kk[0][0]);
		kk[1][1] = Math.sqrt(Kk[1][1]);
		kk[2][2] = Math.sqrt(Kk[2][2]);
		kk[3][3] = Math.sqrt(Kk[3][3]);
		P = Matrix.multi(Matrix.minus(Matrix.I(4), kk), P_);
		WalkPString = WalkPString + P[0][0] + "    " + P[1][1] + "    "
				+ P[2][2] + "    " + P[3][3] + "    " + kk[0][0] + "    "
				+ kk[1][1] + "    " + kk[2][2] + "    " + kk[3][3] + "\n";
		XX_ = new double[4][1];
		XX_[0][0] = XX[0][0] + dLat;
		XX_[1][0] = XX[1][0] + dLon;
		XX_[2][0] = XX[2][0];
		XX_[3][0] = XX[3][0];
		XX = Matrix.plus(XX_, Matrix.multi(kk, Matrix.minus(ZZ, XX_)));
		Mylocation = new LatLng(XX[0][0], XX[1][0]);
		resultpoint.add(Mylocation);
		walkpoint.add(WalkLocation);
		gpspoint.add(BroadMyLocation);
		traiString = traiString + Mylocation.latitude + "    "
				+ Mylocation.longitude + "\n";
		walkString = walkString + WalkLocation.latitude + "    "
				+ WalkLocation.longitude + "\n";
		gpsString = gpsString + BroadMyLocation.latitude + "    "
				+ BroadMyLocation.longitude + "\n";
		fangweijiao = fangweijiao + YowYow + "\n";

		double gpspdr = AMapUtils.calculateLineDistance(BroadMyLocation,
				WalkLocation);
		PDRGPSdistance = PDRGPSdistance + gpspdr + "\n";
		drawingPDRtrack();
		if (Mylocation != null) {
			if (MyMarker != null) {
				MyMarker.remove();
			}
			MyMarker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f)
					.position(Mylocation).icon(levellogo).draggable(false));
		}
	}

	long steptime = 0, nowsteptime = 0, laststeptime = System
			.currentTimeMillis();
	long lastlocationtime = System.currentTimeMillis(),
			nowlocationtime = System.currentTimeMillis();
	double broadweidu = 0, broadjingdu = 0, accuracy = 0;
	boolean isGPSOK = false, isWalking = false, isFirstWalking = true;
	boolean isStartWalking = false;
	LatLng StartWalkingLocation = null;
	boolean isFirstGuanDao = true;
	boolean isfirstlocation = true;
	LatLng MyMyLocation = null, LastBoadLocation = null;
	int statestate = 0;
	boolean isstable = false;
	boolean lastisGPSOK = false;

	/**
	 * 广播接收定位服务LocationService的定位结果，并做出集成定位算法的判断
	 */
	public class BroadCast0 extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Bundle extras = intent.getExtras();
			if (extras != null) {
				if (intent.getAction().equals("step")) {
					if (extras.containsKey("stepcount")) {
						stepCount = extras.getInt("stepcount");
						if (stepCount >= 0) {
							nowsteptime = System.currentTimeMillis();
							steptime = System.currentTimeMillis()
									- laststeptime;
							laststeptime = System.currentTimeMillis();
							sendMessage(0);
							if (isStartWalking == true) {// 是否开始PDR航迹推算
								// 判断当前是否真正走了一步
								List<Double> hejiasudu = new ArrayList<Double>();
								hejiasudu.clear();
								for (int i = 0; i < yidongpingjunX.size(); i++) {
									double jiasudu = Math.sqrt(yidongpingjunX
											.get(i)
											* yidongpingjunX.get(i)
											+ yidongpingjunY.get(i)
											* yidongpingjunY.get(i)
											+ yidongpingjunZ.get(i)
											* yidongpingjunZ.get(i));
									hejiasudu.add(jiasudu);
								}
								double jiasuduVV = varianceValue(hejiasudu);
								if (jiasuduVV > 0.15) {
									PDR_AGPS();
								} else {
									Intent in = new Intent(MainActivity.this,
											StepCounterService.class);
									Bundle bundle = new Bundle();
									bundle.putInt("action", 1);
									intent.putExtras(bundle);
									startService(in);
								}
							}
						}
					}
				}
			}
			// 服务获取得到位置，实时获取
			if (intent.getAction().equals("location")) {
				if (extras.containsKey("jingdu")) {
					broadweidu = extras.getDouble("weidu");
					broadjingdu = extras.getDouble("jingdu");
					accuracy = extras.getDouble("accuracy");
					speedtext.setText("定位精度："+accuracy + "m");
					isGPSOK = extras.getBoolean("isGPSOK");
					MyMyLocation = new LatLng(broadweidu, broadjingdu);
					if (isfirstlocation == true) {
						LastMyLocation = new LatLng(broadweidu, broadjingdu);
						isfirstlocation = false;
						return;
					}
					if (isGPSOK == true) {
						if (LastMyLocation != null) {// GPS定位
							double distance = AMapUtils.calculateLineDistance(
									MyMyLocation, LastMyLocation);
							if (distance < 5) {// GPS信号稳定，精度满足要求才会画路线
								BroadMyLocation = new LatLng(broadweidu,
										broadjingdu);
							} else {
							}
							LastMyLocation = MyMyLocation;
						}
						gpsstate.setText("GPS定位");
					} else {
						gpsstate.setText("网络定位");
						if (LastMyLocation != null) {// 网络定位
							double distance = AMapUtils.calculateLineDistance(
									MyMyLocation, LastMyLocation);
							if (distance < 10) {// GPS信号稳定，才会画路线
								BroadMyLocation = new LatLng(broadweidu,
										broadjingdu);
							} else {
							}
							LastMyLocation = MyMyLocation;
						}
					}
					if (lastisGPSOK == true && isGPSOK == false) {// 认为从室外运动到室内,重新开始PDR，更相信PDR

					}
					if (lastisGPSOK == false && isGPSOK == true) {// 认为从室内走到了室外，重新开始PDR

					}
					lastisGPSOK = isGPSOK;
					if (BroadMyLocation != null) {
						// 判断是否进入计步模式
						if (statestate == 1) {// 用户为步行跑步状态，行人航迹推算
							sportstate.setText("步行状态");
							if (isFirstWalking == true) {
								StartWalkingLocation = BroadMyLocation;
								if (Mylocation != null) {
									StartWalkingLocation = Mylocation;
								}
								isFirstWalking = false;
								// 初始化数据
								AllAcc.clear();
								ACCZ.clear();
								ACCX.clear();
								ACCY.clear();
								YowList.clear();
								XX[0][0] = StartWalkingLocation.latitude;
								XX[1][0] = StartWalkingLocation.longitude;
								XX[2][0] = 1.0;
								XX[3][0] = d2R(Yow);
								Rw[0][0] = distanceLatLng(
										BroadMyLocation.latitude,
										BroadMyLocation.longitude, accuracy, 0).latitude
										- BroadMyLocation.latitude;
								Rw[1][1] = distanceLatLng(
										BroadMyLocation.latitude,
										BroadMyLocation.longitude, accuracy, 90).longitude
										- BroadMyLocation.longitude;
								Rw[0][0] = Rw[0][0] * Rw[0][0];
								Rw[1][1] = Rw[1][1] * Rw[1][1];
								Rw[2][2] = (double) 0.01;
								Rw[3][3] = d2R(0.01);
								P = new double[][] { { Rw[0][0], 0, 0, 0 },
										{ 0, Rw[1][1], 0, 0 },
										{ 0, 0, Rw[2][2], 0 },
										{ 0, 0, 0, Rw[3][3] } };
							}
							isStartWalking = true;
						} else if (statestate == 2) {// 用户为非步行状态，比如乘车，惯性导航融合
							sportstate.setText("乘车状态");
							isFirstWalking = true;
							isStartWalking = false;
							WalkLocation = null;
							if (isFirstGuanDao == true) {
								startInsLocation = BroadMyLocation;
								if (Mylocation != null) {
									StartWalkingLocation = Mylocation;
								}
								isFirstGuanDao = false;
								// 初始化数据
								Vx = 0;
								Vy = 0;
								Vz = 0;
								AllAcc.clear();
								ACCY.clear();
								ACCZ.clear();
								ACCX.clear();
								GuanYowList.clear();
								insXX[0][0] = startInsLocation.latitude;
								insXX[1][0] = startInsLocation.longitude;
								insXX[2][0] = 0.0;
								insXX[3][0] = d2R(Yow);
								GuanYowList.add(Yow);
								GuanRw[0][0] = distanceLatLng(
										BroadMyLocation.latitude,
										BroadMyLocation.longitude, accuracy, 0).latitude
										- BroadMyLocation.latitude;
								GuanRw[1][1] = distanceLatLng(
										BroadMyLocation.latitude,
										BroadMyLocation.longitude, accuracy, 90).longitude
										- BroadMyLocation.longitude;
								GuanRw[0][0] = GuanRw[0][0] * GuanRw[0][0];
								GuanRw[1][1] = GuanRw[1][1] * GuanRw[1][1];
								GuanRw[2][2] = 0.01;
								GuanRw[3][3] = d2R(0.01);
								GuanP = new double[][] {
										{ GuanRw[0][0], 0, 0, 0 },
										{ 0, GuanRw[1][1], 0, 0 },
										{ 0, 0, GuanRw[2][2], 0 },
										{ 0, 0, 0, GuanRw[3][3] } };
								insTraipPoint.getPoints().clear();
								insPoint.getPoints().clear();
								insGpsPoint.getPoints().clear();
							} else {

							}
							INS_GPS();
						}
					}
				}
			}
		}
	}

	// INS和AGPS卡尔曼滤波融合各种变量
	double insYow = 0;
	double GuanS = 0;
	LatLng insLocation = null, startInsLocation = null;
	double[][] insXX = new double[4][1], insZZ = new double[4][1];
	double[][] insKk = new double[4][4];
	String insTraiString = "", insString = "", insGpsString = "";
	String insGpsDistance = "", insTraiGpsDistance = "";
	PolylineOptions insTraipPoint = new PolylineOptions();
	PolylineOptions insPoint = new PolylineOptions();
	PolylineOptions insGpsPoint = new PolylineOptions();
	double[][] GuanP = new double[][] { { Qx * Qx, 0, 0, 0 },
			{ 0, Qy * Qy, 0, 0 }, { 0, 0, (double) 1, 0 },
			{ 0, 0, 0, d2R(0.5) } };
	double[][] GuanRw = new double[][] { { Qx * Qx, 0, 0, 0 },
			{ 0, Qy * Qy, 0, 0 }, { 0, 0, (double) 1, 0 },
			{ 0, 0, 0, d2R(0.5) } };
	String insMatrixtring = "";
	String insAzimuth = "";
	double insLastYow = -1;
	String insDS = "";
	String heACCXYZ = "", noJingzhiV = "", JingzhiV = "";

	/**
	 * INS和AGPS卡尔曼滤波融合
	 */
	private void INS_GPS() {
		dt = 0.18;// 传感器采样时间
		Sx = 0;
		Sy = 0;
		Sz = 0;
		for (int i = 0; i < ACCY.size(); i++) {
			Vy = Vy - ACCY.get(i) * dt;
			Vy = Math.abs(Vy);
			VVV = VVV - ACCY.get(i) * dt;
			VVV = Math.abs(VVV);
			Sy = Sy + Vy * dt;
			double he = Math.sqrt(ACCX.get(i) * ACCX.get(i) + ACCY.get(i)
					* ACCY.get(i) + ACCZ.get(i) * ACCZ.get(i));
			heACCXYZ = heACCXYZ + he + "\n";
		}
		JingzhiV = JingzhiV + Vy + "\n";
		noJingzhiV = noJingzhiV + VVV + "\n";
		// 平均方位角
		String result = String.format("%.2f", Vy);
		speedtext.setText(result + "m/s");
		double SYow = 0;
		for (double yow : GuanYowList) {
			SYow = SYow + yow;
		}
		insYow = SYow / GuanYowList.size();
		double dYow = 1;
		if (insLastYow > 0 && insYow != 0) {
			dYow = (insYow - insLastYow);
		}
		if (dYow < 3) {
			dYow = 1;
		}
		insLastYow = insYow;
		GuanS = Sy;
		if (GuanS > 2) {
			GuanS = 2;
		}
		insDS = insDS + GuanS + "\n";
		GuanYowList.clear();
		ACCX.clear();
		ACCY.clear();
		ACCZ.clear();
		// 认为手机沿Y轴运动，SY即为运动距离
		if (insLocation != null) {
			insLocation = distanceLatLng(insLocation.latitude,
					insLocation.longitude, GuanS, insYow);
		} else {
			insLocation = startInsLocation;
		}
		// INS和ＧＰＳ卡尔曼滤波组合过程
		insZZ[0][0] = BroadMyLocation.latitude;
		insZZ[1][0] = BroadMyLocation.longitude;
		insZZ[2][0] = GuanS;
		insZZ[3][0] = d2R(insYow);
		double sinZZ = Math.sin(insXX[3][0]), cosZZ = Math.cos(insXX[3][0]);
		double dLat = distanceLatLng(BroadMyLocation.latitude,
				BroadMyLocation.longitude, insXX[2][0] * cosZZ, 0).latitude
				- BroadMyLocation.latitude;
		double dLon = distanceLatLng(BroadMyLocation.latitude,
				BroadMyLocation.longitude, insXX[2][0] * sinZZ, 90).longitude
				- BroadMyLocation.longitude;
		double sinfai = Math.sin(d2R(insXX[0][0]));
		double cosfai = Math.cos(d2R(insXX[0][0]));
		double Wk = Math.sqrt(1 - e2 * sinfai * sinfai);
		//
		//
		double Wk3 = Wk * Wk * Wk, ae2 = 1 / (a * (1 - e2)), rou = 180 / Math.PI;
		double m1 = 1 - rou * 3 * e2 * insXX[2][0] * cosZZ * Wk * sinfai
				* cosfai / (a * (1 - e2));
		double m2 = cosZZ * Wk3 * ae2;
		double m3 = -insXX[2][0] * sinZZ * Wk3 * ae2;
		double n1 = (1 - e2) * insXX[2][0] * sinZZ * sinfai
				/ (a * cosfai * cosfai * Wk);
		double n2 = sinZZ * Wk / (a * cosfai);
		double n3 = insXX[2][0] * cosZZ * Wk / (a * cosfai);
		m2 = r2D(m2);
		m3 = r2D(m3);
		n1 = r2D(n1);
		n2 = r2D(n2);
		n3 = r2D(n3);
		double[][] phik = new double[][] { { m1, 0, m2, m3 },
				{ n1, 1, n2, n3 }, { 0, 0, 1, 0 }, { 0, 0, 0, 1 } };
		// 判断是否有ＧＰＳ信号
		if (isGPSOK == true) {// 采用相信GPS定位参数
			Qx = 0.01 * 4.5101e-005;
			Qy = 0.01 * 5.2099e-005;
			QL = 0.5;
			Qp = d2R(0.5);
			GuanRw[0][0] = distanceLatLng(BroadMyLocation.latitude,
					BroadMyLocation.longitude, accuracy / 2, 0).latitude
					- BroadMyLocation.latitude;
			GuanRw[1][1] = distanceLatLng(BroadMyLocation.latitude,
					BroadMyLocation.longitude, accuracy / 2, 90).longitude
					- BroadMyLocation.longitude;
			GuanRw[0][0] = GuanRw[0][0] * GuanRw[0][0];
			GuanRw[1][1] = GuanRw[1][1] * GuanRw[1][1];
			GuanRw[2][2] = (double) 0.01;
			GuanRw[3][3] = d2R(0.01);
			Qw = new double[][] { { Qx * Qx * dYow, 0, 0, 0 },
					{ 0, Qy * Qy * dYow, 0, 0 }, { 0, 0, QL * QL * dYow, 0 },
					{ 0, 0, 0, Qp * Qp * dYow } };
		} else {// 采用相信PDR定位参数
			Qx = 0.003 * 4.5101e-05;
			Qy = 0.003 * 5.2099e-005;
			QL = 0.001;
			Qp = 0.001;
			GuanRw[0][0] = 10
					* distanceLatLng(BroadMyLocation.latitude,
							BroadMyLocation.longitude, accuracy, 0).latitude
					- BroadMyLocation.latitude;
			GuanRw[1][1] = 10
					* distanceLatLng(BroadMyLocation.latitude,
							BroadMyLocation.longitude, accuracy, 90).longitude
					- BroadMyLocation.longitude;
			GuanRw[0][0] = GuanRw[0][0] * GuanRw[0][0];
			GuanRw[1][1] = GuanRw[1][1] * GuanRw[1][1];
			GuanRw[2][2] = (double) 0.01;
			GuanRw[3][3] = d2R(0.01);
			Qw = new double[][] { { Qx * Qx * dYow, 0, 0, 0 },
					{ 0, Qy * Qy * dYow, 0, 0 }, { 0, 0, QL * QL * dYow, 0 },
					{ 0, 0, 0, Qp * Qp * dYow } };
		}
		double[][] P_ = Matrix.plus(
				Matrix.multi(Matrix.multi(phik, GuanP), Matrix.zhz(phik)), Qw);
		//
		// 　　
		// 　
		insKk = Matrix.multi(P_, Matrix.inv(Matrix.plus(P_, GuanRw)));
		GuanP = Matrix.multi(Matrix.minus(Matrix.I(4), insKk), P_);
		insMatrixtring = insMatrixtring + GuanP[0][0] + "    " + GuanP[1][1]
				+ "    " + GuanP[2][2] + "    " + GuanP[3][3] + "\n";
		double[][] insXX_ = new double[4][1];
		insXX_[0][0] = insXX[0][0] + dLat;
		insXX_[1][0] = insXX[1][0] + dLon;
		insXX_[2][0] = insXX[2][0];
		insXX_[3][0] = insXX[3][0];
		insXX = Matrix.plus(insXX_,
				Matrix.multi(insKk, Matrix.minus(insZZ, insXX_)));
		Mylocation = new LatLng(insXX[0][0], insXX[1][0]);

		// 之前假设的线性组合，为了和卡尔曼滤波融合作对比
		// double kk=0.7;
		// double dlat=kk*BroadMyLocation.latitude+(1-kk)*GuanLocation.latitude;
		// double
		// dlon=kk*BroadMyLocation.longitude+(1-kk)*GuanLocation.longitude;
		// Mylocation=new LatLng(dlat,dlon);

		insTraipPoint.add(Mylocation);
		insPoint.add(insLocation);
		insGpsPoint.add(BroadMyLocation);
		insTraiString = insTraiString + Mylocation.latitude + "    "
				+ Mylocation.longitude + "\n";
		insTraiString = insTraiString + insLocation.latitude + "    "
				+ insLocation.longitude + "\n";
		insGpsString = insGpsString + BroadMyLocation.latitude + "    "
				+ BroadMyLocation.longitude + "\n";
		insAzimuth = insAzimuth + insYow + "\n";
		// insMatrixtring
		double gpspdr = AMapUtils.calculateLineDistance(BroadMyLocation,
				insLocation);
		double gpstrai = AMapUtils.calculateLineDistance(Mylocation,
				BroadMyLocation);
		insTraiGpsDistance = insTraiGpsDistance + gpstrai + "\n";
		insGpsDistance = insGpsDistance + gpspdr + "\n";
		// 绘制惯性导航和GPS及融合轨迹
		drawingINStrack();
		if (Mylocation != null) {
			if (MyMarker != null) {
				MyMarker.remove();
			}
			MyMarker = aMap.addMarker(new MarkerOptions().anchor(0.5f, 0.5f)
					.position(Mylocation).icon(levellogo).draggable(false));
		}
	}

	double guantrailDistance = 0, guangpsDistance = 0, guanDistance = 0;
	List<Polyline> Rongheroute = new ArrayList<Polyline>();
	List<Polyline> Singleroute = new ArrayList<Polyline>();
	List<Polyline> GPSroute = new ArrayList<Polyline>();

	/**
	 * 　绘制INS和GPS以及融合轨迹
	 */
	private void drawingINStrack() {
		int length = insTraipPoint.getPoints().size();
		int length1 = insPoint.getPoints().size();
		int length2 = insGpsPoint.getPoints().size();
		if (length >= 2) {
			LatLng lastpoint = insTraipPoint.getPoints().get(length - 2);
			LatLng nowpoint = insTraipPoint.getPoints().get(length - 1);
			int color = Color.RED;
			Rongheroute.add(aMap.addPolyline(new PolylineOptions()
					.add(lastpoint, nowpoint).color(color).width(6)));

			guantrailDistance = guantrailDistance
					+ AMapUtils.calculateLineDistance(lastpoint, nowpoint);
			trailDistance = trailDistance
					+ AMapUtils.calculateLineDistance(lastpoint, nowpoint);
		}
		if (length1 >= 2) {
			LatLng lastpoint = insPoint.getPoints().get(length1 - 2);
			LatLng nowpoint = insPoint.getPoints().get(length1 - 1);
			int color = Color.BLUE;
			Singleroute.add(aMap.addPolyline(new PolylineOptions()
					.add(lastpoint, nowpoint).color(color).width(3)));
			guanDistance = guanDistance
					+ AMapUtils.calculateLineDistance(lastpoint, nowpoint);
			guanDistance = ((int) (guanDistance * 10)) / 10;
		}
		if (length2 >= 2) {
			LatLng lastpoint = insGpsPoint.getPoints().get(length2 - 2);
			LatLng nowpoint = insGpsPoint.getPoints().get(length2 - 1);
			int color = Color.GREEN;
			GPSroute.add(aMap.addPolyline(new PolylineOptions()
					.add(lastpoint, nowpoint).color(color).width(3)));
			guangpsDistance = guangpsDistance
					+ AMapUtils.calculateLineDistance(lastpoint, nowpoint);
			guangpsDistance = ((int) (guangpsDistance * 10)) / 10;
		}
		String result = String.format("%.2f", trailDistance);
		alldistance.setText("总距离:" + result + "m");
		allCalorie = allCalorie + Calorie(guantrailDistance);
		String resulta = String.format("%.2f", allCalorie);
		kaluli.setText("卡路里:" + resulta + "千卡");
	}

	/**
	 * 初始化地图Ｖｉｅｗ
	 */
	private void initMap() {
		if (aMap == null) {
			aMap = mapView.getMap();
			setUpMap();
		}
	}

	/**
	 * 地图初始化设置
	 */
	private void setUpMap() {
		LatLng points0 = new LatLng(30.535913, 114.362403);
		aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(points0, 16));
		MyLocationStyle locatestyle = new MyLocationStyle();
		locatestyle.myLocationIcon(BitmapDescriptorFactory
				.fromResource(R.drawable.maker2));
		locatestyle.radiusFillColor(Color.argb(20, 0, 200, 10));
		locatestyle.strokeColor(Color.argb(20, 0, 200, 10)).strokeWidth(1);
		aMap.setMyLocationStyle(locatestyle);
		aMap.setLocationSource(this);// 设置定位层
		aMap.getUiSettings().setMyLocationButtonEnabled(false);
		aMap.getUiSettings().setCompassEnabled(false);
		aMap.getUiSettings().setLogoPosition(
				AMapOptions.LOGO_POSITION_BOTTOM_RIGHT);
		aMap.getUiSettings().setZoomPosition(
				AMapOptions.ZOOM_POSITION_RIGHT_CENTER);
		// 显示比例尺
		aMap.getUiSettings().setScaleControlsEnabled(true);
		aMap.setMapType(AMap.MAP_TYPE_NORMAL);
		aMap.getUiSettings().setZoomControlsEnabled(false);
		aMap.setMyLocationEnabled(true);// 设置为true表示显示定位层并可触发定位，false表示隐藏定位层并不可触发定位，默认是false
		aMap.setOnMapLoadedListener(MainActivity.this);// 设置amap加载成功事件监听器
		aMap.setOnMarkerClickListener(MainActivity.this);// 设置点击marker事件监听器
		aMap.setOnInfoWindowClickListener(MainActivity.this);// 设置点击infoWindow事件监听器
		aMap.setInfoWindowAdapter(MainActivity.this);// 设置自定义InfoWindow样式
	}

	/**
	 * 计时器函数，开始计时
	 */
	@SuppressWarnings("unused")
	private void startTimer() {
		if (timer == null) {
			timer = new Timer();
		}
		if (task == null) {
			isTimerStart = true;
			task = new TimerTask() {
				@Override
				public void run() {
					sendMessage(2);
				}
			};
		}
		if (timer != null && task != null)
			timer.schedule(task, 0, 500);
	}

	/**
	 * 计时器函数，停止计时
	 */
	@SuppressWarnings("unused")
	private void stopTimer() {
		if (timer != null) {
			isTimerStart = false;
			timer.cancel();
			timer = null;
		}
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	/**
	 * 计时器函数，发送定时消息
	 */
	public void sendMessage(int id) {
		if (handler != null) {
			Message message = Message.obtain(handler, id);
			handler.sendMessage(message);
		}
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

	@Override
	public void onPoiItemSearched(PoiItem arg0, int arg1) {

	}

	@Override
	public void onPoiSearched(PoiResult arg0, int arg1) {

	}

	@Override
	public void onBusRouteSearched(BusRouteResult arg0, int arg1) {

	}

	@Override
	public void onDriveRouteSearched(DriveRouteResult arg0, int arg1) {

	}

	@Override
	public void onWalkRouteSearched(WalkRouteResult arg0, int arg1) {

	}

	@Override
	public void onTouch(MotionEvent arg0) {

	}

	@Override
	public void onMapClick(LatLng arg0) {

	}

	@Override
	public View getInfoContents(Marker arg0) {
		return null;
	}

	@Override
	public View getInfoWindow(Marker arg0) {
		return null;
	}

	@Override
	public void onMapLoaded() {

	}

	@Override
	public void onInfoWindowClick(Marker arg0) {

	}

	@Override
	public boolean onMarkerClick(Marker arg0) {
		return false;
	}

	/**
	 * 定位参数设置
	 * 
	 * @param listener
	 *            定位监听对象
	 */
	@Override
	public void activate(OnLocationChangedListener listener) {
		mListener = listener;
		if (mlocationClient == null) {
			mlocationClient = new AMapLocationClient(this);
			mLocationOption = new AMapLocationClientOption();
			mlocationClient.setLocationListener(this);
			// 设置为高精度定位模式
			mLocationOption.setLocationMode(AMapLocationMode.Hight_Accuracy);
			mLocationOption.setInterval(2000);
			mLocationOption.setNeedAddress(false);
			mLocationOption.setOnceLocation(false);
			mLocationOption.setWifiActiveScan(true);
			mLocationOption.setMockEnable(false);
			mLocationOption.setGpsFirst(true);
			mlocationClient.setLocationOption(mLocationOption);
			mlocationClient.startLocation();
		}
	}

	@Override
	public void deactivate() {
		mListener = null;
		if (mlocationClient != null) {
			mlocationClient.stopLocation();
			mlocationClient.onDestroy();
		}
		mlocationClient = null;
	}

	@Override
	protected void onResume() {
		super.onResume();
		mapView.onResume();
	}

	@Override
	protected void onPause() {
		super.onPause();
		mapView.onPause();
		deactivate();

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		mapView.onSaveInstanceState(outState);
	}

	private void changeCamera(CameraUpdate update, CancelableCallback callback) {
		aMap.animateCamera(update, 400, callback);
	}

	/**
	 * 如果AMapLocationClient是在当前Activity实例化的，
	 * 在Activity的onDestroy中一定要执行AMapLocationClient的onDestroy
	 */
	@Override
	protected void onDestroy() {
		endSensor();// 取消传感器注册
		if (null != mlocationClient) {
			mlocationClient.onDestroy();
			mlocationClient = null;
			mLocationOption = null;
		}
		mapView.onDestroy();
		super.onDestroy();
	}

	@Override
	public void onCameraChange(CameraPosition arg0) {

	}

	/**
	 * 监听地图缩放级别
	 */
	@Override
	public void onCameraChangeFinish(CameraPosition arg0) {
		if (aMap.getCameraPosition().zoom == aMap.getMaxZoomLevel()) {
			map_in.setImageResource(R.drawable.map_in1);
		} else {
			map_in.setImageResource(R.drawable.map_in);
		}

		if (aMap.getCameraPosition().zoom == aMap.getMinZoomLevel()) {
			map_out.setImageResource(R.drawable.map_out1);
		} else {
			map_out.setImageResource(R.drawable.map_out);
		}
	}

	@Override
	public void onMapLongClick(LatLng arg0) {

	}

}
