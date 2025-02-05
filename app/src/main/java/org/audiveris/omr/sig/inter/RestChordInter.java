//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   R e s t C h o r d I n t e r                                  //
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

import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.sig.relation.BeamRestRelation;
import org.audiveris.omr.sig.relation.Relation;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>RestChordInter</code> is a AbstractChordInter composed of (one) rest.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "rest-chord")
public class RestChordInter
        extends AbstractChordInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB (and for FakeChord subclass).
     */
    protected RestChordInter ()
    {
    }

    /**
     * Creates a new <code>RestChordInter</code> object.
     *
     * @param grade the intrinsic grade
     */
    public RestChordInter (Double grade)
    {
        super(grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // getBeams //
    //----------//
    /**
     * Report the sequence of beams that are attached to this chord.
     * <p>
     * The sequence is generally empty except for an interleaved rest.
     *
     * @return the list of attached beams, perhaps empty
     */
    @Override
    public List<AbstractBeamInter> getBeams ()
    {
        if (beams == null) {
            beams = new ArrayList<>();

            if (sig == null) {
                return beams;
            }

            final List<Inter> members = getMembers();

            if (members.isEmpty()) {
                return beams;
            }

            final Inter rest = members.get(0);

            for (Relation rel : sig.getRelations(rest, BeamRestRelation.class)) {
                beams.add((AbstractBeamInter) sig.getOppositeInter(rest, rel));
            }

            final Point restLoc = getCenter();
            final int x = restLoc.x;
            final int yRest = restLoc.y;

            // Keep the sequence ordered by distance from chord tail
            Collections.sort(
                    beams,
                    (AbstractBeamInter b1,
                     AbstractBeamInter b2) -> Double.compare(
                             Math.abs(yRest - LineUtil.yAtX(b2.getMedian(), x)),
                             Math.abs(yRest - LineUtil.yAtX(b1.getMedian(), x))));
        }

        return beams;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "RestChord";
    }
}
