//-----------------------------------------------------------------------//
//                                                                       //
//                      H o r i z o n t a l A r e a                      //
//                                                                       //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.          //
//  This software is released under the terms of the GNU General Public  //
//  License. Please contact the author at herve.bitteur@laposte.net      //
//  to report bugs & suggestions.                                        //
//-----------------------------------------------------------------------//

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
 * Class <code>HorizontalArea</code> processes a horizontal lag to extract
 * horizontal stick as potential candidates for ledgers, alternate endings,
 * etc...
 *
 * <p> Input is a horizontal lag.
 *
 * <p/>Output is a list of horizontal sticks that represent good
 * candidates.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class HorizontalArea
    extends StickArea
{
    //~ Static variables/initializers -------------------------------------

    // Class usual stuff
    private static final Constants constants = new Constants();
    private static final Logger logger = Logger.getLogger(HorizontalArea.class);

    //~ Constructors ------------------------------------------------------

    //----------------//
    // HorizontalArea //
    //----------------//
    /**
     * Build a family of horizontal sticks, that will later be checked for
     * finer recognition
     *
     * @param sheet the sheet to process
     * @param hLag  the horizontal lag from which sticks are built
     *
     * @throws omr.ProcessingException raised when step processing must
     *                                 stop, due to encountered error
     */
    public HorizontalArea (Sheet sheet,
                           GlyphLag hLag,
                           int maxThickness)
            throws omr.ProcessingException
    {
        if (logger.isFineEnabled()) {
            logger.fine("maxThickness=" + maxThickness);
        }

        // Retrieve the stick(s)
        Scale scale = sheet.getScale();
        initialize(hLag,
                   null,
                   new Source(hLag.getVertices()), // source for adequate sections
                   scale.toPixels(constants.coreSectionLength), // minCoreLength
                   constants.maxAdjacency.getValue(), // maxAdjacency
                   maxThickness, // maxThickness
                   constants.maxSlope.getValue(), // maxSlope
                   false); // longAlignment

        // Merge aligned horizontals
        merge(scale.toPixels(constants.maxDeltaCoord),
              scale.toPixels(constants.maxDeltaPos),
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

        if (logger.isFineEnabled()) {
            logger.fine("End of scanning Horizontal Area, found "
                         + sticks.size() + " stick(s)");
        }
    }

    //~ Classes -----------------------------------------------------------

    private static class Constants
        extends ConstantSet
    {
        //~ Instance variables --------------------------------------------

        Scale.Fraction coreSectionLength = new Scale.Fraction
                (2.0,
                 "Minimum length of a section to be processed");

        Constant.Double maxAdjacency = new Constant.Double
                (0.5d,
                 "Maximum adjacency ratio to be a true horizontal line");

        Scale.Fraction maxDeltaCoord = new Scale.Fraction
                (0.25,
                 "Maximum difference of ordinates when merging two sticks");

        Scale.Fraction maxDeltaPos = new Scale.Fraction
                (0.2,
                 "Maximum difference of abscissa (in units) when merging two sticks");

        Constant.Double maxDeltaSlope = new Constant.Double
                (0.01d,
                 "Maximum difference in slope (in radians) when merging two sticks");

        Constant.Double maxSlope = new Constant.Double
                (0.04d,
                 "Maximum slope value for a stick to be horizontal");

        Constants ()
        {
            initialize();
        }
    }
}
