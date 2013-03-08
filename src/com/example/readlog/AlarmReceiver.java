package com.example.readlog;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
	public final String MYTAG = "tomxue";

	@Override
	public void onReceive(Context context, Intent intent) {
		Log.v(MYTAG, "I am AlarmReceiver,I receive the message");
		Intent in = new Intent();
		in.setClass(context, ReadLog.class);
		in.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(in);
	}
}