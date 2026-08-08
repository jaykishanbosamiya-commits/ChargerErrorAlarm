# Charger Error Alarm — Online GitHub Build

This project can be built online using GitHub Actions. Android Studio is NOT required on your laptop.

## Easiest method

1. Go to GitHub in your browser and sign in.
2. Create a new repository, for example:
   `ChargerErrorAlarm`
3. Upload all files and folders from this project into the repository.
4. Commit the files to the `main` branch.
5. Open the repository's **Actions** tab.
6. Select **Build Android APK**.
7. Click **Run workflow** if it is not already running.
8. Wait for the build to finish.
9. Open the completed workflow run.
10. At the bottom, under **Artifacts**, download:
    `ChargerErrorAlarm-debug-apk`
11. Extract the downloaded artifact and install `app-debug.apk` on your Android phone.

## If GitHub says Actions are disabled

Open:
Settings → Actions → General → Actions permissions

Allow actions and save.

## What the app does

- Detects charger connection.
- Waits for the configured delay (1–60 seconds).
- Checks Android's charging state.
- If a charger is connected but Android reports that the battery is not charging, it starts the error alarm.
- Alarm continues until charging begins or the charger is disconnected.
- Provides vibration and notification.

## Important

This app uses Android's reported charging state (`BatteryManager.isCharging`). It cannot guarantee detection of every hardware condition where a phone reports "charging" despite very low charging current.

For reliable background operation, some phone manufacturers may require battery-optimization exemption.
