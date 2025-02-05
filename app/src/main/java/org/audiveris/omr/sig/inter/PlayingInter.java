//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     P l a y i n g I n t e r                                    //
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
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sig.relation.HeadPlayingRelation;
import org.audiveris.omr.sig.relation.Link;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>PlayingInter</code> represents a percussion playing sign.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "playing")
public class PlayingInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(PlayingInter.class);

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private PlayingInter ()
    {
    }

    /**
     * Creates a new <code>PlayingInter</code> object.
     *
     * @param glyph underlying glyph
     * @param shape PLAYING_OPEN, PLAYING_HALF_OPEN or PLAYING_CLOSED
     * @param grade evaluation value
     */
    public PlayingInter (Glyph glyph,
                         Shape shape,
                         Double grade)
    {
        super(glyph, null, shape, grade);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // checkAbnormal //
    //---------------//
    @Override
    public boolean checkAbnormal ()
    {
        // Check if a drum head  (TBD: or chord?) is connected
        setAbnormal(!sig.hasRelation(this, HeadPlayingRelation.class));

        return isAbnormal();
    }

    //----------//
    // getStaff //
    //----------//
    @Override
    public Staff getStaff ()
    {
        if (staff == null) {
            for (Relation rel : sig.getRelations(this, HeadPlayingRelation.class)) {
                final HeadInter head = (HeadInter) sig.getOppositeInter(this, rel);

                return staff = head.getStaff();
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
        for (Relation rel : sig.getRelations(this, HeadPlayingRelation.class)) {
            return sig.getOppositeInter(this, rel).getEnsemble().getVoice();
        }

        return null;
    }

    //------------//
    // lookupLink //
    //------------//
    /**
     * Try to detect a link between this playing instance and a head nearby.
     * <p>
     * We give preference to chords located below the playing sign, over the ones located above.
     *
     * @param systemHeadChords abscissa-ordered collection of head chords in system
     * @param profile          desired profile level
     * @return the link found or null
     */
    protected Link lookupLink (List<Inter> systemHeadChords,
                               int profile)
    {
        if (systemHeadChords.isEmpty()) {
            return null;
        }

        final SystemInfo system = systemHeadChords.get(0).getSig().getSystem();
        final Scale scale = system.getSheet().getScale();
        final int maxDx = scale.toPixels(HeadPlayingRelation.getXOutGapMaximum(profile));
        final int maxDy = scale.toPixels(HeadPlayingRelation.getYGapMaximum(profile));
        final Rectangle playingBox = getBounds();
        final Point playingCenter = getCenter();
        final Rectangle luBox = new Rectangle(playingCenter);
        luBox.grow(maxDx, maxDy);

        final List<Inter> chords = Inters.intersectedInters(
                systemHeadChords,
                GeoOrder.BY_ABSCISSA,
                luBox);

        if (chords.isEmpty()) {
            return null;
        }

        Collections.sort(chords, Inters.byReverseCenterOrdinate);
        HeadPlayingRelation bestRel = null;
        HeadChordInter bestChord = null;
        double bestYGap = Double.MAX_VALUE;
        boolean below = true; // We start with chords below the playing sign

        for (Inter chord : chords) {
            final Rectangle chordBox = chord.getBounds();

            // The playing sign cannot intersect the chord
            if (chordBox.intersects(playingBox)) {
                continue;
            }

            final Point chordCenter = chord.getCenter();

            // Select proper chord reference point (top or bottom)
            final int yRef = (playingCenter.y > chordCenter.y) //
                    ? chordBox.y + chordBox.height
                    : chordBox.y;
            final double dy = yRef - playingCenter.y;

            // Switching from chords below sign to chords above sign?
            if (below && dy < 0) {
                below = false;

                if (bestRel != null) {
                    break;
                }
            }

            double xGap = Math.abs(chordCenter.x - playingCenter.x);
            double yGap = Math.abs(dy);
            HeadPlayingRelation rel = new HeadPlayingRelation();
            rel.setOutGaps(scale.pixelsToFrac(xGap), scale.pixelsToFrac(yGap), profile);

            if (rel.getGrade() >= rel.getMinGrade()) {
                if ((bestRel == null) || (bestYGap > yGap)) {
                    bestRel = rel;
                    bestChord = (HeadChordInter) chord;
                    bestYGap = yGap;
                }
            }
        }

        if (bestChord != null) {
            // Choose preferred head in chord, according to vertical relative positions
            final List<? extends Inter> notes = bestChord.getNotes(); // Always bottom up
            final Inter bestHead = (playingCenter.y < bestChord.getCenter().y) ? notes.get(
                    notes.size() - 1) : notes.get(0);

            return new Link(bestHead, bestRel, false);
        }

        return null;
    }

    //-------------//
    // searchLinks //
    //-------------//
    @Override
    public Collection<Link> searchLinks (SystemInfo system)
    {
        final int profile = Math.max(getProfile(), system.getProfile());
        final List<Inter> systemHeadChords = system.getSig().inters(HeadChordInter.class);
        Collections.sort(systemHeadChords, Inters.byAbscissa);

        Link link = lookupLink(systemHeadChords, profile);

        return (link == null) ? Collections.emptyList() : Collections.singleton(link);
    }

    //---------------//
    // searchUnlinks //
    //---------------//
    @Override
    public Collection<Link> searchUnlinks (SystemInfo system,
                                           Collection<Link> links)
    {
        return searchObsoletelinks(links, HeadPlayingRelation.class);
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //------------------//
    // createValidAdded //
    //------------------//
    /**
     * (Try to) create and add a valid PlayingInter.
     *
     * @param glyph            underlying glyph
     * @param shape            detected shape
     * @param grade            assigned grade
     * @param system           containing system
     * @param systemHeadChords system head chords, ordered by abscissa
     * @return the created playing sign or null
     */
    public static PlayingInter createValidAdded (Glyph glyph,
                                                 Shape shape,
                                                 double grade,
                                                 SystemInfo system,
                                                 List<Inter> systemHeadChords)
    {
        if (glyph.isVip()) {
            logger.info("VIP PlayingInter createValidAdded {} as {}", glyph, shape);
        }

        PlayingInter playing = new PlayingInter(glyph, shape, grade);
        Link link = playing.lookupLink(systemHeadChords, system.getProfile());

        if (link != null) {
            system.getSig().addVertex(playing);
            link.applyTo(playing);

            return playing;
        }

        return null;
    }
}
