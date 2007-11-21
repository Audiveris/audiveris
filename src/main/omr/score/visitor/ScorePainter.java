//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e P a i n t e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.plugin.Plugin;
import omr.plugin.PluginType;

import omr.score.Score;
import omr.score.common.ScorePoint;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.common.UnitDimension;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Barline;
import omr.score.entity.Beam;
import omr.score.entity.Chord;
import omr.score.entity.Clef;
import omr.score.entity.Coda;
import omr.score.entity.Dynamics;
import omr.score.entity.Fermata;
import omr.score.entity.KeySignature;
import omr.score.entity.Mark;
import omr.score.entity.Measure;
import omr.score.entity.MeasureElement;
import omr.score.entity.Note;
import omr.score.entity.Ornament;
import omr.score.entity.Pedal;
import omr.score.entity.Segno;
import omr.score.entity.Slot;
import omr.score.entity.Slur;
import omr.score.entity.Staff;
import omr.score.entity.System;
import omr.score.entity.SystemPart;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.entity.Tuplet;
import omr.score.entity.Wedge;
import static omr.score.ui.ScoreConstants.*;
import omr.score.ui.ScoreController;

import omr.sheet.Scale;

import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;

import omr.util.Implement;
import omr.util.Logger;
import omr.util.TreeNode;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;

/**
 * Class <code>ScorePainter</code> defines for every node in Score hierarchy
 * the painting of node in the <b>Score</b> display.
 *
 * @author Herv&eacute Bitteur
 * @version $Id$
 */
