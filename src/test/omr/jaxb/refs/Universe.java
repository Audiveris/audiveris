//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         U n i v e r s e                                        //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.refs;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Universe}
 *
 * @author Hervé Bitteur
 */
@XmlRootElement
public class Universe
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlElement
    Store store = new Store();

    @XmlElement
    Basket basket = new Basket();
}
