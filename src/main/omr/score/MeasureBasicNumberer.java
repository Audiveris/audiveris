//----------------------------------------------------------------------------//
//                                                                            //
//                  M e a s u r e B a s i c N u m b e r e r                   //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.score.entity.Measure;
import omr.score.entity.Page;
import omr.score.visitor.AbstractScoreVisitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code MeasureBasicNumberer} visits a page hierarchy to
 * assign very basic measures ids.
 * These Ids are very basic (and temporary), ranging from 1 for the first
 * measure in the page.
 *
 * @author Hervé Bitteur
 */
public class MeasureBasicNumberer
        extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(
            MeasureBasicNumberer.class);

    //~ Constructors -----------------------------------------------------------
    //----------------------//
    // MeasureBasicNumberer //
    //----------------------//
    /**
     * Creates a new MeasureBasicNumberer object.
     */
    public MeasureBasicNumberer ()
    {
    }

    //~ Methods ----------------------------------------------------------------
    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        try {
            // Set measure id, based on a preceding measure, whatever the part
            Measure precedingMeasure = measure.getPrecedingInPage();

            if (precedingMeasure != null) {
                int precedingId = precedingMeasure.getIdValue();
                measure.setPageId(precedingId + 1, false);
            } else {
                // Very first measure (in this page)
                measure.setPageId(1, false);
            }
        } catch (Exception ex) {
            logger.warn(
                    getClass().getSimpleName() + " Error visiting " + measure,
                    ex);
        }

        return true;
    }

    //------------//
    // visit Page //
    //------------//
    @Override
    public boolean visit (Page page)
    {
        page.acceptChildren(this);

        // Temporary value
        page.setDeltaMeasureId(0);

        return false;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        score.acceptChildren(this);

        return false;
    }
}
