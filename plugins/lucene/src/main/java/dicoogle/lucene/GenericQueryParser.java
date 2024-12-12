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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.FloatPoint;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A query parser that is also compatible with numeric ranges.
 *
 * This allows terms like "MyField:Int:[0 TO 200]" to become allowed, as well as
 * "MyField:Float:[1 TO 2]".
 *
 */
public class GenericQueryParser extends QueryParser
{
    private static final Pattern QUERY_TERM_PATTERN = Pattern.compile("([a-zA-Z_0-9]*:(Float|Numeric|Int):)+");
    private Set<String> numericTags;

    public GenericQueryParser(){
        super("others", new StandardAnalyzer());
    }

    public GenericQueryParser(Analyzer a){
        super("others", a);
    }

    public GenericQueryParser(String field, Analyzer a){
        super(field, a);
    }

    @Override
    public Query parse(String query) throws ParseException {
        Matcher matcher = QUERY_TERM_PATTERN.matcher(query);
        this.numericTags = new HashSet<>();
        while (matcher.find()) {
            String field = matcher.group().split(":")[0];
            numericTags.add(field);
        }

        query = query.replace("Float:", "");
        query = query.replace("Int:", "");
        query = query.replace("Numeric:", "");
        return super.parse(query);
    }

    @Override
    protected Query getRangeQuery(String field, String low, String high, boolean startInclusive, boolean endInclusive) throws ParseException
    {
        if (numericTags.contains(field))
        {
            final String pointField = "_point_" + field;
            return FloatPoint.newRangeQuery(pointField,
                    Float.parseFloat(low),
                    Float.parseFloat(high));
        }

        return super.getRangeQuery(field, low, high, startInclusive, endInclusive);
    }


 /*   public static void main(String [] args)
    {
        Pattern pattern = Pattern.compile("([a-zA-Z0-9]*:Float:)+");
        Matcher matcher = pattern.matcher(" PatientName:A* OR  MyString2:Float:[20 TO 10] AND PatientName:A* OR  MyStringS:Float:[20 TO 10]" );
        String str ="  PatientName:A* OR  MyString2:Float:[20 TO 10] AND PatientName:A* OR  MyStringS:Float:[20 TO 10]";
        //System.out.println(str.replace(":Float", ""));

        while (matcher.find())
        {
            System.out.println(matcher.group());
            System.out.println(matcher.start());
            System.out.println(matcher.end());
            String field = matcher.group().split(":")[0];
            System.out.println("Found" + field);
        }
    }*/

}
