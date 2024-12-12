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

import org.apache.commons.configuration.XMLConfiguration;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.DicomOutputStream;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.StorageInterface;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.ieeta.filestorageplugin.utils.*;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

public class FileStoragePlugin implements StorageInterface {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(FileStoragePlugin.class);

    // SETTINGS
    private ConfigurationHolder settings;
    private boolean isEnabled = false;
    private String scheme;
    private Path rootDir;
    private boolean useRelativePath;
    private PathTranslator pathTranslator;

    public FileStoragePlugin() {
        this.scheme = "file";
    }

    @Override
    public ConfigurationHolder getSettings() {
        return this.settings;
    }

    @Override
    public void setSettings(ConfigurationHolder xmlSettings) {
        this.settings = xmlSettings;
        XMLConfiguration cnf = xmlSettings.getConfiguration();

        cnf.setThrowExceptionOnMissing(true);

        String rootdir = System.getProperty("java.io.tmpdir");
        try {
            rootdir = cnf.getString("root-dir");
        } catch (NoSuchElementException ex) {
            logger.warn("File storage root directory is not configured! All stored data will be sent to temporary storage.");
            logger.warn("Please enter the file storage configuration file and modify the \"root-dir\" property.");
            cnf.setProperty("root-dir", rootdir);
        }
        try {
            this.useRelativePath = cnf.getBoolean("use-relative-path");
        } catch (NoSuchElementException ex) {
            this.useRelativePath = false;
            cnf.setProperty("use-relative-path", false);
        }

        this.rootDir = Paths.get(rootdir).toAbsolutePath().normalize();
        logger.debug("Configured file root directory: {}", rootDir);

        this.scheme = "file";
        try {
            this.scheme = cnf.getString("schema");
            logger.warn("Settings property \"schema\" is deprecated. Please use \"scheme\" instead.");
        } catch (NoSuchElementException ex) { /* do nothing */ }
        try {
            this.scheme = cnf.getString("scheme");
            cnf.clearProperty("schema");
        } catch (NoSuchElementException ex) {
            cnf.setProperty("scheme", this.scheme);
        }

        this.isEnabled = cnf.getBoolean("enabled", true);

        if (this.useRelativePath) {
            this.pathTranslator = new RelativePathTranslator(this.scheme, this.rootDir);
        } else {
            this.pathTranslator = new AbsolutePathTranslator(this.scheme);
        }
    }

    @Override
    public String getName() {
        return "file-storage";
    }

    /**
     *
     * @return Returns the scheme of this Storage Plugin example: storage
     */
    @Override
    public String getScheme() {
        return scheme;
    }

    /**
     * Stores a DICOM Object.
     *
     * @param dicomObject DICOM Object to be Stored
     * @param args variable length arguments, currently unused
     * @return The URI of the previously stored object.
     */
    @Override
    public URI store(DicomObject dicomObject, Object... args) {
        if (rootDir == null) throw new IllegalStateException("File storage plugin is not ready!");
        if (!isEnabled) {
            return null;
        }

        Path relativePath = DicomUtils.getPath(dicomObject);
        Path filePath = this.rootDir.resolve(relativePath);

        logger.debug("Trying to store file to FS path {}", filePath);

        try {
            Files.createDirectories(filePath.getParent());
        } catch (IOException e) {
            logger.error("Failed to create directories for file {}", filePath, e);
            return null;
        }

        URI finalUri = this.pathTranslator.toUri(filePath);
        logger.debug("Assigning new file to URI {}", finalUri);

        try (OutputStream fos = Files.newOutputStream(filePath);
                BufferedOutputStream bos = new BufferedOutputStream(fos);
                DicomOutputStream dos = new DicomOutputStream(bos)) {
            dos.writeDicomFile(dicomObject);
        } catch (IOException ex) {
            logger.error("Failed to store file {}", finalUri, ex);
            // FOOO I COULD NOT WRITE THE FILE.
            return null;
        }

        return finalUri;
    }

    /**
     * Stores a DICOM InputStream
     *
     * @param inStream InputStream to be stored
     * @param args variable length arguments, currently unused
     * @return The URI of the previously stored stream
     * @throws IOException
     */
    @Override
    public URI store(DicomInputStream inStream, Object... args) throws IOException {
        if (rootDir == null) throw new IllegalStateException("File storage plugin is not ready!");
        if (!isEnabled) {
            return null;
        }

        DicomInputStream inputStream = inStream;
        DicomObject obj = inputStream.readDicomObject();

        return store(obj);
    }

    @Override
    public boolean enable() {
        this.isEnabled = true;
        return true;
    }

    @Override
    public boolean disable() {
        this.isEnabled = false;
        return true;
    }

    @Override
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * @param uri
     */
    @Override
    public void remove(URI uri) {
        if (!isEnabled) {
            return;
        }
        if (!handles(uri)) {
            return;
        }
        Path p = this.pathTranslator.toPath(uri);
        try {
            boolean b = Files.deleteIfExists(p);
            logger.debug("Removal of file at {}: {}", uri, b);
        } catch (IOException e) {
            logger.warn("Failed to delete file at {}", p, e);
        }
    }

    @Override
    public boolean handles(URI location) {
        Objects.requireNonNull(location);
        return Objects.equals(location.getScheme(), this.scheme);
    }

    @Override
    public Iterable<StorageInputStream> at(URI location, Object... args) {
        if (rootDir == null) throw new IllegalStateException("File storage plugin is not ready!");
        logger.debug("FileStorage.at(\"{}\")", location);

        if (!handles(location)) {
            logger.warn("Illegal URI provided to file storage: {}", location);
            return Collections.EMPTY_LIST;
        }

        Path locationPath = this.pathTranslator.toPath(location);

        return new MyIterable(locationPath);
    }

    private class MyIterable implements Iterable<StorageInputStream> {

        private final Iterator<StorageInputStream> it;

        public MyIterable(Path baseLocation) {
            super();
            this.it = createIterator(baseLocation);
        }

        @Override
        public Iterator<StorageInputStream> iterator() {
            return this.it;
        }

        private Iterator<StorageInputStream> createIterator(Path baseLocation) {
            return DicoogleFileIterator.createFromLocation(FileStoragePlugin.this.pathTranslator, baseLocation);
        }
    }
}
