//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   H e a d C h o r d I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code HeadChordInter} is a ChordInter composed of heads.
 *
 * @author Hervé Bitteur
 */
public class HeadChordInter
        extends ChordInter
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

    //~ Methods ------------------------------------------------------------------------------------
    //-----------//
    // duplicate //
    //-----------//
    /**
     * Make a clone of a chord (except for its beams).
     * <p>
     * This duplication is needed when a chord is shared by two BeamGroups.
     *
     * @param toBlack should we duplicate to black head? (for void head)
     * @return a clone of this chord (including notes, but beams are not copied)
     */
    public HeadChordInter duplicate (boolean toBlack)
    {
        // Beams are not copied
        HeadChordInter clone = new HeadChordInter(getGrade());
        clone.setMirror(this);
        sig.addVertex(clone);
        setMirror(clone);

        clone.setStaff(staff);

        clone.stem = stem.duplicate();

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

                // Replicate HeadStem relations
                for (Relation hs : sig.getRelations(note, HeadStemRelation.class)) {
                    sig.addEdge(newHead, clone.stem, hs.duplicate());
                }
            }
        }

        clone.tupletFactor = tupletFactor; /// ??? TODO
        clone.dotsNumber = dotsNumber; /// ??? TODO

        ///clone.flagsNumber = flagsNumber; // Not sure TODO
        // Insure correct ordering of chords within their container
        ///Collections.sort(getParent().getChildren(), chordComparator);
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
