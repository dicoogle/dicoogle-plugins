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
package dicoogle.lucene;

import dicoogle.lucene.dicom.abstraction.DicomDocument;
import dicoogle.lucene.dicom.abstraction.IDoc;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.dcm4che2.data.DicomElement;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.dcm4che2.data.VR;
import org.dcm4che2.io.DicomInputStream;
import org.dcm4che2.io.StopTagInputHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.IndexerInterface;
import pt.ua.dicoogle.sdk.StorageInputStream;
import pt.ua.dicoogle.sdk.core.DicooglePlatformInterface;
import pt.ua.dicoogle.sdk.core.PlatformCommunicatorInterface;
import pt.ua.dicoogle.sdk.datastructs.IndexReport2;
import pt.ua.dicoogle.sdk.datastructs.Report;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;
import pt.ua.dicoogle.sdk.task.ProgressCallable;
import pt.ua.dicoogle.sdk.task.Task;
import pt.ua.dicoogle.sdk.utils.TagValue;
import pt.ua.dicoogle.sdk.utils.TagsStruct;

import java.io.*;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class implements the indexing strategy of the Lucene Plugin
 *
 *
 */
public class LuceneIndexer implements IndexerInterface, PlatformCommunicatorInterface {

    //access to dicoogle platform
    private static DicooglePlatformInterface platform = null;


    private static final Logger log = LoggerFactory.getLogger(LuceneIndexer.class);
	static final String DEFAULT_INDEX_PATH = "./index/";
	static final String INDEX_PATH_DIR_SUFFIX = "indexed";
	static final String INDEX_PATH_COMPRESSED_SUFFIX = "compressed";


    private ConfigurationHolder settings;


    /**
     * where the index files will be located
     */
    private String indexFilePath;

    /**
     * lucene variables which we need to track
     */
    private Directory index;
    private Analyzer analyzer;
    
    private LuceneQuery lQuery = null;

    private final Set<String> sopInstanceUIDs = Collections.newSetFromMap(new ConcurrentHashMap<>(1024));

    /**
     * constructs an indexer instance
     */
    public LuceneIndexer() {
        this.indexFilePath = DEFAULT_INDEX_PATH;
        log.info("Created Lucene Indexer Plugin");
        
    }
    
    public final void setIndexPath(String indexPath) {
        this.indexFilePath = indexPath;

        log.debug("LUCENE: indexing at {}", indexFilePath);

        // This is required because Dicoogle may not shut plugins down properly,
        // leaving the write lock behind.
        File fileLock = new File(indexFilePath + File.separator + INDEX_PATH_DIR_SUFFIX + "/write.lock");
        if (fileLock.exists()) {
            fileLock.delete();
        }

        try {
            index = FSDirectory.open(new File(indexFilePath + INDEX_PATH_DIR_SUFFIX).toPath());
            File f = new File(indexFilePath + File.separator + INDEX_PATH_COMPRESSED_SUFFIX);
            f.mkdirs();
            analyzer = new StandardAnalyzer();

            IndexWriterConfig indexConfig = new IndexWriterConfig(analyzer)
                    .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            // this will create the index if it does not exist yet
            new IndexWriter(index, indexConfig).close();
            
        } catch (IOException ex) {
            log.error("Failed to open index", ex);
        }

        lQuery = new LuceneQuery();
        lQuery.setIndexPath(getLuceneDirectory());

    }

    public Directory getLuceneDirectory() {
        return index;
    }

    @Override
    public Task<Report> index(final StorageInputStream file, Object ... args) {
        
        return new Task<>(
                new ProgressCallable<Report>() {
                	
                    private float progress = -1.f;

                    @Override
                    public Report call() throws Exception {

                    	log.debug("Started single index task: {}", file.getURI());

                        IndexReport2 r = new IndexReport2();
                        r.started();
                        try
                        {
                            if(handles(file.getURI())) {
                                beginTransaction();
                                indexStream(file, r);
                                endTransaction();
                            }
                        } catch (Exception e) {
                            log.error("Error in last commits", e);
                            r.setnErrors(1);
                        }
                        
                        progress = 1.0f;
                        log.info("Finished Single Index Task: {},{}", (Object)this.hashCode(), r);
                        r.finished();
                        return r;
                    }

                    @Override
                    public float getProgress() {
                        return progress;
                    }
                });
    }

