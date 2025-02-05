//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       F o n t S y m b o l                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.font.TextLayout;
import java.awt.geom.Point2D;

/**
 * Class <code>FontSymbol</code> handles a couple made of a font and a symbol,
 * because often a symbol geometry depends on the underlying font.
 *
 * @author Hervé Bitteur
 */
public class FontSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    public final MusicFont font;

    public final ShapeSymbol symbol;

    //~ Constructors -------------------------------------------------------------------------------

    public FontSymbol (MusicFont font,
                       ShapeSymbol symbol)
    {
        this.font = font;
        this.symbol = symbol;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // getDimension //
    //--------------//
    /**
     * Report the bounding dimension of this symbol.
     *
     * @return the bounding dimension
     */
    public Dimension getDimension ()
    {
        ShapeSymbol.Params p = symbol.getParams(font);

        if (p.rect == null) {
            return null;
        }

        return new Dimension(
                (int) Math.rint(p.rect.getWidth()),
                (int) Math.rint(p.rect.getHeight()));
    }

    //-----------//
    // getLayout //
    //-----------//
    /**
     * Report this symbol layout.
     *
     * @return symbol layout
     */
    public TextLayout getLayout ()
    {
        return symbol.getLayout(font);
    }

    //-----------//
    // getLayout //
    //-----------//
    /**
     * Report this symbol layout, adjusted to provided dimension.
     *
     * @param dim imposed dimension
     * @return symbol layout
     */
    public TextLayout getLayout (Dimension dim)
    {
        return font.layoutShape(symbol.getShape(), dim);
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint this symbol, using its related font and context, aligned at provided location.
     *
     * @param g         graphic context
     * @param location  where to paint the shape with provided alignment
     * @param alignment the way the symbol is aligned WRT the location
     */
    public void paintSymbol (Graphics2D g,
                             Point2D location,
                             Alignment alignment)
    {
        symbol.paint(g, symbol.getParams(font), location, alignment);
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return new StringBuilder(getClass().getSimpleName()).append('{').append("font:").append(
                font).append(" symbol:").append(symbol).append('}').toString();
    }
}
