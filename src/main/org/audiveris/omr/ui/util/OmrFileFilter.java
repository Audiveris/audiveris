//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   O m r F i l e F i l t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.ui.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;

import javax.swing.filechooser.FileFilter;

/**
 * Class {@code OmrFileFilter} is a special file filter, based on file extensions
 *
 * @author Hervé Bitteur
 */
public class OmrFileFilter
        extends FileFilter
        implements FilenameFilter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** User readable description */
    private final String description;

    /** Array of accepted file extensions */
    private final String[] extensions;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a file filter, with only one file extension to consider
     *
     * @param extension the only file extension
     */
    public OmrFileFilter (String extension)
    {
        this(null, extension);
    }

    /**
     * Create a file filter, with only one file extension to consider, and
     * a related description.
     *
     * @param description the description to be displayed
     * @param extension   the only file name extension to consider
     */
    public OmrFileFilter (String description,
                          String extension)
    {
        this(description, new String[]{extension});
    }

    /**
     * Create a file filter with a whole array of file name extensions, and
     * the related user description.
     *
     * @param description the user readable description
     * @param extensions  the array of allowed file extensions
     */
    public OmrFileFilter (String description,
                          String... extensions)
    {
        if (description == null) {
            this.description = (extensions.length > 1) ? Arrays.toString(extensions) : extensions[0];
        } else {
            this.description = description;
        }

        this.extensions = extensions.clone();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    /**
     * Tests if a specified file should be included in a file list.
     * Directories are always accepted, and tests on file name extensions are
     * not case sensitive.
     *
     * @param f the candidate file entity
     * @return true if the file is OK, false otherwise
     */
    @Override
    public boolean accept (File f)
    {
        if (f.isDirectory()) {
            return true;
        }

        String path = f.getAbsolutePath();

        for (String ext : extensions) {
            if (path.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    //--------//
    // accept //
    //--------//
    /**
     * Tests if a specified file should be included in a file list.
     *
     * @param dir  the directory in which the file was found.
     * @param name the name of the file.
     * @return {@code true} if and only if the name should be
     *         included in the file list; {@code false} otherwise.
     */
    @Override
    public boolean accept (File dir,
                           String name)
    {
        return accept(new File(dir, name));
    }

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report the filter description
     *
     * @return the description of this filter
     */
    @Override
    public String getDescription ()
    {
        return description;
    }
}
