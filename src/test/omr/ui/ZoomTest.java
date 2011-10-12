//----------------------------------------------------------------------------//
//                                                                            //
//                              Z o o m T e s t                               //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Hervé Bitteur 2000-2011. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.ui;

import omr.ui.field.LDoubleField;
import omr.ui.view.*;

import omr.util.BaseTestCase;
import static junit.framework.Assert.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * DOCUMENT ME!
 *
 * @author TBD
 * @version TBD
  */
public class ZoomTest
    extends BaseTestCase
{
    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    public static void main (String... args)
    {
        new ZoomTest().play();
    }

    //------//
    // play //
    //------//
    public void play ()
    {
        final Zoom zoom = new Zoom();

        JFrame     frame = new JFrame(getClass().toString());
        Container  pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        final LDoubleField ldf = new LDoubleField("Ratio", "Current ratio");
        JPanel             panel = new JPanel();
        panel.add(ldf.getLabel(), BorderLayout.WEST);
        panel.add(ldf.getField(), BorderLayout.CENTER);
        pane.add(panel, BorderLayout.NORTH);

        ldf.getField()
           .addActionListener(
            new ActionListener() {
                    public void actionPerformed (ActionEvent e)
                    {
                        zoom.setRatio(ldf.getValue());
                    }
                });

        LogSlider slider = new LogSlider(2, 5, LogSlider.HORIZONTAL, -5, 5, 0);
        slider.setSnapToTicks(false);
        slider.setPreferredSize(new Dimension(800, 70));
        pane.add(slider, BorderLayout.SOUTH);

        zoom.addChangeListener(
            new ChangeListener() {
                    public void stateChanged (ChangeEvent e)
                    {
                        ldf.setValue(zoom.getRatio());
                    }
                });

        zoom.setSlider(slider);

        zoom.setRatio(1.5d);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocation(100, 100);
        frame.pack();
        frame.setVisible(true);
    }

    //----------//
    // testZoom //
    //----------//
    public void testZoom ()
    {
        play();
    }
}
