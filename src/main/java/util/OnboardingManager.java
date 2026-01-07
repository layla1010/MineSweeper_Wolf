package util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.prefs.Preferences;

public final class OnboardingManager {

    private static final Preferences PREFS =
            Preferences.userNodeForPackage(OnboardingManager.class);

    private static final String KEY_PREFIX = "onboarding.done.";

    private OnboardingManager() {}

    public static boolean isDone(String flowKey, String userKey) {
        if (userKey == null || userKey.isBlank()) return false; // guest/admin -> never "done"
        String uk = shortKey(userKey);
        return PREFS.getBoolean(KEY_PREFIX + flowKey + "." + uk, false);
    }

    private static String shortKey(String s) {
        if (s == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(s.getBytes(StandardCharsets.UTF_8));

            StringBuilder sb = new StringBuilder(24);
            for (int i = 0; i < 12; i++) sb.append(String.format("%02x", hash[i]));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    public static void markDone(String flowKey, String userKey) {
        if (userKey == null || userKey.isBlank()) return;
        String uk = shortKey(userKey);
        PREFS.putBoolean(KEY_PREFIX + flowKey + "." + uk, true);
    }

    public static void reset(String flowKey, String userKey) {
        if (userKey == null || userKey.isBlank()) return;
        String uk = shortKey(userKey);
        PREFS.remove(KEY_PREFIX + flowKey + "." + uk);
    }
    
    public static void runWithPolicy(
            String flowKey,
            Parent root,
            List<OnboardingStep> steps,
            OnboardingPolicy policy,
            String userKey
    ) {
        runWithPolicy(flowKey, root, steps, policy, userKey, null);
    }


    public static void runWithPolicy(
            String flowKey,
            Parent root,
            List<OnboardingStep> steps,
            OnboardingPolicy policy,
            String userKey,
            Runnable afterClose
    ) {
        Objects.requireNonNull(flowKey);
        Objects.requireNonNull(root);
        Objects.requireNonNull(steps);
        Objects.requireNonNull(policy);

        if (steps.isEmpty()) return;
        if (policy == OnboardingPolicy.NEVER) return;

        if (policy == OnboardingPolicy.ALWAYS) {
        	Platform.runLater(() -> start(flowKey, root, steps, null, afterClose));
            return;
        }

        boolean done = isDone(flowKey, userKey);
        if (!done) {
        	Platform.runLater(() -> start(flowKey, root, steps, userKey, afterClose));
        } else {
            Platform.runLater(() -> installHoverHints(root, steps));
        }
    }

    private static void installHoverHints(Parent root, List<OnboardingStep> steps) {
        for (OnboardingStep s : steps) {
            String selector = s.getTargetSelector();
            if (selector == null || selector.isBlank()) continue;

            Node target = root.lookup(selector);
            if (target == null) continue;

            javafx.scene.control.Tooltip tip = new javafx.scene.control.Tooltip(
                    s.getTitle() + "\n" + s.getText()
            );
            tip.setWrapText(true);
            tip.setMaxWidth(320);

            javafx.scene.control.Tooltip.install(target, tip);
        }
    }

    public static void start(String flowKey, Parent root, List<OnboardingStep> steps, String userKey, Runnable afterClose) {
        Scene scene = root.getScene();
        if (scene == null) return;

        final int[] idx = {0};
        AtomicReference<OnboardingOverlay> overlayRef = new AtomicReference<>();

        Runnable close = () -> {
            OnboardingOverlay o = overlayRef.get();
            if (o != null) OnboardingOverlay.detachFromScene(scene, o);
            if (afterClose != null) afterClose.run();
        };

        
        OnboardingOverlay overlay = new OnboardingOverlay(
                v -> { // finish
                    markDone(flowKey, userKey);
                    close.run();
                },
                () -> { // back
                    if (idx[0] > 0) idx[0]--;
                    OnboardingOverlay o = overlayRef.get();
                    if (o != null) render(o, root, steps, idx[0]);
                },
                () -> { // next
                    if (idx[0] < steps.size() - 1) idx[0]++;
                    OnboardingOverlay o = overlayRef.get();
                    if (o != null) render(o, root, steps, idx[0]);
                },
                () -> { // skip
                    markDone(flowKey, userKey);
                    close.run();
                }
        );

        overlayRef.set(overlay);

        // Attach overlay INSIDE the scene graph (no popup)
        OnboardingOverlay.attachToScene(scene, overlay);

        // Render after layout is ready
        Platform.runLater(() -> render(overlay, root, steps, idx[0]));
    }

    private static void render(OnboardingOverlay overlay, Parent root, List<OnboardingStep> steps, int index) {
        root.applyCss();
        root.layout();

        OnboardingStep step = steps.get(index);
        Node target = null;

        String selector = step.getTargetSelector();
        if (selector != null && !selector.isBlank()) {
            target = root.lookup(selector);
        }

        overlay.renderStep(steps, index, target);
    }
}
