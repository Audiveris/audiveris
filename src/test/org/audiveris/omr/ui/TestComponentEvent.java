//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                              T e s t C o m p o n e n t E v e n t                               //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2017. All rights reserved.
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
package org.audiveris.omr.ui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * DOCUMENT ME!
 *
 * @author Hervé Bitteur
 */
public class TestComponentEvent
        extends ComponentAdapter
        implements ChangeListener
{
    //~ Instance fields ----------------------------------------------------------------------------

    JFrame frame = new JFrame(getClass().getName().toString());

    JTabbedPane tabbedPane = new JTabbedPane();

    JButton button = new JButton(new ToggleAction());

    //~ Methods ------------------------------------------------------------------------------------
    public void componentHidden (ComponentEvent e)
    {
        System.out.println("Hidden" + " " + e.getComponent().getName());
    }

    public void componentShown (ComponentEvent e)
    {
        System.out.println("Shown" + " " + e.getComponent().getName());

        if (e.getComponent() == tabbedPane) {
            System.out.println("Forcing selection to tab " + tabbedPane.getSelectedIndex());
            tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex());
        }
    }

    public static void main (String... args)
    {
        new TestComponentEvent().play();
    }

    public void play ()
    {
        frame.setName("LaFrame");
        frame.addComponentListener(this);

        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        button.setText("Toggle");
        button.setName("LeBouton");
        pane.add(button, BorderLayout.NORTH);

        tabbedPane.setName("LeTabbedPane");
        pane.add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.addComponentListener(this);
        tabbedPane.addChangeListener(this);

        for (int i = 0; i < 5; i++) {
            createTab(i);
        }

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setLocation(100, 100);
        frame.setSize(300, 120);
        frame.setVisible(true);

        System.out.println("**** Frame shown ****");
    }

    public void stateChanged (ChangeEvent e)
    {
        // This is for tabbed pane
        System.out.println(
                "Changed " + e.getSource().getClass().getName() + " to "
                + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
    }

    private void createTab (int index)
    {
        JPanel tab = new JPanel();
        tab.setName("Tab#" + index);
        tab.addComponentListener(this);
        tab.add(new JLabel("Texte de " + tab.getName()));
        tabbedPane.addTab(tab.getName(), tab);
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    class ToggleAction
            extends AbstractAction
    {
        //~ Methods --------------------------------------------------------------------------------

        public void actionPerformed (ActionEvent e)
        {
            tabbedPane.setVisible(!tabbedPane.isVisible());
        }
    }
}
