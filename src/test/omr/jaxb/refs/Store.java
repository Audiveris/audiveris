//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                            S t o r e                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.refs;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Store}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement
public class Store
{
    //~ Instance fields ----------------------------------------------------------------------------

    // Containment to define all IDs
    @XmlElementWrapper(name = "fruits")
    @XmlElementRef
    //    @XmlElements({
    //        @XmlElement(name = "apple", type = Apple.class),
    //        @XmlElement(name = "orange", type = Orange.class)
    //    })
    ArrayList<Fruit> fruits = new ArrayList<Fruit>();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Store} object.
     */
    public Store ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void add (Apple apple)
    {
        fruits.add(apple);
    }

    public void add (Orange orange)
    {
        fruits.add(orange);
    }
}
