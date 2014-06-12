//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e g m e n t I n t e r                                    //
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

import omr.sheet.curve.SegmentInfo;

/**
 * Class {@code SegmentInter} represents a line segment (used in wedge or ending).
 *
 * @author Hervé Bitteur
 */
public class SegmentInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    private final SegmentInfo info;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SegmentInter object.
     *
     * @param info    segment information
     * @param impacts assignment details
     */
    public SegmentInter (SegmentInfo info,
                         GradeImpacts impacts)
    {
        super(null, info.getBounds(), Shape.SEGMENT, impacts);

        this.info = info;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // getInfo //
    //---------//
    public SegmentInfo getInfo ()
    {
        return info;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            extends BasicImpacts
    {
        //~ Static fields/initializers -------------------------------------------------------------

        private static final String[] NAMES = new String[]{"dist"};

        private static final double[] WEIGHTS = new double[]{1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double dist)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, dist);
        }
    }
}
