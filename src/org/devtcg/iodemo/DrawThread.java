package org.devtcg.iodemo;

import org.devtcg.iodemo.NumberFont.Glyph;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import java.util.ArrayList;
import java.util.Random;

public class DrawThread extends Thread {
    private static final String TAG = DrawThread.class.getSimpleName();

    private static final int PHYS_X_ACCEL_SEC = 12;
    private static final int PHYS_Y_ACCEL_SEC = 12;
    private static final float PHYS_Y_FRICTION_SORT_OF = 0.90f;
    private static final float PHYS_MIN_Y_ACCEL_AT_BOTTOM = 4f;

    private static final Random sRandom = new Random();

    private SurfaceHolder mSurfaceHolder;
    private final Context mContext;

    /**
     * Array of all animating balls on screen (does not include the balls used
     * to draw the clock).
     */
    private final ArrayList<Ball> mAnimatingBalls = new ArrayList<Ball>();

    /**
     * Array of static balls on screen used for the clock. Balls will be cloned
     * from this set when they are to begin animating. These balls are actually
     * allocated once on initialization and merely change paints to adjust
     * color. This completes the object oriented analogy that the demo is trying
     * to convey.
     * <p>
     * This array includes the balls to draw the colon separating each set of
     * digits.
     */
    private final ArrayList<Ball> mClockBalls = new ArrayList<Ball>();

    /*
     * Organization of each part of the clock in terms of the ball objects that
     * are used to draw it. The purpose of this organization is to be able to
     * conveniently determine which balls are being "turned off" when the digit
     * changes so that they can be copied into the animating set and thus begin
     * animating.
     */
    private final DigitSet mDayDigits;
    private final DigitSet mHourDigits;
    private final DigitSet mMinuteDigits;
    private final DigitSet mSecondDigits;

    private final Paint mGrayPaint;

    private final int mBackgroundColor;
    private final float mDigitSpacing;
    private final float mBallRadius;
    private final float mBallSpacing;
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

    /**
     * Records the previous times so that if they change we can animate the
     * bouncing balls.
     */
    private final CountdownClock mLastCountdown = new CountdownClock();

    /**
     * Convenient container for the current countdown (to compare with the last
     * one).
     */
    private final CountdownClock mCurrentCountdown = new CountdownClock();

    public DrawThread(SurfaceHolder surfaceHolder, Context context) {
        mSurfaceHolder = surfaceHolder;
        mContext = context;

        Resources res = context.getResources();

        Paint redPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        redPaint.setColor(res.getColor(R.color.red));
        Paint greenPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        greenPaint.setColor(res.getColor(R.color.green));
        Paint bluePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bluePaint.setColor(res.getColor(R.color.blue));
        Paint purplePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        purplePaint.setColor(res.getColor(R.color.purple));

        mDayDigits = new DigitSet(purplePaint);
        mHourDigits = new DigitSet(bluePaint);
        mMinuteDigits = new DigitSet(redPaint);
        mSecondDigits = new DigitSet(greenPaint);

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
    }

