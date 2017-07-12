//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   B a s i c E x c l u s i o n                                  //
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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code BasicExclusion}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "exclusion")
public class BasicExclusion
        extends AbstractRelation
        implements Exclusion
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    public final Cause cause;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new BasicExclusion object.
     *
     * @param cause root cause of this exclusion
     */
    public BasicExclusion (Cause cause)
    {
        this.cause = cause;
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BasicExclusion ()
    {
        this.cause = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public String getName ()
    {
        return "Exclusion";
    }

    @Override
    protected String internals ()
    {
        return super.internals() + cause;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    static class Adapter
            extends XmlAdapter<BasicExclusion, Exclusion>
    {
        //~ Constructors ---------------------------------------------------------------------------

        public Adapter ()
        {
        }

        //~ Methods --------------------------------------------------------------------------------
        @Override
        public BasicExclusion marshal (Exclusion itf)
                throws Exception
        {
            return (BasicExclusion) itf;
        }

        @Override
        public Exclusion unmarshal (BasicExclusion impl)
                throws Exception
        {
            return impl;
        }
    }
}
