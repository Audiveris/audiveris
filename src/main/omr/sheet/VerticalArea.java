//-----------------------------------------------------------------------//
//                                                                       //
//                        V e r t i c a l A r e a                        //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2005. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//
//      $Id$
package omr.sheet;

import omr.constant.Constant;
import omr.constant.ConstantSet;
import omr.glyph.GlyphLag;
import omr.stick.Stick;
import omr.stick.StickArea;
import omr.util.Logger;

import java.util.Collections;
import java.util.Comparator;

/**
 * Class <code>VerticalArea</code> processes a vertical lag to extract
 * vertical stick as potential candidates for bars, stems, ...
 * <p/>
 * <p/>
 * Input is a vertical lag. </p>
 * <p/>
 * <p/>
 * Output is a list of vertical sticks that represent good candidates. </p>
 */
public class VerticalArea
    extends StickArea
{
    //~ Static variables/initializers -------------------------------------

    // Class usual stuff
    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(VerticalArea.class);

    //~ Constructors ------------------------------------------------------

    //--------------//
    // VerticalArea //
    //--------------//

    /**
     * Build a family of vertical sticks, that will later be checked for
     * finer recognition
     *
     * @param sheet the sheet to process
     * @param vLag  the vertical lag from which bar sticks are built
     *
     * @throws omr.ProcessingException raised when step processing must
     *                                 stop, due to encountered error
     */
    public VerticalArea (Sheet sheet,
                         GlyphLag vLag,
                         int maxThickness)
            throws omr.ProcessingException
    {
        if (logger.isDebugEnabled()) {
            logger.debug("maxThickness=" + maxThickness);
        }

        // Retrieve the stick(s)
        Scale scale = sheet.getScale();
        initialize(vLag,
                   null,
                   new Source(vLag.getVertices()), // source for adequate sections
                   scale.fracToPixels(constants.coreSectionLength), // minCoreLength
                   constants.maxAdjacency.getValue(), // maxAdjacency
                   maxThickness, // maxThickness
                   constants.maxSlope.getValue(), // maxSlope
                   false); // longAlignment

        // Merge aligned verticals
        merge(scale.fracToPixels(constants.maxDeltaCoord),
              scale.fracToPixels(constants.maxDeltaPos),
              constants.maxDeltaSlope.getValue());

        // Sort sticks found
        Collections.sort(sticks,
                         new Comparator<Stick>()
                         {
                             public int compare (Stick s1,
                                                 Stick s2)
                             {
                                 return s1.getId() - s2.getId();
                             }
                         });

        if (logger.isDebugEnabled()) {
            logger.debug("End of scanning Vertical Area, found "
                         + sticks.size() + " stick(s)");
        }
    }

    //~ Classes -----------------------------------------------------------

    private static class Constants
            extends ConstantSet
    {
        Scale.Fraction coreSectionLength = new Scale.Fraction
                (2.0,
                 "Minimum length of a section to be processed");

        Constant.Double maxAdjacency = new Constant.Double
                (0.5d,
                 "Maximum adjacency ratio to be a true vertical line");

        Scale.Fraction maxDeltaCoord = new Scale.Fraction
                (0.25,
                 "Maximum difference of ordinates when merging two sticks");

        Scale.Fraction maxDeltaLength = new Scale.Fraction
                (0.25,
                 "Maximum difference in run length to be part of the same section");

        Scale.Fraction maxDeltaPos = new Scale.Fraction
                (0.2,
                 "Maximum difference of abscissa (in units) when merging two sticks");

        Constant.Double maxDeltaSlope = new Constant.Double
                (0.01d,
                 "Maximum difference in slope (in radians) when merging two sticks");

        Constant.Double maxSlope = new Constant.Double
                (0.04d,
                 "Maximum slope value for a stick to be vertical");

        Constants ()
        {
            initialize();
        }
    }
}
