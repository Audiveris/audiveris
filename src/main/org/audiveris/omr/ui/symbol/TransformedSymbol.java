//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               T r a n s f o r m e d S y m b o l                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2022. All rights reserved.
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
import static org.audiveris.omr.ui.symbol.Alignment.*;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * Class <code>TransformedSymbol</code> displays a baseShape symbol with AffineTransform.
 * <p>
 * NOTA: This class is no longer needed since we moved to Bravura font.
 * It is kept only for potential reuse.
 *
 * @author Hervé Bitteur
 */
public class TransformedSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Proper transformation */
    private final AffineTransform at;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new TransformedSymbol object.
     *
     * @param shape the related shape
     * @param at    the AffineTransform to apply
     * @param codes the codes for MusicFont characters
     */
    public TransformedSymbol (Shape shape,
                              AffineTransform at,
                              int... codes)
    {

        super(shape, codes);
        this.at = at;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        Params p = new Params();

        p.layout = font.layout(getString(), at);
        p.rect = p.layout.getBounds();

        return p;
    }
//
//    //-------//
//    // paint //
//    //-------//
//    @Override
//    protected void paint (Graphics2D g,
//                          Params p,
//                          Point2D location,
//                          Alignment alignment)
//    {
//        Point2D loc = alignment.translatedPoint(TOP_LEFT, p.rect, location);
//        MusicFont.paint(g, p.layout, loc, TOP_LEFT);
//    }
}
