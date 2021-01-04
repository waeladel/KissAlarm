package io.github.carlorodriguez.alarmon.Utils;

import android.text.InputFilter;

import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;

public class StringUtils {

    private final static String TAG = StringUtils.class.getSimpleName();

    public static String getFirstWord(String text) {
        String[] arrSplitName = text.split("\\s"); // split after every space
        return arrSplitName[0];
    }

    public static void setMaxLength(TextInputEditText textView, int length) {
        InputFilter[] inputFilters = textView.getFilters();
        ArrayList<InputFilter> inputFilterArray = new ArrayList<InputFilter>();

        if (inputFilters != null) {
            for (int i = 0; i < inputFilters.length; i++) {
                InputFilter inputFilter = inputFilters[i];

                if (!(inputFilter instanceof InputFilter.LengthFilter))
                    inputFilterArray.add(inputFilter);
            }

        }
        inputFilterArray.add(new InputFilter.LengthFilter(length));
        textView.setFilters(inputFilterArray.toArray(new InputFilter[0]));
    }

}
