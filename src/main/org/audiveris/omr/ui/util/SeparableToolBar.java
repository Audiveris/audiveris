//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                S e p a r a b l e T o o l B a r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.ui.util;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;

import java.awt.Dimension;

import javax.swing.JSeparator;
import javax.swing.JToolBar;

/**
 * Class {@code SeparableToolBar} is a tool bar which is able to collapse unneeded separators
 *
 * @author Brenton Partridge
 */
public class SeparableToolBar
        extends JToolBar
{

    private static final Constants constants = new Constants();

    /**
     * Dimension of the separator.
     */
    private static final Dimension gap = new Dimension(
            constants.separatorWidth.getValue(),
            constants.separatorWidth.getValue());

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
     *
     * @param toolBar the toolBar to purge
     */
    public static void purgeSeparator (JToolBar toolBar)
    {
        int count = toolBar.getComponentCount();

        if (toolBar.getComponent(count - 1) instanceof JSeparator) {
            toolBar.remove(count - 1);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer separatorWidth = new Constant.Integer(
                "Pixels",
                15,
                "Width of separator");
    }
}
