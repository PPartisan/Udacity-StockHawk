package com.sam_chordas.android.stockhawk.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.ui.DetailStocksActivity;

public class StockWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {

        for (int id : appWidgetIds) {

            Intent serviceIntent = new Intent(context, StockWidgetService.class);
            serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id);

            RemoteViews widget = new RemoteViews(context.getPackageName(), R.layout.stock_widget);

            widget.setRemoteAdapter(R.id.stock_widget_list, serviceIntent);

            Intent clickIntent = new Intent(context, DetailStocksActivity.class);
            PendingIntent clickPendingIntent = PendingIntent.getActivity(
                    context, 0, clickIntent, 0
            );

            widget.setPendingIntentTemplate(R.id.stock_widget_list, clickPendingIntent);
            widget.setEmptyView(R.id.stock_widget_list, R.id.stock_widget_empty);

            appWidgetManager.updateAppWidget(id, widget);

        }
        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] widgetIds = manager.getAppWidgetIds(new ComponentName(context, getClass()));
        manager.notifyAppWidgetViewDataChanged(widgetIds, R.id.stock_widget_list);

    }

    public static void updateStockWidget(Context context) {

        Intent updateIntent = new Intent(context, StockWidgetProvider.class);
        updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        int[] widgetIds = { R.xml.stock_widget_provider };
        updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        context.sendBroadcast(updateIntent);

    }

}