	@Override
	public Task<Report> index(final Iterable<StorageInputStream> files, Object ... args) {

		Task<Report> t = new Task<>(new ProgressCallable<Report>() {
			private float progress = -1.0f;

			@Override
			public Report call() throws Exception {

				log.debug("Started Index Task: {}", (Object)this.hashCode());
				IndexReport2 taskReport = new IndexReport2();
				taskReport.started();
				
				try {

					Iterator<StorageInputStream> it = files.iterator();

					beginTransaction();
					int i = 1;

					while (it.hasNext()) {
						StorageInputStream s = it.next();
                        if(!handles(s.getURI())){continue;}

						log.debug("Started Indexing: {},{},{}", (Object)this.hashCode(), (Object)i, s.getURI());
						try {
							indexStream(s, taskReport);
						} catch (Exception e) {
							log.error("ERROR Indexing: {},{},{}", (Object)this.hashCode(), (Object)i, s.getURI(), e);
							taskReport.addError();
						}
						log.info("Finished Indexing: {},{},{},{}", (Object)this.hashCode(), (Object)i, s.getURI(), taskReport);
						i++;
					}
					endTransaction();
					progress = 1.0f;
					
				} catch (Exception e) {
					log.error("ERROR in Indexing Task", e);
					taskReport.addError();
				}

                taskReport.finished();
                log.info("Finished Index Task: {},{}", (Object)this.hashCode(), taskReport);
				return taskReport;
			}

			@Override
			public float getProgress() {
				return progress;
			}
		});
		return t;
	}
	
	private int transactions=0;
	private IndexWriter writer;

	private int maxRAMBufferSize;

	private boolean enabled;
	
	private synchronized void beginTransaction() throws IOException{		
		if(transactions==0) {
            IndexWriterConfig indexConfig = new IndexWriterConfig(analyzer)
                    .setRAMBufferSizeMB(maxRAMBufferSize)
                    .setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			this.writer = new IndexWriter(index, indexConfig);
		}		
		this.transactions++;
	}
	
	private synchronized void endTransaction() throws IOException{
		this.transactions--;
		this.writer.commit();
		if(transactions==0){
			this.writer.close();
			this.writer = null;
		}
	}	
	
	private synchronized void indexStream(StorageInputStream file, IndexReport2 r) {

        try {
            IDoc idoc = this.docFromFile(file);

            if (idoc != null) {
                Document luceneDoc = idoc.toDocument();
                try {
                    // Index file size
                    StoredField fileSize = new StoredField("FileSize", file.getSize());
                    luceneDoc.add(fileSize);
                } catch (Exception e) {
                    log.warn("Failed to add file size field to document", e);
                }

                try {
                    this.writer.addDocument(luceneDoc);
                    r.addIndexFile();
                } catch (IOException ex) {
                    log.error("Failed to add document to index", ex);
                    r.addError();
                }

            }
        } catch (IOException e) {
            log.error("Failed to produce a document for the index", e);
            r.addError();
        }
	}

