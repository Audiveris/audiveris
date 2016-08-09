//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      A R T M o m e n t s                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//
//  This program is free software: you can redistribute it and/or modify it under the terms of the
//  GNU Affero General Public License as published by the Free Software Foundation, either version
//  3 of the License, or (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
//  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
//  See the GNU Affero General Public License for more details.
//
//  You should have received a copy of the GNU Affero General Public License along with this
//  program.  If not, see <http://www.gnu.org/licenses/>.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.moments;

/**
 * Interface {@code ARTMoments} defines a region shape features descriptor based on
 * Angular Radial Transform.
 * <p>
 * See MPEG-7 Experimentation Model for the original C++ code
 *
 * @author Hervé Bitteur
 */
public interface ARTMoments
        extends OrthogonalMoments<ARTMoments>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /** Number of angular indixes */
    public static final int ANGULAR = 12;

    /** Number of radius indices */
    public static final int RADIAL = 3;

    //~ Methods ------------------------------------------------------------------------------------
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
