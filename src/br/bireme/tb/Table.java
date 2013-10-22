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

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a TabNet table of data
 * @author Heitor Barbieri
 * date 20130911
 */
class Table {
    private String title;
    private String subtitle;
    private ArrayList<String> scope;
    private String rowHeader; // label at rowXcolumn
    private ArrayList<ArrayList<String>> header; // label of each column
    private ArrayList<String> row; // label of each data line
    private ArrayList<ArrayList<String>> lines; // line of the table data
    private ArrayList<String> sources; // table other info
    private ArrayList<String> labels;  // table other info
    private ArrayList<String> notes;   // table other info

    String getTitle() {
        return title;
    }

    void setTitle(final String title) {
        this.title = title;
    }

    String getSubtitle() {
        return subtitle;
    }

    void setSubtitle(final String subtitle) {
        this.subtitle = subtitle;
    }

    List<String> getScope() {
        return scope;
    }

    void setScope(final ArrayList<String> scope) {
        this.scope = scope;
    }

    String getRowHeader() {
        return rowHeader;
    }

    void setRowHeader(final String rowHeader) {
        this.rowHeader = rowHeader;
    }

    List<String> getRow() {
        return row;
    }

    void setRow(final ArrayList<String> row) {
        this.row = row;
    }

    ArrayList<ArrayList<String>> getHeader() {
        return header;
    }

    void setHeader(final ArrayList<ArrayList<String>> header) {
        this.header = header;
    }

    ArrayList<ArrayList<String>> getLines() {
        return lines;
    }

    void setLines(final ArrayList<ArrayList<String>> lines) {
        this.lines = lines;
    }

    List<String> getSources() {
        return sources;
    }

    void setSources(final ArrayList<String> sources) {
        this.sources = sources;
    }

    List<String> getLabels() {
        return labels;
    }

    void setLabels(final ArrayList<String> labels) {
        this.labels = labels;
    }

    List<String> getNotes() {
        return notes;
    }

    void setNotes(final ArrayList<String> notes) {
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
