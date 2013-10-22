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

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Represents a cell of a TabNet table
 * @author Heitor Barbieri
 * date: 20130912
 */
class Cell {
    static final NumberFormat NFMT = NumberFormat.getInstance(
                                                        new Locale("pt", "BR"));
    
    private int idx; // order number of the cell inside a table
    private String title;
    private String subtitle;
    private List<String> scope;
    private List<String> header;
    private String row;
    private String value;
    private List<String> sources;
    private List<String> labels;
    private List<String> notes;
    private UrlElem elem; // low level info

    int getIdx() {
        return idx;
    }

    void setIdx(int idx) {
        this.idx = idx;
    }

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

    void setScope(final List<String> scope) {
        this.scope = scope;
    }

    List<String> getHeader() {
        return header;
    }

    void setHeader(final List<String> header) {
        this.header = header;
    }

    String getRow() {
        return row;
    }

    void setRow(final String row) {
        this.row = row;
    }

    String getValue() {
        return value;
    }

    void setValue(final String value) {
        final String nvalue = (value == null) ? null : 
                                               value.replaceAll("( +|\\+)", "");
        this.value = nvalue;
    }

    List<String> getSources() {
        return sources;
    }

    void setSources(final List<String> sources) {
        this.sources = sources;
    }

    List<String> getLabels() {
        return labels;
    }

    void setLabels(final List<String> labels) {
        this.labels = labels;
    }

    List<String> getNotes() {
        return notes;
    }

    void setNotes(final List<String> notes) {
        this.notes = notes;
    }

    UrlElem getElem() {
        return elem;
    }

