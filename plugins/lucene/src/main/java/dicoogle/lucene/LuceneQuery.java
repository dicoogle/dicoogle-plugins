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

import java.io.IOException;
import java.util.*;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;

import dicoogle.lucene.query.ShardResultStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pt.ua.dicoogle.sdk.QueryInterface;
import pt.ua.dicoogle.sdk.datastructs.SearchResult;
import pt.ua.dicoogle.sdk.settings.ConfigurationHolder;

/**
 * Implementation of the Query Plugin.
 *
 *
 */
public class LuceneQuery implements QueryInterface{

    private static final Logger logger = LoggerFactory.getLogger(LuceneQuery.class);

	private ConfigurationHolder settings;
    private Directory indexDir;
    private Analyzer analyzer = new StandardAnalyzer();
    
    private volatile DirectoryReader reader;

    public LuceneQuery() {
    }
    
    protected DirectoryReader reloadedReader() throws IOException {
        if (this.reader == null) {
            this.reader = DirectoryReader.open(this.indexDir);
            logger.debug("New Reader: {}", reader);
        } else {
            DirectoryReader nreader = DirectoryReader.openIfChanged(reader);
            if(nreader != null) {
                this.reader = nreader;
                logger.debug("New Reader: {}", reader);
            }
        }
        return this.reader;
    }
    
    protected DirectoryReader reader() throws IOException {
        if (this.reader == null) {
            this.reader = DirectoryReader.open(indexDir);
            logger.debug("Reader: {}", reader);
        }
        return this.reader;
    }
    
    public void setIndexPath(Directory index) {
        this.indexDir = index;
        this.reader = null;
    }
    
	@Override
	public Iterable<SearchResult> query(String query, Object... parameters) {
		long time = System.currentTimeMillis();
		
        if (this.indexDir == null) {
            logger.warn("Query was attempted before settings were initialized");
            return Collections.emptyList();
        }
        
		//Check for changes in the reader;
        try {
    	    this.reader = reloadedReader();
        } catch (IOException ex) {
            logger.warn("Trying to open index file", ex);
            return Collections.emptyList();
        }
	    
		GenericQueryParser parser = new GenericQueryParser("others", analyzer);
		parser.setAllowLeadingWildcard(true);
		AllDocCollector collector = new AllDocCollector();
		
		Query queryObject;
		try {
			queryObject = parser.parse(query);
		} catch (ParseException e) {
			logger.error("Error parsing query", e);
			return Collections.emptyList();
		}
		
        Iterable<SearchResult> rs = justQuery(queryObject, parameters);

		time = System.currentTimeMillis() - time;
		logger.info("Finished opening result stream, Query: {},{},{}",
                (Object)collector.getHits().size(), (Object)time, query);
		
		return rs;
	}

    public Iterable<SearchResult> query(Query query, Object... parameters) {
        if (this.indexDir == null) {
            logger.warn("Query was attempted before settings were initialized");
            return Collections.emptyList();
        }

        long time = System.currentTimeMillis();

        //Check for changes in the reader;
        try {
            this.reader = reloadedReader();
        } catch (IOException ex) {
            logger.warn("Trying to open index file", ex);
            return Collections.emptyList();
        }

        Iterable<SearchResult> rs = justQuery(query, parameters);
        time = System.currentTimeMillis() - time;
        logger.info("Finished opening result stream in {} ms", time);

        return rs;
    }

    private Iterable<SearchResult> justQuery(Query query, Object... parameters) {
        IndexSearcher searcher = new IndexSearcher(reader);
        HashMap<String, Object> extrafields = null;
        //AllDocCollector collector = new AllDocCollector();
        if (parameters.length > 0 && parameters[0] instanceof HashMap)
            extrafields = (HashMap<String, Object>) parameters[0];
        return new ShardResultStream(searcher, query, extrafields);
    }

    @Override
    public String getName() {return "lucene";}

    @Override
    public boolean enable() {return true;}

    @Override
    public boolean disable() {return false;}

    @Override
    public boolean isEnabled() {return true;}

    @Override
    public void setSettings(ConfigurationHolder settings) {
        this.settings = settings;
    }

    @Override
    public ConfigurationHolder getSettings() 
    {
        return settings;
    }
}
