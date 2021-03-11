//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   S t e m H a l f L i n k e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sheet.stem;

import java.awt.geom.Area;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.Set;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphFactory;
import org.audiveris.omr.math.LineUtil;

/**
 * Interface {@code StemHalfLinker} handles connection above or below a StemLinker.
 * <p>
 * For Beam: VLinker, for Head: CLinker.
 *
 * @author Hervé Bitteur
 */
public abstract class StemHalfLinker
        extends StemLinker
{

    /**
     * Report the look-up area for stem items
     *
     * @return the stem lookup area
     */
    public abstract Area getLookupArea ();

    /**
     * Report the theoretical line, going from reference point to target point.
     *
     * @return line from refPt (p1) to some targetPt (p2)
     */
    public abstract Line2D getTheoreticalLine ();

    /**
     * Update the stem line being built, by including a new glyph.
     * <p>
     * We translate the stem line horizontally to go through glyph centroid.
     *
     * @param glyph  the new glyph to include
     * @param glyphs the set of glyphs already included
     * @param sLine  the current stem line
     */
    protected static void updateStemLine (Glyph glyph,
                                          Set<Glyph> glyphs,
                                          Line2D sLine)
    {
        if ((glyph != null) && glyphs.add(glyph)) {
            final Glyph stemGlyph = (glyphs.size() > 1) ? GlyphFactory.buildGlyph(glyphs) : glyph;
            final Point2D centroid = stemGlyph.getCentroidDouble();
            final Point2D xp = LineUtil.intersectionAtY(sLine, centroid.getY());
            final double dx = centroid.getX() - xp.getX();
            sLine.setLine(sLine.getX1() + dx, sLine.getY1(), sLine.getX2() + dx, sLine.getY2());
        }
    }

}
