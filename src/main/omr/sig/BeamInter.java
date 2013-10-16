//----------------------------------------------------------------------------//
//                                                                            //
//                              B e a m I n t e r                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.glyph.Shape;
import omr.glyph.facets.Glyph;

import omr.math.AreaUtil;

import omr.util.VerticalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.geom.Area;
import java.awt.geom.Line2D;

/**
 * Class {@code BeamInter} represents a beam interpretation.
 *
 * @author Hervé Bitteur
 */
public class BeamInter
        extends BasicInter
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            BeamInter.class);

    //~ Instance fields --------------------------------------------------------
    /** Median line. */
    private final Line2D median;

    /** Beam height. */
    private final double height;

    /** Beam precise area. */
    private final Area area;

    //~ Constructors -----------------------------------------------------------
    /**
     * Creates a new BeamInter object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the grade details
     * @param median  median beam line
     * @param height  beam height
     */
    public BeamInter (Glyph glyph,
                      Impacts impacts,
                      Line2D median,
                      double height)
    {
        super(glyph, Shape.BEAM, impacts.computeGrade());
        setImpacts(impacts);

        this.median = median;
        this.height = height;

        area = AreaUtil.horizontalParallelogram(
                median.getP1(),
                median.getP2(),
                height);

        // Define precise bounds based on this path
        setBounds(area.getBounds());
    }

    //~ Methods ----------------------------------------------------------------
    @Override
    public boolean isGood ()
    {
        return grade >= 0.5; //TODO: use constant
    }
    
    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //---------//
    // getArea //
    //---------//
    /**
     * Report the precise defining area.
     *
     * @return the inter area
     */
    public Area getArea ()
    {
        return area;
    }

    //-----------//
    // getBorder //
    //-----------//
    /**
     * Report the beam border line on desired side
     *
     * @param side the desired side
     * @return the beam border line on desired side
     */
    public Line2D getBorder (VerticalSide side)
    {
        final double dy = (side == VerticalSide.TOP) ? (-height / 2) : (height / 2);

        return new Line2D.Double(
                median.getX1(),
                median.getY1() + dy,
                median.getX2(),
                median.getY2() + dy);
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * @return the height
     */
    public double getHeight ()
    {
        return height;
    }

    //-----------//
    // getMedian //
    //-----------//
    /**
     * Report the median line
     *
     * @return the beam median line
     */
    public Line2D getMedian ()
    {
        return median;
    }

    //~ Inner Classes ----------------------------------------------------------
    //---------//
    // Impacts //
    //---------//
    public static class Impacts
            implements GradeImpacts
    {
        //~ Instance fields ----------------------------------------------------

        /** width impact. */
        final double width;

        /** core ratio impact. */
        final double core;

        /** belt ratio impact. */
        final double belt;

        //~ Constructors -------------------------------------------------------
        public Impacts (double width,
                        double core,
                        double belt)
        {
            this.width = width;
            this.core = core;
            this.belt = belt;
        }

        //~ Methods ------------------------------------------------------------
        @Override
        public double computeGrade ()
        {
            return (width + core + belt) / 3;
        }

        @Override
        public String toString ()
        {
            return String.format(
                    "width:%.2f core:%.2f belt:%.2f",
                    width,
                    core,
                    belt);
        }
    }
}
