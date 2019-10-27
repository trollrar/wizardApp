package si.fullin.wizardapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.PixelCopy;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.*;
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

    Material colorBlue;
    Material colorRed;
    Material colorGreen;

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

        // init material colors

        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.RED))
                .thenAccept(material -> colorRed = material);
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.BLUE))
                .thenAccept(material -> colorBlue = material);
        MaterialFactory.makeOpaqueWithColor(this, new Color(android.graphics.Color.GREEN))
                .thenAccept(material -> colorGreen = material);


        sceneLoader.onCreate(this, arFragment);
        initOnTap();


        // init VR faker
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                VRActivity.this.runOnUiThread(() -> copyImage());
                handler.postDelayed(this, 42);
            }
        }, 2000);

        new Handler().postDelayed(() -> showSpellAtLastTappedAnchor(colorGreen), 10000);
        new Handler().postDelayed(this::showDamageOverlay, 15000);
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

        if (surfaceview.getWidth() <= 0 || surfaceview.getHeight() <= 0)
            return;

        Bitmap plotBitmap = Bitmap.createBitmap(surfaceview.getWidth(), surfaceview.getHeight(), Bitmap.Config.ARGB_8888);
        PixelCopy.request(surfaceview, plotBitmap, copyResult -> {
        }, surfaceview.getHandler());

        imageView.setImageBitmap(plotBitmap);
    }

    private Anchor lastTappedAnchorNode = null;

    void initOnTap() {
        arFragment.setOnTapArPlaneListener(
                (HitResult hitResult, Plane plane, MotionEvent motionEvent) -> {
                    lastTappedAnchorNode = hitResult.createAnchor();

                    showSpellAtLastTappedAnchor(colorGreen);
                });
    }

    void showDamageOverlay() {
        View damageOverlay = findViewById(R.id.damageOverlay);

        damageOverlay.setVisibility(View.VISIBLE);
        damageOverlay.setAlpha(1);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            final int ALL_STEPS = 75;
            int stepsRemaining = ALL_STEPS;

            @Override
            public void run() {
                stepsRemaining--;

                VRActivity.this.runOnUiThread(() -> {
                    if (stepsRemaining == 0) {
                        damageOverlay.setVisibility(View.GONE);
                    } else {

                        float x = (float) stepsRemaining / ALL_STEPS * 0.3f;
                        float alpha = (float) Math.pow((1.0 - x) * Math.sin(Math.PI * (1.0 - x) / 0.59), 0.85);

                        damageOverlay.setAlpha(alpha);
                    }
                });

                if (stepsRemaining > 0)
                    handler.postDelayed(this, 15);
            }
        }, 15);
    }

    void showSpellAtLastTappedAnchor(Material color) {
        if (lastTappedAnchorNode == null)
            return;

        AnchorNode anchorNode = new AnchorNode(lastTappedAnchorNode);

        runOnUiThread(() -> anchorNode.setParent(arFragment.getArSceneView().getScene()));

        final Renderable[] renderable = new Renderable[1];
        ModelRenderable.builder()
                .setSource(this, R.raw.arzeninball)
                .build()
                .thenAccept(r -> renderable[0] = r)
                .exceptionally(throwable -> null);

        Frame frame = arFragment.getArSceneView().getArFrame();
        Pose objectPose = lastTappedAnchorNode.getPose();
        Pose cameraPose = frame.getCamera().getPose();

        float dx = objectPose.tx() - cameraPose.tx();
        float dy = objectPose.ty() - cameraPose.ty();
        float dz = objectPose.tz() - cameraPose.tz();

        ///Compute the straight-line distance.
        float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            final int ALL_STEPS = 20;
            int stepsRemaining = ALL_STEPS;
            TransformableNode sphere = null;

            @Override
            public void run() {
                stepsRemaining--;

                VRActivity.this.runOnUiThread(() -> {
                    if (sphere != null) {
                        anchorNode.removeChild(sphere);
                    }

                    if (stepsRemaining == 0) {
                        arFragment.getArSceneView().getScene().removeChild(anchorNode);
                    } else {

                        float radius = (float) stepsRemaining / ALL_STEPS * 0.3f;
                        float distance = distanceMeters * ((float) stepsRemaining / ALL_STEPS);


                        //Renderable renderable = ShapeFactory.makeSphere(radius, new Vector3(0.0f, 0.5f, 0.0f), color);
                        sphere = new TransformableNode(arFragment.getTransformationSystem());
                        sphere.getScaleController().setMaxScale(0.25f);
                        sphere.getScaleController().setMinScale(0.2f);
                        Vector3 vector3 = sphere.getWorldPosition();
                        Quaternion rotation = sphere.getWorldRotation();
                        rotation.y = 180;
                        vector3.z = (distance)-1;
                        vector3.y = 0.4f;
                        sphere.setWorldPosition(vector3);
                        sphere.setWorldRotation(rotation);
                        Log.i(TAG, String.valueOf(rotation.y));
                        Log.i(TAG, String.valueOf(vector3.z));
                        sphere.setParent(anchorNode);
                        //sphere.setRenderable(renderable);
                        sphere.setRenderable(renderable[0]);
                        sphere.select();


                    }
                });

                if (stepsRemaining > 0)
                    handler.postDelayed(this, 40);
            }
        }, 0);
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
