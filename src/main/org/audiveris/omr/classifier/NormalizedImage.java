//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  N o r m a l i z e d I m a g e                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.util.ByteUtil;

import java.awt.Point;

/**
 * Class {@code NormalizedImage} produces a rectangular image, with size normalized by
 * reference interline value, and centered on glyph centroid.
 *
 * @author Hervé Bitteur
 */
public class NormalizedImage
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Normalized interline value. */
    public static int INTERLINE = 20;

    /** Target width. */
    public static int WIDTH = 80; // 5 * 2**4

    /** Target height. */
    public static int HEIGHT = 192; // 12 * 2**4

    //~ Instance fields ----------------------------------------------------------------------------
    public final ByteProcessor buffer;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code NormalizedImage} object.
     *
     * @param buffer the underlying normalized buffer
     */
    public NormalizedImage (ByteProcessor buffer)
    {
        this.buffer = buffer;
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Compute the normalized image for the provided glyph, using related staff
     * interline value.
     * <p>
     * TODO: This is a first implementation, that could certainly be optimized.
     *
     * @param glyph     the source glyph
     * @param interline the related staff interline
     * @return the computed image
     */
    public static NormalizedImage getInstance (Glyph glyph,
                                               int interline)
    {
        final RunTable runTable = glyph.getRunTable();
        final double scale = (double) INTERLINE / interline;

        // Build resized buffer, filled by (resized) glyph
        final int rWidth = (int) Math.rint(runTable.getWidth() * scale);
        final int rHeight = (int) Math.ceil(runTable.getHeight() * scale);
        final ByteProcessor glyphBuffer = runTable.getBuffer();
        final ByteProcessor rBuffer = (ByteProcessor) glyphBuffer.resize(rWidth, rHeight, true);

        // Target buffer, of normalized size, centered on glyph centroid
        final Point centroid = glyph.getCentroid();
        final Point center = glyph.getCenter();
        final int dx = centroid.x - center.x; // X shift of centroid WRT center
        final int dy = centroid.y - center.y; // Y shift of centroid WRT center
        final int rDx = (int) Math.rint(dx * scale);
        final int rDy = (int) Math.rint(dy * scale);
        final int xOffset = ((WIDTH - rWidth) / 2) - rDx;
        final int yOffset = ((HEIGHT - rHeight) / 2) - rDy;
        final ByteProcessor buffer = new ByteProcessor(WIDTH, HEIGHT); // Same dim for any symbol
        ByteUtil.raz(buffer); // Correct
        ///ByteUtil.fill(targetBuffer, 100); // Not correct, just meant to visualize limits...

        buffer.copyBits(rBuffer, xOffset, yOffset, Blitter.COPY);

        return new NormalizedImage(buffer);
    }
}
