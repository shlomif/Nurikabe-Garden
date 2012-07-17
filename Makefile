# This makefile (useful with GNU make - not tested with other make
# implementations) is intended for people who cannot use Eclipse or do not
# want to.

JAVA_SRCS = \
	src/net/huttar/nuriGarden/ContradictionException.java \
	src/net/huttar/nuriGarden/Coords.java \
	src/net/huttar/nuriGarden/ModalDialog.java \
	src/net/huttar/nuriGarden/NuriBoard.java \
	src/net/huttar/nuriGarden/NuriCanvas.java \
	src/net/huttar/nuriGarden/NuriFrame.java \
	src/net/huttar/nuriGarden/NuriParser.java \
	src/net/huttar/nuriGarden/NuriSolver.java \
	src/net/huttar/nuriGarden/NurIterator.java \
	src/net/huttar/nuriGarden/NuriVisualizer.java \
	src/net/huttar/nuriGarden/NuriWriter.java \
	src/net/huttar/nuriGarden/Region.java \
	src/net/huttar/nuriGarden/UCRegion.java

JAVA_BINS = $(patsubst src/%.java,bin/%.class,$(JAVA_SRCS))

all: java_bins

java_bins: $(JAVA_BINS)

$(JAVA_BINS): bin/%.class: src/%.java
	javac -d bin $(JAVA_SRCS)

test: all
	prove tests/tap/*.t

runtest: all
	runprove tests/tap/*.t
