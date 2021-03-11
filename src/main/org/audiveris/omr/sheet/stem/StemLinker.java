//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S t e m L i n k e r                                      //
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
package org.audiveris.omr.sheet.stem;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.sig.inter.Inter;

import java.awt.geom.Point2D;
import java.util.Collection;

/**
 * Interface {@code StemLinker} abstract the common features of beam and head linkers.
 *
 * @author Hervé Bitteur
 */
public abstract class StemLinker
{

    /**
     * Report whether this linker has been closed, preventing any link.
     *
     * @return true if so
     */
    public abstract boolean isClosed ();

    /**
     * Set the closed status.
     *
     * @param closed the new closed value
     */
    public abstract void setClosed (boolean closed);

    /**
     * Report whether this linker has been successfully linked.
     *
     * @return true if so
     */
    public abstract boolean isLinked ();

    /**
     * Set the linked status.
     *
     * @param linked the new linked value
     */
    public abstract void setLinked (boolean linked);

    /**
     * Report the source Inter instance (Head or Beam) the stem is to be linked to.
     *
     * @return the inter instance to connect with stem
     */
    public abstract Inter getSource ();

    /**
     * Report the head or beam reference point for the stem connection.
     *
     * @return reference point
     */
    public abstract Point2D getReferencePoint ();

    /**
     * Report the starting stump glyph, if any, around the reference point.
     *
     * @return the stump glyph or null
     */
    public abstract Glyph getStump ();

    /**
     * Report the half linkers managed by this linker.
     *
     * @return top and bottom linkers, if any
     */
    public abstract Collection<? extends StemHalfLinker> getHalfLinkers ();
}
