//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C o n c u r r e n c y                                      //
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
package org.audiveris.omr.util;

/**
 * Interface {@code Concurrency} declares if an entity (class instance) can be used by
 * concurrent threads.
 * This complements the JCIP annotations in a more dynamic way.
 *
 * @author Hervé Bitteur
 */
public interface Concurrency
{

    /**
     * Report whether the entity can be used concurrently.
     *
     * @return true if so
     */
    boolean isThreadSafe ();
}
