package Cepstrum;

import Fourier.*;

/**
 * Created by Zell on 12/3/15.
 */
public class Cepstrum extends FFT{

    /*** GENERATE CEPSTRUM OF SIGNAL ***/
    public static double[] Cepstrum(double input[], int samples){
        Complex[] x = new Complex[samples];

        for(int i = 0; i < samples; i++){
            x[i] = new Complex(input[i], 0);
        }

        Complex[] y = fft(x);

        Complex[] c = new Complex[samples];

        int i = 0;
        for(Complex complex: y){
            double abs = complex.abs()*hamming(y.length, i);
            c[i] = new Complex(Math.log(abs), 0);
            i++;
        }

        Complex[] ceptstrum = ifft(c);
        double[] cepstrum_mag = new double[samples];
        for(int n = 0; n < samples; n++){
            cepstrum_mag[n] = ceptstrum[n].abs();
        }

        return cepstrum_mag;
    }


    /*** FINDING THE PITCH (IN Hz)  OF A WINDOW ***/
    public float getPitch(double[] array,int frameRate){

        //initialize the peak as of frequency 70Hz - lower bound of fundamental frequency
        double max = array[frameRate/280];
        float frequency = 85;
        int i = 0;

        //Find the peak in range 70Hz - 635Hz
        if(array.length > frameRate/87) {
            for (i = (frameRate / 280); i < (frameRate / 87); i++) {
                if (Math.abs(array[i]) > max) {
                    max = Math.abs(array[i]);
                    frequency = frameRate / i;
                }
            }
        }
        else frequency = 85;

        return frequency;
    }


    /*** HAMMING FUNCTION ***/
    public static double hamming(int length, int index){
        double angle = (2*Math.PI*index)/((double)length);
        double hamming = 0.54 - 0.46*Math.cos(angle);

        return hamming;
    }

}
