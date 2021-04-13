/*
Aedict - an EDICT browser for Android
Copyright (C) 2009 Martin Vysny

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package sk_x.baka.aedict.indexer;

import android.annotation.SuppressLint;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.commons.cli.Options;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import sk_x.baka.aedict.dict.LuceneSearch;
import sk_x.baka.autils.MiscUtils;

/**
 * Downloads the EDict file, indexes it with Lucene then zips it.
 * 
 * @author Martin Vysny
 */
public class Main {

    static private final String CWD = new File("").getAbsolutePath();
    static final File resourcesDir = new File(CWD, "resources");
    static final File assetsDir = new File(CWD, "assets");

    static final String LUCENE_INDEX =
            assetsDir.getAbsolutePath() + "/index";

    public static void main(String[] args) {
        if (! resourcesDir.exists())
            throw new RuntimeException("Aedict.indexer:" +
                    " failed to find resources directory:"
                    + resourcesDir.toString());
        if (! assetsDir.exists())
            throw new RuntimeException("Aedict.indexer:"
                    + " failed to find assets directory:"
                    + assetsDir.toString());
        try {
            if (args == null || args.length == 0) {
                printHelp();
                System.exit(255);
            }
            //noinspection ConstantConditions
            new Main(args).run();
        } catch (Exception ex) {
            // ex.printStackTrace();
            System.out.println("Indexing failed: " + ex.toString());
            printHelp();
            System.exit(1);
        }
    }
    private static final String REMOTE_DIR = "/home/moto/public_html/aedict/dictionaries";

    private static void exec(SSHClient ssh, String cmd) throws IOException {
        final Session s = ssh.startSession();
        try {
            final Command c = s.exec(cmd);
            if (c.getExitErrorMessage() != null) {
                throw new RuntimeException("Command " + cmd + " failed to execute with status " + c.getExitStatus() +
                        ": " + c.getExitErrorMessage() + ", " + c.getExitErrorMessage());
            }
        } finally {
            MiscUtils.closeQuietly(s);
        }
    }

    /*
    private void upload() throws Exception {
        System.out.println("Uploading");
        final SSHClient ssh = new SSHClient();
        ssh.loadKnownHosts();
        String password = config.password;
        if (password == null) {
            System.out.println("Enter password");
            final Scanner s = new Scanner(System.in);
            password = s.nextLine();
            if (MiscUtils.isBlank(password)) {
                throw new RuntimeException("Invalid password: blank");
            }
        }
        System.out.println("Connecting");
        ssh.connect("rt.sk");
        try {
            System.out.println("Authenticating");
            ssh.authPassword("moto", password);
            System.out.println("Uploading version");
            final String targetFName = REMOTE_DIR + "/" + config.getTargetFileName();
            exec(ssh, "echo `date +%Y%m%d` >" + REMOTE_DIR + "/" + config.getTargetFileName() + ".version");
            exec(ssh, "rm -f " + targetFName);
            System.out.println("Uploading");
            final SCPFileTransfer ft = ssh.newSCPFileTransfer();
            ft.upload(config.getTargetFileName(), targetFName);
        } finally {
            ssh.disconnect();
        }
    }

     */

    public Config config = null;

    private static Options getOptions() {
        final Options opts = new Options();
        opts.addOption("?", null, false,
                "prints this help");
        opts.addOption("e", "edict", false,
                "process  edic.EUC-JP");
        opts.addOption("k", "kanjidic", false,
                "process is kanjidic");
        opts.addOption("T", "tatoeba", false,
                "Tatoeba Project file with example sentences");
        opts.addOption("s", "sod", false,
                "process sod file");
        return opts;
    }

