//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    B e a t U n i t I n t e r                                   //
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
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.ui.DefaultEditor;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;
import org.audiveris.omr.util.StringUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.util.Objects;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>BeatUnitInter</code> is a word that represents the beat specification part in a
 * metronome mark.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "beat-unit")
@XmlAccessorType(XmlAccessType.NONE)
public class BeatUnitInter
        extends MusicWordInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BeatUnitInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /** Unit symbol, perhaps dotted. */
    @XmlElement
    private Note note;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    @SuppressWarnings("unused")
    private BeatUnitInter ()
    {
    }

    /**
     * Creates a new <code>BeatUnitInter</code> object meant for manual assignment.
     *
     * @param shape one of the METRO shapes
     * @param grade the interpretation quality
     */
    public BeatUnitInter (Shape shape,
                          Double grade)
    {
        super(grade, shape, Note.noteOf(shape).getString());
        note = Note.noteOf(shape);
    }

    /**
     * Creates a new <code>BeatUnitInter</code> object.
     *
     * @param glyph     the underlying glyph
     * @param bounds    the precise object bounds
     * @param grade     the interpretation quality
     * @param value     the word content (music characters)
     * @param musicFont the current music font
     * @param note      the metronome note type
     * @param location  the baseline location
     */
    public BeatUnitInter (Glyph glyph,
                          Rectangle bounds,
                          Double grade,
                          String value,
                          MusicFont musicFont,
                          Note note,
                          Point location)
    {
        super(glyph, bounds, grade, note.toShape(), value, musicFont, location);
        this.note = note;
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

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation)
    {
        //logger.info("BeatUnitInter.deriveFrom {}", symbol);
        //        MetronomeSymbol metroSymbol = (MetronomeSymbol) symbol;
        //        MetronomeInter.Model model = metroSymbol.getModel(font, dropLocation);
        //        setValue(model.value);
        //        fontInfo = model.fontInfo;
        //        location = new Point2D.Double(model.baseLoc.getX(), model.baseLoc.getY());
        //        setBounds(null);
        //
        //        return true;

        return false;
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new DefaultEditor(this);
    }

    //---------//
    // getNote //
    //---------//
    /**
     * Report the note used as the beat unit.
     *
     * @return the unit note
     */
    public Note getNote ()
    {
        return note;
    }

    //---------//
    // setNote //
    //---------//
    public void setNote (Note note)
    {
        this.note = note;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return shape.toString();
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Assign a new value and update note accordingly.
     *
     * @param value the new value
     */
    @Override
    public void setValue (String value)
    {
        super.setValue(value);

        note = Note.decode(value);
        shape = note.toShape();
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //------//
    // Note //
    //------//
    /** Notes that can appear as beat units in a metronome mark. */
    public static enum Note
    {
        WHOLE(32),
        HALF(16),
        QUARTER(8),
        EIGHTH(4),
        SIXTEENTH(2),
        DOTTED_HALF(24),
        DOTTED_QUARTER(12),
        DOTTED_EIGHTH(6),
        DOTTED_SIXTEENTH(3);

        /** Duration, specified in 1/32. */
        private final int duration;

        Note (int duration)
        {
            this.duration = duration;
        }

        public String getString ()
        {
            final Shape shape = toShape();
            final Symbols symbols = MusicFamily.Bravura.getSymbols();

            return MusicFont.getString(symbols.getCode(shape));
        }

        public boolean hasDot ()
        {
            return switch (this) {
                case WHOLE, HALF, QUARTER, EIGHTH, SIXTEENTH -> false;
                case DOTTED_HALF, DOTTED_QUARTER, DOTTED_EIGHTH, DOTTED_SIXTEENTH -> true;
            };
        }

        /**
         * Report the note duration, expressed in quarters.
         *
         * @return A rational number representing the quarter-based duration
         */
        public Rational quarterValue ()
        {
            return new Rational(duration, QUARTER.duration);
        }

        public Shape toShape ()
        {
            return switch (this) {
                case WHOLE -> Shape.METRO_WHOLE;
                case HALF -> Shape.METRO_HALF;
                case QUARTER -> Shape.METRO_QUARTER;
                case EIGHTH -> Shape.METRO_EIGHTH;
                case SIXTEENTH -> Shape.METRO_SIXTEENTH;
                case DOTTED_HALF -> Shape.METRO_DOTTED_HALF;
                case DOTTED_QUARTER -> Shape.METRO_DOTTED_QUARTER;
                case DOTTED_EIGHTH -> Shape.METRO_DOTTED_EIGHTH;
                case DOTTED_SIXTEENTH -> Shape.METRO_DOTTED_SIXTEENTH;
            };
        }

        public String toMusicXml ()
        {
            // Reminder: the potential augmentation dot is handled separately in MusicXML
            return switch (this) {
                case WHOLE -> "whole";
                case HALF, DOTTED_HALF -> "half";
                case QUARTER, DOTTED_QUARTER -> "quarter";
                case EIGHTH, DOTTED_EIGHTH -> "eighth";
                case SIXTEENTH, DOTTED_SIXTEENTH -> "16th";
            };
        }

        /**
         * Infer the note from the provided string codes.
         *
         * @param str the provided string
         * @return the decoded note, or null
         */
        public static Note decode (String str)
        {
            final String shrunk = StringUtil.shrink(str);

            for (Note note : Note.values()) {
                final String noteStr = StringUtil.shrink(note.getString());

                if (shrunk.equals(noteStr)) {
                    return note;
                }
            }

            return null;
        }

        public static Note noteOf (Shape shape)
        {
            Objects.requireNonNull(shape, "Null shape value");

            return switch (shape) {
                case Shape.METRO_WHOLE -> Note.WHOLE;
                case Shape.METRO_HALF -> Note.HALF;
                case Shape.METRO_QUARTER -> Note.QUARTER;
                case Shape.METRO_EIGHTH -> Note.EIGHTH;
                case Shape.METRO_SIXTEENTH -> Note.SIXTEENTH;
                case Shape.METRO_DOTTED_HALF -> Note.DOTTED_HALF;
                case Shape.METRO_DOTTED_QUARTER -> Note.DOTTED_QUARTER;
                case Shape.METRO_DOTTED_EIGHTH -> Note.DOTTED_EIGHTH;
                case Shape.METRO_DOTTED_SIXTEENTH -> Note.DOTTED_SIXTEENTH;
                default -> null;
            };
        }
    }
}
