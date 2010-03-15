/** TODO: should be able to optimize by putting in some simple
 * specific cases, which are quicker to check, before the
 * more general cases that take longer to compute. 
 ** TODO: add profiling: in each rule method, record:
 * - count of calls
 * - duration of call
 * - success/failure (deduced anything or not) of each call
 * We want to derive:
 * - what % of calls result in success (progress)
 * - how long it takes on average
 *   - when successful
 *   - when failing
 *   - overall
 * Some of these will be influenced by the order in which
 * we call rules.
 */
package net.huttar.nuriGarden;

import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;


class NuriSolver extends Thread  {
	// For explanation of comments below of the form //A or B or C, see notes.txt

	enum StopMode { ONESTEP, STEPIN, STEPOVER, STEPOUT, CONTINUE, RESTART };
	
	volatile StopMode stopMode = StopMode.CONTINUE; //B (or A)

	/** Tells thread when to pause. */
	volatile boolean threadSuspended = false; //B (or A)
	
	/** Whether to output debug messages. */
	private static boolean debugMode = false; //A
	
	/** Time the solving process started/ended. */
	private long startTime = 0; //B
	private long endTime = 0; //B

	/** The last rule that was used for an inference. */
	volatile String lastRule; //B (strictly should be C, but for our purposes,
	// B is fine. If we need the accuracy, we could set it on return from recursion.)
	
	/** Coordinates of the last cell changed... for display purposes. */
	volatile Coords lastChangedCell = null; //B
	
	/** State of board we're solving. */
	private NuriState topBoard = null; //C

	volatile NuriState latestBoard = null; //B
	
	private NuriVisualizer visualizer = null; //B (or A)

	// boolean success = false; //B if needed (not currently)
	
	/** "Rule" label for guesses. */
	//TODO: check, is this actually useful?
	private static final String hypothesisLabel = "[guess]"; //A
	
	/** Max depth of recursive search. */
	static private int maxSearchDepth = 0; //B

	/** Depth of recursive search. */
	short searchDepth() {
		if (latestBoard != null)
			return latestBoard.searchDepth;
		else
			return 0;
	}
	
	/** Number of hypotheses tried through all instances of this class. */
	static private int totalHypotheses = 0; //B

	/** Number of inferences made through all instances of this class. */
	static private int totalInferences = 0; //B

	/** Rule 0 only has to be run once, since numbered cells don't change. */
	private static boolean ranRule0 = false; //B


	/** Read specified puzzle from the given file. */
	NuriSolver(NuriState board,
			boolean debugMode, NuriVisualizer vis) {
		NuriSolver.debugMode = debugMode;
		this.visualizer = vis;

		latestBoard = topBoard = board;
	}

	/** Read the specified puzzle, solve it, and verify the solution. */
	/** Usage: java ClassName [-d] filename (minus ".puz") */
	public static void main(String[] args) {
		int fileArg = 0;

		if (args[0].equals("-d")) { debugMode = true; fileArg++; }
		// if (args[fileArg].equals("-b")) {
		//   fileArg++;
		//   TODO: process batch of puzzles
		// }

		NuriParser parser = new NuriParser(args[fileArg].indexOf('.') > -1 ?
				args[fileArg] : args[fileArg] + ".puz");
		NuriState board = parser.loadFile(182);

		NuriSolver solver = new NuriSolver(board, true, null);
		// board.setSolver(solver);
				
		System.out.println(solver.topBoard);

		solver.solveWrapper();
	}

	/** Start solver thread only if not already started. */
	public void maybeStart() {
		if (getState() == Thread.State.NEW)
			start();		
	}

	// How to run as a thread.
	// Later if we use threads within a solver, we'll want run() to call solve().
	public void run() {
		solveWrapper();
	}

	boolean solveWrapper() {
		boolean success  = false;
		startSolve();
		try {
			success = solve(topBoard);
        } catch (ContradictionException e) {
			System.out.println("Contradiction: " + e.toString()
					+ "\nThis puzzle is inconsistent.");        	
        }
        
        endSolve(success);	
        return success;
	}
	
