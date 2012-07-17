/*
 * TODO: features for drafting a puzzle...
 * - mode where clicking on a cell places an island, e.g. a 1, and clicking again increments it
 *   - right-clicking decrements it, and when it reaches 0 it's gone
 * - function to "sprinkle seeds" 
 * TODO: need to redraw the board on a resize, even when in noLoop.
 *   So we need to detect a resize either here, or in the frame.
 *   Could detect it in the frame and then call vis.redraw().
 *   Or try to understand http://dev.processing.org/bugs/show_bug.cgi?id=209
 *   I did this, and it works sometimes, but sometimes no redraw occurs.
 * DONE: I'm going to need text labels, buttons and stuff to give more info about
 * what's happening, and more user control. UI is outside the scope of Processing.
 * Surround the Processing applet component with a regular Java GUI frame
 * or whatever. Using Swing. Some say it's bloated, others say it's not substantially
 * slower than SWT. It sounds like Swing is more widespread and easier to use.
 * I'll go with it. I don't expect the UI to be the performance bottleneck. 
 * DONE: during solving, turn on loop; when not solving, turn it off.
 *  (but make sure redraw still occurs).
 * DONE: cache result of isSolved; pass it back and forth via setState; clear it when sthg changes
 * DONE: parse new file format (lgo_1500.txt)
 * DONE: visualize solving progress (colors of islands, etc.)
 * DONE: implement stop-motion mode; solver thread pauses after every
 * inference or at each new hypotheses, waiting for user input to continue.
 * Key controls:
 * DONE: 1 ("one step") - do one step and pause until further input.
 *   When starting a new hypothesis, do not run it (recursively) to conclusion.
 * TODO: i ("step into") - run until starting a new hypothesis.
 * TODO: v ("step over") -  do one step and pause until further input.
 *   When starting a new hypothesis, DO run it (recursively) to conclusion.
 * TODO: u ("step out")  -  continue until current hypothesis trial returns.
 *   If no hypothesis trial is running, equivalent to 'c'.
 * p or SPACE ("pause") - pause until further input.
 *   If already paused, Continue.
 * (Any of the above can be used as an interrupt.)
 * c ("continue") - run and do not stop until finished or interrupted.
 * ESC - stop solving; if already stopped, dispose of solver.
 *   (maybe ask for confirmation?)
 * TODO: r ("restart") - go back to all unknowns except the numbers.
 * A "step" is typically the application (fruitful or not) of one rule,
 * or the creation of a new trial (hypothesis) or the return from a hypothesis trial.
 * TODO: profiling (accumulate performance stats about rules fired)
 * TODO: add some rules?
 * TODO: batch mode
 * DONE: when user resizes window, resize grid squares accordingly. (but not font?) 
 */

package net.huttar.nuriGarden;

import java.awt.Dimension;

import processing.core.PApplet;
import processing.core.PFont;

