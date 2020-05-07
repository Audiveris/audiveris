//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                       B r a c e I n t e r                                      //
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

import org.audiveris.omr.glyph.Glyph;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.PointUtil;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sig.ui.InterEditor;
import org.audiveris.omr.sig.ui.InterUIModel;
import org.audiveris.omr.ui.symbol.Alignment;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code BraceInter} represents a brace.
 *
 * @author Hervé Bitteur
 */
@XmlRootElement(name = "brace")
public class BraceInter
        extends AbstractInter
{

    private static final Logger logger = LoggerFactory.getLogger(BraceInter.class);

    /**
     * Creates a new BraceInter object.
     *
     * @param glyph underlying glyph
     * @param grade evaluation value
     */
    public BraceInter (Glyph glyph,
                       double grade)
    {
        super(glyph, null, Shape.BRACE, grade);
    }

    /**
     * Creates a new BraceInter object, meant for manual use.
     *
     * @param grade evaluation value
     */
    public BraceInter (double grade)
    {
        this(null, grade);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BraceInter ()
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
     * A manual addition implies to merge the embraced staves into a single part.
     */
    @Override
    public void added ()
    {
        super.added();

        if (isManual()) {
            // Check brace center is located below brace "staff" and above the next staff in system
            Part myPart = staff.getPart();
            List<Part> systemParts = myPart.getSystem().getParts();
            int myIndex = systemParts.indexOf(myPart);

            if (systemParts.size() > myIndex + 1) {
                Part partBelow = systemParts.get(myIndex + 1);
                myPart.mergeWithBelow(partBelow);
            } else {
                logger.warn("No part to merge below {}", myPart);
            }
        }
    }

    //----------//
    // contains //
    //----------//
    @Override
    public boolean contains (Point point)
    {
        getBounds();

        if (bounds != null) {
            return bounds.contains(point);
        }

        return false;
    }

    //------------//
    // deriveFrom //
    //------------//
    @Override
    public boolean deriveFrom (ShapeSymbol symbol,
                               Sheet sheet,
                               MusicFont font,
                               Point dropLocation,
                               Alignment alignment)
    {
        // Needed to get head bounds
        super.deriveFrom(symbol, sheet, font, dropLocation, alignment);

        // Make sure brace staff is the upper staff
        if (staff != null) {
            Rectangle box = getBounds();
            Point topRight = new Point(box.x + box.width, box.y);
            SystemInfo system = sheet.getSystemManager().getClosestSystem(topRight);
            staff = system.getClosestStaff(topRight);
        }

        return true;
    }

    //-----------//
    // getBounds //
    //-----------//
    @Override
    public Rectangle getBounds ()
    {
        if (bounds == null) {
            if (glyph != null) {
                // Extend brace glyph box to related part
                final SystemInfo system = sig.getSystem();
                final Rectangle box = glyph.getBounds();
                final int xRight = box.x + box.width;

                try {
                    final Staff staff1 = system.getClosestStaff(new Point(xRight, box.y));
                    final Staff staff2 = system.getClosestStaff(
                            new Point(xRight, (box.y + box.height) - 1));
                    final int y1 = staff1.getFirstLine().yAt(xRight);
                    final int y2 = staff2.getLastLine().yAt(xRight);
                    bounds = new Rectangle(box.x, y1, box.width, y2 - y1 + 1);
                } catch (Exception ex) {
                    logger.warn("Error in getBounds for {}", this, ex);
                }
            }
        }

        if (bounds != null) {
            return new Rectangle(bounds);
        }

        return null;
    }

    //-----------//
    // getEditor //
    //-----------//
    @Override
    public InterEditor getEditor ()
    {
        return new Editor(this);
    }

    //---------------//
    // getFirstStaff //
    //---------------//
    /**
     * Report the first staff embraced by this brace.
     *
     * @return first staff or null
     */
    public Staff getFirstStaff ()
    {
        if (glyph != null) {
            final SystemInfo system = sig.getSystem();
            final Rectangle box = glyph.getBounds();
            final int xRight = box.x + box.width;

            try {
                return system.getClosestStaff(new Point(xRight, box.y));
            } catch (Exception ex) {
                logger.warn("Error in getFirstStaff for {}", this, ex);
            }
        }

        return null;
    }

    //--------------//
    // getLastStaff //
    //--------------//
    /**
     * Report the last staff embraced by this brace.
     *
     * @return first staff or null
     */
    public Staff getLastStaff ()
    {
        if (glyph != null) {
            final SystemInfo system = sig.getSystem();
            final Rectangle box = glyph.getBounds();
            final int xRight = box.x + box.width;

            try {
                return system.getClosestStaff(new Point(xRight, (box.y + box.height) - 1));
            } catch (Exception ex) {
                logger.warn("Error in getLastStaff for {}", this, ex);
            }
        }

        return null;
    }

    //--------//
    // remove //
    //--------//
    /**
     * Separate the embraced staves into separate parts.
     *
     * @param extensive true for non-manual removals only
     * @see #added()
     */
    @Override
    public void remove (boolean extensive)
    {
        if (isManual()) {
            Part myPart = staff.getPart();

            if (myPart.getStaves().size() < 2) {
                logger.warn("Not enough staves in {} to split it", myPart);
            } else {
                myPart.splitBefore(myPart.getLastStaff());
            }
        }

        super.remove(extensive);
    }

    //--------//
    // Editor //
    //--------//
    /**
     * User editor for a Brace.
     * <p>
     * For a brace, there are 3 handles:
     * <ul>
     * <li>top handle, moving vertically
     * <li>middle handle, moving the whole item in any direction
     * <li>bottom handle, moving vertically
     * </ul>
     */
    private static class Editor
            extends InterEditor
    {

        private final Model originalModel;

        private final Model model;

        public Editor (BraceInter inter)
        {
            super(inter);

            final Rectangle b = inter.getBounds();
            originalModel = new Model(b.x + b.width / 2, b.y, b.x + b.width / 2, b.y + b.height);
            model = new Model(b.x + b.width / 2, b.y, b.x + b.width / 2, b.y + b.height);

            final Point2D middle = PointUtil.middle(model.p1, model.p2);

            // Move top, only vertically
            handles.add(new Handle(model.p1)
            {
                @Override
                public boolean move (Point vector)
                {
                    final int dy = vector.y;

                    if (dy == 0) {
                        return false;
                    }

                    PointUtil.add(model.p1, 0, dy); // Data & handle
                    PointUtil.add(middle, 0, dy / 2.0); // Handle

                    return true;
                }
            });

            // Global move
            handles.add(selectedHandle = new Handle(middle)
            {
                @Override
                public boolean move (Point vector)
                {
                    // Data (and shared handles)
                    for (Handle handle : handles) {
                        PointUtil.add(handle.getHandleCenter(), vector);
                    }

                    return true;
                }
            });

            // Bottom move, only vertically
            handles.add(new Handle(model.p2)
            {
                @Override
                public boolean move (Point vector)
                {
                    final int dy = vector.y;

                    if (dy == 0) {
                        return false;
                    }

                    PointUtil.add(model.p2, 0, dy); // Data & handle
                    PointUtil.add(middle, 0, dy / 2.0); // Handle

                    return true;
                }
            });
        }

        @Override
        protected void doit ()
        {
            Rectangle box = inter.getBounds();
            inter.setBounds(
                    new Rectangle((int) Math.rint(model.p1.getX() - box.width / 2.0),
                                  (int) Math.rint(model.p1.getY()),
                                  box.width,
                                  (int) Math.rint(model.p2.getY() - model.p1.getY())));
            super.doit();  // No more glyph
        }

        @Override
        public void undo ()
        {
            Rectangle box = inter.getBounds();
            inter.setBounds(
                    new Rectangle((int) Math.rint(originalModel.p1.getX() - box.width / 2.0),
                                  (int) Math.rint(originalModel.p1.getY()),
                                  box.width,
                                  (int) Math.rint(originalModel.p2.getY() - originalModel.p1.getY())));
            super.undo();
        }
    }

    //-------//
    // Model //
    //-------//
    public static class Model
            implements InterUIModel
    {

        // Upper middle point
        public final Point2D p1;

        // Lower middle point
        public final Point2D p2;

        public Model (double x1,
                      double y1,
                      double x2,
                      double y2)
        {
            p1 = new Point2D.Double(x1, y1);
            p2 = new Point2D.Double(x2, y2);
        }
//
//        public Model (Line2D line)
//        {
//            p1 = line.getP1();
//            p2 = line.getP2();
//        }
//

        @Override
        public void translate (double dx,
                               double dy)
        {
            PointUtil.add(p1, dx, dy);
            PointUtil.add(p2, dx, dy);
        }
    }
}
