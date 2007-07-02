//----------------------------------------------------------------------------//
//                                                                            //
//                          V e r t i c a l A r e a                           //
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
import omr.glyph.GlyphSection;

import omr.stick.Stick;
import omr.stick.SticksBuilder;
import omr.stick.SticksSource;
import omr.stick.UnknownSectionPredicate;

import omr.util.Logger;
import omr.util.Predicate;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import omr.glyph.Glyph;
import omr.step.StepException;

/**
 * Class <code>VerticalArea</code> processes a vertical lag to extract vertical
 * stick as potential candidates for bars, stems, ...
 *
 * <p>Input is a vertical lag.</p>
 *
 * <p>Output is a list of vertical sticks that represent good candidates.</p>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class VerticalArea
    extends SticksBuilder
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(VerticalArea.class);

    //~ Constructors -----------------------------------------------------------

    //--------------//
    // VerticalArea //
    //--------------//
    /**
     * Build a family of vertical sticks, that will later be checked for finer
     * recognition, with default section predicate
     *
     * @param sheet the sheet to process
     * @param vLag  the vertical lag from which bar sticks are built
     * @param maxThickness max value for vertical stick thickness
     * @exception StepException raised when step processing must stop,
     *                                 due to encountered error
     */
    public VerticalArea (Sheet    sheet,
                         GlyphLag vLag,
                         int      maxThickness)
        throws StepException
    {
        this(sheet, vLag, new UnknownSectionPredicate(), maxThickness);
    }

    //--------------//
    // VerticalArea //
    //--------------//
    /**
     * Build a family of vertical sticks, that will later be checked for finer
     * recognition
     *
     * @param sheet the sheet to process
     * @param vLag  the vertical lag from which bar sticks are built
     * @param predicate a specific predicate for sections to consider
     * @param maxThickness max value for vertical stick thickness
     * @exception StepException raised when step processing must stop,
     *                                 due to encountered error
     */
    public VerticalArea (Sheet                   sheet,
                         GlyphLag                vLag,
                         Predicate<GlyphSection> predicate,
                         int                     maxThickness)
        throws StepException
    {
        this(vLag.getVertices(), sheet, vLag, predicate, maxThickness);
    }

    //--------------//
    // VerticalArea //
    //--------------//
    /**
     * Build a family of vertical sticks, that will later be checked for finer
     * recognition
     *
     * @param sections 
     * @param sheet the sheet to process
     * @param vLag  the vertical lag from which bar sticks are built
     * @param predicate a specific predicate for sections to consider
     * @param maxThickness max value for vertical stick thickness
     * @exception StepException raised when step processing must stop,
     *                                 due to encountered error
     */
    public VerticalArea (Collection<GlyphSection> sections,
                         Sheet                    sheet,
                         GlyphLag                 vLag,
                         Predicate<GlyphSection>  predicate,
                         int                      maxThickness)
        throws StepException
    {
        super(
            sheet,
            vLag,
            new SticksSource(sections, predicate), // source for adequate sections
            sheet.getScale().toPixels(constants.coreSectionLength), // minCoreLength
            constants.maxAdjacency.getValue(), // maxAdjacency
            maxThickness, // maxThickness
            constants.maxSlope.getValue(), // maxSlope
            false); // longAlignment

        if (logger.isFineEnabled()) {
            logger.fine("maxThickness=" + maxThickness);
        }

        // Retrieve the stick(s)
        createSticks(null);

        // Merge aligned verticals
        Scale scale = sheet.getScale();
        merge(
            scale.toPixels(constants.maxDeltaCoord),
            scale.toPixels(constants.maxDeltaPos),
            constants.maxDeltaSlope.getValue());

        // Sort sticks found (TBD: is this useful?)
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
                "End of scanning Vertical Area, found " + sticks.size() +
                " stick(s): " + Glyph.toString(sticks));
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    private static final class Constants
        extends ConstantSet
    {
        Scale.Fraction coreSectionLength = new Scale.Fraction(
            2.0,
            "Minimum length of a section to be processed");
        Constant.Ratio maxAdjacency = new Constant.Ratio(
            0.5d,
            "Maximum adjacency ratio to be a true vertical line");
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
            "Maximum slope value for a stick to be vertical");
    }
}
