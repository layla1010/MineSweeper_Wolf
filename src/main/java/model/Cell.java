package model;


public class Cell {

    private CellType type = CellType.EMPTY;
    private int neighborMines = 0;

    public CellType getType() {
        return type;
    }

    public void setType(CellType type) {
        this.type = type;
    }

    public int getNeighborMines() {
        return neighborMines;
    }

    public void setNeighborMines(int neighborMines) {
        this.neighborMines = neighborMines;
        if (neighborMines > 0 && type == CellType.EMPTY) {
            this.type = CellType.NUMBER;
        }
    }

    public boolean isMine() {
        return type == CellType.MINE;
    }
}

