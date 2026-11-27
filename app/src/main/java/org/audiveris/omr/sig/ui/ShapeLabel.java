//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S h a p e L a b e l                                      //
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

import javax.swing.JLabel;

/**
 * Class <code>ShapeLabel</code> is a label assigned to a shape.
 *
 * @author Hervé Bitteur
 */
public class ShapeLabel
        extends JLabel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related shape. */
    private Shape shape;

    //~ Constructors -------------------------------------------------------------------------------

    public ShapeLabel ()
    {
        super();
    }

    public ShapeLabel (Shape shape)
    {
        super();
        this.shape = shape;

        if (shape != null) {
            setText(shape.getLocalizedName());
            setToolTipText(shape.getTip());
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
            setText(shape.getLocalizedName());
            setToolTipText(shape.getTip());
            setIcon(shape.getDecoratedSymbol(family));
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //~ Inner Classes ------------------------------------------------------------------------------

}
