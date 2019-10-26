package si.fullin.wizardapp.ar;
import android.os.Handler;
import android.os.Looper;

class MainThreadContext {
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private static final Looper mainLooper = Looper.getMainLooper();

    public static void runOnUiThread(Runnable runnable){
        if (mainLooper.isCurrentThread()) {
            runnable.run();
        } else {
            mainHandler.post(runnable);
        }
    }
}
