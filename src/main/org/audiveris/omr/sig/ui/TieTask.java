//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          T i e T a s k                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.sig.ui;

import org.audiveris.omr.sig.inter.SlurInter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class <code>TieTask</code> toggles the "tie" aspect of a slur.
 *
 * @author Hervé Bitteur
 */
public class TieTask
        extends InterTask
{

    //~ Static fields/initializers -----------------------------------------------------------------
    private static final Logger logger = LoggerFactory.getLogger(TieTask.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new <code>TieTask</code> object.
     *
     * @param slur the slur to be modified
     */
    public TieTask (SlurInter slur)
    {
        super(slur.getSig(), slur, null, null, "tie");
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public SlurInter getInter ()
    {
        return (SlurInter) inter;
    }

    @Override
    public void performDo ()
    {
        final SlurInter slur = getInter();
        final boolean tied = slur.isTie();
        slur.setTie(!tied);
        logger.info("Slur #{} tie set as: {}", slur.getId(), !tied);

        sheet.getInterIndex().publish(slur);
    }

    @Override
    public void performUndo ()
    {
        performDo(); // Since it's just a toggle
    }
}
