/**
 * Copyright (C) 2015  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/filestorage.
 *
 * Dicoogle/filestorage is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/filestorage is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package pt.ua.ieeta.filestorageplugin;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import pt.ua.ieeta.filestorageplugin.utils.DICOMFile;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DICOMFileTest {

    private static final String TMP_DIR = createRootDirPath();
    private static final String SCHEME = "local"; // using something different for testing purposes
    private static final int MAGIC_CODE = 0xD1C00C13;
    private Path rootDir;
    private URI rootUri;
    private Path pathToFile;
    private Path pathToFile2;

    private static String createRootDirPath() {
        String s = System.getProperty("java.io.tmpdir").replace('\\', '/');
        if (!s.endsWith("/")) {
            s += "/";
        }
        return s;
    }

    @Before
    public void init() throws IOException {
        this.rootDir = Paths.get(TMP_DIR).resolve("dicoogle-file-storage-test1/mydataset").toAbsolutePath().normalize();

        URI bland_uri = rootDir.toAbsolutePath().toUri();
        this.rootUri = URI.create(SCHEME + ":" + bland_uri.getRawSchemeSpecificPart().substring(2));

        Files.createDirectories(this.rootDir);
        this.pathToFile = rootDir.resolve("CT/file.dcm");
        Files.createDirectories(this.pathToFile.getParent());
        try (DataOutputStream ostream = new DataOutputStream(Files.newOutputStream(pathToFile))) {
            ostream.writeInt(MAGIC_CODE);
        }

        this.pathToFile2 = rootDir.resolve("CT/file2.dcm.gz");
        try (DataOutputStream ostream = new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(pathToFile2)))) {
            ostream.writeInt(MAGIC_CODE);
        }

        assertTrue(this.rootDir.isAbsolute());
        assertNotNull(this.rootDir.getRoot());
        assertTrue(pathToFile.startsWith(this.rootDir));
        assertTrue(pathToFile2.startsWith(this.rootDir));
    }

    @Test
    public void test() throws IOException {
        URI uri = this.rootUri.resolve("CT/file.dcm");

        DICOMFile dcmFile = new DICOMFile(this.pathToFile.toFile(), uri);
        assertEquals(uri, dcmFile.getURI());
        try (DataInputStream istream = new DataInputStream(dcmFile.getInputStream())) {
            int c = istream.readInt();
            assertEquals(MAGIC_CODE, c);
        }
    }
    
    @Test
    public void test2() throws IOException {
        URI uri = this.rootUri.resolve("CT/file2.dcm.gz");

        DICOMFile dcmFile = new DICOMFile(this.pathToFile2.toFile(), uri);
        assertEquals(uri, dcmFile.getURI());
        try (DataInputStream istream = new DataInputStream(dcmFile.getInputStream())) {
            int c = istream.readInt();
            assertEquals(MAGIC_CODE, c);
        }
    }

    @After
    public void cleanup() throws IOException {
        Files.delete(pathToFile);
    }
}
