//----------------------------------------------------------------------------//
//                                                                            //
//                             P a g e E r a s e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.ConstantSet;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.math.GeoUtil;

import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.AbstractBeamInter;
import omr.sig.BarConnectionInter;
import omr.sig.BarlineInter;
import omr.sig.Inter;
import omr.sig.InterVisitor;
import omr.sig.LedgerInter;
import omr.sig.SIGraph;
import omr.sig.SlurInter;
import omr.sig.StemInter;

import omr.ui.symbol.Alignment;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import omr.util.Predicate;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.util.Collection;
import java.util.List;

/**
 * Class {@code PageEraser} erases selected shapes on the provided
 * graphics environment.
 *
 * @author Hervé Bitteur
 */
public class PageEraser
        extends AbstractScoreVisitor
        implements InterVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields --------------------------------------------------------
    // Graphic context
    private final Graphics2D g;

    // Related sheet
    private final Sheet sheet;

    // Specific font for music symbols
    private final MusicFont musicFont;

    // Vertical margin added above and below any staff DMZ
    private final int dmzDyMargin;

    // Original stroke
    private final Stroke defaultStroke;

    // Stroke for stems
    private final Stroke stemStroke;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new PageEraser object.
     *
     * @param g     graphics context
     * @param sheet related sheet
     */
    public PageEraser (Graphics2D g,
                       Sheet sheet)
    {
        this.g = g;
        this.sheet = sheet;

        // Properly scaled font
        Scale scale = sheet.getScale();
        int symbolSize = scale.toPixels(constants.symbolSize);
        musicFont = MusicFont.getFont(symbolSize);

        dmzDyMargin = scale.toPixels(constants.staffVerticalMargin);

        defaultStroke = g.getStroke();
        stemStroke = new BasicStroke(
                (float) scale.getMainStem(),
                BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND);

        g.setColor(Color.WHITE);

        // Anti-aliasing
        g.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    //~ Methods ----------------------------------------------------------------
    //-------//
    // erase //
    //-------//
    /**
     * Erase from image graphics all good instances of provided shapes.
     *
     * @param shapes the shapes to process
     */
    public void erase (final Collection<Shape> shapes)
    {
        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();

            final List<Inter> goodies = sig.inters(
                    new Predicate<Inter>()
                    {
                        @Override
                        public boolean check (Inter inter)
                        {
                            return !inter.isDeleted()
                                   && shapes.contains(inter.getShape())
                                   && inter.isGood();
                        }
                    });

            for (Inter inter : goodies) {
                inter.accept(this);
            }

            // Erase each staff DMZ
            for (StaffInfo staff : system.getStaves()) {
                eraseDmz(staff);
            }
        }
    }

    @Override
    public void visit (Inter inter)
    {
        ShapeSymbol symbol = Symbols.getSymbol(inter.getShape());
        Glyph glyph = inter.getGlyph();
        Point center = (glyph != null) ? glyph.getCentroid()
                : GeoUtil.centerOf(inter.getBounds());
        symbol.paintSymbol(g, musicFont, center, Alignment.AREA_CENTER);
    }

    @Override
    public void visit (AbstractBeamInter beam)
    {
        g.fill(beam.getArea());
    }

    @Override
    public void visit (StemInter inter)
    {
        g.setStroke(stemStroke);

        inter.getGlyph().renderLine(g);

        g.setStroke(defaultStroke);
    }

    @Override
    public void visit (LedgerInter inter)
    {
    }

    @Override
    public void visit (SlurInter inter)
    {
    }

    @Override
    public void visit (BarlineInter inter)
    {
        g.fill(inter.getArea());
    }

    @Override
    public void visit (BarConnectionInter inter)
    {
        g.fill(inter.getArea());
    }

    //----------//
    // eraseDmz //
    //----------//
    /**
     * Erase from image the DMZ part of a staff
     *
     * @param staff the staff to process
     */
    private void eraseDmz (StaffInfo staff)
    {
        int dmzEnd = staff.getDmzEnd();
        int top = staff.getFirstLine().yAt(dmzEnd) - dmzDyMargin;
        int bot = staff.getLastLine().yAt(dmzEnd) + dmzDyMargin;
        g.fillRect(0, top, dmzEnd, bot - top + 1);
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        final Scale.Fraction symbolSize = new Scale.Fraction(
                1.1,
                "Symbols size to use for eraser");

        final Scale.Fraction staffVerticalMargin = new Scale.Fraction(
                2.0,
                "Margin erased above & below staff DMZ area");
    }
}
