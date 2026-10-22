# Fire TV Omni cleanup

Ad-free home screen for Amazon's own Fire TV smart TVs (Omni QLED / 4-Series, model **AFTTIFF43**,
**Fire OS 7.7.1.7**), done over network ADB with no root, nothing uninstalled, and an undo for every step.

Built on [cleanyourtv](https://github.com/Gathin23/cleanyourtv) by Gathin and two Fire TV forks of it,
[itsmeterrylin/fire-tv-cleanup](https://github.com/itsmeterrylin/fire-tv-cleanup) (dock + grid launcher) and
[andrewsaad/fire-tv-cleanup](https://github.com/andrewsaad/fire-tv-cleanup) (fast Home redirect for Fire OS 7).
This repo adds a tested debloat list for this TV, merges the two launchers, and adds an idle photo slideshow,
because Fire OS 7 will not run a third-party screensaver.

## What you get

- **37 Amazon packages disabled** (`debloat.txt`): screen-content recognition (ACR), the advertising ID,
  sponsored rows and autoplaying promos, shopping, telemetry and A/B experiments, Amazon Music/Photos/Luna/Kids,
  Alexa alerts and notifications. Prime Video, Alexa voice search, HDMI inputs, AirPlay, casting, the Appstore
  and the remote keep working.
- **My Home**, a small launcher (`launcher/`, package `com.gathin.tvhome`, no internet permission):
  a dock of four favourite apps, the rest in a grid one press down, Inputs / Wi-Fi / Settings buttons, a clock
  and a time-of-day background. Pressing Home on the remote lands on it in about half a second.
- **Your photos**: four time-of-day backgrounds and a slideshow that starts after 4 idle minutes on the home
  screen. The repo ships plain gradients; `tools/prep_photos.py` swaps in your own.
- **Privacy settings and ad-blocking DNS**: the `settings put` commands below.

## What you need

- A computer on the same network as the TV, with Python 3. On the TV: **Settings → My Fire TV → About**,
  select the device name 7 times, then **Developer Options → ADB debugging** on. Find the TV's IP under
  **Settings → My Fire TV → About → Network**, and check it there: another device on your network may have
  ADB open too.
- [cleanyourtv](https://github.com/Gathin23/cleanyourtv) cloned **next to** this repo, with its venv set up
  (its README, "Doing it by hand"). Its `tools/tv.py` talks ADB itself; on macOS the platform-tools `adb connect`
  fails with "No route to host" under Local Network privacy.
- To build the launcher: JDK 17+ (Android Studio's bundled one works) and the Android SDK with platform 34.

```bash
git clone https://github.com/Gathin23/cleanyourtv
git clone https://github.com/smittyPNW/firetv-omni-cleanup
export TV_HOST=192.168.1.50                       # your TV
cleanyourtv/tools/venv/bin/python -I cleanyourtv/tools/tv.py --probe    # accept "Allow USB debugging?" on the TV
```

`--probe` should report `ro.product.model = AFTTIFF43`.

## 1. Debloat

Read `debloat.txt` first and comment out anything you use. Then, from this repo:

```bash
../cleanyourtv/tools/disable.sh debloat.txt
```

`disable.sh` records the TV's state in `~/.cleanyourtv/` before changing anything, which is what the undo uses.

**Leave Amazon's screensaver alone.** On this build Amazon's AmbientManagerService starts only its own
screensaver (`com.amazon.tv.ftvambient` hosting `com.amazon.ftv.screensaver`) and ignores any other one. With
those two disabled the TV goes to sleep after 5 idle minutes instead of showing a screensaver.

## 2. Privacy settings and ad-blocking DNS

```bash
T="../cleanyourtv/tools/venv/bin/python -I ../cleanyourtv/tools/tv.py"
printf '%s\n' \
  "settings put secure amz_limit_ad_tracking 1" \
  "settings put secure limit_ad_tracking 1" \
  "settings put secure non_amz_limit_ad_tracking 1" \
  "settings put secure advertisingIdApp:interestBasedAds:value 0" \
  "settings put secure USAGE_METRICS_UPLOAD_ENABLED 0" \
  "settings put secure usage_metrics_marketing_enabled 0" \
  "settings put secure settingsApp:appUsageData:value 0" \
  "settings put secure settingsApp:deviceUsageData:value 0" \
  "settings put global com.amazon.tv.settings.featured_item_video_enabled 0" \
  "settings put global com.amazon.tv.settings.featured_item_video_volume 0.0" \
  "settings put global private_dns_specifier dns.adguard-dns.com" \
  "settings put global private_dns_mode hostname" | $T --batch
```

Write down the old values first (`settings get ...`) if you want to put them back exactly. AdGuard DNS blocks
ad and tracker domains inside every app; it does not block YouTube ads, which come from the same servers as
the video ([SmartTube](https://github.com/yuliskov/SmartTube) does). The streaming apps' server-side ad
insertion domains still resolve, so ad-supported tiers keep playing.

## 3. Launcher

Edit `launcher/app/src/main/res/values/config.xml` for your dock and hidden apps, then:

```bash
TV_HOST=192.168.1.50 launcher/build-and-install.sh
```

It builds the APK, installs it, grants it `WRITE_SECURE_SETTINGS` (so it can switch its own redirect on and
off from the TV), and enables its accessibility service. **Amazon's launcher stays installed**: Fire OS hosts
Settings in it and ignores the default-home setting, so disabling it locks you out of Settings.

How Home works on Fire OS 7: Android 9 holds back any app started from the background for 5 s after Home is
pressed, and Fire OS never passes the Home key to accessibility services. So the service watches for Amazon's
home screen and clicks the **My Home** tile in Amazon's apps row; Amazon opens it, which is not held back.
Keep that tile at the front of the row (highlight it, Menu, **Move to front**). Without it the service covers
Amazon's screen in black and opens My Home after the 5 s.

On the TV: hold OK (or press Menu) on an app to pin, unpin or move it in the dock. Hold OK on the Settings
button for **Use Amazon Home Screen**.

### Your own photos

```bash
pip install pillow
python3 tools/prep_photos.py ~/Pictures/tv --backgrounds dawn.jpg afternoon.jpg sunset.jpg night.jpg
TV_HOST=192.168.1.50 launcher/build-and-install.sh
```

Every landscape photo at least 1600 px wide becomes a slideshow image (`saver_01.jpg`, ...), center-cropped to
16:9 at 1920×1080; the four `--backgrounds` names become the morning (5–12), afternoon (12–17), evening (17–22)
and night backgrounds. The slideshow starts after 4 idle minutes on the home screen (30 s before the TV's own
screensaver timeout, if that is shorter), any key closes it, volume keys still work, and after 20 minutes it
lets the TV's own screensaver and sleep timers take over.

## After an Amazon update

An update can turn packages back on, and any APK update unbinds the launcher's accessibility service. Re-run
`../cleanyourtv/tools/disable.sh debloat.txt` (it skips what is already disabled) and
`launcher/build-and-install.sh`.

## Undo

- **Home redirect off**: on the TV, hold OK on My Home's Settings button → **Use Amazon Home Screen**. Or
  `settings put secure enabled_accessibility_services null` and `settings put secure accessibility_enabled 0`.
  Remove the app in **Settings → Applications** if you want it gone.
- **Packages back**: `TV_HOST=<tv ip> ../cleanyourtv/tools/restore.sh debloat.txt com.amazon.tv.launcher`
- **DNS back**: `settings put global private_dns_mode opportunistic`
- **Privacy settings**: put back the values you wrote down.
- A factory reset undoes everything.

When you are done, turn **ADB debugging** off in Developer Options. My Home keeps working without it.

## Licence

MIT, see `LICENSE` (cleanyourtv and its forks are MIT, © 2026 Gathin). The Outfit and Open Sans typefaces are
under the SIL Open Font License, see `launcher/FONT-LICENSE-OFL.txt`. Nothing here is Amazon code.
