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
package omr.sig.inter;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.sheet.Scale;
import omr.sheet.rhythm.Voice;

import omr.sig.GradeImpacts;
import omr.sig.SIGraph;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.HeadStemRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.StemConnection;
import omr.sig.relation.StemPortion;
import static omr.sig.relation.StemPortion.STEM_BOTTOM;
import static omr.sig.relation.StemPortion.STEM_TOP;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;
import static omr.util.HorizontalSide.RIGHT;

import java.awt.geom.Line2D;
import java.util.ArrayList;
import java.util.List;

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
        super(glyph, null, Shape.STEM, impacts);
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

    //-----------//
    // duplicate //
    //-----------//
    public StemInter duplicate ()
    {
        StemInter clone = new StemInter(glyph, impacts);
        clone.setMirror(this);
        sig.addVertex(clone);
        setMirror(clone);

        return clone;
    }

    //--------------//
    // getDirection //
    //--------------//
    /**
     * Report the direction (from head to tail) of this stem.
     * <p>
     * For this, we check what is found on each stem end (is it a tail: beam/flag or is it a head)
     * and use contextual grade to pick up the best reference.
     *
     * @return -1 for stem up, +1 for stem down, 0 for unknown
     */
    public int getDirection ()
    {
        Scale scale = sig.getSystem().getSheet().getScale();
        final Line2D stemLine = sig.getStemLine(this);
        final List<Relation> links = new ArrayList<Relation>(
                sig.getRelations(this, StemConnection.class));
        sig.sortBySource(links);

        for (Relation rel : links) {
            Inter source = sig.getEdgeSource(rel); // Source is a head, a beam or a flag

            // Retrieve the stem portion for this link
            if (rel instanceof HeadStemRelation) {
                // Head -> Stem
                HeadStemRelation link = (HeadStemRelation) rel;
                StemPortion portion = link.getStemPortion(source, stemLine, scale);

                if (portion == STEM_BOTTOM) {
                    if (link.getHeadSide() == RIGHT) {
                        return -1;
                    }
                } else if (portion == STEM_TOP) {
                    if (link.getHeadSide() == LEFT) {
                        return 1;
                    }
                }
            } else {
                // Tail (Beam or Flag) -> Stem
                if (rel instanceof BeamStemRelation) {
                    // Beam -> Stem
                    BeamStemRelation link = (BeamStemRelation) rel;
                    StemPortion portion = link.getStemPortion(source, stemLine, scale);

                    return (portion == STEM_TOP) ? (-1) : 1;
                } else {
                    // Flag -> Stem
                    FlagStemRelation link = (FlagStemRelation) rel;
                    StemPortion portion = link.getStemPortion(source, stemLine, scale);

                    if (portion == STEM_TOP) {
                        return -1;
                    }

                    if (portion == STEM_BOTTOM) {
                        return 1;
                    }
                }
            }
        }

        return 0; // Cannot decide!
    }

    //-------------//
    // getMinGrade //
    //-------------//
    public static double getMinGrade ()
    {
        return AbstractInter.getMinGrade();
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, HeadStemRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //--------//
    // isGood //
    //--------//
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
                AbstractHeadInter head = (AbstractHeadInter) sig.getEdgeSource(rel);

                if (head.getIntegerPitch() == pitch) {
                    return head;
                }
            }
        }

        return null;
    }
}
