package control;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.layout.*;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public class HowToPlayBoardBuilder {

    public StackPane[][] build(GridPane grid, int rows, int cols, boolean isPlayer1) {
        grid.getChildren().clear();
        grid.getColumnConstraints().clear();
        grid.getRowConstraints().clear();

        double colPercent = 100.0 / cols;
        double rowPercent = 100.0 / rows;

        for (int c = 0; c < cols; c++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(colPercent);
            cc.setHalignment(HPos.CENTER);
            grid.getColumnConstraints().add(cc);
        }

        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(rowPercent);
            rc.setValignment(VPos.CENTER);
            grid.getRowConstraints().add(rc);
        }

        StackPane[][] tiles = new StackPane[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                StackPane tile = createCellTile(isPlayer1);
                tiles[r][c] = tile;
                grid.add(tile, c, r);
            }
        }

        return tiles;
    }

    private StackPane createCellTile(boolean isPlayer1) {
        Button button = new Button();
        button.setMinSize(0, 0);
        button.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        button.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        button.getStyleClass().addAll("cell-tile", "cell-hidden");
        button.getStyleClass().add(isPlayer1 ? "p1-cell" : "p2-cell");

        StackPane tile = new StackPane(button);
        tile.setMinSize(0, 0);
        tile.setPrefSize(Region.USE_COMPUTED_SIZE, Region.USE_COMPUTED_SIZE);
        tile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        GridPane.setHgrow(tile, Priority.ALWAYS);
        GridPane.setVgrow(tile, Priority.ALWAYS);

        return tile;
    }
}