    public void setSurfaceSize(int width, int height) {
        synchronized (mSurfaceHolder) {
            mCanvasWidth = width;
            mCanvasHeight = height;
            positionClock();
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
                            Ball ball = new Ball(x, y, mBallRadius);
                            ball.dx = randomFloatWithinRange(mBallMinDeltaX, mBallMaxDeltaX);
                            ball.dy = randomFloatWithinRange(mBallMinDeltaY, mBallMaxDeltaY);

                            switch (sRandom.nextInt(4)) {
                                case 0: ball.paint = mMinuteDigits.litPaint; break;
                                case 1: ball.paint = mSecondDigits.litPaint; break;
                                case 2: ball.paint = mHourDigits.litPaint; break;
                                case 3: ball.paint = mDayDigits.litPaint; break;
                            }

                            mAnimatingBalls.add(ball);
                        }
                        x += ballDiameter + mBallSpacing;
                    }
                    y += ballDiameter + mBallSpacing;
                    x = startX;
                }
            }
        }
    }

    private void positionClock() {
        /*
         * Kinda lame to have to clear the balls and reallocate when really all
         * we're doing is adjusting the x/y coordinates of them. Oh well, this
         * simplifies the logic a bit.
         */
        mClockBalls.clear();

        /* Initialize the positions of the static clock balls. */
        if (mClockBalls.isEmpty()) {
            float digitWidth = (mBallRadius * 2 * NumberFont.CONSTANT_WIDTH) +
                    (mBallSpacing * (NumberFont.CONSTANT_WIDTH - 1));
            float digitHeight = (mBallRadius * 2 * NumberFont.CONSTANT_HEIGHT) +
                    (mBallSpacing * (NumberFont.CONSTANT_HEIGHT - 1));

            float clockWidth = (digitWidth * 8) + (mDigitSpacing * 10);
            float clockHeight = digitHeight;

            float x = (mCanvasWidth - clockWidth) / 2f;
            float y = (mCanvasHeight - clockHeight) / 2f;

            createClockDigit(mClockBalls, mDayDigits.bitmaps[0], x, y); x += digitWidth + mDigitSpacing;
            createClockDigit(mClockBalls, mDayDigits.bitmaps[1], x, y); x += digitWidth + mDigitSpacing;
            createClockColon(mClockBalls, x, y); x += mDigitSpacing;
            createClockDigit(mClockBalls, mHourDigits.bitmaps[0], x, y); x += digitWidth + mDigitSpacing;
            createClockDigit(mClockBalls, mHourDigits.bitmaps[1], x, y); x += digitWidth + mDigitSpacing;
            createClockColon(mClockBalls, x, y); x += mDigitSpacing;
            createClockDigit(mClockBalls, mMinuteDigits.bitmaps[0], x, y); x += digitWidth + mDigitSpacing;
            createClockDigit(mClockBalls, mMinuteDigits.bitmaps[1], x, y); x += digitWidth + mDigitSpacing;
            createClockColon(mClockBalls, x, y); x += mDigitSpacing;
            createClockDigit(mClockBalls, mSecondDigits.bitmaps[0], x, y); x += digitWidth + mDigitSpacing;
            createClockDigit(mClockBalls, mSecondDigits.bitmaps[1], x, y); x += digitWidth + mDigitSpacing;
        }

        /* Reset this so that we trigger a full visual update. */
        mLastCountdown.setTimeLeft(0);
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
        handleClock(canvas, startTime);

        if (!mAnimatingBalls.isEmpty()) {
            mElapsed = (startTime - mLastDraw) / 1000.0;
            if (mElapsed > 0) {
                updatePhysics();
            }
        }

        drawBalls(canvas, mClockBalls);
        drawBalls(canvas, mAnimatingBalls);

        mLastDraw = startTime;
    }

    private void drawBackground(Canvas canvas) {
        canvas.drawColor(mBackgroundColor);
    }

    private void createClockDigit(ArrayList<Ball> drawList, Ball[][] digit, float x, float y) {
        /*
         * Adjust for the fact that drawCircle draws at the center, but our
         * API suggests that we draw at the upper-left bounding box.
         */
        x += mBallRadius;
        y += mBallRadius;

        float curX = x;
        for (int posY = 0; posY < NumberFont.CONSTANT_HEIGHT; posY++) {
            for (int posX = 0; posX < NumberFont.CONSTANT_WIDTH; posX++) {
                Ball ball = new Ball(curX, y, mBallRadius, mGrayPaint);
                drawList.add(ball);
                digit[posX][posY] = ball;
                curX += (mBallRadius * 2) + mBallSpacing;
            }
            y += (mBallRadius * 2) + mBallSpacing;
            curX = x;
        }
    }

    private void createClockColon(ArrayList<Ball> drawList, float x, float y) {
        float colonTopY = y + (mBallRadius * 5f) + (mBallSpacing * 2f);
        float colonBottomY = colonTopY + (mBallRadius * 4f) + (mBallSpacing * 2f);

        drawList.add(new Ball(x, colonTopY, mBallRadius, mGrayPaint));
        drawList.add(new Ball(x, colonBottomY, mBallRadius, mGrayPaint));
    }

    private void handleClock(Canvas canvas, long now) {
        long timeLeft;
        if (now >= Constants.COUNTDOWN_TO_WHEN) {
            Log.d(TAG, "Go to I/O, it's happening RIGHT NOW!");
            timeLeft = 0;
        } else {
            timeLeft = Constants.COUNTDOWN_TO_WHEN - now;
        }

        mCurrentCountdown.setTimeLeft(timeLeft);

        handleDigitChange(mDayDigits, mLastCountdown.days, mCurrentCountdown.days);
        handleDigitChange(mHourDigits, mLastCountdown.hours, mCurrentCountdown.hours);
        handleDigitChange(mMinuteDigits, mLastCountdown.minutes, mCurrentCountdown.minutes);
        handleDigitChange(mSecondDigits, mLastCountdown.seconds, mCurrentCountdown.seconds);

        mLastCountdown.setTimeLeft(mCurrentCountdown);
    }

    private void handleDigitChange(DigitSet digitSet, int lastCount, int newCount) {
        if (lastCount == newCount) {
            return;
        }
        int numDigits = digitSet.getNumDigits();
        int value = newCount;
        while (numDigits-- > 0) {
            int digitValue = value % 10;
            Ball[][] digitBitmap = digitSet.bitmaps[numDigits];
            Glyph glyph = NumberFont.sFont[digitValue];
            for (int x = 0; x < NumberFont.CONSTANT_WIDTH; x++) {
                for (int y = 0; y < NumberFont.CONSTANT_HEIGHT; y++) {
                    Ball ball = digitBitmap[x][y];

                    boolean shouldBeLit = glyph.isLit(x, y);
                    boolean isLit = ball.paint != mGrayPaint;

                    if (shouldBeLit != isLit) {
                        if (isLit) {
                            Ball anim = new Ball(ball.x, ball.y, ball.radius, ball.paint);
                            anim.dx = randomFloatWithinRange(mBallMinDeltaX, mBallMaxDeltaX);
                            anim.dy = randomFloatWithinRange(mBallMinDeltaY, mBallMaxDeltaY);
                            mAnimatingBalls.add(anim);
                        }
                        ball.paint = shouldBeLit ? digitSet.litPaint : mGrayPaint;
                    }
                }
            }
            value /= 10;
        }
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

        int N = mAnimatingBalls.size();
        for (int i = 0; i < N; i++) {
            Ball ball = mAnimatingBalls.get(i);

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
                mAnimatingBalls.remove(i);
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

    private void drawBalls(Canvas canvas, ArrayList<Ball> balls) {
        int N = balls.size();
        for (int i = 0; i < N; i++) {
            Ball ball = balls.get(i);
            canvas.drawCircle(ball.x, ball.y, ball.radius, ball.paint);
        }
    }

    private static class Ball {
        public float x, y;
        public float dx, dy;
        public float radius;
        public Paint paint;

        public Ball(float x, float y, float radius) {
            this(x, y, radius, null);
        }

        public Ball(float x, float y, float radius, Paint paint) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.paint = paint;
        }

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
            return "{pos=(" + x + "," + y + "); delta=(" + dx + "," + dy +")}";
        }
    }

    private static class CountdownClock {
        public int days;
        public int hours;
        public int minutes;
        public int seconds;

        public void setTimeLeft(long timeLeft) {
            timeLeft /= 1000;
            seconds = (int)(timeLeft % 60);
            timeLeft /= 60;
            minutes = (int)(timeLeft % 60);
            timeLeft /= 60;
            hours = (int)(timeLeft % 24);
            timeLeft /= 24;
            days = (int)timeLeft;
        }

        public void setTimeLeft(CountdownClock source) {
            days = source.days;
            hours = source.hours;
            minutes = source.minutes;
            seconds = source.seconds;
        }
    }

    private static class DigitSet {
        /**
         * Bitmap of balls for each digit in the set.
         */
        public final Ball[][][] bitmaps = new Ball[2][NumberFont.CONSTANT_WIDTH][NumberFont.CONSTANT_HEIGHT];

        /**
         * Paint to use when the ball is "lit" (not gray).
         */
        public final Paint litPaint;

        public DigitSet(Paint litPaint) {
            this.litPaint = litPaint;
        }

        public int getNumDigits() {
            return bitmaps.length;
        }
    }
}
