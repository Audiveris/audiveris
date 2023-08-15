//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S c o p e d P a n e l                                     //
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
package org.audiveris.omr.score.ui;

import org.audiveris.omr.ui.util.Panel;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Class <code>ScopedPanel</code> is a panel corresponding to a given scope tab.
 *
 * @author Hervé Bitteur
 */
public class ScopedPanel
        extends Panel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    // JGoodies column specification:       SelBox     Item1       Item2            Box
    private static final String colSpec4 = "10dlu,1dlu,100dlu,1dlu,55dlu,1dlu,right:10dlu";

    //~ Instance fields ----------------------------------------------------------------------------

    /** Collection of individual data panes. */
    protected final List<XactDataPane> panes = new ArrayList<>();

    /**
     * Creates a new <code>ScopedPanel</code> object, using default colSpec4.
     *
     * @param name  panel name
     * @param panes contained data panes
     */
    public ScopedPanel (String name,
                        List<XactDataPane> panes)
    {
        this(name, panes, colSpec4, 3);
    }

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>ScopedPanel</code> object.
     *
     * @param name       panel name
     * @param panes      contained data panes
     * @param colSpec    specific column specification
     * @param titleWidth number of cells for title, either 1 (just Item1) or 3 (Item1,|,Item2)
     */
    public ScopedPanel (String name,
                        List<XactDataPane> panes,
                        String colSpec,
                        int titleWidth)
    {
        setName(name);

        for (XactDataPane pane : panes) {
            if (pane != null) {
                this.panes.add(pane);
            }
        }

        defineLayout(colSpec, titleWidth);

        for (XactDataPane pane : this.panes) {
            // Pane is pre-selected if model has specific data
            final boolean isSpecific = pane.model.isSpecific();
            pane.selBox.setSelected(isSpecific);
            // Fill pane data
            pane.actionPerformed(null);
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Define layout of the pane.
     *
     * @param colSpec    column specification offering either 3 or 4 logical fields
     * @param titleWidth number of cells for title
     */
    private void defineLayout (String colSpec,
                               int titleWidth)
    {
        // Compute the total number of logical rows
        int logicalRowCount = 0;

        for (XactDataPane pane : panes) {
            logicalRowCount += pane.getLogicalRowCount();
        }

        FormLayout layout = new FormLayout(colSpec, Panel.makeRows(logicalRowCount));
        PanelBuilder builder = new PanelBuilder(layout, this);
        CellConstraints cst = new CellConstraints();
        int r = 1;

        for (XactDataPane pane : panes) {
            r = pane.defineLayout(builder, cst, titleWidth, r);
        }
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
