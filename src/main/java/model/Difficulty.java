package model;

public enum Difficulty {

	EASY(9, 9,10, 10,6 ,2),
	MEDIUM(13, 13, 8, 26, 7, 3),
	HARD(16, 16, 6, 44, 11, 4);

	private final int rows;
	private final int cols;
	private final int initialLives;
	private final int mines;
	private final int questions;
	private final int surprises;
	
	Difficulty(int rows, int cols, int initialLives, int mines, int questions, int surprises){
		this.rows = rows;
		this.cols = cols;
		this.initialLives = initialLives;
		this.mines = mines;
		this.questions = questions;
		this.surprises = surprises;
	}

	public int getRows() {
		return rows;
	}

	public int getCols() {
		return cols;
	}

	public int getInitialLives() {
		return initialLives;
	}

	public int getMines() {
		return mines;
	}

	public int getQuestions() {
		return questions;
	}

	public int getSurprises() {
		return surprises;
	}

    public int getSurpriseCount() {
        return switch (this) {
            case EASY -> 2;
            case MEDIUM -> 3;
            case HARD -> 4;
        };
    }

