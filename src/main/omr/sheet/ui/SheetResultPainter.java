//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                               S h e e t R e s u l t P a i n t e r                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.ui;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.math.PointUtil;
import omr.math.Rational;

import omr.score.ui.PaintingParameters;

import omr.sheet.Part;
import omr.sheet.Scale;
import omr.sheet.Sheet;
import omr.sheet.Staff;
import omr.sheet.SystemInfo;
import omr.sheet.rhythm.Measure;
import omr.sheet.rhythm.MeasureStack;
import omr.sheet.rhythm.Slot;
import omr.sheet.rhythm.Voice;

import omr.sig.SIGraph;
import omr.sig.inter.AbstractChordInter;
import omr.sig.inter.AbstractNoteInter;
import omr.sig.inter.Inter;
import omr.sig.inter.StemInter;
import omr.sig.relation.AccidHeadRelation;
import omr.sig.relation.AugmentationRelation;
import omr.sig.relation.BeamStemRelation;
import omr.sig.relation.DoubleDotRelation;
import omr.sig.relation.FlagStemRelation;
import omr.sig.relation.Relation;
import omr.sig.relation.StaccatoChordRelation;
import omr.sig.ui.SigPainter;

import omr.ui.Colors;
import omr.ui.symbol.Alignment;
import static omr.ui.symbol.Alignment.BOTTOM_CENTER;
import static omr.ui.symbol.Alignment.TOP_LEFT;
import omr.ui.symbol.OmrFont;
import omr.ui.util.Panel;
import omr.ui.util.UIUtil;

import omr.util.HorizontalSide;
import static omr.util.HorizontalSide.LEFT;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Class {@code SheetResultPainter} paints the items resulting from the processing of a
 * sheet.
 * <p>
 * A same default color is used for most of items. If coloredVoice flag is set, all items related to
 * a same voice are painted in a same specific voice-dependent color.
 * <p>
 * Remaining SIG inters, plus additional items such as measure, time slots, etc.
 * TODO: handling of colored voices
 * TODO: painting of Measure ID
 * TODO: painting of time slots
 *
 * @author Hervé Bitteur
 */
