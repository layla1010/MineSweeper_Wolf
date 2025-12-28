
package model;

//Abstract base class that represents a single logical cell on the board.
public abstract class Cell {

    private final int row;      // The row of the cell on the board
    private final int col;      // The column of the cell on the board
    private int adjacentMines;  // How many mines exist around this cell (0–8)
	private CellType type;

  
    protected Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.adjacentMines = 0; // default, will be updated later
    }

    //return the logical type of this cell (MINE, QUESTION, EMPTY, NUMBER, SURPRISE)
    public abstract CellType getType();


    public int getRow() {
        return row;
    }


    public int getCol() {
        return col;
    }


    public int getAdjacentMines() {
        return adjacentMines;
    }

    public void setAdjacentMines(int adjacentMines) {
        this.adjacentMines = adjacentMines;
    }
    
    public void setType(CellType type) {
        this.type = type;
    }

   
   //Helper method to quickly check if this cell is a mine.
    public boolean isMine() {
        return getType() == CellType.MINE;
    }
    
    public boolean isEmpty() {
        return getType() == CellType.EMPTY;
    }
}
