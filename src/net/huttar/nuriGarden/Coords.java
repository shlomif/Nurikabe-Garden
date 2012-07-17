package net.huttar.nuriGarden;
class Coords {

	private int row;

	private int column;

	Coords(int r, int c) {
		row = r;
		column = c;
	}

	int getRow() {
		return row;
	}

	/** Return Manhattan distance between this cell and another. */
	int manhattanDist(Coords cell) {
		return Math.abs(row - cell.row) + Math.abs(column - cell.column);
	}

	public String toString() {
		return "<" + row + "," + column + ">";
	}

	int getColumn() {
		return column;
	}

	public int hashCode() {
		return (row << 16) + column;
	}

	public boolean equals(Object that) {
		if (this == that) {
			return true;
		}
		if (that == null) {
			return false;
		}
		if (getClass() != that.getClass()) {
			return false;
		}
		Coords thatCoords = (Coords) that;
		return (row == thatCoords.row) && (column == thatCoords.column);
	}

	/** set coords of this to given cell */
	void copy(Coords cell) {
		row = cell.row;
		column = cell.column;
	}
}
