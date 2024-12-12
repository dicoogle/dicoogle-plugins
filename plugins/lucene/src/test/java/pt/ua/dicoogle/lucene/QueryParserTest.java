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
package pt.ua.dicoogle.lucene;

import dicoogle.lucene.GenericQueryParser;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRefBuilder;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Eduardo Pinho <eduardopinho@ua.pt>
 */
public class QueryParserTest {

    private Analyzer analyzer;
    @Before
    public void init() {
        analyzer = new StandardAnalyzer();
    }

    @Test
    public void test1() throws ParseException {
        String queryText = "MyStringS:Float:[1 TO 10]";

        Query expected = FloatPoint.newRangeQuery("_point_MyStringS", 1, 10);

        GenericQueryParser parser = new GenericQueryParser("others", analyzer);

        assertEquals(expected, parser.parse(queryText));
    }

    @Test
    public void test2() throws ParseException {
        String queryText = "AccessionNumber:Int:[1 TO 10]";

        Query expected = FloatPoint.newRangeQuery("_point_AccessionNumber", 1, 10);

        GenericQueryParser parser = new GenericQueryParser(analyzer);

        assertEquals(expected, parser.parse(queryText));
    }

    @Test
    public void test3() throws ParseException {
        String queryText = "Something:Numeric:[0.25 TO 0.75]";

        Query expected = FloatPoint.newRangeQuery("_point_Something", 0.25f, 0.75f);

        GenericQueryParser parser = new GenericQueryParser(analyzer);

        assertEquals(expected, parser.parse(queryText));
    }

    @Test
    public void test4() throws ParseException {
        String queryText = "StudyDate:[20100118 TO 20100328]";
        BytesRefBuilder low = new BytesRefBuilder();
        low.copyChars("20100118");
        BytesRefBuilder high = new BytesRefBuilder();
        high.copyChars("20100328");
        Query expected = new TermRangeQuery("StudyDate", low.toBytesRef(), high.toBytesRef(), true, true);

        GenericQueryParser parser = new GenericQueryParser(analyzer);

        assertEquals(expected, parser.parse(queryText));
    }

    @Test
    public void test5() throws ParseException {
        String queryText = "PatientName:Salvador AND StudyDate:[20100118 TO 20100328]";
        BytesRefBuilder low = new BytesRefBuilder();
        low.copyChars("20100118");
        BytesRefBuilder high = new BytesRefBuilder();
        high.copyChars("20100328");

        Query expected = new BooleanQuery.Builder()
                // text field indexing is case and symbol insensitive, it seems...
                .add(new TermQuery(new Term("PatientName", "salvador")), BooleanClause.Occur.MUST)
                .add(new TermRangeQuery("StudyDate", low.toBytesRef(), high.toBytesRef(), true, true), BooleanClause.Occur.MUST)
                .build();

        GenericQueryParser parser = new GenericQueryParser(analyzer);

        assertEquals(expected.getClass(), parser.parse(queryText).getClass());
        assertEquals(expected, parser.parse(queryText));
    }
}
