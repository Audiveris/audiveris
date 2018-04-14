//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    M e a s u r e F i l l e r                                   //
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
package org.audiveris.omr.sheet.rhythm;

import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.KeyInter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class {@code MeasureFiller} works at system level to fill each measure with its
 * relevant inters.
 *
 * @author Hervé Bitteur
 */
public class MeasureFiller
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(MeasureFiller.class);

    /** Filling classes. (Clefs and Key signatures) */
    public static final Class<?>[] FILLING_CLASSES = new Class<?>[]{ClefInter.class, KeyInter.class};

    //~ Instance fields ----------------------------------------------------------------------------
    /** Containing system. */
    private final SystemInfo system;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code MeasureFiller} object.
     *
     * @param system the containing system
     */
    public MeasureFiller (SystemInfo system)
    {
        this.system = system;
    }

    //~ Methods ------------------------------------------------------------------------------------
    public void process ()
    {
        // Lookup the relevant inters from system SIG
        final List<Inter> systemInters = system.getSig().inters(FILLING_CLASSES);

        // Dispatch system inters per stack & measure
        for (MeasureStack stack : system.getMeasureStacks()) {
            final List<Inter> stackInters = stack.filter(systemInters);

            for (Inter inter : stackInters) {
                Part part = inter.getPart();

                if (part != null) {
                    Measure measure = stack.getMeasureAt(part);
                    measure.addInter(inter);
                }
            }
        }
    }
}
