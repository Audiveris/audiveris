//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       P a g e E r a s e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.constant.ConstantSet;

import omr.glyph.facets.Glyph;

import omr.grid.StaffInfo;

import omr.lag.Section;

import omr.math.GeoUtil;

import omr.score.visitor.AbstractScoreVisitor;

import omr.sig.AbstractBeamInter;
import omr.sig.AbstractNoteInter;
import omr.sig.BarConnectionInter;
import omr.sig.BarlineInter;
import omr.sig.BraceInter;
import omr.sig.ClefInter;
import omr.sig.EndingInter;
import omr.sig.Inter;
import omr.sig.InterVisitor;
import omr.sig.KeyAlterInter;
import omr.sig.LedgerInter;
import omr.sig.SlurInter;
import omr.sig.StemInter;
import omr.sig.TimeInter;
import omr.sig.WedgeInter;

import omr.ui.symbol.Alignment;
import omr.ui.symbol.MusicFont;
import omr.ui.symbol.ShapeSymbol;
import omr.ui.symbol.Symbols;

import ij.process.ByteProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;

import static java.awt.BasicStroke.*;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import omr.sig.BracketConnectionInter;
import omr.sig.BracketInter;

/**
 * Class {@code PageEraser} erases selected shapes on the provided graphics environment.
 *
 * @author Hervé Bitteur
 */
