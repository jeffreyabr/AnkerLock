# AnkerLock

One-tap Quick Settings tile that disables touch controls on the Soundcore V30i
by sending `08ee00000001010a0002` over Bluetooth Classic RFCOMM channel 6.

Config lives at the top of `app/src/main/java/dev/budlock/SoundcoreLock.kt`
(MAC, channel, optional SERVICE_UUID, payload).

Build: push to GitHub → Actions → "Build APK" → download the BudLock-apk artifact.
