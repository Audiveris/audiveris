//----------------------------------------------------------------------------//
//                                                                            //
//                        E x a m p l e s C o m p a n i o n                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;

/**
 * Class {@code ExamplesCompanion} handles the installation of a few
 * image examples for Audiveris.
 *
 * @author Hervé Bitteur
 */
public class ExamplesCompanion
        extends AbstractCompanion
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            ExamplesCompanion.class);

    private static final String DESC = "<html>[This component is <i>optional</i>]"
                                       + "<br/>It installs a few image examples.</html>";

    /** Environment descriptor. */
    private static final Descriptor descriptor = DescriptorFactory.getDescriptor();

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new ExamplesCompanion object.
     *
     * @param hasUI true for a related view
     */
    public ExamplesCompanion (boolean hasUI)
    {
        super("Examples", DESC);
        need = Need.SELECTED;

        if (hasUI) {
            view = new BasicCompanionView(this, 90);
        }
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // doInstall //
    //-----------//
    @Override
    protected void doInstall ()
            throws Exception
    {
        final URI codeBase = Jnlp.basicService.getCodeBase()
                .toURI();
        final URL url = Utilities.toURI(codeBase, "resources/examples.jar")
                .toURL();

        Utilities.downloadJarAndExpand(
                "Examples",
                url.toString(),
                descriptor.getTempFolder(),
                "examples",
                makeTargetFolder());
    }

    //-----------------//
    // getTargetFolder //
    //-----------------//
    @Override
    protected File getTargetFolder ()
    {
        return new File(descriptor.getDataFolder(), "examples");
    }
}
