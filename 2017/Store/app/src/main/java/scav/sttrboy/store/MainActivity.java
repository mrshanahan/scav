package scav.sttrboy.store;

import android.graphics.Canvas;
import android.graphics.Movie;
import android.media.MediaPlayer;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.VideoView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements MediaPlayer.OnCompletionListener {

    private MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPlayer = MediaPlayer.create(MainActivity.this, R.raw.store);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setVolume(1f, 1f);
        mPlayer.seekTo(42000);
    }

    private void setVisibility(int id, boolean isVisible) {
        ((TextView) findViewById(id)).setVisibility(isVisible ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayer.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.pause();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.start();
    }
}
