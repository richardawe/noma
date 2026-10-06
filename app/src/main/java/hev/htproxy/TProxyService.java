package hev.htproxy;

/** The fixed JNI contract of the pinned upstream HEV forwarding engine. */
public final class TProxyService {
    private TProxyService() {}
    public static native boolean TProxyStartService(String configPath, int fd);
    public static native boolean TProxyStopService();
    public static native boolean TProxyIsRunning();
    public static native long[] TProxyGetStats();
    static { System.loadLibrary("hev-socks5-tunnel"); }
}
