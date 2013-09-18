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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 * date: 20130913
 */
public class DEF_File {
    private static final Pattern FORM_PATTERN = Pattern.compile(
                                   "(?i)<FORM.+?ACTION=\\\"([^\\\"]+)\\\".*?>");
    private static final Pattern SELECT_PATTERN = Pattern.compile(
                       "(?i)(?s)<SELECT.+?ID=\"(\\w{1,3})\".*?>(.+?)</SELECT>");
    private static final Pattern OPTION_PATTERN = Pattern.compile(
                                        "(?i)<OPTION.+?VALUE=\"([^\"]+)\".*?>");
    
    public static Set<String[]> generateDefUrls(final URL url) throws IOException {
        if (url == null) {
            throw new NullPointerException("url");
        }
        final Set<String[]> set = new HashSet<>();
        final String content = new URLS().loadPageGet(url)[1];
        final Map<String,Set<String>> selectOpts = getSelectOptions(content);
        final Set<Map<String,String>> postOpts = generatePostOptions(selectOpts);
        final String target = getFormTarget(content).trim();
        final String tgt = (target.endsWith("/")) 
                              ? target.substring(0, target.length()-1) : target;
        final URL durl = URLS.withDomain(url, tgt);       

        for (Map<String,String> map : postOpts) {
            final StringBuilder builder = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String,String> entry : map.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    builder.append("&");
                }                
                builder.append(entry.getKey());
                builder.append("=\"");
                builder.append(entry.getValue());
                builder.append("\"");
            }
                        
            set.add(new String[] {durl.toString(), builder.toString()});
        }
        
        return set;
    }

    private static Map<String,Set<String>> getSelectOptions(
                                      final String content) throws IOException {
        assert content != null;

        final Map<String,Set<String>> map = new HashMap<>();
        final Matcher mat = SELECT_PATTERN.matcher(content);
        
        while (mat.find()) {
            final String selectId = mat.group(1);
            final String selectContent = mat.group(2);
            final Matcher mat2 = OPTION_PATTERN.matcher(selectContent);
            final Set<String> options = new HashSet<>();
            
            while (mat2.find()) {
                options.add(mat2.group(1));
            }
            map.put(selectId, options);
        }
        return map;
    }
    
    private static Set<Map<String,String>> generatePostOptions(
                                       Map<String,Set<String>> selectOptions) {
        assert selectOptions != null;
        
        final String REGION_AND_FEDERATION_UNIT = 
                                                "Região_e_Unidade_da_Federação";
        final String YEAR = "Ano";
        final String NOT_ACTIVE = "--Não-Ativa--";
        final String ALL_CATEGORIES = "TODAS_AS_CATEGORIAS__";        
        
        final Set<Map<String,String>> set = new HashSet<>();
        final Set<String> line = selectOptions.get("L");
        final Set<String> content = selectOptions.get("I");
        final Set<String> time = selectOptions.get("A");
        
        line.remove(REGION_AND_FEDERATION_UNIT);
        line.remove(YEAR);
        
        selectOptions.remove("L");
        selectOptions.remove("C");
        selectOptions.remove("I");
        selectOptions.remove("A");
        final Set<String> others = selectOptions.keySet();
        
        for (String lineElem : line) {            
            for (String contentElem : content) {
                for (String timeElem : time) {
                    final Map<String,String> map = new HashMap<>();
                    map.put("L", lineElem);
                    map.put("C", NOT_ACTIVE);
                    map.put("I", contentElem);
                    map.put("A", timeElem);
                    for (String otherElem : others) {
                        map.put(otherElem, ALL_CATEGORIES);
                    }
                    set.add(map);
                }                
            }
        }
        
        return set;
    }
    
    private static String getFormTarget(final String content) 
                                                            throws IOException {
        assert content != null;
        
        final Matcher mat = FORM_PATTERN.matcher(content);        
        if (!mat.find()) {
            throw new IOException("form target (url) not found");
        }
        return mat.group(1);
    }                
}
