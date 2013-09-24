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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
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
                                      "(?i)(?s)<SELECT ([^>]+)>(.+?)</SELECT>");
    private static final Pattern ID_PATTERN = Pattern.compile(
                                                         "(?i)ID=\"([^\"]+)\"");
    private static final Pattern NAME_PATTERN = Pattern.compile(
                                                       "(?i)NAME=\"([^\"]+)\"");
    private static final Pattern OPTION_PATTERN = Pattern.compile(
                                        "(?i)<OPTION.+?VALUE=\"([^\"]+)\".*?>");

    public static Set<String[]> generateDefUrls(final URL url) 
                                                            throws IOException {
        if (url == null) {
            throw new NullPointerException("url");
        }
        final Set<String[]> set = new HashSet<>();
        final String content = new URLS().loadPageGet(url)[1];
        final Map<String,AbstractMap.SimpleEntry<String,Set<String>>>
                                         selectOpts = getSelectOptions(content);
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
                builder.append("=");
                builder.append(entry.getValue());
            }

            set.add(new String[] {durl.toString(), builder.toString()});
        }

        return set;
    }

    private static Map<String,AbstractMap.SimpleEntry<String,Set<String>>>
                     getSelectOptions(final String content) throws IOException {
        assert content != null;

        final Map<String,AbstractMap.SimpleEntry<String,Set<String>>> map =
                                                                new HashMap<>();
        final Matcher matSel = SELECT_PATTERN.matcher(content);

        while (matSel.find()) {
            final String selAtt = matSel.group(1);
            final Matcher matId = ID_PATTERN.matcher(selAtt);
            final Matcher matName = NAME_PATTERN.matcher(selAtt);
            if (!matId.find()) {
                throw new IOException("id attr not found: [" + selAtt + "]");
            }
            final String selectId = matId.group(1);
            if (!matName.find()) {
                throw new IOException("name attr not found: [" + selAtt + "]");
            }
            final String selectName = matName.group(1);
            final String selectContent = matSel.group(2);
            final Matcher mat2 = OPTION_PATTERN.matcher(selectContent);
            final Set<String> options = new HashSet<>();

            while (mat2.find()) {
                options.add(mat2.group(1));
            }
            map.put(selectId,
                            new AbstractMap.SimpleEntry<>(selectName, options));
        }
        return map;
    }

    private static Set<Map<String,String>> generatePostOptions(
        Map<String,AbstractMap.SimpleEntry<String,Set<String>>> selectOptions) {

        assert selectOptions != null;

        final String REGION_AND_FEDERATION_UNIT =
                                                "Região_e_Unidade_da_Federação";
        final String YEAR = "Ano";
        final String NOT_ACTIVE = "--Não-Ativa--";
        final String ALL_CATEGORIES = "TODAS_AS_CATEGORIAS__";

        final Set<Map<String,String>> set = new HashSet<>();
        final AbstractMap.SimpleEntry<String,Set<String>>
                                                  line = selectOptions.get("L");
        final AbstractMap.SimpleEntry<String,Set<String>>
                                               content = selectOptions.get("I");
        final AbstractMap.SimpleEntry<String,Set<String>>
                                time = filterDates(selectOptions.get("A"), 3);
        final String lineKey = line.getKey();
        final String colummKey = selectOptions.get("C").getKey();
        final String contentKey = content.getKey();
        final String timeKey = time.getKey();

        line.getValue().remove(REGION_AND_FEDERATION_UNIT);
        line.getValue().remove(YEAR);

        selectOptions.remove("L");
        selectOptions.remove("C");
        selectOptions.remove("I");
        selectOptions.remove("A");
        final Collection<AbstractMap.SimpleEntry<String,Set<String>>>
                                                others = selectOptions.values();

        for (String lineElem : line.getValue()) {
            for (String contentElem : content.getValue()) {
                for (String timeElem : time.getValue()) {
                    final Map<String,String> map = new HashMap<>();
                    map.put(lineKey, lineElem);
                    map.put(colummKey, NOT_ACTIVE);
                    map.put(contentKey, contentElem);
                    map.put(timeKey, timeElem);
                    for (AbstractMap.SimpleEntry<String,Set<String>> otherElem
                                                                     : others) {
                        map.put(otherElem.getKey(), ALL_CATEGORIES);
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
    
    private static AbstractMap.SimpleEntry<String,Set<String>> filterDates(
                        final AbstractMap.SimpleEntry<String,Set<String>> dates,             
                        final int max) {
    
        assert dates != null;
        assert max >= 1;
        
        final Set<String> in = dates.getValue();
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
        
        final Set<String> setDates = new HashSet<>();
        int remaining = max;
        
        for (String x2 : set2x.descendingSet()) {
            if (remaining == 0) {
                break;
            }
            setDates.add(x2);
            remaining--;
        }
        for (String x1 : set1x.descendingSet()) {
            if (remaining == 0) {
                break;
            }
            setDates.add(x1);
            remaining--;
        }
        for (String x0 : set0x.descendingSet()) {
            if (remaining == 0) {
                break;
            }
            setDates.add(x0);
            remaining--;
        }
        for (String x9 : set9x.descendingSet()) {
            if (remaining == 0) {
                break;
            }
            setDates.add(x9);
            remaining--;
        }
        
        return new AbstractMap.SimpleEntry<>(dates.getKey(), setDates);
    }
}
