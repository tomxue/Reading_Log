package com.example.readlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.achartengine.ChartFactory;
import org.achartengine.chart.BarChart.Type;
import org.achartengine.model.CategorySeries;
import org.achartengine.model.XYMultipleSeriesDataset;
import org.achartengine.renderer.SimpleSeriesRenderer;
import org.achartengine.renderer.XYMultipleSeriesRenderer;
import org.achartengine.renderer.XYSeriesRenderer;

import com.example.readlog.R;
import android.os.Bundle;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.os.Vibrator;

public class ReadLog extends Activity {

	private static final String TAG = "TomXue";
	private final static String FROMPATH = "/data/data/com.example.readlog/databases/";
	private final static String TOPATH = "/mnt/extSdCard/Tom/Readlog/";

	private Button button_one, button_backup, button_restore, button_about,
			button_minus;
	private TextView textNum;
	private LinearLayout logLayout1;
	private SeekBar scale;

	protected static final int STOP = 0x10000;
	protected static final int NEXT = 0x10001;

	private static String mydate_key;
	private final String DBNAME = "readlog.db";
	private static SQLiteDatabase db;
	private static Vibrator vt;
	private static int TotalNum, TodayNum, BookPages = 300; // default value,
															// 300 pages/book
	private static String BookNum;

	private static Map map = new TreeMap<String, Object>(); // TreeMap是有序的，充分利用之，by

	private static final int defaultDays = 7 + 1;
	private static int days = defaultDays; // set 8 means recent 7 days
											// statistics