    Main(final String[] args) {
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            switch (arg) {
                case "?":
                    printHelp();
                    System.exit(255);
                case "-k":
                    config = new Config(FileTypeEnum.Kanjidic, resourcesDir);
                    break;
                case "-T":
                    config = new Config(FileTypeEnum.Tatoeba,
                            new File(resourcesDir, "/tatoeba"));
                    break;
                case "-e":
                    config = new Config(FileTypeEnum.Edict, resourcesDir);
                    break;
                default:
                    System.out.println("Unrecognized option or no options given.");
                    printHelp();
                    System.exit(255);
            }
            try {
                run();
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0xff);
            }
            i++;
        }
        System.exit(0);
    }

    private static void printHelp() {
        final HelpFormatter f = new HelpFormatter();
        f.printHelp(Main.class.getName(), "Aedict index file generator\n"
                        + "Produces a Lucene-indexed file from given dictionary file. "
                , getOptions(), null, true);
    }

    void run() throws Exception {
        String sb = "Indexing " +
                config.getFileType() +
                " file from " +
                config.getFileType().getSourceFile();
        System.out.println(sb);
        indexWithLucene();
        zipLuceneIndex();
    }

    @SuppressWarnings("deprecation")
    private void indexWithLucene() throws IOException {
        System.out.println("Deleting old Lucene index");
        FileUtils.deleteDirectory(new File(LUCENE_INDEX));
        System.out.println("Indexing with Lucene");
        final BufferedReader dictionary = config.newReader();
        try {
            final Directory directory = FSDirectory.open(new File(LUCENE_INDEX));
            try {
                try (IndexWriter luceneWriter = new IndexWriter(directory,
                        new StandardAnalyzer(LuceneSearch.LUCENE_VERSION), true,
                        IndexWriter.MaxFieldLength.UNLIMITED)) {
                    final IDictParser parser = config.getFileType().newParser(config);
                    indexWithLucene(dictionary, luceneWriter, parser);
                    System.out.println("Optimizing Lucene index");
                    //noinspection deprecation
                    luceneWriter.optimize();
                }
            } finally {
                closeQuietly(directory);
            }
        } finally {
            IOUtils.closeQuietly(dictionary);
        }
    }
    private static final Logger log = Logger.getLogger(Main.class.getName());

    private static void closeQuietly(final Directory d) {
        try {
            d.close();
        } catch (Exception ex) {
            log.log(Level.WARNING, "Failed to close a Directory object", ex);
        }
    }

    private static void indexWithLucene(BufferedReader edict,
            IndexWriter luceneWriter, final IDictParser parser) throws IOException {
        for (String line = edict.readLine(); line != null; line = edict.readLine()) {
            if (line.startsWith("#")) {
                // skip comments
                continue;
            }
            if (line.trim().length() == 0) {
                // skip blank lines
                continue;
            }
            parser.addLine(line, luceneWriter);
        }
        parser.onFinish(luceneWriter);
        luceneWriter.commit();
    }

    @SuppressWarnings("ConstantConditions")
    private void zipLuceneIndex() throws IOException {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat("-yyyMMdd.");
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        String dateFormatted = sdf.format(new Date());
        String[] s = config.getTargetFileName().split("\\.");
        String outFile = assetsDir.toString()
                + "/" + s[0] + dateFormatted + s[1];
        System.out.println("Zipping the index file to " + outFile);
        final File zip = new File(outFile);
        if (zip.exists() && !zip.delete()) {
            throw new IOException("Cannot delete " + zip.getAbsolutePath());
        }
        final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(
                zip));
        try {
            out.setLevel(9);
            final File[] luceneIndexFiles = new File(LUCENE_INDEX).listFiles();
            for (final File indexFile : luceneIndexFiles) {
                final ZipEntry entry = new ZipEntry(indexFile.getName());
                entry.setSize(indexFile.length());
                out.putNextEntry(entry);
                final InputStream in = new FileInputStream(indexFile);
                try {
                    IOUtils.copy(in, out);
                } finally {
                    IOUtils.closeQuietly(in);
                }
                out.closeEntry();
            }
        } finally {
            IOUtils.closeQuietly(out);
        }
        System.out.println("Finished index zipping");
    }
}
