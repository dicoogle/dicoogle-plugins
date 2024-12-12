/**
 * Copyright (C) 2015  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/lucene.
 *
 * Dicoogle/lucene is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/lucene is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package dicoogle.lucene.dicom.abstraction;

import org.apache.lucene.document.*;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DocValuesType;
import org.apache.lucene.index.IndexOptions;

public class DicomLongField implements IDicomField
{
    private final String name;
    private final long value;
    
    public DicomLongField(String name, long value)
    {
        this.name = name;
        this.value = value;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @return the value
     */
    public long getValue() {
        return value;
    }

    static final FieldType FIELD_TYPE;
    static {
        FIELD_TYPE = new FieldType();
        FIELD_TYPE.setIndexOptions(IndexOptions.DOCS);
        FIELD_TYPE.setStored(true);
        FIELD_TYPE.setTokenized(false);
        FIELD_TYPE.setDocValuesType(DocValuesType.NUMERIC);
    }

    public Field toField() {
        return new Field(this.name, String.valueOf(this.value), FIELD_TYPE);
    }

    @Override
    public void addToDoc(Document doc) {
        // index & store as number, for retrieval and range-based queries
        doc.add(new StoredField(this.name, this.value));
        // index as string for keyword-based queries
        doc.add(new StringField(this.name, String.valueOf(this.value), Store.NO));
        doc.add(new FloatPoint("_point_" + this.name, (float)this.value));
    }
}
