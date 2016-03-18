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
import omr.glyph.GlyphFactory;
import omr.glyph.Glyphs;

import omr.ui.ViewParameters;
import omr.ui.selection.EntityListEvent;
import omr.ui.selection.EntityService;
import omr.ui.selection.IdEvent;
import omr.ui.selection.LocationEvent;
import omr.ui.selection.MouseMovement;
import omr.ui.selection.SelectionHint;

import static omr.ui.selection.SelectionHint.*;

import omr.ui.selection.SelectionService;
import omr.ui.selection.UserEvent;

import omr.util.Entities;
import omr.util.EntityIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Class {@code GlyphService} is an EntityService for glyphs.
 *
 * @author Hervé Bitteur
 */
public class GlyphService
        extends EntityService<Glyph>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphService.class);

    /** Events that can be published on a glyph service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{
        IdEvent.class, EntityListEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** Manual and incremental user selection of glyphs. */
    private final Set<Glyph> basket = new HashSet<Glyph>();

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

            if (event instanceof LocationEvent) {
                handleEvent((LocationEvent) event); // Location -> basket
            } else {
                super.onEvent(event); // Id, List contour
            }
        } catch (Throwable ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in location => list (basket?)
     *
     * @param locationEvent
     */
    private void handleEvent (LocationEvent locationEvent)
    {
        // In glyph-selection mode?
        if (ViewParameters.getInstance().isSectionMode()) {
            return;
        }

        SelectionHint hint = locationEvent.hint;
        MouseMovement movement = locationEvent.movement;
        Rectangle rect = locationEvent.getData();

        if (!hint.isLocation() && !hint.isContext()) {
            return;
        }

        if (rect == null) {
            return;
        }

        final Set<Glyph> found;

        if ((rect.width > 0) && (rect.height > 0)) {
            // Non-degenerated rectangle: look for contained entities
            found = Entities.containedEntities(index.iterator(), rect);
            publish(
                    new EntityListEvent<Glyph>(this, hint, movement, new ArrayList<Glyph>(found)));
        } else {
            // Just a point: look for smallest containing entity
            found = Entities.containingEntities(index.iterator(), rect.getLocation());

            // Specific behavior for displayed glyph
            ArrayList<Glyph> list = new ArrayList<Glyph>(found);
            Glyph glyph = null;

            if (!list.isEmpty()) {
                // Pick up the smallest containing glyph
                Collections.sort(list, Glyphs.byWeight);
                glyph = list.get(0);
            }

            // Update basket
            switch (hint) {
            case LOCATION_INIT:

                if (glyph != null) {
                    basket.clear();
                    basket.add(glyph);
                } else {
                    basket.clear();
                }

                break;

            case LOCATION_ADD:

                if (glyph != null) {
                    if (basket.contains(glyph)) {
                        basket.remove(glyph);
                    } else {
                        basket.add(glyph);
                    }
                }

                break;

            case CONTEXT_INIT:

                if (glyph != null) {
                    if (!basket.contains(glyph)) {
                        basket.clear();
                        basket.add(glyph);
                    }
                } else {
                    basket.clear();
                }

                break;

            case CONTEXT_ADD:
            default:
            }

            // Publish basket
            publish(
                    new EntityListEvent<Glyph>(
                            this,
                            SelectionHint.GLYPH_TRANSIENT,
                            movement,
                            new ArrayList<Glyph>(basket)));

            if (basket.size() > 1) {
                // Build compound on-the-fly and publish it
                Glyph compound = GlyphFactory.buildGlyph(basket);
                publish(
                        new EntityListEvent<Glyph>(
                                this,
                                SelectionHint.GLYPH_TRANSIENT,
                                movement,
                                Arrays.asList(compound)));
            }
        }
    }
}
