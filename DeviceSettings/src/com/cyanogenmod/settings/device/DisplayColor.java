/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.settings.device;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Button;
import java.io.IOException;

/**
 * Special preference type that allows configuration of Color settings
 */
public class DisplayColor extends DialogPreference {
    private static final String TAG = "ColorCalibration";

    private static final String[] FILE_PATH = new String[] {
            "/sys/class/misc/samoled_color/red_multiplier",
            "/sys/class/misc/samoled_color/green_multiplier",
            "/sys/class/misc/samoled_color/blue_multiplier"
    };

    public static boolean isSupported() {
        for (String filePath : FILE_PATH) {
            if (!Utils.fileExists(filePath)) {
                return false;
            }
        }
        return true;
    }

    public static int getDefValue()  {
        return 100000;
    }

    public static int getMaxValue()  {
        return 200000;
    }

    public static int getMinValue()  {
        return 10000;
    }

    public static String getCurColors()  {
        StringBuilder values = new StringBuilder();
        for (String filePath : FILE_PATH) {
            values.append(Utils.readValue(filePath)).append(" ");
        }
        return values.toString();
    }

    public static void setColors(String colors)  {
        String[] valuesSplit = colors.split(" ");
        for (int i = 0; i < valuesSplit.length; i++) {
            String targetFile = FILE_PATH[i];
            Utils.writeValue(targetFile, valuesSplit[i]);
        }
    }

    // These arrays must all match in length and order
    private static final int[] SEEKBAR_ID = new int[] {
        R.id.color_red_seekbar,
        R.id.color_green_seekbar,
        R.id.color_blue_seekbar
    };

    private static final int[] SEEKBAR_VALUE_ID = new int[] {
        R.id.color_red_value,
        R.id.color_green_value,
        R.id.color_blue_value
    };

    private ColorSeekBar[] mSeekBars = new ColorSeekBar[SEEKBAR_ID.length];
    private String[] mCurrentColors;
    private String mOriginalColors;

    public DisplayColor(Context context, AttributeSet attrs) {
        super(context, attrs);

        if (!isSupported()) {
            return;
        }

        setDialogLayoutResource(R.layout.display_color_calibration);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.reset_button,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mOriginalColors = getCurColors();
        mCurrentColors = mOriginalColors.split(" ");

        for (int i = 0; i < SEEKBAR_ID.length; i++) {
            SeekBar seekBar = (SeekBar) view.findViewById(SEEKBAR_ID[i]);
            TextView value = (TextView) view.findViewById(SEEKBAR_VALUE_ID[i]);
            mSeekBars[i] = new ColorSeekBar(seekBar, value, i);
            mSeekBars[i].setValueFromString(mCurrentColors[i]);
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // Can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        AlertDialog d = (AlertDialog) getDialog();
        Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
        defaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int defaultValue = getDefValue();
                for (int i = 0; i < mSeekBars.length; i++) {
                    mSeekBars[i].mSeekBar.setProgress(defaultValue);
                    mCurrentColors[i] = String.valueOf(defaultValue);
                }
                setColors(TextUtils.join(" ", mCurrentColors));
            }
        });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            Editor editor = getEditor();
            editor.putString("display_color_calibration", getCurColors());
            editor.commit();
        } else if (mOriginalColors != null) {
            setColors(mOriginalColors);
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        if (getDialog() == null || !getDialog().isShowing()) {
            return superState;
        }

        // Save the dialog state
        final SavedState myState = new SavedState(superState);
        myState.currentColors = mCurrentColors;
        myState.originalColors = mOriginalColors;

        // Restore the old state when the activity or dialog is being paused
        setColors(mOriginalColors);
        mOriginalColors = null;

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state == null || !state.getClass().equals(SavedState.class)) {
            // Didn't save state for us in onSaveInstanceState
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;
        super.onRestoreInstanceState(myState.getSuperState());
        mOriginalColors = myState.originalColors;
        mCurrentColors = myState.currentColors;
        for (int i = 0; i < mSeekBars.length; i++) {
            mSeekBars[i].setValueFromString(mCurrentColors[i]);
        }
        setColors(TextUtils.join(" ", mCurrentColors));
    }

    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String value = prefs.getString("display_color_calibration", null);

        if (value != null) {
            setColors(value);
        }
    }

    private static class SavedState extends BaseSavedState {
        String originalColors;
        String[] currentColors;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel source) {
            super(source);
            originalColors = source.readString();
            currentColors = source.createStringArray();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeString(originalColors);
            dest.writeStringArray(currentColors);
        }

        public static final Parcelable.Creator<SavedState> CREATOR =
                new Parcelable.Creator<SavedState>() {

            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    private class ColorSeekBar implements SeekBar.OnSeekBarChangeListener {
        private int mIndex;
        private SeekBar mSeekBar;
        private TextView mValue;

        public ColorSeekBar(SeekBar seekBar, TextView value, int index) {
            mSeekBar = seekBar;
            mValue = value;
            mIndex = index;

            mSeekBar.setMax(getMaxValue() -
                    getMinValue());
            mSeekBar.setOnSeekBarChangeListener(this);
        }

        public void setValueFromString(String valueString) {
            mSeekBar.setProgress(Integer.valueOf(valueString));
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int min = getMinValue();
            int max = getMaxValue();

            if (fromUser) {
                mCurrentColors[mIndex] = String.valueOf(progress + min);
                setColors(TextUtils.join(" ", mCurrentColors));
            }

            int percent = Math.round(100F * progress / (max - min));
            mValue.setText(String.format("%d%%", percent));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            // Do nothing here
        }
    }

}
