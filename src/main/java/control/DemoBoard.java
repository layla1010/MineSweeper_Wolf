package control;

public class DemoBoard {
    public final int rows;
    public final int cols;
    public final DemoCellType[][] cells;

    public DemoBoard(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        this.cells = new DemoCellType[rows][cols];

        // default hidden
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) cells[r][c] = DemoCellType.HIDDEN;
        }

        
        cells[1][1] = DemoCellType.NUMBER_1;
        cells[1][2] = DemoCellType.MINE;
        cells[2][2] = DemoCellType.QUESTION;
        cells[3][3] = DemoCellType.SURPRISE;
        cells[4][1] = DemoCellType.EMPTY;
    }
}
