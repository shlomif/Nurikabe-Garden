package net.huttar.nuriGarden;

/**
 * DONE CREATING: a way to place numbers. Suggest an editing mode and a solving mode.
 * "New" switches to editing mode automatically. "Solve" does the reverse.
 * TODO: different cursor to represent editing vs solving mode. Maybe even bg color.
 * TODO: Also provide manual mode switch.
 * DONE: Mouse click places a number: the current number.
 *   numbers 1-9 set the current number TODO: (and modifies the last-edit number if any).
 *   DONE: 0 means current *= 10. +/- increment/decrement.
 *   DONE: Clicking on an existing number overwrites it with the current number.
 *   TODO: Del deletes the number at the last-edit cell, if any.
 * TODO CREATING: provide a way to save. 1st - to new file (.puz). 2nd - append to file (.txt format)
 *   with id, comment, maybe solution.
 * TODO: file open dialog; from whole file (.puz) or from one-in-file, by ID (.txt) 
 * TODO: vis should show most recent edit, when in edit mode
 * DONE: visualizer doesn't show guesses any more! or at least not in color...
 * TODO: Need a way to stop solver, leaving its sure results on the board but
 * allowing user to set white/black; then restart solver taking into account those
 * new settings.
 * TODO: allow user to click on rule shown in status label, to get help
 * frame (attached on right of button panel? or separate window?) that shows
 * html describing the rule.
 * See http://www.softcoded.com/web_design/java_help_files.php
 * for processing hyperlinks.
 * Also http://www.devdaily.com/blog/post/jfc-swing/how-create-simple-swing-html-viewer-browser-java
 * Can use JTextPane rather than JEditorPane, the main difference being that
 * JTextPane doesn't have a constructor that sets you set the HTML page immediately;
 * you have to call .setPage(). Which is fine.
 */

// Using AWT:
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.BorderFactory;
import javax.swing.JMenuBar;

import net.huttar.nuriGarden.NuriSolver.StopMode;

/** 
 * Frame for Nurikabe Garden. Using AWT because Processing (NuriVisualizer)
 * uses AWT, and I don't think we need anything more (Swing, AWT) for now.
 * 
 * @author huttarl
 *
 */
// AWT: 	public class NuriFrame extends Frame {
public class NuriFrame extends JFrame implements ActionListener, ComponentListener {

	private static final long serialVersionUID = 1L;
	private static final int buttonSpacing = 6;
	
	public NuriFrame() {
        super("Nurikabe Garden");
    }

	/** The parser object used to read in files. */
	private NuriParser parser = null;
	private NuriSolver solver = null;
	private NuriBoard board = null;
	private NuriVisualizer vis = null;

	private JLabel statusLabel, depthLabel;
	
	private JButton solveButton;
	private Box buttonPanel;
	
	private int currentNumber = 1;
	
	public enum GardenMode { EDIT, SOLVE };
	GardenMode gardenMode = GardenMode.SOLVE;
		
	private void init() {
		// helpful: see http://zetcode.com/tutorials/javaswingtutorial/swinglayoutmanagement/
		Container panel = this.getContentPane();

		//FIXME: menu disappears behind PApplet. Need to catch event when any menu opens
		// and pause the visualizer?  (noLoop())
		// No, that won't help... it's already not looping. The problem is the z-order 
		// difference between AWT & Swing... http://blogs.sun.com/Swing/entry/awt_swt_swing_java_gui1
		// Solution would be to move menu and PApplet into different windows;
		// or stop using Processing. Or switch from Swing to AWT.
		// The last two mean a loss of coding investment.
		JMenuBar menu = new JMenuBar();
		panel.add(menu, BorderLayout.NORTH);
		JMenu fileMenu = new JMenu("File");
		fileMenu.setMnemonic(KeyEvent.VK_F); // probably need a makeMenuItem method
		JMenuItem saveItem = new JMenuItem("Save");
		saveItem.setMnemonic(KeyEvent.VK_S);
		fileMenu.add(saveItem);
		menu.add(fileMenu);
		
		buttonPanel = new Box(BoxLayout.Y_AXIS);
		// BoxLayout boxLayout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
		// buttonPanel.setLayout(boxLayout);
		panel.add(buttonPanel, BorderLayout.EAST);
		buttonPanel.setBorder(BorderFactory.createEtchedBorder());
		
		makeButton("New", KeyEvent.VK_N,
				"Create a new puzzle", "new");
		// TODO: make Ctrl+N work for this.
		// Probably by making a menu item.
		// This doesn't work: newButton.setAccelerator(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S,
        //  java.awt.Event.CTRL_MASK));
		
		solveButton = makeButton("Solve", KeyEvent.VK_S,
			"Attempt to solve the puzzle uniquely",	"solve");

		makeButton("Redraw", KeyEvent.VK_D,
			"Redraw the puzzle board", "redraw");

		makeButton("Reset Board", KeyEvent.VK_B,
			"Clear the puzzle back to numbers only", "resetBoard");

		makeButton("Exit", KeyEvent.VK_X,
			"Exit Nurikabe Garden", "quit");

        // panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // panel.setLayout(new GridLayout(5, 4, 5, 5));

        // AWT: setLayout(new BorderLayout());

        // JLabel titleLabel = new JLabel("<html><h1>Nurikabe Garden</h1></html>");
        // panel.add(titleLabel, BorderLayout.NORTH);

		/** Status labels at bottom */
		Box labelContainer = new Box(BoxLayout.X_AXIS);
		panel.add(labelContainer, BorderLayout.SOUTH);

        depthLabel = new JLabel();
        labelContainer.add(depthLabel);
        
		// space between labels
		labelContainer.add(Box.createHorizontalStrut(20));

        statusLabel = new JLabel();
        labelContainer.add(statusLabel);
        
        // TODO: get frame to properly surround PApplet.
        setSize(700, 500);
        setResizable(true);
        // pack(); Doesn't work! :-)

        // TODO: parameterize; or get from a File Open dlg
		// parser = new NuriParser("samples/huttar_ts.txt");
		// board = parser.loadFile(9);
		parser = new NuriParser("samples/janko_ts.txt");
		board = parser.loadFile(25); // Janko 25: two-digit
		
		// initial state, debug mode, visualizer
		solver = new NuriSolver(board, false, vis);
		// board.setSolver(solver);
		
        // For embedding Processing applet see
        // http://dev.processing.org/reference/core/javadoc/processing/core/PApplet.html
        vis = new NuriVisualizer(board, solver);
        vis.frame = this;
        // AWT: add(vis, BorderLayout.CENTER);
        panel.add(vis);

		solver.setVisualizer(vis);

		//panel.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));

		// add(panel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

		// Listen for resize events on the frame:
		this.addComponentListener(this);
		
        setVisible(true);
        
        // important to call this whenever embedding a PApplet.
        // It ensures that the animation thread is started and
        // that other internal variables are properly set.
        vis.init();
		vis.setSizeToBoard(board);
        vis.redraw();
	}
	
