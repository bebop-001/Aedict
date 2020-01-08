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
