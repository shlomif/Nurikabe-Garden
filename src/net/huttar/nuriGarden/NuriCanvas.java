/**
 *
 */
package net.huttar.nuriGarden;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;

import javax.swing.JPanel;

import processing.core.PFont;

/**
 * Draw board and solver state.
 * TODO: develop this to replace NuriVisualizer.
 */
public class NuriCanvas extends JPanel implements MouseListener {

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

	private int frameRate = 10; //## Do something with this.

	NuriFrame frame = null;
	NuriBoard puz = null;
	NuriSolver solver = null;

	public NuriCanvas(NuriBoard board, NuriSolver s, NuriFrame f) {
		super(); // redundant?
		puz = board;
		solver = s;
		thinStroke = new BasicStroke();
		frame = f;
		f.addMouseListener(this);
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
		//## bigFont = loadFont("Gentium-40.vlw");
		//## smallFont = loadFont("Gentium-24.vlw");
		//## currFont = bigFont;
		//## textFont(bigFont);
		// textAlign(); // CENTER
		// currFontSize = currFont.size; // docs don't say if this is pixels or points
		// System.out.println("Curr font size: " + currFontSize);

		// use HSB color mode with low-res hue
		// colorMode(HSB, 30, 100, 100);
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
			//##textFont(smallFont);
			currFontSize = smallFontSize;
		} else if (cellSize >= (bigFontSize * 1.2) && currFont != bigFont){
			currFont = bigFont;
			//##textFont(bigFont);
			currFontSize = bigFontSize;
		}
	}

	private BasicStroke thinStroke = null;
    private BasicStroke thickStroke = null;

	//TODO: draw numbers in decimal (2-digit) when > 9
    void drawGrid(NuriBoard puz, Graphics2D g2d) {
		// System.out.println("In drawGrid() at " + System.currentTimeMillis()); // debugging
    	//TODO: probably need to add synchronization or sthg to make sure the board object doesn't disappear on us
    	// while we're accessing it.
    	int left = 0, top = 0, margin = 10;
    	cellW = (getWidth() - margin * 2) / puz.getWidth();
    	cellH = (getHeight() - margin * 2) / puz.getHeight();
    	// Make cells square.
    	cellW = Math.min(cellW, cellH);
    	cellH = cellW;
    	thickStroke = new BasicStroke(cellW / 10.0f);
    	decideFont(cellW);
    	int cw2 = cellW / 2;
    	int numberHeight = cw2 + currFontSize * 7 / 24; // This approximation seems to work well.
    	int right = puz.getWidth() * cellW,
    		bottom = puz.getHeight() * cellH;
    	int i, j;

    	// background(0, 0, 75); // light gray


    	AffineTransform saveXform = g2d.getTransform(); // pushMatrix();
    	g2d.translate(margin, margin);
    	g2d.setColor(Color.gray); // stroke(0, 0, 40); // medium gray
        g2d.setStroke(thinStroke); // strokeWeight(1);

        drawLines(cellW, cellH, top, left, right, bottom, g2d);

        // draw shadow lines for etched look?
        if (cellW >= prefCellSize) {
	        g2d.setColor(Color.lightGray); // light gray
	    	g2d.translate(1, 1);
	        drawLines(cellW, cellH, top, left, right, bottom, g2d);
	        g2d.translate(-1, -1); // popMatrix();
        }

        //noStroke();
        for (i = 0; i < puz.getHeight(); i++) {
        	for (j = 0; j < puz.getWidth(); j++) {
        		char c = puz.get(i, j);
        		short gl = puz.getGuessLevel(i, j);
        		int x = j * cellW + 1, y = i * cellH + 1;
                // color = g.getHSBColor( ...);
        		if (c == NuriBoard.BLACK) {
        			setFill(gl, NuriBoard.BLACK, g2d);
    				g2d.fillRect(x, y, cellW - 1, cellH - 1);
        		} else if (NuriBoard.isWhite(c)) {
        			setFill(gl, NuriBoard.WHITE, g2d);
					g2d.fillRect(x, y, cellW - 1, cellH - 1);
					if (NuriBoard.isANumber(c)) {
						drawNumber(c, x + cw2, y + numberHeight, g2d);
						// text(c, x + cw2, y + numberHeight);
					}
        		}
        	}
        }

        if (solver != null && solver.lastChangedCell != null) {
	        g2d.setColor(Color.yellow); // yellow, (float) 5.1, 85, 100
	        g2d.setStroke(thickStroke);// strokeWeight((float) (cellW * 0.1));
	        i = solver.lastChangedCell.getRow();
	        j = solver.lastChangedCell.getColumn();
	        g2d.drawRect(cellW * j, cellH * i, cellW, cellH);
        }

        g2d.setTransform(saveXform); // popMatrix();
    }

    private void drawLines(int cellW, int cellH, int top, int left, int right,
			int bottom, Graphics2D g2d) {
        for (int i = 0; i <= puz.getHeight(); i++)
        	g2d.drawLine(left, i * cellH, right, i * cellH);
        for (int j = 0; j <= puz.getWidth(); j++)
        	g2d.drawLine(j * cellW, top, j * cellW, bottom);
	}

	/** Draw digits on grid.
     * c is character representation of digit.
     * Nudge left if 2-digit number. */
    private void drawNumber(char c, int x, int y, Graphics2D g2d) {
    	int d = NuriBoard.numberValue(c);
    	assert(d < 100); // The parser can't give us numbers > 99.
		g2d.setColor(Color.black);
    	if (d < 10)
    		g2d.drawString(Character.toString(c), x, y);
    	else
    		g2d.drawString(Integer.toString(d), x - currFontSize / 20, y);
	}

	/** Set fill color for square based on guessLevel
     * (known vs. hypothesis --> hue) and putative value.
     * @param guessLevel
     * @param value
     */
    private void setFill(short guessLevel, char value, Graphics2D g2d) {
    	if (guessLevel == 0)
    		g2d.setColor(value == NuriBoard.BLACK ? Color.black : Color.white);
    	else
    		g2d.setColor(Color.getHSBColor(guessLevel / 30.0f, 0.4f,
    				(value == NuriBoard.BLACK) ? 0.4f : 1.0f));
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
			drawGrid(puz, g2d);
			frame.updateStatus();
		}
	}

	@Override
	public void mouseClicked(MouseEvent arg0) {
		// we don't care about this event.
	}

	@Override
	public void mouseEntered(MouseEvent e) {
		// we don't care about this event.
	}

	@Override
	public void mouseExited(MouseEvent e) {
		// we don't care about this event.
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		// we don't care about this event.
	}

	public void mousePressed(MouseEvent e) {
		//##FIXME: r seems to be off by one
		// get cell where the click occurred
		int c = (e.getX() - margin) / cellW;
		int r = (e.getY() - margin) / cellH;
		// System.out.println("Mouse clicked in cell: " + c + ", " + r);
		frame.clickedCell(r, c, (e.getButton() == MouseEvent.BUTTON1));

	}

}
