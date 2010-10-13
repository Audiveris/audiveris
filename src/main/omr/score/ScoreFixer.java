//----------------------------------------------------------------------------//
//                                                                            //
//                            S c o r e F i x e r                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.Population;

import omr.score.common.PixelRectangle;
import omr.score.entity.Beam;
import omr.score.entity.BeamItem;
import omr.score.entity.Measure;
import omr.score.entity.ScoreSystem;
import omr.score.visitor.AbstractScoreVisitor;

import java.awt.Rectangle;

/**
 * Class <code>ScoreFixer</code> visits the score hierarchy to fix
 * internal data.
 * <ul>
 * <li>Assign Measure ids</li>
 * <li>Compute System contours</li>
 * <li>Compute average beam thickness</li>
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class ScoreFixer
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(ScoreFixer.class);

    //~ Instance fields --------------------------------------------------------

    /** Population of all beam thickness values */
    private Population beamPopulation = new Population();

    //~ Constructors -----------------------------------------------------------

    //------------//
    // ScoreFixer //
    //------------//
    /**
     * Creates a new ScoreFixer object.
     */
    public ScoreFixer ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // visit Beam //
    //------------//
    @Override
    public boolean visit (Beam beam)
    {
        // Cumulate heights of beam items
        for (BeamItem item : beam.getItems()) {
            Glyph glyph = item.getGlyph();
            beamPopulation.includeValue(
                (double) glyph.getWeight() / glyph.getContourBox().width);
        }

        return true;
    }

    //---------------//
    // visit Measure //
    //---------------//
    @Override
    public boolean visit (Measure measure)
    {
        // Set measure id, based on a preceding measure, whatever the part
        Measure precedingMeasure = measure.getPreceding();

        // No preceding system?
        if (precedingMeasure == null) {
            ScoreSystem prevSystem = (ScoreSystem) measure.getSystem()
                                                          .getPreviousSibling();

            if (prevSystem != null) { // No preceding part
                precedingMeasure = prevSystem.getFirstRealPart()
                                             .getLastMeasure();
            }
        }

        if (precedingMeasure != null) {
            measure.setId(precedingMeasure.getId() + 1);
        } else {
            // Very first measure
            measure.setId(measure.isImplicit() ? 0 : 1);
        }

        return true;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        score.acceptChildren(this);

        if (beamPopulation.getCardinality() > 0) {
            score.setBeamThickness(
                (int) Math.rint(beamPopulation.getMeanValue()));
        }

        return false;
    }

    //--------------//
    // visit System //
    //--------------//
    @Override
    public boolean visit (ScoreSystem system)
    {
        // Use system boundaries to define system contours
        Rectangle bounds = system.getInfo()
                                 .getBoundary()
                                 .getBounds();
        system.setDisplayContour(
            new PixelRectangle(bounds.x, bounds.y, bounds.width, bounds.height));

        return true;
    }
}
