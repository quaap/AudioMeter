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


import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
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

        mMicLevelReader = new MicLevelReader(this, MicLevelReader.LevelMethod.SqrtRMS);

        mMeterView.setupMeter(mMicLevelReader.getMaxLevel(), 20);

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
                    if (!startit()) {
                        onoff.setChecked(false);
                    }
                } else {
                    stopit();
                }
            }
        });


        final SharedPreferences pref = getApplicationContext().getSharedPreferences("main", MODE_PRIVATE);
        MicLevelReader.LevelMethod levM = MicLevelReader.LevelMethod.valueOf(pref.getString("levelMethod", MicLevelReader.LevelMethod.SqrtRMS.toString()));

        final Spinner levelType = (Spinner)findViewById(R.id.levelType);
        ArrayAdapter<MicLevelReader.LevelMethod> levelTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, MicLevelReader.LevelMethod.values());
        levelType.setAdapter(levelTypeAdapter);


        levelType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                MicLevelReader.LevelMethod lmeth = (MicLevelReader.LevelMethod)adapterView.getSelectedItem();
                levelMethodChanged(lmeth);
                SharedPreferences.Editor editor = pref.edit();
                editor.putString("levelMethod", lmeth.toString());
                editor.commit();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        levelType.setSelection(levelTypeAdapter.getPosition(levM));

        levelMethodChanged((MicLevelReader.LevelMethod)levelType.getSelectedItem());
        checkMicrophoneAccess();
    }


    private boolean checkMicrophoneAccess() {
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    REQUEST_RECORD_AUDIO);
            return false;
        }
        return true;
    }

    private static final int REQUEST_RECORD_AUDIO = 121;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_RECORD_AUDIO: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(this, "Yay!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "This application will not function without access to the microphone", Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void levelMethodChanged(MicLevelReader.LevelMethod levelMethod) {
        mMicLevelReader.setLevelMethod(levelMethod);
        mMeterView.setmMeterMax(mMicLevelReader.getMaxLevel());
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
        System.out.println(level);

        mHandler.obtainMessage(1).sendToTarget();
    }


    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mMicLevelReader.isRunning()) {
                mMeterView.setMeterValue(mMeterValue.intValue());
            }
        }
    };


    public boolean startit() {
        if (checkMicrophoneAccess()) {
            mRecorderThread = new Thread(mMicLevelReader, "AudioListener Thread");
            mRecorderThread.start();
            Window w = this.getWindow();
            w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            return true;
        }
        return false;
    }



    public void stopit() {
        if (mMicLevelReader.isRunning()) {
            mMicLevelReader.stop();
            Window w = this.getWindow();
            w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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
