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

import java.util.ArrayList;
import java.util.List;

public class DicomDocument implements IDoc
{

    private final List<IDicomField> dicomFields = new ArrayList<>();

    @Override
    public void add(String name, String value) 
    {
        this.getDicomFields().add(new DicomTextField(name, value));
    }

    @Override
    public void add(String name, float value) {
        this.getDicomFields().add(new DicomNumericField(name, value));
    }

    @Override
    public void add(String name, long value) {
        this.getDicomFields().add(new DicomLongField(name, value));
    }

    @Override
    public void add(String name, byte[] value) {
        this.getDicomFields().add(new DicomByteArrField(name, value));
    }
    
    /**
     * @return the dicomFields
     */
    @Override
    public List<IDicomField> getDicomFields() {
        return dicomFields;
    }

    @Override
    public Document toDocument() {
        Document doc = new Document();
        dicomFields.stream().forEachOrdered(d -> {
            d.addToDoc(doc);
        });
        return doc;
    }


}
