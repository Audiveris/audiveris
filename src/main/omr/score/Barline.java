//----------------------------------------------------------------------------//
//                                                                            //
//                               B a r l i n e                                //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.score;

import omr.glyph.Shape;

import omr.lag.Lag;

import omr.score.visitor.ScoreVisitor;

import omr.sheet.Scale;

import omr.stick.Stick;

import omr.ui.view.Zoom;

import omr.util.Logger;

import java.awt.*;
import java.util.*;

/**
 * Class <code>Barline</code> encapsulates a logical bar line, that may be
 * composed of several physical components : repeat dots, thin and thick bars.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Barline
    extends PartNode
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Barline.class);

    /** Map of signature -> bar shape */
    private static Map<String, Shape> sigs;

    //~ Instance fields --------------------------------------------------------

    /** Precise bar line shape */
    private Shape shape;

    /**
     * Related physical sticks (bar sticks and dots), which is kept sorted on
     * stick abscissa
     */
    private SortedSet<Stick> sticks = new TreeSet<Stick>();

    /** Signature of this bar line, as abstracted from its constituents */
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

    //---------//
    // Barline //
    //---------//
    /**
     * Needed for XML binding
     */
    private Barline ()
    {
        super(null);
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

        for (Stick stick : sticks) {
            if ((stick.getShape() == Shape.THICK_BAR_LINE) ||
                (stick.getShape() == Shape.THIN_BAR_LINE)) {
                // Beware : Vertical sticks using Horizontal line equation
                int x = stick.getLine()
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

        for (Stick stick : sticks) {
            if ((stick.getShape() == Shape.THICK_BAR_LINE) ||
                (stick.getShape() == Shape.THIN_BAR_LINE)) {
                // Beware : Vertical sticks using Horizontal line equation
                int x = stick.getLine()
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
            shape = getSignatures()
                        .get(getSignature());
        }

        return shape;
    }

    //-----------//
    // getSticks //
    //-----------//
    /**
     * Report the collection of physical sticks that compose this bar line
     *
     * @return the collection of sticks
     */
    public Collection<Stick> getSticks ()
    {
        return sticks;
    }

    //--------//
    // accept //
    //--------//
    @Override
    public boolean accept (ScoreVisitor visitor)
    {
        return visitor.visit(this);
    }

    //----------//
    // addStick //
    //----------//
    /**
     * Include a new individual bar stick in the (complex) bar line. This
     * automatically invalidates the other bar line parameters, which will be
     * lazily re-computed when needed.
     *
     * @param stick the bar stick to include
     */
    public void addStick (Stick stick)
    {
        sticks.add(stick);

        // Invalidate parameters
        reset();
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
        for (Stick stick : other.sticks) {
            addStick(stick);
        }
    }

    //--------//
    // render //
    //--------//
    /**
     * Render the bar contour
     *
     * @param g the graphics context
     * @param z the display zoom
     */
    public void render (Graphics g,
                        Zoom     z)
    {
        for (Stick stick : sticks) {
            stick.renderLine(g, z);
        }
    }

    //-------//
    // reset //
    //-------//
    /**
     * Invalidate cached data, so that it gets lazily recomputed when needed
     */
    public void reset ()
    {
        signature = null;
        setCenter(null);
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
        sb.append("{Barline")
          .append(" ")
          .append(getShape())
          .append(" center=")
          .append(getCenter())
          .append(" sig=")
          .append(getSignature())
          .append(" sticks[");

        for (Stick stick : sticks) {
            sb.append("#")
              .append(stick.getId());
        }

        sb.append("]");
        sb.append("}");

        return sb.toString();
    }

    //---------------//
    // computeCenter //
    //---------------//
    @Override
    protected void computeCenter ()
    {
        setCenter(computeGlyphsCenter(sticks));
    }

    //-----------//
    // getLetter //
    //-----------//
    private String getLetter (Shape shape)
    {
        switch (shape) {
        case THICK_BAR_LINE :
            return "K";

        case THIN_BAR_LINE :
            return "N";

        case REPEAT_DOTS :
            return "O";

        default :
            addError("Unknown bar component : " + shape);

            return null;
        }
    }

    //--------------//
    // getSignature //
    //--------------//
    private String getSignature ()
    {
        if (signature == null) {
            StringBuilder sb = new StringBuilder();
            String        last = null;

            for (Stick stick : sticks) {
                String letter = getLetter(stick.getShape());

                if (letter == null) {
                    continue;
                }

                if (last == null) {
                    sb.append(letter);
                } else {
                    if (last.equals(letter)) {
                        if (!letter.equals("N")) {
                            // Nothing ?
                        } else {
                            sb.append(letter);
                        }
                    } else {
                        sb.append(letter);
                    }
                }

                last = letter;
            }

            signature = sb.toString();

            if (logger.isFineEnabled()) {
                logger.fine("sig=" + sb);
            }
        }

        return signature;
    }

    //---------------//
    // getSignatures //
    //---------------//
    private static Map<String, Shape> getSignatures ()
    {
        if (sigs == null) {
            sigs = new HashMap<String, Shape>();
            sigs.put("N", Shape.SINGLE_BARLINE);
            sigs.put("NN", Shape.DOUBLE_BARLINE);
            sigs.put("NK", Shape.FINAL_BARLINE);
            sigs.put("KN", Shape.REVERSE_FINAL_BARLINE);
            sigs.put("ONK", Shape.RIGHT_REPEAT_SIGN);
            sigs.put("KNO", Shape.LEFT_REPEAT_SIGN);

            sigs.put("ONKNO", Shape.BACK_TO_BACK_REPEAT_SIGN);
            sigs.put("NKNO", Shape.BACK_TO_BACK_REPEAT_SIGN); // For convenience
            sigs.put("ONKN", Shape.BACK_TO_BACK_REPEAT_SIGN); // For convenience
            sigs.put("NKN", Shape.BACK_TO_BACK_REPEAT_SIGN); // For convenience
        }

        return sigs;
    }
}