	void startSolve() {
		startTime = System.currentTimeMillis();		
	}

	void endSolve(boolean success) {
		if (success) {
			endTime = System.currentTimeMillis();
			System.out.println("Successfully solved puzzle in " + (endTime - startTime)
					+ " ms with " + totalInferences
					+ " inferences and " + totalHypotheses +
					" guesses at a max search depth of " + maxSearchDepth);			
			// System.out.println("Correct? " + checkSolution(args[fileArg])); TODO: implement this
		} else {
			System.out.println("Unable to solve puzzle. I got this far:");
			System.out.println(latestBoard);
			debugMsg(searchDepth(), "Regions:");
			latestBoard.debugDetails();
		}
	}

	/** Output debugging message. */
	static void debugMsg(String msg) {
		if (debugMode) {
			System.out.println("> " + msg);
		}
	}

	/** Output debugging message. */
	static void debugMsg(int depth, String msg) {
		if (debugMode) {
			// Is there an easier way to repeating a character n times?
			for (int i = 0; i < depth; i++)
				System.out.print(' ');
			System.out.println(depth + "> " + msg);
		}
	}

	/** Output progress metrics to debugging console or visualizer. */
	void showProgress(NuriState board) {
		// if (visualizer != null) visualizer.draw();

		/** From deep in a search, find innermost puzzle whose state was sure. */
		/** TODO: could optimize this by making lastSureBoard
		 * an instance variable of nuriSolver and make sure to clear it
		 * if we return from a sure board.
		 */
		NuriState lastSureBoard;
		for (lastSureBoard = board; !lastSureBoard.isSure;
			lastSureBoard = lastSureBoard.predecessor)
			;
		int numCells = board.getWidth() * board.getHeight();
		int numGuessed = numCells - board.numUnknownCells;
		int numSure = numCells - lastSureBoard.numUnknownCells;
		debugMsg(searchDepth(), "Progress: " + numSure + " (" + ((100 * numSure) / numCells) + "%) cells known, "
				+ numGuessed + " (" + ((100 * numGuessed) / numCells) + "%) guessed or known.");
	}

	/** Solve the puzzle and return true if successful. */
	boolean solve(NuriState board) throws ContradictionException {
		board.changed = true;
		board.validityKnown = false;
		
		if (maxSearchDepth < board.searchDepth) maxSearchDepth = board.searchDepth;
		showProgress(board);
		
		/*
		 * Try rules and search until puzzle is solved, or until they don't
		 * help.
		 */
		while (!board.isSolved() && board.changed) {
			/*
			 * Apply production rules until puzzle is solved, or until they
			 * don't help.
			 */
			while (!board.isSolved() && board.changed) {
				// check for any constraint violations to catch them early and prune.
				board.checkConstraints();
				
				board.changed = false;
				try {
					applyProductionRules(board);
				} catch (ContradictionException e) {
					throw e;
				}
			}
		}

		// Last resort: try searching.
		if (!board.isSolved()) {
			// Find an unknown cell.
			Object cellAndColor[] = board.pickUnknownCell();
			Coords unknownCell = (Coords)(cellAndColor[0]);
			char color = ((Character)(cellAndColor[1])).charValue();
			// Try out a hypothesis. Each hypothesis will either solve the puzzle
			// or fail completely.
			//TODO: use child threads for the two searches, if appropriate.
			// How do we find out whether multiple processors are available?
			if (!trySearch(board, unknownCell, color, false))
				// Try out other hypothesis.
				trySearch(board, unknownCell, NuriState.isWhite(color) ? NuriState.BLACK : NuriState.WHITE, true);
		}

		return board.isSolved();
	}
	
	/** Infer that the given cell holds the given value.
	 * Call set() and issue a debugging message. */
	protected void infer(NuriState board, Coords cell, char value, String rule) throws ContradictionException {
		if (board.alreadyIs(cell, value)) return; // Already was that value.
		totalInferences++;
		lastRule = rule;
		if (lastChangedCell == null)
			lastChangedCell = new Coords(0, 0);
		lastChangedCell.copy(cell);
		debugMsg(searchDepth(), "Rule " + rule + ": " + cell + " = " + value);
		board.set(cell, value);
		board.setGuessLevel(cell, board.searchDepth);
		debugMsg(searchDepth(), "\n" + cell.toString());
		checkControls();	// obey execution controls
	}
	
