//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        H e a d s S t e p                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.step;

import omr.sheet.NoteSpotsBuilder;
import omr.glyph.facets.Glyph;

import omr.image.DistanceTable;

import omr.sheet.DistancesBuilder;
import omr.sheet.NoteHeadsBuilder;
import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Class {@code HeadsStep} implements <b>HEADS</b> step, which uses distance matching
 * technique to retrieve all possible interpretations of note heads (black and void) or
 * whole notes, but no rest notes.
 *
 * @author Hervé Bitteur
 */
public class HeadsStep
        extends AbstractSystemStep<HeadsStep.Context>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(HeadsStep.class);

    //~ Constructors -------------------------------------------------------------------------------
    //-----------//
    // HeadsStep //
    //-----------//
    /**
     * Creates a new {@code HeadsStep} object.
     */
    public HeadsStep ()
    {
        super(Steps.HEADS,
                Level.SHEET_LEVEL,
                Mandatory.MANDATORY,
                DATA_TAB,
                "Retrieve note heads & whole notes");
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // doSystem //
    //----------//
    @Override
    public void doSystem (SystemInfo system,
                          Context context)
            throws StepException
    {
        final List<Glyph> spots = context.sheetSpots.get(system);
        new NoteHeadsBuilder(system, context.distanceTable, spots).buildNotes();
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Collection<SystemInfo> systems,
                                Sheet sheet)
            throws StepException
    {
        // Build proper distance table and make it available for system-level processing
        DistanceTable distances = new DistancesBuilder(sheet).buildDistances();

        // Retrieve spots for notes
        Map<SystemInfo, List<Glyph>> sheetSpots = new NoteSpotsBuilder(sheet).getSpots();

        return new Context(distances, sheetSpots);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    public static class Context
    {
        //~ Instance fields ------------------------------------------------------------------------

        public final DistanceTable distanceTable;

        public final Map<SystemInfo, List<Glyph>> sheetSpots;

        //~ Constructors ---------------------------------------------------------------------------
        public Context (DistanceTable distanceTable,
                        Map<SystemInfo, List<Glyph>> sheetSpots)
        {
            this.distanceTable = distanceTable;
            this.sheetSpots = sheetSpots;
        }
    }
}
