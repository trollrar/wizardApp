package si.fullin.wizardapp;

import android.app.Activity;
import android.widget.Toast;

/**
 * the main service that analyzes data from the wand, uses speech recog service and api service
 */
public class WandService implements UsbSerialService.OnDataRecieved {

    Activity activity;
    OnSpellCast onSpellCastListener;

    UsbSerialService usbSerialService;
    SpeechService speechService;

    public WandService(Activity activity, OnSpellCast onSpellCastListener) {
        this.activity = activity;
        this.onSpellCastListener = onSpellCastListener;

        usbSerialService = new UsbSerialService(activity, this);
        speechService = new SpeechService(activity);
    }

    public void onPause() {
        usbSerialService.disconnect();
    }

    public void onResume() {
        usbSerialService.connect();
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
