//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       L e d g e r I n t e r                                    //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.sig.inter;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.run.Orientation;
import org.audiveris.omr.sheet.Versions;
import org.audiveris.omr.sig.GradeImpacts;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterEditor.Handle;
import org.audiveris.omr.sig.ui.InterUIModel;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.LedgerSymbol;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Version;

import java.awt.geom.Line2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;

import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code LedgerInter} represents a Ledger interpretation.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "ledger")
public class LedgerInter
        extends AbstractInter
{

    private static final Constants constants = new Constants();

    // Persistent data
    //----------------
    //
    /** Default thickness of a ledger. */
    public static final double DEFAULT_THICKNESS = constants.defaultThickness.getValue();

    /** Mean ledger thickness. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = double.class, value = Jaxb.Double1Adapter.class)
    protected double thickness;

    /** Median line. */
    @XmlElement
    @XmlJavaTypeAdapter(Jaxb.Line2DAdapter.class)
    protected Line2D median;

    // Transient data
    //---------------
    //
    /**
     * Index of virtual line relative to staff.
     * <p>
     * Above staff, if index is negative (-1, -2, etc)
     * <pre>
     * -2 -
     * -1 -
     * ---------------------------------
     * ---------------------------------
     * ---------------------------------
     * ---------------------------------
     * ---------------------------------
     * +1 -
     * +2 -
     * </pre>
     * Below staff, if index is positive (+1, +2, etc)
     */
    private Integer index;

    /**
     * Creates a new LedgerInter object.
     *
     * @param glyph   the underlying glyph
     * @param impacts the assignment details
     */
    public LedgerInter (Glyph glyph,
                        GradeImpacts impacts)
    {
        super(glyph, null, Shape.LEDGER, impacts);

        if (glyph != null) {
            thickness = glyph.getMeanThickness(Orientation.HORIZONTAL);
            median = glyph.getCenterLine();

            computeArea();
        }
    }

    /**
     * Creates a new LedgerInter object.
     *
     * @param glyph the underlying glyph
     * @param grade quality
     */
    public LedgerInter (Glyph glyph,
                        double grade)
    {
        super(glyph, null, Shape.LEDGER, grade);

        if (glyph != null) {
            thickness = glyph.getMeanThickness(Orientation.HORIZONTAL);
            median = glyph.getCenterLine();

            computeArea();
        }
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private LedgerInter ()
    {
        super(null, null, null, null);
    }

    //--------//
    // accept //
    //--------//
    @Override
    public void accept (InterVisitor visitor)
    {
        visitor.visit(this);
    }

    //-------//
    // added //
    //-------//
    /**
     * Since a ledger instance is held by its containing staff, make sure staff
     * ledgers collection is updated.
     *
     * @see #remove(boolean)
     */
    @Override
    public void added ()
    {
        super.added();

        if (staff != null) {
            staff.addLedger(this);
        }
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        getBounds();

        if ((bounds != null) && !bounds.contains(point)) {
            return false;
        }

        if ((glyph != null) && glyph.contains(point)) {
            return true;
        }

        if (area == null) {
            computeArea();
        }

        return area.contains(point);
    }

    //------------//
    // getDetails //
    //------------//
    @Override
    public String getDetails ()
    {
        StringBuilder sb = new StringBuilder(super.getDetails());

        if (index != null) {
            sb.append(" index:").append(index);
        }

        return sb.toString();
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //----------//
    // getIndex //
    //----------//
    /**
     * Report the ledger index with respect to staff.
     *
     * @return the index
     */
    public Integer getIndex ()
    {
        return index;
    }

    //----------//
    // setIndex //
    //----------//
    /**
     * Set the ledger index, with respect to staff.
     *
     * @param index the index to set
     */
    public void setIndex (int index)
    {
        this.index = index;
    }

    //-----------//
    // getMedian //
    //-----------//
    /**
     * Report item median line.
     *
     * @return copy of the median line
     */
    public Line2D getMedian ()
    {
        return new Line2D.Double(median.getX1(), median.getY1(), median.getX2(), median.getY2());
    }

    //--------------//
    // getThickness //
    //--------------//
    /**
     * Report mean ledger thickness
     *
     * @return ledger thickness
     */
    public double getThickness ()
    {
        return thickness;
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public void deriveFrom (ShapeSymbol symbol,
                            MusicFont font,
                            Point dropLocation,
                            Alignment alignment)
    {
        LedgerSymbol ledgerSymbol = (LedgerSymbol) symbol;
        Model model = ledgerSymbol.getModel(font, dropLocation, alignment);
        median = new Line2D.Double(model.p1, model.p2);
        thickness = DEFAULT_THICKNESS;
        computeArea();
    }

    //--------//
    // remove //
    //--------//
    /**
     * Since a ledger instance is held by its containing staff, make sure staff
     * ledgers collection is updated.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (staff != null) {
            staff.removeLedger(this);
        }

        super.remove(extensive);
    }

    //-----------//
    // setBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds != null) {
            return new Rectangle(bounds);
        }

        if (glyph != null) {
            return new Rectangle(bounds = glyph.getBounds());
        }

        if (area == null) {
            computeArea();
        }

        return new Rectangle(bounds = area.getBounds());
    }

    //-----------------//
    // upgradeOldStuff //
    //-----------------//
    @Override
    public boolean upgradeOldStuff (List<Version> upgrades)
    {
        boolean upgraded = false;

        if (upgrades.contains(Versions.INTER_GEOMETRY)) {
            if (glyph != null) {
                if (thickness == 0) {
                    thickness = glyph.getMeanThickness(Orientation.HORIZONTAL);
                    upgraded = true;
                }

                if (median == null) {
                    median = glyph.getCenterLine();
                    upgraded = true;
                }
            } else {
                if (thickness == 0) {
                    thickness = DEFAULT_THICKNESS;
                    upgraded = true;
                }

                if (median == null) {
                    getBounds();

                    if (bounds != null) {
                        median = new Line2D.Double(
                                bounds.x, bounds.y + bounds.height / 2.0,
                                bounds.x + bounds.width, bounds.y + bounds.height / 2.0);
                        upgraded = true;
                    }
                }
            }

            if (upgraded) {
                computeArea();
            }
        }

        return upgraded;
    }

    //-------------//
    // computeArea //
    //-------------//
    /**
     * Compute the ledger area.
     */
    protected final void computeArea ()
    {
        setArea(AreaUtil.horizontalParallelogram(median.getP1(), median.getP2(), thickness));

        // Define precise bounds based on this path
        bounds = getArea().getBounds();
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled for this object,
     * but before this object is set to the parent object.
     */
    @SuppressWarnings("unused")
    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        if (median != null) {
            computeArea();
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Double defaultThickness = new Constant.Double(
                "pixels",
                3.0,
                "Default ledger thickness");
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a ledger.
     * <p>
     * For a ledger, there are 3 handles:
     * <ul>
     * <li>Left handle, moving horizontally
     * <li>Middle handle, moving the whole ledger vertically
     * <li>Right handle, moving horizontally
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {

        private final Model originalModel;

        private final Model model;

        private final Point2D middle;

        public Editor (LedgerInter ledger)
        {
            super(ledger);

            originalModel = new Model(ledger.median);
            model = new Model(ledger.median);

            middle = PointUtil.middle(model.p1, model.p2);

            // Move left, only horizontally
            handles.add(new Handle(model.p1)
            {
                @Override
                public boolean applyMove (Point vector)
                {
                    final int dx = vector.x;

                    if (dx == 0) {
                        return false;
                    }

                    PointUtil.add(model.p1, dx, 0);
                    PointUtil.add(middle, dx / 2.0, 0);

                    return true;
                }
            });

            // Global move, only vertically
            handles.add(selectedHandle = new Handle(middle)
            {
                @Override
                public boolean applyMove (Point vector)
                {
                    final int dy = vector.y;

                    if (dy == 0) {
                        return false;
                    }

                    for (Handle handle : handles) {
                        PointUtil.add(handle.getHandleCenter(), 0, dy);
                    }

                    return true;
                }
            });

            // Move right, only horizontally
            handles.add(new Handle(model.p2)
            {
                @Override
                public boolean applyMove (Point vector)
                {
                    final int dx = vector.x;

                    if (dx == 0) {
                        return false;
                    }

                    PointUtil.add(middle, dx / 2.0, 0);
                    PointUtil.add(model.p2, dx, 0);

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            final LedgerInter ledger = (LedgerInter) inter;
            ledger.median.setLine(model.p1, model.p2);
            ledger.computeArea(); // Set bounds also

            super.doit(); // No more glyph
        }

        @Override
        public void undo ()
        {
            final LedgerInter ledger = (LedgerInter) inter;
            ledger.median.setLine(originalModel.p1, originalModel.p2);
            ledger.computeArea(); // Set bounds also

            super.undo();
        }
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements InterUIModel
    {

        // Left point of median line
        public final Point2D p1;

        // Right point of median line
        public final Point2D p2;

        public Model (double x1,
                      double y1,
                      double x2,
                      double y2)
        {
            p1 = new Point2D.Double(x1, y1);
            p2 = new Point2D.Double(x2, y2);
        }

        public Model (Line2D line)
        {
            p1 = line.getP1();
            p2 = line.getP2();
        }

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(p1, dx, dy);
            PointUtil.add(p2, dx, dy);
        }
    }
}
