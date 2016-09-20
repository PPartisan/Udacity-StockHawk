package com.sam_chordas.android.stockhawk.ui;

import android.app.LoaderManager;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.data.QuoteColumns;
import com.sam_chordas.android.stockhawk.data.QuoteProvider;
import com.sam_chordas.android.stockhawk.rest.QuoteCursorAdapter;
import com.sam_chordas.android.stockhawk.rest.Utils;
import com.sam_chordas.android.stockhawk.service.DetailedStockTaskIntentService;
import com.sam_chordas.android.stockhawk.service.StockIntentService;
import com.sam_chordas.android.stockhawk.service.StockTaskService;
import com.google.android.gms.gcm.GcmNetworkManager;
import com.google.android.gms.gcm.PeriodicTask;
import com.google.android.gms.gcm.Task;
import com.melnykov.fab.FloatingActionButton;
import com.sam_chordas.android.stockhawk.touch_helper.SimpleItemTouchHelperCallback;
import com.sam_chordas.android.stockhawk.util.NetworkUtils;
import com.sam_chordas.android.stockhawk.widget.StockWidgetProvider;

public class MyStocksActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor>, QuoteCursorAdapter.Callbacks{

    private static final String RECYCLERVIEW_STATE_KEY = "recycler_view_state_key";

    private static final int CURSOR_LOADER_ID = 0;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;
    private QuoteCursorAdapter mCursorAdapter;
    boolean isConnected;

    private Cursor mCursor = null;

    private RecyclerView.LayoutManager mLayoutManager;
    private Parcelable mListState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isConnected = NetworkUtils.isConnectedToNetwork(this);

        setContentView(R.layout.activity_my_stocks);

        // The intent service is for executing immediate pulls from the Yahoo API
        // GCMTaskService can only schedule tasks, they cannot execute immediately
        Intent mServiceIntent = new Intent(this, StockIntentService.class);

        if (savedInstanceState == null){
            // Run the initialize task service so that some stocks appear upon an empty database
            mServiceIntent.putExtra(StockIntentService.KEY, StockIntentService.INIT);
            if (isConnected){
                startService(mServiceIntent);
            } else{
                NetworkUtils.buildNetworkFailureToast(this).show();
            }
        }

        EmptyRecyclerView recyclerView = (EmptyRecyclerView) findViewById(R.id.recycler_view);
        mLayoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setEmptyView(findViewById(R.id.ams_empty_view));
        getLoaderManager().initLoader(CURSOR_LOADER_ID, null, this);

        mCursorAdapter = new QuoteCursorAdapter(this, null);
        recyclerView.setAdapter(mCursorAdapter);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.attachToRecyclerView(recyclerView);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {

                if (isConnected) {
                    NetworkUtils.buildAddStockSymbolDialog(MyStocksActivity.this).show();
                } else {
                    NetworkUtils.buildNetworkFailureToast(MyStocksActivity.this).show();
                }

            }
        });

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mCursorAdapter);
        ItemTouchHelper mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(recyclerView);

        mTitle = getTitle();
        if (isConnected){
            final long period = 3600L;
            final long flex = 10L;
            final String periodicTag = "periodic";

            // create a periodic task to pull stocks once every hour after the app has been opened. This
            // is so Widget data stays up to date.
            PeriodicTask periodicTask = buildPeriodicTask(period, flex, periodicTag);
            // Schedule task with tag "periodic." This ensure that only the stocks present in the DB
            // are updated.
            GcmNetworkManager.getInstance(this).schedule(periodicTask);
        }

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mListState = mLayoutManager.onSaveInstanceState();
        outState.putParcelable(RECYCLERVIEW_STATE_KEY, mListState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState != null) {
            mListState = savedInstanceState.getParcelable(RECYCLERVIEW_STATE_KEY);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getLoaderManager().restartLoader(CURSOR_LOADER_ID, null, this);
        if(mListState != null) mLayoutManager.onRestoreInstanceState(mListState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
    }

    public void restoreActionBar() {

        if(getSupportActionBar() == null) {
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.my_stocks, menu);
        restoreActionBar();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        final int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                break;
            case R.id.action_change_units:
                // this is for changing stock changes from percent value to dollar value
                Utils.showPercent = !Utils.showPercent;
                getContentResolver().notifyChange(QuoteProvider.Quotes.CONTENT_URI, null);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args){
        // This narrows the return to only the stocks that are most current.

        final String[] projection = {
                QuoteColumns._ID,
                QuoteColumns.SYMBOL,
                QuoteColumns.BIDPRICE,
                QuoteColumns.PERCENT_CHANGE,
                QuoteColumns.CHANGE,
                QuoteColumns.IS_UP
        };

        final String where = QuoteColumns.IS_CURRENT + "=?";
        final String[] whereArgs = { "1" };

        return new CursorLoader(
                this,
                QuoteProvider.Quotes.CONTENT_URI,
                projection,
                where,
                whereArgs,
                null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data){
        mCursorAdapter.swapCursor(data);
        StockWidgetProvider.updateStockWidget(this);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader){
        mCursorAdapter.swapCursor(null);
    }

    private PeriodicTask buildPeriodicTask(long period, long flex, String periodicTag) {

        return new PeriodicTask.Builder()
                .setService(StockTaskService.class)
                .setPeriod(period)
                .setFlex(flex)
                .setTag(periodicTag)
                .setRequiredNetwork(Task.NETWORK_STATE_CONNECTED)
                .setRequiresCharging(false)
                .build();
    }

    @Override
    public void onItemClick(int position) {

        mCursor = mCursorAdapter.getCursor();

        mCursor.moveToPosition(position);

        final String symbol = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.SYMBOL));
        final String bid = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.BIDPRICE));
        final String percentChange = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.PERCENT_CHANGE));
        final String change = mCursor.getString(mCursor.getColumnIndex(QuoteColumns.CHANGE));
        final boolean isUp = (mCursor.getInt(mCursor.getColumnIndex(QuoteColumns.IS_UP)) == 1);

        Intent launchDetailActivity = new Intent(this, DetailStocksActivity.class);
        launchDetailActivity.putExtra(DetailedStockTaskIntentService.KEY_SYMBOL, symbol);
        launchDetailActivity.putExtra(DetailedStockTaskIntentService.KEY_BID, bid);
        launchDetailActivity.putExtra(DetailedStockTaskIntentService.KEY_PERCENT_CHANGE, percentChange);
        launchDetailActivity.putExtra(DetailedStockTaskIntentService.KEY_CHANGE, change);
        launchDetailActivity.putExtra(DetailedStockTaskIntentService.KEY_IS_UP, isUp);

        startActivity(launchDetailActivity);

    }

}
