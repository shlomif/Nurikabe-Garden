package nuriSolver;
/** DONE: keep a static member pointing to the "current" Nurikabe board (or its grid) */
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
 * where every level from 0 to i is a isSure. Maybe each Nurikabe instance
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

import nurikabeVisualizer.NurikabeVisualizer;
import nuriSolver.NuriParser;

/** Program to solve Nurikabe puzzles. 
 * A "Nurikabe" instance represents a board state.
 * We should probably split this up. */
public class Nurikabe extends Thread implements Iterable<Coords>, Cloneable  {
	public enum StopMode { ONESTEP, STEPIN, STEPOVER, STEPOUT, CONTINUE };
	
	public static StopMode stopMode = StopMode.CONTINUE;
	
	/** Character for a filled-in cell. */
	public static final char BLACK = '#';

	/** Character for a cell known to be white. */
	public static final char WHITE = '.';

	/** Character for a cell of unknown status. */
	public static final char UNKNOWN = '?';

	/** Allowed number characters: 1 to 35, base 36 */
	public static final String NUMBERS = "123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	
	/** "Rule" label for guesses. */
	private static final String hypothesisLabel = "[guess]";
	
	/** Whether to output debug messages. */
	private static boolean debug = false;
	
	/** Time the computation started. */
	private static long startTime = 0;

	/** Time the computation ended. */
	private static long endTime = 0;

	/** Tells thread when to pause. */
	public static volatile boolean threadSuspended = false;
	
	/** Read the specified puzzle, solve it, and verify the solution. */
	/** Usage: java ClassName [-d] filename (minus ".puz") */
	public static void main(String[] args) {
		int fileArg = 0;
		
		if (args[0].equals("-d")) { debug = true; fileArg++; }
		// if (args[fileArg].equals("-b")) {
		//   fileArg++;
		//   TODO: process batch of puzzles
		// }
		Nurikabe puzzle = new Nurikabe(args[fileArg].indexOf('.') > -1 ?
				args[fileArg] : args[fileArg] + ".puz");
		System.out.println(puzzle);

		puzzle.solveWrapper();
	}

	public void startSolve() {
		startTime = System.currentTimeMillis();		
	}

	public void endSolve(boolean success) {
		if (success) {
			endTime = System.currentTimeMillis();
			System.out.println("Successfully solved puzzle in " + (endTime - startTime)
					+ " ms with " + totalInferences
					+ " inferences and " + totalHypotheses +
					" guesses at a max search depth of " + maxSearchDepth);			
			// System.out.println("Correct? " + checkSolution(args[fileArg])); TODO: implement this
		} else {
			System.out.println("Unable to solve puzzle. I got this far:");
			System.out.println(this);
			debug("Regions:");
			for (UCRegion region : whiteRegions)
				debug(region.toString());
			for (UCRegion region : blackRegions)
				debug(region.toString());
		}
	}

	/* For use as an API (e.g. from visualizer).
	 * Read or create a puzzle and return it as an object.
	 * i is index of puzzle in the file (1-based).
	 */
	public static Nurikabe init(String filename, int i, boolean debug, NurikabeVisualizer vis) {
		Nurikabe.debug = debug; 
		Nurikabe puzzle = new Nurikabe(filename, i);
		puzzle.visualizer = vis;
		// debug = true;
		// System.out.println(puzzle);
		return puzzle;
	}
	
	/** Grid of cells. Each is BLACK, WHITE, UNKNOWN, or a digit 1-9A-Z. */
	private char[][] grid;

	/** Number of white cells expected in the solution. */
	private int expWhiteCount;

	/** Number of black cells expected in the solution. */
	private int expBlackCount;
	
	/** Depth of recursive search. */
	private int searchDepth;
	
	/** Max depth of recursive search. */
	static private int maxSearchDepth = 0;

	/** Number of hypotheses tried through all instances of this class. */
	static private int totalHypotheses = 0;

	/** Number of inferences made through all instances of this class. */
	static private int totalInferences = 0;

	/** list of white ucRegions being used during solving. */
	private ArrayList<UCRegion> whiteRegions;

	/** list of black ucRegions being used during solving. */
	private ArrayList<UCRegion> blackRegions;
	
