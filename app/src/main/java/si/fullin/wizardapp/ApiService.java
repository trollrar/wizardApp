package si.fullin.wizardapp;

import okhttp3.*;

public class ApiService {

    private String url = "http://aljaz.erzen.si:9000";
    private static String key = Integer.toString((int) (Math.random() * 1024));

    private final OkHttpClient client = new OkHttpClient();

    public void postSpell(String spellId, Callback callback) {
        RequestBody formBody = new FormBody.Builder()
                .build();
        Request request = new Request.Builder()
                .url(url + "?spell" + spellId + "&key=" + key)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void getSpell(Callback callback) {
        Request request = new Request.Builder()
                .url(url + "?key=" + key)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
