package com.nercms.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

import com.nercms.Config;
import com.nercms.R;
import com.nercms.audio.AudioServer;


public class MainActivity extends AppCompatActivity {
    EditText editText;
    AudioServer audioServer;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        editText = (EditText) findViewById(R.id.editText);

       // audioServer=new AudioServer(15484);


    }

    public void doSend(View v) {
      /*  int port = Integer.parseInt(editText.getText().toString());
        Intent intent = new Intent(this, VideoChatActivity.class);
        intent.putExtra("remote_ip", "111.22.15.239");
        intent.putExtra("remote_port", port);
        startActivity(intent);*/
       //audioServer.startRecording();


    }

    public void doReceive(View v) {
    /*    int port = Integer.parseInt(editText.getText().toString());
        Intent intent = new Intent(this, VideoChatActivity.class);
        intent.putExtra("remote_ip", "111.8.2.136");
        intent.putExtra("remote_port", port);
        startActivity(intent);*/
        //audioServer.stopRecording();
    }

    public void doServer(View v) {
        Intent intent = new Intent(this, VideoChatActivity.class);
        //intent.putExtra("remote_ip","111.22.15.239");
        //intent.putExtra("remote_port",10043);
        intent.putExtra("remote_ip", Config.serverIP);
        intent.putExtra("remote_port", Config.server_port);
        startActivity(intent);

    }
}
