# Device deploy

adb not on PATH here. Use SDK copy:

```bash
ADB="/home/toonzzzrock/900-Setup/Android Studio/platform-tools/adb"
```

## Check connected phone

```bash
"$ADB" devices -l
```
Daemon auto-starts if down. Expect one `device` line (unauthorized = accept USB debug prompt on phone; offline = replug cable).

## Push / update app

```bash
cd App
./build.sh installDebug
```
Builds debug APK, installs over adb to whatever `adb devices` lists. Re-run anytime after code changes — Gradle only rebuilds what changed.

## Other useful gradlew targets

```bash
./build.sh uninstallDebug   # remove app from phone
./build.sh assembleDebug    # build APK only, no install
```

## Logs from phone

```bash
"$ADB" logcat | grep -i <tag>
```
