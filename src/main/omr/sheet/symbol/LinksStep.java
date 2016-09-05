//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        L i n k s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
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
package omr.sheet.symbol;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.sheet.Part;
import omr.sheet.SystemInfo;

import omr.sig.SigReducer;
import omr.sig.inter.HeadInter;
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

    private static final Constants constants = new Constants();

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

        // Remove all free glyphs?
        if (constants.removeFreeGlyphs.isSet()) {
            system.clearFreeGlyphs();
        }
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
                HeadInter head = slur.getHead(side);

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

    //~ Inner Classes ------------------------------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean removeFreeGlyphs = new Constant.Boolean(
                true,
                "Should we remove all free glyphs?");
    }
}
