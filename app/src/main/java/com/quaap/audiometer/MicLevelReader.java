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

    public double getMaxLevel() {
        switch (mLevelMethod) {
            case LogRMS:
                return Math.log(Short.MAX_VALUE * .707f);

            case SqrtRMS:
                return Math.sqrt(Short.MAX_VALUE * .707f);

            case RMS:
                return Short.MAX_VALUE * .707f;

            case Max:
            case Avg:
            default:
                return Short.MAX_VALUE;

        }

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
                if (read==0) {
                    continue;
                }
                int max = 0;
                int min = Short.MAX_VALUE;
                double avg = 0;
                double rmssum = 0;
                for (int i=0; i<read; i++) {
                    short dat = sData[i];
                    int abs = Math.abs(dat);
//                    if (abs>BASENOISE) {
//                        abs -= BASENOISE;
//                    } else {
//                        abs=0;
//                    }
                    rmssum += abs*abs;
                    avg += abs;
                    if (abs > max) max = abs;
                    if (abs < min) min = abs;
                    // System.out.println(dat);
                }

               // System.out.println(min + " - " + max);
                double rmsavg = Math.sqrt(rmssum/read);

                double resultval = 0;
                switch (mLevelMethod) {
                    case Max:
                        resultval = max; break;
                    case Avg:
                        resultval = avg/read; break;
                    case LogRMS:
                        resultval = Math.log(rmsavg-min); break;
                    case SqrtRMS:
                        resultval = Math.sqrt(rmsavg); break;
                    case RMS:
                        resultval = rmsavg; break;

                }

                mValueListener.valueCalculated(resultval);

            }
        } finally {
            recorder.stop();
            recorder.release();
        }
    }

    public enum LevelMethod {SqrtRMS, RMS, LogRMS, Max, Avg}

    public interface MicLevelReaderValueListener {

        void valueCalculated(double level);

    }
}
