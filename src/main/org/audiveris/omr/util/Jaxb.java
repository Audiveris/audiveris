//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             J a x b                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
package org.audiveris.omr.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import static java.nio.file.StandardOpenOption.CREATE;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * Class {@code Jaxb} provides a collection of type adapters and facades for JAXB.
 *
 * @author Hervé Bitteur
 */
public abstract class Jaxb
{

    private static final Logger logger = LoggerFactory.getLogger(Jaxb.class);

    /** Not meant to be instantiated. */
    private Jaxb ()
    {
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal an object to a file, using provided JAXB context.
     *
     * @param object      instance to marshal
     * @param path        target file
     * @param jaxbContext proper context
     * @throws IOException        on IO error
     * @throws JAXBException      on JAXB error
     * @throws XMLStreamException on XML error
     */
    public static void marshal (Object object,
                                Path path,
                                JAXBContext jaxbContext)
            throws IOException,
                   JAXBException,
                   XMLStreamException
    {

        try (OutputStream os = Files.newOutputStream(path, CREATE);) {
            Marshaller m = jaxbContext.createMarshaller();
            XMLStreamWriter writer = new CustomXMLStreamWriter(
                    XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8"));
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            m.marshal(object, writer);
            os.flush();
        }
    }

    //---------//
    // marshal //
    //---------//
    /**
     * Marshal an object to a stream, using provided JAXB context.
     *
     * @param object      instance to marshal
     * @param os          output stream, not closed by this method
     * @param jaxbContext proper context
     * @throws JAXBException      on JAXB error
     * @throws XMLStreamException on XML error
     */
    public static void marshal (Object object,
                                OutputStream os,
                                JAXBContext jaxbContext)
            throws JAXBException,
                   XMLStreamException
    {
        Marshaller m = jaxbContext.createMarshaller();
        XMLStreamWriter writer = new CustomXMLStreamWriter(
                XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8"));
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(object, writer);
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal an object from a file, using provided JAXB context.
     *
     * @param path        input file
     * @param jaxbContext proper context
     * @return the unmarshalled object
     * @throws IOException   on IO error
     * @throws JAXBException on JAXB error
     */
    public static Object unmarshal (Path path,
                                    JAXBContext jaxbContext)
            throws IOException,
                   JAXBException
    {
        try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
            return unmarshal(is, jaxbContext);
        }
    }

    //-----------//
    // unmarshal //
    //-----------//
    /**
     * Unmarshal an object from a stream, using provided JAXB context.
     *
     * @param is          input stream, not closed by this method
     * @param jaxbContext proper context
     * @return the unmarshalled object
     * @throws IOException   on IO error
     * @throws JAXBException on JAXB error
     */
    public static Object unmarshal (InputStream is,
                                    JAXBContext jaxbContext)
            throws IOException,
                   JAXBException
    {
        return jaxbContext.createUnmarshaller().unmarshal(is);
    }

    //----------------------//
    // AtomicIntegerAdapter //
    //----------------------//
    /**
     * Adapter for AtomicInteger.
     */
    public static class AtomicIntegerAdapter
            extends XmlAdapter<Integer, AtomicInteger>
    {

        @Override
        public Integer marshal (AtomicInteger atomic)
                throws Exception
        {
            return atomic.get();
        }

        @Override
        public AtomicInteger unmarshal (Integer i)
                throws Exception
        {
            return new AtomicInteger(i);
        }
    }

    //------------------------//
    // BooleanPositiveAdapter //
    //------------------------//
    /**
     * Adapter for Boolean, by which only true value is marshalled into the output,
     * false value is not marshalled.
     */
    public static class BooleanPositiveAdapter
            extends XmlAdapter<String, Boolean>
    {

        private static final String TRUE = Boolean.toString(true);

        @Override
        public String marshal (Boolean b)
                throws Exception
        {
            if (b == null) {
                return null;
            }

            return b ? TRUE : null;
        }

        @Override
        public Boolean unmarshal (String s)
                throws Exception
        {
            if (s == null) {
                return false;
            }

            return Boolean.parseBoolean(s);
        }
    }

    //--------------//
    // CubicAdapter //
    //--------------//
    /**
     * Adapter for Cubic.
     */
    public static class CubicAdapter
            extends XmlAdapter<CubicAdapter.CubicFacade, CubicCurve2D>
    {

        @Override
        public CubicFacade marshal (CubicCurve2D curve)
                throws Exception
        {
            if (curve == null) {
                return null;
            }

            return new CubicFacade(curve);
        }

        @Override
        public CubicCurve2D unmarshal (CubicFacade facade)
                throws Exception
        {
            if (facade == null) {
                return null;
            }

            return facade.getCurve();
        }

        @XmlRootElement
        private static class CubicFacade
        {

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

            // Needed for JAXB
            CubicFacade ()
            {
            }

            CubicFacade (CubicCurve2D curve)
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

            public CubicCurve2D getCurve ()
            {
                return new CubicCurve2D.Double(x1, y1, ctrlx1, ctrly1, ctrlx2, ctrly2, x2, y2);
            }
        }
    }

    //------------------//
    // DimensionAdapter //
    //------------------//
    /**
     * Adapter for Dimension.
     */
    public static class DimensionAdapter
            extends XmlAdapter<DimensionAdapter.DimensionFacade, Dimension>
    {

        @Override
        public DimensionFacade marshal (Dimension dim)
                throws Exception
        {
            if (dim == null) {
                return null;
            }

            return new DimensionFacade(dim);
        }

        @Override
        public Dimension unmarshal (DimensionFacade facade)
                throws Exception
        {
            if (facade == null) {
                return null;
            }

            return facade.getDimension();
        }

        private static class DimensionFacade
        {

            @XmlAttribute(name = "w")
            public int width;

            @XmlAttribute(name = "h")
            public int height;

            /**
             * Needed for JAXB.
             */
            DimensionFacade ()
            {
            }

            /**
             * Creates a new DimensionFacade object.
             *
             * @param dimension the interfaced dimension
             */
            DimensionFacade (Dimension dimension)
            {
                width = dimension.width;
                height = dimension.height;
            }

            public Dimension getDimension ()
            {
                return new Dimension(width, height);
            }
        }
    }

    //----------------//
    // Double1Adapter //
    //----------------//
    /**
     * Adapter for Double, with maximum 1 decimal.
     */
    public static class Double1Adapter
            extends XmlAdapter<String, Double>
    {

        private static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);

        static {
            nf.setGroupingUsed(false);
            nf.setMaximumFractionDigits(1); // For a maximum of 1 decimal
        }

        @Override
        public String marshal (Double d)
                throws Exception
        {
            if (d == null) {
                return null;
            }

            return nf.format(d);
        }

        @Override
        public Double unmarshal (String s)
                throws Exception
        {
            if (s == null) {
                return null;
            }

            return Double.valueOf(s);
        }
    }

    //----------------//
    // Double3Adapter //
    //----------------//
    /**
     * Adapter for Double, with maximum 3 decimals.
     */
    public static class Double3Adapter
            extends XmlAdapter<String, Double>
    {

        private static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);

        static {
            nf.setGroupingUsed(false);
            nf.setMaximumFractionDigits(3); // For a maximum of 3 decimals
        }

        @Override
        public String marshal (Double d)
                throws Exception
        {
            if (d == null) {
                return null;
            }

            return nf.format(d);
        }

        @Override
        public Double unmarshal (String s)
                throws Exception
        {
            if (s == null) {
                return null;
            }

            return Double.valueOf(s);
        }
    }

    //----------------//
    // Double5Adapter //
    //----------------//
    /**
     * Adapter for Double, with maximum 5 decimals.
     */
    public static class Double5Adapter
            extends XmlAdapter<String, Double>
    {

        private static final NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);

        static {
            nf.setGroupingUsed(false);
            nf.setMaximumFractionDigits(5); // For a maximum of 5 decimals
        }

        @Override
        public String marshal (Double d)
                throws Exception
        {
            if (d == null) {
                return null;
            }

            return nf.format(d);
        }

        @Override
        public Double unmarshal (String s)
                throws Exception
        {
            if (s == null) {
                return null;
            }

            return Double.valueOf(s);
        }
    }

