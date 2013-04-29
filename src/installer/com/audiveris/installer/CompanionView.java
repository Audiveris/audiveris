//----------------------------------------------------------------------------//
//                                                                            //
//                         C o m p a n i o n V i e w                          //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package com.audiveris.installer;

import java.awt.Color;

import javax.swing.JComponent;

/**
 * Interface {@code CompanionView} defines a view on a {@link
 * Companion}
 *
 * @author Hervé Bitteur
 */
public interface CompanionView
{
    //~ Methods ----------------------------------------------------------------

    /** Report the underlying companion. */
    Companion getCompanion ();

    /** Report the Swing component. */
    JComponent getComponent ();

    /** Refresh the view from related companion. */
    void update ();

    //~ Inner Interfaces -------------------------------------------------------

    interface COLORS
    {
        //~ Static fields/initializers -----------------------------------------

        static final Color NOT_INST = new Color(245, 230, 220);
        static final Color INST = new Color(200, 255, 200);
        static final Color UNUSED = new Color(220, 220, 220);
        static final Color BEING = new Color(255, 200, 0);
        static final Color FAILED = new Color(255, 100, 100);
    }
}
