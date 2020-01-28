//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S e c t i o n B o a r d                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.lag.ui;

import com.jgoodies.forms.layout.CellConstraints;

import org.audiveris.omr.lag.Lag;
import org.audiveris.omr.lag.Section;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.ui.Board;
import org.audiveris.omr.ui.EntityBoard;
import org.audiveris.omr.ui.field.LIntegerField;
import org.audiveris.omr.ui.selection.EntityListEvent;

import org.jdesktop.application.Application;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;

/**
 * Class {@code SectionBoard} defines a board dedicated to the display of {@link
 * Section} information, it can also be used as an input means by directly entering the
 * section id in the proper Id spinner.
 *
 * @author Hervé Bitteur
 */
public class SectionBoard
        extends EntityBoard<Section>
{

    private static final Logger logger = LoggerFactory.getLogger(SectionBoard.class);

    private static final ResourceMap resources = Application.getInstance().getContext()
            .getResourceMap(SectionBoard.class);

    /** Underlying lag */
    protected final Lag lag;

    /** Field for left abscissa. */
    private final LIntegerField x = new LIntegerField(
            false,
            resources.getString("x.text"),
            resources.getString("x.toolTipText"));

    /** Field for top ordinate. */
    private final LIntegerField y = new LIntegerField(
            false,
            resources.getString("y.text"),
            resources.getString("y.toolTipText"));

    /** Field for width. */
    private final LIntegerField width = new LIntegerField(
            false,
            resources.getString("width.text"),
            resources.getString("width.toolTipText"));

    /** Field for height. */
    private final LIntegerField height = new LIntegerField(
            false,
            resources.getString("height.text"),
            resources.getString("height.toolTipText"));

    /** Field for weight. */
    private final LIntegerField weight = new LIntegerField(
            false,
            resources.getString("weight.text"),
            resources.getString("weight.toolTipText"));

    /**
     * Create a Section Board
     *
     * @param lag      the related lag
     * @param selected true for pre-selected, false for collapsed
     */
    public SectionBoard (Lag lag,
                         boolean selected)
    {
        super(
                new Desc(
                        Board.SECTION.name + ((lag.getOrientation() == Orientation.VERTICAL)
                        ? " Vert" : " Hori"),
                        Board.SECTION.position + ((lag.getOrientation() == Orientation.VERTICAL)
                        ? 100 : 0)),
                lag.getEntityService(),
                selected);

        this.lag = lag;

        x.setEnabled(false);
        y.setEnabled(false);
        weight.setEnabled(false);
        width.setEnabled(false);
        height.setEnabled(false);

        defineLayout();
    }

    //-----------------------//
    // handleEntityListEvent //
    //-----------------------//
    /**
     * Interest in EntityListEvent for x, y, width, height fields.
     *
     * @param listEvent the list event
     */
    @Override
    protected void handleEntityListEvent (EntityListEvent<Section> listEvent)
    {
        super.handleEntityListEvent(listEvent);

        // Info on last section in list
        final Section section = listEvent.getEntity();

        // Update section fields in this board
        emptyFields(getBody());

        if (section != null) {
            // We have a valid section, let's display its fields
            Rectangle box = section.getBounds();
            x.setValue(box.x);
            y.setValue(box.y);
            width.setValue(box.width);
            height.setValue(box.height);
            weight.setValue(section.getWeight());
        }

        x.setEnabled(section != null);
        y.setEnabled(section != null);
        weight.setEnabled(section != null);
        width.setEnabled(section != null);
        height.setEnabled(section != null);
    }

    //--------------//
    // defineLayout //
    //--------------//
    private void defineLayout ()
    {
        CellConstraints cst = new CellConstraints();

        int r = 1; // --------------------------------
        builder.add(x.getLabel(), cst.xy(5, r));
        builder.add(x.getField(), cst.xy(7, r));

        builder.add(width.getLabel(), cst.xy(9, r));
        builder.add(width.getField(), cst.xy(11, r));

        r += 2; // --------------------------------
        builder.add(weight.getLabel(), cst.xy(1, r));
        builder.add(weight.getField(), cst.xy(3, r));

        builder.add(y.getLabel(), cst.xy(5, r));
        builder.add(y.getField(), cst.xy(7, r));

        builder.add(height.getLabel(), cst.xy(9, r));
        builder.add(height.getField(), cst.xy(11, r));
    }
}
