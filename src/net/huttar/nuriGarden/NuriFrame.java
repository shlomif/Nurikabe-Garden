package net.huttar.nuriGarden;

// Using AWT:
//import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.Container;
// import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyEvent;
// import java.awt.Insets;
//import java.awt.Label;
// Using Swing:
// import java.awt.GridLayout;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
// import javax.swing.JPanel;
import javax.swing.BorderFactory;
// import javax.swing.border.Border;
// import javax.swing.border.EmptyBorder;
import javax.swing.JMenuBar;

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

	public NuriFrame() {
        super("Nurikabe Garden");
    }

	/** The parser object used to read in files. */
	private NuriParser parser = null; //B
	private NuriSolver solver = null;
	private NuriState board = null;
	private NuriVisualizer vis = null;

	private JLabel statusLabel, depthLabel;
	
	private JButton solveButton;
	
	private void init() {
		// helpful: see http://zetcode.com/tutorials/javaswingtutorial/swinglayoutmanagement/
		Container panel = this.getContentPane();

		JMenuBar menu = new JMenuBar();
		panel.add(menu, BorderLayout.NORTH);
		
		Box buttonPanel = new Box(BoxLayout.Y_AXIS);
		// BoxLayout boxLayout = new BoxLayout(buttonPanel, BoxLayout.Y_AXIS);
		// buttonPanel.setLayout(boxLayout);
		panel.add(buttonPanel, BorderLayout.EAST);
		buttonPanel.setBorder(BorderFactory.createEtchedBorder());
		
		solveButton = new JButton("Solve");
		solveButton.setMnemonic(KeyEvent.VK_S);
		solveButton.setToolTipText("Attempt to solve the puzzle uniquely");
		solveButton.setActionCommand("solve");
		solveButton.addActionListener(this);
		buttonPanel.add(solveButton);
		
		// space between buttons
		buttonPanel.add(Box.createVerticalStrut(6));

		JButton redrawButton = new JButton("Redraw");
		redrawButton.setMnemonic(KeyEvent.VK_D);
		redrawButton.setToolTipText("Redraw the puzzle board");
		redrawButton.setActionCommand("redraw");
		redrawButton.addActionListener(this);
		buttonPanel.add(redrawButton);
		
		// space between buttons
		buttonPanel.add(Box.createVerticalStrut(6));

		JButton resetButton = new JButton("Reset");
		resetButton.setMnemonic(KeyEvent.VK_R);
		resetButton.setToolTipText("Clear the puzzle back to numbers only");
		resetButton.setActionCommand("reset");
		resetButton.addActionListener(this);
		buttonPanel.add(resetButton);
		
		// space between buttons
		buttonPanel.add(Box.createVerticalStrut(6));

		JButton quitButton = new JButton("Exit");
		quitButton.setMnemonic(KeyEvent.VK_X);
		quitButton.setToolTipText("Exit Nurikabe Garden");
		quitButton.setActionCommand("quit"); // System.exit(1);
		quitButton.addActionListener(this);
		buttonPanel.add(quitButton);

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
        // pack();

        // TODO: parameterize; or get from a File Open dlg
		parser = new NuriParser("samples/huttar_ts.txt");
		board = parser.loadFile(9); // Janko 25: two-digit
		
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
        if ("solve".equals(e.getActionCommand())) {
        	solver.maybeStart();
        	solveButton.setEnabled(false);
        	vis.loop();
        } else if ("redraw".equals(e.getActionCommand())) {
        	vis.redraw();
        } else if ("quit".equals(e.getActionCommand())) {
        	System.exit(1);
        } else if ("reset".equals(e.getActionCommand())) {
        	/** Don't let the solver thread or visualizer
        	 * access the board or solver while they're null.
        	 */
        	synchronized(vis) {
        		solver.threadSuspended = true;
        		solver.stopMode = NuriSolver.StopMode.EXIT;
        		vis.noLoop();
	        	solver = null;
	        	vis.setSolver(null);
	        	board = board.resetCopy();
	    		solver = new NuriSolver(board, false, vis);
	    		vis.setSolver(solver);
	    		vis.puz = board;
	    		vis.redraw();
	        	solveButton.setEnabled(true);
        	}
    		statusLabel.setText("Board cleared.");
    		depthLabel.setText("");
        } else {
        	// assert(false); // unrecognized action event
        }
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
}
