package si.fullin.wizardapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "mainactivity";

    ApiService apiService = new ApiService();
    SpeechService speechService;

    @BindView(R.id.textViewMain)
    TextView textViewMain;

    @BindView(R.id.spellTextView)
    TextView spellText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        speechService = new SpeechService(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @OnClick(R.id.ButtonPost)
    void postSpell() {
        apiService.postSpell("SkadidlSkadudlYourDickIsANudl");
    }

    @OnClick(R.id.buttonSpell)
    void listenForSpell() {
        final TextView view = findViewById(R.id.spellTextView);
        view.setText("...");

        speechService.listenForSpell(spellName -> runOnUiThread(() -> {
            view.setText(spellName == null ? "You muggle!" : spellName);
        }));

    }

    @OnClick(R.id.buttonVR)
    public void openVr(View view) {
        startActivity(new Intent(this, VRActivity.class));
    }
}
