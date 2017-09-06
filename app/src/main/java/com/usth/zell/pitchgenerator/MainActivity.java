package com.usth.zell.pitchgenerator;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.File;
import java.io.IOException;

import SoundFile.WavFileException;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*PitchGenerator pg = new PitchGenerator(this, R.raw.test);
        pg.run();*/

        String readFile = "/mnt/shared/src/test_file/test.wav";
        String writeFile = "/storage/emulated/legacy/processed.wav";
        VoiceEffect.Effect fx = VoiceEffect.Effect.POP;
        VoiceEffect ve = null;
        try {
            ve = new VoiceEffect(readFile, writeFile, fx);
            ve.run();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (WavFileException e) {
            e.printStackTrace();
        }
    }


}
