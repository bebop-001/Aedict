

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

## Running the indexer:
The Indexer is actually 2 pure java apps.  To run them
the first time you have to try to start the main of each.
Probably it will fail to start.  Use File->Invalidate/Restart
in Android Studio and the second time it will restart.
* Navigate to app/src/main/java/sk_x.baka/indexer/SodMain.java.
* Select 'run SodMain.main()' in the left of the editor pane.
Expect it to fail.
* Do the same for app/src/main/java/sk_x.baka/indexer/Main.java
and likewise, expect it to fail.
* Select File->Invalidate Caches/Restart from the Android Studio 
File pulldown.

After restart you should be able to select either Main or SodMain from
the app pulldown and start the app.  Output will appear in the
AndroidStudio Run Window below the Editor window.

Both the indexer functions require arguments and you should see their
usage message in the Run window.  "EditConfiguration" for the app
to add the appropriate arguments.  You will get a new zip or .gz file
with new index data.  Copy this to the dictionary assets directory
(dictionary/app/src/main/assets) and rebuild and reinstall the
dictionary.  When you update the app, it should use the new indexes.

**However** -- to be honest -- I haven't tried updating the indexes yet.
I actually just got the indexer working today.

If you find problems, please let me know.

Steve S.
