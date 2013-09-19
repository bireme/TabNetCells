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
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
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
    
    private static final String DEFAULT_ENCODING = "ISO8859-1";
    private static final Pattern CSV_PATTERN = Pattern.compile(
                                            "(?i)<a href=\"?([^\\.]+.csv)\"?>");
    private static final Pattern QUALIF_REC_PATTERN = Pattern.compile(
                    "(?i)<a href=\"([^\"]+)\">.*?Ficha de qualificação.*?</a>");
    private static final Pattern URL_PATTERN = Pattern.compile(
                                 "(?s)<a[^>]*?href=\"([^\"]+)\"[^>]*?>.+?</a>");
    private static final int MAX_LEVEL = 2;

    public class UrlElem {
        public URL father;    // url of the html page with the Qualif Record and csv links
        public URL csv;       // url of the csv page
        public URL qualifRec; // url of the Qualification Record page                
    }
    
    private class MultiDefLoad extends Thread {
        final URL html;
        final String postParam;
        final int level;
        final Set<URL> history;
        final CountDownLatch latch;
        Set<UrlElem> result;
        
        MultiDefLoad(final URL html,
                     final String postParam,
                     final int level,
                     final Set<URL> history,
                     final CountDownLatch latch) {
            this.html = html;
            this.postParam = postParam;
            this.level = level;
            this.history = history;            
            this.latch = latch;
            result = null;
        }
        
        @Override
        public void run() {
            try {
                result = loadCsvFromHtml(html, postParam, level, history);
            } catch (IOException ex) {
                Logger.getLogger(URLS.class.getName())
                                                   .log(Level.SEVERE, null, ex);
            }
            latch.countDown();
        }
        
        Set<UrlElem> getResult() {
            return result;
        }
    }
        
    public Set<UrlElem> loadCsvFromHtml(final URL html) throws IOException {
        final Set<URL> set = Collections.newSetFromMap(
                                         new ConcurrentHashMap<URL, Boolean>());
        
        return loadCsvFromHtml(html, null, 0, set);
    }
    
    private Set<UrlElem> loadCsvFromHtml(final URL html,
                                         final String postParam,
                                         final int level,
                                         final Set<URL> history) 
                                                            throws IOException {
        if (html == null) {
            throw new NullPointerException("html");
        }
        if (level < 0) {
            throw new IllegalArgumentException("level [" + level + "] < 0");
        }
        if (history == null) {
            throw new NullPointerException("history");
        }
        final Set<UrlElem> set = new HashSet<>();
        
        if ((postParam != null) || !history.contains(html)) {
            if (level <= MAX_LEVEL) {
                final String[] page = (postParam == null) ? loadPageGet(html)
                                                : loadPagePost(html, postParam);
                final String content = page[1];
                final Matcher mat = CSV_PATTERN.matcher(content);

                if (mat.find()) {
                    final Matcher mat2 = QUALIF_REC_PATTERN.matcher(content);                
                    if (mat2.find()) {
                        final UrlElem elem = new UrlElem();
                        elem.father = html;
                        elem.csv = withDomain(html, mat.group(1));
                        elem.qualifRec = withDomain(html, mat2.group(1));
                        set.add(elem);
                    }                
                } else {
                    final Set<URL> urls = getPageDefHtmlUrls(
                                            new URL(page[0]), content, history);
                    for (URL url : urls) {
                        final String file = url.getFile();
                        if (file.endsWith(".def")) {
                            set.addAll(loadCsvFromDef(url, history));
                        } else {
                            set.addAll(loadCsvFromHtml(url, null, level + 1, 
                                                                      history));
                        }
                    }
                }            
            }
            history.add(html);
        }
        
        return set;
    }
    /**
     * Extracts a list of cvs by combining fields from a def file.
     * @param def url of the def page from which the cvs will be extracted
     * @return  A set of UrlElem elements
     * @throws IOException
     */
    private Set<UrlElem> loadCsvFromDef(final URL def,
                                        final Set<URL> history) 
                                                            throws IOException {
        if (def == null) {
            throw new NullPointerException("def");
        }
        
        final Set<UrlElem> set = new HashSet<>();
        final Set<String[]> urls = DEF_File.generateDefUrls(def);
        
        for (String[] url : urls) {            
            set.addAll(loadCsvFromHtml(new URL(url[0]), url[1], 
                                                           MAX_LEVEL, history));
        }
        
        return set;
    }                        
    
    /*private Set<UrlElem> loadCsvFromDef_Teste(final URL def,
                                        final Set<URL> history) 
                                                            throws IOException {
        if (def == null) {
            throw new NullPointerException("def");
        }
        
        final Set<UrlElem> set = new HashSet<>();
        final Set<String[]> urls = DEF_File.generateDefUrls(def);
        final int size = urls.size();
        CountDownLatch latch = new CountDownLatch(size);
        final List<MultiDefLoad> lst = new ArrayList<>();
        int current = 0;
        
        for (String[] url : urls) {
            if (current % 30 == 0) {
                latch = new CountDownLatch(30);
                try {
                    latch.await();  //main thread is waiting on CountDownLatch to finish
                } catch (InterruptedException ie){
                   throw new IOException(ie);
                }
                for (MultiDefLoad load : lst) {                                                                
                    set.addAll(load.getResult());
                }
                current = 0;                            
            }
            MultiDefLoad load = new MultiDefLoad(new URL(url[0]), url[1], 
                                                     MAX_LEVEL, history, latch);
            lst.add(load);
            load.start();
        }
        if (current > 0) {
            
        }
        for (String[] url : urls) {
            MultiDefLoad load = new MultiDefLoad(new URL(url[0]), url[1], 
                                                     MAX_LEVEL, history, latch);
            lst.add(load);
            load.start();
        }
        try {
            latch.await();  //main thread is waiting on CountDownLatch to finish
        } catch (InterruptedException ie){
           throw new IOException(ie);
        }
        for (MultiDefLoad load : lst) {                                                                
            set.addAll(load.getResult());
        }
        
        return set;
    }*/
        
    /**
     * Given an url, loads its content (GET - method)
     * @param url url to be loaded
     * @return an array with the real location of the page (in case of redirect)
     * and its content.
     * @throws IOException 
     */
    public String[] loadPageGet(final URL url) throws IOException {    
        if (url == null) {
            throw new NullPointerException("url");
        }
//System.out.println("loading page (GET) : [" + url + "]");
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
        System.out.print("+");
        
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
//System.out.println("loading page (POST): [" + url + "] params: " + urlParameters);        

        //Create connection
        final HttpURLConnection connection = 
                                        (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", 
                                           "application/x-www-form-urlencoded");			
        connection.setRequestProperty("Content-Length", "" + 
                             Integer.toString(urlParameters.getBytes(DEFAULT_ENCODING).length));
        connection.setRequestProperty("Content-Language", "pt-BR");  			
        connection.setUseCaches (false);
        connection.setDoInput(true);
        connection.setDoOutput(true);
        try (DataOutputStream wr = new DataOutputStream(
                                                connection.getOutputStream())) {
            wr.write(urlParameters.getBytes(DEFAULT_ENCODING));
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
        System.out.print(".");
      
        return new String[] {url.toString() + "?" + urlParameters, 
                                                           response.toString()};
    }

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
            
    public static void main(final String[] args) throws IOException {
        final URLS urls = new URLS(); 
        
        /*final URL xurl = new URL("http://tabnet.datasus.gov.br/cgi/deftohtm.exe?idb2011/a16.def");
        final Set<UrlElem> set0 = urls.loadCsvFromDef(xurl, new HashSet<URL>());
        for (UrlElem elem : set0) {
            System.out.println("-------------(" + elem.csv + "," + elem.father 
                                  + "," + elem.qualifRec + ")--------------\n");
            
        }*/
        
        final Set<UrlElem> set = urls.loadCsvFromHtml(new URL(ROOT_URL));
        for (UrlElem elem : set) {
            System.out.println("-------------(" + elem.csv + "," + elem.father 
                                  + "," + elem.qualifRec + ")--------------\n");

            final String[] page = urls.loadPageGet(elem.csv);
            final String content = page[1];
            final CSV_File csv = new CSV_File();
            final Table table = csv.parse(content, ';');

            System.out.println(table);
            System.out.println("\n");
        }
    }
}
