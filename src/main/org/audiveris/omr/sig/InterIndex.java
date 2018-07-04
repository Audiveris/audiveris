//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       I n t e r I n d e x                                      //
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

import org.audiveris.omr.OMR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.ui.InterService;
import org.audiveris.omr.util.BasicIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code InterIndex} keeps an index of all Inter instances registered
 * in a sheet, regardless of their containing system.
 *
 * @author Hervé Bitteur
 */
public class InterIndex
        extends BasicIndex<Inter>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(InterIndex.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new InterIndex object.
     */
    public InterIndex ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------------//
    // initTransients //
    //----------------//
    /**
     * Initialize needed transient members.
     * (which by definition have not been set by the unmarshalling).
     *
     * @param sheet the related sheet
     */
    public final void initTransients (Sheet sheet)
    {
        // Use sheet ID generator
        lastId = sheet.getPersistentIdGenerator();

        // Declared VIP IDs?
        setVipIds(constants.vipInters.getValue());

        // User Inter service?
        if (OMR.gui != null) {
            setEntityService(new InterService(this, sheet.getLocationService()));
        } else {
            entityService = null;
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.String vipInters = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP inters IDs");
    }
}
