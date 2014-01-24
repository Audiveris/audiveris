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
import omr.sig.StemInter;

import omr.ui.symbol.Alignment;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import omr.util.Predicate;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.List;
import java.util.Set;

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

        // Properly scaled font (sufficiently enlarged)
        Scale scale = sheet.getScale();
        int symbolSize = scale.toPixels(constants.symbolSize);
        musicFont = MusicFont.getFont(symbolSize);

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
    public void erase (final Set<Shape> shapes)
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
    }

    @Override
    public void visit (LedgerInter inter)
    {
    }

    @Override
    public void visit (BarlineInter inter)
    {
    }

    @Override
    public void visit (BarConnectionInter inter)
    {
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
                1.0, //1.25,
                "Symbols size to use for eraser");
    }
}
