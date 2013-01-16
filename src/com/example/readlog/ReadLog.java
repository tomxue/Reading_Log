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

	private static Map map = new TreeMap<String, Object>(); // TreeMap������ģ��������֮��by

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

		// ��ʼ��mydate_key
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
		// ÿ����һ��page���ٲ���db
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
			// ��дOnSeeBarChangeListener����������
			// ��һ��ʱOnStartTrackingTouch,�ڽ��ȿ�ʼ�ı�ʱִ��
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
			}

			// �ڶ�������onProgressChanged�ǵ����ȷ����ı�ʱִ��
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				days = seekBar.getProgress() + defaultDays;
				onLogShow();
			}

			// ��������onStopTrackingTouch,��ֹͣ�϶�ʱִ��
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

		// �򿪻򴴽�tompomodoros.db���ݿ�
		db = openOrCreateDatabase(DBNAME, Context.MODE_PRIVATE, null);
		db.execSQL("CREATE TABLE if not exists mytable (_id INTEGER PRIMARY KEY AUTOINCREMENT, mydate VARCHAR, mydata SMALLINT)");
		XYMultipleSeriesRenderer renderer = getBarDemoRenderer();
		setChartSettings(renderer);
		// (1) during it map was filled, by Tom Xue
		XYMultipleSeriesDataset getBarDataset2 = getBarDataset(this);

		int count = 1;
		// ����Ƚ���Ҫ�������ֶ���X����̶ȡ��ж��������ݣ����Ҫ����ٸ��̶ȣ�����X�����ʾ����ʱ�䣬Ҳ����ʾ��������ͼ
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

		// �򿪻򴴽�tompomodoros.db���ݿ�
		db = openOrCreateDatabase(DBNAME, Context.MODE_PRIVATE, null);
		// ����mytable��
		db.execSQL("CREATE TABLE if not exists mytable (_id INTEGER PRIMARY KEY AUTOINCREMENT, mydate VARCHAR, mydata SMALLINT)");
		// ContentValues�Լ�ֵ�Ե���ʽ�������, make the table not empty, by Tom Xue
		ContentValues cv;

		int mydata_dbitem = 0;
		boolean dbitem_exist = false;
		Cursor c = db.rawQuery("SELECT _id, mydate, mydata FROM mytable",
				new String[] {});
		while (c.moveToNext()) { // ��������ݿ����ҵ��������ڣ�����µ������ݺ�TotalNum
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
				// ��������
				db.update("mytable", cv, "mydate = ?",
						new String[] { mydate_key });
				dbitem_exist = true;
			} else { // ��������ݿ��������ǵ������ڣ���ֻ����TotalNum
				mydata_dbitem = c.getInt(c.getColumnIndex("mydata"));
				TotalNum = TotalNum + mydata_dbitem;
				TodayNum = 0;
			}
		}
		BookNum = df.format((float) TotalNum / BookPages);

		c.close();

		// ����������ڲ������ݿ��У�����뵱�����ڵ�����
		if (dbitem_exist == false) {
			// ContentValues�Լ�ֵ�Ե���ʽ�������
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
			// ����ContentValues�е�����
			db.insert("mytable", null, cv);

			BookNum = df.format((float) TotalNum / BookPages);
		}

		db.close();
	}

	public int copy(String fromFile, String toFile) {
		// Ҫ���Ƶ��ļ�Ŀ¼
		File[] currentFiles;
		File root = new File(fromFile);
		// ��ͬ�ж�SD���Ƿ���ڻ����ļ��Ƿ����
		// ����������� return��ȥ
		if (!root.exists()) {
			return -1;
		}
		// ����������ȡ��ǰĿ¼�µ�ȫ���ļ� �������
		currentFiles = root.listFiles();

		// Ŀ��Ŀ¼
		File targetDir = new File(toFile);
		// ����Ŀ¼
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}
		// ����Ҫ���Ƹ�Ŀ¼�µ�ȫ���ļ�
		for (int i = 0; i < currentFiles.length; i++) {
			if (currentFiles[i].isDirectory())// �����ǰ��Ϊ��Ŀ¼,���еݹ�
			{
				copy(currentFiles[i].getPath() + "/",
						toFile + currentFiles[i].getName() + "/");

			} else// �����ǰ��Ϊ�ļ�������ļ�����
			{
				CopySdcardFile(currentFiles[i].getPath(), toFile
						+ currentFiles[i].getName());
			}
		}
		return 0;
	}

	// �ļ�����
	// Ҫ���Ƶ�Ŀ¼�µ����з���Ŀ¼(�ļ���)�ļ�����
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

		// map -> series, ������ʾ����
		// ����û������޸��ֻ����ڣ���ôdb�е����ݾ�δ���ǰ����������е�
		// Ϊ�˰���������ʾ����������������TreeMap
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
		// ����ҳ�߿հ׵���ɫ
		renderer.setMarginsColor(Color.GRAY);
		// ����x,y����ʾ������
		renderer.setXLabelsAlign(Align.CENTER);
		renderer.setYLabelsAlign(Align.RIGHT);
		// ����������,�����ɫ
		renderer.setAxesColor(Color.RED);
		// ��ʾ����
		renderer.setShowGrid(true);
		// ����x,y���ϵĿ̶ȵ���ɫ
		renderer.setLabelsColor(Color.GREEN);
		// �����Ƿ���ʾ,���������,Ĭ��Ϊ true
		renderer.setShowAxes(true);
		// ��������ͼ֮��ľ���
		renderer.setBarSpacing(2.5);

		// by it, the x-axis number 0 10 20 30.. can be hiden
		renderer.setXLabels(RESULT_OK);

		int length = renderer.getSeriesRendererCount();

		for (int i = 0; i < length; i++) {
			SimpleSeriesRenderer ssr = renderer.getSeriesRendererAt(i);
			// ��֪�����ߵľ�������ô�����,Ĭ����Align.CENTER,���Ƕ����������ϵ�������ʾ
			// �ͻ��������ұ�
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