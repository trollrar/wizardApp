package si.fullin.wizardapp.ar;

import android.app.Activity;
import android.view.Gravity;
import android.widget.Toast;
import com.google.ar.sceneform.ArSceneView;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.microsoft.azure.spatialanchors.*;
import si.fullin.wizardapp.R;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SceneLoader {
    private final ConcurrentHashMap<String, AnchorVisual> anchorVisuals = new ConcurrentHashMap<>();
    private AzureSpatialAnchorsManager cloudAnchorManager;

    // UI Elements
    private ArFragment arFragment;
    private ArSceneView sceneView;
    private Activity activity;

    private Map<Integer, ModelRenderable> renderables = new HashMap<>();
    private Map<String, Integer> objects = new HashMap<>();

    @SuppressWarnings({"AndroidApiChecker", "FutureReturnValueIgnored"})
    public void onCreate(Activity activity, ArFragment arFragment) {
        this.activity = activity;
        this.arFragment = arFragment;
        sceneView = arFragment.getArSceneView();

        Scene scene = sceneView.getScene();
        scene.addOnUpdateListener(frameTime -> {
            if (cloudAnchorManager != null) {
                // Pass frames to Spatial Anchors for processing.
                cloudAnchorManager.update(sceneView.getArFrame());
            }
        });

        for (int resourceId : new int[]{R.raw.dragon, R.raw.dragon_big, R.raw.andy, R.raw.pumpkin}) {
            ModelRenderable.builder()
                    .setSource(activity, resourceId)
                    .build()
                    .thenAccept(renderable -> renderables.put(resourceId, renderable))
                    .exceptionally(
                            throwable -> {
                                Toast toast =
                                        Toast.makeText(activity, "Unable to load andy renderable", Toast.LENGTH_LONG);
                                toast.setGravity(Gravity.CENTER, 0, 0);
                                toast.show();
                                return null;
                            });
        }

        MaterialFactory.makeOpaqueWithColor(activity, new Color(android.graphics.Color.rgb(229, 119, 2)))
                .thenAccept(material -> {
                    renderables.get(R.raw.pumpkin).setMaterial(material);
                });

        MaterialFactory.makeOpaqueWithColor(activity, new Color(android.graphics.Color.rgb(56, 32, 26)))
                .thenAccept(material -> {
                    renderables.get(R.raw.dragon).setMaterial(material);
                    renderables.get(R.raw.dragon_big).setMaterial(material);
                });
    }

    public void onDestroy() {
        destroySession();
    }

    public void locateObjects(Map<String, Integer> objects) {
        // ArFragment of Sceneform automatically requests the camera permission before creating the AR session,
        // so we don't need to request the camera permission explicitly.
        // This will cause onResume to be called again after the user responds to the permission request.
        if (!SceneformHelper.hasCameraPermission(activity)) {
            return;
        }

        if (sceneView != null && sceneView.getSession() == null) {
            SceneformHelper.setupSessionForSceneView(activity, sceneView);
        }

        startNewSession();

        AnchorLocateCriteria criteria = new AnchorLocateCriteria();
        criteria.setIdentifiers(objects.keySet().toArray(new String[0]));
        this.objects.putAll(objects);

        stopWatcher();

        cloudAnchorManager.startLocating(criteria);
    }

    private void destroySession() {
        if (cloudAnchorManager != null) {
            cloudAnchorManager.stop();
            cloudAnchorManager = null;
        }

        for (AnchorVisual visual : anchorVisuals.values()) {
            visual.destroy();
        }

        anchorVisuals.clear();
    }

    private void onAnchorLocated(AnchorLocatedEvent event) {
        LocateAnchorStatus status = event.getStatus();
        if (status == LocateAnchorStatus.Located)
            activity.runOnUiThread(() -> renderLocatedAnchor(event.getAnchor()));
    }

    private void onLocateAnchorsCompleted(LocateAnchorsCompletedEvent ignored) {
        stopWatcher();
    }

    private void renderLocatedAnchor(CloudSpatialAnchor anchor) {
        AnchorVisual foundVisual = new AnchorVisual(anchor.getLocalAnchor());
        foundVisual.setCloudAnchor(anchor);
        foundVisual.getAnchorNode().setParent(arFragment.getArSceneView().getScene());
        String cloudAnchorIdentifier = foundVisual.getCloudAnchor().getIdentifier();
        foundVisual.render(arFragment, renderables.get(objects.get(cloudAnchorIdentifier)));
        anchorVisuals.put(cloudAnchorIdentifier, foundVisual);
    }

    private void startNewSession() {
        destroySession();

        cloudAnchorManager = new AzureSpatialAnchorsManager(sceneView.getSession());
        cloudAnchorManager.addAnchorLocatedListener(this::onAnchorLocated);
        cloudAnchorManager.addLocateAnchorsCompletedListener(this::onLocateAnchorsCompleted);
        cloudAnchorManager.start();
    }

    private void stopWatcher() {
        if (cloudAnchorManager != null) {
            cloudAnchorManager.stopLocating();
        }
    }
}