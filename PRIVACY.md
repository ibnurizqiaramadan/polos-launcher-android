# Polos privacy policy

Last updated: 5 October 2026

Polos is a home screen launcher for Android. It does not collect, store or share any personal data. It has no accounts, no analytics, no advertising and no crash reporting. Everything it shows stays on your phone.

The source code is public, so every claim below can be checked: https://github.com/ibnurizqiaramadan/polos-launcher-android

## Data that leaves your phone

Only one thing: if you turn on weather, Polos sends your approximate location (coordinates rounded to about 1 km, nothing else) to Open-Meteo (https://open-meteo.com) to fetch the forecast. Open-Meteo is a free weather service that does not require an account or API key. Its privacy policy applies to that request. Weather is off until you grant the location permission, and can be turned off again at any time in Settings.

Nothing else is ever sent anywhere. Polos makes no other network requests.

## Permissions and what each is used for

All permissions are optional. Polos works without any of them; you only lose the related feature.

- Approximate location: the weather forecast, as described above. The location is not stored.
- Calendar (read): to show today's events and the next upcoming one on the home screen. Events are read from the phone's calendar provider and are not stored or sent anywhere.
- Usage access: to show today's screen time and your recently used apps in the app drawer. Usage data is read from the system and is not stored or sent anywhere.
- Notification access: to show what is currently playing and offer play, pause and skip buttons (through the system's media session API), and to put a dot next to apps that have notifications. For the dots Polos only looks at which app posted a notification and what kind it is (ongoing, media, or one the app allows a dot for); it never reads what a notification says, and nothing is stored or sent. Dots can be turned off in Settings.
- Accessibility service: to provide navigation gestures on phones that force 3-button navigation (swipe in from an edge for Back, swipe up from the bottom for Home, swipe up and hold for Recents) and to lock the screen on double-tap. The service only places invisible touch strips along the screen edges and triggers those system actions. It does not read, observe or record anything on your screen, and it receives no accessibility events.
- Internet: only for the Open-Meteo weather request.
- Set alarm and expand status bar: to open the clock app from the time and to open the notification shade on swipe down.

## Data stored on your phone

Your preferences (favorites, hidden apps, renamed labels, wallpaper and settings) are stored in the app's private storage on your phone. They are removed when you uninstall Polos. They are never backed up to or synced with any server by Polos.

## Children

Polos does not collect data from anyone, including children.

## Changes

If this policy changes, the new version will be published at the same address with an updated date.

## Contact

Open an issue at https://github.com/ibnurizqiaramadan/polos-launcher-android/issues
