//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       T e m p o I n t e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextRole;
import static org.audiveris.omr.util.RegexUtil.getGroup;
import static org.audiveris.omr.util.RegexUtil.group;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class <code>TempoInter</code> is a sentence that specifies a tempo value.
 * <p>
 * Its contained words are expected as:
 * <ol>
 * <li>A note symbol (quarter, half, dotted quarter, 8th note).
 * <li>The "=" sign
 * <li>A positive number giving the beats-per-minute value
 * </ol>
 * Comments:
 * <ol>
 * <li>This is a preliminary version meant for scores where the note symbol is a quarter,
 * often OCR'ed as a capital 'J'.
 * <li>Perhaps a better version could recognize a note symbol on the left side of the "=" sign.
 * We would need to add the supported note symbols as physical shapes in the {@link Shape} class,
 * provide samples and train the glyph classifier on these new shapes.
 * </ol>
 *
 * @author Hervé Bitteur
 */
public class TempoInter
        extends SentenceInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(TempoInter.class);

    private static final String NOTE = "note";

    private static final String EQUAL = "equal";

    private static final String BPM = "bpm";

    private static final String spacePat = "\\s*";

    /** Pattern for note. */
    private static final String notePat = group(NOTE, "[J]"); // To be improved!

    /** Pattern for equal. */
    private static final String equalPat = group(EQUAL, "=");

    /** Pattern for bpm. */
    private static final String bpmPat = group(BPM, "[0-9]+");

    /** Pattern for the whole tempo instruction. */
    private static final String tempoPat = notePat + spacePat + equalPat + spacePat + bpmPat;

    private static final Pattern tempoPattern = Pattern.compile(tempoPat);

    //~ Instance fields ----------------------------------------------------------------------------

    final TempoNote note;

    final int bpm;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    private TempoInter ()
    {
        this.note = null;
        this.bpm = 0;
    }

    /**
     * Create a new TempoInter object.
     *
     * @param note the base note (QUARTER only for this version)
     * @param bpm  the number of beats per minute
     */
    private TempoInter (TextLine line,
                        TempoNote note,
                        Integer bpm)
    {
        super(line.getBounds(), line.getGrade(), line.getMeanFont(), TextRole.Tempo);
        this.note = note;
        this.bpm = bpm;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // getBpm //
    //--------//
    public int getBpm ()
    {
        return bpm;
    }

    //---------//
    // getNote //
    //---------//
    public TempoNote getNote ()
    {
        return note;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return new StringBuilder(super.internals())//
                .append(" note:").append(note)//
                .append(" bpm:").append(bpm)//
                .toString();
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //-------------//
    // createValid //
    //-------------//
    /**
     * Try to create a TempoInter instance from the provided text line.
     *
     * @param line the provided text line
     * @return the tempo specification or null if failed
     */
    public static TempoInter createValid (TextLine line)
    {

        final String str = line.getValue();
        final Matcher matcher = tempoPattern.matcher(str);

        if (matcher.matches()) {

            final TempoNote note = TempoNote.QUARTER; // Imposed for the first version
            final String noteStr = getGroup(matcher, NOTE);
            final String equalStr = getGroup(matcher, EQUAL);
            final String bpmStr = getGroup(matcher, BPM);

            try {
                final Integer bpm = Integer.decode(bpmStr);

                return new TempoInter(line, note, bpm);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        return null;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    public static enum TempoNote
    {
        QUARTER,
        HALF,
        DOTTED_QUARTER,
        EIGHTH;
    }
}
