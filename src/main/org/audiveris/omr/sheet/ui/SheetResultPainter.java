//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S h e e t R e s u l t P a i n t e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.sheet.ui;

import static org.audiveris.omr.ui.symbol.Alignment.BOTTOM_CENTER;
import static org.audiveris.omr.ui.symbol.Alignment.BOTTOM_LEFT;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;
import static org.audiveris.omr.util.HorizontalSide.LEFT;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.math.Rational;
import org.audiveris.omr.score.LogicalPart;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.sheet.rhythm.Slot;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sheet.rhythm.Voices;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractNoteInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.BeamStemRelation;
import org.audiveris.omr.sig.relation.ChordArpeggiatoRelation;
import org.audiveris.omr.sig.relation.ChordArticulationRelation;
import org.audiveris.omr.sig.relation.ChordTupletRelation;
import org.audiveris.omr.sig.relation.DoubleDotRelation;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

/**
 * Class <code>SheetResultPainter</code> paints the items resulting from the processing of a
 * sheet.
 * <p>
 * A same default color is used for most of items. If coloredVoice flag is set, all items related to
 * a same voice are painted in a same specific voice-dependent color.
 * <p>
 * Remaining SIG inters, plus additional items such as measure, time slots, etc.
 *
 * @author Hervé Bitteur
 */
