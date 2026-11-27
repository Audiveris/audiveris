//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S h a p e B u t t o n                                     //
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

import javax.swing.JButton;

/**
 * Class <code>ShapeButton</code> is a button assigned to a shape.
 *
 * @author Hervé Bitteur
 */
public class ShapeButton
        extends JButton
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The related shape. */
    private Shape shape;

    //~ Constructors -------------------------------------------------------------------------------

    public ShapeButton ()
    {
        super();
    }

    public ShapeButton (Shape shape)
    {
        super();

        setShape(shape);
    }

    //~ Methods ------------------------------------------------------------------------------------

    public Shape getShape ()
    {
        return shape;
    }

    public void setShape (Shape shape)
    {
        this.shape = shape;

        if (shape != null) {
            setText(shape.getLocalizedName());
            setToolTipText(shape.getTip());
        }
    }
}
