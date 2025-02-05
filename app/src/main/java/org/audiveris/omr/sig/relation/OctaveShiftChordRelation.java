//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                         O c t a v e S h i f t C h o r d R e l a t i o n                        //
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
package org.audiveris.omr.sig.relation;

import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.OctaveShiftInter;
import org.audiveris.omr.util.HorizontalSide;
import static org.audiveris.omr.util.HorizontalSide.LEFT;
import static org.audiveris.omr.util.HorizontalSide.RIGHT;

import org.jgrapht.event.GraphEdgeChangeEvent;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>OctaveShiftChordRelation</code> represents the relation between an instance of
 * <b>OctaveShiftInter</b> and an embraced chord on side of the shift sign.
 *
 * @see OctaveShiftInter
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "octave-shift-chord")
public class OctaveShiftChordRelation
        extends Support
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** This is the octave shift side (left or right) where the chord is located. */
    @XmlAttribute(name = "side")
    private HorizontalSide side;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB and user allocation.
     */
    public OctaveShiftChordRelation ()
    {
    }

    /**
     * Creates a new <code>OctaveShiftChordRelation</code> object.
     *
     * @param side the left or right side of the octave shift
     */
    public OctaveShiftChordRelation (HorizontalSide side)
    {
        this.side = side;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    /**
     * Populate side if needed.
     *
     * @param e edge change event
     */
    @Override
    public void added (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final OctaveShiftInter os = (OctaveShiftInter) e.getEdgeSource();

        if (side == null) {
            final Inter chord = e.getEdgeTarget();
            side = (os.getCenter().x < chord.getCenter().x) ? RIGHT : LEFT;
        }

        os.checkAbnormal();
    }

    //---------//
    // getSide //
    //---------//
    /**
     * @return the side
     */
    public HorizontalSide getSide ()
    {
        return side;
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        // A source octave shift can be linked to two chords, one on left side and one of right side
        return false;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        // A target chord can be linked to at most one octave shift.
        return true;
    }

    //---------//
    // removed //
    //---------//
    @Override
    public void removed (GraphEdgeChangeEvent<Inter, Relation> e)
    {
        final OctaveShiftInter os = (OctaveShiftInter) e.getEdgeSource();

        if (!os.isRemoved()) {
            os.checkAbnormal();
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return super.toString() + "/" + side;
    }
}
