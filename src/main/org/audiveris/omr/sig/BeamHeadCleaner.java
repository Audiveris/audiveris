//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                  B e a m H e a d C l e a n e r                                 //
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
package org.audiveris.omr.sig;

import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.relation.BeamHeadRelation;
import org.audiveris.omr.sig.relation.Relation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * Class {@code BeamHeadCleaner} remove all BeamHeadRelation links of a system
 * when they are no longer needed.
 *
 * @author Hervé Bitteur
 */
public class BeamHeadCleaner
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BeamHeadCleaner.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related system. */
    private final SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code BeamHeadCleaner} object.
     *
     * @param system the system to process
     */
    public BeamHeadCleaner (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // process //
    //---------//
    public void process ()
    {
        SIGraph sig = system.getSig();
        Set<Relation> set = SIGraph.getRelations(sig.edgeSet(), BeamHeadRelation.class);
        logger.debug("System#{} BeamHeadRelation instances: {}", system.getId(), set.size());
        sig.removeAllEdges(set);
    }
}
