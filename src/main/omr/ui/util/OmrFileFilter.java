//----------------------------------------------------------------------------//
//                                                                            //
//                         O m r F i l e F i l t e r                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import java.io.File;

/**
 * Class {@code OmrFileFilter} is a special file filter, based on file
 * extensions
 *
 * @author Hervé Bitteur
 */
public class OmrFileFilter
        extends javax.swing.filechooser.FileFilter
        implements java.io.FilenameFilter
{
    //~ Instance fields --------------------------------------------------------

    /** User readable description */
    private final String description;

    /** Array of accepted file extensions */
    private final String[] extensions;

    //~ Constructors -----------------------------------------------------------
    //---------------//
    // OmrFileFilter //
    //---------------//
    /**
     * Create a file filter, with only one file extension to consider
     *
     * @param extension the only file extension
     */
    public OmrFileFilter (String extension)
    {
        this(null, extension);
    }

    //---------------//
    // OmrFileFilter //
    //---------------//
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

    //---------------//
    // OmrFileFilter //
    //---------------//
    /**
     * Create a file filter with a whole array of file name extensions, and
     * the related user description.
     *
     * @param description the user readable description
     * @param extensions  the array of allowed file extensions
     */
    public OmrFileFilter (String description,
                          String[] extensions)
    {
        if (description == null) {
            // Use first extension and # of extensions as desc.
            this.description = extensions[0] + "{" + extensions.length + "}";
        } else {
            this.description = description;
        }

        this.extensions = extensions.clone();
    }

    //~ Methods ----------------------------------------------------------------
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
