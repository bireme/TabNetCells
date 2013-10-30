/*=========================================================================

    Copyright © 2013 BIREME/PAHO/WHO

    This file is part of TabNetCells.

    TabNetCells is free software: you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public License as
    published by the Free Software Foundation, either version 2.1 of
    the License, or (at your option) any later version.

    TabNetCells is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public
    License along with TabNetCells. If not, see
    <http://www.gnu.org/licenses/>.

=========================================================================*/

package br.bireme.tb;

import au.com.bytecode.opencsv.CSVReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts a csv file into a Table object
 * @author Heitor Barbieri
 * date: 20130819
 */
class CSV_File {
    private static final Pattern LABEL_PAT =
                                          Pattern.compile("\\s*\\*+\\s*");

    /**
     * Parses a csv file and converts it into a Table object
     * @param csv comma separated value file to be parsed
     * @param encoding file encoding
     * @param separator field separator character
     * @return the Table object
     * @throws IOException 
     */
    Table parse(final File csv,
                final String encoding,
                final char separator) throws IOException {
        if (csv == null) {
            throw new NullPointerException("csv");
        }
        if (encoding == null) {
            throw new NullPointerException("encoding");
        }
        final List<String[]> myEntries;
        try (CSVReader reader = new CSVReader(new InputStreamReader(
                              new FileInputStream(csv), encoding), separator)) {
            myEntries = reader.readAll();
        }

        return parse0(myEntries);
    }

    /**
     * Parses a csv String and converts it into a Table object
     * @param csv comma separated value file to be parsed
     * @param separator field separator character
     * @return the Table object
     * @throws IOException 
     */
    Table parse(final String csv,
                final char separator) throws IOException {
        if (csv == null) {
            throw new NullPointerException("csv");
        }
        final List<String[]> myEntries;
        try (CSVReader reader =
                              new CSVReader(new StringReader(csv), separator)) {
            myEntries = reader.readAll();
        }

        return parse0(myEntries);
    }

    /**
     * Parses the csv line elements seaching for Table elements.
     * @param myEntries list of string of csv fields as returned by raw csv parser
     * @return The Table object
     * @throws IOException 
     */
    private Table parse0(final List<String[]> myEntries) throws IOException {
        assert myEntries != null;

        final Table table = new Table();
        final Deque<String[]> lines = new ArrayDeque<>();

        for (String[] line : myEntries) {
            lines.add(line);
        }

        parseTitle(lines, table);
        parseSubtile(lines, table);
        parseScope(lines, table);
        parseHeader(lines, table);
        parseData(lines, table);
        parseSources(lines, table);
        parseLabels(lines, table);
        parseNotes(lines, table);

        return table;
    }


    private void parseTitle(final Deque<String[]> lines,
                            final Table table) throws IOException {
        assert lines != null;
        assert table != null;

        if (lines.isEmpty()) {
            throw new IOException("null title");
        }
        final String[] line = lines.remove();
        table.setTitle(line[0].trim());           
    }

    private void parseSubtile(final Deque<String[]> lines,
                              final Table table) throws IOException {
        assert lines != null;
        assert table != null;

        if (lines.isEmpty()) {
            throw new IOException("null subtitle");
        }
        final String[] line = lines.peek();
        
        if (!line[0].startsWith("Período:")) {
            table.setSubtitle(line[0].trim());
            lines.remove();
        }        
    }

    private void parseScope(final Deque<String[]> lines,
                            final Table table) throws IOException {
        assert lines != null;
        assert table != null;

        final ArrayList<String> scope = new ArrayList<>();

        while (true) {
            if (lines.isEmpty()) {
                throw new IOException("null scope");
            }
            final String[] line = lines.peek();
            if ((line.length > 1) && (!line[1].trim().isEmpty())) {
                break;
            }
            scope.add(line[0].trim().replaceAll("\\:(\\d)", ": $1"));
            lines.remove();
        }

        table.setScope(scope);
    }

    private void parseHeader(final Deque<String[]> lines,
                             final Table table) throws IOException {
        assert lines != null;
        assert table != null;

        final ArrayList<ArrayList<String>> header = new ArrayList<>();
        boolean found = false;

        while (!found) {
            if (lines.isEmpty()) {
                throw new IOException("null header");
            }
            final String[] line = lines.remove();
            final int lsize = line.length;

            if (!line[0].trim().isEmpty()) {
                table.setRowHeader(line[0]);
                found = true;
            }
            if ((lsize == 1) || (line[1].trim().isEmpty())) {
                throw new IOException("invalid header: [" + line[0] + "]");
            }
            for (int idx = 1; idx < lsize; idx++) {
                ArrayList<String> lst = (header.size() < idx) ? null
                                                         : header.get(idx-1);
                if (lst == null) {
                    lst = new ArrayList<>();
                    header.add(idx-1, lst);
                }
                lst.add(line[idx]);
            }
        }

        table.setHeader(header);
    }

