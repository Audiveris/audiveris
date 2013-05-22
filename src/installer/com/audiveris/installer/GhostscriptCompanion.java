//----------------------------------------------------------------------------//
//                                                                            //
//                    G h o s t s c r i p t C o m p a n i o n                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import org.slf4j.LoggerFactory;

/**
 * Class {@code GhostscriptCompanion} handles the installation of
 * Ghostscript.
 *
 * @author Hervé Bitteur
 */
class GhostscriptCompanion
        extends AbstractCompanion
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(
            GhostscriptCompanion.class);

    private static final String DESC = "<html><b>Ghostscript</b> component is mandatory."
                                       + "<br/>It allows to process <b>PDF</b> input files.</html>";

    /** Environment descriptor. */
    private static final Descriptor descriptor = DescriptorFactory.getDescriptor();

    //~ Constructors -----------------------------------------------------------
    //----------------------//
    // GhostscriptCompanion //
    //----------------------//
    /**
     * Creates a new GhostscriptCompanion object.
     *
     * @param hasUI true for a related view
     */
    public GhostscriptCompanion (boolean hasUI)
    {
        super("Ghostscript", DESC);

        if (hasUI) {
            view = new BasicCompanionView(this, 90);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // checkInstalled //
    //----------------//
    @Override
    public boolean checkInstalled ()
    {
        status = descriptor.isGhostscriptInstalled() ? Status.INSTALLED
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
        descriptor.installGhostscript();
    }
}
