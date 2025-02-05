//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  T e c h n i c a l H e l p e r                                 //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.GeoUtil;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.List;

/**
 * Class <code>TechnicalHelper</code> provides utilities for music technical elements,
 * notably Fingering and Plucking.
 *
 * @author Hervé Bitteur
 */
public abstract class TechnicalHelper
{
    //~ Constructors -------------------------------------------------------------------------------

    private TechnicalHelper ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

    /**
     * Look up for best matching head.
     *
     * @param item             technical inter (fingering or plucking)
     * @param systemHeadChords Head chords in system
     * @param maxXDist         Maximum abscissa distance from inter center to chord
     * @param maxYDist         Maximum ordinate distance from inter center to chord
     * @return the best head in best chord
     */
    public static HeadInter lookupHead (Inter item,
                                        List<Inter> systemHeadChords,
                                        int maxXDist,
                                        int maxYDist)
    {
        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final Point center = item.getCenter();
        final Rectangle luBox = item.getBounds();
        luBox.add(new Point(center.x + maxXDist, center.y - maxYDist));
        luBox.add(new Point(center.x + maxXDist, center.y + maxYDist));

        final List<Inter> chords = Inters.intersectedInters(
                systemHeadChords,
                GeoOrder.BY_ABSCISSA,
                luBox);

        if (chords.isEmpty()) {
            return null;
        }

        // Select closest chord, euclidean-wise
        HeadChordInter bestChord = null;
        double bestD2 = Double.MAX_VALUE;

        for (Inter chord : chords) {
            final Rectangle chordBox = chord.getBounds();
            final double d2 = GeoUtil.ptDistanceSq(chordBox, center.x, center.y);

            if (bestD2 > d2) {
                bestD2 = d2;
                bestChord = (HeadChordInter) chord;
            }
        }

        if (bestChord == null) {
            return null;
        }

        // Choose closest head in chord, using vertical distance with head
        final List<? extends Inter> notes = bestChord.getNotes();
        Inter bestHead = null;
        int bestDy = Integer.MAX_VALUE;

        for (Inter note : notes) {
            final Point noteCenter = note.getCenter();
            final int dy = Math.abs(noteCenter.y - center.y);

            if (bestDy > dy) {
                bestDy = dy;
                bestHead = note;
            }
        }

        return (HeadInter) bestHead;
    }
}
