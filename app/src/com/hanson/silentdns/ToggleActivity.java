package com.hanson.silentdns;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.widget.Toast;

/**
 * Invisible screen opened by the widget. Running in the foreground lets it
 * start or stop the DNS service on every Android version; it closes at once.
 */
public class ToggleActivity extends Activity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        SharedPreferences sp = getSharedPreferences(DnsVpnService.PREFS, MODE_PRIVATE);
        try {
            if (DnsVpnService.running) {
                startService(new Intent(this, DnsVpnService.class).setAction(DnsVpnService.ACTION_STOP));
                Toast.makeText(this, "SilentDNS off", Toast.LENGTH_SHORT).show();
            } else if (!sp.getBoolean(MainActivity.K_DISCLOSED, false) || VpnService.prepare(this) != null) {
                // First use, or VPN permission not given yet: the app asks for it
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                Toast.makeText(this, "Tap TURN ON once to give permission", Toast.LENGTH_LONG).show();
            } else {
                sp.edit().putBoolean(DnsVpnService.K_ENABLED, true).apply();
                startService(new Intent(this, DnsVpnService.class).setAction(DnsVpnService.ACTION_START));
                Toast.makeText(this, "SilentDNS on", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        finish();
        overridePendingTransition(0, 0);
    }
}
