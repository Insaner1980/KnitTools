package android.app;

/** Supplies the process name used by Glance when the host Android stubs report API 0. */
public final class ActivityThread {
    private ActivityThread() {}

    public static String currentProcessName() {
        return "com.finnvek.knittools.jvmtest";
    }
}
