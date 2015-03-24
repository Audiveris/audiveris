//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       S i g M a n a g e r                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sig.inter.Inter;

import omr.util.IntUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class {@code SigManager} handles the system SIG's of a sheet.
 *
 * @author Hervé Bitteur
 */
public class SigManager
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            SigManager.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Global id to uniquely identify an inter instance. */
    private final AtomicInteger globalInterId = new AtomicInteger(0);

    /**
     * Collection of all inter instances ever identified in the sheet.
     */
    private final ConcurrentHashMap<Integer, Inter> allInters = new ConcurrentHashMap<Integer, Inter>();

    /** List of IDs for VIP inters. */
    private final List<Integer> vipInters;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SigManager object.
     */
    public SigManager ()
    {
        vipInters = IntUtil.parseInts(constants.vipInters.getValue());

        if (!vipInters.isEmpty()) {
            logger.info("VIP inters: {}", vipInters);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getInter //
    //----------//
    /**
     * Report the inter instance corresponding to the provided id.
     *
     * @param id the provided id
     * @return the corresponding inter instance, or null if not found
     */
    public Inter getInter (int id)
    {
        return allInters.get(id);
    }

    //-------//
    // isVip //
    //-------//
    public boolean isVip (Inter inter)
    {
        return vipInters.contains(inter.getId());
    }

    //----------//
    // register //
    //----------//
    /**
     * Assign a unique id (within this SigManager instance) to the
     * provided interpretation.
     *
     * @param inter the provided inter
     * @return the assigned unique id
     */
    public int register (Inter inter)
    {
        int id = globalInterId.incrementAndGet();
        inter.setId(id);

        allInters.put(id, inter);

        if (isVip(inter)) {
            inter.setVip();
            logger.info("VIP inter {} registered", inter);
        }

        return id;
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        Constant.String vipInters = new Constant.String(
                "",
                "(Debug) Comma-separated values of VIP inters IDs");
    }
}