    void setElem(final UrlElem elem) {
        this.elem = elem;
    }

    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        
        if ((title != null) && (!title.isEmpty())) {
            builder.append("Title:\n\t");
            builder.append(title);
        }
        if ((subtitle != null) && (!subtitle.isEmpty())) {
            builder.append("\nSubtitle:\n\t");
            builder.append(subtitle);
        }
        if ((scope != null) && (!scope.isEmpty())) {
            builder.append("\nScope:\n");
            for (String line : scope) {
                if ((line != null) && (!line.isEmpty())) {
                    builder.append("\t");
                    builder.append(line);
                }
            }
        }
        if ((header != null) && (!header.isEmpty())) {
            builder.append("\nHeader:");
            for (String line : header) {
                if ((line != null) && (!line.isEmpty())) {
                    builder.append("\n\t");
                    builder.append(line);
                }
            }
        }
        if ((row != null) && (!row.isEmpty())) {
            builder.append("\nRow:\n\t");
            builder.append(row);
        }
        if ((value != null) && (!value.isEmpty())) {
            builder.append("\nValue:\t");
            builder.append(value);
        }
        if ((sources != null) && (!sources.isEmpty())) {
            builder.append("\nSource:");
            for (String line : sources) {
                if ((line != null) && (!line.isEmpty())) {
                    builder.append("\n\t");
                    builder.append(line);
                }
            }
        }
        if ((labels != null) && (!labels.isEmpty())) {
            builder.append("\nLabel:");
            for (String line : labels) {
                if ((line != null) && (!line.isEmpty())) {
                    builder.append("\n\t");
                    builder.append(line);
                }
            }
        }
        if ((notes != null) && (!notes.isEmpty())) {
            builder.append("\nNote:");
            for (String line : notes) {
                if ((line != null) && (!line.isEmpty())) {
                    builder.append("\n\t");
                    builder.append(line);
                }
            }
        }
        if (elem != null) {
            if (elem.csv != null) {
                builder.append("\nCVS url:\n\t");
                builder.append(elem.csv);
            }
            if (elem.father != null) {
                builder.append("\nFather url:\n\t");
                builder.append(elem.father);
            }
            if (elem.tableOptions != null) {
                builder.append("\nTable options:\n\t");
                for (Map.Entry<String,String> entry : 
                                                 elem.tableOptions.entrySet()) {
                    builder.append("\n\t");
                    builder.append(entry.getKey());
                    builder.append(": ");
                    builder.append(entry.getValue());
                }
            }
            if (elem.qualifRec != null) {
                builder.append("\nQualification Record url:\n\t");
                builder.append(elem.qualifRec);
            }
        }
        return builder.toString();
    }

    String toHtml() {        
        final StringBuilder builder = new StringBuilder();

        builder.append("<!DOCTYPE html>\n");
        builder.append("<html>\n");

        builder.append("<head>\n");
        builder.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        builder.append("</head>\n");

        builder.append("<body>\n");
        
        if ((title != null) && (!title.isEmpty())) {
            builder.append("<h1>\n");
            builder.append(title);
            builder.append("</h1>\n");
        }
        if ((subtitle != null) && (!subtitle.isEmpty())) {
            builder.append("<h2>\n");
            builder.append(subtitle);
            builder.append("</h2>\n");
        }
        if ((scope != null) && (!scope.isEmpty())) {
            builder.append("<h3>\n");
            for (String line : scope) {
                if ((line != null) && (!line.isEmpty())) {
                    builder.append(line);
                    builder.append("<br/>\n");
                }
            }
            builder.append("</h3>\n");
        }

        builder.append("<p><b>Célula:</b><br/>(");
        boolean first = true;
        if ((header != null) && (!header.isEmpty())) {
            for (String line : header) {
                if ((line != null) && (!line.isEmpty())) {
                    if (first) {
                        first = false;
                    } else {
                        builder.append("| ");
                    }
                    builder.append(line);
                }
            }
        }
        builder.append("; ");
        if ((row != null) && (!row.isEmpty())) {
            builder.append(row);
        }
        builder.append("; ");
        if ((value != null) && (!value.isEmpty())) {
            try {
                builder.append(NFMT.format(NFMT.parse(value).floatValue()));
            } catch (ParseException ex) {
                builder.append(value);
            }
        }
        builder.append(")");
        builder.append("</p>\n");

        if ((sources != null) && (!sources.isEmpty())) {
            builder.append("<p><b>Fonte(s):</b>\n");
            for (String line : sources) {
                if ((line != null) && (!line.isEmpty())) {
                    builder.append("<br/>");
                    builder.append(line);
                }
            }
            builder.append("</p>\n");
        }
        if ((labels != null) && (!labels.isEmpty())) {
            builder.append("<p><b>Legenda(s):</b>\n");
            for (String line : labels) {
                if ((line != null) && (!line.isEmpty())) {
                    builder.append("<br/>");
                    builder.append(line);
                }
            }
            builder.append("</p>\n");
        }
        if ((notes != null) && (!notes.isEmpty())) {
            builder.append("<p><b>Nota(s):</b>");
            for (String line : notes) {
                if ((line != null) && (!line.isEmpty())) {
                    builder.append("<br/>");
                    builder.append(line);
                }
            }
            builder.append("</p>\n");
        }
        if (elem != null) {
            /*if (elem.csv != null) {
                builder.append("<p><b>Url da CVS:</b><br/>\n");
                builder.append("<a href=\"");
                builder.append(elem.csv);
                builder.append("\">");
                builder.append(elem.csv);
                builder.append("</a>\n</p>\n");
            }*/
            if (elem.father != null) {
                builder.append("<p><b>Url da tabela:</b><br/>\n");
                builder.append("<a href=\"");
                builder.append(elem.father);
                builder.append("\">");
                builder.append(elem.father);
                builder.append("</a>\n</p>\n");
            }
            if (elem.tableOptions != null) {
                builder.append("<p><b>Opções da tabela:</b><br/>\n");
                for (Map.Entry<String,String> entry : 
                                                 elem.tableOptions.entrySet()) {
                    builder.append(entry.getKey());
                    builder.append(": ");
                    builder.append(entry.getValue());
                    builder.append("<br/>\n");
                }
                builder.append("</p>\n");            
            }
            if (elem.qualifRec != null) {
                builder.append("<p><b>Url da Ficha de Qualificação:</b><br/>\n");
                builder.append("<a href=\"");
                builder.append(elem.qualifRec);
                builder.append("\">");
                builder.append(elem.qualifRec);
                builder.append("</a>\n</p>\n");
            }
        }

        builder.append("</body>");
        builder.append("</html>");

        return builder.toString();
    }
}
