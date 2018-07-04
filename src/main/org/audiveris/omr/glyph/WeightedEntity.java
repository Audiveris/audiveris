//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   W e i g h t e d E n t i t y                                  //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.glyph;

import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.ui.util.AttachmentHolder;
import org.audiveris.omr.util.Entity;

import java.awt.Point;
import java.util.EnumSet;

/**
 * Interface {@code WeightedEntity} is the base interface for a (fixed or dynamic) set
 * of pixels.
 *
 * @author Hervé Bitteur
 */
public interface WeightedEntity
        extends Entity, AttachmentHolder
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Assign a group to this symbol.
     *
     * @param group the group to be added to this glyph
     */
    void addGroup (GlyphGroup group);

    /**
     * Report the ratio of length over thickness, using provided orientation.
     *
     * @param orientation the general orientation reference
     * @return the "slimness" of the symbol
     * @see #getLength
     */
    double getAspect (Orientation orientation);

    /**
     * Report the symbol area center.
     *
     * @return the area center point
     */
    Point getCenter ();

    /**
     * Report the glyph absolute centroid (mass center).
     *
     * @return the absolute mass center point
     */
    Point getCentroid ();

    /**
     * Report the set of groups, perhaps empty, assigned to the symbol
     *
     * @return set of assigned groups
     */
    EnumSet<GlyphGroup> getGroups ();

    /**
     * Report the symbol height in pixels.
     *
     * @return height in pixels, along y-axis
     */
    int getHeight ();

    /**
     * Report the abscissa of top left corner.
     *
     * @return abscissa of top left corner
     */
    int getLeft ();

    /**
     * Report the length of the symbol, along the provided orientation.
     *
     * @param orientation the general orientation reference
     * @return the symbol length in pixels
     */
    int getLength (Orientation orientation);

    /**
     * Report the average thickness, perpendicular to the provided orientation
     *
     * @param orientation the provided axis orientation
     * @return the mean thickness (vertical thickness for a horizontal orientation)
     */
    double getMeanThickness (Orientation orientation);

    /**
     * Report the weight of this symbol, after normalization to sheet interline.
     *
     * @param interline sheet main interline
     * @return the weight value, expressed as an interline square fraction
     */
    double getNormalizedWeight (int interline);

    /**
     * Report the ordinate of top left corner
     *
     * @return the ordinate of top left corner
     */
    int getTop ();

    /**
     * Report (a copy of) symbol top left corner.
     *
     * @return top left location
     */
    Point getTopLeft ();

    /**
     * Report the total weight of this symbol, as its number of pixels.
     *
     * @return the total weight (number of pixels)
     */
    int getWeight ();

    /**
     * Report the symbol width in pixels.
     *
     * @return width in pixels, along x-axis
     */
    int getWidth ();

    /**
     * Check whether the provided group is assigned to the symbol.
     *
     * @param group the group value to check
     * @return true if assigned
     */
    boolean hasGroup (GlyphGroup group);
}
