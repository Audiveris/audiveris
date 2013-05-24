//----------------------------------------------------------------------------//
//                                                                            //
//                    A b s t r a c t C o m p a n i o n                       //
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

/**
 * Class {@code AbstractCompanion} is a basis for Companion
 * implementations.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractCompanion
        implements Companion
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            AbstractCompanion.class);

    /** To display a serial number for each companion. */
    private static int globalIndex = 0;

    //~ Instance fields --------------------------------------------------------
    /** Companion ID. */
    protected final int index;

    /** Companion title. */
    protected final String title;

    /** Companion description. */
    protected final String description;

    /** View on this companion, if any. */
    protected CompanionView view;

    /** Need for this companion. */
    protected Need need = Need.MANDATORY;

    /** Current installation status. */
    protected Status status = Status.NOT_INSTALLED;

    //~ Constructors -----------------------------------------------------------
    //-------------------//
    // AbstractCompanion //
    //-------------------//
    /**
     * Creates a new AbstractCompanion object.
     *
     * @param title the assigned title
     */
    public AbstractCompanion (String title,
                              String description)
    {
        this.index = ++globalIndex;
        this.title = title;
        this.description = description;
    }

    //~ Methods ----------------------------------------------------------------
    //----------------//
    // checkInstalled //
    //----------------//
    @Override
    public boolean checkInstalled ()
    {
        status = getTargetFolder()
                .exists() ? Status.INSTALLED : Status.NOT_INSTALLED;

        return status == Status.INSTALLED;
    }

    //----------------//
    // getDescription //
    //----------------//
    @Override
    public String getDescription ()
    {
        return description;
    }

    //----------//
    // getIndex //
    //----------//
    @Override
    public int getIndex ()
    {
        return index;
    }

    //------------------//
    // getInstallWeight //
    //------------------//
    @Override
    public int getInstallWeight ()
    {
        return isNeeded() ? 1 : 0;
    }

    //---------//
    // getNeed //
    //---------//
    @Override
    public Need getNeed ()
    {
        return need;
    }

    //-----------//
    // getStatus //
    //-----------//
    @Override
    public Status getStatus ()
    {
        return status;
    }

    //----------//
    // getTitle //
    //----------//
    @Override
    public String getTitle ()
    {
        return title;
    }

    //--------------------//
    // getUninstallWeight //
    //--------------------//
    @Override
    public int getUninstallWeight ()
    {
        return checkInstalled() ? 1 : 0;
    }

    //---------//
    // getView //
    //---------//
    @Override
    public CompanionView getView ()
    {
        return view;
    }

    //---------//
    // install //
    //---------//
    @Override
    public void install ()
            throws Exception
    {
        try {
            startInstallation();
            doInstall();
            completeInstallation();
        } catch (Exception ex) {
            abortInstallation(ex);
            throw ex;
        } finally {
            checkInstalled();
            updateView();
        }
    }

    //----------//
    // isNeeded //
    //----------//
    @Override
    public boolean isNeeded ()
    {
        return need != Need.NOT_SELECTED;
    }

    //---------//
    // setNeed //
    //---------//
    @Override
    public void setNeed (Need need)
    {
        this.need = need;
    }

    //-----------//
    // uninstall //
    //-----------//
    @Override
    public void uninstall ()
    {
        try {
            startUninstallation();
            doUninstall();
            completeUninstallation();
        } catch (Exception ex) {
            abortUninstallation(ex);
        }
    }

    //-----------//
    // doInstall //
    //-----------//
    /**
     * Actual Installation.
     */
    protected abstract void doInstall ()
            throws Exception;

    //-------------//
    // doUninstall //
    //-------------//
    /**
     * Actual Uninstallation.
     */
    protected void doUninstall ()
            throws Exception
    {
        // Void by default
    }

    //-----------------//
    // getTargetFolder //
    //-----------------//
    /**
     * Report the target folder, if any.
     * This method must be overridden to provide precise target folder for a
     * companion that uses a target folder.
     *
     * @return the target folder for this companion, or throw
     *         IllegalStateException if no folder is defined
     */
    protected File getTargetFolder ()
    {
        // By default
        throw new IllegalStateException(
                "No target folder is defined for " + getTitle());
    }

    //------------------//
    // makeTargetFolder //
    //------------------//
    /**
     * Create the target folder, if needed, and return it.
     *
     * @return the target folder for this companion, or throw
     *         IllegalStateException if no folder is defined
     */
    protected File makeTargetFolder ()
    {
        File folder = getTargetFolder();

        if (!folder.exists()) {
            if (folder.mkdirs()) {
                logger.info("Created folder {}", folder.getAbsolutePath());
            }
        }

        return folder;
    }

    //------------//
    // updateView //
    //------------//
    protected void updateView ()
    {
        if (view != null) {
            view.update();
        }
    }

    //-------------------//
    // abortInstallation //
    //-------------------//
    private void abortInstallation (Throwable ex)
    {
        status = Status.FAILED_TO_INSTALL;

        if (ex instanceof LicenseCompanion.LicenseDeclinedException) {
            logger.warn(getTitle() + " declined.");
        } else {
            logger.warn(getTitle() + " failed to install.", ex);
        }
    }

    //---------------------//
    // abortUninstallation //
    //---------------------//
    private void abortUninstallation (Throwable ex)
    {
        status = Status.FAILED_TO_UNINSTALL;
        logger.warn(getTitle() + " failed to uninstall.", ex);
    }

    //----------------------//
    // completeInstallation //
    //----------------------//
    private void completeInstallation ()
    {
        status = Status.INSTALLED;
        logger.info("{} completed successfully.", getTitle());
    }

    //------------------------//
    // completeUninstallation //
    //------------------------//
    private void completeUninstallation ()
    {
        status = Status.NOT_INSTALLED;
        logger.info("{} completed successfully.", getTitle());
        updateView();
    }

    //-------------------//
    // startInstallation //
    //-------------------//
    private void startInstallation ()
    {
        status = Status.BEING_INSTALLED;
        logger.info("\n{} is being processed...", getTitle());
        updateView();
    }

    //---------------------//
    // startUninstallation //
    //---------------------//
    private void startUninstallation ()
    {
        status = Status.BEING_UNINSTALLED;
        logger.info("\n{} is being processed...", getTitle());
        updateView();
    }
}