public abstract class PageEraser
        extends AbstractScoreVisitor
        implements InterVisitor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(PageEraser.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Page buffer. */
    protected final ByteProcessor buffer;

    // Graphic context
    protected final Graphics2D g;

    // Related sheet
    protected final Sheet sheet;

    // Specific font for music symbols
    protected final MusicFont musicFont;

    private final float marginThickness;

    // Stroke for margins
    private final Stroke marginStroke;

    // Stroke for lines (endings, wedges, slurs)
    private final Stroke lineStroke;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new PageEraser object.
     *
     * @param buffer page buffer
     * @param g      graphics context on buffer
     * @param sheet  related sheet
     */
    public PageEraser (ByteProcessor buffer,
                       Graphics2D g,
                       Sheet sheet)
    {
        this.buffer = buffer;
        this.g = g;
        this.sheet = sheet;

        Scale scale = sheet.getScale();

        int symbolSize = scale.toPixels(constants.symbolSize);
        musicFont = MusicFont.getFont(symbolSize);

        marginThickness = (float) scale.toPixelsDouble(constants.lineMargin);
        marginStroke = new BasicStroke(marginThickness, CAP_SQUARE, JOIN_MITER);

        float lineThickness = scale.getMainFore() + (2 * marginThickness);
        lineStroke = new BasicStroke(lineThickness, CAP_SQUARE, JOIN_MITER);

        g.setColor(Color.WHITE);

        // Anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Default strategy for a basic inter is to paint in white the related symbol.
     *
     * @param inter the basic inter to erase
     */
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
    public void visit (KeyAlterInter alter)
    {
        processGlyph(alter.getGlyph());
    }

    @Override
    public void visit (TimeInter time)
    {
        processGlyph(time.getGlyph());
    }

    @Override
    public void visit (ClefInter clef)
    {
        processGlyph(clef.getGlyph());
    }

    @Override
    public void visit (AbstractBeamInter beam)
    {
        g.fill(beam.getArea());

        g.setStroke(marginStroke);
        g.draw(beam.getArea());
    }

    @Override
    public void visit (AbstractNoteInter inter)
    {
        // Use plain symbol painting to symply erase the note
        visit((Inter) inter);
    }

    @Override
    public void visit (StemInter stem)
    {
        processGlyph(stem.getGlyph());
    }

    @Override
    public void visit (LedgerInter ledger)
    {
        processGlyph(ledger.getGlyph());
    }

    @Override
    public void visit (SlurInter slur)
    {
        //        CubicCurve2D curve = slur.getInfo().getCurve();
        //
        //        if (curve != null) {
        //            g.setStroke(lineStroke);
        //            g.draw(curve);
        //        }
        processGlyph(slur.getGlyph());
    }

    @Override
    public void visit (BarlineInter inter)
    {
        g.fill(inter.getArea());
        g.setStroke(marginStroke);
        g.draw(inter.getArea());
    }

    @Override
    public void visit (BracketInter inter)
    {
        g.fill(inter.getArea());
        g.setStroke(marginStroke);
        g.draw(inter.getArea());
    }

    @Override
    public void visit (BarConnectionInter inter)
    {
        g.fill(inter.getArea());
        g.setStroke(marginStroke);
        g.draw(inter.getArea());
    }

    @Override
    public void visit (BracketConnectionInter inter)
    {
        g.fill(inter.getArea());
        g.setStroke(marginStroke);
        g.draw(inter.getArea());
    }

    @Override
    public void visit (WedgeInter wedge)
    {
        g.setStroke(lineStroke);
        g.draw(wedge.getLine1());
        g.draw(wedge.getLine2());
    }

    @Override
    public void visit (EndingInter ending)
    {
        g.setStroke(lineStroke);
        g.draw(ending.getLine());

        if (ending.getLeftLeg() != null) {
            g.draw(ending.getLeftLeg());
        }

        if (ending.getRightLeg() != null) {
            g.draw(ending.getRightLeg());
        }
    }

    @Override
    public void visit (BraceInter inter)
    {
        //void
    }

    //---------//
    // canHide //
    //---------//
    /**
     * Check if we can safely hide the inter (which cannot be mistaken for a symbol).
     * TODO: Quick hack, to be better implemented.
     *
     * @param inter the inter to check
     * @return true if we can safely hide the inter
     */
    protected boolean canHide (Inter inter)
    {
        return inter.isGood();
    }

    //----------------//
    // eraseStavesDmz //
    //----------------//
    /**
     * Erase from image the DMZ part for each staff of a system.
     * There is one erased area per staff, with an abscissa range from
     * staff left abscissa to DMZ end.
     *
     * @param system  the system to process
     * @param yMargin the vertical margin erased above and below each staff
     */
    protected void eraseStavesDmz (SystemInfo system,
                                   Scale.Fraction yMargin)
    {
        for (StaffInfo staff : system.getStaves()) {
            int dy = staff.getSpecificScale().toPixels(yMargin);
            int left = staff.getDmzStart();
            int right = staff.getDmzStop();
            int top = staff.getFirstLine().yAt(right) - dy;
            int bot = staff.getLastLine().yAt(right) + dy;
            g.fillRect(left, top, right - left + 1, bot - top + 1);
        }
    }

    //----------------//
    // eraseSystemDmz //
    //----------------//
    /**
     * Erase from image the DMZ part of a system.
     * The erased area is a single rectangle for the whole system, with an abscissa range from
     * system left bound to DMZ end.
     *
     * @param system  the system to process
     * @param yMargin the vertical margin erased above first staff and below last staff
     */
    protected void eraseSystemDmz (SystemInfo system,
                                   Scale.Fraction yMargin)
    {
        int dy = sheet.getScale().toPixels(yMargin);
        StaffInfo firstStaff = system.getFirstStaff();
        StaffInfo lastStaff = system.getLastStaff();
        int dmzEnd = firstStaff.getDmzStop();
        int top = firstStaff.getFirstLine().yAt(dmzEnd) - dy;
        int bot = lastStaff.getLastLine().yAt(dmzEnd) + dy;
        g.fillRect(system.getBounds().x, top, dmzEnd, bot - top + 1);
    }

    //--------------//
    // processGlyph //
    //--------------//
    /**
     * Process glyph-based inter
     *
     * @param glyph the inter underlying glyph
     */
    protected void processGlyph (Glyph glyph)
    {
        // Use pixels of underlying glyph
        for (Section section : glyph.getMembers()) {
            section.render(g, false, Color.WHITE);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Scale.Fraction symbolSize = new Scale.Fraction(
                1.1, //1.2, // 1.0,
                "Symbols size to use for eraser");

        final Scale.Fraction lineMargin = new Scale.Fraction(
                0.1,
                "Thickness of white lines drawn on items borders");
    }
}
