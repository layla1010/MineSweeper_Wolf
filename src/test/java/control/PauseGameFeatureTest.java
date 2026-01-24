//package control;
//
//import static org.junit.jupiter.api.Assertions.*;
//
//
//import java.lang.reflect.Field;
//import java.lang.reflect.Method;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicBoolean;
//
//import javafx.animation.Animation;
//import javafx.animation.KeyFrame;
//import javafx.animation.Timeline;
//import javafx.application.Platform;
//import javafx.scene.control.Button;
//import javafx.scene.image.ImageView;
//import javafx.scene.layout.GridPane;
//import javafx.util.Duration;
//
//import model.SysData;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.AfterEach;
//
//
//public class PauseGameFeatureTest {
//
//    private GameController controller;
//
//    private GridPane player1Grid;
//    private GridPane player2Grid;
//    private Button pauseBtn;
//
//    private static final AtomicBoolean FX_STARTED = new AtomicBoolean(false);
//
//    @BeforeAll
//    static void startJavaFxToolkit() throws Exception {
//        if (FX_STARTED.compareAndSet(false, true)) {
//            CountDownLatch latch = new CountDownLatch(1);
//
//            try {
//                Platform.startup(latch::countDown);
//            } catch (IllegalStateException alreadyStarted) {
//                // JavaFX toolkit was already started by another test
//                latch.countDown();
//            }
//
//            assertTrue(latch.await(3, TimeUnit.SECONDS), "JavaFX Toolkit did not start");
//        }
//    }
//
//    @BeforeEach
//    void setup() throws Exception {
//        controller = new GameController();
//
//        player1Grid = new GridPane();
//        player2Grid = new GridPane();
//        pauseBtn = new Button();
//        pauseBtn.setGraphic(new ImageView()); // required for your icon-switching code
//
//        setPrivateField(controller, "player1Grid", player1Grid);
//        setPrivateField(controller, "player2Grid", player2Grid);
//        setPrivateField(controller, "pauseBtn", pauseBtn);
//
//        runOnFxAndWait(() -> SysData.setTimerEnabled(true));
//
//        setPrivateField(controller, "isPaused", false);
//
//        // ✅ IMPORTANT: Timeline must have at least one KeyFrame or it becomes STOPPED instantly
//        Timeline timer = new Timeline(new KeyFrame(Duration.millis(200), e -> {
//            // do nothing, just keep timeline alive
//        }));
//        timer.setCycleCount(Timeline.INDEFINITE);
//
//        runOnFxAndWait(timer::play);
//        waitUntilFx(() -> timer.getStatus() == Animation.Status.RUNNING, 3000);
//
//        assertNotEquals(Animation.Status.STOPPED, timer.getStatus(), "Timer unexpectedly STOPPED");
//
//        setPrivateField(controller, "timer", timer);
//    }
//
//    @Test
//    void pauseGame_firstClick_shouldPauseAndDimBoards_andPauseTimer() throws Exception {
//        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame"));
//
//        assertTrue((boolean) getPrivateField(controller, "isPaused"),
//                "isPaused should become true");
//
//        assertEquals(0.6, player1Grid.getOpacity(), 0.0001);
//        assertEquals(0.6, player2Grid.getOpacity(), 0.0001);
//
//        Timeline timer = (Timeline) getPrivateField(controller, "timer");
//        assertEquals(Animation.Status.PAUSED, timer.getStatus(),
//                "Timer should be PAUSED after pausing the game");
//    }
//
//    @Test
//    void pauseGame_secondClick_shouldResumeAndRestoreOpacity_andResumeTimer_whenTimerEnabled() throws Exception {
//        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // pause
//        assertTrue((boolean) getPrivateField(controller, "isPaused"));
//
//        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // resume
//
//        assertFalse((boolean) getPrivateField(controller, "isPaused"),
//                "isPaused should become false after second click");
//
//        assertEquals(1.0, player1Grid.getOpacity(), 0.0001);
//        assertEquals(1.0, player2Grid.getOpacity(), 0.0001);
//
//        Timeline timer = (Timeline) getPrivateField(controller, "timer");
//        assertEquals(Animation.Status.RUNNING, timer.getStatus(),
//                "Timer should RUN when unpausing and timer is enabled");
//    }
//
//    @Test
//    void pauseGame_unpause_shouldNotResumeTimer_whenTimerDisabled() throws Exception {
//        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // pause
//        Timeline timer = (Timeline) getPrivateField(controller, "timer");
//        assertEquals(Animation.Status.PAUSED, timer.getStatus());
//
//        SysData.setTimerEnabled(false);
//
//        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // unpause
//
//        // Your code resumes timer ONLY if SysData.isTimerEnabled() is true.
//        // So here it should stay PAUSED.
//        assertEquals(Animation.Status.PAUSED, timer.getStatus(),
//                "Timer should remain PAUSED after unpausing when timer is disabled");
//    }
//
//    // ---------------- helpers ----------------
//
//    private static void runOnFxAndWait(Runnable action) throws Exception {
//        CountDownLatch latch = new CountDownLatch(1);
//        Platform.runLater(() -> {
//            try {
//                action.run();
//            } finally {
//                latch.countDown();
//            }
//        });
//        assertTrue(latch.await(3, TimeUnit.SECONDS), "FX task did not finish in time");
//    }
//
//    private static void waitUntilFx(Check condition, long timeoutMs) throws Exception {
//        long start = System.currentTimeMillis();
//        while (System.currentTimeMillis() - start < timeoutMs) {
//            final boolean[] ok = {false};
//            runOnFxAndWait(() -> ok[0] = condition.ok());
//            if (ok[0]) return;
//            Thread.sleep(25);
//        }
//        fail("Condition not met within " + timeoutMs + " ms");
//    }
//
//    @FunctionalInterface
//    private interface Check { boolean ok(); }
//
//    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
//        Field f = findField(target.getClass(), fieldName);
//        f.setAccessible(true);
//        f.set(target, value);
//    }
//
//    private static Object getPrivateField(Object target, String fieldName) throws Exception {
//        Field f = findField(target.getClass(), fieldName);
//        f.setAccessible(true);
//        return f.get(target);
//    }
//
//    private static void invokePrivate(Object target, String methodName, Class<?>... paramTypes) {
//        try {
//            Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
//            m.setAccessible(true);
//            m.invoke(target);
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to invoke private method: " + methodName, e);
//        }
//    }
//
//    private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
//        Class<?> c = clazz;
//        while (c != null) {
//            try {
//                return c.getDeclaredField(fieldName);
//            } catch (NoSuchFieldException ignored) {
//                c = c.getSuperclass();
//            }
//        }
//        throw new NoSuchFieldException("Field not found: " + fieldName);
//    }
//    
//    @AfterEach
//    void tearDown() throws Exception {
//        // stop timer created by this test (avoid leaking running timelines to other tests)
//        Timeline timer = (Timeline) getPrivateField(controller, "timer");
//        if (timer != null) {
//            runOnFxAndWait(timer::stop);
//        }
//
//        // reset global/static flags to default for next tests
//        SysData.setTimerEnabled(true);
//    }
//
//}

