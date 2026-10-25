# New launcher for old Android TV

**Calm** is a tiny home screen for old Android TVs stuck on Android 4.4 KitKat (API 19).
It was built for a KTC TV338 (MStar chip, 1 GB RAM), but it should work on any 4.4+ TV box or set.

- A large clock and date over your own wallpaper, sharp at 1080p (or a soft built-in glow)
- A dock of favourite apps that grow and outline themselves under the remote's focus
- An **Apps** grid with every installed app
- Wi-Fi / Ethernet status with signal bars at the top right
- Long-press OK (or press Menu) on a tile to move it, remove it, open app info or uninstall
- Back does nothing on the home screen, so nobody gets stuck

## Light by design

| | Memory (PSS) |
|---|---|
| Stock KTC launcher | ~33 MB |
| Calm, default wallpaper | ~6 MB |
| Calm, 1080p photo wallpaper | ~14 MB |

Plain Java and Android's own views: no support libraries, no Gradle, no web views.
The APK is about 30 KB.

## Build

You need a JDK (any recent one) and the Android SDK with `platforms;android-19` and any build-tools.

```bash
sdkmanager "platforms;android-19"
./build.sh
```

This writes `build/calm-launcher.apk`, signed with your debug key.

## Install over adb

Turn on ADB / network debugging on the TV, then:

```bash
adb connect <tv-ip>:5555
adb install -r build/calm-launcher.apk
```

Press **Home** on the remote and pick **Calm → Always**.

> On Android 4.4, reinstalling clears the default home app, so pick Calm again after each update.

To go back to the stock launcher: Settings → Apps → Calm → Clear defaults, then press Home.

## Wallpaper

Calm uses the newest image in `/data/local/tmp/wallpapers` automatically. Many TVs have a read-only
shared storage, but this folder is writable over adb:

```bash
adb shell mkdir -p /data/local/tmp/wallpapers
adb shell chmod 755 /data/local/tmp/wallpapers
adb push my-wallpaper.jpg /data/local/tmp/wallpapers/
adb shell chmod 644 /data/local/tmp/wallpapers/my-wallpaper.jpg
```

Use a 1920×1080 image for the sharpest result. You can also choose one from the menu
(long-press the Apps tile → Change wallpaper), which also lists Pictures, Download, DCIM and USB drives.

## Code

| File | What it does |
|---|---|
| `HomeActivity.java` | Home screen, dock, app grid, menus, network status |
| `AppStore.java` | Installed apps and lazily loaded icons |
| `DockStore.java` | Dock order, saved in SharedPreferences |
| `Wallpaper.java` | Crops photos to the screen; draws the blurred default |
| `Tiles.java` | Glass tiles and their focus animation |
| `NetworkIcon.java` | Wi-Fi / Ethernet / offline icon, drawn in code |
| `build.sh` | aapt2 → javac → d8 → zipalign → apksigner |
