package com.quaap.audiometer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

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


public class MicLevelReader implements Runnable {
    private static final int RECORDER_SAMPLERATE = 16000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int RECORDER_BUFFER_SIZE = RECORDER_SAMPLERATE/40;

    private boolean mIsRunning = false;

    private final MicLevelReaderValueListener mValueListener;

    public MicLevelReader(MicLevelReaderValueListener valueListener) {
        mValueListener = valueListener;
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void stop() {
        mIsRunning = false;
    }

    @Override
    public void run() {

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
        AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,
                RECORDER_AUDIO_ENCODING, bufferSize);
        try {

            recorder.startRecording();
            mIsRunning = true;

            while (mIsRunning && recorder.getState()==AudioRecord.STATE_INITIALIZED) {
                short sData[] = new short[RECORDER_BUFFER_SIZE];

                int read = recorder.read(sData, 0, RECORDER_BUFFER_SIZE);

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
                mValueListener.valueCalculated(rmsavg/.7);

            }
        } finally {
            recorder.stop();
            recorder.release();
        }
    }

    public interface MicLevelReaderValueListener {

        void valueCalculated(double level);

    }
}
