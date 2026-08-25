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

## Test without a real LLM key

A local OpenAI-compatible mock answers as Tom Riddle:

```bash
python3 scripts/tom_riddle_test_api.py
```

Then on your phone (USB debugging):

```bash
adb reverse tcp:8787 tcp:8787
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

In the app: **Settings → Connect local test API →** write something → **Seal & Send**.

- USB + `adb reverse`: base URL `http://127.0.0.1:8787/v1/` (what the button sets)
- Emulator: set Base URL to `http://10.0.2.2:8787/v1/`
- API key can be any non-empty string (button uses `test`)

Smoke-check the mock:

```bash
curl -s http://127.0.0.1:8787/v1/chat/completions \
  -H 'Authorization: Bearer test' \
  -H 'Content-Type: application/json' \
  -d '{"model":"tom-riddle-test","messages":[{"role":"user","content":"Who are you?"}]}'
```

## Notes

- Handwriting is converted to text on-device with ML Kit Digital Ink before being sent to the LLM.
- Vanishing mode clears the conversation after the ink fades.
- No API key ever leaves the device except as a request Authorization header to the provider you configure.
