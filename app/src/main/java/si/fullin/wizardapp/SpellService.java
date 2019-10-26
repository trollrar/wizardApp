package si.fullin.wizardapp;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class SpellService {

    String url = "http://aljaz.erzen.si:9000";
    String key = "fares";
    private final OkHttpClient client = new OkHttpClient();

    public void postSpell(String spellId, Callback callback) {
        RequestBody formBody = new FormBody.Builder()
                .add("aljaz", "geiii")
                .build();
        Request request = new Request.Builder()
                .url(url+"?spell"+spellId+"&key="+key)
                .post(formBody)
                .build();

        client.newCall(request).enqueue(callback);
    }

    public void getSpell(Callback callback) {
        Request request = new Request.Builder()
                .url(url+"?key="+key)
                .build();
        client.newCall(request).enqueue(callback);
    }
}
