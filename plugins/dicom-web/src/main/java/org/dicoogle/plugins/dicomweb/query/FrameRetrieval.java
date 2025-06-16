package org.dicoogle.plugins.dicomweb.query;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.io.DicomInputStream;
import org.dicoogle.plugins.dicomweb.DicomJson;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;

import org.json.JSONObject;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.task.Task;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class FrameRetrieval  implements PlatformCommunicatorInterface {

    public static final String FRAME_RETRIEVAL = "frame_retrieval";
    private DicooglePlatformInterface platform;


    public void process(HttpServletRequest request, HttpServletResponse resp) throws IOException {
        File jpegFile = new File("/path/to/image.jpg");

        String path = request.getPathInfo();  // e.g., /1.2.3/series/4.5.6/metadata

        String[] parts = path.split("/");
        String studyUID = parts[1];
        String seriesUID = parts[3];
        String sopUID = parts[5];
        String frame = parts[7];
        String boundary = "----Boundary_" + UUID.randomUUID();


        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("multipart/related; type=\"image/jpeg\"; boundary=" + boundary);

        int frameIndex = 0; // 0-based index for the frame
        frameIndex = Integer.parseInt(frame) - 1;
        HashMap<String, String> extraFields = new HashMap<>();

        extraFields.put("SOPInstanceUID", "SOPInstanceUID");
        Iterable<SearchResult> rr = null;
        try {
            // Use the UID in the query if necessary.
            Task<Iterable<SearchResult>> result = this.platform.query("lucene", "SOPInstanceUID:" + sopUID, extraFields);
            rr = result.get();
        } catch (InterruptedException | ExecutionException ex) {
        }
        URI uri = null;
        if (rr != null) {
            for (SearchResult sr : rr) {
                uri = sr.getURI();

            }
        }
        OutputStream out = resp.getOutputStream();
        //DicomFrameExtractor.extract(new File(uri.toURL().getPath()));
        InputStream stream =  uri.toURL().openStream();
        ByteArrayOutputStream arr = Convert2PNG.DICOM2PNGStream( stream, frameIndex);
        // Start multipart body
        writeBoundary(out, false, boundary);
        writeHeaders(out, "image/jpeg");
        out.write(arr.toByteArray());
        // Write JPEG binary content
        out.write("\r\n".getBytes());

        // End boundary
        writeBoundary(out, true, boundary);



        /*if (uri != null) {
            // Start multipart body
            writeBoundary(out, false, boundary);
            writeHeaders(out, "image/jpeg");

            // Write JPEG binary content
            copyStream(jpegInput, out);
            out.write("\r\n".getBytes());

            // End boundary
            writeBoundary(out, true, boundary);
        }*/
    }




    private void writeBoundary(OutputStream out, boolean isFinal, String BOUNDARY) throws IOException {
        if (isFinal) {
            out.write(("--" + BOUNDARY + "--\r\n").getBytes());
        } else {
            out.write(("--" + BOUNDARY + "\r\n").getBytes());
        }
    }

    private void writeHeaders(OutputStream out, String contentType) throws IOException, IOException {
        out.write(("Content-Type: " + contentType + "\r\n").getBytes());
        out.write("Content-Transfer-Encoding: binary\r\n".getBytes());
        out.write("\r\n".getBytes());
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = in.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
        }
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface dicooglePlatformInterface) {
        this.platform = dicooglePlatformInterface;
    }
}

