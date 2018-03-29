//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P a g e C l e a n e r                                     //
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
package org.audiveris.omr.sheet;

import ij.process.ByteProcessor;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractInterVisitor;
import org.audiveris.omr.sig.inter.BarConnectorInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.BracketConnectorInter;
import org.audiveris.omr.sig.inter.BracketInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.Symbols;

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

    /** Slightly thicker fonts for music symbols. */
    protected final MusicFont musicFont;

    protected final MusicFont headMusicFont;

    protected final MusicFont smallMusicFont;

    protected final MusicFont smallHeadMusicFont;

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

        // Use music fonts slightly larger (TODO: perhaps only for heads?)
        final int interline = scale.getInterline();
        int pointSize = MusicFont.getPointSize(interline);
        musicFont = MusicFont.getPointFont(dilated(pointSize), interline);

        int headPointSize = MusicFont.getHeadPointSize(scale, interline);
        headMusicFont = MusicFont.getPointFont(dilated(headPointSize), interline);

        final Integer smallInterline = scale.getSmallInterline();

        if (smallInterline != null) {
            smallMusicFont = MusicFont.getBaseFont(smallInterline);
            smallHeadMusicFont = MusicFont.getHeadFont(scale, smallInterline);
        } else {
            smallMusicFont = smallHeadMusicFont = null;
        }

        // Thickness of margins applied (around areas & lines)
        final float marginThickness = (float) scale.toPixelsDouble(constants.lineMargin);
        marginStroke = new BasicStroke(marginThickness, CAP_SQUARE, JOIN_MITER);

        float lineThickness = scale.getFore() + (2 * marginThickness);
        lineStroke = new BasicStroke(lineThickness, CAP_SQUARE, JOIN_MITER);

        // No anti-aliasing
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

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
    public void visit (AbstractChordInter inter)
    {
        for (Inter member : inter.getNotes()) {
            member.accept(this);
        }
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
    public void visit (HeadInter head)
    {
        ShapeSymbol symbol = Symbols.getSymbol(head.getShape());
        Glyph glyph = head.getGlyph();
        Point center = (glyph != null) ? glyph.getCenter() : GeoUtil.centerOf(
                head.getBounds());
        MusicFont font = head.getStaff().isSmall() ? smallHeadMusicFont : headMusicFont;
        symbol.paintSymbol(g, font, center, Alignment.AREA_CENTER);
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
        Point center = (glyph != null) ? glyph.getCenter() : GeoUtil.centerOf(
                inter.getBounds());
        boolean isSmall = (inter.getStaff() != null) && inter.getStaff().isSmall();
        MusicFont font = isSmall ? smallMusicFont : musicFont;
        symbol.paintSymbol(g, font, center, Alignment.AREA_CENTER);
    }

    @Override
    public void visit (KeyAlterInter alter)
    {
        // Process isolated keyAlter
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
    public void visit (TimePairInter pair)
    {
        for (Inter member : pair.getMembers()) {
            processGlyph(member.getGlyph());
        }
    }

    @Override
    public void visit (TimeWholeInter inter)
    {
        processGlyph(inter.getGlyph());
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
        // Process isolated word
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
            int dy = InterlineScale.toPixels(staff.getSpecificInterline(), yMargin);
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

    private int dilated (int pointSize)
    {
        return (int) Math.rint(pointSize * constants.dilationRatio.getValue());
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Ratio dilationRatio = new Constant.Ratio(
                1.1,
                "Size augmentation to use with eraser music font");

        private final Scale.Fraction symbolSize = new Scale.Fraction(
                1.1,
                "Symbols size to use with eraser music font");

        private final Scale.Fraction lineMargin = new Scale.Fraction(
                0.1,
                "Thickness of white lines drawn on items borders");
    }
}
