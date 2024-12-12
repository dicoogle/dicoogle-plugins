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

import org.junit.*;
import org.slf4j.LoggerFactory;
import pt.ua.ieeta.filestorageplugin.utils.AbsolutePathTranslator;
import pt.ua.ieeta.filestorageplugin.utils.PathTranslator;
import pt.ua.ieeta.filestorageplugin.utils.RelativePathTranslator;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static junit.framework.Assert.failNotEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.*;

public class PathTranslatorTest {

    private static final String TMP_DIR = createRootDirPath();
    private static final String SCHEME = "local"; // using something different for testing purposes
    private static final int MAGIC_CODE = 0xD1C00C13;
    private static Path rootDir;
    private static URI rootUri;
    private static Path pathToFile;

    private static String createRootDirPath() {
        String s = System.getProperty("java.io.tmpdir");
        if (!s.endsWith("/")) {
            s += "/";
        }
        return s;
    }

    @BeforeClass
    public static void init() throws IOException {
        rootDir = Paths.get(TMP_DIR).resolve("dicoogle-file-storage-test1/mydataset").toAbsolutePath();

        URI bland_uri = rootDir.toUri();
        rootUri = URI.create(SCHEME + ":" + bland_uri.getRawSchemeSpecificPart().substring(2));
        LoggerFactory.getLogger(PathTranslatorTest.class).info("Root URI: {}", rootUri);
        Files.createDirectories(rootDir);
        pathToFile = rootDir.resolve("CT/file.dcm");
        Files.createDirectories(pathToFile.getParent());
        try (DataOutputStream ostream = new DataOutputStream(Files.newOutputStream(pathToFile))) {
            ostream.writeInt(MAGIC_CODE);
        }

        assertTrue(rootDir.isAbsolute());
        assertNotNull(rootDir.getRoot());
        assertTrue(pathToFile.startsWith(rootDir));
    }

    @Test
    public void testAbsolute() throws IOException {
        PathTranslator pt = new AbsolutePathTranslator(SCHEME);

        URI fileUri = pt.toUri(pathToFile);
        final URI expectedUri = rootUri.resolve("CT/file.dcm");
        assertStrictEquals(expectedUri, fileUri);
        Path p = pt.toPath(fileUri);
        assertEquals(pathToFile, p);

        try (DataInputStream istream = new DataInputStream(Files.newInputStream(p))) {
            int c = istream.readInt();
            assertEquals(MAGIC_CODE, c);
        }
    }

    @Test
    public void testAbsolute2() throws IOException {
        PathTranslator pt = new AbsolutePathTranslator(SCHEME);
        Path p = rootDir.resolve("new folder").resolve("file.dcm");
        URI fileUri = pt.toUri(p);
        final URI expectedUri = rootUri.resolve("new%20folder/file.dcm");
        assertStrictEquals(expectedUri, fileUri);
        Path back = pt.toPath(fileUri);
        assertEquals(p, back);
    }

    @Test
    public void testRelative() throws IOException {
        PathTranslator pt = new RelativePathTranslator(SCHEME, rootDir);

        URI fileUri = pt.toUri(pathToFile);
        assertEquals(URI.create(SCHEME + ":/CT/file.dcm"), fileUri);
        Path p = pt.toPath(fileUri);
        assertEquals(pathToFile, p);

        try (DataInputStream istream = new DataInputStream(Files.newInputStream(p))) {
            int c = istream.readInt();
            assertEquals(MAGIC_CODE, c);
        }
    }

    @Test
    public void testRelative2() throws IOException {
        PathTranslator pt = new RelativePathTranslator(SCHEME, rootDir);
        Path p = rootDir.resolve("CR").resolve("with some spaces.dcm");
        URI fileUri = pt.toUri(p);
        assertEquals(URI.create(SCHEME + ":/CR/with%20some%20spaces.dcm"), fileUri);
        Path back = pt.toPath(fileUri);
        assertEquals(p, back);
    }

    @AfterClass
    public static void cleanup() throws IOException {
        Files.delete(pathToFile);
    }

    /** A stricter check on URI: leading double slashes in the scheme specific part are not ignored.
     */
    private static boolean strictEquals(URI uri1, URI uri2) {
        return Objects.equals(uri1.toString(), uri2.toString());
    }

    private static void assertStrictEquals(URI uri1, URI uri2) {
        if (!strictEquals(uri1, uri2)) {
            failNotEquals("URIs should be strictly equal", uri1, uri2);
        }
    }

}
