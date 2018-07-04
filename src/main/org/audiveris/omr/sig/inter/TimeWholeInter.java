//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T i m e W h o l e I n t e r                                  //
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
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.ShapeSet;
import org.audiveris.omr.score.TimeRational;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.NumDenSymbol;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code TimeWholeInter} is a time signature defined by a single symbol (either
 * COMMON or CUT or predefined combo like 6/8).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "time-whole")
public class TimeWholeInter
        extends AbstractTimeInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new {@code TimeWholeInter} object.
     *
     * @param glyph underlying glyph
     * @param shape precise shape (COMMON_TIME, CUT_TIME or predefined combo like TIME_FOUR_FOUR)
     * @param grade evaluation grade
     */
    public TimeWholeInter (Glyph glyph,
                           Shape shape,
                           double grade)
    {
        super(glyph, shape, grade);

        if (!ShapeSet.WholeTimes.contains(shape)) {
            throw new IllegalArgumentException(shape + " not allowed as TimeWholeInter shape");
        }
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private TimeWholeInter ()
    {
        super(null, null, 0);
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

    //--------//
    // create //
    //--------//
    /**
     * Create a TimeWholeInter.
     *
     * @param glyph underlying glyph
     * @param shape precise shape
     * @param grade evaluation value
     * @param staff related staff
     * @return the created instance or null if failed
     */
    public static TimeWholeInter create (Glyph glyph,
                                         Shape shape,
                                         double grade,
                                         Staff staff)
    {
        TimeWholeInter time = new TimeWholeInter(glyph, shape, grade);
        time.setStaff(staff);

        return time;
    }

    //-----------------//
    // getSymbolBounds //
    //-----------------//
    /**
     * {@inheritDoc}.
     * <p>
     * This implementation uses two rectangles, one for numerator and one for denominator.
     *
     * @param interline scaling factor
     * @return the symbol bounds
     */
    @Override
    public Rectangle getSymbolBounds (int interline)
    {
        // Single symbol
        if (ShapeSet.SingleWholeTimes.contains(shape)) {
            return super.getSymbolBounds(interline);
        }

        // Multi symbol (num / den), such as for shape TIME_FOUR_FOUR
        Point center = getCenter(); // Use area center
        TimeRational nd = getTimeRational();
        NumDenSymbol symbol = new NumDenSymbol(shape, nd.num, nd.den);
        MusicFont musicFont = MusicFont.getBaseFont(interline);
        Dimension dim = symbol.getDimension(musicFont);

        return new Rectangle(
                center.x - (dim.width / 2),
                center.y - (dim.height / 2),
                dim.width,
                dim.height);
    }

    //-----------//
    // replicate //
    //-----------//
    @Override
    public TimeWholeInter replicate (Staff targetStaff)
    {
        TimeWholeInter inter = new TimeWholeInter(null, shape, 0);
        inter.setStaff(targetStaff);

        return inter;
    }
}
