//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           E n d i n g S e n t e n c e R e l a t i o n                          //
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
package omr.sig.relation;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code EndingSentenceRelation} represents a support relation between an ending
 * and its related text (such as "1." or "1,2").
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "ending-sentence")
public class EndingSentenceRelation
        extends AbstractSupport
{
}
