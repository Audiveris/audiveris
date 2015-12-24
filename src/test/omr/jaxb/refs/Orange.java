//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                           O r a n g e                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.refs;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Orange}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "orange")
public class Orange
        extends Fruit
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    final String name;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code Orange} object.
     *
     * @param id   DOCUMENT ME!
     * @param name DOCUMENT ME!
     */
    public Orange (String id,
                   String name)
    {
        super(id);
        this.name = name;
    }

    /**
     * Creates a new {@code Orange} object.
     */
    public Orange ()
    {
        this.name = null;
    }
}
