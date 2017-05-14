package scav.sttrbboy.onedirection;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity implements SensorEventListener, MediaPlayer.OnCompletionListener {

    private SensorManager mSensorManager;
    private Sensor mMagnetometer;
    private Sensor mAccelerometer;
    private float[] mLastMagnetometer = new float[3];
    private float[] mLastAccelerometer = new float[3];
    private boolean mLastMagnetometerIsSet = false;
    private boolean mLastAccelerometerIsSet = false;
    private float[] mRotationMatrix = new float[9];
    private float[] mOrientation = new float[3];
    private float mCurrentDegree = 0f;

    private EditText mStartRange;
    private EditText mEndRange;
    private MediaPlayer mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        mStartRange = (EditText) findViewById(R.id.range_start);
        mEndRange = (EditText) findViewById(R.id.range_end);
        mPlayer = MediaPlayer.create(MainActivity.this, R.raw.nobody);
        mPlayer.setOnCompletionListener(this);
        mPlayer.setVolume(0.0f, 0.0f);
    }

    private void SetDegreesInView() {
        ((TextView) findViewById(R.id.degrees)).setText(String.valueOf(mCurrentDegree));
    }

    //private float DetermineLoudness(float middleOfRange) {
    //    float propDiff = Math.min(1.0f, Math.abs(middleOfRange - mCurrentDegree) / middleOfRange);
    //    return 1.0f - propDiff;
    //}

    private void AdjustVolume() {
        String strStartRange = mStartRange.getText().toString();
        String strEndRange = mEndRange.getText().toString();
        if (!strStartRange.equals("") && !strEndRange.equals("")) {
            float startRange = Float.parseFloat(mStartRange.getText().toString());
            float endRange = Float.parseFloat(mEndRange.getText().toString());
            //float middle = (endRange - startRange) % 360;
            //float newVolume = DetermineLoudness();
            float newVolume;
            if (startRange < endRange) {
                newVolume = (startRange <= mCurrentDegree && mCurrentDegree <= endRange) ? 1f : 0f;
            } else {
                newVolume = (startRange <= mCurrentDegree || mCurrentDegree <= endRange) ? 1f : 0f;
            }
            mPlayer.setVolume(newVolume, newVolume);
        }
    }


    @Override
    public void onCompletion(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerIsSet = true; // mLastAccelerometer is initially all zero; this
                                            // would fuck with the initial compass setting.
        }
        else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerIsSet = true; // mLastMagnetometer is initially all zero; this
                                            // would fuck with the initial compass setting.
        }
        if (mLastAccelerometerIsSet && mLastMagnetometerIsSet) {
            SensorManager.getRotationMatrix(mRotationMatrix, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mRotationMatrix, mOrientation);
            float azimuthInRadians = mOrientation[0];
            mCurrentDegree = (float)(Math.toDegrees(azimuthInRadians)+360) % 360;
            SetDegreesInView();
            AdjustVolume();
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not going to do anything.
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPlayer.start();
        if (mSensorManager != null && mMagnetometer != null && mAccelerometer != null) {
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_NORMAL);
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mPlayer.pause();
        if (mSensorManager != null) {
            mSensorManager.unregisterListener(this);
        }
    }
}