	/** true if this puzzle's state is sure.
	 * This means it is not the result of guesses, except for guesses
	 * such that the inverse of the guess has already led to a contradiction.
	 * Thus the current state is sure unless the initially given puzzle is
	 * intrinsically inconsistent (has no solution).
	 */
	private boolean isSure = true;
	
	/** cached result of isValid() call. */
	private boolean validityKnown = false, validity = false;

	/** The puzzle that this puzzle is a hypothesis branch from. */
	private Nurikabe predecessor = null;
	
	/** The parser object used to read in files. */
	private static NuriParser parser = null;

	/** Read specified puzzle from the given file. */
	public Nurikabe(String filename, int i) {
		if (parser == null)
			parser = new NuriParser(filename);
		
		parser.loadFile(this, i);
		
		latestBoard = this;
	}

	/** Read first puzzle from the specified file. */
	public Nurikabe(String filename) {
		this(filename, 0);
	}

	private void setState(char[][] grid, ArrayList<UCRegion> blackRegions,
			ArrayList<UCRegion> whiteRegions, int searchDepth, int numUnknownCells) {
		this.blackRegions = blackRegions;
		this.whiteRegions = whiteRegions;
		this.grid = grid;
		this.searchDepth = searchDepth;
		this.numUnknownCells = numUnknownCells;		
	}

	/**
	 * Initialize blackRegions, whiteRegions, and other derived data.
	 */
	protected void prepareStats() {
		/* Initialize ucRegions. At first there are no black regions. */
		blackRegions = new ArrayList<UCRegion>();
		whiteRegions = new ArrayList<UCRegion>();

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
		expBlackCount = getHeight() * getWidth() - expWhiteCount;
		// initially all cells are unknown except the seeds (numbered cells)
		numUnknownCells = getHeight() * getWidth() - whiteRegions.size();
		debug("expWhiteCount: " + expWhiteCount + "; expBlackCount: "
				+ expBlackCount + "; numUnknownCells: " + numUnknownCells);
	}

