//----------------------------------------------------------------------------//
//                                                                            //
//                         C h a m f e r M a t c h i n g                      //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code ChamferMatching} is a basic implementation of image
 * matching based on chamfer distances.
 *
 * @author Hervé Bitteur
 */
public class ChamferMatching
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The DistanceTransform image.
     * The value at each (x,y) location is the distance to nearest reference
     * point.
     */
    private final double[][] distances;

    //~ Constructors -----------------------------------------------------------
    //-----------------//
    // ChamferMatching //
    //-----------------//
    /**
     * Creates a new ChamferMatching object from an image of reference
     * points.
     *
     * @param image the reference points (true for a reference point, false
     *              for a non-reference point)
     */
    public ChamferMatching (boolean[][] image)
    {
        // Compute the distant transform
        this(new ChamferDistance().compute(image));
    }

    //-----------------//
    // ChamferMatching //
    //-----------------//
    /**
     * Creates a new ChamferMatching object from a distant transform
     * image.
     *
     * @param distances the distance transform image
     */
    public ChamferMatching (double[][] distances)
    {
        this.distances = distances;
    }

    //~ Methods ----------------------------------------------------------------
    //----------//
    // matchAll //
    //----------//
    /**
     * Run the provided template on the whole image to evaluate all
     * possible matches.
     *
     * @param template    the template to be used
     * @param maxDistance the maximum acceptable distance for keep a location
     * @return an (unsorted) list of locations with acceptable distance
     */
    public List<PixDistance> matchAll (Template template,
                                         double maxDistance)
    {
        final int scanWidth = distances.length
                              - template.getWidth();
        final int scanHeight = distances[0].length
                               - template.getHeight();
        final List<PixDistance> locations = new ArrayList<PixDistance>();

        for (int x = 0; x < scanWidth; x++) {
            for (int y = 0; y < scanHeight; y++) {
                // Get match value for a template located at (x,y)
                double dist = template.evaluate(x, y, null, distances);

                if (dist <= maxDistance) {
                    locations.add(new PixDistance(x, y, dist));
                }
            }
        }

        return locations;
    }
}
