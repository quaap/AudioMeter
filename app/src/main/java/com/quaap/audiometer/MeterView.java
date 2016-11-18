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
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;



public class MeterView extends LinearLayout {

    public MeterView(Context context) {
        super(context);
        setOrientation(VERTICAL);
    }

    public MeterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
    }

    public MeterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        setOrientation(VERTICAL);
    }

    private TextView[] mMeterElements;
    private double mMeterMax = 0;
    private int mMeterBars = 0;

    private final float mAlphaInactive = .03f;
    private final float mAlphaActive = 1f;

    public void setupMeter(double meterMax, int numBars) {

        this.mMeterMax = meterMax;
        this.mMeterBars = numBars;

        mMeterElements = new TextView[numBars];

        int fontsize = 9;

        for (int i = 0; i < mMeterElements.length; i++) {

            mMeterElements[i] = new TextView(getContext());

            addView(mMeterElements[i]);
            addView(new Space(getContext()));

            mMeterElements[i].setText("_________________________");
            mMeterElements[i].setTextSize(fontsize);
            mMeterElements[i].setAlpha(mAlphaInactive);

        }
        for (int i = 0; i < mMeterElements.length; i++) {
            int ind = mMeterBars - i - 1;
            int percent = i * 100 / mMeterBars;

            if (percent < 40) {
                mMeterElements[ind].setBackgroundColor(Color.GREEN);
            } else if (percent < 70) {
                mMeterElements[ind].setBackgroundColor(Color.YELLOW);
            } else {
                mMeterElements[ind].setBackgroundColor(Color.RED);
            }

        }

    }

    public void setMeterValue(double val) {
        setMeterBars((int) (val / mMeterMax * mMeterBars));
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
}