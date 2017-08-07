package com.flyingkite.mysensors;

import android.graphics.Bitmap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.DiskCache;
import com.nostra13.universalimageloader.cache.memory.MemoryCache;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;

import java.io.File;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.ViewHolder> {

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        //Log.e("MyAdapter", "create type = " + viewType);
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.view_recycler_item, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        ImageLoader.getInstance().loadImage("drawable://" + R.drawable.ic_launcher_round, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {
                //Log.e("UIL", " Go #" + position);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                //Log.e("UIL", "Fail #" + position);
            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                //Log.e("UIL", " OK  #" + position + ", View = " + view + ", uri = " + imageUri);
                //((ImageView)view).setImageBitmap(loadedImage);

                MemoryCache cache = ImageLoader.getInstance().getMemoryCache();
                cache.put(imageUri, loadedImage);
                holder.img.setImageBitmap(loadedImage);
            }

            @Override
            public void onLoadingCancelled(String imageUri, View view) {
                //Log.e("UIL", "X__X #" + position);

                /*
                if (position == 1) {
                    MemoryCache cache = ImageLoader.getInstance().getMemoryCache();
                    Log.e("UIL", "keys = " + cache.keys());
                    for (String key : cache.keys()) {
                        Log.e("UIL", "key = " + key + ", bmp = " + cache.get(key));
                    }

                    DiskCache dk = ImageLoader.getInstance().getDiskCache();
                    Log.e("UIL", "File = " + dk.get(imageUri) + "\n uri = " + imageUri);
                }
                */

                MemoryCache cache = ImageLoader.getInstance().getMemoryCache();
                Bitmap bmp = cache.get(imageUri);
                //Log.e("UIL", "Mem cache = " + bmp);
                if (bmp != null && !bmp.isRecycled()) {
                    //Log.e("UIL", "Set it #" + position);
                    holder.img.setImageBitmap(bmp);
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
        //ImageLoader.getInstance().displayImage("drawable://" + R.drawable.ic_launcher_round, holder.img);
        holder.txt.setText(position + "");
        //Log.e("MyAdapter", "bind #" + position);
    }

    @Override
    public int getItemCount() {
        return 30;
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView img;
        private TextView txt;

        public ViewHolder(View itemView) {
            super(itemView);
            itemView.setOnClickListener((view) -> {
                Toast.makeText(view.getContext(), txt.getText(), Toast.LENGTH_SHORT).show();
            });
            img = (ImageView) itemView.findViewById(R.id.myImg);
            txt = (TextView) itemView.findViewById(R.id.myText);
        }
    }
}
