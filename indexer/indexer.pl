#!/usr/bin/perl -w
$|++;
use strict;
use warnings;
use Cwd qw(abs_path getcwd);
my ($funcDir, $funcName) = (abs_path($0) =~ m{^(.*)/(.*)});
my $baseJarName = 'indexer.jar';
my $jarFile = "$funcDir/$baseJarName";
my $USAGE = "$funcName [--jar] <indexer args>
    '--jar' causes creation of an executable jar file called
            $jarFile.
    Without the --jar arg, we call java with the jar files we think are
    needed.

    To see the indexer args, call this script with '-?'.
";
die $USAGE, "No arguments\n" unless @ARGV;

# files go in same directory as script.
{
    package Dirs;
    use Cwd qw(abs_path getcwd);
    my $dirs = [];
    sub new {
        return bless $dirs = [abs_path(getcwd())], $_[0];
    }
    sub push {
        # absolute or relative path.
        my $dest = ($_[1] =~ m{^/})
            ? $_[1]
            : "$dirs->[-1]/$_[1]";
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
    sub up {
        return $dirs->[-1];
    }
    sub DESTROY {
        chdir($dirs->[0]) if @$dirs;
    }
};

my $dirs = Dirs->new();

$dirs->push($funcDir);

my $entryPoint = 'sk_x.baka.aedict.indexer.Main';
my @jars = qw (
/home/sjs/dev/mvysny/Aedict/indexer/app/build/intermediates/javac/debug/classes
/home/sjs/.gradle/caches/modules-2/files-2.1/commons-cli/commons-cli/1.4/c51c00206bb913cd8612b24abd9fa98ae89719b1/commons-cli-1.4.jar
/home/sjs/.gradle/caches/modules-2/files-2.1/commons-io/commons-io/2.5/2852e6e05fbb95076fc091f6d1780f1f8fe35e0f/commons-io-2.5.jar
/home/sjs/.gradle/caches/modules-2/files-2.1/org.apache.lucene/lucene-core/3.6.2/9ec77e2507f9cc01756964c71d91efd8154a8c47/lucene-core-3.6.2.jar
/home/sjs/.gradle/caches/modules-2/files-2.1/org.apache.commons/commons-compress/1.19/7e65777fb451ddab6a9c054beb879e521b7eab78/commons-compress-1.19.jar
);

if (grep $_ eq '--jar', @ARGV) {
    my $tmpDir = "/tmp/$$.$funcName.tmp";
    mkdir $tmpDir;
    $dirs->push($tmpDir);

    @ARGV = grep $_  ne '--jar', @ARGV;
    for my $jar (@jars) {
        if (-d $jar) {
            $dirs->push($jar);
            chomp(my @classes
                = `/usr/bin/find . -name \*.class | grep -v android`);
            my $tarFile = "/tmp/$$.classes.tar";
            if (@classes) {
                my @cmd = ('/bin/tar', 'cf', $tarFile, @classes);
                system @cmd;
            }
            $dirs->pop();
            system('/bin/tar', 'xf', $tarFile);
            unlink $tarFile;
        }
        else {
            `jar -xf $jar`;
        }
    }

    $dirs->push($tmpDir);
    `jar -cef $entryPoint $jarFile .`;
    $dirs->pop();
    `/bin/rm -rf $tmpDir`;
    print `java -jar $baseJarName @ARGV`;
    $dirs->pop();
}
else {
    my $classPath = join(':', @jars);
    print `java -classpath $classPath $entryPoint  @ARGV`;
}

$dirs->pop();
