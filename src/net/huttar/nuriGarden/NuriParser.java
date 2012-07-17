package net.huttar.nuriGarden;

/**
 *
 */

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.regex.Pattern;


/**
 * NuriParser: load a nurikabe puzzle from a file.
 * Note that one parser <---> one file; you will be able to
 * load multiple puzzles from the same file by reusing the same
 * parser.
 *
 * @author Lars Huttar, lars at huttar dot net
 *
 */
class NuriParser {
	private String filename = null;

	NuriParser(String fn) {
		filename = fn;
	}

	NuriBoard loadFile(int puzzleIndex) {
		NuriBoard puz = new NuriBoard();
		if (filename.endsWith(".puz"))
			loadPuz(puz, filename);
		else
			loadTxt(puz, filename, puzzleIndex);

		return puz;
	}

	void loadPuz(NuriBoard puz, String filename) {
		try {
			Scanner in = new Scanner(new FileInputStream(filename));
			ArrayList<String> lines = new ArrayList<String>();
			while (in.hasNextLine()) {
				lines.add(in.nextLine());
			}
			int rows = lines.size();
			int columns = lines.get(0).length();
			// grid = new char[lines.size()][lines.get(0).length()];
			puz.newGrid(rows, columns);
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < columns; c++) {
					char symbol = lines.get(r).charAt(c);
					// grid[r][c] = (symbol == WHITE) ? UNKNOWN : symbol;
					puz.initializeCell(r, c,
							(symbol == NuriBoard.WHITE || symbol == '-') ? NuriBoard.UNKNOWN : symbol);
				}
			}
			puz.prepareStats(false);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	/*
		 * TODO: make parser work when solution is absent
		 * DONE: make parser expect ID
		 * TODO: make parser able to select puzzle from a file by ID
		 */

		/** Load i-th puzzle from filename (1-based indexing). */
		void loadTxt(NuriBoard puz, String filename, int puzzleIndex) {
			try {
				Scanner in = new Scanner(new FileInputStream(filename));
				// ArrayList<String> lines = new ArrayList<String>();
				// while (in.hasNextLine()) {
				// 	lines.add(in.nextLine());
				// }

				/** Code for parsing the earlier format:
				 9,9,"1x2=4,2x3=2,6x4=4,1x5=3,9x5=5,4x6=7,8x7=5,9x8=3" # lgo puzzle 1 */
	//			String line;
	//			// Skip any comments or blank lines.
	//			do {
	//				line = in.nextLine();
	//			} while (line.startsWith(";") || line.length() == 0);
	//
	//			int c1 = line.indexOf(','), c2 = line.indexOf(',', c1 + 1);
	//			int columns = Integer.parseInt(line.substring(0, c1));
	//			int rows = Integer.parseInt(line.substring(c1 + 1, c2));
	//
	//			grid = new char[rows][columns];
	//			int c, r;
	//			// initialize all cells to Unknown
	//			for (c = 0; c < columns; c++)
	//				for (r = 0; r < rows; r++)
	//					grid[r][c] = UNKNOWN;
	//
	//			// All this splitting probably leads to excessive GC. ##TODO: optimize
	//			String seedString = line.substring(line.indexOf('"') + 1, line.lastIndexOf('"'));
	//			String[] seeds = seedString.split(",");
	//
	//			for (int i = 0; i < seeds.length; i++) {
	//				String[] coords = seeds[i].split("[x=]");
	//				c = Integer.parseInt(coords[0]) - 1;
	//				r = Integer.parseInt(coords[1]) - 1;
	//				grid[r][c] = NUMBERS.charAt(Integer.parseInt(coords[2])-1);
	//			}

				// Read this format (fake example):
				// "lgo1",3,3,"-4-------",".4###..#." ; lgo puzzle 1
				// i.e. "id",width,height,"puzzle","solution" ; comment
				// where '-' is 'unknown', '.' is 'white', and '#' is 'black'.

				Pattern rePuzData = Pattern.compile("\"([^\"]+)\",(\\d+),(\\d+),\"([^\"]+)\"(,\"([^\"]+)\")?(\\s*;(.*))?");
				String tmp;

				for (int curPuz = 1; curPuz <= puzzleIndex; curPuz++) {
					do {
						tmp = in.findInLine(rePuzData);
						// skip blank or comment-only lines
						if (tmp == null) in.nextLine();
					} while (tmp == null);
					// Now, we have advanced to the next (or first) real puzzle line.
				}

				java.util.regex.MatchResult mr = in.match();
				String id = mr.group(1);
				short columns = Short.parseShort(mr.group(2));
				short rows = Short.parseShort(mr.group(3));
				// System.out.println("ID: " + id + "; columns: " + columns + "; rows: " + rows);

				String puzzle = mr.group(4);
				@SuppressWarnings("unused")
				String solution = (mr.groupCount() >= 6) ? mr.group(6) : null;
				@SuppressWarnings("unused")
				String comment = (mr.groupCount() >= 8) ? mr.group(8) : null;

				// System.out.println("Puzzle: " + puzzle+ "\nSolution: " + solution + "\nComment: " + comment);

				puz.newGrid(rows, columns);

				for (int r = 0; r < rows; r++) {
					for (int c = 0; c < columns; c++) {
						char ch = puzzle.charAt(r * columns + c);
						if (ch == '-' || ch == '?') {
							puz.initializeCell(r, c, NuriBoard.UNKNOWN);
						} else if (ch == NuriBoard.WHITE || ch == NuriBoard.BLACK ||
								NuriBoard.isANumber(ch)) {
							puz.initializeCell(r, c, ch);
						} else {
							throw new IOException("Unrecognized character " + ch + " in puzzle " + id);
						}
					}
				}

				puz.prepareStats(false);

			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("Current dir: " + System.getProperty("user.dir"));
				System.exit(1);
			}
		}

}
