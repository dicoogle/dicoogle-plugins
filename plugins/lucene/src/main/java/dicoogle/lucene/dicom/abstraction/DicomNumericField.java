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

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.document.StoredField;

public class DicomNumericField implements IDicomField
{
    private final String name;
    private final float value;

    public DicomNumericField(String name, float value) {
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
    public float getValue() {
        return value;
    }

    @Override
    public void addToDoc(Document doc) {
        doc.add(new StoredField(this.name, this.value));
        doc.add(new FloatPoint("_point_" + this.name, this.value));
    }
}
