package itme.clapapp;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final int RECORDER_SAMPLERATE = 8000;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final short WORD_SPLIT_LOWER_BOUND = 1000;
    private static final short WORD_SPLIT_UPPER_BOUND = 1800;
    private AudioRecord recorder = null;
    private ProgressBar progressBar = null;
    private Thread recordingThread = null;
    private boolean isRecording = false;
    private UpdateTextViewRunner amplValueTextUpdateRunner = null;
    private WordSplitDetector wordSplitDetector = null;
    private MediaPlayer soundPlayer = null;
    private ImageBlinker clappingsHandsImageBlinker = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        progressBar = ((ProgressBar) findViewById(R.id.progressBar));
        TextView amplValueText = ((TextView) findViewById(R.id.amplValue));
        amplValueTextUpdateRunner = new UpdateTextViewRunner(amplValueText);
        wordSplitDetector = new WordSplitDetector(WORD_SPLIT_LOWER_BOUND, WORD_SPLIT_UPPER_BOUND);

        ImageView clappingHandsImageView = ((ImageView) findViewById(R.id.clappingHands));
        clappingsHandsImageBlinker = new ImageBlinker(clappingHandsImageView);

        ((Button) findViewById(R.id.btnStartRecord)).setOnClickListener(btnStartRecord_OnClick);
        ((Button) findViewById(R.id.btnStopRecord)).setOnClickListener(btnStopRecord_OnClick);

        int bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,
                RECORDER_CHANNELS, RECORDER_AUDIO_ENCODING);
    }

    private Boolean permissionGranted = false;

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults.length < 1 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this,
                        "Permission denied to record audio. This permission will be needed to use this app.",
                        Toast.LENGTH_LONG);
                permissionGranted = true;
            }
        }
    }

    int BufferElements2Rec = 1024; // want to play 2048 (2K) since 2 bytes we use only 1024
    int BytesPerElement = 2; // 2 bytes in 16bit format

    private void startRecording() {
        checkRecordPermission();

        soundPlayer = MediaPlayer.create(getApplicationContext(), R.raw.clap);

        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
            RECORDER_SAMPLERATE,
            RECORDER_CHANNELS,
            RECORDER_AUDIO_ENCODING,
            BufferElements2Rec * BytesPerElement);
        recorder.startRecording();

        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                captureAudio();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();
    }

    private void stopRecording() {
        // stops the recording activity
        if (null != recorder) {
            isRecording = false;
            recorder.stop();
            recorder.release();
            recorder = null;
            recordingThread = null;
            soundPlayer = null;
        }
    }

    private void checkRecordPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }
    }

    private void captureAudio() {
        short[] sData = new short[BufferElements2Rec];

        int numWords = 0;
        while (isRecording) {
            // display root mean square of amplitude
            double sum = 0;
            int readSize = recorder.read(sData, 0, sData.length);
            for (int i = 0; i < readSize; i++) {
                sum += sData[i] * sData[i];
            }
            if (readSize > 0) {
                final double amplitude = sum / readSize;
                final int rms = (int) Math.sqrt(amplitude);
                progressBar.setProgress(rms);

                amplValueTextUpdateRunner.setValue(String.valueOf(rms));
                runOnUiThread(amplValueTextUpdateRunner);

                if (wordSplitDetector.add((short) rms)) {
                    performClap();
                }
            }
        }

        progressBar.setProgress(0);

        amplValueTextUpdateRunner.setValue("0");
        runOnUiThread(amplValueTextUpdateRunner);
    }

    private void performClap() {
        Thread playingThread = new Thread(new Runnable() {
            public void run() {
                playFile();
            }
        }, "AudioRecorder Thread");
        playingThread.start();
    }

    private void playFile() {
        soundPlayer.start();
        runOnUiThread(clappingsHandsImageBlinker);
        try {
            Thread.sleep(400);
        } catch (InterruptedException ex) {}
        runOnUiThread(clappingsHandsImageBlinker);
    }

    private class ImageBlinker implements Runnable {
        private ImageView _imageView;

        ImageBlinker(ImageView imageView) {
            _imageView = imageView;
        }

        @Override
        public void run() {
            int visibility = _imageView.getVisibility();
            if (visibility == View.VISIBLE) {
                _imageView.setVisibility(View.INVISIBLE);
            }
            else {
                _imageView.setVisibility(View.VISIBLE);
            }
        }
    }

    // Detects whether a definite sound has been detected.
    private class WordSplitDetector {
        private short _lowerThreshold, _upperThreshold;
        private boolean _wordDetected;

        WordSplitDetector(short lowerThreshold, short upperThreshold) {
            _lowerThreshold = lowerThreshold;
            _upperThreshold = upperThreshold;
            _wordDetected = false;
        }

        public boolean add(short value) {
            if (_wordDetected && value <= _lowerThreshold) {
                _wordDetected = false;
                return true;
            }

            if (!_wordDetected && value >= _upperThreshold) {
                _wordDetected = true;
            }
            return false;
        }
    }

    // Can't update the contents of a TextView from a background thread;
    // this is a simple class that wraps a TextView and can be run on the
    // UI thread using Activity.runOnUiThread.
    private class UpdateTextViewRunner implements Runnable {
        private TextView _ref;
        private String _value;

        UpdateTextViewRunner(TextView ref) {
            _ref = ref;
        }

        public void setValue(String value) {
            _value = value;
        }

        @Override
        public void run() {
            _ref.setText(_value);
        }
    }

    private void toggleButtons(boolean recording) {
        ((Button) findViewById(R.id.btnStartRecord)).setEnabled(!recording);
        ((Button) findViewById(R.id.btnStopRecord)).setEnabled(recording);
    }

    private View.OnClickListener btnStartRecord_OnClick = new View.OnClickListener() {
        public void onClick(View v) {
            startRecording();
            toggleButtons(true);
        }
    };

    private View.OnClickListener btnStopRecord_OnClick = new View.OnClickListener() {
        public void onClick(View v) {
            stopRecording();
            toggleButtons(false);
        }
    };
}
