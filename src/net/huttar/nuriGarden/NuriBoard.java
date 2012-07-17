package net.huttar.nuriGarden;
/** DONE: keep a static member pointing to the "current" NuriBoard board (or its grid) */
/** TODO: use multiple threads (depending on # of processors available) to
 * investigate multiple lines of backtracking simultaneously.
 */
/** DONE: separate file I/O / parsing / scanning out into another class in the same package, NuriParse */
/** TODO: If we take a guess at the last remaining unknown cell, I'm not sure
 * we check the validity well enough. I don't think this is a problem b/c
 * when there's only one remaining cell, it should be easily inferrable. But
 * we might guess the second-last and infer the last incorrectly; in that case would the proper
 * constraints be checked?
 * Done; now when we fill in the last remaining cell, we check the validity of the
 * solution rigorously. */
/** TODO: Be heuristic about picking a cell to make a hypothesis about.
 * Try to pick one that's likely to have more consequences sooner.
 * E.g. if a region (especially a 1-hungry region) can only grow in two possible ways,
 * pick one of them and try making it black as that will imply the other is white.
 * Or find a place where there are two black cells in a square of 4 with
 * two unknowns; try making one of the unknowns black.
 * This is partially implemented (pickUnknownCell()). Can make it smarter.
 * */
/** TODO:
 * Make debugging output more helpful for gauging progress when solving a large puzzle.
 * 1) Output sparser debugging, e.g. only output
 * every nth debug message (where n gets gradually larger?)
 * But we should print several consecutive messages when we print one,
 * since they are more meaningful in sequence
 * 2) More meaningful metrics. Report how many cells are known, and
 * how many just guessed. Use trySearch()'s isSure parameter...
 * maybe count as known all non-UNKNOWN cells in puzzle at hypoth-level i,
 * where every level from 0 to i is a isSure. Maybe each NuriBoard instance
 * can have an "isSure" field. I have begun implementing this.
 */
/** TODO: would be nice to allow comments in *.puz files. Requires changing
 * format. */
/** TODO: allow *.puz files to describe partially-solved puzzles. Thus '.' must
 * mean WHITE, not UNKNOWN. So you can input the puzzles you partially
 * solved by hand, and get an automated assist. Maybe also output the results
 * of production rules (and perhaps k levels of hypothesizing) to a file
 * for further hand-processing.
 */
/** TODO: my real desire is a GUI that allows interactive solving, with both
 * user insight and automated help at the user's discretion.
 */
/** TODO: We currently short-circuit as soon as we find a solution.
 * For generating, we will need to know whether there are multiple solutions.
 * So we will have to tweak that short-circuit.
 */

import java.io.*;
import java.util.*;

import net.huttar.nuriGarden.NuriSolver;

/** Program to solve NuriBoard puzzles.
 * A "NuriBoard" instance represents a board state.
 * Used to be one class with NuriSolver, named Nurikabe, but I split it up. */
class NuriBoard implements Iterable<Coords>, Cloneable  {
	/** Character for a filled-in cell. */
	static final char BLACK = '#';
	/** Character for a cell known to be white. */
	static final char WHITE = '.';
	/** Character for a cell of unknown status. */
	static final char UNKNOWN = '?';

	/** Never used as a cell state; instead used for operation to toggle the
	 * existing state. */
	static final char TOGGLEFWD = '/', TOGGLEBWD = '\\';

	/** Allowed number characters: 1 to 35, base 36 */
	static final String NUMBERS = "123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	static final int maxCellValue = NUMBERS.length();

//	/** The solver using this board state. */
//	private NuriSolver solver;
//
//	void setSolver(NuriSolver s) { solver = s; }

	/** Depth of recursive search. */
	short searchDepth;

	/* For use as an API (e.g. from visualizer).
	 * Read or create a puzzle and return it as an object.
	 * i is index of puzzle in the file (1-based).
	 */
	/* Obsolete.
	static NuriBoard init(String filename, int i, NuriSolver solver) {
		NuriBoard.debug = debug;
		NuriBoard puzzle = new NuriBoard(filename, i);
		puzzle.solver = solver;
		// debug = true;
		// System.out.println(puzzle);
		return puzzle;
	}
	*/

