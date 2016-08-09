//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                         V i s i t a b l e I n t e r p r e t a t i o n                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sig.inter;

/**
 * Interface {@code VisitableInterpretation} must be implemented by any class to be
 * visited by an InterVisitor.
 *
 * @author Hervé Bitteur
 */
public interface VisitableInter
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * General entry for any visiting
     *
     * @param visitor the concrete visitor object which defines the actual
     *                processing
     */
    void accept (InterVisitor visitor);
}
