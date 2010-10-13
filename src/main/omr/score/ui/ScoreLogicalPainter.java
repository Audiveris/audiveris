//----------------------------------------------------------------------------//
//                                                                            //
//                   S c o r e L o g i c a l P a i n t e r                    //
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

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.common.PixelRectangle;
import omr.score.entity.Barline;
import omr.score.entity.Chord;
import omr.score.entity.Mark;
import omr.score.entity.Measure;
import omr.score.entity.Note;
import omr.score.entity.ScoreSystem;
import omr.score.entity.Slot;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.ui.MusicFont.Alignment;
import omr.score.ui.MusicFont.Alignment.Horizontal;
import static omr.score.ui.MusicFont.Alignment.Horizontal.*;
import static omr.score.ui.MusicFont.Alignment.Vertical.*;

import omr.sheet.Scale;
import omr.sheet.Sheet;

import omr.ui.util.UIUtilities;
import omr.ui.view.Zoom;

import omr.util.TreeNode;

import java.awt.*;
import java.awt.font.TextLayout;
import java.util.*;
import java.util.List;

/**
 * Class <code>ScoreLogicalPainter</code> paints the recognized score entities
 * using an ideal layout, where all staff lines are parallel and equidistant,
 * all notes are perfectly located according to their pitch position,etc.
 *
 * <p>TODO:
 *
 * @author Hervé Bitteur
 */
