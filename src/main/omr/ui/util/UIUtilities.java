//-----------------------------------------------------------------------//
//                                                                       //
//                         U I U t i l i t i e s                         //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
package omr.ui.util;

import omr.util.Logger;

import java.util.Collection;
import java.util.Iterator;

import javax.swing.*;
import javax.swing.border.Border;

/**
 * Class <code>UIUtilities</code> gathers utilities related to User Interface
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class UIUtilities
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = Logger.getLogger(UIUtilities.class);

    /**
     * Customized border for tool buttons, to use consistently in all UI
     * components
     */
    private static Border toolBorder;

    //~ Methods ----------------------------------------------------------------

    //---------------//
    // getToolBorder //
    //---------------//
    /**
     * Report a standard tool border entity, which is a raised bevel border
     *
     * @return the standard tool border
     */
    public static Border getToolBorder ()
    {
        if (toolBorder == null) {
            toolBorder = BorderFactory.createRaisedBevelBorder();
        }

        return toolBorder;
    }

    //---------------//
    // enableActions //
    //---------------//
    /**
     * Given a list of actions, set all these actions (whether they descend
     * from AbstractAction or AbstractButton) enabled or not, according to
     * the bool parameter provided.
     *
     * @param actions list of actions to enable/disable as a whole
     * @param bool    true for enable, false for disable
     */
    public static void enableActions (Collection actions,
                                      boolean    bool)
    {
        for (Iterator it = actions.iterator(); it.hasNext();) {
            Object next = it.next();

            if (next instanceof AbstractAction) {
                ((AbstractAction) next).setEnabled(bool);
            } else if (next instanceof AbstractButton) {
                ((AbstractButton) next).setEnabled(bool);
            } else {
                logger.warning("Neither Button nor Action : " + next);
            }
        }
    }
}
