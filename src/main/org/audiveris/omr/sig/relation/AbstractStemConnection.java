//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                           A b s t r a c t S t e m C o n n e c t i o n                          //
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

import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sig.inter.Inter;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;

import javax.xml.bind.annotation.XmlElement;

/**
 * Class {@code AbstractStemConnection} is the basis for connections to a stem.
 *
 * @author Hervé Bitteur
 */
public abstract class AbstractStemConnection
        extends AbstractConnection
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Logical extension point. */
    @XmlElement(name = "extension-point")
    protected Point2D extensionPoint;

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // getStemPortion //
    //----------------//
    /**
     * Report the portion of the stem the provided source is connected to
     *
     * @param source   the item connected to the stem (head, beam, flag)
     * @param stemLine logical range of the stem
     * @param scale    global scale
     * @return the stem Portion
     */
    public abstract StemPortion getStemPortion (Inter source,
                                                Line2D stemLine,
                                                Scale scale);

    //-------------------//
    // getExtensionPoint //
    //-------------------//
    /**
     * Report the logical connection point, which is defined as the point with maximum
     * extension along the logical stem.
     * This definition allows to use the extension ordinate to determine the precise stem portion of
     * the connection.
     *
     * @return the extension point
     */
    public Point2D getExtensionPoint ()
    {
        return extensionPoint;
    }

    //-------------------//
    // setExtensionPoint //
    //-------------------//
    /**
     * Set the logical extension point.
     *
     * @param extensionPoint the extension point to set
     */
    public void setExtensionPoint (Point2D extensionPoint)
    {
        this.extensionPoint = extensionPoint;
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        if (extensionPoint != null) {
            sb.append(
                    String.format(" [x:%.0f,y:%.0f]", extensionPoint.getX(), extensionPoint.getY()));
        }

        return sb.toString();
    }
}
