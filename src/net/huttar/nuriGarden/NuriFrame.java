package net.huttar.nuriGarden;

import java.awt.Frame;
import java.awt.BorderLayout;
import java.awt.Label;


/** 
 * Frame for Nurikabe Garden. Using AWT because Processing (NuriVisualizer)
 * uses AWT, and I don't think we need anything more (Swing, AWT) for now.
 * 
 * @author huttarl
 *
 */
public class NuriFrame extends Frame {

	private static final long serialVersionUID = 1L;

	public NuriFrame() {
        super("Nurikabe garden");
    }

	/** The parser object used to read in files. */
	private NuriParser parser = null; //B
	private NuriSolver solver = null;
	private NuriState board = null;
	private NuriVisualizer vis = null;
	private Label statusLabel = null;
	
	private void init() {
        setLayout(new BorderLayout());

        Label titleLabel = new Label("Nurikabe Garden");
        add(titleLabel, BorderLayout.NORTH);

        statusLabel = new Label("Status...");
        add(statusLabel, BorderLayout.SOUTH);

        // For embedding Processing applet see
        // http://dev.processing.org/reference/core/javadoc/processing/core/PApplet.html
        vis = new NuriVisualizer();
        vis.frame = this;
        add(vis, BorderLayout.CENTER);
        
        // TODO: get frame to properly surround PApplet.
        setSize(600, 500);
        setResizable(true);
        // pack();

        // TODO: parameterize; or get from a File Open dlg
		parser = new NuriParser("samples/janko_ts.txt");
		board = parser.loadFile(182);

		solver = new NuriSolver(board, true, vis);
		// board.setSolver(solver);
		vis.setSolver(solver);
		
        setVisible(true);
        
        // important to call this whenever embedding a PApplet.
        // It ensures that the animation thread is started and
        // that other internal variables are properly set.
        vis.init();
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
	
	public static void main(String[] args) {
		NuriFrame nf = new NuriFrame();
		nf.init();
	}
    
}