public class SheetResultPainter
        extends SheetPainter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetResultPainter.class);

    /** Abscissa offset, in pixels, for annotation near system. */
    protected static final int annotationDx = constants.annotationDx.getValue();

    /** Ordinate offset, in pixels, for annotation near staff or system. */
    protected static final int annotationDy = constants.annotationDy.getValue();

    //~ Instance fields ----------------------------------------------------------------------------

    /** For staff lines. */
    protected Stroke lineStroke;

    /** Painting voices with different colors?. */
    protected final boolean coloredVoices;

    /** Painting staff lines?. */
    protected final boolean linePainting;

    /** Default color. */
    protected final Color defaultColor;

    /** Should we draw annotations?. */
    protected final boolean annotated;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>SheetResultPainter</code> object.
     *
     * @param sheet         the sheet to process
     * @param g             Graphic context
     * @param coloredVoices true for voices with different colors
     * @param linePainting  true for painting staff lines
     * @param annotated     true if annotations are to be drawn
     */
    public SheetResultPainter (Sheet sheet,
                               Graphics g,
                               boolean coloredVoices,
                               boolean linePainting,
                               boolean annotated)
    {
        super(sheet, g);

        this.coloredVoices = coloredVoices;
        this.linePainting = linePainting;
        this.annotated = annotated;

        defaultColor = g.getColor();

        // Default font for annotations
        g.setFont(basicFont);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //----------//
    // drawSlot //
    //----------//
    /**
     * Draw a time slot in the score display.
     *
     * @param slot  the slot to draw
     * @param color the color to use in drawing
     */
    public void drawSlot (Slot slot,
                          Color color)
    {
        final MeasureStack stack = slot.getStack();
        final SystemInfo system = stack.getSystem();
        final Color oldColor = g.getColor();
        final Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1);
        g.setColor(color);

        try {
            // Draw slot for the whole system height
            int xOffset = slot.getXOffset();
            Staff topStaff = system.getFirstStaff();
            int xTop = xOffset + stack.getMeasureAt(topStaff).getAbscissa(LEFT, topStaff);
            Point2D top = new Point2D.Double(xTop, topStaff.getFirstLine().yAt(xTop));

            Staff botStaff = system.getLastStaff();
            int xBot = xOffset + stack.getMeasureAt(botStaff).getAbscissa(LEFT, botStaff);
            Point2D bot = new Point2D.Double(xBot, botStaff.getLastLine().yAt(xBot));

            g.draw(new Line2D.Double(top, bot));

            // Draw slot time offset (with a maximum font size)
            Rational slotTimeOffset = slot.getTimeOffset();

            if (slotTimeOffset != null) {
                TextLayout layout;
                double zoom = g.getTransform().getScaleX();

                if (zoom <= 2) {
                    layout = basicLayout(slotTimeOffset.toString(), halfAT);
                } else {
                    AffineTransform at = AffineTransform.getScaleInstance(1 / zoom, 1 / zoom);
                    layout = basicLayout(slotTimeOffset.toString(), at);
                }

                Point topInt = PointUtil.rounded(top);
                topInt.translate(0, -annotationDy);
                paint(layout, topInt, BOTTOM_CENTER);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getSimpleName() + " Error drawing " + slot, ex);
        }

        g.setColor(oldColor);
        g.setStroke(oldStroke);
    }

    //---------------//
    // getSigPainter //
    //---------------//
    @Override
    protected SigPainter getSigPainter ()
    {
        return new ResultSigPainter();
    }

    //---------------//
    // highlightSlot //
    //---------------//
    /**
     * Highlight a slot with its related chords (stem / note-head)
     *
     * @param slot the slot to highlight
     */
    public void highlightSlot (Slot slot)
    {
        sigPainter = getSigPainter();

        final SIGraph sig = slot.getStack().getSystem().getSig();
        final Color oldColor = g.getColor();
        g.setColor(Colors.SLOT_CURRENT);

        // Draw the slot components
        for (AbstractChordInter chord : slot.getChords()) {
            // Paint chord stem & flags if any
            StemInter stem = chord.getStem();

            if (stem != null) {
                stem.accept(sigPainter);

                // Flags?
                for (Relation fRel : sig.getRelations(stem, FlagStemRelation.class)) {
                    sig.getOppositeInter(stem, fRel).accept(sigPainter);
                }

                // Beams?
                for (Relation bRel : sig.getRelations(stem, BeamStemRelation.class)) {
                    sig.getOppositeInter(stem, bRel).accept(sigPainter);
                }
            }

            // Related tuplet, if any
            for (Relation tRel : sig.getRelations(chord, ChordTupletRelation.class)) {
                sig.getOppositeInter(chord, tRel).accept(sigPainter);
            }

            sigPainter.visit(chord);

            for (Inter inter : chord.getNotes()) {
                AbstractNoteInter note = (AbstractNoteInter) inter;
                note.accept(sigPainter);

                // Paint note related stuff: alteration if any
                for (Relation accidRel : sig.getRelations(note, AlterHeadRelation.class)) {
                    sig.getOppositeInter(note, accidRel).accept(sigPainter);
                }

                // Paint note related stuff: augmentation dot(s) if any
                for (Relation augRel : sig.getRelations(note, AugmentationRelation.class)) {
                    Inter dot = sig.getOppositeInter(note, augRel);
                    dot.accept(sigPainter);

                    // Any second dot?
                    for (Relation ddRel : sig.getRelations(dot, DoubleDotRelation.class)) {
                        sig.getOppositeInter(dot, ddRel).accept(sigPainter);
                    }
                }
            }

            // Paint chord related stuff: articulation & arpeggiato if any
            for (Relation aRel : sig.getRelations(
                    chord,
                    ChordArticulationRelation.class,
                    ChordArpeggiatoRelation.class)) {
                sig.getOppositeInter(chord, aRel).accept(sigPainter);
            }
        }

        // Highlight the vertical slot line
        drawSlot(slot, Colors.SLOT_CURRENT);
        g.setColor(oldColor);
    }

    //--------------//
    // processParts //
    //--------------//
    /**
     * For every part in provided system, display the corresponding logical part.
     *
     * @param system the containing system
     */
    private void processParts (SystemInfo system)
    {

        final int partDx = constants.partDx.getValue();
        final int partDy = constants.partDy.getValue();

        final double zoom = constants.zoomForLogicalPart.getValue();
        final AffineTransform fat = AffineTransform.getScaleInstance(zoom, zoom);
        final int x = system.getBounds().x;

        for (Part part : system.getParts()) {
            final LogicalPart logical = part.getLogicalPart();

            if (logical != null) {
                final String fullName = logical.getFullName();

                if (fullName != null) {
                    final Staff s1 = part.getFirstStaff();
                    final LineInfo l1 = s1.getMidLine();
                    int y = l1.yAt(x + partDx) + partDy;

                    final Staff s2 = part.getLastStaff();
                    if (s2 != s1) {
                        final LineInfo l2 = s2.getMidLine();
                        y = (y + l2.yAt(x + partDx) + partDy) / 2;
                    }

                    final TextLayout layout = basicLayout(fullName, fat);
                    ///logger.info("{} {}", fullName, layout.getBounds());
                    paint(layout, new Point(x + partDx, y), BOTTOM_LEFT);
                }
            }
        }
    }

    //--------------//
    // processStack //
    //--------------//
    /**
     * Annotate measure stack with measure id and draw stack slots if so desired.
     *
     * @param stack the measure stack to process
     */
    private void processStack (MeasureStack stack)
    {
        if (annotated) {
            final Color oldColor = g.getColor();

            // Write the score-based measure id, on first real part only
            String mid = stack.getPageId();

            g.setColor(Colors.ANNOTATION);

            // Work with top non-dummy staff & measure
            SystemInfo system = stack.getSystem();
            Staff staff = system.getFirstStaff();
            Part topRealPart = staff.getPart();
            int stackIndex = system.getStacks().indexOf(stack);
            Measure topRealMeasure = topRealPart.getMeasures().get(stackIndex);
            int left = topRealMeasure.getAbscissa(HorizontalSide.LEFT, staff);
            Point loc = new Point(left, staff.getFirstLine().yAt(left) - annotationDy);
            paint(basicLayout(mid, null), loc, BOTTOM_CENTER);

            // Draw slot vertical lines ?
            if (viewParams.isSlotPainting() && (stack.getSlots() != null)) {
                for (Slot slot : stack.getSlots()) {
                    drawSlot(slot, Colors.SLOT);
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
            g.setColor(oldColor);
        }
    }

    //---------------//
    // processSystem //
    //---------------//
    @Override
    protected void processSystem (SystemInfo system)
    {
        g.setColor(defaultColor);

        // Staff lines
        if (linePainting) {
            Scale scale = system.getSheet().getScale();

            if (scale != null) {
                lineStroke = new BasicStroke(
                        sheet.getScale().getFore(),
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND);
                g.setStroke(lineStroke);
            }
        } else {
            UIUtil.setAbsoluteStroke(g, 1f);
        }

        for (Staff staff : system.getStaves()) {
            staff.render(g);
        }

        // All inters
        super.processSystem(system);

        if (annotated) {
            Color oldColor = g.getColor();
            g.setColor(Colors.ANNOTATION);

            // System id annotation
            Point ul = new Point(
                    system.getBounds().x,
                    system.getTop() + (system.getDeltaY() / 2) + sheet.getScale().getInterline());

            paint(
                    basicLayout("S#" + system.getId(), null),
                    new Point(ul.x + annotationDx, ul.y + annotationDy),
                    TOP_LEFT);

            // Information for every part
            processParts(system);

            g.setColor(oldColor);
        }

        g.setColor(defaultColor);

        // Additional stuff
        for (MeasureStack stack : system.getStacks()) {
            processStack(stack);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer annotationDx = new Constant.Integer(
                "pixels",
                15,
                "Abscissa offset for annotation near system");

        private final Constant.Integer annotationDy = new Constant.Integer(
                "pixels",
                15,
                "Ordinate offset for annotation near system");

        private final Constant.Integer partDx = new Constant.Integer(
                "pixels",
                50,
                "Abscissa left offset for logical part annotation");

        private final Constant.Integer partDy = new Constant.Integer(
                "pixels",
                -20,
                "Ordinate down offset for logical part annotation");

        private final Constant.Ratio zoomForLogicalPart = new Constant.Ratio(
                0.6,
                "Zoom applied on logical part names");
    }

    //------------------//
    // PdfResultPainter //
    //------------------//
    public static class PdfResultPainter
            implements SimpleSheetPainter
    {

        @Override
        public void paint (Sheet sheet,
                           Graphics2D g)
        {
            SheetResultPainter painter = new SheetResultPainter(
                    sheet,
                    g,
                    false, // No voice painting
                    true, // Paint staff lines
                    false); // No annotations
            painter.process();
        }
    }

    //------------------//
    // ResultSigPainter //
    //------------------//
    private class ResultSigPainter
            extends SigPainter
    {

        @Override
        protected void setColor (Inter inter)
        {
            final Voice voice = inter.getVoice();
            Color c;

            if (coloredVoices && (voice != null)) {
                c = Voices.colorOf(voice);
            } else {
                c = defaultColor;
            }

            if (inter.isImplicit()) {
                c = new Color(c.getRed(), c.getGreen(), c.getBlue(), Colors.IMPLICIT_ALPHA);
            }

            g.setColor(c);
        }

        @Override
        protected boolean splitMirrors ()
        {
            return coloredVoices;
        }
    }
}
