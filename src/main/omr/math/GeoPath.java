//----------------------------------------------------------------------------//
//                                                                            //
//                               G e o P a t h                                //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.math;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import static java.awt.geom.PathIterator.*;

/**
 * Class {@code GeoPath} is a Path2D.Double with some additions
 *
 * @author Hervé Bitteur
 */
public class GeoPath
    extends Path2D.Double
{
    //~ Constructors -----------------------------------------------------------

    //---------//
    // GeoPath //
    //---------//
    /**
     * Creates a new GeoPath object.
     */
    public GeoPath ()
    {
    }

    //---------//
    // GeoPath //
    //---------//
    /**
     * Creates a new GeoPath object.
     *
     * @param s the specified {@code Shape} object
     */
    public GeoPath (Shape s)
    {
        this(s, null);
    }

    //---------//
    // GeoPath //
    //---------//
    /**
     * Creates a new GeoPath object.
     *
     * @param s the specified {@code Shape} object
     * @param at the specified {@code AffineTransform} object
     */
    public GeoPath (Shape           s,
                    AffineTransform at)
    {
        super(s, at);
    }

    //~ Methods ----------------------------------------------------------------

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{");
        sb.append(getClass().getSimpleName());

        double[] buffer = new double[6];

        for (PathIterator it = getPathIterator(null); !it.isDone();
             it.next()) {
            int segmentKind = it.currentSegment(buffer);

            sb.append(" ")
              .append(labelOf(segmentKind))
              .append("(");

            int     coords = countOf(segmentKind);
            boolean firstCoord = true;

            for (int ic = 0; ic < (coords - 1); ic += 2) {
                if (!firstCoord) {
                    sb.append(",");
                    firstCoord = false;
                }

                sb.append("[")
                  .append((float) buffer[ic])
                  .append(",")
                  .append((float) buffer[ic + 1])
                  .append("]");
            }

            sb.append(")");
        }

        sb.append("}");

        return sb.toString();
    }

    //---------//
    // countOf //
    //---------//
    /**
     * Report how many coordinate values a path segment contains
     * @param segmentKind the int-based segment kind
     * @return the number of coordinates values
     */
    protected static int countOf (int segmentKind)
    {
        switch (segmentKind) {
        case SEG_MOVETO :
        case SEG_LINETO :
            return 2;

        case SEG_QUADTO :
            return 4;

        case SEG_CUBICTO :
            return 6;

        case SEG_CLOSE :
            return 0;

        default :
            throw new RuntimeException("Illegal segmentKind " + segmentKind);
        }
    }

    //---------//
    // labelOf //
    //---------//
    /**
     * Report the kind label of a segment
     * @param segmentKind the int-based segment kind
     * @return the label for the curve
     */
    protected static String labelOf (int segmentKind)
    {
        switch (segmentKind) {
        case SEG_MOVETO :
            return "SEG_MOVETO";

        case SEG_LINETO :
            return "SEG_LINETO";

        case SEG_QUADTO :
            return "SEG_QUADTO";

        case SEG_CUBICTO :
            return "SEG_CUBICTO";

        case SEG_CLOSE :
            return "SEG_CLOSE";

        default :
            throw new RuntimeException("Illegal segmentKind " + segmentKind);
        }
    }
}
