//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S l u r I n t e r                                       //
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

import omr.sheet.curve.SlurInfo;

import java.util.Map.Entry;

/**
 * Class {@code SlurInter} represents a slur interpretation.
 *
 * @author Hervé Bitteur
 */
public class SlurInter
        extends AbstractInter
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Physical characteristics. */
    private final SlurInfo info;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SlurInter object.
     *
     * @param info    the underlying slur information
     * @param impacts the assignment details
     */
    public SlurInter (SlurInfo info,
                      GradeImpacts impacts)
    {
        super(info.getBounds(), Shape.SLUR, impacts);
        this.info = info;

        // To debug attachments
        for (Entry<String, java.awt.Shape> entry : info.getAttachments().entrySet()) {
            addAttachment(entry.getKey(), entry.getValue());
        }
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

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        sb.append(" ").append(info);

        return sb.toString();
    }

    //---------//
    // getInfo //
    //---------//
    public SlurInfo getInfo ()
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

        private static final String[] NAMES = new String[]{
            "dist", "width", "height", "angle", "vert"
        };

        private static final double[] WEIGHTS = new double[]{3, 1, 1, 1, 1};

        //~ Constructors ---------------------------------------------------------------------------
        public Impacts (double dist,
                        double width,
                        double height,
                        double angle,
                        double vert)
        {
            super(NAMES, WEIGHTS);
            setImpact(0, dist);
            setImpact(1, width);
            setImpact(2, height);
            setImpact(3, angle);
            setImpact(4, vert);
        }
    }
}
