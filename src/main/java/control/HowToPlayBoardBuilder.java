package control;

import javafx.geometry.HPos;
import javafx.geometry.VPos;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;

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
            cc.setHgrow(Priority.ALWAYS);
            grid.getColumnConstraints().add(cc);
        }

        for (int r = 0; r < rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setPercentHeight(rowPercent);
            rc.setValignment(VPos.CENTER);
            rc.setVgrow(Priority.ALWAYS);
            grid.getRowConstraints().add(rc);
        }

        StackPane[][] tiles = new StackPane[rows][cols];

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {

                Button btn = new Button();
                btn.setMinSize(0, 0);
                btn.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                btn.getStyleClass().addAll("cell-tile", "cell-hidden");
                btn.getStyleClass().add(isPlayer1 ? "p1-cell" : "p2-cell");

                btn.setDisable(true);
                btn.setFocusTraversable(false);

                StackPane tile = new StackPane(btn);
                tile.setMinSize(0, 0);
                tile.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

                GridPane.setHgrow(tile, Priority.ALWAYS);
                GridPane.setVgrow(tile, Priority.ALWAYS);

                GridPane.setFillWidth(tile, true);
                GridPane.setFillHeight(tile, true);
                GridPane.setHalignment(tile, HPos.CENTER);
                GridPane.setValignment(tile, VPos.CENTER);

                tiles[r][c] = tile;
                grid.add(tile, c, r);
            }
        }

        return tiles;
    }
}
