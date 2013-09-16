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

import java.util.List;

/**
 *
 * @author Heitor Barbieri
 * date 20130911
 */
public class Table {
    private String title;
    private String subtitle;
    private List<String> scope;   
    private String rowHeader;
    private List<List<String>> header;
    private List<String> row;
    private List<List<String>> lines;
    private List<String> sources;
    private List<String> labels;
    private List<String> notes;

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(final String subtitle) {
        this.subtitle = subtitle;
    }

    public List<String> getScope() {
        return scope;
    }

    public void setScope(final List<String> scope) {
        this.scope = scope;
    }

    public String getRowHeader() {
        return rowHeader;
    }

    public void setRowHeader(final String rowHeader) {
        this.rowHeader = rowHeader;
    }

    public List<String> getRow() {
        return row;
    }

    public void setRow(final List<String> row) {
        this.row = row;
    }
    
    public List<List<String>> getHeader() {
        return header;
    }

    public void setHeader(final List<List<String>> header) {
        this.header = header;
    }

    public List<List<String>> getLines() {
        return lines;
    }

    public void setLines(final List<List<String>> lines) {
        this.lines = lines;
    }

    public List<String> getSources() {
        return sources;
    }

    public void setSources(final List<String> sources) {
        this.sources = sources;
    }

    public List<String> getLabels() {
        return labels;
    }

    public void setLabels(final List<String> labels) {
        this.labels = labels;
    }

    public List<String> getNotes() {
        return notes;
    }

    public void setNotes(final List<String> notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Title:\n\t");
        builder.append(title);
        if (!subtitle.isEmpty()) {
            builder.append("\nSubtitle:\n\t");
            builder.append(subtitle);
        }
        if (!scope.isEmpty()) {
            builder.append("\nScope:\n");
            for (String line : scope) {
                builder.append("\t");
                builder.append(line);
            }
        }                        
        builder.append("\nHeader:\n");        
        for (List<String> hrow : header) {
            boolean first = true;
            builder.append("(");
            for (String line : hrow) {                
                if (first) {
                    first = false;
                } else {
                    builder.append(",");
                }
                builder.append(line);
            }
            builder.append(")\t");            
        }

        builder.append("\nrowHeader:\n\t");            
        builder.append(rowHeader);

        builder.append("\nRow:\n");
        for (String line : row) {
            builder.append("\t");
            builder.append(line);
        }
        builder.append("\nLine:");
        for (List<String> line : lines) {
            builder.append("\n");
            for (String cel : line) {
                builder.append("\t");
                builder.append(cel);
            }
        }
        if ((sources != null) && (!sources.isEmpty())) {
            builder.append("\nSource:");
            for (String line : sources) {
                builder.append("\n\t");
                builder.append(line);
            }
        }
        if ((labels != null) && (!labels.isEmpty())) {
            builder.append("\nLabel:");
            for (String line : labels) {
                builder.append("\n\t");
                builder.append(line);
            }
        }
        if ((notes != null) && (!notes.isEmpty())) {
            builder.append("\nNote:");
            for (String line : notes) {
                builder.append("\n\t");
                builder.append(line);
            }
        }
        return builder.toString();
    }    
}
