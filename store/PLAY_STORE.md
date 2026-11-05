# Publishing SilentDNS on Google Play

Everything to copy and paste into Play Console, in the order it asks. Files to upload are in this `store/` folder or on the Releases page.

---

## 0. Before you start

| Item | Where |
|---|---|
| App file | `SilentDNS-vX.Y-playstore.aab` from the latest **Release** (built by GitHub) |
| App icon (512×512) | `store/icon-512.png` |
| Feature graphic (1024×500) | `store/feature-graphic.png` |
| Phone screenshots | Take 2–4 on your phone (see section 3) |
| Privacy policy URL | `https://hansonds.github.io/SilentDNS/privacy.html` |

When Play Console asks about **app signing**, choose **"Use Google-generated key"** (Play App Signing). Your own key (`silentdns.jks`) becomes the **upload key**: GitHub signs every AAB with it, and Google re-signs the app for users.

---

## 1. Create the app

- **App name:** SilentDNS
- **Default language:** English (United States) – en-US
- **App or game:** App
- **Free or paid:** Free (cannot be changed to paid later)

---

## 2. Store listing (Grow → Store presence → Main store listing)

**App name** (max 30)
```
SilentDNS: Private DNS Changer
```

**Short description** (max 80)
```
Change DNS for your whole phone in one tap. Encrypted, no logs, no ads.
```

**Full description** (max 4000)
```
SilentDNS changes the DNS server for your whole phone – on Wi-Fi and mobile data – with one tap. It is tiny, free and quiet: no ads, no account, no tracking.

WHY CHANGE DNS?
DNS turns website names into addresses. The DNS server your network gives you can be slow, can log the sites you visit, and can block or redirect sites. With SilentDNS you choose a faster, more private server instead.

FEATURES
• Primary and secondary DNS – if one doesn't answer, the other is used automatically
• Encrypted lookups (DNS-over-HTTPS) where the server supports it, so your network can't read or change them
• One-tap presets: Cloudflare + Google, Cloudflare, Google, Quad9, AdGuard – or type your own servers
• Switch servers instantly while it's running
• Quick settings button – turn it on or off from the pull-down panel, next to Wi-Fi and Bluetooth
• Home-screen widget – a simple one-tap on/off button
• Runs quietly in the background, with no notification, until you turn it off
• Starts again after a restart, and supports Android's Always-on VPN
• Live counter of answered, encrypted and failed lookups
• Very small and light on battery: no background polling, no extra libraries

PRIVACY FIRST
SilentDNS does not collect, store or share any data. Your lookups go only to the DNS servers you choose. There are no analytics, no ads and no logs. The full source code is public on GitHub.

HOW IT WORKS (VPN USE)
SilentDNS uses Android's VpnService only to receive your phone's DNS lookups and send them to the DNS servers you chose. Only DNS goes through it – all other internet traffic goes directly to the internet, untouched, so your speed is not affected. Android shows a small key icon while it is on; this is normal for any app that changes DNS for the whole phone.

TIPS
• If "Private DNS" in your phone's settings is set to a hostname, set it to Off or Automatic, otherwise it overrides SilentDNS.
• For the most reliable background running, set SilentDNS's battery usage to Unrestricted.
```

**Graphics**
- App icon → `store/icon-512.png`
- Feature graphic → `store/feature-graphic.png`
- Phone screenshots → see section 3

**Category:** Tools
**Contact email:** hanson.d.raj@gmail.com (shown publicly on the listing)
**Website (optional):** `https://github.com/hansonds/SilentDNS`

---

## 3. Screenshots (take on your phone)

You need at least 2 (up to 8). Take them with Power + Volume-down:

1. Main screen, app **off**, with **Cloudflare + Google** selected
2. Main screen, app **on**, after browsing a bit so the counters show numbers
3. The **"How SilentDNS uses VPN"** dialog (appears the first time you tap TURN ON after a fresh install)
4. *(optional)* Android's VPN settings page showing SilentDNS with Always-on VPN

---

## 4. App content (Policy → App content)

**Privacy policy:** `https://hansonds.github.io/SilentDNS/privacy.html`