	/** Grid of cells. Each is BLACK, WHITE, UNKNOWN, or a digit 1-9A-Z. */
	char[][] grid;

	private short[][] guessLevel;

	/** Number of white cells expected in the solution. */
	private int expWhiteCount = 0;

	/** Number of black cells expected in the solution. */
	private int expBlackCount = 0;

	/** list of white ucRegions being used during solving. */
	ArrayList<UCRegion> whiteRegions;

	/** list of black ucRegions being used during solving. */
	ArrayList<UCRegion> blackRegions;

	/** true if this puzzle's state is sure.
	 * This means it is not the result of guesses, except for guesses
	 * such that the inverse of the guess has already led to a contradiction.
	 * Thus the current state is sure unless the initially given puzzle is
	 * intrinsically inconsistent (has no solution).
	 */
	boolean isSure = true;

	/** cached result of isValid() call. */
	boolean validityKnown = false;

	private boolean validity = false;

	/** The puzzle that this puzzle is a hypothesis branch from. */
	NuriBoard predecessor = null;

	NuriBoard() {
	}

	/** Create object and initialize to given dimensions. */
	public NuriBoard(int height, int width) {
		newGrid(height, width);
		for (int i = 0; i < getHeight(); i++)
			for (int j = 0; j < getWidth(); j++)
				initializeCell(i, j, UNKNOWN);
		updateGuessLevel();
		prepareStats(true);
	}

	void setState(char[][] grid, ArrayList<UCRegion> blackRegions,
			ArrayList<UCRegion> whiteRegions, short searchDepth, int numUnknownCells) {
		this.blackRegions = blackRegions;
		this.whiteRegions = whiteRegions;
		this.grid = grid;
		this.searchDepth = searchDepth;
		this.numUnknownCells = numUnknownCells;
	}

	/**
	 * Initialize blackRegions, whiteRegions, and other derived data.
	 * If isNew is true, assume all cells are UNKNOWN.
	 */
	protected void prepareStats(boolean isNew) {
		/* Initialize ucRegions. At first there are no black regions. */
		blackRegions = new ArrayList<UCRegion>();
		whiteRegions = new ArrayList<UCRegion>();
		expWhiteCount = 0;

		if (!isNew) {
			for (Coords cell : this) {
				if (isANumber(get(cell))) {
					UCRegion region = new UCRegion(this, cell);
					whiteRegions.add(region);
					expWhiteCount += region.getLimit();
				} else if (get(cell) == BLACK && containingRegion(cell) == null) {
					// sometimes we have black cells in input files for testing.
					UCRegion region = new UCRegion(this, cell);
					blackRegions.add(region);
				}
			}
		}
		expBlackCount = getHeight() * getWidth() - expWhiteCount;
		// initially all cells are unknown except the seeds (numbered cells)
		// TODO: that may not be true if we load a half-solved board.
		numUnknownCells = getHeight() * getWidth() - whiteRegions.size();
		NuriSolver.debugMsg("expWhiteCount: " + expWhiteCount + "; expBlackCount: "
				+ expBlackCount + "; numUnknownCells: " + numUnknownCells);
	}

	/**
	 * Return number of black cells there should be in the solution of this
	 * puzzle.
	 */
	int getExpBlackCount() {
		return expBlackCount;
	}

	/** Create a new board with only the numbers from this board,
	 * and the rest unknown. */
	public NuriBoard resetCopy() {
		NuriBoard newBoard = new NuriBoard();
		newBoard.newGrid(getHeight(), getWidth());
		for (Coords cell : this) {
			if (isANumber(get(cell)))
				newBoard.grid[cell.getRow()][cell.getColumn()] = get(cell);
			else
				newBoard.grid[cell.getRow()][cell.getColumn()] = UNKNOWN;
		}
		newBoard.prepareStats(false);
		return newBoard;
	}

	/**
	 * Create a copy of the grid. Useful for undoing during search.
	 */
	protected char[][] copyGrid() {
		char[][] result = new char[getHeight()][getWidth()];
		for (int r = 0; r < getHeight(); r++) {
			for (int c = 0; c < getWidth(); c++) {
				result[r][c] = grid[r][c];
			}
		}
		return result;
	}

	/** Return the label at a particular location. */
	char get(Coords cell) {
		return grid[cell.getRow()][cell.getColumn()];
	}

