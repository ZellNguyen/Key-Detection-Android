package com.usth.zell.pitchgenerator;

import org.apache.commons.lang3.ArrayUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import Fourier.Complex;
import Fourier.FFT;
import SoundFile.WavFile;
import SoundFile.WavFileException;

/**
 * Created by Zell on 5/23/16.
 */
public class VoiceEffect implements Runnable {
    WavFile readFile;
    WavFile writeFile;

    Effect fx;

    public enum Effect{
        POP, CLASSIC, ROCK, FULL_BASS, ORIGINAL, ROBOT
    }

    public VoiceEffect(String readPath, String writePath, Effect fx) throws IOException, WavFileException {
        //Open read Wav File
        this.readFile = WavFile.openWavFile(new File(readPath));
        long numFrames = readFile.getNumFrames();
        int numChannels = readFile.getNumChannels();
        int validBits = readFile.getValidBits();
        long sampleRate = readFile.getSampleRate();

        //Create new Wav File
        this.writeFile = WavFile.newWavFile(new File(writePath), numChannels, numFrames, validBits, sampleRate);

        //Effect
        this.fx = fx;
    }

    @Override
    public void run() {
        final int BUFFER_SIZE = 1024;
        int numChannels = readFile.getNumChannels();
        long sampleRate = readFile.getSampleRate();

        double[] buffer = new double[BUFFER_SIZE*numChannels];
        double[] write_data = new double[]{};

        int frameRead = 0;
        int frameWritten = 0;
        int index = 0;

        try {
            do{
                index++;
                frameRead = readFile.readFrames(buffer, BUFFER_SIZE);
                if((frameRead & (frameRead-1)) == 0 && frameRead != 0) {
                    Complex[] amplitudes = new Complex[frameRead*numChannels];

                    for (int i = 0; i < frameRead * numChannels; i++) {
                        amplitudes[i] = new Complex(buffer[i], 0);
                    }

                    //FOURIER TRANSFORM
                    Complex[] spectrum = FFT.fft(amplitudes);
                    spectrum = applyEq(spectrum, sampleRate, fx);

                    //INVERSE FOURIER TRANSFORM
                    Complex[] outAmplitudes = FFT.ifft(spectrum);

                    double[] write_buffer = new double[outAmplitudes.length];
                    for (int i = 0; i < write_buffer.length; i++) {
                        write_buffer[i] = outAmplitudes[i].re()*1.2;
                    }

                    if(fx == Effect.ROBOT) write_buffer = echo(write_buffer, 300, 0.7);

                    //WRITE TO FILE
                    write_data = ArrayUtils.addAll(write_data, write_buffer);
                }
                else write_data = ArrayUtils.addAll(write_data, buffer);

            }while(frameRead != 0);

            write_data = applyReverb(write_data, sampleRate, fx);

            writeFile.writeFrames(write_data, write_data.length);

            readFile.close();
            writeFile.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WavFileException e) {
            e.printStackTrace();
        }

    }

    public static Complex[] gaussian(Complex[] input, long Fs, double peakF, double peakY, long width){
        int N = input.length;
        double[] gauss = new double[N];
        Arrays.fill(gauss, (double)1);

        Complex[] output = new Complex[N];

        double peakX = peakF * N / Fs;
        for(int x = 0; x < N; x++){
            double coeff = -(x-peakX)*(x-peakX)/width;
            gauss[x] = (peakY-1)*Math.exp(coeff) + 1;
            output[x] = input[x].times(gauss[x]);
        }

        return output;
    }

    public static Complex[] applyEq(Complex[] input, long Fs, Effect fx){
        double[] eq;
        switch (fx){
            case POP: eq = new double[]{0.1, 1.41, 1.30, 1, 0.92, 0.22};
                break;
            case CLASSIC: eq = new double[]{0.1, 1.41, 1.30, 1, 0.92, 0.22};
                break;
            case ROCK: eq = new double[]{0.1, 1.58, 0.79, 1.41, 1.52, 1};
                break;
            case FULL_BASS: eq = new double[]{0.28, 1.41, 2.42, 1.99, 1, 0.22};
                break;
            default: eq = new double[]{1, 1, 1, 1, 1};
        }

        Complex[] output;
        output = gaussian(input, Fs, 60, eq[0], 8000);
        output = gaussian(output, Fs, 80, eq[1], 4000);
        output = gaussian(output, Fs, 240, eq[2], 8000);
        output = gaussian(output, Fs, 750, eq[3], 40000);
        output = gaussian(output, Fs, 2200, eq[4], 200000);
        output = gaussian(output, Fs, 6600, eq[5], 400000);
        return output;
    }

    public static double[] echo(double[] input, int delay, double gain){
        double[] output = input;
        for(int i = delay; i < output.length; i++){
            double sampleDelay = input[i-delay]*gain;
            output[i] = output[i] + sampleDelay;
        }
        return output;
    }

    public static double[] reverb(double[] input, long fs, int delay, double decay){
        double[] output = input;
        int delaySamples = (int) (delay * (float)fs/1000);
        for(int i = 0; i < input.length - delaySamples; i++){
            output[i + delaySamples] += input[i] * decay;
        }
        return output;
    }

    public static double[] applyReverb(double[] input, long fs, Effect fx){
        double[] output = null;
        switch (fx){
            case POP: output = reverb(input, fs, 200, 0.1);
                break;
            case CLASSIC: output = reverb(input, fs, 300, 0.23);
                break;
            case ROCK: output = reverb(input, fs, 100, 0.2);
                break;
            case FULL_BASS: output = reverb(input, fs, 100, 0.2);
                break;
            default: output = input;
        }
        return output;
    }
}