	public View chart;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// to keep the screen on
		// WakeLock does not work...
		// PowerManager pm = (PowerManager)
		// getSystemService(Context.POWER_SERVICE);
		// mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK,
		// "TomXue");
		// mWakeLock.acquire();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		vt = (Vibrator) getApplication().getSystemService(
				Service.VIBRATOR_SERVICE);

		// 初始化mydate_key
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		mydate_key = year + "." + (month < 10 ? ("0" + month) : month) + "."
				+ (day < 10 ? ("0" + day) : day);

		button_one = (Button) findViewById(R.id.button1);
		button_backup = (Button) findViewById(R.id.button2);
		button_restore = (Button) findViewById(R.id.button4);
		button_about = (Button) findViewById(R.id.button5);
		button_minus = (Button) findViewById(R.id.button6);
		textNum = (TextView) findViewById(R.id.textView1);
		logLayout1 = (LinearLayout) findViewById(R.id.linearLayout1);
		scale = (SeekBar) findViewById(R.id.seekBar1);

		button_restore.setEnabled(true);
		scale.setMax(365 - days + 1); // to show last (4 months = 120 days) log
										// data within one chart

		// to show Total read pages marked with red color
		// 每结束一个page，再操作db
		// to initialize today's log data
		dbHandler(0);
		textNum.setText("Read total " + Integer.toString(TotalNum)
				+ " pages, today " + Integer.toString(TodayNum)
				+ " pages, total " + BookNum + " books");
		textNum.setTextColor(android.graphics.Color.RED);
		TotalNum = 0;

		onLogShow();

		button_one.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// (1) calculate the date stamp
				Calendar c = Calendar.getInstance();
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH) + 1;
				int day = c.get(Calendar.DAY_OF_MONTH);
				mydate_key = year + "." + (month < 10 ? ("0" + month) : month)
						+ "." + (day < 10 ? ("0" + day) : day);

				// (2) operate the db after one page
				dbHandler(1);

				// play the sound
				startService(new Intent("com.example.readlog.MUSIC"));
				// vibrate
				// vt.vibrate(1000);

				textNum.setText("Read total " + Integer.toString(TotalNum)
						+ " pages, today " + Integer.toString(TodayNum)
						+ " pages, total " + BookNum + " books");
				textNum.setTextColor(android.graphics.Color.RED);
				TotalNum = 0;

				onLogShow();
			}
		});

		button_minus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// (1) calculate the date stamp
				Calendar c = Calendar.getInstance();
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH) + 1;
				int day = c.get(Calendar.DAY_OF_MONTH);
				mydate_key = year + "." + (month < 10 ? ("0" + month) : month)
						+ "." + (day < 10 ? ("0" + day) : day);

				// (2) operate the db after one page
				dbHandler(-1);

				// play the sound
				startService(new Intent("com.example.readlog.MUSIC"));
				// vibrate
				// vt.vibrate(1000);

				textNum.setText("Read total " + Integer.toString(TotalNum)
						+ " pages, today " + Integer.toString(TodayNum)
						+ " pages, total " + BookNum + " books");
				textNum.setTextColor(android.graphics.Color.RED);
				TotalNum = 0;

				onLogShow();
			}
		});

		button_backup.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (copy(FROMPATH, TOPATH) == 0) {
					Toast.makeText(ReadLog.this, "Backup succeed!",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(ReadLog.this, "Backup failed!",
							Toast.LENGTH_SHORT).show();
				}

				dummybutton_zero();
			}
		});

		button_restore.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				if (copy(TOPATH, FROMPATH) == 0) {
					Toast.makeText(ReadLog.this, "Restore succeed!",
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(ReadLog.this, "Restore failed!",
							Toast.LENGTH_SHORT).show();
				}

				dummybutton_zero();

				// Clear db
				// try {
				// db.close();
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				//
				// deleteDatabase("readlog.db");
				//
				// Toast toast;
				// toast = Toast.makeText(getApplicationContext(),
				// "the db is deleted.", Toast.LENGTH_LONG);
				// toast.setGravity(Gravity.BOTTOM, 0, 0);
				// toast.show();
			}
		});

		button_about.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				Toast toast;
				toast = Toast.makeText(getApplicationContext(),
						"Author: Tom Xue" + "\n"
								+ "Email: tomxue0126@gmail.com" + "\n"
								+ "https://github.com/tomxue/Reading_Log.git",
						Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM, 0, 0);
				toast.show();
			}
		});

		scale.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			// 复写OnSeeBarChangeListener的三个方法
			// 第一个时OnStartTrackingTouch,在进度开始改变时执行
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			// 第二个方法onProgressChanged是当进度发生改变时执行
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				days = seekBar.getProgress() + defaultDays;
				onLogShow();
			}

			// 第三个是onStopTrackingTouch,在停止拖动时执行
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				days = seekBar.getProgress() + defaultDays;
				onLogShow();
			}
		});
	}

	public void dummybutton_zero() {
		// (1) calculate the date stamp
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		mydate_key = year + "." + (month < 10 ? ("0" + month) : month) + "."
				+ (day < 10 ? ("0" + day) : day);

		// (2) operate the db after one page
		dbHandler(0);

		// play the sound
		startService(new Intent("com.example.readlog.MUSIC"));
		// vibrate
		// vt.vibrate(1000);

		textNum.setText("Read total " + Integer.toString(TotalNum)
				+ " pages, today " + Integer.toString(TodayNum)
				+ " pages, total " + BookNum + " books");
		textNum.setTextColor(android.graphics.Color.RED);
		TotalNum = 0;

		onLogShow();
	}

	public void onLogShow() {
		// Switch to log page/activity
		// Intent intent = new Intent();
		// intent.setClass(ReadLog.this, History.class);
		// startActivity(intent);

		// 打开或创建tompomodoros.db数据库
		db = openOrCreateDatabase(DBNAME, Context.MODE_PRIVATE, null);
		db.execSQL("CREATE TABLE if not exists mytable (_id INTEGER PRIMARY KEY AUTOINCREMENT, mydate VARCHAR, mydata SMALLINT)");
		XYMultipleSeriesRenderer renderer = getBarDemoRenderer();
		setChartSettings(renderer);
		// (1) during it map was filled, by Tom Xue
		XYMultipleSeriesDataset getBarDataset2 = getBarDataset(this);

		int count = 1;
		// 这里比较重要，这里手动给X轴填刻度。有多少条内容，你就要添多少个刻度，这样X轴就显示的是时间，也能显示出长方形图
		// (2) then map is further rendered, by Tom Xue
		for (Object key_tmp : map.keySet()) {
			renderer.addXTextLabel(count, key_tmp.toString());
			System.out.println("------map-------");
			System.out.println(key_tmp.toString());
			count++;
		}

		// show the last 7 data/bars
		if (count < days) {
			renderer.setXAxisMin(0.5);
			renderer.setXAxisMax(days + 0.5);
		} else {
			renderer.setXAxisMin(count - days + 0.5);
			renderer.setXAxisMax(count + 0.5);
		}

		chart = ChartFactory.getBarChartView(this, getBarDataset2, renderer,
				Type.DEFAULT);
		// setContentView(chart);

		// refresh the View chart
		logLayout1.addView(chart);
		logLayout1.removeAllViewsInLayout();
		logLayout1.addView(chart);

		db.close();
	}

	private void dbHandler(int pages) {
		DecimalFormat df = new DecimalFormat("0.000");

		// 打开或创建tompomodoros.db数据库
		db = openOrCreateDatabase(DBNAME, Context.MODE_PRIVATE, null);
		// 创建mytable表
		db.execSQL("CREATE TABLE if not exists mytable (_id INTEGER PRIMARY KEY AUTOINCREMENT, mydate VARCHAR, mydata SMALLINT)");
		// ContentValues以键值对的形式存放数据, make the table not empty, by Tom Xue
		ContentValues cv;

		int mydata_dbitem = 0;
		boolean dbitem_exist = false;
		Cursor c = db.rawQuery("SELECT _id, mydate, mydata FROM mytable",
				new String[] {});
		while (c.moveToNext()) { // 如果在数据库中找到当日日期，则更新当日数据和TotalNum
			String mydate_item = c.getString(c.getColumnIndex("mydate"));
			if (mydate_item.equals(mydate_key)) {
				mydata_dbitem = c.getInt(c.getColumnIndex("mydata"));
				cv = new ContentValues();
				if (pages == 1) {
					cv.put("mydata", 1 + mydata_dbitem);
					TotalNum = TotalNum + 1 + mydata_dbitem;
					TodayNum = 1 + mydata_dbitem;
				} else if (pages == 0) {
					cv.put("mydata", 0 + mydata_dbitem);
					TotalNum = TotalNum + 0 + mydata_dbitem;
					TodayNum = 0 + mydata_dbitem;
				} else if (pages == -1) {
					if (mydata_dbitem > 0) {
						cv.put("mydata", -1 + mydata_dbitem);
						TotalNum = TotalNum - 1 + mydata_dbitem;
						TodayNum = -1 + mydata_dbitem;
					} else
						cv.put("mydata", 0);
				}
				// 更新数据
				db.update("mytable", cv, "mydate = ?",
						new String[] { mydate_key });
				dbitem_exist = true;
			} else { // 如果在数据库中遇到非当日日期，则只更新TotalNum
				mydata_dbitem = c.getInt(c.getColumnIndex("mydata"));
				TotalNum = TotalNum + mydata_dbitem;
				TodayNum = 0;
			}
		}
		BookNum = df.format((float) TotalNum / BookPages);

		c.close();

		// 如果当日日期不在数据库中，则插入当日日期的数据
		if (dbitem_exist == false) {
			// ContentValues以键值对的形式存放数据
			cv = new ContentValues();
			cv.put("mydate", mydate_key);
			if (pages == 1) {
				cv.put("mydata", 1);
				TotalNum = TotalNum + 1;
				TodayNum = TodayNum + 1;
			} else if (pages == 0)
				cv.put("mydata", 0);
			else if (pages == -1)
				cv.put("mydata", 0);
			// 插入ContentValues中的数据
			db.insert("mytable", null, cv);

			BookNum = df.format((float) TotalNum / BookPages);
		}

		db.close();
	}

	public int copy(String fromFile, String toFile) {
		// 要复制的文件目录
		File[] currentFiles;
		File root = new File(fromFile);
		// 如同判断SD卡是否存在或者文件是否存在
		// 如果不存在则 return出去
		if (!root.exists()) {
			return -1;
		}
		// 如果存在则获取当前目录下的全部文件 填充数组
		currentFiles = root.listFiles();

		// 目标目录
		File targetDir = new File(toFile);
		// 创建目录
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		// 遍历要复制该目录下的全部文件
		for (int i = 0; i < currentFiles.length; i++) {
			if (currentFiles[i].isDirectory())// 如果当前项为子目录,进行递归
			{
				copy(currentFiles[i].getPath() + "/",
						toFile + currentFiles[i].getName() + "/");

			} else// 如果当前项为文件则进行文件拷贝
			{
				CopySdcardFile(currentFiles[i].getPath(), toFile
						+ currentFiles[i].getName());
			}
		}
		return 0;
	}

	// 文件拷贝
	// 要复制的目录下的所有非子目录(文件夹)文件拷贝
	public int CopySdcardFile(String fromFile, String toFile) {
		try {
			InputStream fosfrom = new FileInputStream(fromFile);
			OutputStream fosto = new FileOutputStream(toFile);
			byte bt[] = new byte[1024];
			int c;
			while ((c = fosfrom.read(bt)) > 0) {
				fosto.write(bt, 0, c);
			}
			fosfrom.close();
			fosto.close();
			return 0;

		} catch (Exception ex) {
			return -1;
		}
	}

	public void onPause() {
		super.onPause();

		// if (mWakeLock.isHeld())
		// mWakeLock.release();
		Log.v(TAG, "onPause");
	}

	public void onStop() {
		super.onStop();

		// if (mWakeLock.isHeld())
		// mWakeLock.release();
		Log.v(TAG, "onStop");
	}

	// achartengine related functions below
	private static XYMultipleSeriesDataset getBarDataset(Context cxt) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		CategorySeries series = new CategorySeries("All technical books");

		Cursor c = db.rawQuery("SELECT _id, mydate, mydata FROM mytable",
				new String[] {});
		while (c.moveToNext()) {
			int _id = c.getInt(c.getColumnIndex("_id"));
			String mydate = c.getString(c.getColumnIndex("mydate"));
			int mydata = c.getInt(c.getColumnIndex("mydata"));
			map.put(mydate, mydata);
			// series.add(mydate, mydata);
			Log.v(TAG, "while loop times");
			System.out.println(_id);
			System.out.println(mydate);
			System.out.println(mydata);
			System.out.println("---------------------");
		}
		c.close();

		// map -> series, 有序化显示数据
		// 如果用户随意修改手机日期，那么db中的数据就未必是按照日期排列的
		// 为了按照日期显示结果，利用了有序的TreeMap
		Iterator it = map.entrySet().iterator();
		double value_tmp;
		String key_tmp;
		while (it.hasNext()) {
			Map.Entry e = (Map.Entry) it.next();
			System.out.println("Key: " + e.getKey() + "; Value: "
					+ e.getValue());
			key_tmp = (String) e.getKey();
			value_tmp = Integer.parseInt((e.getValue().toString()));
			series.add(key_tmp, value_tmp);
		}

		dataset.addSeries(series.toXYSeries());
		return dataset;
	}

	private static XYMultipleSeriesRenderer getBarDemoRenderer() {
		XYMultipleSeriesRenderer renderer = new XYMultipleSeriesRenderer();
		renderer.setAxisTitleTextSize(16);
		renderer.setChartTitleTextSize(16);
		renderer.setLabelsTextSize(16);
		renderer.setLegendTextSize(16);
		renderer.setMargins(new int[] { 20, 30, 15, 0 });
		XYSeriesRenderer r = new XYSeriesRenderer();
		r.setColor(Color.BLUE);
		renderer.addSeriesRenderer(r);
		return renderer;
	}

	private static void setChartSettings(XYMultipleSeriesRenderer renderer) {
		renderer.setChartTitle("Recent " + Integer.toString(days - 1) + " days");
		renderer.setXTitle("Date");
		renderer.setYTitle("Pages");
		renderer.setYAxisMin(0);
		renderer.setYAxisMax(31);
		// set it by default
		renderer.setXAxisMin(0.5);
		renderer.setXAxisMax(days + 0.5); // show recent 7 days statistics

		renderer.setShowLegend(true);
		renderer.setShowLabels(true);
		renderer.setXLabels(1);
		renderer.setBackgroundColor(Color.WHITE);
		// 设置页边空白的颜色
		renderer.setMarginsColor(Color.GRAY);
		// 设置x,y轴显示的排列
		renderer.setXLabelsAlign(Align.CENTER);
		renderer.setYLabelsAlign(Align.RIGHT);
		// 设置坐标轴,轴的颜色
		renderer.setAxesColor(Color.RED);
		// 显示网格
		renderer.setShowGrid(true);
		// 设置x,y轴上的刻度的颜色
		renderer.setLabelsColor(Color.GREEN);
		// 设置是否显示,坐标轴的轴,默认为 true
		renderer.setShowAxes(true);
		// 设置条形图之间的距离
		renderer.setBarSpacing(2.5);

		// by it, the x-axis number 0 10 20 30.. can be hiden
		renderer.setXLabels(RESULT_OK);

		int length = renderer.getSeriesRendererCount();

		for (int i = 0; i < length; i++) {
			SimpleSeriesRenderer ssr = renderer.getSeriesRendererAt(i);
			// 不知道作者的居中是怎么计算的,默认是Align.CENTER,但是对于两个以上的条形显示
			// 就画在了最右边
			ssr.setChartValuesTextAlign(Align.RIGHT);
			ssr.setChartValuesTextSize(16);
			ssr.setDisplayChartValues(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}
}