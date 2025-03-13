//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    S h e e t P a i n t e r                                     //
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
package org.audiveris.omr.sheet.ui;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.glyph.Shape;
import org.audiveris.omr.math.AreaUtil;
import org.audiveris.omr.math.GeoUtil;
import org.audiveris.omr.score.LogicalPart;
import org.audiveris.omr.score.PartRef;
import org.audiveris.omr.score.Score;
import org.audiveris.omr.sheet.Part;
import org.audiveris.omr.sheet.Scale;
import org.audiveris.omr.sheet.Sheet;
import org.audiveris.omr.sheet.Staff;
import org.audiveris.omr.sheet.SystemInfo;
import org.audiveris.omr.sheet.curve.Curves;
import org.audiveris.omr.sheet.grid.LineInfo;
import org.audiveris.omr.sheet.rhythm.Voice;
import org.audiveris.omr.sheet.rhythm.Voices;
import org.audiveris.omr.sig.SIGraph;
import org.audiveris.omr.sig.inter.AbstractBeamInter;
import org.audiveris.omr.sig.inter.AbstractChordInter;
import org.audiveris.omr.sig.inter.AbstractFlagInter;
import org.audiveris.omr.sig.inter.AbstractInterVisitor;
import org.audiveris.omr.sig.inter.AbstractNumberInter;
import org.audiveris.omr.sig.inter.AbstractPitchedInter;
import org.audiveris.omr.sig.inter.AlterInter;
import org.audiveris.omr.sig.inter.ArpeggiatoInter;
import org.audiveris.omr.sig.inter.AugmentationDotInter;
import org.audiveris.omr.sig.inter.BarConnectorInter;
import org.audiveris.omr.sig.inter.BarlineInter;
import org.audiveris.omr.sig.inter.BeatUnitInter;
import org.audiveris.omr.sig.inter.BraceInter;
import org.audiveris.omr.sig.inter.BracketConnectorInter;
import org.audiveris.omr.sig.inter.BracketInter;
import org.audiveris.omr.sig.inter.ClefInter;
import org.audiveris.omr.sig.inter.EndingInter;
import org.audiveris.omr.sig.inter.GraceChordInter;
import org.audiveris.omr.sig.inter.HeadInter;
import org.audiveris.omr.sig.inter.Inter;
import org.audiveris.omr.sig.inter.KeyAlterInter;
import org.audiveris.omr.sig.inter.KeyInter;
import org.audiveris.omr.sig.inter.LedgerInter;
import org.audiveris.omr.sig.inter.LyricLineInter;
import org.audiveris.omr.sig.inter.MetronomeInter;
import org.audiveris.omr.sig.inter.MultipleRestInter;
import org.audiveris.omr.sig.inter.MusicWordInter;
import org.audiveris.omr.sig.inter.OctaveShiftInter;
import org.audiveris.omr.sig.inter.RestInter;
import org.audiveris.omr.sig.inter.SentenceInter;
import org.audiveris.omr.sig.inter.SlurInter;
import org.audiveris.omr.sig.inter.StaffBarlineInter;
import org.audiveris.omr.sig.inter.StemInter;
import org.audiveris.omr.sig.inter.TimeCustomInter;
import org.audiveris.omr.sig.inter.TimeWholeInter;
import org.audiveris.omr.sig.inter.VerticalSerifInter;
import org.audiveris.omr.sig.inter.WedgeInter;
import org.audiveris.omr.sig.inter.WordInter;
import org.audiveris.omr.sig.relation.AlterHeadRelation;
import org.audiveris.omr.sig.relation.AugmentationRelation;
import org.audiveris.omr.sig.relation.FlagStemRelation;
import org.audiveris.omr.sig.relation.Relation;
import org.audiveris.omr.text.FontInfo;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.ViewParameters;
import org.audiveris.omr.ui.symbol.Alignment;
import static org.audiveris.omr.ui.symbol.Alignment.AREA_CENTER;
import static org.audiveris.omr.ui.symbol.Alignment.BASELINE_LEFT;
import static org.audiveris.omr.ui.symbol.Alignment.BOTTOM_LEFT;
import static org.audiveris.omr.ui.symbol.Alignment.MIDDLE_LEFT;
import static org.audiveris.omr.ui.symbol.Alignment.MIDDLE_RIGHT;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_CENTER;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_LEFT;
import static org.audiveris.omr.ui.symbol.Alignment.TOP_RIGHT;
import org.audiveris.omr.ui.symbol.FontSymbol;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.MusicFont;
import org.audiveris.omr.ui.symbol.NumDenSymbol;
import org.audiveris.omr.ui.symbol.NumberSymbol;
import org.audiveris.omr.ui.symbol.OctaveShiftSymbol;
import org.audiveris.omr.ui.symbol.OmrFont;
import org.audiveris.omr.ui.symbol.ShapeSymbol;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.ui.symbol.TextFont;
import org.audiveris.omr.ui.util.Panel;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.HorizontalSide;
import org.audiveris.omr.util.StringUtil;
import org.audiveris.omr.util.VerticalSide;

import org.jdesktop.application.Application;
import org.jdesktop.application.ApplicationContext;
import org.jdesktop.application.ResourceMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jgoodies.forms.builder.FormBuilder;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.CubicCurve2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * Class <code>SheetPainter</code> provides a basis to paint sheet content.
 * <p>
 * It is specialized in:
 * <ul>
 * <li>{@link SheetGradedPainter} which displays all SIG inters with opacity derived from each inter
 * grade value.</li>
 * <li>{@link SheetResultPainter} which displays the resulting score (SIG remaining inters,
 * measures, time slots, etc).</li>
 * <li>{@link SelectionPainter} which focuses on user-selected items.</li>
 * </ul>
 * The bulk of painting is delegated to an internal {@link SigPainter} instance.
 *
 * @author Hervé Bitteur
 */
