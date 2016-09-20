package com.sam_chordas.android.stockhawk.util;


public final class AccessibilityUtils {

    public static String getStringAsAudibleCharacters(String inputString) {
        return inputString.replace("", " ").trim();
    }

}
