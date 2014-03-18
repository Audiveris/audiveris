//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A b s t r a c t N o t e I n t e r                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.Shape;

import omr.image.ShapeDescriptor;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * Class {@code AbstractNoteInter} is an abstract base for heads and notes interpretations.
 *
 * @author Hervé Bitteur
 */
public class AbstractNoteInter
        extends AbstractInter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    //~ Instance fields ----------------------------------------------------------------------------
    /** Shape template descriptor. */
    protected final ShapeDescriptor descriptor;

    /** Pitch step. */
    protected final int pitch;

    //~ Constructors -------------------------------------------------------------------------------
    //    /**
    //     * Creates a new AbstractNoteInter object.
    //     *
    //     * @param descriptor the shape template descriptor
    //     * @param box        the object bounds
    //     * @param shape      the underlying shape
    //     * @param grade      the inter intrinsic grade
    //     * @param pitch      the note pitch
    //     */
    //    public AbstractNoteInter (ShapeDescriptor descriptor,
    //                              Rectangle box,
    //                              Shape shape,
    //                              double grade,
    //                              int pitch)
    //    {
    //        super(box, shape, grade);
    //
    //        this.descriptor = descriptor;
    //        this.pitch = pitch;
    //    }
    //
    /**
     * Creates a new AbstractNoteInter object.
     *
     * @param descriptor the shape template descriptor
     * @param box        the object bounds
     * @param shape      the underlying shape
     * @param impacts    the grade details
     * @param pitch      the note pitch
     */
    public AbstractNoteInter (ShapeDescriptor descriptor,
                              Rectangle box,
                              Shape shape,
                              GradeImpacts impacts,
                              int pitch)
    {
        super(box, shape, impacts);
        this.descriptor = descriptor;
        this.pitch = pitch;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // getDescriptor //
    //---------------//
    public ShapeDescriptor getDescriptor ()
    {
        return descriptor;
    }

    //----------//
    // getPitch //
    //----------//
    /**
     * @return the pitch
     */
    public int getPitch ()
    {
        return pitch;
    }

    //--------------------//
    // getShrinkHoriRatio //
    //--------------------//
    public static double getShrinkHoriRatio ()
    {
        return constants.shrinkHoriRatio.getValue();
    }

    //--------------------//
    // getShrinkVertRatio //
    //--------------------//
    public static double getShrinkVertRatio ()
    {
        return constants.shrinkVertRatio.getValue();
    }

    //--------//
    // shrink //
    //--------//
    /**
     * Shrink a bit a bounding box when checking for note overlap.
     *
     * @param box the bounding box
     * @return the shrunk box
     */
    public static Rectangle2D shrink (Rectangle box)
    {
        double newWidth = constants.shrinkHoriRatio.getValue() * box.width;
        double newHeight = constants.shrinkVertRatio.getValue() * box.height;

        return new Rectangle2D.Double(
                box.getCenterX() - (newWidth / 2.0),
                box.getCenterY() - (newHeight / 2.0),
                newWidth,
                newHeight);
    }

    //---------------//
    // getCoreBounds //
    //---------------//
    @Override
    public Rectangle2D getCoreBounds ()
    {
        return shrink(getBounds());
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

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        final Constant.Ratio shrinkHoriRatio = new Constant.Ratio(
                0.5,
                "Horizontal shrink ratio to apply when checking note overlap");

        final Constant.Ratio shrinkVertRatio = new Constant.Ratio(
                0.5,
                "Vertical shrink ratio to apply when checking note overlap");
    }
}
