//----------------------------------------------------------------------------//
//                                                                            //
//                          S c o r e P a i n t e r                           //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.score.visitor;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Glyph;
import omr.glyph.Shape;
import static omr.glyph.Shape.*;

import omr.score.Barline;
import omr.score.Beam;
import omr.score.Chord;
import omr.score.Clef;
import omr.score.Dynamics;
import omr.score.KeySignature;
import omr.score.Mark;
import omr.score.Measure;
import omr.score.Note;
import omr.score.Pedal;
import omr.score.Score;
import static omr.score.ScoreConstants.*;
import omr.score.ScoreController;
import omr.score.ScorePoint;
import omr.score.Slot;
import omr.score.Slur;
import omr.score.Staff;
import omr.score.System;
import omr.score.SystemPart;
import omr.score.SystemPoint;
import omr.score.SystemRectangle;
import omr.score.TimeSignature;
import omr.score.UnitDimension;
import omr.score.Wedge;

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

import javax.swing.AbstractAction;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;

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

    //    /** Brace icon */
    //    private static final SymbolIcon braceIcon = IconManager.getInstance()
    //                                                      .loadSymbolIcon("BRACE");

    /** Stroke to draw beams */
    private static final Stroke beamStroke = new BasicStroke(4f);

    /** Stroke to draw stems */
    private static final Stroke stemStroke = new BasicStroke(1f);

    /** Sequence of colors for voices */
    private static Color[] voiceColors = new Color[] {
                                             Color.CYAN, Color.ORANGE,
                                             Color.GREEN, Color.GRAY, Color.PINK,
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

    //-----------------//
    // insertMenuItems //
    //-----------------//
    public static void insertMenuItems (JMenu menu)
    {
        JCheckBoxMenuItem voiceItem = new JCheckBoxMenuItem(new VoiceAction());
        voiceItem.setSelected(constants.voicePainting.getValue());
        menu.add(voiceItem)
            .setToolTipText("Show the different voices in every measure");

        JCheckBoxMenuItem slotItem = new JCheckBoxMenuItem(new SlotAction());
        slotItem.setSelected(constants.slotPainting.getValue());
        menu.add(slotItem)
            .setToolTipText("Show the different slots in every measure");

        JCheckBoxMenuItem markItem = new JCheckBoxMenuItem(new MarkAction());
        markItem.setSelected(constants.markPainting.getValue());
        menu.add(markItem)
            .setToolTipText("Show the different marks in every measure");
    }

    //---------------//
    // visit Barline //
    //---------------//
    @Override
    public boolean visit (Barline barline)
    {
        Shape shape = barline.getShape();

        if (shape != null) {
            // Draw the barline symbol for each stave in the measure
            SystemPart part = barline.getPart();

            for (TreeNode node : part.getStaves()) {
                Staff staff = (Staff) node;
                paintSymbol(shape, barline.getCenter(), staff, 0);
            }
        } else {
            logger.warning("No shape for barline " + this);
        }

        return true;
    }

    //------------//
    // visit Beam //
    //------------//
    @Override
    public boolean visit (Beam beam)
    {
        Stroke oldStroke = g.getStroke();
        Color  oldColor = g.getColor();

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
        Color oldColor = g.getColor();

        // Stem
        if (chord.getStem() != null) {
            Stroke      oldStroke = g.getStroke();
            SystemPoint tail = chord.getTailLocation();
            SystemPoint head = chord.getHeadLocation();
            g.setStroke(stemStroke);
            paintLine(chord.getDisplayOrigin(), tail, head);
            g.setStroke(oldStroke);

            // Flags ?
            int fn = chord.getFlagsNumber();

            if (fn != 0) {
                Shape       shape;
                SystemPoint center = new SystemPoint(tail);

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
                try {
                    g.setColor(voiceColors[chord.getVoice() - 1]);
                } catch (Exception ex) {
                    logger.warning(
                        chord.getContextString() + " " + ex + " voice=" +
                        chord.getVoice());
                }

                // Link to previous chord with same voice
                Chord prev = chord.getPreviousChordInVoice();

                if (prev != null) {
                    paintLine(
                        chord.getDisplayOrigin(),
                        prev.getHeadLocation(),
                        chord.getHeadLocation());
                }
            } else {
                logger.warning("No voice for chord " + chord);
            }
        }

        // Marks ?
        if (constants.markPainting.getValue()) {
            for (Mark mark : chord.getMarks()) {
                HorizontalAlignment hAlign = (mark.getPosition() == Mark.Position.BEFORE)
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
                        mark.getData().toString(),
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

    //----------------//
    // visit Dynamics //
    //----------------//
    public boolean visit (Dynamics dynamics)
    {
        paintSymbol(
            dynamics.getShape(),
            dynamics.getPoint(),
            dynamics.getDisplayOrigin());

        return true;
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
        Color      oldColor = g.getColor();
        SystemPart part = measure.getPart();

        // Write the measure id, on the first staff  of the first part only
        if (part.getId() == 1) {
            ScorePoint staffOrigin = measure.getPart()
                                            .getFirstStaff()
                                            .getDisplayOrigin();
            g.setColor(Color.lightGray);
            g.drawString(
                Integer.toString(measure.getId()),
                zoom.scaled(staffOrigin.x + measure.getLeftX()) - 5,
                zoom.scaled(staffOrigin.y) - 15);
        }

        // Draw slot vertical lines ?
        if (constants.slotPainting.getValue()) {
            g.setColor(slotColor);

            for (Slot slot : measure.getSlots()) {
                // Draw vertical line using slot abscissa
                ScorePoint partOrigin = measure.getPart()
                                               .getFirstStaff()
                                               .getDisplayOrigin();
                int        x = zoom.scaled(partOrigin.x + slot.getX());
                g.drawLine(
                    x,
                    zoom.scaled(partOrigin.y),
                    x,
                    zoom.scaled(
                        measure.getPart().getLastStaff().getDisplayOrigin().y +
                        STAFF_HEIGHT));
            }
        }

        // Flag for erroneous measure ?
        if (measure.isErroneous()) {
            ScorePoint staffOrigin = measure.getPart()
                                            .getFirstStaff()
                                            .getDisplayOrigin();
            g.setColor(Color.red);
            g.drawString(
                "Error",
                zoom.scaled(staffOrigin.x + measure.getLeftX()) + 10,
                zoom.scaled(staffOrigin.y) - 15);
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
        SystemPart  part = note.getPart();
        Staff       staff = note.getStaff();
        Chord       chord = note.getChord();
        Glyph       stem = chord.getStem();
        Shape       shape = note.getShape();
        int         pitch = (int) Math.rint(note.getPitchPosition());
        SystemPoint center = note.getCenter();
        Shape       displayShape; // What is really displayed

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
            SystemPoint dotCenter = new SystemPoint(note.getCenter());
            dotCenter.x += zoom.unscaled(
                displayShape.getIcon().getIconWidth() / 2);
            dotCenter.x += note.getScale()
                               .toUnits(constants.dotDx);

            if ((pitch % 2) == 0) {
                pitch -= 1;
            }

            paintSymbol(Shape.DOT, dotCenter, staff, pitch);
        } else if (chord.getDotsNumber() == 2) {
            // TO BE IMPLEMENTED
        }

        // Accidental ?
        if (note.getAccidental() != null) {
            SystemPoint accidCenter = new SystemPoint(note.getCenter());
            accidCenter.x -= zoom.unscaled(
                displayShape.getIcon().getIconWidth() / 2);
            accidCenter.x -= note.getScale()
                                 .toUnits(constants.accidDx);
            paintSymbol(
                note.getAccidental(),
                accidCenter,
                staff,
                pitch,
                HorizontalAlignment.RIGHT);
        }

        // Ledgers ?
        if (!note.isRest() && (Math.abs(pitch) >= 6)) {
            int         halfLedger = note.getScale()
                                         .toUnits(constants.halfLedgerLength);
            SystemPoint left = new SystemPoint(center.x - halfLedger, 0);

            if (pitch > 0) {
                left.y += STAFF_HEIGHT;
            }

            SystemPoint right = new SystemPoint(
                left.x + (2 * halfLedger),
                left.y);
            int         sign = Integer.signum(pitch);

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
        slur.getArc()
            .draw(g, slur.getDisplayOrigin(), zoom);

        return true;
    }

    //-------------//
    // visit Staff //
    //-------------//
    @Override
    public boolean visit (Staff staff)
    {
        Point origin = staff.getDisplayOrigin();
        g.setColor(Color.black);

        // Draw the staff lines
        for (int i = 0; i < LINE_NB; i++) {
            // Y of this staff line
            int y = zoom.scaled(origin.y + (i * INTER_LINE));
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
        Rectangle clip = g.getClipBounds();
        int       xMargin = INTER_SYSTEM;
        int       systemLeft = system.getRightPosition() + xMargin;
        int       systemRight = system.getDisplayOrigin().x - xMargin;

        if ((zoom.unscaled(clip.x) > systemLeft) ||
            (zoom.unscaled(clip.x + clip.width) < systemRight)) {
            return false;
        } else {
            UnitDimension dimension = system.getDimension();
            Point         origin = system.getDisplayOrigin();
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
            int        top = part.getFirstStaff()
                                 .getDisplayOrigin().y;
            int        bot = part.getLastStaff()
                                 .getDisplayOrigin().y + STAFF_HEIGHT;
            double     height = zoom.scaled(bot - top + 1);

            // Vertical ratio to extend the icon */
            SymbolIcon braceIcon = (SymbolIcon) Shape.BRACE.getIcon();
            double     ratio = height / braceIcon.getIconHeight();

            // Offset on left of system
            int        dx = 10;

            Graphics2D g2 = (Graphics2D) g;
            g.setColor(Color.black);
            transform.setTransform(
                1,
                0,
                0,
                ratio,
                zoom.scaled(part.getSystem()
                                .getDisplayOrigin().x) - dx,
                zoom.scaled(top));
            g2.drawRenderedImage(braceIcon.getImage(), transform);
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
        Shape      shape = timeSignature.getShape();
        SystemPart part = timeSignature.getPart();

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
                Shape s = glyph.getShape();

                if (s != null) {
                    SystemPoint center = timeSignature.computeGlyphCenter(
                        glyph);
                    Staff       staff = part.getStaffAt(center);
                    int         pitch = staff.unitToPitch(center.y);
                    paintSymbol(s, center, staff, pitch);
                }
            }
        }

        return true;
    }

    //-------------//
    // visit Pedal //
    //-------------//
    public boolean visit (Pedal pedal)
    {
        paintSymbol(
            pedal.getShape(),
            pedal.getPoint(),
            pedal.getDisplayOrigin());

        return true;
    }

    //-------------//
    // visit Wedge //
    //-------------//
    @Override
    public boolean visit (Wedge wedge)
    {
        if (wedge.isStart()) {
            System          system = wedge.getSystem();
            SystemRectangle box = system.toSystemRectangle(
                wedge.getGlyph().getContourBox());

            SystemPoint     single;
            SystemPoint     top;
            SystemPoint     bot;

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
        g.drawLine(
            zoom.scaled(displayOrigin.x + from.x),
            zoom.scaled(displayOrigin.y + from.y),
            zoom.scaled(displayOrigin.x + to.x),
            zoom.scaled(displayOrigin.y + to.y));
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
        SymbolIcon icon = (SymbolIcon) shape.getIcon();
        Point      topLeft = toScaledPoint(
            point,
            displayOrigin,
            hAlign,
            icon.getActualWidth(),
            vAlign,
            icon.getIconHeight());
        g.drawImage(icon.getImage(), topLeft.x, topLeft.y, null);
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
        SymbolIcon icon = (SymbolIcon) shape.getIcon();
        Point      topLeft = new Point(
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
        SymbolIcon icon = (SymbolIcon) shape.getIcon();
        ScorePoint displayOrigin = staff.getDisplayOrigin();
        int        dy = staff.pitchToUnit(pitchPosition);

        // Position of symbol wrt stem
        int stemX = chord.getTailLocation().x;
        int iconX = zoom.scaled(displayOrigin.x + stemX);

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
        SymbolIcon icon = (SymbolIcon) shape.getIcon();
        Point      topLeft = new Point(
            zoom.scaled(staff.getDisplayOrigin().x + center.x),
            zoom.scaled(
                staff.getDisplayOrigin().y + staff.pitchToUnit(pitchPosition)));

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

    //----------------//
    // repaintDisplay //
    //----------------//
    private static void repaintDisplay ()
    {
        // Update current score display if any
        Score score = ScoreController.getCurrentScore();

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
        Point point = new Point(
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

    //-----------//
    // Constants //-
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        /** Alpha parameter for slot axis transparency (0 .. 255) */
        Constant.Integer slotAlpha = new Constant.Integer(
            20,
            "Alpha parameter for slot axis transparency (0 .. 255)");

        /** Should the slots be painted */
        Constant.Boolean slotPainting = new Constant.Boolean(
            true,
            "Should the slots be painted");

        /** Should the voices be painted */
        Constant.Boolean voicePainting = new Constant.Boolean(
            false,
            "Should the voices be painted");

        /** Should the marks be painted */
        Constant.Boolean markPainting = new Constant.Boolean(
            true,
            "Should the marks be painted");

        /**
         * dx between note and augmentation dot
         */
        Scale.Fraction dotDx = new Scale.Fraction(
            0.3,
            "dx (in interline fraction) between note and augmentation dot");

        /**
         * dy between note and augmentation dot, when on a line
         */
        Scale.Fraction dotDy = new Scale.Fraction(
            0.3,
            "dy (in interline fraction) between note and augmentation dot " +
            "when on staff line");

        /**
         * dx between accidental and note
         */
        Scale.Fraction accidDx = new Scale.Fraction(
            0.6,
            "dx (in interline fraction) accidental and note");

        /**
         * Half length of a ledger
         */
        Scale.Fraction halfLedgerLength = new Scale.Fraction(
            1,
            "Half length of a ledger (in interline fraction)");
    }

    //------------//
    // MarkAction //
    //------------//
    private static class MarkAction
        extends AbstractAction
    {
        public MarkAction ()
        {
            super("Show score Marks");
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
    private static class SlotAction
        extends AbstractAction
    {
        public SlotAction ()
        {
            super("Show score Slots");
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
    private static class VoiceAction
        extends AbstractAction
    {
        public VoiceAction ()
        {
            super("Show score Voices");
        }

        @Implement(ActionListener.class)
        public void actionPerformed (ActionEvent e)
        {
            JCheckBoxMenuItem item = (JCheckBoxMenuItem) e.getSource();
            constants.voicePainting.setValue(item.isSelected());
            repaintDisplay();
        }
    }
}
