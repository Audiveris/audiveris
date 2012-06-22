/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package omr.ui;

import omr.ui.field.LDoubleField;
import omr.ui.view.LogSlider;
import omr.ui.view.Zoom;

import org.junit.Test;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseWheelEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Class {@code MouseWheelTest}
 *
 * @author Hervé Bitteur
 */
public class MouseWheelTest
    extends MouseAdapter
{
    //~ Static fields/initializers ---------------------------------------------

    private static final double base = 2;
    private static final double intervals = 5;
    private static final double factor = Math.pow(base, 1d / intervals);

    //~ Instance fields --------------------------------------------------------

    LogSlider  slider;
    final Zoom zoom = new Zoom();

    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    public static void main (String... args)
    {
        new MouseWheelTest().play();
    }

    //-----------------//
    // mouseWheelMoved //
    //-----------------//
    @Override
    public void mouseWheelMoved (MouseWheelEvent e)
    {
        /*
         * java.awt.event.MouseWheelEvent
         * [MOUSE_WHEEL,(628,24),absolute(0,0),button=0,modifiers=Ctrl,extModifiers=Ctrl,clickCount=0,scrollType=WHEEL_UNIT_SCROLL,scrollAmount=3,wheelRotation=-1]
         * on javax.swing.JPanel[,0,0,800x30,layout=java.awt.FlowLayout,alignmentX=0.0,alignmentY=0.0,border=,flags=9,maximumSize=,minimumSize=,preferredSize=]
         */

        //System.out.println("e: " + e);
        int scrollAmount = e.getScrollAmount();
        int     scrollType = e.getScrollType();
        int     unitsToScroll = e.getUnitsToScroll();
        int     wheelRotation = e.getWheelRotation();
        boolean ctrl = e.isControlDown();

        //        System.out.println(
        //            "CTRL:" + ctrl + " amount:" + scrollAmount + " type:" + scrollType +
        //            " units:" + unitsToScroll + " rotation:" + wheelRotation);
        if (ctrl) {
            double val = slider.getDoubleValue();

            System.out.println("val:" + val);

            if (wheelRotation > 0) {
                val /= factor;
            } else {
                val *= factor;
            }

            System.out.println("  val:" + val);

            slider.setDoubleValue(val);
        }
    }

    //------//
    // play //
    //------//
    @Test
    public void play ()
    {
        JFrame    frame = new JFrame(getClass().toString());
        Container pane = frame.getContentPane();
        pane.setLayout(new BorderLayout());

        final LDoubleField ldf = new LDoubleField("Ratio", "Current ratio");
        JPanel             panel = new JPanel();
        panel.add(ldf.getLabel(), BorderLayout.WEST);
        panel.add(ldf.getField(), BorderLayout.CENTER);
        pane.add(panel, BorderLayout.NORTH);

        ldf.getField()
           .addActionListener(
            new ActionListener() {
                    @Override
                    public void actionPerformed (ActionEvent e)
                    {
                        zoom.setRatio(ldf.getValue());
                    }
                });

        slider = new LogSlider(2, 5, LogSlider.HORIZONTAL, -5, 5, 0);
        slider.setSnapToTicks(false);
        slider.setPreferredSize(new Dimension(800, 70));
        pane.add(slider, BorderLayout.SOUTH);

        zoom.addChangeListener(
            new ChangeListener() {
                    @Override
                    public void stateChanged (ChangeEvent e)
                    {
                        ldf.setValue(zoom.getRatio());
                    }
                });

        zoom.setSlider(slider);

        zoom.setRatio(2d);

        // Register for wheel events
        panel.addMouseWheelListener(this);

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
