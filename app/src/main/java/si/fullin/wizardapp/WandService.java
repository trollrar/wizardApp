package si.fullin.wizardapp;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Handler;
import android.icu.util.Measure;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.nimbusds.jose.util.Resource;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.lang.Character;

/**
 * the main service that analyzes data from the wand, uses speech recog service and api service
 */
public class WandService implements UsbSerialService.OnDataRecieved {

    Activity activity;
    OnSpellCast onSpellCastListener;

    UsbSerialService usbSerialService;
    SpeechService speechService;
    ApiService apiService = new ApiService();


    ArrayList<Measurment> measurments = new ArrayList<>();

    public WandService(Activity activity, OnSpellCast onSpellCastListener, boolean enablePinger) {
        this.activity = activity;
        this.onSpellCastListener = onSpellCastListener;

        usbSerialService = new UsbSerialService(activity, this);
        speechService = new SpeechService(activity);

        if (enablePinger)
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


    ArrayList<ArrayList<Measurment>> list = new ArrayList<>();
    int timerCounter = 0;
    boolean startTimerStarted = false;
    boolean stopTimerStarted = false;

    boolean logging = false;

    byte[] currentStream = new byte[0];
    private String spellName = "Failed";

    boolean firstTimeSpell = true;
    @Override
    public void onDataReceived(byte[] data) {
        currentStream = data;

        String currentString = new String(data, StandardCharsets.UTF_8).replace("X", "");

        String[] split1 = currentString.split(";");
        if (split1.length == 3) {
            Measurment measurment;
            try {
                measurment = new Measurment(Double.valueOf(split1[0]), Double.valueOf(split1[1]), Double.valueOf(split1[2]));
            } catch (Exception ex) {
                return;
            }

            if (!stopTimerStarted) {
                if (measurment.x > 14000 && !startTimerStarted) {
                    // Start stopwatch and measurement
                    startTimerStarted = true;

                } else if (measurment.x > 14000 && startTimerStarted) {
                    // Start stopwatch and measurement
                    timerCounter++;
                } else if (measurment.x < 14000 && startTimerStarted) {
                    // Start stopwatch and measurement
                    startTimerStarted = false;
                    timerCounter = 0;
                }
                if (timerCounter >= 2 && startTimerStarted) {
                    // Start logging gestures.
                    startTimerStarted = false;
                    timerCounter = 0;
                    logging = true;
                    measurments.clear();

                    if(firstTimeSpell) {
                        firstTimeSpell = false;
                        spellName = "Failed";
                        speechService.listenForSpell(spellName -> {
                            this.spellName = spellName;
                        });

                        activity.runOnUiThread(() ->
                                {
                                    TextView viewById = (TextView) activity.findViewById(R.id.textViewMain);
                                    viewById.setText("Listening....");

                                }
                        );
                    }

                }
            }

            if (logging) {
                measurments.add(measurment);
            }

            if (!startTimerStarted && logging) {
                if (measurment.z > 14000 && !stopTimerStarted) {
                    // Start stopwatch and measurement
                    stopTimerStarted = true;

                } else if (measurment.z > 14000 && stopTimerStarted) {
                    // Start stopwatch and measurement
                    timerCounter++;
                } else if (measurment.z < 14000 && stopTimerStarted) {

                    // Start stopwatch and measurement
                    stopTimerStarted = false;
                    timerCounter = 0;
                }

                if (timerCounter >= 2 && stopTimerStarted) {
                    // stop logging gestures.
                    stopTimerStarted = false;
                    timerCounter = 0;
                    list.add(measurments);

                    firstTimeSpell = true;
                    logging = false;



                    activity.runOnUiThread(() ->
                            {
                                Resources res = activity.getResources();
                                if(spellName.equals(res.getStringArray(R.array.fire_atk)[0])) {
                                    if (measurments.stream().anyMatch(x->x.x>13000)
                                        &&measurments.stream().allMatch(y->y.y<13000)) {
                                        TextView viewById = (TextView) activity.findViewById(R.id.textViewMain);
                                        viewById.setText(spellName);
                                        onSpellCastListener.spellCast(true, spellName);
                                    } else {
                                        TextView viewById = (TextView) activity.findViewById(R.id.textViewMain);
                                        viewById.setText("Wrong Gesture");

                                    }


                                } else  if(spellName.equals(res.getStringArray(R.array.water_def)[0])) {
                                    if (measurments.stream().anyMatch(x->x.y>13000)
                                        &&measurments.stream().allMatch(y->y.x<13000)) {
                                        TextView viewById = (TextView) activity.findViewById(R.id.textViewMain);
                                        viewById.setText(spellName);
                                        onSpellCastListener.spellCast(true, spellName);
                                    }else {
                                        TextView viewById = (TextView) activity.findViewById(R.id.textViewMain);
                                        viewById.setText("Wrong Gesture");
                                    }


                                } else if(spellName.equals(res.getStringArray(R.array.plant_atk)[0])) {
                                    if (measurments.stream().anyMatch(x -> x.y > 13000)
                                            && measurments.stream().anyMatch(y -> y.x > 13000)) {
                                        TextView viewById = (TextView) activity.findViewById(R.id.textViewMain);
                                        viewById.setText(spellName);
                                        onSpellCastListener.spellCast(true, spellName);
                                    }else {
                                        TextView viewById = (TextView) activity.findViewById(R.id.textViewMain);
                                        viewById.setText("Wrong Gesture");
                                    }
                                } else {
                                    TextView viewById = (TextView) activity.findViewById(R.id.textViewMain);
                                    viewById.setText(spellName);
                                    onSpellCastListener.spellCast(true, spellName);
                                }
                                spellName = "Failed";
                                measurments = new ArrayList<>();
                            }
                    );

                }
            }




        }

        //activity.runOnUiThread(() ->
        //        Toast.makeText(activity, new String(data), Toast.LENGTH_SHORT).show()
        //);

    }

    interface OnSpellCast {
        void spellCast(boolean me, String spellName);
    }

    private class Measurment {
        public double x;
        public double y;
        public double z;

        public Measurment(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }



        @Override
        public String toString() {
            return "{" +
                    "x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }
    }
}