public class ScoreLogicalPainter
    extends ScorePainter
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(
        ScoreLogicalPainter.class);

    /** Sequence of colors for voices */
    private static Color[] voiceColors = new Color[] {
                                             Color.CYAN, Color.ORANGE,
                                             Color.PINK, Color.GRAY, Color.GREEN,
                                             Color.MAGENTA, Color.BLUE,
                                             Color.YELLOW
                                         };

    /** Stroke to draw voices */
    private static final Stroke voiceStroke = new BasicStroke(
        6f,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_BEVEL,
        0f,
        new float[] { 20f, 10f },
        0);

    //~ Instance fields --------------------------------------------------------

    /** Related score layout (only Horizontal is supported now) */
    private final ScoreLayout scoreLayout;

    /** Color for slot axis */
    private final Color slotColor = new Color(
        0,
        255,
        0,
        constants.slotAlpha.getValue());

    //~ Constructors -----------------------------------------------------------

    //---------------------//
    // ScoreLogicalPainter //
    //---------------------//
    /**
     * Creates a new ScoreLogicalPainter object.
     *
     * @param scoreLayout the score layout, with its orientation
     * @param initialGraphics the Graphic context, already properly scaled
     * @param zoom the zoom factor (a temporary fix to "descale" for symbols)
     */
    public ScoreLogicalPainter (ScoreLayout scoreLayout,
                                Graphics    initialGraphics,
                                Zoom        zoom)
    {
        super(initialGraphics);

        this.scoreLayout = scoreLayout;
    }

    //~ Methods ----------------------------------------------------------------

    //------------------//
    // drawAbsoluteSlot //
    //------------------//
    /**
     * Draw a time slot in the score display, with graphics not yet translated.
     *
     * @param wholeSystem if true, the slot will embrace the whole system,
     * otherwise only the part is embraced
     * @param measure the containing measure
     * @param slot the slot to draw
     * @param color the color to use in drawing
     */
    public void drawAbsoluteSlot (boolean wholeSystem,
                                  Measure measure,
                                  Slot    slot,
                                  Color   color)
    {
        //        final SystemView systemView = scoreLayout.getSystemView(
        //            measure.getSystem());
        //        final Point      origin = systemView.getDisplayOrigin();

        //        // Now use system topLeft as the origin
        //        g.translate(origin.x, origin.y);
        drawSlot(wholeSystem, measure, slot, color);
    }

    //---------------//
    // visit Barline //
    //---------------//
    @Override
    public boolean visit (Barline barline)
    {
        //logger.info("Visit " + barline);
        try {
            final Shape shape = barline.getShape();

            if (shape != null) {
                // Draw the barline symbol for each stave in the measure
                for (TreeNode node : barline.getPart()
                                            .getStaves()) {
                    final Staff staff = (Staff) node;

                    if (shape == DOUBLE_BARLINE) {
                        TextLayout layout = layout(THIN_BARLINE);
                        PixelPoint left = new PixelPoint(barline.getLeftX(), 0);
                        paint(layout, location(left, staff, 0));

                        PixelPoint right = new PixelPoint(
                            barline.getRightX(),
                            0);
                        paint(layout, location(right, staff, 0));
                    } else if (shape == BACK_TO_BACK_REPEAT_SIGN) {
                        PixelPoint point = new PixelPoint(barline.getCenter());
                        TextLayout layout = layout(RIGHT_REPEAT_SIGN);
                        int        dx = (int) Math.rint(
                            layout.getBounds().getWidth() / 5); // Heuristic
                        point.x += dx;
                        paint(
                            layout,
                            location(point, staff, 0),
                            new Alignment(RIGHT, MIDDLE));
                        layout = layout(LEFT_REPEAT_SIGN);
                        point.x -= (2 * dx);
                        paint(
                            layout,
                            location(point, staff, 0),
                            new Alignment(LEFT, MIDDLE));
                    } else {
                        paint(
                            layout(shape),
                            location(barline.getCenter(), staff, 0));
                    }
                }
            } else {
                barline.addError("No shape for barline " + barline);
            }
        } catch (Exception ex) {
            // We do nothing
            logger.warning("Could not draw Barline", ex);
        }

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        // Super: check, flags
        if (!super.visit(chord)) {
            return false;
        }

        Stroke oldStroke = g.getStroke();

        // Stem (logical)
        if (chord.getStem() != null) {
            g.setStroke(stemStroke);

            final PixelPoint tail = chord.getTailLocation();
            final PixelPoint head = new PixelPoint(chord.getHeadLocation());
            List<TreeNode>   allNotes = new ArrayList<TreeNode>(
                chord.getNotes());
            Collections.sort(allNotes, Chord.noteHeadComparator);

            Note  firstNote = (Note) allNotes.get(0);
            Staff staff = chord.getStaff();
            head.y = staff.getTopLeft().y +
                     staff.pitchToPixels(firstNote.getPitchPosition());
            paintLine(head, tail);
            g.setStroke(oldStroke);
        }

        // Voice indication ?
        final Color oldColor = g.getColor();

        if (PaintingParameters.getInstance()
                              .isVoicePainting()) {
            if (chord.getVoice() != null) {
                // Link to previous chord with same voice
                final Chord prev = chord.getPreviousChordInVoice();

                if (prev != null) {
                    ////logger.info("from " + prev.getHeadLocation() + " to " + chord.getHeadLocation());
                    oldStroke = g.getStroke();

                    try {
                        g.setColor(voiceColors[chord.getVoice()
                                                    .getId() - 1]);
                        g.setStroke(voiceStroke);
                        paintLine(
                            prev.getHeadLocation(),
                            chord.getHeadLocation());
                    } catch (Exception ex) {
                        chord.addError(ex + " voice=" + chord.getVoice());
                    }

                    g.setStroke(oldStroke);
                }
            } else {
                chord.addError("No voice for chord " + chord);
            }
        }

        // Marks ?
        if (PaintingParameters.getInstance()
                              .isMarkPainting()) {
            for (Mark mark : chord.getMarks()) {
                final Horizontal hAlign = (mark.getPosition() == Mark.Position.BEFORE)
                                          ? Horizontal.RIGHT : Horizontal.LEFT;
                g.setColor(Color.RED);
                paint(
                    layout(mark.getCharDesc().getString()),
                    location(mark.getLocation()),
                    new Alignment(hAlign, MIDDLE));

                //                                if (mark.getData() != null) {
                //                                    Point topLeft = topLeftOf(
                //                                        mark.getLocation(),
                //                                        hAlign,
                //                                        10,
                //                                        Vertical.BOTTOM,
                //                                        -5);
                //                
                //                                    g.drawString(
                //                                        Note.quarterValueOf((Integer) mark.getData()),
                //                                        topLeft.x,
                //                                        topLeft.y);
                //                                }
            }
        }

        g.setColor(oldColor);

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        final SystemPart part = measure.getPart();
        final Color      oldColor = g.getColor();

        if (measure.isDummy()) {
            // Draw left side
            for (TreeNode node : measure.getPart()
                                        .getStaves()) {
                final Staff staff = (Staff) node;
                paint(
                    layout(THIN_BARLINE),
                    location(new PixelPoint(measure.getLeftX(), 0), staff, 0));
            }
        } else {
            // Write the measure id, on first staff of the first real part only
            if ((part == measure.getSystem()
                                .getFirstRealPart()) &&
                (measure.getId() != 0)) {
                g.setColor(Color.lightGray);

                g.drawString(
                    (measure.isPartial() ? "P" : "") +
                    (measure.isImplicit() ? "I" : "") +
                    Integer.toString(measure.getId()),
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

            // Flag for measure excess duration?
            if (measure.getExcess() != null) {
                g.setColor(Color.red);
                g.drawString(
                    "Excess " + Note.quarterValueOf(measure.getExcess()),
                    measure.getLeftX() + 10,
                    measure.getPart().getFirstStaff().getTopLeft().y - 15);
            }
        }

        g.setColor(oldColor);

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (Note note)
    {
        // Paint note head and accidentals
        super.visit(note);

        final Staff      staff = note.getStaff();
        final int        pitch = (int) Math.rint(note.getPitchPosition());
        final PixelPoint center = note.getCenter();
        final Chord      chord = note.getChord();

        // Augmentation dots ?
        if (chord.getDotsNumber() > 0) {
            final PixelPoint dotCenter = new PixelPoint(note.getCenterRight());
            final int        dotDx = scale.toPixels(constants.dotDx);

            for (int i = 0; i < chord.getDotsNumber(); i++) {
                dotCenter.x += dotDx;

                // Avoid dot on line (staff or ledger)
                if ((pitch % 2) == 0) {
                    paint(layout(DOT), location(dotCenter, staff, pitch - 1));
                } else {
                    paint(layout(DOT), location(dotCenter, staff, pitch));
                }
            }
        }

        // Ledgers ?
        if (!note.isRest() && (Math.abs(pitch) >= 6)) {
            Stroke           oldStroke = UIUtilities.setAbsoluteStroke(g, 1);
            final int        halfLedger = note.getScale()
                                              .toPixels(
                constants.halfLedgerLength);

            // Left side of the ledger (on staff external line)
            final PixelPoint left = new PixelPoint(
                center.x - halfLedger,
                (pitch < 0) ? staff.getTopLeft().y
                                : (staff.getTopLeft().y +
                                score.getMeanStaffHeight()));

            // Right side of the ledger (on staff external line)
            final PixelPoint right = new PixelPoint(
                left.x + (2 * halfLedger),
                left.y);
            final int        sign = Integer.signum(pitch);

            // We draw ledgers until we reach absolute target note pitch
            int interline = scale.interline();

            for (int p = 6; p <= (pitch * sign); p = p + 2) {
                left.y += (interline * sign);
                right.y += (interline * sign);
                paintLine(left, right);
            }

            g.setStroke(oldStroke);
        }

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        this.score = score;

        final Sheet sheet = score.getSheet();
        scale = sheet.getScale();

        if (scale == null) {
            return false;
        }

        // Set all painting parameters
        initParameters();

        // Determine size for music font (same staff heights everywhere)
        musicFont = MusicFont.getFont(score.getMeanStaffHeight());

        // Use basic font by default
        g.setFont(basicFont);

        // Browse
        score.acceptChildren(this);

        return false;
    }

    //-------------------//
    // visit ScoreSystem //
    //-------------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        SystemView systemView = scoreLayout.getSystemView(system);

        if (systemView == null) {
            return false;
        }

        // Check that displayOrigin has been set, otherwise there is no point
        // in displaying the system, which is currently being built and which
        // will later use ScoreFixer to assign proper displayOrigin
        if (systemView.getDisplayOrigin() == null) {
            return false;
        }

        final PixelPoint     origin = system.getTopLeft();
        final PixelDimension dim = system.getDimension();

        // Check whether our system is impacted
        final Rectangle systemRect = system.getDisplayContour(); // (We get a copy)

        if ((origin == null) || (systemRect == null)) { // Safer

            return false;
        }

        if (!systemRect.intersects(g.getClipBounds())) {
            return false;
        }

        // Write system # at the top of the display (if horizontal layout)
        // and at the left of the display (if vertical layout)
        final Color oldColor = g.getColor();
        g.setColor(Color.lightGray);

        if (systemView.getOrientation() == ScoreOrientation.HORIZONTAL) {
            g.drawString("S" + system.getId(), origin.x, 24);
        } else {
            g.drawString("S" + system.getId(), 0, origin.y);
        }

        g.setColor(oldColor);

        // Draw the system left edge
        Stroke oldStroke = UIUtilities.setAbsoluteStroke(g, 1);
        g.drawLine(
            origin.x,
            origin.y,
            origin.x,
            origin.y + dim.height + score.getMeanStaffHeight());

        // Draw the system right edge
        g.drawLine(
            origin.x + dim.width,
            origin.y,
            origin.x + dim.width,
            origin.y + dim.height + score.getMeanStaffHeight());
        g.setStroke(oldStroke);

        this.system = system;

        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    /**
     * This specific version draws <b>artificial</b> ideal staff lines, all as
     * horizontal straight lines, equidistant to each other.
     * <p>The distance used should be the average distance between the physical
     * lines of the staff. For the time being, we use the interline value from
     * the whole page.
     * @param staff the staff to handle
     * @return true if actually painted
     */
    @Override
    public boolean visit (Staff staff)
    {
        final Color oldColor = g.getColor();
        Stroke      oldStroke = UIUtilities.setAbsoluteStroke(g, 1);

        try {
            int leftX = staff.getTopLeft().x;
            int topY = staff.getTopLeft().y;

            if (staff.isDummy()) {
                g.setColor(Color.LIGHT_GRAY);
            } else {
                g.setColor(Color.black);
            }

            // Draw the staff lines
            for (int i = 0; i < Score.LINE_NB; i++) {
                // Y of this staff line
                final int y = topY + (i * scale.interline());
                g.drawLine(leftX, y, leftX + staff.getWidth(), y);
            }
        } catch (Exception ex) {
            logger.warning("Cannot paint " + staff);
        }

        g.setColor(oldColor);
        g.setStroke(oldStroke);

        return true;
    }

    //--------------------//
    // accidentalLocation //
    //--------------------//
    @Override
    protected PixelPoint accidentalLocation (Note  note,
                                             Glyph accidental)
    {
        final Staff staff = note.getStaff();
        final int   pitch = (int) Math.rint(note.getPitchPosition());

        return location(accidental.getAreaCenter(), staff, pitch);
    }

    //----------//
    // braceBox //
    //----------//
    @Override
    protected PixelRectangle braceBox (SystemPart part)
    {
        // Top & bottom of brace to draw
        int top = part.getFirstStaff()
                      .getTopLeft().y;
        int width = part.getBrace()
                        .getContourBox().width;
        int bottom = part.getLastStaff()
                         .getTopLeft().y + score.getMeanStaffHeight();

        return new PixelRectangle(
            part.getBox().x - width,
            top,
            width,
            bottom - top + 1);
    }

    //--------------//
    // noteLocation //
    //--------------//
    protected PixelPoint noteLocation (Note note)
    {
        final PixelPoint center = note.getCenter();
        final Chord      chord = note.getChord();
        final Glyph      stem = chord.getStem();
        final Staff      staff = note.getStaff();
        final double     pitch = note.getPitchPosition();

        //final int        pitch = (int) Math.rint(note.getPitchPosition());
        if (stem != null) {
            return location(center, chord, staff, pitch);
        } else {
            return location(center, staff, pitch);
        }
    }

    //----------//
    // drawSlot //
    //----------//
    /**
     * Draw a time slot in the score display, using the current graphics assumed
     * to be translated to the system origin.
     *
     * @param wholeSystem if true, the slot will embrace the whole system,
     * otherwise only the part is embraced
     * @param measure the containing measure
     * @param slot the slot to draw
     * @param color the color to use in drawing
     */
    private void drawSlot (boolean wholeSystem,
                           Measure measure,
                           Slot    slot,
                           Color   color)
    {
        final Color oldColor = g.getColor();
        g.setColor(color);

        final int            x = slot.getX();
        final PixelDimension systemDimension = measure.getSystem()
                                                      .getDimension();

        if (wholeSystem) {
            // Draw for the system height
            g.drawLine(
                x,
                measure.getSystem()
                       .getTopLeft().y,
                x,
                measure.getSystem().getTopLeft().y + systemDimension.height +
                score.getMeanStaffHeight());
        } else {
            // Draw for the part height
            g.drawLine(
                x,
                measure.getPart()
                       .getFirstStaff()
                       .getTopLeft().y,
                x,
                measure.getPart().getLastStaff().getTopLeft().y +
                score.getMeanStaffHeight());
        }

        g.setColor(oldColor);
    }

    //~ Inner Classes ----------------------------------------------------------

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

        /** Half length of a ledger */
        final Scale.Fraction halfLedgerLength = new Scale.Fraction(
            1,
            "Half length of a ledger");

        /** dx between note and augmentation dot */
        final Scale.Fraction dotDx = new Scale.Fraction(
            0.5,
            "dx between note and augmentation dot");
    }
}
