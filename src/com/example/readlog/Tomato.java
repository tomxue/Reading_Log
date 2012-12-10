package com.example.readlog;

import java.util.Calendar;
import com.example.readlog.R;
import android.os.Bundle;
import android.os.PowerManager;
import android.app.Activity;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.os.Vibrator;

public class Tomato extends Activity {

	private static final String TAG = "TomXue";

	private Button button_one, button_zero, button_history, button_clear,
			button_about, button_minus;
	
	private TextView textNum;

	protected static final int STOP = 0x10000;
	protected static final int NEXT = 0x10001;

	private static String mydate_key;
	private final String DBNAME = "readlog.db";
	private static SQLiteDatabase db;
	private static PowerManager.WakeLock mWakeLock;
	private static Vibrator vt;
	private static int TotalNum;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		// screen kept on related
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "TomXue");
		try {
			mWakeLock.acquire();
		} catch (Exception e) {
			e.printStackTrace();
		}

		vt = (Vibrator) getApplication().getSystemService(
				Service.VIBRATOR_SERVICE);

		// 初始化mydate_key
		Calendar c = Calendar.getInstance();
		int year = c.get(Calendar.YEAR);
		int month = c.get(Calendar.MONTH) + 1;
		int day = c.get(Calendar.DAY_OF_MONTH);
		mydate_key = year + "-" + (month < 10 ? ("0" + month) : month) + "-"
				+ (day < 10 ? ("0" + day) : day);

		button_one = (Button) findViewById(R.id.button1);
		button_zero = (Button) findViewById(R.id.button2);
		button_history = (Button) findViewById(R.id.button3);
		button_clear = (Button) findViewById(R.id.button4);
		button_about = (Button) findViewById(R.id.button5);
		button_minus = (Button) findViewById(R.id.button6);
		textNum = (TextView) findViewById(R.id.textView1);
				
		button_clear.setEnabled(true);

		button_one.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				// 每结束一个page，再操作db
				dbHandler(1);

				// play the sound
				startService(new Intent("com.example.readlog.MUSIC"));
				// vibrate
				vt.vibrate(1000);

				Calendar c = Calendar.getInstance();
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH) + 1;
				int day = c.get(Calendar.DAY_OF_MONTH);
				mydate_key = year + "-" + (month < 10 ? ("0" + month) : month)
						+ "-" + (day < 10 ? ("0" + day) : day);
				
				textNum.setText("Total: " + Integer.toString(TotalNum) + " pages");				
				TotalNum = 0;
			}
		});

		button_zero.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// 每结束一个page，再操作db
				dbHandler(0);

				// play the sound
				startService(new Intent("com.example.readlog.MUSIC"));
				// vibrate
				vt.vibrate(1000);

				Calendar c = Calendar.getInstance();
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH) + 1;
				int day = c.get(Calendar.DAY_OF_MONTH);
				mydate_key = year + "-" + (month < 10 ? ("0" + month) : month)
						+ "-" + (day < 10 ? ("0" + day) : day);

				textNum.setText("Total: " + Integer.toString(TotalNum) + " pages");				
				TotalNum = 0;
			}
		});

		button_minus.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// 每结束一个page，再操作db
				dbHandler(-1);

				// play the sound
				startService(new Intent("com.example.readlog.MUSIC"));
				// vibrate
				vt.vibrate(1000);

				Calendar c = Calendar.getInstance();
				int year = c.get(Calendar.YEAR);
				int month = c.get(Calendar.MONTH) + 1;
				int day = c.get(Calendar.DAY_OF_MONTH);
				mydate_key = year + "-" + (month < 10 ? ("0" + month) : month)
						+ "-" + (day < 10 ? ("0" + day) : day);

				textNum.setText("Total: " + Integer.toString(TotalNum) + " pages");				
				TotalNum = 0;
			}
		});

		button_history.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// Switch to report page
				Intent intent = new Intent();
				intent.setClass(Tomato.this, History.class);
				startActivity(intent);
			}
		});

		button_clear.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				try {
					db.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

				deleteDatabase("readlog.db");

				Toast toast;
				toast = Toast.makeText(getApplicationContext(),
						"the db is deleted.", Toast.LENGTH_LONG);
				toast.setGravity(Gravity.BOTTOM, 0, 0);
				toast.show();
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
	}

	private void dbHandler(int pages) {
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
				} else if (pages == 0) {
					cv.put("mydata", 0 + mydata_dbitem);
					TotalNum = TotalNum + 0 + mydata_dbitem;
				} else if (pages == -1) {
					if (mydata_dbitem > 0) {
						cv.put("mydata", -1 + mydata_dbitem);
						TotalNum = TotalNum - 1 + mydata_dbitem;
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
			}
		}

		c.close();

		// 如果当日日期不在数据库中，则插入当日日期的数据
		if (dbitem_exist == false) {
			// ContentValues以键值对的形式存放数据
			cv = new ContentValues();
			cv.put("mydate", mydate_key);
			if (pages == 1) {
				cv.put("mydata", 1);
				TotalNum = TotalNum + 1;
			} else if (pages == 0)
				cv.put("mydata", 0);
			else if (pages == -1)
				cv.put("mydata", 0);
			// 插入ContentValues中的数据
			db.insert("mytable", null, cv);
		}

		db.close();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	public void onPause() {
		super.onPause();

		if (mWakeLock.isHeld())
			mWakeLock.release();
		Log.v(TAG, "onPause");
	}

	public void onStop() {
		super.onStop();

		if (mWakeLock.isHeld())
			mWakeLock.release();
		Log.v(TAG, "onPause");
	}
}
