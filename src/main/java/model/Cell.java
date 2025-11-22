package model;

/**
* Abstract base class that represents a single logical cell on the board.
* 
* Every cell has:
*  - a type (mine, question, surprise, empty)
*  - its coordinates (row, column)
*  - the number of adjacent mines (1-8 or 0)
*  
* The view/controller decide how to display and reveal the cell,
* this class only stores the logical state.
*/
public abstract class Cell {

 private final int row;
 private final int col;
 private int adjacentMines; // number of mines in the 8 neighbours

 protected Cell(int row, int col) {
     this.row = row;
     this.col = col;
     this.adjacentMines = 0;
 }

 /**
  * @return the logical type of this cell.
  */
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

 /**
  * Sets the number of adjacent mines around this cell.
  * This is calculated by the Board after it finishes placing mines.
  */
 public void setAdjacentMines(int adjacentMines) {
     this.adjacentMines = adjacentMines;
 }

 /**
  * Convenience method.
  * return true if this cell is a mine.
  */
 public boolean isMine() {
     return getType() == CellType.MINE;
 }
}
