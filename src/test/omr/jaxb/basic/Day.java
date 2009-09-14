//----------------------------------------------------------------------------//
//                                                                            //
//                                   D a y                                    //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.jaxb.basic;

import java.util.List;
import javax.xml.bind.annotation.*;

public class Day
{
    @XmlAttribute
    public Weekday label;
    //
    @XmlElement(name="meeting")
    List<Meeting> meetings;
}
