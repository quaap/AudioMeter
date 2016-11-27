package com.quaap.audiometer;

/**
 *   Copyright 2016 Tom Kliethermes
 *
 *   This file is part of AudioMeter.
 *
 *   AudioMeter is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   AudioMeter is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with AudioMeter.  If not, see <http://www.gnu.org/licenses/>.
 */


import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;



public class MeterView extends LinearLayout {

    public MeterView(Context context) {
        super(context);
        setOrientation(VERTICAL);
        determineScreenSize(context);
    }

    public MeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        determineScreenSize(context);
    }

    public MeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
        determineScreenSize(context);
    }


    private void determineScreenSize(Context context) {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        screenHeight = size.y;
    }

    private TextView[] mMeterElements;

    private int mMeterBars = 0;

    private double[] mMeterTicks;

    private final float mAlphaInactive = .03f;
    private final float mAlphaActive = 1f;

    private int screenHeight;

    public void setupMeter(double[] meterTicks) {

        mMeterTicks = meterTicks;

        mMeterBars = meterTicks.length;

        mMeterElements = new TextView[mMeterBars];

        int fontsize = (int)(screenHeight/2.2/mMeterBars);

        removeAllViews();

        for (int i = 0; i < mMeterElements.length; i++) {

            LinearLayout wrapper = new LinearLayout(getContext());
            TextView numbers = new TextView(getContext());
            TextView balance = new TextView(getContext());
            mMeterElements[i] = new TextView(getContext());
            wrapper.addView(numbers);
            wrapper.addView(mMeterElements[i]);
            wrapper.addView(balance);

            addView(wrapper);
            addView(new Space(getContext()));

            int ind = mMeterBars - i - 1;

            numbers.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontsize);
            balance.setTextSize(TypedValue.COMPLEX_UNIT_PX, fontsize);
            numbers.setTypeface(Typeface.MONOSPACE);
            balance.setTypeface(Typeface.MONOSPACE);
            balance.setText(String.format(" %7s", ""));
            if (i%2==0) {
                String fmt = "%7.1f ";
                if (Math.abs(meterTicks[ind])>22) {
                    fmt = "%7.0f ";
                }
                numbers.setText(String.format(fmt, meterTicks[ind]));
            } else {
                numbers.setText(String.format(" %7s", ""));
            }


            mMeterElements[i].setTextSize(TypedValue.COMPLEX_UNIT_PX, fontsize);
            mMeterElements[i].setText("_________________________");
            mMeterElements[i].setAlpha(mAlphaInactive);

        }
        for (int i = 0; i < mMeterElements.length; i++) {
            int ind = mMeterBars - i - 1;
            int percent = i * 100 / mMeterBars;

            if (percent < 60) {
                mMeterElements[ind].setBackgroundColor(Color.GREEN);
            } else if (percent < 85) {
                mMeterElements[ind].setBackgroundColor(Color.YELLOW);
            } else {
                mMeterElements[ind].setBackgroundColor(Color.RED);
            }

        }

    }

    public void setMeterValue(double val) {
       // setMeterBars((int) (val / getMeterMax() * mMeterBars));
        if (val<mMeterTicks[0]) {
            setMeterBars(0);
        } else {
            //Log.d("m", "================\n" + val);
            for (int i = 1; i < mMeterTicks.length; i++) {
               // Log.d("m", val + " "  + mMeterTicks[i]);

                if (val <= mMeterTicks[i]) {
                    setMeterBars(i);
                    break;
                }
            }
        }
    }


    public void setMeterBars(int numBars) {

        for (int i = 0; i < mMeterElements.length; i++) {
            int ind = mMeterBars - i - 1;
            if (i < numBars) {
                mMeterElements[ind].setAlpha(mAlphaActive);
            } else {
                mMeterElements[ind].setAlpha(mAlphaInactive);
            }
        }
    }

    public double getMeterMax() {
        return mMeterTicks[mMeterTicks.length-1];
    }
    public double getMeterMin() {
        return mMeterTicks[0];
    }

}