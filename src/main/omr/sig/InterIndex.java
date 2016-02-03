//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       I n t e r I n d e x                                      //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig;

import omr.OMR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.sig.inter.Inter;
import omr.sig.ui.InterService;

import omr.ui.selection.EntityListEvent;
import omr.ui.selection.EntityService;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.SelectionHint;

import omr.util.BasicIndex;
import omr.util.IdUtil;
import omr.util.IntUtil;
import omr.util.Navigable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import javax.swing.SwingUtilities;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code InterIndex} keeps an index of all Inter instances registered
 * in a sheet, regardless of their containing system.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "inter-index")
@XmlJavaTypeAdapter(InterIndex.Adapter.class)
@XmlAccessorType(XmlAccessType.NONE)
public class InterIndex
        extends BasicIndex<Inter>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(InterIndex.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Related sheet. */
    @Navigable(false)
    private Sheet sheet;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new InterManager object.
     *
     * @param sheet the related sheet
     */
    public InterIndex (Sheet sheet)
    {
        this();

        initTransients(sheet);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private InterIndex ()
    {
        super("");
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
        this.sheet = sheet;

        // Declared VIP IDs?
        List<Integer> vipIds = IntUtil.parseInts(constants.vipInters.getValue());

        if (!vipIds.isEmpty()) {
            logger.info("VIP inters: {}", vipIds);
            setVipIds(vipIds);
        }

        // Collect inters from all SIGs
        for (SystemInfo system : sheet.getSystems()) {
            SIGraph sig = system.getSig();

            for (Inter inter : sig.vertexSet()) {
                if (this.isVipId(inter.getId())) {
                    inter.setVip(true);
                }
            }
        }

        // User Inter service?
        if (OMR.getGui() != null) {
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
        return "inters";
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
                    interService.publish(
                            new EntityListEvent(
                                    this,
                                    SelectionHint.ENTITY_INIT,
                                    MouseMovement.PRESSING,
                                    (inter != null) ? Arrays.asList(inter) : null));
                }
            });
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    public static class Adapter
            extends XmlAdapter<InterIndexValue, InterIndex>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public InterIndexValue marshal (InterIndex index)
                throws Exception
        {
            return new InterIndexValue(index.getPrefix(), IdUtil.getIntValue(index.getLastId()));
        }

        @Override
        public InterIndex unmarshal (InterIndexValue shell)
                throws Exception
        {
            InterIndex index = new InterIndex();
            index.setLastId(shell.prefix + shell.lastIdValue);

            return index;
        }
    }

    //-----------------//
    // InterIndexValue //
    //-----------------//
    @XmlRootElement(name = "inter-index")
    public static class InterIndexValue
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlAttribute(name = "prefix")
        private String prefix;

        @XmlAttribute(name = "last-id-value")
        private int lastIdValue;

        //~ Constructors ---------------------------------------------------------------------------
        public InterIndexValue ()
        {
        }

        public InterIndexValue (String prefix,
                                int lastIdValue)
        {
            this.prefix = prefix;
            this.lastIdValue = lastIdValue;
        }
    }

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
