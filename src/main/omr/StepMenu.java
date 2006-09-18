//----------------------------------------------------------------------------//
//                                                                            //
//                              S t e p M e n u                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr;

import omr.sheet.InstanceStep;
import omr.sheet.Sheet;
import omr.sheet.SheetManager;

import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Class <code>StepMenu</code> encapsulates the user interface needed to deal
 * with application steps.  Steps are represented by menu items, each one being
 * a check box, to indicate the current status regarding the execution of the
 * step (done or not done).
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class StepMenu
{
    //~ Instance fields --------------------------------------------------------

    // The concrete UI menu
    private final JMenu menu;

    //~ Constructors -----------------------------------------------------------

    //----------//
    // StepMenu //
    //----------//
    /**
     * Generates the sub-menu to be inserted in the application menu hierarchy.
     *
     * @param label Name for the sub-menu
     */
    public StepMenu (String label)
    {
        menu = new JMenu(label);

        // Listener to item selection
        ActionListener actionListener = new StepListener();

        // List of Steps classes
        for (Step step : Sheet.getSteps()) {
            menu.add(new StepItem(step, actionListener));
        }

        // Listener to modify attributes on-the-fly
        menu.addMenuListener(new Listener());
    }

    //~ Methods ----------------------------------------------------------------

    //---------//
    // getMenu //
    //---------//
    /**
     * Report the concrete UI menu
     *
     * @return the menu entity
     */
    public JMenu getMenu ()
    {
        return menu;
    }

    //~ Inner Classes ----------------------------------------------------------

    //----------//
    // StepItem //
    //----------//
    /**
     * Class <code>StepItem</code> implements a menu item linked to a given step
     */
    private static class StepItem
        extends JCheckBoxMenuItem
    {
        // The related step
        final transient Step step;

        //----------//
        // StepItem //
        //----------//
        public StepItem (Step           step,
                         ActionListener actionListener)
        {
            super(step.toString());
            this.step = step;
            this.setToolTipText(step.getDescription());
            this.addActionListener(actionListener);
        }

        //--------------//
        // displayState //
        //--------------//
        public void displayState (Sheet sheet)
        {
            if (sheet == null) {
                setState(false);
                setEnabled(false);
            } else {
                if (sheet.isBusy()) {
                    setState(false);
                    setEnabled(false);
                } else {
                    InstanceStep is = sheet.getInstanceStep(step);

                    if (is != null) {
                        setState(is.isDone());
                        setEnabled(!is.isDone());
                    }
                }
            }
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
        public void menuCanceled (MenuEvent e)
        {
        }

        public void menuDeselected (MenuEvent e)
        {
        }

        public void menuSelected (MenuEvent e)
        {
            Sheet sheet = SheetManager.getSelectedSheet();

            for (int i = 0; i < menu.getItemCount(); i++) {
                StepItem item = (StepItem) menu.getItem(i);

                // Adjust the status for each step
                item.displayState(sheet);
            }
        }
    }

    //--------------//
    // StepListener //
    //--------------//
    /**
     * Class <code>StepListener</code> is triggered when a specific menu item is
     * selected
     */
    private class StepListener
        implements ActionListener
    {
        //------------------//
        // itemStateChanged //
        //------------------//
        public void actionPerformed (ActionEvent e)
        {
            StepItem item = (StepItem) e.getSource();
            Sheet    sheet = SheetManager.getSelectedSheet();
            Main.getJui()
                .setTarget(sheet.getPath());
            item.step.perform(sheet, null);
        }
    }
}
