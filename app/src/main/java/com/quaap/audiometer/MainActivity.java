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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
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

public class MainActivity extends AppCompatActivity {
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private Thread recordingThread = null;

    private boolean running = false;

    private final int BufferElements2Rec = RECORDER_SAMPLERATE/40;

    private double scale = 1;
    private MeterView meterView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        meterView = (MeterView)findViewById(R.id.meterLayout);

        meterView.setupMeter(Short.MAX_VALUE, 21);

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
        scale = (scaleCtrl.getProgress()+.001)/(scaleCtrl.getMax()/2);
        scaleVal.setText(String.format("%1.1f", scale));
    }


    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {

        if (running) {
            final ToggleButton onoff = (ToggleButton) findViewById(R.id.toggleButton);
            onoff.setChecked(false);
            stopit();
        }

        super.onPause();
    }




    AtomicInteger latestAvg = new AtomicInteger();


    public void sampleMic() {
        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);
        try {

            recorder.startRecording();
            running = true;

            while (running && recorder.getState()==AudioRecord.STATE_INITIALIZED) {
                short sData[] = new short[BufferElements2Rec];

                int read = recorder.read(sData, 0, BufferElements2Rec);

                int max = 0;
                int avg = 0;
                double rmssum = 0;
                for (int i=0; i<read; i++) {
                    short dat = sData[i];
                    rmssum += dat*dat;
                    avg += dat;
                    if (Math.abs(dat) > max) max = Math.abs(dat);
                   // System.out.println(dat);
                }
                double rmsavg = Math.sqrt(rmssum/read);

                //RMS max is about .7 of raw max.
                latestAvg.set((int)(rmsavg/.7 * scale));
                //System.out.println(rmsavg);

                mHandler.obtainMessage(1).sendToTarget();

            }
        } finally {
            recorder.stop();
            recorder.release();
        }
    }

    public Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (running) {
                meterView.setMeterValue(latestAvg.intValue());
            }
        }
    };


    public void startit() {

        recordingThread = new Thread(new Runnable() {
            public void run() {
                sampleMic();
            }
        }, "AudioListener Thread");

        recordingThread.start();

        final TextView numberTxt = (TextView)findViewById(R.id.numberTxt);


    }



    public void stopit() {
        if (running) {
            running = false;
            try {
                recordingThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        meterView.setMeterBars(0);

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
