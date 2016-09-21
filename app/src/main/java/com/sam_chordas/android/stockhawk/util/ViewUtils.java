package com.sam_chordas.android.stockhawk.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Toast;

public final class ViewUtils {

    private ViewUtils() { throw new AssertionError(); }

    @SuppressWarnings("deprecation")
    public static void setViewBackgroundDrawable(View view, int resId) {

        Drawable drawable = ContextCompat.getDrawable(view.getContext(), resId);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            view.setBackground(drawable);
        } else {
            view.setBackgroundDrawable(drawable);
        }

    }

    public static int getDpAsPx(int dp) {
        return (int) (dp * Resources.getSystem().getDisplayMetrics().density);
    }

    static Toast makeLongToast(Context context, int messageId) {
        return Toast.makeText(context, messageId, Toast.LENGTH_LONG);
    }

}
