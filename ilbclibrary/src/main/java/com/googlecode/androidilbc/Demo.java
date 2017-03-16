package com.googlecode.androidilbc;

import android.app.Activity;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;


//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;


public class Demo extends Activity {
    public static final String TAG = "ilbc";
    private Button mButton;
    private Demo.Recorder mRecorder;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
        }
    };
    public static final int STATE_IDLE = 70;
    public static final int STATE_RECORDING = 71;
    public static final int STATE_RECORDED = 72;
    public static final int STATE_PLAYING = 73;
    private int mState;

    public Demo() {
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_demo);
        this.mRecorder = new Demo.Recorder();
        this.mButton = (Button) this.findViewById(R.id.button);
        this.mButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                switch (Demo.this.mState) {
                    case 70:
                        Demo.this.updateState(71);
                        break;
                    case 71:
                        Demo.this.updateState(72);
                    case 72:
                        Demo.this.updateState(70);
                }

            }
        });
        this.updateState(70);
    }

    private void updateState(int state) {
        this.mState = state;
        switch (state) {
            case 70:
                this.mButton.setText("Record");
                break;
            case 71:
                this.mButton.setText("Recording");

                try {
                    this.mRecorder.setSavePath("/sdcard/rec.ilbc");
                    this.mRecorder.start();
                } catch (IOException var3) {
                    Log.e("ilbc", " " + var3);
                }
                break;
            case 72:
                this.mRecorder.stop();
                this.mButton.setText("Play");
                break;
            case 73:
                this.mButton.setText("Record");
        }

    }

    class Player {
        private static final int DEFAULT_BUFFER_SIZE = 1024;
        private AudioTrack mTrack;
        private int mBufferSize;

        public Player() {
            int bufferSize = AudioRecord.getMinBufferSize(8000, 2, 2);
            this.mTrack = new AudioTrack(3, 8000, 2, 2, bufferSize, 1);
        }

        public void play(byte[] data, int offset, int length) {
            this.mTrack.play();
            this.mTrack.write(data, 0, length);
        }

        public void play(String path) {
            File file = new File(path);
            int audioLength = (int) file.length();
            byte[] audio = new byte[audioLength];

            try {
                DataInputStream dis = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

                for (int i = 0; dis.available() > 0; ++i) {
                    audio[i] = dis.readByte();
                }

                dis.close();
            } catch (Exception var7) {
                ;
            }

        }
    }

    class Recorder {
        private String mPath;
        private int mBufferSize;
        private AudioRecord mRecorder;
        private FileOutputStream mOut;

        Recorder() {
        }

        public void setSavePath(String path) throws IOException {
            File file = new File(path);
            if (file.exists()) {
                Log.i("ilbc", "Remove exists file, " + path);
            }

            file.createNewFile();
            if (!file.canWrite()) {
                throw new IOException("Cannot write to " + path);
            } else {
                this.mPath = path;
            }
        }

        public void start() throws IOException {
            this.mBufferSize = AudioRecord.getMinBufferSize(8000, 16, 2);
            if (this.mBufferSize == -2) {
                Log.e("ilbc", "buffer error");
            } else {
                int truncated = this.mBufferSize % 480;
                if (truncated != 0) {
                    this.mBufferSize += 480 - truncated;
                    Log.i("ilbc", "Extend buffer to " + this.mBufferSize);
                }

                try {
                    this.mOut = new FileOutputStream(new File(this.mPath));
                    this.mOut.write("#!iLBC30\n".getBytes());
                } catch (FileNotFoundException var3) {
                    throw new IOException("File not found");
                }

                this.mRecorder = new AudioRecord(1, 8000, 16, 2, this.mBufferSize);
                this.mRecorder.startRecording();
                (new Demo.Recorder.RecordThread()).start();
            }
        }

        public void stop() {
            if (this.mRecorder == null) {
                Log.w("ilbc", "Recorder has not start yet");
            } else {
                this.mRecorder.stop();

                try {
                    this.mOut.close();
                } catch (IOException var2) {
                    ;
                }

            }
        }

        private void record() {
            int bufferSize = AudioRecord.getMinBufferSize(8000, 16, 2);
            if (bufferSize == -2) {
                Log.e("ilbc", "buffer error");
            } else {
                bufferSize *= 10;
                AudioRecord record = new AudioRecord(1, 8000, 16, 2, bufferSize);
                Log.d("ilbc", "buffer size: " + bufferSize);
                byte[] tempBuffer = new byte[bufferSize];
                record.startRecording();
                int bufferRead = record.read(tempBuffer, 0, bufferSize);
                if (bufferRead == -3) {
                    throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
                } else if (bufferRead == -2) {
                    throw new IllegalStateException("read() returned AudioRecord.ERROR_BAD_VALUE");
                } else if (bufferRead == -3) {
                    throw new IllegalStateException("read() returned AudioRecord.ERROR_INVALID_OPERATION");
                } else {
                    record.stop();
                    Log.e("ilbc", "playing, " + bufferRead);
                    byte[] samples = new byte['ꀀ'];
                    byte[] data = new byte['ꀀ'];
                    int dataLength = Codec.instance().encode(tempBuffer, 0, bufferRead, data, 0);
                    Log.d("ilbc", "encode " + bufferRead + " to " + dataLength);
                    Log.d("ilbc", "data[0]: " + data[0] + "data[dataLength-1]: " + data[dataLength - 1]);
                    int samplesLength = Codec.instance().decode(data, 0, dataLength, samples,0);
                    Log.d("ilbc", "decode " + dataLength + " to " + samplesLength);
                    Log.d("ilbc", "samples[0]: " + samples[0] + "samples[samplesLength-1]: " + samples[samplesLength - 1]);
                    (Demo.this.new Player()).play(samples, 0, samplesLength);
                    Log.e("ilbc", "rec");
                }
            }
        }

        class RecordThread extends Thread {
            RecordThread() {
            }

            public void run() {
                while (Recorder.this.mRecorder != null && 3 == Recorder.this.mRecorder.getRecordingState()) {
                    byte[] samples = new byte[Recorder.this.mBufferSize];
                    byte[] data = new byte[Recorder.this.mBufferSize];
                    int bytesRecord = Recorder.this.mRecorder.read(samples, 0, Recorder.this.mBufferSize);

                    if (bytesRecord == -3) {
                        Log.e("ilbc", "read() returned AudioRecord.ERROR_INVALID_OPERATION");
                    } else if (bytesRecord == -2) {
                        Log.e("ilbc", "read() returned AudioRecord.ERROR_BAD_VALUE");
                    } else if (bytesRecord == -3) {
                        Log.e("ilbc", "read() returned AudioRecord.ERROR_INVALID_OPERATION");
                    }

                    int bytesEncoded = Codec.instance().encode(samples, 0, bytesRecord, data, 0);
                    Log.d("ggg","长度：："+bytesEncoded);
                    try {
                        Recorder.this.mOut.write(data, 0, bytesEncoded);
                    } catch (IOException var6) {
                        Log.e("ilbc", "Failed to write");
                    }
                }

            }
        }
    }
}

