//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  T u p l e t G e n e r a t o r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.math.Rational;
import static org.audiveris.omr.math.Rational.ONE_OVER_FOUR;
import static org.audiveris.omr.math.Rational.THREE_OVER_TWO;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractTimeInter;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.TupletInter;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.util.Entities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Class {@code TupletGenerator} check each voice of a measure for suitable implicit
 * tuplet signs.
 * <p>
 * We have to choose the type (3 or 6) for all inserted tuplet instances and their locations.
 * <ol>
 * <li>Find the number of beats in the measure,
 * <li>Group chords on these beats,
 * <li>Assign one implicit tuplet per beat.
 * </ol>
 * BEWARE:
 * Moonlight sonata has a cut common signature (2/2), for which a beat value is 1/2, but
 * the notes in a measure are grouped as in a 4/4 signature (4 triplets) rather than 2 sixlets.
 * <p>
 * So, using the beat unit does not always work.
 * We thus look at beam groups as well when processing a voice with x/2 signature
 *
 * @author Hervé Bitteur
 */
public class TupletGenerator
{

    private static final Logger logger = LoggerFactory.getLogger(TupletGenerator.class);

    /** Underlying measure. */
    private final Measure measure;

    private final SystemInfo system;

    private final Scale scale;

    /**
     * Create a {@code TupletGenerator} object.
     *
     * @param measure the underlying measure
     */
    public TupletGenerator (Measure measure)
    {
        this.measure = measure;

        system = measure.getPart().getSystem();
        scale = system.getSheet().getScale();
    }

    //---------------------//
    // findImplicitTuplets //
    //---------------------//
    /**
     * Inspect all measure voices for those that start in first slot and exhibit a
     * duration which is exactly 3/2 times the measure expected duration.
     * <p>
     * Then force tuplet injection to each such voice
     *
     * @return the collection of tuplet signs created, perhaps empty
     */
    public List<TupletInter> findImplicitTuplets ()
    {
        final Rational expected = measure.getStack().getExpectedDuration();
        List<TupletInter> created = null;

        for (Voice voice : measure.getVoices()) {
            if (!voice.isWhole()) {
                final List<AbstractChordInter> chords = voice.getChords();
                final Rational start = chords.get(0).getTimeOffset();

                if (Rational.ZERO.equals(start)) {
                    final AbstractChordInter lastChord = chords.get(chords.size() - 1);

                    if (!lastChord.isWholeHead()) {
                        final Rational stop = lastChord.getEndTime();
                        final Rational ratio = stop.divides(expected);

                        if (ratio.equals(THREE_OVER_TWO)) {
                            if (created == null) {
                                created = new ArrayList<>();
                            }

                            created.addAll(forceTuplets(voice));
                            logger.info("{} {} rechecked with implicit tuplets", measure, voice);
                        }
                    }
                }
            }
        }

        if (created == null) {
            return Collections.EMPTY_LIST;
        }

        return created;
    }

    //----------------//
    // generateTuplet //
    //----------------//
    /**
     * Generate an implicit tuplet for the provided group of chords.
     * <p>
     * We use the approximate number of notes in the provided beat group to choose between
     * TUPLET_THREE and TUPLET_SIX for tuplet shape.
     * <p>
     * Working assumption: If a chord of a beam group is implied, link the whole beam group,
     * including the interleaved rests.
     *
     * @param group    (input) the original group of chords
     * @param extGroup (output) the extended group of chords
     * @return the generated implicit tuplet
     */
    public TupletInter generateTuplet (List<AbstractChordInter> group,
                                       List<AbstractChordInter> extGroup)
    {
        // Extend to beam group?
        for (AbstractChordInter chord : group) {
            BeamGroup bg = chord.getBeamGroup();

            if (bg != null) {
                for (AbstractChordInter c : bg.getAllChords()) {
                    if (!extGroup.contains(c)) {
                        extGroup.add(c);
                    }
                }
            } else {
                extGroup.add(chord);
            }
        }

        Collections.sort(extGroup, Inters.byCenterAbscissa);

        // Simplistic approach
        final int size = extGroup.size();
        final int nb = (int) Math.rint(size / 3.0);
        final Shape shape = (nb == 1) ? Shape.TUPLET_THREE : Shape.TUPLET_SIX;

        final TupletInter tuplet = new TupletInter(null, shape, 1.0);
        tuplet.setImplicit(true);

        // Precise tuplet bounds, above or below group
        tuplet.setBounds(inferTupletBounds(extGroup, shape));

        final SIGraph sig = system.getSig();
        sig.addVertex(tuplet);
        tuplet.setAbnormal(false);
        measure.addInter(tuplet);

        // Set relation between tuplet and every chord in group
        for (AbstractChordInter ch : extGroup) {
            sig.addEdge(ch, tuplet, new ChordTupletRelation(shape));
        }

        return tuplet;
    }

