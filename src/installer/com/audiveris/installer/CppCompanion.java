//----------------------------------------------------------------------------//
//                                                                            //
//                         C p p C o m p a n i o n                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import org.slf4j.LoggerFactory;

/**
 * Class {@code CppCompanion} handles installation of C++ runtime.
 *
 * @author Hervé Bitteur
 */
public class CppCompanion
        extends AbstractCompanion
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(
            CppCompanion.class);

    /** Environment descriptor. */
    private static final Descriptor descriptor = DescriptorFactory.getDescriptor();

    private static final String DESC = "<html>Audiveris <b>Java</b> application uses <b>JNI</b>"
                                       + "<br/>to access Tesseract <b>C++</b> software."
                                       + "<br/>We thus need a specific version of"
                                       + "<br/> C++ runtime.</html>";

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // CppCompanion //
    //--------------//
    /**
     * Creates a new CppCompanion object.
     *
     * @param hasUI true for a related view
     */
    public CppCompanion (boolean hasUI)
    {
        super("C++", DESC);

        if (hasUI) {
            view = new BasicCompanionView(this, 60);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // checkInstalled //
    //----------------//
    @Override
    public boolean checkInstalled ()
    {
        status = descriptor.isCppInstalled() ? Status.INSTALLED
                : Status.NOT_INSTALLED;

        return status == Status.INSTALLED;
    }

    //-----------//
    // doInstall //
    //-----------//
    @Override
    protected void doInstall ()
            throws Exception
    {
        descriptor.installCpp();
    }
}
