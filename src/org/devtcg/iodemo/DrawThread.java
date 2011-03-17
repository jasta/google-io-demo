package org.devtcg.iodemo;

import org.devtcg.iodemo.NumberFont.Glyph;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.SensorManager;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Random;

public class DrawThread extends Thread {
    private static final String TAG = DrawThread.class.getSimpleName();

    private static final boolean DEBUG_DRAW_BALL_ID = Constants.DEBUG && false;

    private static final int PHYS_X_ACCEL_SEC = 8;
    private static final int PHYS_Y_ACCEL_SEC = 8;
    private static final float PHYS_Y_FRICTION_SORT_OF = 0.90f;
    private static final float PHYS_MIN_Y_ACCEL_AT_BOTTOM = 4f;

    private static final Random sRandom = new Random();

    /**
     * Incremeneted and assigned to each ball for debug purposes.
     */
    public int mNextBallId = 0;

    private SurfaceHolder mSurfaceHolder;
    private final Context mContext;

    /**
     * Array of all animating balls on screen (does not include the balls used
     * to draw the clock).
     */
    private final ArrayList<Ball> mBalls = new ArrayList<Ball>();
    private final Paint mRedPaint;
    private final Paint mGreenPaint;
    private final Paint mBluePaint;
    private final Paint mPurplePaint;
    private final Paint mGrayPaint;

    private final int mBackgroundColor;
    private final float mDigitSpacing;
    private final float mBallRadius;
    private final float mBallSpacing;
    private final float mBallMinDeltaX;
    private final float mBallMaxDeltaX;
    private final float mBallMinDeltaY;
    private final float mBallMaxDeltaY;

    private final NumericSprite mNumericSprite;

    /* Computed once to save time. */
    private float mClockWidth;
    private float mClockHeight;

    /**
     * True if we our surface is valid and we can draw; false otherwise.
     */
    private boolean mDrawing;

    private int mCanvasWidth;
    private int mCanvasHeight;

    /**
     * Holds the yaw, pitch, and roll of the device collected from the MainView
     * sensors.
     */
    private float[] mGData;
    private float[] mOrientation;

    /**
     * Time that the last draw frame occurred (in milliseconds).
     */
    private long mLastDraw;

    /**
     * During a draw event represents the amount of time (in seconds) that has
     * elapsed since the previous draw event.
     */
    private double mElapsed;

    /*
     * Record the previous times so that if they change we can animate the
     * bouncing balls.
     */
    private int mLastDay;
    private int mLastHour;
    private int mLastMinute;
    private int mLastSecond;

    public DrawThread(SurfaceHolder surfaceHolder, Context context) {
        mSurfaceHolder = surfaceHolder;
        mContext = context;

        Resources res = context.getResources();

        mRedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mRedPaint.setColor(res.getColor(R.color.red));
        mGreenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGreenPaint.setColor(res.getColor(R.color.green));
        mBluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBluePaint.setColor(res.getColor(R.color.blue));
        mPurplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPurplePaint.setColor(res.getColor(R.color.purple));
        mGrayPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mGrayPaint.setColor(res.getColor(R.color.gray));

        mBackgroundColor = res.getColor(R.color.background);
        mDigitSpacing = res.getDimension(R.dimen.digitSpacing);
        mBallRadius = res.getDimension(R.dimen.ballRadius);
        mBallSpacing = res.getDimension(R.dimen.ballSpacing);
        mBallMinDeltaX = res.getDimension(R.dimen.ballMinDeltaX);
        mBallMaxDeltaX = res.getDimension(R.dimen.ballMaxDeltaX);
        mBallMinDeltaY = res.getDimension(R.dimen.ballMinDeltaY);
        mBallMaxDeltaY = res.getDimension(R.dimen.ballMaxDeltaY);

        mNumericSprite = new NumericSprite();
    }

