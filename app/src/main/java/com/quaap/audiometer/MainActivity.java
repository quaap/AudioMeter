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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;


import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.ViewSwitcher;


public class MainActivity extends AppCompatActivity implements MicLevelReader.MicLevelReaderValueListener {

    private Thread mRecorderThread = null;

    private MeterView mMeterView;
    private double mMeterValue = 0;

    private MicLevelReader mMicLevelReader;

    private static final int NUMBARS = 20;
    private SharedPreferences pref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pref = getApplicationContext().getSharedPreferences("main", MODE_PRIVATE);
        mMeterView = (MeterView)findViewById(R.id.meterLayout);

        mMicLevelReader = new MicLevelReader(this, LevelMethod.dBFS);

        ImageButton show_ctrls = (ImageButton)findViewById(R.id.show_ctrls);
        show_ctrls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewSwitcher ctrls = (ViewSwitcher)findViewById(R.id.ctrls);
                ctrls.showNext();
            }
        });
        ImageButton hide_ctrls = (ImageButton)findViewById(R.id.hide_ctrls);
        hide_ctrls.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ViewSwitcher ctrls = (ViewSwitcher)findViewById(R.id.ctrls);
                ctrls.showPrevious();
            }
        });

        final SeekBar scaleCtrl = (SeekBar)findViewById(R.id.scaleCtrl);

        scaleCtrl.setProgress(pref.getInt("scaleCtrl", scaleCtrl.getProgress()));

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


        LevelMethod levM = LevelMethod.valueOf(pref.getString("levelMethod", LevelMethod.dBFS.toString()));

        final Spinner levelType = (Spinner)findViewById(R.id.levelType);
        ArrayAdapter<LevelMethod> levelTypeAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, LevelMethod.values());
        levelType.setAdapter(levelTypeAdapter);


        levelType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                LevelMethod lmeth = (LevelMethod)adapterView.getSelectedItem();
                levelMethodChanged(lmeth);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        levelType.setSelection(levelTypeAdapter.getPosition(levM));

        levelMethodChanged((LevelMethod)levelType.getSelectedItem());
        checkMicrophoneAccess();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.menu_options) {
            ViewSwitcher ctrls = (ViewSwitcher)findViewById(R.id.ctrls);
            ctrls.showNext();
        } else if (id==R.id.menu_about) {
            Intent intent = new Intent(this, AboutActivity.class);
            startActivity(intent);
        } else if (id==R.id.menu_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
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

                    Toast.makeText(this, R.string.got_microphone, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, R.string.need_microphone, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void setUnits()  {
        TextView units = (TextView)findViewById(R.id.units);
        String str = mMicLevelReader.getLevelMethod().toString();
        if (mMicLevelReader.getScale()!=1) {
            str += " x " + String.format("%1.1f", mMicLevelReader.getScale());
        }
        units.setText(str);
    }
    private void levelMethodChanged(LevelMethod levelMethod) {
        mMicLevelReader.setLevelMethod(levelMethod);
        mMeterView.setupMeter(levelMethod.getTicks(NUMBARS));
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("levelMethod", levelMethod.toString());
        editor.apply();

        setUnits();

//        double [] ticks = levelMethod.getTicks(NUMBARS);
//        for (int i=0; i<ticks.length; i++) {
//            Log.d("ticks", i + " " + ticks[i]);
//        }
    }

    private void setScale() {
        final TextView scaleVal = (TextView)findViewById(R.id.scaleVal);
        final SeekBar scaleCtrl = (SeekBar)findViewById(R.id.scaleCtrl);
        mMicLevelReader.setScale(((double)scaleCtrl.getProgress())/(scaleCtrl.getMax()/2));
        scaleVal.setText(String.format("%1.1f", mMicLevelReader.getScale()));

        SharedPreferences.Editor editor = pref.edit();
        editor.putInt("scaleCtrl", scaleCtrl.getProgress());
        editor.apply();

        setUnits();
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
        mMeterValue = level;

        mHandler.obtainMessage(1).sendToTarget();
    }


    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (mMicLevelReader.isRunning()) {
                mMeterView.setMeterValue(mMeterValue);
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
