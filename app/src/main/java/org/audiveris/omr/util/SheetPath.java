//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h e e t P a t h                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
package org.audiveris.omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Class <code>SheetPath</code> is a (book) Path potentially augmented with sheet number
 * within the book.
 *
 * @author Hervé Bitteur
 */
public class SheetPath
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SheetPath.class);

    /** Separating char to introduce sheet number. */
    private static final char SHEET_SEPARATOR = '#';

    //~ Instance fields ----------------------------------------------------------------------------

    /** Path to book .omr file. */
    private final Path bookPath;

    /** Sheet number within book, if any. */
    private final Integer sheetNumber;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SheetPath</code> object.
     *
     * @param bookPath Path to the book file
     */
    public SheetPath (Path bookPath)
    {
        this(bookPath, null);
    }

    /**
     * Creates a new <code>SheetPath</code> object, with a desired sheet number.
     *
     * @param bookPath    Path to the book file
     * @param sheetNumber Desired sheet number to focus upon
     */
    public SheetPath (Path bookPath,
                      Integer sheetNumber)
    {
        this.bookPath = bookPath;
        this.sheetNumber = sheetNumber;
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Getter for book path.
     *
     * @return inner book path
     */
    public Path getBookPath ()
    {
        return bookPath;
    }

    /**
     * Getter for sheet number.
     *
     * @return inner sheet number, perhaps null
     */
    public Integer getSheetNumber ()
    {
        return sheetNumber;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append(bookPath.toAbsolutePath());

        if (sheetNumber != null) {
            sb.append(SHEET_SEPARATOR).append(sheetNumber);
        }

        return sb.toString();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //--------//
    // decode //
    //--------//
    /**
     * Decode a SheetPath string value.
     *
     * @param str the string value
     * @return the decoded SheetPath
     */
    public static SheetPath decode (String str)
    {
        final int sep = str.lastIndexOf(SHEET_SEPARATOR);

        if (sep != -1) {
            try {
                final String bookString = str.substring(0, sep);
                final String sheetString = str.substring(sep + 1);
                final Path bookPath = Paths.get(bookString);
                final Integer sheetNumber = Integer.decode(sheetString);

                return new SheetPath(bookPath, sheetNumber);
            } catch (Throwable ex) {
                logger.warn("Illegal SheetPath {}, trying as a standard path.", str, ex);

                return new SheetPath(Paths.get(str));
            }
        } else {
            final Path bookPath = Paths.get(str);

            return new SheetPath(bookPath);
        }
    }
}
