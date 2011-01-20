//----------------------------------------------------------------------------//
//                                                                            //
//                   P a g e P h y s i c a l P a i n t e r                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.facets.Glyph;
import omr.glyph.facets.Stick;

import omr.log.Logger;

import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Barline;
import omr.score.entity.Chord;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.Page;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;

import omr.sheet.Ending;
import omr.sheet.Ledger;
import omr.sheet.Sheet;
import omr.sheet.StaffInfo;
import omr.sheet.SystemInfo;

import omr.ui.util.UIUtilities;

import java.awt.*;
import java.util.ConcurrentModificationException;

/**
 * Class <code>PagePhysicalPainter</code> paints the recognized page
 * entities at the location of their image counterpart, so that discrepancies
 * between them can be easily seen.
 *
 * <p>TODO:
 * - Paint breath marks
 *
 * @author Hervé Bitteur
 */
public class PagePhysicalPainter
    extends PagePainter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        PagePhysicalPainter.class);

    //~ Instance fields --------------------------------------------------------

    /** Color for slot axis */
    private final Color slotColor = new Color(
        192,
        192,
        192,
        constants.slotAlpha.getValue());

    //~ Constructors -----------------------------------------------------------

    //---------------------//
    // PagePhysicalPainter //
    //---------------------//
    /**
     * Creates a new PagePhysicalPainter object.
     *
     * @param graphics Graphic context
     * @param color the color to be used for foreground
     * @param annotated true if annotations are to be drawn
     */
    public PagePhysicalPainter (Graphics graphics,
                                Color    color,
                                boolean  annotated)
    {
        super(graphics, annotated);

        // Use a specific color for all score entities
        g.setColor(color);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // drawSlot //
    //----------//
    /**
     * Draw a time slot in the score display.
     *
     * @param wholeSystem if true, the slot will embrace the whole system,
     * otherwise only the part is embraced
     * @param measure the containing measure
     * @param slot the slot to draw
     * @param color the color to use in drawing
     */
    public void drawSlot (boolean wholeSystem,
                          Measure measure,
                          Slot    slot,
                          Color   color)
    {
        final Color oldColor = g.getColor();
        g.setColor(color);

        final Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1);
        final int    x = slot.getX();

        if (wholeSystem) {
            // Draw for the whole system height
            system = measure.getSystem();

            final PixelDimension systemDimension = system.getDimension();
            g.drawLine(
                x,
                system.getTopLeft().y,
                x,
                system.getTopLeft().y + systemDimension.height +
                system.getLastPart().getLastStaff().getHeight());
        } else {
            // Draw for just the part height
            SystemPart part = measure.getPart();
            Staff      firstStaff = part.getFirstStaff();
            Staff      lastStaff = part.getLastStaff();
            g.drawLine(
                x,
                firstStaff.getTopLeft().y,
                x,
                lastStaff.getTopLeft().y + lastStaff.getHeight());
        }

        g.setStroke(oldStroke);
        g.setColor(oldColor);
    }

    //---------------//
    // visit Barline //
    //---------------//
    @Override
    public boolean visit (Barline barline)
    {
        try {
            //barline.render(g);
            Stroke oldStroke = g.getStroke();

            for (Glyph glyph : barline.getGlyphs()) {
                Shape shape = glyph.getShape();

                if (glyph.isBar()) {
                    Stick stick = (Stick) glyph;
                    float thickness = (float) stick.getWeight() / stick.getLength();
                    g.setStroke(new BasicStroke(thickness));
                    stick.renderLine(g);
                } else if ((shape == REPEAT_DOTS) || (shape == DOT)) {
                    paint(layout(DOT), location(glyph.getCentroid()));
                } else {
                    // ???
                }
            }

            g.setStroke(oldStroke);
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + barline,
                ex);
        }

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        try {
            // Super: check, flags
            if (!super.visit(chord)) {
                return false;
            }

            // Draw the stem (physical)
            if (chord.getStem() != null) {
                Stroke oldStroke = g.getStroke();
                g.setStroke(stemStroke);

                final PixelPoint tail = chord.getTailLocation();
                final PixelPoint head = new PixelPoint(chord.getHeadLocation());

                // Slightly correct the ordinate on head side
                final int dyFix = scale.interline() / 4;

                if (tail.y < head.y) {
                    // Stem up
                    head.y -= dyFix;
                } else {
                    // Stem down
                    head.y += dyFix;
                }

                g.drawLine(head.x, head.y, tail.x, tail.y);
                g.setStroke(oldStroke);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + chord,
                ex);
        }

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        if (annotated) {
            final SystemPart part = measure.getPart();
            final Color      oldColor = g.getColor();

            if (!measure.isDummy()) {
                // Write the score-based measure id, on first real part only
                if (part == measure.getSystem()
                                   .getFirstRealPart()) {
                    g.setColor(Color.lightGray);
                    g.drawString(
                        measure.getScoreId(pageMeasureIdOffset),
                        measure.getLeftX() - 5,
                        measure.getPart().getFirstStaff().getTopLeft().y - 15);
                }

                // Draw slot vertical lines ?
                if (PaintingParameters.getInstance()
                                      .isSlotPainting() &&
                    (measure.getSlots() != null)) {
                    for (Slot slot : measure.getSlots()) {
                        drawSlot(false, measure, slot, slotColor);
                    }
                }

                //            // Flag for measure excess duration?
                //            if (measure.getExcess() != null) {
                //                g.setColor(Color.red);
                //                g.drawString(
                //                    "Excess " + Note.quarterValueOf(measure.getExcess()),
                //                    measure.getLeftX() + 10,
                //                    measure.getPart().getFirstStaff().getTopLeft().y - 15);
                //            }
            }

            g.setColor(oldColor);
        }

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (Note note)
    {
        try {
            // Paint note head and accidentals
            super.visit(note);

            // Augmentation dots ?
            if (note.getFirstDot() != null) {
                paint(
                    layout(DOT),
                    location(note.getFirstDot().getAreaCenter()));
            }

            if (note.getSecondDot() != null) {
                paint(
                    layout(DOT),
                    location(note.getSecondDot().getAreaCenter()));
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + note,
                ex);
        }

        return true;
    }

    //------------//
    // visit Page //
    //------------//
    @Override
    public boolean visit (Page page)
    {
        try {
            score = page.getScore();
            scale = page.getScale();

            final Sheet sheet = page.getSheet();

            if ((sheet == null) || (scale == null)) {
                return false;
            }

            // Set all painting parameters
            initParameters();

            // Determine beams parameters
            if (page.getBeamThickness() != null) {
                beamThickness = page.getBeamThickness();
                beamHalfThickness = beamThickness / 2;
            }

            // Determine offset of measure ids for this page
            pageMeasureIdOffset = score.getMeasureIdOffset(page);

            if (!page.getSystems()
                     .isEmpty()) {
                // Normal (full) rendering of the score
                page.acceptChildren(this);
            } else {
                // Render only what we have got so far...
                g.setStroke(lineStroke);

                ///g.setColor(lineColor);

                // Staff lines
                if (sheet.getStaves() != null) {
                    for (StaffInfo staff : sheet.getStaves()) {
                        staff.render(g);
                    }
                }

                if (sheet.getHorizontals() != null) {
                    // Horizontals
                    g.setStroke(lineStroke);

                    // Ledgers
                    for (Ledger ledger : sheet.getHorizontals()
                                              .getLedgers()) {
                        ledger.render(g);
                    }

                    // Endings
                    for (Ending ending : sheet.getHorizontals()
                                              .getEndings()) {
                        ending.render(g);
                    }
                }
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + page,
                ex);
        }

        return false;
    }

    //-------------------//
    // visit ScoreSystem //
    //-------------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        this.system = system;
        this.systemInfo = system.getInfo();

        if (!visit(systemInfo)) {
            return false;
        }

        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    /**
     * This specific version paints the staff lines as closely as possible to
     * the physical sheet lines
     * @param staff the staff to handle
     * @return true if actually painted
     */
    @Override
    public boolean visit (Staff staff)
    {
        try {
            if (staff.isDummy()) {
                return false;
            }

            staff.getInfo()
                 .render(g);
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + staff,
                ex);
        }

        return true;
    }

    //------------------//
    // visit SystemInfo //
    //------------------//
    public boolean visit (SystemInfo systemInfo)
    {
        try {
            // Check that this system is visible
            PixelRectangle bounds = systemInfo.getBounds();

            if ((bounds == null) || !bounds.intersects(g.getClipBounds())) {
                return false;
            }

            // Determine proper font size for the system, based on staff height
            StaffInfo firstStaff = systemInfo.getStaves()
                                             .get(0);
            int       staffHeight = firstStaff.getHeight();
            musicFont = MusicFont.getFont(staffHeight + scale.mainFore());

            // Compute delta y for FLAG_2 & FLAG_2_UP
            FLAG_2_DY = (int) Math.rint(
                layout(COMBINING_FLAG_2).getBounds().getHeight() * 0.5);

            g.setStroke(lineStroke);

            ///g.setColor(lineColor);

            // Ledgers
            for (Ledger ledger : systemInfo.getLedgers()) {
                ledger.render(g);
            }

            // Endings
            for (Ending ending : systemInfo.getEndings()) {
                ending.render(g);
            }
        } catch (ConcurrentModificationException ignored) {
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + systemInfo,
                ex);
        }

        return true;
    }

    //--------------------//
    // accidentalLocation //
    //--------------------//
    @Override
    protected PixelPoint accidentalLocation (Note  note,
                                             Glyph accidental)
    {
        return new PixelPoint(accidental.getAreaCenter().x, note.getCenter().y);
    }

    //----------//
    // braceBox //
    //----------//
    @Override
    protected PixelRectangle braceBox (SystemPart part)
    {
        PixelRectangle braceBox = part.getBrace()
                                      .getContourBox();

        // Cheat a little, so that top and bottom are aligned with part extrema
        int leftX = braceBox.x + braceBox.width;
        int top = part.getFirstStaff()
                      .getInfo()
                      .getFirstLine()
                      .yAt(leftX);
        int bot = part.getLastStaff()
                      .getInfo()
                      .getLastLine()
                      .yAt(leftX);
        braceBox.y = top;
        braceBox.height = bot - top + 1;

        return braceBox;
    }

    //--------------//
    // noteLocation //
    //--------------//
    protected PixelPoint noteLocation (Note note)
    {
        final PixelPoint center = note.getCenter();
        final Chord      chord = note.getChord();
        final Glyph      stem = chord.getStem();

        if (stem != null) {
            return location(center, chord);
        } else {
            return location(center);
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //    //----------//
    //    // drawSlot //
    //    //----------//
    //    /**
    //     * Draw a time slot in the score display, using the current graphics assumed
    //     * to be translated to the system origin.
    //     *
    //     * @param wholeSystem if true, the slot will embrace the whole system,
    //     * otherwise only the part is embraced
    //     * @param measure the containing measure
    //     * @param slot the slot to draw
    //     * @param color the color to use in drawing
    //     */
    //    private void drawSlot (boolean wholeSystem,
    //                           Measure measure,
    //                           Slot    slot,
    //                           Color   color)
    //    {
    //        final Color oldColor = g.getColor();
    //        g.setColor(color);
    //
    //        final Stroke         oldStroke = UIUtilities.setAbsoluteStroke(g, 1);
    //        final int            x = slot.getX();
    //        final PixelDimension systemDimension = measure.getSystem()
    //                                                      .getDimension();
    //
    //        if (wholeSystem) {
    //            // Draw for the system height
    //            g.drawLine(
    //                x,
    //                measure.getSystem()
    //                       .getTopLeft().y,
    //                x,
    //                measure.getSystem().getTopLeft().y + systemDimension.height +
    //                score.getMeanStaffHeight());
    //        } else {
    //            // Draw for the part height
    //            g.drawLine(
    //                x,
    //                measure.getPart()
    //                       .getFirstStaff()
    //                       .getTopLeft().y,
    //                x,
    //                measure.getPart().getLastStaff().getTopLeft().y +
    //                score.getMeanStaffHeight());
    //        }
    //
    //        g.setStroke(oldStroke);
    //        g.setColor(oldColor);
    //    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        /** Alpha parameter for slot axis transparency (0 .. 255) */
        final Constant.Integer slotAlpha = new Constant.Integer(
            "ByteLevel",
            150,
            "Alpha parameter for slot axis transparency (0 .. 255)");
    }
}