	private JButton makeButton(String label, int mnem, String toolTip,
			String actionCommand) {
		if (buttonPanel.getComponentCount() > 0) {
			// space between buttons
			buttonPanel.add(Box.createVerticalStrut(buttonSpacing));
		}
		
		JButton newButton = new JButton(label);
		newButton.setMnemonic(mnem);
		newButton.setToolTipText(toolTip);
		newButton.setActionCommand(actionCommand);
		newButton.addActionListener(this);
		buttonPanel.add(newButton);
		
		return newButton;
	}

	/** Update status display.
	 * Called from event loop in visualizer.
	 * I don't think we need to synchronize this, unless it gets critical.
	 * Keep an eye on it though.
	 */
	void updateStatus() {
		if (solver.lastRule != null) {
			// if (statusLabel.getText() != solver.lastRule)
			// TODO: optimize: don't set if not changed.
			statusLabel.setText("Inferred a cell using rule " + solver.lastRule);
		}
		depthLabel.setText("Search depth: " + solver.searchDepth());
	}
	
	public void actionPerformed(ActionEvent e) {
		if ("new".equals(e.getActionCommand())) {
			resetBoard(true);
		} else if ("solve".equals(e.getActionCommand())) {
        	solver.maybeStart();
        	setGardenMode(GardenMode.SOLVE);
        	vis.loop();
        } else if ("redraw".equals(e.getActionCommand())) {
        	vis.redraw();
        } else if ("quit".equals(e.getActionCommand())) {
        	quit();
        } else if ("resetBoard".equals(e.getActionCommand())) {
        	resetBoard(false);
        } else {
        	// assert(false); // unrecognized action event
        }
    }
	
	/** Erase black & white cell states, leaving only numbers. 
	 * If isNew, create a fresh puzzle board, using pop up dialog to ask for dimensions. */
	void resetBoard(boolean isNew) {
		Dimension boardSize = null;
		if (isNew) {
			boardSize = ModalDialog.getBoardDimensions(this); // new Dimension(9, 9); // 
			if (boardSize == null) return; // if user canceled
			setGardenMode(GardenMode.EDIT);
		}

		/** Don't let the solver thread or visualizer
		 * access the board or solver while they're null.
		 */
		synchronized(this) {
			solver.threadSuspended = true;
			solver.stopMode = NuriSolver.StopMode.EXIT;
			vis.noLoop();
	    	solver = null;
	    	vis.setSolver(null);
	    	if (isNew) {
	    		board = new NuriBoard(boardSize.height, boardSize.width);
	    	} else {
	    		board = board.resetCopy();
	    	}
			solver = new NuriSolver(board, false, vis);
			vis.setSolver(solver);
			vis.puz = board;
			vis.redraw();
	    	solveButton.setEnabled(true);
		}
		statusLabel.setText("Board cleared.");
		depthLabel.setText("");
	}

	private void setGardenMode(GardenMode mode) {
		gardenMode = mode;
		solveButton.setEnabled(mode == GardenMode.EDIT);
	}

	public static void main(String[] args) {
		NuriFrame nf = new NuriFrame();
		nf.init();
	}    
	
