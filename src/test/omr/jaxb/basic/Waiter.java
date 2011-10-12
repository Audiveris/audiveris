//----------------------------------------------------------------------------//
//                                                                            //
//                                W a i t e r                                 //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.basic;

import java.awt.Point;
import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * DOCUMENT ME!
 *
 * @author Hervé Bitteur
 * @version $Id$
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
    //~ Instance fields --------------------------------------------------------

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

    /** not accessed directly */
    public String[] titles;

    //~ Methods ----------------------------------------------------------------

    public void setDays (List<Day> days)
    {
        this.days = days;
    }

    public List<Day> getDays ()
    {
        return days;
    }

    @XmlElement(name = "location")
    public void setPoint (MyPoint mp)
    {
        location = mp.getPoint();
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
}
