//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            D e l e t e d I n t e r E x c e p t i o n                           //
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
package org.audiveris.omr.sig.inter;

/**
 * Class {@code DeletedInterException} defines an exception thrown when an Inter
 * instance no longer exists in its SIG.
 *
 * @author Hervé Bitteur
 */
public class DeletedInterException
        extends Exception
{

    public final Inter inter;

    /**
     * Creates a new {@code DeletedInterException} object.
     *
     * @param inter the deleted inter
     */
    public DeletedInterException (Inter inter)
    {
        this.inter = inter;
    }
}
