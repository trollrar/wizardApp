package si.fullin.wizardapp;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "app";


    private static final String SpeechSubscriptionKey = "898cd384426a4986b5cca5a902b8c395";
    // Replace below with your own service region (e.g., "westus").
    private static final String SpeechRegion = "francecentral";

    private SpeechConfig speechConfig;



    SpellService spellService = new SpellService();
    @BindView(R.id.textViewMain)
    TextView textViewMain;

    @BindView(R.id.spellTextView)
    TextView spellText;

    private MicrophoneStream microphoneStream;
    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }

        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        getDetail();

        // Request permissions needed for speech recognition
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{RECORD_AUDIO, INTERNET}, 5);

        // create config

        try {
            speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            Log.e(TAG, ex.getMessage());
            return;
        }

    }

    @OnClick(R.id.ButtonGet)

    void getSpell() {
        Log.i(TAG, "jej");
            spellService.getSpell(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    e.printStackTrace();
                    Log.i(TAG, "dej ne faki");
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    try (ResponseBody responseBody = response.body()) {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                        Headers responseHeaders = response.headers();
                        for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                            System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                        }

                        Log.i(TAG, responseBody.string());
                    }
                }
            });
    }

    @OnClick(R.id.ButtonPost)
    void postSpell() {
        Log.i(TAG, "jej");
        spellService.postSpell("SkadidlSkadudlYourDickIsANudl", new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                e.printStackTrace();
                Log.i(TAG, "dej ne faki");
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                try (ResponseBody responseBody = response.body()) {
                    if (!response.isSuccessful())
                        throw new IOException("Unexpected code " + response);

                    Headers responseHeaders = response.headers();
                    for (int i = 0, size = responseHeaders.size(); i < size; i++) {
                        System.out.println(responseHeaders.name(i) + ": " + responseHeaders.value(i));
                    }

                    Log.i(TAG, responseBody.string());
                }
            }
        });
    }

    @OnClick(R.id.buttonSpell)
    void checkSpell() {
        try {
            // final AudioConfig audioInput = AudioConfig.fromDefaultMicrophoneInput();
            final AudioConfig audioInput = AudioConfig.fromStreamInput(createMicrophoneStream());
            final SpeechRecognizer reco = new SpeechRecognizer(speechConfig, audioInput);

            final Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();
            setOnTaskCompletedListener(task, result -> {
                String s = result.getText();
                if (result.getReason() != ResultReason.RecognizedSpeech) {
                    String errorDetails = (result.getReason() == ResultReason.Canceled) ? CancellationDetails.fromResult(result).getErrorDetails() : "";
                    s = "Recognition failed with " + result.getReason() + ". Did you enter your subscription?" + System.lineSeparator() + errorDetails;
                }

                reco.close();

                final String finalS = getSpellName(s);
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((TextView)MainActivity.this.findViewById(R.id.spellTextView)).setText(finalS);
                    }
                });

            });
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            Log.e(TAG, ex.getMessage());
        }
    }

    public void getDetail() {
        String actionString = this.getPackageName() + ".action.USB_PERMISSION";

        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(this, 0, new
                Intent(actionString), 0);

        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> deviceList = manager.getDeviceList();
        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        while (deviceIterator.hasNext()) {
            UsbDevice device = deviceIterator.next();


            manager.requestPermission(device, mPermissionIntent);
            String Model = device.getDeviceName();

            int DeviceID = device.getDeviceId();
            int Vendor = device.getVendorId();
            int Product = device.getProductId();
            int Class = device.getDeviceClass();
            int Subclass = device.getDeviceSubclass();

            textViewMain.setText(textViewMain.getText() + "\nid "+ DeviceID + ", vid "+Vendor+",pid "+Product+",class "+Class+",sublcass "+Subclass);

            Log.i("DEVICE", "id "+ DeviceID + ", vid "+Vendor+",pid "+Product+",class "+Class+",sublcass "+Subclass);


        }
    }

    private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    private static ExecutorService s_executorService;
    static {
        s_executorService = Executors.newCachedThreadPool();
    }

    private static String getSpellName(String query) {
        ArrayList<String[]> spellTypes = new ArrayList<>();
        Resources res = WizardApp.getAppContext().getResources();
        spellTypes.add(res.getStringArray(R.array.fire_atk));
        spellTypes.add(res.getStringArray(R.array.fire_def));
        spellTypes.add(res.getStringArray(R.array.water_atk));
        spellTypes.add(res.getStringArray(R.array.water_def));
        spellTypes.add(res.getStringArray(R.array.plant_atk));
        spellTypes.add(res.getStringArray(R.array.plant_def));

        String normalize = query.toLowerCase().replaceAll("( |[.])", "");
        for (String[] spellType: spellTypes) {
            for (int i = 1; i < spellType.length; i++) {
                if (spellType[i].equals(normalize)) {
                    return spellType[0];
                }
            }
        }
        Log.i(TAG, query);
        return "You are nothing but a filthy muggle!";
    }

    public void openVr(View view) {
        startActivity(new Intent(this, VRActivity.class));
    }

}
