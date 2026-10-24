package com.hanson.silentdns;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.view.WindowInsets;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.VpnService;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQ_VPN = 7;
    private static final int BG = 0xFF0D1117, CARD = 0xFF161B22, TXT = 0xFFE6EDF3,
            MUTED = 0xFF8B949E, ON = 0xFF2EA043, OFF = 0xFF30363D, ACCENT = 0xFF58A6FF;

    private static final String[][] PRESETS = {
            {"Cloudflare + Google", "1.1.1.1", "8.8.8.8"},
            {"Cloudflare", "1.1.1.1", "1.0.0.1"},
            {"Google", "8.8.8.8", "8.8.4.4"},
            {"Quad9", "9.9.9.9", "149.112.112.112"},
            {"AdGuard", "94.140.14.14", "94.140.15.15"},
    };

    private EditText primary, secondary;
    private Button toggle;
    private TextView status;
    private SharedPreferences sp;
    private final Handler h = new Handler();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences(DnsVpnService.PREFS, MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(28), dp(20), dp(20));

        TextView title = text(getString(R.string.app_name), 28, TXT);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);
        TextView sub = text("Custom DNS for your whole phone", 14, MUTED);
        root.addView(sub);

        status = text("", 15, TXT);
        status.setPadding(dp(16), dp(14), dp(16), dp(14));
        status.setBackground(round(CARD, 14));
        LinearLayout.LayoutParams sl = lp();
        sl.topMargin = dp(20);
        root.addView(status, sl);

        root.addView(label("Primary DNS"));
        primary = field(sp.getString(DnsVpnService.K_PRIMARY, DnsVpnService.DEF_PRIMARY));
        root.addView(primary, lp());
        root.addView(label("Secondary DNS"));
        secondary = field(sp.getString(DnsVpnService.K_SECONDARY, DnsVpnService.DEF_SECONDARY));
        root.addView(secondary, lp());

        root.addView(label("Quick presets"));
        LinearLayout row0 = new LinearLayout(this), row1 = new LinearLayout(this), row2 = new LinearLayout(this);
        for (int i = 0; i < PRESETS.length; i++) {
            final String[] p = PRESETS[i];
            Button pb = new Button(this);
            pb.setText(p[0]);
            pb.setAllCaps(false);
            pb.setTextColor(ACCENT);
            pb.setTextSize(14);
            pb.setBackground(round(CARD, 12));
            pb.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {
                    primary.setText(p[1]);
                    secondary.setText(p[2]);
                    if (DnsVpnService.running) applyNow(p[0]);
                }
            });
            LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0, dp(46), 1f);
            bl.setMargins(dp(4), dp(4), dp(4), dp(4));
            (i == 0 ? row0 : i < 3 ? row1 : row2).addView(pb, bl);
        }
        root.addView(row0, lp());
        root.addView(row1, lp());
        root.addView(row2, lp());

        applyBtn = new Button(this);
        applyBtn.setText("APPLY NEW DNS");
        applyBtn.setTextColor(Color.WHITE);
        applyBtn.setTextSize(15);
        applyBtn.setTypeface(Typeface.DEFAULT_BOLD);
        applyBtn.setBackground(round(0xFF1F6FEB, 14));
        applyBtn.setVisibility(View.GONE);
        applyBtn.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { applyNow(null); }
        });
        LinearLayout.LayoutParams abl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(50));
        abl.topMargin = dp(16);
        root.addView(applyBtn, abl);
        android.text.TextWatcher tw = new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence c, int a, int b, int d) {}
            public void onTextChanged(CharSequence c, int a, int b, int d) {}
            public void afterTextChanged(android.text.Editable e) { updateApply(); }
        };
        primary.addTextChangedListener(tw);
        secondary.addTextChangedListener(tw);

        toggle = new Button(this);
        toggle.setTextColor(Color.WHITE);
        toggle.setTextSize(18);
        toggle.setTypeface(Typeface.DEFAULT_BOLD);
        toggle.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { onToggle(); }
        });
        LinearLayout.LayoutParams tl = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
        tl.topMargin = dp(24);
        root.addView(toggle, tl);

        Button always = new Button(this);
        always.setText("Auto-start after reboot (Always-on)");
        always.setAllCaps(false);
        always.setTextColor(MUTED);
        always.setTextSize(13);
        always.setBackgroundColor(Color.TRANSPARENT);
        always.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_VPN_SETTINGS));
                    Toast.makeText(MainActivity.this,
                            "Tap the gear next to SilentDNS → turn on Always-on VPN",
                            Toast.LENGTH_LONG).show();
                } catch (Exception ignored) {}
            }
        });
        LinearLayout.LayoutParams al = lp();
        al.topMargin = dp(10);
        root.addView(always, al);

        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        sv.setFillViewport(true);
        sv.addView(root);
        // Android 15+ draws apps edge-to-edge: keep content clear of status bar, nav bar and keyboard
        final int pad = dp(20), padTop = dp(28);
        final LinearLayout fRoot = root;
        sv.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override public WindowInsets onApplyWindowInsets(View v, WindowInsets in) {
                int l, t, r, btm;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets i = in.getInsets(WindowInsets.Type.systemBars()
                            | WindowInsets.Type.displayCutout() | WindowInsets.Type.ime());
                    l = i.left; t = i.top; r = i.right; btm = i.bottom;
                } else {
                    l = in.getSystemWindowInsetLeft(); t = in.getSystemWindowInsetTop();
                    r = in.getSystemWindowInsetRight(); btm = in.getSystemWindowInsetBottom();
                }
                fRoot.setPadding(pad + l, padTop + t, pad + r, pad + btm);
                return in;
            }
        });
        setContentView(sv);
    }

    @Override
    protected void onResume() {
        super.onResume();
        h.removeCallbacks(ticker);
        h.post(ticker);
    }

    private void onToggle() {
        if (DnsVpnService.running) {
            startService(new Intent(this, DnsVpnService.class).setAction(DnsVpnService.ACTION_STOP));
            later();
            return;
        }
        String p = primary.getText().toString().trim();
        String s = secondary.getText().toString().trim();
        if (!validIp(p)) { primary.setError("Enter a valid IPv4 address"); return; }
        if (s.length() == 0) s = p;
        if (!validIp(s)) { secondary.setError("Enter a valid IPv4 address"); return; }
        sp.edit().putString(DnsVpnService.K_PRIMARY, p).putString(DnsVpnService.K_SECONDARY, s).apply();

        if (!sp.getBoolean(K_DISCLOSED, false)) { showDisclosure(); return; }
        askVpn();
    }

    private static final String K_DISCLOSED = "disclosed_v1";

    /** Prominent disclosure required by Google Play for apps that use VpnService. */
    private void showDisclosure() {
        new AlertDialog.Builder(this, android.R.style.Theme_DeviceDefault_Dialog_Alert)
                .setTitle("How SilentDNS uses VPN")
                .setMessage("SilentDNS uses Android's VPN feature only to send your phone's DNS lookups "
                        + "(the website names your apps ask for) to the DNS servers you choose.\n\n"
                        + "• Only DNS goes through it. All other internet traffic goes out normally.\n"
                        + "• Lookups are encrypted (HTTPS) when the server supports it.\n"
                        + "• Nothing is logged, stored, or sent to the developer or anyone else.\n"
                        + "• No ads, no tracking, no account.\n\n"
                        + "Tap Agree to continue. Android will then ask for permission to set up the VPN.")
                .setCancelable(false)
                .setPositiveButton("Agree", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        sp.edit().putBoolean(K_DISCLOSED, true).apply();
                        askVpn();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askVpn() {
        Intent ask = VpnService.prepare(this);
        if (ask != null) startActivityForResult(ask, REQ_VPN);
        else onActivityResult(REQ_VPN, RESULT_OK, null);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        if (req != REQ_VPN) return;
        if (res == RESULT_OK) {
            sp.edit().putBoolean(DnsVpnService.K_ENABLED, true).apply();
            startService(new Intent(this, DnsVpnService.class).setAction(DnsVpnService.ACTION_START));
            later();
        } else {
            Toast.makeText(this, "Permission needed to change DNS", Toast.LENGTH_SHORT).show();
        }
    }

    private Button applyBtn;

    /** Shows "Apply" only while running and the typed servers differ from the active ones. */
    private void updateApply() {
        if (applyBtn == null) return;
        boolean changed = !primary.getText().toString().trim().equals(sp.getString(DnsVpnService.K_PRIMARY, ""))
                || !secondary.getText().toString().trim().equals(sp.getString(DnsVpnService.K_SECONDARY, ""));
        applyBtn.setVisibility(DnsVpnService.running && changed ? View.VISIBLE : View.GONE);
    }

    /** Switch DNS servers immediately while running (no turn off / on needed). */
    private void applyNow(String presetName) {
        String p = primary.getText().toString().trim();
        String s = secondary.getText().toString().trim();
        if (!validIp(p)) { primary.setError("Enter a valid IPv4 address"); return; }
        if (s.length() == 0) { s = p; secondary.setText(s); }
        if (!validIp(s)) { secondary.setError("Enter a valid IPv4 address"); return; }
        sp.edit().putString(DnsVpnService.K_PRIMARY, p).putString(DnsVpnService.K_SECONDARY, s).commit();
        startService(new Intent(this, DnsVpnService.class).setAction(DnsVpnService.ACTION_APPLY));
        android.view.inputmethod.InputMethodManager im =
                (android.view.inputmethod.InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (im != null) im.hideSoftInputFromWindow(primary.getWindowToken(), 0);
        Toast.makeText(this, "Now using " + (presetName != null ? presetName : p + " / " + s),
                Toast.LENGTH_SHORT).show();
        updateApply();
        refresh();
    }

    private void later() {
        h.postDelayed(new Runnable() { @Override public void run() { refresh(); } }, 500);
    }

    private void refresh() {
        boolean on = DnsVpnService.running;
        updateApply();
        toggle.setText(on ? "TURN OFF" : "TURN ON");
        toggle.setBackground(round(on ? 0xFFB62324 : ON, 16));
        status.setText(on
                ? "●  Active — using " + sp.getString(DnsVpnService.K_PRIMARY, "") + " / " + sp.getString(DnsVpnService.K_SECONDARY, "")
                : "○  Off — phone is using its normal DNS");
        if (on) {
            status.append("\n\nAnswered: " + DnsVpnService.answered.get()
                    + "   (encrypted: " + DnsVpnService.encrypted.get() + ")"
                    + "\nFailed: " + DnsVpnService.failed.get());
        }
        status.setTextColor(on ? ON : MUTED);
    }

    private final Runnable ticker = new Runnable() {
        @Override public void run() { refresh(); h.postDelayed(this, 2000); }
    };

    @Override
    protected void onPause() {
        super.onPause();
        h.removeCallbacks(ticker);
    }

    static boolean validIp(String ip) {
        String[] parts = ip.split("\\.", -1);
        if (parts.length != 4) return false;
        for (String x : parts) {
            if (x.length() == 0 || x.length() > 3) return false;
            for (int i = 0; i < x.length(); i++) if (!Character.isDigit(x.charAt(i))) return false;
            if (Integer.parseInt(x) > 255) return false;
        }
        return true;
    }

    // ---- small UI helpers ----
    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density + 0.5f); }

    private LinearLayout.LayoutParams lp() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private TextView text(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private TextView label(String s) {
        TextView t = text(s, 13, MUTED);
        t.setPadding(dp(4), dp(18), 0, dp(6));
        return t;
    }

    private EditText field(String v) {
        EditText e = new EditText(this);
        e.setText(v);
        e.setSingleLine(true);
        e.setTextColor(TXT);
        e.setHintTextColor(MUTED);
        e.setTextSize(17);
        e.setGravity(Gravity.CENTER_VERTICAL);
        e.setInputType(InputType.TYPE_CLASS_PHONE);
        e.setPadding(dp(16), dp(14), dp(16), dp(14));
        e.setBackground(round(CARD, 12));
        return e;
    }

    private GradientDrawable round(int color, int r) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(r));
        return g;
    }
}
