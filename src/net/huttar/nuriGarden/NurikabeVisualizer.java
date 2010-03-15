/*
 * TODO: I'm going to need text labels, buttons and stuff to give more info about
 * what's happening, and more user control. UI is outside the scope of Processing.
 * Surround the Processing applet component with a regular Java GUI frame
 * or whatever. Probably not Swing -- I hear it's bloated. 
 * TODO: during solving, turn on loop; when not solving, turn it off.
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

import processing.core.PApplet;
import processing.core.PFont;

import net.huttar.nuriGarden.NuriSolver.StopMode;

class NurikabeVisualizer extends PApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1306716595311291496L;

	int margin = 10;

	NuriState puz = null;
	NuriSolver solver = null;
	
	boolean startedSolving = false;
	
	public void setup() {
		size(400, 400);
		
		System.out.println(Runtime.getRuntime().availableProcessors());
		
		// Start with given file and debug mode.
		// solver = new NuriSolver("../samples/lgo_1500.txt", 19, false, this);
		// solver = new NuriSolver("../samples/tiny3.puz", 1, false, this);
		// solver = new NuriSolver("samples/janko_ts.txt", 182, false, this);

		frameRate(15);
		PFont fontClue = loadFont("CourierNew36.vlw");
		textFont(fontClue, 36);
		textAlign(CENTER);
		
		// use HSB color mode with low-res hue 
		colorMode(HSB, 30, 100, 100);

		startedSolving = false;
	}
        
    void drawGrid(NuriState puz) {
		// System.out.println("In drawGrid() at " + System.currentTimeMillis()); // debugging
    	//TODO: need to add synchronization or sthg to make sure the board object doesn't disappear on us
    	// while we're accessing it.
    	puz = solver.latestBoard;
    	int left = 0, top = 0, margin = 10;
    	int cellW = (width - margin * 2) / puz.getWidth();
    	int cellH = (height - margin * 2) / puz.getHeight();
    	// Make cells square.
    	cellW = Math.min(cellW, cellH);
    	cellH = cellW;
    	int right = puz.getWidth() * cellW,
    		bottom = puz.getHeight() * cellH;
    	int i, j;

    	background(0, 0, 75); // light gray
    	
    	pushMatrix();
    	translate(margin, margin);

    	stroke(0, 0, 50); // medium gray
        strokeWeight(1);
    	
        for (i = 0; i <= puz.getHeight(); i++)
        	line(left, i * cellH, right, i * cellH);
        for (j = 0; j <= puz.getWidth(); j++)
        	line(j * cellW, top, j * cellW, bottom);
        
        noStroke();
        for (i = 0; i < puz.getHeight(); i++) {
        	for (j = 0; j < puz.getWidth(); j++) {
        		char c = puz.get(i, j);
        		short gl = puz.getGuessLevel(i, j);
        		int x = j * cellW + 1, y = i * cellH + 1;
        		if (c == NuriState.BLACK) {
        			setFill(gl, NuriState.BLACK);
    				rect(x, y, cellW - 1, cellH - 1);
        		} else if (NuriState.isWhite(c)) {
        			setFill(gl, NuriState.WHITE);
					rect(x, y, cellW - 1, cellH - 1);
					if (NuriState.isANumber(c)) {
						fill(0, 0, 0);
						text(c, x + (int)(cellW * 0.5), y + (int)(cellH * 0.5) + 10);
					}
        		}
        	}
        }
        
        if (solver.lastChangedCell != null) {
	        stroke((float) 5.1, 85, 100); // yellow
	        noFill();
	        strokeWeight((float) (cellW * 0.1));
	        i = solver.lastChangedCell.getRow();
	        j = solver.lastChangedCell.getColumn();
	        rect(cellW * j, cellH * i, cellW, cellH);
        }
        
    	popMatrix();
    }

    /** Set fill color for square based on guessLevel
     * (known vs. hypothesis --> hue) and putative value.
     * @param guessLevel
     * @param value
     */
    private void setFill(short guessLevel, char value) {
    	if (guessLevel == 0)
    		fill(0, 0, (value == NuriState.BLACK) ? 0 : 100);
    	else
    		fill(guessLevel, 40,
    				(value == NuriState.BLACK) ? 40 : 100);
    }
    
	public void draw() {
		// System.out.println("In draw() at " + System.currentTimeMillis()); // debugging
		drawGrid(puz);
	}
	
	public void mouseClicked() {
		go();
	}
	
	void go() {
		if (!startedSolving) {
			drawGrid(puz);
			startedSolving = true;
			solver.start();
		}
	}
	
	//TODO: move these controls out of visualizer
	public void keyPressed() {
		System.out.println("Got key: " + key);

		switch(key) {
		case '1':
			solver.stopMode = StopMode.ONESTEP;
			solver.threadSuspended = false;
			break;
		case ' ':
		case 'p':
			solver.stopMode = StopMode.CONTINUE;
			solver.threadSuspended = !solver.threadSuspended;
			break;
		case 'c':
		case 'C':
			solver.stopMode = StopMode.CONTINUE;
			solver.threadSuspended = false;
			break;
		case 'r':
			solver.stopMode = StopMode.RESTART;
			solver.threadSuspended = false;
			break;
		}

		go(); // start if not yet started

		if (!solver.threadSuspended) {
			synchronized(this) {
	            this.notifyAll();					
			}
		}
	}

	void setSolver(NuriSolver s) {
		solver = s;		
	}
}
