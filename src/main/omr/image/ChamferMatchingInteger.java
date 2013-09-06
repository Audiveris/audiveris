//----------------------------------------------------------------------------//
//                                                                            //
//                  C h a m f e r M a t c h i n g I n t e g e r               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Herve Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.image;


/**
 * Class {@code ChamferMatchingInteger}
 *
 * @author Hervé Bitteur
 */
public class ChamferMatchingInteger
{
    //~ Instance fields --------------------------------------------------------

    /**
     * The DistanceTransform image.
     * The value at each (x,y) location is the distance to nearest reference
     * point.
     */
    private final int[][] distances;

    /** Width of image. */
    private final int imageWidth;

    /** Height of image. */
    private final int imageHeight;

    //~ Constructors -----------------------------------------------------------

    //------------------------//
    // ChamferMatchingInteger //
    //------------------------//
    /**
     * Creates a new ChamferMatching object from an image of reference
     * points.
     *
     * @param image the reference points (true for a reference point, false
     *              for a non-reference point)
     */
    public ChamferMatchingInteger (boolean[][] image)
    {
        // Compute the distant transform
        this(new ChamferDistanceInteger().compute(image, null));
    }

    //------------------------//
    // ChamferMatchingInteger //
    //------------------------//
    /**
     * Creates a new ChamferMatching object from a distant transform
     *
     * @param distances the distance transform image
     */
    public ChamferMatchingInteger (int[][] distances)
    {
        this.distances = distances;
        imageWidth = distances.length;
        imageHeight = distances[0].length;
    }

    //~ Methods ----------------------------------------------------------------

    //-------//
    // match //
    //-------//
    /**
     * Run the provided template on the image to evaluate all possible
     * matches.
     *
     * @param template the template to be used
     * @param output   the pre-allocated output table, or null
     * @return a table parallel to original image, with value at each (x,y)
     *         being the match evaluation at this location.
     */
    public double[][] match (boolean[][] template,
                             double[][]  output)
    {
        final int tempWidth = template.length;
        final int tempHeight = template[0].length;

        if (output == null) {
            output = new double[imageWidth - tempWidth][imageHeight -
                     tempHeight];
        }

        for (int x = 0; x < (imageWidth - tempWidth); x++) {
            for (int y = 0; y < (imageHeight - tempHeight); y++) {
                // Get match value for a template located at (x,y)
                int q = 0;
                int count = 0;

                for (int i = 0; i < tempWidth; i++) {
                    for (int j = 0; j < tempHeight; j++) {
                        if (template[i][j]) {
                            count++;

                            int dist = distances[x + i][y + j];
                            q += (dist * dist);
                        }
                    }
                }

                // Should use (1/normalizer)*sqrt(q/count)
                output[x][y] = q / (9f * count);
            }
        }

        return output;
    }
}
