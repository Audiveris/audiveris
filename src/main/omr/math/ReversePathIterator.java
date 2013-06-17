//----------------------------------------------------------------------------//
//                                                                            //
//                   R e v e r s e P a t h I t e r a t o r                    //
//                                                                            //
//----------------------------------------------------------------------------//
package omr.math;

// ============================================================================
// File:               ReversePathIterator.java
//
// Project:            General utilities.
//
// Purpose:            Is missing in java.awt.geom.
//
// Author:             Rammi
//
// Copyright Notice:   (c) 2005  Rammi (rammi@caff.de)
//                     This code is in the public domain.
//                     This usage of this code is allowed without any restrictions.
//                     No guarantees are given whatsoever.
//
// Latest change:      $Date: 2005/02/08 11:59:19 $
//
// History:            $Log: ReversePathIterator.java,v $
// History:            Revision 1.3  2005/02/08 11:59:19  rammi
// History:            Transferred into the public domain.
// History:
// History:            Revision 1.2  2005/02/07 18:58:45  rammi
// History:            Added optimizations
// History:
// History:            Revision 1.1  2005/01/25 14:17:20  rammi
// History:            First version
// History:
//=============================================================================
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.IllegalPathStateException;
import java.awt.geom.PathIterator;

/**
 * A path iterator which iterates over a path in the reverse direction.
 * This is missing in the java.awt.geom package, although it's quite simple to
 * implement.
 * After initialization the original PathIterator is not used any longer.
 * <p>
 * There are several static convenience methods to create a reverse path
 * iterator from
 * a shape directly:
 * <ul>
 * <li>{@link #getReversePathIterator(java.awt.Shape)}
 * for reversing the standard path iterator</li>
 * <li>{@link #getReversePathIterator(java.awt.Shape, double)}
 * for reversing a flattened path iterator</li>
 * <li>{@link #getReversePathIterator(java.awt.Shape, java.awt.geom.AffineTransform)}
 * for reversing a transformed path iterator</li>
 * <li>{@link #getReversePathIterator(java.awt.Shape, java.awt.geom.AffineTransform, double)}
 * for reversing a transformed flattened path iterator</li>
 * <li>{@link #getReversePathIterator(java.awt.Shape, int)}
 * for reversing the standard path iterator while explicitely defining a winding
 * rule</li>
 * <li>{@link #getReversePathIterator(java.awt.Shape, double, int)}
 * for reversing a flattened path iterator while explicitely defining a winding
 * rule</li>
 * <li>{@link #getReversePathIterator(java.awt.Shape, java.awt.geom.AffineTransform, int)}
 * for reversing a transformed path iterator while explicitely defining a
 * winding rule</li>
 * <li>{@link #getReversePathIterator(java.awt.Shape, java.awt.geom.AffineTransform, double, int)}
 * for reversing a transformed flattened path iterator while explicitely
 * defining a winding rule</li>
 * </ul>
 *
 * @author <a href="mailto:rammi@caff.de">Rammi</a>
 * @version $Revision: 1.3 $
 */
