//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           O m r G u i                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package org.audiveris.omr.ui;

import org.jdesktop.application.Application;
import org.jdesktop.application.SingleFrameApplication;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Class {@code OmrGui} defines the minimum offered by an OMR GUI.
 * <ul>
 * <li>A SingleFrameApplication as defined by Swing Application Framework</li>
 * <li>Various forms of message display to the end user</li>
 * <li>Log pane</li>
 * <li>Errors pane</li>
 * <li>Boards pane</li>
 * <li>The actual Swing frame and the Glass pane.</li>
 * </ul>
 * <p>
 * This is an abstract class rather than a true interface, because it must derive from SAF
 * SingleFrameApplication.
 *
 * @author Hervé Bitteur
 */
public abstract class OmrGui
        extends SingleFrameApplication
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Erase the content of the log window (but not the log itself).
     */
    public abstract void clearLog ();

    /**
     * Allow to display a modal confirmation dialog with a message.
     *
     * @param message the message asking for confirmation
     * @return true if confirmed, false otherwise
     */
    public abstract boolean displayConfirmation (String message);

    /**
     * Allow to display a modal dialog with an error message.
     *
     * @param message the error message
     */
    public abstract void displayError (String message);

    /**
     * Allow to display a modal dialog with an html content.
     *
     * @param htmlStr the HTML string
     */
    public abstract void displayMessage (String htmlStr);

    /**
     * Allow to display a non-modal confirmation dialog.
     *
     * @param message the confirmation message
     * @return the option chosen
     */
    public abstract int displayModelessConfirm (String message);

    /**
     * Allow to display a modal dialog with a warning message.
     *
     * @param message the warning message
     */
    public abstract void displayWarning (String message);

    /**
     * Report the concrete Swing frame.
     *
     * @return the OmrGui frame
     */
    public abstract JFrame getFrame ();

    /**
     * Report the main window glassPane, needed for drag and drop.
     *
     * @return the ghost glass pane
     */
    public abstract OmrGlassPane getGlassPane ();

    /**
     * Notify that one or several new log records are available for display.
     */
    public abstract void notifyLog ();

    /**
     * Remove the current boards pane, if any.
     */
    public abstract void removeBoardsPane ();

    /**
     * Remove the specific component of the errors pane.
     *
     * @param errorsPane the precise component to remove
     */
    public abstract void removeErrorsPane (JComponent errorsPane);

    /**
     * Set a new boards pane to the boards holder.
     *
     * @param boards the boards pane to be shown
     */
    public abstract void setBoardsPane (JComponent boards);

    /**
     * Show the provided errors pane.
     *
     * @param errorsPane the errors pane to be shown
     */
    public abstract void setErrorsPane (JComponent errorsPane);

    //----------------//
    // getApplication //
    //----------------//
    /**
     * Report the single instance of this GUI SAF application.
     *
     * @return the SingleFrameApplication instance
     */
    public static SingleFrameApplication getApplication ()
    {
        return (SingleFrameApplication) Application.getInstance();
    }
}
