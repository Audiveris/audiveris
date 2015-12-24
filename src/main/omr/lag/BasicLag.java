//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c L a g                                         //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunTable;

import omr.selection.SelectionService;

import omr.util.BasicIndex;
import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
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
        super(orientation.isVertical() ? "V" : "H");
        this.name = name;
        this.orientation = orientation;

        logger.debug("Created lag {}", name);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // getRunAt //
    //----------//
    @Override
    public final Run getRunAt (int x,
                               int y)
    {
        return runTable.getRunAt(x, y);
    }

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

    //---------------//
    // purgeSections //
    //---------------//
    @Override
    public List<Section> purgeSections (Predicate<Section> predicate)
    {
        // List of sections to be purged (to avoid concurrent modifications)
        List<Section> purges = new ArrayList<Section>(2000);

        // Iterate on all sections
        for (Section section : getEntities()) {
            // Check predicate on the current section
            if (predicate.check(section)) {
                logger.debug("Purging {}", section);
                purges.add(section);
            }
        }

        // Now, actually perform the needed removals
        for (Section section : purges) {
            ///section.delete();
            remove(section);

            // Remove the related runs from the underlying runsTable
            int pos = section.getFirstPos();

            for (Run run : section.getRuns()) {
                runTable.removeRun(pos++, run);
            }
        }

        // Return the sections purged
        return purges;
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
    public void setRuns (RunTable runsTable)
    {
        if (this.runTable != null) {
            throw new RuntimeException("Attempt to overwrite lag runs table");
        } else {
            this.runTable = runsTable;
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
//
//    //~ Inner Classes ------------------------------------------------------------------------------
//    //----------------//
//    // SectionService //
//    //----------------//
//    private class SectionService
//            extends EntityService<Section>
//    {
//        //~ Constructors ---------------------------------------------------------------------------
//
//        public SectionService ()
//        {
//            super(BasicLag.this, BasicLag.eventsAllowed);
//        }
//
//        //---------//
//        // onEvent //
//        //---------//
//        @Override
//        public void onEvent (UserEvent event)
//        {
//            try {
//                // Ignore RELEASING
//                if (event.movement == MouseMovement.RELEASING) {
//                    return;
//                }
//
//                super.onEvent(event); // Location, Id
//
//                if (event instanceof SectionIdEvent) {
//                    // Section ID => Section
//                    handleEvent((SectionIdEvent) event);
//                } else if (event instanceof SectionEvent) {
//                    // Section => contour & SectionSet update + Glyph?
//                    handleEvent((SectionEvent) event);
//                }
//            } catch (Exception ex) {
//                logger.warn(getClass().getName() + " onEvent error", ex);
//            }
//        }
//
//        //-------------//
//        // handleEvent //
//        //-------------//
//        /**
//         * Interest in lasso SheetLocation => Section(s)
//         *
//         * @param sheetLocation
//         */
//        private void handleEvent (LocationEvent locationEvent)
//        {
//            logger.debug("Lag. sheetLocation:{}", locationEvent);
//
//            Rectangle rect = locationEvent.getData();
//
//            if (rect == null) {
//                return;
//            }
//
//            SelectionHint hint = locationEvent.hint;
//            MouseMovement movement = locationEvent.movement;
//
//            if (!hint.isLocation() && !hint.isContext()) {
//                return;
//            }
//
//            // Section selection mode?
//            if (ViewParameters.getInstance().isSectionMode()) {
//                final Collection<Section> allSections = index.getEntities();
//                final Set<Section> sectionsFound;
//
//                if ((rect.width > 0) && (rect.height > 0)) {
//                    // Non-degenerated rectangle, look for enclosed sections
//                    sectionsFound = Entities.containedEntities(allSections, rect);
//                } else {
//                    // Just a point, look for containing section(s)
//                    sectionsFound = Entities.containingEntities(allSections, rect.getLocation());
//                }
//
//                // Publish whole section list
//                publish(
//                        new EntityListEvent<Section>(
//                                this,
//                                hint,
//                                movement,
//                                new ArrayList<Section>(sectionsFound)));
//            }
//        }
//
//        //-------------//
//        // handleEvent //
//        //-------------//
//        /**
//         * Interest in SectionId => Section
//         *
//         * @param idEvent
//         */
//        private void handleEvent (SectionIdEvent idEvent)
//        {
//            Integer id = idEvent.getData();
//
//            if ((id == null) || (id == 0)) {
//                return;
//            }
//
//            SelectionHint hint = idEvent.hint;
//            MouseMovement movement = idEvent.movement;
//
//            // Always publish a null Run
//            publish(new RunEvent(this, hint, movement, null));
//
//            // Lookup a lag section with proper ID
//            publish(new SectionEvent(this, hint, movement, getEntity(id)));
//        }
//
//        //-------------//
//        // handleEvent //
//        //-------------//
//        /**
//         * Interest in Section => section contour + update SectionSet
//         *
//         * @param sectionEvent
//         */
//        private void handleEvent (SectionEvent sectionEvent)
//        {
//            SelectionHint hint = sectionEvent.hint;
//            MouseMovement movement = sectionEvent.movement;
//            Section section = sectionEvent.getData();
//
//            if (hint == SelectionHint.SECTION_INIT) {
//                // Publish section contour
//                publish(
//                        new LocationEvent(
//                                this,
//                                hint,
//                                null,
//                                (section != null) ? section.getBounds() : null));
//            }
//
//            // In section-selection mode, update section set
//            if (ViewParameters.getInstance().isSectionMode()) {
//                // Section mode: Update section set
//                List<Section> sections = getSelectedEntityList();
//
//                if (sections == null) {
//                    sections = new ArrayList<Section>();
//                }
//
//                if (hint == SelectionHint.LOCATION_ADD) {
//                    if (section != null) {
//                        if (movement == MouseMovement.PRESSING) {
//                            // Adding to (or Removing from) the set of sections
//                            if (sections.contains(section)) {
//                                sections.remove(section);
//                            } else {
//                                sections.add(section);
//                            }
//                        } else if (movement == MouseMovement.DRAGGING) {
//                            // Always adding to the set of sections
//                            sections.add(section);
//                        }
//                    }
//                } else {
//                    // Overwriting the set of sections
//                    if (section != null) {
//                        // Make a one-section set
//                        sections.clear();
//                        sections.add(section);
//                    } else if (!sections.isEmpty()) {
//                        // Empty the section set
//                        sections.clear();
//                    }
//                }
//
//                logger.debug("{}. Publish section set {}", getName(), sections);
//                publish(new EntityListEvent<Section>(this, hint, movement, sections));
//            }
//        }
//    }
//}
//
//    //-------------//
//    // cutServices //
//    //-------------//
//    @Override
//    public void cutServices ()
//    {
//        if (runTable != null) {
//            RunService runService = runTable.getRunService();
//
//            if (runService != null) {
//                runService.cutLocationService(locationService);
//            }
//        }
//
//        for (Class<?> eventClass : locEventsRead) {
//            locationService.unsubscribe(eventClass, this);
//        }
//
//        for (Class<?> eventClass : sctEventsRead) {
//            entityService.unsubscribe(eventClass, this);
//        }
//    }
//
//
//    //-------------//
//    // setServices //
//    //-------------//
//    @Override
//    public void setServices (SelectionService locationService)
//    {
//        this.locationService = locationService;
//
//        if (runTable != null) {
//            RunService runService = runTable.getRunService();
//
//            if (runService == null) {
//                runTable.setRunService(runService = new RunService(getName() + "-runs", runTable));
//            }
//
//            runService.setLocationService(locationService);
//        }
//
//        for (Class<?> eventClass : locEventsRead) {
//            locationService.subscribeStrongly(eventClass, entityService);
//        }
//
//        for (Class<?> eventClass : sctEventsRead) {
//            entityService.subscribeStrongly(eventClass, entityService);
//        }
//    }