    //------------------------//
    // IntegerPositiveAdapter //
    //------------------------//
    /**
     * Only strictly positive value is marshalled into the output,
     * zero or negative value is not marshalled.
     */
    public static class IntegerPositiveAdapter
            extends XmlAdapter<String, Integer>
    {

        @Override
        public String marshal (Integer i)
                throws Exception
        {
            if (i == null) {
                return null;
            }

            return (i > 0) ? Integer.toString(i) : null;
        }

        @Override
        public Integer unmarshal (String s)
                throws Exception
        {
            if (s == null) {
                return 0;
            }

            return Integer.parseInt(s);
        }
    }

    //---------------//
    // Line2DAdapter //
    //---------------//
    /**
     * Adapter for Line2D.
     */
    public static class Line2DAdapter
            extends XmlAdapter<Line2DAdapter.Line2DFacade, Line2D>
    {

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

        @XmlRootElement
        private static class Line2DFacade
        {

            @XmlElement
            public Point2DFacade p1;

            @XmlElement
            public Point2DFacade p2;

            Line2DFacade ()
            {
            }

            Line2DFacade (Line2D line)
            {
                Objects.requireNonNull(line, "Cannot create Line2DFacade with a null line");
                p1 = new Point2DFacade(line.getP1());
                p2 = new Point2DFacade(line.getP2());
            }

            public Line2D getLine ()
            {
                return new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
            }

            @Override
            public String toString ()
            {
                final StringBuilder sb = new StringBuilder("Line2DF{");

                if (p1 != null) {
                    sb.append("p1:").append(p1);
                }

                if (p2 != null) {
                    sb.append(",p2:").append(p2);
                }

                sb.append('}');

                return sb.toString();
            }
        }
    }

