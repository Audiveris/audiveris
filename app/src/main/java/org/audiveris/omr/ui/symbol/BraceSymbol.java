//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B r a c e S y m b o l                                      //
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

import static org.audiveris.omr.ui.symbol.OmrFont.RATIO_TINY;

import org.audiveris.omr.glyph.Shape;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.AffineTransform;

/**
 * Class <code>BraceSymbol</code> displays a BRACE symbol: '{'
 * <p>
 * This class exists only to significantly modify the standard size of Brace symbol.
 *
 * @author Hervé Bitteur
 */
public class BraceSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BraceSymbol.class);

    /** Scaling to apply on default brace symbol size: {@value}. */
    private static final int MULTIPLIER = 4;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Create a BraceSymbol.
     *
     * @param family the MusicFont family
     */
    public BraceSymbol (MusicFamily family)
    {
        super(Shape.BRACE, family);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        // This impacts brace symbol drawing in SheetView and in boards
        final Params p = new Params();
        final double ratio = MULTIPLIER * (isTiny ? RATIO_TINY : 1);
        final AffineTransform at = AffineTransform.getScaleInstance(ratio, ratio);
        p.layout = font.layoutShapeByCode(Shape.BRACE, at);
        p.rect = p.layout.getBounds();

        return p;
    }
}
