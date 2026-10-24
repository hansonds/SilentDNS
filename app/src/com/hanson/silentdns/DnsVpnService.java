package com.hanson.silentdns;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;

import java.io.FileDescriptor;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Lightweight DNS-only VPN. Only traffic to two private "virtual" DNS
 * addresses enters the tunnel; everything else goes out normally.
 * DNS queries are forwarded to the user's primary/secondary servers
 * (each falls back to the other on failure).
 * No notification is created.
 */
public class DnsVpnService extends VpnService implements Runnable {

    public static final String ACTION_START = "com.hanson.silentdns.START";
    public static final String ACTION_STOP = "com.hanson.silentdns.STOP";
    public static final String ACTION_BOOT = "com.hanson.silentdns.BOOT";
    private static final int FG_ID = 11;

    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private int retries = 0;

    static final String PREFS = "silentdns";
    static final String K_ENABLED = "enabled";
    static final String K_PRIMARY = "primary";
    static final String K_SECONDARY = "secondary";
    static final String DEF_PRIMARY = "1.1.1.1";
    static final String DEF_SECONDARY = "8.8.8.8";

    // Virtual DNS addresses inside the tunnel
    private static final byte[] VIRT_1 = {10, 111, (byte) 222, 2};
    private static final byte[] VIRT_2 = {10, 111, (byte) 222, 3};

    public static volatile boolean running = false;

