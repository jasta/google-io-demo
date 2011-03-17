package org.devtcg.iodemo;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        /* I'll probably be needing this eventually... */
        MainView mainView = (MainView)findViewById(R.id.main);
    }
}
