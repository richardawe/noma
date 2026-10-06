package org.noma;

import android.app.Activity;
import android.net.DnsResolver;
import java.util.List;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import java.net.InetAddress;

/** Separate test-APK UID; platform-only Java so it needs no target-app classes. */
public class ResolverProbeActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView text = new TextView(this);
        text.setText("Noma device test: resolving a public test domain");
        setContentView(text);
        if ("revoke".equals(getIntent().getStringExtra("mode"))) {
            finishProbe(android.net.VpnService.prepare(this)==null,"Test APK prepared as replacement VPN");
        } else if (getIntent().getBooleanExtra("async_dns", false)) {
        new Thread(() -> {
            android.net.Network selected="async_explicit".equals(getIntent().getStringExtra("mode")) ? getSystemService(android.net.ConnectivityManager.class).getActiveNetwork() : null;
            java.util.concurrent.Executor executor=command -> new Thread(command,"noma-test-dns-callback").start();
            DnsResolver.Callback<List<InetAddress>> callback=new DnsResolver.Callback<List<InetAddress>>() {
                @Override public void onAnswer(List<InetAddress> answer, int rcode) {
                    finishProbe(rcode == 0 && !answer.isEmpty(), "rcode=" + rcode + ", answers=" + answer.size());
                }
                @Override public void onError(DnsResolver.DnsException error) {
                    android.net.ConnectivityManager cm=getSystemService(android.net.ConnectivityManager.class);
                    android.net.Network active=cm.getActiveNetwork();
                    finishProbe(false,"error="+error.code+", cause="+error.getCause()+" network="+active+" links="+cm.getLinkProperties(active));
                }
            };
            int flags=DnsResolver.FLAG_NO_CACHE_LOOKUP | DnsResolver.FLAG_NO_CACHE_STORE;
            if("async_a".equals(getIntent().getStringExtra("mode"))) DnsResolver.getInstance().query(selected,"www.google-analytics.com",DnsResolver.TYPE_A,flags,executor,null,callback);
            else DnsResolver.getInstance().query(selected,"www.google-analytics.com",flags,executor,null,callback);
        },"noma-test-dns-query").start();
        } else {
            new Thread(() -> {
                try {
                    String mode=getIntent().getStringExtra("mode");
                    if ("https".equals(mode)) {
                        javax.net.ssl.HttpsURLConnection connection=(javax.net.ssl.HttpsURLConnection)new java.net.URL("https://example.com/").openConnection();
                        connection.setConnectTimeout(15000);connection.setReadTimeout(15000);
                        try { int code=connection.getResponseCode();finishProbe(code==200,"HTTPS="+code); }
                        finally { connection.disconnect(); }
                    } else if ("tcp_dns".equals(mode) || "udp_dns".equals(mode)) {
                        java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
                        java.io.DataOutputStream query=new java.io.DataOutputStream(bytes);
                        query.writeShort(421);query.writeShort(256);query.writeShort(1);query.writeShort(0);query.writeShort(0);query.writeShort(0);
                        for(String part:"graph.facebook.com".split("\\.")) { query.writeByte(part.length());query.writeBytes(part); }
                        query.writeByte(0);query.writeShort(1);query.writeShort(1);
                        byte[] payload=bytes.toByteArray();byte[] answer;
                        if ("tcp_dns".equals(mode)) {
                            try(java.net.Socket socket=new java.net.Socket()) {
                                socket.connect(new java.net.InetSocketAddress("10.111.0.1",53),10000);socket.setSoTimeout(15000);
                                java.io.DataOutputStream out=new java.io.DataOutputStream(socket.getOutputStream());out.writeShort(payload.length);out.write(payload);out.flush();
                                java.io.DataInputStream in=new java.io.DataInputStream(socket.getInputStream());answer=new byte[in.readUnsignedShort()];in.readFully(answer);
                            }
                        } else {
                            try(java.net.DatagramSocket socket=new java.net.DatagramSocket()) {
                                socket.setSoTimeout(15000);socket.send(new java.net.DatagramPacket(payload,payload.length,InetAddress.getByName("10.111.0.1"),53));
                                java.net.DatagramPacket response=new java.net.DatagramPacket(new byte[4096],4096);socket.receive(response);answer=java.util.Arrays.copyOf(response.getData(),response.getLength());
                            }
                        }
                        finishProbe(answer.length>=12 && (answer[0]&255)==1 && (answer[1]&255)==165 && (answer[3]&15)==0,mode+" bytes="+answer.length);
                    } else {
                        InetAddress[] answer = InetAddress.getAllByName("www.google-analytics.com");
                        finishProbe(answer.length > 0, "answers=" + answer.length);
                    }
                } catch (Exception error) {
                    finishProbe(false, error.toString());
                }
            }, "noma-test-resolver").start();
        }
    }

    private void finishProbe(boolean success, String detail) {
        sendBroadcast(new Intent("org.noma.test.DNS_RESULT").setPackage("org.noma")
            .putExtra("success", success).putExtra("detail", detail));
        runOnUiThread(this::finish);
    }
}
