#!/usr/bin/perl -w
use strict;
use warnings;
chomp(my $branch = `git rev-parse --abbrev-ref HEAD`);
chomp(my @log = `git log $branch`);
my $date = (grep /^Date:/, @log)[0];
print(($date =~ m{^Date:\s+(.*)$})[0], "\n");

