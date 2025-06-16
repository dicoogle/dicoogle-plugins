/**
 * Copyright (C) 2014  Universidade de Aveiro, DETI/IEETA, Bioinformatics Group - http://bioinformatics.ua.pt/
 *
 * This file is part of Dicoogle/dicoogle-plugin-sample.
 *
 * Dicoogle/dicoogle-plugin-sample is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Dicoogle/dicoogle-plugin-sample is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Dicoogle.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.dicoogle.plugins.dicomweb.query;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomInputStream;
import org.dicoogle.plugins.dicomweb.DicomJson;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.task.Task;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
// Import file
import java.io.File;
/** Sample Jetty servlet-based web service.
 *
 * @author Luís A. Bastião Silva - <bastiao@ua.pt>
 */
public class InstancesProcess  implements PlatformCommunicatorInterface {
    private static final Logger logger = LoggerFactory.getLogger(InstancesProcess.class);

    private DicooglePlatformInterface platform;

    public void process(HttpServletRequest req, HttpServletResponse response)
            throws ServletException, IOException {
        String path = req.getPathInfo();  // e.g., /1.2.3/series/4.5.6/metadata

        String[] parts = path.split("/");


        String studyUID = parts[1];
        String seriesUID = parts[3];
        String sopUID = parts[5];

        HashMap<String, String> extraFields = new HashMap<>();
        extraFields.put("PatientName", "PatientName");
        extraFields.put("PatientID", "PatientID");
        extraFields.put("Modality", "Modality");
        extraFields.put("StudyDate", "StudyDate");
        extraFields.put("SeriesInstanceUID", "SeriesInstanceUID");
        extraFields.put("StudyID", "StudyID");
        extraFields.put("StudyInstanceUID", "StudyInstanceUID");
        extraFields.put("Thumbnail", "Thumbnail");
        extraFields.put("SOPInstanceUID", "SOPInstanceUID");
        Iterable<SearchResult> rr = null;
        try {
            // Use the UID in the query if necessary.
            Task<Iterable<SearchResult>> result = this.platform.query("lucene", "SOPInstanceUID:"+req.getParameter("SOPInstanceUID"), extraFields);
            rr = result.get();
        } catch (InterruptedException | ExecutionException ex) {
            logger.warn("Operation failed", ex);
        }

        // Construct a JSON response by iterating over SearchResult elements.
        List<Map<String, Object>> results = new ArrayList<>();
        JSONArray jsonArray = new JSONArray();

        Set<String> keys = new HashSet<>();

        // Assuming SearchResult has a method getAttribute for retrieving properties.
        if (rr != null) {
            for (SearchResult sr : rr) {
                Map<String, Object> resultMap = new HashMap<>();
                // Assuming each SearchResult has a method getAttribute for retrieving properties.
                resultMap.put("PatientName", sr.get("PatientName"));
                resultMap.put("PatientID", sr.get("PatientID"));
                resultMap.put("Modality", sr.get("Modality"));
                resultMap.put("StudyDate", sr.get("StudyDate"));
                resultMap.put("SeriesInstanceUID", sr.get("SeriesInstanceUID"));
                resultMap.put("StudyID", sr.get("StudyID"));
                resultMap.put("StudyInstanceUID", sr.get("StudyInstanceUID"));
                resultMap.put("Thumbnail", sr.get("Thumbnail"));
                resultMap.put("SOPInstanceUID", sr.get("SOPInstanceUID"));
                results.add(resultMap);
                JSONObject json = new JSONObject();
                if (keys.add((String)sr.get("SOPInstanceUID"))) {
// Read Dicom from URI
                    // Assuming getURI() returns the URI of the DICOM object.
                    try (DicomInputStream dcm = new DicomInputStream(sr.getURI().toURL().openStream())) {
                        // Process the DICOM object as needed.
                        // For example, you can read attributes or perform other operations.
                        Attributes attributes = dcm.readDataset(-1, -1);


                        JSONObject obj =  DicomJson.fromAttributes(attributes, (String)sr.get("SOPInstanceUID"));
                        jsonArray.put(obj);
                    } catch (IOException e) {
                        logger.error("Error reading DICOM object", e);
                    }

                }

            }
        }

        response.setContentType("application/json;charset=utf-8");
        response.setContentType("application/json;charset=utf-8");

        response.getWriter().write(jsonArray.toString());

    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface core) {
        this.platform = core;
    }

}
