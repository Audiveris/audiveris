//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S l u r H e a d L i n k                                    //
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
package org.audiveris.omr.sheet.curve;

import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.HeadChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.SlurHeadRelation;
import org.audiveris.omr.util.HorizontalSide;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.util.Comparator;

/**
 * Class {@code SlurHeadLink} formalizes a link between a slur end and a head
 * in a chord nearby.
 * <p>
 * Rather than chord box center point, we use chord box middle vertical line.
 *
 * @author Hervé Bitteur
 */
public class SlurHeadLink
        extends Link
{
    //~ Static fields/initializers -----------------------------------------------------------------

    public static Comparator<SlurHeadLink> byEuclidean = new Comparator<SlurHeadLink>()
    {
        @Override
        public int compare (SlurHeadLink o1,
                            SlurHeadLink o2)
        {
            return Double.compare(
                    ((SlurHeadRelation) o1.relation).getEuclidean(),
                    ((SlurHeadRelation) o2.relation).getEuclidean());
        }
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SlurHeadLink} object.
     *
     * @param head the linked head
     * @param rel  precise relation instance
     */
    public SlurHeadLink (HeadInter head,
                         SlurHeadRelation rel)
    {
        super(head, rel, true);
    }

    //~ Methods ------------------------------------------------------------------------------------
    public static SlurHeadLink create (Point slurEnd,
                                       HorizontalSide slurSide,
                                       AbstractChordInter chord,
                                       HeadInter head)
    {
        SlurHeadRelation rel = new SlurHeadRelation(slurSide);

        // Define middle vertical line of chord box
        Rectangle box = chord.getBounds();
        Line2D vert = new Line2D.Double(
                box.x + (box.width / 2),
                box.y,
                box.x + (box.width / 2),
                box.y + box.height);
        rel.setEuclidean(vert.ptSegDist(slurEnd));

        return new SlurHeadLink(head, rel);
    }

    /**
     * Report the HeadChord which contains the linked head.
     *
     * @return the containing HeadChord
     */
    public HeadChordInter getChord ()
    {
        return (HeadChordInter) ((HeadInter) partner).getChord();
    }
}
