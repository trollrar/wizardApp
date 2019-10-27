package si.fullin.wizardapp;

import android.app.Activity;
import android.content.res.Resources;
import android.os.Handler;
import android.widget.TextView;
import android.widget.Toast;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * the main service that analyzes data from the wand, uses speech recog service and api service
 */
public class WandService implements UsbSerialService.OnDataRecieved {

    private Activity activity;
    private OnSpellCast onSpellCastListener;

    private UsbSerialService usbSerialService;
    private SpeechService speechService;
    private ApiService apiService = new ApiService();


    private ArrayList<Measurment> measurments = new ArrayList<>();

    WandService(Activity activity, OnSpellCast onSpellCastListener, boolean enablePinger) {
        this.activity = activity;
        this.onSpellCastListener = onSpellCastListener;

        usbSerialService = new UsbSerialService(activity, this);
        speechService = new SpeechService(activity);

        if (enablePinger)
            initApiPinger();
    }

    void onPause() {
        usbSerialService.disconnect();
    }

    void onResume() {
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

                    if (firstTimeSpell) {
                        firstTimeSpell = false;
                        speechService.listenForSpell(spellName -> {
                            this.spellName = spellName;
                            if (spellName != null)
                                onSpellCastListener.spellStatus(SpellResult.SPEECH_RECOGNISED);
                        });

                        onSpellCastListener.spellStatus(SpellResult.START);
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


                    activity.runOnUiThread(() -> {
                                Resources res = activity.getResources();
                                if (spellName == null) {
                                    onSpellCastListener.spellStatus(SpellResult.SPEECH_FAILED);
                                } else {
                                    if (spellName.equals(res.getStringArray(R.array.fire_moving)[0])) {
                                        if (measurments.stream().anyMatch(x -> x.x > 13000)
                                                && measurments.stream().allMatch(y -> y.y < 13000)) {
                                            castSpell(spellName);
                                        } else {
                                            onSpellCastListener.spellStatus(SpellResult.WAND_FAILED);
                                        }


                                    } else if (spellName.equals(res.getStringArray(R.array.water_static)[0])) {
                                        if (measurments.stream().anyMatch(x -> x.y > 13000)
                                                && measurments.stream().allMatch(y -> y.x < 13000)) {
                                            castSpell(spellName);
                                        } else {
                                            onSpellCastListener.spellStatus(SpellResult.WAND_FAILED);
                                        }


                                    } else if (spellName.equals(res.getStringArray(R.array.plant_moving)[0])) {
                                        if (measurments.stream().anyMatch(x -> x.y > 13000)
                                                && measurments.stream().anyMatch(y -> y.x > 13000)) {
                                            castSpell(spellName);
                                        } else {
                                            onSpellCastListener.spellStatus(SpellResult.WAND_FAILED);
                                        }
                                    } else {
                                        castSpell(spellName);
                                        toast("spell checking not implemented");
                                    }
                                }
                                spellName = null;
                                measurments = new ArrayList<>();
                            }
                    );

                }
            }
        }
    }

    private void castSpell(String spellName) {
        onSpellCastListener.spellCast(true, spellName);
        apiService.postSpell(spellName);
    }

    private void toast(String text) {
        activity.runOnUiThread(() -> Toast.makeText(activity, text, Toast.LENGTH_SHORT).show());
    }


    interface OnSpellCast {
        void spellCast(boolean me, String spellName);

        void spellStatus(SpellResult result);
    }

    private class Measurment {
        public double x;
        public double y;
        public double z;

        Measurment(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }


        @NotNull
        @Override
        public String toString() {
            return "{" +
                    "x=" + x +
                    ", y=" + y +
                    ", z=" + z +
                    '}';
        }
    }

    public enum SpellResult {
        WAND_FAILED, SPEECH_FAILED, START, SPEECH_RECOGNISED
    }
}