	/** Return the cell value at a particular location. */
	char get(int r, int c) {
		return grid[r][c];
	}

	/** Return the label at a particular location. */
	short getGuessLevel(int r, int c) {
		return guessLevel[r][c];
	}

	/** Return the number of rows in the puzzle. */
	int getHeight() {
		return grid.length;
	}

	/** Return the number of columns in the puzzle. */
	int getWidth() {
		return grid[0].length;
	}

	/** Return true if the given character is a digit (1-9A-Z). */
	static boolean isANumber(char c) {
		return NUMBERS.indexOf(c) >= 0;
	}

	/** Return value of the given character as a digit (1-9A-Z). */
	static protected int numberValue(char c) {
		return NUMBERS.indexOf(c) + 1;
	}

	/** Return true if the given character is white or is a digit (1-9). */
	static boolean isWhite(char c) {
		return c == WHITE || isANumber(c);
	}

	/** Return WHITE if the given character is a number; otherwise return given
	 * character. */
	static protected char unifyWhite(char c) {
		return isANumber(c) ? WHITE : c;
	}

	/** Iterate through the cells in the grid. */
	public Iterator<Coords> iterator() {
		return new NurIterator(this);
	}

	/** Return the coordinates of all cells adjacent to a given cell. */
	protected List<Coords> neighbors(Coords cell) {
		return neighbors(cell.getRow(), cell.getColumn());
	}

	/** Return the coordinates of all cells adjacent to a given cell. */
	protected List<Coords> neighbors(int r, int c) {
		List<Coords> result = new ArrayList<Coords>();
		if (r > 0) {
			result.add(new Coords(r - 1, c));
		}
		if (r < getHeight() - 1) {
			result.add(new Coords(r + 1, c));
		}
		if (c > 0) {
			result.add(new Coords(r, c - 1));
		}
		if (c < getWidth() - 1) {
			result.add(new Coords(r, c + 1));
		}
		return result;
	}

	/** Return the region containing the given cell.
	 * TODO: Optimize; maybe a spatial index or something.
	 * Or a hash table. Where is this used? only in prepareStats()? */
	protected UCRegion containingRegion(Coords cell) {
		List<UCRegion> regions;
		char content = get(cell);
		regions = isWhite(content) ? whiteRegions :
			content == BLACK ? blackRegions : null;
		for (UCRegion region : regions) {
			if (region.contains(cell)) return region;
		}
		return null;
	}

	/** Add given cell to a UCRegion in whiteRegions or blackRegions.
	 * May involve creating a region, merging two regions, or simply growing one
	 * adjacent region. */
	protected UCRegion addToARegion(Coords cell) throws ContradictionException {
		HashSet<UCRegion> neighborRegions = new HashSet<UCRegion>();
		char content = get(cell);
		NuriSolver.debugMsg(searchDepth, "Adding cell " + cell + "(" + content + ") to a region");
		// Using immediate neighbors of cell,
		// make collection of neighbors' regions of needed color
		for (Coords neighbor : neighbors(cell)) {
			if (get(neighbor) == content || (isWhite(get(neighbor)) && isWhite(content))) {
				UCRegion contReg = containingRegion(neighbor);
				neighborRegions.add(contReg);
			}
		}

		Iterator<UCRegion> iter = neighborRegions.iterator();
		UCRegion firstRegion = !neighborRegions.isEmpty() ? (UCRegion)iter.next() : null;
		List<UCRegion> puzzleColorRegions = (content == WHITE ? whiteRegions : blackRegions);
		NuriSolver.debugMsg(searchDepth, "  found " + neighborRegions.size() + " neighbor regions of same color");
		switch (neighborRegions.size()) {
		case 0:
			// If no available regions, create one.
			UCRegion newRegion = new UCRegion(this, cell);
			puzzleColorRegions.add(newRegion);
			return newRegion;
		case 1:
			// If one available region, add to it.
			firstRegion.addCell(cell);
			return firstRegion;
		default:
			// If multiple available regions, add cell to first one
			// and then join the remaining regions.
			firstRegion.addCell(cell);
			while (iter.hasNext()) {
				UCRegion region = (UCRegion)iter.next();
				if (region != firstRegion) {
					firstRegion.addAllCells(region);
					puzzleColorRegions.remove(region);
				}
			}
			return firstRegion;
		}
	}

