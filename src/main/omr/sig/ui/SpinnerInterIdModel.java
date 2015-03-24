//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              S p i n n e r I n t e r I d M o d e l                             //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sig.ui;

import omr.sig.SigManager;
import omr.sig.inter.Inter;
import static omr.ui.field.SpinnerUtil.NO_VALUE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractSpinnerModel;

/**
 * Class {@code SpinnerInterIdModel} is a spinner model backed by a {@link SigManager}
 * instance.
 *
 * @author Hervé Bitteur
 */
public class SpinnerInterIdModel
        extends AbstractSpinnerModel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(SpinnerInterIdModel.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Underlying SIG manager. */
    private final SigManager sigManager;

    /** Current inter id. */
    private int currentId = NO_VALUE;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new SpinnerInterIdModel object.
     *
     * @param sigManager underlying SIG manager
     */
    public SpinnerInterIdModel (SigManager sigManager)
    {
        this.sigManager = sigManager;
    }

    //~ Methods ------------------------------------------------------------------------------------
    @Override
    public Object getNextValue ()
    {
        Inter inter = sigManager.getInter(currentId + 1);

        if (inter != null) {
            return inter.getId();
        } else {
            return null;
        }
    }

    @Override
    public Object getPreviousValue ()
    {
        if (currentId == NO_VALUE) {
            return NO_VALUE;
        } else {
            Inter inter = sigManager.getInter(currentId - 1);

            if (inter != null) {
                return inter.getId();
            } else {
                return null;
            }
        }
    }

    @Override
    public Object getValue ()
    {
        return currentId;
    }

    @Override
    public void setValue (Object value)
    {
        Integer id = (Integer) value;
        boolean ok = false;

        if (id == NO_VALUE) {
            ok = true;
        } else {
            Inter inter = sigManager.getInter(id);

            if (inter != null) {
                ok = true;
            }
        }

        if (ok) {
            currentId = id;
            fireStateChanged();
        } else {
            logger.warn("Invalid inter id: {}", id);
        }
    }
}
