package com.flyingkite.mysensors;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.v4.view.PagerAdapter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.flyingkite.utils.Say;
import com.flyingkite.utils.ThreadUtil;
import com.flyingkite.utils.TicTac2;

import java.io.File;
import java.util.concurrent.ExecutionException;

public class ImagePagerAdapter extends PagerAdapter {
    @Override
    public int getCount() {
        return 100;
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        logF("destroy #%s, %s", position, object);
        container.removeView((View) object);
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        log("init #" + position);
        View v = LayoutInflater.from(container.getContext()).inflate(R.layout.view_recycler_item, container, false);
        TextView txt = (TextView) v.findViewById(R.id.myText);
        txt.setText("" + position);

        final ImageView img = (ImageView) v.findViewById(R.id.myImg);

        String name = "1.jpg";
        final File f = new File(Environment.getExternalStorageDirectory(), name);
        Glide.with(img.getContext()).load(f).into(img);

        /*
        TicTac2 t = new TicTac2();
        t.tic();
        //Bitmap bmp = BitmapFactory.decodeResource(v.getResources(), R.drawable.img_1920x1200);
        //t.tacF("bmp = %s x %s", bmp.getWidth(), bmp.getHeight());
        Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
        Say.LogF("bmp = %s x %s", bmp.getWidth(), bmp.getHeight());
        Say.LogF("bmp = %s MB", bmp.getByteCount() / 1024.0 / 1024.0);
        //img.setImageURI(Uri.fromFile(f));
        t.tacF("done : " + f.getAbsolutePath());
        img.setImageBitmap(bmp);
        */
        getBitmapFromGlide(f, img);

        container.addView(v);
        return v;
    }

    private void getBitmapFromGlide(final File f, final ImageView img) {
        ThreadUtil.runOnWorkerThread(new Runnable() {
            private static final int GLIDE = 0;
            private static final int BITMAP_FACTORY = 1;
            private static final int BITMAP_FACTORY_OPTION = 2;

            @Override
            public void run() {
                testDecodeBmp(GLIDE);
                testDecodeBmp(BITMAP_FACTORY);
                testDecodeBmp(BITMAP_FACTORY_OPTION);
            }

            private void testDecodeBmp(int fetchMethod) {
                Bitmap bmp;
                TicTac2 t = new TicTac2();
                String head = "";
                try {
                    t.tic();

                    switch (fetchMethod) {
                        default:
                        case GLIDE:
                            head = "Glide >      ";
                            bmp = Glide.with(img.getContext()).load(f).asBitmap().into(-1, -1).get();
                            break;
                        case BITMAP_FACTORY:
                            head = "Decode >     ";
                            bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                            break;
                        case BITMAP_FACTORY_OPTION:
                            head = "Decode 565 > ";
                            BitmapFactory.Options opt = new BitmapFactory.Options();
                            opt.inPreferredConfig = Bitmap.Config.RGB_565;
                            bmp = BitmapFactory.decodeFile(f.getAbsolutePath(), opt);
                            break;
                    }
                    if (bmp != null) {
                        t.tacF("%s bmp = %s x %s, %s", head, bmp.getWidth(), bmp.getHeight(), bmp);
                        Say.LogF("%s bmp = %s MB, file = %s, conf = %s", head, bmp.getByteCount() / 1024.0 / 1024.0, f.getName(), bmp.getConfig());
                        bmp.recycle();
                    } else {
                        t.tacF("%s bmp = null, failed : %s", head, f.getName());
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

            }
        });
    }


    private static void log(String msg) {
        Log.e("IMPAdapter", msg);
    }
    private static void logF(String format, Object... params) {
        Log.e("IMPAdapter", String.format(java.util.Locale.US, format, params));
    }
}
