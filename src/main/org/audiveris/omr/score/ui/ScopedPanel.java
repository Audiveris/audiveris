//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S c o p e d P a n e l                                     //
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
package org.audiveris.omr.score.ui;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import org.audiveris.omr.ui.util.Panel;

import java.util.ArrayList;
import java.util.List;

/**
 * Class {@code ScopedPanel} is a panel corresponding to a given scope tab.
 *
 * @author Hervé Bitteur
 */
public final class ScopedPanel
        extends Panel
{

    /** Standard column spec for 4 fields. */
    private static final String colSpec4 = "12dlu,1dlu,100dlu,1dlu,35dlu,1dlu,right:12dlu";

    /** Collection of individual data panes. */
    private final List<XactDataPane> panes = new ArrayList<XactDataPane>();

    /**
     * Creates a new {@code ScopedPanel} object.
     *
     * @param name  panel name
     * @param panes contained data panes
     */
    public ScopedPanel (String name,
                        List<XactDataPane> panes)
    {
        setName(name);

        for (XactDataPane pane : panes) {
            if (pane != null) {
                this.panes.add(pane);
            }
        }

        defineLayout();

        for (XactDataPane pane : this.panes) {
            // Pane is pre-selected if model has specific data
            final boolean isSpecific = pane.model.isSpecific();
            pane.selBox.setSelected(isSpecific);
            // Fill pane data
            pane.actionPerformed(null);
        }
    }

    public void defineLayout ()
    {
        // Compute the total number of logical rows
        int logicalRowCount = 0;

        for (XactDataPane pane : panes) {
            logicalRowCount += pane.getLogicalRowCount();
        }

        FormLayout layout = new FormLayout(colSpec4, Panel.makeRows(logicalRowCount));
        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cst = new CellConstraints();
        int r = 1;

        for (XactDataPane pane : panes) {
            r = pane.defineLayout(builder, cst, r);
        }
    }

    public XactDataPane getPane (Class classe)
    {
        for (XactDataPane pane : panes) {
            if (classe.isAssignableFrom(pane.getClass())) {
                return pane;
            }
        }

        return null;
    }

    /**
     * Report the contained data panes.
     *
     * @return the sequence of data panes
     */
    public List<XactDataPane> getPanes ()
    {
        return panes;
    }
}
