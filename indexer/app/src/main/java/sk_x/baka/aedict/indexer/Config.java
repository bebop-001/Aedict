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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.zip.GZIPInputStream;

public class Config {

    private final FileTypeEnum fileType;
    private final Charset encoding;
    public String name;
    private final File outputDir;

    public String getTargetFileName() {
        return fileType.getTargetFileName(name);
    }
    public FileTypeEnum getFileType () {return fileType;}

    public BufferedReader newReader() throws IOException {
        File localSrc = fileType.getSourceFile();
        return new BufferedReader(new InputStreamReader(
                new FileInputStream(localSrc), encoding));
    }
    public BufferedReader newReader(String srcFileName) throws IOException {
        return new BufferedReader(new InputStreamReader(
                new FileInputStream(srcFileName), encoding));
    }
    public BufferedReader newReader(File srcFileName) throws IOException {
        return new BufferedReader(new InputStreamReader(
                new FileInputStream(srcFileName), encoding));
    }

    Config(FileTypeEnum fileType, File outputDir) {
        this.fileType = fileType; this.outputDir = outputDir;
        encoding = Charset.forName(fileType.getDefaultEncoding());
    }
}
