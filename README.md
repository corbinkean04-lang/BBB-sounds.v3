# BBB Sounds — Quest APK project

This is the GitHub-ready Android project for the BBB Sounds Quest soundboard.

## Included on the main board
These three sounds are bundled inside the APK and are intentionally locked:
1. Trollface Phonk Edit
2. Mr Beast Phonk
3. Snow White Trailer Comments

There is no delete control for these sounds.

## Features
- BBB red / black / white UI
- Landscape / Quest-friendly layout
- Add one or multiple audio files with Android's file picker
- Custom sounds persist after restarting the app
- Play / stop
- Per-sound loop
- Loop All
- Stop All
- Master volume
- Custom sounds can be deleted
- Main-board sounds cannot be deleted
- Quest playback is local to the headset

## Build the APK on GitHub
1. Create a new empty GitHub repository.
2. Upload the contents of this folder to the repository root.
3. Commit to the `main` branch.
4. Open the repository's **Actions** tab.
5. Run **Build BBB Sounds APK** (or push to `main`).
6. Open the completed workflow run.
7. Under **Artifacts**, download `BBB-Sounds-debug-APK`.
8. Extract it and install `app-debug.apk` on the Quest.

The included GitHub Actions workflow installs Gradle and builds the APK automatically, so Gradle does not need to be installed on your PC just to build it on GitHub.

## Important
This project does not inject audio into another game's microphone on Quest. Quest currently does not provide a general application API for virtual-microphone audio injection. The Quest build therefore plays through the headset normally.

The PC build can use a virtual audio cable when game voice-chat routing is required.
