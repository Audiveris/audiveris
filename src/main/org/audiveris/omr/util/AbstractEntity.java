//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                   A b s t r a c t E n t i t y                                  //
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
package org.audiveris.omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlID;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code AbstractEntity}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "entity")
public abstract class AbstractEntity
        implements Entity
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            AbstractEntity.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Identity for debugging. */
    @XmlID
    @XmlAttribute(name = "id")
    @XmlJavaTypeAdapter(type = int.class, value = Jaxb.StringIntegerAdapter.class)
    protected int id;

    // Transient data
    //---------------
    //
    /** (Debug) flag this as VIP. */
    protected boolean vip;

    //~ Methods ------------------------------------------------------------------------------------
    //--------//
    // dumpOf //
    //--------//
    @Override
    public String dumpOf ()
    {
        return this.toString(); // By default
    }

    //-------//
    // getId //
    //-------//
    @Override
    public int getId ()
    {
        return id;
    }

    //-------//
    // isVip //
    //-------//
    @Override
    public boolean isVip ()
    {
        return vip;
    }

    //-------//
    // setId //
    //-------//
    @Override
    public void setId (int id)
    {
        this.id = id;
    }

    //--------//
    // setVip //
    //--------//
    @Override
    public void setVip (boolean vip)
    {
        this.vip = vip;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();

        sb.append(getClass().getSimpleName()).append("{").append("#").append(id);

        try {
            sb.append(internals());
        } catch (Throwable ex) {
            // Temporarily, some internals may not be printable
            // Hence, to not perturb debugging print outs, we use a basic placeholder here
            ///logger.warn("toString ex:" + ex, ex);
            sb.append("<invalid-internals>");
        }

        sb.append("}");

        return sb.toString();
    }

    //-----------//
    // internals //
    //-----------//
    protected String internals ()
    {
        return "";
    }
}
