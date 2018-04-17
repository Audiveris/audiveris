//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     C h o r d s L i n k e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.sheet.note;

import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.beam.BeamGroup;
import org.audiveris.omr.sheet.rhythm.MeasureStack;
import org.audiveris.omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code ChordsLinker} works at system level to handle relations between chords
 * and other entities.
 * <p>
 * These relationships can be addressed only when ALL system chord candidates have been retrieved.
 *
 * @author Hervé Bitteur
 */
public class ChordsLinker
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(ChordsLinker.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code ChordsLinker} object.
     *
     * @param system the dedicated system
     */
    public ChordsLinker (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------------//
    // linkChords //
    //------------//
    public void linkChords ()
    {
        // Allocate beam groups per stack
        for (MeasureStack stack : system.getStacks()) {
            BeamGroup.populate(stack);
        }
    }
}
