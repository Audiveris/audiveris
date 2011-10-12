//----------------------------------------------------------------------------//
//                                                                            //
//                            B e a m R e a d e r                             //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.score;

import omr.glyph.facets.Glyph;

import omr.log.Logger;

import omr.math.Population;

import omr.score.entity.Beam;
import omr.score.entity.BeamItem;
import omr.score.entity.Page;
import omr.score.visitor.AbstractScoreVisitor;

/**
 * Class <code>BeamReader</code> visits a score or page hierarchy to retrieve
 * the mean beam thickness per page.
 *
 * @author Hervé Bitteur
 */
public class BeamReader
    extends AbstractScoreVisitor
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(BeamReader.class);

    //~ Instance fields --------------------------------------------------------

    /** Population of all (page) beam thickness values */
    private Population pageBeamPopulation = new Population();

    //~ Constructors -----------------------------------------------------------

    //------------//
    // BeamReader //
    //------------//
    /**
     * Creates a new BeamReader object.
     */
    public BeamReader ()
    {
    }

    //~ Methods ----------------------------------------------------------------

    //------------//
    // visit Beam //
    //------------//
    @Override
    public boolean visit (Beam beam)
    {
        try {
            // Cumulate heights of beam items
            for (BeamItem item : beam.getItems()) {
                Glyph glyph = item.getGlyph();
                pageBeamPopulation.includeValue(
                    (double) glyph.getWeight() / glyph.getContourBox().width);
            }
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + beam,
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
        try {
            pageBeamPopulation.reset();
            page.acceptChildren(this);

            if (pageBeamPopulation.getCardinality() > 0) {
                page.setBeamThickness(
                    (int) Math.rint(pageBeamPopulation.getMeanValue()));
            }
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + page,
                ex);
        }

        return false;
    }

    //-------------//
    // visit Score //
    //-------------//
    @Override
    public boolean visit (Score score)
    {
        try {
            score.acceptChildren(this);
        } catch (Exception ex) {
            logger.warning(
                getClass().getSimpleName() + " Error visiting " + score,
                ex);
        }

        return false;
    }
}
