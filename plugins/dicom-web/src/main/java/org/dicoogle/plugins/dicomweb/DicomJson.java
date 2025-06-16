/**
 * Copyright (c) 2015 BMD Software, Lda.
 * All Rights Reserved
 *
 * All information contained herein is, and remains the property of BMD Software, Lda. and its suppliers, if any.
 * The intellectual and technical concepts contained herein are proprietary to BMD Software, Lda. and its suppliers,
 * being protected by trade secret or copyright law. Dissemination of this information or reproduction of this
 * material is strictly forbidden unless prior written permission is obtained from BMD Software, Lda.
 *
 * This file is part of Dicoogle/dicoogle-plugins.
 */
package org.dicoogle.plugins.dicomweb;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.ElementDictionary;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.VR;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * DICOM JSON Serialization utility module
 */
public final class DicomJson {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DicomJson.class);

    private DicomJson() {}

    /**
     * This method builds a JSON object from the given DICOM attributes, including sequences.
     * It is used to create the JSON representation of DICOM metadata.
     *
     * The attributes are expected to be a root dataset containing a SOP Instance UID.
     *
     * VRs OB, OW, OD, and OF are not serialized so that other routines may add Bulk data URIs instead.
     *
     * @param dicomAttributes The DICOM attributes to convert to JSON.
     * @return A JSONObject representing the DICOM attributes.
     */
    public static JSONObject fromAttributes(Attributes dicomAttributes) {
        String sopInstanceUID = dicomAttributes.getString(Tag.SOPInstanceUID);
        return fromAttributes(dicomAttributes, sopInstanceUID);
    }

    /**
     * This method builds a JSON object from the given DICOM attributes, including sequences.
     * It is used to create the JSON representation of DICOM metadata.
     *
     * Nested datasets can also be serialized here.
     *
     * Values of VR OB, OW, OD, and OF are not serialized
     * so that other routines may add Bulk data URIs instead.
     *
     * @param dicomAttributes The DICOM attributes to convert to JSON.
     * @param sopInstanceUID The SOP Instance UID at the root dataset for the DICOM instance.
     * @return A JSONObject representing the DICOM attributes.
     */
    public static JSONObject fromAttributes(Attributes dicomAttributes, String sopInstanceUID) {
        JSONObject instanceObject = new JSONObject();

        Arrays.stream(dicomAttributes.tags()).forEach(tag -> {
            try {
                VR vr = dicomAttributes.getVR(tag);

                switch (vr) {
                    case SQ: {
                        // Handle DICOM sequences recursively
                        JSONArray sequenceArray = new JSONArray();
                        dicomAttributes.getSequence(tag).forEach(item -> {
                            sequenceArray.put(fromAttributes(item, sopInstanceUID));
                        });
                        JSONObject sequenceObj = new JSONObject();
                        sequenceObj.put("vr", "SQ");
                        if (sequenceArray.length() > 0) {
                            sequenceObj.put("Value", sequenceArray);
                        }
                        instanceObject.put(formatTag(tag), sequenceObj);
                        break;
                    }
                    case OB:
                    case OW:
                    case OD:
                    case OF:
                    case UN: {
                        // do not serialize these here
                        break;
                    }
                    // handle numeric values
                    case US:
                    case SS:
                    case UL:
                    case SL: {
                        int[] intValues = dicomAttributes.getInts(tag);
                        JSONArray valueArray = serializeNumberValues(intValues);
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("vr", vr.toString());
                        if (valueArray.length() > 0) {
                            valueObj.put("Value", valueArray);
                        }
                        instanceObject.put(formatTag(tag), valueObj);
                        break;
                    }
                    case FL: {
                        float[] floatValues = dicomAttributes.getFloats(tag);
                        JSONArray valueArray = serializeNumberValues(floatValues);
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("vr", vr.toString());
                        if (valueArray.length() > 0) {
                            valueObj.put("Value", valueArray);
                        }
                        instanceObject.put(formatTag(tag), valueObj);
                        break;
                    }
                    case FD: {
                        double[] doubleValues = dicomAttributes.getDoubles(tag);
                        JSONArray valueArray = serializeNumberValues(doubleValues);
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("vr", vr.toString());
                        if (valueArray.length() > 0) {
                            valueObj.put("Value", valueArray);
                        }
                        instanceObject.put(formatTag(tag), valueObj);
                        break;
                    }
                    case PN:
                        // TODO(#697): treat person names correctly as per DICOM F.2.2
                    // treat any other as strings
                    default: {
                        JSONArray valueArray = serializeStringValues(dicomAttributes.getStrings(tag));
                        JSONObject valueObj = new JSONObject();
                        valueObj.put("vr", vr.toString());
                        if (valueArray.length() > 0) {
                            valueObj.put("Value", valueArray);
                        }
                        instanceObject.put(formatTag(tag), valueObj);
                    }
                }
            } catch (JSONException e) {
                LOG.error("Error creating JSON object for tag {} in SOPInstanceUID {}", tag, sopInstanceUID, e);
            }
        });

        return instanceObject;
    }

    /** Serialize a list of textual values as an array of strings.
     *
     * @param values The list of strings to serialize (may contain null values)
     * @return A DICOM JSON array of values to be put in the "Value" field
     */
    static JSONArray serializeStringValues(String[] values) {
        JSONArray valueArray = new JSONArray();
        if (values != null) {
            for (String v : values) {
                if (v == null || v.isEmpty()) {
                    valueArray.put(JSONObject.NULL);
                } else {
                    valueArray.put(v);
                }
            }
        }
        return valueArray;
    }

    /** Serialize a list of 32-bit integer values as an array of numbers.
     *
     * @param values The list of integers to serialize
     * @return A DICOM JSON array of values to be put in the "Value" field
     */
    static JSONArray serializeNumberValues(int[] values) {
        JSONArray valueArray = new JSONArray();
        if (values != null) {
            for (int v : values) {
                valueArray.put(v);
            }
        }
        return valueArray;
    }

    /** Serialize a list of 32-bit floating point values as an array of numbers.
     *
     * @param values The list of numbers to serialize
     * @return A DICOM JSON array of values to be put in the "Value" field
     */
    static JSONArray serializeNumberValues(float[] values) {
        JSONArray valueArray = new JSONArray();
        if (values != null) {
            for (float v : values) {
                if (Float.isFinite(v)) {
                    try {
                        valueArray.put(v);
                    } catch (JSONException e) {
                        // we already checked whether the value is finite,
                        // so this should not happen
                        throw new RuntimeException("Failed to serialize float value: " + v, e);
                    }
                } else if (Float.isNaN(v)) {
                    // put "NaN"
                    valueArray.put("NaN");
                } else {
                    // infinity or -infinity
                    valueArray.put(v > 0 ? "inf" : "-inf");
                }
            }
        }
        return valueArray;
    }

    /** Serialize a list of 64-bit floating point values as an array of numbers.
     *
     * @param values The list of numbers to serialize
     * @return A DICOM JSON array of values to be put in the "Value" field
     */
    static JSONArray serializeNumberValues(double[] values) {
        JSONArray valueArray = new JSONArray();
        if (values != null) {
            for (double v : values) {
                if (Double.isFinite(v)) {
                    try {
                        valueArray.put(v);
                    } catch (JSONException e) {
                        // we already checked whether the value is finite,
                        // so this should not happen
                        throw new RuntimeException("Failed to serialize double value: " + v, e);
                    }
                } else if (Double.isNaN(v)) {
                    // put "NaN"
                    valueArray.put("NaN");
                } else {
                    // infinity or -infinity
                    valueArray.put(v > 0 ? "inf" : "-inf");
                }
            }
        }
        return valueArray;
    }

    /**
     * This method converts a DICOM tag keyword into its DICOM JSON form,
     * as necessary to return the final JSON response in DICOMWeb.
     *
     * @param keyword The DICOM tag keyword to convert (e.g. <code>SOPInstanceUID</code>)
     */
    public static String convertToDicomTag(String keyword) throws IllegalAccessException {
        int tag = tagForKeyword(keyword);
        return formatTag(tag);
    }

    /** Convert a DICOM keyword to the respective DICOM tag
     * by consulting the standard element dictionary.
     * @param keyword the DICOM keyword (e.g. <code>SOPInstanceUID</code>)
     * @return the corresponding DICOM tag in integer form
     */
    public static int tagForKeyword(String keyword) {
        return ElementDictionary.getStandardElementDictionary().tagForKeyword(keyword);
    }

    /** Convert a DICOM tag to DICOM JSON
     *
     * @param tag the DICOM tag in integer form
     * @return a string in the form <code>GGGGEEEE</code>
     */
    public static String formatTag(int tag) {
        return String.format("%04X%04X", (tag >> 16) & 0xFFFF, tag & 0xFFFF);
    }
}