    /**
     * @return the constructed document, or `null` if the file was ignored
     * @exception IOException on I/O errors
     */
    private IDoc docFromFile(StorageInputStream storage) throws IOException {

        // Check whether the document already exists
        final Iterable<SearchResult> it = this.lQuery.query(new TermQuery(new Term("uri", storage.getURI().toString())));
        if (it.iterator().hasNext()) {
            log.info("File {} already exists, ignoring", storage.getURI());
        	return null;
        }
        
        TagsStruct tagStruct = TagsStruct.getInstance();

        BufferedInputStream bufferedStream = null;
        DicomInputStream dicomStream = null;
        
        try (InputStream fileStream = storage.getInputStream()) {
            bufferedStream = new BufferedInputStream(fileStream);

            dicomStream = new DicomInputStream(bufferedStream);
            dicomStream.setHandler(new StopTagInputHandler(Tag.PixelData));
            DicomObject dicomObject = dicomStream.readDicomObject();
            IDoc returnDoc = new DicomDocument();
            returnDoc.add("uri", storage.getURI().toString());
            String SOPInstanceUID = dicomObject.getString(Tag.SOPInstanceUID).trim();
            // check for collision from the local UID registry,
            // This is currently required because snapshot may not include the most
            // recently indexed items, and so could end up re-indexing the same object.
            if (sopInstanceUIDs.contains(SOPInstanceUID)) {
                log.info("SOPInstanceUID already exists, ignoring: {}", SOPInstanceUID);
                return null;
            }
            sopInstanceUIDs.add(SOPInstanceUID);
            // check for collision by SOPInstanceUID from the database's last snapshot
            final Iterable<?> it2 = this.lQuery.query(new TermQuery(new Term("SOPInstanceUID", SOPInstanceUID)));
            if (it2.iterator().hasNext()) {
            	log.info("SOPInstanceUID already exists, ignoring: {}", SOPInstanceUID);
                return null;
            }

            for (TagValue tag : tagStruct.getDIMFields()) {
                String data;
                DicomElement e = dicomObject.get(tag.getTagNumber());
                if (e != null) {
                    data = getValue(e);
                } else {
                    data = "";
                }
                addField(returnDoc, dicomObject.vrOf(tag.getTagNumber()), tag.getAlias(), data);
            }


            if (tagStruct.isModalityEnable(dicomObject.getString(Tag.Modality).trim()) || tagStruct.isIndexAllModalitiesEnabled()) {
                fetchAllContent(returnDoc, dicomObject, tagStruct.isDeepSearchModalitiesEnabled());
            }

            return returnDoc;
        } catch (IOException ex) {
            throw ex;
        } finally {
            if (dicomStream != null) dicomStream.close();
            if (bufferedStream != null) bufferedStream.close();
        }
    }

    @Override
    public String getName() {
        return "lucene";
    }

    @Override
    public boolean enable() {
        return true;
    }

    @Override
    public boolean disable() {
        return false;
    }

    @Override
    public boolean isEnabled() {
        return this.enabled;
    }

    @Override
    public void setSettings(ConfigurationHolder settings) {
    	
    	this.settings = settings;
        //loadDefaults
        
        XMLConfiguration cnf = this.settings.getConfiguration();
        
        cnf.setThrowExceptionOnMissing(true);

        try {
			this.enabled = cnf.getBoolean("indexer.enabled");
		} catch (NoSuchElementException ex) {
			this.enabled = true;
			cnf.setProperty("indexer.enabled", this.enabled);
		}
        
		try {
			this.maxRAMBufferSize = cnf.getInt("indexer.maxRAMBufferSize");
		} catch (NoSuchElementException ex) {
			this.maxRAMBufferSize = 255;
			cnf.setProperty("indexer.maxRAMBufferSize", this.maxRAMBufferSize);
		}
        
        this.setIndexPath(cnf.getString("indexer.path", DEFAULT_INDEX_PATH));
		
		try {
			cnf.save();

		} catch (ConfigurationException ex) {
			log.warn("Failed to save configuration", ex);
		}
		
		log.debug("Loaded lucene plugin configurations");
    }

    @Override
    public ConfigurationHolder getSettings() {
        return settings;
    }

    public static boolean isBinaryField(VR vr) {
        return vr == VR.SS || vr == VR.US || vr == VR.SL || vr == VR.UL || vr == VR.FL ||vr == VR.FD;
    }

