#!/usr/bin/perl -w
use strict;
use warnings;
use Cwd qw(abs_path getcwd);

my ($funcDir, $baseDir) = (abs_path($0) =~ m{^(.*)/(.*)});

# files go in same directory as script.
chdir($funcDir) unless (getcwd() eq $funcDir);
{
    package Dirs;
    use Cwd qw(abs_path getcwd);
    my $dirs = [];
    sub new {
        return bless $dirs = [abs_path(getcwd())], $_[0];
    }
    sub push {
        my $dest = "$dirs->[-1]/$_[1]";
        die "Dirs::push: Not a directory $dest\n" unless -d $dest;
        chdir($dest);
        push @$dirs, abs_path(getcwd());
    }
    sub pop {
        pop @$dirs;
        die "Dirs::pop: stack empty\n" unless @$dirs;
        die "Dirs::pop: not a directory: $dirs->[-1]" unless -d $dirs->[-1];
        chdir(pop(@$dirs));
    }
    sub DESTROY {
        chdir($dirs->[0]) if @$dirs;
    }
};

my $dirs = new Dirs();

sub getEdict {
    my $baseUrl = "ftp://ftp.edrdg.org/pub/Nihongo";
    my $file = "edict";
    my $downloadFile = "$baseUrl/$file.gz";
    print "Fetch $downloadFile";
    system("/usr/bin/wget", '-O', "$file.EUC-JP.gz", $downloadFile) &&
        die "wget $downloadFile FAILED:$!\n"; 
    system("/bin/gunzip", '-f', "$file.EUC-JP.gz") &&
        die "gunzip $file FAILED:$!\n"; 
    system(qw(/usr/bin/iconv -f EUC-JP -t UTF-8 -o),
            $file, "$file.EUC-JP") &&
        die "iconv $file.EUC-JP FAILED:$!\n"; 
    print "\n";
}

