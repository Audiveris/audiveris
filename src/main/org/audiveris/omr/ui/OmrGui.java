//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           O m r G u i                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

import javax.swing.JFrame;

/**
 * Class {@code OmrGui} defines the minimum offered by an OMR GUI.
 * <ul>
 * <li>A SingleFrameApplication as defined by Swing Application Framework</li>
 * <li>Various forms of message display to the end user</li>
 * <li>Log pane</li>
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
    public abstract boolean displayConfirmation (Object message);

    /**
     * Allow to display a modal confirmation dialog with a message and specific title
     *
     * @param message the message asking for confirmation
     * @param title   dialog title
     * @return true if confirmed, false otherwise
     */
    public abstract boolean displayConfirmation (Object message,
                                                 String title);

    /**
     * Allow to display a modal confirmation dialog with a message, specific title,
     * and specific option type
     *
     * @param message     the message asking for confirmation
     * @param title       dialog title
     * @param optionType  YES_NO_OPTION, YES_NO_CANCEL_OPTION or OK_CANCEL_OPTION
     * @param messageType ERROR_MESSAGE, INFORMATION_MESSAGE, WARNING_MESSAGE, QUESTION_MESSAGE or
     *                    PLAIN_MESSAGE
     * @return true if confirmed, false otherwise
     */
    public abstract boolean displayConfirmation (Object message,
                                                 String title,
                                                 int optionType,
                                                 int messageType);

    /**
     * Allow to display a modal dialog with an error message.
     *
     * @param message the error message
     */
    public abstract void displayError (Object message);

    /**
     * Allow to display a modal dialog with an HTML content.
     *
     * @param htmlStr the HTML string
     */
    public abstract void displayHtmlMessage (String htmlStr);

    /**
     * Allow to display a modal dialog with a string content.
     *
     * @param message the message object
     * @param title   dialog title
     */
    public abstract void displayMessage (Object message,
                                         String title);

    /**
     * Allow to display a non-modal confirmation dialog.
     *
     * @param message the confirmation message object
     * @return the option chosen
     */
    public abstract int displayModelessConfirm (Object message);

    /**
     * Allow to display a modal dialog with a warning message.
     *
     * @param message the warning message object
     */
    public abstract void displayWarning (Object message);

    /**
     * Allow to display a modal dialog with a warning message and specific title
     *
     * @param message the warning message object
     * @param title   dialog title
     */
    public abstract void displayWarning (Object message,
                                         String title);

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
