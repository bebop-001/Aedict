#!/usr/bin/perl -w
use utf8;
use strict;
use warnings;
use Cwd qw(abs_path getcwd);
$|++;

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

sub getTatoeba {
    my $baseUrl = "http://downloads.tatoeba.org/exports";
    my @files = qw(sentences.tar.bz2 links.tar.bz2 jpn_indices.tar.bz2);
    my $destDir = 'tatoeba';
    -d $destDir
        || mkdir $destDir
        || die "Failed to create directory $destDir:$!\n";
    $dirs->push($destDir);
    unlink @files;
    for my $file (@files) {
        my $downloadFile = "$baseUrl/$file";
        print "Fetch $downloadFile";
        system('/usr/bin/wget', $downloadFile)
            && die "wget $downloadFile FAILED:$!\n";
        system('/bin/tar', '--bzip2', '-xvf', $file)
            && die "untar $file File FAILED:$!\n";
        print "\n";
    }
    my $sentencesFile = "sentences.csv";
    my %jpnSentences = ();
    my %engSentences = ();
    my $i = 0;
    open(F, $sentencesFile) || die "open $sentencesFile for read FAILED:$!\n";
    binmode(F, ':utf8');
    foreach (<F>) {
        chomp;
        if ((my @matches = /^(\d+)\s+(jpn|eng)\s+(.*)$/) == 3) {
            printf("Sentences %5d\r", $i) if ((++$i % 5000) == 0);
            my ($id, $lang, $sentence) = @matches;
            if ($lang eq 'jpn') {
                $jpnSentences{$id} = $sentence;
            }
            else {
                $engSentences{$id} = $sentence;
            }
        }
    }
    printf("Sentences %5d\n", $i);
    close F;

    my $linksFile = "links.csv";
    my %links;
    $i = 0;
    open(F, $linksFile) || die "open $linksFile for read FAILED:$!\n";
    binmode(F, ':utf8');
    foreach (<F>) {
        chomp;
        if ((my @matches = /^(\d+)\s+(\d+)$/) == 2) {
            printf("Links %5d\r", $i) if ((++$i % 5000) == 0);
            my ($link1, $link2, $sentence) = @matches;
            if (defined $jpnSentences{$link1}
                    && defined $engSentences{$link2}) {
                push @{$links{$link1}}, $link2;
            }
        }
    }
    printf("Links %5d\n", $i);
    close F;

    my $indicesFile = "jpn_indices.csv";
    my %indices;
    $i = 0;
    open(F, $indicesFile) || die "open $indicesFile for read FAILED:$!\n";
    binmode(F, ':utf8');
    foreach (<F>) {
        chomp;
        if ((my @matches = /^(\d+)\s+(\d+)\s+(\S+.*)$/) == 3) {
            printf("Indices %5d\r", $i) if ((++$i % 5000) == 0);
            my ($jpn, $eng, $indice) = @matches;
            $indices{$jpn}{engLink} = $eng;
            $indices{$jpn}{indice} = $indice;
        }
    }
    printf("Indices %5d\n", $i);
    close F;

    for my $jpLink (sort {$a <=> $b} keys %links) {
        my @engLinks = @{$links{$jpLink}};
        if (defined $indices{$jpLink}) {
            my $ref = $indices{$jpLink};
            # if the sentence doesn't have a valid english translation
            # but one exists (i.e. link{jp}{eng}), substitute that.
            unless (defined $engSentences{$ref->{engLink}}) {
                $indices{$jpLink}{engLink} = $links{$jpLink}[0];
                $links{$jpLink} = [$links{$jpLink}[0]];
            }
        }
        # for simplicity and since everything is going into
        # asset memory, save only the first english sentence.
        if (@{$links{$jpLink}} > 1) {
            $links{$jpLink} = [$engLinks[0]];
        }
    }
    my $parsedSentences = "sentences.parsed.csv";
    open(my $sentencesOut, '>', $parsedSentences) 
        || die "open $parsedSentences for output FAILED:$!\n";
    binmode($sentencesOut, ':utf8');
    my $parsedLinks = "links.parsed.csv";
    open(my $linksOut, '>', $parsedLinks) 
        || die "open $parsedLinks for output FAILED:$!\n";
    binmode($linksOut, ':utf8');
    my $parsedIndices = "jpn_indices.parsed.csv";
    open(my $indicesOut, '>', $parsedIndices) 
        || die "open $parsedIndices for output FAILED:$!\n";
    binmode($indicesOut, ':utf8');
    my $fmt = "%d\t%s\t%s\n";
    my %englishPrinted = ();
    foreach my $jpLink (sort {$a <=> $b} keys %links) {
        my ($jp, $eng) = ($jpLink, $links{$jpLink}[0]);
        printf($sentencesOut $fmt, $jp, 'jpn', $jpnSentences{$jp});
        # Lots of english sentences are dups.  Only print first one.
        unless (defined $englishPrinted{$eng}) {
            printf($sentencesOut $fmt, $eng, 'eng', $engSentences{$eng});
        }
        $englishPrinted{$eng}++;
        printf($linksOut "%d\t%d\n", $jp, $eng);
    }
    foreach my $jp (sort {$a <=> $b} keys %indices) {
        printf($indicesOut "%d\t%d\t%s\n", 
            $jp, $indices{$jp}{engLink}, $indices{$jp}{indice});
    }
    close($sentencesOut); close $linksOut; close $indicesOut;
    printf("Found:\n\t%5d sentences\n\t%5d links\n\t%5d indices\n",
        scalar keys %jpnSentences, scalar keys %links, scalar keys %indices);
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
