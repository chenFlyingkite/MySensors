package com.flyingkite.mysensors;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.flyingkite.utils.Say;

public class MyWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        super.onUpdate(context, appWidgetManager, appWidgetIds);
        int n = appWidgetIds.length;

        Say.LogF("onUpdate %s items", n);
        for (int i = 0; i < n; i++) {
            int id = appWidgetIds[i];

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendIntent = PendingIntent.getActivity(context, 0, intent, 0);

            RemoteViews v = new RemoteViews(context.getPackageName(), R.layout.view_widget);
            v.setTextViewText(R.id.itsText, "#" + i);
            v.setOnClickPendingIntent(R.id.itsText, pendIntent);

            appWidgetManager.updateAppWidget(id, v);
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        int n = appWidgetIds.length;
        Say.LogF("onDeleted %s items", n);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        Say.LogF("onEnabled");
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Say.LogF("onDisabled");
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Say.LogF("onReceive");
    }

    @Override
    public void onRestored(Context context, int[] oldWidgetIds, int[] newWidgetIds) {
        super.onRestored(context, oldWidgetIds, newWidgetIds);
        int n = newWidgetIds.length;
        Say.LogF("onRestored %s items", n);
    }

}
