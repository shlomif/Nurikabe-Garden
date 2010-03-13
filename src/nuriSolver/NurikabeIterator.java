package nuriSolver;
import java.util.Iterator;

/** Iterates through the cells of a NuriState puzzle. */
public class NurikabeIterator implements Iterator<Coords> {

	private NuriState puzzle;
	
	int row;
	
	int column;
	
	public NurikabeIterator(NuriState puzzle) {
		super();
		this.puzzle = puzzle;
		row = 0;
		column = 0;
	}

	public boolean hasNext() {
		return (row <= puzzle.getHeight() - 1) &&
		(column <= puzzle.getWidth() - 1);
	}

	public Coords next() {
		Coords result = new Coords(row, column);
		if (column == puzzle.getWidth() - 1) {
			row++;
			column = 0;
		} else {
			column++;
		}
		return result;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}

}
