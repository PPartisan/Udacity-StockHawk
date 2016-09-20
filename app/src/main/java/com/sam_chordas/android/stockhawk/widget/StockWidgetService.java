package com.sam_chordas.android.stockhawk.widget;

import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViewsService;

public class StockWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new StockWidgetViewFactory(this.getApplicationContext());
    }

}