	/**
	 * Return number of black cells there should be in the solution of this
	 * puzzle.
	 */
	int getExpBlackCount() {
		return expBlackCount;
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
	public char get(Coords cell) {
		return grid[cell.getRow()][cell.getColumn()];
	}

	/** Return the label at a particular location. */
	public char get(int r, int c) {
		return grid[r][c];
	}

	/** Return the number of rows in the puzzle. */
	public int getHeight() {
		return grid.length;
	}

	/** Return the number of columns in the puzzle. */
	public int getWidth() {
		return grid[0].length;
	}

	/** Return true if the given character is a digit (1-9A-Z). */
	public static boolean isANumber(char c) {
		return NUMBERS.indexOf(c) >= 0;
	}

	/** Return value of the given character as a digit (1-9A-Z). */
	static protected int numberValue(char c) {
		return NUMBERS.indexOf(c) + 1;
	}

	/** Return true if the given character is white or is a digit (1-9). */
	static public boolean isWhite(char c) {
		return c == WHITE || isANumber(c);
	}
	
	/** Return WHITE if the given character is a number; otherwise return given
	 * character. */
	static protected char unifyWhite(char c) {
		return isANumber(c) ? WHITE : c;
	}

	/** Iterate through the cells in the grid. */
	public Iterator<Coords> iterator() {
		return new NurikabeIterator(this);
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

	/** Return the region containing the given cell. */
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
		debug("Adding cell " + cell + "(" + content + ") to a region");
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
		debug("  found " + neighborRegions.size() + " neighbor regions of same color");
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
	
	public int numUnknownCells = 0;
	
	/** Infer that the given cell holds the given value.
	 * Call set() and issue a debugging message. */
	protected void infer(Coords cell, char value, String rule) throws ContradictionException {
		if (unifyWhite(get(cell)) == unifyWhite(value)) return; // Already was that value.
		totalInferences++;
		debug("Rule " + rule + ": " + cell + " = " + value);
		if (get(cell) == UNKNOWN)
			numUnknownCells--;
		set(cell, value);
		debug("\n" + toString(cell));
		checkControls();	// obey execution controls
	}

	/** Set the value at a given location.
	 * Do not use this when initializing a board, as it can have
	 * complex side-effects. */
	protected void set(Coords cell, char value) throws ContradictionException {
		if (get(cell) == value) return;
		if (get(cell) != UNKNOWN)
			throw new ContradictionException("Tried to set cell " + cell + " to " + value + " when already " + get(cell));

		changed = true;
		validityKnown = false;
		latestBoard = this; // for good measure
		
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
	void initialize(int r, int c, char value) {
		grid[r][c] = value;
	}

	/** Has the board changed in the last round of applying rules? */
	private boolean changed;

	/** Do we really need this to be an instance variable?
	 * Only if we plan on multiple visualizers... which could be useful if we have multiple
	 * threads running.
	 */
	private NurikabeVisualizer visualizer;

	public static Nurikabe latestBoard = null;
		
	public boolean success = false;

	// How to run as a thread.
	// Later if we use threads within a solver, we'll want run() to call solve().
	public void run() {
		solveWrapper();
	}
	
	public boolean solveWrapper() {
		success  = false;
		startSolve();
		try {
			success = solve();
        } catch (ContradictionException e) {
			System.out.println("Contradiction: " + e.toString()
					+ "\nThis puzzle is inconsistent.");        	
        }
        
        endSolve(success);	
        return success;
	}
	
	/** Solve the puzzle and return true if successful. */
	public boolean solve() throws ContradictionException {
		changed = true;
		validityKnown = false;
		
		if (maxSearchDepth < searchDepth) maxSearchDepth = searchDepth;
		showProgress();
		
		/*
		 * Try rules and search until puzzle is solved, or until they don't
		 * help.
		 */
		while (!isSolved() && changed) {
			/*
			 * Apply production rules until puzzle is solved, or until they
			 * don't help.
			 */
			while (!isSolved() && changed) {
				// check for any constraint violations to catch them early and prune.
				checkConstraints();
				
				changed = false;
				try {
					applyProductionRules();
				} catch (ContradictionException e) {
					throw e;
				}
			}
		}

		// Last resort: try searching.
		if (!isSolved()) {
			// Find an unknown cell.
			Object cellAndColor[] = pickUnknownCell();
			Coords unknownCell = (Coords)(cellAndColor[0]);
			char color = ((Character)(cellAndColor[1])).charValue();
			// Try out a hypothesis. Each hypothesis will either solve the puzzle
			// or fail completely.
			//TODO: use child threads for the two searches, if appropriate.
			// How do we find out whether multiple processors are available?
			if (!trySearch(unknownCell, color, false))
				// Try out other hypothesis.
				trySearch(unknownCell, isWhite(color) ? BLACK : WHITE, true);
		}

		return isSolved();
	}
	
	/** Check whether we need to pause this thread.
	 * See http://java.sun.com/javase/7/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
	 */
	private void checkControls() {
		if (stopMode == StopMode.ONESTEP)
			threadSuspended = true;
        try {
        	if (threadSuspended) {
        		System.out.println("Suspending thread...");
	            synchronized(visualizer) {
	                while (threadSuspended)
	                    visualizer.wait();
	            }
	            System.out.println("Resuming thread...");
        	}
        } catch (InterruptedException e){
        }
	}

	/** Return an unknown cell for hypothesizing about, and a color for the initial hypothesis.
	 * Return as an array of Object[2]. TODO: maybe better as a nested class? (See
	 * http://java.sun.com/developer/TechTips/2000/tt1205.html)
	 * Try to find one that will quickly lead to firm conclusions
	 * about the values of cells.
	 */
	private Object[] pickUnknownCell() {
		Character WHITEchar = new Character(WHITE); 
		// First preference: find a 1-hungry region with just two
		// spots to grow into.
		UCRegion region1 = null;
		for (UCRegion region : whiteRegions) {
			if (region.getHunger() == 1) {
				region1 = region;
				Region neighbors = region.getNeighbors("" + UNKNOWN);
				if (neighbors.size() == 2) return new Object[]{neighbors.iterator().next(), WHITEchar};
			}
		}
		// If none of the 1-hungry regions has exactly two places to grow
		// into, just pick any of the 1-hungry regions.
		if (region1 != null)
			return new Object[]{region1.getNeighbors("" + UNKNOWN).iterator().next(), WHITEchar};
		
		// last resort: return any unknown cell.
		for (Coords cell : this) {
			if (get(cell) == UNKNOWN) return new Object[]{cell, WHITEchar};
		}
		// should never happen:
		assert(false) : "Puzzle unsolved but no unknown cells!";
		return null;
	}

	/** Throw an exception if any puzzle constraints have been violated. */
	private void checkConstraints() throws ContradictionException {
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
				if (isANumber(get(cell))) {
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
	
	/** Assuming there are no more unknown cells, return true if the potential
	 * solution is a correct (valid) solution.
	 * Otherwise throw ContradictionException.
	 */
	public boolean isValid() throws ContradictionException {
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
		 * on the way back out after a successful solve. TODO: We need to cache the result for that case...
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

	/** Put the given value in the given cell and recursively try to solve the puzzle.
	 * Return true if successful. */ 
	private boolean trySearch(Coords cell, char value, boolean isSure) {
		totalHypotheses++;
		showProgress();
		
		// Save state
		Nurikabe trialPuzzle = (Nurikabe)this.clone();
		latestBoard = trialPuzzle;
		trialPuzzle.isSure = (isSure ? this.isSure : false);
		trialPuzzle.predecessor = this;
		
		String hypoth = "Hypothesis " + cell + " = '" + value + "'";
		try {
			debug("Trying " + hypoth + "...");
			trialPuzzle.infer(cell, value, hypothesisLabel);
			boolean result = trialPuzzle.solve();
			debug(hypoth + " success: " + result);
			if (result) {	
				this.setState(trialPuzzle.grid, trialPuzzle.blackRegions,
						trialPuzzle.whiteRegions, searchDepth, trialPuzzle.numUnknownCells);
				latestBoard = this;
				// adjust regions to point to this instead of trialPuzzle.
				for (UCRegion region : blackRegions)
					region.setState(this);
				for (UCRegion region : whiteRegions)
					region.setState(this);				
			}
			return result;
		}
		catch(ContradictionException e) {
			debug(hypoth + " led to contradiction: " + e.getMessage());
			return false;
		}
	}

	/** Return mostly-deep copy of puzzle.
	 * Grid and regions are copied by their contents.
	 */
	public Object clone() {
		Nurikabe newPuzzle = null;
		try {
			newPuzzle = (Nurikabe)super.clone();
		}
		catch (CloneNotSupportedException e) {
			System.err.println("Nurikabe can't clone");
			return null;
		}
		newPuzzle.setState(copyGrid(), copyRegions(blackRegions, newPuzzle),
			copyRegions(whiteRegions, newPuzzle), searchDepth + 1, numUnknownCells);
		latestBoard = newPuzzle;
		return (Object)newPuzzle;
	}
	
	/** Return mostly-deep copy of a regions List, with newPuzzle context.
	 * Coords are merely copied by reference.
	 */
	private ArrayList<UCRegion> copyRegions(ArrayList<UCRegion> regions, Nurikabe newPuzzle) {
		ArrayList<UCRegion> newRegions = new ArrayList<UCRegion>(regions.size());
		for (UCRegion region : regions) {
			UCRegion newRegion = (UCRegion)region.clone();
			newRegion.setState(newPuzzle);
			newRegions.add(newRegion);
		}
		assert(regions.size() == newRegions.size());
		return newRegions;
	}

	/** Apply the inference rules that don't require any trial-and-error. */
	private void applyProductionRules() throws ContradictionException {
		// Is there a way to put references to these methods into an iterator and
		// loop through them?
		rule0();
		if (isSolved()) return;
		rule1();
		if (isSolved()) return;
		rule2();
		if (isSolved()) return;
		rule3();
		if (isSolved()) return;
		rule4();
		if (isSolved()) return;
		rule5();
		if (isSolved()) return;
		rule5a();
		if (isSolved()) return;
		rule6();
	}

	/** Rule 0 only has to be run once, since numbered cells don't change. */
	private static boolean ranRule0 = false;

	/**
	 * Production rule 0: if a cell is too far from any numbered cell
	 * to be part of its region, that cell must be black.
	 * 
	 * @throws ContradictionException
	 */
	private void rule0() throws ContradictionException {
		if (ranRule0) return; else ranRule0 = true;
		
		outer:
		for (Coords cell : this) {
			if (isANumber(get(cell))) continue;
			for (UCRegion region : whiteRegions) {
				if (cell.manhattanDist(region.getNumberedCell()) + 1 <= region.getLimit())
					continue outer;
			}
			/** No numbered cell is close enough, so this cell must be black. */
			infer(cell, BLACK, "0");
		}
	}

	/**
	 * Production rule 1: if a white region is full, all its immediate neighbors
	 * must be black.
	 * 
	 * @throws ContradictionException
	 */
	private void rule1() throws ContradictionException {
		for (UCRegion region : whiteRegions) {
			if (!region.isHungry()) {
				// Make sure all its neighbors are black
				for (Coords cell : region.getOnlyNeighbors()) {
					switch (get(cell)) {
					case UNKNOWN:
						// set cell to BLACK by rule 1
						infer(cell, BLACK, "1");
						break;
					case BLACK:
						break;
					default: // white or number
						throw new ContradictionException(
								"Full white region has white neighbor at "
										+ cell);
					}
				}
			}
		}
	}

	/**
	 * Production rule 2: If an unknown cell has at least two white neighbors that belong to
	 * different regions that both contain numbers, the unknown cell must be black.
	 * 
	 * @throws ContradictionException
	 */
	private void rule2() throws ContradictionException {
		for (Coords cell : this) {
			if (get(cell) != UNKNOWN) continue;
			UCRegion reg1 = null, reg2 = null;
			// TODO Enhancement: compare each neighboring numbered white region
			// to every neighboring unnumbered white region, because some unnumbered regions
			// could be too big to unify with 
			// 
			for (Coords neighbor : neighbors(cell)) {
				if (isWhite(get(neighbor))) {
					// Haven't yet found a neighboring region with a number? 
					if (reg1 == null) {
						reg1 = containingRegion(neighbor);
						assert(reg1 != null): "Uh oh, white cell not in a region: " + neighbor;
						assert(!reg1.isHungry()): "A full white region at " + neighbor +
							" has neighbor not known to be black, at " + cell; 
						// Only regions containing numbers matter.
						if (reg1.getLimit() == 0) reg1 = null;												
					} else {
						// This is potentially the second neighboring numbered region.
						reg2 = containingRegion(neighbor);
						if (reg2 == reg1) continue;
						assert(reg2 != null): "Uh oh, white cell not in a region: " + neighbor;
						assert(!reg2.isHungry()): "A full white region at " + neighbor +
							" has neighbor not known to be black, at " + cell; 
						// Only regions containing numbers matter.
						if (reg2.getLimit() == 0) reg2 = null;
						else {
							// ok, we have two. Then the cell is black.
							infer(cell, BLACK, "2");
							break; // out of inner for loop
						}
					}
				}
			}
		}
	}

	/**
	 * Production rule 3: If a hungry region can only grow in one direction, grow it that way.
	 * Otherwise, if the hungry region only wants one more cell, and there are exactly two candidates,
	 * which are diagonal from each other, the square adjacent to both candidates that is not part
	 * of the hungry region must be the opposite color.
	 * 
	 * @throws ContradictionException
	 */
	private void rule3() throws ContradictionException {
		try {
			for (UCRegion region : whiteRegions) rule3a(region);
			if (isSolved()) return;
			for (UCRegion region : blackRegions) rule3a(region);
		}
		catch (ConcurrentModificationException e) {
			// This just means that a region was added to or removed from the list
			// we were iterating over. So bail out, and if necessary we'll get another
			// chance to iterate over the lists again later.
			debug("Bailing out of rule3 on ConcurrentModificationException");
			return;
		}
	}

	/**
	 * helper for rule3: If the given region is hungry and can only grow in one direction, grow it that way.
	 * Otherwise, if the hungry region only wants one more cell, and there are exactly two candidates,
	 * which are diagonal from each other, the square adjacent to both candidates that is not part
	 * of the hungry region must be the opposite color.
	 * Or, if the hungry region is white and and there are exactly two candidates,
	 * which are diagonal from each other, and the square adjacent to both candidates that is not part
	 * of the hungry white region is adjacent to another (hungry) white region, that square must be black.  
	 * 
	 * @throws ContradictionException
	 */
	private void rule3a(UCRegion region) throws ContradictionException {
		if (!region.isHungry()) return;
		// Find UNKNOWN neighbors.
		Region neighbors = region.getNeighbors("" + UNKNOWN);
		switch(neighbors.size()) {
		case 0:
			throw new ContradictionException("Hungry UCRegion " + region + " has nowhere to grow.");
		case 1:
			// The simplest way I know to get the sole item in a HashMap is to use the iterator.
			for (Coords cell : neighbors) {
				// TODO enhancement: we might improve performance a little by passing the region to
				// infer() to set() to addToARegion(), but we'd still have to let the latter
				// search for other possible adjacent regions to merge in.
				infer(cell, region.getContentType(), "3");
			}
			return;
		case 2:
			// Two candidates; we may be able to deduce a cell, if region needs exactly one cell
			int hunger = region.getLimit() - region.size();
			// ... and if candidates are caddy-corner...
			Object nb[] = neighbors.toArray();
			int r0 = ((Coords)nb[0]).getRow(), r1 = ((Coords)nb[1]).getRow();  
			int c0 = ((Coords)nb[0]).getColumn(), c1 = ((Coords)nb[1]).getColumn();  
			if (c0 == c1 || r0 == r1) return;
			// OK, we qualify. Find out which other cell is not in the hungry region.
			Coords cellA = new Coords(r0, c1), cellB = new Coords(r1, c0);
			Coords targetCell = region.contains(cellA) ? cellB : cellA;
			char targetColor = region.getContentType() == WHITE ? BLACK : WHITE;
			boolean nearWhite = false;
			// ... or if region is white and target cell is next to another white cell.
			if (hunger != 1 && region.getContentType() == WHITE) {
				// TODO:
//				Coords cellC = ...;
//				Coords cellD = ...;
//				if (isWhite(get(cellC)) || isWhite(get(cellD))) nearWhite = true;
			}
			if (hunger == 1 || nearWhite) {
				if (get(targetCell) == UNKNOWN)
					infer(targetCell, targetColor, "3a");
				else if (isWhite(get(targetCell)) != isWhite(targetColor))
						throw new ContradictionException("Diagonal cell " + targetCell + " should be '" +
								targetColor + "' but is '" + get(targetCell) + "'.");
			}
			return;
		default:
			// nothing we can do, for now.
			return;
		}
	}

	/** Production rule 4: If we have 3 black cells in an L-shape, the 4th cell must be white. */
	private void rule4() throws ContradictionException {
		for (Coords cell : this) {
			if (cell.getRow() < getHeight() - 1 &&
					cell.getColumn() < getWidth() - 1) {
				Coords cell01 = new Coords(cell.getRow(), cell.getColumn()+1);
				Coords cell10 = new Coords(cell.getRow()+1, cell.getColumn());
				Coords cell11 = new Coords(cell.getRow()+1, cell.getColumn()+1);
				Coords whiteCell = null;
				int blackCells = 0;
				if (get(cell) == BLACK) blackCells++; else whiteCell = cell;
				if (get(cell01) == BLACK) blackCells++; else whiteCell = cell01;
				if (get(cell10) == BLACK) blackCells++; else whiteCell = cell10;
				if (get(cell11) == BLACK) blackCells++; else whiteCell = cell11;
				if (blackCells == 4)
					throw new ContradictionException("2x2 black cells at " + cell);
				else if (blackCells == 3 && get(whiteCell) == UNKNOWN) {
					infer(whiteCell, WHITE, "4");
					if (isSolved()) break;
				}
			}
		}
		return;
	}

	/**
	 * Production rule 5: Any UNKNOWN cell whose neighbors are all black or all white must be
	 * that color. (A white cell with number '1' in it is surrounded by black but is never UNKNOWN, so this rule works.) 
	 * This rule covers a subset of rule 5a but is quicker to compute.
	 */
	private void rule5() throws ContradictionException {
		outer:
		for (Coords cell : this) {
			if (get(cell) != '1') {
				List<Coords> neighbors = neighbors(cell);
				Iterator<Coords> iter = neighbors.iterator();
				Coords firstNeighbor = (Coords)iter.next();
				char firstValue = unifyWhite(get(firstNeighbor));
				if (firstValue == UNKNOWN) continue outer;
				while (iter.hasNext()) {
					if (unifyWhite(get((Coords)iter.next())) != firstValue)
						continue outer;
				}
				/* Neighbors are either all white or all black. Conform. */
				if (get(cell) != UNKNOWN && isWhite(get(cell)) != isWhite(firstValue))
					throw new ContradictionException(cell + " surrounded by opposite color");
				infer(cell, firstValue, "5");
			}
		}
	}

	/**
	 * Production rule 5a: Any cell that has no path to a number
	 * must be black; and if there are any known black cells, any cell
	 * that has no path to a black cell must be white.   
	 * This rule overlaps somewhat with rule 6 but is easier to implement.
	 */
	private void rule5a() throws ContradictionException {
		// outer:
		for (Coords cell : this) {
			if (get(cell) == UNKNOWN || (isWhite(get(cell)) && get(cell) != '1')) {
				// Do we have a path to a number?
				Coords target = areaFind(cell, "" + WHITE + UNKNOWN, NUMBERS);
				if (target == null) {
					// No path to number so must be black.
					if (isWhite(get(cell)))
						throw new ContradictionException("White cell " + cell + " has no path to number.");
					else {
						infer(cell, BLACK, "5a1");
						// short-circuit on change
						return;
					}
				}
			}
			if ((get(cell) == UNKNOWN || get(cell) == BLACK) && !blackRegions.isEmpty()) {
				// Do we have a path to a number?
				Coords target = areaFind(cell, "" + UNKNOWN, "" + BLACK);
				if (target == null) {
					// No path to black so must be white.
					if (get(cell) == BLACK)
						throw new ContradictionException("Black cell " + cell + " has no path to black.");
					else {
						infer(cell, WHITE, "5a2");
						// short-circuit on change
						return;
					}
				}
			}
		}
	}

	/* TODO: check manhattan distance */

	/**
	 * Production rule 6: Any cell that cannot be reached from a numbered region
	 * by a path of n UNKNOWN cells (where numbered region's size + n + unnumbered region's size <= limit of either)
	 * must be black.
	 * TODO: need a method for tracing a path from A to B of length n. 
	 */
	private void rule6() throws ContradictionException {
		/*
		// First check all UNKNOWN cells.
		for (Coords cell : this) {
			if (get(cell) == UNKNOWN) {
				// TODO				
			}
		}
		// Then check all unnumbered white regions.
		for (UCRegion region : whiteRegions) {
			if (region.getLimit() == 0) {
				// TODO				
			}
		}
		*/
	}

	/** Return a cell whose contents are in target, reachable from
	 * given cell, via cells whose contents are in allowable.
	 */
	private Coords areaFind(Coords cell, String allowable, String target) {
		Region region = new Region(this, cell);
		return region.find(allowable, target);
	}
	
	/** Return true if the puzzle been solved, i.e. the color of all squares is
	 * known, and the solution is valid.
	 * Otherwise throw a ContradictionException. */
	protected boolean isSolved() throws ContradictionException {
		//TODO: when the following is true, we also need to check for validity!
		return (numUnknownCells == 0 ? isValid() : false);
	}

	public String toString() {
		return toString(null);
	}
	
	public String toString(Coords modified) {
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

	/**
	 * Return true if the current state of this puzzle is the same as the
	 * solution in the file.
	 */
	public boolean checkSolution(String filename) {
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
			System.exit(1);
			return false; // Not actually executed, but the compiler can't
			// tell this
		}
	}

	/** Output debugging message. */
	public void debug(String msg) {
		if (debug) {
			// Is there an easier way to repeating a character n times?
			for (int i = 0; i < searchDepth; i++)
				System.out.print(' ');
			System.out.println(searchDepth + "> " + msg);
		}
	}

	/** Output progress metrics to debugging console or visualizer. */
	public void showProgress() {
		// if (visualizer != null) visualizer.draw();

		/** From deep in a search, find innermost puzzle whose state was sure. */
		Nurikabe lastSurePuzzle;
		for (lastSurePuzzle = this; !lastSurePuzzle.isSure;
			lastSurePuzzle = lastSurePuzzle.predecessor)
			;
		int numCells = getWidth() * getHeight();
		int numGuessed = numCells - numUnknownCells;
		int numSure = numCells - lastSurePuzzle.numUnknownCells;
		debug("Progress: " + numSure + " (" + ((100 * numSure) / numCells) + "%) cells known, "
				+ numGuessed + " (" + ((100 * numGuessed) / numCells) + "%) guessed or known.");
	}

	/** recreate grid in given size. Any old grid is discarded. */
	void newGrid(int rows, int columns) {
		grid = new char[rows][columns];
	}
}
