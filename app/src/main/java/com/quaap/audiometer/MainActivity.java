package com.quaap.audiometer;

/**
 *   Copyright 2016 Tom Kliethermes
 *
 *   This file is part of AudioMeter.
 *
 *   GoalTender is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   GoalTender is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with AudioMeter.  If not, see <http://www.gnu.org/licenses/>.
 */


import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.view.Display;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;
import java.util.concurrent.atomic.AtomicInteger;



public class MainActivity extends AppCompatActivity implements MicLevelReader.MicLevelReaderValueListener {

    private Thread mRecorderThread = null;

    private double mScale = 1;
    private MeterView mMeterView;
    private AtomicInteger mMeterValue = new AtomicInteger();

    private MicLevelReader mMicLevelReader;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMeterView = (MeterView)findViewById(R.id.meterLayout);
        mMeterView.setupMeter(Short.MAX_VALUE, 21);

        mMicLevelReader = new MicLevelReader(this);

        final SeekBar scaleCtrl = (SeekBar)findViewById(R.id.scaleCtrl);

        setScale();

        scaleCtrl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setScale();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        final ToggleButton onoff = (ToggleButton)findViewById(R.id.toggleButton);

        onoff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onoff.isChecked()) {
                    startit();
                } else {
                    stopit();
                }
            }
        });
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        if (size.x>size.y) {
            ((LinearLayout)findViewById(R.id.activity_main)).setOrientation(LinearLayout.HORIZONTAL);
            ((LinearLayout)findViewById(R.id.meter_metalayout)).setMinimumWidth(size.x/2);
        }
    }


    private void setScale() {
        final TextView scaleVal = (TextView)findViewById(R.id.scaleVal);
        final SeekBar scaleCtrl = (SeekBar)findViewById(R.id.scaleCtrl);
        mScale = (scaleCtrl.getProgress()+.001)/(scaleCtrl.getMax()/2);
        scaleVal.setText(String.format("%1.1f", mScale));
    }


    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {

        if (mMicLevelReader.isRunning()) {
            final ToggleButton onoff = (ToggleButton) findViewById(R.id.toggleButton);
            onoff.setChecked(false);
            stopit();
        }

        super.onPause();
    }



    @Override
    public void valueCalculated(double level) {
        mMeterValue.set((int)(level * mScale));
        //System.out.println(rmsavg);

        mHandler.obtainMessage(1).sendToTarget();
    }


    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mMicLevelReader.isRunning()) {
                mMeterView.setMeterValue(mMeterValue.intValue());
            }
        }
    };


    public void startit() {
        mRecorderThread = new Thread(mMicLevelReader, "AudioListener Thread");
        mRecorderThread.start();
    }



    public void stopit() {
        if (mMicLevelReader.isRunning()) {
            mMicLevelReader.stop();
            try {
                mRecorderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        mMeterView.setMeterBars(0);

    }

    @Override
    protected void onStop() {
        stopit();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        stopit();

        super.onDestroy();
    }



}
