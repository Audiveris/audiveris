//----------------------------------------------------------------------------//
//                                                                            //
//                               B a r l i n e                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score.entity;

import omr.glyph.Glyph;
import omr.glyph.Glyphs;
import omr.glyph.Shape;

import omr.log.Logger;

import omr.score.common.PagePoint;
import omr.score.common.SystemRectangle;
import omr.score.visitor.ScoreVisitor;

import omr.stick.Stick;

import java.awt.*;
import java.util.*;

/**
 * Class <code>Barline</code> encapsulates a logical bar line, that may be
 * composed of several physical components : repeat dots, thin and thick bars.
 *
 * @author Hervé Bitteur
 */
public class Barline
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Barline.class);

    //~ Instance fields --------------------------------------------------------

    /** Precise bar line shape */
    private Shape shape;

    /** Signature of this bar line, as inferred from its components */
    private String signature;

    //~ Constructors -----------------------------------------------------------

    //---------//
    // Barline //
    //---------//
    /**
     * Create a bar line, in a containing measure
     *
     * @param measure the containing measure
     */
    public Barline (Measure measure)
    {
        super(measure);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // getLeftX //
    //----------//
    /**
     * Report the abscissa of the left side of the bar line
     *
     * @return abscissa (in units wrt system top left) of the left side
     */
    public int getLeftX ()
    {
        PagePoint topLeft = getSystem()
                                .getTopLeft();

        for (Glyph glyph : getGlyphs()) {
            if ((glyph.getShape() == Shape.PART_DEFINING_BARLINE) ||
                (glyph.getShape() == Shape.THIN_BARLINE) ||
                (glyph.getShape() == Shape.THICK_BARLINE)) {
                // Beware : Vertical sticks using Horizontal line equation
                Stick stick = (Stick) glyph;
                int   x = stick.getLine()
                               .yAt(getScale()
                                        .toPixelPoint(topLeft).y);

                return getScale()
                           .pixelsToUnits(x) - topLeft.x;
            }
        }

        // No usable stick
        addError("No usable stick to compute barline abscissa");

        return 0;
    }

    //-----------//
    // getRightX //
    //-----------//
    /**
     * Report the abscissa of the right side of the bar line
     *
     * @return abscissa (in units wrt staff top left) of the right side
     */
    public int getRightX ()
    {
        int       right = 0;
        PagePoint topLeft = getSystem()
                                .getTopLeft();

        for (Glyph glyph : getGlyphs()) {
            if (glyph.isBar()) {
                // Beware : Vertical sticks using Horizontal line equation
                Stick stick = (Stick) glyph;
                int   x = stick.getLine()
                               .yAt(getScale()
                                        .toPixelPoint(topLeft).y);

                if (x > right) {
                    right = x;
                }
            }
        }

        return getScale()
                   .pixelsToUnits(right) - topLeft.x;
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

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //------------//
    // forceShape //
    //------------//
    /**
     * Normally, shape should be inferred from the signature of stick
     * combination that compose the bar line, so this method is provided only
     * for the (rare) cases when we want to force the bar line shape.
     *
     * @param shape the forced shape
     */
    public void forceShape (Shape shape)
    {
        this.shape = shape;
    }

    //----------------//
    // joinsAllStaves //
    //----------------//
    /**
     * Check whether all staves are physically connected by the sticks of the
     * barline
     *
     * @param staves the collection of staves to check
     * @return true if the barline touches all staves, false otherwise
     */
    public boolean joinsAllStaves (Collection<Staff> staves)
    {
        // We check that the barline box intersects each staff box
        SystemRectangle barBox = getBox();

        for (Staff staff : staves) {
            if (!barBox.intersects(staff.getBox())) {
                return false;
            }
        }

        return true;
    }

    //-----------//
    // mergeWith //
    //-----------//
    /**
     * Merge into this bar line the components of another bar line
     *
     * @param other the other (merged) stick
     */
    public void mergeWith (Barline other)
    {
        for (Glyph glyph : other.getGlyphs()) {
            addGlyph(glyph);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the bar contour
     *
     * @param g the graphics context
     */
    public void render (Graphics g)
    {
        for (Glyph glyph : getGlyphs()) {
            if (glyph.isBar()) {
                ((Stick) glyph).renderLine(g);
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
            sb.append(" ")
              .append(getShape())
              .append(" center=")
              .append(getCenter())
              .append(" sig=")
              .append(getSignature())
              .append(Glyphs.toString(" glyphs", glyphs));
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
     * @param shape the provided shape
     * @return a sequence of chars
     */
    private String getChars (Shape shape)
    {
        if (shape == null) {
            logger.warning("Barline. getChars() for null shape");

            return null;
        }

        switch (shape) {
        case THICK_BARLINE :
            return "K";

        case THIN_BARLINE :
        case PART_DEFINING_BARLINE :
            return "N";

        case DOUBLE_BARLINE :
            return "NN";

        case FINAL_BARLINE :
            return "NK";

        case REVERSE_FINAL_BARLINE :
            return "KN";

        case LEFT_REPEAT_SIGN :
            return "KNO";

        case RIGHT_REPEAT_SIGN :
            return "ONK";

        case BACK_TO_BACK_REPEAT_SIGN :
            return "ONKNO";

        case DOT :
        case REPEAT_DOTS :
            return "O"; // Capital o (not zero)

        default :
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
            final Measure       measure = (Measure) getParent();
            final ScoreSystem   system = measure.getSystem();
            final StringBuilder sb = new StringBuilder();
            final Staff         staffRef = measure.getPart()
                                                  .getFirstStaff();
            final int           topStaff = staffRef.getPageTopLeft().y -
                                           system.getTopLeft().y;
            final int           botStaff = topStaff + staffRef.getHeight();
            String              last = null; // Last stick

            for (Glyph glyph : getGlyphs()) {
                String chars = getChars(glyph.getShape());

                if (chars != null) {
                    if (chars.equals("O")) {
                        // DOT
                        Staff staff = system.getStaffAt(
                            system.toSystemPoint(glyph.getLocation()));

                        if (staff != staffRef) {
                            continue;
                        }
                    } else {
                        // BAR : Check overlap with staff reference
                        SystemRectangle box = system.toSystemRectangle(
                            glyph.getContourBox());

                        if (Math.max(box.y, topStaff) > Math.min(
                            box.y + box.height,
                            botStaff)) {
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

            if (logger.isFineEnabled()) {
                logger.fine("sig=" + sb);
            }
        }

        return signature;
    }

    //~ Inner Classes ----------------------------------------------------------

    //------------//
    // Signatures //
    //------------//
    private static class Signatures
    {
        //~ Static fields/initializers -----------------------------------------

        public static final Map<String, Shape> map = new HashMap<String, Shape>();

        static {
            map.put("N", Shape.THIN_BARLINE);
            map.put("NN", Shape.DOUBLE_BARLINE);
            map.put("NK", Shape.FINAL_BARLINE);
            map.put("KN", Shape.REVERSE_FINAL_BARLINE);
            map.put("ONK", Shape.RIGHT_REPEAT_SIGN);
            map.put("KNO", Shape.LEFT_REPEAT_SIGN);

            map.put("ONKNO", Shape.BACK_TO_BACK_REPEAT_SIGN);
            map.put("NKNO", Shape.BACK_TO_BACK_REPEAT_SIGN); // For convenience
            map.put("ONKN", Shape.BACK_TO_BACK_REPEAT_SIGN); // For convenience
            map.put("NKN", Shape.BACK_TO_BACK_REPEAT_SIGN); // For convenience
        }
    }
}
