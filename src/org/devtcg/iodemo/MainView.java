package org.devtcg.iodemo;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class MainView extends SurfaceView implements SurfaceHolder.Callback, SensorEventListener {
    private DrawThread mThread;

    /*
     * Sensor data used to ultimately determine pitch and send that information
     * along to the DrawThread.
     */
    private float[] mGData = new float[3];
    private float[] mMData = new float[3];
    private float[] mR = new float[16];
    private float[] mI = new float[16];
    private float[] mOrientation = new float[3];

    /*
     * Copy of data designated for the draw thread (this copy is used to limit
     * the synchronization window so that the potentially expensive {@link
     * SensorManager#getOrientation} call is not included in it).
     */
    private float[] mGDataCopy = new float[mGData.length];
    private float[] mOrientationCopy = new float[mOrientation.length];

    public MainView(Context context) {
        this(context, null);
    }

    public MainView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MainView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        SurfaceHolder holder = getHolder();
        holder.addCallback(this);
        holder.setKeepScreenOn(true);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mThread.doTouchEvent(event);
        return true;
    }

    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mThread.setSurfaceSize(width, height);
    }

    public void surfaceCreated(SurfaceHolder holder) {
        mThread = new DrawThread(holder, getContext());
        mThread.setDrawing(true);
        mThread.start();
        mThread.setSensorData(mGDataCopy, mOrientationCopy);
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mThread.setDrawing(false);
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, mGData, 0, event.values.length);
        } else if (type == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, mMData, 0, event.values.length);
        } else {
            throw new IllegalStateException("Unknown sensor type=" + type);
        }

        SensorManager.getRotationMatrix(mR, mI, mGData, mMData);
        SensorManager.getOrientation(mR, mOrientation);

        /* Copy the new orientation data directly into the game thread. */
        synchronized (getHolder()) {
            System.arraycopy(mGData, 0, mGDataCopy, 0, mGData.length);
            System.arraycopy(mOrientation, 0, mOrientationCopy, 0, mOrientation.length);
        }
    }
}
