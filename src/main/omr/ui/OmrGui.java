//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           O m r G u i                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.ui.dnd.GhostGlassPane;

import org.jdesktop.application.SingleFrameApplication;

import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * Class {@code OmrGui} defines the minimum offered by an OMR Gui.
 * <ul>
 * <li>A SingleFrameApplication as defined by Swing Application Framework</li>
 * <li>Various forms of message display to the end user</li>
 * <li>Log pane</li>
 * <li>Errors pane</li>
 * <li>Boards pane</li>
 * <li>The actual Swing frame and the Glass pane.</li>
 * </ul>
 * <p>
 * This is an abstract class rather than an interface, because it must derive from SAF
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
    public abstract GhostGlassPane getGlassPane ();

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
}
