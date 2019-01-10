//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        G l y p h L i n k                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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

/**
 * Interface {@code GlyphLink} represents a relationship (such as neighborhood) between
 * two glyphs instances.
 *
 * @author Hervé Bitteur
 */
public interface GlyphLink
{

    /**
     * Neighborhood relationship.
     */
    public static class Nearby
            implements GlyphLink
    {

        /** Measured distance between the two glyph instances. */
        private final double distance;

        /**
         * Creates a new Nearby object.
         *
         * @param distance the measured distance between the two linked glyph instances
         */
        public Nearby (double distance)
        {
            this.distance = distance;
        }

        /**
         * Report the link distance
         *
         * @return distance
         */
        public double getDistance ()
        {
            return distance;
        }
    }
}
