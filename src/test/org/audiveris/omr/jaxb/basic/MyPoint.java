//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M y P o i n t                                          //
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

import java.awt.Point;

import javax.xml.bind.annotation.*;

/**
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "point")
public class MyPoint
{

    private Point p;

    /** Creates a new instance of MyPoint */
    public MyPoint ()
    {
    }

    /**
     * Creates a new MyPoint object.
     */
    public MyPoint (Point p)
    {
        this.p = p;
    }

    public Point getPoint ()
    {
        return p;
    }

    public int getX ()
    {
        return p.x;
    }

    public int getY ()
    {
        return p.y;
    }

    @XmlElement
    public void setX (int x)
    {
        p.x = x;
    }

    @XmlElement
    public void setY (int y)
    {
        p.y = y;
    }
}
