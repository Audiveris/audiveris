//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        E x c l u s i o n                                       //
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
package org.audiveris.omr.sig.relation;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class <code>Exclusion</code> is a relation that indicates exclusion between two
 * possible interpretations.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "exclusion")
public class Exclusion
        extends Relation
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    public final ExclusionCause cause;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-arg constructor meant for JAXB.
     */
    private Exclusion ()
    {
        this.cause = null;
    }

    /**
     * Creates a new Exclusion object.
     *
     * @param cause root cause of this exclusion
     */
    public Exclusion (ExclusionCause cause)
    {
        this.cause = cause;
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        return super.internals() + cause;
    }

    //----------------//
    // isSingleSource //
    //----------------//
    @Override
    public boolean isSingleSource ()
    {
        return false;
    }

    //----------------//
    // isSingleTarget //
    //----------------//
    @Override
    public boolean isSingleTarget ()
    {
        return false;
    }

    /**
     * Enum <code>ExclusionCause</code> describes the cause for exclusion.
     */
    public enum ExclusionCause
    {
        /** Physical overlap. */
        OVERLAP,
        /** Some logical exclusion. */
        INCOMPATIBLE;
    }
}
