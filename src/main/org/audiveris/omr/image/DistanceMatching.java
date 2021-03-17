//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                 D i s t a n c e M a t c h i n g                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.image;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code DistanceMatching} is a basic implementation of image matching based on
 * distances.
 *
 * @author Hervé Bitteur
 */
public class DistanceMatching
{
    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The DistanceTransform image.
     * The value at each (x,y) location is the distance to nearest reference
     * point.
     */
    private final DistanceTable distances;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new DistanceMatching object from a distant transform image.
     *
     * @param distances the distance transform image
     */
    public DistanceMatching (DistanceTable distances)
    {
        this.distances = distances;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // matchAll //
    //----------//
    /**
     * Run the provided template on the whole image to evaluate all possible matches.
     *
     * @param template    the template to be used
     * @param maxDistance the maximum acceptable distance for keeping a location
     * @return an (unsorted) list of locations with acceptable distance
     */
    public List<PixelDistance> matchAll (Template template,
                                         double maxDistance)
    {
        final int scanWidth = distances.getWidth() - template.getWidth();
        final int scanHeight = distances.getHeight() - template.getHeight();
        final List<PixelDistance> locations = new ArrayList<>();

        for (int x = 0; x < scanWidth; x++) {
            for (int y = 0; y < scanHeight; y++) {
                // Get match value for a template located at (x,y)
                double dist = template.evaluate(x, y, null, distances);

                if (dist <= maxDistance) {
                    locations.add(new PixelDistance(x, y, dist));
                }
            }
        }

        return locations;
    }
}
