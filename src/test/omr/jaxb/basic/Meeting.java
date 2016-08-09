//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M e e t i n g                                          //
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
package omr.jaxb.basic;

import javax.xml.bind.annotation.XmlAttribute;

/**
 * DOCUMENT ME!
 *
 * @author Hervé Bitteur
 */
public class Meeting
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    public int start;

    @XmlAttribute
    public int stop;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Meeting object.
     *
     * @param start DOCUMENT ME!
     * @param stop  DOCUMENT ME!
     */
    public Meeting (int start,
                    int stop)
    {
        this.start = start;
        this.stop = stop;
    }

    /**
     * Creates a new Meeting object.
     */
    public Meeting ()
    {
    }
}
