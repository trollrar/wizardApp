package si.fullin.wizardapp;
import com.physicaloid.lib.Physicaloid;
import com.physicaloid.lib.usb.driver.uart.ReadLisener;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "mainactivity";
    SpellService spellService = new SpellService();
    @BindView(R.id.textViewMain)
    TextView textViewMain;

    Physicaloid mPhysicaloid; // initialising library

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        getDetail();

        mPhysicaloid = new Physicaloid(this);
        mPhysicaloid.setBaudrate(9600);
        while (!mPhysicaloid.open());
            Log.d("UI thread", "openned");
            textViewMain.setText("opened");
            mPhysicaloid.addReadListener(new ReadLisener() {
                private String full="";
                private Double currentFront = 0d;

            @Override
            public void onRead(int size) {
                final byte[] buf = new byte[size];
                mPhysicaloid.read(buf, size);
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        String msg = new String(buf);
                        full+=msg;
                        Log.d("UI thread", full);


                        String[] split = full.split("\n");
                        if(split.length>=2) {
                            String line = split[split.length - 2];
                            full = split[split.length - 1];
                            String[] values = line.split(";");
                            Map<String, Double> valuesMap = new HashMap<>();
                            valuesMap.put("acx", Double.valueOf(values[0]));
                            valuesMap.put("acy", Double.valueOf(values[1]));
                            valuesMap.put("acz", Double.valueOf(values[2]));
                            valuesMap.put("temp", Double.valueOf(values[3]));
                            valuesMap.put("gyrox", Double.valueOf(values[4]));
                            valuesMap.put("gyroy", Double.valueOf(values[5]));
                            valuesMap.put("gyroz", Double.valueOf(values[6]));
                            double acx = Double.valueOf(values[0]);
                            double acy = Double.valueOf(values[0]);
                            double acz = Double.valueOf(values[0]);
                            /*if(acz>1900d && acz < 2500d) {
                                textViewMain.setText("UP");
                                currentFront =
                            }*/
                           // textViewMain.setText(String.format("acx:%s\nacy: %s\nacz: %s\ntemp: %s\ngyrox: %s\ngyroy: %s\ngyroz: %s", valuesMap.get("acx"), valuesMap.get("acy"), valuesMap.get("acz"), valuesMap.get("temp"), valuesMap.get("gyrox"), valuesMap.get("gyroy"), valuesMap.get("gyroz"))
                           // );
                        }
                    }
                });
            }
        });

        //getDetail();
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


        }}
}
