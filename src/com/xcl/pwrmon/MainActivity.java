package com.xcl.pwrmon;

import android.app.Activity;
import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity {
    private MonitorView view;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        w.setDecorFitsSystemWindows(false);
        view = new MonitorView(this);
        setContentView(view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        view.start();
    }

    @Override
    protected void onPause() {
        view.stop();
        super.onPause();
    }
}
