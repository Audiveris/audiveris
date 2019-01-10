//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M y S y m b o l                                        //
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
package org.audiveris.omr.jaxb.itf;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

/**
 * Class {@code MySymbol}
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class MySymbol
        extends MyAbstractEntity
{

    @XmlAttribute
    private final int weight;

    /**
     * Creates a new {@code MySymbol} object.
     */
    public MySymbol ()
    {
        this.weight = 0;
    }

    /**
     * Creates a new {@code MySymbol} object.
     *
     * @param weight DOCUMENT ME!
     */
    public MySymbol (int weight)
    {
        this.weight = weight;
    }

    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append("{");
        sb.append("w:").append(weight);
        sb.append(" id:").append(getId());
        sb.append("}");

        return sb.toString();
    }
}