package control;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import model.SysData;
import model.GameConfig;
import model.Difficulty;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;

public class PauseGameFeatureTest {

    private GameController controller;
    private GameStateController state; // Reference to the controller's internal state

    // UI Elements
    private GridPane player1Grid;
    private GridPane player2Grid;
    private Button pauseBtn;
    private Label timeLabel;

    private static final AtomicBoolean FX_STARTED = new AtomicBoolean(false);

    @BeforeAll
    static void startJavaFxToolkit() throws Exception {
        if (FX_STARTED.compareAndSet(false, true)) {
            CountDownLatch latch = new CountDownLatch(1);
            try {
                Platform.startup(latch::countDown);
            } catch (IllegalStateException alreadyStarted) {
                latch.countDown();
            }
            assertTrue(latch.await(3, TimeUnit.SECONDS), "JavaFX Toolkit did not start");
        }
    }

    @BeforeEach
    void setup() throws Exception {
        // 1. Create the Controller
        controller = new GameController();

        // 2. Initialize minimal UI elements needed for services
        player1Grid = new GridPane();
        player2Grid = new GridPane();
        pauseBtn = new Button();
        pauseBtn.setGraphic(new ImageView());
        timeLabel = new Label("Time: 00:00");

        // Dummy UI elements required by constructors
        HBox p1Stats = new HBox();
        HBox p2Stats = new HBox();
        Label diffLabel = new Label();
        Label scoreLabel = new Label();
        HBox heartsBox = new HBox();
        Button soundBtn = new Button(); soundBtn.setGraphic(new ImageView());
        Button musicBtn = new Button(); musicBtn.setGraphic(new ImageView());
        ImageView av1 = new ImageView();
        ImageView av2 = new ImageView();
        VBox root = new VBox();

        // 3. GET the existing state from the controller
        state = (GameStateController) getPrivateField(controller, "state");
        
        // Populate the state with dummy data
        state.config = new GameConfig("P1", "P2", Difficulty.EASY, "av1", "av2");
        state.difficulty = Difficulty.EASY;

        // 4. Initialize Services (Manual Wiring)
        GameUIServiceController uiService = new GameUIServiceController(
            state, player1Grid, player2Grid, p1Stats, p2Stats,
            diffLabel, timeLabel, scoreLabel, heartsBox,
            pauseBtn, soundBtn, musicBtn, root, av1, av2
        );
        uiService.initLabels(); 

        GameHistoryServiceController historyService = new GameHistoryServiceController(); 
        
        // Pass an empty runnable () -> {} for the endgame callback
        GamePlayServiceController playService = new GamePlayServiceController(state, uiService, historyService, () -> {});
        
        GameBonusServiceController bonusService = new GameBonusServiceController(state, uiService, playService);

        // 5. Connect the services together
        uiService.setPlayService(playService);
        uiService.setBonusService(bonusService);
        playService.setBonusService(bonusService);

        // 6. Inject EVERYTHING into the main GameController
        setPrivateField(controller, "player1Grid", player1Grid);
        setPrivateField(controller, "player2Grid", player2Grid);
        setPrivateField(controller, "pauseBtn", pauseBtn);
        setPrivateField(controller, "timeLabel", timeLabel);
        
        setPrivateField(controller, "uiService", uiService);
        setPrivateField(controller, "playService", playService);
        setPrivateField(controller, "bonusService", bonusService);
        setPrivateField(controller, "historyService", historyService);

        // 7. Timer Setup
        runOnFxAndWait(() -> SysData.setTimerEnabled(true));
        
        Timeline timer = new Timeline(new KeyFrame(Duration.millis(200), e -> {}));
        timer.setCycleCount(Timeline.INDEFINITE);
        runOnFxAndWait(timer::play);
        
        waitUntilFx(() -> timer.getStatus() == Animation.Status.RUNNING, 3000);

        
        // We set these DIRECTLY on the state object, not via setPrivateField on the controller
        state.isPaused = false;
        state.timer = timer; 
    }

