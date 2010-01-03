//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e P a i n t e r                           //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;
import omr.glyph.ShapeRange;
import omr.glyph.text.Sentence;
import omr.glyph.text.TextInfo;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.SystemPoint;
import omr.score.common.SystemRectangle;
import omr.score.common.UnitDimension;
import omr.score.entity.Arpeggiate;
import omr.score.entity.Articulation;
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
import omr.score.entity.ScoreSystem;
import omr.score.entity.Segno;
import omr.score.entity.Slot;
import omr.score.entity.Slur;
import omr.score.entity.Staff;
import omr.score.entity.SystemPart;
import omr.score.entity.Text;
import omr.score.entity.TimeSignature;
import omr.score.entity.TimeSignature.InvalidTimeSignature;
import omr.score.entity.Tuplet;
import omr.score.entity.Wedge;
import static omr.score.ui.ScoreConstants.*;
import omr.score.visitor.AbstractScoreVisitor;

import omr.sheet.Scale;

import omr.ui.icon.SymbolIcon;
import omr.ui.view.Zoom;

import omr.util.TreeNode;

import java.awt.*;
import java.awt.geom.*;
import java.util.List;

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
        //~ Enumeration constant initializers ----------------------------------

        LEFT,CENTER, RIGHT;
    }

    /** How a symbol should be vertically aligned wrt a given point */
    private static enum VerticalAlignment {
        //~ Enumeration constant initializers ----------------------------------

        TOP,CENTER, BOTTOM;
    }

    //~ Instance fields --------------------------------------------------------

    /** Related score view, with a specific system layout */
    private final ScoreView scoreView;

    /** Graphic context */
    private final Graphics2D g;

    /** Display zoom */
    private final Zoom zoom;

    /** Stroke to draw beams */
    private final Stroke beamStroke = new BasicStroke(8f);

    /** Stroke to draw stems */
    private final Stroke stemStroke = new BasicStroke(2f);

    /** Stroke to draw voices */
    private final Stroke voiceStroke = new BasicStroke(
        6f,
        BasicStroke.CAP_BUTT,
        BasicStroke.JOIN_BEVEL,
        0f,
        new float[] { 20f, 10f },
        0);

    /** Used for icon image transformation */
    private final AffineTransform transform = new AffineTransform();

    /** Color for slot axis */
    private final Color slotColor = new Color(
        0,
        255,
        0,
        constants.slotAlpha.getValue());

    /** Saved graphics set at init-time, to easily start a new System */
    private AffineTransform savedTransform;

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // ScorePainter //
    //--------------//
    /**
     * Creates a new ScorePainter object.
     *
     * @param scoreView the score view with its layout orientation
     * @param initialGraphics the Graphic context, already properly scaled
     * @param zoom the zoom factor (a temporary fix to "descale" for symbols)
     */
    public ScorePainter (ScoreView scoreView,
                         Graphics  initialGraphics,
                         Zoom      zoom)
    {
        this.scoreView = scoreView;
        g = (Graphics2D) initialGraphics.create();
        this.zoom = zoom;

        // Size of default font
        Font  font = g.getFont();
        float fontSize = font.getSize2D();
        g.setFont(font.deriveFont(fontSize / (float) zoom.getRatio()));

        // Anti-aliasing for beams especially
        g.setRenderingHint(
            RenderingHints.KEY_ANTIALIASING,
            RenderingHints.VALUE_ANTIALIAS_ON);

        // Save the transform
        savedTransform = g.getTransform();
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
        final ScoreSystem system = measure.getSystem();
        final SystemView  systemView = scoreView.getSystemView(system);
        final Point       origin = systemView.getDisplayOrigin();

        // Now use system topLeft as the origin
        g.setTransform(savedTransform);
        g.translate(origin.x, origin.y);

        drawSlot(wholeSystem, measure, slot, color);
    }

    //--------------------//
    // visit Articulation //
    //--------------------//
    @Override
    public boolean visit (Articulation articulation)
    {
        return visit((MeasureElement) articulation);
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
        final double          height = bot - top + 1;
        final SymbolIcon      icon = (SymbolIcon) Shape.ARPEGGIATO.getIcon();

        if (icon != null) {
            // Vertical ratio to extend the icon */
            final double ratio = height / icon.getIconHeight();

            g.setColor(Color.black);
            transform.setTransform(
                1 / zoom.getRatio(),
                0,
                0,
                ratio,
                arpeggiate.getReferencePoint().x,
                top);

            g.drawRenderedImage(icon.getImage(), transform);
        }

        return true;
    }

    //---------------//
    // visit Barline //
    //---------------//
    @Override
    public boolean visit (Barline barline)
    {
        try {
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
        } catch (Exception ex) {
            // We do nothing
            logger.warning("Could not draw Barline", ex);
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
        paintLine(beam.getLeftPoint(), beam.getRightPoint());
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
            paintLine(tail, head);
            g.setStroke(oldStroke);

            // Flags ?
            final int fn = chord.getFlagsNumber();

            if (fn != 0) {
                Shape             shape;
                final SystemPoint center = new SystemPoint(tail);

                if (tail.y < head.y) { // Flags down
                    shape = Shape.values()[(COMBINING_FLAG_1.ordinal() + fn) -
                            1];
                    paintSymbol(shape, center, chord, VerticalAlignment.TOP);
                } else { // Flags up
                    shape = Shape.values()[(COMBINING_FLAG_1_UP.ordinal() + fn) -
                            1];
                    paintSymbol(shape, center, chord, VerticalAlignment.BOTTOM);
                }
            }
        }

        // Voice indication ?
        if (PaintingParameters.getInstance()
                              .isVoicePainting()) {
            if (chord.getVoice() != null) {
                // Link to previous chord with same voice
                final Chord prev = chord.getPreviousChordInVoice();

                if (prev != null) {
                    ////logger.info("from " + prev.getHeadLocation() + " to " + chord.getHeadLocation());
                    final Stroke oldStroke = g.getStroke();

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
                final HorizontalAlignment hAlign = (mark.getPosition() == Mark.Position.BEFORE)
                                                   ? HorizontalAlignment.RIGHT
                                                   : HorizontalAlignment.LEFT;
                paintSymbol(
                    mark.getShape(),
                    mark.getLocation(),
                    hAlign,
                    VerticalAlignment.CENTER);

                if (mark.getData() != null) {
                    g.setColor(Color.RED);

                    Point topLeft = topLeftOf(
                        mark.getLocation(),
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
        try {
            if (keySignature.getPitchPosition() != null) {
                paintSymbol(
                    keySignature.getShape(),
                    keySignature.getCenter(),
                    keySignature.getStaff(),
                    keySignature.getPitchPosition());
            }
        } catch (Exception ex) {
            keySignature.addError("Cannot paint keySignature");
        }

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        ///logger.info("Visiting " + measure);
        final SystemPart part = measure.getPart();
        final Color      oldColor = g.getColor();

        if (measure.isDummy()) {
            // Draw left side
            for (TreeNode node : measure.getPart()
                                        .getStaves()) {
                final Staff staff = (Staff) node;
                paintSymbol(
                    Shape.THIN_BARLINE,
                    new SystemPoint(measure.getLeftX(), 0),
                    staff,
                    0);
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
                    (measure.getLeftX()) - 5,
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

    //----------------------//
    // visit MeasureElement //
    //----------------------//
    @Override
    public boolean visit (MeasureElement measureElement)
    {
        if (measureElement.getShape() != null) {
            paintSymbol(
                measureElement.getShape(),
                measureElement.getReferencePoint());
        }

        return true;
    }

    //------------//
    // visit Note //
    //------------//
    @Override
    public boolean visit (Note note)
    {
        final Staff       staff = note.getStaff();
        final Chord       chord = note.getChord();
        final Glyph       stem = chord.getStem();
        final Shape       shape = note.getShape();
        final int         pitch = (int) Math.rint(note.getPitchPosition());
        final SystemPoint center = note.getCenter();
        Shape             displayShape; // What is really displayed

        if (stem != null) {
            // Note is attached to a stem, link the note display to the stem one
            if (ShapeRange.HeadAndFlags.contains(shape)) {
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

            paintSymbol(displayShape, center);
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
            // TODO: TO BE IMPLEMENTED
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

            // Left side of the ledger (on staff external line)
            final SystemPoint left = new SystemPoint(
                center.x - halfLedger,
                (pitch < 0) ? staff.getTopLeft().y
                                : (staff.getTopLeft().y + STAFF_HEIGHT));

            // Right side of the ledger (on staff external line)
            final SystemPoint right = new SystemPoint(
                left.x + (2 * halfLedger),
                left.y);
            final int         sign = Integer.signum(pitch);

            // We draw ledgers until we reach absolute target note pitch
            for (int p = 6; p <= (pitch * sign); p = p + 2) {
                left.y += (INTER_LINE * sign);
                right.y += (INTER_LINE * sign);
                paintLine(left, right);
            }
        }

        return true;
    }

    //----------------//
    // visit Ornament //
    //----------------//
    @Override
    public boolean visit (Ornament ornament)
    {
        return visit((MeasureElement) ornament);
    }

    //-------------//
    // visit Pedal //
    //-------------//
    @Override
    public boolean visit (Pedal pedal)
    {
        return visit((MeasureElement) pedal);
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

    //-------------//
    // visit Segno //
    //-------------//
    @Override
    public boolean visit (Segno segno)
    {
        return visit((MeasureElement) segno);
    }

    //------------//
    // visit Slur //
    //------------//
    @Override
    public boolean visit (Slur slur)
    {
        g.draw(slur.getCurve());

        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    @Override
    public boolean visit (Staff staff)
    {
        final Color oldColor = g.getColor();

        try {
            int topY = staff.getTopLeft().y;

            if (staff.isDummy()) {
                g.setColor(Color.LIGHT_GRAY);
            } else {
                g.setColor(Color.black);
            }

            // Draw the staff lines
            for (int i = 0; i < LINE_NB; i++) {
                // Y of this staff line
                final int y = topY + (i * INTER_LINE);
                g.drawLine(0, y, staff.getWidth(), y);
            }
        } catch (Exception ex) {
            logger.warning("Cannot paint " + staff);
        }

        g.setColor(oldColor);

        return true;
    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        SystemView systemView = scoreView.getSystemView(system);

        if (systemView == null) {
            return false;
        }

        // Check that displayOrigin has been set, otherwise there is no point
        // in displaying the system, which is currently being built and which
        // will later use ScoreFixer to assign proper displayOrigin
        if (systemView.getDisplayOrigin() == null) {
            return false;
        }

        // Restore saved transform
        g.setTransform(savedTransform);

        final UnitDimension dim = system.getDimension();
        final Point         origin = systemView.getDisplayOrigin();

        // Check whether our system is impacted
        final Rectangle systemRect = system.getDisplayContour(); // (We get a copy)

        if ((origin == null) || (systemRect == null)) { // Safer

            return false;
        }

        systemRect.translate(origin.x, origin.y);

        if (!systemRect.intersects(g.getClipBounds())) {
            return false;
        }

        final Color oldColor = g.getColor();
        g.setColor(Color.lightGray);

        // Write system # at the top of the display (if horizontal layout)
        // and at the left of the display (if vertical layout)
        if (systemView.getOrientation() == ScoreOrientation.HORIZONTAL) {
            g.drawString("S" + system.getId(), origin.x, 24);
        } else {
            g.drawString("S" + system.getId(), 0, origin.y);
        }

        // Now use system topLeft as the origin
        g.translate(origin.x, origin.y);

        // Draw the system left edge
        g.drawLine(0, 0, 0, dim.height + STAFF_HEIGHT);

        // Draw the system right edge
        g.drawLine(dim.width, 0, dim.width, dim.height + STAFF_HEIGHT);
        g.setColor(oldColor);

        return true;
    }

    //------------------//
    // visit SystemPart //
    //------------------//
    @Override
    public boolean visit (SystemPart part)
    {
        // Should we draw dummy parts?
        if (part.isDummy() &&
            !PaintingParameters.getInstance()
                               .isDummyPainting()) {
            return false;
        }

        // Draw a brace if there is more than one stave in the part
        if (part.getStaves()
                .size() > 1) {
            // Top & bottom of brace to draw
            final int        top = part.getFirstStaff()
                                       .getTopLeft().y;
            final int        bot = part.getLastStaff()
                                       .getTopLeft().y + STAFF_HEIGHT;
            final double     height = bot - top + 1;

            // Vertical ratio to extend the icon */
            final SymbolIcon braceIcon = (SymbolIcon) Shape.BRACE.getIcon();

            if (braceIcon != null) {
                final double ratio = height / braceIcon.getIconHeight();

                // Offset on left of system
                final int dx = 10;
                g.setColor(Color.black);
                transform.setTransform(
                    1 / zoom.getRatio(),
                    0,
                    0,
                    ratio,
                    -dx,
                    top);
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

    //------------//
    // visit Text //
    //------------//
    @Override
    public boolean visit (Text text)
    {
        Color oldColor = g.getColor();
        Font  oldFont = g.getFont();

        // Text can be outside the boundaries of a system

        // Don't ask me why we need this ratio for display
        Font font = text.getFont()
                        .deriveFont(
            text.getFontSize() * TextInfo.FONT_DISPLAY_RATIO);
        g.setFont(font);

        // Special color for text with unknown role
        if (text instanceof Text.DefaultText) {
            g.setColor(Color.GRAY);
        } else {
            g.setColor(Color.BLUE);
        }

        // Force y alignment for items of the same sentence
        Sentence sentence = text.getSentence();
        int      y = sentence.getLocation().y;

        g.drawString(text.getContent(), text.getReferencePoint().x, y);

        g.setFont(oldFont);
        g.setColor(oldColor);

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
                if (shape == NO_LEGAL_TIME) {
                    // If this is an illegal shape, do not draw anything.
                    // TODO: we could draw a special sign for this
                } else if (shape == CUSTOM_TIME_SIGNATURE) {
                    // A custom shape, use every digit shape
                    final SystemPoint center = timeSignature.getCenter();
                    final Staff       staff = part.getStaffAt(center);

                    // Numerator
                    paintShapeSequence(
                        timeSignature.getNumeratorShapes(),
                        center,
                        staff,
                        -2);

                    // Denominator
                    paintShapeSequence(
                        timeSignature.getDenominatorShapes(),
                        center,
                        staff,
                        +2);
                } else if (ShapeRange.FullTimes.contains(shape)) {
                    // It is a complete (one-symbol) time signature
                    paintSymbol(shape, timeSignature.getCenter());
                }
            } else {
                // Assume a (legal) multi-symbol signature
                for (Glyph glyph : timeSignature.getGlyphs()) {
                    final Shape s = glyph.getShape();

                    if ((s != null) && (s != Shape.GLYPH_PART)) {
                        final SystemPoint center = timeSignature.getCenter();
                        final Staff       staff = part.getStaffAt(center);
                        final int         pitch = (int) Math.rint(
                            staff.pitchPositionOf(center));
                        paintSymbol(s, center, staff, pitch);
                    }
                }
            }
        } catch (InvalidTimeSignature ex) {
            logger.warning("Invalid time signature", ex);
        }

        return true;
    }

    //--------------//
    // visit Tuplet //
    //--------------//
    @Override
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
            final ScoreSystem     system = wedge.getSystem();
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

            paintLine(single, top);
            paintLine(single, bot);
        }

        return true;
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

        final int           x = slot.getX();
        final UnitDimension systemDimension = measure.getSystem()
                                                     .getDimension();

        if (wholeSystem) {
            // Draw for the system height
            g.drawLine(x, 0, x, systemDimension.height + STAFF_HEIGHT);
        } else {
            // Draw for the part height
            g.drawLine(
                x,
                measure.getPart()
                       .getFirstStaff()
                       .getTopLeft().y,
                x,
                measure.getPart().getLastStaff().getTopLeft().y + STAFF_HEIGHT);
        }

        g.setColor(oldColor);
    }

    //-----------//
    // paintLine //
    //-----------//
    /**
     * Draw a line from one SystemPoint to another SystemPoint within their
     * containing system.
     *
     * @param from first point
     * @param to second point
     */
    private void paintLine (SystemPoint from,
                            SystemPoint to)
    {
        if ((from != null) && (to != null)) {
            g.drawLine(from.x, from.y, to.x, to.y);
        } else {
            logger.warning("line not painted due to null reference");
        }
    }

    //--------------------//
    // paintShapeSequence //
    //--------------------//
    /**
     * Paint a line of shapes, the sequence being horizontally centered using
     * the provided center and pitch position.
     * This is meant for complex time signatures.
     *
     * @param shapes the sequence of shapes to paint
     * @param center system-based coordinates of bounding center in units (only
     *               abscissa is actually used)
     * @param staff the related staff
     * @param pitchPosition staff-based ordinate in step lines
     */
    private void paintShapeSequence (List<Shape> shapes,
                                     SystemPoint center,
                                     Staff       staff,
                                     double      pitchPosition)
    {
        int n = shapes.size();
        int mid = n / 2;
        int dx = 0;

        for (int i = 0; i < mid; i++) {
            Shape      s = shapes.get(i);
            SymbolIcon icon = (SymbolIcon) s.getIcon();
            dx += icon.getActualWidth();
        }

        if ((n % 2) == 1) {
            Shape      s = shapes.get(mid);
            SymbolIcon icon = (SymbolIcon) s.getIcon();
            dx += (icon.getActualWidth() / 2);
        }

        // Left side of first shape
        SystemPoint start = new SystemPoint(center);
        start.x -= zoom.unscaled(dx);

        // Draw each shape
        for (int i = 0; i < n; i++) {
            Shape            s = shapes.get(i);
            final SymbolIcon icon = (SymbolIcon) s.getIcon();
            int              shift = zoom.unscaled(icon.getActualWidth());
            start.x += (shift / 2);
            paintSymbol(s, start, staff, pitchPosition);
            start.x += (shift / 2);
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
     */
    private void paintSymbol (Shape       shape,
                              SystemPoint point)
    {
        paintSymbol(
            shape,
            point,
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
     * @param hAlign the horizontal alignment wrt the point
     * @param vAlign the vertical alignment wrt the point
     */
    private void paintSymbol (Shape               shape,
                              SystemPoint         point,
                              HorizontalAlignment hAlign,
                              VerticalAlignment   vAlign)
    {
        final SymbolIcon icon = (SymbolIcon) shape.getIcon();

        if (icon != null) {
            try {
                SystemPoint scaledPoint = new SystemPoint(point);
                zoom.scale(scaledPoint);

                final Point topLeft = topLeftOf(
                    scaledPoint,
                    hAlign,
                    icon.getActualWidth(),
                    vAlign,
                    icon.getIconHeight());
                g.scale(1 / zoom.getRatio(), 1 / zoom.getRatio());
                g.drawImage(icon.getImage(), topLeft.x, topLeft.y, null);
            } finally {
                g.scale(zoom.getRatio(), zoom.getRatio());
            }
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
     * @param chord the chord stem to attach the symbol to
     * @param vAlign the vertical alignment wrt the point
     */
    private void paintSymbol (Shape             shape,
                              SystemPoint       center,
                              Chord             chord,
                              VerticalAlignment vAlign)
    {
        final SymbolIcon icon = (SymbolIcon) shape.getIcon();

        if (icon != null) {
            try {
                final Point topLeft = new Point(
                    zoom.scaled(chord.getTailLocation().x),
                    zoom.scaled(center.y));

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

                g.scale(1 / zoom.getRatio(), 1 / zoom.getRatio());
                g.drawImage(icon.getImage(), topLeft.x, topLeft.y, null);
            } finally {
                g.scale(zoom.getRatio(), zoom.getRatio());
            }
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
            try {
                final int dy = Staff.pitchToUnit(pitchPosition);

                // Position of symbol wrt stem
                final int stemX = chord.getTailLocation().x;
                int       iconX = zoom.scaled(stemX);

                if (center.x < stemX) {
                    // Symbol is on left side of stem (-1 is for stem width)
                    iconX -= (icon.getActualWidth() - 1);
                }

                g.scale(1 / zoom.getRatio(), 1 / zoom.getRatio());
                g.drawImage(
                    icon.getImage(),
                    iconX,
                    zoom.scaled(staff.getTopLeft().y + dy) -
                    icon.getCenter().y,
                    null);
            } finally {
                g.scale(zoom.getRatio(), zoom.getRatio());
            }
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
            try {
                final Point topLeft = new Point(
                    zoom.scaled(center.x),
                    zoom.scaled(
                        staff.getTopLeft().y +
                        Staff.pitchToUnit(pitchPosition)));

                // Horizontal alignment
                if (hAlign == HorizontalAlignment.CENTER) {
                    topLeft.x -= (icon.getActualWidth() / 2);
                } else if (hAlign == HorizontalAlignment.RIGHT) {
                    topLeft.x -= icon.getActualWidth();
                }

                // Specific vertical alignment
                topLeft.y -= icon.getCenter().y;

                g.scale(1 / zoom.getRatio(), 1 / zoom.getRatio());
                g.drawImage(icon.getImage(), topLeft.x, topLeft.y, null);
            } catch (Exception ex) {
                logger.warning(
                    "Cannot paint symbol " + shape + " at center " + center,
                    ex);
            } finally {
                g.scale(zoom.getRatio(), zoom.getRatio());
            }
        }
    }

    //-----------//
    // topLeftOf //
    //-----------//
    private Point topLeftOf (SystemPoint         sysPt,
                             HorizontalAlignment hAlign,
                             int                 width,
                             VerticalAlignment   vAlign,
                             int                 height)
    {
        final Point point = new Point(sysPt);

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

        /** dx between note and augmentation dot */
        final Scale.Fraction dotDx = new Scale.Fraction(
            0.5,
            "dx between note and augmentation dot");

        /** Half length of a ledger */
        final Scale.Fraction halfLedgerLength = new Scale.Fraction(
            1,
            "Half length of a ledger");
    }
}
