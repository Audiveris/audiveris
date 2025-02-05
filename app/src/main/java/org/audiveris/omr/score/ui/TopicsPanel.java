//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      T o p i c s P a n e l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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

import org.jdesktop.application.ResourceMap;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.SwingConstants;

/**
 * Class <code>TopicsPanel</code> is a panel where the individual components (panes) are
 * gathered by topics.
 *
 * @author Hervé Bitteur
 */
public class TopicsPanel
        extends Panel
{
    //~ Static fields/initializers -----------------------------------------------------------------

    // JGoodies columns specification:
    private static final String colSpec =
            //Topic     SelBox     Item1       Item2            Box
            "10dlu,1dlu,10dlu,1dlu,100dlu,1dlu,55dlu,1dlu,right:10dlu";

    //~ Instance fields ----------------------------------------------------------------------------

    /** Collection of topics. */
    protected final List<XactTopic> topics = new ArrayList<>();

    /** For I18N. */
    protected final ResourceMap resources;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>TopicsPanel</code> object, using default colSpec.
     *
     * @param name      panel name
     * @param topics    contained data topics
     * @param resources UI resources
     */
    public TopicsPanel (String name,
                        List<XactTopic> topics,
                        ResourceMap resources)
    {
        this(name, topics, resources, colSpec, 3);
    }

    /**
     * Creates a new <code>TopicsPanel</code> object, with a provided columns specification.
     *
     * @param name       panel name
     * @param topics     contained data topics
     * @param resources  UI resources
     * @param colSpec    specific columns specification
     * @param titleWidth number of cells for title, either 1 (just Item1) or 3 (Ite
     */
    public TopicsPanel (String name,
                        List<XactTopic> topics,
                        ResourceMap resources,
                        String colSpec,
                        int titleWidth)
    {
        setName(name);
        this.resources = resources;

        for (XactTopic topic : topics) {
            if (topic != null) {
                this.topics.add(topic);
            }
        }

        defineLayout(colSpec, titleWidth);

        for (XactTopic topic : this.topics) {
            for (XactPane pane : topic) {
                // Pane is pre-selected if model has specific data
                final boolean isSpecific = pane.model.isSpecific();
                pane.selBox.setSelected(isSpecific);

                // Fill pane data
                pane.actionPerformed(null);
            }
        }
    }

    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Define layout of the panel.
     *
     * @param colSpec    column specification offering either 3 or 4 logical fields
     * @param titleWidth number of cells for title
     */
    private void defineLayout (String colSpec,
                               int titleWidth)
    {
        // Compute the total number of logical rows
        int logicalRowCount = 0;

        for (XactTopic topic : topics) {
            logicalRowCount++; // One row for the topic title

            for (XactPane pane : topic) {
                logicalRowCount += pane.getLogicalRowCount();
            }
        }

        FormLayout layout = new FormLayout(colSpec, Panel.makeRows(logicalRowCount));
        FormBuilder builder = FormBuilder.create().layout(layout).panel(this);
        int r = 1;

        for (XactTopic topic : topics) {
            // Topic title
            final JLabel title = new JLabel(textOf(topic.name));
            title.setHorizontalAlignment(SwingConstants.LEFT);
            builder.addRaw(title).xyw(1, r, 7);
            r += 2;

            for (XactPane pane : topic) {
                r = pane.defineLayout(builder, titleWidth, r);
            }
        }
    }

    /**
     * Report the contained data panes.
     *
     * @return the sequence of data panes
     */
    public List<XactPane> getPanes ()
    {
        final List<XactPane> panes = new ArrayList<>();

        for (XactTopic topic : topics) {
            panes.addAll(topic);
        }

        return panes;
    }

    private String textOf (String name)
    {
        // Priority is given to text in resources file if any
        final String desc = resources.getString(name + ".text");

        return (desc != null) ? desc : name;
    }
}
