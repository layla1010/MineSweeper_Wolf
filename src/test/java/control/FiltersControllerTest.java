/**
 * FiltersControllerTest
 *
 * This test class verifies the correct behavior of the FiltersController,
 * which was implemented as part of the new iteration of the project.
 *
 * The purpose of this test is to validate that user interactions with
 * filter toggle buttons correctly update the global configuration stored
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
import javafx.scene.control.ToggleButton;
import model.SysData;

public class FiltersControllerTest {

    private FiltersController controller;
    private ToggleButton timerToggle;

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
        timerToggle = new ToggleButton();

        // inject timerToggle into controller (because it's @FXML private)
        Field f = FiltersController.class.getDeclaredField("timerToggle");
        f.setAccessible(true);
        f.set(controller, timerToggle);
    }

    /**
     * Test Case: onTimerToggled_WhenSelectedTrue_TimerEnabledBecomesTrue
     *
     * Test Objective:
     * To verify that when the Timer toggle button is selected (ON),
     * the corresponding configuration flag in SysData is updated correctly.
     *
     * Test Description:
     * This test simulates a user enabling the Timer option in the Filters screen.
     * The onTimerToggled() method is invoked, and the test verifies that
     * SysData.isTimerEnabled() becomes true.
     *
     * Test Steps:
     * 1. Reset all global settings in SysData to their default values.
     * 2. Explicitly disable the timer setting in SysData.
     * 3. Create a ToggleButton and set it to selected.
     * 4. Inject the ToggleButton into the FiltersController using reflection.
     * 5. Invoke the onTimerToggled() method on the JavaFX Application Thread.
     * 6. Verify that SysData.isTimerEnabled() is true.
     *
     * Expected Result:
     * SysData.isTimerEnabled() should be set to true.
     *
     * Actual Result:
     * SysData.isTimerEnabled() is true.
     *
     * Test Result:
     * PASS
     */

    @Test
    void testOnTimerToggled_WhenSelectedTrue_TimerEnabledBecomesTrue() throws Exception {
        SysData.setTimerEnabled(false);    
        timerToggle.setSelected(true);

        runOnFxThreadAndWait(() -> {
            try {
                Method m = FiltersController.class.getDeclaredMethod("onTimerToggled");
                m.setAccessible(true);
                m.invoke(controller);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        assertTrue(SysData.isTimerEnabled());
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

