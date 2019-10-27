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
import android.widget.LinearLayout;
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

        LinearLayout vrLayout = findViewById(R.id.vr_layout);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );

        params.setMargins(90, 0, 90, 0);
        //vrLayout.setLayoutParams(params);

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
        initialScene.put("6ef478f4-07e1-4c9f-bd11-285918ab344a", R.raw.dragon);
        initialScene.put("cc446848-7910-4ca3-837b-3b429db49c0a", R.raw.pumpkin);
        initialScene.put("7479f39e-0e29-460a-ae27-7f12bb22d1e4", R.raw.pumpkin);
        initialScene.put("28970a09-c09b-40f1-b93c-6629edd8dc85", R.raw.pumpkin);
        initialScene.put("f201f3ec-4cd8-4c23-a316-35117082abe9", R.raw.andy);

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
                        if (callback != null) callback.run();
                    } else {

                        float distance = distanceMeters * ((float) stepsRemaining / ALL_STEPS);


                        //Renderable renderable = ShapeFactory.makeSphere(radius, new Vector3(0.0f, 0.5f, 0.0f), color);
                        sphere = new TransformableNode(arFragment.getTransformationSystem());
                        sphere.getScaleController().setMaxScale(0.25f);
                        sphere.getScaleController().setMinScale(0.2f);
                        Vector3 vector3 = sphere.getWorldPosition();
                        Quaternion rotation = sphere.getWorldRotation();
                        rotation.y = 180;
                        vector3.z = (distance) - (amICaster ? -1f : -0.1f);
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

    void staticSpell(AnchorNode anchorNode, int resource, boolean amICaster, Callback callback) {
        if (amICaster) {
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
                            if (callback != null) callback.run();
                        } else {


                            float size = (0.3f * ((float) (ALL_STEPS - stepsRemaining) / ALL_STEPS)) + 0.1f;

                            //Renderable renderable = ShapeFactory.makeSphere(radius, new Vector3(0.0f, 0.5f, 0.0f), color);
                            sphere = new TransformableNode(arFragment.getTransformationSystem());
                            sphere.getScaleController().setMaxScale(size + 0.05f);
                            sphere.getScaleController().setMinScale(size);
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
        } else {

        }
    }

    void shieldSpell(AnchorNode anchorNode, int resource, boolean me) {
        final Renderable[] renderable = new Renderable[1];
        ModelRenderable.builder()
                .setSource(this, resource)
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
        float distanceMeters = (float) Math.sqrt(dx * dx + dy * dy + dz * dz) * (me ? 1f : -1f);

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

                        float distance = distanceMeters * ((float) stepsRemaining / ALL_STEPS);


                        //Renderable renderable = ShapeFactory.makeSphere(radius, new Vector3(0.0f, 0.5f, 0.0f), color);
                        sphere = new TransformableNode(arFragment.getTransformationSystem());
                        sphere.getScaleController().setMaxScale(0.25f);
                        sphere.getScaleController().setMinScale(0.2f);
                        Vector3 vector3 = sphere.getWorldPosition();
                        Quaternion rotation = sphere.getWorldRotation();
                        rotation.y = 180;
                        vector3.z = distance-0.3f;
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

    void showSpellAtLastTappedAnchor(int resource, boolean moving, boolean amICaster, Callback callback) {
        if (lastTappedAnchorNode == null)
            return;

        AnchorNode anchorNode = new AnchorNode(lastTappedAnchorNode);

        anchorNode.setParent(arFragment.getArSceneView().getScene());

        if (moving) {
            movingSpell(anchorNode, resource, amICaster, callback);
        } else {
            staticSpell(anchorNode, resource, amICaster, callback);
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
            int resource = 0;

            switch (spellName) {
                case "Ferocious Flames" :
                    resource = R.id.arzeninball;
                    break;
                case "Sulfurous Smoke" :
                    resource = R.id.drek;
                    break;
                case "Wrath of Waterfall" :
                    resource = R.id.pizza;
                    break;
                case "Mysterious Mist" :
                    resource = R.id.somestuff;
                    break;
                case "Revengefull Roses" :
                    resource = R.id.drek;
                    break;
                case "Fierce Forest" :
                    resource = R.id.grdatla;
                    break;
            }

            showSpellAtLastTappedAnchor(resource, moving, amICaster, () -> {

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

    @Override
    public void spellStatus(WandService.SpellResult result) {
        if (result == null) result = WandService.SpellResult.START;

        switch (result) {
            case START:
                findViewById(R.id.status2).setBackgroundResource(android.R.color.holo_purple);
                findViewById(R.id.status1).setBackgroundResource(android.R.color.holo_purple);
                break;

            case SPEECH_RECOGNISED:
                findViewById(R.id.status2).setBackgroundResource(android.R.color.holo_blue_dark);
                findViewById(R.id.status1).setBackgroundResource(android.R.color.holo_blue_dark);
                break;

            case WAND_FAILED:
                findViewById(R.id.status1).setBackgroundResource(android.R.color.holo_red_dark);
                findViewById(R.id.status2).setBackgroundResource(android.R.color.holo_red_dark);
                return;

            case SPEECH_FAILED:
                findViewById(R.id.status2).setBackgroundResource(android.R.color.holo_orange_dark);
                findViewById(R.id.status1).setBackgroundResource(android.R.color.holo_orange_dark);
                return;

            case SUCESSFUL:
                findViewById(R.id.status2).setBackgroundResource(android.R.color.holo_green_dark);
                findViewById(R.id.status1).setBackgroundResource(android.R.color.holo_green_dark);
                return;
        }

    }

    interface Callback {
        void run();
    }
}