    private ParcelFileDescriptor tun;
    private Thread worker;
    private ExecutorService pool;
    private FileOutputStream out;
    private final Object writeLock = new Object();
    private FileDescriptor[] wake;   // pipe used to wake the poll loop on stop
    private volatile InetAddress primary, secondary;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_BOOT.equals(action)) goForeground(); // required when started at boot
        SharedPreferences sp = getSharedPreferences(PREFS, MODE_PRIVATE);

        if (ACTION_STOP.equals(action)) {
            stopVpn();
            stopSelf();
            return START_NOT_STICKY;
        }
        // Explicit start, always-on start (SERVICE_INTERFACE) or sticky restart (null)
        if (action == null || SERVICE_INTERFACE.equals(action)) {
            if (SERVICE_INTERFACE.equals(action)) {
                sp.edit().putBoolean(K_ENABLED, true).apply();
            } else if (!sp.getBoolean(K_ENABLED, false)) {
                stopSelf();
                return START_NOT_STICKY;
            }
        }
        retries = 0;
        noDoh.clear();
        answered.set(0); failed.set(0); encrypted.set(0);
        startVpn(sp);
        return START_STICKY;
    }

    /** Briefly promote to foreground (needed for boot start), removed as soon as DNS is up. */
    private void goForeground() {
        if (Build.VERSION.SDK_INT < 26) return;
        try {
            android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            android.app.NotificationChannel ch = new android.app.NotificationChannel("s", "Background",
                    android.app.NotificationManager.IMPORTANCE_MIN);
            ch.setShowBadge(false);
            ch.setLockscreenVisibility(android.app.Notification.VISIBILITY_SECRET);
            nm.createNotificationChannel(ch);
            android.app.Notification n = new android.app.Notification.Builder(this, "s")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle(getString(R.string.app_name))
                    .setVisibility(android.app.Notification.VISIBILITY_SECRET)
                    .build();
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(FG_ID, n, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
            } else {
                startForeground(FG_ID, n);
            }
        } catch (Exception ignored) {}
    }

    private void leaveForeground() {
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
    }

    private synchronized void startVpn(SharedPreferences sp) {
        try {
            primary = InetAddress.getByName(sp.getString(K_PRIMARY, DEF_PRIMARY));
            secondary = InetAddress.getByName(sp.getString(K_SECONDARY, DEF_SECONDARY));
        } catch (Exception e) {
            leaveForeground();
            return;
        }
        if (tun != null) { running = true; return; } // already up, new servers applied

        try {
            Builder b = new Builder();
            b.setSession(getString(R.string.app_name));
            b.addAddress("10.111.222.1", 24);
            b.addDnsServer(InetAddress.getByAddress(VIRT_1));
            b.addDnsServer(InetAddress.getByAddress(VIRT_2));
            b.addRoute(InetAddress.getByAddress(VIRT_1), 32);
            b.addRoute(InetAddress.getByAddress(VIRT_2), 32);
            b.setMtu(4500);
            b.setBlocking(true);
            try { b.addDisallowedApplication(getPackageName()); } catch (Exception ignored) {}
            if (Build.VERSION.SDK_INT >= 29) b.setMetered(false);
            tun = b.establish();
        } catch (Exception e) {
            tun = null;
        }
        if (tun == null) {
            running = false;
            // At boot the network stack may not be ready yet: retry a few times
            if (retries++ < 10) {
                final SharedPreferences fsp = sp;
                main.postDelayed(new Runnable() {
                    @Override public void run() {
                        if (fsp.getBoolean(K_ENABLED, false)) startVpn(fsp);
                    }
                }, 3000);
            } else {
                leaveForeground();
            }
            return;
        }
        leaveForeground();

        try { wake = Os.pipe(); } catch (Exception e) { wake = null; }
        out = new FileOutputStream(tun.getFileDescriptor());
        ThreadPoolExecutor tp = new ThreadPoolExecutor(6, 6, 30, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(256), new ThreadPoolExecutor.DiscardPolicy());
        tp.allowCoreThreadTimeOut(true); // threads exit when idle
        pool = tp;
        running = true;
        worker = new Thread(this, "silentdns");
        worker.setDaemon(true);
        worker.start();
    }

    private synchronized void stopVpn() {
        running = false;
        main.removeCallbacksAndMessages(null);
        leaveForeground();
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(K_ENABLED, false).apply();
        closeTun();
    }

    private synchronized void closeTun() {
        running = false;
        if (pool != null) { pool.shutdownNow(); pool = null; }
        FileDescriptor[] w = wake;
        if (w != null) { try { Os.write(w[1], new byte[]{1}, 0, 1); } catch (Exception ignored) {} }
        Thread t = worker;
        worker = null;
        if (t != null && t != Thread.currentThread()) {
            try { t.join(1500); } catch (InterruptedException ignored) {}
        }
        releaseFds();
    }

    private synchronized void releaseFds() {
        out = null;
        if (tun != null) { try { tun.close(); } catch (Exception ignored) {} tun = null; }
        if (wake != null) {
            try { Os.close(wake[0]); } catch (Exception ignored) {}
            try { Os.close(wake[1]); } catch (Exception ignored) {}
            wake = null;
        }
    }

    @Override
    public void onRevoke() { // another VPN took over or user revoked
        stopVpn();
        stopSelf();
    }

    @Override
    public void onDestroy() {
        running = false;
        closeTun();
        super.onDestroy();
    }

    // ---- packet loop ----
    @Override
    public void run() {
        ParcelFileDescriptor t = tun;
        FileDescriptor[] w = wake;
        if (t == null) return;
        FileDescriptor fd = t.getFileDescriptor();
        FileInputStream in = new FileInputStream(fd);
        byte[] buf = new byte[32767];
        StructPollfd pt = new StructPollfd();
        pt.fd = fd;
        pt.events = (short) OsConstants.POLLIN;
        StructPollfd pw = null;
        if (w != null) {
            pw = new StructPollfd();
            pw.fd = w[0];
            pw.events = (short) OsConstants.POLLIN;
        }
        StructPollfd[] set = pw != null ? new StructPollfd[]{pt, pw} : new StructPollfd[]{pt};
        while (running) {
            try {
                Os.poll(set, pw != null ? -1 : 1000); // no idle wake-ups when pipe exists
            } catch (Exception e) {
                if (!running) break;
                continue;
            }
            if (!running || (pw != null && pw.revents != 0)) break;
            if ((pt.revents & OsConstants.POLLIN) == 0) {
                if ((pt.revents & (OsConstants.POLLERR | OsConstants.POLLHUP | OsConstants.POLLNVAL)) != 0) break;
                continue;
            }
            int n;
            try {
                n = in.read(buf);
            } catch (Exception e) {
                break;
            }
            if (n > 0) handlePacket(buf, n);
        }
    }

    private void handlePacket(byte[] p, int len) {
        if (len < 28 || (p[0] >> 4) != 4) return;           // IPv4 only
        int ihl = (p[0] & 0x0F) * 4;
        if (p[9] != 17 || len < ihl + 8) return;            // UDP only
        int dstPort = ((p[ihl + 2] & 0xFF) << 8) | (p[ihl + 3] & 0xFF);
        if (dstPort != 53) return;
        boolean second = p[19] == VIRT_2[3];
        int payloadLen = len - ihl - 8;
        if (payloadLen <= 0) return;

        final byte[] srcIp = {p[12], p[13], p[14], p[15]};
        final byte[] dstIp = {p[16], p[17], p[18], p[19]};
        final int srcPort = ((p[ihl] & 0xFF) << 8) | (p[ihl + 1] & 0xFF);
        final byte[] query = new byte[payloadLen];
        System.arraycopy(p, ihl + 8, query, 0, payloadLen);
        final InetAddress first = second ? secondary : primary;
        final InetAddress fallback = second ? primary : secondary;

        ExecutorService ex = pool;
        if (ex == null) return;
        try {
            ex.execute(new Runnable() {
                @Override public void run() {
                    byte[] resp = ask(first, query);
                    if (resp == null && fallback != null && !fallback.equals(first)) resp = ask(fallback, query);
                    if (resp != null) { answered.incrementAndGet(); reply(dstIp, srcIp, srcPort, resp); }
                    else failed.incrementAndGet();
                }
            });
        } catch (Exception ignored) {}
    }

    // ---- stats shown in the app ----
    public static final java.util.concurrent.atomic.AtomicInteger answered = new java.util.concurrent.atomic.AtomicInteger();
    public static final java.util.concurrent.atomic.AtomicInteger failed = new java.util.concurrent.atomic.AtomicInteger();
    public static final java.util.concurrent.atomic.AtomicInteger encrypted = new java.util.concurrent.atomic.AtomicInteger();
    private static final java.util.Set<String> noDoh =
            java.util.Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String, Boolean>());

    private byte[] ask(InetAddress server, byte[] query) {
        if (server == null) return null;
        byte[] r = doh(server, query);
        if (r != null) { encrypted.incrementAndGet(); return r; }
        return udp(server, query);
    }

    /** DNS-over-HTTPS (RFC 8484) straight to the server IP. ISPs cannot intercept or alter it. */
    private byte[] doh(InetAddress server, byte[] query) {
        String host = server.getHostAddress();
        if (noDoh.contains(host)) return null;
        javax.net.ssl.HttpsURLConnection c = null;
        try {
            c = (javax.net.ssl.HttpsURLConnection) new java.net.URL("https://" + host + "/dns-query").openConnection();
            c.setConnectTimeout(3000);
            c.setReadTimeout(3000);
            c.setUseCaches(false);
            c.setDoOutput(true);
            c.setRequestMethod("POST");
            c.setRequestProperty("Content-Type", "application/dns-message");
            c.setRequestProperty("Accept", "application/dns-message");
            c.setFixedLengthStreamingMode(query.length);
            java.io.OutputStream os = c.getOutputStream();
            os.write(query);
            os.close();
            if (c.getResponseCode() != 200) { noDoh.add(host); return null; }
            java.io.InputStream is = c.getInputStream();
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream(512);
            byte[] b = new byte[2048];
            int n;
            while ((n = is.read(b)) > 0) bo.write(b, 0, n);
            is.close();
            byte[] r = bo.toByteArray();
            return r.length >= 12 ? r : null;
        } catch (java.net.ConnectException e) {
            noDoh.add(host);      // server has no HTTPS DNS: use normal DNS for it
            return null;
        } catch (javax.net.ssl.SSLException e) {
            noDoh.add(host);
            return null;
        } catch (Exception e) {
            return null;
        } finally {
            if (c != null) { try { c.getInputStream().close(); } catch (Exception ignored) {} }
        }
    }

    private byte[] udp(InetAddress server, byte[] query) {
        DatagramSocket s = null;
        try {
            s = new DatagramSocket();
            protect(s);
            s.setSoTimeout(2500);
            s.send(new DatagramPacket(query, query.length, server, 53));
            byte[] rb = new byte[4096];
            DatagramPacket rp = new DatagramPacket(rb, rb.length);
            s.receive(rp);
            byte[] r = new byte[rp.getLength()];
            System.arraycopy(rb, 0, r, 0, r.length);
            return r;
        } catch (Exception e) {
            return null;
        } finally {
            if (s != null) s.close();
        }
    }

    private void reply(byte[] from, byte[] to, int toPort, byte[] data) {
        int total = 28 + data.length;
        byte[] pk = new byte[total];
        pk[0] = 0x45;
        pk[2] = (byte) (total >> 8); pk[3] = (byte) total;
        pk[6] = 0x40;               // don't fragment
        pk[8] = 64;                 // TTL
        pk[9] = 17;                 // UDP
        System.arraycopy(from, 0, pk, 12, 4);
        System.arraycopy(to, 0, pk, 16, 4);
        int sum = 0;
        for (int i = 0; i < 20; i += 2) sum += ((pk[i] & 0xFF) << 8) | (pk[i + 1] & 0xFF);
        while ((sum >> 16) != 0) sum = (sum & 0xFFFF) + (sum >> 16);
        sum = ~sum;
        pk[10] = (byte) (sum >> 8); pk[11] = (byte) sum;
        // UDP header (checksum 0 = none, valid for IPv4)
        pk[20] = 0; pk[21] = 53;
        pk[22] = (byte) (toPort >> 8); pk[23] = (byte) toPort;
        int ulen = 8 + data.length;
        pk[24] = (byte) (ulen >> 8); pk[25] = (byte) ulen;
        System.arraycopy(data, 0, pk, 28, data.length);
        FileOutputStream o = out;
        if (o == null) return;
        synchronized (writeLock) {
            try { o.write(pk); } catch (Exception ignored) {}
        }
    }
}
