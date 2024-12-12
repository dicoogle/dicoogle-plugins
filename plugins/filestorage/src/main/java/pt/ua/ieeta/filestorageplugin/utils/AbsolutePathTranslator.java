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
import java.nio.file.Paths;

public class AbsolutePathTranslator implements PathTranslator {
    private final String scheme;

    public AbsolutePathTranslator(String scheme) {
        this.scheme = scheme;
    }

    @Override
    public Path toPath(URI uri) {
        return Paths.get(withFileScheme(uri));
    }

    @Override
    public URI toUri(Path path) {
        return withScheme(this.scheme, path.toUri());
    }

    private static URI withFileScheme(URI uri) {
        if ("file".equals(uri.getScheme())) {
            return uri;
        }
        String part = uri.getRawSchemeSpecificPart();
        if (part.startsWith("//")) {
            part = part.substring(2);
        }
        return URI.create("file:" + part);
    }

    private static URI withScheme(String scheme, URI uri) {
        if (scheme.equals(uri.getScheme())) {
            return uri;
        }
        String part = uri.getRawSchemeSpecificPart();
        if (part.startsWith("//")) {
            part = part.substring(2);
        }
        return URI.create(scheme + ":" + part);
    }
}
