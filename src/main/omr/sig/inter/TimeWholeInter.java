//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   T i m e W h o l e I n t e r                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
        extends TimeInter
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