    public void setSurfaceSize(int width, int height) {
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height;
        }
    }

    public void setSensorData(float[] gData, float[] orientation) {
        synchronized (mSurfaceHolder) {
            mGData = gData;
            mOrientation = orientation;
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
                Glyph glyph = NumberFont.sFont[sRandom.nextInt(NumberFont.sFont.length)];

                float ballDiameter = mBallRadius * 2;

                float startX = event.getX() - ((ballDiameter + mBallSpacing) * 2f);
                float x = startX;
                float y = event.getY() - ((ballDiameter + mBallSpacing) * 3.5f);

                for (int posY = 0; posY < glyph.getHeight(); posY++) {
                    for (int posX = 0; posX < glyph.getWidth(); posX++) {
                        if (glyph.isLit(posX, posY)) {
                            Ball ball = new Ball();
                            ball.id = mNextBallId++;
                            ball.x = x;
                            ball.y = y;
                            ball.dx = randomFloatWithinRange(mBallMinDeltaX, mBallMaxDeltaX);
                            ball.dy = randomFloatWithinRange(mBallMinDeltaY, mBallMaxDeltaY);
                            ball.radius = mBallRadius;

                            switch (sRandom.nextInt(4)) {
                                case 0: ball.paint = mRedPaint; break;
                                case 1: ball.paint = mGreenPaint; break;
                                case 2: ball.paint = mBluePaint; break;
                                case 3: ball.paint = mPurplePaint; break;
                            }

                            mBalls.add(ball);
                        }
                        x += ballDiameter + mBallSpacing;
                    }
                    y += ballDiameter + mBallSpacing;
                    x = startX;
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

        long startTime = System.currentTimeMillis();
        drawClock(canvas, startTime);

        if (!mBalls.isEmpty()) {
            mElapsed = (startTime - mLastDraw) / 1000.0;
            if (mElapsed > 0) {
                updatePhysics();
            }
            drawBalls(canvas);
        }

        mLastDraw = startTime;
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(mBackgroundColor);
    }

    private void drawClock(Canvas canvas, long now) {
        long timeLeft;
        if (now >= Constants.COUNTDOWN_TO_WHEN) {
            Log.d(TAG, "Go to I/O, it's happening RIGHT NOW!");
            timeLeft = 0;
        } else {
            timeLeft = Constants.COUNTDOWN_TO_WHEN - now;
        }

        timeLeft /= 1000;
        int sec = (int)(timeLeft % 60);
        timeLeft /= 60;
        int min = (int)(timeLeft % 60);
        timeLeft /= 60;
        int hour = (int)(timeLeft % 24);
        timeLeft /= 24;
        int day = (int)timeLeft;

        if (mClockWidth == 0) {
            mNumericSprite.setValue(0, 2);
            mClockWidth = (mNumericSprite.width() * 4) + (mDigitSpacing * 6);
            mClockHeight = mNumericSprite.height();
        }

        float x = (mCanvasWidth - mClockWidth) / 2f;
        float y = (mCanvasHeight - mClockHeight) / 2f;

        float colonTopY = y + (mBallRadius * 5f) + (mBallSpacing * 2f);
        float colonBottomY = colonTopY + (mBallRadius * 4f) + (mBallSpacing * 2f);

        mNumericSprite.setValue(day, 2);
        mNumericSprite.draw(canvas, x, y, mPurplePaint);
        x += mNumericSprite.width();
        canvas.drawCircle(x + mDigitSpacing, colonTopY, mBallRadius, mGrayPaint);
        canvas.drawCircle(x + mDigitSpacing, colonBottomY, mBallRadius, mGrayPaint);
        x += mDigitSpacing * 2;
        mNumericSprite.setValue(hour, 2);
        mNumericSprite.draw(canvas, x, y, mBluePaint);
        x += mNumericSprite.width();
        canvas.drawCircle(x + mDigitSpacing, colonTopY, mBallRadius, mGrayPaint);
        canvas.drawCircle(x + mDigitSpacing, colonBottomY, mBallRadius, mGrayPaint);
        x += mDigitSpacing * 2;
        mNumericSprite.setValue(min, 2);
        mNumericSprite.draw(canvas, x, y, mRedPaint);
        x += mNumericSprite.width();
        canvas.drawCircle(x + mDigitSpacing, colonTopY, mBallRadius, mGrayPaint);
        canvas.drawCircle(x + mDigitSpacing, colonBottomY, mBallRadius, mGrayPaint);
        x += mDigitSpacing * 2;
        mNumericSprite.setValue(sec, 2);
        mNumericSprite.draw(canvas, x, y, mGreenPaint);
    }

    private void updatePhysics() {
        float verticalForce;
        float horizontalForce;
        if (mGData != null && mOrientation != null) {
            float pitch = mOrientation[1];
            horizontalForce = (float)(PHYS_X_ACCEL_SEC * (float)Math.sin(-pitch) * mElapsed);
            verticalForce = (float)(PHYS_Y_ACCEL_SEC * (float)Math.cos(-pitch) * mElapsed);

            /*
             * The device must be upside down, invert the vertical force so we
             * "drop" toward the ceiling.
             */
            if (mGData[0] < 0) {
                verticalForce *= -1;
            }
        } else {
            horizontalForce = 0f;
            verticalForce = (float)(PHYS_Y_ACCEL_SEC * mElapsed);
        }

        int N = mBalls.size();
        for (int i = 0; i < N; i++) {
            Ball ball = mBalls.get(i);

            /* Apply the device pitch (as an accelerating force). */
            float dx = ball.dx + horizontalForce;
            ball.dx = dx;

            /* Apply vertical acceleration. */
            float dy = ball.dy + verticalForce;
            float posy = ball.y + dy;
            if (posy > mCanvasHeight && dy > 0) {
                if (dy < PHYS_MIN_Y_ACCEL_AT_BOTTOM) {
                    dy = PHYS_MIN_Y_ACCEL_AT_BOTTOM;
                }
                dy *= -PHYS_Y_FRICTION_SORT_OF;
            }
            ball.dy = dy;

            /* Reposition. */
            ball.x += dx;
            ball.y = posy;

            /* Prune. */
            if (ball.x < 0 || ball.x > mCanvasWidth) {
                mBalls.remove(i);
                N--;
            } else {
//                /* Check for a hit (XXX: this algorithm is n^2). */
//                for (int j = 0; j < N; j++) {
//                    Ball otherBall = mBalls.get(j);
//                    if (otherBall.intersects(ball)) {
//                        Log.d(TAG, "HIT!");
//                        Log.d(TAG, "ball1=" + ball);
//                        Log.d(TAG, "ball2=" + otherBall);
//                        mBalls.remove(i);
//                        N--;
//                        break;
//                    }
//                }
            }
        }
    }

    private void drawBalls(Canvas canvas) {
        int N = mBalls.size();
        for (int i = 0; i < N; i++) {
            Ball ball = mBalls.get(i);
            canvas.drawCircle(ball.x, ball.y, ball.radius, ball.paint);
            if (DEBUG_DRAW_BALL_ID) {
                canvas.drawText(String.valueOf(ball.id), ball.x + ball.radius, ball.y - ball.radius,
                        ball.paint);
            }
        }
    }

    private static class Ball {
        public int id;
        public float x, y;
        public float dx, dy;
        public float radius;
        public Paint paint;

        public boolean intersects(Ball ball) {
            //Initialize the return value *t = 0.0f;
            float dvx = ball.dx - dx;
            float dvy = ball.dy - dy;
            float dpx = ball.x - x;
            float dpy = ball.y - y;
            float r = ball.radius + radius;
            //dP^2-r^2
            float pp = dpx * dpx + dpy * dpy - r*r;
            //(1)Check if the spheres are already intersecting
            if ( pp < 0 ) return true;
            //dP*dV
            float pv = dpx * dvx + dpy * dvy;
            //(2)Check if the spheres are moving away from each other
            if ( pv >= 0 ) return false;
            //dV^2
            float vv = dvx * dvx + dvy * dvy;
            //(3)Check if the spheres can intersect within 1 frame
            if ( (pv + vv) <= 0 && (vv + 2 * pv + pp) >= 0 ) return false;
            //tmin = -dP*dV/dV*2
            //the time when the distance between the spheres is minimal
            float tmin = -pv/vv;
            //Discriminant/(4*dV^2) = -(dp^2-r^2+dP*dV*tmin)
            return ( pp + pv * tmin > 0 );
        }

        @Override
        public String toString() {
            return "{id=" + id + "; pos=(" + x + "," + y + "); delta=(" + dx + "," + dy +")}";
        }
    }

    private class NumericSprite {
        private int mValue;
        private int mDigits;

        public void setValue(int value, int digits) {
            mValue = value;
            mDigits = digits;
        }

        private float distanceBetweenBalls() {
            return (mBallRadius * 2) + mBallSpacing;
        }

        private float digitWidth() {
            return (distanceBetweenBalls() * NumberFont.CONSTANT_WIDTH) - mBallSpacing;
        }

        private float digitWidthPlusSpacing() {
            return digitWidth() + mDigitSpacing;
        }

        public void draw(Canvas canvas, float x, float y, Paint paint) {
            /*
             * We draw right to left, so lets advance the x position
             * accordingly.
             */
            x += width() - digitWidth();
            int value = mValue;
            int digits = mDigits;
            while (digits-- > 0) {
                int digit = value % 10;
                drawDigit(canvas, x, y, digit, paint);
                value /= 10;
                x -= digitWidthPlusSpacing();
            }
        }

        private void drawDigit(Canvas canvas, float x, float y, int digit, Paint paint) {
            /*
             * Adjust for the fact that drawCircle draws at the center, but our
             * API suggests that we draw at the upper-left bounding box.
             */
            x += mBallRadius;
            y += mBallRadius;

            float curX = x;
            Glyph glyph = NumberFont.sFont[digit];
            for (int posY = 0; posY < glyph.getHeight(); posY++) {
                for (int posX = 0; posX < glyph.getWidth(); posX++) {
                    if (glyph.isLit(posX, posY)) {
                        canvas.drawCircle(curX, y, mBallRadius, paint);
                    } else {
                        canvas.drawCircle(curX, y, mBallRadius, mGrayPaint);
                    }
                    curX += distanceBetweenBalls();
                }
                y += distanceBetweenBalls();
                curX = x;
            }
        }

        public float width() {
            return (digitWidthPlusSpacing() * mDigits) - mDigitSpacing;
        }

        public float height() {
            return (distanceBetweenBalls() * NumberFont.CONSTANT_HEIGHT) - mBallSpacing;
        }
    }
}
