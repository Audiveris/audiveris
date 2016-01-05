//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G l y p h L i n k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph;

/**
 * Interface {@code GlyphLink} represents a relationship (such as neighborhood) between
 * two glyphs instances.
 *
 * @author Hervé Bitteur
 */
public interface GlyphLink
{
    //~ Inner Classes ------------------------------------------------------------------------------

    /**
     * Neighborhood relationship.
     */
    public static class Nearby
            implements GlyphLink
    {
        //~ Instance fields ------------------------------------------------------------------------

        /** Measured distance between the two glyph instances. */
        private final double distance;

        //~ Constructors ---------------------------------------------------------------------------
        /**
         * Creates a new Nearby object.
         *
         * @param distance the measured distance between the two linked glyph instances
         */
        public Nearby (double distance)
        {
            this.distance = distance;
        }

        //~ Methods --------------------------------------------------------------------------------
        public double getDistance ()
        {
            return distance;
        }
    }
}
