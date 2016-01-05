//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                     G l y p h S e r v i c e                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.glyph.ui;

import omr.glyph.Glyph;
import omr.glyph.Symbol;

import omr.selection.EntityListEvent;
import omr.selection.EntityService;
import omr.selection.GroupEvent;
import omr.selection.IdEvent;
import omr.selection.MouseMovement;
import omr.selection.SelectionService;
import omr.selection.UserEvent;

import omr.util.EntityIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code GlyphService} is an EntityService for glyphs.
 * TODO: investigate need for implementation of add/remove items for a compound glyph?
 *
 * @author Hervé Bitteur
 */
public class GlyphService
        extends EntityService<Glyph>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphService.class);

    /** Events that can be published on a nest service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{
        IdEvent.class, EntityListEvent.class,
        GroupEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Current group used for UI lookup (null means no specific group). */
    private Symbol.Group currentGroup;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code GlyphService} object.
     *
     * @param index           underlying glyph index (typically a nest)
     * @param locationService related service for location info
     */
    public GlyphService (EntityIndex<Glyph> index,
                         SelectionService locationService)
    {
        super(index, locationService, eventsAllowed);

        //        // Larger cache for EntityListEvent ???
        //        setCacheSizeForEventClass(EntityListEvent.class, 1); //TODO: certainly more!
    }

    //~ Methods ------------------------------------------------------------------------------------
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

            super.onEvent(event); // Id, location

            if (event instanceof GroupEvent) {
                handleEvent((GroupEvent) event); // Group
            }
        } catch (Throwable ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //
    //    //-------------//
    //    // handleEvent //
    //    //-------------//
    //    /**
    //     * Interest in sheet location => glyph(s)
    //     *
    //     * @param locationEvent
    //     */
    //    private void handleEvent (LocationEvent locationEvent)
    //    {
    //        SelectionHint hint = locationEvent.hint;
    //        MouseMovement movement = locationEvent.movement;
    //        Rectangle rect = locationEvent.getData();
    //
    //        if (!hint.isLocation() && !hint.isContext()) {
    //            return;
    //        }
    //
    //        if (rect == null) {
    //            return;
    //        }
    //
    //        // Glyphs to be browsed
    //        final Collection<Glyph> source = ((BasicNest) index).getGlyphCollection(currentGroup);
    //        final Set<Glyph> found;
    //
    //        if ((rect.width > 0) && (rect.height > 0)) {
    //            // Non-degenerated rectangle: look for enclosed glyphs in current group (or all)
    //            found = Glyphs.containedGlyphs(source, rect);
    //        } else {
    //            // Just a point: look for containing glyphs in current group (or all)
    //            found = Glyphs.containingGlyphs(source, rect.getLocation());
    //        }
    //
    //        // Publish GlyphList
    //        publish(
    //                new EntityListEvent<Glyph>(this, hint, movement, new ArrayList<Glyph>(found)));
    //    }
    //
    //    //-------------//
    //    // handleEvent //
    //    //-------------//
    //    /**
    //     * Interest in Glyph => glyph contour & GlyphSet update
    //     *
    //     * @param listEvent
    //     */
    //    private void handleEvent (EntityListEvent<Glyph> listEvent)
    //    {
    //        final SelectionHint hint = listEvent.hint;
    //        final MouseMovement movement = listEvent.movement;
    //
    //        if ((hint == SelectionHint.GLYPH_INIT) || (hint == SelectionHint.GLYPH_MODIFIED)) {
    //            final Glyph glyph = listEvent.getEntity();
    //
    //            // Display glyph contour
    //            if (glyph != null) {
    //                Rectangle box = glyph.getBounds();
    //                locationService.publish(new LocationEvent(this, hint, movement, box));
    //            }
    //        }
    //
    //        //TODO: handle add/remove to selected set of glyphs
    //        //
    //        //
    //        //            // In glyph-selection mode, for non-transient glyphs
    //        //            // (and only if we have interested subscribers)
    //        //            if ((hint != GLYPH_TRANSIENT)
    //        //                && !ViewParameters.getInstance().isSectionMode()
    //        //                && (subscribersCount(EntityListEvent.class) > 0)) {
    //        //                // Update glyph set
    //        //                //                List cached = entityService.getCachedEvents(EntityListEvent.class);
    //        //                //
    //        //                Set<Glyph> set = getSelectedGlyphList();
    //        //
    //        //                if (set == null) {
    //        //                    set = new LinkedHashSet<Glyph>();
    //        //                }
    //        //
    //        //                if (hint == LOCATION_ADD) {
    //        //                    // Adding to (or Removing from) the set of glyphs
    //        //                    if (glyph != null) {
    //        //                        if (set.contains(glyph)) {
    //        //                            set.remove(glyph);
    //        //                        } else {
    //        //                            set.add(glyph);
    //        //                        }
    //        //                    }
    //        //                } else if (hint == CONTEXT_ADD) {
    //        //                    // Don't modify the set
    //        //                } else {
    //        //                    // Overwriting the set of glyphs
    //        //                    if (glyph != null) {
    //        //                        // Make a one-glyph set
    //        //                        set = Glyphs.sortedSet(glyph);
    //        //                    } else {
    //        //                        // Make an empty set
    //        //                        set = Glyphs.sortedSet();
    //        //                    }
    //        //                }
    //        //
    //        //                publish(new GlyphSetEvent(this, hint, movement, set));
    //        //            }
    //    }
    //
    //
    //        //-------------//
    //        // handleEvent //
    //        //-------------//
    //        /**
    //         * Interest in GlyphList=> prebuild a compound
    //         *
    //         * @param glyphSetEvent
    //         */
    //        private void handleEvent (GlyphSetEvent glyphSetEvent)
    //        {
    //            if (ViewParameters.getInstance().isSectionMode()) {
    //                // Section mode
    //                return;
    //            }
    //
    //            // Glyph mode
    //            MouseMovement movement = glyphSetEvent.movement;
    //            Set<Glyph> glyphs = glyphSetEvent.getData();
    //
    //            if ((glyphs != null) && (glyphs.size() > 1)) {
    //                Glyph compound = buildGlyph(glyphs, false);
    //                publish(new GlyphEvent(this, SelectionHint.GLYPH_TRANSIENT, movement, compound));
    //            }
    //        }
    //
    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in Group
     *
     * @param groupEvent
     */
    private void handleEvent (GroupEvent groupEvent)
    {
        // Update current group for future lookup
        currentGroup = groupEvent.getData();

        //
        //        // Should we publish a new glyph selection, according to new group?
        //        // Forge a new location event (to override the RELEASING movement) & publish it
        //        if (locationService != null) {
        //            LocationEvent ev = (LocationEvent) locationService.getLastEvent(LocationEvent.class);
        //
        //            if (ev != null) {
        //                locationService.publish(
        //                        new LocationEvent(this, ev.hint, MouseMovement.PRESSING, ev.getData()));
        //            }
        //        }
    }
}
