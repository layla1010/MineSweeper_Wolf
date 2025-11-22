package model;

import java.util.Scanner;

/**
 * Acceptance test and interactive tool for the "adjacent mines" logic
 * of the Board.
 *
 * Goals:
 *  1. Acceptance test:
 *     - Build a board for each difficulty.
 *     - Verify that each cell's stored adjacentMines value equals
 *       the real number of neighbouring mines.
 *
 *  2. Interactive debug tool:
 *     - Build a small (EASY) board.
 *     - Print a textual representation of the board showing where mines are.
 *     - Allow the tester to enter (row, col) and see how many mines
 *       are adjacent to that cell.
 *
 * This class is NOT used by the game itself. It is a QA/diagnostics helper.
 */
public class NeighborMinesQA {

    public static void main(String[] args) {
    	System.out.println(">>> STARTING NeighborMinesQA <<<");
        // 1) Run the automatic acceptance test:
        runAcceptanceTest();

        // 2) Run the interactive small-board inspection:
        runInteractiveBoardInspector();
    }

    // ==========================================================
    //  Part 1: Acceptance test for all difficulties
    // ==========================================================

    /**
     * Runs the acceptance test on all difficulties.
     */
    public static void runAcceptanceTest() {
        boolean allPassed = true;

        for (Difficulty difficulty : Difficulty.values()) {
            boolean passedForDifficulty = verifyBoardForDifficulty(difficulty);
            allPassed = allPassed && passedForDifficulty;
        }

        System.out.println("=================================================");
        if (allPassed) {
            System.out.println("NeighborMinesQA: ALL difficulties PASSED.");
        } else {
            System.out.println("NeighborMinesQA: TEST FAILED. See messages above.");
        }
        System.out.println("=================================================\n");
    }

    /**
     * Creates a board for the given difficulty and verifies that for every
     * cell on that board, the stored 'adjacentMines' equals the real number
     * of mines in its 8 neighbouring cells.
     *
     * @return true if all cells on this board are correct, false otherwise.
     */
    private static boolean verifyBoardForDifficulty(Difficulty difficulty) {
        System.out.println("Checking adjacent mines for difficulty: " + difficulty);

        Board board = new Board(difficulty);

        int rows = board.getRows();
        int cols = board.getCols();
        boolean success = true;

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);

                int stored = cell.getAdjacentMines();
                int recomputed = recomputeAdjacentMines(board, r, c);

                if (stored != recomputed) {
                    success = false;
                    System.out.printf(
                        "Mismatch at (%d,%d): stored=%d, recomputed=%d, type=%s%n",
                        r, c, stored, recomputed, cell.getType()
                    );
                }
            }
        }

        if (success) {
            System.out.println("  ✓ All cells correct for difficulty " + difficulty + "\n");
        } else {
            System.out.println("  ✗ There are errors for difficulty " + difficulty + "\n");
        }

        return success;
    }

    /**
     * Independent implementation of "count how many mines are in the 8 neighbours".
     * This must NOT call Board.countAdjacentMines so that we really verify the logic.
     */
    private static int recomputeAdjacentMines(Board board, int row, int col) {
        int count = 0;

        for (int dr = -1; dr <= 1; dr++) {
            for (int dc = -1; dc <= 1; dc++) {

                // Skip the cell itself
                if (dr == 0 && dc == 0) {
                    continue;
                }

                int nr = row + dr;
                int nc = col + dc;

                if (nr >= 0 && nr < board.getRows()
                        && nc >= 0 && nc < board.getCols()) {

                    if (board.getCell(nr, nc).isMine()) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    // ==========================================================
    //  Part 2: Interactive small-board inspector
    // ==========================================================

    /**
     * Builds a small board (EASY difficulty), prints its mines, and allows
     * the tester to enter (row, col) to see the number of neighbouring mines.
     */
    public static void runInteractiveBoardInspector() {
        System.out.println("Interactive Neighbor Mines Inspector");
        System.out.println("-----------------------------------");

        // Use the small board (EASY) for easier manual checks.
        Board board = new Board(Difficulty.EASY);

        printBoardWithMines(board);

        System.out.println("\nEnter row and column to inspect a cell.");
        System.out.println("Example: 0 0  (for first row, first column)");
        System.out.println("Enter -1 to exit.\n");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.print("Row (or -1 to quit): ");
                int row = scanner.nextInt();
                if (row == -1) {
                    break;
                }

                System.out.print("Col: ");
                int col = scanner.nextInt();

                if (row < 0 || row >= board.getRows()
                        || col < 0 || col >= board.getCols()) {
                    System.out.println("  -> Out of range, please try again.\n");
                    continue;
                }

                Cell cell = board.getCell(row, col);
                int stored = cell.getAdjacentMines();
                int recomputed = recomputeAdjacentMines(board, row, col);

                System.out.printf("  Cell (%d,%d) type=%s%n", row, col, cell.getType());
                System.out.printf("  Stored adjacent mines : %d%n", stored);
                System.out.printf("  Recomputed neighbours : %d%n", recomputed);
                System.out.println();
            }
        }

        System.out.println("Inspector finished. Goodbye.");
    }

    /**
     * Prints a textual representation of the board, marking the mines.
     * 'M' = mine, '.' = non-mine cell.
     * Also prints row/column indices to make manual checking easier.
     */
    private static void printBoardWithMines(Board board) {
        int rows = board.getRows();
        int cols = board.getCols();

        System.out.println("Current board layout (EASY difficulty):");
        System.out.println("M = mine, . = non-mine");
        System.out.println();

        // Print column indices header
        System.out.print("    ");
        for (int c = 0; c < cols; c++) {
            System.out.printf("%2d ", c);
        }
        System.out.println();

        System.out.print("    ");
        for (int c = 0; c < cols; c++) {
            System.out.print("---");
        }
        System.out.println();

        // Print each row with its index
        for (int r = 0; r < rows; r++) {
            System.out.printf("%2d | ", r);
            for (int c = 0; c < cols; c++) {
                Cell cell = board.getCell(r, c);
                char symbol = cell.isMine() ? 'M' : '.';
                System.out.print(" " + symbol + " ");
            }
            System.out.println();
        }
    }
}
