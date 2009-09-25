//----------------------------------------------------------------------------//
//                                                                            //
//                                u t i l . j s                               //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//      $Id$

// Extract a date out of a CVS tag
function getCVSDate(s)
{
    var as = s.split(" ");
    var d = new Date(as[3]);
    var months = new Array("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec");

    return d.getDate() + "-" + months[d.getMonth()] + "-" + d.getFullYear()
}

// Special nonav prefix for java.net hosted files                       
function nonav(url)
{
    if (document.title != "Audiveris Home Page")
        return "<A TARGET='_top' HREF='nonav/" + url + "'>";
    else
        return "<A TARGET='_top' HREF='" + url + "'>";
}