    private void parseData(final Deque<String[]> lines,
                           final Table table) throws IOException {
        assert lines != null;
        assert table != null;

        final ArrayList<ArrayList<String>> ret = new ArrayList<>();
        final ArrayList<String> row = new ArrayList<>();
        final Matcher mat = LABEL_PAT.matcher("");

        while (true) {
            if (lines.isEmpty()) {
                throw new IOException("null data");
            }
            final String[] line = lines.peek();
            final int lSize = line.length;
            
            if ((lSize == 1) || (line[1].trim().isEmpty())) {
                break;
            }
            lines.remove();
            row.add(line[0]);
            
            final ArrayList<String> lstLines = new ArrayList<>();
            for (int idx = 1; idx < lSize; idx++) {
                mat.reset(line[idx]);               
                if (mat.matches()) {
                    boolean obs = table.getHeader().get(idx-1).get(0)
                                                                 .equals("Obs");                    
                    if (obs) {
                        if (idx == 1) {
                            throw new IOException("Invalid *** position");
                        }     
                        final int last = lstLines.size() - 1;
                        final String aux = lstLines.get(last) 
                                                             + line[idx].trim();
                        lstLines.set(last, aux);
                    } else {
                        lstLines.add(line[idx].trim());
                    }
                } else {
                    lstLines.add(line[idx].trim());
                }
            }
            ret.add(lstLines);
        }
        
        final ArrayList<ArrayList<String>> header = new ArrayList<>();
        for (ArrayList<String> hdr : table.getHeader()) {
            if (!hdr.get(0).equals("Obs")) {
                header.add(hdr);
            }
        }
        if (header.size() != ret.get(0).size()) {
            throw new IOException("header size differs from data line size");
        }
        table.setHeader(header);
        table.setRow(row);
        table.setLines(ret);
    }

    private void parseSources(final Deque<String[]> lines,
                              final Table table) throws IOException {
        assert lines != null;
        assert table != null;

        final ArrayList<String> sources = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        boolean first = true;

        while (true) {
            if (lines.isEmpty()) {
                if (first) {
                    throw new IOException("null source");
                }
                break;
            }
            final String[] line = lines.peek();
            /*if (!line[1].trim().isEmpty()) {
                throw new IOException("invalid line format: [" + line[0] + "]");
            }*/
            String content = line[0].trim();
            if (first) {
                if (!content.startsWith("Fonte")) {
                    break;
                    //throw new IOException("'Fonte' espected. [" + line[0] + "]");
                }
                content = content.substring(content.indexOf(':') + 1).trim();
                if (!content.isEmpty()) {
                    builder.append(content);
                }
                first = false;
            } else {
                if (content.startsWith("Legenda") ||
                                                   content.startsWith("Nota")) {
                    break;
                }
                if (!content.isEmpty()) { 
                    builder.append(" ");
                    builder.append(content);
                }
            }
            if (content.endsWith(".")) {
                sources.add(builder.toString());
                builder.setLength(0);
            }
            lines.remove();
        }
        if (builder.length() > 0) {
            sources.add(builder.toString());
        }

        table.setSources(sources);
    }

    private void parseLabels(final Deque<String[]> lines,
                             final Table table) throws IOException {
        assert lines != null;
        assert table != null;

        final ArrayList<String> labels = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        boolean first = true;

        while (true) {
            if (lines.isEmpty()) {
                break;
            }
            final String[] line = lines.peek();
            /*if (!line[1].trim().isEmpty()) {
                throw new IOException("invalid line format: [" + line[0] + "]");
            }*/
            String content = line[0].trim();
            if (first) {
                if (!content.startsWith("Legenda")) {
                    break;
                }
                content = content.substring(content.indexOf(':') + 1).trim();
                if (!content.isEmpty()) {
                    builder.append(content);
                }
                first = false;
            } else {
                if (content.startsWith("Nota")) {
                    break;
                }
                if (!content.isEmpty()) { 
                    if (content.startsWith("*")) {
                        if (builder.length() > 0) {
                            labels.add(builder.toString());
                            builder.setLength(0);
                        }                        
                    } else {
                        builder.append(" ");
                    }
                    builder.append(content);
                }
            }            
            lines.remove();
        }
        if (builder.length() > 0) {
            labels.add(builder.toString());
        }

        table.setLabels(labels);
    }

    private void parseNotes(final Deque<String[]> lines,
                            final Table table) throws IOException {
        assert lines != null;
        assert table != null;

        final ArrayList<String> notes = new ArrayList<>();
        final StringBuilder builder = new StringBuilder();
        boolean first = true;        

        while (true) {
            if (lines.isEmpty()) {
                break;
            }
            final String[] line = lines.peek();
            String content = line[0].trim();
            if (first) {
                if (!content.startsWith("Nota")) {
                    throw new IOException("'Nota' espected. [" + line[0] + "]");
                }
                content = content.substring(content.indexOf(':') + 1).trim();
                if (!content.isEmpty()) {
                    builder.append(content);                    
                }
                first = false;
            } else {
                if (!content.isEmpty()) { 
                    builder.append(" ");
                    builder.append(content);
                }
            }
            if (content.endsWith(".")) {
                notes.add(builder.toString());
                builder.setLength(0);
            }
            lines.remove();
        }
        if (builder.length() > 0) {
            notes.add(builder.toString());
        }

        table.setNotes(notes);
    }        
    
    public static void main(final String[] args) throws IOException {
        final CSV_File csv = new CSV_File();
        final Table table = csv.parse(new File(
                                      "/home/heitor/Downloads/d05_06cap.csv"),
                                      //  "/home/heitor/Downloads/g07_89.csv"),
                                                              "ISO8859-1", ';');

        System.out.println(table);
    }
}
