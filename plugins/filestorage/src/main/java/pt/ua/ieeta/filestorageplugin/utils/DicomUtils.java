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

import java.nio.file.Path;
import java.nio.file.Paths;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;

/**
 *
 */
public class DicomUtils {

    public static Path getDirectory(DicomObject d) {
        String institutionName = d.getString(Tag.InstitutionName);
        String modality = d.getString(Tag.Modality);
        String studyDate = d.getString(Tag.StudyDate);
        String accessionNumber = d.getString(Tag.AccessionNumber);
        String studyInstanceUID = d.getString(Tag.StudyInstanceUID);
        String patientName = d.getString(Tag.PatientName);

        if (institutionName == null || institutionName.isEmpty()) {
            institutionName = "UN_IN";
        }
        institutionName = institutionName.trim()
            .replace(" ", "")
            .replace(".", "")
            .replace("&", "");


        if (modality == null || modality.isEmpty()) {
            modality = "UN_MODALITY";
        }

        if (studyDate == null || studyDate.isEmpty()) {
            studyDate = "UN_DATE";
        } else {
            try {
                String year = studyDate.substring(0, 4);
                String month = studyDate.substring(4, 6);
                String day = studyDate.substring(6, 8);

                studyDate = year + "/" + month + "/" + day;

            } catch (Exception e) {
                e.printStackTrace();
                studyDate = "UN_DATE";
            }
        }

        if (accessionNumber == null || accessionNumber.isEmpty()) {
            if (patientName == null) {
                patientName = "";
            }
            patientName = patientName.trim()
                .replaceAll(" ", "")
                .replaceAll(".", "")
                .replaceAll("&", "");

            if (patientName.isEmpty()) {
                if (studyInstanceUID == null || studyInstanceUID.isEmpty()) {
                    accessionNumber = "UN_ACC";
                } else {
                    accessionNumber = studyInstanceUID;
                }
            } else {
                accessionNumber = patientName;

            }

        }

        return Paths.get(institutionName, modality, studyDate, accessionNumber);
    }

    public static String getBaseName(DicomObject d) {
        String sopInstanceUID = d.getString(Tag.SOPInstanceUID);
        return sopInstanceUID + ".dcm";
    }

    public static Path getPath(DicomObject obj) {
        return getDirectory(obj).resolve(getBaseName(obj));
   }
}
