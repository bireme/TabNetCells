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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
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
    
    private static final String DEFAULT_ENCODING = "ISO8859-1";
    private static final Pattern CSV_PATTERN = Pattern.compile(
                                                  "<a href=\"([^\\.]+.csv)\">");
    private static final Pattern QUALIF_REC_PATTERN = Pattern.compile(
       "<a href=\"([^\"]+)\">.*?Ficha de qualificação.*?</a>");
    private static final Pattern URL_PATTERN = Pattern.compile(
                                 "(?s)<a[^>]*?href=\"([^\"]+)\"[^>]*?>.+?</a>");
    private static final int MAX_LEVEL = 2;

    public class UrlElem {
        public URL father;    // url of the html page with the Qualif Record and csv links
        public URL csv;       // url of the csv page
        public URL qualifRec; // url of the Qualification Record page                
    }
    
    public Set<UrlElem> loadCsvFromHtml(final URL html) throws IOException {
        return loadCsvFromHtml(html, true, 0);
    }
    
    public Set<UrlElem> loadCsvFromHtml(final URL html,
                                        final boolean getMethod,
                                        final int level) throws IOException {
        if (html == null) {
            throw new NullPointerException("html");
        }
        if (level < 0) {
            throw new IllegalArgumentException("level [" + level + "] < 0");
        }
        final Set<UrlElem> set = new HashSet<>();
        
        if (level <= MAX_LEVEL) {
            final String[] page = loadPage(html, getMethod);
            final String content = page[1];
            final Matcher mat = CSV_PATTERN.matcher(content);
        
            if (mat.find()) {
                final Matcher mat2 = QUALIF_REC_PATTERN.matcher(content);
                final UrlElem elem = new UrlElem();
                elem.father = html;
                elem.csv = withDomain(html, mat.group(1));
                if (!mat2.find()) {
                    throw new IOException("Qualification Record url not found");
                }
                elem.qualifRec = withDomain(html, mat2.group(1));
                set.add(elem);
            } else {
                final Set<URL> urls = getPageDefHtmlUrls(
                                                     new URL(page[0]), content);
                for (URL url : urls) {
                    final String file = url.getFile();
                    if (file.endsWith(".def")) {
                        set.addAll(loadCsvFromDef(url));
                    } else {
                        set.addAll(loadCsvFromHtml(url, getMethod, level + 1));
                    }
                }
            }            
        }
        return set;
    }
    /**
     * Extracts a list of cvs by combining fields from a def file.
     * @param def url of the def page from which the cvs will be extracted
     * @return  A set of UrlElem elements
     * @throws IOException
     */
    public Set<UrlElem> loadCsvFromDef(final URL def) throws IOException {
        if (def == null) {
            throw new NullPointerException("def");
        }
        
        final Set<UrlElem> set = new HashSet<>();
        /*final Set<URL> urls = DEF_File.generateDefUrls(def);
        
        for (URL url : urls) {
            set.addAll(loadCsvFromHtml(url, false, 0));
        }*/
        
        return set;
    }                        
    
    /**
     * Given an url, loads its content
     * @param url url to be loaded
     * @return an array with the real location of the page (in case of redirect)
     * and its content.
     * @throws IOException 
     */
    public String[] loadPage(final URL url) throws IOException {    
        return loadPage(url, true);
    }
    
    /**
     * Given an url, loads its content
     * @param url url to be loaded
     * @param get true - get method, false - post method
     * @return an array with the real location of the page (in case of redirect)
     * and its content.
     * @throws IOException 
     */
    public String[] loadPage(final URL url,
                             final boolean get) throws IOException {    
        if (url == null) {
            throw new NullPointerException("url");
        }
        HttpURLConnection.setFollowRedirects(false);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection(); 
        connection.setRequestMethod(get ? "GET" : "POST");
        connection.setRequestProperty("Accept-Charset", DEFAULT_ENCODING);
        if (!get) {            
            connection.setDoInput(true);
             //connection.setRequestProperty("Content-Type", 
             //                              "application/x-www-form-urlencoded");
            connection.setDoOutput(true); 
            connection.setRequestProperty("Content-Length", "10000");//"" + 
               //Integer.toString(url.getQuery().getBytes("UTF-8").length + 5));
//connection.setFixedLengthStreamingMode(url.getQuery().getBytes().length-10); 
        }
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
        return new String[] {location, builder.toString()};
    }

    private static Set<URL> getPageDefHtmlUrls(final URL url,
                                               final String content) 
                                                            throws IOException {
        assert url != null;
        assert content != null;
        
        final Set<URL> ret = new HashSet<>();        
        final Matcher mat = URL_PATTERN.matcher(content);
        
        while (mat.find()) {
            final String furl = mat.group(1);
            if (furl.endsWith(".def") || furl.endsWith(".html") 
                                                     || furl.endsWith(".htm")) {
                ret.add(withDomain(url, furl));
            }
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
            
    public static void main(final String[] args) throws IOException {
        final URLS urls = new URLS();
 
final String[] page2 = urls.loadPage(new URL("http://tabnet.datasus.gov.br/cgi/tabcgi.exe?DEF=idb2011/c06.def&Unidade_da_Federa%E7%E3o&Coluna=--N%E3o-Ativa--&Incremento=Popula%E7%E3o&Arquivos=popa10.dbf&SUnidade_da_Federa%E7%E3o=TODAS_AS_CATEGORIAS__&SRegi%E3o=TODAS_AS_CATEGORIAS__&SRegi%E3o_Metropolitana=TODAS_AS_CATEGORIAS__&SCapital=TODAS_AS_CATEGORIAS__&SSexo=TODAS_AS_CATEGORIAS__&SFaixa_Et%E1ria=TODAS_AS_CATEGORIAS__&opcoes=ordenar&formato=table&mostre=Mostra"), false);
        
        final Set<UrlElem> set = urls.loadCsvFromHtml(new URL(ROOT_URL));
        
        for (UrlElem elem : set) {
            System.out.println("-------------(" + elem.csv + "," + elem.father 
                                  + "," + elem.qualifRec + ")--------------\n");

            final String[] page = urls.loadPage(elem.csv);
            final String content = page[1];
            final CSV_File csv = new CSV_File();
            final Table table = csv.parse(content, ';');

            System.out.println(table);
            System.out.println("\n");
        }
    }
}
