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
import org.junit.Test;
import pt.ua.ieeta.filestorageplugin.utils.DicoogleFileIterator;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DigoogleFileIteratorTest {

    private static final String TMP_DIR = createRootDirPath();
    private static final String SCHEME = "local"; // using something different for testing purposes
    private Path rootDir;

    private static String createRootDirPath() {
        String s = System.getProperty("java.io.tmpdir").replace('\\', '/');
        if (!s.endsWith("/")) {
            s += "/";
        }
        return s;
    }

    private static void safeCreateDirectories(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
    }
    private static void safeCreateDirectory(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
    }
    private static void safeCreateFile(Path path) throws IOException {
        if (!Files.exists(path)) {
            Files.createFile(path);
        }
    }

    @Before
    public void init() throws IOException {
        this.rootDir = Paths.get(TMP_DIR).resolve("dicoogle-file-storage-test2/my dataset").toAbsolutePath().normalize();

        safeCreateDirectories(this.rootDir);
        safeCreateDirectory(this.rootDir.resolve("CT"));
        safeCreateDirectory(this.rootDir.resolve("CR"));
        safeCreateFile(this.rootDir.resolve("CT/0.dcm"));
        safeCreateFile(this.rootDir.resolve("CR/1.dcm"));
        safeCreateFile(this.rootDir.resolve("CR/2.dcm"));
        safeCreateFile(this.rootDir.resolve("CR/a b c.dcm"));
    }

    @Test
    public void test_root() throws IOException {
        DicoogleFileIterator it = DicoogleFileIterator.newRelative(SCHEME, this.rootDir, this.rootDir.resolve(""));
        List<URI> l = new ArrayList<>();
        while (it.hasNext()) {
            l.add(it.next().getURI());
        }
        Collections.sort(l);

        assertEquals(4, l.size());

        assertEquals(URI.create("local:/CR/1.dcm"), l.get(0));
        assertEquals(URI.create("local:/CR/2.dcm"), l.get(1));
        assertEquals(URI.create("local:/CR/a%20b%20c.dcm"), l.get(2));
        assertEquals(URI.create("local:/CT/0.dcm"), l.get(3));
    }

    @Test
    public void test_1() throws IOException {
        DicoogleFileIterator it = DicoogleFileIterator.newRelative(SCHEME, this.rootDir, Paths.get("CR/1.dcm"));
        List<URI> l = new ArrayList<>();
        while (it.hasNext()) {
            l.add(it.next().getURI());
        }
        assertEquals(1, l.size());

        assertEquals(URI.create("local:/CR/1.dcm"), l.iterator().next());
    }

    @Test
    public void test_not_exist() throws IOException {
        DicoogleFileIterator it = DicoogleFileIterator.newRelative(SCHEME, this.rootDir, Paths.get("idonotexist"));
        List<URI> l = new ArrayList<>();
        while (it.hasNext()) {
            l.add(it.next().getURI());
        }
        assertEquals(Collections.emptyList(), l);
    }

    @After
    public void cleanup() throws IOException {
        Files.deleteIfExists(this.rootDir.resolve("CT/0.dcm"));
        Files.deleteIfExists(this.rootDir.resolve("CT"));
        Files.deleteIfExists(this.rootDir.resolve("CR/1.dcm"));
        Files.deleteIfExists(this.rootDir.resolve("CR/2.dcm"));
        Files.deleteIfExists(this.rootDir.resolve("CR/a b c.dcm"));
        Files.deleteIfExists(this.rootDir.resolve("CR"));
        Files.deleteIfExists(this.rootDir);
    }
}
