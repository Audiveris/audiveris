/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.moments;

import java.awt.geom.Point2D;

/**
 * Class {@code AbstractExtractor} provides the basis for moments
 * extraction.
 *
 * @param <D> actual descriptor type
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractExtractor<D extends OrthogonalMoments>
    implements MomentsExtractor<D>
{
    //~ Instance fields --------------------------------------------------------

    /** Input abscissae. */
    protected int[] xx;

    /** Input ordinates. */
    protected int[] yy;

    /** Image mass (number of foreground points). */
    protected int mass;

    /** Center of mass. */
    protected Point2D center;

    /** Image max radius around its mass center. */
    protected double radius;

    /** The target descriptor. */
    protected D descriptor;

    //~ Methods ----------------------------------------------------------------

    //---------//
    // extract //
    //---------//
    @Override
    public void extract (int[] xx,
                         int[] yy,
                         int   mass)
    {
        // Check arguments
        if ((xx == null) || (yy == null)) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + " cannot process a null array");
        }

        if ((mass <= 0) || (mass > xx.length) || (mass > yy.length)) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + " on inconsistent input");
        }

        if (descriptor == null) {
            throw new IllegalArgumentException(
                getClass().getSimpleName() + " has no target descriptor");
        }

        this.xx = xx;
        this.yy = yy;
        this.mass = mass;

        findCenterOfMass();
        findRadius();

        //        long start = System.currentTimeMillis();
        //
        //        for (int k = 0; k < 100; k++) {
        extractMoments();

        //        }
        //
        //        long stop = System.currentTimeMillis();
        //        System.out.println("100 runs : " + (stop - start) + " ms");
    }

    //---------------//
    // setDescriptor //
    //---------------//
    @Override
    public void setDescriptor (D descriptor)
    {
        this.descriptor = descriptor;
    }

    //----------------//
    // extractMoments //
    //----------------//
    /** Actual extraction core, to be provided by subclasses. */
    protected abstract void extractMoments ();

    //------------------//
    // findCenterOfMass //
    //------------------//
    /**
     * Computer the image mass center coordinates.
     */
    private void findCenterOfMass ()
    {
        int m10 = 0;
        int m01 = 0;

        for (int i = 0; i < mass; i++) {
            m10 += xx[i];
            m01 += yy[i];
        }

        center = new Point2D.Double(
            (double) m10 / (double) mass,
            (double) m01 / (double) mass);

        ///System.out.println("center: " + center);
    }

    //------------//
    // findRadius //
    //------------//
    /**
     * Compute the image contour, centered around its mass center
     */
    private void findRadius ()
    {
        radius = Double.MIN_VALUE;

        for (int i = 0; i < mass; i++) {
            double x = xx[i] - center.getX();
            double y = yy[i] - center.getY();
            radius = Math.max(radius, Math.abs(x));
            radius = Math.max(radius, Math.abs(y));
        }

        ///System.out.println("radius:" + radius);
    }
}
