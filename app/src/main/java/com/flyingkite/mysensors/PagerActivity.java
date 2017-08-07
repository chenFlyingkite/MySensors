package com.flyingkite.mysensors;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.widget.TextView;

public class PagerActivity extends Activity {
    private TabLayout tabs;
    private ViewPager pager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pager);

        init();
    }

    private void init() {
        PagerAdapter pa;
        //pa = new MyPagerAdapter();
        pa = new ImagePagerAdapter();

        tabs = (TabLayout) findViewById(R.id.myPagerTabs);
        pager = (ViewPager) findViewById(R.id.myPager);

        for (int i = 0; i < pa.getCount(); i++) {
            TabLayout.Tab t = tabs.newTab().setCustomView(R.layout.view_text);
            TextView txt = (TextView) t.getCustomView().findViewById(R.id.itsText);
            txt.setText("#" + i);
            tabs.addTab(t);
        }
        tabs.addOnTabSelectedListener(new TabLayout.ViewPagerOnTabSelectedListener(pager));

        pager.setAdapter(pa);
        pager.addOnPageChangeListener(new TabLayout.TabLayoutOnPageChangeListener(tabs));
    }

    private static void log(String msg) {
        Log.e("Pager", msg);
    }
    private static void logF(String format, Object... params) {
        Log.e("Pager", String.format(java.util.Locale.US, format, params));
    }
}
