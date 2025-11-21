package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
* Logical representation of a single game board for one player.
*
* Responsibilities:
*  - Hold a 2D grid of Cell objects.
*  - Randomly distribute mines, question cells and surprise cells
*    according to the chosen Difficulty.
*  - Ensure there are no duplicates in the same cell.
*  - After mines are placed, compute the "adjacent mines" number
*    for every non-mine cell.
*
* This class does NOT deal with GUI, players turn, hearts, or questions text.
* It is pure backend logic for board generation.
*/
public class Board {

 private final Difficulty difficulty;
 private final int rows;
 private final int cols;

 private final int mineCount;
 private final int questionCount;
 private final int surpriseCount;

 private final Cell[][] cells;
 private final Random random;

 /**
  * Creates a new board according to the given difficulty.
  * The constructor immediately builds a new random board.
  */
 public Board(Difficulty difficulty) {
     this.difficulty = difficulty;
     this.random = new Random();

     // Configure size and quantities according to the assignment.
     switch (difficulty) {
         case EASY:
             this.rows = 9;
             this.cols = 9;
             this.mineCount = 10;
             this.questionCount = 6;
             this.surpriseCount = 2;
             break;

         case MEDIUM:
             this.rows = 13;
             this.cols = 13;
             this.mineCount = 26;
             this.questionCount = 7;
             this.surpriseCount = 3;
             break;

         case HARD:
         default:
             this.rows = 16;
             this.cols = 16;
             this.mineCount = 44;
             this.questionCount = 11;
             this.surpriseCount = 4;
             break;
     }

     this.cells = new Cell[rows][cols];

     buildNewRandomBoard();
 }

 /**
  * Builds a completely new random board.
  * Can be called from tests to generate multiple games in a row.
  */
 public final void buildNewRandomBoard() {
     // Step 1: create a shuffled list of ALL positions.
     List<Position> positions = createShuffledPositions();

     int index = 0;

     // Step 2: place mines.
     for (int i = 0; i < mineCount; i++, index++) {
         Position p = positions.get(index);
         cells[p.row][p.col] = new MineCell(p.row, p.col);
     }

     // Step 3: place question cells on still-empty positions.
     for (int i = 0; i < questionCount; i++, index++) {
         Position p = positions.get(index);
         cells[p.row][p.col] = new QuestionCell(p.row, p.col);
     }

     // Step 4: place surprise cells on still-empty positions.
     for (int i = 0; i < surpriseCount; i++, index++) {
         Position p = positions.get(index);
         cells[p.row][p.col] = new SurpriseCell(p.row, p.col);
     }

     // Step 5: fill the rest with empty cells.
     while (index < positions.size()) {
         Position p = positions.get(index++);
         cells[p.row][p.col] = new EmptyCell(p.row, p.col);
     }

     // Step 6: compute adjacent mines number for every cell.
     computeAdjacentMinesForAllCells();
 }

 /**
  * Helper method – creates a list of all board positions and shuffles it.
  * This guarantees we never put two different logical cells in the same place.
  */
 private List<Position> createShuffledPositions() {
     List<Position> positions = new ArrayList<>(rows * cols);
     for (int r = 0; r < rows; r++) {
         for (int c = 0; c < cols; c++) {
             positions.add(new Position(r, c));
         }
     }
     Collections.shuffle(positions, random);
     return positions;
 }

 /**
  * Calculates the number of adjacent mines around every cell.
  * For mine cells the value is not used by the game,
  * but we still compute it for completeness.
  */
 private void computeAdjacentMinesForAllCells() {
     for (int r = 0; r < rows; r++) {
         for (int c = 0; c < cols; c++) {
             int minesAround = countAdjacentMines(r, c);
             cells[r][c].setAdjacentMines(minesAround);
         }
     }
 }

 /**
  * Counts how many mines exist in the 8 neighbours of the given cell.
  */
 private int countAdjacentMines(int row, int col) {
     int count = 0;

     for (int dr = -1; dr <= 1; dr++) {
         for (int dc = -1; dc <= 1; dc++) {
             // skip the cell itself
             if (dr == 0 && dc == 0) {
                 continue;
             }
          // dr = change in row (-1, 0, +1), dc = change in column.
          // We compute the neighbour's coordinates by adding the offsets
          // to the current cell position. This gives us all 8 adjacent cells.
          // Example: (row+1, col-1) = bottom-left neighbour.
             int nr = row + dr;
             int nc = col + dc;

             if (isInsideBoard(nr, nc) && cells[nr][nc].isMine()) {
                 count++;
             }
         }
     }

     return count;
 }

 private boolean isInsideBoard(int row, int col) {
     return row >= 0 && row < rows && col >= 0 && col < cols;
 }

 // ---------- Getters used by other layers (controller / view / QA) ----------

 public Difficulty getDifficulty() {
     return difficulty;
 }

 public int getRows() {
     return rows;
 }

 public int getCols() {
     return cols;
 }

 public int getMineCount() {
     return mineCount;
 }

 public int getQuestionCount() {
     return questionCount;
 }

 public int getSurpriseCount() {
     return surpriseCount;
 }

 public Cell getCell(int row, int col) {
     if (!isInsideBoard(row, col)) {
         throw new IllegalArgumentException("Row/col out of range");
     }
     return cells[row][col];
 }

 /**
  * Utility method for QA tests:
  * counts how many cells of the given type exist on the current board.
  */
 public int countCellsOfType(CellType type) {
     int count = 0;
     for (int r = 0; r < rows; r++) {
         for (int c = 0; c < cols; c++) {
             if (cells[r][c].getType() == type) {
                 count++;
             }
         }
     }
     return count;
 }

 // ---------- Inner helper class: immutable position ----------

//Inner class: represents a single position (row,col) on the board.
//This class is private because only Board needs it.
//It is immutable (fields are final) and used internally for shuffling positions.
 private static class Position {
     final int row;
     final int col;

     Position(int row, int col) {
         this.row = row;
         this.col = col;
     }
 }
}
