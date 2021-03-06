package com.sam_chordas.android.stockhawk.service;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import com.google.android.gms.gcm.TaskParams;

/**
 * Created by sam_chordas on 10/1/15.
 */
public class StockIntentService extends IntentService {

    public static final String KEY = "stock_intent_service_tag";
    public static final String INIT = "init";
    public static final String ADD = "add";
    public static final String SYMBOL = "symbol";
    public static final String PERIODIC = "periodic";

    public StockIntentService(){
        super(StockIntentService.class.getName());
    }

    public StockIntentService(String name) {
        super(name);
    }

    @Override protected void onHandleIntent(Intent intent) {

        StockTaskService stockTaskService = new StockTaskService(this);
        Bundle args = new Bundle();

        if (intent.getStringExtra(KEY).equals(ADD)){
            args.putString(SYMBOL, intent.getStringExtra(SYMBOL));
        }

        // We can call OnRunTask from the intent service to force it to run immediately instead of
        // scheduling a task.

        stockTaskService.onRunTask(new TaskParams(intent.getStringExtra(KEY), args));

    }
}
