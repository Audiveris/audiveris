//----------------------------------------------------------------------------//
//                                                                            //
//                        T r a i n i n g C o m p a n i o n                   //
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
 * Class {@code TrainingCompanion} handles the installation of glyph
 * samples used for (re)training Audiveris classifier.
 *
 * @author Hervé Bitteur
 */
public class TrainingCompanion
        extends AbstractCompanion
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            TrainingCompanion.class);

    /** Tooltip. */
    private static final String DESC = "<html>[This component is <i>optional</i>]"
                                       + "<br/>It installs samples that allow"
                                       + "<br/>to retrain the shape classifier.</html>";

    /** Environment descriptor. */
    private static final Descriptor descriptor = DescriptorFactory.getDescriptor();

    //~ Constructors -----------------------------------------------------------
    //-------------------//
    // TrainingCompanion //
    //-------------------//
    /**
     * Creates a new TrainingCompanion object.
     *
     * @param hasUI true for a related view
     */
    public TrainingCompanion (boolean hasUI)
    {
        super("Training", DESC);
        need = Need.NOT_SELECTED;

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

        final URL url = Utilities.toURI(codeBase, "resources/train.jar")
                .toURL();

        Utilities.downloadJarAndExpand(
                "Training",
                url.toString(),
                descriptor.getTempFolder(),
                "train",
                makeTargetFolder());
    }

    //-----------------//
    // getTargetFolder //
    //-----------------//
    @Override
    protected File getTargetFolder ()
    {
        return new File(descriptor.getDataFolder(), "train");
    }
}
