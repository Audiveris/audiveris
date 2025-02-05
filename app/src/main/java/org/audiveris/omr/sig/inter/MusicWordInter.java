//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   M u s i c W o r d I n t e r                                  //
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
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>MusicWordInter</code> represents a word made of music characters.
 * <p>
 * This kind of word is used to handle the music notes in a metronome indication.
 * <p>
 * Examples taken from MusicXML reference:
 * <dl>
 * <dt>metronome</dt>
 * <dd><img src="https://www.w3.org/2021/06/musicxml40/static/examples/metronome-element.png">
 * </dd>
 * <dt>metronome-note</dt>
 * <dd><img src=
 * "https://www.w3.org/2021/06/musicxml40/static/examples/metronome-note-element.png"></dd>
 * <dt>per-minute</dt>
 * <dd><img src=
 * "https://www.w3.org/2021/06/musicxml40/static/examples/per-minute-element.png"></dd>
 * <dt>beat-unit-dot</dt>
 * <dd><img src=
 * "https://www.w3.org/2021/06/musicxml40/static/examples/beat-unit-dot-element.png"></dd>
 * <dt>beat-unit-tied</dt>
 * <dd><img src=
 * "https://www.w3.org/2021/06/musicxml40/static/examples/beat-unit-tied-element.png"></dd>
 * <dt>beat-unit</dt>
 * <dd><img src=
 * "https://www.w3.org/2021/06/musicxml40/static/examples/beat-unit-element.png"></dd>
 * <dt>metronome-arrows</dt>
 * <dd><img src=
 * "https://www.w3.org/2021/06/musicxml40/static/examples/metronome-arrows-element.png"></dd>
 * <dt>metronome-tied</dt>
 * <dd><img src=
 * "https://www.w3.org/2021/06/musicxml40/static/examples/metronome-tied-element.png"></dd>
 * <dt>beat-unit-dot</dt>
 * <dd><img src=
 * "https://www.w3.org/2021/06/musicxml40/static/examples/beat-unit-dot-element.png"></dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "music-word")