public abstract class SheetPainter
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetPainter.class);

    private static final ApplicationContext applicationContext = Application.getInstance()
            .getContext();

    private static final ResourceMap resources = applicationContext.getResourceMap(
            SheetPainter.class);

    /** A transformation to half scale. (used for slot time annotation) */
    protected static final AffineTransform halfAT = AffineTransform.getScaleInstance(0.5, 0.5);

    /** Font for annotations. */
    protected static final Font basicFont = new Font(
            "Sans Serif",
            Font.PLAIN,
            constants.basicFontSize.getValue());

    /** Specifications of Inter classes/shapes that can be displayed in jumbo mode. */
    protected static final List<JumboSpec> jumboSpecs = getJumboSpecs();

    //~ Instance fields ----------------------------------------------------------------------------

    /** Sheet. */
    protected final Sheet sheet;

    /** Scale. */
    protected final Scale scale;

    /** View parameters. */
    protected final ViewParameters viewParams = ViewParameters.getInstance();

    /** Graphic context. */
    protected final Graphics2D g;

    /** Clip rectangle. */
    protected final Rectangle clip;

    /** Painting voices with different colors. */
    protected final boolean withVoices;

    /** Painting some inters classes in jumbo mode. */
    protected final boolean withJumbos;

    /** Painter for Inter instances. */
    protected SigPainter sigPainter;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new SheetPainter object.
     *
     * @param sheet      the sheet to paint
     * @param g          Graphic context
     * @param withVoices true to paint voices with different colors
     * @param withJumbos true to paint some inter classes in jumbo mode
     */
    public SheetPainter (Sheet sheet,
                         Graphics g,
                         boolean withVoices,
                         boolean withJumbos)
    {
        this.sheet = sheet;
        this.scale = sheet.getScale();
        this.g = (Graphics2D) g;
        this.withVoices = withVoices;
        this.withJumbos = withJumbos;

        clip = g.getClipBounds();

        // To avoid the display being slightly clipped near a window border
        final int margin = scale.toPixels(constants.clipMargin);
        clip.grow(margin, margin);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-------------//
    // basicLayout //
    //-------------//
    /**
     * Build a TextLayout from a String of BasicFont characters
     * (transformed by the provided AffineTransform if any)
     *
     * @param str the string of proper codes
     * @param fat potential affine transformation
     * @return the (sized) TextLayout ready to be drawn
     */
    protected TextLayout basicLayout (String str,
                                      AffineTransform fat)
    {
        FontRenderContext frc = g.getFontRenderContext();
        Font font = (fat == null) ? basicFont : basicFont.deriveFont(fat);

        return new TextLayout(str, font, frc);
    }

    //----------------//
    // drawPartLimits //
    //----------------//
    /**
     * Draw the upper and lower core limits of the system.
     * <p>
     * This is just for visual inspection of these "real" limits for important music symbols.
     *
     * @param system the system to be processed
     */
    protected void drawPartLimits (SystemInfo system)
    {
        g.setColor(Colors.PART_CORE_LIMIT);

        for (Part part : system.getParts()) {
            for (VerticalSide side : VerticalSide.values()) {
                final int dy = part.getCoreMargin(side);

                if (dy != 0) {
                    final AffineTransform savedAT = g.getTransform();
                    final LineInfo line;
                    final AffineTransform at;

                    if (side == VerticalSide.TOP) {
                        line = part.getFirstStaff().getFirstLine();
                        at = AffineTransform.getTranslateInstance(0, -dy);
                    } else {
                        line = part.getLastStaff().getLastLine();
                        at = AffineTransform.getTranslateInstance(0, +dy);
                    }

                    g.transform(at);
                    line.renderLine(g, false, 0);
                    g.setTransform(savedAT);
                }
            }
        }
    }

    //---------------//
    // getSigPainter //
    //---------------//
    /**
     * Report the concrete sig painter to be used.
     *
     * @return the sig painter
     */
    protected abstract SigPainter getSigPainter ();

    //---------//
    // isJumbo //
    //---------//
    /**
     * Tell whether the provided inter should be displayed in jumbo mode
     * (larger size, bright color).
     *
     * @param inter the inter to check for jumbo mode
     * @return true if so
     */
    protected boolean isJumbo (Inter inter)
    {
        if (!withJumbos) {
            return false;
        }

        final Class<?> interClass = inter.getClass();
        final Shape interShape = inter.getShape();

        for (JumboSpec spec : jumboSpecs) {
            if (spec.classe.isAssignableFrom(interClass)) {
                return (spec.shape == null) || (spec.shape == interShape);
            }
        }

        return false;
    }

    //----------------//
    // isJumboColored //
    //----------------//
    /**
     * Tell whether the provided inter should be displayed in jumbo mode with a specific color.
     *
     * @param inter the inter to check
     * @return true if so
     */
    protected boolean isJumboColored (Inter inter)
    {
        return constants.jumboColored.isSet() && isJumbo(inter);
    }

    //-------//
    // paint //
    //-------//
    /**
     * This is the general paint method for drawing a symbol layout, at a specified
     * location, using a specified alignment
     *
     * @param layout    what: the symbol, perhaps transformed
     * @param location  where: the precise location in the display
     * @param alignment how: the way the symbol is aligned wrt the location
     */
    protected void paint (TextLayout layout,
                          Point2D location,
                          Alignment alignment)
    {
        OmrFont.paint(g, layout, location, alignment);
    }

    //---------//
    // process //
    //---------//
    /**
     * Paint the sheet.
     */
    public void process ()
    {
        if (scale == null)
            return;

        sigPainter = getSigPainter();

        if (!sheet.getSystems().isEmpty()) {
            for (SystemInfo system : sheet.getSystems()) {
                // Check whether this system is visible
                Rectangle bounds = system.getBounds();

                if ((bounds != null) && ((clip == null) || bounds.intersects(clip))) {
                    processSystem(system);
                }
            }
        }
    }

    //--------------//
    // processParts //
    //--------------//
    /**
     * For every part in provided system, make sure some part name or abbreviation is printed.
     *
     * @param system the containing system
     */
    private void processParts (SystemInfo system)
    {
        if (!viewParams.isPartNamePainting()) {
            return;
        }

        final Color oldColor = g.getColor();
        g.setColor(Colors.MUSIC_ALONE);

        final int partDx = constants.partDx.getValue();
        final int partDy = constants.partDy.getValue();

        final Score score = system.getPage().getScore();
        final double zoom = constants.zoomForPartName.getValue();
        final AffineTransform fat = AffineTransform.getScaleInstance(zoom, zoom);

        for (Part part : system.getParts()) {
            if (part.getName() != null)
                continue; // There is a partName inter, it is displayed by the sig painter

            // Otherwise use info from logical part, if available
            final LogicalPart logical = part.getLogicalPart();
            final PartRef partRef = part.getRef();

            if (logical != null) {
                // First logicap part occurrence displays logical name
                // The next ones display logical abbreviation if available, otherwise the name
                final String str;
                if (score.getFirstOccurrence(logical) == partRef) {
                    str = logical.getName();
                } else {
                    final String abbrev = logical.getAbbreviation();
                    if ((abbrev != null) && !abbrev.isBlank())
                        str = abbrev;
                    else
                        str = logical.getName();
                }

                if (str != null) {
                    final Staff s1 = part.getFirstStaff();
                    final int x = s1.getAbscissa(HorizontalSide.LEFT);
                    final LineInfo l1 = s1.getMidLine();
                    int y = l1.yAt(x - partDx) + partDy;

                    final Staff s2 = part.getLastStaff();
                    if (s2 != s1) {
                        final LineInfo l2 = s2.getMidLine();
                        y = (y + l2.yAt(x - partDx) + partDy) / 2;
                    }

                    final TextLayout layout = basicLayout(str, fat);
                    paint(layout, new Point(x - partDx, y), TOP_RIGHT);
                }
            }
        }

        g.setColor(oldColor);
    }

    //---------------//
    // processSystem //
    //---------------//
    /**
     * Process a system.
     *
     * @param system the system to process
     */
    protected void processSystem (SystemInfo system)
    {
        try {
            // Staff lines attachments
            UIUtil.setAbsoluteStroke(g, 1.0f);

            for (Staff staff : system.getStaves()) {
                staff.renderAttachments(g);
            }

            // Part limits
            if (constants.drawPartLimits.isSet()) {
                drawPartLimits(system);
            }

            // Parts name/abbreviation if needed
            processParts(system);

            // All interpretations for this system
            sigPainter.process(system.getSig());
        } catch (ConcurrentModificationException ignored) { //
        } catch (Exception ex) {
            logger.warn("Cannot paint system#{}", system.getId(), ex);
        }
    }

    //~ Static Methods -----------------------------------------------------------------------------

    //---------------//
    // getJumboSpecs //
    //---------------//
    /**
     * Build the list of jumbo specifications.
     *
     * @return the populated list of specs
     */
    private static List<JumboSpec> getJumboSpecs ()
    {
        final List<JumboSpec> specs = new ArrayList<>();
        final String interPackageName = Inter.class.getPackageName();
        final List<String> specStrings = StringUtil.parseStrings(constants.jumboSpecs.getValue());

        for (String str : specStrings) {
            final String className;
            final String shapeName;
            final int slash = str.indexOf('/');

            if (slash != -1) {
                className = str.substring(0, slash).trim();
                shapeName = str.substring(slash + 1).trim();
            } else {
                className = str;
                shapeName = "";
            }

            final String qualifiedName = interPackageName + "." + className;

            try {
                final Class<?> classe = Class.forName(qualifiedName);
                final Shape shape = !shapeName.isBlank() ? Shape.valueOf(shapeName) : null;
                specs.add(new JumboSpec(classe, shape));
            } catch (ClassNotFoundException ex) {
                logger.warn("Unknown Inter class: {}", qualifiedName);
            } catch (IllegalArgumentException ex) {
                logger.warn("Unknown Shape: {}", shapeName);
            }
        }

        return specs;
    }

    //---------------//
    // getVoicePanel //
    //---------------//
    /**
     * Build a panel which displays all defined voice ID colors.
     * <p>
     * Separate numbers for first staff and second staff as: 1234 / 5678
     *
     * @return the populated voice panel
     */
    public static JPanel getVoicePanel ()
    {
        final int length = Voices.getColorCount();
        final Font font = new Font("Arial", Font.BOLD, UIUtil.adjustedSize(18));
        final Color background = Color.WHITE;
        final StringBuilder sbc = new StringBuilder();

        for (int i = 0; i <= length; i++) {
            if (i != 0) {
                sbc.append(",");
            }

            sbc.append("10dlu");
        }

        final FormLayout layout = new FormLayout(sbc.toString(), "pref");
        final Panel panel = new Panel();
        panel.setName("VoicePanel");
        final FormBuilder builder = FormBuilder.create().layout(layout).panel(panel);

        // Adjust dimensions
        final Dimension cellDim = new Dimension(5, 22);
        panel.setInsets(3, 0, 0, 3); // TLBR

        final int mid = length / 2;

        for (int c = 1; c <= length; c++) {
            final Color color = new Color(Voices.colorOf(c).getRGB()); // Remove alpha
            final JLabel label = new JLabel("" + c, JLabel.CENTER);
            label.setPreferredSize(cellDim);
            label.setFont(font);
            label.setOpaque(true);
            label.setBackground(background);
            label.setForeground(color);

            int col = (c <= mid) ? c : (c + 1);
            builder.addRaw(label).xy(col, 1);
        }

        // Separation between staves
        {
            final Color color = Color.BLACK;
            final JLabel label = new JLabel(" /");
            label.setPreferredSize(cellDim);
            label.setFont(font);
            label.setOpaque(true);
            label.setBackground(background);
            label.setForeground(color);
            builder.addRaw(label).xy(mid + 1, 1);
        }

        // Resource injection
        resources.injectComponents(panel);

        return panel;
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Integer basicFontSize = new Constant.Integer(
                "points",
                30,
                "Standard font size for annotations");

        private final Constant.Boolean drawPartLimits = new Constant.Boolean(
                false,
                "Should we draw part upper and lower core limits");

        private final Constant.Boolean chordVoiceAppended = new Constant.Boolean(
                false,
                "Should the chords voices be appended to ID?");

        private final Constant.Ratio minDisplayZoomForChordId = new Constant.Ratio(
                0.75,
                "Minimum display zoom value to display chords ID");

        private final Constant.Ratio chordIdZoom = new Constant.Ratio(
                0.5,
                "Zoom applied on font for chords ID");

        private final Constant.Integer partDx = new Constant.Integer(
                "pixels",
                40,
                "Abscissa right offset for part name");

        private final Constant.Integer partDy = new Constant.Integer(
                "pixels",
                20,
                "Ordinate down offset for part name");

        private final Constant.Ratio zoomForPartName = new Constant.Ratio(
                0.6,
                "Zoom applied on part names");

        private final Constant.Ratio zoomForJumbos = new Constant.Ratio(
                2.0,
                "Zoom applied on jumbo inters");

        private final Constant.String jumboSpecs = new Constant.String(
                "AugmentationDotInter, ArticulationInter/STACCATO",
                "Comma-separated list of jumbo Inter specifications (class[/shape])");

        private final Constant.Boolean jumboColored = new Constant.Boolean(
                true,
                "Should the jumbo items be colored specifically?");

        private final Scale.Fraction clipMargin = new Scale.Fraction(
                4.0,
                "Margin added to clip bounds to avoid truncation");
    }

    //-----------//
    // JumboSpec //
    //-----------//
    /**
     * Class <code>JumboSpec</code> defines a specification for jumbo display.
     * <p>
     * It is a Inter class, potentially augmented by a Shape.
     */
    private static class JumboSpec
    {
        /** Mandatory. */
        public final Class<?> classe;

        /** Optional shape within the class. */
        public final Shape shape;

        JumboSpec (Class<?> classe,
                   Shape shape)
        {
            this.classe = classe;
            this.shape = shape;
        }
    }

    //------------//
    // SigPainter //
    //------------//
    /**
     * Class <code>SigPainter</code> paints all the {@link Inter} instances of a SIG.
     * <p>
     * Its life ends with the painting of a sheet.
     * <p>
     * Ensembles are generally not painted directly but via their members:
     * <ul>
     * <li>{@link AbstractChordInter}: Notes and stem are painted on their own.
     * SigPainter subclass in {@link SheetResultPainter} adds painting of chord ID and voice.
     * <li>{@link KeyInter}: Each key item member is painted on its own, except for a manual key
     * because such key has no concrete members.
     * <li>{@link SentenceInter} and its subclass {@link LyricLineInter}: Each member word is
     * painted using the sentence mean font.
     * </ul>
     */
    protected abstract class SigPainter
            extends AbstractInterVisitor
    {
        /** Any shape, jumbo size. */
        protected MusicFont musicFontJumbo;

        /** General shape, large size. */
        protected final MusicFont musicFont;

        /** Head shapes, large size. */
        protected final MusicFont headMusicFont;

        /** General shape, small size. */
        protected final MusicFont musicFontSmall;

        /** Head shapes, small size. */
        protected final MusicFont headMusicFontSmall;

        /** Textual score items. */
        protected final TextFont textFont;

        /** Global stroke for curves (slur, wedge, ending). */
        protected final Stroke curveStroke;

        /** Global stroke for stems. */
        protected final Stroke stemStroke;

        /** Global stroke for ledgers, with no glyph. */
        protected final Stroke ledgerStroke;

        /**
         * Creates a new <code>SigPainter</code> object.
         */
        public SigPainter ()
        {
            // Determine proper music fonts
            final MusicFamily musicFamily = sheet.getStub().getMusicFamily();
            final TextFamily textFamily = sheet.getStub().getTextFamily();

            // Jumbo music font?
            if (withJumbos) {
                musicFontJumbo = MusicFont.getBaseFont(
                        musicFamily,
                        (int) Math.rint(scale.getInterline() * constants.zoomForJumbos.getValue()));
            }

            // Standard (large) size
            final int largeInterline = scale.getInterline();
            musicFont = MusicFont.getBaseFont(musicFamily, largeInterline);
            headMusicFont = MusicFont.getHeadFont(musicFamily, scale, largeInterline);
            textFont = TextFont.getTextFont(textFamily, largeInterline * 4);

            // Smaller size?
            final Integer smallInterline = scale.getSmallInterline();
            if (smallInterline != null) {
                musicFontSmall = MusicFont.getBaseFont(musicFamily, smallInterline);
                headMusicFontSmall = MusicFont.getHeadFont(musicFamily, scale, smallInterline);
            } else {
                musicFontSmall = null;
                headMusicFontSmall = null;
            }

            {
                // Stroke for curves (slurs, wedges and endings)
                Integer fore = scale.getFore();
                float width = (float) ((fore != null) ? fore : Curves.DEFAULT_THICKNESS);
                curveStroke = new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
            }

            {
                // Stroke for stems
                Integer stem = scale.getStemThickness();
                float width = (float) ((stem != null) ? stem : StemInter.DEFAULT_THICKNESS);
                stemStroke = new BasicStroke(width, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL);
            }

            {
                // Stroke for ledgers
                float width = (float) LedgerInter.DEFAULT_THICKNESS;
                ledgerStroke = new BasicStroke(
                        width,
                        BasicStroke.CAP_ROUND,
                        BasicStroke.JOIN_ROUND);
            }
        }

        //--------------//
        // getMusicFont //
        //--------------//
        public MusicFont getMusicFont ()
        {
            return getMusicFont(false, false);
        }

        //--------------//
        // getMusicFont //
        //--------------//
        /**
         * Select the proper music font according to end-user font family choice.
         *
         * @param isHead     true for a head inter, which implies a separate font size
         * @param smallStaff true for a small staff
         * @return the sequence of one or several music fonts
         */
        private MusicFont getMusicFont (boolean isHead,
                                        boolean smallStaff)
        {
            if (smallStaff) {
                return isHead ? headMusicFontSmall : musicFontSmall;
            } else {
                return isHead ? headMusicFont : musicFont;
            }
        }

        //--------------//
        // getMusicFont //
        //--------------//
        private MusicFont getMusicFont (boolean isHead,
                                        Staff staff)
        {
            return getMusicFont(isHead, (staff != null) ? staff.isSmall() : false);
        }

        //--------------//
        // getMusicFont //
        //--------------//
        private MusicFont getMusicFont (Staff staff)
        {
            return getMusicFont(false, staff);
        }

        //-------------//
        // getTextFont //
        //-------------//
        public TextFont getTextFont ()
        {
            return textFont;
        }

        //-------------//
        // paintCenter //
        //-------------//
        /**
         * Paint an Inter, using the color of provided colorRef inter, at inter center.
         *
         * @param inter    the inter to paint
         * @param colorRef the inter from which color must be taken
         */
        private void paintCenter (Inter inter,
                                  Inter colorRef)
        {
            final Shape shape = inter.getShape();

            if (shape == null) {
                logger.warn("SigPainter.paintCenter no shape for {}", inter);

                return;
            }

            final Rectangle bounds = inter.getBounds();

            if (bounds != null) {
                final Point2D center = GeoUtil.center2D(bounds);
                final Staff staff = inter.getStaff();
                setColor(colorRef);

                final FontSymbol fs = shape.getFontSymbol(
                        (isJumbo(inter)) ? musicFontJumbo : getMusicFont(shape.isHead(), staff));

                if (fs != null) {
                    fs.paintSymbol(g, center, AREA_CENTER);
                } else {
                    logger.error("No symbol to paint {}", inter);
                }
            }
        }

        //-----------//
        // paintHalf //
        //-----------//
        /**
         * Paint upper and lower parts of a symbol, if it is linked to two shared heads.
         * Otherwise, paint it normally as a whole.
         *
         * @param inter  the inter to paint
         *               (AugmentationDotInter or AlterInter)
         * @param classe the relation class to search between inter and head
         *               (AugmentationRelation or AlterHeadRelation)
         */
        private void paintHalf (Inter inter,
                                Class<? extends Relation> classe)
        {
            final SIGraph sig = inter.getSig();

            if (!splitMirrors() || (sig == null)) {
                paintInter(inter, inter);

                return;
            }

            final List<HeadInter> heads = new ArrayList<>();

            for (Relation rel : sig.getRelations(inter, classe)) {
                Inter opposite = sig.getOppositeInter(inter, rel);

                if (opposite instanceof HeadInter headInter) {
                    heads.add(headInter);
                }
            }

            if ((heads.size() != 2) || (heads.get(0).getMirror() != heads.get(1))) {
                // Standard case where symbol is painted as a whole
                paintInter(inter, inter);
            } else {
                // Split according to linked shared heads
                final Rectangle box = inter.getBounds();
                final int height = box.height;
                final Shape shape = inter.getShape();
                final Staff staff = inter.getStaff();
                final FontSymbol fs = shape.getFontSymbol(getMusicFont(staff));

                if (fs == null) {
                    logger.warn("No symbol for shared head {}", inter);
                    return;
                }

                final Dimension dim = fs.getDimension();
                final int w = dim.width;
                final Point2D ref = inter.getRelationCenter(); // Not always the area center
                final Line2D line = new Line2D.Double(
                        ref.getX() - w,
                        ref.getY(),
                        ref.getX() + w,
                        ref.getY());

                // Draw each inter half
                for (HeadInter h : heads) {
                    // Define clipping area for half above or below line
                    final AbstractChordInter ch = h.getChord();
                    final int yDir = (ch.getCenter().y > h.getCenter().y) ? (+1) : (-1);
                    final Path2D p = new Path2D.Double();
                    p.append(line, false);
                    p.lineTo(line.getX2(), line.getY2() + (yDir * height));
                    p.lineTo(line.getX1(), line.getY1() + (yDir * height));
                    p.closePath();
                    final java.awt.Shape oldClip = g.getClip();
                    g.clip(p);

                    paintInter(inter, ch);

                    g.setClip(oldClip);
                }
            }
        }

        //------------//
        // paintInter //
        //------------//
        /**
         * Paint an Inter, using the color of provided colorRef inter.
         *
         * @param inter    the inter to paint
         * @param colorRef the inter from which color must be taken
         */
        private void paintInter (Inter inter,
                                 Inter colorRef)
        {
            if (inter instanceof AbstractPitchedInter pitched) {
                paintPitched(pitched, colorRef);
            } else {
                paintCenter(inter, colorRef);
            }
        }

        //--------------//
        // paintPitched //
        //--------------//
        /**
         * Paint a pitched inter by centering on its pitch position.
         *
         * @param inter    the AbstractPichedInter to paint (AlterInter or KeyAlterInter)
         * @param colorRef the inter to be used as reference for color
         */
        private void paintPitched (AbstractPitchedInter inter,
                                   Inter colorRef)
        {
            setColor(colorRef);

            final Shape shape = inter.getShape();
            final Point2D center = GeoUtil.center2D(inter.getBounds());
            Staff staff = inter.getStaff();

            if (staff == null) {
                SystemInfo system = inter.getSig().getSystem();
                staff = system.getClosestStaff(center);
            }

            if (!staff.isTablature()) {
                center.setLocation(
                        center.getX(),
                        staff.pitchToOrdinate(center.getX(), inter.getPitch()));
            }

            final FontSymbol fs = shape.getFontSymbol(getMusicFont(shape.isHead(), staff));

            if (fs != null) {
                final TextLayout layout = fs.getLayout();
                final Rectangle2D box = layout.getBounds();
                center.setLocation(center.getX(), center.getY() + box.getY() + box.getHeight());
                OmrFont.paint(g, layout, center, Alignment.BOTTOM_CENTER);
            } else {
                logger.error("No symbol to paint {}", inter);
            }
        }

        //-----------//
        // paintWord //
        //-----------//
        protected void paintWord (WordInter word,
                                  FontInfo fontInfo)
        {
            if (word.getValue().trim().isEmpty()) {
                return;
            }

            if (fontInfo == null) {
                logger.warn("No font information for {}", word);

                return;
            }

            setColor(word);

            if (word instanceof MusicWordInter) {
                final MusicFont mf = musicFont.deriveFont((float) fontInfo.pointsize);
                final TextLayout layout = mf.layout(word.getValue());
                paint(layout, word.getCenter(), AREA_CENTER);
            } else {
                final TextFont tf = TextFont.create(textFont, fontInfo);
                final TextLayout layout = tf.layout(word.getValue());
                paint(layout, word.getLocation(), BASELINE_LEFT);
            }
        }

        //---------//
        // process //
        //---------//
        public void process (SIGraph sig)
        {
            // Use a COPY of vertices, to reduce risks of concurrent modifications (but not all...)
            for (Inter inter : new LinkedHashSet<>(sig.vertexSet())) {
                if (!inter.isRemoved()) {
                    final Rectangle bounds = inter.getBounds();

                    if (bounds != null) {
                        if ((clip == null) || clip.intersects(bounds)) {
                            inter.accept(this);
                        }
                    }
                }
            }
        }

        //----------//
        // setColor //
        //----------//
        /**
         * Use color adapted to current inter and global viewing parameters.
         *
         * @param inter the interpretation to colorize
         */
        protected abstract void setColor (Inter inter);

        //--------------//
        // splitMirrors //
        //--------------//
        /**
         * Tell whether shared heads are split.
         *
         * @return true if so
         */
        protected abstract boolean splitMirrors ();

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (AbstractBeamInter beam)
        {
            setColor(beam);
            g.fill(beam.getArea());
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (AbstractChordInter chord)
        {
            // Draw chord ID & voice if any
            if (!viewParams.isChordIdsPainting()) {
                return;
            }

            // For readability, we need a sufficient display zoom
            final double displayZoom = g.getTransform().getScaleX();

            if (displayZoom < constants.minDisplayZoomForChordId.getValue()) {
                return;
            }

            Color oldColor = g.getColor();
            g.setColor(Colors.ANNOTATION_CHORD);

            Rectangle box = chord.getBounds();
            Point pt = new Point(box.x, box.y + (box.height / 2));

            // Chord ID
            String str = Integer.toString(chord.getId());

            // Chord voice
            if (constants.chordVoiceAppended.isSet()) {
                Voice voice = chord.getVoice();

                if (voice != null) {
                    str = str + (" v" + voice.getId());
                }
            }

            final double idZoom = constants.chordIdZoom.getValue();
            final double z = Math.max(idZoom, displayZoom);
            final AffineTransform at = AffineTransform.getScaleInstance(idZoom / z, idZoom / z);
            final TextLayout layout = basicLayout(str, at);
            paint(layout, pt, MIDDLE_LEFT);

            g.setColor(oldColor);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (AbstractFlagInter flag)
        {
            setColor(flag);

            SIGraph sig = flag.getSig();
            Set<Relation> rels = (sig != null) ? sig.getRelations(flag, FlagStemRelation.class)
                    : Collections.emptySet();

            if (rels.isEmpty()) {
                // The flag exists in sig, but is not yet linked to a stem, use default painting
                visit((Inter) flag);
            } else {
                // Paint the flag precisely on stem abscissa
                StemInter stem = (StemInter) sig.getOppositeInter(flag, rels.iterator().next());
                Point location = new Point(stem.getCenter().x, flag.getCenter().y);

                final Shape shape = flag.getShape();
                final FontSymbol fs = shape.getFontSymbol(getMusicFont(flag.getStaff()));

                if (fs != null) {
                    fs.paintSymbol(g, location, MIDDLE_LEFT);
                } else {
                    logger.error("No symbol to paint flag {}", flag);
                }
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (AbstractNumberInter inter)
        {
            setColor(inter);

            final Staff staff = inter.getStaff();
            final Rectangle bounds = inter.getBounds();

            if (bounds != null) {
                MusicFont font = getMusicFont(staff);
                final ShapeSymbol symbol = new NumberSymbol(
                        inter.getShape(),
                        font.getMusicFamily(),
                        inter.getValue());
                final Point2D center = GeoUtil.center2D(bounds);

                // Adapt symbol to actual bounds, which can be much larger than standard symbol
                final Dimension dim = symbol.getDimension(font);
                final float ratio = bounds.height / (float) dim.getHeight();
                font = font.deriveFont(ratio * font.getSize());
                symbol.paintSymbol(g, font, center, AREA_CENTER);
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (AlterInter inter)
        {
            paintHalf(inter, AlterHeadRelation.class);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (ArpeggiatoInter arpeggiato)
        {
            setColor(arpeggiato);

            final Rectangle bx = arpeggiato.getBounds();
            final Point location = new Point(bx.x + (bx.width / 2), bx.y);
            final Shape shape = arpeggiato.getShape();
            final FontSymbol fs = shape.getFontSymbol(getMusicFont(arpeggiato.getStaff()));

            if (fs == null) {
                logger.warn("No symbol to paint for arpeggiato {}", arpeggiato);
                return;
            }

            Dimension dim = fs.getDimension();
            bx.grow(dim.width, 0); // To avoid any clipping on x

            if (clip != null) {
                g.setClip(clip.intersection(bx));
            }

            // Nb of symbols to draw, one below the other
            final int nb = (int) Math.ceil((double) bx.height / dim.height);

            for (int i = 0; i < nb; i++) {
                fs.paintSymbol(g, location, TOP_CENTER);
                location.y += dim.height;
            }

            if (clip != null) {
                g.setClip(clip);
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (AugmentationDotInter inter)
        {
            paintHalf(inter, AugmentationRelation.class);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (BarConnectorInter connector)
        {
            setColor(connector);
            g.fill(connector.getArea());
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (BarlineInter barline)
        {
            setColor(barline);
            g.fill(barline.getArea());
        }

        // No visit for beam group

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (BeatUnitInter word)
        {
            final FontInfo fontInfo = word.getFontInfo();
            paintWord(word, fontInfo);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (BraceInter brace)
        {
            final Rectangle bounds = brace.getBounds();

            if (bounds != null) {
                setColor(brace);
                final Staff staff = brace.getStaff();
                final Point2D center = GeoUtil.center2D(bounds);
                final FontSymbol fs = Shape.BRACE.getFontSymbol(getMusicFont(staff));
                final TextLayout layout = fs.getLayout(new Dimension(bounds.width, bounds.height));
                OmrFont.paint(g, layout, center, AREA_CENTER);
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (BracketConnectorInter connector)
        {
            setColor(connector);
            g.fill(connector.getArea());
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (BracketInter bracket)
        {
            setColor(bracket);

            final BracketInter.BracketKind kind = bracket.getKind();
            final Line2D median = bracket.getMedian(); // Trunk median
            final double width = bracket.getWidth(); // Trunk width

            final MusicFont font = getMusicFont();

            // Upper symbol part?
            if ((kind == BracketInter.BracketKind.TOP) || (kind == BracketInter.BracketKind.BOTH)) {
                final FontSymbol upper = Shape.BRACKET_UPPER_SERIF.getFontSymbol(font);
                if (upper != null) {
                    final Point2D topLeft = new Point2D.Double(
                            median.getX1() - (width / 2),
                            median.getY1());
                    upper.paintSymbol(g, topLeft, BOTTOM_LEFT);
                }
            }

            // Lower symbol part?
            if ((kind == BracketInter.BracketKind.BOTTOM)
                    || (kind == BracketInter.BracketKind.BOTH)) {
                final FontSymbol lower = Shape.BRACKET_LOWER_SERIF.getFontSymbol(font);
                if (lower != null) {
                    final Point2D botLeft = new Point2D.Double(
                            median.getX2() - (width / 2),
                            median.getY2());
                    lower.paintSymbol(g, botLeft, TOP_LEFT);
                }
            }

            // Trunk area
            g.fill(AreaUtil.verticalParallelogram(median.getP1(), median.getP2(), width));
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (ClefInter clef)
        {
            visit((Inter) clef);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (EndingInter ending)
        {
            setColor(ending);
            g.setStroke(curveStroke);
            g.draw(ending.getLine());

            if (ending.getLeftLeg() != null) {
                g.draw(ending.getLeftLeg());
            }

            if (ending.getRightLeg() != null) {
                g.draw(ending.getRightLeg());
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (GraceChordInter grace)
        {
            visit((Inter) grace); // Paint grace shape
            super.visit(grace); // Paint chord
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (HeadInter head)
        {
            final Line2D midLine = head.getMidLine();

            if (midLine != null) {
                if (splitMirrors()) {
                    // Draw head proper half
                    int width = head.getBounds().width;
                    int xDir = (midLine.getY2() > midLine.getY1()) ? (-1) : (+1);
                    Path2D p = new Path2D.Double();
                    p.append(midLine, false);
                    p.lineTo(midLine.getX2() + (xDir * width), midLine.getY2());
                    p.lineTo(midLine.getX1() + (xDir * width), midLine.getY1());
                    p.closePath();

                    java.awt.Shape oldClip = g.getClip();
                    g.clip(p);
                    visit((Inter) head);
                    g.setClip(oldClip);
                } else {
                    visit((Inter) head);
                }

                // Draw midLine using complementary color of head
                Color compColor = UIUtil.complementaryColor(g.getColor());
                Stroke oldStroke = UIUtil.setAbsoluteStroke(g, 1f);
                g.setColor(compColor);
                g.draw(midLine);
                g.setStroke(oldStroke);
            } else {
                visit((Inter) head);
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (Inter inter)
        {
            paintInter(inter, inter);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (KeyAlterInter inter)
        {
            paintInter(inter, inter);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (KeyInter key)
        {
            // Normally, key ensemble is painted via its alter members
            // But for a manual key, there are no members available, so we paint the symbol
            if (key.isManual()) {
                final Rectangle bounds = key.getBounds();

                if (bounds != null) {
                    final Point2D center = GeoUtil.center2D(bounds);
                    final Staff staff = key.getStaff();
                    setColor(key);

                    final MusicFont font = getMusicFont(staff);
                    ShapeSymbol symbol = key.getSymbolToDraw(font);

                    if (symbol == null) {
                        symbol = font.getSymbol(Shape.NON_DRAGGABLE);
                    }

                    symbol.paintSymbol(g, font, center, AREA_CENTER);
                }
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (LedgerInter ledger)
        {
            setColor(ledger);

            final double thickness = ledger.getThickness();

            if (thickness != 0) {
                g.setStroke(
                        new BasicStroke(
                                (float) Math.rint(thickness),
                                BasicStroke.CAP_ROUND,
                                BasicStroke.JOIN_ROUND));
            } else {
                g.setStroke(ledgerStroke); // Should not occur
            }

            g.draw(ledger.getMedian());
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (MetronomeInter inter)
        {
            // Painted directky only when its member words are not yet created (case of a ghost)
            // Otherwise, the member words are painted individually
            if (inter.getId() == 0) {
                visit((Inter) inter);
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (MultipleRestInter rest)
        {
            setColor(rest);

            final MusicFont font = getMusicFont();
            final Line2D median = rest.getMedian();

            // Left
            final TextLayout left = Shape.MULTIPLE_REST_LEFT.getFontSymbol(font).getLayout();
            OmrFont.paint(g, left, median.getP1(), MIDDLE_LEFT);

            // Right
            final TextLayout right = Shape.MULTIPLE_REST_RIGHT.getFontSymbol(font).getLayout();
            OmrFont.paint(g, right, median.getP2(), MIDDLE_RIGHT);

            // Middle
            final TextLayout middle = Shape.MULTIPLE_REST_MIDDLE.getFontSymbol(font).getLayout();
            g.fill(
                    AreaUtil.horizontalParallelogram(
                            new Point2D.Double(
                                    median.getX1() + left.getBounds().getWidth() / 2,
                                    median.getY1()),
                            new Point2D.Double(
                                    median.getX2() - right.getBounds().getWidth() / 2,
                                    median.getY2()),
                            middle.getBounds().getHeight()));
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (OctaveShiftInter os)
        {
            final Rectangle bounds = os.getBounds();
            if (bounds == null) {
                return;
            }

            setColor(os);
            final Staff staff = os.getStaff();
            final MusicFont font = getMusicFont(staff);

            // Value part
            final FontSymbol fs = os.getShape().getFontSymbol(font);
            final TextLayout layout = fs.getLayout();
            final Rectangle2D symBounds = layout.getBounds();
            final Point2D p1 = os.getLine().getP1();
            fs.paintSymbol(g, p1, AREA_CENTER);

            // Line (drawn from right to left, to preserve the right corner with hook)
            g.setStroke(OctaveShiftSymbol.DEFAULT_STROKE);
            g.draw(
                    new Line2D.Double(
                            os.getLine().getP2(),
                            new Point2D.Double(p1.getX() + symBounds.getWidth() / 2, p1.getY())));

            // Hook?
            final Point2D hookEnd = os.getHookCopy();
            if (hookEnd != null) {
                g.draw(new Line2D.Double(os.getLine().getP2(), hookEnd));
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (RestInter rest)
        {
            switch (rest.getShape()) {
                default -> paintPitched(rest, rest);
                // Symbols for these shapes are not aligned with pitch ordinate, so we use area center
                case HALF_REST, WHOLE_REST, BREVE_REST -> paintCenter(rest, rest);
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (SentenceInter sentence)
        {
            //            final FontInfo lineMeanFont = sentence.getMeanFont();
            //            for (Inter member : sentence.getMembers()) {
            //                WordInter word = (WordInter) member;
            //
            //                if (!(word instanceof MusicWordInter)) {
            //                    paintWord(word, lineMeanFont);
            //                }
            //                ///paintWord(word, word.getFontInfo());
            //            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (SlurInter slur)
        {
            CubicCurve2D curve = slur.getCurve();

            if (curve != null) {
                setColor(slur);
                g.setStroke(curveStroke);
                g.draw(curve);
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (StaffBarlineInter inter)
        {
            List<Inter> members = inter.getMembers(); // Needs sig, thus it can't be used for ghost.

            if (!members.isEmpty()) {
                for (Inter member : members) {
                    member.accept(this);
                }
            } else if (inter.getShape() != null) {
                visit((Inter) inter);
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (StemInter stem)
        {
            setColor(stem);

            g.setStroke(stemStroke);

            g.draw(stem.getMedian());
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (TimeCustomInter inter)
        {
            setColor(inter);

            final Staff staff = inter.getStaff();
            final Rectangle bounds = inter.getBounds();

            if (bounds != null) {
                final MusicFont font = getMusicFont(staff);
                final ShapeSymbol symbol = new NumDenSymbol(
                        Shape.TIME_CUSTOM,
                        font.getMusicFamily(),
                        inter.getNumerator(),
                        inter.getDenominator());
                final Point2D center = GeoUtil.center2D(bounds);
                symbol.paintSymbol(g, font, center, AREA_CENTER);
            }
        }

        // No time pair

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (TimeWholeInter inter)
        {
            visit((Inter) inter);
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (VerticalSerifInter serif)
        {
            // We don't display the vertical serifs if the MultipleRestInter is present
            // because the MultipleRestInter already displays the left & right portions
            if (serif.isAbnormal()) {
                setColor(serif);
                g.fill(serif.getArea());
            }
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (WedgeInter wedge)
        {
            setColor(wedge);
            g.setStroke(curveStroke);
            g.draw(wedge.getLine1());
            g.draw(wedge.getLine2());
        }

        //-------//
        // visit //
        //-------//
        @Override
        public void visit (WordInter word)
        {
            // Usually, words are displayed via their containing sentence, using sentence mean font.
            // But in the specific case of a (temporarily) orphan word, we display the word as it is.
            //            if ((word.getSig() == null) || (word.getEnsemble() == null)) {
            FontInfo fontInfo = word.getFontInfo();
            paintWord(word, fontInfo);
            //            }
        }
    }
}
