//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      G l y p h B o a r d                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2023. All rights reserved.
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.GlyphGroup;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.EntityBoard;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.util.Panel;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.util.EnumSet;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Class <code>GlyphBoard</code> defines a UI board dedicated to the display of {@link Glyph}
 * information.
 *
 * @author Hervé Bitteur
 */
public class GlyphBoard
        extends EntityBoard<Glyph>
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(GlyphBoard.class);

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(GlyphBoard.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** The related glyph model. */
    protected final GlyphsController controller;

    /** Output : group info. */
    protected final JLabel groupField = new JLabel();

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
        groupField.setToolTipText(resources.getString("groupField.toolTipText"));

        defineLayout();
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Basic constructor, to set common characteristics.
     *
     * @param controller the related glyphs controller, if any
     * @param selected   true if board must be initially selected
     */
    public GlyphBoard (GlyphsController controller,
                       boolean selected)
    {
        super(Board.GLYPH, controller.getGlyphService(), selected);

        this.controller = controller;

        groupField.setHorizontalAlignment(SwingConstants.CENTER);
        groupField.setToolTipText(resources.getString("groupField.toolTipText"));

        defineLayout();
    }

    //~ Methods ------------------------------------------------------------------------------------

    //--------------//
    // defineLayout //
    //--------------//
    /**
     * Define the layout for common fields of all GlyphBoard classes.
     */
    private void defineLayout ()
    {
        final CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------

        builder.add(groupField, cst.xyw(5, r, 3));
    }

    //---------------//
    // getFormLayout //
    //---------------//
    @Override
    protected FormLayout getFormLayout ()
    {
        return Panel.makeFormLayout(3, 3);
    }

    //-----------------------//
    // handleEntityListEvent //
    //-----------------------//
    /**
     * Interest in EntityList for Group field
     *
     * @param listEvent EntityListEvent
     */
    @Override
    protected void handleEntityListEvent (EntityListEvent<Glyph> listEvent)
    {
        super.handleEntityListEvent(listEvent);

        final Glyph entity = listEvent.getEntity();

        if (entity != null) {
            // Group
            EnumSet<GlyphGroup> groups = entity.getGroups();

            if (groups.isEmpty()) {
                groupField.setText("");
            } else {
                GlyphGroup firstFroup = groups.iterator().next();
                groupField.setText(firstFroup.toString());
            }
        } else {
            // Group
            groupField.setText("");
        }
    }
}
