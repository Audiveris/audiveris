//
package omr.math;

import Jama.*;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import static java.lang.Math.*;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * DOCUMENT ME!
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class ShowEllipse
    extends javax.swing.JFrame
{
    //~ Instance fields --------------------------------------------------------

    // Ellipse
    private final double         a = 200;
    private final double         b = 200;
    private Ellipse              ellipse;

    // Translation
    private final double         dx = 300;
    private final double         dy = 200;
    private final Matrix         translation = new Matrix(
        new double[][] {
            { 1, 0, dx },
            { 0, 1, dy },
            { 0, 0, 1 }
        });

    // Rotation
    private final double         theta =  0.3;
    private final Matrix         rotation = new Matrix(
        new double[][] {
            { cos(theta), -sin(theta), 0 },
            { sin(theta), cos(theta), 0 },
            { 0, 0, 1 }
        });

    // Points
    private List<Point2D.Double> points = new ArrayList<Point2D.Double>();

    // Widgets
    private JLabel coeffs;
    private JLabel dist;
    private JLabel minor;
    private JPanel axes;
    private JPanel panel;

    //~ Constructors -----------------------------------------------------------

    /**
     * Creates a new ShowEllipse object.
     */
    public ShowEllipse ()
    {
        initComponents();

        //        Ellipse.print(translation, "Translation matrix");
        //        Ellipse.print(rotation, "Rotation matrix");
        populatePoints();

        // Copy to separate arrays
        double[] x = new double[points.size()];
        double[] y = new double[points.size()];

        for (int i = 0; i < points.size(); i++) {
            Point2D.Double point = points.get(i);
            x[i] = point.x;
            y[i] = point.y;
        }

        ellipse = new Ellipse(x, y);
        ///ellipse = new Circle(x, y);
        coeffs.setText(stringOf(ellipse.getCoefficients()));

        dist.setText(
            String.format(
                "dist=%g  Center[x=%d,y=%d]  ang=%g  major=%g  minor=%g",
                ellipse.getDistance(),
                (int) rint(ellipse.getCenter().x),
                (int) rint(ellipse.getCenter().y),
                toDegrees(ellipse.getAngle()),
                ellipse.getMajor(),
                ellipse.getMinor()));
    }

    //~ Methods ----------------------------------------------------------------

    //------//
    // main //
    //------//
    public static void main (String[] args)
    {
        EventQueue.invokeLater(
            new Runnable() {
                    public void run ()
                    {
                        new ShowEllipse().setVisible(true);
                    }
                });
    }

    //----------------//
    // initComponents //
    //----------------//
    private void initComponents ()
    {
        panel = new MyPanel();
        axes = new JPanel();
        coeffs = new JLabel();
        dist = new JLabel();
        minor = new JLabel();

        setTitle("ShowEllipse");
        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        BorderLayout layout = new BorderLayout();
        Container    pane = getContentPane();
        pane.setLayout(layout);
        pane.add(axes, BorderLayout.NORTH);
        pane.add(panel, BorderLayout.CENTER);
        pane.add(dist, BorderLayout.SOUTH);

        axes.setLayout(new BorderLayout());
        axes.add(coeffs, BorderLayout.WEST);
        axes.add(minor, BorderLayout.EAST);

        pack();
        setBounds(100, 100, 600, 500);
    }

    //----------------//
    // populatePoints //
    //----------------//
    private void populatePoints ()
    {
        double dt = (1 * (Math.PI * 2)) / (a + b);
        Matrix el = new Matrix(3, 1);
        el.set(2, 0, 1);

        for (double t = 0; t < (2 * Math.PI); t += dt) {
            el.set(0, 0, a * Math.cos(t));
            el.set(1, 0, b * Math.sin(t));

            Matrix R = translation.times(rotation)
                                  .times(el);
            points.add(new Point2D.Double(R.get(0, 0), R.get(1, 0)));
        }
    }

    //----------//
    // stringOf //
    //----------//
    private String stringOf (double[] k)
    {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            sb.append(String.format("%15g", k[i]));
        }

        return sb.toString();
    }

    //~ Inner Classes ----------------------------------------------------------

    //---------//
    // MyPanel //
    //---------//
    class MyPanel
        extends JPanel
    {
        @Override
        protected void paintComponent (Graphics graphics)
        {
            Graphics2D g = (Graphics2D) graphics;
            super.paintComponent(g);

            int[] x = new int[points.size()];
            int[] y = new int[points.size()];

            for (int i = 0; i < points.size(); i++) {
                Point2D.Double point = points.get(i);
                x[i] = (int) Math.rint(point.x);
                y[i] = (int) Math.rint(point.y);
            }

            g.drawPolyline(x, y, points.size());
        }
    }
}
