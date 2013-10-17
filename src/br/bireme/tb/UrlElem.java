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

import java.net.URL;
import java.util.Map;

/**
 *
 * @author Heitor Barbieri
 * date 20131008
 */
class UrlElem implements Comparable<UrlElem> {
    URL father; // url of the html page with the Qualif Record and csv links
    String fatherParams; // POST parameters
    Map<String, String> tableOptions; // def tables options
    URL csv; // url of the csv page
    URL qualifRec; // url of the Qualification Record page

    @Override
    public int compareTo(final UrlElem other) {
        final String url = csv.toString();
        final int ret;
        if (url == null) {
            ret = ((other == null) || (other.father == null)) ? 0 : -1;
        } else {
            ret = (other == null) ? +1 : url.compareTo(other.csv.toString());
        }
        return ret;
    }    
}
