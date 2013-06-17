//----------------------------------------------------------------------------//
//                                                                            //
//                              B a s i c L a g                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.lag;

import omr.glyph.facets.Glyph;
import omr.glyph.ui.ViewParameters;

import omr.graph.BasicDigraph;

import omr.run.Orientation;
import omr.run.Run;
import omr.run.RunsTable;

import omr.selection.GlyphEvent;
import omr.selection.LagEvent;
import omr.selection.LocationEvent;
import omr.selection.MouseMovement;
import omr.selection.RunEvent;
import omr.selection.SectionEvent;
import omr.selection.SectionIdEvent;
import omr.selection.SectionSetEvent;
import omr.selection.SelectionHint;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.util.Predicate;

import org.bushe.swing.event.EventSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Class {@code BasicLag} is a basic implementation of {@link Lag}
 * interface.
 *
 * @author Hervé Bitteur
 */
public class BasicLag
        extends BasicDigraph<Lag, Section>
        implements Lag,
                   EventSubscriber<UserEvent>
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(BasicLag.class);

    /** Events read on location service */
    public static final Class[] locEventsRead = new Class<?>[]{LocationEvent.class};

    /** Events read on run service */
    public static final Class[] runEventsRead = new Class<?>[]{RunEvent.class};

    /** Events read on section service */
    public static final Class[] sctEventsRead = new Class<?>[]{
        SectionIdEvent.class,
        SectionEvent.class
    };

    //~ Instance fields --------------------------------------------------------
    /** Orientation of the lag */
    private final Orientation orientation;

    /** Underlying runs table */
    private RunsTable runsTable;

    /** Location service */
    private SelectionService locationService;

    /** Hosted section service */
    protected final SelectionService lagService;

    /** Scene service */
    private SelectionService glyphService;

    //~ Constructors -----------------------------------------------------------
    //----------//
    // BasicLag //
    //----------//
    /**
     * Constructor with specified orientation
     *
     * @param name        the distinguished name for this instance
     * @param orientation the desired orientation of the lag
     */
    public BasicLag (String name,
                     Orientation orientation)
    {
        this(name, BasicSection.class, orientation);
    }

    //----------//
    // BasicLag //
    //----------//
    /**
     * Constructor with specified orientation and section class
     *
     * @param name        the distinguished name for this instance
     * @param orientation the desired orientation of the lag
     */
    public BasicLag (String name,
                     Class<? extends Section> sectionClass,
                     Orientation orientation)
    {
        super(name, sectionClass);
        this.orientation = orientation;
        lagService = new SelectionService(name, Lag.eventsWritten);
    }

    //~ Methods ----------------------------------------------------------------
    //---------//
    // addRuns //
    //---------//
    @Override
    public void addRuns (RunsTable runsTable)
    {
        if (this.runsTable == null) {
            this.runsTable = runsTable.copy();
        } else {
            // Add runs into the existing table
            this.runsTable.include(runsTable);
        }
    }

    //---------------//
    // createSection //
    //---------------//
    @Override
    public Section createSection (int firstPos,
                                  Run firstRun)
    {
        if (firstRun == null) {
            throw new IllegalArgumentException("null first run");
        }

        Section section = createVertex();
        section.setFirstPos(firstPos);
        section.append(firstRun);

        return section;
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
        return runsTable.getRunAt(x, y);
    }

    //---------------//
    // getRunService //
    //---------------//
    @Override
    public SelectionService getRunService ()
    {
        return runsTable.getRunService();
    }

    //---------//
    // getRuns //
    //---------//
    @Override
    public RunsTable getRuns ()
    {
        return runsTable;
    }

    //-------------------//
    // getSectionService //
    //-------------------//
    @Override
    public SelectionService getSectionService ()
    {
        return lagService;
    }

    //-------------//
    // getSections //
    //-------------//
    @Override
    public final Collection<Section> getSections ()
    {
        return getVertices();
    }

    //--------------------//
    // getSelectedSection //
    //--------------------//
    @Override
    public Section getSelectedSection ()
    {
        return (Section) getSectionService().getSelection(SectionEvent.class);
    }

    //-----------------------//
    // getSelectedSectionSet //
    //-----------------------//
    @Override
    @SuppressWarnings("unchecked")
    public Set<Section> getSelectedSectionSet ()
    {
        return (Set<Section>) getSectionService().getSelection(
                SectionSetEvent.class);
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

    //---------------------------//
    // lookupIntersectedSections //
    //---------------------------//
    @Override
    public Set<Section> lookupIntersectedSections (Rectangle rect)
    {
        return Sections.lookupIntersectedSections(rect, getSections());
    }

    //----------------//
    // lookupSections //
    //----------------//
    @Override
    public Set<Section> lookupSections (Rectangle rect)
    {
        return Sections.lookupSections(rect, getSections());
    }

    //---------//
    // onEvent //
    //---------//
    @Override
    public void onEvent (UserEvent event)
    {
        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            if (event instanceof LocationEvent) {
                // Location => lassoed Section(s)
                handleEvent((LocationEvent) event);
            } else if (event instanceof RunEvent) {
                // Run => Section
                handleEvent((RunEvent) event);
            } else if (event instanceof SectionIdEvent) {
                // Section ID => Section
                handleEvent((SectionIdEvent) event);
            } else if (event instanceof SectionEvent) {
                // Section => contour & SectionSet update + Glyph?
                handleEvent((SectionEvent) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //---------//
    // publish //
    //---------//
    /**
     * Publish on Lag selection service
     *
     * @param event the event to publish
     */
    public void publish (LagEvent event)
    {
        lagService.publish(event);
    }

    //---------//
    // publish //
    //---------//
    /**
     * Publish a RunEvent on RunsTable service
     *
     * @param event the event to publish
     */
    public void publish (RunEvent event)
    {
        // Delegate to RunsTable
        getRunService().publish(event);
    }

    //---------//
    // publish //
    //---------//
    public void publish (LocationEvent locationEvent)
    {
        locationService.publish(locationEvent);
    }

    //---------------//
    // purgeSections //
    //---------------//
    @Override
    public List<Section> purgeSections (Predicate<Section> predicate)
    {
        // List of sections to be purged (to avoid concurrent modifications)
        List<Section> purges = new ArrayList<>(2000);

        // Iterate on all sections
        for (Section section : getSections()) {
            // Check predicate on the current section
            if (predicate.check(section)) {
                logger.debug("Purging {}", section);
                purges.add(section);
            }
        }

        // Now, actually perform the needed removals
        for (Section section : purges) {
            section.delete();

            // Remove the related runs from the underlying runsTable
            int pos = section.getFirstPos();

            for (Run run : section.getRuns()) {
                runsTable.removeRun(pos++, run);
            }
        }

        // Return the sections purged
        return purges;
    }

    //---------//
    // setRuns //
    //---------//
    @Override
    public void setRuns (RunsTable runsTable)
    {
        if (this.runsTable != null) {
            throw new RuntimeException("Attempt to overwrite lag runs table");
        } else {
            this.runsTable = runsTable;
        }
    }

    //-------------//
    // setServices //
    //-------------//
    @Override
    public void setServices (SelectionService locationService,
                             SelectionService sceneService)
    {
        this.locationService = locationService;
        this.glyphService = sceneService;

        runsTable.setLocationService(locationService);

        for (Class<?> eventClass : locEventsRead) {
            locationService.subscribeStrongly(eventClass, this);
        }

        for (Class<?> eventClass : runEventsRead) {
            getRunService().subscribeStrongly(eventClass, this);
        }

        for (Class<?> eventClass : sctEventsRead) {
            lagService.subscribeStrongly(eventClass, this);
        }
    }

    //-------------//
    // cutServices //
    //-------------//
    @Override
    public void cutServices ()
    {
        runsTable.cutLocationService(locationService);

        for (Class<?> eventClass : locEventsRead) {
            locationService.unsubscribe(eventClass, this);
        }

        for (Class<?> eventClass : runEventsRead) {
            getRunService().unsubscribe(eventClass, this);
        }

        for (Class<?> eventClass : sctEventsRead) {
            lagService.unsubscribe(eventClass, this);
        }
    }

    //-----------------//
    // internalsString //
    //-----------------//
    @Override
    protected String internalsString ()
    {
        StringBuilder sb = new StringBuilder(super.internalsString());

        // Orientation
        sb.append(" ").append(orientation);

        //        // Runs
        //        if (runsTable != null) {
        //            sb.append((" runs:"))
        //              .append(runsTable.getRunCount());
        //        }
        return sb.toString();
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in lasso SheetLocation => Section(s)
     *
     * @param sheetLocation
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        logger.debug("Lag. sheetLocation:{}", locationEvent);

        Rectangle rect = locationEvent.getData();

        if (rect == null) {
            return;
        }

        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;

        if (!hint.isLocation() && !hint.isContext()) {
            return;
        }

        // Section selection mode?
        if (ViewParameters.getInstance().isSectionMode()) {
            // Non-degenerated rectangle? 
            if ((rect.width > 0) && (rect.height > 0)) {
                // Look for enclosed sections
                Set<Section> sectionsFound = lookupSections(rect);

                // Publish (first) Section found
                Section section = sectionsFound.isEmpty() ? null
                        : sectionsFound.iterator().next();
                publish(new SectionEvent(this, hint, movement, section));

                // Publish whole SectionSet
                publish(
                        new SectionSetEvent(this, hint, movement, sectionsFound));
            }
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Run => Section
     *
     * @param run
     */
    private void handleEvent (RunEvent runEvent)
    {
        logger.debug("Lag. run:{}", runEvent);

        // Lookup for Section linked to this Run
        // Search and forward section info
        Run run = runEvent.getData();

        SelectionHint hint = runEvent.hint;
        MouseMovement movement = runEvent.movement;

        if (!hint.isLocation() && !hint.isContext()) {
            return;
        }

        // Publish Section information
        Section section = (run != null) ? run.getSection() : null;
        publish(new SectionEvent(this, hint, movement, section));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in SectionId => Section
     *
     * @param idEvent
     */
    private void handleEvent (SectionIdEvent idEvent)
    {
        Integer id = idEvent.getData();

        if ((id == null) || (id == 0)) {
            return;
        }

        SelectionHint hint = idEvent.hint;
        MouseMovement movement = idEvent.movement;

        // Always publish a null Run
        publish(new RunEvent(this, hint, movement, null));

        // Lookup a lag section with proper ID
        publish(new SectionEvent(this, hint, movement, getVertexById(id)));
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Section => section contour + update SectionSet
     *
     * @param sectionEvent
     */
    private void handleEvent (SectionEvent sectionEvent)
    {
        SelectionHint hint = sectionEvent.hint;
        MouseMovement movement = sectionEvent.movement;
        Section section = sectionEvent.getData();

        if (hint == SelectionHint.SECTION_INIT) {
            // Publish section contour
            publish(
                    new LocationEvent(
                    this,
                    hint,
                    null,
                    (section != null) ? section.getBounds() : null));
        }

        // In section-selection mode, update section set
        if (ViewParameters.getInstance().isSectionMode()) {
            // Section mode: Update section set
            Set<Section> sections = getSelectedSectionSet();

            if (sections == null) {
                sections = new LinkedHashSet<>();
            }

            if (hint == SelectionHint.LOCATION_ADD) {
                if (section != null) {
                    if (movement == MouseMovement.PRESSING) {
                        // Adding to (or Removing from) the set of sections
                        if (sections.contains(section)) {
                            sections.remove(section);
                        } else {
                            sections.add(section);
                        }
                    } else if (movement == MouseMovement.DRAGGING) {
                        // Always adding to the set of sections
                        sections.add(section);
                    }
                }
            } else {
                // Overwriting the set of sections
                if (section != null) {
                    // Make a one-section set
                    sections.clear();
                    sections.add(section);
                } else if (!sections.isEmpty()) {
                    // Empty the section set
                    sections.clear();
                }
            }

            logger.debug("{}. Publish section set {}", getName(), sections);
            publish(new SectionSetEvent(this, hint, movement, sections));
        } else if (glyphService != null) {
            // Section -> Glyph
            if (hint.isLocation() || hint.isContext() || hint.isSection()) {
                // Select related Glyph if any
                Glyph glyph = (section != null) ? section.getGlyph() : null;

                if (glyph != null) {
                    logger.debug("{}. Publish glyph {}", getName(), glyph);
                    glyphService.publish(
                            new GlyphEvent(this, hint, movement, glyph));
                }
            }
        }
    }
}
