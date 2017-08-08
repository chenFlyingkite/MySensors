package com.flyingkite.mysensors;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.flyingkite.utils.TextAutoRun;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class MainActivity extends Activity {
    private SensorManager mSensorManager;
    private Sensor thermal;
    private RecyclerView recycler;
    private TextAutoRun run;

    private static final int[] SENSOR_TYPES = {Sensor.TYPE_PROXIMITY, Sensor.TYPE_LIGHT};

    private interface F {
        void g();
        default void f() {
            log("Hi, f");
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        log("onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this)
                //.memoryCache(new LruMemoryCache(20 * 1024 * 1024))
                .build();

        ImageLoader.getInstance().init(config);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        thermal = mSensorManager.getDefaultSensor(SensorManager.SENSOR_TEMPERATURE);

        init();
    }

    private void init() {
        GridLayoutManager grid = new GridLayoutManager(this, 3);
        recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setLayoutManager(grid);
        recycler.setAdapter(new MyAdapter());

        findViewById(R.id.enterPager).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, PagerActivity.class));
            }
        });

        run = new TextAutoRun((TextView) findViewById(R.id.autoText)).to(50).speed(10);
        run.run();
    }

    @Override
    protected void onResume() {
        log("onResume");
        super.onResume();
        listSensor(Sensor.TYPE_ALL);
        log("---------------------");
//        listSensor(Sensor.TYPE_TEMPERATURE);
//        log("---------------------");
//        listSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);
        for (int type : SENSOR_TYPES) {
            mSensorManager.registerListener(seListener, mSensorManager.getDefaultSensor(type), SensorManager.SENSOR_DELAY_UI);
        }


        listActivity();
        if (run != null) {
            run.resume();
        }
    }

    private void listActivity() {
        ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        List<ActivityManager.RunningTaskInfo> list = am.getRunningTasks(Integer.MAX_VALUE);

        logF("----   -----");
        int n = list.size();
        logF("%s items", n);
        for (int i = 0; i < n; i++) {
            ActivityManager.RunningTaskInfo r = list.get(i);
            logF("#%s = %s, %s, %s, %s", i
                    , r.numActivities, r.numRunning
                    , r.baseActivity, r.description
            );
        }
    }

    private enum ABCD {
        A("a"), B("b") ,C("c"), D("d");

        private final String msg;

        ABCD(String s) {
            msg = s;
        }
    }

    private void listSensor(int type) {
        List<Sensor> list = mSensorManager.getSensorList(type);
        int n = list.size();
        logF("%s items", n);
        for (int i = 0; i < n; i++) {
            logF("#%s = %s", i, list.get(i));
        }
    }

    private void saySwitch(String s) {
        switch (s) {
            case "a":
                log("See a = " + s);
                break;
            case "b":
                log("See b = " + s);
                break;
            case "c":
                log("See c = " + s);
                break;
            case "d":
                log("See d = " + s);
                break;
            default:
                log("See ? = " + s);
                break;
        }
    }

    @Override
    protected void onPause() {
        log("onPause");
        super.onPause();
        mSensorManager.unregisterListener(seListener);
        if (run != null) {
            run.pause();
        }
    }

    private static void log(String msg) {
        Log.e("Main", msg);
    }
    private static void logF(String format, Object... params) {
        Log.e("Main", String.format(java.util.Locale.US, format, params));
    }

    private SensorEventListener seListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            logF("Sensor event = %s, %s, %s, %s"
                    , event.accuracy
                    , Arrays.toString(event.values)
                    , new Date(event.timestamp).toGMTString()
                    , event.sensor
            );
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            logF("Acc = %s, sensor = %s", accuracy, sensor.getName());
        }
    };
}
