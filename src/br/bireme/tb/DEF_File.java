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
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 * date: 20130913
 */
class DEF_File {
    private static final Pattern FORM_PATTERN = Pattern.compile(
                                   "(?i)<FORM.+?ACTION=\\\"([^\\\"]+)\\\".*?>");
    private static final Pattern SELECT_PATTERN = Pattern.compile(
                                      "(?i)(?s)<SELECT ([^>]+)>(.+?)</SELECT>");
    private static final Pattern ID_PATTERN = Pattern.compile(
                                                         "(?i)ID=\"([^\"]+)\"");
    private static final Pattern NAME_PATTERN = Pattern.compile(
                                                       "(?i)NAME=\"([^\"]+)\"");
    private static final Pattern OPTION_PATTERN = Pattern.compile(
                           "(?i)<OPTION.+?VALUE=\"([^\"]+)\".*?>(.+?)(<|\n|$)");
    
    /**
     * Pssible values of a selectable options table
     */
    class SelectableOptions {
        String label;
        String name;
        String id;
        Map<String,String> options; // value, label
    }
    
    class DefUrls implements Comparable<DefUrls> {

        String url;
        String postParams;
        Map<String,String> options; // opt label, value label
        
        @Override
        public int compareTo(final DefUrls other) {
            int ret;
                                                
            if (url == null) {                
                ret = ((other == null) || (other.url == null)) ? 0 : -1;
            } else {
                ret = (other == null) ? +1 : url.compareTo(other.url);
                if (ret == 0) {
                    if (postParams == null) {
                        if (other.postParams != null) {
                            ret = -1;
                        }
                    } else {
                        ret = postParams.compareTo(other.postParams);
                    }
                }
            }
            return ret;
        }
    }

    Set<DefUrls> generateDefUrls(final URL url) throws IOException {
        if (url == null) {
            throw new NullPointerException("url");
        }
        final Set<DefUrls> set = new TreeSet<>();
        final String content = URLS.loadPageGet(url)[1];
        final Map<String,SelectableOptions> selectOpts = getSelectOptions(content);
        final Set<Map<String,Map.Entry<String,String>>> postOpts = 
                                                generatePostOptions(selectOpts);
        final String target = getFormTarget(content).trim();
        final String tgt = (target.endsWith("/"))
                              ? target.substring(0, target.length()-1) : target;
        final URL durl = URLS.withDomain(url, tgt);
        final Map<String,String> nameLabel = new HashMap<>();
        
        for (SelectableOptions selOpt : selectOpts.values()) {
            nameLabel.put(selOpt.name, selOpt.label);
        }

        for (Map<String,Map.Entry<String,String>> map : postOpts) {
            final StringBuilder builder = new StringBuilder();
            final Map<String,String> options = new TreeMap<>();
            boolean first = true;
            for (Map.Entry<String,Map.Entry<String,String>> entry 
                                                             : map.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    builder.append("&");
                }
                final String optLabel = entry.getKey();
                final Map.Entry<String,String> optValue = entry.getValue();
                builder.append(optLabel);
                builder.append("=");
                builder.append(optValue.getKey());
                options.put(nameLabel.get(optLabel), optValue.getValue());
            }
            final DefUrls def = new DefUrls();
            def.url = durl.toString();
            def.postParams = builder.toString() + "&formato=table&mostre=Mostra";
            def.options = options;
            set.add(def);
        }

