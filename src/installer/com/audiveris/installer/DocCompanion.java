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
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import javax.swing.JOptionPane;

/**
 * Class {@code DocCompanion} handles the local installation of
 * documentation.
 * <p>Since it's the only mandatory general purpose companion, it also handles:
 * <ul>
 * <li>the creation of the data directories
 * ("benches", "eval", "print", "scores", "scripts").</li>
 * <li>the creation of specific files (.bat and .sh)</li>
 * </ul>
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
    private static final String DESC =
            "<html>The whole Audiveris documentation is made"
            + "<br/>available in one local browsable folder.</html>";

    /** List of sub-folders created in Data folder. */
    private static final String[] dataFolders = new String[]{
        "benches", "eval", "print", "scores", "scripts"};

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
    //----------------//
    // checkInstalled //
    //----------------//
    @Override
    public boolean checkInstalled ()
    {
        if (getTargetFolder().exists()
            && descriptor.getConfigFolder().exists()
            && checkDirectories(descriptor.getDataFolder(), dataFolders)
            && checkSpecificFiles()) {
            status = Status.INSTALLED;
        } else {
            status = Status.NOT_INSTALLED;
        }

        return status == Status.INSTALLED;
    }

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
        createDirectories(descriptor.getDataFolder(), dataFolders);

        // Install some specific files
        installSpecificFiles();
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

    //------------------//
    // checkDirectories //
    //------------------//
    private boolean checkDirectories (File root,
                                      String... names)
    {
        if (!root.exists()) {
            return false;
        }

        for (String name : names) {
            File folder = new File(root, name);

            if (!folder.exists()) {
                return false;
            }
        }

        return true;
    }

    //----------------------//
    // installSpecificFiles //
    //----------------------//
    /**
     * Install specific files for the current OS.
     * For the time being, we assume that all these files are located in
     * writable locations. If this assumption turns out to be false, we'll have
     * to fall back to posting shell commands (Not yet implemented).
     *
     * @throws Exception
     */
    private void installSpecificFiles ()
            throws Exception
    {
        // Download the global archive of specific files
        final URI codeBase = Jnlp.basicService.getCodeBase().toURI();
        final URL url = Utilities.toURI(codeBase, "resources/specifics.jar")
                .toURL();
        final String jarName = new File(url.toString()).getName();
        final File jarFile = new File(descriptor.getTempFolder(), jarName);
        Utilities.download(url.toString(), jarFile);

        final JarFile jar = new JarFile(jarFile);

        // Process each desired specific file
        for (SpecificFile specificFile : descriptor.getSpecificFiles()) {
            // Make sure the target folder exists
            final Path target = Paths.get(specificFile.target);
            final Path parent = target.getParent();

            if (!Files.exists(parent)) {
                Files.createDirectories(parent);
                logger.info("Created dir {}", parent);
            }

            // Copy the source entry to the target file
            ZipEntry entry = jar.getEntry(specificFile.source);
            if (entry != null) {
                try (InputStream is = jar.getInputStream(entry)) {
                    Files.copy(is, target, StandardCopyOption.REPLACE_EXISTING);
                    logger.info("Specific {} copied", target);
                    if (specificFile.isExec) {
                        try {
                            descriptor.setExecutable(
                                    target.toAbsolutePath().toString());
                        } catch (Exception ignored) {
                            // User already warned, proceed to next file
                        }
                    }
                }
            } else {
                // We can live without these files, so warn the user
                // but keep the installation going.
                String msg = "No entry " + specificFile.source + "\n in " + url;
                logger.warn(msg);
                JOptionPane.showMessageDialog(
                        Installer.getFrame(),
                        msg,
                        "Entry not found",
                        JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    //--------------------//
    // checkSpecificFiles //
    //--------------------//
    private boolean checkSpecificFiles ()
    {
        for (SpecificFile specificFile : descriptor.getSpecificFiles()) {
            if (!new File(specificFile.target).exists()) {
                return false;
            }
        }

        return true;
    }
}
