//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                            B r a c k e t C o n n e c t o r I n t e r                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import static org.audiveris.omr.glyph.Shape.BRACKET_CONNECTOR;

import org.audiveris.omr.sheet.grid.BarConnection;
import org.audiveris.omr.sig.GradeImpacts;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>BracketConnectorInter</code> represents a vertical connector between two
 * brackets inters across staves.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "bracket-connector")
public class BracketConnectorInter
        extends AbstractVerticalConnectorInter
{
    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    private BracketConnectorInter ()
    {
    }

    /**
     * Creates a new BracketConnectorInter object.
     *
     * @param connection the underlying connection
     * @param impacts    the assignment details
     */
    public BracketConnectorInter (BarConnection connection,
                                  GradeImpacts impacts)
    {
        super(connection, BRACKET_CONNECTOR, impacts);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }
}
