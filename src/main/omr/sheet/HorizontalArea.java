//----------------------------------------------------------------------------//
//                                                                            //
//                        H o r i z o n t a l A r e a                         //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;

import omr.step.StepException;

import omr.stick.Stick;
import omr.stick.SticksBuilder;
import omr.stick.SticksSource;

import omr.util.Logger;

import java.util.*;

/**
 * Class <code>HorizontalArea</code> processes a horizontal lag to extract
 * horizontal stick as potential candidates for ledgers, alternate endings,
 * etc...
 *
 * <p> Input is a horizontal lag.
 *
 * <p/>Output is a list of horizontal sticks that represent good candidates.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class HorizontalArea
    extends SticksBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(HorizontalArea.class);

    //~ Constructors -----------------------------------------------------------

    //----------------//
    // HorizontalArea //
    //----------------//
    /**
     * Build a family of horizontal sticks, that will later be checked for finer
     * recognition
     *
     * @param sheet the sheet to process
     * @param hLag  the horizontal lag from which sticks are built
     * @param maxThickness maximum thickness accepted
     * @throws StepException raised when step processing must stop, due to some
     * encountered error
     */
    public HorizontalArea (Sheet    sheet,
                           GlyphLag hLag,
                           int      maxThickness)
        throws StepException
    {
        super(
            sheet,
            hLag,
            new SticksSource(hLag.getVertices()), // source for adequate sections
            sheet.getScale().toPixels(constants.coreSectionLength), // minCoreLength
            constants.maxAdjacency.getValue(), // maxAdjacency
            maxThickness, // maxThickness
            constants.maxSlope.getValue(), // maxSlope
            false); // longAlignment);

        if (logger.isFineEnabled()) {
            logger.fine("maxThickness=" + maxThickness);
        }

        // Retrieve the stick(s)
        Scale scale = sheet.getScale();
        createSticks(null);

        // Merge aligned horizontals
        merge(
            scale.toPixels(constants.maxDeltaCoord),
            scale.toPixels(constants.maxDeltaPos),
            constants.maxDeltaSlope.getValue());

        // Sort sticks found
        Collections.sort(
            sticks,
            new Comparator<Stick>() {
                    public int compare (Stick s1,
                                        Stick s2)
                    {
                        return s1.getId() - s2.getId();
                    }
                });

        if (logger.isFineEnabled()) {
            logger.fine(
                "End of scanning Horizontal Area, found " + sticks.size() +
                " stick(s)");
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction coreSectionLength = new Scale.Fraction(
            2.0,
            "Minimum length of a section to be processed");
        Constant.Ratio maxAdjacency = new Constant.Ratio(
            0.5d,
            "Maximum adjacency ratio to be a true horizontal line");
        Scale.Fraction maxDeltaCoord = new Scale.Fraction(
            0.25,
            "Maximum difference of ordinates when merging two sticks");
        Scale.Fraction maxDeltaPos = new Scale.Fraction(
            0.2,
            "Maximum difference of abscissa (in units) when merging two sticks");
        Constant.Angle maxDeltaSlope = new Constant.Angle(
            0.01d,
            "Maximum difference in slope (in radians) when merging two sticks");
        Constant.Angle maxSlope = new Constant.Angle(
            0.04d,
            "Maximum slope value for a stick to be horizontal");
    }
}
