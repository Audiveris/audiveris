//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        X a c t T o p i c                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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
package org.audiveris.omr.score.ui;

import java.util.ArrayList;

/**
 * Class <code>XactTopic</code> gathers several XactPane instances under a main topic.
 *
 * @author Hervé Bitteur
 */
public class XactTopic
        extends ArrayList<XactPane>
{
    //~ Instance fields ----------------------------------------------------------------------------

    public final String name;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>XactTopic</code> object.
     *
     * @param name the topic name
     */
    public XactTopic (String name)
    {
        this.name = name;
    }
}
