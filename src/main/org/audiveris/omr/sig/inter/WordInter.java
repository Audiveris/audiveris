//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        W o r d I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import org.audiveris.omr.glyph.Grades;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.TextWord;
import org.audiveris.omr.util.Jaxb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code WordInter} represents a text word.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "word")
public class WordInter
        extends AbstractInter
{

    private static final Logger logger = LoggerFactory.getLogger(WordInter.class);

    /** Word text content. */
    @XmlAttribute
    protected String value;

    /** Detected font attributes. */
    @XmlAttribute(name = "font")
    @XmlJavaTypeAdapter(FontInfo.Adapter.class)
    protected final FontInfo fontInfo;

    /** Precise word starting point. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.PointAdapter.class)
    protected Point location;

    /**
     * Creates a new {@code WordInter} object, with TEXT shape.
     *
     * @param textWord the OCR'ed text word
     */
    public WordInter (TextWord textWord)
    {
        this(textWord, Shape.TEXT);
    }

    /**
     * Creates a new {@code WordInter} object, with provided shape.
     *
     * @param textWord the OCR'ed text word
     * @param shape    specific shape (TEXT or LYRICS)
     */
    public WordInter (TextWord textWord,
                      Shape shape)
    {
        super(
                textWord.getGlyph(),
                textWord.getBounds(),
                shape,
                textWord.getConfidence() * Grades.intrinsicRatio);
        value = textWord.getValue();
        fontInfo = textWord.getFontInfo();
        location = textWord.getLocation();
    }

    /**
     * Creates a new {@code WordInter} object meant for manual assignment.
     *
     * @param grade inter grade
     */
    public WordInter (double grade)
    {
        super(null, null, Shape.TEXT, grade);

        this.value = "";
        this.fontInfo = null;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected WordInter ()
    {
        super(null, null, null, null);

        this.fontInfo = null;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------------//
    // getFontInfo //
    //-------------//
    /**
     * Report the related font attributes.
     *
     * @return the fontInfo
     */
    public FontInfo getFontInfo ()
    {
        return fontInfo;
    }

    //-------------//
    // getLocation //
    //-------------//
    /**
     * @return the location
     */
    public Point getLocation ()
    {
        return location;
    }

    //----------//
    // getValue //
    //----------//
    /**
     * @return the value
     */
    public String getValue ()
    {
        return value;
    }

    //----------//
    // setValue //
    //----------//
    /**
     * Assign a new text value.
     *
     * @param value the new value
     */
    public void setValue (String value)
    {
        this.value = value;
    }

    //----------//
    // setGlyph //
    //----------//
    @Override
    public void setGlyph (Glyph glyph)
    {
        super.setGlyph(glyph);

        // Location?
        // FontInfo?
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "WORD";
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        sb.append(" \"").append(value).append("\"");

        return sb.toString();
    }
}
