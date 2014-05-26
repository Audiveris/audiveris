//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         S t e m I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.util.HorizontalSide;

/**
 * Class {@code StemInter} represents Stem interpretations.
 *
 * @author Hervé Bitteur
 */
public class StemInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new StemInter object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the grade details
     */
    public StemInter (Glyph glyph,
                      GradeImpacts impacts)
    {
        super(glyph, Shape.STEM, impacts);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------------//
    // getMinGrade //
    //-------------//
    public static double getMinGrade ()
    {
        return AbstractInter.getMinGrade();
    }

    @Override
    public boolean isGood ()
    {
        return getGrade() >= 0.45; // BINGO DIRTY HACK
    }

    //------------//
    // lookupHead //
    //------------//
    /**
     * Lookup a head connected to this stem, with proper head side and pitch values.
     * Beware side is defined WRT head, not WRT stem.
     *
     * @param side  desired head side
     * @param pitch desired pitch position
     * @return the head instance if found, null otherwise
     */
    public Inter lookupHead (HorizontalSide side,
                             int pitch)
    {
        for (Relation rel : sig.getRelations(this, HeadStemRelation.class)) {
            HeadStemRelation hsRel = (HeadStemRelation) rel;

            // Check side
            if (hsRel.getHeadSide() == side) {
                // Check pitch
                AbstractNoteInter head = (AbstractNoteInter) sig.getEdgeSource(rel);

                if (head.getPitch() == pitch) {
                    return head;
                }
            }
        }

        return null;
    }
}
