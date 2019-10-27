package si.fullin.wizardapp;

import android.app.Activity;
import android.os.Handler;
import android.icu.util.Measure;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

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
    int timerCounter=0;
    boolean startTimerStarted = false;
    boolean stopTimerStarted = false;

    boolean logging = false;

    @Override
    public void onDataReceived(byte[] data) {
        activity.runOnUiThread(() ->
                Toast.makeText(activity, "data came", Toast.LENGTH_SHORT).show()
        );
        // TODO
        String dataStream = new String(data);

        String[] split = dataStream.split("\r\n");
        if (split.length>2) {
            for (int i = 2; i < split.length-1; i++) {
                String[] split1 = split[i].split(";");
                if(split1.length==3){
                    Measurment measurment;
                    try {
                        measurment = new Measurment(Double.valueOf(split1[0]), Double.valueOf(split1[1]), Double.valueOf(split1[2]));
                    } catch (Exception ex) {
                        continue;
                    }

                    if(!stopTimerStarted) {
                        if (measurment.x > 14000 && !startTimerStarted) {
                            /*activity.runOnUiThread(() ->
                                    Toast.makeText(activity, "up started", Toast.LENGTH_SHORT).show()
                            );*/
                            // Start stopwatch and measurement
                            startTimerStarted = true;

                        } else if (measurment.x > 14000 && startTimerStarted) {
                            // Start stopwatch and measurement
                            timerCounter++;
                        } else if (measurment.x < 14000 && startTimerStarted) {
                            // Start stopwatch and measurement
                            startTimerStarted = false;
                            timerCounter = 0;
                            /*activity.runOnUiThread(() ->
                                    Toast.makeText(activity, "Failed up", Toast.LENGTH_SHORT).show()
                            );*/
                        }
                        if (timerCounter > 500 && startTimerStarted) {
                            // Start logging gestures.
                            startTimerStarted = false;
                            timerCounter = 0;
                            logging = true;
                            measurments.clear();
                            /*activity.runOnUiThread(() ->
                                    Toast.makeText(activity, "up ended", Toast.LENGTH_SHORT).show()
                            );*/
                        }
                    }
                    if(logging) {
                        measurments.add(measurment);
                    }

                    if(!startTimerStarted && logging) {
                        if (measurment.z > 14000 && !stopTimerStarted) {
                            // Start stopwatch and measurement
                            stopTimerStarted = true;

                            /*activity.runOnUiThread(() ->
                                    Toast.makeText(activity, "stop started", Toast.LENGTH_SHORT).show()
                            );*/
                        } else if (measurment.z > 14000 && stopTimerStarted) {
                            // Start stopwatch and measurement
                            timerCounter++;
                        } else if (measurment.z < 14000 && stopTimerStarted) {
                            /*activity.runOnUiThread(() ->
                                    Toast.makeText(activity, "stop failed", Toast.LENGTH_SHORT).show()
                            );*/
                            // Start stopwatch and measurement
                            stopTimerStarted = false;
                            timerCounter = 0;
                        }

                        if (timerCounter > 500 && stopTimerStarted) {
                            // stop logging gestures.
                            stopTimerStarted = false;
                            timerCounter = 0;
                            list.add(measurments);
                            measurments = new ArrayList<>();
                            // call onSpellCastListener.spellCast(boolean) when animations should be displayed

                            /*activity.runOnUiThread(() ->
                                    Toast.makeText(activity, "Spell happened", Toast.LENGTH_SHORT).show()
                            );*/
                            logging = false;
                        }
                    }

                    activity.runOnUiThread(() ->
                            {
                                TextView viewById = (TextView) activity.findViewById(R.id.textViewMain);
                                viewById.setText("start " + startTimerStarted + "\nstop " + stopTimerStarted + "\nlogging " + logging);
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
