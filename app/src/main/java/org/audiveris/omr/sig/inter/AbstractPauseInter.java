//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               A b s t r a c t P a u s e I n t e r                              //
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.ChordPauseRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Class <code>AbstractPauseInter</code> represents a breath-mark or a caesura.
 * It is located right after its prior reference head chord.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractPauseInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(AbstractPauseInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    protected AbstractPauseInter ()
    {
    }

    /**
     * Creates a new <code>AbstractPauseInter</code> object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (BREATH_MARK, CAESURA)
     * @param grade the interpretation quality
     */
    public AbstractPauseInter (Glyph glyph,
                               Shape shape,
                               Double grade)
    {
        super(glyph, (glyph != null) ? glyph.getBounds() : null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------//
    // added //
    //-------//
    @Override
    public void added ()
    {
        super.added();

        setAbnormal(true); // No chord linked yet
    }

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a chord is connected
        setAbnormal(!sig.hasRelation(this, ChordPauseRelation.class));

        return isAbnormal();
    }

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            for (Relation rel : sig.getRelations(this, ChordPauseRelation.class)) {
                return staff = sig.getOppositeInter(this, rel).getStaff();
            }
        }

        return staff;
    }

    //----------//
    // getVoice //
    //----------//
    @Override
    public Voice getVoice ()
    {
        for (Relation rel : sig.getRelations(this, ChordPauseRelation.class)) {
            return sig.getOppositeInter(this, rel).getVoice();
        }

        return null;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this Pause instance and a prior HeadChord.
     * <p>
     * We can reasonably restrict the abscissa of the prior head chord to be located
     * between the measure start abscissa and the pause right side.
     * And we choose the head chord which is abscissa-wise closest to the pause sign.
     * <p>
     * NOTA: The staff element of the Pause instance is assumed to be set.
     *
     * @param systemHeadChords collection of head chords in system, ordered by abscissa
     * @param profile          desired profile level
     * @return the link found or null
     */
    private Link lookupLink (List<Inter> systemHeadChords,
                             int profile)
    {
        if (staff == null) {
            logger.error("Method lookupLink() called on a AbstractPauseInter with a null staff");
            return null;
        }

        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final SystemInfo system = staff.getSystem();
        final MeasureStack stack = system.getStackAt(getCenter());
        if (stack == null) {
            return null;
        }

        final Measure measure = stack.getMeasureAt(staff);
        final int xMin = measure.getAbscissa(HorizontalSide.LEFT, staff);
        final int xMax = getCenterRight().x;

        final List<Inter> chords = Inters.inters(systemHeadChords, (Inter inter) -> {
            if (inter instanceof HeadChordInter headChord) {
                if (!headChord.getStaves().contains(staff)) {
                    return false;
                }
            } else if ((inter == null) || (inter.getStaff() != staff)) {
                return false;
            }

            final Point center = inter.getCenter();

            return (center.x >= xMin) && (center.x <= xMax);
        });

        if (chords.isEmpty()) {
            return null;
        }

        // The best chord is simply the last (right-most) one
        final Inter bestChord = chords.get(chords.size() - 1);
        final Scale scale = system.getSheet().getScale();
        final double dx = xMax - bestChord.getCenter().x;
        final ChordPauseRelation bestRel = new ChordPauseRelation();
        bestRel.setOutGaps(scale.pixelsToFrac(dx), 0 /* dy is irrelevant */, profile);

        if (bestRel.getGrade() < bestRel.getMinGrade()) {
            return null;
        }

        return new Link(bestChord, bestRel, false);
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        final int profile = Math.max(getProfile(), system.getProfile());
        final Link link = lookupLink(systemHeadChords, profile);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, ChordPauseRelation.class);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create a AbstractPauseInter.
     *
     * @param glyph            underlying glyph
     * @param shape            detected shape
     * @param grade            assigned grade
     * @param staff            the closest staff
     * @param systemHeadChords system head chords, ordered by abscissa
     * @return the created articulation or null
     */
    public static AbstractPauseInter createValidAdded (Glyph glyph,
                                                       Shape shape,
                                                       Double grade,
                                                       Staff staff,
                                                       List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP PauseInter create {} as {}", glyph, shape);
        }

        final AbstractPauseInter pause = switch (shape) {
            case BREATH_MARK -> new BreathMarkInter(glyph, grade);
            case CAESURA -> new CaesuraInter(glyph, grade);
            default -> throw new IllegalArgumentException("No PauseInter for shape " + shape);
        };

        pause.setStaff(staff);

        final SystemInfo system = staff.getSystem();
        final Link link = pause.lookupLink(systemHeadChords, system.getProfile());

        if (link != null) {
            system.getSig().addVertex(pause);
            link.applyTo(pause);

            return pause;
        }

        return null;
    }
}