sub getKanjiDic {
    my $baseUrl = "ftp://ftp.edrdg.org/pub/Nihongo";
    my $file = "kanjidic.gz";
    my $downloadFile = "$baseUrl/$file";
    print "Fetch $downloadFile";
    system("/usr/bin/wget $downloadFile") &&
        die "wget $downloadFile FAILED:$!\n"; 
    system("/bin/gunzip", '-f', $file) &&
        die "gunzip $file FAILED:$!\n"; 
    print "\n";
}
sub getSod {
    my $baseUrl = "ftp://ftp.edrdg.org/pub/Nihongo";
    my $file = "sod-utf8.tar.gz";
    my $tarFile = ($file =~ m{^(.*).gz$})[0];
    my $downloadFile = "$baseUrl/$file";
    print "Fetch $downloadFile";
    system("/usr/bin/wget $downloadFile") &&
        die "wget $downloadFile FAILED:$!\n"; 
    system("/bin/gunzip", '-f', $file) &&
        die "gunzip $file FAILED:$!\n"; 
    system("/bin/tar", 'xvfp', $tarFile) &&
        die "untar of $tarFile FAILED:$!\n"; 
    print "\n";
}
sub parseTatoeba{
    my $linksFile = 'links.csv';
    my $linksParsedFile = 'links.parsed.csv';

    my $sentencesFile = 'sentences.csv';
    my $sentencesParsedFile = 'sentences.parsed.csv';

    my $jpnIndicesFile = 'jpn_indices.csv';
    my $jpnIndicesParsedFile = 'jpn_indices.parsed.csv';

    my %sentences = ();
    my %jpnIndices = ();
    my %jpnLinks = ();
    my %engLinks = ();

    open(F, $sentencesFile) || die "Open $sentencesFile for read FAILED:$!";
    my $lc = 0;
    foreach (<F>) {
        chomp();
        $lc++;
        my ($id, $lang, $sentence) = m{^(\d+)\s+(\S+)\s+(.*)};
        unless (defined $lang) {
            print "$lc: <$_>\n";
            next;
        }
        next unless $lang eq 'eng' || $lang eq 'jpn';
        $sentences{$id}->{lang} = $lang;
        $sentences{$id}->{sentence} = $sentence;
    }
    close F;
    open(F, $jpnIndicesFile) || die "Open $jpnIndicesFile for read FAILED:$!";
    $lc = 0;
    my @badXlatId = ();
    my @badValue = ();
    foreach (<F>) {
        chomp;
        my ($id, $xlatId, $value) = m{^(\d+)\s+(\S+)\s+(.*)$};
        # There's something going on here I don't understand.
        # As I read the docs, this should be '\d+ \d+ .*' but
        # sometimes the second \d+ (meaning id) is something else (I-1)
        # and I can't find it.
        next if $xlatId == -1;
        unless (defined $id && defined  $xlatId && defined $value) {
            push @badXlatId, $id unless defined $xlatId;
            push @badValue, $id unless defined $value;
            print(".");
        }
        # ignore anything for which there isn't a sentence.
        if (defined $sentences{$id}) {
            $jpnIndices{$id}{xlat} = $xlatId;
            $jpnIndices{$id}{value} = $value;
        }
    }
    close F;
    if (@badXlatId || @badValue) {
        print STDERR "jpn_indice incostencies:\n";
        if (@badXlatId) {
            print STDERR "Valuation with translation sentence not found:\n",
                join(', ', @badXlatId);
        }
        if (@badValue) {
            print STDERR "sentence value not found:\n",
                join(', ', @badValue);
        }
    }


    $lc = 0;
    open(F, "links.csv") || die "Failed to open links.csv";
    open(F, $linksFile) || die "Open $linksFile for read FAILED:$!";
    foreach (<F>) {
        my ($to, $from) = m{^(\d+)\s+(\d+)$};
if($to == 1207 || $from == 1207) {
    print "$to:$from\n";
}
        if (exists $sentences{$to} && exists $sentences{$from}) {
            if ($sentences{$to}{lang} eq 'eng') {
                push @{$engLinks{$to}}, $from;
            }
            else {
                push @{$engLinks{$to}}, $from;
            }
        }
    }
    exit();

    sub printSentence {
        my ($FH, $idx) = @_;
        printf($FH "%d\t%s\t%s\n",
            $idx, $sentences{$idx}{lang}, $sentences{$idx}{sentence}
        );
    }
    # This is going to run on android so I have limited space.  I
    # decided to limit the links to the first english->jap translation
    # I encounter -- at least as a first cut.
    # first element is index to english sentence, second is link to japenese.
    my (@links, %jpnIndicesUsed);
    for my $to (sort {$a <=> $b} keys %engLinks) {
        my @from = @{$engLinks{$to}}; 
        my @jp = grep defined $jpnIndices{$_}, @from;

        # from is first value in jpn_indexes if it exists,
        # otherwise it's first japanese sentence for this eng. sentence.
        my $from = (@jp)
            ? $jp[0]
            : @from
                ? $from[0]
                : die "No from for id $to.\n";
        $jpnIndicesUsed{$jp[0]}++ if @jp > 0;
        push @links, [$to, $from];
    }
    open(F1, '>', $linksParsedFile)
        || die "Open $linksParsedFile for output FAILED:$!\n";
    open(F2, '>', $sentencesParsedFile)
        || die "Open $sentencesParsedFile for output FAILED:$!\n";
    open(F3, '>', $jpnIndicesParsedFile)
        || die "Open $jpnIndicesParsedFile for output FAILED:$!\n";
    for my $ref (@links) {
        my ($to, $from) = @$ref;
        printf(F1 "%d\t%d\n", $to, $from);
        printSentence(*F2, $to);
        printSentence(*F2, $from);
        if ($jpnIndicesUsed{$to}) {
            my $ref = $jpnIndices{$to};
            printf(F3 "%d\t%d\t%s\n", $to, $from, $ref->{value})
        }
    }
    close F1;
    close F2;
    close F3;
}
sub getTatoeba {
    my $baseUrl = "http://downloads.tatoeba.org/exports";
    my @files = qw(sentences.tar.bz2 links.tar.bz2 jpn_indices.tar.bz2);
    my $destDir = 'tatoeba';
    -d $destDir
        || mkdir $destDir
        || die "Failed to create directory $destDir:$!\n";
    $dirs->push($destDir);
#    unlink @files;
#    for my $file (@files) {
#        my $downloadFile = "$baseUrl/$file";
#        print "Fetch $downloadFile";
#        system('/usr/bin/wget', $downloadFile)
#            && die "wget $downloadFile FAILED:$!\n";
#        system('/bin/tar', '--bzip2', '-xvf', $file)
#            && die "untar $file File FAILED:$!\n";
#        print "\n";
#    }
    parseTatoeba();
    $dirs->pop();
}


if (@ARGV == 0)  {die "Usage: $0 [-e -T -k -s ]\n";}
for my $arg (@ARGV) {
    if   ($arg eq '-e') { getEdict();       }
    elsif ($arg eq '-k') { getKanjiDic();    }
    elsif ($arg eq '-T') { getTatoeba();     }
    elsif ($arg eq '-s') { getSod();         }
    else  { die "Unrecognized argument: $arg\n";}
}

print "Complete: pwd = ", getcwd(), "\n";