public class ReversePathIterator
        implements PathIterator
{
    //~ Instance fields --------------------------------------------------------

    /** The winding rule. */
    private final int windingRule;

    /** The reversed coordinates. */
    private final double[] coordinates;

    /** The reversed segment types. */
    private final int[] segmentTypes;

    /** The index into the coordinates during iteration. */
    private int coordIndex = 0;

    /** The index into the segment types during iteration. */
    private int segmentIndex = 0;

    //~ Constructors -----------------------------------------------------------
    /**
     * Create an inverted path iterator from a standard one, keeping the winding
     * rule.
     *
     * @param original original iterator
     */
    public ReversePathIterator (PathIterator original)
    {
        this(original, original.getWindingRule());
    }

    /**
     * Create an inverted path iterator from a standard one.
     *
     * @param original    original iterator
     * @param windingRule winding rule of newly created iterator
     */
    public ReversePathIterator (PathIterator original,
                                int windingRule)
    {
        this.windingRule = windingRule;

        double[] coords = new double[16];
        int coordPos = 0;
        int[] segTypes = new int[8];
        int segPos = 0;
        boolean first = true;

        double[] temp = new double[6];

        while (!original.isDone()) {
            if (segPos == segTypes.length) {
                // resize
                int[] dummy = new int[2 * segPos];
                System.arraycopy(segTypes, 0, dummy, 0, segPos);
                segTypes = dummy;
            }

            final int segType = segTypes[segPos++] = original.currentSegment(
                    temp);

            if (first) {
                if (segType != SEG_MOVETO) {
                    throw new IllegalPathStateException(
                            "missing initial moveto in path definition");
                }

                first = false;
            }

            int copy;

            switch (segType) {
            case SEG_MOVETO:
            case SEG_LINETO:
                copy = 2;

                break;

            case SEG_QUADTO:
                copy = 4;

                break;

            case SEG_CUBICTO:
                copy = 6;

                break;

            default:
                copy = 0;

                break;
            }

            if (copy > 0) {
                if ((coordPos + copy) > coords.length) {
                    // resize
                    double[] dummy = new double[coords.length * 2];
                    System.arraycopy(coords, 0, dummy, 0, coords.length);
                    coords = dummy;
                }

                for (int c = 0; c < copy; ++c) {
                    coords[coordPos++] = temp[c];
                }
            }

            original.next();
        }

        // === reverse everything ===
        // --- reverse coordinates ---
        coordinates = new double[coordPos];

        for (int p = (coordPos / 2) - 1; p >= 0; --p) {
            coordinates[2 * p] = coords[coordPos - (2 * p) - 2];
            coordinates[(2 * p) + 1] = coords[coordPos - (2 * p) - 1];
        }

        // --- reverse segment types ---
        segmentTypes = new int[segPos];

        if (segPos > 0) {
            boolean pendingClose = false;
            int sr = 0;
            segmentTypes[sr++] = SEG_MOVETO;

            for (int s = segPos - 1; s > 0; --s) {
                switch (segTypes[s]) {
                case SEG_MOVETO:

                    if (pendingClose) {
                        pendingClose = false;
                        segmentTypes[sr++] = SEG_CLOSE;
                    }

                    segmentTypes[sr++] = SEG_MOVETO;

                    break;

                case SEG_CLOSE:
                    pendingClose = true;

                    break;

                default:
                    segmentTypes[sr++] = segTypes[s];

                    break;
                }
            }

            if (pendingClose) {
                segmentTypes[sr] = SEG_CLOSE;
            }
        }
    }

    //~ Methods ----------------------------------------------------------------
    /**
     * Get a reverse path iterator for a shape, keeping the shape's winding
     * rule.
     *
     * @param shape shape for which a reverse path iterator is needed
     * @return reverse path iterator
     */
    public static PathIterator getReversePathIterator (Shape shape)
    {
        return new ReversePathIterator(shape.getPathIterator(null));
    }

    /**
     * Get a reverse flattened path iterator for a shape, keeping the shape's
     * winding rule.
     *
     * @param shape    shape for which a reverse flattened path iterator is
     *                 needed
     * @param flatness flatness epsilon
     * @return reverse flattened path iterator
     */
    public static PathIterator getReversePathIterator (Shape shape,
                                                       double flatness)
    {
        return new ReversePathIterator(shape.getPathIterator(null, flatness));
    }

    /**
     * Get a reverse transformed path iterator for a shape, keeping the shape's
     * winding rule.
     *
     * @param shape shape for which a reverse transformed path iterator is
     *              needed
     * @return reverse transformed path iterator
     */
    public static PathIterator getReversePathIterator (Shape shape,
                                                       AffineTransform at)
    {
        return new ReversePathIterator(shape.getPathIterator(at));
    }

    /**
     * Get a reverse transformed flattened path iterator for a shape, keeping
     * the shape's winding rule.
     *
     * @param shape    shape for which a reverse transformed flattened path
     *                 iterator is needed
     * @param flatness flatness epsilon
     * @return reverse transformed flattened path iterator
     */
    public static PathIterator getReversePathIterator (Shape shape,
                                                       AffineTransform at,
                                                       double flatness)
    {
        return new ReversePathIterator(shape.getPathIterator(at, flatness));
    }

    /**
     * Get a reverse path iterator for a shape.
     *
     * @param shape       shape for which a reverse path iterator is needed
     * @param windingRule winding rule of newly created iterator
     * @return reverse path iterator
     */
    public static PathIterator getReversePathIterator (Shape shape,
                                                       int windingRule)
    {
        return new ReversePathIterator(
                shape.getPathIterator(null),
                windingRule);
    }

    /**
     * Get a reverse flattened path iterator for a shape.
     *
     * @param shape       shape for which a reverse flattened path iterator is
     *                    needed
     * @param flatness    flatness epsilon
     * @param windingRule winding rule of newly created iterator
     * @return reverse flattened path iterator
     */
    public static PathIterator getReversePathIterator (Shape shape,
                                                       double flatness,
                                                       int windingRule)
    {
        return new ReversePathIterator(
                shape.getPathIterator(null, flatness),
                windingRule);
    }

    /**
     * Get a reverse transformed path iterator for a shape.
     *
     * @param shape       shape for which a reverse transformed path iterator is
     *                    needed
     * @param windingRule winding rule of newly created iterator
     * @return reverse transformed path iterator
     */
    public static PathIterator getReversePathIterator (Shape shape,
                                                       AffineTransform at,
                                                       int windingRule)
    {
        return new ReversePathIterator(shape.getPathIterator(at), windingRule);
    }

    /**
     * Get a reverse transformed flattened path iterator for a shape.
     *
     * @param shape       shape for which a reverse transformed flattened path
     *                    iterator is needed
     * @param flatness    flatness epsilon
     * @param windingRule winding rule of newly created iterator
     * @return reverse transformed flattened path iterator
     */
    public static PathIterator getReversePathIterator (Shape shape,
                                                       AffineTransform at,
                                                       double flatness,
                                                       int windingRule)
    {
        return new ReversePathIterator(
                shape.getPathIterator(at, flatness),
                windingRule);
    }

    /**
     * Returns the coordinates and type of the current path segment in
     * the iteration.
     * The return value is the path-segment type:
     * SEG_MOVETO, SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE.
     * A double array of length 6 must be passed in and can be used to
     * store the coordinates of the point(s).
     * Each point is stored as a pair of double x,y coordinates.
     * SEG_MOVETO and SEG_LINETO types returns one point,
     * SEG_QUADTO returns two points,
     * SEG_CUBICTO returns 3 points
     * and SEG_CLOSE does not return any points.
     *
     * @param coords an array that holds the data returned from
     *               this method
     * @return the path-segment type of the current path segment.
     * @see #SEG_MOVETO
     * @see #SEG_LINETO
     * @see #SEG_QUADTO
     * @see #SEG_CUBICTO
     * @see #SEG_CLOSE
     */
    @Override
    public int currentSegment (double[] coords)
    {
        final int segmentType = segmentTypes[segmentIndex];
        final int copy = coordinatesForSegmentType(segmentType);

        if (copy > 0) {
            System.arraycopy(coordinates, coordIndex, coords, 0, copy);
        }

        return segmentType;
    }

    /**
     * Returns the coordinates and type of the current path segment in
     * the iteration.
     * The return value is the path-segment type:
     * SEG_MOVETO, SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE.
     * A float array of length 6 must be passed in and can be used to
     * store the coordinates of the point(s).
     * Each point is stored as a pair of float x,y coordinates.
     * SEG_MOVETO and SEG_LINETO types returns one point,
     * SEG_QUADTO returns two points,
     * SEG_CUBICTO returns 3 points
     * and SEG_CLOSE does not return any points.
     *
     * @param coords an array that holds the data returned from
     *               this method
     * @return the path-segment type of the current path segment.
     * @see #SEG_MOVETO
     * @see #SEG_LINETO
     * @see #SEG_QUADTO
     * @see #SEG_CUBICTO
     * @see #SEG_CLOSE
     */
    @Override
    public int currentSegment (float[] coords)
    {
        final int segmentType = segmentTypes[segmentIndex];
        final int copy = coordinatesForSegmentType(segmentType);

        if (copy > 0) {
            for (int c = 0; c < copy; ++c) {
                coords[c] = (float) coordinates[coordIndex + c];
            }
        }

        return segmentType;
    }

    /**
     * Returns the winding rule for determining the interior of the
     * path. This just returns the winding rule of the original path,
     * which may or may not be what is wanted.
     *
     * @return the winding rule.
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     */
    @Override
    public int getWindingRule ()
    {
        return windingRule;
    }

    /**
     * Tests if the iteration is complete.
     *
     * @return {@code true} if all the segments have
     *         been read; {@code false} otherwise.
     */
    @Override
    public boolean isDone ()
    {
        return segmentIndex >= segmentTypes.length;
    }

    /**
     * Moves the iterator to the next segment of the path forwards
     * along the primary direction of traversal as long as there are
     * more points in that direction.
     */
    @Override
    public void next ()
    {
        coordIndex += coordinatesForSegmentType(segmentTypes[segmentIndex++]);
    }

    /**
     * Get the number of coordinates needed for a segment type.
     *
     * @param segtype segment type
     * @return coordinates needed
     */
    private static int coordinatesForSegmentType (int segtype)
    {
        switch (segtype) {
        case SEG_MOVETO:
        case SEG_LINETO:
            return 2;

        case SEG_QUADTO:
            return 4;

        case SEG_CUBICTO:
            return 6;
        }

        return 0;
    }
}
