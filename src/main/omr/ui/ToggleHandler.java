//-----------------------------------------------------------------------//
//                                                                       //
//                       T o g g l e H a n d l e r                       //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

package omr.ui;

import omr.lag.LagView;
import omr.util.Logger;

import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.JButton;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class <code>ToggleHandler</code> handles the general-purpose toggle
 * button of the user interface, according to the currently selected view
 * within the {@link SheetAssembly}.
 *
 * <p>When the related view is active (and this is handled through the
 * AncestorListener interface), pressing the toggle button triggers the
 * {@link omr.lag.LagView#toggle} method of the selected {@link
 * omr.lag.LagView}.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ToggleHandler
    implements ActionListener,
               AncestorListener
{
    //~ Static variables/initializers -------------------------------------

    private static final Logger logger = Logger.getLogger(ToggleHandler.class);

    //~ Instance variables ------------------------------------------------

    private JButton button;
    private String title;
    private LagView lagView;
    private String  tip;

    //~ Constructors ------------------------------------------------------

    //---------------//
    // ToggleHandler //
    //---------------//
    public ToggleHandler (String  title,
                          LagView lagView,
                          String  tip)
    {
        this.title   = title;
        this.lagView = lagView;
        this.tip     = tip;
        button       = omr.Main.getJui().sheetPane.getToggleButton();
        useButton();
    }

    //~ Methods -----------------------------------------------------------

    //-----------------//
    // actionPerformed //
    //-----------------//
    /**
     * Method called by the toggle button when pressed
     *
     * @param e the action event
     */
    public void actionPerformed (ActionEvent e)
    {
        lagView.toggle();
    }

    //---------------//
    // ancestorAdded //
    //---------------//
    /**
     * Method called when the related view becomes visible
     *
     * @param event
     */
    public void ancestorAdded (AncestorEvent event)
    {
        useButton();
    }

    //---------------//
    // ancestorMoved //
    //---------------//
    /**
     * Not interested in that method, imposed by the AncestorListener
     * interface
     *
     * @param event
     */
    public void ancestorMoved (AncestorEvent event)
    {
    }

    //-----------------//
    // ancestorRemoved //
    //-----------------//
    /**
     * Method called when the related view gets hidden
     *
     * @param event
     */
    public void ancestorRemoved (AncestorEvent event)
    {
        discardButton();
    }

    //---------------//
    // discardButton //
    //---------------//
    private void discardButton ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug(title + " discardButton");
        }

        button.setToolTipText("");
        button.setEnabled(false);
        button.removeActionListener(this);
    }

    //-----------//
    // useButton //
    //-----------//
    private void useButton ()
    {
        if (logger.isDebugEnabled()) {
            logger.debug(title + " useButton");
        }

        button.setToolTipText(tip);
        button.setEnabled(true);
        button.removeActionListener(this); // Safer
        button.addActionListener(this);
    }
}
