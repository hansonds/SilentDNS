<p align="center"><img src="icon.png" width="96" alt="SilentDNS icon"></p>

<h1 align="center">SilentDNS</h1>

<p align="center">A tiny (~50 KB) Android app that sets a custom DNS for your whole phone — quietly, in the background.</p>

## Features

- **Primary + secondary DNS.** If one server doesn't answer, the other is used automatically.
- **Encrypted DNS (DNS-over-HTTPS).** Lookups are sent over HTTPS, so your mobile network can't see or change them. Falls back to normal DNS for servers that don't support it.
- **Runs silently.** No notification. It keeps running until you open the app and tap **Turn off**.
- **Starts after reboot.** Also supports Android's *Always-on VPN* setting.
- **One-tap presets.** Cloudflare + Google (default), Cloudflare, Google, Quad9, AdGuard.
- **Live counter.** Shows answered, encrypted and failed lookups.
- **No logs, no tracking.** Nothing about the sites you visit is saved or sent anywhere except to the DNS servers you choose.
- **Light.** No background polling and no extra libraries. Only DNS goes through the app; all other traffic goes out normally.

## Download

Get the latest APK from the [**Releases**](../../releases/latest) page.

Android 7.0 or newer is required.

Privacy policy: <https://hansonds.github.io/SilentDNS/privacy.html>

## Install and set up

1. Download `SilentDNS.apk` and open it. Allow *Install unknown apps* if asked.
2. If **Google Play Protect** warns *"Harmful app blocked"*, tap **Install anyway**. It warns about every app that uses a VPN and isn't from the Play Store. The full source code is in this repo.
3. Open SilentDNS, pick a preset or type your own servers, and tap **TURN ON**. Accept the connection request.
4. For the most reliable background running:
   - App info → Battery → **Unrestricted**
   - Tap **Auto-start after reboot** in the app → gear icon next to SilentDNS → **Always-on VPN**

## Good to know

- Android always shows a small **🔑 key icon** in the status bar while any app changes DNS for the whole phone. No normal app can hide it.
- If **Private DNS** in your phone settings is set to a hostname, it overrides this app. Set it to *Off* or *Automatic*.
- In Chrome, set **Settings → Privacy → Use secure DNS** to *With your current service provider*, so Chrome uses the app's DNS.
- To change servers, turn the app off, edit them, and turn it on again.

## How it works

SilentDNS uses Android's `VpnService` to create a tunnel that only carries traffic sent to two private virtual DNS addresses. Each DNS query read from the tunnel is forwarded to your primary server. It goes over DNS-over-HTTPS first (`https://<server>/dns-query`, RFC 8484) and falls back to plain UDP. If that fails, the query goes to the secondary server. The reply is written back into the tunnel.

| File | Purpose |
|---|---|
| `app/src/.../DnsVpnService.java` | The tunnel, DNS forwarding, DoH, retries |
| `app/src/.../MainActivity.java` | The screen |
| `app/src/.../BootReceiver.java` | Restart after reboot or app update |

## Build from source

**On GitHub (automatic):** go to **Actions → Build APK → Run workflow**, enter a version such as `1.3`, and run it. GitHub builds and signs the APK (for sideloading) and the AAB (for Google Play) and publishes both as a new Release. See [`store/PLAY_STORE.md`](store/PLAY_STORE.md) for the Play Store steps. Pushing a tag like `v1.3` does the same.

This needs three repository secrets (**Settings → Secrets and variables → Actions**):

| Secret | Value |
|---|---|
| `KEYSTORE_BASE64` | The signing keystore, base64-encoded |
| `KS_PASS` | Keystore password |
| `KS_ALIAS` | Key alias |

**On your own computer:** the app has no Gradle setup and no dependencies, only the Android SDK:

```bash
KS=my.jks KS_PASS=mypassword KS_ALIAS=mykey BUNDLETOOL=bundletool.jar tools/build.sh
```
