//----------------------------------------------------------------------------//
//                                                                            //
//                            A R T M o m e n t s                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.moments;

/**
 * Interface {@code ARTMoments} defines a region shape features
 * descriptor based on Angular Radial Transform.
 *
 * See MPEG-7 Experimentation Model for the original C++ code
 *
 * @author Hervé Bitteur
 */
public interface ARTMoments
        extends OrthogonalMoments<ARTMoments>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Number of angular indixes */
    public static final int ANGULAR = 12;

    /** Number of radius indices */
    public static final int RADIAL = 3;

    //~ Methods ----------------------------------------------------------------
    /**
     * Report the argument value for provided phase and radius indices.
     *
     * @param p phase index
     * @param r radius index
     * @return the argument double value [-PI..PI]
     */
    double getArgument (int p,
                        int r);

    /**
     * Report the imaginary value for provided phase and radius indices.
     *
     * @param p phase index
     * @param r radius index
     * @return the module double value [0..1]
     */
    double getImag (int p,
                    int r);

    /**
     * Report the module value for provided phase and radius indices.
     *
     * @param p phase index
     * @param r radius index
     * @return the module double value [0..1]
     */
    double getModule (int p,
                      int r);

    /**
     * Report the real value for provided phase and radius indices.
     *
     * @param p phase index
     * @param r radius index
     * @return the module double value [0..1]
     */
    double getReal (int p,
                    int r);

    /**
     * Set the argument value for provided phase and radius indices.
     *
     * @param p     phase index
     * @param r     radius index
     * @param value the argument double value [-PI..PI]
     */
    void setArgument (int p,
                      int r,
                      double value);

    /**
     * Set the imaginary value for provided phase and radius indices.
     *
     * @param p     phase index
     * @param r     radius index
     * @param value the element double value [0..1]
     */
    void setImag (int p,
                  int r,
                  double value);

    /**
     * Set the module value for provided phase and radius indices.
     *
     * @param p     phase index
     * @param r     radius index
     * @param value the element double value [0..1]
     */
    void setModule (int p,
                    int r,
                    double value);

    /**
     * Set the real value for provided phase and radius indices.
     *
     * @param p     phase index
     * @param r     radius index
     * @param value the element double value [0..1]
     */
    void setReal (int p,
                  int r,
                  double value);
}
