package com.example.readlog;

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

import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Paint.Align;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class History extends Activity {
	private static Map map = new TreeMap<String, Object>();  // TreeMap是有序的，充分利用之，by Tom Xue
	private static final String TAG = "tomxue";
	private static SQLiteDatabase db;
	private final String DBNAME = "readlog.db";
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        
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
		if(count < 7){
			renderer.setXAxisMin(0.5);
			renderer.setXAxisMax(7.5);
		}
		else
		{			
			renderer.setXAxisMin(count-7+0.5);
			renderer.setXAxisMax(count-7+7.5);	
		}

		View chart = ChartFactory.getBarChartView(this, getBarDataset2,
				renderer, Type.DEFAULT);
		setContentView(chart);
		
		db.close();
	}

	private static XYMultipleSeriesDataset getBarDataset(Context cxt) {
		XYMultipleSeriesDataset dataset = new XYMultipleSeriesDataset();
		CategorySeries series = new CategorySeries("最近31天");

		Cursor c = db.rawQuery("SELECT _id, mydate, mydata FROM mytable", new String[]{});  
        while (c.moveToNext()) {
            int _id = c.getInt(c.getColumnIndex("_id"));
            String mydate = c.getString(c.getColumnIndex("mydate"));
            int mydata = c.getInt(c.getColumnIndex("mydata"));
            map.put(mydate, mydata);
//          series.add(mydate, mydata);
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
        	              key_tmp = (String)e.getKey();
        	              value_tmp = Integer.parseInt((e.getValue().toString()));
        	              series.add(key_tmp,value_tmp);
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
		renderer.setChartTitle("最近31天");
		renderer.setXTitle("日期");
		renderer.setYTitle("番茄数");
		renderer.setYAxisMin(0);
		renderer.setYAxisMax(31);
		// set it by default
		renderer.setXAxisMin(0.5);
		renderer.setXAxisMax(7.5);
		
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

}