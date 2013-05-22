//----------------------------------------------------------------------------//
//                                                                            //
//                        P l u g i n s C o m p a n i o n                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
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
 * Class {@code PluginsCompanion} handles the local installation of
 * JavaScript plugins.
 *
 * @author Hervé Bitteur
 */
public class PluginsCompanion
        extends AbstractCompanion
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            PluginsCompanion.class);

    /** Tooltip. */
    private static final String DESC = "<html>[This component is <i>optional</i>]"
                                       + "<br/>It installs <b>JavaScript</b> plugins"
                                       + "<br/>which can be customized manually.</html>";

    /** Environment descriptor. */
    private static final Descriptor descriptor = DescriptorFactory.getDescriptor();

    //~ Constructors -----------------------------------------------------------
    //------------------//
    // PluginsCompanion //
    //------------------//
    /**
     * Creates a new PluginsCompanion object.
     *
     * @param hasUI true for a related view
     */
    public PluginsCompanion (boolean hasUI)
    {
        super("Plugins", DESC);
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

        final URL url = Utilities.toURI(codeBase, "resources/plugins.jar")
                .toURL();

        Utilities.downloadJarAndExpand(
                "Plugins",
                url.toString(),
                descriptor.getTempFolder(),
                "plugins",
                makeTargetFolder());
    }

    //-----------------//
    // getTargetFolder //
    //-----------------//
    @Override
    protected File getTargetFolder ()
    {
        return new File(descriptor.getConfigFolder(), "plugins");
    }
}
