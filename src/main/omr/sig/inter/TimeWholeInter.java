//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T i m e W h o l e I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig.inter;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import omr.glyph.ShapeSet;

import omr.score.TimeRational;
import omr.score.TimeValue;

import omr.sheet.Staff;

import omr.ui.symbol.MusicFont;
import omr.ui.symbol.NumDenSymbol;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code TimeWholeInter} is a time signature defined by a single symbol (either
 * COMMON or CUT or predefined combos).
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
     * @param shape precise shape (COMMON_TIME, CUT_TIME or predefined combos like TIME_FOUR_FOUR)
     * @param grade evaluation grade
     */
    public TimeWholeInter (Glyph glyph,
                           Shape shape,
                           double grade)
    {
        super(glyph, shape, grade);
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
        MusicFont musicFont = MusicFont.getFont(interline);
        Dimension dim = symbol.getDimension(musicFont);

        return new Rectangle(
                center.x - (dim.width / 2),
                center.y - (dim.height / 2),
                dim.width,
                dim.height);
    }

    //----------//
    // getValue //
    //----------//
    @Override
    public TimeValue getValue ()
    {
        return new TimeValue(shape);
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
