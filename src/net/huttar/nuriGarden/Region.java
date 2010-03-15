package net.huttar.nuriGarden;
import java.util.*;

/** A set of locations in a NuriState puzzle. */
class Region extends HashSet<Coords> {

	/** Eclipse says this class should have a serialVersionUID, so here it is. */
	private static final long serialVersionUID = -6500576669930239666L;

	/** The puzzle in which this Region belongs. */
	protected NuriState puzzle;

	/** Create an empty Region. */
	Region(NuriState puzzle) {
		this.puzzle = puzzle;
	}

	/**
	 * Create a Region in puzzle containing cell.
	 */
	Region(NuriState puzzle, Coords cell) {
		this.puzzle = puzzle;
		add(cell);
	}

	/**
	 * Create a Region in puzzle containing cell and all contiguous cells with
	 * labels in allowable.
	 */
	Region(NuriState puzzle, Coords cell, String allowable) {
		this.puzzle = puzzle;
		add(cell);
		find(allowable);
	}

	/** Add all contiguous cells with labels in allowable. */
	protected void find(String allowable) {
		/* TODO: optimization: save the new cells in getNeighbors() and only
		 * get their neighbors. */
		int previousSize = -1;
		while (previousSize < size()) {
			previousSize = size();
			addAll(getNeighbors(allowable));
		}
	}

	/** Return cell with contents in target, accessible via
	 * cells with contents in allowable. Return null if target not found.
	 * Side effect: adds some contiguous cells with labels an allowable.
	 * When null is returned, has same effect as find(allowable). */
	protected Coords find(String allowable, String target) {
		Region newNeighbors = this;

		while (!newNeighbors.isEmpty()) {
			Region targets = newNeighbors.getNeighbors(target);
			if (!targets.isEmpty()) return (Coords)(targets.iterator().next());
			/* Save the new cells from getNeighbors() and only
			 * get their neighbors. */
			newNeighbors = newNeighbors.getNeighbors(allowable);
			newNeighbors.removeAll(this);
			this.addAll(newNeighbors);
		}
		return null;
	}

	/** Return the set of cells adjacent to this Region with labels in allowable.
	 * Result may include cells in this Region. */
	Region getNeighbors(String allowable) {
		Region result = new Region(puzzle);
		for (Coords cell : this) {
			for (Coords neighbor : puzzle.neighbors(cell)) {
				if (allowable.indexOf(puzzle.get(neighbor)) >= 0) {
					result.add(neighbor);
				}
			}
		}
		return result;
	}
	
	/**
	 * Return the set of cells adjacent to this Region, not including cells
	 * in this Region.
	 * 
	 */
	Region getOnlyNeighbors() {
		Region result = new Region(puzzle);
		for (Coords cell : this) {
			for (Coords neighbor : puzzle.neighbors(cell)) {
				if (!this.contains(neighbor)) {
					result.add(neighbor);
				}
			}
		}
		return result;
	}

}
