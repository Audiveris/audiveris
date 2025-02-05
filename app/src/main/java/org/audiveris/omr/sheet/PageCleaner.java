//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      P a g e C l e a n e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sheet.Scale.InterlineScale;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.AbstractInterVisitor;
import org.audiveris.omr.sig.inter.AbstractNumberInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.BarConnectorInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeatUnitInter;
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
import org.audiveris.omr.sig.inter.MultipleRestInter;
import org.audiveris.omr.sig.inter.OctaveShiftInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeCustomInter;
import org.audiveris.omr.sig.inter.TimePairInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.VerticalSerifInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.FontSymbol;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.OmrFont;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.process.ByteProcessor;

import java.awt.BasicStroke;
import static java.awt.BasicStroke.CAP_SQUARE;
import static java.awt.BasicStroke.JOIN_MITER;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Area;
import java.awt.geom.Point2D;
import java.util.List;

/**
 * Class <code>PageCleaner</code> erases selected inter instances on the provided graphics
 * environment by painting them using white background color.
 * <p>
 * Painting uses various techniques:
 * <ul>
 * <li>Default is to use inter shape to paint the shape symbol with a thicker MusicFont.</li>
 * <li>For an area-based inter, area is filled exactly and area contour is drawn with
 * {@link #marginStroke}.</li>
 * <li>For a glyph-based inter, all glyph runs are painted with no margin.</li>
 * <li>For a line-based inter, the line is drawn with a thicker {@link #lineStroke}.</li>
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

    /** Stroke for margin around areas. */
    private final Stroke marginStroke;

    /** Slightly thicker stroke for lines. (endings, wedges, slurs) */
    private final Stroke lineStroke;

    /** Preferred font family for sheet at hand. */
    private final MusicFamily family;

    /** Slightly enlarged font point size. */
    private final int enlargedSize;

    /** Slightly enlarged font point size for heads. */
    private final int enlargedHeadSize;

    /** Font point size on small staves, if any. */
    private final int smallPointSize;

    /** Font point size for small heads, if any. */
    private final int smallHeadSize;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>PageCleaner</code> object.
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

        final Scale scale = sheet.getScale();
        family = sheet.getStub().getMusicFamily();

        // NOTA: To clean items from buffer, we use slightly dilated music fonts
        final int interline = scale.getInterline();
        enlargedSize = enlarged(MusicFont.getPointSize(interline));
        enlargedHeadSize = enlarged(MusicFont.getHeadPointSize(scale, interline));

        // Item size for small staves
        final Integer smallInterline = scale.getSmallInterline();
        smallPointSize = (smallInterline != null) ? MusicFont.getPointSize(smallInterline) : 0;

        // Size for small heads (regardless of staff size)
        smallHeadSize = MusicFont.getHeadPointSize(scale, interline * OmrFont.RATIO_SMALL);

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
        return inter.isFrozen() || inter.isContextuallyGood();
    }

    //----------//
    // enlarged //
    //----------//
    /**
     * Report a slightly enlarged value of provided point size
     *
     * @param pointSize the provided point size
     * @return slightly increased point size value
     */
    private int enlarged (int pointSize)
    {
        return (int) Math.rint(pointSize * constants.enlargementRatio.getValue());
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
            if (staff.isTablature()) {
                continue;
            }

            final int dy = InterlineScale.toPixels(staff.getSpecificInterline(), yMargin);
            final int left = staff.getHeaderStart();
            final int right = staff.getHeaderStop();
            final int top = staff.getFirstLine().yAt(right) - dy;
            final int bot = staff.getLastLine().yAt(right) + dy;
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
        final int dy = sheet.getScale().toPixels(yMargin);
        final Staff firstStaff = system.getFirstStaff();
        final Staff lastStaff = system.getLastStaff();

        // Determine abscissa at end of header(s)
        // Mind the fact that we may encounter a tablature, thus without header
        for (Staff staff : system.getStaves()) {
            if (staff.getHeader() != null) {
                final int dmzEnd = staff.getHeaderStop();
                final int top = firstStaff.getFirstLine().yAt(dmzEnd) - dy;
                final int bot = lastStaff.getLastLine().yAt(dmzEnd) + dy;
                g.fillRect(system.getBounds().x, top, dmzEnd, bot - top + 1);

                return;
            }
        }
    }

    //-----------------//
    // eraseTablatures //
    //-----------------//
    /**
     * Erase tablatures areas in sheet.
     *
     * @param yMargin the vertical margin erased above and below each tablature
     */
    public void eraseTablatures (Scale.Fraction yMargin)
    {
        sheet.getSystems().forEach(system -> eraseTablatures(system, yMargin));
    }

    //-----------------//
    // eraseTablatures //
    //-----------------//
    /**
     * Erase tablatures areas.
     *
     * @param system  the system to process
     * @param yMargin the vertical margin erased above and below each tablature
     */
    protected void eraseTablatures (SystemInfo system,
                                    Scale.Fraction yMargin)
    {
        for (Staff staff : system.getStaves()) {
            if (staff.isTablature()) {
                final int dy = InterlineScale.toPixels(staff.getSpecificInterline(), yMargin);
                final Area core = StaffManager.getCoreArea(staff, 0, dy);
                g.fill(core);
            }
        }
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
        if (glyph != null) {
            // Use pixels of underlying glyph
            glyph.getRunTable().render(g, glyph.getTopLeft());
        }
    }

    @Override
    public void visit (AbstractBeamInter inter)
    {
        processArea(inter.getArea());
    }

    @Override
    public void visit (AbstractChordInter inter)
    {
        inter.getNotes().forEach(note -> note.accept(this));
    }

    @Override
    public void visit (AbstractFlagInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (AbstractNumberInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (ArpeggiatoInter inter)
    {
        processGlyph(inter.getGlyph());
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

    // No BeamGroup

    @Override
    public void visit (BeatUnitInter inter)
    {
        processGlyph(inter.getGlyph());
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
        // We now check small size for head directly (regardless of the containing staff size)
        final int size = head.getShape().isSmallHead() ? smallHeadSize : enlargedHeadSize;
        final FontSymbol fs = head.getShape().getFontSymbolBySize(family, size);

        if (fs.symbol == null) {
            logger.warn("No symbol for head {}", head);
            return;
        }

        final Glyph glyph = head.getGlyph();
        final Point2D center = (glyph != null) ? glyph.getCenter2D() : head.getCenter2D();
        fs.symbol.paintSymbol(g, fs.font, center, Alignment.AREA_CENTER);
    }

    /**
     * Default strategy for a basic inter is to paint the related symbol.
     *
     * @param inter the basic inter to erase
     */
    @Override
    public void visit (Inter inter)
    {
        if (inter.isImplicit()) {
            return;
        }

        final boolean isSmall = (inter.getStaff() != null) && inter.getStaff().isSmall();
        final int size = isSmall ? smallPointSize : enlargedSize;
        final FontSymbol fs = inter.getShape().getFontSymbolBySize(family, size);

        if (fs.symbol == null) {
            logger.warn("No symbol for inter {}", inter);
            return;
        }

        final Glyph glyph = inter.getGlyph();
        final Point2D center = (glyph != null) ? glyph.getCenter2D() : inter.getCenter2D();
        fs.symbol.paintSymbol(g, fs.font, center, Alignment.AREA_CENTER);
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
        key.getMembers().forEach(member -> processGlyph(((KeyAlterInter) member).getGlyph()));
    }

    @Override
    public void visit (LedgerInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (MultipleRestInter inter)
    {
        if (inter.getGlyph() != null) {
            processGlyph(inter.getGlyph());
        } else {
            processArea(inter.getArea());
        }
    }

    @Override
    public void visit (OctaveShiftInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (SentenceInter inter)
    {
        inter.getMembers().forEach(member -> processGlyph(member.getGlyph()));
    }

    @Override
    public void visit (SlurInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (StaffBarlineInter inter)
    {
        final List<Inter> members = inter.getMembers();

        if (!members.isEmpty()) {
            members.forEach(member -> member.accept(this));
        } else if (inter.getShape() != null) {
            visit((Inter) inter);
        }
    }

    @Override
    public void visit (StemInter inter)
    {
        if (inter.getGlyph() != null) {
            processGlyph(inter.getGlyph());
        } else {
            processArea(new Area(inter.getBounds()));
        }
    }

    @Override
    public void visit (TimeCustomInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (TimePairInter pair)
    {
        pair.getMembers().forEach(member -> processGlyph(member.getGlyph()));
    }

    @Override
    public void visit (TimeWholeInter inter)
    {
        processGlyph(inter.getGlyph());
    }

    @Override
    public void visit (VerticalSerifInter inter)
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
        processGlyph(word.getGlyph());
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Ratio enlargementRatio =
                new Constant.Ratio(1.1, "Size augmentation to use with eraser music font");

        private final Scale.Fraction lineMargin =
                new Scale.Fraction(0.1, "Thickness of white lines drawn on items borders");
    }
}
