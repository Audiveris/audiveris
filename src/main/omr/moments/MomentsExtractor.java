//----------------------------------------------------------------------------//
//                                                                            //
//                      M o m e n t s E x t r a c t o r                       //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.moments;

import java.awt.image.WritableRaster;

/**
 * Interface {@code MomentsExtractor} is a general definition for an
 * extractor of {@link OrthogonalMoments}.
 *
 * @param <D> the descriptor type
 *
 * @author Hervé Bitteur
 */
public interface MomentsExtractor<D extends OrthogonalMoments<D>>
{
    //~ Methods ----------------------------------------------------------------

    /**
     * Extract information from provided foreground points and save
     * the results in the target descriptor.
     *
     * @param xx   the array of abscissa values
     * @param yy   the array of ordinate values
     * @param mass the number of points
     */
    void extract (int[] xx,
                  int[] yy,
                  int mass);

    /**
     * Reconstruct an image from the shape features.
     *
     * @param raster the (square) raster to populate
     */
    void reconstruct (WritableRaster raster);

    /**
     * Assign a target descriptor of this extractor, to receive
     * extraction results.
     *
     * @param descriptor the target descriptor
     */
    void setDescriptor (D descriptor);
}
