//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         B a r l i n e                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.run.Orientation;

import omr.score.visitor.ScoreVisitor;

import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code OldBarline} represents a logical bar line for a part, that is composed
 * of several {@link StaffBarline} instances when the part comprises several staves.
 * <p>
 * In the case of "back to back" repeat configuration, we use two instances of this class, one
 * for the backward repeat and one for the forward repeat.
 *
 * @author Hervé Bitteur
 */
public class OldBarline
        extends PartNode
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(OldBarline.class);

    /** Predicate to detect a barline glyph (not a repeat dot) */
    public static final Predicate<Glyph> linePredicate = new Predicate<Glyph>()
    {
        @Override
        public boolean check (Glyph glyph)
        {
            Shape shape = glyph.getShape();

            return (shape == Shape.THIN_BARLINE) || (shape == Shape.THICK_BARLINE);
        }
    };

    //~ Enumerations -------------------------------------------------------------------------------
    /**
     * OldBarline style.
     * Identical to (or subset of) MusicXML BarStyle, to avoid strict dependency on MusicXML.
     */
    public static enum Style
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        REGULAR,
        DOTTED,
        DASHED,
        HEAVY,
        LIGHT_LIGHT,
        LIGHT_HEAVY,
        HEAVY_LIGHT,
        HEAVY_HEAVY,
        TICK,
        SHORT,
        NONE;
    }

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying {@link StaffBarline} instances, one per staff in the part. */
    private final List<StaffBarline> staffBarlines = new ArrayList<StaffBarline>();

    /** Precise bar line shape. */
    private Shape shape;

    /** Signature of this bar line, as inferred from its components. */
    private String signature;

    //~ Constructors -------------------------------------------------------------------------------
    //---------//
    // OldBarline //
    //---------//
    /**
     * Create a bar line, in a containing measure
     *
     * @param measure the containing measure
     */
    public OldBarline (OldMeasure measure)
    {
        super(measure);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //-----------------//
    // addStaffBarline //
    //-----------------//
    public void addStaffBarline (StaffBarline staffBarline)
    {
        if (staffBarline == null) {
            throw new NullPointerException("Trying to add a null StaffBarline");
        }

        staffBarlines.add(staffBarline);
    }

    //------------//
    // forceShape //
    //------------//
    /**
     * Normally, shape should be inferred from the signature of stick
     * combination that compose the bar line, so this method is provided
     * only for the (rare) cases when we want to force the barline shape
     *
     * @param shape the forced shape
     */
    public void forceShape (Shape shape)
    {
        this.shape = shape;
    }

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the center abscissa of the left bar
     *
     * @return abscissa of the left side
     */
    public int getLeftX ()
    {
        if (!staffBarlines.isEmpty()) {
            return staffBarlines.get(0).getLeftX();
        } else {
            throw new IllegalStateException("Part Barline with no StaffBarline");
        }
    }

    //-----------//
    // getRightX //
    //-----------//
    /**
     * Report the center abscissa of the right bar
     *
     * @return abscissa of the right side
     */
    public int getRightX ()
    {
        if (!staffBarlines.isEmpty()) {
            return staffBarlines.get(0).getRightX();
        } else {
            throw new IllegalStateException("Part Barline with no StaffBarline");
        }
    }

    //----------//
    // getShape //
    //----------//
    /**
     * Report the shape of this bar line
     *
     * @return the (lazily determined) shape
     */
    public Shape getShape ()
    {
        if (shape == null) {
            // Use the map of signatures
            shape = Signatures.map.get(getSignature());
        }

        return shape;
    }

    //----------//
    // getStyle //
    //----------//
    public Style getStyle ()
    {
        throw new RuntimeException("No longer implemented");
//        if (staffBarlines.isEmpty()) {
//            return null;
//        }
//
//        return staffBarlines.get(0).getStyle();
    }

    //-----------//
    // mergeWith //
    //-----------//
    /**
     * Merge into this bar line the components of another bar line
     *
     * @param other the other (merged) stick
     */
    public void mergeWith (OldBarline other)
    {
        for (Glyph glyph : other.getGlyphs()) {
            addGlyph(glyph);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the bar contour, with proper strokes according to the
     * thickness of each barline component
     *
     * @param g the graphics context
     */
    public void render (Graphics2D g)
    {
        Stroke oldStroke = g.getStroke();

        for (Glyph glyph : getGlyphs()) {
            if (glyph.isBar()) {
                float thickness = (float) glyph.getWeight() / glyph.getLength(Orientation.VERTICAL);
                g.setStroke(new BasicStroke(thickness));
                glyph.renderLine(g);
            }
        }

        g.setStroke(oldStroke);
    }

    //------------//
    // renderLine //
    //------------//
    /**
     * Render the axis of each component of the bar
     *
     * @param g the graphics context
     */
    public void renderLine (Graphics2D g)
    {
        for (Glyph glyph : getGlyphs()) {
            if (glyph.isBar()) {
                glyph.renderLine(g);
            }
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when needed
     */
    @Override
    public void reset ()
    {
        super.reset();

        signature = null;
        shape = null;
        staffBarlines.clear();
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a readable description
     *
     * @return a string based on main members
     */
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Barline");

        try {
            sb.append(" ").append(getStyle());
            sb.append(" x=").append(getRightX());
        } catch (NullPointerException e) {
            sb.append(" INVALID");
        }

        sb.append("}");

        return sb.toString();
    }

    //-----------------//
    // translateGlyphs //
    //-----------------//
    /**
     * Make all barline glyphs point to it as the translated entity
     */
    public void translateGlyphs ()
    {
        for (Glyph glyph : getGlyphs()) {
            glyph.setTranslation(this);
        }
    }

    //----------//
    // getChars //
    //----------//
    /**
     * Report the sequence of chars that describes the provided shape
     *
     * @param shape the provided shape
     * @return a sequence of chars
     */
    private String getChars (Shape shape)
    {
        if (shape == null) {
            logger.warn("Barline. getChars() for null shape");

            return null;
        }

        switch (shape) {
        case THICK_BARLINE:
            return "K";

        case THIN_BARLINE:
            return "N";

        case DOUBLE_BARLINE:
            return "NN";

        case FINAL_BARLINE:
            return "NK";

        case REVERSE_FINAL_BARLINE:
            return "KN";

        case LEFT_REPEAT_SIGN:
            return "KNO";

        case RIGHT_REPEAT_SIGN:
            return "ONK";

        case BACK_TO_BACK_REPEAT_SIGN:
            return "ONKNO";

        case DOT_set:
        case REPEAT_DOT:
            return "O"; // Capital o (not zero)

        default:
            addError("Unknown bar component : " + shape);

            return null;
        }
    }

    //--------------//
    // getSignature //
    //--------------//
    /**
     * Compute a signature for this barline, based on the composing sticks.
     * We elaborate this signature for first staff of the part only, to get rid
     * of sticks roughly one above the other
     */
    private String getSignature ()
    {
        if (signature == null) {
            final OldMeasure measure = (OldMeasure) getParent();
            final ScoreSystem system = measure.getSystem();
            final StringBuilder sb = new StringBuilder();
            final OldStaff staffRef = measure.getPart().getFirstStaff();
            final int topStaff = staffRef.getTopLeft().y;
            final int botStaff = topStaff + staffRef.getHeight();
            String last = null; // Last stick

            for (Glyph glyph : getGlyphs()) {
                String chars = getChars(glyph.getShape());

                if (chars != null) {
                    if (chars.equals("O")) {
                        // DOT_set
                        OldStaff staff = system.getStaffAt(glyph.getLocation());

                        if (staff != staffRef) {
                            continue;
                        }
                    } else {
                        // BAR : Check overlap with staff reference
                        Rectangle box = glyph.getBounds();

                        if (Math.max(box.y, topStaff) > Math.min(box.y + box.height, botStaff)) {
                            continue;
                        }
                    }

                    if (last == null) {
                        sb.append(chars);
                    } else {
                        if (last.equals(chars)) {
                            if (chars.equals("N")) {
                                sb.append(chars);
                            }
                        } else {
                            sb.append(chars);
                        }
                    }

                    last = chars;
                }
            }

            signature = sb.toString();
            logger.debug("sig={}", sb);
        }

        return signature;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //------------//
    // Signatures //
    //------------//
    private static class Signatures
    {
        //~ Static fields/initializers -------------------------------------------------------------

        public static final Map<String, Shape> map = new HashMap<String, Shape>();

        static {
            map.put("N", Shape.THIN_BARLINE);
            map.put("NN", Shape.DOUBLE_BARLINE);
            map.put("NK", Shape.FINAL_BARLINE);
            map.put("KN", Shape.REVERSE_FINAL_BARLINE);
            map.put("ONK", Shape.RIGHT_REPEAT_SIGN);
            map.put("KNO", Shape.LEFT_REPEAT_SIGN);
        }

        //~ Constructors ---------------------------------------------------------------------------
        private Signatures ()
        {
        }
    }
}
