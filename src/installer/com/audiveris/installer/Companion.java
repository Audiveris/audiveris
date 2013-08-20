//----------------------------------------------------------------------------//
//                                                                            //
//                             C o m p a n i o n                              //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

/**
 * Interface {@code Companion} defines a companion to install as part
 * of Audiveris bundle.
 * A companion does the maximum via Java code run in user mode.
 * If some operation needs admin privilege then this operation (as small as
 * possible) must be written as a command and appended to the bundle global
 * command list which will be run at the end.
 *
 * @author Hervé Bitteur
 */
public interface Companion
{
    //~ Enumerations -----------------------------------------------------------

    /** Need of Audiveris with respect to a companion. */
    enum Need
    {
        //~ Enumeration constant initializers ----------------------------------

        /**
         * Companion must be present.
         */
        MANDATORY,
        /**
         * Optional companion but selected.
         */
        SELECTED,
        /**
         * Optional companion not selected.
         */
        NOT_SELECTED;

    }

    /** Current installation status of a companion. */
    enum Status
    {
        //~ Enumeration constant initializers ----------------------------------

        /**
         * Companion is not (yet) installed.
         */
        NOT_INSTALLED,
        /**
         * Companion is being installed.
         */
        BEING_INSTALLED,
        /**
         * Companion is being uninstalled.
         */
        BEING_UNINSTALLED,
        /**
         * Companion is installed.
         */
        INSTALLED,
        /**
         * Installation has failed.
         */
        FAILED_TO_INSTALL,
        /**
         * Uninstallation has failed.
         */
        FAILED_TO_UNINSTALL;

    }

    //~ Methods ----------------------------------------------------------------
    /** Check whether this companion has been installed. */
    boolean checkInstalled ();

    /** Report the full description. */
    String getDescription ();

    /** Report the companion index. */
    int getIndex ();

    /** Report a weight for installation. */
    int getInstallWeight ();

    /** Get companion need. */
    Need getNeed ();

    /** Get companion installation status. */
    Status getStatus ();

    /** Report the companion title for display. */
    String getTitle ();

    /** Report a weight for uninstallation. */
    int getUninstallWeight ();

    /** Report the related view, if any. */
    CompanionView getView ();

    /** Launch installation of this companion. */
    void install ()
            throws Exception;

    /** Check whether installation is needed. */
    boolean isNeeded ();

    /** Set companion need. */
    void setNeed (Need need);

    /** Launch de-installation of this companion. */
    void uninstall ();
}
