//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  T i m e C u s t o m I n t e r                                 //
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.NumDenSymbol;
import org.audiveris.omr.ui.symbol.ShapeSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import org.audiveris.omr.glyph.Shape;

/**
 * Class {@code TimeCustomInter} is a user-populated time signature.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "time-custom")
@XmlAccessorType(XmlAccessType.NONE)
public class TimeCustomInter
        extends AbstractTimeInter
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(TimeCustomInter.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Numerator value, perhaps zero. */
    @XmlAttribute
    private int num;

    /** Denominator value, perhaps zero. */
    @XmlAttribute
    private int den;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code TimeCustomInter} object.
     *
     * @param num   numerator value, perhaps zero
     * @param den   denominator value, perhaps zero
     * @param grade quality
     */
    public TimeCustomInter (int num,
                            int den,
                            double grade)
    {
        super(null, Shape.CUSTOM_TIME, grade);

        this.num = num;
        this.den = den;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private TimeCustomInter ()
    {
        super(null, Shape.CUSTOM_TIME, 0);
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

    //----------------//
    // getDenominator //
    //----------------//
    @Override
    public int getDenominator ()
    {
        return den;
    }

    //----------------//
    // setDenominator //
    //----------------//
    public void setDenominator (int den)
    {
        this.den = den;
        timeRational = null;
    }

    //--------------//
    // getNumerator //
    //--------------//
    @Override
    public int getNumerator ()
    {
        return num;
    }

    //--------------//
    // setNumerator //
    //--------------//
    public void setNumerator (int num)
    {
        this.num = num;
        timeRational = null;
    }

    //----------------//
    // getShapeString //
    //----------------//
    @Override
    public String getShapeString ()
    {
        return "TIME_SIG:" + getTimeRational();
    }

    //----------------//
    // getShapeSymbol //
    //----------------//
    @Override
    public ShapeSymbol getShapeSymbol ()
    {
        return new NumDenSymbol(shape, ShapeSymbol.numberCodes(num), ShapeSymbol.numberCodes(den));
    }

    //-----------------//
    // getSymbolBounds //
    //-----------------//
    @Override
    public Rectangle getSymbolBounds (int interline)
    {
        // Multi symbol (num / den)
        Point center = getCenter(); // Use area center
        NumDenSymbol symbol = new NumDenSymbol(
                shape, ShapeSymbol.numberCodes(num), ShapeSymbol.numberCodes(den));
        MusicFont musicFont = MusicFont.getBaseFont(interline);
        Dimension dim = symbol.getDimension(musicFont);

        return new Rectangle(
                center.x - (dim.width / 2),
                center.y - (dim.height / 2),
                dim.width,
                dim.height);
    }

    //-----------------//
    // getTimeRational //
    //-----------------//
    /**
     * Report the timeRational value, lazily computed.
     *
     * @return the timeRational
     */
    @Override
    public TimeRational getTimeRational ()
    {
        if (timeRational == null) {
            timeRational = new TimeRational(num, den);
        }

        return timeRational;
    }

    //-----------------//
    // invalidateCache //
    //-----------------//
    @Override
    public void invalidateCache ()
    {
        bounds = null;
        timeRational = null;
    }

    //-----------//
    // replicate //
    //-----------//
    @Override
    public AbstractTimeInter replicate (Staff targetStaff)
    {
        final TimeCustomInter inter = new TimeCustomInter(num, den, 0);
        inter.setStaff(targetStaff);

        return inter;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
}