public class SheetResultPainter
        extends SheetPainter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SheetResultPainter.class);

    /** Font for annotations. */
    protected static final Font basicFont = new Font(
            "Sans Serif",
            Font.PLAIN,
            constants.basicFontSize.getValue());

    /** A transformation to half scale. (used for slot time annotation) */
    protected static final AffineTransform halfAT = AffineTransform.getScaleInstance(0.5, 0.5);

    /** Abscissa offset, in pixels, for annotation near system. */
    protected static final int annotationDx = 15;

    /** Ordinate offset, in pixels, for annotation near staff or system. */
    protected static final int annotationDy = 15;

    /** Painting parameters. */
    protected static final PaintingParameters parameters = PaintingParameters.getInstance();

    /** Sequence of colors for voices. */
    private static final int alpha = 150;

    private static final Color[] voiceColors = new Color[]{
        /** Cyan */
        new Color(0, 255, 255, alpha),
        /** Orange */
        new Color(255, 200, 0, alpha),
        /** Pink */
        new Color(255, 150, 150, alpha),
        /** Green */
        new Color(0, 255, 0, alpha),
        /** Magenta */
        new Color(255, 0, 255, alpha),
        /** Blue */
        new Color(0, 0, 255, alpha),
        /** Yellow */
        new Color(255, 255, 0, alpha)
    };

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
     * Creates a new {@code SheetResultPainter} object.
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
    //---------------//
    // getVoicePanel //
    //---------------//
    /**
     * Build a panel which displays all defined voice ID colors.
     *
     * @return the populated voice panel
     */
    public static JPanel getVoicePanel ()
    {
        final int length = voiceColors.length;
        final Font font = new Font("SansSerif", Font.BOLD, 22);
        final Color background = new Color(220, 220, 220);
        final FormLayout layout = Panel.makeLabelsLayout(1, length, "0dlu", "10dlu");
        final PanelBuilder builder = new PanelBuilder(layout, new Panel());
        final CellConstraints cst = new CellConstraints();
        final int r = 1;

        for (int c = 1; c <= length; c++) {
            final Color color = new Color(voiceColors[c - 1].getRGB()); // Remove alpha
            final JLabel label = new JLabel("" + c, JLabel.CENTER);
            label.setFont(font);
            label.setOpaque(true);
            label.setBackground(background);
            label.setForeground(color);
            builder.add(label, cst.xy((2 * c) - 1, r));
        }

        return builder.getPanel();
    }

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
        final MeasureStack stack = slot.getMeasureStack();
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

            // Draw slot start time (with a maximum font size)
            Rational slotStartTime = slot.getStartTime();

            if (slotStartTime != null) {
                TextLayout layout;
                double zoom = g.getTransform().getScaleX();

                if (zoom <= 2) {
                    layout = basicLayout(slotStartTime.toString(), halfAT);
                } else {
                    AffineTransform at = AffineTransform.getScaleInstance(1 / zoom, 1 / zoom);
                    layout = basicLayout(slotStartTime.toString(), at);
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

        final SIGraph sig = slot.getMeasureStack().getSystem().getSig();
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

            sigPainter.visit(chord);

            for (Inter inter : chord.getNotes()) {
                AbstractNoteInter note = (AbstractNoteInter) inter;
                note.accept(sigPainter);

                // Paint note related stuff: alteration if any
                for (Relation accidRel : sig.getRelations(note, AccidHeadRelation.class)) {
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

            // Paint chord related stuff: staccato dot(s) if any
            for (Relation sRel : sig.getRelations(chord, StaccatoChordRelation.class)) {
                Inter dot = sig.getOppositeInter(chord, sRel);
                dot.accept(sigPainter);
            }
        }

        // Highlight the vertical slot line
        drawSlot(slot, Colors.SLOT_CURRENT);
        g.setColor(oldColor);
    }

    //-------------//
    // basicLayout //
    //-------------//
    /**
     * Build a TextLayout from a String of BasicFont characters
     * (transformed by the provided AffineTransform if any)
     *
     * @param str the string of proper codes
     * @param fat potential affine transformation
     * @return the (sized) TextLayout ready to be drawn
     */
    protected TextLayout basicLayout (String str,
                                      AffineTransform fat)
    {
        FontRenderContext frc = g.getFontRenderContext();
        Font font = (fat == null) ? basicFont : basicFont.deriveFont(fat);

        return new TextLayout(str, font, frc);
    }

    //---------------//
    // getSigPainter //
    //---------------//
    @Override
    protected SigPainter getSigPainter ()
    {
        return new ResultSigPainter(g, sheet.getScale());
    }

    //-------//
    // paint //
    //-------//
    /**
     * This is the general paint method for drawing a symbol layout, at a specified
     * location, using a specified alignment
     *
     * @param layout    what: the symbol, perhaps transformed
     * @param location  where: the precise location in the display
     * @param alignment how: the way the symbol is aligned wrt the location
     */
    protected void paint (TextLayout layout,
                          Point location,
                          Alignment alignment)
    {
        OmrFont.paint(g, layout, location, alignment);
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
                        (float) sheet.getScale().getMainFore(),
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

        // System id annotation
        if (annotated) {
            Color oldColor = g.getColor();
            g.setColor(Colors.ANNOTATION);

            Point ul = new Point(
                    system.getBounds().x,
                    system.getTop() + (system.getDeltaY() / 2) + sheet.getScale().getInterline());

            paint(
                    basicLayout("S" + system.getId(), null),
                    new Point(ul.x + annotationDx, ul.y + annotationDy),
                    TOP_LEFT);
            g.setColor(oldColor);
        }

        g.setColor(defaultColor);

        // Additional stuff
        for (MeasureStack stack : system.getMeasureStacks()) {
            processStack(stack);
        }
    }

    //---------//
    // colorOf //
    //---------//
    /**
     * Report the color to use when painting elements related to the provided voice
     *
     * @param voice the provided voice
     * @return the color to use
     */
    private Color colorOf (Voice voice)
    {
        if (coloredVoices) {
            // Use table of colors, circularly.
            int index = (voice.getId() - 1) % voiceColors.length;

            return voiceColors[index];
        } else {
            return defaultColor;
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

            if (mid != null) {
                g.setColor(Colors.ANNOTATION);

                // Work with top non-dummy staff & measure
                SystemInfo system = stack.getSystem();
                Staff staff = system.getFirstStaff();
                Part topRealPart = system.getPartOf(staff);
                int stackIndex = system.getMeasureStacks().indexOf(stack);
                Measure topRealMeasure = topRealPart.getMeasures().get(stackIndex);
                int left = topRealMeasure.getAbscissa(HorizontalSide.LEFT, staff);
                Point loc = new Point(left, staff.getFirstLine().yAt(left) - annotationDy);
                paint(basicLayout(mid, null), loc, BOTTOM_CENTER);
            }

            // Draw slot vertical lines ?
            if (parameters.isSlotPainting() && (stack.getSlots() != null)) {
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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Integer basicFontSize = new Constant.Integer(
                "points",
                30,
                "Standard font size for annotations");

        private final Scale.Fraction keySigItemDx = new Scale.Fraction(
                1.1,
                "dx between items in a key signature");
    }

    //------------------//
    // ResultSigPainter //
    //------------------//
    private class ResultSigPainter
            extends SigPainter
    {
        //~ Constructors ---------------------------------------------------------------------------

        public ResultSigPainter (Graphics g,
                                 Scale scale)
        {
            super(g, scale);
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        protected void setColor (Inter inter)
        {
            if (coloredVoices) {
                final Voice voice = inter.getVoice();

                if (voice != null) {
                    g.setColor(colorOf(voice));
                } else {
                    g.setColor(defaultColor);
                }
            } else {
                super.setColor(inter);
            }
        }
    }
}
