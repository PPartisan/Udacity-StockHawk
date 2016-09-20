package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import com.db.chart.model.ChartSet;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;
import com.sam_chordas.android.stockhawk.util.TypefaceUtils;
import com.sam_chordas.android.stockhawk.util.ViewUtils;

import java.util.ArrayList;


public final class SelectiveLineChartView extends LineChartView {

    public static final int DESELECT_POINT = -1;
    public static final int NO_DATA = Integer.MIN_VALUE;
    public static final int LOADING = NO_DATA + 1;

    private int selectedEntry = LOADING;
    private Paint mSelectedPaint, mTextPaint;

    private String mNoDataString, mLoadingString;

    public SelectiveLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SelectiveLineChartView(Context context) {
        super(context);
        init();
    }

    private void init() {
        mSelectedPaint = new Paint();
        mSelectedPaint.setColor(ContextCompat.getColor(getContext(), R.color.material_red_700));
        mSelectedPaint.setStyle(Paint.Style.FILL);
        mSelectedPaint.setAntiAlias(true);

        mTextPaint = new Paint();
        mTextPaint.setTypeface(TypefaceUtils.getRobotoLightTypeface(getContext().getAssets()));
        mTextPaint.setTextSize(ViewUtils.getDpAsPx(24));
        mTextPaint.setTextAlign(Paint.Align.CENTER);
        mTextPaint.setColor(Color.WHITE);
        mTextPaint.setAntiAlias(true);

        mNoDataString = getContext().getString(R.string.alg_line_chart_no_data);
        mLoadingString = getContext().getString(R.string.alg_line_chart_loading);
    }

    public void setSelectedEntry(int selectedEntry) {
        this.selectedEntry = selectedEntry;
        invalidate();
    }

    public int getSelectedEntry() {
        return selectedEntry;
    }

    @Override
    public void onDrawChart(Canvas canvas, ArrayList<ChartSet> data) {
        super.onDrawChart(canvas, data);

        if (selectedEntry != NO_DATA && selectedEntry != DESELECT_POINT) {
            LineSet lineSet = (LineSet) data.get(0);
            Point dot = (Point) lineSet.getEntry(selectedEntry);

            canvas.drawCircle(dot.getX(), dot.getY(), dot.getRadius(), mSelectedPaint);
        }

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (selectedEntry == NO_DATA){
            updateCanvasText(canvas, mNoDataString);
        } else if (selectedEntry == LOADING) {
            updateCanvasText(canvas, mLoadingString);
        }

    }

    private void updateCanvasText(Canvas canvas, String message) {
        final int y = (int) ((canvas.getHeight() / 2) - ((mTextPaint.descent() + mTextPaint.ascent()) / 2));
        final int x = canvas.getWidth()/2;
        canvas.drawText(message, x, y, mTextPaint);
    }

}
