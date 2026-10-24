package com.hanson.silentdns;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;

/** Restarts DNS after reboot / app update if it was left ON. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (!c.getSharedPreferences(DnsVpnService.PREFS, Context.MODE_PRIVATE)
                .getBoolean(DnsVpnService.K_ENABLED, false)) return;
        if (VpnService.prepare(c) != null) return; // permission not granted
        try {
            Intent si = new Intent(c, DnsVpnService.class).setAction(DnsVpnService.ACTION_BOOT);
            if (android.os.Build.VERSION.SDK_INT >= 26) c.startForegroundService(si);
            else c.startService(si);
        } catch (Exception ignored) {
            // Some Android versions block this; Always-on VPN covers it.
        }
    }
}
