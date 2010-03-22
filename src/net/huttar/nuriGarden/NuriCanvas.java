/**
 * 
 */
package net.huttar.nuriGarden;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.JPanel;

import processing.core.PFont;

/**
 * Draw board and solver state.
 * TODO: develop this to replace NuriVisualizer.
 */
public class NuriCanvas extends JPanel {
	
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

	private int frameRate = 10;
	
	NuriFrame frame = null;
	NuriBoard puz = null;
	NuriSolver solver = null;
	
	
	public NuriCanvas(NuriBoard board, NuriSolver s) {
		super(); // redundant?
		puz = board;
		solver = s;
	}

	public void setup() {
		setSize(400, 400); // Is this right for JComponent?
		if (puz != null) setSizeToBoard(puz);
		
		System.out.println(Runtime.getRuntime().availableProcessors());
		
		// Start with given file and debug mode.
		// solver = new NuriSolver("../samples/lgo_1500.txt", 19, false, this);
		// solver = new NuriSolver("../samples/tiny3.puz", 1, false, this);
		// solver = new NuriSolver("samples/janko_ts.txt", 182, false, this);

		frameRate = 10;
		bigFont = loadFont("Gentium-40.vlw");
		smallFont = loadFont("Gentium-24.vlw");
		currFont = bigFont;
		textFont(bigFont);
		textAlign(CENTER);
		currFontSize = currFont.size; // docs don't say if this is pixels or points
		// System.out.println("Curr font size: " + currFontSize);
		
		// use HSB color mode with low-res hue 
		colorMode(HSB, 30, 100, 100);
		// TODO: noLoop(); // draw only on demand, except during solving
	}
        
	void setSizeToBoard(NuriBoard puz) {
		int prefW = puz.getWidth() * prefCellSize + margin * 2;
		int prefH = puz.getHeight() * prefCellSize + margin * 2; 
		setPreferredSize(new Dimension(prefW, prefH));
		setSize(prefW, prefH); // TODO: right?
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

	public void paintComponent(Graphics g) {
        super.paintComponent(g); 

        Graphics2D g2d = (Graphics2D) g;

		// System.out.println("In paintComponent() at " + System.currentTimeMillis()); // debugging
		if (solver != null && solver.latestBoard != null) {
			if (puz == null)				
				setSizeToBoard(solver.latestBoard);
			puz = solver.latestBoard;
			drawGrid(puz);
			frame.updateStatus();
		}
	}
}
