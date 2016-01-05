//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           S y m b o l                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

import omr.run.Orientation;

import omr.ui.util.AttachmentHolder;

import omr.util.Entity;

import java.awt.Color;
import java.awt.Point;
import java.util.EnumSet;

/**
 * Interface {@code Symbol} is the base interface for a (fixed or dynamic) set of pixels.
 *
 * @author Hervé Bitteur
 */
public interface Symbol
        extends Entity, AttachmentHolder
{
    //~ Enumerations -------------------------------------------------------------------------------

    /**
     * This enumeration is used to group glyph instances by their intended use.
     * A single glyph instance can be assigned several groups.
     * TODO: perhaps with should be moved from Symbol to Glyph only (not dynamic ones)?
     */
    enum Group
    {
        //~ Enumeration constant initializers ------------------------------------------------------

        DEFAULT("Default group", new Color(0x00ffff)),
        STAFF_LINE("Staff Line", new Color(0xffffcc)),
        BEAM_SPOT("Beam-oriented spot", new Color(0xaaaaaa)),
        HEAD_SPOT("Head-oriented spot", new Color(0xbbbbbb)),
        VERTICAL_SEED("Vertical seed", new Color(0xccffcc)),
        LEDGER("Ledger", new Color(0xaaaaaa)),
        LEDGER_CANDIDATE("Ledger candidate", new Color(0xaaffaa)),
        WEAK_PART("Optional part", new Color(0xffaaaa)),
        SYMBOL("Fixed symbol", new Color(0xaaaaff)),
        DROP("DnD glyph", new Color(0xffbbbb));
        //~ Instance fields ------------------------------------------------------------------------

        /** Role of the group. */
        public final String description;

        /** Related color. */
        public final Color color;

        //~ Constructors ---------------------------------------------------------------------------
        Group (String description,
               Color color)
        {
            this.description = description;
            this.color = color;
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Assign a group to this symbol.
     *
     * @param group the group to be added to this glyph
     */
    void addGroup (Group group);

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
    EnumSet<Group> getGroups ();

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
    boolean hasGroup (Group group);
}