	int numUnknownCells = 0;

	void setGuessLevel(Coords cell, short gl) {
		guessLevel[cell.getRow()][cell.getColumn()] = gl;
	}

	void setGuessLevel(int c, int r, short gl) {
		guessLevel[c][r] = gl;
	}

	/** Set guessLevel of all cells to at most the current guessLevel.
	 * This is done when a trial/hypothesis completes successfully.
	 */
	void updateGuessLevel() {
		for (int i = 0; i < getHeight(); i++)
			for (int j = 0; j < getWidth(); j++)
				if (getGuessLevel(i, j) > searchDepth)
					setGuessLevel(i, j, searchDepth);
	}

	/** Return an unknown cell for hypothesizing about, and a color for the initial hypothesis.
	 * Return as an array of Object[2]. TODO: maybe better as a nested class? (See
	 * http://java.sun.com/developer/TechTips/2000/tt1205.html)
	 * Try to find one that will quickly lead to firm conclusions
	 * about the values of cells.
	 */
	Object[] pickUnknownCell() {
		Character WHITEchar = new Character(NuriBoard.WHITE);
		// First preference: find a 1-hungry region with just two
		// spots to grow into.
		UCRegion region1 = null;
		for (UCRegion region : whiteRegions) {
			if (region.getHunger() == 1) {
				region1 = region;
				Region neighbors = region.getNeighbors("" + NuriBoard.UNKNOWN);
				if (neighbors.size() == 2) return new Object[]{neighbors.iterator().next(), WHITEchar};
			}
		}
		// If none of the 1-hungry regions has exactly two places to grow
		// into, just pick any of the 1-hungry regions.
		if (region1 != null)
			return new Object[]{region1.getNeighbors("" + NuriBoard.UNKNOWN).iterator().next(), WHITEchar};

		// last resort: return any unknown cell.
		for (Coords cell : this) {
			if (get(cell) == NuriBoard.UNKNOWN) return new Object[]{cell, WHITEchar};
		}
		// should never happen:
		assert(false) : "Puzzle unsolved but no unknown cells!";
		return null;
	}

	/** Throw an exception if any puzzle constraints have been violated. */
	void checkConstraints() throws ContradictionException {
		 /* We only need to check any constraints that aren't already checked by the production rules.
			A) The black (a.k.a. "water") areas must form one connected region ("wall").
			B) There can never be a 2x2 square of black cells.
			C) Every region ("island") of white ("land") cells must contain exactly one number.
			D) Each written number must be in a region of white cells whose cell count is equal to the number.
		 But since the puzzle is not completely solved, we can only check that these constraints are still
		 possible to adhere to.
		 A: We could (TODO) check that there is a clear route of UNKNOWN cells from every black region to another
		 	black region, and that the resulting graph is connected.
		 B: This is checked by rule 4.
		 C: 1) We check that each white region has no more than one number below, and also in UCRegion.addAllCells().
		    2) We verify that each white region that doesn't have a number has the potential to grow, and thus
		    connect to a numbered region. We could (TODO) also check that there is a path of UNKNOWN cells leading to a
		    numbered region (bonus: such that the length of the path plus the size of the two regions <= the number).
		 D: rule1 checks that no full white regions have neighboring cells of the same color. Below we check that no
		 	region has more cells than its limit.
		 */
		for (UCRegion region : whiteRegions) {
			if (region.size() > region.getLimit() && region.getLimit() > 0)
				throw new ContradictionException("Overfull region " + region);
			int numbers = 0;
			for (Coords cell : region) {
				if (NuriBoard.isANumber(get(cell))) {
					numbers++;
			if (numbers > 1)
						throw new ContradictionException("Region " + region + " contains at least 2 numbers; second is " + get(cell));
				}
			}
		}
		for (UCRegion region : blackRegions) {
			if (region.size() > region.getLimit() && region.getLimit() > 0)
				throw new ContradictionException("Overfull region " + region);
		}
	}

