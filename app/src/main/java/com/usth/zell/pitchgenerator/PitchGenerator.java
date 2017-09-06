package com.usth.zell.pitchgenerator;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;

import Cepstrum.Cepstrum;

/**
 * Created by Zell on 5/21/16.
 */
public class PitchGenerator implements Runnable {

    public final String LOG_TAG = "Encoder";
    MediaCodec codec;
    MediaExtractor extractor;

    MediaFormat format;
    ByteBuffer[] codecInputBuffers;
    ByteBuffer[] codecOutputBuffers;
    MediaCodec.BufferInfo info;
    Resources mResources;
    Context mContext;

    ArrayList<Integer> amplitudes;

    int mFile;

    public PitchGenerator(Context mContext, int file) {
        this.mContext = mContext;
        this.mFile = file;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        amplitudes = this.toAmplitude(mFile);
        long end = System.currentTimeMillis();
        Log.d("===Running time===", String.valueOf(end-start));
    }

    public ArrayList<Integer> toAmplitude(int file){
        ArrayList<Integer> iAmplitude = new ArrayList<Integer>();

        mResources = mContext.getResources();
        AssetFileDescriptor testFd = mResources.openRawResourceFd(file);
        extractor = new MediaExtractor();

        try {
            extractor.setDataSource(testFd.getFileDescriptor(), testFd.getStartOffset(),
                    testFd.getLength());
            testFd.close();
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(LOG_TAG, "no connection");
        }
        format = extractor.getTrackFormat(0);
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 352800);
        String mime = format.getString(MediaFormat.KEY_MIME);

        try {
            codec = MediaCodec.createDecoderByType(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }
        codec.configure(format, null, null, 0);
        codec.start();

        codecInputBuffers = codec.getInputBuffers();
        codecOutputBuffers = codec.getOutputBuffers();

        extractor.selectTrack(0);

        info = new MediaCodec.BufferInfo();

        boolean isEOS = false;

        while (true) {
            if (!isEOS) {
                int inIndex = codec.dequeueInputBuffer(100);
                if (inIndex >= 0) {
                    ByteBuffer buffer = codecInputBuffers[inIndex];
                    int sampleSize = extractor.readSampleData(buffer, 0);
                    if (sampleSize < 0) {
                        // We shouldn't stop the playback at this point, just pass the EOS
                        // flag to decoder, we will get it again from the
                        // dequeueOutputBuffer
                        //Log.d("DecodeActivity", "InputBuffer BUFFER_FLAG_END_OF_STREAM");
                        codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        isEOS = true;
                    } else {
                        codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }

            int outIndex = codec.dequeueOutputBuffer(info, 100);
            switch (outIndex) {
                case MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED:
                    //Log.d("DecodeActivity", "INFO_OUTPUT_BUFFERS_CHANGED");
                    codecOutputBuffers = codec.getOutputBuffers();
                    break;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    //Log.d("DecodeActivity", "New format " + codec.getOutputFormat());
                    break;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                    //Log.d("DecodeActivity", "dequeueOutputBuffer timed out!");
                    break;
                default:
                    ByteBuffer buffer = codecOutputBuffers[outIndex];

                    // Obtain PCM byte array
                    int[] is = new int[(info.size-info.offset)/2];
                    int a = buffer.position();
                    for (int i = 0; i < is.length; i++) {
                        short s = buffer.getShort();
                        is[i] = s >= 0 ? s : 0x10000 + s;
                    }

                    iAmplitude.add(average(is));
                    buffer.position(a);

                    //Retrieving pitch from window
                    //Cepstrum cepstrum = new Cepstrum();
                    //float pitch = cepstrum.getPitch(data, frameRate);

                    //Add pitch into array
                    //iPitches.add(pitch);

                    codec.releaseOutputBuffer(outIndex, true);
                    break;
            }

            // All decoded frames have been rendered, we can stop playing now
            if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                //Log.d("DecodeActivity", "OutputBuffer BUFFER_FLAG_END_OF_STREAM");
                break;
            }
        }

        iAmplitude = normalize(iAmplitude,100);

        Log.d("Result","=========Finish generating=======");
        return iAmplitude;
    }

    public static ArrayList<Integer> normalize(ArrayList<Integer> in, float upperBound){
        float max = Collections.max(in);
        float min = Collections.min(in);

        ArrayList<Integer> out = new ArrayList<>();

        for(int fin : in){
            int fout = (int) (upperBound - (upperBound * (max - fin)/(max-min)));
            out.add(fout);
        }
        return out;
    }

    public static int average(int[] in){
        short sum = 0;
        for(int s : in) sum += s;

        return (int) (sum/in.length);
    }
}
