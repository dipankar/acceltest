package com.example.acceltest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphView.LegendAlign;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphViewSeries.GraphViewSeriesStyle;
import com.jjoe64.graphview.LineGraphView;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class MainActivity extends Activity implements SensorEventListener  {

	private float mLastX, mLastY, mLastZ;
	private SensorManager mSensorManager;
	private Sensor mAccelerometer;
	private GraphViewSeries seriesX;
	private GraphViewSeries seriesY;
	private GraphViewSeries seriesZ;
	private int graphCount = 1;
	private Handler mHandler;
	private Button button;
	private boolean loggingOn = false;
	private File logFile;
	
	Runnable graphTimer = new Runnable() {
		@Override
		public void run() {
			  seriesX.appendData(new GraphViewData(graphCount, mLastX), true, 1000);
			  seriesY.appendData(new GraphViewData(graphCount, mLastY), true, 1000);
			  seriesZ.appendData(new GraphViewData(graphCount, mLastZ), true, 1000);
			  graphCount++;
		      mHandler.postDelayed(this, 100);
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
		GraphViewData[] data = new GraphViewData[1];
		data[0] = new GraphViewData(0, 0);
	    
		seriesX = new GraphViewSeries("X", new GraphViewSeriesStyle(Color.RED, 3),data);
		seriesY = new GraphViewSeries("Y", new GraphViewSeriesStyle(Color.BLUE, 3),data);
		seriesZ = new GraphViewSeries("Z", new GraphViewSeriesStyle(Color.GREEN, 3),data);
	
		GraphView graphView = new LineGraphView(
			      this
			      , "Accelerometer"
		);
		
		LinearLayout layout = (LinearLayout) findViewById(R.id.graph1);
		graphView.addSeries(seriesX);
		graphView.addSeries(seriesY);
		graphView.addSeries(seriesZ);
		graphView.setShowLegend(true);
		graphView.setLegendAlign(LegendAlign.TOP);
		graphView.setScalable(true);
		graphView.setViewPort(0, 999);		
		layout.addView(graphView);
		
		mHandler = new Handler();
		mHandler.postDelayed(graphTimer, 1000);
		
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        BroadcastReceiver mReceiver = new ScreenReceiver();
        registerReceiver(mReceiver, filter);
        
        button = (Button) findViewById(R.id.button1);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
				if(!loggingOn) {
            		button.setEnabled(false);
            		loggingOn = true;
            		mHandler.post(stateLogger);
            		logFile = new File("sdcard/log.file");
            		   if (!logFile.exists())
            		   {
            		      try
            		      {
            		         logFile.createNewFile();
            		      } 
            		      catch (IOException e)
            		      {
            		         // TODO Auto-generated catch block
            		         e.printStackTrace();
            		      }
            		   }
            	}
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		// TODO Auto-generated method stub
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		// TODO Auto-generated method stub
		float x = event.values[0];
		float y = event.values[1];
		float z = event.values[2];
		mLastX = x;
		mLastY = y;
		mLastZ = z;
	}

	private Boolean screenStateOn = true; 

	protected void onResume() {
        if (!ScreenReceiver.wasScreenOn) {
        	screenStateOn = true;
        } else {
        }
		super.onResume();
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
	}
	protected void onPause() {
        if (ScreenReceiver.wasScreenOn) {
        	screenStateOn = false;
        } else {
        }
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
	
	private Boolean clicked = false;
	private float clickedX = 0f;
	private float clickedY = 0f;
	
	@Override
    public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction() == MotionEvent.ACTION_DOWN) {
        	clicked = true;
            clickedX = event.getX();
            clickedY = event.getY();
        } 
        if(event.getAction() == MotionEvent.ACTION_UP) {
        	clicked = false;
            clickedX = 0f;
            clickedY = 0f;
        }
        return super.onTouchEvent(event);
    }
	
	private int stateNum = 0;
	Runnable stateLogger = new Runnable() {
		@Override
		public void run() {
			  // Write the logging code
			try
			   {
				  String text = String.format("%f,%f,%f,%b,%b,%f,%f",mLastX,mLastY,mLastZ,screenStateOn,clicked,clickedX,clickedY);
			      BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
			      buf.append(text);
			      buf.newLine();
			      buf.close();
			   }
			   catch (IOException e)
			   {
			      // TODO Auto-generated catch block
			      e.printStackTrace();
			   }
			  if(stateNum<15000) {
				  stateNum++;
				  mHandler.postDelayed(this, 20);
			  } else {
				  stateNum=0;
				  button.setEnabled(true);
				  loggingOn = false;
			  }
		}
	};
}