	/** Set the value at a given location.
	 * Do not use this when initializing a board, as it can have
	 * complex side-effects. */
	protected void set(Coords cell, char value) throws ContradictionException {
		if (get(cell) == value) return;
		else if (get(cell) == UNKNOWN)
			numUnknownCells--;
		else
			throw new ContradictionException("Tried to set cell " + cell + " to " + value + " when already " + get(cell));

		changed = true;
		validityKnown = false;

		grid[cell.getRow()][cell.getColumn()] = value;
		addToARegion(cell);
	}

	/** Set the value at a given location.
	 * Suitable for use in initializing a board.
	 *
	 * @param r: row of cell
	 * @param c: column of cell
	 * @param value: new value
	 */
	void initializeCell(int r, int c, char value) {
		if (value == TOGGLEFWD) {
			//TODO: later, make behavior of TOGGLE configurable.
			switch(grid[r][c]) {
			case BLACK: value = WHITE; break;
			case WHITE: value = UNKNOWN; break;
			case UNKNOWN: value = BLACK; break;
			default: assert(true); // unexpected value
			}
		} else if (value == TOGGLEBWD) {
			switch(grid[r][c]) {
			case BLACK: value = UNKNOWN; break;
			case WHITE: value = BLACK; break;
			case UNKNOWN: value = WHITE; break;
			default: assert(true); // unexpected value
			}
		}
		grid[r][c] = value;
	}

	/** Has the board changed in the last round of applying rules? */
	boolean changed;

	/** Return mostly-deep copy of puzzle.
	 * Grid and regions are copied by their contents.
	 */
	protected Object clone() {
		NuriBoard newPuzzle = null;
		try {
			newPuzzle = (NuriBoard)super.clone();
		}
		catch (CloneNotSupportedException e) {
			System.err.println("NuriBoard can't clone");
			return null;
		}
		newPuzzle.setState(copyGrid(), copyRegions(blackRegions, newPuzzle),
			copyRegions(whiteRegions, newPuzzle), (short)(searchDepth + 1), numUnknownCells);
		return (Object)newPuzzle;
	}

	/** Return mostly-deep copy of a regions List, with newPuzzle context.
	 * Coords are merely copied by reference.
	 */
	private ArrayList<UCRegion> copyRegions(ArrayList<UCRegion> regions, NuriBoard newPuzzle) {
		ArrayList<UCRegion> newRegions = new ArrayList<UCRegion>(regions.size());
		for (UCRegion region : regions) {
			UCRegion newRegion = (UCRegion)region.clone();
			newRegion.setState(newPuzzle);
			newRegions.add(newRegion);
		}
		assert(regions.size() == newRegions.size());
		return newRegions;
	}


	/** Return a cell whose contents are in target, reachable from
	 * given cell, via cells whose contents are in allowable.
	 */
	Coords areaFind(Coords cell, String allowable, String target) {
		Region region = new Region(this, cell);
		return region.find(allowable, target);
	}

	public String toString() {
		return toString(null);
	}

	/** Output current state, including solving attempts, as a string
	 * ("puz" format but with spacing for easier reading). */
	String toString(Coords modified) {
		StringBuilder result = new StringBuilder();
		StringBuilder indent = new StringBuilder();
		for (int i = 0; i < searchDepth; i++)
			indent.append(' ');
		for (int r = 0; r < getHeight(); r++) {
			result.append(indent.toString());
			for (int c = 0; c < getWidth(); c++) {
				result.append(grid[r][c]);
				if (modified != null && c == modified.getColumn() && r == modified.getRow())
					result.append(">");
				else if (modified != null && c == modified.getColumn() - 1 && r == modified.getRow())
					result.append("<");
				else
					result.append(" ");
			}
			result.append("\n");
		}
		return result.toString();
	}

	/** Output numbers, as a string
	 * ("puz" format). */
	String puzzle() {
		StringBuilder result = new StringBuilder();
		for (int r = 0; r < getHeight(); r++) {
			for (int c = 0; c < getWidth(); c++) {
				if (isANumber(grid[r][c]))
					result.append(grid[r][c]);
				else
					result.append('-');
			}
			result.append("\n");
		}
		return result.toString();
	}

	/** Return true if the puzzle been solved, i.e. the color of all squares is
	 * known, and the solution is valid.
	 * Otherwise throw a ContradictionException. */
	protected boolean isSolved() throws ContradictionException {
		//DONE: when the following is true, we also need to check for validity!
		return (numUnknownCells == 0 ? isValid() : false);
	}

