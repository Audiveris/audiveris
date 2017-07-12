//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      N o E x c l u s i o n                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sig.relation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code NoExclusion} is used to formalize that two Inters, generally originating
 * from mirrored entities, do not exclude each other, although they overlap.
 * This occurs with beams of mirrored stems.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "no-exclusion")
public class NoExclusion
        extends AbstractSupport
{
}
