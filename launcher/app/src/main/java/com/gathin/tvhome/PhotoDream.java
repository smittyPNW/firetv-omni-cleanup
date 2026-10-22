package com.gathin.tvhome;

import android.service.dreams.DreamService;

/**
 * Screensaver: your photos (PhotoShow). Chosen with:
 *   settings put secure screensaver_components com.gathin.tvhome/com.gathin.tvhome.PhotoDream
 * and Amazon's back with:
 *   settings put secure screensaver_components com.amazon.ftv.screensaver/.app.services.ScreensaverService
 * Fire OS 7 (AFTTIFF43, 7.7.1.7) never starts it: Amazon's AmbientManagerService runs only Amazon's own
 * screensaver, and the TV goes to sleep instead. There the home screen's SlideshowActivity shows the photos.
 */
public class PhotoDream extends DreamService {

    private PhotoShow show;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        show = new PhotoShow(this);
        setContentView(show.view());
    }

    @Override
    public void onDreamingStarted() {
        super.onDreamingStarted();
        show.start();
    }

    @Override
    public void onDreamingStopped() {
        show.stop();
        super.onDreamingStopped();
    }
}
