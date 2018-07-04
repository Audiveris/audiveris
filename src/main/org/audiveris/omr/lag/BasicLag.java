//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c L a g                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.lag;

import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.run.Run;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.BasicIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Class {@code BasicLag} is a basic implementation of {@link Lag} interface.
 *
 * @author Hervé Bitteur
 */
public class BasicLag
        extends BasicIndex<Section>
        implements Lag
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(BasicLag.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Orientation of the lag. */
    private final Orientation orientation;

    /** Underlying runs table. */
    private RunTable runTable;

    /** Lag name. */
    private final String name;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Constructor with specified orientation
     *
     * @param name        the distinguished name for this instance
     * @param orientation the desired orientation of the lag
     */
    public BasicLag (String name,
                     Orientation orientation)
    {
        super(new AtomicInteger(0));
        this.name = name;
        this.orientation = orientation;

        logger.debug("Created lag {}", name);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------------//
    // addRunTable //
    //-------------//
    @Override
    public void addRunTable (RunTable runTable)
    {
        if (this.runTable == null) {
            this.runTable = runTable;
        } else {
            // Add runs into the existing table
            this.runTable.include(runTable);
        }
    }

    //---------//
    // getName //
    //---------//
    @Override
    public String getName ()
    {
        return name;
    }

    //----------------//
    // getOrientation //
    //----------------//
    @Override
    public Orientation getOrientation ()
    {
        return orientation;
    }

    //----------//
    // getRunAt //
    //----------//
    @Override
    public final Run getRunAt (int x,
                               int y)
    {
        return runTable.getRunAt(x, y);
    }

    //---------------//
    // getRunService //
    //---------------//
    @Override
    public SelectionService getRunService ()
    {
        return runTable.getRunService();
    }

    //-------------//
    // getRunTable //
    //-------------//
    @Override
    public RunTable getRunTable ()
    {
        return runTable;
    }

    //---------------------//
    // intersectedSections //
    //---------------------//
    @Override
    public Set<Section> intersectedSections (Rectangle rect)
    {
        return Sections.intersectedSections(rect, getEntities());
    }

    //------------//
    // isVertical //
    //------------//
    /**
     * Predicate on lag orientation
     *
     * @return true if vertical, false if horizontal
     */
    public boolean isVertical ()
    {
        return orientation.isVertical();
    }

    //----------------//
    // removeSections //
    //----------------//
    @Override
    public void removeSections (Collection<Section> sections)
    {
        for (Section section : sections) {
            // Make sure the section has not already been removed
            if (getEntity(section.getId()) == null) {
                logger.info("Section {} already removed", section);
            } else {
                // Remove the related runs from the underlying runTable
                int pos = section.getFirstPos();

                for (Run run : section.getRuns()) {
                    runTable.removeRun(pos++, run);
                }

                remove(section);
            }
        }
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void reset ()
    {
        super.reset(); // To clear sections & last ID
        runTable = null;
    }

    //---------//
    // setRuns //
    //---------//
    @Override
    public void setRuns (RunTable runTable)
    {
        if (this.runTable != null) {
            throw new RuntimeException("Attempt to overwrite lag runs table");
        } else {
            this.runTable = runTable;
        }
    }

    //-----------//
    // internals //
    //-----------//
    @Override
    protected String internals ()
    {
        StringBuilder sb = new StringBuilder(super.internals());

        // Orientation
        sb.append(" ").append(orientation);

        // Size
        sb.append(" sections:").append(entities.size());

        return sb.toString();
    }
}