    //---------------//
    // MarshalLogger //
    //---------------//
    /**
     * A logger specific for marshalling.
     */
    public static class MarshalLogger
            extends Marshaller.Listener
    {

        @Override
        public void afterMarshal (Object source)
        {
            logger.info("GL afterMarshal  {}", source);
        }

        @Override
        public void beforeMarshal (Object source)
        {
            logger.info("GL beforeMarshal {}", source);
        }
    }

    //-------------//
    // PathAdapter //
    //-------------//
    /**
     * Adapter for Path interface.
     */
    public static class PathAdapter
            extends XmlAdapter<String, Path>
    {

        @Override
        public String marshal (Path path)
                throws Exception
        {
            if (path == null) {
                return null;
            }

            return path.toString();
        }

        @Override
        public Path unmarshal (String str)
        {
            if (str == null) {
                return null;
            }

            return Paths.get(str);
        }
    }

    //----------------//
    // Point2DAdapter //
    //----------------//
    /**
     * Adapter for Point2D.
     */
    public static class Point2DAdapter
            extends XmlAdapter<Point2DFacade, Point2D>
    {

        @Override
        public Point2DFacade marshal (Point2D point)
                throws Exception
        {
            if (point == null) {
                return null;
            }

            return new Point2DFacade(point);
        }

        @Override
        public Point2D unmarshal (Point2DFacade facade)
                throws Exception
        {
            if (facade == null) {
                return null;
            }

            return facade.getPoint();
        }
    }

    //--------------//
    // PointAdapter //
    //--------------//
    /**
     * Adapter for Point.
     */
    public static class PointAdapter
            extends XmlAdapter<PointAdapter.PointFacade, Point>
    {

        @Override
        public PointFacade marshal (Point point)
                throws Exception
        {
            if (point == null) {
                return null;
            }

            return new PointFacade(point);
        }

        @Override
        public Point unmarshal (PointFacade facade)
                throws Exception
        {
            if (facade == null) {
                return null;
            }

            return facade.getPoint();
        }

        private static class PointFacade
        {

            @XmlAttribute
            public int x;

            @XmlAttribute
            public int y;

            // Needed for JAXB
            PointFacade ()
            {
            }

            PointFacade (Point point)
            {
                this.x = point.x;
                this.y = point.y;
            }

            public Point getPoint ()
            {
                return new Point(x, y);
            }

            @Override
            public String toString ()
            {
                final StringBuilder sb = new StringBuilder("PointF{");
                sb.append("x:").append(x);
                sb.append(",y:").append(y);
                sb.append('}');

                return sb.toString();
            }
        }
    }

