package control;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.Duration;

import model.SysData;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PauseGameFeatureTest {

    private GameController controller;

    private GridPane player1Grid;
    private GridPane player2Grid;
    private Button pauseBtn;

    @BeforeAll
    static void startJavaFxToolkit() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.startup(latch::countDown);
        assertTrue(latch.await(3, TimeUnit.SECONDS), "JavaFX Toolkit did not start");
    }

    @BeforeEach
    void setup() throws Exception {
        controller = new GameController();

        player1Grid = new GridPane();
        player2Grid = new GridPane();
        pauseBtn = new Button();
        pauseBtn.setGraphic(new ImageView()); // required for your icon-switching code

        setPrivateField(controller, "player1Grid", player1Grid);
        setPrivateField(controller, "player2Grid", player2Grid);
        setPrivateField(controller, "pauseBtn", pauseBtn);

        SysData.setTimerEnabled(true);
        setPrivateField(controller, "isPaused", false);

        // ✅ IMPORTANT: Timeline must have at least one KeyFrame or it becomes STOPPED instantly
        Timeline timer = new Timeline(new KeyFrame(Duration.millis(200), e -> {
            // do nothing, just keep timeline alive
        }));
        timer.setCycleCount(Timeline.INDEFINITE);

        runOnFxAndWait(timer::play);
        waitUntilFx(() -> timer.getStatus() == Animation.Status.RUNNING, 1500);

        setPrivateField(controller, "timer", timer);
    }

    @Test
    void pauseGame_firstClick_shouldPauseAndDimBoards_andPauseTimer() throws Exception {
        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame"));

        assertTrue((boolean) getPrivateField(controller, "isPaused"),
                "isPaused should become true");

        assertEquals(0.6, player1Grid.getOpacity(), 0.0001);
        assertEquals(0.6, player2Grid.getOpacity(), 0.0001);

        Timeline timer = (Timeline) getPrivateField(controller, "timer");
        assertEquals(Animation.Status.PAUSED, timer.getStatus(),
                "Timer should be PAUSED after pausing the game");
    }

    @Test
    void pauseGame_secondClick_shouldResumeAndRestoreOpacity_andResumeTimer_whenTimerEnabled() throws Exception {
        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // pause
        assertTrue((boolean) getPrivateField(controller, "isPaused"));

        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // resume

        assertFalse((boolean) getPrivateField(controller, "isPaused"),
                "isPaused should become false after second click");

        assertEquals(1.0, player1Grid.getOpacity(), 0.0001);
        assertEquals(1.0, player2Grid.getOpacity(), 0.0001);

        Timeline timer = (Timeline) getPrivateField(controller, "timer");
        assertEquals(Animation.Status.RUNNING, timer.getStatus(),
                "Timer should RUN when unpausing and timer is enabled");
    }

    @Test
    void pauseGame_unpause_shouldNotResumeTimer_whenTimerDisabled() throws Exception {
        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // pause
        Timeline timer = (Timeline) getPrivateField(controller, "timer");
        assertEquals(Animation.Status.PAUSED, timer.getStatus());

        SysData.setTimerEnabled(false);

        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // unpause

        // Your code resumes timer ONLY if SysData.isTimerEnabled() is true.
        // So here it should stay PAUSED.
        assertEquals(Animation.Status.PAUSED, timer.getStatus(),
                "Timer should remain PAUSED after unpausing when timer is disabled");
    }

    // ---------------- helpers ----------------

    private static void runOnFxAndWait(Runnable action) throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                latch.countDown();
            }
        });
        assertTrue(latch.await(3, TimeUnit.SECONDS), "FX task did not finish in time");
    }

    private static void waitUntilFx(Check condition, long timeoutMs) throws Exception {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            final boolean[] ok = {false};
            runOnFxAndWait(() -> ok[0] = condition.ok());
            if (ok[0]) return;
            Thread.sleep(25);
        }
        fail("Condition not met within " + timeoutMs + " ms");
    }

    @FunctionalInterface
    private interface Check { boolean ok(); }

    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    private static Object getPrivateField(Object target, String fieldName) throws Exception {
        Field f = findField(target.getClass(), fieldName);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void invokePrivate(Object target, String methodName, Class<?>... paramTypes) {
        try {
            Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
            m.setAccessible(true);
            m.invoke(target);
        } catch (Exception e) {
            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
        Class<?> c = clazz;
        while (c != null) {
            try {
                return c.getDeclaredField(fieldName);
            } catch (NoSuchFieldException ignored) {
                c = c.getSuperclass();
            }
        }
        throw new NoSuchFieldException("Field not found: " + fieldName);
    }
}
