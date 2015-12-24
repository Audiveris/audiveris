//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             J a x b                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code Jaxb} provides a collection of type adapters and facades for JAXB.
 *
 * @author Hervé Bitteur
 */
public abstract class Jaxb
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Jaxb.class);

    /** Used for booleans. */
    public static final True TRUE = new True();

    //~ Inner Classes ------------------------------------------------------------------------------
    //----------------------//
    // AtomicIntegerAdapter //
    //----------------------//
    public static class AtomicIntegerAdapter
            extends XmlAdapter<Integer, AtomicInteger>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Integer marshal (AtomicInteger atomic)
                throws Exception
        {
            ///System.out.println("atomic: " + atomic + " value=" + atomic.get());
            return atomic.get();
        }

        @Override
        public AtomicInteger unmarshal (Integer i)
                throws Exception
        {
            ///System.out.println("i: " + i);
            return new AtomicInteger(i);
        }
    }

    //--------------//
    // CubicAdapter //
    //--------------//
    public static class CubicAdapter
            extends XmlAdapter<CubicFacade, CubicCurve2D>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public CubicFacade marshal (CubicCurve2D curve)
                throws Exception
        {
            return new CubicFacade(curve);
        }

        @Override
        public CubicCurve2D unmarshal (CubicFacade facade)
                throws Exception
        {
            return facade.getCurve();
        }
    }

    //-------------//
    // CubicFacade //
    //-------------//
    @XmlRootElement
    public static class CubicFacade
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double x1;

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double y1;

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double ctrlx1;

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double ctrly1;

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double ctrlx2;

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double ctrly2;

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double x2;

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double y2;

        //~ Constructors ---------------------------------------------------------------------------
        public CubicFacade ()
        {
        }

        public CubicFacade (CubicCurve2D curve)
        {
            this.x1 = curve.getX1();
            this.y1 = curve.getY1();
            this.ctrlx1 = curve.getCtrlX1();
            this.ctrly1 = curve.getCtrlY1();
            this.ctrlx2 = curve.getCtrlX2();
            this.ctrly2 = curve.getCtrlY2();
            this.x2 = curve.getX2();
            this.y2 = curve.getY2();
        }

        //~ Methods --------------------------------------------------------------------------------
        public CubicCurve2D getCurve ()
        {
            return new CubicCurve2D.Double(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
        }
    }

    //----------------//
    // Double1Adapter //
    //----------------//
    public static class Double1Adapter
            extends XmlAdapter<String, Double>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String marshal (Double d)
                throws Exception
        {
            if (d == null) {
                return null;
            }

            return String.format("%.1f", d);
        }

        @Override
        public Double unmarshal (String s)
                throws Exception
        {
            return Double.valueOf(s);
        }
    }

    //----------------//
    // Double3Adapter //
    //----------------//
    public static class Double3Adapter
            extends XmlAdapter<String, Double>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String marshal (Double d)
                throws Exception
        {
            if (d == null) {
                return null;
            }

            return String.format("%.3f", d);
        }

        @Override
        public Double unmarshal (String s)
                throws Exception
        {
            return Double.valueOf(s);
        }
    }

    //---------------//
    // Line2DAdapter //
    //---------------//
    public static class Line2DAdapter
            extends XmlAdapter<Line2DFacade, Line2D>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Line2DFacade marshal (Line2D line)
                throws Exception
        {
            if (line == null) {
                return null;
            }

            return new Line2DFacade(line);
        }

        @Override
        public Line2D unmarshal (Line2DFacade facade)
                throws Exception
        {
            if (facade == null) {
                return null;
            }

            return facade.getLine();
        }
    }

    //--------------//
    // Line2DFacade //
    //--------------//
    @XmlRootElement
    public static class Line2DFacade
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlElement
        public Point2DFacade p1;

        @XmlElement
        public Point2DFacade p2;

        //~ Constructors ---------------------------------------------------------------------------
        public Line2DFacade ()
        {
        }

        public Line2DFacade (Line2D line)
        {
            Objects.requireNonNull(line, "Cannot create Line2DFacade with a null line");
            p1 = new Point2DFacade(line.getP1());
            p2 = new Point2DFacade(line.getP2());
        }

        //~ Methods --------------------------------------------------------------------------------
        public Line2D getLine ()
        {
            return new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
        }
    }

    //---------------//
    // MarshalLogger //
    //---------------//
    public static class MarshalLogger
            extends Marshaller.Listener
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void afterMarshal (Object source)
        {
            logger.info("GL afterMarshal source: {}", source);
        }

        @Override
        public void beforeMarshal (Object source)
        {
            logger.info("GL beforeMarshal source: {}", source);
        }
    }

    //-------------//
    // PathAdapter //
    //-------------//
    public static class PathAdapter
            extends XmlAdapter<String, Path>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String marshal (Path path)
                throws Exception
        {
            return path.toString();
        }

        @Override
        public Path unmarshal (String str)
        {
            return Paths.get(str);
        }
    }

    //----------------//
    // Point2DAdapter //
    //----------------//
    public static class Point2DAdapter
            extends XmlAdapter<Point2DFacade, Point2D>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public Point2DFacade marshal (Point2D point)
                throws Exception
        {
            return new Point2DFacade(point);
        }

        @Override
        public Point2D unmarshal (Point2DFacade facade)
                throws Exception
        {
            return facade.getPoint();
        }
    }

    //---------------//
    // Point2DFacade //
    //---------------//
    @XmlRootElement
    public static class Point2DFacade
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double x;

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double y;

        //~ Constructors ---------------------------------------------------------------------------
        public Point2DFacade ()
        {
        }

        public Point2DFacade (Point2D point)
        {
            this.x = point.getX();
            this.y = point.getY();
        }

        //~ Methods --------------------------------------------------------------------------------
        public Point2D getPoint ()
        {
            return new Point2D.Double(x, y);
        }
    }

    //--------------//
    // PointAdapter //
    //--------------//
    public static class PointAdapter
            extends XmlAdapter<PointFacade, Point>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public PointFacade marshal (Point point)
                throws Exception
        {
            if (point == null) {
                logger.warn("Null point");

                return null;
            }

            return new PointFacade(point);
        }

        @Override
        public Point unmarshal (PointFacade facade)
                throws Exception
        {
            if (facade == null) {
                logger.warn("Null point facade");

                return null;
            }

            return facade.getPoint();
        }
    }

    //-------------//
    // PointFacade //
    //-------------//
    @XmlRootElement
    public static class PointFacade
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlAttribute
        public int x;

        @XmlAttribute
        public int y;

        //~ Constructors ---------------------------------------------------------------------------
        public PointFacade ()
        {
        }

        public PointFacade (Point point)
        {
            this.x = point.x;
            this.y = point.y;
        }

        //~ Methods --------------------------------------------------------------------------------
        public Point getPoint ()
        {
            return new Point(x, y);
        }
    }

    //------------------//
    // RectangleAdapter //
    //------------------//
    public static class RectangleAdapter
            extends XmlAdapter<RectangleFacade, Rectangle>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public RectangleFacade marshal (Rectangle rect)
                throws Exception
        {
            return new RectangleFacade(rect);
        }

        @Override
        public Rectangle unmarshal (RectangleFacade facade)
                throws Exception
        {
            return facade.getRectangle();
        }
    }

    //-----------------//
    // RectangleFacade //
    //-----------------//
    @XmlRootElement
    public static class RectangleFacade
    {
        //~ Instance fields ------------------------------------------------------------------------

        @XmlAttribute
        public int x;

        @XmlAttribute
        public int y;

        @XmlAttribute
        public int width;

        @XmlAttribute
        public int height;

        //~ Constructors ---------------------------------------------------------------------------
        public RectangleFacade ()
        {
        }

        public RectangleFacade (Rectangle rect)
        {
            x = rect.x;
            y = rect.y;
            width = rect.width;
            height = rect.height;
        }

        //~ Methods --------------------------------------------------------------------------------
        public Rectangle getRectangle ()
        {
            return new Rectangle(x, y, width, height);
        }
    }

    //----------------------//
    // StringIntegerAdapter //
    //----------------------//
    public static class StringIntegerAdapter
            extends XmlAdapter<String, Integer>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String marshal (Integer i)
                throws Exception
        {
            return Integer.toString(i);
        }

        @Override
        public Integer unmarshal (String s)
                throws Exception
        {
            return Integer.decode(s);
        }
    }

    //------//
    // True //
    //------//
    public static class True
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public String toString ()
        {
            return "TRUE";
        }
    }

    //-----------------//
    // UnmarshalLogger //
    //-----------------//
    public static class UnmarshalLogger
            extends Unmarshaller.Listener
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public void afterUnmarshal (Object target,
                                    Object parent)
        {
            logger.info("GL afterUnmarshal parent: {} for {}", parent, target);
        }

        @Override
        public void beforeUnmarshal (Object target,
                                     Object parent)
        {
            logger.info("GL beforeUnmarshal parent: {} for class {}", parent, target.getClass());
        }
    }
}
