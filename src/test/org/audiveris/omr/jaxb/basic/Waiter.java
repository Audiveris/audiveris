//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          W a i t e r                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
import java.io.File;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * DOCUMENT ME!
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
//@XmlType(propOrder =  {
//    //    "firstName", "location", "purse", "days"}
//    "firstName", "location"}
//    //"firstName"}
//)
@XmlRootElement
public class Waiter
{

    /** A simple id */
    @XmlAttribute
    public int id;

    /** A matrix of double values */
    @XmlElementWrapper(name = "matrix")
    @XmlElement(name = "row")
    public double[][] mat;

    /** An array of double values */
    @XmlElementWrapper(name = "results")
    @XmlElement(name = "val")
    public double[] results;

    /** A simple string */
    @XmlElement(name = "first-name")
    public String firstName;

    /** A instance of a class I cannot annotate directly */
    @XmlTransient
    public Point location;

    /** An external complex type */
    @XmlElement(name = "tips")
    public Purse purse;

    /** A collection of complex types */
    ///@XmlElementWrapper(name = "days")
    @XmlElement(name = "day")
    private List<Day> days;

    @XmlElement(name = "path")
    public File path;

    /** not accessed directly */
    public String[] titles;

    public List<Day> getDays ()
    {
        return days;
    }

    public MyPoint getPoint ()
    {
        return new MyPoint(location);
    }

    //    public void setTitles (String[] titles)
    //    {
    //        System.out.println("setTitles");
    //        this.titles = titles;
    //    }
    @XmlElementWrapper(name = "titles")
    @XmlElement(name = "title")
    public String[] getTitles ()
    {
        System.out.println("getTitles");

        return titles;
    }

    public void setDays (List<Day> days)
    {
        this.days = days;
    }

    @XmlElement(name = "location")
    public void setPoint (MyPoint mp)
    {
        location = mp.getPoint();
    }
}
