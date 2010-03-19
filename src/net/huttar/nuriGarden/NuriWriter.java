package net.huttar.nuriGarden;

import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JOptionPane;

/** Class to save a Nurikabe puzzle board state to a file. */
class NuriWriter {

	/** Overwrite is already confirmed when we get here. */
	public static void saveToFile(Component parent, NuriBoard board, File file) {
		// If file exists, we need to overwrite it.
		// not necessary I think: if (file.exists()) file.delete();
		FileWriter fw = null;
		try {
			fw = new FileWriter(file);
			fw.write(board.puzzle());
			//DONE: make a version of toString that omits black/white,
			// only including numbers. Maybe write the full version as the "solution"
			// (which may be incomplete).
			//TODO: tell user the save was successful?
		} catch (IOException e) {
			JOptionPane.showMessageDialog(parent,
					"Unable to save file:\n" + e.getMessage(),
					"Save failed",
					JOptionPane.ERROR_MESSAGE);
		}

		if (fw != null) {
			try {
				fw.close();
			} catch (IOException e) {
			}
		}
	}

}
