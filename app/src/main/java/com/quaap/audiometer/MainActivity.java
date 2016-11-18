package com.quaap.audiometer;

import android.graphics.Color;
import android.graphics.Point;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Display;
import android.view.View;

import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends AppCompatActivity {
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;

    private Thread recordingThread = null;

    private boolean running = false;

    int BufferElements2Rec = 1024;

    double scale = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupMeter(Math.pow(2,15)+1, 20);

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



    private TextView[] meterElements;
    private double meterMax=0;
    private int meterBars=0;

    public void setupMeter(double meterMax, int numBars) {
        final LinearLayout meterLayout = (LinearLayout) findViewById(R.id.meterLayout);
        this.meterMax = meterMax;
        this.meterBars = numBars;

        meterElements = new TextView[numBars];

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int fontsize = (int)(size.y/4.5/meterBars);

        System.out.println(fontsize + " " + size.y);

        for (int i=0; i<meterElements.length; i++) {

            meterElements[i] = new TextView(meterLayout.getContext());

            meterLayout.addView(meterElements[i]);
            //meterElements[i].setVisibility(View.INVISIBLE);

            meterElements[i].setText("________________");
            meterElements[i].setTextSize(fontsize);
            meterElements[i].setAlpha(.05f);
            //meterElements[i].setPadding(i,0,i,0);

        }
        for (int i=0; i<meterElements.length; i++) {
            int ind = meterBars - i - 1;
            int percent = i * 100/meterBars;

            if (percent < 40) {
                meterElements[ind].setBackgroundColor(Color.GREEN);
            } else if (percent < 80) {
                meterElements[ind].setBackgroundColor(Color.YELLOW);
            } else {
                meterElements[ind].setBackgroundColor(Color.RED);
            }

        }

    }

    public void setMeterValue(double val) {
        setMeterBars((int)(val/meterMax * meterBars));
    }


    public void setMeterBars(int numBars) {
        final TextView numberTxt = (TextView) findViewById(R.id.numberTxt);
        //numberTxt.setText(numBars +" val");
        for (int i = 0; i < meterElements.length; i++) {
            int ind = meterBars-i - 1;
            if (i<numBars) {
//                int percent = i * 100/meterBars;
//                numberTxt.setText(numBars+"");
//                if (percent < 40) {
//                    meterElements[ind].setBackgroundColor(Color.GREEN);
//                } else if (percent < 80) {
//                    meterElements[ind].setBackgroundColor(Color.YELLOW);
//                } else {
//                    meterElements[ind].setBackgroundColor(Color.RED);
//                }
                meterElements[ind].setAlpha(1f);
            } else {
                meterElements[ind].setAlpha(.05f);
               // meterElements[ind].setBackgroundColor(Color.LTGRAY);
            }
        }
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

                double rmssum = 0;
                for (int i=0; i<read; i++) {
                    short dat = sData[i];
                    rmssum += dat*dat;
                   // System.out.println(dat);
                }
                double rmsavg = Math.sqrt(rmssum/sData.length);

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
                setMeterValue(latestAvg.intValue());
            }
        }
    };

    TimerTask timerTask;
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
        setMeterBars(0);

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
