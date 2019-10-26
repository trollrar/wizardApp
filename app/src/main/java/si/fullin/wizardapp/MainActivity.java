package si.fullin.wizardapp;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.SerialInputOutputManager;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

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
import java.util.List;
import java.util.Map;
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

    private static final String TAG = "mainactivity";


    private static final String SpeechSubscriptionKey = "898cd384426a4986b5cca5a902b8c395";
    // Replace below with your own service region (e.g., "westus").
    private static final String SpeechRegion = "francecentral";

    private SpeechConfig speechConfig;
    private static UsbSerialPort sPort = null;
    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();


    SpellService spellService = new SpellService();
    @BindView(R.id.textViewMain)
    TextView textViewMain;

    Physicaloid mPhysicaloid; // initialising library

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

    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateReceivedData(data);
                        }
                    });
                }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

       /* getDetail();
        try {
            chanelUsb();
        } catch (IOException e) {
            textViewMain.setText(e.getMessage());
            e.printStackTrace();
        }*/
        //getDetail();
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

        findPort();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
    }

    void findPort() {
        UsbManager mUsbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        final List<UsbSerialDriver> drivers =
                UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);

        final List<UsbSerialPort> result = new ArrayList<>();
        for (final UsbSerialDriver driver : drivers) {
            final List<UsbSerialPort> ports = driver.getPorts();

            Log.d(TAG, String.format("+ %s: %s port%s",
                    driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));

            for(UsbSerialPort port : ports) {
                Toast.makeText(this, "port: " + port.getPortNumber(), Toast.LENGTH_SHORT).show();
            }

            result.addAll(ports);
        }

        if(!result.isEmpty()) {
            sPort = result.get(0);
            Toast.makeText(this, "port set!", Toast.LENGTH_SHORT).show();
        }

        portChanged();
    }

    void portChanged() {
        if (sPort == null) {
            textViewMain.setText("No serial device.");
        } else {
            final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
                textViewMain.setText("Opening device failed");
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                /*showStatus(mDumpTextView, "CD  - Carrier Detect", sPort.getCD());
                showStatus(mDumpTextView, "CTS - Clear To Send", sPort.getCTS());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "DTR - Data Terminal Ready", sPort.getDTR());
                showStatus(mDumpTextView, "DSR - Data Set Ready", sPort.getDSR());
                showStatus(mDumpTextView, "RI  - Ring Indicator", sPort.getRI());
                showStatus(mDumpTextView, "RTS - Request To Send", sPort.getRTS());*/

            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                textViewMain.setText("Error opening device: " + e.getMessage());
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            textViewMain.setText("Serial device: " + sPort.getClass().getSimpleName());
        }
        onDeviceStateChange();
    }

    private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
        stopIoManager();
        startIoManager();
    }

    /*void chanelUsb() throws IOException {
        // Find all available drivers from attached devices.
        UsbManager manager = (UsbManager) getSystemService(Context.USB_SERVICE);
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager);
        if (availableDrivers.isEmpty()) {
            textViewMain.setText("No drivers");
            return;
        }

        // Open a connection to the first available driver.
        UsbSerialDriver driver = availableDrivers.get(0);
        final UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        Intent usbApporval = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1337, usbApporval, 0);
        usbManager.requestPermission(driver.getDevice(), pendingIntent);
        UsbDeviceConnection connection = manager.openDevice(driver.getDevice());
        if (connection == null) {
            // You probably need to call UsbManager.requestPermission(driver.getDevice(), ..)
            textViewMain.setText("Conn null");
            return;
        }

        // Read some data! Most have just one port (port 0).
        UsbSerialPort port = driver.getPorts().get(0);
        try {
            port.open(connection);
            port.setParameters(9600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            byte buffer[] = new byte[16];
            port.read(buffer, 1000);
            String msg = new String(buffer);
            textViewMain.setText(textViewMain.getText()+msg);

        } catch (IOException e) {
            // Deal with error.
            textViewMain.setText(e.getMessage());
        } finally {
            port.close();
        }


    }*/

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
                        ((TextView) MainActivity.this.findViewById(R.id.spellTextView)).setText(finalS);
                    }
                });

            });
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            Log.e(TAG, ex.getMessage());
        }
    }

   /* public void getDetail() {
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

            textViewMain.setText(textViewMain.getText() + "\nid " + DeviceID + ", vid " + Vendor + ",pid " + Product + ",class " + Class + ",sublcass " + Subclass);

            Log.i("DEVICE", "id " + DeviceID + ", vid " + Vendor + ",pid " + Product + ",class " + Class + ",sublcass " + Subclass);


        }
    }*/

    public void updateReceivedData(byte[] data) {
        final String message = new String(data) + "\n";
        textViewMain.setText(message);
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
        for (String[] spellType : spellTypes) {
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
