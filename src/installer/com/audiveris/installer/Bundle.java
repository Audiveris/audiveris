//----------------------------------------------------------------------------//
//                                                                            //
//                                 B u n d l e                                //
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Class {@code Bundle} handles a sequence of Companions to install
 * (or to uninstall).
 *
 * @author Hervé Bitteur
 */
public class Bundle
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(Bundle.class);

    /** Environment descriptor. */
    private static final Descriptor descriptor = DescriptorFactory.getDescriptor();

    //~ Instance fields --------------------------------------------------------
    /** Sequence of companions. */
    private final List<Companion> companions = new ArrayList<>();

    /** The install target folder, initialized to null. (not used) */
    private File installFolder;

    /** The specific companion in charge of OCR languages. */
    private OcrCompanion ocrCompanion;

    /** Related view on this bundle, if any. */
    private BundleView view;

    /** Flag for cancellation. */
    private boolean cancelled = false;

    /** Global list of commands to be run with admin privileges. */
    private final List<String> commands = new ArrayList<String>();

    //~ Constructors -----------------------------------------------------------
    //--------//
    // Bundle //
    //--------//
    /**
     * Creates a new Bundle object.
     */
    public Bundle (boolean hasUI)
    {
        createCompanions();

        if (Installer.hasUI()) {
            view = new BundleView(this);

            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run ()
                {
                    view.setVisible(true);
                }
            });
        }
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // appendCommand //
    //---------------//
    /**
     * Append a command to the global list of admin commands.
     *
     * @param command the command to append
     */
    public void appendCommand (String command)
    {
        if (!command.isEmpty()) {
            logger.info("Posting: {}", command);
            commands.add(command);
        }
    }

    //---------//
    // getView //
    //---------//
    public BundleView getView ()
    {
        return view;
    }

    //-----------------//
    // getOcrCompanion //
    //-----------------//
    public OcrCompanion getOcrCompanion ()
    {
        return ocrCompanion;
    }

    //--------------------//
    // checkInstallations //
    //--------------------//
    public void checkInstallations ()
    {
        for (Companion companion : companions) {
            boolean installed = companion.checkInstalled();
            if (companion.isNeeded() && !installed) {
                logger.debug("Installation needed for {}", companion.getTitle());
            } else {
                logger.debug("{} needs no action", companion.getTitle());
            }

            if (companion.getView() != null) {
                companion.getView()
                        .update();
            }
        }
    }

    //-------//
    // close //
    //-------//
    public void close ()
    {
        if (view != null) {
            view.setVisible(false);
            view.dispose();
        }

        Installer.latch.countDown();
        logger.info("Installer has stopped.");
    }

    //---------------//
    // getCompanions //
    //---------------//
    public List<Companion> getCompanions ()
    {
        return companions;
    }

    //---------------//
    // installBundle //
    //---------------//
    /**
     * Install the bundle of needed companions.
     * To avoid mixing system and user domains, the installation will proceed
     * in two phases: first a user phase if needed (to install in proper user
     * location using Java code) then a system phase if needed (to install
     * system stuff using shell commands).
     *
     * @throws Exception
     */
    public void installBundle ()
            throws Exception
    {
        logger.debug("installBundle");
        commands.clear();

        // Compute total installation weight
        int totalWeight = 0;

        for (Companion companion : companions) {
            if (companion.isNeeded()) {
                totalWeight += companion.getInstallWeight();
            }
        }

        int progress = 0;

        if (totalWeight > 0) {
            // First phase in user mode
            for (Companion companion : companions) {
                if (companion.isNeeded()) {
                    // Visual information
                    int weight = companion.getInstallWeight();
                    Jnlp.extensionInstallerService.updateProgress(
                            (progress * 100) / totalWeight);
                    logger.debug("Processing {}", companion.getTitle());
                    Jnlp.extensionInstallerService.setHeading(
                            companion.getIndex() + ") Processing " + companion.getTitle());
                    if (view != null) {
                        Thread.sleep(100); // Let user see infos for a while
                    }

                    // Install
                    if (!companion.checkInstalled()) {
                        companion.install();
                    } else {
                        logger.debug("[{} is already installed]", companion.getTitle());
                    }
                    progress += weight;
                    logger.debug("Progress: {}/{}", progress, totalWeight);
                }
            }

            // Second phase for commands in system mode
            if (!commands.isEmpty()) {
                Jnlp.extensionInstallerService.setHeading("System commands");
                logger.info("\nFinal commands to be run at admin level:");
                for (String command : commands) {
                    logger.info("   {}", command);
                }
                if (view != null) {
                    Thread.sleep(100); // Let user see infos for a while
                }
                JOptionPane.showMessageDialog(
                        Installer.getFrame(),
                        "To complete installation, you will now be prompted for"
                        + " administration privileges",
                        commands.size() + " Additional command(s) to be run",
                        JOptionPane.INFORMATION_MESSAGE);

                // One shell for all commands
                try {
                    descriptor.runShell(!Installer.isAdmin, commands);
                } catch (Exception ex) {
                    // Notify failure
                    Jnlp.extensionInstallerService.installFailed();
                    throw ex;
                }
            }

            // Update status for all companions
            checkInstallations();
        }

        Jnlp.extensionInstallerService.updateProgress(100);
    }

    //-----------------//
    // uninstallBundle //
    //-----------------//
    public void uninstallBundle ()
            throws Throwable
    {
        for (Companion companion : companions) {
            if (companion.checkInstalled()) {
                companion.uninstall();
            }
        }
    }

    //------------------//
    // createCompanions //
    //------------------//
    private void createCompanions ()
    {
        companions.add(new LicenseCompanion());
        companions.add(new CppCompanion());
        companions.add(new GhostscriptCompanion());
        companions.add(ocrCompanion = new OcrCompanion());
        companions.add(new DocCompanion());
        companions.add(new ExamplesCompanion());
        companions.add(new PluginsCompanion());
        companions.add(new TrainingCompanion());
    }

    //------------------//
    // createTempFolder //
    //------------------//
    /**
     * Create a fresh temp folder.
     */
    public void createTempFolder ()
            throws IOException
    {
        File folder = descriptor.getTempFolder();
        if (!folder.exists()) {
            // Create the folder
            if (folder.mkdirs()) {
                logger.info("Created folder {}", folder.getAbsolutePath());
            }
        }
    }

    //------------------//
    // deleteTempFolder //
    //------------------//
    /**
     * Remove the temp folder, with all its content.
     */
    public void deleteTempFolder ()
            throws IOException
    {
        File folder = descriptor.getTempFolder();
        if (folder.exists()) {
            // Clean up everything in the folder
            while (true) {
                try {
                    TreeRemover.remove(folder.toPath());
                    break;
                } catch (IOException ex) {
                    if (view != null) {
                        int opt = JOptionPane.showConfirmDialog(
                                Installer.getFrame(),
                                "Cannot delete installation temporary folder "
                                + "\nlocated at " + folder
                                + "\n"
                                + "\nMake sure no window or process is using it"
                                + ", then press OK",
                                "Installation temporary folder",
                                JOptionPane.OK_CANCEL_OPTION,
                                JOptionPane.WARNING_MESSAGE);
                        if (opt != JOptionPane.OK_OPTION) {
                            throw ex;
                        }
                    } else {
                        throw ex;
                    }
                }
            }
        }
    }

