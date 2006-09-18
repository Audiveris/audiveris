//-----------------------------------------------------------------------//
//                                                                       //
//                          F i l e F i l t e r                          //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.util;

import omr.util.FileUtil;

import java.io.File;

/**
 * Class <code>FileFilter</code> is a dialog to let the user choose
 * interactively among a hierarchy of files.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class FileFilter
    extends javax.swing.filechooser.FileFilter
{
    //~ Instance fields --------------------------------------------------------

    private final String   description;
    private final String[] extensions;

    //~ Constructors -----------------------------------------------------------

    //------------//
    // FileFilter //
    //------------//
    /**
     * Create a file filter, with only one file extension to consider
     *
     * @param extension the only file extension
     */
    public FileFilter (String extension)
    {
        this(null, extension);
    }

    //------------//
    // FileFilter //
    //------------//
    /**
     * Create a file filter, with only one file extension to consider, and
     * a related description.
     *
     * @param description the description to be displayed
     * @param extension   the only file name extension to consider
     */
    public FileFilter (String description,
                       String extension)
    {
        this(description, new String[] { extension });
    }

    //------------//
    // FileFilter //
    //------------//
    /**
     * Create a file filter with a whole array of file name extensions, and
     * the related user description.
     *
     * @param description the user readable description
     * @param extensions  the array of allowed file extensions
     */
    public FileFilter (String   description,
                       String[] extensions)
    {
        if (description == null) {
            // Use first extension and # of extensions as desc.
            this.description = extensions[0] + "{" + extensions.length + "}";
        } else {
            this.description = description;
        }

        // Convert array to lower case, once for all
        this.extensions = extensions.clone();

        for (int i = 0; i < this.extensions.length; i++) {
            this.extensions[i] = this.extensions[i].toLowerCase();
        }
    }

    //~ Methods ----------------------------------------------------------------

    //----------------//
    // getDescription //
    //----------------//
    /**
     * Report the filter description
     *
     * @return the description of this filter
     */
    public String getDescription ()
    {
        return description;
    }

    //--------//
    // accept //
    //--------//
    /**
     * Return a boolean to state whether the provided file should be
     * considered. Directories are always accepted, and tests on file name
     * extensions are not case sensitive
     *
     * @param f the candidate file entity
     *
     * @return true if the file is OK, false otherwise
     */
    public boolean accept (File f)
    {
        if (f.isDirectory()) {
            return true;
        }

        final String extension = FileUtil.getExtension(f);

        if (extension != null) {
            for (int i = 0, n = extensions.length; i < n; i++) {
                if (extension.equals(extensions[i])) {
                    return true;
                }
            }
        }

        return false;
    }
}
