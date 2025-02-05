//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             J a x b                                            //
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
import java.io.File;
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
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

/**
 * Class <code>Jaxb</code> provides methods to help [un]marshalling objects,
 * together with many JAXB adapters and type facades.
 * <ul>
 * <li>These adapters and facades are meant for predefined classes we cannot annotate.
 * <li>For our own OMR classes that we can annotate, facades are not needed and JAXB adapters
 * should preferably be located in the class itself.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public abstract class Jaxb
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Jaxb.class);

    //~ Constructors -------------------------------------------------------------------------------

    /** Not meant to be instantiated. */
    @SuppressWarnings("unused")
    private Jaxb ()
    {
    }

    //~ Static Methods -----------------------------------------------------------------------------

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
        throws JAXBException, XMLStreamException
    {
        Marshaller m = jaxbContext.createMarshaller();
        XMLStreamWriter writer = new CustomXMLStreamWriter(
                XMLOutputFactory.newInstance().createXMLStreamWriter(os, "UTF-8"));
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        m.marshal(object, writer);
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
        throws IOException, JAXBException, XMLStreamException
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
        throws IOException, JAXBException
    {
        return jaxbContext.createUnmarshaller().unmarshal(is);
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
        throws IOException, JAXBException
    {
        try (InputStream is = Files.newInputStream(path, StandardOpenOption.READ)) {
            return unmarshal(is, jaxbContext);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

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
            extends XmlAdapter<Boolean, Boolean>
    {
        @Override
        public Boolean marshal (Boolean b)
            throws Exception
        {
            if (b == null) {
                return null;
            }

            return b ? true : null;
        }

        @Override
        public Boolean unmarshal (Boolean s)
            throws Exception
        {
            if (s == null) {
                return false;
            }

            return s;
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

        /**
         * Class <code>CubicFacade</code> is a JAXB-compatible facade for predefined
         * {@link java.awt.geom.CubicCurve2D.Double} class.
         * <p>
         * All coordinates are coded with a maximum of 1 digit after the dot.
         *
         * @author Hervé Bitteur
         */
        @XmlRootElement(name = "cubic")
        private static class CubicFacade
        {
            /**
             * Abscissa of the start point of the cubic curve segment.
             */
            @XmlAttribute
            @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
            public double x1;

            /**
             * Ordinate of the start point of the cubic curve segment.
             */
            @XmlAttribute
            @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
            public double y1;

            /**
             * Abscissa of the first control point of the cubic curve segment.
             */
            @XmlAttribute
            @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
            public double ctrlx1;

            /**
             * Ordinate of the first control point of the cubic curve segment.
             */
            @XmlAttribute
            @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
            public double ctrly1;

            /**
             * Abscissa of the second control point of the cubic curve segment.
             */
            @XmlAttribute
            @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
            public double ctrlx2;

            /**
             * Ordinate of the second control point of the cubic curve segment.
             */
            @XmlAttribute
            @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
            public double ctrly2;

            /**
             * Abscissa of the end point of the cubic curve segment.
             */
            @XmlAttribute
            @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
            public double x2;

            /**
             * Ordinate of the end point of the cubic curve segment.
             */
            @XmlAttribute
            @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
            public double y2;

            /** No-argument constructor needed for JAXB. */
            @SuppressWarnings("unused")
            private CubicFacade ()
            {
            }

            /**
             * Creates an instance of <code>CubicFacade</code> from a {@link CubicCurve2D}
             * parameter.
             *
             * @param curve the CubicCurve2D instance to interface.
             */
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

        /**
         * Class <code>DimensionFacade</code> is a JAXB-compatible facade for predefined
         * {@link java.awt.geom.Dimension} class.
         */
        @XmlRootElement(name = "dimension")
        private static class DimensionFacade
        {
            /** The width dimension. */
            @XmlAttribute(name = "w")
            public int width;

            /** The height dimension. */
            @XmlAttribute(name = "h")
            public int height;

            /**
             * Needed for JAXB.
             */
            @SuppressWarnings("unused")
            private DimensionFacade ()
            {
            }

            /**
             * Creates a new <code>DimensionFacade</code> object.
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

        /**
         * Class <code>Line2DFacade</code> is a JAXB-compatible facade for predefined
         * {@link java.awt.geom.Line2D.Double} class.
         */
        @XmlRootElement(name = "line2d")
        private static class Line2DFacade
        {
            /**
             * Start point of line.
             */
            @XmlElement
            public Point2DFacade p1;

            /**
             * End point of line.
             */
            @XmlElement
            public Point2DFacade p2;

            /** No-argument constructor needed for JAXB. */
            @SuppressWarnings("unused")
            private Line2DFacade ()
            {
            }

            /**
             * Creates a <code>Line2DFacade</code> from a {@link Line2D} parameter.
             *
             * @param line the Line2D instance to interface
             */
            Line2DFacade (Line2D line)
            {
                Objects.requireNonNull(line, "Cannot create Line2DFacade with a null line");
                p1 = new Point2DFacade(line.getP1());
                p2 = new Point2DFacade(line.getP2());
            }

            /**
             * Report the interfaced Line2D object.
             *
             * @return the interfaced object
             */
            public Line2D getLine ()
            {
                return new Line2D.Double(p1.x, p1.y, p2.x, p2.y);
            }

            @Override
            public String toString ()
            {
                return new StringBuilder("Line2DF{").append("p1:").append(p1).append(",p2:")
                        .append(p2).append('}').toString();
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

    //-------------------------//
    // OmrSchemaOutputResolver //
    //-------------------------//
    /**
     * A SchemaOutputResolver meant for this OMR application.
     */
    public static class OmrSchemaOutputResolver
            extends SchemaOutputResolver
    {
        /** Output file name. */
        private final String outputFileName;

        public OmrSchemaOutputResolver (String outputFileName)
        {
            this.outputFileName = outputFileName;
        }

        @Override
        public Result createOutput (String namespaceURI,
                                    String suggestedFileName)
        {
            File file = new File(outputFileName);
            StreamResult result = new StreamResult(file);
            result.setSystemId(file.getAbsolutePath());

            return result;
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

    //---------------//
    // Point2DFacade //
    //---------------//
    /**
     * Class <code>Point2DFacade</code> is a JAXB-compatible facade for predefined
     * {@link java.awt.geom.Point2D.Double} class.
     * <p>
     * All coordinates are coded with a maximum of 1 digit after the dot.
     */
    @XmlRootElement(name = "point2d")
    private static class Point2DFacade
    {
        /**
         * Abscissa value.
         */
        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double x;

        /**
         * Ordinate value.
         */
        @XmlAttribute
        @XmlJavaTypeAdapter(type = double.class, value = Double1Adapter.class)
        public double y;

        /** No-argument constructor needed for JAXB. */
        @SuppressWarnings("unused")
        private Point2DFacade ()
        {
        }

        /**
         * Creates a new <code>Point2DFacade</code> object from a Point2D parameter.
         *
         * @param point the Point2D object to interface
         */
        public Point2DFacade (Point2D point)
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
            return new StringBuilder("Point2DF{").append("x:").append(x).append(",y:").append(y)
                    .append('}').toString();
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

        /**
         * Class <code>PointFacade</code> is a JAXB-compatible facade for predefined
         * {@link java.awt.Point} class.
         */
        @XmlRootElement(name = "point")
        private static class PointFacade
        {
            /**
             * Point abscissa.
             */
            @XmlAttribute
            public int x;

            /**
             * Point ordinate.
             */
            @XmlAttribute
            public int y;

            // Needed for JAXB
            @SuppressWarnings("unused")
            private PointFacade ()
            {
            }

            /**
             * Creates a <code>PointFacade</code> object from a Point parameter.
             *
             * @param point the Point to interface
             */
            PointFacade (Point point)
            {
                this.x = point.x;
                this.y = point.y;
            }

            /**
             * Report the interfaced Point.
             *
             * @return the Point value
             */
            public Point getPoint ()
            {
                return new Point(x, y);
            }

            @Override
            public String toString ()
            {
                return new StringBuilder("PointF{").append("x:").append(x).append(",y:").append(y)
                        .append('}').toString();
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

        /**
         * Class <code>Rectangle2DFacade</code> is a JAXB-compatible facade for predefined
         * {@link java.awt.geom.Rectangle2D} class.
         * <p>
         * All values are coded with a maximum of 3 digits after the dot.
         */
        @XmlRootElement(name = "rectangle2d")
        private static class Rectangle2DFacade
        {
            /**
             * Abscissa of upper-left corner of rectangle.
             */
            @XmlAttribute(name = "x")
            @XmlJavaTypeAdapter(type = double.class, value = Double3Adapter.class)
            public double x;

            /**
             * Ordinate of upper-left corner of rectangle.
             */
            @XmlAttribute(name = "y")
            @XmlJavaTypeAdapter(type = double.class, value = Double3Adapter.class)
            public double y;

            /**
             * Width of rectangle.
             */
            @XmlAttribute(name = "w")
            @XmlJavaTypeAdapter(type = double.class, value = Double3Adapter.class)
            public double width;

            /**
             * Height of rectangle.
             */
            @XmlAttribute(name = "h")
            @XmlJavaTypeAdapter(type = double.class, value = Double3Adapter.class)
            public double height;

            /**
             * No-argument constructor needed for JAXB.
             */
            @SuppressWarnings("unused")
            private Rectangle2DFacade ()
            {
            }

            /**
             * Creates a <code>Rectangle2DFacade</code> object from a Rectangle2D parameter.
             *
             * @param rect the provided Rectangle2D
             */
            Rectangle2DFacade (Rectangle2D rect)
            {
                x = rect.getX();
                y = rect.getY();
                width = rect.getWidth();
                height = rect.getHeight();
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

        /**
         * Class <code>RectangleFacade</code> is a JAXB-compatible facade for predefined
         * {@link java.awt.Rectangle} class.
         */
        @XmlRootElement(name = "rectangle")
        private static class RectangleFacade
        {
            /**
             * Abscissa of upper-left corner of rectangle.
             */
            @XmlAttribute
            public int x;

            /**
             * Ordinate of upper-left corner of rectangle.
             */
            @XmlAttribute
            public int y;

            /**
             * Width of rectangle.
             */
            @XmlAttribute(name = "w")
            public int width;

            /**
             * Height of rectangle.
             */
            @XmlAttribute(name = "h")
            public int height;

            /**
             * No-argument constructor needed for JAXB.
             */
            @SuppressWarnings("unused")
            private RectangleFacade ()
            {
            }

            /**
             * Creates a <code>RectangleFacade</code> object from a Rectangle parameter.
             *
             * @param rect the provided Rectangle
             */
            public RectangleFacade (Rectangle rect)
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
                return new StringBuilder("RectangleF{").append("x:").append(x).append(",y:")
                        .append(y).append(",w:").append(width).append(",h:").append(height)
                        .append('}').toString();
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
}
