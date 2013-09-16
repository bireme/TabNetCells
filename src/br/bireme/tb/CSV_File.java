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
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

/**
 *
 * @author Heitor Barbieri
 * date: 20130819
 */
public class CSV_File {
    
    public Table parse(final File csv,
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
    
    public Table parse(final String csv,
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
        final String[] line = lines.remove();
        table.setSubtitle(line[0].trim());
    }
    
    private void parseScope(final Deque<String[]> lines,
                            final Table table) throws IOException {
        assert lines != null;
        assert table != null;
        
        final List<String> scope = new ArrayList<>();
        
        while (true) {
            if (lines.isEmpty()) {
                throw new IOException("null scope");
            }
            final String[] line = lines.peek();
            if (!line[1].trim().isEmpty()) {
                break;
            }
            scope.add(line[0].trim());
            lines.remove();
        }
        
        table.setScope(scope);
    }
    
    private void parseHeader(final Deque<String[]> lines,
                             final Table table) throws IOException {
        assert lines != null;
        assert table != null;
                
        final List<List<String>> header = new ArrayList<>();
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
            if (line[1].trim().isEmpty()) {
                throw new IOException("invalid header: [" + line[0] + "]");
            }
            for (int idx = 1; idx < lsize; idx++) {
                List<String> lst = (header.size() < idx) ? null 
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
        
        final List<List<String>> ret = new ArrayList<>();
        final List<String> row = new ArrayList<>();
        
        while (true) {            
            if (lines.isEmpty()) {
                throw new IOException("null data");
            }
            final String[] line = lines.peek();
            if (line[1].trim().isEmpty()) {
                break;
            }
            lines.remove();
            row.add(line[0]);
            ret.add(Arrays.asList(Arrays.copyOfRange(line, 1, line.length)));
        }
        
        table.setRow(row);
        table.setLines(ret);
    }
    
    private void parseSources(final Deque<String[]> lines,
                              final Table table) throws IOException {
        assert lines != null;
        assert table != null;
            
        final List<String> sources = new ArrayList<>();
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
            final String content = line[0].trim();
            if (first) {
                if (!content.startsWith("Fonte")) {
                    throw new IOException("'Fonte' espected. [" + line[0] + "]");
                }
                final String ctt = content.substring(content.indexOf(':') + 1)
                                                                        .trim();
                if (!ctt.isEmpty()) {
                    sources.add(ctt);                
                }
                first = false;
            } else {
                if (content.startsWith("Legenda") || 
                                                   content.startsWith("Nota")) {
                    break;
                }
                sources.add(content);
            }
            lines.remove();
        }
        
        table.setSources(sources);
    }
    
    private void parseLabels(final Deque<String[]> lines,
                             final Table table) throws IOException {
        assert lines != null;
        assert table != null;
        
        final List<String> labels = new ArrayList<>();
        boolean first = true;
        
        while (true) {
            if (lines.isEmpty()) {
                break;
            }
            final String[] line = lines.peek();
            /*if (!line[1].trim().isEmpty()) {
                throw new IOException("invalid line format: [" + line[0] + "]");
            }*/
            final String content = line[0].trim();
            if (first) {
                if (!content.startsWith("Legenda")) {
                    break;
                }
                final String ctt = content.substring(content.indexOf(':') + 1)
                                                                        .trim();
                if (!ctt.isEmpty()) {
                    labels.add(ctt);                
                }
                first = false;
            } else {
                if (content.startsWith("Nota")) {
                    break;
                }
                labels.add(content);
            }
            lines.remove();
        }
        
        table.setLabels(labels);
    }
    
    private void parseNotes(final Deque<String[]> lines,
                            final Table table) throws IOException {
        assert lines != null;
        assert table != null;
        
        final List<String> notes = new ArrayList<>();
        boolean first = true;
        
        while (true) {
            if (lines.isEmpty()) {
                break;
            }
            final String[] line = lines.peek();
            final String content = line[0].trim();
            if (first) {
                if (!content.startsWith("Nota")) {
                    throw new IOException("'Nota' espected. [" + line[0] + "]");
                }
                final String ctt = content.substring(content.indexOf(':') + 1)
                                                                        .trim();
                if (!ctt.isEmpty()) {
                    notes.add(ctt);                
                }
                first = false;
            } else {
                if (content.startsWith("Nota")) {
                    break;
                }
                notes.add(content);
            }
            lines.remove();
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
