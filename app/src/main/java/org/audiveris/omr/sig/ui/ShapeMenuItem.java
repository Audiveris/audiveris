//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S h a p e M e n u I t e m                                   //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2026. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.ui.symbol.MusicFamily;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JMenuItem;

/**
 * Class <code>ShapeMenuItem</code> is a menu item assigned to a shape.
 *
 * @author Hervé Bitteur
 */
public class ShapeMenuItem
        extends JMenuItem
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ShapeMenuItem.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related shape. */
    private Shape shape;

    //~ Constructors -------------------------------------------------------------------------------

    public ShapeMenuItem ()
    {
        super();
    }

    public ShapeMenuItem (Shape shape)
    {
        super();
        this.shape = shape;

        if (shape != null) {
            setText(shape.getDescription());
            setToolTipText(shape.getTip());
        }
    }

    public ShapeMenuItem (Shape shape,
                          MusicFamily family)
    {
        super();
        this.shape = shape;

        if (shape != null) {
            setText(shape.getDescription());
            setToolTipText(shape.getTip());
            setIcon(shape.getDecoratedSymbol(family).getTinyVersion());
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    public Shape getShape ()
    {
        return shape;
    }

    public void setShape (Shape shape,
                          MusicFamily family)
    {
        this.shape = shape;

        if (shape != null) {
            setText(shape.getDescription());
            setToolTipText(shape.getTip());
            setIcon(shape.getDecoratedSymbol(family));
        }
    }
}
