//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P a g e C l e a n e r                                     //
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

import omr.glyph.Glyph;

import omr.math.GeoUtil;

import omr.sig.inter.AbstractBeamInter;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.AbstractInterVisitor;
import omr.sig.inter.BarConnectorInter;
import omr.sig.inter.BarlineInter;
import omr.sig.inter.BraceInter;
import omr.sig.inter.BracketConnectorInter;
import omr.sig.inter.BracketInter;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.ClefInter;
import omr.sig.inter.EndingInter;
import omr.sig.inter.Inter;
import omr.sig.inter.KeyAlterInter;
import omr.sig.inter.KeyInter;
import omr.sig.inter.LedgerInter;
import omr.sig.inter.SentenceInter;
import omr.sig.inter.SlurInter;
import omr.sig.inter.StemInter;
import omr.sig.inter.TimePairInter;
import omr.sig.inter.TimeWholeInter;
import omr.sig.inter.WedgeInter;
import omr.sig.inter.WordInter;

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
import java.awt.geom.Area;

/**
 * Class {@code PageCleaner} erases selected inter instances on the provided graphics
 * environment by painting them using white background color.
 * <p>
 * Painting uses various techniques:<ul>
 * <li>Default is to use inter shape to paint the shape symbol with a thicker
 * {@link #musicFont}.</li>
 * <li>For an area-based inter, area is filled exactly and area contour is drawn with
 * {@link #marginStroke}.</li>
 * <li>For a glyph-based inter, all glyph runs are painted with no margin. </li>
 * <li>For a line-based inter, the line is drawn with a thicker {@link #lineStroke}. </li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class PageCleaner
        extends AbstractInterVisitor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(PageCleaner.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Sheet buffer. */
    protected final ByteProcessor buffer;

    /** Graphic context. */
    protected final Graphics2D g;

    /** Related sheet. */
    protected final Sheet sheet;

    /** Slightly thicker font for music symbols. */
    protected final MusicFont musicFont;

    /** Stroke for margin around areas. */
    private final Stroke marginStroke;

    /** Slightly thicker stroke for lines. (endings, wedges, slurs) */
    private final Stroke lineStroke;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code PageCleaner} object.
     *
     * @param buffer sheet buffer
     * @param g      graphics context on buffer
     * @param sheet  related sheet
     */
    public PageCleaner (ByteProcessor buffer,
                        Graphics2D g,
                        Sheet sheet)
    {
        this.buffer = buffer;
        this.g = g;
        this.sheet = sheet;

        Scale scale = sheet.getScale();

        // Use a music font slightly larger
        int symbolSize = scale.toPixels(constants.symbolSize);
        musicFont = MusicFont.getFont(symbolSize);

        // Thickness of margins applied (around areas & lines)
        final float marginThickness = (float) scale.toPixelsDouble(constants.lineMargin);
        marginStroke = new BasicStroke(marginThickness, CAP_SQUARE, JOIN_MITER);

        float lineThickness = scale.getMainFore() + (2 * marginThickness);
        lineStroke = new BasicStroke(lineThickness, CAP_SQUARE, JOIN_MITER);

        // No anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        //        g.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
        //        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        //        g.setRenderingHint(
        //                RenderingHints.KEY_ALPHA_INTERPOLATION,
        //                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        //        g.setRenderingHint(
        //                RenderingHints.KEY_COLOR_RENDERING,
        //                RenderingHints.VALUE_COLOR_RENDER_SPEED);
        //        g.setRenderingHint(
        //                RenderingHints.KEY_FRACTIONALMETRICS,
        //                RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        //        g.setRenderingHint(
        //                RenderingHints.KEY_INTERPOLATION,
        //                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        //        g.setRenderingHint(
        //                RenderingHints.KEY_TEXT_ANTIALIASING,
        //                RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        //        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        //        g.setRenderingHint(
        //                RenderingHints.KEY_STROKE_CONTROL,
        //                RenderingHints.VALUE_STROKE_NORMALIZE);
        //
        // Paint in background color
        g.setColor(Color.WHITE);
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public void visit (AbstractBeamInter inter)
    {
        processArea(inter.getArea());
    }

    @Override
    public void visit (AbstractHeadInter inter)
    {
        visit((Inter) inter);
    }

    @Override
    public void visit (BarConnectorInter inter)
    {
        processArea(inter.getArea());
    }

    @Override
    public void visit (BarlineInter inter)
    {
        processArea(inter.getArea());
    }

    @Override
    public void visit (BraceInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (BracketConnectorInter inter)
    {
        processArea(inter.getArea());
    }

    @Override
    public void visit (BracketInter inter)
    {
        processArea(inter.getArea());
    }

    @Override
    public void visit (ClefInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (AbstractChordInter inter)
    {
        for (Inter member : inter.getNotes()) {
            member.accept(this);
        }
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
        if (alter.getEnsemble() == null) {
            processGlyph(alter.getGlyph());
        }
    }

    @Override
    public void visit (KeyInter key)
    {
        for (Inter member : key.getMembers()) {
            KeyAlterInter alter = (KeyAlterInter) member;
            processGlyph(alter.getGlyph());
        }
    }

    @Override
    public void visit (LedgerInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (SentenceInter inter)
    {
        for (Inter member : inter.getMembers()) {
            processGlyph(member.getGlyph());
        }
    }

    @Override
    public void visit (SlurInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (StemInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (TimeWholeInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (TimePairInter pair)
    {
        for (Inter member : pair.getMembers()) {
            processGlyph(member.getGlyph());
        }
    }

    @Override
    public void visit (WedgeInter wedge)
    {
        g.setStroke(lineStroke);
        g.draw(wedge.getLine1());
        g.draw(wedge.getLine2());
    }

    @Override
    public void visit (WordInter word)
    {
        if (word.getEnsemble() == null) {
            processGlyph(word.getGlyph());
        }
    }

    //---------//
    // canHide //
    //---------//
    /**
     * Check if we can safely hide the inter.
     * TODO: Quick hack, to be better implemented.
     *
     * @param inter the inter to check
     * @return true if we can safely hide the inter
     */
    protected boolean canHide (Inter inter)
    {
        return inter.isContextuallyGood();
    }

    //-------------------//
    // eraseStavesHeader //
    //-------------------//
    /**
     * Erase from image the staff header part for each staff of a system.
     * There is one erased area per staff with an abscissa range from staff left abscissa to header
     * end.
     *
     * @param system  the system to process
     * @param yMargin the vertical margin erased above and below each staff
     */
    protected void eraseStavesHeader (SystemInfo system,
                                      Scale.Fraction yMargin)
    {
        for (Staff staff : system.getStaves()) {
            int dy = staff.getSpecificScale().toPixels(yMargin);
            int left = staff.getHeaderStart();
            int right = staff.getHeaderStop();
            int top = staff.getFirstLine().yAt(right) - dy;
            int bot = staff.getLastLine().yAt(right) + dy;
            g.fillRect(left, top, right - left + 1, bot - top + 1);
        }
    }

    //-------------------//
    // eraseSystemHeader //
    //-------------------//
    /**
     * Erase from image the header part of a system.
     * The erased area is a single rectangle for the whole system, with an abscissa range from
     * system left bound to header end.
     *
     * @param system  the system to process
     * @param yMargin the vertical margin erased above first staff and below last staff
     */
    protected void eraseSystemHeader (SystemInfo system,
                                      Scale.Fraction yMargin)
    {
        int dy = sheet.getScale().toPixels(yMargin);
        Staff firstStaff = system.getFirstStaff();
        Staff lastStaff = system.getLastStaff();
        int dmzEnd = firstStaff.getHeaderStop();
        int top = firstStaff.getFirstLine().yAt(dmzEnd) - dy;
        int bot = lastStaff.getLastLine().yAt(dmzEnd) + dy;
        g.fillRect(system.getBounds().x, top, dmzEnd, bot - top + 1);
    }

    //-------------//
    // processArea //
    //-------------//
    /**
     * Process area-based inter.
     * Strategy is to fill the area with white and also draw its contour in white with a specific
     * stroke.
     *
     * @param area the inter underlying area
     */
    protected void processArea (Area area)
    {
        // Erase area
        g.fill(area);

        // Erase contour of area
        g.setStroke(marginStroke);
        g.draw(area);
    }

    //--------------//
    // processGlyph //
    //--------------//
    /**
     * Process glyph-based inter.
     * Strategy is to paint the glyph (its runTable actually) in white.
     *
     * @param glyph the inter underlying glyph
     */
    protected void processGlyph (Glyph glyph)
    {
        // Use pixels of underlying glyph
        Color oldColor = g.getColor();
        g.setColor(Color.WHITE);
        glyph.getRunTable().render(g, glyph.getTopLeft());
        g.setColor(oldColor);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Scale.Fraction symbolSize = new Scale.Fraction(
                1.1,
                "Symbols size to use with eraser music font");

        private final Scale.Fraction lineMargin = new Scale.Fraction(
                0.1,
                "Thickness of white lines drawn on items borders");
    }
}
