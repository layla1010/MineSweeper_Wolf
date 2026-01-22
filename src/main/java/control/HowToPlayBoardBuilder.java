package control;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;


/*
 * HowToPlayBoardBuilder
 * ----------------------
 * Builds a responsive Minesweeper-like board for the "How To Play" screen.
 *
 * What this class does:
 * - Creates a GridPane board (rows x cols) where each cell is a StackPane containing a Button.
 * - Applies percent-based row/column constraints so the board scales nicely when the window resizes.
 * - Adds CSS classes to each cell button so the controller can later paint states (hidden/revealed/flag/etc).
 *
 * What this class does NOT do:
 * - No game logic, no mines placement, no turn management.
 * - Only UI construction.
 */
public class HowToPlayBoardBuilder {

    /*
     * build(...)
     * ----------
     * Builds and injects the board into the given GridPane.
     *
     * Steps:
     * 1) Clear any previous content and constraints from the GridPane.
     * 2) Create equal-width columns (percentWidth) and equal-height rows (percentHeight).
     * 3) Create a 2D array of tiles to allow access by [row][col].
     * 4) For each cell: create a tile and add it to GridPane at (col, row).
     *
     * Params:
     * - grid: The GridPane container that holds the board.
     * - rows/cols: Board dimensions.
     * - isPlayer1: Used only for styling (adds CSS class p1-cell or p2-cell).
     *
     * Returns:
     * - StackPane[][] tiles: Each tile wraps a Button (tile.getChildren().get(0)).
     */
    public StackPane[][] build(GridPane grid, int rows, int cols, boolean isPlayer1) {
        // Clear old board
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();

        // Use percentage sizing so the board is responsive
        double colPercent = 100.0 / cols;
        double rowPercent = 100.0 / rows;

        // Create equal-width columns
        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(colPercent);
            cc.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(cc);
        }

        // Create equal-height rows
        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(rowPercent);
            rc.setValignment(VPos.CENTER);
            grid.getRowConstraints().add(rc);
        }

        StackPane[][] tiles = new StackPane[rows][cols];

        // Create tiles and add them to the grid (note: GridPane uses (col, row))
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                StackPane tile = createCellTile(isPlayer1);
                tiles[r][c] = tile;
                grid.add(tile, c, r);
            }
        }

        return tiles;
    }

    /*
     * createCellTile(...)
     * -------------------
     * Creates one cell tile.
     *
     * Structure:
     * - StackPane (tile)
     *   - Button (cell)
     *
     * Why StackPane:
     * - Allows placing overlays above the button later (highlights, "used" overlay, etc.)
     *
     * Styling:
     * - Adds "cell-tile" and "cell-hidden" by default.
     * - Adds "p1-cell" OR "p2-cell" for player-specific styling.
     *
     * Resizing:
     * - Button and tile can grow to fill the GridPane cell.
     */
    private StackPane createCellTile(boolean isPlayer1) {
        Button button = new Button();

        // Make the button resizable (fill available space)
        button.setMinSize(0, 0);
        button.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Default CSS classes (hidden state)
        button.getStyleClass().addAll("cell-tile", "cell-hidden");

        // Player-specific CSS class
        button.getStyleClass().add(isPlayer1 ? "p1-cell" : "p2-cell");

        StackPane tile = new StackPane(button);

        // Make the tile resizable too
        tile.setMinSize(0, 0);
        tile.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        tile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        // Ensure the tile expands inside the GridPane
        GridPane.setHgrow(tile, Priority.ALWAYS);
        GridPane.setVgrow(tile, Priority.ALWAYS);

        return tile;
    }
    
    
}
