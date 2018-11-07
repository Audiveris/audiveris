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
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.ui.InterService;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.SelectionHint;
import org.audiveris.omr.util.BasicIndex;
import org.audiveris.omr.util.IntUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import javax.swing.SwingUtilities;

/**
 * Class {@code InterIndex} keeps an index of all Inter instances registered
 * in a sheet, regardless of their containing system.
 *
 * @author Hervé Bitteur
 */
public class InterIndex
        extends BasicIndex<Inter>
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(InterIndex.class);

    /**
     * Creates a new InterManager object.
     */
    public InterIndex ()
    {
    }

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
        List<Integer> vipIds = IntUtil.parseInts(constants.vipInters.getValue());

        if (!vipIds.isEmpty()) {
            logger.info("VIP inters: {}", vipIds);
            setVipIds(vipIds);
        }

        // Browse inters from all SIGs to set VIPs
        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();

            for (Inter inter : sig.vertexSet()) {
                if (this.isVipId(inter.getId())) {
                    inter.setVip(true);
                }
            }
        }

        // User Inter service?
        if (OMR.gui != null) {
            setEntityService(new InterService(this, sheet.getLocationService()));
        } else {
            entityService = null;
        }
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return "interIndex";
    }

    //---------//
    // publish //
    //---------//
    /**
     * Convenient method to publish an Inter instance.
     *
     * @param inter the inter to publish (can be null)
     */
    public void publish (final Inter inter)
    {
        final EntityService<Inter> interService = this.getEntityService();

        if (interService != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    interService.publish(new EntityListEvent<Inter>(this, SelectionHint.ENTITY_INIT,
                                                                    MouseMovement.PRESSING, inter));
                }
            });
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {

        private final Constant.String vipInters = new Constant.String("",
                                                                      "(Debug) Comma-separated values of VIP inters IDs");
    }
}
