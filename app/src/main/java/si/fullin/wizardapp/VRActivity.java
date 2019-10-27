package si.fullin.wizardapp;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.*;
import android.widget.FrameLayout;
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

public class VRActivity extends AppCompatActivity implements WandService.OnSpellCast {
    private static final String TAG = VRActivity.class.getSimpleName();

    private static final double MIN_OPENGL_VERSION = 3.0;

    private int MAX_HEALTH = 10;
    private int health = MAX_HEALTH;

    WandService wandService;
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


        wandService = new WandService(this, this, true);
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
    }

    @Override
    protected void onResume() {
        super.onResume();
        wandService.onResume();

        Map<String, Integer> initialScene = new HashMap<>();
        initialScene.put("01e6d8ad-665f-429a-989e-f837331f5b46", R.raw.pumpkin);
        initialScene.put("172150e8-6305-43a4-8a31-6a9a40426fc2", R.raw.pumpkin);
        initialScene.put("ed8ee00b-6efd-4971-9aed-9bd24987ea7f", R.raw.dragon);
        initialScene.put("f1d4144f-e0f8-4622-847b-1a6b709b56f1", R.raw.andy);
        initialScene.put("9fb68a63-c511-486d-a93b-cd96d21fef5e", R.raw.pumpkin);

        sceneLoader.locateObjects(initialScene);
    }

    @Override
    protected void onPause() {
        super.onPause();
        wandService.onPause();
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

                    showSpellAtLastTappedAnchor(R.raw.pumpkin, false, true, null);
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

                        float x = (float) stepsRemaining / ALL_STEPS;
                        float alpha = (float) Math.pow(x * Math.sin(Math.PI * x) / 0.59, 0.85);

                        damageOverlay.setAlpha(alpha);
                    }
                });

                if (stepsRemaining > 0)
                    handler.postDelayed(this, 15);
            }
        }, 15);
    }

    void movingSpell(AnchorNode anchorNode, int resource, boolean amICaster, Callback callback) {
        final Renderable[] renderable = new Renderable[1];
        ModelRenderable.builder()
                .setSource(this, resource)
                .build()
                .thenAccept(r -> renderable[0] = r)
                .exceptionally(throwable -> null);

        Frame frame = arFragment.getArSceneView().getArFrame();
        if (frame == null) return;
        Pose objectPose = lastTappedAnchorNode.getPose();
        Pose cameraPose = frame.getCamera().getPose();

        float dx = objectPose.tx() - cameraPose.tx();
        float dy = objectPose.ty() - cameraPose.ty();
        float dz = objectPose.tz() - cameraPose.tz();

        ///Compute the straight-line distance.
        float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * (amICaster ? 1f : -1f);

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
                        if(callback != null) callback.run();
                    } else {

                        float distance = distanceMeters * ((float) stepsRemaining / ALL_STEPS);


                        //Renderable renderable = ShapeFactory.makeSphere(radius, new Vector3(0.0f, 0.5f, 0.0f), color);
                        sphere = new TransformableNode(arFragment.getTransformationSystem());
                        sphere.getScaleController().setMaxScale(0.25f);
                        sphere.getScaleController().setMinScale(0.2f);
                        Vector3 vector3 = sphere.getWorldPosition();
                        Quaternion rotation = sphere.getWorldRotation();
                        rotation.y = 180;
                        vector3.z = (distance) - 1;
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

    void staticSpell(AnchorNode anchorNode, int resource, Callback callback) {
        final Renderable[] renderable = new Renderable[1];
        ModelRenderable.builder()
                .setSource(this, resource)
                .build()
                .thenAccept(r -> renderable[0] = r)
                .exceptionally(throwable -> null);

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
                        if(callback != null) callback.run();
                    } else {

                        //Renderable renderable = ShapeFactory.makeSphere(radius, new Vector3(0.0f, 0.5f, 0.0f), color);
                        sphere = new TransformableNode(arFragment.getTransformationSystem());
                        sphere.getScaleController().setMaxScale(0.45f);
                        sphere.getScaleController().setMinScale(0.4f);
                        /*Vector3 vector3 = sphere.getWorldPosition();
                        Quaternion rotation = sphere.getWorldRotation();
                        rotation.y = 180;
                        vector3.z = (distance)-1;
                        vector3.y = 0f;
                        sphere.setWorldPosition(vector3);
                        sphere.setWorldRotation(rotation);*/
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

    void showSpellAtLastTappedAnchor(int resource, boolean moving, boolean amICaster, Callback callback) {
        if (lastTappedAnchorNode == null)
            return;

        AnchorNode anchorNode = new AnchorNode(lastTappedAnchorNode);

        anchorNode.setParent(arFragment.getArSceneView().getScene());

        if (moving) {
            movingSpell(anchorNode, resource, amICaster, callback);
        } else {
            staticSpell(anchorNode, resource, callback);
        }
    }

    void updateHealthBar() {
        int parentWidth = ((FrameLayout) findViewById(R.id.health1).getParent()).getWidth();

        float percent = (float) health / MAX_HEALTH;
        int width = (int) (percent * parentWidth);

        findViewById(R.id.health1).setLayoutParams(new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
        findViewById(R.id.health2).setLayoutParams(new FrameLayout.LayoutParams(width, ViewGroup.LayoutParams.MATCH_PARENT));
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

    @Override
    public void spellCast(boolean amICaster, String spellName) {
        Resources res = WizardApp.getAppContext().getResources();

        if (spellName.equals(getString(R.string.spell_wand_failed))) {
            findViewById(R.id.status1).setBackgroundResource(android.R.color.holo_red_dark);
            findViewById(R.id.status2).setBackgroundResource(android.R.color.holo_red_dark);
            return;
        } else if (spellName.equals(getString(R.string.spell_speech_failed))) {
            findViewById(R.id.status2).setBackgroundResource(android.R.color.holo_orange_dark);
            findViewById(R.id.status1).setBackgroundResource(android.R.color.holo_orange_dark);
            return;
        } else {
            findViewById(R.id.status2).setBackgroundResource(android.R.color.transparent);
            findViewById(R.id.status1).setBackgroundResource(android.R.color.transparent);
        }

        // Ferocious Flames      red     moving
        // Sulfurous Smoke       red     static
        // Wrath of Waterfall    blue    moving
        // Mysterious Mist       blue    static
        // Revengefull Roses     green   moving
        // Fierce Forest         green   static

        boolean moving = spellName.equals(res.getStringArray(R.array.fire_moving)[0]) ||
                spellName.equals(res.getStringArray(R.array.water_moving)[0]) ||
                spellName.equals(res.getStringArray(R.array.plant_moving)[0]);

        runOnUiThread(() -> {
            showSpellAtLastTappedAnchor(R.raw.arzeninball, moving, amICaster, () -> {

                if (!amICaster) {

                    runOnUiThread(() -> {
                        health--;
                        updateHealthBar();

                        showDamageOverlay();
                    });
                }

            });
        });
    }

    interface Callback {
        void run();
    }
}
