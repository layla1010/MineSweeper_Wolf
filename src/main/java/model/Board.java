package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Board {

    private final Difficulty difficulty;
    private final int rows;
    private final int cols;
    private final int mineCount;
    private final int questionCount;
    private final int surpriseCount;
    private final Cell[][] cells;

    public Board(Difficulty difficulty) {
        this.difficulty = difficulty;
        this.rows = difficulty.getRows();
        this.cols = difficulty.getCols();
        this.mineCount = difficulty.getMines();
        this.questionCount = difficulty.getQuestions();
        this.surpriseCount = difficulty.getSurprises();
        this.cells = new Cell[rows][cols];

        initEmpty();
        placeRandomSpecialCells();
        computeNeighborNumbers();
    }

    // Fill board with base EmptyCells
    private void initEmpty() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells[r][c] = new EmptyCell(r, c);
            }
        }
    }

    // Randomly place mines, questions, and surprises
    private void placeRandomSpecialCells() {
        List<int[]> positions = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                positions.add(new int[]{r, c});
            }
        }

        Collections.shuffle(positions);
        int index = 0;

        // --- Place mines ---
        for (int i = 0; i < mineCount; i++, index++) {
            int[] pos = positions.get(index);
            int r = pos[0];
            int c = pos[1];
            cells[r][c] = new MineCell(r, c);
        }

        // --- Place question cells ---
        int placedQuestions = 0;
        while (placedQuestions < questionCount && index < positions.size()) {
            int[] pos = positions.get(index++);
            int r = pos[0];
            int c = pos[1];
            if (!cells[r][c].isMine()) {
                cells[r][c] = new QuestionCell(r, c);
                placedQuestions++;
            }
        }

        // --- Place surprise cells ---
        int placedSurprises = 0;
        while (placedSurprises < surpriseCount && index < positions.size()) {
            int[] pos = positions.get(index++);
            int r = pos[0];
            int c = pos[1];
            if (!cells[r][c].isMine() && cells[r][c].getType() != CellType.QUESTION) {
                cells[r][c] = new SurpriseCell(r, c);
                placedSurprises++;
            }
        }
    }

    // Calculate how many adjacent mines each normal cell has
    private void computeNeighborNumbers() {
        int[] dr = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dc = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = cells[r][c];

                // skip special cells
                if (cell.isMine()
                        || cell.getType() == CellType.QUESTION
                        || cell.getType() == CellType.SURPRISE) {
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

                if (count > 0) {
                    NumberCell numberCell = new NumberCell(r, c);
                    numberCell.setAdjacentMines(count);
                    cells[r][c] = numberCell;
                } else {
                    cell.setAdjacentMines(0);
                }
            }
        }
    }

    private boolean inBounds(int r, int c) {
        return r >= 0 && r < rows && c >= 0 && c < cols;
    }

    // ---------- Getters ----------

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
        if (!inBounds(row, col)) {
            throw new IllegalArgumentException("Row/col out of range");
        }
        return cells[row][col];
    }

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
}
