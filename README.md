# Riddle's Diary

An Android diary app inspired by Tom Riddle's diary — parchment pages, cursive ink that writes itself, S Pen handwriting, and an LLM that answers as Tom Riddle.

## Features

- **Tom Riddle persona** via OpenAI-compatible APIs or Google Gemini
- **Secure API key storage** with EncryptedSharedPreferences
- **Personality layer** (Gemini Gems–style custom instructions + tone notes)
- **S Pen handwriting** (Galaxy S25 Ultra–oriented): pressure-aware ink, ML Kit recognition
- **Typing mode** as a second input option
- **Scroll canvas mode**: write → AI replies in cursive → scroll and continue
- **Vanishing mode**: reply appears in ink, then fades from the page
- **Slow cursive reveal** using Great Vibes, character-by-character

## Build the APK

```bash
export ANDROID_HOME=$HOME/android-sdk   # or your SDK path
./gradlew :app:assembleDebug
```

Debug APK:

`app/build/outputs/apk/debug/app-debug.apk`

Release (unsigned/minify):

```bash
./gradlew :app:assembleRelease
```

## First-run setup on device

1. Install the APK on a Galaxy S25 Ultra (or any Android 9+ device)
2. Open **Settings** (gear) → paste your OpenAI-compatible or Gemini API key
3. Optionally open **Personality layer** to customize instructions
4. Choose **Scroll** or **Vanish**, then **S Pen** or **Type**
5. Write or type, tap **Seal & Send**

## Notes

- Handwriting is converted to text on-device with ML Kit Digital Ink before being sent to the LLM.
- Vanishing mode clears the conversation after the ink fades.
- No API key ever leaves the device except as a request Authorization header to the provider you configure.
