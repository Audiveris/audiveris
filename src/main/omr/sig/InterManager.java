//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     I n t e r M a n a g e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.selection.InterIdEvent;
import omr.selection.InterListEvent;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.inter.Inter;

import omr.util.IntUtil;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class {@code InterManager} keeps a dictionary of all Inter instances registered
 * in a sheet.
 *
 * @author Hervé Bitteur
 */
public class InterManager
        implements EventSubscriber<UserEvent>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            InterManager.class);

    /** Events that can be published on inter service. */
    public static final Class<?>[] allowedEvents = new Class<?>[]{
        InterListEvent.class, InterIdEvent.class
    };

    /** Events read by this class on inter service. */
    public static final Class<?>[] eventsRead = new Class<?>[]{
        InterListEvent.class, InterIdEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    private final Sheet sheet;

    /** Global id to uniquely identify an inter instance. */
    private final AtomicInteger globalInterId = new AtomicInteger(0);

    /** Collection of all inter instances ever identified in the sheet. */
    private final ConcurrentHashMap<Integer, Inter> allInters = new ConcurrentHashMap<Integer, Inter>();

    /** Selection service for Inter instances. */
    private final SelectionService interService;

    /** List of IDs for VIP inters. */
    private final List<Integer> vipInters;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new InterManager object.
     *
     * @param sheet the related sheet
     */
    public InterManager (Sheet sheet)
    {
        this.sheet = sheet;

        vipInters = IntUtil.parseInts(constants.vipInters.getValue());

        if (!vipInters.isEmpty()) {
            logger.info("VIP inters: {}", vipInters);
        }

        if (Main.getGui() != null) {
            interService = new SelectionService("inters", allowedEvents);

            // Subscriptions
            for (Class<?> eventClass : eventsRead) {
                interService.subscribeStrongly(eventClass, this);
            }

            sheet.getLocationService().subscribeStrongly(LocationEvent.class, this);
        } else {
            interService = null;
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

    //-----------------//
    // getInterService //
    //-----------------//
    /**
     * Report the service where Inter events are handled
     *
     * @return the interService
     */
    public SelectionService getInterService ()
    {
        return interService;
    }

    //----------------------//
    // getSelectedInterList //
    //----------------------//
    /**
     * Report the currently selected list of interpretations if any
     *
     * @return the current list or null
     */
    @SuppressWarnings("unchecked")
    public List<Inter> getSelectedInterList ()
    {
        return (List<Inter>) interService.getSelection(InterListEvent.class);
    }

    //-------//
    // isVip //
    //-------//
    public boolean isVip (Inter inter)
    {
        return vipInters.contains(inter.getId());
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (event instanceof LocationEvent) {
                // Location => InterList
                handleEvent((LocationEvent) event);
            } else if (event instanceof InterListEvent) {
                // InterList => contour
                handleEvent((InterListEvent) event);
            } else if (event instanceof InterIdEvent) {
                // InterId => inter
                handleEvent((InterIdEvent) event);
            }
        } catch (ConcurrentModificationException cme) {
            // This can happen because of processing being done on SIG...
            // So, just abort the current UI stuff
            throw cme;
        } catch (Throwable ex) {
            logger.warn(getClass().getSimpleName() + " onEvent error " + ex, ex);
        }
    }

    //----------//
    // register //
    //----------//
    /**
     * Assign a unique id (within this InterManager instance) to the provided inter.
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

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in sheet location => interpretation(s)
     *
     * @param locationEvent
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;
        Rectangle rect = locationEvent.getData();

        if (!hint.isLocation() && !hint.isContext()) {
            return;
        }

        if (rect == null) {
            return;
        }

        final Set<Inter> inters = new LinkedHashSet<Inter>();

        for (SystemInfo system : sheet.getSystemManager().getSystemsOf(rect.getLocation())) {
            SIGraph sig = system.getSig();

            if ((rect.width > 0) && (rect.height > 0)) {
                // This is a non-degenerated rectangle
                // Look for contained interpretations
                inters.addAll(sig.containedInters(rect));
            } else {
                // This is just a point
                // Look for intersected interpretations
                inters.addAll(sig.containingInters(rect.getLocation()));
            }
        }

        // Publish inters found (perhaps none)
        interService.publish(
                new InterListEvent(this, hint, movement, new ArrayList<Inter>(inters)));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Inter => inter contour
     *
     * @param interListEvent
     */
    private void handleEvent (InterListEvent interListEvent)
    {
        SelectionHint hint = interListEvent.hint;
        MouseMovement movement = interListEvent.movement;
        List<Inter> inters = interListEvent.getData();

        if (hint == SelectionHint.INTER_INIT) {
            // Display (last) inter contour
            if ((inters != null) && !inters.isEmpty()) {
                Inter inter = inters.get(inters.size() - 1);
                Rectangle box = inter.getBounds();
                sheet.getLocationService().publish(new LocationEvent(this, hint, movement, box));
            }
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in InterId => inter
     *
     * @param interIdEvent
     */
    private void handleEvent (InterIdEvent interIdEvent)
    {
        SelectionHint hint = interIdEvent.hint;
        MouseMovement movement = interIdEvent.movement;
        int interId = interIdEvent.getData();

        Inter inter = getInter(interId);
        interService.publish(
                new InterListEvent(this, hint, movement, (inter != null) ? Arrays.asList(inter) : null));
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
