package nuriSolver;
public class Coords {

	private int row;

	private int column;

	public Coords(int r, int c) {
		row = r;
		column = c;
	}

	public int getRow() {
		return row;
	}
	
	/** Return Manhattan distance between this cell and another. */
	public int manhattanDist(Coords cell) {
		return Math.abs(row - cell.row) + Math.abs(column - cell.column);
	}

	public String toString() {
		return "<" + row + "," + column + ">";
	}
	
	public int getColumn() {
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
	public void copy(Coords cell) {
		row = cell.row;
		column = cell.column;
	}
}
