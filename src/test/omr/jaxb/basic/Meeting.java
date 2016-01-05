//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                         M e e t i n g                                          //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
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