    public static String getValue(DicomElement element) {

        if (!isBinaryField(element.vr())) {
            String value = null;
            Charset utf8charset = Charset.forName("UTF-8");
            Charset iso88591charset = Charset.forName("iso8859-1");
            byte[] values = element.getBytes();
            ByteBuffer inputBuffer = ByteBuffer.wrap(values);

            // decode UTF-8
            CharBuffer data = iso88591charset.decode(inputBuffer);

            // encode ISO-8559-1
            ByteBuffer outputBuffer = utf8charset.encode(data);

            byte[] outputData = outputBuffer.array();
            try {
                value = new String(outputData, "UTF-8").trim();
            } catch (UnsupportedEncodingException ex) {
                log.error("Failed to retrieve element data", ex);
            }
            /*
             * 
             *  ASCII         "ISO_IR 6"    =>  "UTF-8"
             UTF-8         "ISO_IR 192"  =>  "UTF-8"
             ISO Latin 1   "ISO_IR 100"  =>  "ISO-8859-1"
             ISO Latin 2   "ISO_IR 101"  =>  "ISO-8859-2"
             ISO Latin 3   "ISO_IR 109"  =>  "ISO-8859-3"
             ISO Latin 4   "ISO_IR 110"  =>  "ISO-8859-4"
             ISO Latin 5   "ISO_IR 148"  =>  "ISO-8859-9"
             Cyrillic      "ISO_IR 144"  =>  "ISO-8859-5"
             Arabic        "ISO_IR 127"  =>  "ISO-8859-6"
             Greek         "ISO_IR 126"  =>  "ISO-8859-7"
             Hebrew        "ISO_IR 138"  =>  "ISO-8859-8"
             */
            return value;
        }

        if (element.vr() == VR.FD && element.getBytes().length == 8) {
            double tmpValue = element.getDouble(true);
            return String.valueOf(tmpValue);
        }

        if (element.vr() == VR.FL && element.getBytes().length == 4) {
            float tmpValue = element.getFloat(true);
            return String.valueOf(tmpValue);
        }

        if (element.vr() == VR.UL && element.getBytes().length == 4) {
            long tmpValue = element.getInt(true);
            return String.valueOf(tmpValue);
        }

        if (element.vr() == VR.US && element.getBytes().length == 2) {
            short[] tmpValue = element.getShorts(true);
            return String.valueOf(tmpValue[0]);
        }

        if (element.vr() != VR.US) {
            long tmpValue = byteArrayToInt(element.getBytes());
            return String.valueOf(tmpValue);
        }

        int tmpValue = element.getInt(true);
        return String.valueOf(tmpValue);
    }

    /**
     * Convert the byte array to an int.
     *
     * @param b The byte array
     * @return The integer
     */
    private static long byteArrayToInt(byte[] b) {
        return byteArrayToInt(b, 0);
    }

    /**
     * Convert the byte array to an int starting from the given offset.
     *
     * @param b The byte array
     * @param offset The array offset
     * @return The integer
     */
    private static long byteArrayToInt(byte[] b, int offset) {
        long value = 0;
        for (int i = 0; i < b.length; i++) {
            int shift = (4 - 1 - i) * 8;
            value += (b[i + offset] & 0x000000FF) << shift;
        }
        return value;
    }

    private void addField(IDoc docToAdd, VR vr, String tag, String value) {
        if (docToAdd == null) {
            return;
        }
        if (tag == null) {
            return;
        }
        if (value == null) {
            value = "";
        }

        if (vr == VR.IS || vr == VR.US || vr == VR.SS) {
            try {
                long v = Long.parseLong(value);
                docToAdd.add(tag, v);
            } catch (NumberFormatException ex) {
                docToAdd.add(tag, value);
            }
        } else if (vr == VR.DS || vr == VR.FL || vr == VR.FD) {
            try {
                float v = Float.parseFloat(value);
                docToAdd.add(tag, v);
            } catch (NumberFormatException ex) {
                docToAdd.add(tag, value);
            }
        } else {
            docToAdd.add(tag, value);
        }
    }

    /**
     * Retrieve all textual content of a DICOM object, storing attributes in the given document
     * and returning.
     * This is used for the others query field, which allows free text queries.
     *
     * @param doc the document object for indexing
     * @param obj the DICOM object to scan
     * @param deepSearch
     * @return a 2 element list
     */
    private void fetchAllContent(IDoc doc, DicomObject obj, boolean deepSearch) {
        StringBuilder sb = new StringBuilder();
        getRecursiveDicomElement(sb, doc, obj, "", deepSearch);
        doc.add("others", sb.toString());
    }

