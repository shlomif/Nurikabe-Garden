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
	private NurikabeVisualizer vis = null;
	
	private void init() {
        setLayout(new BorderLayout());
        // for embedding Processing see http://dev.processing.org/reference/core/javadoc/processing/core/PApplet.html
        vis = new NurikabeVisualizer();
        add(vis, BorderLayout.CENTER);
        
        Label label = new Label("Nurikabe Garden");
        add(label, BorderLayout.NORTH);

        // TODO: get frame to properly surround PApplet.
        setSize(600, 400);
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

	public static void main(String[] args) {
		NuriFrame nf = new NuriFrame();
		nf.init();
		

	}
    
}
