package com.example.app_yeongmi;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import android.media.AudioAttributes;
import android.media.AudioManager;

import java.util.Locale;


public class Cybathlon extends AppCompatActivity {
    MediaPlayer mMediaPlayer = new MediaPlayer();

    private TextToSpeech textToSpeech;
    int[] seat = {1, 1, 0, 1, 0, 1};

    private boolean isBound = false;
    private MqttService mqttService;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MqttService.LocalBinder binder = (MqttService.LocalBinder) service;
            mqttService = binder.getService();
            isBound = true;

            // Du kannst jetzt Methoden auf mqttService aufrufen
            mqttService.publish("emptySeats/AppToHardware", "start");
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cybathlon);

        mMediaPlayer = MediaPlayer.create(this, R.raw.sound2);
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        //mMediaPlayer.setLooping(true);
        mMediaPlayer.start();


        Button button = findViewById(R.id.btn_CybathlonActive);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                //vibrateNow(500);


                Intent intent = new Intent(Cybathlon.this, EmptySeatsView.class);
                startActivity(intent);
            }
        });


    }




    @Override
    protected void onStart() {
        super.onStart();

        // Verbinde dich mit dem MqttService
        Intent intent = new Intent(this, MqttService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Löse die Verbindung zum Service auf
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}


    /*private void sound(){

    for (int i = 0; i < distance.length; i++) {

        Log.d("Cybathlon", String.format("i = %d", i));


        if (distance[i] == 0) {

            // mediaPlayer1.start();
            playSound(mediaPlayer1);

        } else {
            //mediaPlayer2.start();
            playSound(mediaPlayer2);


        }
    }
    }

    private void playSound(final MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                // Release the MediaPlayer after sound is played
                mp.release();
            }
        });
    }



    private void vibrateNow (long millis){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE))
                    .vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(millis);
        }

    }





}
*/
