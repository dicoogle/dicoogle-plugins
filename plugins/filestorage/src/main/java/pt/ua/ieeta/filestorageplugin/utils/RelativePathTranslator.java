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

import java.net.URI;
import java.nio.file.Path;

public class RelativePathTranslator implements PathTranslator {
    private final String scheme;
    private final Path rootDir;

    public RelativePathTranslator(String scheme, Path rootDir) {
        if (!rootDir.isAbsolute()) {
            rootDir = rootDir.toAbsolutePath();
        }
        this.scheme = scheme;
        this.rootDir = rootDir;
    }

    @Override
    public Path toPath(URI uri) {
        String relativePart = "./" + uri.getSchemeSpecificPart();
        return this.rootDir.resolve(relativePart).normalize();
    }

    @Override
    public URI toUri(Path path) {
        final int rootLength = this.rootDir.toUri().getRawSchemeSpecificPart().length() - 1;
        final String part = path.toUri().getRawSchemeSpecificPart();
        return URI.create(this.scheme + ":" + part.substring(rootLength));
    }
}
