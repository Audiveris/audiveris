//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c L a g                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunTable;

import omr.ui.selection.SelectionService;

import omr.util.BasicIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.Collection;
import java.util.Set;

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

    //--------//
    // remove //
    //--------//
    @Override
    public void remove (Section section)
    {
        super.remove(section); // Removal from index
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