	/** Put the given value in the given cell and recursively try to solve the puzzle.
	 * Return true if successful. */ 
	private boolean trySearch(NuriState board, Coords cell, char value, boolean isSure) {
		totalHypotheses++;
		showProgress(board);
		
		// Save state
		NuriState trialBoard = (NuriState)board.clone();
		latestBoard = trialBoard;
		trialBoard.isSure = (isSure ? board.isSure : false);
		trialBoard.predecessor = board;
		trialBoard.searchDepth = (short)(board.searchDepth + 1);
		
		String hypoth = "Hypothesis " + cell + " = '" + value + "'";
		try {
			debugMsg(searchDepth(), "Trying " + hypoth + "...");
			infer(trialBoard, cell, value, hypothesisLabel);
			/** TODO: Careful here... do we need to create a recursive instance
			 * of solver? That's how we were doing it before...
			 * Which means creating a separate thread? */
			boolean result = solve(trialBoard);
			debugMsg(searchDepth(), hypoth + " success: " + result);
			if (result) {
				board.setState(trialBoard.grid, trialBoard.blackRegions,
						trialBoard.whiteRegions, board.searchDepth, trialBoard.numUnknownCells);
				latestBoard = board;
				board.updateGuessLevel();
				// adjust regions to point to current board instead of trialBoard.
				for (UCRegion region : board.blackRegions)
					region.setState(board);
				for (UCRegion region : board.whiteRegions)
					region.setState(board);				
			}
			return result;
		}
		catch(ContradictionException e) {
			debugMsg(hypoth + " led to contradiction: " + e.getMessage());
			return false;
		}
	}

	/** Apply the inference rules that don't require any trial-and-error. */
	private void applyProductionRules(NuriState board) throws ContradictionException {
		// Is there a way to put references to these methods into an iterator and
		// loop through them?
		rule0(board);
		if (board.isSolved()) return;
		rule1(board);
		if (board.isSolved()) return;
		rule2(board);
		if (board.isSolved()) return;
		rule3(board);
		if (board.isSolved()) return;
		rule4(board);
		if (board.isSolved()) return;
		rule5(board);
		if (board.isSolved()) return;
		rule5a(board);
		if (board.isSolved()) return;
		rule6(board);
	}

	/**
	 * Production rule 0: if a cell is too far from any numbered cell
	 * to be part of its region, that cell must be black.
	 * 
	 * @throws ContradictionException
	 */
	private void rule0(NuriState board) throws ContradictionException {
		if (ranRule0) return; else ranRule0 = true;
		
		outer:
		for (Coords cell : board) {
			if (NuriState.isANumber(board.get(cell))) continue;
			for (UCRegion region : board.whiteRegions) {
				if (cell.manhattanDist(region.getNumberedCell()) + 1 <= region.getLimit())
					continue outer;
			}
			/** No numbered cell is close enough, so this cell must be black. */
			infer(board, cell, NuriState.BLACK, "0");
		}
	}

