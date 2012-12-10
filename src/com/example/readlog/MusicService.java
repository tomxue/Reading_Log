package com.example.readlog;

import com.example.readlog.R;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;

public class MusicService extends Service {
	// MediaPlayer对象
	private MediaPlayer player;

    // startService() used, bindService() not used
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@SuppressWarnings("deprecation")
	public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
		// 这里可以理解为装载音乐文件
		player = MediaPlayer.create(this, R.raw.anydo_pop);
		// 开始播放
		player.start();
	}

	public void onDestroy() {
		super.onDestroy();
		// 停止音乐-停止Service
		player.stop();
	}

}

