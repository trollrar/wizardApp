package si.fullin.wizardapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

public class MainActivity extends AppCompatActivity implements WandService.OnSpellCast {

    private static final String TAG = "mainactivity";

    ApiService apiService = new ApiService();
    SpeechService speechService;
    WandService wandService;

    @BindView(R.id.textViewMain)
    TextView textViewMain;

    @BindView(R.id.spellTextView)
    TextView spellText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        wandService = new WandService(this, this);
        speechService = new SpeechService(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        wandService.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        wandService.onResume();
    }

    @OnClick(R.id.ButtonGet)
    void getSpell() {
        Log.i(TAG, "jej");
        apiService.getSpell(new Callback() {
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
        apiService.postSpell("SkadidlSkadudlYourDickIsANudl", new Callback() {
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
    void listenForSpell() {
        final TextView view = findViewById(R.id.spellTextView);
        view.setText("...");

        speechService.listenForSpell(spellName -> runOnUiThread(() -> {
            view.setText(spellName);
        }));

    }

    @OnClick(R.id.buttonVR)
    public void openVr(View view) {
        startActivity(new Intent(this, VRActivity.class));
    }

    @Override
    public void spellCast(boolean me, String spellName) {

    }
}
