package model;

//Represents a surprise cell. According to the game rules this cell can cause a good or bad surprise

public class SurpriseCell extends Cell {

    public SurpriseCell(int row, int col) {
        super(row, col);
    }

    @Override
    public CellType getType() {
        return CellType.SURPRISE;
    }
}
