# Pay & Debt

A small native Android app (plain Java, no Kotlin) built with Google's Material 3 components. It answers two questions:

1. **When will I be debt-free?**
2. **How much can I spend this week without missing my debt payment?**

All data stays on your phone. There is no account, no server and no internet permission.

## What it does

- **You owe**: total left, progress bar, several debts supported (the smallest one is suggested first).
- **Debt-free date**: from your weekly debt payment, plus a "pay 100 more a week" what-if.
- **Free to spend**: pay-day rule. The week's debt payment is set aside first; only the rest counts as spendable.
- **Three quick actions**: Got paid, Paid debt, Spent.
- **History**: every entry, tap one to delete it.
- **Streak**: weeks in a row where the debt payment was made.
- **Weekly reminder** on the day and hour you choose.
- **Backup**: share all data as text, restore by pasting it back.

Money is whole dirhams ("dh"). Weeks start on Monday.

## Look and feel

Material 3: tonal cards, bottom navigation bar, outlined text fields, segmented buttons, M3 dialogs and type scale. It follows the phone's light/dark setting. On Android 12+ the colours come from the wallpaper (dynamic colour); on older phones, or if you delete the `DynamicColors` line in `MainActivity`, it uses the green theme in `res/values/colors.xml`.

## Get the APK

Every push to `main` builds the app on GitHub:

- **Releases → "Latest build"** → download `pay-debt-tracker.apk`, or
- **Actions → latest run → Artifacts**.

Open the file on your phone and allow "install from this source" when asked. Installing a newer build over an older one keeps your data.

## Build it yourself

Needs JDK 17, the Android SDK (platform 35, build-tools 35.0.0), Gradle 8.10 and internet access for the one dependency, `com.google.android.material:material:1.12.0`.

```
gradle assembleRelease
```

The APK lands in `app/build/outputs/apk/release/`.

Logic that does not need Android (payoff, budget, streak, formatting) is tested with plain `javac`:

```
./scripts/test-core.sh
```

## About the signing key

`app/debug.keystore` is the standard throw-away key and its password is public (`android`). It exists only so each build is signed the same way and updates install over each other. Do not treat this APK as a store-grade release.

## Privacy and backups

Your numbers live in the app's private storage on the phone. Uninstalling the app or losing the phone deletes them, so use **Settings → Share backup** now and then and send it to yourself. Never commit a backup to this repo.

## Not built yet

- PIN lock
- Importing the data into other apps
