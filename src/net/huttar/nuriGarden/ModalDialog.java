package net.huttar.nuriGarden;

import java.awt.Component;
import java.awt.Dimension;
import java.util.StringTokenizer;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/** Small modal dialogs, such as for collecting dimensions for new puzzles.
 * Type of dialog is specified by type string passed to constructor. 
 * @author huttarl
 *
 */
public class ModalDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	
	String type = null, title = null;
	// static ModalDialog dimensionsDialog = null;
	
//	ModalDialog(Component parent, String type, String title) {
//		this.type = type;
//		this.title = title;
//	}

	static Dimension getBoardDimensions(JFrame parent) {
/*		if (dimensionsDialog == null)
			dimensionsDialog = ModalDialog(parent, "Dimensions of New Puzzle", "dimensions");
		*/
		Dimension result = null;
		String inputValue = JOptionPane.showInputDialog(parent,
			"Please enter the dimensions of the new puzzle, in the format 10x8.",
			"Dimensions", JOptionPane.QUESTION_MESSAGE);
		result = parseDimension(inputValue, parent);
		return result;
	}

	/** parse a string "XxY" to produce a Dimension */
	// This is very loosely based on org.netbeans.modules.editor.lib.SettingsConversions:
	// http://www.google.com/codesearch/p?hl=en#38kK8Xdr328/editor.lib/src/org/netbeans/modules/editor/lib/SettingsConversions.java&q=java%20parseDimension&sa=N&cd=1&ct=rc
	// which is under either GPL2 or CDDL.
    public static Dimension parseDimension(String s, Component parent) {

    	if (s == null || s.isEmpty()) return null;
    	
    	StringTokenizer st = new StringTokenizer(s, "x");

        int w, h;

    	if (st.hasMoreElements()) {
            try {
        		w = Integer.parseInt(st.nextToken());
	        	if (st.hasMoreElements()) {
	        		h = Integer.parseInt(st.nextToken());
	            	return new Dimension(w, h);
	        	}
            } catch (NumberFormatException nfe) {
            }
    	}

        JOptionPane.showMessageDialog(parent,
    			"Could not parse specified dimensions.\nPlease use WxH format, e.g. 11x9.",
    			"Invalid Dimensions", JOptionPane.ERROR_MESSAGE);
        return null;
    }

}