    /**
     * Recursively retrieve all textual content of a DICOM object, storing it in the given document and
     * accumulating values into the given string builder.
     * This is used for the others query field, which allows free text queries.
     *
     * @param acc the accumulator for all DICOM content (to be indexed as "others")
     * @param doc the document object for indexing
     * @param obj the DICOM object to scan
     * @param prefix the prefix at this level
     * @param deepSearch
     * @return the concatenated list of tag names
     */
    private String getRecursiveDicomElement(StringBuilder acc, IDoc doc, DicomObject obj, String prefix, boolean deepSearch) {

        // Hard heuristic just to be sure that application will be not running forever
        if (prefix.length() > 512) {
            return "";
        }

        Map<String, String> sequences = new HashMap<>();
        String tagList = "";

        TagsStruct tagstruct = TagsStruct.getInstance();

        Iterator<DicomElement> it = obj.iterator();
        while (it.hasNext()) {
            DicomElement dcm = it.next();
            int tmpTag = dcm.tag();
            TagValue tag = tagstruct.getTagValue(tmpTag);
            
            boolean index = deepSearch || tagstruct.isOtherField(tag);
            
            if(tag != null && index){
            	
                String tagName = tag.getAlias();

                if (dcm.hasItems()) {
                    String prefixAux = prefix + tagName + "_";

                    if (dcm.countItems() > 0) {
                        acc.append(' ');
                        String tags = getRecursiveDicomElement(acc, doc,
                                dcm.getDicomObject(0), prefixAux, deepSearch);
                        tagList = tagList + " " + tags;
                        sequences.put(tagName, tags);
                    }
                } /*
                 * Drop the non-search-valid fields (Pixel data etc)
                 */ else if (dcm.vr() != VR.OB && dcm.vr() != VR.OW && !tagName.equals("?")) {
                    String value = getValue(dcm);

                    if (value != null) {
                        tagList = tagList + " " + prefix + tagName;
                        addField(doc, dcm.vr(), prefix + tagName, value);
                        acc.append(' ').append(value);
                    }
                }
            	
            }            
        }

        // There is a sequence
        for (Map.Entry<String, String> e : sequences.entrySet()) {
            addField(doc, VR.ST, e.getKey(), e.getValue());
        }

        return tagList;
    }

    @Override
    public boolean unindex(URI uri) {
        Query q = null;
        try {

            // could not make this work without a query parser
            QueryParser parser = new QueryParser( "uri", analyzer);
            String s = uri.toString().replace("/", "\\/");

            try {
                q = parser.parse("uri:\"" + s + "\"");
            } catch (ParseException ex) {
                log.error("Failed to parse query due to bad URI: {}", uri);
                return false;
            }


            log.debug("Query: {}", q);
            beginTransaction();
            this.writer.deleteDocuments(q);
            endTransaction();
            return true;
        } catch (IOException ex) {
            log.error("Failed to unindex, attempting close", ex);
            if (this.writer != null) {
                try {
                    this.writer.close();
                } catch (IOException ex1) {
                    log.error(ex1.getMessage(), ex1);
                }
                this.writer = null;
            }
            return false;
        }
    }

    @Override
    public void setPlatformProxy(DicooglePlatformInterface core) {
        platform = core;
    }

    @Override
    public boolean handles(URI path) {
        final String s = path.toString();

        // specific case not captured by the code below
        // (some DICOM datasets have a preview NIFTI header file)
        if (s.endsWith(".nii.gz")) return false;

        int indexExt = s.lastIndexOf('.');
        if(indexExt == -1) return true; //a lot of dicom files have no extension
        
        String extension = s.substring(indexExt);
        switch(extension.toLowerCase()){
            case ".jpg":    //these are not indexed
            case ".png":
            case ".gif":
            case ".bmp":
            case ".tiff":
            case ".jpeg":
                return false;
            
            case ".dicom":  //these are
            case ".dcm": return true;
        }
        
        // the previous behavior was to index everything anyway
        return true;
    }

}
