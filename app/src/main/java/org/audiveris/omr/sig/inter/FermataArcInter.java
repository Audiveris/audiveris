//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  F e r m a t a A r c I n t e r                                 //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2024. All rights reserved.
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

import org.audiveris.omr.sheet.symbol.SymbolsBuilder;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>FermataArcInter</code> represents the arc part of a fermata, either upright or
 * inverted.
 * <p>
 * Combined with a FermataDotInter, it could lead to a (full) FermataInter.
 * <p>
 * This class is now deprecated, since the full {@link FermataInter} can be directly recognized
 * by the {@link SymbolsBuilder}.
 *
 * @author Hervé Bitteur
 */
@Deprecated
@XmlRootElement(name = "fermata-arc")
public class FermataArcInter
        extends AbstractInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    @SuppressWarnings("unused")
    private FermataArcInter ()
    {
    }
}