	/** Because we implement ComponentListener, we have to provide all these
	 * methods, even though we may only care about resize events at the moment.
	 * Actually these don't seem to be helping... the board still does not get redrawn
	 * when we hide-and-expose the app; and on initial run, the board is drawn too small. */
	public void componentHidden(ComponentEvent e) { }

    public void componentMoved(ComponentEvent e) { }

    public void componentResized(ComponentEvent e) {
    	if (vis != null) vis.redraw();    	
    }

    public void componentShown(ComponentEvent e) {
    	if (vis != null) vis.redraw();
	}


	void keyPressed(char key) {
		int newNumber = currentNumber;
		
		// System.out.println("Visualizer got key: " + key);
		/* First, set mode based on key categories: */
		switch(key) {
		case ' ':
		case 'p':
		case 'c':
		case 'C':
		case 'i':
		case 'I':
		case 'u':
		case 'U':
		case 'v':
		case 'V':
			setGardenMode(GardenMode.SOLVE);
			break;
		case KeyEvent.VK_ESCAPE:
			quit();
			return;
		case '+':
		case '=':
			setGardenMode(GardenMode.EDIT);
			break;
		default:
			if (Character.isDigit(key))
				setGardenMode(GardenMode.EDIT);
			break;
		}
		
		/* Now more specific actions for individual keys: */
		/**##FIXME: single-stepping is broken right now! */
		switch(key) {
		case ' ':
		case 'p':
			solver.stopMode = StopMode.CONTINUE;
			solver.threadSuspended = !solver.threadSuspended;
			break;
		case 's':
			solver.stopMode = StopMode.ONESTEP;
			solver.threadSuspended = false;
			break;
		case 'c':
		case 'C':
			solver.stopMode = StopMode.CONTINUE;
			solver.threadSuspended = false;
			break;
		case 'i':
		case 'I':
			solver.stopMode = StopMode.STEPIN;
			solver.setStepStopDepth(1);
			solver.threadSuspended = false;
			break;
		case 'u':
		case 'U':
			solver.stopMode = StopMode.STEPOUT;
			solver.setStepStopDepth(-1);
			solver.threadSuspended = false;
			break;
		case 'v':
		case 'V':
			solver.stopMode = StopMode.STEPOVER;
			solver.setStepStopDepth(0);
			solver.threadSuspended = false;
			break;
		case '-':
			newNumber--;
			break;
		case '+':
		case '=':
			newNumber++;
			break;

		default:
			if (Character.isDigit(key)) {
				if (key == '0')
					newNumber *= 10;
				else
					newNumber = NuriBoard.numberValue(key);
			} else {
				System.out.println("Unrecognized key: " + key);
				return;
			}
		}

		if (gardenMode == GardenMode.SOLVE) {
			solver.maybeStart(); // start if not yet started
	
			if (!solver.threadSuspended) {
				synchronized(this) {
		            this.notifyAll();					
				}
			}
		} else {
			if (newNumber < 1) newNumber = 1;
			else if (newNumber > NuriBoard.maxCellValue)
				newNumber = NuriBoard.maxCellValue; 
	
			/** TODO: also change last edited cell's number, if any
			if (newNumber != currentNumber &&
					lastChangedCell != null &&
					board.isANumber(board.get(lastChangedCell)))
					placeNumber(lastChangedCell.r, lastChangedCell.c,
						board.NUMBERS.charAt(newNumber));
			*/
			
			currentNumber = newNumber;
			/** TODO: display currentNumber on a status label somewhere. */
		}
	}

	/** Check for unsaved data and confirm if necessary; then close frame. */
    private void quit() {
    	//TODO: check for unsaved data
    	System.exit(1); //TODO: exit frame more cleanly
	}

	/** Handle mouse click on visualizer at given cell. Mode-dependent. */
	public void clickedCell(int r, int c, boolean isLeft) {
		if (gardenMode == GardenMode.SOLVE) {
			toggleCellState(r, c, isLeft);
		} else {
			placeNumber(r, c, currentNumber);
		}
	}
	
	/** Place new number on board.
	 * ##TODO: update other state accordingly. Maybe that's done?
	 */
	private void placeNumber(int r, int c, int n) {
		// Assume solver is not running. Otherwise we would not be in edit mode, and
		// couldn't get here.
		assert(!solver.isAlive());
		board.initializeCell(r, c, NuriBoard.NUMBERS.charAt(n-1));
		board.setGuessLevel(r, c, (short)0);
		/** TODO: if two numbers are adjacent, mark them red or something. */ 
		board.prepareStats(false);
		//DONE: reset solver completely.
		solver = new NuriSolver(board, false, vis);
		vis.setSolver(solver);
		vis.redraw();
	}

	private void toggleCellState(int r, int c, boolean isLeft) {
		if (solver.isAlive()) {
			// TODO: beep or flash or something
		} else {
			board.initializeCell(r, c,
					isLeft ? NuriBoard.TOGGLEFWD : NuriBoard.TOGGLEBWD);
			board.setGuessLevel(r, c, board.searchDepth);
			// update regions accordingly
			// TODO: could optimize the following; it's kind of overkill.
			board.prepareStats(false);
			vis.redraw();
		}
	}

}
