/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
    
    public static boolean deleteFile(final File file) {
        assert file != null;

        boolean status = true;

        if (file.isDirectory()){
            for (File child : file.listFiles()) {
                status = status && deleteFile(child);
            }
        }
        return status && file.delete();
    }
    
    public static File renameFile(final File path,
                                  final String fname) throws IOException {
        assert path != null;
        assert fname != null;
        
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