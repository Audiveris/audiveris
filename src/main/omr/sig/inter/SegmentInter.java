//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     S e g m e n t I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.inter;

import omr.glyph.Shape;

import omr.sheet.curve.SegmentInfo;

import omr.sig.BasicImpacts;
import omr.sig.GradeImpacts;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code SegmentInter} represents a line segment (used in wedge or ending).
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "segment")
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

    /**
     * No-arg constructor meant for JAXB.
     */
    private SegmentInter ()
    {
        this.info = null;
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
