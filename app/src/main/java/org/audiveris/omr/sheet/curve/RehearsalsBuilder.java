//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                R e h e a r s a l s B u i l d e r                               //
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
package org.audiveris.omr.sheet.curve;

import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.glyph.dynamic.StraightFilament;
import org.audiveris.omr.image.ImageUtil;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.sheet.Picture;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.rhythm.Measure;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.Inters;
import org.audiveris.omr.sig.inter.RehearsalInter;
import org.audiveris.omr.sig.inter.SegmentInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.Containment;
import org.audiveris.omr.text.BlockScanner;
import org.audiveris.omr.text.TextBuilder;
import org.audiveris.omr.text.TextLine;
import org.audiveris.omr.text.TextWord;
import org.audiveris.omr.util.Dumping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ij.process.ByteProcessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Class <code>RehearsalsBuilder</code> retrieves the rehearsals enclosures (+ text) out of the
 * segments founds in sheet skeleton.
 *
 * @author Hervé Bitteur
 */
public class RehearsalsBuilder
        extends LeggedIntersBuilder
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(RehearsalsBuilder.class);

    //~ Instance fields ----------------------------------------------------------------------------

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** The sheet collection of segments to pick from. */
    private List<SegmentInter> segments;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new RehearsalsBuilder object.
     *
     * @param curves curves environment
     */
    public RehearsalsBuilder (Curves curves)
    {
        super(curves);

        params = new Parameters(sheet.getScale());
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------------//
    // buildRehearsals //
    //-----------------//
    /**
     * Retrieve the rehearsals among the sheet segment curves.
     */
    public void buildRehearsals ()
    {
        segments = new ArrayList<>(curves.getSegments()); // We use a copy
        Collections.sort(segments, Inters.byOrdinate);

        segments.forEach(segment -> processSegment(segment));
    }

    //-------------//
    // createInter //
    //-------------//
    /**
     * Try to create a RehearsalInter from the provided horizontal segments.
     *
     * @param top    the top horizontal segment
     * @param bottom the bottom horizontal segment
     * @param staff  the staff below
     * @return the created RehearsalInter or null
     */
    private RehearsalInter createInter (SegmentInter top,
                                        SegmentInter bottom,
                                        Staff staff)
    {
        final SystemInfo system = staff.getSystem();

        // Look for the left leg
        final SegmentInfo topSeg = top.getInfo();
        final Point leftPt = topSeg.getEnd(true);
        final int leftMaxY = bottom.getInfo().getEnd(true).y;
        final StraightFilament left = lookupLeg(topSeg, leftPt, leftMaxY, system);
        if (left == null) {
            logger.debug("No rehearsal left leg below {}", topSeg);
            return null;
        }

        // Look for the right leg
        final Point rightPt = topSeg.getEnd(false);
        final int rightMaxY = bottom.getInfo().getEnd(false).y;
        final StraightFilament right = lookupLeg(topSeg, rightPt, rightMaxY, system);
        if (right == null) {
            logger.debug("No rehearsal right leg below {}", topSeg);
            return null;
        }

        // Define the enclosure rectangle
        final Rectangle topRect = top.getBounds();
        final Rectangle bottomRect = bottom.getBounds();
        final Rectangle leftRect = left.getBounds();
        final Rectangle rightRect = right.getBounds();
        final List<Rectangle> allRects = Arrays.asList(topRect, bottomRect, leftRect, rightRect);
        allRects.forEach(r -> r.grow(1, 1)); // To cope with rounding errors
        final Rectangle enclosure = new Rectangle(
                leftRect.x + leftRect.width / 2,
                topRect.y + topRect.height / 2,
                rightRect.x + rightRect.width / 2 - leftRect.x - leftRect.width / 2,
                bottomRect.y + bottomRect.height / 2 - topRect.y - topRect.height / 2);
        logger.debug("enclosure: {}", enclosure);

        // Define the rehearsal scene, focused on the area inside the enclosure
        final Rectangle scene = new Rectangle(topRect);
        allRects.forEach(r -> scene.add(r));
        logger.debug("scene: {}", scene);

        final ByteProcessor src = sheet.getPicture().getSource(Picture.SourceKey.BINARY);
        src.setRoi(scene);
        ByteProcessor buffer = (ByteProcessor) src.crop();
        final BufferedImage imgBuffer = buffer.getBufferedImage();

        // Erase the foreground pixels that compose the enclosure rectangle
        final Graphics2D g = imgBuffer.createGraphics();
        g.setColor(Color.WHITE);
        g.translate(-scene.x, -scene.y);
        allRects.forEach(r -> g.fill(r));
        if (logger.isDebugEnabled()) {
            ImageUtil.saveOnDisk(imgBuffer, sheet.getId(), "rehearsal-" + topRect.y);
        }

        // Look for contained text
        buffer = new ByteProcessor(imgBuffer);
        final List<TextLine> relativeLines = new BlockScanner(sheet).scanBuffer(
                buffer,
                sheet.getStub().getOcrLanguages(),
                topRect.y); // Not relevant
        relativeLines.forEach(rl -> logger.debug("relativeLine: {}", rl));

        // Convert to absolute lines (and the underlying word glyphs)
        final TextBuilder textBuilder = new TextBuilder(system, Shape.TEXT);
        final List<TextLine> lines = textBuilder.processBuffer(
                buffer,
                relativeLines,
                scene.getLocation());
        if (lines.isEmpty()) {
            logger.info("No text found in rehearsal enclosure {}", enclosure);
            return null;
        }
        if (lines.size() > 1) {
            logger.info("Several text lines found in rehearsal enclosure {}", enclosure);
            return null;
        }

        final TextLine line = lines.get(0);

        // Adjust sizes
        line.getWords().forEach(w -> w.adjustFont());

        // Allocate the rehearsal sentence
        final RehearsalInter rehearsal = new RehearsalInter(line.getConfidence(), enclosure);
        rehearsal.setStaff(staff);

        final SIGraph sig = system.getSig();
        sig.addVertex(rehearsal);

        // Retrieve and link the member words
        for (TextWord textWord : line.getWords()) {
            final WordInter word = new WordInter(textWord);
            word.setStaff(staff);
            sig.addVertex(word);
            sig.addEdge(rehearsal, word, new Containment());
        }

        rehearsal.setEnclosure(enclosure); // Just to stick to the initial rectangle

        logger.info("{}", rehearsal);
        return rehearsal;
    }

    //----------------//
    // processSegment //
    //----------------//
    /**
     * Check the horizontal segment for being part of a rehearsal.
     * <p>
     * We first look for horizontal compatible segments, one above the other
     * We then look for left and right legs.
     * <p>
     * Then grab the embedded text using local OCR processing there.
     *
     * @param top the horizontal segment candidate for a enclosure top
     */
    private void processSegment (SegmentInter top)
    {
        final SegmentInfo topSeg = top.getInfo();

        // Maximum segment length
        if (topSeg.getLength() > params.maxLength) {
            return;
        }

        final Point topLeftEnd = topSeg.getEnd(true);
        final Point topRightEnd = topSeg.getEnd(false);

        // Slope
        final Line2D line = new Line2D.Double(topLeftEnd, topRightEnd);
        final double slope = Math.abs(LineUtil.getSlope(line) - sheet.getSkew().getSlope());

        if (slope > params.maxSlope) {
            return;
        }

        // The relevant system is located just below the segments couple
        final List<SystemInfo> systems = sheet.getSystemManager().getSystemsOf(topLeftEnd, null);
        systems.retainAll(sheet.getSystemManager().getSystemsOf(topRightEnd, null));
        Collections.sort(systems, SystemInfo.byId);

        if (systems.isEmpty()) {
            return;
        }

        final SystemInfo system = systems.get(systems.size() - 1);

        // Located above the first staff
        final Staff staff = system.getFirstStaff();
        final int staffY = staff.getFirstLine().yAt(topLeftEnd.x);

        if (topLeftEnd.y > staffY) {
            return;
        }

        // Look for a partnering segment below
        final int index = segments.indexOf(top);
        for (int j = index + 1; j < segments.size(); j++) {
            final SegmentInter bottom = segments.get(j);
            final SegmentInfo bottomSeg = bottom.getInfo();
            final Point bottomLeftEnd = bottomSeg.getEnd(true);
            final Point bottomRightEnd = bottomSeg.getEnd(false);

            if (bottomLeftEnd.y > staffY) {
                break; // Since list is sorted on system id (more or less equivalent to ordinate)
            }

            // Check abscissa & length compatibility
            if (Math.abs(bottomLeftEnd.x - topLeftEnd.x) <= params.maxShift //
                    && (Math.abs(bottomRightEnd.x - topRightEnd.x) <= params.maxShift)) {
                logger.debug("Rehearsal candidate {} {}", topSeg, bottomSeg);
                final RehearsalInter rehearsal = createInter(top, bottom, staff);
                if (rehearsal != null) {
                    final Measure measure = staff.getPart().getMeasureAt(rehearsal.getCenter());
                    if (measure != null) {
                        measure.addInter(rehearsal);
                    } else {
                        logger.info("No containing measure for {}", rehearsal);
                    }

                    break;
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Scale.Fraction maxLength = new Scale.Fraction( //
                20,
                "Maximum length for enclosure horizontal segments");

        private final Scale.Fraction maxShift = new Scale.Fraction( //
                0.25,
                "Maximum horizontal shift between top and bottom segments");
    }

    //------------//
    // Parameters //
    //------------//
    /**
     * All pre-scaled constants.
     */
    private static class Parameters
            extends LeggedIntersBuilder.Parameters
    {
        final int maxLength;

        final int maxShift;

        Parameters (Scale scale)
        {
            super(scale);

            maxLength = scale.toPixels(constants.maxLength);
            maxShift = scale.toPixels(constants.maxShift);

            if (logger.isDebugEnabled()) {
                new Dumping().dump(this);
            }
        }
    }
}