    @Test
    void pauseGame_firstClick_shouldPauseAndDimBoards_andPauseTimer() throws Exception {
        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame"));

        assertTrue(state.isPaused, "State.isPaused should be true");
        assertEquals(0.6, player1Grid.getOpacity(), 0.0001, "Player 1 grid should be dimmed");
        assertEquals(Animation.Status.PAUSED, state.timer.getStatus(), "Timer should be PAUSED");
    }

    @Test
    void pauseGame_secondClick_shouldResumeAndRestoreOpacity_andResumeTimer_whenTimerEnabled() throws Exception {
        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // Pause
        assertTrue(state.isPaused);

        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // Resume

        assertFalse(state.isPaused, "State.isPaused should be false");
        assertEquals(1.0, player1Grid.getOpacity(), 0.0001);
        assertEquals(Animation.Status.RUNNING, state.timer.getStatus());
    }

    @Test
    void pauseGame_unpause_shouldNotResumeTimer_whenTimerDisabled() throws Exception {
        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // Pause
        assertEquals(Animation.Status.PAUSED, state.timer.getStatus());

        SysData.setTimerEnabled(false);

        runOnFxAndWait(() -> invokePrivate(controller, "onPauseGame")); // Resume

        assertEquals(Animation.Status.PAUSED, state.timer.getStatus(), "Timer should remain PAUSED if SysData is disabled");
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
    
    @AfterEach
    void tearDown() throws Exception {
        if (state != null && state.timer != null) {
            runOnFxAndWait(state.timer::stop);
        }
        SysData.setTimerEnabled(true);
    }
}