public class ScorePainter
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScorePainter.class);

    /** Stroke to draw beams */
    private static final Stroke beamStroke = new BasicStroke(4f);

    /** Stroke to draw voices */
    private static final Stroke voiceStroke = new BasicStroke(1.5f);

    /** Stroke to draw stems */
    private static final Stroke stemStroke = new BasicStroke(1f);

    /** Sequence of colors for voices */
    private static Color[] voiceColors = new Color[] {
                                             Color.CYAN, Color.ORANGE,
                                             Color.PINK, Color.GRAY, Color.GREEN,
                                             Color.MAGENTA, Color.BLUE,
                                             Color.YELLOW
                                         };

    //~ Enumerations -----------------------------------------------------------

    /** How a symbol should be horizontally aligned wrt a given point */
    private static enum HorizontalAlignment {
        LEFT,
        CENTER,
        RIGHT;
    }

    /** How a symbol should be vertically aligned wrt a given point */
    private static enum VerticalAlignment {
        TOP,
        CENTER,
        BOTTOM;
    }

    //~ Instance fields --------------------------------------------------------

    /** Graphic context */
    private final Graphics2D g;

    /** Display zoom */
    private final Zoom zoom;

    /** Used for icon image transformation */
    private final AffineTransform transform = new AffineTransform();

    /** Color for slot axis */
    private final Color slotColor = new Color(
        0,
        255,
        0,
        constants.slotAlpha.getValue());

    /** Color for highlighted slot */
    private final Color slotHighLightColor = new Color(
        0,
        0,
        0,
        constants.slotAlpha.getValue());

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScorePainter //
    //--------------//
    /**
     * Creates a new ScorePainter object.
     *
     * @param g Graphic context
     * @param z zoom factor
     */
    public ScorePainter (Graphics g,
                         Zoom     z)
    {
        this.g = (Graphics2D) g;
        this.zoom = z;

        // Anti-aliasing for beams especially
        this.g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // drawSlot //
    //----------//
    public void drawSlot (Measure measure,
                          Slot    slot,
                          Color   color)
    {
        Color oldColor = g.getColor();
        g.setColor(color);

        ScorePoint partOrigin = measure.getPart()
                                       .getFirstStaff()
                                       .getDisplayOrigin();
        final int  x = zoom.scaled(partOrigin.x + slot.getX());
        g.drawLine(
            x,
            zoom.scaled(partOrigin.y),
            x,
            zoom.scaled(
                measure.getPart().getLastStaff().getDisplayOrigin().y +
                STAFF_HEIGHT));

        g.setColor(oldColor);
    }

    //------------------//
    // visit Arpeggiate //
    //------------------//
    @Override
    public boolean visit (Arpeggiate arpeggiate)
    {
        // Draw an arpeggiate symbol with proper height
        // Top & bottom of symbol to draw
        final SystemRectangle box = arpeggiate.getBox();
        final int             top = box.y;
        final int             bot = box.y + box.height;
        final double          height = zoom.scaled(bot - top + 1);
        final SymbolIcon      icon = (SymbolIcon) Shape.ARPEGGIATO.getIcon();

        if (icon != null) {
            // Vertical ratio to extend the icon */
            final double ratio = height / icon.getIconHeight();

            g.setColor(Color.black);
            transform.setTransform(
                1,
                0,
                0,
                ratio,
                zoom.scaled(
                    arpeggiate.getDisplayOrigin().x + arpeggiate.getPoint().x),
                zoom.scaled(arpeggiate.getDisplayOrigin().y + top));
            g.drawRenderedImage(icon.getImage(), transform);
        }

        return false;
    }

    //---------------//
    // visit Barline //
    //---------------//
    @Override
    public boolean visit (Barline barline)
    {
        final Shape shape = barline.getShape();

        if (shape != null) {
            // Draw the barline symbol for each stave in the measure
            final SystemPart part = barline.getPart();

            for (TreeNode node : part.getStaves()) {
                final Staff staff = (Staff) node;
                paintSymbol(shape, barline.getCenter(), staff, 0);
            }
        } else {
            barline.addError("No shape for barline " + barline);
        }

        return true;
    }

    //------------//
    // visit Beam //
    //------------//
    @Override
    public boolean visit (Beam beam)
    {
        final Stroke oldStroke = g.getStroke();
        final Color  oldColor = g.getColor();

        // Draw the beam line, with a specific stroke
        g.setStroke(beamStroke);
        g.setColor(Color.black);
        paintLine(beam.getDisplayOrigin(), beam.getLeft(), beam.getRight());
        g.setColor(oldColor);
        g.setStroke(oldStroke);

        return true;
    }

    //-------------//
    // visit Chord //
    //-------------//
    @Override
    public boolean visit (Chord chord)
    {
        final Color oldColor = g.getColor();

        // Stem
        if (chord.getStem() != null) {
            final Stroke      oldStroke = g.getStroke();
            final SystemPoint tail = chord.getTailLocation();
            final SystemPoint head = chord.getHeadLocation();

            if ((tail == null) || (head == null)) {
                chord.addError("No head - tail defined for chord");

                return true;
            }

            g.setStroke(stemStroke);
            paintLine(chord.getDisplayOrigin(), tail, head);
            g.setStroke(oldStroke);

            // Flags ?
            final int fn = chord.getFlagsNumber();

            if (fn != 0) {
                Shape             shape;
                final SystemPoint center = new SystemPoint(tail);

                if (tail.y < head.y) { // Flags down
                    shape = Shape.values()[(COMBINING_FLAG_1.ordinal() + fn) -
                            1];
                    paintSymbol(
                        shape,
                        center,
                        chord.getDisplayOrigin(),
                        chord,
                        VerticalAlignment.TOP);
                } else { // Flags up
                    shape = Shape.values()[(COMBINING_FLAG_1_UP.ordinal() + fn) -
                            1];
                    paintSymbol(
                        shape,
                        center,
                        chord.getDisplayOrigin(),
                        chord,
                        VerticalAlignment.BOTTOM);
                }
            }
        }

        // Voice indication ?
        if (constants.voicePainting.getValue()) {
            if (chord.getVoice() != null) {
                // Link to previous chord with same voice
                final Chord prev = chord.getPreviousChordInVoice();

                if (prev != null) {
                    final Stroke oldStroke = g.getStroke();

                    try {
                        g.setColor(voiceColors[chord.getVoice() - 1]);
                        g.setStroke(voiceStroke);
                        paintLine(
                            chord.getDisplayOrigin(),
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
        if (constants.markPainting.getValue()) {
            for (Mark mark : chord.getMarks()) {
                final HorizontalAlignment hAlign = (mark.getPosition() == Mark.Position.BEFORE)
                                                   ? HorizontalAlignment.RIGHT
                                                   : HorizontalAlignment.LEFT;
                paintSymbol(
                    mark.getShape(),
                    mark.getLocation(),
                    chord.getDisplayOrigin(),
                    hAlign,
                    VerticalAlignment.CENTER);

                if (mark.getData() != null) {
                    g.setColor(Color.RED);

                    Point topLeft = toScaledPoint(
                        mark.getLocation(),
                        chord.getDisplayOrigin(),
                        hAlign,
                        10,
                        VerticalAlignment.BOTTOM,
                        -5);

                    g.drawString(
                        Note.quarterValueOf((Integer) mark.getData()),
                        topLeft.x,
                        topLeft.y);
                }
            }
        }

        g.setColor(oldColor);

        return true;
    }

    //------------//
    // visit Clef //
    //------------//
    @Override
    public boolean visit (Clef clef)
    {
        // Draw the clef symbol
        paintSymbol(
            clef.getShape(),
            clef.getCenter(),
            clef.getStaff(),
            clef.getPitchPosition());

        return true;
    }

    //------------//
    // visit Coda //
    //------------//
    @Override
    public boolean visit (Coda coda)
    {
        return visit((MeasureElement) coda);
    }

    //----------------//
    // visit Dynamics //
    //----------------//
    @Override
    public boolean visit (Dynamics dynamics)
    {
        return visit((MeasureElement) dynamics);
    }

    //---------------//
    // visit Fermata //
    //---------------//
    @Override
    public boolean visit (Fermata fermata)
    {
        return visit((MeasureElement) fermata);
    }

    //--------------------//
    // visit KeySignature //
    //--------------------//
    @Override
    public boolean visit (KeySignature keySignature)
    {
        if (keySignature.getPitchPosition() != null) {
            paintSymbol(
                keySignature.getShape(),
                keySignature.getCenter(),
                keySignature.getStaff(),
                keySignature.getPitchPosition());
        }

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        final Color      oldColor = g.getColor();
        final SystemPart part = measure.getPart();

        // Write the measure id, on the first staff  of the first part only
        if (part.getId() == 1) {
            final ScorePoint staffOrigin = measure.getPart()
                                                  .getFirstStaff()
                                                  .getDisplayOrigin();
            g.setColor(Color.lightGray);
            g.drawString(
                (measure.isPartial() ? "P" : "") +
                (measure.isImplicit() ? "I" : "") +
                Integer.toString(measure.getId()),
                zoom.scaled(staffOrigin.x + measure.getLeftX()) - 5,
                zoom.scaled(staffOrigin.y) - 15);
        }

        // Draw slot vertical lines ?
        if (constants.slotPainting.getValue()) {
            for (Slot slot : measure.getSlots()) {
                drawSlot(measure, slot, slotColor);
            }
        }

        // Flag for measure excess duration?
        if (measure.getExcess() != null) {
            final ScorePoint staffOrigin = measure.getPart()
                                                  .getFirstStaff()
                                                  .getDisplayOrigin();
            g.setColor(Color.red);
            g.drawString(
                "Excess " + Note.quarterValueOf(measure.getExcess()),
                zoom.scaled(staffOrigin.x + measure.getLeftX()) + 10,
                zoom.scaled(staffOrigin.y) - 15);
        }

        g.setColor(oldColor);

        return true;
    }

    //----------------------//
    // visit MeasureElement //
    //----------------------//
    @Override
    public boolean visit (MeasureElement measureElement)
    {
        if (measureElement.getShape() != null) {
            paintSymbol(
                measureElement.getShape(),
                measureElement.getPoint(),
                measureElement.getDisplayOrigin());
        }

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (Note note)
    {
        final SystemPart  part = note.getPart();
        final Staff       staff = note.getStaff();
        final Chord       chord = note.getChord();
        final Glyph       stem = chord.getStem();
        final Shape       shape = note.getShape();
        final int         pitch = (int) Math.rint(note.getPitchPosition());
        final SystemPoint center = note.getCenter();
        Shape             displayShape; // What is really displayed

        if (stem != null) {
            // Note is attached to a stem, link the note display to the stem one
            if (Shape.HeadAndFlags.contains(shape)) {
                displayShape = Shape.NOTEHEAD_BLACK;
            } else {
                displayShape = shape;
            }

            paintSymbol(displayShape, center, staff, pitch, chord);
        } else {
            // Use special display icons for some shapes
            if (shape == Shape.MULTI_REST) {
                displayShape = Shape.MULTI_REST_DISPLAY;
            } else if ((shape == Shape.WHOLE_REST) ||
                       (shape == Shape.HALF_REST)) {
                displayShape = Shape.WHOLE_OR_HALF_REST;
            } else {
                displayShape = shape;
            }

            paintSymbol(displayShape, center, part.getDisplayOrigin());
        }

        // Augmentation dots ?
        if (chord.getDotsNumber() == 1) {
            final SystemPoint dotCenter = new SystemPoint(
                note.getCenterRight());
            dotCenter.x += note.getScale()
                               .toUnits(constants.dotDx);

            // Avoid dot on line (staff or ledger)
            if ((pitch % 2) == 0) {
                paintSymbol(Shape.DOT, dotCenter, staff, pitch - 1);
            } else {
                paintSymbol(Shape.DOT, dotCenter, staff, pitch);
            }
        } else if (chord.getDotsNumber() == 2) {
            // TO BE IMPLEMENTED
        }

        // Accidental ?
        if (note.getAccidental() != null) {
            final SystemPoint accidCenter = new SystemPoint(note.getCenter());
            accidCenter.x -= note.getAccidentalDx();
            paintSymbol(
                note.getAccidental(),
                accidCenter,
                staff,
                pitch,
                HorizontalAlignment.CENTER);
        }

        // Ledgers ?
        if (!note.isRest() && (Math.abs(pitch) >= 6)) {
            final int         halfLedger = note.getScale()
                                               .toUnits(
                constants.halfLedgerLength);
            final SystemPoint left = new SystemPoint(center.x - halfLedger, 0);

            if (pitch > 0) {
                left.y += STAFF_HEIGHT;
            }

            final SystemPoint right = new SystemPoint(
                left.x + (2 * halfLedger),
                left.y);
            final int         sign = Integer.signum(pitch);

            for (int p = 6; p <= (pitch * sign); p = p + 2) {
                left.y += (INTER_LINE * sign);
                right.y += (INTER_LINE * sign);
                paintLine(staff.getDisplayOrigin(), left, right);
            }
        }

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        score.acceptChildren(this);

        return false;
    }

    //------------//
    // visit Slur //
    //------------//
    @Override
    public boolean visit (Slur slur)
    {
        // Build a new curve, w/ translation and zoom
        final Point        origin = slur.getDisplayOrigin();
        final CubicCurve2D curve = slur.getCurve();
        g.draw(
            new CubicCurve2D.Double(
                zoom.scaled(0.5 + curve.getX1() + origin.x),
                zoom.scaled(0.5 + curve.getY1() + origin.y),
                zoom.scaled(0.5 + curve.getCtrlX1() + origin.x),
                zoom.scaled(0.5 + curve.getCtrlY1() + origin.y),
                zoom.scaled(0.5 + curve.getCtrlX2() + origin.x),
                zoom.scaled(0.5 + curve.getCtrlY2() + origin.y),
                zoom.scaled(0.5 + curve.getX2() + origin.x),
                zoom.scaled(0.5 + curve.getY2() + origin.y)));

        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    @Override
    public boolean visit (Staff staff)
    {
        final Point origin = staff.getDisplayOrigin();
        g.setColor(Color.black);

        // Draw the staff lines
        for (int i = 0; i < LINE_NB; i++) {
            // Y of this staff line
            final int y = zoom.scaled(origin.y + (i * INTER_LINE));
            g.drawLine(
                zoom.scaled(origin.x),
                y,
                zoom.scaled(origin.x + staff.getWidth()),
                y);
        }

        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (System system)
    {
        // Check whether our system is impacted)
        final Rectangle clip = g.getClipBounds();
        final int       xMargin = INTER_SYSTEM;
        final int       systemLeft = system.getRightPosition() + xMargin;
        final int       systemRight = system.getDisplayOrigin().x - xMargin;

        if ((zoom.unscaled(clip.x) > systemLeft) ||
            (zoom.unscaled(clip.x + clip.width) < systemRight)) {
            return false;
        } else {
            final UnitDimension dimension = system.getDimension();
            final Point         origin = system.getDisplayOrigin();
            g.setColor(Color.lightGray);

            // Draw the system left edge
            g.drawLine(
                zoom.scaled(origin.x),
                zoom.scaled(origin.y),
                zoom.scaled(origin.x),
                zoom.scaled(origin.y + dimension.height + STAFF_HEIGHT));

            // Draw the system right edge
            g.drawLine(
                zoom.scaled(origin.x + dimension.width),
                zoom.scaled(origin.y),
                zoom.scaled(origin.x + dimension.width),
                zoom.scaled(origin.y + dimension.height + STAFF_HEIGHT));

            return true;
        }
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart part)
    {
        // Draw a brace if there is more than one stave in the part
        if (part.getStaves()
                .size() > 1) {
            // Top & bottom of brace to draw
            final int        top = part.getFirstStaff()
                                       .getDisplayOrigin().y;
            final int        bot = part.getLastStaff()
                                       .getDisplayOrigin().y + STAFF_HEIGHT;
            final double     height = zoom.scaled(bot - top + 1);

            // Vertical ratio to extend the icon */
            final SymbolIcon braceIcon = (SymbolIcon) Shape.BRACE.getIcon();

            if (braceIcon != null) {
                final double ratio = height / braceIcon.getIconHeight();

                // Offset on left of system
                final int dx = 10;
                g.setColor(Color.black);
                transform.setTransform(
                    1,
                    0,
                    0,
                    ratio,
                    zoom.scaled(part.getSystem()
                                    .getDisplayOrigin().x) - dx,
                    zoom.scaled(top));
                g.drawRenderedImage(braceIcon.getImage(), transform);
            }
        }

        // Draw the starting barline, if any
        if (part.getStartingBarline() != null) {
            part.getStartingBarline()
                .accept(this);
        }

        return true;
    }

    //---------------------//
    // visit TimeSignature //
    //---------------------//
    @Override
    public boolean visit (TimeSignature timeSignature)
    {
        try {
            final Shape      shape = timeSignature.getShape();
            final SystemPart part = timeSignature.getPart();

            if (shape != null) {
                switch (shape) {
                // If this is an illegal shape, do not draw anything.
                // TBD: we could draw a special sign for this
                case NO_LEGAL_SHAPE :
                    break;

                // Is it a complete (one-symbol) time signature ?
                case TIME_FOUR_FOUR :
                case TIME_TWO_TWO :
                case TIME_TWO_FOUR :
                case TIME_THREE_FOUR :
                case TIME_SIX_EIGHT :
                case COMMON_TIME :
                case CUT_TIME :
                    paintSymbol(
                        shape,
                        timeSignature.getCenter(),
                        part.getDisplayOrigin());

                    break;
                }
            } else {
                // Assume a (legal) multi-symbol signature
                for (Glyph glyph : timeSignature.getGlyphs()) {
                    final Shape s = glyph.getShape();

                    if (s != null) {
                        final SystemPoint center = timeSignature.computeGlyphCenter(
                            glyph);
                        final Staff       staff = part.getStaffAt(center);
                        final int         pitch = (int) Math.rint(
                            staff.pitchPositionOf(center));
                        paintSymbol(s, center, staff, pitch);
                    }
                }
            }
        } catch (InvalidTimeSignature ex) {
        }

        return true;
    }

    //-------------//
    // visit Pedal //
    //-------------//
    public boolean visit (Pedal pedal)
    {
        return visit((MeasureElement) pedal);
    }

    //-------------//
    // visit Segno //
    //-------------//
    public boolean visit (Segno segno)
    {
        return visit((MeasureElement) segno);
    }

    //----------------//
    // visit Ornament //
    //----------------//
    public boolean visit (Ornament ornament)
    {
        return visit((MeasureElement) ornament);
    }

    //--------------//
    // visit Tuplet //
    //--------------//
    public boolean visit (Tuplet tuplet)
    {
        return visit((MeasureElement) tuplet);
    }

    //-------------//
    // visit Wedge //
    //-------------//
    @Override
    public boolean visit (Wedge wedge)
    {
        if (wedge.isStart()) {
            final System          system = wedge.getSystem();
            final SystemRectangle box = system.toSystemRectangle(
                wedge.getGlyph().getContourBox());

            SystemPoint           single;
            SystemPoint           top;
            SystemPoint           bot;

            if (wedge.getShape() == Shape.CRESCENDO) {
                single = new SystemPoint(box.x, box.y + (box.height / 2));
                top = new SystemPoint(box.x + box.width, box.y);
                bot = new SystemPoint(box.x + box.width, box.y + box.height);
            } else {
                single = new SystemPoint(
                    box.x + box.width,
                    box.y + (box.height / 2));
                top = new SystemPoint(box.x, box.y);
                bot = new SystemPoint(box.x, box.y + box.height);
            }

            paintLine(system.getDisplayOrigin(), single, top);
            paintLine(system.getDisplayOrigin(), single, bot);
        }

        return true;
    }

    //-----------//
    // paintLine //
    //-----------//
    /**
     * Draw a line from one SystemPoint to another SystemPoint within their
     * containing system.
     *
     * @param displayOrigin the related system display origin
     * @param from first point
     * @param to second point
     */
    private void paintLine (ScorePoint  displayOrigin,
                            SystemPoint from,
                            SystemPoint to)
    {
        if ((g != null) &&
            (zoom != null) &&
            (displayOrigin != null) &&
            (from != null) &&
            (to != null)) {
            g.drawLine(
                zoom.scaled(displayOrigin.x + from.x),
                zoom.scaled(displayOrigin.y + from.y),
                zoom.scaled(displayOrigin.x + to.x),
                zoom.scaled(displayOrigin.y + to.y));
        } else {
            logger.warning("line not painted due to null reference");
        }
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol icon using the coordinates in units of its bounding point
     * within the containing system part, assuming CENTER for both horizontal
     * and vertical alignments
     *
     * @param shape the shape whose icon must be painted
     * @param point system-based given point in units
     * @param displayOrigin the related origin in score display
     */
    private void paintSymbol (Shape       shape,
                              SystemPoint point,
                              ScorePoint  displayOrigin)
    {
        paintSymbol(
            shape,
            point,
            displayOrigin,
            HorizontalAlignment.CENTER,
            VerticalAlignment.CENTER);
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol icon using the coordinates in units of its bounding point
     * within the containing system part
     *
     * @param shape the shape whose icon must be painted
     * @param point system-based given point in units
     * @param displayOrigin the related origin in score display
     * @param hAlign the horizontal alignment wrt the point
     * @param vAlign the vertical alignment wrt the point
     */
    private void paintSymbol (Shape               shape,
                              SystemPoint         point,
                              ScorePoint          displayOrigin,
                              HorizontalAlignment hAlign,
                              VerticalAlignment   vAlign)
    {
        final SymbolIcon icon = (SymbolIcon) shape.getIcon();

        if (icon != null) {
            final Point topLeft = toScaledPoint(
                point,
                displayOrigin,
                hAlign,
                icon.getActualWidth(),
                vAlign,
                icon.getIconHeight());
            g.drawImage(icon.getImage(), topLeft.x, topLeft.y, null);
        }
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol icon using the coordinates in units of its bounding center
     * within the containing system part, forcing adjacency with provided chord
     * stem.
     *
     * @param shape the shape whose icon must be painted
     * @param center system-based bounding center in units
     * @param displayOrigin the related origin in score display
     * @param chord the chord stem to attach the symbol to
     */
    private void paintSymbol (Shape       shape,
                              SystemPoint center,
                              ScorePoint  displayOrigin,
                              Chord       chord)
    {
        paintSymbol(
            shape,
            center,
            displayOrigin,
            chord,
            VerticalAlignment.CENTER);
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol icon using the coordinates in units of its bounding center
     * within the containing system part, forcing adjacency with provided chord
     * stem.
     *
     * @param shape the shape whose icon must be painted
     * @param center system-based bounding center in units
     * @param displayOrigin the related origin in score display
     * @param chord the chord stem to attach the symbol to
     * @param vAlign the vertical alignment wrt the point
     */
    private void paintSymbol (Shape             shape,
                              SystemPoint       center,
                              ScorePoint        displayOrigin,
                              Chord             chord,
                              VerticalAlignment vAlign)
    {
        final SymbolIcon icon = (SymbolIcon) shape.getIcon();

        if (icon != null) {
            final Point topLeft = new Point(
                zoom.scaled(displayOrigin.x + chord.getTailLocation().x),
                zoom.scaled(displayOrigin.y + center.y));

            // Horizontal alignment
            if (center.x < chord.getTailLocation().x) {
                // Symbol is on left side of stem (-1 is for stem width)
                topLeft.x -= (icon.getActualWidth() - 1);
            }

            // Vertical alignment
            if (vAlign == VerticalAlignment.CENTER) {
                topLeft.y -= (icon.getIconHeight() / 2);
            } else if (vAlign == VerticalAlignment.BOTTOM) {
                topLeft.y -= icon.getIconHeight();
            }

            g.drawImage(icon.getImage(), topLeft.x, topLeft.y, null);
        }
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol icon using the coordinates in units of its bounding center
     * within the containing system part, forcing adjacency with provided chord
     * stem.
     *
     * @param shape the shape whose icon must be painted
     * @param center part-based bounding center in units
     * @param chord the chord stem to attach the symbol to
     */
    private void paintSymbol (Shape       shape,
                              SystemPoint center,
                              Staff       staff,
                              double      pitchPosition,
                              Chord       chord)
    {
        final SymbolIcon icon = (SymbolIcon) shape.getIcon();

        if (icon != null) {
            final ScorePoint displayOrigin = staff.getDisplayOrigin();
            final int        dy = Staff.pitchToUnit(pitchPosition);

            // Position of symbol wrt stem
            final int stemX = chord.getTailLocation().x;
            int       iconX = zoom.scaled(displayOrigin.x + stemX);

            if (center.x < stemX) {
                // Symbol is on left side of stem (-1 is for stem width)
                iconX -= (icon.getActualWidth() - 1);
            }

            g.drawImage(
                icon.getImage(),
                iconX,
                zoom.scaled(displayOrigin.y + dy) - icon.getCenter().y,
                null);
        }
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol using its pitch position for ordinate in the containing
     * staff, assuming CENTER for horizontal alignment.
     *
     * @param shape the shape whose icon must be painted
     * @param center system-based coordinates of bounding center in units (only
     *               abscissa is actually used)
     * @param staff the related staff
     * @param pitchPosition staff-based ordinate in step lines
     */
    private void paintSymbol (Shape       shape,
                              SystemPoint center,
                              Staff       staff,
                              double      pitchPosition)
    {
        paintSymbol(
            shape,
            center,
            staff,
            pitchPosition,
            HorizontalAlignment.CENTER);
    }

    //-------------//
    // paintSymbol //
    //-------------//
    /**
     * Paint a symbol using its pitch position for ordinate in the containing
     * staff
     *
     * @param shape the shape whose icon must be painted
     * @param center system-based coordinates of bounding center in units (only
     *               abscissa is actually used)
     * @param staff the related staff
     * @param pitchPosition staff-based ordinate in step lines
     * @param hAlign the horizontal alignment wrt the point
     */
    private void paintSymbol (Shape               shape,
                              SystemPoint         center,
                              Staff               staff,
                              double              pitchPosition,
                              HorizontalAlignment hAlign)
    {
        final SymbolIcon icon = (SymbolIcon) shape.getIcon();

        if (icon != null) {
            final Point topLeft = new Point(
                zoom.scaled(staff.getDisplayOrigin().x + center.x),
                zoom.scaled(
                    staff.getDisplayOrigin().y +
                    Staff.pitchToUnit(pitchPosition)));

            // Horizontal alignment
            if (hAlign == HorizontalAlignment.CENTER) {
                topLeft.x -= (icon.getActualWidth() / 2);
            } else if (hAlign == HorizontalAlignment.RIGHT) {
                topLeft.x -= icon.getActualWidth();
            }

            // Specific vertical alignment
            topLeft.y -= icon.getCenter().y;

            g.drawImage(icon.getImage(), topLeft.x, topLeft.y, null);
        }
    }

    //----------------//
    // repaintDisplay //
    //----------------//
    private static void repaintDisplay ()
    {
        // Update current score display if any
        final Score score = ScoreController.getCurrentScore();

        if (score != null) {
            score.getView()
                 .getComponent()
                 .repaint();
        }
    }

    //---------------//
    // toScaledPoint //
    //---------------//
    private Point toScaledPoint (SystemPoint         sysPt,
                                 ScorePoint          displayOrigin,
                                 HorizontalAlignment hAlign,
                                 int                 width,
                                 VerticalAlignment   vAlign,
                                 int                 height)
    {
        final Point point = new Point(
            zoom.scaled(displayOrigin.x + sysPt.x),
            zoom.scaled(displayOrigin.y + sysPt.y));

        if (hAlign == HorizontalAlignment.CENTER) {
            point.x -= (width / 2);
        } else if (hAlign == HorizontalAlignment.RIGHT) {
            point.x -= width;
        }

        if (vAlign == VerticalAlignment.CENTER) {
            point.y -= (height / 2);
        } else if (vAlign == VerticalAlignment.BOTTOM) {
            point.y -= height;
        }

        return point;
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // MarkAction //
    //------------//
    /**
     * Class <code>MarkAction</code> toggles the display of computed marks in
     * the score
     */
    @Plugin(type = PluginType.SCORE_VIEW, item = JCheckBoxMenuItem.class)
    public static class MarkAction
        extends AbstractAction
    {
        public MarkAction ()
        {
            putValue(SELECTED_KEY, constants.markPainting.getValue());
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
            constants.markPainting.setValue(item.isSelected());
            repaintDisplay();
        }
    }

    //------------//
    // SlotAction //
    //------------//
    /**
     * Class <code>SlotAction</code> toggles the display of vertical time slots
     */
    @Plugin(type = PluginType.SCORE_VIEW, item = JCheckBoxMenuItem.class)
    public static class SlotAction
        extends AbstractAction
    {
        public SlotAction ()
        {
            putValue(SELECTED_KEY, constants.slotPainting.getValue());
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
            constants.slotPainting.setValue(item.isSelected());
            repaintDisplay();
        }
    }

    //-------------//
    // VoiceAction //
    //-------------//
    /**
     * Class <code>VoiceAction</code> toggles the display of voices with
     * specific colors
     */
    @Plugin(type = PluginType.SCORE_VIEW, item = JCheckBoxMenuItem.class)
    public static class VoiceAction
        extends AbstractAction
    {
        public VoiceAction ()
        {
            putValue(SELECTED_KEY, constants.voicePainting.getValue());
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
            constants.voicePainting.setValue(item.isSelected());
            repaintDisplay();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Alpha parameter for slot axis transparency (0 .. 255) */
        final Constant.Integer slotAlpha = new Constant.Integer(
            "ByteLevel",
            20,
            "Alpha parameter for slot axis transparency (0 .. 255)");

        /** Should the slots be painted */
        final Constant.Boolean slotPainting = new Constant.Boolean(
            true,
            "Should the slots be painted");

        /** Should the voices be painted */
        final Constant.Boolean voicePainting = new Constant.Boolean(
            false,
            "Should the voices be painted");

        /** Should the marks be painted */
        final Constant.Boolean markPainting = new Constant.Boolean(
            true,
            "Should the marks be painted");

        /** dx between note and augmentation dot */
        final Scale.Fraction dotDx = new Scale.Fraction(
            0.3,
            "dx between note and augmentation dot");

        /** Half length of a ledger */
        final Scale.Fraction halfLedgerLength = new Scale.Fraction(
            1,
            "Half length of a ledger");
    }
}
