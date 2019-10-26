package si.fullin.wizardapp;

import android.app.Activity;
import android.os.Handler;
import android.widget.Toast;

/**
 * the main service that analyzes data from the wand, uses speech recog service and api service
 */
public class WandService implements UsbSerialService.OnDataRecieved {

    Activity activity;
    OnSpellCast onSpellCastListener;

    UsbSerialService usbSerialService;
    SpeechService speechService;
    ApiService apiService = new ApiService();

    boolean enablePinger;

    public WandService(Activity activity, OnSpellCast onSpellCastListener, boolean enablePinger) {
        this.activity = activity;
        this.onSpellCastListener = onSpellCastListener;
        this.enablePinger = enablePinger;

        usbSerialService = new UsbSerialService(activity, this);
        speechService = new SpeechService(activity);

        initApiPinger();
    }

    public void onPause() {
        usbSerialService.disconnect();
    }

    public void onResume() {
        usbSerialService.connect();
    }

    private void initApiPinger() {
        final int API_PING_INTERVAL = 1000;

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                apiService.getStatus(spellName -> {
                    if (spellName != null)
                        onSpellCastListener.spellCast(false, spellName);
                });

                handler.postDelayed(this, API_PING_INTERVAL);
            }
        }, API_PING_INTERVAL);

    }

    @Override
    public void onDataReceived(byte[] data) {
        // TODO

        activity.runOnUiThread(() ->
                Toast.makeText(activity, new String(data), Toast.LENGTH_SHORT).show()
        );

        // call onSpellCastListener.spellCast(boolean) when animations should be displayed
    }

    interface OnSpellCast {
        void spellCast(boolean me, String spellName);
    }
}
