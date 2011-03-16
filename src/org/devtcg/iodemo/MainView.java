package org.devtcg.iodemo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceView;

public class MainView extends SurfaceView {
    public MainView(Context context) {
        this(context, null);
    }

    public MainView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MainView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

}
