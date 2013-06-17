//----------------------------------------------------------------------------//
//                                                                            //
//                      S e p a r a b l e T o o l B a r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui.util;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import java.awt.Dimension;

import javax.swing.JSeparator;
import javax.swing.JToolBar;

/**
 * Class {@code SeparableToolBar} is a tool bar which is able to collapse
 * unneeded separators
 *
 * @author Brenton Partridge
 */
public class SeparableToolBar
        extends JToolBar
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /**
     * Dimension of the separator.
     */
    private static final Dimension gap = new Dimension(
            constants.separatorWidth.getValue(),
            constants.separatorWidth.getValue());

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new SeparableToolBar object.
     */
    public SeparableToolBar ()
    {
        super();
    }

    /**
     * Creates a new SeparableToolBar object.
     *
     * @param orientation Specific toolbar orientation
     */
    public SeparableToolBar (int orientation)
    {
        super(orientation);
    }

    /**
     * Creates a new SeparableToolBar object.
     *
     * @param name DOCUMENT ME!
     */
    public SeparableToolBar (String name)
    {
        super(name);
    }

    /**
     * Creates a new SeparableToolBar object.
     *
     * @param name        DOCUMENT ME!
     * @param orientation DOCUMENT ME!
     */
    public SeparableToolBar (String name,
                             int orientation)
    {
        super(name, orientation);
    }

    //~ Methods ----------------------------------------------------------------
    //--------------//
    // addSeparator //
    //--------------//
    /**
     * The separator will be inserted only if it is really necessary
     */
    @Override
    public void addSeparator ()
    {
        int count = super.getComponentCount();

        if ((count > 0) && !(getComponent(count - 1) instanceof JSeparator)) {
            super.addSeparator(gap);
        }
    }

    //----------------//
    // purgeSeparator //
    //----------------//
    /**
     * Remove any potential orphan separator at the end of the tool bar
     */
    public static void purgeSeparator (JToolBar toolBar)
    {
        int count = toolBar.getComponentCount();

        if (toolBar.getComponent(count - 1) instanceof JSeparator) {
            toolBar.remove(count - 1);
        }
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        private final Constant.Integer separatorWidth = new Constant.Integer(
                "Pixels",
                15,
                "Width of separator");

    }
}
