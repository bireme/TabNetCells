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

import static br.bireme.tb.Cell.NFMT;
import static br.bireme.tb.URLS.EDITION_PAT;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringEscapeUtils;

/**
 * Fill the template file holes with the TabNetCell cell data
 * @author Heitor Barbieri
 * date 20131002
 */
class RIPSA {
    private static final String TEMPLATE_FILE = "template/template.html";
    private static String TEMPLATE = null;
    
    // Reads the template file only once
    static {
        final StringBuilder builder = new StringBuilder();
        final Path path = FileSystems.getDefault().getPath("", TEMPLATE_FILE);
        final List<String> lines;
        try {
            lines = Files.readAllLines(path, StandardCharsets.UTF_8);
            for (String line : lines) {
                builder.append(line);
                builder.append("\n");
            }
            TEMPLATE = builder.toString();
        } catch (IOException ex) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
                                                   .log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * Given a cell, uses its info tho fill the holes of the template String.
     * @param cell a cell having the data to fill the holes
     * @return a String with holes replaced
     * @throws IOException 
     */
    static String cell2html(final Cell cell) throws IOException {
        assert cell != null;
        
        if (TEMPLATE == null) {
            throw new IOException("TEMPLATE is null");
        }
        final String qualifRec = cell.getElem().qualifRec.toString();
        final Matcher mat = EDITION_PAT.matcher(qualifRec);
        if (!mat.find()) {
            throw new IOException("out of pattern url [" + qualifRec + "]");
        }
        final String edition = mat.group(2);
        final String father = cell.getElem().father.toString();
        final Matcher matf = Pattern.compile("idb(\\d{4})").matcher(father);
        if (!matf.find()) {
            throw new IOException("out of pattern url [" + father + "]");
        }
        final String year = matf.group(1);
        
        String str = TEMPLATE;
        final StringBuilder builder = new StringBuilder();
        boolean first;
        
        final String title = cell.getTitle();
        final String title2 = (title == null) ? "" : title;
        str = str.replace("$$title$$", title2);
        
        final String subtitle = cell.getSubtitle();
        if ((subtitle != null) && (!subtitle.isEmpty())) {
            str = str.replace("$$subtitle$$", "<h2>" + subtitle + "</h2>");
        } else {
            str = str.replace("$$subtitle$$", "");
        }
        
        str = str.replace("$$description$$", "Células IDB - " + edition + " - " 
                                   + year + " - " + title2 + " - " + subtitle);
        
        final StringBuilder aux = new StringBuilder(title);
        for (String opt : cell.getElem().tableOptions.values()) {
            if ((!opt.equals("Não ativa")) && 
                (!opt.equals("Todas as categorias"))) {
                aux.append(", ");
                aux.append(StringEscapeUtils.unescapeHtml4(opt));
            }
        }
        str = str.replace("$$keywords$$", aux.toString());
        
        final List<String> scope = cell.getScope();
        builder.setLength(0);
        first = true;
        if ((scope != null) && (!scope.isEmpty())) {
            builder.append("<h3>");
            for (String scp : scope) {
                if (first) {
                    first = false;
                } else {
                    builder.append("<br/>\n");
                }
                builder.append(scp);
            }
            builder.append("</h3>");
            str = str.replace("$$scope$$", builder.toString());
        } else {
            str = str.replace("$$scope$$", "");
        }
        
        final List<String> header = cell.getHeader();
        builder.setLength(0);
        first = true;
        if ((header != null) && (!header.isEmpty())) {
            for (String hdr : header) {
                if (first) {
                    first = false;
                } else {
                    builder.append("<br/>\n");
                }
                builder.append(hdr);
            }
            str = str.replace("$$celheader$$", builder.toString());
        } else {
            str = str.replace("$$celheader$$", "");
        }
        
        final String row = cell.getRow();
        if ((row != null) && (!row.isEmpty())) {
            str = str.replace("$$celrow$$", row);
        } else {
            str = str.replace("$$celrow$$", "");
        }
        
        final String value = cell.getValue();
        if ((value != null) && (!value.isEmpty())) {
            String celVal;
            try {
                celVal = NFMT.format(NFMT.parse(value).floatValue());
            } catch (ParseException ex) {
                celVal = value;
            }
            str = str.replace("$$celval$$", celVal);
        } else {
            str = str.replace("$$celval$$", "");
        }
        
        final List<String> sources = cell.getSources();
        builder.setLength(0);
        if ((sources != null) && (!sources.isEmpty())) {
            builder.append("<div class=\"note\">\n");
            builder.append("\t\t\t\t\t\t\t<label>Fonte(s):</label>\n");
            for (String source : sources) {
                builder.append("\t\t\t\t\t\t\t<p>");
                builder.append(source);
                builder.append("</p>\n");
            }
            builder.append("\t\t\t\t\t\t</div>\n");
            str = str.replace("$$sources$$", builder.toString());
        } else {
            str = str.replace("$$sources$$", "");
        }            
        
        final List<String> labels = cell.getLabels();
        builder.setLength(0);
        if ((labels != null) && (!labels.isEmpty())) {
            builder.append("<div class=\"note\">\n");
            builder.append("\t\t\t\t\t\t\t<label>Legenda(s):</label>\n");
            for (String label : labels) {
                builder.append("\t\t\t\t\t\t\t<p>");
                builder.append(label);
                builder.append("</p>\n");
            }
            builder.append("\t\t\t\t\t\t</div>\n");
            str = str.replace("$$labels$$", builder.toString());
        } else {
            str = str.replace("$$labels$$", "");
        }
        
        final List<String> notes = cell.getNotes();
        builder.setLength(0);
        if ((notes != null) && (!notes.isEmpty())) {
            builder.append("<div class=\"note\">\n");
            builder.append("\t\t\t\t\t\t\t<label>Nota(s):</label>\n");
            for (String note : notes) {
                builder.append("\t\t\t\t\t\t\t<p>");
                builder.append(note);
                builder.append("</p>\n");
            }
            builder.append("\t\t\t\t\t\t</div>\n");
            str = str.replace("$$notes$$", builder.toString());
        } else {
            str = str.replace("$$notes$$", "");
        }
        
        str = str.replace("$$father$$", cell.getElem().father.toString());
        str = str.replace("$$qualifRec$$", cell.getElem().qualifRec.toString());
        
        final Map<String,String> tableOptions = cell.getElem().tableOptions;
        builder.setLength(0);
        first = true;
        if ((tableOptions != null) && (!tableOptions.isEmpty())) {
            str = str.replace("$$tableHeader$$", "<strong>Filtros usados para a"
            + " geração da tabela de dados do TabNet</strong>\n<ul>\n");
            /*str = str.replace("$$tableHeader$$", "<strong>Tabela de dados do "
                      + "TabNet gerada com os seguintes filtros:</strong><br/><br/>");*/
            for (Map.Entry<String,String> option : tableOptions.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    builder.append("\t\t\t\t\t\t");
                }
                builder.append("<li><label>");
                builder.append(option.getKey());
                builder.append(":</label> ");
                builder.append(option.getValue());
                builder.append("</li>\n");
            }        
            str = str.replace("$$tableOptions$$", builder.toString() + "\n</ul>");
        } else {
            str = str.replace("$$tableHeader$$", "");
            str = str.replace("$$tableOptions$$", "");
        }                
                
        return str;
    }    
}
