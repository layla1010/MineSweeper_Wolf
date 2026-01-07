package model;

public class CellFactory {

    public static Cell createEmpty(int r, int c) {
        return new EmptyCell(r, c);
    }

    public static Cell createMine(int r, int c) {
        return new MineCell(r, c);
    }

    public static Cell createQuestion(int r, int c) {
        return new QuestionCell(r, c);
    }

    public static Cell createSurprise(int r, int c) {
        return new SurpriseCell(r, c);
    }

    public static Cell createNumber(int r, int c, int adjacentMines) {
        NumberCell cell = new NumberCell(r, c);
        cell.setAdjacentMines(adjacentMines);
        return cell;
    }
}
