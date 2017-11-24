//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H e a d C h o r d I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Comparator;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code HeadChordInter} is a AbstractChordInter composed of heads.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "head-chord")
public class HeadChordInter
        extends AbstractChordInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            HeadChordInter.class);

    /**
     * Compare two heads (assumed to be) of the same chord, ordered by
     * increasing distance from chord head ordinate.
     */
    public static final Comparator<HeadInter> headComparator = new Comparator<HeadInter>()
    {
        @Override
        public int compare (HeadInter n1,
                            HeadInter n2)
        {
            if (n1 == n2) {
                return 0;
            }

            AbstractChordInter c1 = n1.getChord();

            //            if (c1 != n2.getChord()) {
            //                logger.error("Comparing notes from different chords");
            //            }
            //
            return c1.getStemDir() * (n1.getCenter().y - n2.getCenter().y);
        }
    };

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code HeadChordInter} object.
     *
     * @param grade the intrinsic grade
     */
    public HeadChordInter (double grade)
    {
        super(grade);
    }

    /**
     * Creates a new {@code HeadChordInter} object.
     *
     * @param grade the intrinsic grade
     * @param stem  the related stem
     */
    public HeadChordInter (double grade,
                           StemInter stem)
    {
        super(grade, stem);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected HeadChordInter ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // duplicate //
    //-----------//
    /**
     * Make a clone of a chord (just its heads, not its stem or its beams).
     * <p>
     * This duplication is needed when a chord is shared by two BeamGroups.
     *
     * @param toBlack should we duplicate to black head? (for void head)
     * @return a clone of this chord (including heads, but stem and beams are not copied)
     */
    public HeadChordInter duplicate (boolean toBlack)
    {
        // Beams are not copied
        HeadChordInter clone = new HeadChordInter(getGrade());
        clone.setMirror(this);
        sig.addVertex(clone);
        setMirror(clone);

        clone.setStaff(staff);

        // Notes (we make a deep copy of each note head)
        for (Inter note : getMembers()) {
            HeadInter head = (HeadInter) note;
            AbstractNoteInter newHead = null;

            switch (head.getShape()) {
            case NOTEHEAD_BLACK:
                newHead = head.duplicate();

                break;

            case NOTEHEAD_VOID:
                newHead = toBlack ? head.duplicateAs(Shape.NOTEHEAD_BLACK) : head.duplicate();

                break;

            default:
                logger.error("No duplication supported for {}", note);

                break;
            }

            if (newHead != null) {
                clone.addMember(newHead);
            }
        }

        return clone;
    }

    //----------------//
    // getHeadsBounds //
    //----------------//
    /**
     * Report the bounding box of just the chord heads, without the stem if any.
     *
     * @return the heads bounding box
     */
    public Rectangle getHeadsBounds ()
    {
        return Entities.getBounds(getMembers());
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "HeadChord";
    }
}
