package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextClock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Your photos full screen, each slowly zooming in, crossfading every SLIDE_MS, with a small clock in the corner.
 * Shared by the screensaver (PhotoDream) and the home screen's idle slideshow (SlideshowActivity).
 * The photos are res/drawable-nodpi/saver_01.jpg, saver_02.jpg, ... in order with no gaps; tools/prep_photos.py
 * writes them.
 */
final class PhotoShow {

    private static final long SLIDE_MS = 20_000;
    private static final long FADE_MS = 2_500;
    private static final float ZOOM = 1.08f;   // reached over one slide, then the next photo takes over

    private final Context ctx;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Integer> order = new ArrayList<>();
    private final FrameLayout root;
    private final TextClock clock;
    private ImageView front, back;
    private int next;

    private final Runnable advance = new Runnable() {
        @Override
        public void run() {
            showNext();
            handler.postDelayed(this, SLIDE_MS);
        }
    };

    PhotoShow(Context ctx) {
        this.ctx = ctx;
        root = new FrameLayout(ctx);
        root.setBackgroundColor(Color.BLACK);
        front = photoView();
        back = photoView();
        root.addView(back);
        root.addView(front);
        clock = clock();
        root.addView(clock);

        for (int i = 1; ; i++) {
            int id = ctx.getResources().getIdentifier(String.format("saver_%02d", i), "drawable", ctx.getPackageName());
            if (id == 0) break;
            order.add(id);
        }
        Collections.shuffle(order);
        front.setAlpha(0f);
        back.setAlpha(0f);
    }

    View view() {
        return root;
    }

    void start() {
        handler.removeCallbacks(advance);
        handler.post(advance);
    }

    void stop() {
        handler.removeCallbacks(advance);
    }

    /** Fade the next photo in over the current one, and start its slow zoom. */
    private void showNext() {
        ImageView incoming = back, outgoing = front;
        incoming.setImageResource(order.get(next));
        next = (next + 1) % order.size();
        incoming.animate().cancel();
        incoming.setScaleX(1f);
        incoming.setScaleY(1f);
        incoming.setAlpha(0f);
        incoming.bringToFront();
        incoming.animate().alpha(1f).setDuration(FADE_MS).start();
        incoming.animate().scaleX(ZOOM).scaleY(ZOOM).setDuration(SLIDE_MS + FADE_MS).start();
        outgoing.animate().alpha(0f).setStartDelay(FADE_MS).setDuration(1).start();
        front = incoming;
        back = outgoing;
        clock.bringToFront();
    }

    private ImageView photoView() {
        ImageView v = new ImageView(ctx);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        v.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));
        return v;
    }

    private TextClock clock() {
        TextClock c = new TextClock(ctx);
        c.setFormat12Hour("h:mm");
        c.setFormat24Hour("HH:mm");
        c.setTextColor(0xE6FFFFFF);
        c.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
        c.setTypeface(ctx.getResources().getFont(R.font.open_sans));
        c.setFontVariationSettings("'wght' 300");
        c.setShadowLayer(12f, 0f, 2f, 0x99000000);
        float dp = ctx.getResources().getDisplayMetrics().density;
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM | Gravity.END);
        lp.setMargins(0, 0, (int) (48 * dp), (int) (36 * dp));
        c.setLayoutParams(lp);
        return c;
    }
}
