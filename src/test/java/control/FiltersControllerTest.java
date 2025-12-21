/**
 * FiltersControllerTest
 *
 * This test class verifies the correct behavior of the FiltersController,
 * which was implemented as part of the new iteration of the project.
 *
 * The purpose of this test is to validate that user interactions with
 * filter toggle images correctly update the global configuration stored
 * in the SysData class.
 *
 * The test focuses on unit-level behavior only and does not involve
 * full UI rendering or navigation between screens.
 *
 * Testing Framework: JUnit 5
 * Test Type: Unit Test
 */

package control;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.image.ImageView;
import model.SysData;

public class FiltersControllerTest {

    private FiltersController controller;
    private ImageView timerToggle;

    @BeforeAll
    static void initJavaFxToolkit() {
        try {
            Platform.startup(() -> {}); // initialize JavaFX toolkit once
        } catch (IllegalStateException e) {
            // toolkit already initialized
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        SysData.resetToDefaults();

        controller = new FiltersController();
        timerToggle = new ImageView();

        // inject timerToggle into controller (because it's @FXML private)
        Field f = FiltersController.class.getDeclaredField("timerToggle");
        f.setAccessible(true);
        f.set(controller, timerToggle);
    }

    /**
     * Test Case: onTimerToggled_FromFalse_ToTrue
     *
     * Test Objective:
     * Verify that when the timer is currently disabled in SysData,
     * calling onTimerToggled() flips it to enabled.
     *
     * Expected Result:
     * SysData.isTimerEnabled() becomes true and the timer toggle image is updated.
     */
    @Test
    void testOnTimerToggled_FromFalse_ToTrue() throws Exception {
        SysData.setTimerEnabled(false);

        runOnFxThreadAndWait(() -> invokePrivate(controller, "onTimerToggled"));

        assertTrue(SysData.isTimerEnabled());
        assertNotNull(timerToggle.getImage()); // UI state updated (switch image set)
    }

    /**
     * Test Case: onTimerToggled_FromTrue_ToFalse
     *
     * Test Objective:
     * Verify that when the timer is currently enabled in SysData,
     * calling onTimerToggled() flips it to disabled.
     *
     * Expected Result:
     * SysData.isTimerEnabled() becomes false and the timer toggle image is updated.
     */
    @Test
    void testOnTimerToggled_FromTrue_ToFalse() throws Exception {
        SysData.setTimerEnabled(true);

        runOnFxThreadAndWait(() -> invokePrivate(controller, "onTimerToggled"));

        assertFalse(SysData.isTimerEnabled());
        assertNotNull(timerToggle.getImage());
    }

    // helper: invoke private method via reflection
    private static void invokePrivate(Object obj, String methodName) {
        try {
            Method m = obj.getClass().getDeclaredMethod(methodName);
            m.setAccessible(true);
            m.invoke(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // helper: run code on JavaFX thread and wait
    private static void runOnFxThreadAndWait(Runnable action) throws InterruptedException {
        if (Platform.isFxApplicationThread()) {
            action.run();
            return;
        }

        CountDownLatch latch = new CountDownLatch(1);
        final RuntimeException[] error = new RuntimeException[1];

        Platform.runLater(() -> {
            try { action.run(); }
            catch (RuntimeException e) { error[0] = e; }
            finally { latch.countDown(); }
        });

        latch.await();
        if (error[0] != null) throw error[0];
    }
}