//    //------------------//
//    // getInstallFolder //
//    //------------------//
//    /**
//     * Report the install folder (as currently defined).
//     *
//     * @return the installFolder
//     */
//    public File getInstallFolder ()
//    {
//        if (installFolder == null) {
//            // No user selection done, so let's use default folder
//            installFolder = descriptor.getInstallFolder();
//            if (!installFolder.exists()) {
//                if (installFolder.mkdirs()) {
//                    logger.info("Created folder {}", installFolder.getAbsolutePath());
//                }
//            }
//        }
//
//        return installFolder;
//    }
//
//    //------------------//
//    // setInstallFolder //
//    //------------------//
//    /**
//     * Assign the install folder.
//     *
//     * @param installFolder the installFolder to set
//     */
//    public void setInstallFolder (File installFolder)
//    {
//        this.installFolder = installFolder;
//
//        // Update installation checks WRT this new folder
//        checkInstallations();
//    }
    //-------------//
    // isCancelled //
    //-------------//
    /**
     * Check for cancellation.
     *
     * @return the cancelled flag value
     */
    public boolean isCancelled ()
    {
        return cancelled;
    }

    //--------------//
    // setCancelled //
    //--------------//
    /**
     * Set cancellation flag.
     *
     * @param cancelled the cancelled value to set
     */
    public void setCancelled (boolean cancelled)
    {
        this.cancelled = cancelled;
    }
}
