package com.sam_chordas.android.stockhawk.util;

import android.content.res.AssetManager;
import android.graphics.Typeface;

public final class TypefaceUtils {

    private TypefaceUtils() { throw new AssertionError(); }

    public static Typeface getRobotoLightTypeface(AssetManager manager) {
        return Typeface.createFromAsset(manager, "fonts/Roboto-Light.ttf");
    }

}
