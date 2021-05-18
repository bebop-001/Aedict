

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

5/10/2021<br>
Romaji support removed.  It adds too much complexity.  Also, my experience
is that it's a crutch that in the longer run will impede learning Japanese
reading and writing.  Kana isn't that complicated. 😊

5/17/2021<br>
Installed kotlin code to install dictionary files from assets.  The code
looks for currently installed dictionaries and if not installed, installs.
It puts up a progress dialog during the install process but this is
currently obscured by a what's new dialog.  Just tap on the what's new
dialog if you want to see progress.

I haven't done a whole lot of testing but on Android 4.4 and Android 10, it
comes up and basic functions are working.  On Android 4.4 the colors are
still screwed up on the example screen though...
<br>
4/18/2021
Colors for both 4.4 and 10 should be workable now.  Behavior between the
two versions with some things (Spinners, checkboxes) is very different.
While not ideal, I think this is workable.

Colors are very different from original app but again, workable.





Steve S.

<center>
  <summary>Demo app screenshot</summary>
  <img alt="demo app screenshot" src="https://github.com/bebop-001/SearchWindow/blob/master/images/Screenshot.png">
</center>
