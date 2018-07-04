//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S c a l e d B u f f e r                                    //
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

import ij.process.Blitter;
import ij.process.ByteProcessor;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.util.ByteUtil;

import java.awt.Point;

/**
 * Class {@code ScaledBuffer} produces a rectangular buffer, with size normalized by
 * reference interline value, and centered on glyph centroid.
 *
 * @author Hervé Bitteur
 */
public class ScaledBuffer
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Scaled interline value. */
    public static int INTERLINE = 5;

    /** Target width. */
    public static int WIDTH = 24; // 24 = 3 * 2**3

    /** Target height. */
    public static int HEIGHT = 48; // 48 = 6 * 2**3

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Compute the scaled buffer for the provided glyph, using related staff interline
     * value.
     *
     * @param glyph     the source glyph
     * @param interline the related staff interline
     * @return the computed buffer using 0 for black (foreground) and 255 for white (background)
     */
    public static ByteProcessor getBuffer (Glyph glyph,
                                           int interline)
    {
        final RunTable runTable = glyph.getRunTable();
        final ByteProcessor glyphBuffer = runTable.getBuffer();
        final double scale = (double) INTERLINE / interline;

        // Build scaled buffer, filled by (scaled) glyph
        final int scaledWidth = (int) Math.ceil(runTable.getWidth() * scale);
        final int scaledHeight = (int) Math.ceil(runTable.getHeight() * scale);
        final ByteProcessor scaledBuffer = (ByteProcessor) glyphBuffer.resize(
                scaledWidth,
                scaledHeight,
                true); // True => use averaging when down-scaling

        // Copy scaledBuffer into a WIDTH*HEIGHT target buffer centered on glyph centroid
        final Point centroid = glyph.getCentroid();
        final Point center = glyph.getCenter();
        final int dx = centroid.x - center.x; // X shift of centroid WRT center
        final int dy = centroid.y - center.y; // Y shift of centroid WRT center
        final int targetDx = (int) Math.rint(dx * scale); // Scaled x shift
        final int targetDy = (int) Math.rint(dy * scale); // Scaled y shift

        final ByteProcessor buffer = new ByteProcessor(WIDTH, HEIGHT); // Same dim for any symbol
        ByteUtil.raz(buffer); // Correct
        ///ByteUtil.fill(targetBuffer, 100); // Not correct, just meant to visualize limits...

        final int xOffset = ((WIDTH - scaledWidth) / 2) - targetDx;
        final int yOffset = ((HEIGHT - scaledHeight) / 2) - targetDy;
        buffer.copyBits(scaledBuffer, xOffset, yOffset, Blitter.COPY);

        return buffer;
    }
}
