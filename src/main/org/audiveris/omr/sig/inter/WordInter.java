//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        W o r d I n t e r                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.text.TextWord;

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
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            WordInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Word text content. */
    @XmlAttribute
    protected final String value;

    /** Detected font attributes. */
    @XmlAttribute(name = "font")
    @XmlJavaTypeAdapter(FontInfo.Adapter.class)
    protected final FontInfo fontInfo;

    /** Precise word starting point. */
    @XmlElement
    protected final Point location;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code WordInter} object.
     *
     * @param textWord the OCR'ed text word
     */
    public WordInter (TextWord textWord)
    {
        super(
                textWord.getGlyph(),
                textWord.getBounds(),
                Shape.TEXT,
                textWord.getConfidence() * Inter.intrinsicRatio);
        value = textWord.getValue();
        fontInfo = textWord.getFontInfo();
        location = textWord.getLocation();
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    protected WordInter ()
    {
        super(null, null, null, null);
        this.value = null;
        this.fontInfo = null;
        this.location = null;
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

    //-------------//
    // getEnsemble //
    //-------------//
    /**
     * Report the sentence that contains this word.
     *
     * @return the containing sentence
     */
    @Override
    public SentenceInter getEnsemble ()
    {
        return (SentenceInter) ensemble;
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

    //-------------//
    // setEnsemble //
    //-------------//
    public void setEnsemble (SentenceInter sentence)
    {
        this.ensemble = sentence;
    }

    //-------------//
    // shapeString //
    //-------------//
    @Override
    public String shapeString ()
    {
        return "WORD_\"" + value + "\"";
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
