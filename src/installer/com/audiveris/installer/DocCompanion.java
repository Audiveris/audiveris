//----------------------------------------------------------------------------//
//                                                                            //
//                           D o c C o m p a n i o n                          //
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
 * Class {@code DocCompanion} handles the local installation of
 * documentation.
 *
 * @author Hervé Bitteur
 */
public class DocCompanion
        extends AbstractCompanion
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            DocCompanion.class);

    /** Environment descriptor. */
    private static final Descriptor descriptor = DescriptorFactory.getDescriptor();

    /** Tooltip. */
    private static final String DESC = "<html>The whole Audiveris documentation is made"
                                       + "<br/>available in one local browsable folder.</html>";

    //~ Constructors -----------------------------------------------------------
    //--------------//
    // DocCompanion //
    //--------------//
    /**
     * Creates a new DocCompanion object.
     */
    public DocCompanion ()
    {
        super("Docs", DESC);

        if (Installer.hasUI()) {
            view = new BasicCompanionView(this, 60);
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
        final URL url = Utilities.toURI(
                codeBase,
                "resources/documentation.jar")
                .toURL();
        Utilities.downloadJarAndExpand(
                "Docs",
                url.toString(),
                descriptor.getTempFolder(),
                "www",
                makeTargetFolder());

        // Also, create user config directories
        //TODO: should populate logging-config.xml, run.properties, user-actions.xml
        createDirectories(descriptor.getConfigFolder());

        // Create empty user data directories
        createDirectories(
                descriptor.getDataFolder(),
                "benches",
                "eval",
                "print",
                "scores",
                "scripts");
    }

    //-----------------//
    // getTargetFolder //
    //-----------------//
    @Override
    protected File getTargetFolder ()
    {
        return new File(descriptor.getDataFolder(), "www");
    }

    //-------------------//
    // createDirectories //
    //-------------------//
    private void createDirectories (File root,
                                    String... names)
    {
        if (root.mkdirs()) {
            logger.info("Created folder {}", root.getAbsolutePath());
        }

        for (String name : names) {
            File folder = new File(root, name);

            if (folder.mkdirs()) {
                logger.info("Created folder {}", folder.getAbsolutePath());
            }
        }
    }
}