	/**
	 * Production rule 1: if a white region is full, all its immediate neighbors
	 * must be black.
	 * 
	 * @throws ContradictionException
	 */
	private void rule1(NuriState board) throws ContradictionException {
		for (UCRegion region : board.whiteRegions) {
			if (!region.isHungry()) {
				// Make sure all its neighbors are black
				for (Coords cell : region.getOnlyNeighbors()) {
					switch (board.get(cell)) {
					case NuriState.UNKNOWN:
						// set cell to NuriState.BLACK by rule 1
						infer(board, cell, NuriState.BLACK, "1");
						break;
					case NuriState.BLACK:
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
	private void rule2(NuriState board) throws ContradictionException {
		for (Coords cell : board) {
			if (board.get(cell) != NuriState.UNKNOWN) continue;
			UCRegion reg1 = null, reg2 = null;
			// TODO Enhancement: compare each neighboring numbered white region
			// to every neighboring unnumbered white region, because some unnumbered regions
			// could be too big to unify with 
			// 
			for (Coords neighbor : board.neighbors(cell)) {
				if (NuriState.isWhite(board.get(neighbor))) {
					// Haven't yet found a neighboring region with a number? 
					if (reg1 == null) {
						reg1 = board.containingRegion(neighbor);
						assert(reg1 != null): "Uh oh, white cell not in a region: " + neighbor;
						assert(!reg1.isHungry()): "A full white region at " + neighbor +
							" has neighbor not known to be black, at " + cell; 
						// Only regions containing numbers matter.
						if (reg1.getLimit() == 0) reg1 = null;												
					} else {
						// This is potentially the second neighboring numbered region.
						reg2 = board.containingRegion(neighbor);
						if (reg2 == reg1) continue;
						assert(reg2 != null): "Uh oh, white cell not in a region: " + neighbor;
						assert(!reg2.isHungry()): "A full white region at " + neighbor +
							" has neighbor not known to be black, at " + cell; 
						// Only regions containing numbers matter.
						if (reg2.getLimit() == 0) reg2 = null;
						else {
							// ok, we have two. Then the cell is black.
							infer(board, cell, NuriState.BLACK, "2");
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
	private void rule3(NuriState board) throws ContradictionException {
		try {
			for (UCRegion region : board.whiteRegions) rule3a(board, region);
			if (board.isSolved()) return;
			for (UCRegion region : board.blackRegions) rule3a(board, region);
		}
		catch (ConcurrentModificationException e) {
			// This just means that a region was added to or removed from the list
			// we were iterating over. So bail out, and if necessary we'll get another
			// chance to iterate over the lists again later.
			debugMsg(searchDepth(), "Bailing out of rule3 on ConcurrentModificationException");
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
	private void rule3a(NuriState board, UCRegion region) throws ContradictionException {
		if (!region.isHungry()) return;
		// Find UNKNOWN neighbors.
		Region neighbors = region.getNeighbors("" + NuriState.UNKNOWN);
		switch(neighbors.size()) {
		case 0:
			throw new ContradictionException("Hungry UCRegion " + region + " has nowhere to grow.");
		case 1:
			// The simplest way I know to get the sole item in a HashMap is to use the iterator.
			for (Coords cell : neighbors) {
				// TODO enhancement: we might improve performance a little by passing the region to
				// infer() to set() to addToARegion(), but we'd still have to let the latter
				// search for other possible adjacent regions to merge in.
				infer(board, cell, region.getContentType(), "3");
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
			char targetColor = region.getContentType() == NuriState.WHITE ? NuriState.BLACK : NuriState.WHITE;
			boolean nearWhite = false;
			// ... or if region is white and target cell is next to another white cell.
			if (hunger != 1 && region.getContentType() == NuriState.WHITE) {
				// TODO:
//				Coords cellC = ...;
//				Coords cellD = ...;
//				if (NuriState.isWhite(board.get(cellC)) || NuriState.isWhite(board.get(cellD))) nearWhite = true;
			}
			if (hunger == 1 || nearWhite) {
				if (board.get(targetCell) == NuriState.UNKNOWN)
					infer(board, targetCell, targetColor, "3a");
				else if (NuriState.isWhite(board.get(targetCell)) != NuriState.isWhite(targetColor))
						throw new ContradictionException("Diagonal cell " + targetCell + " should be '" +
								targetColor + "' but is '" + board.get(targetCell) + "'.");
			}
			return;
		default:
			// nothing we can do, for now.
			return;
		}
	}

	/** Production rule 4: If we have 3 black cells in an L-shape, the 4th cell must be white. */
	private void rule4(NuriState board) throws ContradictionException {
		for (Coords cell : board) {
			if (cell.getRow() < board.getHeight() - 1 &&
					cell.getColumn() < board.getWidth() - 1) {
				Coords cell01 = new Coords(cell.getRow(), cell.getColumn()+1);
				Coords cell10 = new Coords(cell.getRow()+1, cell.getColumn());
				Coords cell11 = new Coords(cell.getRow()+1, cell.getColumn()+1);
				Coords whiteCell = null;
				int blackCells = 0;
				if (board.get(cell) == NuriState.BLACK) blackCells++; else whiteCell = cell;
				if (board.get(cell01) == NuriState.BLACK) blackCells++; else whiteCell = cell01;
				if (board.get(cell10) == NuriState.BLACK) blackCells++; else whiteCell = cell10;
				if (board.get(cell11) == NuriState.BLACK) blackCells++; else whiteCell = cell11;
				if (blackCells == 4)
					throw new ContradictionException("2x2 black cells at " + cell);
				else if (blackCells == 3 && board.get(whiteCell) == NuriState.UNKNOWN) {
					infer(board, whiteCell, NuriState.WHITE, "4");
					if (board.isSolved()) break;
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
	private void rule5(NuriState board) throws ContradictionException {
		outer:
		for (Coords cell : board) {
			if (board.get(cell) != '1') {
				List<Coords> neighbors = board.neighbors(cell);
				Iterator<Coords> iter = neighbors.iterator();
				Coords firstNeighbor = (Coords)iter.next();
				char firstValue = NuriState.unifyWhite(board.get(firstNeighbor));
				if (firstValue == NuriState.UNKNOWN) continue outer;
				while (iter.hasNext()) {
					if (NuriState.unifyWhite(board.get((Coords)iter.next())) != firstValue)
						continue outer;
				}
				/* Neighbors are either all white or all black. Conform. */
				if (board.get(cell) != NuriState.UNKNOWN && NuriState.isWhite(board.get(cell)) != NuriState.isWhite(firstValue))
					throw new ContradictionException(cell + " surrounded by opposite color");
				infer(board, cell, firstValue, "5");
			}
		}
	}

	/**
	 * Production rule 5a: Any cell that has no path to a number
	 * must be black; and if there are any known black cells, any cell
	 * that has no path to a black cell must be white.   
	 * This rule overlaps somewhat with rule 6 but is easier to implement.
	 */
	private void rule5a(NuriState board) throws ContradictionException {
		// outer:
		for (Coords cell : board) {
			if (board.get(cell) == NuriState.UNKNOWN ||
					(NuriState.isWhite(board.get(cell)) && board.get(cell) != '1')) {
				// Do we have a path to a number?
				Coords target = board.areaFind(cell, "" + NuriState.WHITE + NuriState.UNKNOWN,
						NuriState.NUMBERS);
				if (target == null) {
					// No path to number so must be black.
					if (board.alreadyIs(cell, NuriState.WHITE))
						throw new ContradictionException("White cell " + cell + " has no path to number.");
					else {
						infer(board, cell, NuriState.BLACK, "5a1");
						// short-circuit on change
						return;
					}
				}
			}
			if ((board.get(cell) == NuriState.UNKNOWN || board.get(cell) == NuriState.BLACK) &&
					!board.blackRegions.isEmpty()) {
				// Do we have a path to a number?
				Coords target = board.areaFind(cell, "" + NuriState.UNKNOWN, "" + NuriState.BLACK);
				if (target == null) {
					// No path to black so must be white.
					if (board.get(cell) == NuriState.BLACK)
						throw new ContradictionException("Black cell " + cell + " has no path to black.");
					else {
						infer(board, cell, NuriState.WHITE, "5a2");
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
	private void rule6(NuriState board) throws ContradictionException {
		/*
		// First check all UNKNOWN cells.
		for (Coords cell : this) {
			if (board.get(cell) == NuriState.UNKNOWN) {
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

	
	/** Check whether we need to pause this thread.
	 * See http://java.sun.com/javase/7/docs/technotes/guides/concurrency/threadPrimitiveDeprecation.html
	 */
	private void checkControls() {
		if (stopMode == StopMode.ONESTEP)
			threadSuspended = true;
        try {
        	if (threadSuspended) {
        		// System.out.println("Suspending thread...");
	            synchronized(visualizer) {
	                while (threadSuspended)
	                    visualizer.wait();
	            }
	            // System.out.println("Resuming thread...");
        	}
        } catch (InterruptedException e){
        }
	}

}
