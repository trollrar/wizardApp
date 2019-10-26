package si.fullin.wizardapp;

import android.app.Application;
import android.content.Context;

public class WizardApp extends Application {

    private static Context context;

    public void onCreate() {
        super.onCreate();
        WizardApp.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return WizardApp.context;
    }

}
