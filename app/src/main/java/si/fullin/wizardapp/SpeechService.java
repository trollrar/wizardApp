package si.fullin.wizardapp;

import android.app.Activity;
import android.content.res.Resources;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.TextView;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static android.Manifest.permission.INTERNET;
import static android.Manifest.permission.RECORD_AUDIO;

public class SpeechService {

    final static String TAG = "SpeechService";

    private SpeechConfig speechConfig;

    private MicrophoneStream microphoneStream;

    private static final String SpeechSubscriptionKey = "898cd384426a4986b5cca5a902b8c395";
    // Replace below with your own service region (e.g., "westus").
    private static final String SpeechRegion = "francecentral";

    private static ExecutorService s_executorService = Executors.newCachedThreadPool();

    Activity activity;

    private MicrophoneStream createMicrophoneStream() {
        if (microphoneStream != null) {
            microphoneStream.close();
            microphoneStream = null;
        }

        microphoneStream = new MicrophoneStream();
        return microphoneStream;
    }

    public SpeechService(Activity activity) {
        this.activity = activity;
        // Request permissions needed for speech recognition
        ActivityCompat.requestPermissions(activity, new String[]{RECORD_AUDIO, INTERNET}, 5);

        try {
            speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            Log.e(TAG, ex.getMessage());
            return;
        }
    }

    public void listenForSpell(SpellRecognised callback) {
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

                callback.run(getSpellName(s));
            });
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
            Log.e(TAG, ex.getMessage());
        }
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

    private <T> void setOnTaskCompletedListener(Future<T> task, SpeechService.OnTaskCompletedListener<T> listener) {
        s_executorService.submit(() -> {
            T result = task.get();
            listener.onCompleted(result);
            return null;
        });
    }

    private interface OnTaskCompletedListener<T> {
        void onCompleted(T taskResult);
    }

    public interface SpellRecognised {
        void run(String spellName);
    }
}
