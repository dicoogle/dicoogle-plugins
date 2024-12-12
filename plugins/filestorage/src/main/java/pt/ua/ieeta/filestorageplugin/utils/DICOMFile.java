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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.StorageInputStream;

/** Implementation of a Dicoogle file residing in the file system.
 *
 */
public class DICOMFile implements StorageInputStream {
    private static final Logger logger = LoggerFactory.getLogger(DICOMFile.class);

    private final File file;
    private final URI uri;

    public DICOMFile(File file, URI uri) {
        this.file = file;
        this.uri = uri;
    }

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        if (file.getName().endsWith(".gz")) {
            return new GZIPInputStream(new FileInputStream(file));
        } else {
            return new BufferedInputStream(new FileInputStream(file));
        }
    }

    @Override
    public long getSize() throws IOException {
        return file.length();
    }

}