@XmlAccessorType(XmlAccessType.NONE)
public class MusicWordInter
        extends WordInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MusicWordInter.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Temporary use during marshalling/unmarshalling. */
    @XmlElement(name = "code")
    private volatile List<String> codes;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    @SuppressWarnings("unused")
    protected MusicWordInter ()
    {
    }

    /**
     * Creates a new <code>MusicWordInter</code> object, meant for manual assignment.
     *
     * @param grade the interpretation quality
     * @param shape the precise shape if any
     * @param value the word content (music characters)
     */
    public MusicWordInter (Double grade,
                           Shape shape,
                           String value)
    {
        super(null, null, shape, grade, value, null, null);
    }

    /**
     * Creates a new <code>MusicWordInter</code> object.
     *
     * @param glyph     the underlying glyph
     * @param bounds    the precise object bounds
     * @param grade     the interpretation quality
     * @param shape     the precise shape if any
     * @param value     the word content (music characters)
     * @param musicFont the music font to use
     * @param location  the baseline location
     */
    public MusicWordInter (Glyph glyph,
                           Rectangle bounds,
                           Double grade,
                           Shape shape,
                           String value,
                           MusicFont musicFont,
                           Point location)
    {
        super(
                glyph,
                bounds,
                shape,
                grade,
                value,
                new FontInfo(
                        musicFont.computeSize(value, bounds.getSize()),
                        musicFont.getFontName()),
                location);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Called immediately before the marshalling of this object begins.
     * <p>
     * Transcribe value to a list of hexadecimal codes.
     */
    @Override
    @SuppressWarnings("unused")
    protected void beforeMarshal (Marshaller m)
    {
        super.beforeMarshal(m);

        codes = new ArrayList<>();
        value.codePoints().forEach(c -> codes.add("0x" + Integer.toHexString(c)));
        logger.debug("beforeMarshal. codes: {}", codes);
        value = null;
    }

    //--------------//
    // afterMarshal //
    //--------------//
    /**
     * Called immediately after marshalling of this object.
     * We reset any empty RunSequence to null.
     */
    @SuppressWarnings("unused")
    private void afterMarshal (Marshaller m)
    {
        logger.debug("afterMarshal. codes: {}", codes);
        value = computeValue(codes);
        codes = null;
    }

    //-----------------//
    // beforeUnmarshal //
    //-----------------//
    @SuppressWarnings("unused")
    private void beforeUnmarshal (Object target,
                                  Object parent)
    {
        logger.debug("beforeUnmarshal");
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        logger.debug("afterUnmarshal. codes: {}", codes);
        value = computeValue(codes);
        logger.debug("afterUnmarshal. value: {}", value);
    }

    //--------------//
    // computeValue //
    //--------------//
    /**
     * Compute the content value by decoding the sequence of characters codes.
     *
     * @param codes the list of character codes (hexadecimal strings)
     * @return the corresponding content value
     */
    private String computeValue (List<String> codes)
    {
        final int nb = codes.size();
        int[] ints = new int[nb];

        for (int i = 0; i < nb; i++) {
            ints[i] = Integer.decode(codes.get(i));
        }

        return new String(ints, 0, nb);
    }

    //------------//
    // getAdvance //
    //------------//
    @Override
    public int getAdvance ()
    {
        if (value.isEmpty()) {
            return 0;
        }

        // As opposed to a plain WordInter, we use MusicFont rather than TextFont
        final MusicFont font = new MusicFont(fontInfo);
        final TextLayout layout = font.layout(value);

        return (int) Math.rint(layout.getAdvance());
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        }

        if (value.isEmpty()) {
            return new Rectangle(
                    bounds = new Rectangle(
                            (int) Math.rint(location.getX()),
                            (int) Math.rint(location.getY()),
                            0,
                            0));
        }

        final MusicFont textFont = new MusicFont(fontInfo);
        final TextLayout layout = textFont.layout(value);
        final Rectangle2D rect = layout.getBounds();

        return new Rectangle(
                bounds = new Rectangle(
                        (int) Math.rint(location.getX()),
                        (int) Math.rint(location.getY() + rect.getY()),
                        (int) Math.rint(rect.getWidth()),
                        (int) Math.rint(rect.getHeight())));
    }

    //--------------//
    // getDimension //
    //--------------//
    @Override
    public Dimension getDimension ()
    {
        if (bounds != null) {
            return bounds.getSize();
        }

        if (value.isEmpty()) {
            return new Dimension(0, 0);
        }

        final MusicFont musicFont = new MusicFont(fontInfo);
        final TextLayout layout = musicFont.layout(value);
        final Rectangle2D rect = layout.getBounds();

        return new Dimension((int) Math.rint(rect.getWidth()), (int) Math.rint(rect.getHeight()));
    }

    //----------//
    // getValue //
    //----------//
    /**
     * Report the word (music) value, even if called in the middle of any [un]marshalling.
     *
     * @return the value
     */
    @Override
    public String getValue ()
    {
        final String theValue = value;

        if (theValue != null) {
            return theValue;
        }

        final List<String> theCodes = codes;

        if (theCodes != null) {
            return computeValue(theCodes);
        } else {
            return value;
        }
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public void setBounds (Rectangle bounds)
    {
        super.setBounds(bounds);

        if (fontInfo == null) {
            tryToSetFontInfo();
        }
    }

    //----------//
    // setStaff //
    //----------//
    @Override
    public void setStaff (Staff staff)
    {
        super.setStaff(staff);

        if (fontInfo == null) {
            tryToSetFontInfo();
        }
    }

    //------------------//
    // tryToSetFontInfo //
    //------------------//
    private void tryToSetFontInfo ()
    {
        if ((bounds != null) && (staff != null)) {
            final Sheet sheet = staff.getSystem().getSheet();
            final Scale scale = sheet.getScale();
            final MusicFamily family = sheet.getStub().getMusicFamily();
            final MusicFont musicFont = MusicFont.getBaseFont(family, scale.getInterline());
            fontInfo = new FontInfo(
                    musicFont.computeSize(value, bounds.getSize()),
                    musicFont.getFontName());
            location = musicFont.computeLocation(value, bounds);
        }
    }
}
