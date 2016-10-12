//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        H e a d s S t e p                                       //
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
package omr.sheet.note;

import omr.glyph.Glyph;

import omr.image.DistanceTable;

import omr.sheet.Sheet;
import omr.sheet.SystemInfo;

import omr.step.AbstractSystemStep;
import omr.step.StepException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    /**
     * Creates a new {@code HeadsStep} object.
     */
    public HeadsStep ()
    {
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
        new NoteHeadsBuilder(system, context.distanceTable, spots).buildHeads();
    }

    //----------//
    // doProlog //
    //----------//
    @Override
    protected Context doProlog (Sheet sheet)
            throws StepException
    {
        // Build proper distance table and make it available for system-level processing
        DistanceTable distances = new DistancesBuilder(sheet).buildDistances();

        // Retrieve spots for notes
        Map<SystemInfo, List<Glyph>> sheetSpots = new NoteSpotsBuilder(sheet).getSpots();

        return new Context(distances, sheetSpots);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Context //
    //---------//
    protected static class Context
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
