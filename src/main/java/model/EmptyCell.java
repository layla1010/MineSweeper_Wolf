package model;


//Represents a regular empty cell.
//The cell is not a mine, not a question and not a surprise.

public class EmptyCell extends Cell {
    public EmptyCell(int row, int col) {
        super(row, col);
    }

    @Override
    public CellType getType() {
        return CellType.EMPTY;
    }
}
