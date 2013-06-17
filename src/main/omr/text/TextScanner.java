//----------------------------------------------------------------------------//
//                                                                            //
//                           T e x t S c a n n e r                            //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright © Hervé Bitteur and others 2000-2013. All rights reserved.      //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.text;

import omr.constant.ConstantSet;

import omr.glyph.Glyphs;
import omr.glyph.facets.Glyph;

import omr.grid.LineInfo;
import omr.grid.StaffInfo;

import omr.lag.Section;

import omr.math.GeoPath;
import omr.math.ReversePathIterator;

import omr.score.entity.Page;


import omr.sheet.Scale;
import omr.sheet.SystemInfo;

import omr.util.HorizontalSide;
import omr.util.LiveParam;
import omr.util.Navigable;
import omr.util.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class {@code TextScanner} retrieves the text lines by using OCR on
 * the whole system area, ignoring internal staves areas.
 *
 * @author Hervé Bitteur
 */
public class TextScanner
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = LoggerFactory.getLogger(TextScanner.class);

    //~ Instance fields --------------------------------------------------------
    //
    /** The dedicated system. */
    @Navigable(false)
    private final SystemInfo system;

    /** TextBuilder companion. */
    private final TextBuilder textBuilder;

    /** Scale-dependent parameters. */
    private final Parameters params;

    /** Glyphs involved. */
    private Collection<Glyph> allGlyphs;

    /** Sections involved. */
    private Collection<Section> allSections = new ArrayList<>();

    //~ Constructors -----------------------------------------------------------
    //
    //-------------//
    // TextScanner //
    //-------------//
    /**
     * Creates a new TextScanner object.
     *
     * @param system the dedicated system
     */
    public TextScanner (SystemInfo system)
    {
        this.system = system;

        textBuilder = system.getTextBuilder();
        params = new Parameters(system.getSheet().getScale());
    }

    //~ Methods ----------------------------------------------------------------
    //
    //------------//
    // scanSystem //
    //------------//
    /**
     * Look for text items by launching the OCR on system area.
     */
    public void scanSystem ()
    {
        final Page page = system.getSheet().getPage();
        final LiveParam<String> textParam = page.getTextParam();
        final String language = textParam.getTarget();
        logger.debug("scanSystem lan:{} on {}", language, system.idString());
        textParam.setActual(language);

        // Retrieve glyphs
        allGlyphs = retrieveRegionGlyphs();

        // Generate an image with these glyphs
        final Rectangle bounds = system.getBounds();
        final BufferedImage image = new BufferedImage(
                bounds.width,
                bounds.height,
                BufferedImage.TYPE_BYTE_GRAY);

        for (Glyph glyph : allGlyphs) {
            allSections.addAll(glyph.getMembers());
        }
        for (Section section : allSections) {
            section.fillImage(image, bounds);
        }

        // Perform OCR on image
        final List<TextLine> lines = TextBuilder.getOcr().recognize(
                image,
                bounds.getLocation(),
                language,
                OCR.LayoutMode.MULTI_BLOCK,
                system,
                "s" + system.getId());

        // Process results
        if (lines != null) {
            List<TextLine> newLines = textBuilder.recomposeLines(lines);

            textBuilder.mapGlyphs(newLines,
                    allSections,
                    language);
        } else {
            logger.info("{} No line", system.idString());
        }
    }

    //----------------------//
    // retrieveRegionGlyphs //
    //----------------------//
    /**
     * Among the system glyphs, retrieve the ones that should be
     * considered for potential text items.
     *
     * @return the collection of glyph candidates
     */
    private Collection<Glyph> retrieveRegionGlyphs ()
    {
        /** Map staff -> contour. */
        final Map<StaffInfo, GeoPath> pathMap = new HashMap<>();

        // Define system region with staves removed
        for (StaffInfo staff : system.getStaves()) {
            pathMap.put(staff, getStaffContour(staff));
        }

        // Safer
        system.removeInactiveGlyphs();

        // Discard glyphs that intersect a stave core area
        return Glyphs.lookupGlyphs(system.getGlyphs(),
                new Predicate<Glyph>()
        {
            @Override
            public boolean check (Glyph glyph)
            {
                // Reject manual non-text glyphs
                if (glyph.isManualShape() && !glyph.isText()) {
                    return false;
                }

                // Keep known text
                if (glyph.isText()) {
                    return true;
                }

                // Check position wrt closest staff
                StaffInfo staff = system.getStaffAt(glyph.getAreaCenter());
                GeoPath contour = pathMap.get(staff);
                if (contour != null) {
                    if (contour.intersects(glyph.getBounds())) {
                        return false;
                    }

                    // Also, to cope with edition of system boundaries,
                    // reject glyphs that belong to a structure intersecting
                    // a staff region (this former structure appeared as one
                    // glyph before being segmented along stems)
                    for (HorizontalSide side : HorizontalSide.values()) {
                        Glyph stem = glyph.getStem(side);
                        if (stem != null && contour.intersects(stem.getBounds())) {
                            return false;
                        }
                    }
                }

                // Discard too large glyphs
                Rectangle bounds = glyph.getBounds();

                if (bounds.width > params.maxGlyphWidth) {
                    return false;
                }

                if (bounds.height > params.maxGlyphHeight) {
                    return false;
                }

                // All tests are OK
                return true;
            }
        });
    }

    //----------------//
    // getSampledLine //
    //----------------//
    /**
     * Define an approximating path for a provided line, enlarged in
     * abscissa and shifted in ordinate
     *
     * @param line   the line to approximate
     * @param yShift the shift in abscissa
     * @return the path to use
     */
    private GeoPath getSampledLine (LineInfo line,
                                    double yShift)
    {
        double left = line.getEndPoint(HorizontalSide.LEFT).getX() - params.marginLeft;
        double right = line.getEndPoint(HorizontalSide.RIGHT).getX() + params.marginRight;
        double width = right - left;
        final int sampleCount = (int) Math.rint(width / params.samplingDx);
        final double dx = width / sampleCount;

        GeoPath path = new GeoPath();

        for (int i = 0; i <= sampleCount; i++) {
            int x = (int) Math.rint(left + i * dx);
            double y = line.yAt(x) + yShift;

            if (i == 0) {
                path.moveTo(x, y);
            } else {
                path.lineTo(x, y);
            }
        }

        return path;
    }

    //-----------------//
    // getStaffContour //
    //-----------------//
    /**
     * Define an approximated contour for a staff, slightly enlarged
     * in abscissa and ordinate.
     *
     * @param staff the staff to approximate
     * @return an augmented contour for the staff
     */
    private GeoPath getStaffContour (StaffInfo staff)
    {
        GeoPath topLimit = getSampledLine(staff.getFirstLine(),
                -params.marginAbove);
        GeoPath botLimit = getSampledLine(staff.getLastLine(),
                params.marginBelow);

        GeoPath contour = new GeoPath();
        contour.append(topLimit, false);
        contour.append(ReversePathIterator.getReversePathIterator(botLimit),
                true);
        contour.closePath();

        // For visual check
        ///staff.addAttachment("core", contour);
        ///logger.info("Staff#{} contour:{}", staff.getId(), contour.toString());

        return contour;
    }

    //~ Inner Classes ----------------------------------------------------------
    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Scale.Fraction samplingDx = new Scale.Fraction(
                4d,
                "Abscissa sampling for staff contour");

        Scale.Fraction staffMarginAbove = new Scale.Fraction(
                0.25,
                "Minimum distance above staff");

        Scale.Fraction staffMarginBelow = new Scale.Fraction(
                0.25,
                "Minimum distance below staff");

        Scale.Fraction staffMarginLeft = new Scale.Fraction(
                0.25,
                "Minimum distance left of staff");

        Scale.Fraction staffMarginRight = new Scale.Fraction(
                0.25,
                "Minimum distance right of staff");

        Scale.Fraction maxGlyphWidth = new Scale.Fraction(
                7,
                "Maximum glyph width");

        Scale.Fraction maxGlyphHeight = new Scale.Fraction(
                5,
                "Maximum glyph height");

    }

    //------------//
    // Parameters //
    //------------//
    private class Parameters
    {

        final int marginAbove;

        final int marginBelow;

        final int marginLeft;

        final int marginRight;

        final int maxGlyphWidth;

        final int maxGlyphHeight;

        final double samplingDx;

        public Parameters (Scale scale)
        {
            marginAbove = scale.toPixels(constants.staffMarginAbove);
            marginBelow = scale.toPixels(constants.staffMarginBelow);
            marginLeft = scale.toPixels(constants.staffMarginLeft);
            marginRight = scale.toPixels(constants.staffMarginRight);
            maxGlyphWidth = scale.toPixels(constants.maxGlyphWidth);
            maxGlyphHeight = scale.toPixels(constants.maxGlyphHeight);
            samplingDx = scale.toPixelsDouble(constants.samplingDx);
        }
    }
}
