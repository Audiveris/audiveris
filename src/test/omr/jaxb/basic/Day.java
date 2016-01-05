//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             D a y                                              //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.basic;

import java.util.List;

import javax.xml.bind.annotation.*;

/**
 * DOCUMENT ME!
 *
 * @author Hervé Bitteur
  */
public class Day
{
    //~ Instance fields ----------------------------------------------------------------------------

    @XmlAttribute
    public Weekday                             label;

    //
    @XmlElement(name = "meeting")
    List<Meeting> meetings;
}
