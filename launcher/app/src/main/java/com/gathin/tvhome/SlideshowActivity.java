package com.gathin.tvhome;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.WindowManager;

/**
 * The photo slideshow (PhotoShow) as an activity. Fire OS only runs Amazon's own screensaver, so the home screen
 * opens this after idleMs() without a key press. Any key closes it. After MAX_MS it stops holding the screen on,
 * so the TV's own screensaver and sleep timers take over again.
 */
public class SlideshowActivity extends Activity {

    private static final long IDLE_MS = 4 * 60_000L;
    private static final long MAX_MS = 20 * 60_000L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable release = () -> getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    private PhotoShow show;

    /** How long the home screen waits: IDLE_MS, but always 30 s before the TV's own screensaver would start. */
    static long idleMs(Context ctx) {
        long tvTimeout = Settings.System.getLong(ctx.getContentResolver(), Settings.System.SCREEN_OFF_TIMEOUT, 300_000L);
        return Math.max(30_000L, Math.min(IDLE_MS, tvTimeout - 30_000L));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        show = new PhotoShow(this);
        setContentView(show.view());
    }

    @Override
    protected void onResume() {
        super.onResume();
        show.start();
        handler.postDelayed(release, MAX_MS);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(release);
        show.stop();
        super.onPause();
        finish();   // anything that covers the slideshow ends it
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        // Volume keys keep working over the photos.
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
            case KeyEvent.KEYCODE_VOLUME_MUTE:
                return super.dispatchKeyEvent(event);
        }
        // Any other key wakes the home screen; the key itself is swallowed so it does not also open a tile.
        if (event.getAction() == KeyEvent.ACTION_DOWN && !isFinishing()) {
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
        return true;
    }
}
