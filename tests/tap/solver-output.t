#!/usr/bin/perl

use strict;
use warnings;

use Test::More tests => 1;

use Test::Trap qw( trap $trap
    :flow:stderr(systemsafe):stdout(systemsafe):warn
);

use Cwd qw(getcwd);
use File::Spec;
use Env::Path;

my $path = Env::Path->CLASSPATH;
$path->Append(File::Spec->catdir(getcwd(), "bin"));

{
    trap {
        system("java", "net.huttar.nuriGarden.NuriSolver",
            File::Spec->catfile(File::Spec->curdir(), 'samples', 'tiny1.puz')
        );
    };

    my $stdout = $trap->stdout();

    # TEST
    like (
        $stdout,
        qr/^Final solution is:\n1 # 1\s*\n# # #\s*1 # 1\s*\n/ms,
        "Outputting the final solution."
    );
}