    //--------------------//
    // Rectangle2DAdapter //
    //--------------------//
    /**
     * Adapter for Rectangle2D.
     */
    public static class Rectangle2DAdapter
            extends XmlAdapter<Rectangle2DAdapter.Rectangle2DFacade, Rectangle2D>
    {

        @Override
        public Rectangle2DFacade marshal (Rectangle2D rect)
                throws Exception
        {
            if (rect == null) {
                return null;
            }

            return new Rectangle2DFacade(rect);
        }

        @Override
        public Rectangle2D unmarshal (Rectangle2DFacade facade)
                throws Exception
        {
            if (facade == null) {
                return null;
            }

            return facade.getRectangle2D();
        }

        private static class Rectangle2DFacade
        {

            @XmlAttribute(name = "x")
            @XmlJavaTypeAdapter(type = double.class, value = Double3Adapter.class)
            public double x;

            @XmlAttribute(name = "y")
            @XmlJavaTypeAdapter(type = double.class, value = Double3Adapter.class)
            public double y;

            @XmlAttribute(name = "w")
            @XmlJavaTypeAdapter(type = double.class, value = Double3Adapter.class)
            public double width;

            @XmlAttribute(name = "h")
            @XmlJavaTypeAdapter(type = double.class, value = Double3Adapter.class)
            public double height;

            Rectangle2DFacade (Rectangle2D rect)
            {
                x = rect.getX();
                y = rect.getY();
                width = rect.getWidth();
                height = rect.getHeight();
            }

            private Rectangle2DFacade ()
            {
            }

            public Rectangle2D getRectangle2D ()
            {
                return new Rectangle2D.Double(x, y, width, height);
            }
        }
    }

    //------------------//
    // RectangleAdapter //
    //------------------//
    /**
     * Adapter for Rectangle.
     */
    public static class RectangleAdapter
            extends XmlAdapter<RectangleAdapter.RectangleFacade, Rectangle>
    {

        @Override
        public RectangleFacade marshal (Rectangle rect)
                throws Exception
        {
            if (rect == null) {
                return null;
            }

            return new RectangleFacade(rect);
        }

        @Override
        public Rectangle unmarshal (RectangleFacade facade)
                throws Exception
        {
            if (facade == null) {
                return null;
            }

            return facade.getRectangle();
        }

        private static class RectangleFacade
        {

            @XmlAttribute
            public int x;

            @XmlAttribute
            public int y;

            @XmlAttribute(name = "w")
            public int width;

            @XmlAttribute(name = "h")
            public int height;

            RectangleFacade ()
            {
            }

            RectangleFacade (Rectangle rect)
            {
                x = rect.x;
                y = rect.y;
                width = rect.width;
                height = rect.height;
            }

            public Rectangle getRectangle ()
            {
                return new Rectangle(x, y, width, height);
            }

            @Override
            public String toString ()
            {
                final StringBuilder sb = new StringBuilder("RectangleF{");
                sb.append("x:").append(x);
                sb.append(",y:").append(y);
                sb.append(",w:").append(width);
                sb.append(",h:").append(height);
                sb.append('}');

                return sb.toString();
            }
        }
    }

    //----------------------//
    // StringIntegerAdapter //
    //----------------------//
    /**
     * Adapter for Integer, which gives String value mandatory for @XmlID support.
     */
    public static class StringIntegerAdapter
            extends XmlAdapter<String, Integer>
    {

        @Override
        public String marshal (Integer i)
                throws Exception
        {
            if (i == null) {
                return null;
            }

            return Integer.toString(i);
        }

        @Override
        public Integer unmarshal (String s)
                throws Exception
        {
            if (s == null) {
                return null;
            }

            return Integer.decode(s);
        }
    }

    //-----------------//
    // UnmarshalLogger //
    //-----------------//
    /**
     * Specific logger for unmarshalling.
     */
    public static class UnmarshalLogger
            extends Unmarshaller.Listener
    {

        @Override
        public void afterUnmarshal (Object target,
                                    Object parent)
        {
            logger.info("GL afterUnmarshal parent:{} of {}", parent, target);
        }

        @Override
        public void beforeUnmarshal (Object target,
                                     Object parent)
        {
            logger.info("GL beforeUnmarshal parent:{} for target {}", parent, target.getClass());
        }
    }

    //---------------//
    // Point2DFacade //
    //---------------//
    /**
     * A facade to Point2D.
     */
    @XmlRootElement
    private static class Point2DFacade
    {

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double x;

        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double y;

        Point2DFacade ()
        {
        }

        Point2DFacade (Point2D point)
        {
            this.x = point.getX();
            this.y = point.getY();
        }

        public Point2D getPoint ()
        {
            return new Point2D.Double(x, y);
        }

        @Override
        public String toString ()
        {
            final StringBuilder sb = new StringBuilder("Point2DF{");
            sb.append("x:").append(x);
            sb.append(",y:").append(y);
            sb.append('}');

            return sb.toString();
        }
    }
}
