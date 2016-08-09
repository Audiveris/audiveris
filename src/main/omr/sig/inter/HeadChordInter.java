//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H e a d C h o r d I n t e r                                  //
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
package omr.sig.inter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(HeadChordInter.class);

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
     * No-arg constructor meant for JAXB.
     */
    private HeadChordInter ()
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
//
//        clone.stem = stem.duplicate();
//
        // Notes (we make a deep copy of each note)
        for (AbstractNoteInter note : notes) {
            AbstractNoteInter newHead = null;

            if (note instanceof BlackHeadInter) {
                BlackHeadInter blackHead = (BlackHeadInter) note;
                newHead = blackHead.duplicate();
            } else if (note instanceof VoidHeadInter) {
                VoidHeadInter voidHead = (VoidHeadInter) note;
                newHead = toBlack ? voidHead.duplicateAsBlack() : voidHead.duplicate();
            } else {
                logger.error("No duplication supported for {}", note);
            }

            if (newHead != null) {
                clone.addMember(newHead);
//
//                // Replicate HeadStem relations
//                for (Relation hs : sig.getRelations(note, HeadStemRelation.class)) {
//                    sig.addEdge(newHead, clone.stem, hs.duplicate());
//                }
            }
        }

        return clone;
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
