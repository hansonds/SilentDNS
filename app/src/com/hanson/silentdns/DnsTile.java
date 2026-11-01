package com.hanson.silentdns;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.net.VpnService;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

/** Quick settings tile (next to Wi-Fi, Bluetooth...) to turn SilentDNS on or off. */
public class DnsTile extends TileService {

    /** Ask Android to redraw the tile after the state changed somewhere else. */
    public static void refresh(Context c) {
        try {
            TileService.requestListeningState(c, new ComponentName(c, DnsTile.class));
        } catch (Exception ignored) {}
    }

    @Override
    public void onStartListening() {
        draw(DnsVpnService.running);
    }

    @Override
    public void onClick() {
        if (isLocked()) {
            unlockAndRun(new Runnable() { @Override public void run() { toggle(); } });
        } else {
            toggle();
        }
    }

    private void toggle() {
        SharedPreferences sp = getSharedPreferences(DnsVpnService.PREFS, MODE_PRIVATE);
        try {
            if (DnsVpnService.running) {
                startService(new Intent(this, DnsVpnService.class).setAction(DnsVpnService.ACTION_STOP));
                draw(false);
            } else if (!sp.getBoolean(MainActivity.K_DISCLOSED, false) || VpnService.prepare(this) != null) {
                openApp();   // permission still needed: the app asks for it
            } else {
                sp.edit().putBoolean(DnsVpnService.K_ENABLED, true).apply();
                startService(new Intent(this, DnsVpnService.class).setAction(DnsVpnService.ACTION_START));
                draw(true);
            }
        } catch (Exception e) {
            openApp();
        }
    }

    private void openApp() {
        Intent i = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startActivityAndCollapse(PendingIntent.getActivity(this, 1, i,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE));
            } else {
                startActivityAndCollapse(i);
            }
        } catch (Exception ignored) {}
    }

    private void draw(boolean on) {
        Tile t = getQsTile();
        if (t == null) return;
        t.setState(on ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        t.setLabel(getString(R.string.app_name));
        t.setIcon(Icon.createWithResource(this, R.drawable.tile_icon));
        if (Build.VERSION.SDK_INT >= 29) t.setSubtitle(on ? "On" : "Off");
        t.setContentDescription(on ? "SilentDNS on" : "SilentDNS off");
        t.updateTile();
    }
}
