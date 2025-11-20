package model;

/**
 * Abstract base class that represents a single logical cell on the board.
 *
 * A cell contains:
 *   - its position (row, column)
 *   - a logical type (mine, question, surprise, empty, number)
 *   - the number of adjacent mines (0–8), which is calculated by the Board
 *
 * This class stores only the logical data.
 * Rendering / UI behavior is handled elsewhere (View/Controller layers).
 */
public abstract class Cell {

    private final int row;      // The row of the cell on the board
    private final int col;      // The column of the cell on the board
    private int adjacentMines;  // How many mines exist around this cell (0–8)

    /**
     * Base constructor. Must be called by all subclasses.
     *
     * @param row row index of this cell
     * @param col column index of this cell
     */
    protected Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.adjacentMines = 0; // default, will be updated later
    }

    // @return the logical type of this cell (MINE, QUESTION, EMPTY, NUMBER, SURPRISE)
    public abstract CellType getType();

    // @return the row index of this cell
    public int getRow() {
        return row;
    }

    // @return the column index of this cell
    public int getCol() {
        return col;
    }

    //@return number of adjacent mines
    public int getAdjacentMines() {
        return adjacentMines;
    }

   
    public void setAdjacentMines(int adjacentMines) {
        this.adjacentMines = adjacentMines;
    }

    /**
     * Helper method to quickly check if this cell is a mine.
     * @return true if cell type is MINE.
     */
    public boolean isMine() {
        return getType() == CellType.MINE;
    }
}
