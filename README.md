# The Diary

An Android diary app with parchment pages, cursive ink that writes itself, S Pen handwriting, and an LLM behind a customizable personality layer.

## Features

- **LLM chat** via OpenAI-compatible APIs or Google Gemini
- **Secure API key storage** with EncryptedSharedPreferences
- **Personality layer** — custom instructions + tone notes (defaults ship with a sample persona in `DEFAULT_INSTRUCTIONS` only)
- **Diary UI** on parchment with cursive + handwriting fonts
- **Slow ink reveal** for AI replies (configurable ms/character)
- **S Pen handwriting** with pressure-aware strokes + on-device ink recognition, plus **typing**
- **Scroll canvas** and **vanishing** conversation modes
- **Export scroll conversations** as PDF via the system share sheet (e.g. Samsung Notes)

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

A local OpenAI-compatible mock answers in a generic diary voice:

```bash
python3 scripts/diary_test_api.py
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
  -d '{"model":"diary-test","messages":[{"role":"user","content":"Who are you?"}]}'
```

## First-run setup on device

1. Install the APK (Android 9+)
2. Open **Settings** (gear) → paste your OpenAI-compatible or Google Gemini API key, **or** use the local test API
3. Optionally open **Personality layer** to customize (or replace) the default instructions
4. Choose **Scroll** or **Vanish**, then **S Pen** or **Type**
5. Write or type, tap **Seal & Send**

## Notes

- Handwriting is converted to text on-device before being sent to the LLM.
- Vanishing mode clears the conversation after the ink fades.
- No API key ever leaves the device except as a request Authorization header to the provider you configure.
- Product UI and branding stay generic; franchise-specific identity lives only in the editable default custom instructions.
