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
package pt.ua.ieeta.filestorageplugin.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.StorageInputStream;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

public class DicoogleFileIterator implements Iterator<StorageInputStream> {
    private static final Logger logger = LoggerFactory.getLogger(DicoogleFileIterator.class);

    private final PathTranslator pathTranslator;
    private final Iterator<File> it;

    public DicoogleFileIterator(PathTranslator pathTranslator, Iterator<File> it) {
        this.pathTranslator = pathTranslator;
        this.it = it;
    }

    public static DicoogleFileIterator createFromLocation(PathTranslator pathTranslator, Path baseLocation) {
        Objects.requireNonNull(pathTranslator);
        Objects.requireNonNull(baseLocation);
        assert baseLocation.isAbsolute();
        Iterator<File> fileIt = makeIterator(baseLocation);
        return new DicoogleFileIterator(pathTranslator, fileIt);
    }

    public static DicoogleFileIterator newRelative(String scheme, Path rootDir, Path baseLocation) {
        Objects.requireNonNull(scheme);
        Objects.requireNonNull(rootDir);
        Objects.requireNonNull(baseLocation);
        assert rootDir.isAbsolute();
        if (!baseLocation.isAbsolute()) {
            baseLocation = rootDir.resolve(baseLocation);
        }
        return DicoogleFileIterator.createFromLocation(new RelativePathTranslator(scheme, rootDir), baseLocation);
    }

    public static DicoogleFileIterator newAbsolute(String scheme, Path baseLocation) {
        Objects.requireNonNull(scheme);
        Objects.requireNonNull(baseLocation);
        assert baseLocation.isAbsolute();
        return DicoogleFileIterator.createFromLocation(new AbsolutePathTranslator(scheme), baseLocation);
    }

    @Override
    public boolean hasNext() {
        return it.hasNext();
    }

    @Override
    public StorageInputStream next() {
        File f = it.next();
        URI uri = this.pathTranslator.toUri(f.toPath());
        return new DICOMFile(f, uri);
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not allowed");
    }

    /** Make a deep iterator over all files at the given path. */
    private static Iterator<File> makeIterator(Path path) {
        final File f = path.toFile();
        if (!f.canRead()) {
            return Collections.<File>emptyList().iterator();
        }
        if (f.isDirectory()) {
            return new FileIterator(f);
        }
        return Collections.singletonList(f).iterator();
    }
}
