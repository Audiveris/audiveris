//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L i n k s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet.symbol;

import omr.sheet.Part;
import omr.sheet.SystemInfo;

import omr.sig.SigReducer;
import omr.sig.inter.AbstractHeadInter;
import omr.sig.inter.Inter;
import omr.sig.inter.SlurInter;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

import omr.util.HorizontalSide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Class {@code LinksStep} implements <b>LINKS</b> step, which assigns relations between
 * certain symbols and make a final reduction.
 *
 * @author Hervé Bitteur
 */
public class LinksStep
        extends AbstractSystemStep<Void>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(LinksStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code LinksStep} object.
     */
    public LinksStep ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Void context)
            throws StepException
    {
        new SymbolsLinker(system).process();
        new SigReducer(system, false).reduceLinks();

        dispatchSlursToParts(system);
        new InterCleaner(system).purgeContainers();
    }

    //----------------------//
    // dispatchSlursToParts //
    //----------------------//
    /**
     * Dispatch any slur to its containing part.
     *
     * @param system the system to process
     */
    private void dispatchSlursToParts (SystemInfo system)
    {
        final List<Inter> slurs = system.getSig().inters(SlurInter.class);

        for (Inter inter : slurs) {
            SlurInter slur = (SlurInter) inter;
            Part slurPart = null;

            for (HorizontalSide side : HorizontalSide.values()) {
                AbstractHeadInter head = slur.getHead(side);

                if (head != null) {
                    Part headPart = system.getPartOf(head.getStaff());

                    if (slurPart == null) {
                        slurPart = headPart;
                        slurPart.addSlur(slur);
                    } else if (slurPart != headPart) {
                        logger.warn("Slur crosses parts " + slur);
                    }
                }
            }
        }
    }
}