        return set;
    }
    
    private Map<String,SelectableOptions> getSelectOptions(final String content) 
                                                            throws IOException {
        assert content != null;

        final Map<String,SelectableOptions> ret = new HashMap<>();
        final Matcher matSel = SELECT_PATTERN.matcher(content);

        while (matSel.find()) {
            final SelectableOptions opt = new SelectableOptions();
            final String selAtt = matSel.group(1);
            final Matcher matId = ID_PATTERN.matcher(selAtt);
            final Matcher matName = NAME_PATTERN.matcher(selAtt);
            if (!matId.find()) {
                throw new IOException("id attr not found: [" + selAtt + "]");
            }
            opt.id = matId.group(1);
            if (!matName.find()) {
                throw new IOException("name attr not found: [" + selAtt + "]");
            }
            opt.name = matName.group(1);
            
            /*final Pattern frameLabelPat = Pattern.compile(
                  "(?i)(?s)<LABEL for=\"" + opt.id + 
                                             "\">(<b>)?(.+?)(</b>)?</LABEL>");*/
            final Pattern frameLabelPat = Pattern.compile(
                  "(?i)(?s)<LABEL for=\"" + opt.id + 
                                       "\">(<[^>]+>)?(.+?)(</[^>]+>)?</LABEL>");
            final Matcher matLab = frameLabelPat.matcher(content);
            if (!matLab.find()) {
                throw new IOException("Select label not found.");
            }
            opt.label = matLab.group(2).replace('\n', ' ').trim();
            
            final String selectContent = matSel.group(2);
            final Matcher mat2 = OPTION_PATTERN.matcher(selectContent);
            final Map<String,String> optMap = new HashMap<>();

            while (mat2.find()) {
                optMap.put(mat2.group(1).trim(), 
                           mat2.group(2).trim()); // (value, label)
            }
            opt.options = optMap;
            ret.put(opt.id, opt);  // (id, options)
        }
        return ret;
    }
    
    private Set<Map<String,Map.Entry<String,String>>> generatePostOptions(
                           final Map<String, SelectableOptions> selectOptions) {

        assert selectOptions != null;

        final String REGION_AND_FEDERATION_UNIT =
                                                "Região_e_Unidade_da_Federação";
        final String YEAR = "Ano";
        final String NOT_ACTIVE = "--Não-Ativa--";
        final String NOT_ACTIVE_LABEL = "Não Ativa";
        final String ALL_CATEGORIES = "TODAS_AS_CATEGORIAS__";
        final String ALL_CATEGORIES_LABEL = "Todas as categorias";

        final Set<Map<String,Map.Entry<String,String>>> set = new HashSet<>();
        final SelectableOptions line = selectOptions.get("L");
        final SelectableOptions columm = selectOptions.get("C");
        final SelectableOptions content = selectOptions.get("I");
        final SelectableOptions time = filterDates(selectOptions.get("A"), 1);      

        line.options.remove(REGION_AND_FEDERATION_UNIT);
        line.options.remove(YEAR);
        
        for (Map.Entry<String,String> lineElem : line.options.entrySet()) {
            for (Map.Entry<String,String> contentElem : 
                                                   content.options.entrySet()) {
                for (Map.Entry<String,String> timeElem : 
                                                      time.options.entrySet()) {
                    final Map<String,Map.Entry<String,String>> map = 
                                                                new HashMap<>();
                    map.put(line.name, lineElem);
                    map.put(columm.name, new AbstractMap.SimpleEntry<>
                                                (NOT_ACTIVE, NOT_ACTIVE_LABEL));
                    map.put(content.name, contentElem);
                    map.put(time.name, timeElem);
                    for (Map.Entry<String,SelectableOptions> entry : 
                                                     selectOptions.entrySet()) {
                        final String id = entry.getKey();
                        if ((!id.equals("L")) && (!id.equals("C")) && 
                            (!id.equals("I")) && (!id.equals("A"))) {
                            map.put(entry.getValue().name, 
                              new AbstractMap.SimpleEntry<>(ALL_CATEGORIES,
                                                         ALL_CATEGORIES_LABEL));
                        }
                    }
                    set.add(map);
                }
            }
        }
        return set;
    }
    
    private String getFormTarget(final String content) throws IOException {
        assert content != null;

        final Matcher mat = FORM_PATTERN.matcher(content);
        if (!mat.find()) {
            throw new IOException("form target (url) not found");
        }
        return mat.group(1);
    }
    
    private SelectableOptions filterDates(final SelectableOptions dates,             
                                          final int max) {    
        assert dates != null;
        assert max >= 1;
        
        final Map<String,String> opts = dates.options;
        final Set<String> in = opts.keySet();
        final Map<String, String> options = new HashMap<>();
        final Matcher mat9x = Pattern.compile("[^\\d]+9\\d+\\.\\w+").matcher("");
        final Matcher mat0x = Pattern.compile("[^\\d]+0\\d+\\.\\w+").matcher("");
        final Matcher mat1x = Pattern.compile("[^\\d]+1\\d+\\.\\w+").matcher("");
        final Matcher mat2x = Pattern.compile("[^\\d]+2\\d+\\.\\w+").matcher("");
        final TreeSet<String> set9x = new TreeSet<>();
        final TreeSet<String> set0x = new TreeSet<>();
        final TreeSet<String> set1x = new TreeSet<>();
        final TreeSet<String> set2x = new TreeSet<>();
        
        for (String date : in) {
            mat9x.reset(date);
            if (mat9x.matches()) {
                set9x.add(date);
            } else {
                mat0x.reset(date);                         
                if (mat0x.matches()) {
                    set0x.add(date);
                } else {
                    mat1x.reset(date);
                    if (mat1x.matches()) {
                        set1x.add(date);
                    } else {
                        mat2x.reset(date);
                        if (mat2x.matches()) {
                            set2x.add(date);
                        }
                    }
                }
            }
        }
        
        int remaining = max;        
        for (String x2 : set2x.descendingSet()) {
            if (remaining == 0) {
                break;
            }
            options.put(x2, opts.get(x2));
            remaining--;
        }
        if (remaining > 0) {
            for (String x1 : set1x.descendingSet()) {
                if (remaining == 0) {
                    break;
                }
                options.put(x1, opts.get(x1));
                remaining--;
            }
        }
        if (remaining > 0) {
            for (String x0 : set0x.descendingSet()) {
                if (remaining == 0) {
                    break;
                }
                options.put(x0, opts.get(x0));
                remaining--;
            }
        }
        if (remaining > 0) {
            for (String x9 : set9x.descendingSet()) {
                if (remaining == 0) {
                    break;
                }
                options.put(x9, opts.get(x9));
                remaining--;
            }
        }
        dates.options = options;
        
        return dates;
    }
    
    public static void main(final String[] args) throws IOException {
        final String url = 
                "http://tabnet.datasus.gov.br/cgi/deftohtm.exe?idb2011/a01.def";
        final DEF_File def = new DEF_File();
        
        for (DefUrls urls: def.generateDefUrls(new URL(url))) {
            System.out.println(urls.url + "[" + urls.postParams + "]");
        }
    }
}
