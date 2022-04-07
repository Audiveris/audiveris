//------------------------------------------------------------------------------------------------//
//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  S c o r e S i m i l a r i t y                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package conversion.score;

import org.audiveris.proxymusic.Backup;
import org.audiveris.proxymusic.Forward;
import org.audiveris.proxymusic.Note;
import org.audiveris.proxymusic.ScorePartwise;
import org.audiveris.proxymusic.ScorePartwise.Part;
import org.audiveris.proxymusic.ScorePartwise.Part.Measure;
import org.audiveris.proxymusic.Step;

import java.lang.String;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.audiveris.omr.util.SetOperation.diff;
import static org.audiveris.omr.util.SetOperation.intersection;
import static org.audiveris.omr.util.SetOperation.union;
import static org.junit.Assert.assertEquals;

/**
 * Utility class that calculates the similarity of two Scores.
 * Currently the similarity calculation is quite simplistic, just checking pitch and duration for
 * the notes and rests.
 *
 * @author Peter Greth
 */
public abstract class ScoreSimilarity
{
    /**
     * The conversion score of two ScorePartwises is the conversion score of their only Part.
     * Multiple Parts currently are not supported.
     */
    public static int conversionScore (ScorePartwise expected, ScorePartwise actual)
    {
        assertEquals("Multiple parts are currently not supported!", 1, expected.getPart().size());
        assertEquals("Multiple parts are currently not supported!", 1, actual.getPart().size());
        return conversionScore(expected.getPart().get(0), actual.getPart().get(0));
    }

    public static int conversionScore (Part expected, Part actual)
    {
        return conversionScore(expected.getMeasure(), actual.getMeasure());
    }

    /**
     * The conversion score of two lists of measures is the sum of corresponding measures'
     * conversion scores.
     * Two measures correspond if they have the same measure number.
     */
    public static int conversionScore (List<Measure> expected, List<Measure> actual)
    {
        final Map<String, Measure> expectedMeasuresByNumber = groupBy(expected, Measure::getNumber);
        final Map<String, Measure> actualMeasuresByNumber = groupBy(actual, Measure::getNumber);
        final Set<String> allMeasureNumbers = union(expectedMeasuresByNumber.keySet(),
                                                    actualMeasuresByNumber.keySet());

        return allMeasureNumbers
                .stream()
                .mapToInt(measureNumber -> conversionScore(
                        expectedMeasuresByNumber.getOrDefault(measureNumber, new Measure()),
                        actualMeasuresByNumber.getOrDefault(measureNumber, new Measure())
                ))
                .sum();
    }

    /**
     * The conversion score of two Measures is calculated by taking several "samples" and then
     * taking their score.
     * The measure is sampled using {@link #sampleMeasure(Measure)}.
     */
    public static int conversionScore (Measure expected, Measure actual)
    {
        return conversionScore(sampleMeasure(expected), sampleMeasure(actual));
    }

    /**
     * Conversion score of two Sets of samples is calculated by counting the common Samples and then
     * subtracting the amount of samples that just occur in one of the two Sets.
     */
    public static int conversionScore (Set<NoteSample> expected, Set<NoteSample> actual)
    {
        Set<NoteSample> common = intersection(expected, actual);
        Set<NoteSample> missing = diff(expected, actual);
        Set<NoteSample> superfluous = diff(actual, expected);
        return common.size() - missing.size() - superfluous.size();
    }

    /**
     * Takes samples of a measure by taking samples of the measure's notes and their in the measure.
     */
    public static Set<NoteSample> sampleMeasure (Measure measure)
    {
        Set<NoteSample> samples = new HashSet<>();

        BigDecimal currentOffset = BigDecimal.ZERO;
        BigDecimal lastNotesDuration = BigDecimal.ZERO;
        for (Object noteOrBackupOrForward : measure.getNoteOrBackupOrForward()) {
            // calculate a note's offset by tracking Backups and Forwards
            if (noteOrBackupOrForward instanceof Backup backup) {
                currentOffset = currentOffset.subtract(backup.getDuration());
            } else if (noteOrBackupOrForward instanceof Forward forward) {
                currentOffset = currentOffset.add(forward.getDuration());
            } else if (noteOrBackupOrForward instanceof Note note) {
                BigDecimal noteOffset = currentOffset;
                if (note.getChord() != null) { // "chord" note
                    // The "chord" is set for notes that have the same offset than the previous note. Unfortunately,
                    // the previous note's duration has already been added to the current offset (see below). To
                    // mitigate, we subtract it again here, s.t. both notes have the same offset.
                    noteOffset = noteOffset.subtract(lastNotesDuration);
                } else { // normal note
                    currentOffset = currentOffset.add(note.getDuration());
                    lastNotesDuration = note.getDuration();
                }

                samples.add(sampleNoteWithOffset(note, noteOffset));
            }
        }

        return samples;
    }

    /**
     * Takes several sample points of a Note while differing between normal notes and rests.
     */
    private static NoteSample sampleNoteWithOffset (Note note, BigDecimal noteOffset)
    {
        if (note.getRest() != null) {
            return NoteSample.ofRest(noteOffset, note.getDuration());
        } else {
            return NoteSample.ofNote(note.getPitch().getStep(), note.getPitch().getAlter(),
                                     note.getPitch().getOctave(), noteOffset, note.getDuration());
        }
    }

    /**
     * Utility function that transforms a List to a Map by getting the key from each entry.
     *
     * @param list   the List to convert
     * @param getKey the function that is executed for each entry to produce a key
     * @param <K>    the key type
     * @param <V>    the value type
     * @return a map whose values are the entries of the input list
     */
    private static <K, V> Map<K, V> groupBy (List<V> list, Function<V, K> getKey)
    {
        return list
                .stream()
                .collect(Collectors.toMap(getKey, m -> m));
    }

    /**
     * A sample of a note according to several sample points such as pitch, offset and duration.
     */
    private static record NoteSample(boolean isRest,
                                     Step step,
                                     BigDecimal alter,
                                     int octave,
                                     BigDecimal offset,
                                     BigDecimal duration)
    {
        private static NoteSample ofRest (BigDecimal offset, BigDecimal duration)
        {
            return new NoteSample(true, null, null, 0, offset, duration);
        }

        private static NoteSample ofNote (Step step,
                                          BigDecimal alter,
                                          int octave,
                                          BigDecimal offset,
                                          BigDecimal duration)
        {
            return new NoteSample(false, step, alter, octave, offset, duration);
        }
    }

}