    //------------//
    // dumpVoices //
    //------------//
    /**
     * Dump measure voices, except the whole rest voices.
     */
    private void dumpVoices ()
    {
        logger.info("{}", measure);
        List<Voice> voices = new ArrayList<>(measure.getVoices());
        Collections.sort(voices, Voices.byId);

        for (Voice voice : voices) {
            Rational end = null;

            for (AbstractChordInter ch : voice.getChords()) {
                Rational chDur = ch.getDuration();
                end = end == null ? chDur : end.plus(chDur);
            }

            logger.info("   {} duration:{}", voice, end);
        }
    }

    //--------------//
    // forceTuplets //
    //--------------//
    /**
     * Inject implicit tuplet(s) for the provided voice.
     *
     * @param voice the voice to be processed
     * @return the collection of created tuplet signs
     */
    private List<TupletInter> forceTuplets (Voice voice)
    {
        // Use the time signature if available, otherwise assume 4 beats
        final AbstractTimeInter timeSig = measure.getStack().getCurrentTimeSignature();
        final Rational duration;

        if (timeSig != null) {
            duration = timeSig.getBeatValue();
        } else {
            duration = ONE_OVER_FOUR;
        }

        // Group chords by duration
        List<List<AbstractChordInter>> groups = buildGroups(voice, duration);

        // Check beam grouping in voice, is it consistent with beat value?
        // At least we can do this for beat value of 1/2
        if (!checkGroups(groups) && duration.equals(Rational.HALF)) {
            groups = buildGroups(voice, ONE_OVER_FOUR);
        }

        List<TupletInter> created = new ArrayList<>();

        // Process each beat group
        for (List<AbstractChordInter> group : groups) {
            List<AbstractChordInter> extGroup = new ArrayList<>();
            created.add(generateTuplet(group, extGroup));
        }

        return created;
    }

    //-------------//
    // buildGroups //
    //-------------//
    /**
     * Gather the chords of the provided voice into chord groups (likely to be linked
     * to a tuplet sign).
     *
     * @param voice    the provided voice
     * @param duration the normal duration of a chord group (generally one beat)
     * @return the sequence of chord groups
     */
    private List<List<AbstractChordInter>> buildGroups (Voice voice,
                                                        Rational duration)
    {
        // Raw duration (ignoring implicit tuplet signs)
        final Rational rawDuration = duration.times(THREE_OVER_TWO);

        final List<List<AbstractChordInter>> groups = new ArrayList<>();

        Rational nextTime = Rational.ZERO;
        List<AbstractChordInter> currentGroup = null;

        for (AbstractChordInter ch : voice.getChords()) {
            Rational time = ch.getTimeOffset();

            if (time.compareTo(nextTime) >= 0) {
                nextTime = nextTime.plus(rawDuration);
                groups.add(currentGroup = new ArrayList<>());
            }

            if (currentGroup != null) {
                currentGroup.add(ch);
            }
        }

        return groups;
    }

    //-------------//
    // checkGroups //
    //-------------//
    /**
     * Check there is at most one beam group per chord group
     *
     * @param groups the sequence of chord groups
     * @return true if OK
     */
    private boolean checkGroups (List<List<AbstractChordInter>> groups)
    {
        for (List<AbstractChordInter> group : groups) {
            BeamGroup beamGroup = null;

            for (AbstractChordInter ch : group) {
                BeamGroup bg = ch.getBeamGroup();

                if (bg != null) {
                    if (beamGroup == null) {
                        beamGroup = bg;
                    } else if (beamGroup != bg) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    //-------------------//
    // inferTupletBounds //
    //-------------------//
    /**
     * Determine acceptable location and dimension for an implicit tuplet that would
     * apply to the provided group of chords.
     *
     * @param group the provided group of chords
     * @param shape the tuplet shape
     * @return the precise tuplet bounds
     */
    private Rectangle inferTupletBounds (List<AbstractChordInter> group,
                                         Shape shape)
    {
        // Tuplet dimension
        MusicFont font = MusicFont.getBaseFont(scale.getInterline());
        TextLayout layout = font.layout(shape);
        Dimension dim = layout.getBounds().getBounds().getSize();

        // Vertical direction from group to tuplet, based on group tails side
        TreeMap<Integer, List<AbstractChordInter>> dirs = new TreeMap<>();

        for (AbstractChordInter ch : group) {
            int dir = ch.getStemDir();
            if (dir != 0) {
                List<AbstractChordInter> chords = dirs.get(dir);
                if (chords == null) {
                    dirs.put(dir, chords = new ArrayList<>());
                }
                chords.add(ch);
            }
        }
        int dir = 0;
        int bestCount = 0;

        for (Map.Entry<Integer, List<AbstractChordInter>> entry : dirs.entrySet()) {
            int count = entry.getValue().size();
            if (dir == 0 || bestCount < count) {
                dir = entry.getKey();
                bestCount = count;
            }
        }

        if (dir == 0) {
            dir = -1; // Safer
        }

        // Tuplet bounds
        Rectangle box = Entities.getBounds(group);
        Point center = GeoUtil.centerOf(box);
        int margin = dim.height / 10; // Small vertical margin between chord tail and tuplet
        Rectangle tupletBox = new Rectangle(
                center.x - (dim.width / 2),
                center.y + dir * (box.height / 2 + margin + ((dir < 0) ? dim.height : 0)),
                dim.width,
                dim.height);

        return tupletBox;
    }
}
