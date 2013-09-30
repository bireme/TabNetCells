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
import java.util.Map;

/**
 *
 * @author Heitor Barbieri
 * date: 20130912
 */
public class Cell {
    private int idx;
    private String title;
    private String subtitle;
    private List<String> scope;
    private List<String> header;
    private String row;
    private String value;
    private List<String> sources;
    private List<String> labels;
    private List<String> notes;
    private URLS.UrlElem elem;

    public int getIdx() {
        return idx;
    }

    public void setIdx(int idx) {
        this.idx = idx;
    }

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

    public List<String> getHeader() {
        return header;
    }

    public void setHeader(final List<String> header) {
        this.header = header;
    }

    public String getRow() {
        return row;
    }

    public void setRow(final String row) {
        this.row = row;
    }

    public String getValue() {
        return value;
    }

    public void setValue(final String value) {
        this.value = value;
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

    public URLS.UrlElem getElem() {
        return elem;
    }

    public void setElem(final URLS.UrlElem elem) {
        this.elem = elem;
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
        builder.append("\nHeader:");
        for (String line : header) {
            builder.append("\n\t");
            builder.append(line);
        }

        builder.append("\nRow:\n\t");
        builder.append(row);

        builder.append("\nValue:\t");
        builder.append(value);

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
        if (elem != null) {
            if (elem.csv != null) {
                builder.append("\nCVS url:\n\t");
                builder.append(elem.csv);
            }
            if (elem.father != null) {
                builder.append("\nFather url:\n\t");
                builder.append(elem.father);
            }
            if (elem.qualifRec != null) {
                builder.append("\nQualification Record url:\n\t");
                builder.append(elem.qualifRec);
            }
        }
        return builder.toString();
    }

    public String toHtml() {
        final StringBuilder builder = new StringBuilder();

        builder.append("<!DOCTYPE html>\n");
        builder.append("<html>\n");

        builder.append("<head>\n");
        builder.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
        builder.append("</head>\n");

        builder.append("<body>\n");
        builder.append("<h1>\n");
        builder.append(title);
        builder.append("</h1>\n");
        
        if (!subtitle.isEmpty()) {
            builder.append("<h2>\n");
            builder.append(subtitle);
            builder.append("</h2>\n");
        }
        if (!scope.isEmpty()) {
            builder.append("<h3>\n");
            for (String line : scope) {
                builder.append(line);
                builder.append("<br/>\n");
            }
            builder.append("</h3>\n");
        }

        builder.append("<p><b>Célula:</b><br/>(");
        boolean first = true;
        for (String line : header) {
            if (first) {
                first = false;
            } else {
                builder.append("| ");
            }
            builder.append(line);
        }

        builder.append("; ");
        builder.append(row);

        builder.append("; ");
        builder.append(value);
        builder.append(")");
        builder.append("</p>\n");

        if ((sources != null) && (!sources.isEmpty())) {
            builder.append("<p><b>Fonte(s):</b>\n");
            for (String line : sources) {
                builder.append("<br/>");
                builder.append(line);
            }
            builder.append("</p>\n");
        }
        if ((labels != null) && (!labels.isEmpty())) {
            builder.append("<p><b>Label(s):</b>\n");
            for (String line : labels) {
                builder.append("<br/>");
                builder.append(line);
            }
            builder.append("</p>\n");
        }
        if ((notes != null) && (!notes.isEmpty())) {
            builder.append("<p><b>Nota(s):</b>");
            for (String line : notes) {
                builder.append("<br/>");
                builder.append(line);
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
