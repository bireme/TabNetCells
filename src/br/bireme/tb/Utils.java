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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Heitor Barbieri
 * date 20131002
 */
public class Utils {
    /**
     * Copies a directory from one location to another
     * @param sourceLocation path of the directory to be copied
     * @param targetLocation path of the destination of the copied directory
     * @throws IOException 
     */
    public static void copyDirectory(final File sourceLocation, 
                                     final File targetLocation) 
                                                            throws IOException {
        if (sourceLocation.isDirectory()) {
            if (!targetLocation.exists()) {
                targetLocation.mkdir();
            }

            final String[] children = sourceLocation.list();
            for (int i=0; i<children.length; i++) {
                copyDirectory(new File(sourceLocation, children[i]),
                        new File(targetLocation, children[i]));
            }
        } else {
            final OutputStream out;
            try (InputStream in = new FileInputStream(sourceLocation)) {
                out = new FileOutputStream(targetLocation);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
            out.close();
        }
    }
    
    /**
     * deletes recursevelly a directory
     * @param file the path of the directory to be deleted
     * @return  true is the directory was deleted or false if not
     */
    public static boolean deleteFile(final File file) {
        if (file == null) {
            throw new NullPointerException("file");
        }

        boolean status = true;

        if (file.isDirectory()){
            for (File child : file.listFiles()) {
                status = status && deleteFile(child);
            }
        }
        return status && file.delete();
    }
    
    /**
     * Renames a file if the name alread exists. Add an index to the name. 
     * xxxx.txt -> xxxx(1).txt
     * xxxx(1).txt -> xxxx(2).txt
     * @param path directory of the file
     * @param fname file name
     * @return a File object with the file name chenged
     * @throws IOException 
     */
    public static File renameFile(final File path,
                                  final String fname) throws IOException {
        if (path == null) {
            throw new NullPointerException("path");
        }
        if (fname == null) {
            throw new NullPointerException("fname");
        }
        
        final int dotIndex = fname.lastIndexOf('.');
        final String prefix = (dotIndex == -1) ? fname 
                                               : fname.substring(0, dotIndex);
        final String suffix = (dotIndex == -1) ? "" : fname.substring(dotIndex);
        final Pattern pat = Pattern.compile(prefix + "\\((\\d+)\\)" + suffix);
        final Matcher mat = pat.matcher("");
        final String[] fNames = path.list();
        int last = 0;
        
        for (String name : fNames) {
            mat.reset(name);
            if (mat.matches()) {
                final int idx = Integer.parseInt(mat.group(1));
                if (last < idx) {
                    last = idx;
                }
            }
        }
        final String nName = prefix + "(" + (last + 1) + ")" + suffix;            
        final File nfile = new File(path, nName);
        
        if (nfile.exists()) {
            throw new IOException("renameFile failed. File=[" + nName + "]");
        }
        return nfile;
    }
}
