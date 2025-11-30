package model;

//Represents a mine cell. When revealed, it usually causes losing a life / ending the game, depending on the game rules.


public class MineCell extends Cell {

    public MineCell(int row, int col) {
        super(row, col);
    }

    @Override
    public CellType getType() {
        return CellType.MINE;
    }
}
