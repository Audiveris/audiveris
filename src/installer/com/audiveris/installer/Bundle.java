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
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
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

    //~ Constructors -----------------------------------------------------------
    //--------//
    // Bundle //
    //--------//
    /**
     * Creates a new Bundle object.
     *
     * @param hasUI true for user interface displayed
     */
    public Bundle (boolean hasUI)
    {
        createCompanions(hasUI);

        if (hasUI) {
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
    public void installBundle ()
            throws Exception
    {
        logger.debug("installBundle");
        // Compute total installation weight
        int totalWeight = 0;

        for (Companion companion : companions) {
            if (companion.isNeeded()) {
                totalWeight += companion.getInstallWeight();
            }
        }

        int progress = 0;

        if (totalWeight > 0) {
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
    private void createCompanions (boolean hasUI)
    {
        companions.add(new LicenseCompanion(hasUI));
        companions.add(new CppCompanion(hasUI));
        companions.add(new GhostscriptCompanion(hasUI));
        companions.add(ocrCompanion = new OcrCompanion(hasUI));
        companions.add(new DocCompanion(hasUI));
        companions.add(new ExamplesCompanion(hasUI));
        companions.add(new PluginsCompanion(hasUI));
        companions.add(new TrainingCompanion(hasUI));
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
            Files.walkFileTree(folder.toPath(), new FileVisitor<Path>()
            {
                @Override
                public FileVisitResult preVisitDirectory (Path dir,
                                                          BasicFileAttributes attrs)
                        throws IOException
                {
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile (Path file,
                                                  BasicFileAttributes attrs)
                        throws IOException
                {
                    logger.debug("Deleting file {}", file);
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed (Path file,
                                                        IOException ex)
                        throws IOException
                {
                    if (ex == null) {
                        return FileVisitResult.CONTINUE;
                    } else {
                        // file visit failed
                        throw ex;
                    }
                }

                @Override
                public FileVisitResult postVisitDirectory (Path dir,
                                                           IOException ex)
                        throws IOException
                {
                    if (ex == null) {
                        logger.debug("Deleting directory {}", dir);
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        throw ex;
                    }
                }
            });
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