	/** Assuming there are no more unknown cells, return true if the potential
	 * solution is a correct (valid) solution.
	 * Otherwise throw ContradictionException.
	 */
	boolean isValid() throws ContradictionException {
		if (validityKnown) return validity; // skip the unnecessary computations!

		// It will be known by the time we return from this method or throw an exception.
		validityKnown = true;

		if (blackRegions.size() > 1) {
			validity = false;
			throw new ContradictionException("Potential solution has more than one black region");
		}

		/* The following checks are probably redundant, since they should be implied by
		 * the checkConstraints() call and the foregoing. I think.
		 * If they are redundant, then they will only be called when we've found a solution,
		 * which is only once per solve()!* And they serve as a confidence-builder.
		 * If they turn out not to be redundant... then good thing we have them!
		 *
		 * *Actually, this method may be called once or more for every level of recursive backtracking
		 * on the way back out after a successful solve. DONE: We need to cache the result for that case...
		 * and clear the cache if something changes. Need a static? (class) member: boolean isSolved
		 */

		// check that black region and all white islands are the right size

		// A correct solution will always have a black region, except for the most boring puzzles.
		// But we have to check it or the next statement can throw an array out-of-bounds exception.
		if (blackRegions.size() > 0) {
			UCRegion bRegion = blackRegions.get(0);
			if (bRegion.size() != bRegion.getLimit()) {
				validity = false;
				throw new ContradictionException("Black region has wrong size. Expected " +
						bRegion.getLimit() + " but found " + bRegion.size());
			}
		}

		for (UCRegion wRegion : whiteRegions) {
			if (wRegion.getLimit() == 0) {
				validity = false;
				throw new ContradictionException("White region with no number?");
			}

			if (wRegion.size() != wRegion.getLimit()) {
				validity = false;
				throw new ContradictionException("White region has wrong size. Expected " +
					wRegion.getLimit() + " but found " + wRegion.size());
			}
		}

		// check for 2x2 black areas again, just to be thorough. We can't be sure that rule4()
		// has been run since the last inference.
		for (int i = 0; i < getHeight() - 1; i++) {
			for (int j = 0; j < getWidth() - 1; j++) {
				if (get(i,     j    ) == BLACK &&
					get(i,     j + 1) == BLACK &&
					get(i + 1, j    ) == BLACK &&
					get(i + 1, j + 1) == BLACK) {
					validity = false;
					throw new ContradictionException("2x2 black cells at " + i + ", " + j);
				}
			}
		}

		validity = true;
		return true;
	}


	/**
	 * Return true if the current state of this puzzle is the same as the
	 * solution in the file.
	 */
	boolean checkSolution(String filename) {
		if (filename.endsWith(".puz"))
			filename = filename.substring(0, filename.length() - 3) + "sol";
		try {
			Scanner in = new Scanner(new FileInputStream(filename));
			for (int r = 0; r < getHeight(); r++) {
				if (!Arrays.equals(grid[r], in.nextLine().toCharArray())) {
					return false;
				}
			}
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			// System.exit(1);
			return false;
		}
	}


	/** recreate grid in given size. Any old grid is discarded. */
	void newGrid(int rows, int columns) {
		if (grid == null || guessLevel == null ||
				rows != getHeight() || columns != getWidth()) {
			grid = new char[rows][columns];
			guessLevel = new short[rows][columns];
		}
	}

	void debugDetails() {
		for (UCRegion region : whiteRegions)
			NuriSolver.debugMsg(searchDepth, region.toString());
		for (UCRegion region : blackRegions)
			NuriSolver.debugMsg(searchDepth, region.toString());
	}

	/** Return true iff the content of the specified cell
	 * is already equivalent to the given value.
	 * (White value is equivalent to numbers.)
	 */
	boolean alreadyIs(Coords cell, char value) {
		return (unifyWhite(get(cell)) == unifyWhite(value));
	}

	/** Return string describing given cell value, e.g. white or black. */
	static String valueName(char c) {
		switch (c) {
		case NuriBoard.WHITE: return "white";
		case NuriBoard.BLACK: return "black";
		case NuriBoard.UNKNOWN: return "unknown";
		default: return null;
		}
	}
}
