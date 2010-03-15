package net.huttar.nuriGarden;

// Using AWT:
//import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.Container;
// import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
public class NuriFrame extends JFrame implements ActionListener {

	private static final long serialVersionUID = 1L;

	public NuriFrame() {
        super("Nurikabe Garden");
    }

	/** The parser object used to read in files. */
	private NuriParser parser = null; //B
	private NuriSolver solver = null;
	private NuriState board = null;
	private NuriVisualizer vis = null;
	// AWT: private Label statusLabel = null;
	private JLabel statusLabel = null;
	
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
		
		JButton solveButton = new JButton("Solve");
		solveButton.setMnemonic(KeyEvent.VK_S);
		solveButton.setActionCommand("solve");
		solveButton.addActionListener(this);
		buttonPanel.add(solveButton);
		
		// space between buttons
		buttonPanel.add(Box.createVerticalStrut(6));

		JButton quitButton = new JButton("Quit");
		quitButton.setMnemonic(KeyEvent.VK_Q);
		quitButton.setActionCommand("quit"); // System.exit(1);
		quitButton.addActionListener(this);
		buttonPanel.add(quitButton);

        // panel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        // panel.setLayout(new GridLayout(5, 4, 5, 5));

        // AWT: setLayout(new BorderLayout());

        // JLabel titleLabel = new JLabel("<html><h1>Nurikabe Garden</h1></html>");
        // panel.add(titleLabel, BorderLayout.NORTH);

        statusLabel = new JLabel();
        panel.add(statusLabel, BorderLayout.SOUTH);
        
        // For embedding Processing applet see
        // http://dev.processing.org/reference/core/javadoc/processing/core/PApplet.html
        vis = new NuriVisualizer();
        vis.frame = this;
        // AWT: add(vis, BorderLayout.CENTER);
        panel.add(vis);
        
        // TODO: get frame to properly surround PApplet.
        setSize(700, 500);
        setResizable(true);
        // pack();

        // TODO: parameterize; or get from a File Open dlg
		parser = new NuriParser("samples/janko_ts.txt");
		board = parser.loadFile(182);
		
		solver = new NuriSolver(board, true, vis);
		// board.setSolver(solver);
		vis.setSolver(solver);

		//panel.setBorder(new EmptyBorder(new Insets(20, 20, 20, 20)));

		// add(panel);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setLocationRelativeTo(null);

        setVisible(true);
        
        // important to call this whenever embedding a PApplet.
        // It ensures that the animation thread is started and
        // that other internal variables are properly set.
        vis.init();
		// vis.setSizeToBoard(board); // no effect
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
	}
	
   public void actionPerformed(ActionEvent e) {
        if ("solve".equals(e.getActionCommand())) {
        } else if ("quit".equals(e.getActionCommand())) {
        	System.exit(1);
        }
    }

	public static void main(String[] args) {
		NuriFrame nf = new NuriFrame();
		nf.init();
	}
    
}
