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

    private static final int BASENOISE=200;

    private boolean mIsRunning = false;
    private LevelMethod mLevelMethod = LevelMethod.RMS;

    private double scale = 1.0;

    private final MicLevelReaderValueListener mValueListener;

    public MicLevelReader(MicLevelReaderValueListener valueListener, LevelMethod levelMethod) {
        mValueListener = valueListener;
        mLevelMethod = levelMethod;
    }

    public boolean isRunning() {
        return mIsRunning;
    }

    public void stop() {
        mIsRunning = false;
    }

    public void setLevelMethod(LevelMethod levelMethod) {
        mLevelMethod = levelMethod;
    }

    public LevelMethod getLevelMethod() {
        return mLevelMethod;
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



            short sData[] = new short[RECORDER_BUFFER_SIZE];
            while (mIsRunning && recorder.getState()==AudioRecord.STATE_INITIALIZED) {

                int read = recorder.read(sData, 0, RECORDER_BUFFER_SIZE);
                if (read==0) {
                    continue;
                }
                double resultval = mLevelMethod.calculate(sData, read, scale);
                mValueListener.valueCalculated(resultval);

            }
        } finally {
            recorder.stop();
            recorder.release();
        }
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }


    public interface MicLevelReaderValueListener {

        void valueCalculated(double level);

    }
}
