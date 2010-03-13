package nuriSolver;
// import java.util.ArrayList;

/*
 * Created on Sep 3, 2005
 */

/**
 * Uniform Contiguous Region: a contiguous region consisting of all-white
 * or all-black cells.
 */
public class UCRegion extends Region implements Cloneable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 4152740641552650062L;

	static String WhiteAllowable = "123456789" + NuriState.WHITE;

	/** Content of cells in this ucRegion: WHITE or BLACK. */
	private char contentType;
	/** Number of cells this ucRegion can hold. If zero, limit is unknown. */
	private int limit = 0;
	public short r = 255, g = 255, b = 255; // color, for visualization
	
	/** Return number of cells this ucRegion can hold. If zero, limit is unknown. */
	public int getLimit() {
		return limit;
	}
	/** For white regions, the cell containing the number, if any. */
	private Coords numberedCell = null;
	/** Return the numbered cell. */
	public Coords getNumberedCell() {
		return numberedCell;
	}
	
	/** Return content type of this ucRegion. */
	public char getContentType() {
		return contentType;
	}

	/** Return true if this ucRegion wants more cells. */
	public boolean isHungry() {
		return limit == 0 || size() < limit;
	}
	
	/** Return number of cells this region wants (-1 if unknown) */
	public int getHunger() {
		return (limit == 0) ? -1 : (limit - size());
	}
	
	/** Create an empty UCRegion that can only hold cells of
	 * the given content type.
	 */
	public UCRegion(NuriState puzzle, char type) {
		super(puzzle);
		contentType = type;
		if (type == NuriState.BLACK) {
			limit = puzzle.getExpBlackCount();
		} else {
			limit = 0;
			// pick a pastel color for visualization.
			r = (short)(Math.random() % 56 + 200);
			g = (short)(Math.random() % 56 + 200);
			b = (short)(Math.random() % 56 + 200);
		}
	}

	/** Create a UCRegion in puzzle consisting of cell. The region's contentType
	 * is determined by the contents of cell.
	 */ 
	public UCRegion(NuriState puzzle, Coords cell) {
		super(puzzle);
		char content = puzzle.get(cell);
		contentType = NuriState.isANumber(content) ? NuriState.WHITE : content;
		
		if (contentType == NuriState.BLACK) {
			limit = puzzle.getExpBlackCount();
		} else if (NuriState.isANumber(content)) {
			limit = NuriState.numberValue(content);
			numberedCell = cell;
		}
		add(cell);
	}
	
	/** Add given cell to this region and perform consistency checks.
	 * If set is true, set the given cell to the appropriate value. */
	public boolean addCell(Coords cell, boolean set) throws ContradictionException {
		char content = puzzle.get(cell);
		if (!isHungry())
			throw new ContradictionException("Tried to add " + cell + " to full region.");
		if (NuriState.isANumber(content)) {
			if (numberedCell != null)
				throw new ContradictionException("Tried to add numbered cell " + cell +
				" to a numbered region" + this);
			else
				numberedCell = cell;
		}
		if (set) puzzle.set(cell, contentType);
		else if (puzzle.get(cell) != contentType)
			throw new ContradictionException("Tried to add cell of type " + puzzle.get(cell)
					+ " to UCregion of type " + contentType + ".");

		// what does the return value mean?
		return super.add(cell);
	}

	/** Add given cell to this region and perform consistency checks. 
	 * Cell content is assumed to be of the right type for this UCRegion. */
	public boolean addCell(Coords cell) throws ContradictionException {
		return addCell(cell, false);
	}
	
	/** Add all cells of given region to this region, performing consistency checks. */
	public boolean addAllCells(UCRegion newRegion) throws ContradictionException {
		if (!this.isHungry() || !newRegion.isHungry())
			throw new ContradictionException("Tried to add a UCRegion to a full UCRegion: " + this + ", " + newRegion);
		if (newRegion.contentType != this.contentType)
			throw new ContradictionException("Tried to add UCRegion of type " + newRegion.contentType
					+ " to UCRegion of type" + this.contentType + ".");
		if (contentType == NuriState.WHITE && this.limit > 0 && newRegion.limit > 0)
			throw new ContradictionException("Tried to combine two numbered UCRegions " + this + " and " + newRegion);
		if (this.numberedCell == null)
			this.numberedCell = newRegion.numberedCell;
		int newSize = this.size() + newRegion.size();
		if ((this.limit > 0 && newSize > this.limit) || (newRegion.limit > 0 && newSize > newRegion.limit))
			throw new ContradictionException("Tried to combine UCRegions " + this + " and " + newRegion + " for total size " + newSize
					+ " which is too big.");

		this.limit = (this.limit == 0) ? newRegion.limit : this.limit;
		return super.addAll(newRegion);
	}
	
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("UCRegion (type " + contentType + ", limit " + limit + "): ");
		for (Coords cell : this) {
			result.append(cell.toString());
		}
		return result.toString();
	}

	/** Return shallow copy */
	public Object clone() {
		return super.clone();
	}
	
	/** Set puzzle context (after cloning). */
	public void setState(NuriState puzzle) {
		this.puzzle = puzzle;
	}
	
}
