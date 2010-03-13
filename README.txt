How to build/run NurikabeGarden from source in Eclipse:

Install Eclipse & Proclipse
http://code.google.com/p/proclipsing/wiki/GettingStarted

Install Subclipse
http://subclipse.tigris.org/servlets/ProjectProcess?pageID=p4wYuA

Go to SVN Repositories perspective.
- Add repository; URL: https://nurikabe-maker.svn.sourceforge.net/svnroot/nurikabe-maker
- Right-click on 'trunk' folder; Checkout
 - as a project in the workspace; project name: NurikabeGarden

Switch to Java (dev) perspective
In package explorer, open src/nurikabeVisualizer/NurikabeVisualizer.java
  Edit setup to load an existing file in samples/*; save
  Press F11 to build and run.

