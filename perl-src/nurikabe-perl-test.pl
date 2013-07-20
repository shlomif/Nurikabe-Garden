#!/usr/bin/perl

use strict;
use warnings;
use Cwd qw(getcwd);
use File::Spec;
use Env::Path;

BEGIN
{
    my $path = Env::Path->CLASSPATH;
    $path->Append(File::Spec->catdir(getcwd(), "bin"));
}

use Inline Java => <<'END' ;
    import net.huttar.nuriGarden.NuriSolver;
    import net.huttar.nuriGarden.NuriParser;
    import net.huttar.nuriGarden.NuriBoard;
    import java.util.ArrayList;

    class NuriSolver_Wrapper {
        private NuriBoard board;
        public NuriSolver_Wrapper(String [] lines) {
            board = new NuriBoard();

            NuriParser parser = new NuriParser("meaningless.puz");

            ArrayList<String> lines_array = new ArrayList<String>();
            for (String l : lines)
            {
                lines_array.add(l);
            }
            parser.loadPuzFromLines(board, lines_array);
        }
    }
END

my $obj = NuriSolver_Wrapper->new(
    [
        # samples/tiny3.puz
        split(/\n/, <<"EOF")
3...1
.....
.2...
...9.
.....
EOF
    ]
);
