package si.fullin.wizardapp.ar;
import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import com.google.ar.core.exceptions.UnavailableException;
import com.google.ar.sceneform.ArSceneView;

class SceneformHelper {
    private static final String CAMERA_PERMISSION = Manifest.permission.CAMERA;

    // Check to see we have the necessary permissions for this app
    public static boolean hasCameraPermission(Activity activity) {
        return ContextCompat.checkSelfPermission(activity, CAMERA_PERMISSION)
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void setupSessionForSceneView(Context context, ArSceneView sceneView) {
        try {
            Session session = new Session(context);
            Config config = new Config(session);
            config.setUpdateMode(Config.UpdateMode.LATEST_CAMERA_IMAGE);
            session.configure(config);
            sceneView.setupSession(session);
        } catch (UnavailableException e) {
            Log.e("ASADemo: ", e.toString());
        }
    }
}