**Ads:** No, my app does not contain ads.

**App access:** All functionality is available without special access (no login).

**Content rating:** Fill in the questionnaire →
- Category: *All other app types*
- Answer **No** to every question (violence, sexual content, language, controlled substances, gambling, user-generated content, sharing location, buying digital goods…).
- Result will be *Everyone / 3+*.

**Target audience and content:** Ages **18 and over** (simplest — avoids the extra Families rules). Answer **No** to "could the app unintentionally appeal to children".

**News app:** No.

**Health apps:** None.

**Financial features:** None.

**Government app:** No.

**Data safety:**
- Does your app collect or share any of the required user data types? → **No**
- (Play then asks no further collection questions.)
- Is all user data encrypted in transit? → **Yes**
- Do you provide a way for users to request deletion? → *Not applicable / No data collected*

**Foreground service permissions** (asked because of `FOREGROUND_SERVICE_SYSTEM_EXEMPTED`):
- Type: **System exempted** → reason: *VPN app*.
- Description:
  ```
  SilentDNS is a VPN app (VpnService). After the phone restarts, it briefly runs as a foreground service so Android allows the VPN to start again in the background. The service stops being a foreground service as soon as the VPN is running.
  ```
- Video: use the same screen recording as the VPN declaration below.

**VPN service declaration** (Policy → App content → VPN service):
- Is providing a VPN the core functionality of your application? → **No**
- Which permitted functionality does your app provide? → tick **Network related tool (including remote access)** only
- Does your app provide a general VPN service directly to users? → **No** (SilentDNS only changes DNS; it does not tunnel traffic to a remote VPN server. General VPN apps must be published from an organization account.)
- Does your VPN service collect personal and sensitive user data? → **No**
- Does your app redirect or manipulate user traffic from other apps for monetization? → **No**
- Prominent disclosure: shown in-app before the VPN is first started ("How SilentDNS uses VPN" dialog, Agree / Cancel).
- **Video** (upload to YouTube as *Unlisted*, under 90 seconds) – see section 5.

---

## 5. Demo video (for the VPN and foreground-service forms)

Record with Samsung's screen recorder (Quick settings → Screen recorder), about 60 seconds:

1. *(Fresh install – uninstall first, or Settings → Apps → SilentDNS → Storage → Clear data.)*
2. Open SilentDNS. Tap **Cloudflare + Google**.
3. Tap **TURN ON** → the **"How SilentDNS uses VPN"** dialog appears → read it for 2–3 seconds → tap **Agree**.
4. Android's **Connection request** appears → tap **OK**.
5. Show the status: **Active**. Open Chrome, load a couple of sites, go back to SilentDNS and show the counters rising.
6. Tap **TURN OFF**.

Upload to YouTube as **Unlisted** and paste the link.

---

## 6. Testing → Closed testing (required for new personal accounts)

1. **Testing → Closed testing → Create track** (or use "Alpha").
2. **Testers:** create an email list with **at least 12** Gmail addresses (aim for 15 in case some drop out).
3. **Create release** → upload `SilentDNS-vX.Y-playstore.aab` → release notes:
   ```
   First test release of SilentDNS.
   ```
4. Send review → after approval, share the **opt-in link** with testers. Each tester must open it, tap **Become a tester**, install from Play, and open the app.
5. Keep the test running for **14 days in a row**, then go to **Dashboard → Apply for production** and answer the questions about your test.

> Testers who installed your GitHub APK must **uninstall it first** – the Play version is signed by Google with a different key.

---

## 7. Updates later

Release notes for 1.7 (paste into the release-notes box):
```
<en-US>
New in 1.7:
• Quick settings button – turn SilentDNS on or off from the pull-down panel, next to Wi-Fi
• Home-screen widget – a one-tap on/off button
• Change DNS servers instantly while running, no need to turn off and on
• Works on Android 7.0 and newer
</en-US>
```


1. GitHub → **Actions → Build APK → Run workflow** → new version (e.g. `1.4`).
2. Download the new `-playstore.aab` from the Release.
3. Play Console → your track → **Create new release** → upload → roll out.

The version code goes up automatically with every GitHub build, so Play will always accept it.
