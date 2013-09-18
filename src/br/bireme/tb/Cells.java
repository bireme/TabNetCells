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

import br.bireme.utils.TimeString;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 * date 20130911
 */
public class Cells {
    public static final char CSV_SEPARATOR = ';';
    private static final Pattern REFUSE_PAT = 
                                          Pattern.compile("\\s*[\\.\\*]+\\s*");
    private static final Pattern EDITION_PAT = Pattern.compile(
           "\\?node=([^\\&]+)\\&lang=\\w+\\&version=([^\\s]+)");    
            
    public static void generateFileStructure(final String url,
                                             final String rootDir) 
                                                            throws IOException {
        if (url == null) {
            throw new NullPointerException("url");
        }
        if (rootDir == null) {
            throw new NullPointerException("rootDir");
        }
        final File root = new File(rootDir);
        
        deleteFile(root);
        if (!root.mkdirs()) {
            throw new IOException("directory [" + root.getCanonicalPath() 
                                                          + "] creation error");
        }              
        
        System.out.println("Searching cvs files\n");
        final Set<String> files = Cells.generateCells(url, root);
        System.out.println("\nTotal files created: " + files.size());
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(
                                               new File(root, "index.html")))) {
            writer.append("<!DOCTYPE html>\n");
            writer.append("<html>\n");
            writer.append(" <head>\n");
            writer.append("  <meta charset=\"UTF-8\">\n");
            writer.append(" </head>\n");
            writer.append(" <body>\n");
            writer.append("  <h1>Fichas de Qualificação</h1>\n");                                    
            for (String path : files) {
                writer.append("  <ul>\n");
                writer.append("   <li>\n");
                writer.append("    <a href=\"" + path + "\">" + path +"</a>\n"); 
                writer.append("   </li>\n");
                writer.append("  </ul>\n");
            }
            writer.append(" </body>\n");
            writer.append("</html>\n");            
        }        
        System.out.println("Files saved at " + url);
    }
    
    public static Set<String> generateCells(final String url,
                                            final File root) 
                                                            throws IOException {        
        if (url == null) {
            throw new NullPointerException("url");
        }
        if (root == null) {
            throw new NullPointerException("root");
        }
                        
        final Set<String> ret = new TreeSet<>();
        final URLS urls = new URLS();
        final Set<URLS.UrlElem> set = urls.loadCsvFromHtml(new URL(url));
        
        System.out.println("\nTotal csv files found: " + set.size());

        for (URLS.UrlElem elem : set) {            
            final String[] page = urls.loadPageGet(elem.csv);
            final String content = page[1];
            final CSV_File csv = new CSV_File();
            final Table table = csv.parse(content, CSV_SEPARATOR);
            
            System.out.println("Generating cells\n");
            genCellsFromTable(table, elem, root, ret);
        }
        return ret;
    }
    
    private static void genCellsFromTable(final Table table,
                                          final URLS.UrlElem elem,
                                          final File root,
                                          final Set<String> urls) 
                                                            throws IOException {
        assert table != null;
        assert elem != null;
        assert root != null;
        assert urls != null;
        
        final List<List<String>> elems = table.getLines();
        final Iterator<List<String>> yit = elems.iterator(); 
        int idx = 1;
        
        for (String row : table.getRow()) {        
            final Iterator<String> xit = yit.next().iterator();
            for (List<String> hdr : table.getHeader()) {
                final Cell cell = new Cell();
                cell.setIdx(idx++);
                cell.setElem(elem);
                cell.setHeader(hdr);
                cell.setLabels(table.getLabels());
                cell.setNotes(table.getNotes());
                cell.setRow(row);
                cell.setScope(table.getScope());
                cell.setSources(table.getSources());
                cell.setSubtitle(table.getSubtitle());
                cell.setTitle(table.getTitle());
                cell.setValue(xit.next());
                final Matcher mat = REFUSE_PAT.matcher(cell.getValue());
                if (!mat.matches()) {
                    urls.add(saveToFile(cell, root));
                }                
            }
        }
    }
    
    private static String saveToFile(final Cell cell,
                                     final File rootDir) throws IOException {
        assert cell != null;
        assert rootDir != null;        
        
        final String father = cell.getElem().father.toString();
        final String url = cell.getElem().qualifRec.toString();
        final Matcher mat = EDITION_PAT.matcher(url);
        if (!mat.find()) {
            throw new IOException("out of pattern url [" + url + "]");
        }
        final String qualRec = mat.group(1);
        final String edition = mat.group(2);
        final int idx1 = father.lastIndexOf('/');
        final int idx2 = father.lastIndexOf('.');
        final String fname = father.substring(idx1 + 1, idx2) + "_" 
                                                                + cell.getIdx();
        final File path = new File(rootDir, "/" + edition + "/" + qualRec);
        final String spath = path.getPath();
        final String cfname = fname + ".html";
        
        if (!path.isDirectory()) {
            if (!path.mkdirs()) {
                throw new IOException("directory [" + spath 
                                                          + "] creation error");
            }
        }
        saveToFile(cell, path, cfname);
        
        return edition + "/" + qualRec + "/" + cfname;
    }
    
    private static void saveToFile(final Cell cell,
                                   final File path,
                                   final String fname) throws IOException {
        assert cell != null;
        assert path != null;
        assert fname != null;
        
        File file = new File(path, fname);
//System.out.println("writing file: [" + file.getCanonicalPath() + "]");
        if (!file.exists()) {  // existem repetições de arquivos em cel diferentes
            try (BufferedWriter writer = new BufferedWriter(
                                           new FileWriter(file))) {
                writer.append(cell.toHtml());
            }
        }
    }
    
    private static boolean deleteFile(final File file) {
        assert file != null;
        
        boolean status = true;
        
        if (file.isDirectory()){
            for (File child : file.listFiles()) {
                status = status && deleteFile(child);
            }
        }        
        return status && file.delete();
    }
    
    public static void main(final String[] args) throws IOException {
        final String URL = URLS.ROOT_URL; 
        final TimeString time = new TimeString();
        
        time.start();
        generateFileStructure(URL, "FichasDeQualificacao");
        System.out.println("Total time: " + time.getTime());        
    }
}
