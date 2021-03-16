//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           P u r s e                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.jaxb.basic;

import javax.xml.bind.annotation.*;

/**
 * DOCUMENT ME!
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class Purse
{

    @XmlElement(name = "tip")
    public double[] tips = new double[]{1.0, 2.345, 4.5};

    public Double[] getTips ()
    {
        Double[] dd = null;

        if (tips != null) {
            dd = new Double[tips.length];

            for (int i = 0; i < tips.length; i++) {
                dd[i] = tips[i];
            }
        }

        return dd;
    }

    public void setTips (Double[] tips)
    {
        this.tips = new double[tips.length];

        for (int i = 0; i < tips.length; i++) {
            this.tips[i] = tips[i];
        }
    }
}
