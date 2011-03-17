package org.devtcg.iodemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class MainView extends SurfaceView implements SurfaceHolder.Callback {
    private DrawThread mThread;

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
    }

    public void surfaceDestroyed(SurfaceHolder holder) {
        mThread.setDrawing(false);
    }
}
