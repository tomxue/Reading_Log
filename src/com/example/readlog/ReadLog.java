package com.example.readlog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import com.example.readlog.R;
import android.os.Bundle;
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
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.Gravity;
import android.os.Vibrator;

public class ReadLog extends Activity {

	private static final String TAG = "TomXue";
	private final static String FROMPATH = "/data/data/com.example.readlog/databases/";
	private final static String TOPATH = "/mnt/extSdCard/Tom/Readlog/";

	private Button button_one, button_backup, button_history, button_restore,
			button_about, button_minus;

	private TextView textNum;

	protected static final int STOP = 0x10000;
	protected static final int NEXT = 0x10001;

	private static String mydate_key;
	private final String DBNAME = "readlog.db";
	private static SQLiteDatabase db;
	private static Vibrator vt;
	private static int TotalNum;

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
		button_history = (Button) findViewById(R.id.button3);
		button_restore = (Button) findViewById(R.id.button4);
		button_about = (Button) findViewById(R.id.button5);
		button_minus = (Button) findViewById(R.id.button6);
		textNum = (TextView) findViewById(R.id.textView1);

		button_restore.setEnabled(true);

		// to show Total read pages marked with red color
		// 每结束一个page，再操作db
		dbHandler(0);
		textNum.setText("Read total " + Integer.toString(TotalNum) + " pages");
		textNum.setTextColor(android.graphics.Color.RED);
		TotalNum = 0;

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
				vt.vibrate(1000);

				textNum.setText("Total: " + Integer.toString(TotalNum)
						+ " pages");
				textNum.setTextColor(android.graphics.Color.RED);
				TotalNum = 0;
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
				vt.vibrate(1000);

				textNum.setText("Total: " + Integer.toString(TotalNum)
						+ " pages");
				textNum.setTextColor(android.graphics.Color.RED);
				TotalNum = 0;
			}
		});

		button_backup.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				if (copy(FROMPATH, TOPATH) == 0) {
					Toast.makeText(ReadLog.this, "Backup succeed!",
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(ReadLog.this, "Backup failed!",
							Toast.LENGTH_LONG).show();
				}

			}
		});

		button_restore.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {

				if (copy(TOPATH, FROMPATH) == 0) {
					Toast.makeText(ReadLog.this, "Restore succeed!",
							Toast.LENGTH_LONG).show();
				} else {
					Toast.makeText(ReadLog.this, "Restore failed!",
							Toast.LENGTH_LONG).show();
				}

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

		button_history.setOnClickListener(new Button.OnClickListener() {
			public void onClick(View v) {
				// Switch to report page
				Intent intent = new Intent();
				intent.setClass(ReadLog.this, History.class);
				startActivity(intent);
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
}
