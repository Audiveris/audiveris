//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e s i z e d S y m b o l                                    //
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

import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;

/**
 * Class {@code ResizedSymbol} is a {@link ShapeSymbol} with a ratio different from 1.
 * This is meant for shapes like G_CLEF_SMALL and F_CLEF_SMALL.
 *
 * @author Hervé Bitteur
 */
public class ResizedSymbol
        extends ShapeSymbol
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** The display ratio */
    protected final double ratio;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a non decorated standard ResizedSymbol with the provided
     * shape and codes.
     *
     * @param shape the related shape
     * @param ratio the resizing ratio
     * @param codes the codes for MusicFont characters
     */
    public ResizedSymbol (Shape shape,
                          double ratio,
                          int... codes)
    {
        this(false, shape, ratio, false, codes);
    }

    /**
     * Create a ResizedSymbol with the provided shape and codes
     *
     * @param isIcon    true for an icon
     * @param shape     the related shape
     * @param ratio     the resizing ratio
     * @param decorated true if the symbol uses decoration around the shape
     * @param codes     the codes for MusicFont characters
     */
    public ResizedSymbol (boolean isIcon,
                          Shape shape,
                          double ratio,
                          boolean decorated,
                          int... codes)
    {
        super(isIcon, shape, decorated, codes);
        this.ratio = ratio;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // createIcon //
    //------------//
    @Override
    protected ResizedSymbol createIcon ()
    {
        return new ResizedSymbol(true, shape, ratio, decorated, codes);
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        sb.append(" ratio:").append((float) ratio);

        return sb.toString();
    }

    //--------//
    // layout //
    //--------//
    /**
     * Report a single layout, based on symbol codes if they exist.
     * This feature can work only with a single "line" of music codes.
     *
     * @param font the specifically-scaled font to use
     * @return the layout ready to be drawn, or null
     */
    @Override
    protected TextLayout layout (MusicFont font)
    {
        AffineTransform at = AffineTransform.getScaleInstance(ratio, ratio);

        return font.layout(shape, at);
    }
}
