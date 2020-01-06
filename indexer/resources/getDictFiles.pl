#!/usr/bin/perl -w
use strict;
use warnings;
use Cwd qw(abs_path getcwd);

my ($funcDir, $baseDir) = (abs_path($0) =~ m{^(.*)/(.*)});

# files go in same directory as script.
chdir($funcDir) unless (getcwd() eq $funcDir);

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
    my @files = qw(sentences.tar.bz2  links.tar.bz2 jpn_indices.tar.bz2);
    my $destDir = 'tatoeba';
    -d $destDir
        || mkdir $destDir
        || die "Failed to create directory $destDir:$!\n";
    for my $file (@files) {
        my $downloadFile = "$baseUrl/$file";
        print "Fetch $downloadFile";
        system('/usr/bin/wget', $downloadFile, '-O', "$destDir/$file")
            && die "wget $downloadFile FAILED:$!\n";
        system('/bin/tar', '--bzip2', '-xvf', "$destDir/$file")
            && die "untar $file File FAILED:$!\n";
        print "\n";
    }
}

getEdict();
getKanjiDic();
getTatoeba();
getSod();

