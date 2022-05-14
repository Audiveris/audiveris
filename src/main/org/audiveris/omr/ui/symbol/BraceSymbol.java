//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     B r a c e S y m b o l                                      //
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
import static org.audiveris.omr.ui.symbol.MusicFont.DEFAULT_INTERLINE;
import static org.audiveris.omr.ui.symbol.MusicFont.getPointFont;
import static org.audiveris.omr.ui.symbol.MusicFont.getPointSize;

import java.awt.Dimension;
import java.awt.geom.AffineTransform;

/**
 * Class <code>BraceSymbol</code> displays a BRACE symbol: '{'
 * <p>
 * This class exists only to significantly modify the standard size of Bravura Brace symbol.
 *
 * @author Hervé Bitteur
 */
public class BraceSymbol
        extends ShapeSymbol
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Scaling to apply on default brace symbol size: {@value}. */
    private static final int MULTIPLIER = 4;

    /** But keep the tiny version really tiny. */
    private static final int myTinyInterline
            = (int) Math.rint(2 * DEFAULT_INTERLINE * OmrFont.RATIO_TINY / MULTIPLIER);

    private static final MusicFont myTinyMusicFont = getPointFont(
            getPointSize(myTinyInterline), myTinyInterline);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a BraceSymbol.
     *
     * @param codes the codes for MusicFont characters
     */
    public BraceSymbol (int... codes)
    {
        super(Shape.BRACE, codes);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------------//
    // computeImage //
    //--------------//
    @Override
    protected void computeImage ()
    {
        image = buildImage(isTiny ? myTinyMusicFont : MusicFont.baseMusicFont);

        dimension = new Dimension(image.getWidth(), image.getHeight());
    }

    //-----------//
    // getParams //
    //-----------//
    @Override
    protected Params getParams (MusicFont font)
    {
        final Params p = new Params();
        final AffineTransform at = AffineTransform.getScaleInstance(MULTIPLIER, MULTIPLIER);
        p.layout = font.layout(getString(), at);
        p.rect = p.layout.getBounds();

        return p;
    }
}
