package com.flyingkite.mysensors;

import android.graphics.Bitmap;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.memory.MemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;

public class MyPagerAdapter extends PagerAdapter {
    @Override
    public int getCount() {
        return 30;
    }

    @Override
    public Object instantiateItem(ViewGroup container, final int position) {
        log("init #" + position);
        View v = LayoutInflater.from(container.getContext()).inflate(R.layout.view_recycler_item, container, false);
        TextView txt = (TextView) v.findViewById(R.id.myText);
        txt.setText("" + position);

        final ImageView img = (ImageView) v.findViewById(R.id.myImg);

        ImageLoader.getInstance().loadImage("drawable://" + R.drawable.ic_launcher_round, new ImageLoadingListener() {
            private static final String TAG = "UIL_VP";
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                Log.e(TAG, " Go #" + position);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                Log.e(TAG, "Fail #" + position);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                Log.e(TAG, " OK  #" + position + ", View = " + view + ", uri = " + imageUri);
                //((ImageView)view).setImageBitmap(loadedImage);

                MemoryCache cache = ImageLoader.getInstance().getMemoryCache();
                cache.put(imageUri + "VP", loadedImage);
                img.setImageBitmap(loadedImage);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                Log.e(TAG, "X__X #" + position);

                if (position == 1) {
                    MemoryCache cache = ImageLoader.getInstance().getMemoryCache();
                    Log.e(TAG, "keys = " + cache.keys());
                    for (String key : cache.keys()) {
                        Log.e(TAG, "key = " + key + ", bmp = " + cache.get(key));
                    }

                    DiskCache dk = ImageLoader.getInstance().getDiskCache();
                    Log.e(TAG, "File = " + dk.get(imageUri) + "\n uri = " + imageUri);
                }

                MemoryCache cache = ImageLoader.getInstance().getMemoryCache();
                Bitmap bmp = cache.get(imageUri + "VP");
                Log.e(TAG, "Mem cache = " + bmp);
                if (bmp != null && !bmp.isRecycled()) {
                    Log.e(TAG, "Set it #" + position);
                    img.setImageBitmap(bmp);
                }

                DiskCache dk = ImageLoader.getInstance().getDiskCache();


                //Log.e("UIL", "File = " + dk.get(imageUri));
                File f = dk.get(imageUri);
                //holder.img.setImageURI(Uri.fromFile(f));
                //Log.e("UIL", "File = " + f.getAbsolutePath());
                //Log.e("UIL", "File = " + f.exists());
                //Glide.with(holder.img.getContext()).load(f).into(holder.img);

            }
        });
        container.addView(v);
        return v;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        logF("destroy #%s, %s", position, object);
        container.removeView((View)object);
    }

    private static void log(String msg) {
        Log.e("PagerAdapter", msg);
    }
    private static void logF(String format, Object... params) {
        Log.e("PagerAdapter", String.format(java.util.Locale.US, format, params));
    }
}
