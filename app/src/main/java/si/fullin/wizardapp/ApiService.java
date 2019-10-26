package si.fullin.wizardapp;

import android.util.Log;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Objects;

public class ApiService {

    private String url = "http://aljaz.erzen.si:9000";
    private static String key = Integer.toString((int) (Math.random() * 1024));

    private final OkHttpClient client = new OkHttpClient();

    public void postSpell(String spellName) {
        Request request = new Request.Builder()
                .url(url + "?name=" + spellName + "&key=" + key)
                .post(new FormBody.Builder().build())
                .build();

        client.newCall(request).enqueue(emptyCallback);
    }

    public void getStatus(StatusCallback callback) {
        Request request = new Request.Builder()
                .url(url + "?key=" + key).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Log.e("apiservice", e.toString());
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                byte[] body = response.body() == null ? null : Objects.requireNonNull(response.body()).bytes();
                callback.fire(body != null && body.length == 0 ? null : new String(body));
            }
        });
    }

    public interface StatusCallback {
        void fire(String spellName);
    }

    private Callback emptyCallback = new Callback() {
        @Override
        public void onFailure(@NotNull Call call, @NotNull IOException e) {
            Log.e("apiservice", e.toString());
        }

        @Override
        public void onResponse(@NotNull Call call, @NotNull Response response) {
            Log.i("apiservice", "call successful");
        }
    };
}
