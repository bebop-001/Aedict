

Sat Jan  4 16:17:27 PST 2020

# About Aedict

This is another clone of
[Martin Vysny's Aedict 2](https://github.com/mvysny/aedict).
If you're looking for a Japanese Dictionary, this will work
but it was last updated in 2009.  Martin has Aedict3 on the 
app store.

Changes I've made to this copy:
* I made copies of the downloaded index files and put them
in the assets directory.  The dictionary installs those
during the install process.  I did this so you wouldn't
hit Martin's site with the downloads.  It means a much
larger download for the git directories and at install time
however.
* I split the indexer and the dictionary.  The indexer
  is a pure java app and the dictionary is an Android app.
  There is shared code but by splitting the app it's easier
  to understand what's going on.
* I removed the kana card from the app.
* Tanaka Corpus sentences are being removed.  From what I'm seeing on
  the web, they are obsolete.
* A new Kotlin file has been added and changes to the to
  gradle build files and a new dictionary/utils/gitCommitDate.pl
  script added and a menu option for getting info on the
  build version, etc.
* Bumped rev to 3.00.
* Bumped Lucene version to 3.6.2 -- latest on the 3 release stream.
* Changed the way dictionary files are downloaded.
    * a PERL script indexer/resources/getDictFiles.pl is responsible
      for downloading and pre-parsing of dictionary files.
    * Input files are no longer gzipped.
    * It is assumed that any input files will be found in the
      'resources' directory at the same level as the 'indexer'
      and 'dictionary' directorys.
    * output directories from tne indexer are placed in the assets
      directory at the same level as the 'indexer'.  The developer
      must copy them to the 'dictionary/app/src/main/assets/dictionary'
      directory to have them included in the dictionary build.
    * Command line args were changed on the 'indexer'.  It now only
      accepts '?', '-T', '-e', '-k' and '-s' options.
    * '-e' edict option seems to work.
    * '-T' seems to work.
* added a perl script for executing the indexer and for creating an 
  executable jar file.


Steve S.
