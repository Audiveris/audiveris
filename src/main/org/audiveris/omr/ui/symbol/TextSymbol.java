//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T e x t S y m b o l                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.ui.symbol;

import org.audiveris.omr.glyph.Shape;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code TextSymbol} implements a decorated text symbol
 *
 * @author Hervé Bitteur
 */
public class TextSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The text string to use */
    private final String str;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create an TextSymbol
     *
     * @param shape the precise shape
     * @param str   the text to draw
     */
    public TextSymbol (Shape shape,
                       String str)
    {
        this(false, shape, str);
    }

    /**
     * Create an TextSymbol
     *
     * @param isIcon true for an icon
     * @param shape  the precise shape
     * @param str    the text to draw
     */
    protected TextSymbol (boolean isIcon,
                          Shape shape,
                          String str)
    {
        super(isIcon, shape, true); // Decorated
        this.str = str;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ShapeSymbol createIcon ()
    {
        return new TextSymbol(true, shape, str);
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        ///TextFont textFont = new TextFont((int) Math.rint(font.getSize2D() * 0.62));
        TextFont textFont = new TextFont((int) Math.rint(font.getSize2D() * 0.4));
        p.layout = textFont.layout(str);

        Rectangle2D r = p.layout.getBounds();
        p.rect = new Rectangle((int) Math.ceil(r.getWidth()), (int) Math.ceil(r.getHeight()));

        return p;
    }
}
