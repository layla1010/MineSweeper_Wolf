package model;

/**
 * Represents a numbered cell (1–8).
 * This cell appears when the cell is not a mine but has
 * at least one adjacent mine.
 */
public class NumberCell extends Cell {

    public NumberCell(int row, int col) {
        super(row, col);
    }

    @Override
    public CellType getType() {
        return CellType.NUMBER;
    }
}
