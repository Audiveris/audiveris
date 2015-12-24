//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           B a s k e t                                          //
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

import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Basket}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "basket")
public class Basket
{
    //~ Instance fields ----------------------------------------------------------------------------

    //    @XmlElementWrapper(name = "apples")
    //    @XmlElement(name = "apple")
    @XmlList
    @XmlIDREF
    ArrayList<Apple> apples = new ArrayList<Apple>();

    //    @XmlElementWrapper(name = "oranges")
    //    @XmlElement(name = "orange")
    @XmlList
    @XmlIDREF
    ArrayList<Orange> oranges = new ArrayList<Orange>();

    //~ Methods ------------------------------------------------------------------------------------
    public void add (Apple apple)
    {
        apples.add(apple);
    }

    public void add (Orange orange)
    {
        oranges.add(orange);
    }
}
