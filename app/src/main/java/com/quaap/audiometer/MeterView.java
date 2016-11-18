package com.quaap.audiometer;


import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;

/**
 * Created by tom on 11/17/16.
 */

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

    private TextView[] meterElements;
    private double meterMax = 0;
    private int meterBars = 0;

    private final float alphaInactive = .03f;
    private final float alphaActive = 1f;

    public void setupMeter(double meterMax, int numBars) {

        this.meterMax = meterMax;
        this.meterBars = numBars;

        meterElements = new TextView[numBars];

        int fontsize = 9;

        for (int i = 0; i < meterElements.length; i++) {

            meterElements[i] = new TextView(getContext());

            addView(meterElements[i]);
            addView(new Space(getContext()));

            meterElements[i].setText("_________________________");
            meterElements[i].setTextSize(fontsize);
            meterElements[i].setAlpha(alphaInactive);

        }
        for (int i = 0; i < meterElements.length; i++) {
            int ind = meterBars - i - 1;
            int percent = i * 100 / meterBars;

            if (percent < 40) {
                meterElements[ind].setBackgroundColor(Color.GREEN);
            } else if (percent < 70) {
                meterElements[ind].setBackgroundColor(Color.YELLOW);
            } else {
                meterElements[ind].setBackgroundColor(Color.RED);
            }

        }

    }

    public void setMeterValue(double val) {
        setMeterBars((int) (val / meterMax * meterBars));
    }


    public void setMeterBars(int numBars) {

        for (int i = 0; i < meterElements.length; i++) {
            int ind = meterBars - i - 1;
            if (i < numBars) {
                meterElements[ind].setAlpha(alphaActive);
            } else {
                meterElements[ind].setAlpha(alphaInactive);
            }
        }
    }
}