//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      A l i g n V a l u e s                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Class <code>AlignValues</code> is a utility meant to pretty-format ".properties" files.
 *
 * @author Hervé Bitteur
 */
public class AlignValues
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AlignValues.class);

    //~ Static Methods -----------------------------------------------------------------------------

    private static int getCol (String from)
    {
        int col = 0;

        try (Reader reader = new InputStreamReader(new FileInputStream(from), "UTF-8");
                BufferedReader fin = new BufferedReader(reader)) {
            String sin;
            while ((sin = fin.readLine()) != null) {
                sin = sin.trim();

                if (sin.isEmpty() || sin.charAt(0) == '#') {
                    // Blank or comment line
                } else {
                    final int eq = sin.indexOf('=');

                    if (eq != -1) {
                        // Stop at first non-blank char just before '='
                        for (int i = eq - 1; i >= 0; i--) {
                            final char c = sin.charAt(i);

                            if (c != ' ') {
                                col = Math.max(col, i + 2); // Leave a blank before '='
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Make sure col is a tab value
        col = 4 * Math.ceilDiv(col, 4);
        System.out.println("AlignValues. col: " + col);

        return col;
    }

    private static void align (int col,
                               String from)
    {
        final String to = from + ".aligned";

        try (Reader reader = new InputStreamReader(new FileInputStream(from), "UTF-8");
                BufferedReader fin = new BufferedReader(reader);
                Writer writer = new OutputStreamWriter(new FileOutputStream(to), "UTF-8");
                BufferedWriter fout = new BufferedWriter(writer)) {
            String sin;
            while ((sin = fin.readLine()) != null) {
                String sout = null;
                final int eq = sin.indexOf('=');
                if (eq != -1) {
                    // Index of first non-blank char after '=', if any
                    Integer nb = null;
                    for (int i = eq + 1; i < sin.length(); i++) {
                        if (sin.charAt(i) != ' ') {
                            nb = i;
                            break;
                        }
                    }

                    if (eq <= col) {
                        // Insert needed spaces before '='
                        final StringBuilder sb = new StringBuilder();
                        for (int i = col - eq; i > 0; i--) {
                            sb.append(' ');
                        }
                        sout = sin.substring(0, eq) + sb.toString() + "=";
                    } else if (col < eq) {
                        // Remove as many spaces as possible before '='
                        final String gap = sin.substring(col, eq);
                        if (gap.isBlank()) {
                            sout = sin.substring(0, col) + "=";
                        } else {
                            for (int i = eq - 1; i >= col; i--) {
                                if (sin.charAt(i) != ' ') {
                                    sout = sin.substring(0, i + 1) + "=";
                                    break;
                                }
                            }
                        }
                    }

                    // After '='
                    if (nb != null) {
                        sout = sout + " " + sin.substring(nb);
                    }
                }

                if (sout == null) {
                    sout = sin;
                }

                fout.write(sout);
                fout.newLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void process (Path dir)
    {
        try {
            Files.walkFileTree(dir, new SimpleFileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory (Path dir,
                                                          BasicFileAttributes attrs)
                    throws IOException
                {
                    final Path path = dir.toAbsolutePath().normalize();
                    logger.trace("path: {}", path);
                    final String fileName = path.getFileName().toString();

                    if (fileName.equals("build") //
                            || fileName.startsWith("."))
                        return FileVisitResult.SKIP_SUBTREE;
                    else
                        return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile (Path file,
                                                  BasicFileAttributes attrs)
                    throws IOException
                {
                    final String fileName = file.getFileName().toString();

                    if (fileName.endsWith(".properties")) {
                        final Path path = file.toAbsolutePath().normalize();
                        System.out.println(path.toString());

                        final int col = getCol(path.toString());
                        align(col, path.toString());
                    }

                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException ex) {
            logger.warn("Error processing {}", dir, ex);
        }

    }

    public static void main (String[] args)
    {
        final Path dir = Paths.get(".").resolve("src/main/java/org/audiveris/omr");
        System.out.println("AlignValues. dir: " + dir);

        process(dir);
    }
}
