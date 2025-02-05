//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S m a l l C h o r d I n t e r                                 //
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
import org.audiveris.omr.math.GeoOrder;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.ChordGraceRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>SmallChordInter</code> is a general head chord composed of <b>small</b> heads.
 * <p>
 * For specific grace structures (acciaccatura and appoggiatura) we use sub-class
 * {@link GraceChordInter}.
 *
 * @see GraceChordInter
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "small-chord")
public class SmallChordInter
        extends HeadChordInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    protected SmallChordInter ()
    {
    }

    /**
     * Creates a new <code>SmallChordInter</code> object.
     *
     * @param grade the intrinsic grade
     */
    public SmallChordInter (Double grade)
    {
        super(grade);
    }

    /**
     * Protected constructor meant for GraceChordInter from a glyph recognized as a grace note.
     *
     * @param glyph underlying glyph
     * @param shape GRACE_NOTE, GRACE_NOTE_DOWN, GRACE_NOTE_SLASH or GRACE_NOTE_SLASH_DOWN
     * @param grade evaluation value
     */
    protected SmallChordInter (Glyph glyph,
                               Shape shape,
                               Double grade)
    {
        super(glyph, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "SmallChord";
    }

    //----------//
    // getVoice //
    //----------//
    /**
     * {@inheritDoc}
     * <p>
     * Specifically, a cue chord can't be part of voice chords.
     * So, we have to use a pull approach, to retrieve cue voice on demand only
     */
    @Override
    public Voice getVoice ()
    {
        AbstractChordInter lastChord = this;

        // If grouped via a beam, only the righ-most cue chord is linked to a standard chord
        final BeamGroupInter beamGroup = getBeamGroup();
        if (beamGroup != null) {
            final List<AbstractChordInter> siblings = beamGroup.getChords();
            lastChord = siblings.get(siblings.size() - 1);
        }

        for (Relation rel : sig.getRelations(lastChord, ChordGraceRelation.class)) {
            AbstractChordInter stdChord = (AbstractChordInter) sig.getOppositeInter(lastChord, rel);
            if (stdChord instanceof SmallChordInter) {
                continue; // Safer
            }
            return stdChord.getVoice();
        }

        return null;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this grace instance and a HeadChord on the right.
     * <p>
     * Grace head ordinate must be rather close to related chord head ordinate:
     * <ul>
     * <li>For melodic staff, delta pitch position = 1
     * <li>For percussion staff, delta pitch position = 0
     * </ul>
     *
     * @param system           related system
     * @param systemHeadChords abscissa-ordered collection of head chords in system
     * @param headCenter       center of grace head
     * @param profile          desired profile level
     * @return the link found or null
     */
    protected Link lookupLink (SystemInfo system,
                               List<Inter> systemHeadChords,
                               Point headCenter,
                               int profile)
    {
        if (staff == null) {
            staff = system.getClosestStaff(headCenter);
        }

        final Scale scale = system.getSheet().getScale();
        final int maxDx = scale.toPixels(ChordGraceRelation.getXOutGapMaximum(profile));
        final int maxDy = scale.toPixels(ChordGraceRelation.getYGapMaximum(profile));
        final Rectangle luBox = new Rectangle(headCenter);
        final boolean isDrum = staff.getPart().isDrumPart();
        luBox.grow(0, isDrum ? maxDy / 2 : maxDy);
        luBox.width = maxDx;

        final List<Inter> chords = Inters.intersectedInters(
                systemHeadChords,
                GeoOrder.BY_ABSCISSA,
                luBox);

        if (chords.isEmpty()) {
            return null;
        }

        // Choose the chord whose ending head is euclidian-wise closest to grace head center
        final Rectangle graceBox = getBounds();
        ChordGraceRelation bestRel = null;
        Inter bestChord = null;
        double bestDist = Double.MAX_VALUE;

        for (Inter inter : chords) {
            final HeadChordInter chord = (HeadChordInter) inter;
            final Rectangle chordBox = chord.getBounds();

            // The grace cannot intersect the chord
            if (chordBox.intersects(graceBox)) {
                continue;
            }

            final Point hc = chord.getLeadingNote().getCenter(); // Head center
            final Point vector = PointUtil.subtraction(hc, headCenter); // target -> head center

            final double dist = PointUtil.length(vector);
            final double xGap = Math.abs(vector.x);
            final double yGap = Math.abs(vector.y);
            final ChordGraceRelation rel = new ChordGraceRelation();
            rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), profile);

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestDist > dist)) {
                    bestRel = rel;
                    bestChord = chord;
                    bestDist = dist;
                }
            }
        }

        return (bestRel != null) ? new Link(bestChord, bestRel, false) : null;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        final HeadInter head = getLeadingNote();
        final int profile = Math.max(getProfile(), system.getProfile());
        final Link link = lookupLink(system, systemHeadChords, head.getCenter(), profile);
        return (link != null) ? Arrays.asList(link) : Collections.emptyList();
    }

    //----------//
    // setVoice //
    //----------//
    /**
     * We should not explicitly set a voice to a cue chord, because its voice is determined
     * dynamically (pull approach) in getVoice() method.
     *
     * @see #getVoice()
     */
    @Override
    public void setVoice (Voice voice)
    {
        throw new IllegalStateException("Attempt to setVoice() on " + this);
    }
}
