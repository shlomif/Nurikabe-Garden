/*
 * DONE: cache result of isSolved; pass it back and forth via setState; clear it when sthg changes
 * DONE: parse new file format (lgo_1500.txt)
 * TODO: visualize solving progress (colors of islands, etc.)
 * TODO: implement stop-motion mode; solver thread pauses after every
 * inference or at each new hypotheses, waiting for user input to continue.
 * Key controls:
 * 1 ("step into") - do one step and pause until further input.
 *   When starting a new hypothesis, do not run it (recursively) to conclusion.
 * v ("step over") -  do one step and pause until further input.
 *   When starting a new hypothesis, DO run it (recursively) to conclusion.
 * u ("step out")  -  continue until current hypothesis trial returns.
 *   If no hypothesis trial is running, equivalent to 'c'.
 * p or SPACE ("pause") - pause until further input.
 *   If already paused, Continue.
 * (Any of the above can be used as an interrupt.)
 * c ("continue") - run and do not stop until finished or interrupted.
 * ESC - stop solving; if already stopped, dispose of solver.
 *   (maybe ask for confirmation?)
 * A "step" is typically the application (fruitful or not) of one rule,
 * or the creation of a new trial (hypothesis) or the return from a hypothesis trial.
 * TODO: profiling (accumulate performance stats about rules fired)
 * TODO: add some rules?
 * TODO: batch mode
 */

package nurikabeVisualizer;

import nuriSolver.*;
import processing.core.PApplet;
import processing.core.PFont;

public class NurikabeVisualizer extends PApplet {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1306716595311291496L;

	int cellW = 40, cellH = 40, margin = 10;

	Nurikabe puz = null;
	
	boolean startedSolving = false;
	
	public void setup() {
		frameRate(15);
		PFont fontClue = loadFont("CourierNew36.vlw");
		textFont(fontClue, 36);
		textAlign(CENTER);

		// Start with given file and debug mode.
        puz = Nurikabe.init("../samples/lgo_1500.txt", 19, false, this);
        // puz = Nurikabe.init("../samples/lgo_puzzles.txt", false);
		size(puz.getWidth() * cellW + margin * 2,
				puz.getHeight() * cellH + margin * 2);
		startedSolving = false;
	}
        
    public void drawGrid(Nurikabe puz) {
		// System.out.println("In drawGrid() at " + System.currentTimeMillis()); // debugging
    	//TODO: need to add synchronization or sthg to make sure the board object doesn't disappear on us
    	// while we're accessing it.
    	puz = Nurikabe.latestBoard;
    	int left = 0, top = 0, margin = 10;
    	int right = puz.getWidth() * cellW,
    		bottom = puz.getHeight() * cellH;
    	int i, j;

    	pushMatrix();
    	translate(margin, margin);

    	stroke(128);
    	
        for (i = 0; i <= puz.getHeight(); i++)
        	line(left, i * cellH, right, i * cellH);
        for (j = 0; j <= puz.getWidth(); j++)
        	line(j * cellW, top, j * cellW, bottom);
        
        noStroke();
        for (i = 0; i < puz.getHeight(); i++) {
        	for (j = 0; j < puz.getWidth(); j++) {
        		char c = puz.get(i, j);
        		int x = j * cellW + 1, y = i * cellH + 1;
        		if (c == Nurikabe.BLACK) {
    				fill(0);
    				rect(x, y, cellW - 1, cellH - 1);
        		} else if (Nurikabe.isWhite(c)) {
					fill(255);
					rect(x, y, cellW - 1, cellH - 1);
					if (Nurikabe.isANumber(c)) {
						fill(0);
						text(c, x + (int)(cellW * 0.5), y + (int)(cellH * 0.75));
					}
        		}
        	}
        }
        
    	popMatrix();
    }

	public void draw() {
		// System.out.println("In draw() at " + System.currentTimeMillis()); // debugging
		drawGrid(puz);
	}
	
	public void mouseClicked() {
		if (!startedSolving) {
			drawGrid(puz);
			startedSolving = true;
			puz.start();
		}
	}
	
	public void keyPressed() {
		System.out.println("Got key: " + key);

		switch(key) {
		case '1':
			Nurikabe.stopMode = Nurikabe.StopMode.ONESTEP;
			if (!Nurikabe.threadSuspended) Nurikabe.threadSuspended = true;
			break;
		case ' ':
		case 'p':
			Nurikabe.threadSuspended = !Nurikabe.threadSuspended;
			if (!Nurikabe.threadSuspended) {
				synchronized(this) {
		            this.notifyAll();					
				}
			}
			break;
		case 'c':
		case 'C':
			break;
			
		}
	}
}
