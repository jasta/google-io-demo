package org.devtcg.iodemo;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Random;

public class DrawThread extends Thread {
    private static final String TAG = DrawThread.class.getSimpleName();

    private static final int PHYS_ACCEL_Y_SEC = 8;

    private static final Random sRandom = new Random();

    private SurfaceHolder mSurfaceHolder;
    private final Context mContext;

    private final LinkedList<Ball> mBalls = new LinkedList<Ball>();
    private final ListIterator<Ball> mBallIterator = mBalls.listIterator();
    private final Paint mRedPaint;

    private final int mBackgroundColor;
    private final float mBallRadius;
    private final float mBallMinDeltaX;
    private final float mBallMaxDeltaX;
    private final float mBallMinDeltaY;
    private final float mBallMaxDeltaY;

    /**
     * True if we our surface is valid and we can draw; false otherwise.
     */
    private boolean mDrawing;

    private int mCanvasWidth;
    private int mCanvasHeight;

    /**
     * Time that the last draw frame occurred (in milliseconds).
     */
    private long mLastDraw;

    /**
     * During a draw event represents the amount of time (in seconds) that has
     * elapsed since the previous draw event.
     */
    private double mElapsed;

    public DrawThread(SurfaceHolder surfaceHolder, Context context) {
        mSurfaceHolder = surfaceHolder;
        mContext = context;

        mRedPaint = new Paint();
        mRedPaint.setColor(Color.RED);

        Resources res = context.getResources();
        mBackgroundColor = res.getColor(R.color.backgroundColor);
        mBallRadius = res.getDimension(R.dimen.ballRadius);
        mBallMinDeltaX = res.getDimension(R.dimen.ballMinDeltaX);
        mBallMaxDeltaX = res.getDimension(R.dimen.ballMaxDeltaX);
        mBallMinDeltaY = res.getDimension(R.dimen.ballMinDeltaY);
        mBallMaxDeltaY = res.getDimension(R.dimen.ballMaxDeltaY);
    }

    public void setSurfaceSize(int width, int height) {
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height;
        }
    }

    public void setDrawing(boolean isDrawing) {
        synchronized (mSurfaceHolder) {
            mDrawing = isDrawing;
        }
    }

    private float randomFloatWithinRange(float min, float max) {
        return (sRandom.nextFloat() * (max - min)) + min;
    }

    public void doTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_UP) {
            synchronized (mSurfaceHolder) {
                int newBalls = sRandom.nextInt(6) + 2;

                while (newBalls-- > 0) {
                    Ball ball = new Ball();
                    ball.x = event.getX();
                    ball.y = event.getY();
                    ball.dx = randomFloatWithinRange(mBallMinDeltaX, mBallMaxDeltaX);
                    ball.dy = randomFloatWithinRange(mBallMinDeltaY, mBallMaxDeltaY);
                    ball.radius = mBallRadius;
                    ball.paint = mRedPaint;
                    mBalls.add(ball);
                }
            }
        }
    }

    @Override
    public void run() {
        while (mDrawing) {
            Canvas canvas = mSurfaceHolder.lockCanvas();
            try {
                synchronized (mSurfaceHolder) {
                    doDraw(canvas);
                }
            } finally {
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }
    }

    private void doDraw(Canvas canvas) {
        drawBackground(canvas);

        if (!mBalls.isEmpty()) {
            long startTime = System.currentTimeMillis();
            mElapsed = (startTime - mLastDraw) / 1000.0;
            if (mElapsed > 0) {
                updatePhysics();
            }
            drawBalls(canvas);
            mLastDraw = startTime;
        }
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(mBackgroundColor);
    }

    private void updatePhysics() {
        int N = mBalls.size();
        for (int i = 0; i < N; i++) {
            Ball ball = mBalls.get(i);
            float dy = ball.dy + (float)(PHYS_ACCEL_Y_SEC * mElapsed);
            float posy = ball.y + dy;
            if (posy > mCanvasHeight && dy > 0) {
                dy *= -0.85f;
            }
            ball.dy = dy;
            ball.x += ball.dx;
            ball.y = posy;
        }
    }

    private void drawBalls(Canvas canvas) {
        int N = mBalls.size();
        for (int i = 0; i < N; i++) {
            Ball ball = mBalls.get(i);
            canvas.drawCircle(ball.x, ball.y, ball.radius, ball.paint);
        }
    }

    private static class Ball {
        public float x, y;
        public float dx, dy;
        public float radius;
        public Paint paint;
    }
}
