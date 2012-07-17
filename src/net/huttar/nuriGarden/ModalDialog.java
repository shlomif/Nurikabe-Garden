package net.huttar.nuriGarden;

import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.util.StringTokenizer;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

/** Small modal dialogs, such as for collecting dimensions for new puzzles.
 * Type of dialog is specified by type string passed to constructor.
 * @author huttarl
 *
 */
public class ModalDialog extends JDialog {
	private static final long serialVersionUID = 1L;
	private static JFileChooser fileChooser = null;

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
		if (result != null && (result.width <= 0 || result.height <= 0)) {
	        JOptionPane.showMessageDialog(parent,
	    			"The width and height of the new board must each be greater than zero.",
	    			"Invalid Dimensions", JOptionPane.ERROR_MESSAGE);
	        return null;
		}
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
    			"Could not parse the specified dimensions.\nPlease use WxH format, e.g. 11x9.",
    			"Invalid Dimensions", JOptionPane.ERROR_MESSAGE);
        return null;
    }

    /** Run a fileChooser save dialog, confirm overwrite if applicable, and return File
     * if successful. */
	public static File getSaveFile(NuriFrame nuriFrame) {
		if (fileChooser == null)
			fileChooser = new JFileChooser();
		int result = fileChooser.showSaveDialog(nuriFrame);
		if (result == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (file.exists()) {
				// confirm overwrite
				if (JOptionPane.showConfirmDialog(nuriFrame,
						"Overwrite existing file?",
						"Confirm Overwrite",
						JOptionPane.OK_CANCEL_OPTION,
						JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION) {
					return file;
				}
				// else user canceled warning dialog.
				else return null;
			} else return file;
		}
		// else user canceled save file chooser.
		return null;
	}

    /** Run a fileChooser save dialog, confirm overwrite if applicable, and return File
     * if successful. */
	public static File getLoadFile(NuriFrame nuriFrame) {
		if (fileChooser == null)
			fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(nuriFrame);
		if (result == JFileChooser.APPROVE_OPTION) {
			File file = fileChooser.getSelectedFile();
			if (!file.exists()) {
		        JOptionPane.showMessageDialog(nuriFrame,
		    			"The file " + file.getAbsolutePath() + " does not exist.",
		    			"File not found", JOptionPane.ERROR_MESSAGE);
			} else return file;
		}
		// else user canceled the file chooser.
		return null;
	}
}
