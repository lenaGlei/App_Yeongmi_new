package com.example.app_yeongmi;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Locale;



public class Cybathlon extends AppCompatActivity {
    private MediaPlayer player;
    private MediaPlayer beepdouble;
    private MediaPlayer beep;

    private boolean isBound = false;
    private MqttService mqttService;



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            MqttService.LocalBinder binder = (MqttService.LocalBinder) service;
            mqttService = binder.getService();
            isBound = true;
            // Du kannst jetzt Methoden auf mqttService aufrufen
            mqttService.publish(mqttService.getPublishTopic(), "start");
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

        // Verbinde dich mit dem MqttService
        Intent intent = new Intent(this, MqttService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);

        LocalBroadcastManager.getInstance(this).registerReceiver(mqttMessageReceiver,
                new IntentFilter("com.example.app.MQTT_MESSAGE"));

        LocalBroadcastManager.getInstance(this).registerReceiver(mqttMessageReceiver,
                new IntentFilter("com.example.app.MQTT_NAVIGATION"));


        Button button = findViewById(R.id.btn_CybathlonActive);

        beepdouble = MediaPlayer.create(this, R.raw.beepdouble);
        beep = MediaPlayer.create(this, R.raw.beep);



        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                vibrateNow(500);


                Intent intent = new Intent(Cybathlon.this, EmptySeatsView.class);
                startActivity(intent);
            }
        });

    }


    private void playSounds(int[] Distancearray) {
        for (int value : Distancearray) {
            if (value == 1) {
                // Play sound 1
                playSound(beepdouble);
            } else {
                // Play sound 2
                playSound(beep);
            }
            try {
                Thread.sleep(1000); // Adjust as needed
            }catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void playSound(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
    }


    private void vibrateNow (long millis){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE))
                    .vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(millis);
        }

    }


    @Override
    protected void onStart() {

        super.onStart();

        // Start playing the sound when the activity starts
        player = MediaPlayer.create(Cybathlon.this, R.raw.sound2);
        player.start();

        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {

                // hier Distancearray durch mqtt ersetzen :)
                //int[] Distancearray = {1, 0, 1, 0, 1};
                //playSounds(Distancearray);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Release the MediaPlayer when the activity is paused
        if (player != null) {
            player.release();
        }
    }


    @Override
    public void onBackPressed() {
        // Stop TextToSpeech if it's speaking


        // Stop MediaPlayer if it's playing
        if (player != null && player.isPlaying()) {
            player.stop();
        }

        // Call super method for default back behavior
        super.onBackPressed();
    }



    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mqttMessageReceiver);
        // Löse die Verbindung zum Service auf
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
        super.onStop();
    }


    private BroadcastReceiver mqttMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Result Array
            if ("com.example.app.MQTT_MESSAGE".equals(intent.getAction())) {
                String payload = intent.getStringExtra("payload");
                Log.d("MQTT", "Message received in Cybathlon: " + payload);
                // Konvertiere die Payload in ein Array von Integern
                try {
                    JSONArray jsonArray = new JSONArray(payload);
                    int[] seatStatus = new int[jsonArray.length()];
                    for (int i = 0; i < jsonArray.length(); i++) {
                        seatStatus[i] = jsonArray.getInt(i);
                    }
                    mqttService.setSeatStatus(seatStatus); //save result array in mqttService

                    //start emptyview activity
                    Intent newIntent = new Intent(Cybathlon.this, EmptySeatsView.class);
                    startActivity(newIntent);


                } catch (JSONException e) {
                    Log.e("MQTT", "Fehler beim Parsen der Payload", e);
                }
            }

            // navigation: 1 --> double beep 2--> beep
            if ("com.example.app.MQTT_NAVIGATION".equals(intent.getAction())) {
                String payload = intent.getStringExtra("payload");
                Log.d("MQTT", "Message received in Cybathlon: " + payload);
                if ("1".equals(payload)) {
                    playSound(beepdouble);
                }
                if ("2".equals(payload)) {
                    playSound(beep);
                }
            }
        }
    };
}
