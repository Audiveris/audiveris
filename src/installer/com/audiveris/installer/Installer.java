//----------------------------------------------------------------------------//
//                                                                            //
//                             I n s t a l l e r                              //
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

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;

import javax.jnlp.UnavailableServiceException;
import javax.swing.JOptionPane;

/**
 * Class {@code Installer} is the main class for installation of
 * Audiveris complete bundle using Java Web Start technology.
 * <p>
 * We can assume that, thanks to Java Web Start, a proper JRE version is made
 * available for this installer before it is launched.
 * Since subsequent Audiveris application needs java 1.7 as of this writing, we
 * can base this Installer on Java 1.7 as well.
 *
 * @author Hervé Bitteur
 */
public class Installer
{
    //~ Static fields/initializers ---------------------------------------------

    static {
        LogUtilities.initialize();
    }

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            Installer.class);

    /** Single instance. */
    private static Installer INSTANCE;

    /** Flag an installation. */
    private static boolean isInstall;

    /** Flag an uninstallation. */
    private static boolean isUninstall;

    /** Flag a process running as administrator. */
    private static boolean isAdmin;

    /** Flag an interactive (vs batch) run. */
    private static boolean hasUI;

    /** To force main thread to wait for UI completion. */
    public static CountDownLatch latch = new CountDownLatch(1);

    //~ Instance fields --------------------------------------------------------
    //
    /** Bundle of companions to install (or insinstall). */
    private final Bundle bundle;

    //~ Constructors -----------------------------------------------------------
    //-----------//
    // Installer //
    //-----------//
    /**
     * Creates a new Installer object.
     */
    private Installer ()
    {
        Jnlp.extensionInstallerService.setHeading(
                "Running Audiveris installer.");

        // Handling the bundle of all companions, with potential UI
        bundle = new Bundle(hasUI);
    }

    //~ Methods ----------------------------------------------------------------
    //-----------//
    // getBundle //
    //-----------//
    /**
     * Report the bundle of companions to process.
     *
     * @return the bundle of companions.
     */
    public static Bundle getBundle ()
    {
        if (INSTANCE == null) {
            return null;
        }

        return INSTANCE.bundle;
    }

    //----------//
    // getFrame //
    //----------//
    /**
     * Convenient method to get access to the Installer frame.
     * Useful for example to locate dialogs with respect to this frame.
     *
     * @return the Installer frame
     */
    public static BundleView getFrame ()
    {
        Bundle bundle = getBundle();

        return (bundle != null) ? bundle.getView() : null;
    }

    //-------------//
    // getInstance //
    //-------------//
    /**
     * Report the single instance.
     *
     * @return the Installer single instance
     */
    public static Installer getInstance ()
    {
        return INSTANCE;
    }

    //-------//
    // hasUI //
    //-------//
    /**
     * Report whether the Installer is run in interactive mode.
     *
     * @return true if interactive
     */
    public static boolean hasUI ()
    {
        return hasUI;
    }

    //------//
    // main //
    //------//
    /**
     * The Installer.main method is called by Java Web Start on two
     * occasions within the application cycle: at installation time
     * (with single argument {@code install}) and at de-installation
     * time (with single argument {@code uninstall}).
     *
     * @param args order and case of arguments are not relevant.
     *             We expect "install" for installation and "uninstall" for
     *             de-installation.
     * @throws UnavailableServiceException
     */
    public static void main (String[] args)
            throws UnavailableServiceException
    {
        if (args.length == 0) {
            throw new IllegalArgumentException("No argument for Installer");
        }

        // Do we want a bundle view?
        hasUI = true;

        Descriptor descriptor = DescriptorFactory.getDescriptor();

        isInstall = isArgSet("install", args);
        isUninstall = isArgSet("uninstall", args);
        isAdmin = descriptor.isAdmin();

        if (isAdmin) {
            logger.info("Running as administrator.");
        }

        if (isInstall) {
            if (isAdmin) {
                // We have the needed priviledges, let's go
                INSTANCE = new Installer();
                INSTANCE.install();
            } else {
                // Relaunch with administrator priviledges
                logger.info(
                        "Launching a new installer at Administrator level.");
                descriptor.relaunchAsAdmin();
                logger.info("Back from Administrator installer.");
            }
        } else if (isUninstall) {
            INSTANCE = new Installer();
            INSTANCE.uninstall();
        } else {
            // Not a valid argument array
            logger.error(
                    "Illegal Installer arguments: {}",
                    Arrays.deepToString(args));
        }
    }

    //---------//
    // install //
    //---------//
    /**
     * Launch installation.
     */
    public void install ()
    {
        logger.debug("install");

        try {
            // Use a fresh install folder
            bundle.deleteTempFolder();
            bundle.createTempFolder();

            // Get all initial installation statuses
            bundle.checkInstallations();

            ///installerService.hideProgressBar();
            if (hasUI) {
                logger.info(
                        "\n   Please:\n"
                        + //                    "- Select Audiveris installation folder,\n" +
                        "- Add or remove OCR-supported languages,\n"
                        + "- Check or uncheck optional components,\n"
                        + "- Launch installation.\n");

                // Wait until UI has finished...
                latch.await();

                // To avoid immediate closing...
                //                JOptionPane.showMessageDialog(
                //                    Installer.getFrame(),
                //                    "Closing time!",
                //                    "This is the end",
                //                    JOptionPane.INFORMATION_MESSAGE);
            } else {
                bundle.installBundle();

                Jnlp.extensionInstallerService.installSucceeded(false);
            }
        } catch (Throwable ex) {
            logger.error("Error encountered", ex);
            JOptionPane.showMessageDialog(
                    Installer.getFrame(),
                    "Installation has failed",
                    "Installation completion",
                    JOptionPane.WARNING_MESSAGE);

            Jnlp.extensionInstallerService.installFailed();
        } finally {
            bundle.close();
        }
    }

    //-----------//
    // uninstall //
    //-----------//
    /**
     * Launch uninstallation.
     */
    public void uninstall ()
    {
        logger.debug("uninstall");

        try {
            bundle.uninstallBundle();
            JOptionPane.showMessageDialog(
                    Installer.getFrame(),
                    "Bundle successfully uninstalled",
                    "Uninstallation completion",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (Throwable ex) {
            JOptionPane.showMessageDialog(
                    Installer.getFrame(),
                    "Uninstallation has failed: " + ex,
                    "Uninstallation completion",
                    JOptionPane.WARNING_MESSAGE);
        } finally {
            //bundle.close();
        }
    }

    //----------//
    // isArgSet //
    //----------//
    /**
     * Check whether the provided name appears in the args array.
     *
     * @param name the name of interest
     * @param args the program arguments
     * @return true if found, false otherwise
     */
    private static boolean isArgSet (String name,
                                     String[] args)
    {
        for (String arg : args) {
            if (arg.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }
}
