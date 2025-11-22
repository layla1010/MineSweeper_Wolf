package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {

    private final int rows;
    private final int cols;
    private final int mineCount;
    private final int questionCount;
    private final int surpriseCount;
    private final Cell[][] cells;

    public Board(Difficulty difficulty) {
        this.rows = difficulty.getRows();
        this.cols = difficulty.getCols();
        this.mineCount = difficulty.getMineCount();
        this.questionCount = difficulty.getQuestionCount();
        this.surpriseCount = difficulty.getSurpriseCount();
        this.cells = new Cell[rows][cols];

        initEmpty();
        placeRandomSpecialCells();
        computeNeighborNumbers();
    }
    //fill an empty board
    private void initEmpty() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new Cell();
            }
        }
    }

    //Method purpose: randomly placing the Mines, Question cells, and Surprise cells on the board.
    private void placeRandomSpecialCells() {
        //create list of all coordinates: loop through every row and every column and store each coordinate as:[r,c]
        List<int[]> positions = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                positions.add(new int[]{r, c});
            }
        }
        //now positions contains all possible cell location but shuffled!
        
        //Here the list is randomly mixed, so the order becomes unpredictable
        Collections.shuffle(positions);

        int index = 0;

        //mines
        for (int i = 0; i < mineCount; i++, index++) {
            int[] pos = positions.get(index);
            cells[pos[0]][pos[1]].setType(CellType.MINE);
        }

        //question marks
        for (int i = 0; i < questionCount; i++, index++) {
            int[] pos = positions.get(index);
            if (!cells[pos[0]][pos[1]].isMine()) {
                cells[pos[0]][pos[1]].setType(CellType.QUESTION);
            }
        }

        //surprise cells
        for (int i = 0; i < surpriseCount; i++, index++) {
            int[] pos = positions.get(index);
            if (!cells[pos[0]][pos[1]].isMine()) {
                cells[pos[0]][pos[1]].setType(CellType.SURPRISE);
            }
        }
    }

    //Method purpose: it calculates how many adjacent mines each normal cell has
    private void computeNeighborNumbers() {
    	//These two arrays represent the 8 directions around a cell:
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};
        //-1,-1: up left, -1,0: up center, -1,1: up right
        //0,-1: left, 0,1: right
        //1,-1: down left, 1,0: down center, 1,1: down right
        
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = cells[r][c];
                //skip cells that are non numbered!
                if (cell.isMine() || cell.getType() == CellType.QUESTION || cell.getType() == CellType.SURPRISE) {
                    continue;
                }
                int count = 0;
                for (int i = 0; i < 8; i++) {
                    int nr = r + dr[i];
                    int nc = c + dc[i];
                    if (inBounds(nr, nc) && cells[nr][nc].isMine()) {
                        count++;
                    }
                }
                cell.setNeighborMines(count);
            }
        }
    }

    //to ensure we don't read outside the board
    private boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
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

    public Cell getCell(int row, int col) {
        return cells[row][col];
    }
}

