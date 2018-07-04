//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            S h a p e D e s c r i p t o r P i x e l s                           //
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
package org.audiveris.omr.classifier;

import ij.process.ByteProcessor;

import org.audiveris.omr.glyph.Glyph;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ImgGlyphDescriptor} is a glyph descriptor based directly on the pixels
 * of glyph properly scaled image.
 *
 * @author Hervé Bitteur
 */
public class ImgGlyphDescriptor
        extends GlyphDescriptor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ImgGlyphDescriptor.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ImgGlyphDescriptor} object.
     */
    public ImgGlyphDescriptor ()
    {
        super(ScaledBuffer.HEIGHT + "x" + ScaledBuffer.WIDTH);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String[] getFeatureLabels ()
    {
        return null;
    }

    @Override
    public double[] getFeatures (Glyph glyph,
                                 int interline)
    {
        final ByteProcessor buffer = ScaledBuffer.getBuffer(glyph, interline);
        buffer.invert(); // 0 for background, 255 for foreground

        // Layout: row by row
        final double[] doubles = new double[length()];
        int i = 0;

        for (int y = 0; y < ScaledBuffer.HEIGHT; y++) {
            for (int x = 0; x < ScaledBuffer.WIDTH; x++) {
                doubles[i++] = buffer.get(x, y);
            }
        }

        return doubles;
    }

    @Override
    public int length ()
    {
        return ScaledBuffer.HEIGHT * ScaledBuffer.WIDTH;
    }
}