class NuriVisualizer extends PApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1306716595311291496L;
	
	private PFont currFont = null, bigFont = null, smallFont = null; 
	private final int smallFontSize = 24, bigFontSize = 40;
	private int currFontSize = 0;

	private final int margin = 10;
	private final int prefCellSize = smallFontSize;
	private int cellW = prefCellSize, cellH = prefCellSize;
	
	NuriFrame frame = null;
	NuriBoard puz = null;
	NuriSolver solver = null;
	
	
	public NuriVisualizer(NuriBoard board, NuriSolver s) {
		super();
		puz = board;
		solver = s;
	}

	public void setup() {
		size(400, 400); // default
		if (puz != null) setSizeToBoard(puz);
		
		System.out.println(Runtime.getRuntime().availableProcessors());
		
		// Start with given file and debug mode.
		// solver = new NuriSolver("../samples/lgo_1500.txt", 19, false, this);
		// solver = new NuriSolver("../samples/tiny3.puz", 1, false, this);
		// solver = new NuriSolver("samples/janko_ts.txt", 182, false, this);

		frameRate(10);
		bigFont = loadFont("Gentium-40.vlw");
		smallFont = loadFont("Gentium-24.vlw");
		currFont = bigFont;
		textFont(bigFont);
		textAlign(CENTER);
		currFontSize = currFont.getSize(); // docs don't say if this is pixels or points
		// System.out.println("Curr font size: " + currFontSize);
		
		// use HSB color mode with low-res hue 
		colorMode(HSB, 30, 100, 100);
		noLoop(); // draw only on demand, except during solving
	}
        
	void setSizeToBoard(NuriBoard puz) {
		int prefW = puz.getWidth() * prefCellSize + margin * 2;
		int prefH = puz.getHeight() * prefCellSize + margin * 2; 
		setPreferredSize(new Dimension(prefW, prefH));
		size(prefW, prefH);
	}
	
	void decideFont(int cellSize) {
		if (cellSize < (bigFontSize * 1.2) && currFont != smallFont) {
			currFont = smallFont;
			textFont(smallFont);
			currFontSize = smallFontSize;
		} else if (cellSize >= (bigFontSize * 1.2) && currFont != bigFont){
			currFont = bigFont;
			textFont(bigFont);
			currFontSize = bigFontSize;
		}
	}
	
	//TODO: draw numbers in decimal (2-digit) when > 9
    void drawGrid(NuriBoard puz) {
		// System.out.println("In drawGrid() at " + System.currentTimeMillis()); // debugging
    	//TODO: probably need to add synchronization or sthg to make sure the board object doesn't disappear on us
    	// while we're accessing it.
    	int left = 0, top = 0, margin = 10;
    	cellW = (width - margin * 2) / puz.getWidth();
    	cellH = (height - margin * 2) / puz.getHeight();
    	// Make cells square.
    	cellW = Math.min(cellW, cellH);
    	cellH = cellW;
    	decideFont(cellW);
    	int cw2 = cellW / 2;
    	int numberHeight = cw2 + currFontSize * 7 / 24; // This approximation seems to work well.
    	int right = puz.getWidth() * cellW,
    		bottom = puz.getHeight() * cellH;
    	int i, j;

    	background(0, 0, 75); // light gray
    	
    	pushMatrix();
    	translate(margin, margin);

    	stroke(0, 0, 40); // medium gray
        strokeWeight(1);

        drawLines(cellW, cellH, top, left, right, bottom);

        // draw shadow lines for etched look?
        if (cellW >= prefCellSize) {
	        pushMatrix();
	        stroke(0, 0, 90); // light gray
	    	translate(1, 1);
	        drawLines(cellW, cellH, top, left, right, bottom);
	        popMatrix();
        }
        
        noStroke();
        for (i = 0; i < puz.getHeight(); i++) {
        	for (j = 0; j < puz.getWidth(); j++) {
        		char c = puz.get(i, j);
        		short gl = puz.getGuessLevel(i, j);
        		int x = j * cellW + 1, y = i * cellH + 1;
        		if (c == NuriBoard.BLACK) {
        			setFill(gl, NuriBoard.BLACK);
    				rect(x, y, cellW - 1, cellH - 1);
        		} else if (NuriBoard.isWhite(c)) {
        			setFill(gl, NuriBoard.WHITE);
					rect(x, y, cellW - 1, cellH - 1);
					if (NuriBoard.isANumber(c)) {
						drawNumber(c, x + cw2, y + numberHeight);
						// text(c, x + cw2, y + numberHeight);
					}
        		}
        	}
        }
        
        if (solver != null && solver.lastChangedCell != null) {
	        stroke((float) 5.1, 85, 100); // yellow
	        noFill();
	        strokeWeight((float) (cellW * 0.1));
	        i = solver.lastChangedCell.getRow();
	        j = solver.lastChangedCell.getColumn();
	        rect(cellW * j, cellH * i, cellW, cellH);
        }
        
    	popMatrix();
    }

    private void drawLines(int cellW, int cellH, int top, int left, int right,
			int bottom) {
        for (int i = 0; i <= puz.getHeight(); i++)
        	line(left, i * cellH, right, i * cellH);
        for (int j = 0; j <= puz.getWidth(); j++)
        	line(j * cellW, top, j * cellW, bottom);
	}

	/** Draw digits on grid. 
     * c is character representation of digit.
     * Nudge left if 2-digit number. */
    private void drawNumber(char c, int x, int y) {
    	int d = NuriBoard.numberValue(c);
    	assert(d < 100); // The parser can't give us numbers > 99.
		fill(0, 0, 0);  // black
    	if (d < 10)
    		text(c, x, y);
    	else
    		text(Integer.toString(d), x - currFontSize / 20, y);
	}

	/** Set fill color for square based on guessLevel
     * (known vs. hypothesis --> hue) and putative value.
     * @param guessLevel
     * @param value
     */
    private void setFill(short guessLevel, char value) {
    	if (guessLevel == 0)
    		fill(0, 0, (value == NuriBoard.BLACK) ? 0 : 100);
    	else
    		fill(guessLevel, 40,
    				(value == NuriBoard.BLACK) ? 40 : 100);
    }
    
	public void draw() {
		// System.out.println("In draw() at " + System.currentTimeMillis()); // debugging
		if (solver != null && solver.latestBoard != null) {
			if (puz == null)				
				setSizeToBoard(solver.latestBoard);
			puz = solver.latestBoard;
			drawGrid(puz);
			frame.updateStatus();
		}
	}
	
	public void mousePressed() {
		// get cell where the click occurred
		int c = (mouseX - margin) / cellW;
		int r = (mouseY - margin) / cellH;
		// System.out.println("Mouse clicked in cell: " + c + ", " + r);
		frame.clickedCell(r, c, (mouseButton == LEFT));
	}
	
	//TODO: detect keys in the frame, not in the visualizer
	public void keyPressed() {
		frame.keyPressed(key);
	}

	void setSolver(NuriSolver s) {
		solver = s;		
	}
}
