//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G l y p h B o a r d                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Hervé Bitteur and others 2000-2017. All rights reserved.
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
package org.audiveris.omr.glyph.ui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Symbol.Group;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.EntityBoard;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.GroupEvent;
import org.audiveris.omr.ui.selection.MouseMovement;
import org.audiveris.omr.ui.selection.UserEvent;
import org.audiveris.omr.ui.util.Panel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.EnumSet;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Class {@code GlyphBoard} defines a UI board dedicated to the display of {@link Glyph}
 * information.
 *
 * @author Hervé Bitteur
 */
public class GlyphBoard
        extends EntityBoard<Glyph>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphBoard.class);

    /** Events this board is interested in.
     * TODO: not correctly used, need GroupEvent!!!!!!!!!!!!!!!!!!!!!! */
    private static final Class<?>[] eventClasses = new Class<?>[]{
        EntityListEvent.class, GroupEvent.class
    };

    //~ Instance fields ----------------------------------------------------------------------------
    /** The related glyph model. */
    protected final GlyphsController controller;

    /** Output : group info. */
    protected final JLabel groupField = new JLabel();

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Basic constructor, to set common characteristics.
     *
     * @param controller  the related glyphs controller, if any
     * @param useSpinners true for use of spinners
     * @param selected    true if board must be initially selected
     */
    public GlyphBoard (GlyphsController controller,
                       boolean useSpinners,
                       boolean selected)
    {
        super(Board.GLYPH, (EntityService<Glyph>) controller.getGlyphService(), selected);

        this.controller = controller;

        groupField.setHorizontalAlignment(SwingConstants.CENTER);
        groupField.setToolTipText("Assigned group(s)");

        defineLayout();
    }

    /**
     * A basic GlyphBoard, with just a glyph service
     *
     * @param glyphService the provided glyph service
     * @param selected     true if board must be initially selected
     */
    public GlyphBoard (EntityService<Glyph> glyphService,
                       boolean selected)
    {
        super(Board.GLYPH, glyphService, selected);

        this.controller = null;

        groupField.setHorizontalAlignment(SwingConstants.CENTER);
        groupField.setToolTipText("Assigned group(s)");

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // onEvent //
    //---------//
    /**
     * Call-back triggered when Glyph Selection has been modified
     *
     * @param event of current glyph or glyph set
     */
    @Override
    public void onEvent (UserEvent event)
    {
        logger.debug("GlyphBoard event:{}", event);

        try {
            // Ignore RELEASING
            if (event.movement == MouseMovement.RELEASING) {
                return;
            }

            super.onEvent(event); // count, vip, dump, id

            if (event instanceof EntityListEvent) {
                // Display additional entity parameters
                handleEvent((EntityListEvent<Glyph>) event);
            }
        } catch (Exception ex) {
            logger.warn(getClass().getName() + " onEvent error", ex);
        }
    }

    //    //--------------//
    //    // stateChanged //
    //    //--------------//
    //    /**
    //     * CallBack triggered by a change in one of the spinners.
    //     *
    //     * @param e the change event, this allows to retrieve the originating spinner
    //     */
    //    @Override
    //    public void stateChanged (ChangeEvent e)
    //    {
    //        JSpinner spinner = (JSpinner) e.getSource();
    //
    //        if (spinner == groupSpinner) {
    //            //            getSelectionService().publish(
    //            //                    new GroupEvent(this, SelectionHint.ENTITY_INIT, null, (Group) spinner.getValue()));
    //        } else {
    //            super.stateChanged(e);
    //        }
    //    }
    //
    //---------------//
    // getFormLayout //
    //---------------//
    @Override
    protected FormLayout getFormLayout ()
    {
        return Panel.makeFormLayout(6, 3);
    }

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout for common fields of all GlyphBoard classes
     */
    private void defineLayout ()
    {
        final CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        // Shape Icon (start, spans several rows) + layer + Deassign button

        ///builder.add(shapeIcon, cst.xywh(1, r, 1, 5));
        ///builder.add(groupSpinner, cst.xyw(5, r, 3));
        builder.add(groupField, cst.xyw(5, r, 3));

        r += 2; // --------------------------------
    }

    //-------------//
    // handleEvent //
    //-------------//
    /**
     * Interest in EntityList
     *
     * @param EntityListEvent
     */
    private void handleEvent (EntityListEvent<Glyph> listEvent)
    {
        final Glyph entity = listEvent.getEntity();

        if (entity != null) {
            // Group
            EnumSet<Group> groups = entity.getGroups();

            if (groups.isEmpty()) {
                groupField.setText("");
            } else {
                Group firstFroup = groups.iterator().next();
                groupField.setText(firstFroup.toString());
            }
        } else {
            // Group
            groupField.setText("");
        }
    }
}
