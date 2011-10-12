//----------------------------------------------------------------------------//
//                                                                            //
//                    T e s t C o m p o n e n t E v e n t                     //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

public class TestComponentEvent
        extends ComponentAdapter
        implements ChangeListener
{
    JFrame frame = new JFrame(getClass().getName().toString());
    JTabbedPane tabbedPane = new JTabbedPane();
    JButton button = new JButton(new ToggleAction());

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

    private void createTab (int index)
    {
        JPanel tab = new JPanel();
        tab.setName("Tab#" + index);
        tab.addComponentListener(this);
        tab.add(new JLabel("Texte de " + tab.getName()));
        tabbedPane.addTab(tab.getName(), tab);
    }

    class ToggleAction
            extends AbstractAction
    {
        public void actionPerformed(ActionEvent e) {
            tabbedPane.setVisible(!tabbedPane.isVisible());
        }
    }

    public void componentShown(ComponentEvent e) {
        System.out.println("Shown" + " " + e.getComponent().getName());
        if (e.getComponent() == tabbedPane) {
            System.out.println("Forcing selection to tab " + tabbedPane.getSelectedIndex());
            tabbedPane.setSelectedIndex(tabbedPane.getSelectedIndex());
        }
    }

    public void componentHidden(ComponentEvent e) {
        System.out.println("Hidden" + " " + e.getComponent().getName());
    }

    public void stateChanged(ChangeEvent e) {
        // This is for tabbed pane
        System.out.println("Changed " + e.getSource().getClass().getName()
        + " to " + tabbedPane.getTitleAt(tabbedPane.getSelectedIndex()));
    }
}
