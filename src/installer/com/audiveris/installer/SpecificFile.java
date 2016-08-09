//----------------------------------------------------------------------------//
//                                                                            //
//                           S p e c i f i c F i l e                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

/**
 * Class {@code SpecificFile} is used to govern the installation of
 * a specific file in proper target location.
 * All source files are provided in a common .jar file ("specifics.jar" as
 * found in installer /resources folder).
 * The purpose of this class is to define the source folder and name (in the
 * common .jar file) and the target folder and name (in the user machine).
 *
 * @author Hervé Bitteur
 */
public class SpecificFile
{
    //~ Instance fields --------------------------------------------------------

    /** Full resource name in source archive. */
    public final String source;

    /** Full target file path on user machine. */
    public final String target;

    /** Target to be set as executable. */
    public final boolean isExec;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SpecificFile object.
     *
     * @param source   full resource name in source archive
     * @param target   full target path on user machine
     * @param isExec   true if target is to be set as executable
     */
    public SpecificFile (String source,
                         String target,
                         boolean isExec)
    {
        this.source = source;
        this.target = target;
        this.isExec = isExec;
    }
}
