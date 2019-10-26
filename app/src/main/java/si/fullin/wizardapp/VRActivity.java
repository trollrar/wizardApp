package si.fullin.wizardapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;
import com.microsoft.CloudServices;
import si.fullin.wizardapp.ar.SceneLoader;

import java.util.HashMap;
import java.util.Map;

public class VRActivity extends AppCompatActivity {
    private static final String TAG = VRActivity.class.getSimpleName();

    private static final double MIN_OPENGL_VERSION = 3.0;

    private ArFragment arFragment;
    ImageView imageView;
    SceneLoader sceneLoader = new SceneLoader();

    @Override
    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    // CompletableFuture requires api level 24
    // FutureReturnValueIgnored is not valid
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        CloudServices.initialize(this);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            return;
        }

        setContentView(R.layout.activity_vr);

        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ar_fragment);
        imageView = this.findViewById(R.id.myimage);

        sceneLoader.onCreate(this, arFragment);
//        init();
        onTapPlace();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                VRActivity.this.runOnUiThread(() -> copyImage());
                handler.postDelayed(this, 42);
            }
        }, 2000);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Map<String, Integer> initialScene = new HashMap<>();
        initialScene.put("d9b2a566-bb12-42e7-8063-d84144871e66", R.raw.andy);
        initialScene.put("9fad35d2-560f-4f93-8259-5d54f6bb2858", R.raw.dragon);
        initialScene.put("b01fd3a5-e420-4cb1-baef-ac6086f004e9", R.raw.pumpkin);
        initialScene.put("2b1e6c9e-ba66-4d0f-89ec-6f9a3beb7352", R.raw.pumpkin);

        sceneLoader.locateObjects(initialScene);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sceneLoader.onDestroy();
    }

    void copyImage() {
        if (arFragment == null) {
            Log.e("test", "arFragment is null");
            return;
        }

        SurfaceView surfaceview = arFragment.getArSceneView();

        if(surfaceview.getWidth() <= 0 || surfaceview.getHeight() <= 0)
            return;

        Bitmap plotBitmap = Bitmap.createBitmap(surfaceview.getWidth(), surfaceview.getHeight(), Bitmap.Config.ARGB_8888);
        PixelCopy.request(surfaceview, plotBitmap, copyResult -> {
        }, surfaceview.getHandler());

        imageView.setImageBitmap(plotBitmap);
    }

    void onTapPlace() {

        final ModelRenderable[] renderable = new ModelRenderable[1];

        ModelRenderable.builder()
                .setSource(this, R.raw.dragon_big)
                .build()
                .thenAccept(r -> renderable[0] = r)
                .exceptionally(
                        throwable -> {
                            Toast toast =
                                    Toast.makeText(this, "Unable to load andy renderable", Toast.LENGTH_LONG);
                            toast.setGravity(Gravity.CENTER, 0, 0);
                            toast.show();
                            return null;
                        });

        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    if (renderable[0] == null) {
                        return;
                    }

                    // Create the Anchor.
                    Anchor anchor = hitResult.createAnchor();
                    AnchorNode anchorNode = new AnchorNode(anchor);
                    anchorNode.setParent(arFragment.getArSceneView().getScene());

                    // Create the transformable andy and add it to the anchor.
                    TransformableNode andy = new TransformableNode(arFragment.getTransformationSystem());
                    andy.setParent(anchorNode);
                    andy.setRenderable(renderable[0]);
                    andy.select();
                });

    }

    /**
     * Returns false and displays an error message if Sceneform can not run, true if Sceneform can run
     * on this device.
     *
     * <p>Sceneform requires Android N on the device as well as OpenGL 3.0 capabilities.
     *
     * <p>Finishes the activity if Sceneform can not run
     */
    public static boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        if (ArCoreApk.getInstance().checkAvailability(activity)
                == ArCoreApk.Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE) {
            Log.e(TAG, "Augmented Faces requires ARCore.");
            Toast.makeText(activity, "Augmented Faces requires ARCore", Toast.LENGTH_LONG).show();
            activity.finish();
            return false;
        }
        String openGlVersionString =
                ((ActivityManager) activity.getSystemService(Context.ACTIVITY_SERVICE))
                        .getDeviceConfigurationInfo()
                        .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL ES 3.0 later");
            Toast.makeText(activity, "Sceneform requires OpenGL ES 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
}
