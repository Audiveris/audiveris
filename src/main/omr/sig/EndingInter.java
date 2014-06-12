//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      E n d i n g I n t e r                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;

import java.awt.Rectangle;
import java.awt.geom.Line2D;

/**
 * Class {@code EndingInter} represents an ending.
 *
 * @author Hervé Bitteur
 */
public class EndingInter
    extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final SegmentInter segment;
    private final Line2D       line;

    // Mandatory left leg
    private final Line2D leftLeg;

    // Optional right leg
    private final Line2D rightLeg;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new EndingInter object.
     *
     * @param segment  horizontal segment
     * @param line     precise line
     * @param leftLeg  mandatory left leg
     * @param rightLeg optional right leg
     * @param bounds   bounding box
     * @param impacts  assignments details
     */
    public EndingInter (SegmentInter segment,
                        Line2D       line,
                        Line2D       leftLeg,
                        Line2D       rightLeg,
                        Rectangle    bounds,
                        GradeImpacts impacts)
    {
        super(null, bounds, Shape.ENDING, impacts);
        this.segment = segment;
        this.line = line;
        this.leftLeg = leftLeg;
        this.rightLeg = rightLeg;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    /**
     * @return the leftLeg
     */
    public Line2D getLeftLeg ()
    {
        return leftLeg;
    }

    /**
     * @return the line
     */
    public Line2D getLine ()
    {
        return line;
    }

    /**
     * @return the rightLeg
     */
    public Line2D getRightLeg ()
    {
        return rightLeg;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //---------//
    // Impacts //
    //---------//
    public static class Impacts
        extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[] {
                                                  "straight", "slope", "length", "leftBar",
                                                  "rightBar"
                                              };
        private static final double[] WEIGHTS = new double[] { 1, 1, 1, 1, 1 };

        //~ Constructors ---------------------------------------------------------------------------

        public Impacts (double straight,
                        double slope,
                        double length,
                        double leftBar,
                        double rightBar)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, straight);
            setImpact(1, slope);
            setImpact(2, length);
            setImpact(3, leftBar);
            setImpact(4, rightBar);
        }
    }
}
