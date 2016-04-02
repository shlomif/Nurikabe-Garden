How to build/run NurikabeGarden from source in Eclipse:

Install Eclipse & Proclipse
http://code.google.com/p/proclipsing/wiki/GettingStarted

Install Subclipse
http://subclipse.tigris.org/servlets/ProjectProcess?pageID=p4wYuA

Go to SVN Repositories perspective.
- Add repository; URL: https://nurikabe-maker.svn.sourceforge.net/svnroot/nurikabe-maker
- Expand that repository to show trunk, branches etc.
- Right-click on 'trunk' folder; Checkout
 - as a project in the workspace; project name: NurikabeGarden

Switch to Java (dev) perspective
In package explorer, open src/net.huttar.nuriGarden/NuriFrame.java
  Edit init() to load an existing file in samples/*:
  		parser = new NuriParser("samples/huttar_ts.txt");
		board = parser.loadFile(182); // index of puzzle
  Press F11 to build and run.


Using Nurikabe Garden:
Click Solve to solve the puzzle.
  While solving, press 'p' to pause, 's' to go one step at a time, 'c' to continue. (broken at the moment!)
Click Reset to clear solving attempts, leaving only the numbers.
Click on a square in the puzzle to change its state between white, black, and unknown (gray).
Click New (or press Ctrl+N) to create a new puzzle.
