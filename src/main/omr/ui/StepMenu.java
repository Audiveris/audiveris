//-----------------------------------------------------------------------//
//                                                                       //
//                            S t e p M e n u                            //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.ui;

import omr.Main;
import omr.Step;
import omr.sheet.InstanceStep;
import omr.sheet.Sheet;

import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Class <code>StepMenu</code> encapsulates the user interface needed to
 * deal with application steps.  Steps are represented by menu items, each
 * one being a check box, to indicate the current status regarding the
 * execution of the step (done or not done).
 */
public class StepMenu
    extends JMenu
{
    //~ Constructors ------------------------------------------------------

    //----------//
    // StepMenu //
    //----------//

    /**
     * Generates the sub-menu to be inserted in the application menu
     * hierarchy.
     *
     * @param label Name for the sub-menu
     */
    public StepMenu (String label)
    {
        super(label);

        // Listener to item selection
        ActionListener actionListener = new StepListener();

        // List of Steps classes
        for (Step step : Sheet.getSteps()) {
            add(new StepItem(step, actionListener));
        }

        // Listener to modify attributes on-the-fly
        addMenuListener(new Listener());
    }

    //~ Methods -----------------------------------------------------------

    //----------//
    // getSheet //
    //----------//
    private Sheet getSheet ()
    {
        return Main.getJui().sheetPane.getCurrentSheet();
    }

    //~ Classes -----------------------------------------------------------

    //----------//
    // StepItem //
    //----------//
    /**
     * Class <code>StepItem</code> implements a menu item linked to a given
     * step
     */
    private class StepItem
        extends JCheckBoxMenuItem
    {
        //~ Instance variables --------------------------------------------

        // The related step
        final Step step;

        //~ Constructors --------------------------------------------------

        //----------//
        // StepItem //
        //----------//
        public StepItem (Step step,
                         ActionListener actionListener)
        {
            super(step.toString());
            this.step = step;
            this.setToolTipText(step.getDescription());
            this.addActionListener(actionListener);
        }

        //~ Methods -------------------------------------------------------

        //--------------//
        // displayState //
        //--------------//
        public void displayState (Sheet sheet)
        {
            if (sheet == null) {
                setState(false);
                setEnabled(false);
            } else {
                InstanceStep is = sheet.getInstanceStep(step);
                if (is != null) {
                    setState(is.isDone());
                    if (sheet.getOrder() == null) {
                        setEnabled(!is.isDone());
                    } else {
                        setEnabled(false);
                    }
                } else {
                    setState(false);
                    setEnabled(false);
                }
            }
        }
    }

    //--------------//
    // StepListener //
    //--------------//

    /**
     * Class <code>StepListener</code> is triggered when a specific menu
     * item is selected
     */
    private class StepListener
        implements ActionListener
    {
        //~ Methods -------------------------------------------------------

        //------------------//
        // itemStateChanged //
        //------------------//
        public void actionPerformed (ActionEvent e)
        {
            StepItem item = (StepItem) e.getSource();
            Sheet sheet = getSheet();
            Main.getJui().setTarget(sheet.getPath());
            item.step.perform(sheet, null);
        }
    }

    //----------//
    // Listener //
    //----------//

    /**
     * Class <code>Listener</code> is triggered when the whole sub-menu is
     * entered. This is done with respect to currently displayed sheet. The
     * steps already done are flagged as such.
     */
    private class Listener
        implements MenuListener
    {
        //~ Methods -------------------------------------------------------

        public void menuCanceled (MenuEvent e)
        {
        }

        public void menuDeselected (MenuEvent e)
        {
        }

        public void menuSelected (MenuEvent e)
        {
            Sheet sheet = getSheet();

            for (int i = 0; i < getItemCount(); i++) {
                StepItem item = (StepItem) getItem(i);

                // Adjust the status for each step
                item.displayState(sheet);
            }
        }
    }
}
