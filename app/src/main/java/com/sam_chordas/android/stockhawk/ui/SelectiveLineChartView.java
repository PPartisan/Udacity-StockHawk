package com.sam_chordas.android.stockhawk.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;

import com.db.chart.model.ChartSet;
import com.db.chart.model.LineSet;
import com.db.chart.model.Point;
import com.db.chart.view.LineChartView;
import com.sam_chordas.android.stockhawk.R;

import java.util.ArrayList;


public final class SelectiveLineChartView extends LineChartView {

    public static final int DESELECT_POINT = -1;

    private int selectedEntry = DESELECT_POINT;
    private Paint mSelectedPaint;

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

        if (selectedEntry != DESELECT_POINT) {
            LineSet lineSet = (LineSet) data.get(0);
            Point dot = (Point) lineSet.getEntry(selectedEntry);

            canvas.drawCircle(dot.getX(), dot.getY(), dot.getRadius(), mSelectedPaint);
        }

    }
}
