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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 * date: 20130815
 */
public class URLS {
    public static final String ROOT_URL = "http://www.datasus.gov.br/idb";
    //public static final String ROOT_URL = "http://tabnet.datasus.gov.br/cgi/idb2011/matriz.htm";

    public static final String DEFAULT_ENCODING = "ISO8859-1";
    
    public static final char CSV_SEPARATOR = ';';
    
    private static final Pattern CSV_PATTERN = Pattern.compile(
                                         "(?i)<a href=\"?([^\\.\n]+?.csv)\"?>");
    private static final Pattern QUALIF_REC_PATTERN = Pattern.compile(
                    "(?i)<a href=\"([^\"]+)\">.*?Ficha de qualificação.*?</a>");
    private static final Pattern URL_PATTERN = Pattern.compile(
                                 "(?s)<a[^>]*?href=\"([^\"]+)\"[^>]*?>.+?</a>");
    private static final int MAX_LEVEL = 2;
    
    private static final Pattern STAR_PAT = Pattern.compile("\\*+");
    
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

        if (root.exists() && (!Utils.deleteFile(root))) {
            final String msg = "Directory [" + root.getAbsolutePath()
                                                          + "] creation error.";
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(msg);
            throw new IOException(msg);
        }
        if (!root.mkdirs()) {
            final String msg = "Directory [" + root.getAbsolutePath()
                                                          + "] creation error.";
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).severe(msg);
            throw new IOException(msg);
        }

        System.out.println("Searching cvs files\n");
        final Set<String> files = generateCells(url, root);
        System.out.println("Total cell files created: " + files.size());

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
        } catch (IOException ioe) {
            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME).log(Level.SEVERE,
                                             "Index file creation error.", ioe);
        }
        System.out.println("Files saved at: " + root.getAbsolutePath());
    }
    
    /**
     * Creates files associating each file with a table cell from a csv file
     * @param url html file where the csv links will be recursively searched.
     * @param root the output directory where the files will be created
     * @return a list of new created file names
     * @throws IOException 
     */
    public static Set<String> generateCells(final String url,
                                            final File root) 
                                                            throws IOException {
        if (url == null) {
            throw new NullPointerException("url");
        }
        if (root == null) {
            throw new NullPointerException("root");
        }

        final Set<String> urls = loadCsvFromHtml(new URL(url), root);
        
        try {
            Utils.copyDirectory(new File("template/css"), new File(root, "css"));
            Utils.copyDirectory(new File("template/img"), new File(root, "img"));
        } catch (IOException ioe) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
                       .log(Level.SEVERE, "skipping diretory: (css/img)", ioe);
        }
        
        return urls;
    }

    /**
     * Loads all cvs links from a root html page if possible otherwise find then
     * recursively. Then the csv s will be loaded and the table cells will be
     * saved at files.
     * @param html root page where the csv will be searched
     * @param root path where the files will be saved
     * @return a list of new created file names
     * @throws IOException 
     */
    private static Set<String> loadCsvFromHtml(final URL html,
                                               final File root) 
                                                            throws IOException {
        final Set<String> setUrls = new TreeSet<>();
        final Set<URL> history = new HashSet<>();

        final int aux[] = loadCsvFromHtml(html, null, null, 0, 0, setUrls, 
                                                                 history, root);
        System.out.println("\nTotal csv files parsed: " + aux[1]);
        
        return setUrls;
    }

    /**
     * Loads all cvs links from a root html page if possible otherwise find then
     * recursively. Then the csv s will be loaded and the table cells will be
     * saved at files.
     * @param html
     * @param postParam
     * @param tableOptions
     * @param tableNum
     * @param level
     * @param setUrls
     * @param history
     * @param root
     * @return array of int { number of the last table generated, number of csv files read }
     * @throws IOException 
     */
    private static int[] loadCsvFromHtml(final URL html,
                                         final String postParam,
                                         final Map<String,String> tableOptions,
                                         final int tableNum,
                                         final int level,
                                         final Set<String> setUrls,
                                         final Set<URL> history,
                                         final File root) throws IOException {
        assert html != null;
        assert tableNum >= 0;
        assert level >= 0;
        assert setUrls != null;
        assert history != null;
        assert root != null;
        
        int[] ret = new int[] { tableNum, 0 };

        if ((postParam != null) || (!history.contains(html))) {
            if (level <= MAX_LEVEL) {
                history.add(html);

                final String[] page;
                try {
                    page = (postParam == null) ? loadPageGet(html)
                                               : loadPagePost(html, postParam);
                } catch (IOException ioe) {
                    Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
                         .log(Level.SEVERE, "error loading file: [" + html 
                                        + "] params: [" + postParam + "]", ioe);
                    return ret;
                }    
                final String content = page[1];
                final Matcher mat = CSV_PATTERN.matcher(content);

                if (mat.find()) {   // Found a cvs link in that page  
                    final Matcher mat2 = QUALIF_REC_PATTERN.matcher(content);
                    if (mat2.find()) {                        
                        final UrlElem elem = new UrlElem();
                        elem.father = html;
                        elem.fatherParams = postParam;
                        elem.tableOptions = tableOptions;
                        elem.csv = withDomain(html, mat.group(1));
                        elem.qualifRec = withDomain(html, mat2.group(1));
                        try {
                            final String[] csvpage = loadPageGet(elem.csv);
                            final CSV_File csv = new CSV_File();
                            final Table table = csv.parse(csvpage[1], 
                                                                 CSV_SEPARATOR);

                            genCellsFromTable(table, elem, root, setUrls, 
                                                                      ++ret[0]);
                            ret[1] = 1;
                        } catch (Exception ex) {
                            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
                           .log(Level.SEVERE, "skipping file: " + elem.csv, ex);
                        }   
                    }
                } else {            // Did not find a cvs link in that page
                    if (postParam != null) {
                        Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
                              .log(Level.SEVERE, 
                         " skipping loadCsvFromHtml/Def file: " + html + 
                         " params:" + postParam + 
                         "\nCSV link not found into def page [" + 
                         findErrorMessage(content) + "]");                        
                    }
                    final Set<URL> urls = getPageDefHtmlUrls(
                                            new URL(page[0]), content, history);
                    for (URL url : urls) {
                        try {
                            final String file = url.getFile();
                            final int[] aux;
                            if (file.endsWith(".def")) {
                                aux = loadCsvFromDef(url, ret[0], setUrls, 
                                                                 history, root);                                
                            } else {
                                aux = loadCsvFromHtml(url, null, 
                                               null, ret[0], level + 1, setUrls, 
                                                                 history, root);
                            }
                            ret[0] = aux[0];
                            ret[1] += aux[1];
                        } catch (IOException ioe) {                             
                            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
                                           .log(Level.SEVERE, 
                                           "skipping loadCsvFromHtml/Def file: " 
                                                                    + url, ioe);
                            //throw ioe;
                        }
                    }
                }
            }
        }
        
        return ret;
    }
    
    /**
     * Given a table, generates all of its cells and save each one into a file
     * @param table table used to generate cells
     * @param elem 
     * @param root root directory where the files will be created
     * @param urls
     * @param tableNum table number used to create the file name
     */
    private static void genCellsFromTable(final Table table,
                                          final UrlElem elem,
                                          final File root,
                                          final Set<String> urls,
                                          final int tableNum) {
        assert table != null;
        assert elem != null;
        assert root != null;
        assert urls != null;
        assert tableNum >= 0;
                
        final ArrayList<ArrayList<String>> elems = table.getLines();
        final Iterator<ArrayList<String>> yit = elems.iterator();
        int idx = 1;

        for (String row : table.getRow()) {
            final Iterator<String> xit = yit.next().iterator();            
            for (List<String> hdr : table.getHeader()) {
                if (xit.hasNext()) {
                    final Cell cell = new Cell();
                    cell.setIdx(idx++);
                    cell.setElem(elem);
                    cell.setHeader(hdr);                                                
                    cell.setNotes(table.getNotes());
                    cell.setRow(row);
                    cell.setScope(table.getScope());
                    cell.setSources(table.getSources());
                    cell.setSubtitle(table.getSubtitle());
                    cell.setTitle(table.getTitle());
                    cell.setValue(xit.next());
                    cell.setLabels(adjustLabel(cell.getValue(), 
                                                            table.getLabels()));
                    final Matcher mat = REFUSE_PAT.matcher(cell.getValue());
                    if (!mat.matches()) {
                        try {
                            urls.add(saveToFile(cell, root, tableNum));
                        } catch (IOException ioe) {
                            Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
                                  .log(Level.SEVERE, "Can not save file.", ioe);
                        }
                    }
                }
            }
        }
    }
        
    /**
     * Returns only labels related with a given cell value
     * @param cellValue the cell value (ending with stars ***)
     * @param labels all labels of a table
     * @return  only labels with the right number of stars.
     */
    private static List<String> adjustLabel(final String cellValue,
                                            final List<String> labels) {
        assert cellValue != null;
        assert labels != null;
        
        final List<String> ret = new ArrayList<>();
        final Matcher smat = STAR_PAT.matcher(cellValue);
        final int starNum = smat.find() ? smat.group().length() : 0;      
        
        if (starNum > 0) {
            final char[] chars = new char[starNum];
            Arrays.fill(chars, '*');
            final String stars = new String(chars) + " ";
            
            for (String label : labels) {
                if (label.startsWith(stars)) {
                    ret.add(label);
                }
            }
        }
        
        return ret;
    }
    
    /**
     * Extracts cvs links by combining fields from a def file.
     * @param def url of the def page from which the cvs will be extracted
     * @return array of int { number of the last table generated, number of csv files read }
     * @throws IOException
     */
    private static int[] loadCsvFromDef(final URL def,
                                        final int tableNum,
                                        final Set<String> setUrls,
                                        final Set<URL> history,
                                        final File root) throws IOException {
        assert def != null;
        assert tableNum >= 0;
        assert setUrls != null;
        assert history != null;
        assert root != null;
        
        final Set<DEF_File.DefUrls> urls = new DEF_File().generateDefUrls(def);
        final int[] ret = new int[2];
        ret[0] = tableNum;
        
        for (DEF_File.DefUrls url : urls) {            
            try {
                final int[] aux = loadCsvFromHtml(new URL(url.url), 
                        url.postParams, url.options, ret[0], MAX_LEVEL, setUrls, 
                                                                 history, root);
                ret[0] = aux[0];
                ret[1] += aux[1];
            } catch (IOException ioe) {
                Logger.getLogger(Logger.GLOBAL_LOGGER_NAME)
                    .log(Level.SEVERE, "skipping loadCsvFromHtml file: " 
                                                                + url.url, ioe);
            }
        }
        
        return ret;
    }

    /**
     * Given an url, loads its content (GET - method)
     * @param url url to be loaded
     * @return an array with the real location of the page (in case of redirect)
     * and its content.
     * @throws IOException
     */
    public static String[] loadPageGet(final URL url) throws IOException {
        if (url == null) {
            throw new NullPointerException("url");
        }
        System.out.print("loading page (GET) : [" + url + "]");
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept-Charset", DEFAULT_ENCODING);
        connection.setRequestProperty("User-Agent", "curl/7.29.0");
        connection.setRequestProperty("Accept", "*/*");
        connection.connect();

        int respCode = connection.getResponseCode();
        final StringBuilder builder = new StringBuilder();
        String location = url.toString();

        while ((respCode >= 300) && (respCode <= 399)) {
            location = connection.getHeaderField("Location");
            connection = (HttpURLConnection)new URL(location).openConnection();
            respCode = connection.getResponseCode();
        }

        final boolean respCodeOk = (respCode == 200);
        final BufferedReader reader;
        boolean skipLine = false;

        if (respCodeOk) {
            reader = new BufferedReader(new InputStreamReader(
                                connection.getInputStream(), DEFAULT_ENCODING));
        } else {
            reader = new BufferedReader(new InputStreamReader(
                                connection.getErrorStream(), DEFAULT_ENCODING));
        }

        while (true) {
            final String line = reader.readLine();
            if (line == null) {
                break;
            }
            final String line2 = line.trim();

            if (line2.startsWith("<!--")) {
                skipLine = true;
            } else if (line2.endsWith("-->")) {
                skipLine = false;
            }
            if (! skipLine) {
                builder.append(line);
                builder.append("\n");
            }
        }
        reader.close();
        connection.disconnect();

        if (!respCodeOk) {
            throw new IOException("url=[" + url + "]\ncode=" + respCode + "\n"
                                                          + builder.toString());
        }
        //System.out.print("+");
        System.out.println(" - OK");

        return new String[] {location, builder.toString()};
    }

    /**
     * Given an url, loads its content (POST - method)
     * @param url url to be loaded
     * @param urlParameters post parameters
     * @return an array with the real location of the page (in case of redirect)
     * and its content.
     * @throws IOException
     */
    public static String[] loadPagePost(final URL url,
                                        final String urlParameters)
                                                            throws IOException {
        if (url == null) {
            throw new NullPointerException("url");
        }
        if (urlParameters == null) {
            throw new NullPointerException("urlParameters");
        }
        final String encodedParams = URLEncoder.encode(urlParameters, 
                                                              DEFAULT_ENCODING);
//System.out.print("loading page (POST): [" + url + "] params: " + urlParameters);

        //Create connection
        final HttpURLConnection connection =
                                        (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type",
                                           "application/x-www-form-urlencoded");
        connection.setRequestProperty("Content-Length", "" +
                             Integer.toString(encodedParams
                                    .getBytes().length));
                                    //.getBytes(DEFAULT_ENCODING).length));
        connection.setRequestProperty("Content-Language", "pt-BR");
        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(
                                                connection.getOutputStream())) {
            wr.write(encodedParams.getBytes(DEFAULT_ENCODING));
            //wr.writeBytes(urlParameters);
            wr.flush ();
        }

        //Get Response
        final StringBuffer response = new StringBuffer();
        try (BufferedReader rd = new BufferedReader(
                      new InputStreamReader(connection.getInputStream(),
                                                           DEFAULT_ENCODING))) {
            while (true) {
                final String line = rd.readLine();
                if (line == null) {
                    break;
                }
                response.append(line);
                response.append('\n');
            }
        }
        connection.disconnect();

        return new String[] {url.toString() + "?" + urlParameters,
                                                           response.toString()};
    }

    /**
     * Looks for html and/or def links inside a html page.
     * @param url the url of the html page where to look for links
     * @param content the html page content
     * @param history a set of links already searched. Avoid cyclic searchs
     * @return a set if html and/or def links found inside a page.
     * @throws IOException 
     */
    private static Set<URL> getPageDefHtmlUrls(final URL url,
                                               final String content,
                                               final Set<URL> history)
                                                            throws IOException {
        assert url != null;
        assert content != null;
        assert history != null;

        final Set<URL> ret = new HashSet<>();

        if (! history.contains(url)) {
            final Matcher mat = URL_PATTERN.matcher(content);

            while (mat.find()) {
                final String furl = mat.group(1);
                if (furl.endsWith(".def") || furl.endsWith(".html")
                                                     || furl.endsWith(".htm")) {
                    ret.add(withDomain(url, furl));
                }
            }
            history.add(url);
        }
        return ret;
    }

    static URL withDomain(final URL furl,
                          final String url) throws IOException {
        assert furl != null;
        assert url != null;

        final String url2 = url.trim();
        final URL ret;

        if (url2.startsWith("http://") || (url2.startsWith("www"))) {
            ret = new URL(url2);
        } else {
            if (url2.charAt(0) == '/') {
                ret = new URL(furl.getProtocol() + "://"
                                                       + furl.getHost() + url2);
            } else {
                final String path0 = furl.getPath();
                final String path = path0.substring(0, path0.lastIndexOf('/'));
                ret = new URL(furl.getProtocol() + "://" + furl.getHost()
                                                 + path + "/" + url2);
            }
        }
        return ret;
    }
        
    /**
     * Saves a cell info into a file
     * @param cell cell to be saved
     * @param rootDir directory where the file will be created
     * @param tableNum the table number to be used to create the file name
     * @return the new created file name. Where path is composed by cells edition
     * + qualification record category + file name.
     * @throws IOException 
     */
    private static String saveToFile(final Cell cell,
                                     final File rootDir,
                                     final int tableNum) throws IOException {
        assert cell != null;
        assert rootDir != null;
        assert tableNum > 0;

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
        final String fname = father.substring(idx1 + 1, idx2) + "_tb"
                                             + tableNum + "_ce" + cell.getIdx();
        final File path = new File(rootDir, "/" + edition + "/" + qualRec);
        final String spath = path.getPath();
        final String cfname = fname + ".html";

        if (!path.isDirectory()) {
            if (!path.mkdirs()) {
                throw new IOException("directory [" + spath
                                                          + "] creation error");
            }
        }
        saveToRipsaFile(cell, path, cfname);

        return edition + "/" + qualRec + "/" + cfname;
    }

    /**
     * Given a cell object, saves it into a file
     * @param cell the object info to be saved
     * @param path the path where the file will be created
     * @param fname file name
     * @throws IOException 
     */
    private static void saveToRipsaFile(final Cell cell,
                                        final File path,
                                        final String fname) throws IOException {
        assert cell != null;
        assert path != null;
        assert fname != null;

        File file = new File(path, fname);
        final String content = RIPSA.cell2html(cell);
        
        if (file.exists()) {
            throw new IOException("File[" + file.getPath() 
                                                          + "] already exists");
        }
        try (BufferedWriter writer = new BufferedWriter(
                                       new FileWriter(file))) {
            writer.append(content);
        }        
    }
    
    /**
     * Some html pages warns why they not have csv links inside header marks
     * @param content the html page content
     * @return the warning message
     */
    private static String findErrorMessage(final String content) {
        assert content != null;
        
        final Pattern pat = Pattern.compile("(?i)<h(\\d)>(.+?)</h\\1>");        
        final Matcher mat = pat.matcher(content);
            
        return mat.find() ? mat.group(2) : "";
    }

    private static String getLogFileName(final String logDir) {
        assert logDir != null;
        
        final Calendar cal = Calendar.getInstance();
        final StringBuilder builder = new StringBuilder(logDir);
        final char last = logDir.charAt(logDir.length() - 1);
        
        if ((last != '/') && (last != '\\')) {
            builder.append('/');
        }
        builder.append(cal.get(Calendar.YEAR));
        builder.append(cal.get(Calendar.MONTH));
        builder.append(cal.get(Calendar.DAY_OF_MONTH));
        builder.append(cal.get(Calendar.HOUR_OF_DAY));
        builder.append(cal.get(Calendar.MINUTE));
        builder.append(cal.get(Calendar.SECOND));
        builder.append(".log");
        
        return builder.toString();
    }
    
    private static void usage() {
        System.err.println("usage: URLS <outputDir>");
        System.exit(1);
    }
    
    public static void main(final String[] args) throws IOException {                
        if (args.length != 1) {
            usage();
        }
        
        final String out = args[0].trim();
        final String outDir = (out.endsWith("/")) ? out : out + "/";
        final String LOG_DIR = "log";
        final File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            if (!logDir.mkdir()) {
                throw new IOException("log directory [" + LOG_DIR + 
                                                            "] creation error");
            }
        }
        
        final Logger logger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        final FileHandler fh = new FileHandler(getLogFileName(LOG_DIR), false);  
        logger.addHandler(fh); 
        
        final String URL = URLS.ROOT_URL;
        final TimeString time = new TimeString();

        time.start();
        generateFileStructure(URL, outDir + "celulasIDB");
        
        System.out.println("Total time: " + time.getTime());
    }
}
