//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  O l d S t a f f B a r l i n e                                 //
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlIDREF;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlValue;

/**
 * Class {@code OldStaffBarline} is a temporary fix to keep compatibility with old
 * .omr files.
 * <p>
 * Replaced by {@link StaffBarlineInter}.
 *
 * @author Hervé Bitteur
 */
@Deprecated
@XmlAccessorType(XmlAccessType.NONE)
public class OldStaffBarline
{
    //~ Instance fields ----------------------------------------------------------------------------

    /** Abscissa-ordered sequence of physical barlines. */
    @XmlList
    @XmlIDREF
    @XmlValue
    public final ArrayList<BarlineInter> bars = new ArrayList<BarlineInter>();
}
