package com.hanson.silentdns;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

/** 1x1 home-screen button: tap to turn SilentDNS on or off. */
public class ToggleWidget extends AppWidgetProvider {

    @Override
    public void onUpdate(Context c, AppWidgetManager m, int[] ids) {
        draw(c, m, ids);
    }

    /** Redraw every SilentDNS widget with the current on/off state. */
    public static void refresh(Context c) {
        DnsTile.refresh(c);   // keep the quick settings tile in step too
        try {
            AppWidgetManager m = AppWidgetManager.getInstance(c);
            int[] ids = m.getAppWidgetIds(new ComponentName(c, ToggleWidget.class));
            if (ids != null && ids.length > 0) draw(c, m, ids);
        } catch (Exception ignored) {}
    }

    private static void draw(Context c, AppWidgetManager m, int[] ids) {
        boolean on = DnsVpnService.running;
        RemoteViews v = new RemoteViews(c.getPackageName(), R.layout.widget_toggle);
        v.setInt(R.id.widget_root, "setBackgroundResource", on ? R.drawable.widget_bg_on : R.drawable.widget_bg_off);
        v.setImageViewResource(R.id.widget_icon, on ? R.drawable.widget_icon_on : R.drawable.widget_icon_off);
        v.setTextViewText(R.id.widget_label, on ? "ON" : "OFF");
        v.setTextColor(R.id.widget_label, on ? 0xFFFFFFFF : 0xFF8B949E);
        v.setContentDescription(R.id.widget_root, on ? "SilentDNS is on. Tap to turn off." : "SilentDNS is off. Tap to turn on.");

        Intent tap = new Intent(c, ToggleActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION
                        | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        PendingIntent pi = PendingIntent.getActivity(c, 0, tap,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_root, pi);
        m.updateAppWidget(ids, v);
    }
}
