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
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class <code>AlignValues</code>
 *
 * @author Hervé Bitteur
 */
public class AlignValues
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AlignValues.class);

    //~ Instance fields ----------------------------------------------------------------------------

    //~ Constructors -------------------------------------------------------------------------------

    //~ Methods ------------------------------------------------------------------------------------

    //~ Static Methods -----------------------------------------------------------------------------

    private static void process (int col,
                                 String from,
                                 String to)
    {
        logger.info("AlignValues col:{} from:{} to:{}", col, from, to);

        try (Reader reader = new InputStreamReader(new FileInputStream(from), "UTF-8");
                BufferedReader fin = new BufferedReader(reader);
                Writer writer = new OutputStreamWriter(new FileOutputStream(to), "UTF-8");
                BufferedWriter fout = new BufferedWriter(writer)) {
            String sin;
            while ((sin = fin.readLine()) != null) {
                String sout = null;
                final int eq = sin.indexOf('=');
                if (eq != -1) {
                    if (eq < col) {
                        // Insert spaces
                        final StringBuilder sb = new StringBuilder();
                        for (int i = col - eq; i > 0; i--) {
                            sb.append(' ');
                        }
                        sout = sin.substring(0, eq) + sb.toString() + sin.substring(eq);
                    } else if (col < eq) {
                        // Remove as many spaces as possible
                        final String gap = sin.substring(col, eq);
                        if (gap.isBlank()) {
                            sout = sin.substring(0, col) + sin.substring(eq);
                        } else {
                            for (int i = eq - 1; i >= col; i--) {
                                if (sin.charAt(i) != ' ') {
                                    sout = sin.substring(0, i + 1) + sin.substring(eq);
                                    break;
                                }
                            }
                        }
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

    public static void main (String[] args)
    {
        final Path p = Paths.get(".").resolve("src/main/java/org/audiveris/omr/glyph/resources");
        logger.info("p: {}", p);
        process(
                44, // 44
                p.resolve("Shape_fi.properties").toString(),
                p.resolve("Shape_fi.properties.txt").toString());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
