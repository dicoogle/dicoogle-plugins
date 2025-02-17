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

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

public class FileIterator implements Iterator<File> {
    private IOFileFilter fileFilter;
    private File root;
    private String[] filenames;
    private Iterator<File> fileIt;
    private int i;
    private boolean iteratingInFiles;

    public FileIterator(File root) {
        this(root, (IOFileFilter)null);
    }

    public FileIterator(File root, IOFileFilter fileFilter) {
        this.fileFilter = null;
        this.fileFilter = fileFilter;
        this.root = root;
        this.filenames = this.mList(true);
        this.i = 0;
        this.iteratingInFiles = true;
        this.fileIt = null;
    }

    private String[] mList(boolean files) {
        IOFileFilter filter;
        if (files) {
            if (this.fileFilter != null) {
                filter = FileFilterUtils.and(new IOFileFilter[]{FileFileFilter.FILE, this.fileFilter});
            } else {
                filter = FileFileFilter.FILE;
            }
        } else {
            filter = DirectoryFileFilter.INSTANCE;
        }

        String[] ret = this.root.list(filter);
        return ret == null ? new String[0] : ret;
    }

    private boolean switchIterationMode() {
        if (!this.iteratingInFiles) {
            return false;
        } else {
            this.iteratingInFiles = false;
            this.i = 0;
            this.fileIt = Collections.emptyIterator();
            this.filenames = this.mList(false);
            if (!this.hasNextFilename()) {
                return false;
            } else {
                do {
                    this.fileIt = new FileIterator(new File(this.root, this.nextFilename()), this.fileFilter);
                } while(!this.fileIt.hasNext() && this.hasNextFilename());

                return this.fileIt.hasNext();
            }
        }
    }

    private boolean hasNextFilename() {
        return this.i < this.filenames.length;
    }

    private String nextFilename() {
        if (!this.hasNextFilename()) {
            return null;
        } else {
            String f = this.filenames[this.i];
            this.filenames[this.i] = null;
            ++this.i;
            return f;
        }
    }

    public boolean hasNext() {
        if (this.iteratingInFiles) {
            return this.hasNextFilename() ? true : this.switchIterationMode();
        } else if (this.fileIt.hasNext()) {
            return true;
        } else if (!this.hasNextFilename()) {
            return false;
        } else {
            do {
                this.fileIt = new FileIterator(new File(this.root, this.nextFilename()), this.fileFilter);
            } while(!this.fileIt.hasNext() && this.hasNextFilename());

            return this.fileIt.hasNext();
        }
    }

    public File next() {
        if (!this.hasNext()) {
            return null;
        } else {
            return this.iteratingInFiles ? new File(this.root, this.nextFilename()) : (File)this.fileIt.next();
        }
    }

    public void remove() {
        throw new UnsupportedOperationException();
    }
}
