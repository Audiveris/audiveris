//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h e e t                                  //
//                                                                            //
//----------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">                          //
//  Copyright (C) Herve Bitteur 2000-2010. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.   //
//----------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.SymbolsModel;
import omr.glyph.facets.Glyph;
import omr.glyph.ui.SymbolsController;
import omr.glyph.ui.SymbolsEditor;

import omr.lag.Sections;

import omr.log.Logger;

import omr.score.Score;
import omr.score.ScoresManager;
import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.entity.Page;
import omr.score.entity.SystemNode;

import omr.selection.SelectionService;
import omr.selection.SheetLocationEvent;

import omr.sheet.picture.ImageFormatException;
import omr.sheet.picture.Picture;
import omr.sheet.picture.PictureView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.ScoreColorizer;
import omr.sheet.ui.SheetAssembly;
import omr.sheet.ui.SheetsController;

import omr.step.Step;
import omr.step.StepException;
import omr.step.Stepping;
import omr.step.Steps;

import omr.ui.BoardsPane;
import omr.ui.ErrorsEditor;

import omr.util.BrokenLine;

import java.awt.*;
import java.awt.image.RenderedImage;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * Class <code>Sheet</code> is the corner stone for Sheet processing, keeping
 * pointers to all processings related to the image.
 *
 * @author Hervé Bitteur
 */
public class Sheet
{
    //~ Static fields/initializers ---------------------------------------------

    /** Specific application parameters */
    private static final Constants constants = new Constants();

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Sheet.class);

    //~ Instance fields --------------------------------------------------------

    /** Containing score */
    private final Score score;

    /** Corresponding page */
    private final Page page;

    /** The recording of key processing data */
    private SheetBench bench;

    /** The related picture */
    private Picture picture;

    /** Global scale for this sheet */
    private Scale scale;

    /** Initial skew value */
    private Skew skew;

    /** Retrieved staves */
    private List<StaffInfo> staves;

    /** Horizontal entities */
    private Horizontals horizontals;

    /** Horizontal lag (built by LINES/LinesBuilder) */
    private GlyphLag hLag;

    /** Vertical lag (built by SYSTEMS/BarsBuilder) */
    private GlyphLag vLag;

    /** Sheet height in pixels */
    private int height = -1;

    /** Sheet width in pixels */
    private int width = -1;

    /** Retrieved systems. Set by SYSTEMS. */
    private final List<SystemInfo> systems = new ArrayList<SystemInfo>();

    /**
     * Non-lag related selections for this sheet
     * (SheetLocation, ScoreLocation and PixelLevel)
     */
    private SelectionService selectionService = new SelectionService();

    // Companion processors

    /** Related assembly instance */
    private volatile SheetAssembly assembly;

    /** Dedicated skew builder */
    private volatile SkewBuilder skewBuilder;

    /** A staff line extractor for this sheet */
    private volatile StavesBuilder stavesBuilder;

    /** A ledger line extractor for this sheet */
    private volatile HorizontalsBuilder horizontalsBuilder;

    /** A bar line extractor for this sheet */
    private volatile SystemsBuilder systemsBuilder;

    /** Specific builder dealing with glyphs */
    private volatile SymbolsController symbolsController;

    /** Related verticals model */
    private volatile VerticalsController verticalsController;

    /** Related symbols editor */
    private SymbolsEditor editor;

    /** Related errors editor */
    private volatile ErrorsEditor errorsEditor;

    /** The current maximum value for foreground pixels */
    private Integer maxForeground;

    /** The histogram ratio to be used on this sheet to retrieve staves */
    private Double histoRatio;

    /** The step being done on this sheet */
    private Step currentStep;

    /** All steps already done on this sheet */
    private Set<Step> doneSteps = new HashSet<Step>();

    //~ Constructors -----------------------------------------------------------

    //-------//
    // Sheet //
    //-------//
    /**
     * Create a new <code>Sheet</code> instance, based on a given image.
     *
     * @param page the related score page
     * @param image the already loaded image
     */
    public Sheet (Page          page,
                  RenderedImage image)
        throws StepException
    {
        this.page = page;
        this.score = page.getScore();

        // Related bench at sheet level
        bench = new SheetBench(this);

        // Update UI information if so needed
        if (Main.getGui() != null) {
            errorsEditor = new ErrorsEditor(this);
            Main.getGui().sheetsController.createAssembly(this);
        }

        setImage(image);

        if (logger.isFineEnabled()) {
            logger.fine("Created " + this);
        }
    }

    //~ Methods ----------------------------------------------------------------

    //-----------------//
    // getActiveGlyphs //
    //-----------------//
    /**
     * Export the active glyphs of the vertical lag.
     *
     * @return the collection of glyphs for which at least a section is assigned
     */
    public Collection<Glyph> getActiveGlyphs ()
    {
        return vLag.getActiveGlyphs();
    }

    //-------------//
    // setAssembly //
    //-------------//
    /**
     * Remember the link to the related sheet display assembly
     *
     * @param assembly the related sheet assembly
     */
    public void setAssembly (SheetAssembly assembly)
    {
        this.assembly = assembly;
    }

    //-------------//
    // getAssembly //
    //-------------//
    /**
     * Report the related SheetAssembly for GUI
     *
     * @return the assembly, or null otherwise
     */
    public SheetAssembly getAssembly ()
    {
        return assembly;
    }

    //----------//
    // getBench //
    //----------//
    /**
     * Report the related sheet bench
     * @return the related bench
     */
    public SheetBench getBench ()
    {
        return bench;
    }

    //----------------//
    // getCurrentStep //
    //----------------//
    /**
     * Retrieve the step being processed "as we speak"
     * @return the current step
     */
    public Step getCurrentStep ()
    {
        return currentStep;
    }

    //----------------------//
    // setDefaultHistoRatio //
    //----------------------//
    /**
     * Set the default value of histogram threhold for staff detection
     * @param histoRatio the default ratio of maximum histogram value
     */
    public static void setDefaultHistoRatio (double histoRatio)
    {
        constants.defaultStaffThreshold.setValue(histoRatio);
    }

    //----------------------//
    // getDefaultHistoRatio //
    //----------------------//
    /**
     * Report the default value of histogram threhold for staff detection
     * @return the default ratio of maximum histogram value
     */
    public static double getDefaultHistoRatio ()
    {
        return constants.defaultStaffThreshold.getValue();
    }

    //-------------------------//
    // setDefaultMaxForeground //
    //-------------------------//
    public static void setDefaultMaxForeground (int level)
    {
        constants.maxForegroundGrayLevel.setValue(level);
    }

    //-------------------------//
    // getDefaultMaxForeground //
    //-------------------------//
    public static int getDefaultMaxForeground ()
    {
        return constants.maxForegroundGrayLevel.getValue();
    }

    //----------------//
    // setCurrentStep //
    //----------------//
    public void setCurrentStep (Step step)
    {
        currentStep = step;
    }

    //--------//
    // isDone //
    //--------//
    /**
     * Report whether the specified step has been performed onn this sheet
     * @param step the step to check
     * @return true if already performed
     */
    public boolean isDone (Step step)
    {
        return doneSteps.contains(step);
    }

    //-----------------//
    // getErrorsEditor //
    //-----------------//
    public ErrorsEditor getErrorsEditor ()
    {
        return errorsEditor;
    }

    //-----------//
    // getHeight //
    //-----------//
    /**
     * Report the picture height in pixels
     *
     * @return the picture height
     */
    public int getHeight ()
    {
        return height;
    }

    //---------------//
    // setHistoRatio //
    //---------------//
    /**
     * Set the sheet value of histogram threhold for staff detection
     * @param histoRatio the ratio of maximum histogram value
     */
    public void setHistoRatio (double histoRatio)
    {
        this.histoRatio = histoRatio;
    }

    //---------------//
    // getHistoRatio //
    //---------------//
    /**
     * Get the sheet value of histogram threhold for staff detection.
     * If the value is not yet set, it is set to the default value and returned.
     * @return the ratio of maximum histogram value
     * @see #hasHistoRatio()
     */
    public double getHistoRatio ()
    {
        if (!hasHistoRatio()) {
            setHistoRatio(getDefaultHistoRatio());
        }

        return histoRatio;
    }

    //------------------//
    // setHorizontalLag //
    //------------------//
    /**
     * Assign the current horizontal lag for the sheet
     *
     * @param hLag the horizontal lag at hand
     */
    public void setHorizontalLag (GlyphLag hLag)
    {
        this.hLag = hLag;
    }

    //------------------//
    // getHorizontalLag //
    //------------------//
    /**
     * Report the current horizontal lag for this sheet
     *
     * @return the current horizontal lag
     */
    public GlyphLag getHorizontalLag ()
    {
        return hLag;
    }

    //----------------//
    // setHorizontals //
    //----------------//
    /**
     * Set horizontals system by system
     *
     * @param horizontals the horizontals found
     */
    public void setHorizontals (Horizontals horizontals)
    {
        this.horizontals = horizontals;
    }

    //----------------//
    // getHorizontals //
    //----------------//
    /**
     * Retrieve horizontals system by system
     *
     * @return the horizontals found
     */
    public Horizontals getHorizontals ()
    {
        return horizontals;
    }

    //-----------------------//
    // setHorizontalsBuilder //
    //-----------------------//
    /**
     * Set the builder in charge of ledger lines
     *
     * @param horizontalsBuilder the builder instance
     */
    public void setHorizontalsBuilder (HorizontalsBuilder horizontalsBuilder)
    {
        this.horizontalsBuilder = horizontalsBuilder;
    }

    //-----------------------//
    // getHorizontalsBuilder //
    //-----------------------//
    /**
     * Give access to the builder in charge of ledger lines
     *
     * @return the builder instance
     */
    public HorizontalsBuilder getHorizontalsBuilder ()
    {
        return horizontalsBuilder;
    }

    //-------//
    // getId //
    //-------//
    public String getId ()
    {
        return page.getId();
    }

    //----------//
    // setImage //
    //----------//
    public final void setImage (RenderedImage image)
        throws StepException
    {
        try {
            picture = new Picture(image);

            if (picture.getImplicitForeground() == null) {
                picture.setMaxForeground(getMaxForeground());
            }

            setPicture(picture);
            getBench()
                .recordImageDimension(picture.getWidth(), picture.getHeight());

            done(Steps.valueOf(Steps.LOAD));
        } catch (ImageFormatException ex) {
            String msg = "Unsupported image format in file " +
                         getScore()
                             .getImagePath() + "\n" + ex.getMessage();

            if (Main.getGui() != null) {
                Main.getGui()
                    .displayWarning(msg);
            } else {
                logger.warning(msg);
            }

            throw new StepException(ex);
        }
    }

    //--------------//
    // getInterline //
    //--------------//
    /**
     * Convenient method to report the scaling information of the sheet
     * @return the scale interline
     */
    public int getInterline ()
    {
        return scale.interline();
    }

    //--------------//
    // getLogPrefix //
    //--------------//
    /**
     * Report the proper prefix to use when logging a message
     * @return the proper prefix
     */
    public String getLogPrefix ()
    {
        if (ScoresManager.isMultiScore()) {
            return getId() + " ";
        } else {
            if (score.isMultiPage()) {
                return "#" + page.getIndex() + " ";
            } else {
                return "";
            }
        }
    }

    //------------------//
    // setMaxForeground //
    //------------------//
    public void setMaxForeground (int level)
    {
        this.maxForeground = level;
    }

    //------------------//
    // getMaxForeground //
    //------------------//
    public int getMaxForeground ()
    {
        if (!hasMaxForeground()) {
            maxForeground = getDefaultMaxForeground();
        }

        return maxForeground;
    }

    //-------------//
    // isOnSymbols //
    //-------------//
    /**
     * Check whether current step is SYMBOLS
     *
     * @return true if on SYMBOLS
     */
    public boolean isOnSymbols ()
    {
        return Stepping.getLatestStep(this) == Steps.valueOf(Steps.SYMBOLS);
    }

    //---------//
    // getPage //
    //---------//
    /**
     * @return the page
     */
    public Page getPage ()
    {
        return page;
    }

    //------------//
    // getPicture //
    //------------//
    /**
     * Report the picture of this sheet, that is the image to be processed.
     *
     * @return the related picture
     */
    public Picture getPicture ()
    {
        return picture;
    }

    //----------//
    // setScale //
    //----------//
    /**
     * Link scale information to this sheet
     *
     * @param scale the computed (or read from score file) scale
     * @throws StepException
     */
    public void setScale (Scale scale)
        throws StepException
    {
        this.scale = scale;
        page.setScale(scale);

        // Remember current sheet dimensions in pixels
        width = getPicture()
                    .getWidth();
        height = getPicture()
                     .getHeight();

        page.setDimension(new PixelDimension(width, height));

        // Check we've got something usable
        if (scale.mainFore() == 0) {
            logger.warning(
                "Invalid scale mainFore value : " + scale.mainFore());
            throw new StepException();
        }
    }

    //----------//
    // getScale //
    //----------//
    /**
     * Report the computed scale of this sheet. This drives several processing
     * thresholds.
     *
     * @return the sheet scale
     */
    public Scale getScale ()
    {
        return scale;
    }

    //----------//
    // getScore //
    //----------//
    /**
     * Return the eventual Score that gathers in a score the information
     * retrieved from this sheet.
     *
     * @return the related score, or null if not available
     */
    public Score getScore ()
    {
        return score;
    }

    //---------------------//
    // getSelectionService //
    //---------------------//
    /**
     * Report the sheet selection service
     * (which handles SheetLocationEvent, PixelLevelEvent, ScoreLocationEvent)
     * @return the sheet dedicated event service
     */
    public SelectionService getSelectionService ()
    {
        return selectionService;
    }

    //-----------------//
    // getShapedGlyphs //
    //-----------------//
    /**
     * Report the collection of glyphs whose shape is identical to the provided
     * shape
     * @param shape the imposed shape
     * @return the (perhaps empty) collection of active glyphs with right shape
     */
    public Collection<Glyph> getShapedGlyphs (Shape shape)
    {
        List<Glyph> found = new ArrayList<Glyph>();

        for (Glyph glyph : getActiveGlyphs()) {
            if (glyph.getShape() == shape) {
                found.add(glyph);
            }
        }

        return found;
    }

    //---------//
    // setSkew //
    //---------//
    /**
     * Link skew information to this sheet
     *
     * @param skew the skew information
     */
    public void setSkew (Skew skew)
    {
        this.skew = skew;
        page.setSkewAngle(getSkew().angle());

        // Update displayed image if any
        if (getPicture()
                .isRotated() && (Main.getGui() != null)) {
            assembly.getComponent()
                    .repaint();
        }

        // Remember final sheet dimensions in pixels
        width = getPicture()
                    .getWidth();
        height = getPicture()
                     .getHeight();
    }

    //---------//
    // getSkew //
    //---------//
    /**
     * Report the skew information for this sheet.  If not yet available,
     * processing is launched to compute the average skew in the sheet image.
     *
     * @return the skew information
     */
    public Skew getSkew ()
    {
        return skew;
    }

    //----------------//
    // setSkewBuilder //
    //----------------//
    public void setSkewBuilder (SkewBuilder skewBuilder)
    {
        this.skewBuilder = skewBuilder;
    }

    //----------------//
    // getSkewBuilder //
    //----------------//
    /**
     * Give access to the builder in charge of skew computation
     *
     * @return the builder instance
     */
    public SkewBuilder getSkewBuilder ()
    {
        return skewBuilder;
    }

    //------------------//
    // getStaffIndexAtY //
    //------------------//
    /**
     * Given the ordinate of a point, retrieve the index of the nearest staff
     *
     * @param y the point ordinate
     *
     * @return the index of the nearest staff
     */
    public int getStaffIndexAtY (int y)
    {
        int res = Collections.binarySearch(
            getStaves(),
            Integer.valueOf(y),
            new Comparator<Object>() {
                    public int compare (Object o1,
                                        Object o2)
                    {
                        int y;

                        if (o1 instanceof Integer) {
                            y = ((Integer) o1).intValue();

                            StaffInfo staff = (StaffInfo) o2;

                            if (y < staff.getAreaTop()) {
                                return -1;
                            }

                            if (y > staff.getAreaBottom()) {
                                return +1;
                            }

                            return 0;
                        } else {
                            return -compare(o2, o1);
                        }
                    }
                });

        if (res >= 0) { // Found

            return res;
        } else {
            // Should not happen!
            logger.severe("getStaffIndexAtY. No nearest staff for y = " + y);

            return -res - 1; // Not found
        }
    }

    //-----------//
    // setStaves //
    //-----------//
    /**
     * Set the list of staves found in the sheet
     *
     * @param staves the collection of staves found
     */
    public void setStaves (List<StaffInfo> staves)
    {
        this.staves = staves;
    }

    //-----------//
    // getStaves //
    //-----------//
    /**
     * Report the list of staves found in the sheet
     *
     * @return the collection of staves found
     */
    public List<StaffInfo> getStaves ()
    {
        return staves;
    }

    //------------------//
    // setStavesBuilder //
    //------------------//
    /**
     * Set the builder in charge of staff lines
     *
     * @param stavesBuilder the builder instance
     */
    public void setStavesBuilder (StavesBuilder stavesBuilder)
    {
        this.stavesBuilder = stavesBuilder;
    }

    //------------------//
    // getStavesBuilder //
    //------------------//
    /**
     * Give access to the builder in charge of staff lines
     *
     * @return the builder instance
     */
    public StavesBuilder getStavesBuilder ()
    {
        return stavesBuilder;
    }

    //----------------------//
    // getSymbolsController //
    //----------------------//
    /**
     * Give access to the module dealing with symbol management
     *
     * @return the symbols model
     */
    public SymbolsController getSymbolsController ()
    {
        if (symbolsController == null) {
            createSymbolsControllerAndEditor();
        }

        return symbolsController;
    }

    //------------------//
    // getSymbolsEditor //
    //------------------//
    /**
     * Give access to the UI dealing with symbol recognition
     *
     * @return the symbols editor
     */
    public SymbolsEditor getSymbolsEditor ()
    {
        return editor;
    }

    //---------------//
    // getSystemById //
    //---------------//
    /**
     * Report the system info for which id is provided
     * @param id id of desired system
     * @return the desired system info
     */
    public SystemInfo getSystemById (int id)
    {
        return systems.get(id - 1);
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the system info that contains the provided point
     * @param point the provided pixel point
     * @return the containing system info
     * (or null if there is no enclosing system)
     */
    public SystemInfo getSystemOf (PixelPoint point)
    {
        for (SystemInfo info : getSystems()) {
            if (info.getBoundary()
                    .contains(point)) {
                return info;
            }
        }

        return null;
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the system, if any, which contains the provided glyph
     * (as determined by the first section of the glyph)
     * @param glyph the provided glyph
     * @return the containing system, or null
     */
    public SystemInfo getSystemOf (Glyph glyph)
    {
        ///return getSystemOf(glyph.getAreaCenter());
        return glyph.getMembers()
                    .first()
                    .getSystem();
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the system, if any, which contains the provided vertical section
     * @param section the provided section
     * @return the containing system, or null
     */
    public SystemInfo getSystemOf (GlyphSection section)
    {
        return section.getSystem();
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the system that contains ALL glyphs provided.
     * If all glyphs do not belong to the same system, exception is thrown
     * @param glyphs the collection of glyphs
     * @return the containing system
     * @exception IllegalArgumentException raised if glyphs collection is not OK
     */
    public SystemInfo getSystemOf (Collection<Glyph> glyphs)
    {
        if ((glyphs == null) || glyphs.isEmpty()) {
            throw new IllegalArgumentException(
                "getSystemOf. Glyphs collection is null or empty");
        }

        SystemInfo        system = null;
        Collection<Glyph> toRemove = new ArrayList<Glyph>();

        for (Glyph glyph : glyphs) {
            SystemInfo glyphSystem = glyph.isVirtual()
                                     ? getSystemOf(glyph.getAreaCenter())
                                     : getSystemOf(glyph);

            if (glyphSystem == null) {
                toRemove.add(glyph);
            } else {
                if (system == null) {
                    system = glyphSystem;
                } else {
                    // Make sure we are still in the same system
                    if (glyphSystem != system) {
                        throw new IllegalArgumentException(
                            "getSystemOf. Glyphs from different systems (" +
                            getSystemOf(glyph) + " and " + system + ") " +
                            Glyphs.toString(glyphs));
                    }
                }
            }
        }

        if (!toRemove.isEmpty()) {
            logger.warning("No system for " + Glyphs.toString(toRemove));
            glyphs.removeAll(toRemove);
        }

        return system;
    }

    //---------------------//
    // getSystemOfSections //
    //---------------------//
    /**
     * Report the system that contains ALL sections provided.
     * If all sections do not belong to the same system, exception is thrown
     * @param sections the collection of sections
     * @return the containing system
     * @exception IllegalArgumentException raised if section collection is not OK
     */
    public SystemInfo getSystemOfSections (Collection<GlyphSection> sections)
    {
        if ((sections == null) || sections.isEmpty()) {
            throw new IllegalArgumentException(
                "getSystemOfSections. Sections collection is null or empty");
        }

        SystemInfo               system = null;
        Collection<GlyphSection> toRemove = new ArrayList<GlyphSection>();

        for (GlyphSection section : sections) {
            SystemInfo sectionSystem = section.getSystem();

            if (sectionSystem == null) {
                toRemove.add(section);
            } else {
                if (system == null) {
                    system = sectionSystem;
                } else {
                    // Make sure we are still in the same system
                    if (sectionSystem != system) {
                        throw new IllegalArgumentException(
                            "getSystemOfSections. Sections from different systems (" +
                            section.getSystem() + " and " + system + ") " +
                            Sections.toString(sections));
                    }
                }
            }
        }

        if (!toRemove.isEmpty()) {
            logger.warning("No system for " + Sections.toString(toRemove));
            sections.removeAll(toRemove);
        }

        return system;
    }

    //------------//
    // getSystems //
    //------------//
    /**
     * Report the retrieved systems (infos)
     *
     * @return the list of SystemInfo's
     */
    public List<SystemInfo> getSystems ()
    {
        return systems;
    }

    //-------------------//
    // getSystemsBuilder //
    //-------------------//
    /**
     * Give access to the builder in charge of bars & systems computation
     *
     * @return the builder instance
     */
    public SystemsBuilder getSystemsBuilder ()
    {
        return systemsBuilder;
    }

    //----------------//
    // getSystemsNear //
    //----------------//
    /**
     * Report the ordered list of systems containing or close to the provided
     * point
     * @param point the provided point
     * @return a collection of systems ordered by increasing distance from the
     * provided point
     */
    public List<SystemInfo> getSystemsNear (final Point point)
    {
        List<SystemInfo> neighbors = new ArrayList<SystemInfo>(systems);
        Collections.sort(
            neighbors,
            new Comparator<SystemInfo>() {
                    public int compare (SystemInfo s1,
                                        SystemInfo s2)
                    {
                        int y1 = (s1.getTop() + s1.getBottom()) / 2;
                        int d1 = Math.abs(point.y - y1);
                        int y2 = (s2.getTop() + s2.getBottom()) / 2;
                        int d2 = Math.abs(point.y - y2);

                        return Integer.signum(d1 - d2);
                    }
                });

        return neighbors;
    }

    //----------------//
    // setVerticalLag //
    //----------------//
    /**
     * Assign the current vertical lag for the sheet
     *
     * @param vLag the current vertical lag
     * @return the previous vLag, or null
     */
    public GlyphLag setVerticalLag (GlyphLag vLag)
    {
        GlyphLag old = this.vLag;
        this.vLag = vLag;

        return old;
    }

    //----------------//
    // getVerticalLag //
    //----------------//
    /**
     * Report the current vertical lag of the sheet
     *
     * @return the current vertical lag
     */
    public GlyphLag getVerticalLag ()
    {
        return vLag;
    }

    //------------------------//
    // getVerticalsController //
    //------------------------//
    public VerticalsController getVerticalsController ()
    {
        return verticalsController;
    }

    //----------//
    // getWidth //
    //----------//
    /**
     * Report the picture width in pixels
     *
     * @return the picture width
     */
    public int getWidth ()
    {
        return width;
    }

    //----------//
    // addError //
    //----------//
    /**
     * Register an error in the sheet ErrorsWindow
     * @param container the immediate container for the error location
     * @param glyph the related glyph if any
     * @param text the error message
     */
    public void addError (SystemNode container,
                          Glyph      glyph,
                          String     text)
    {
        if (Main.getGui() != null) {
            getErrorsEditor()
                .addError(container, glyph, text);
        }
    }

    //----------//
    // colorize //
    //----------//
    /**
     * Set proper colors for sections of all recognized items so far, using the
     * provided color
     *
     * @param lag       the lag to be colorized
     * @param viewIndex the provided lag view index
     * @param color     the color to use
     */
    public void colorize (GlyphLag lag,
                          int      viewIndex,
                          Color    color)
    {
        if (score != null) {
            // Colorization of all known score items
            score.accept(new ScoreColorizer(lag, viewIndex, color));
        } else {
            // Nothing to colorize ? TODO
        }
    }

    //-------------------------//
    // computeSystemBoundaries //
    //-------------------------//
    /**
     * Compute the default boundary of the related area of each system
     */
    public void computeSystemBoundaries ()
    {
        // Compute the dimensions of the picture area of every system
        SystemInfo prevSystem = null;
        int        top = 0;
        BrokenLine north = new BrokenLine(
            new Point(0, top),
            new Point(getWidth(), top));
        BrokenLine south;

        for (SystemInfo system : getSystems()) {
            // Not the very first system?
            if (prevSystem != null) {
                // Top of system area, defined as middle ordinate between
                // ordinate of last line of last staff of previous system and
                // ordinate of first line of first staff of current system
                int bottom = (prevSystem.getBottom() + system.getTop()) / 2;
                south = new BrokenLine(
                    new Point(0, bottom),
                    new Point(getWidth(), bottom));
                prevSystem.setBoundary(
                    new SystemBoundary(prevSystem, north, south));
                north = south;
            }

            // Remember this info for next system
            prevSystem = system;
        }

        // Last system
        if (prevSystem != null) {
            south = new BrokenLine(
                new Point(0, getHeight()),
                new Point(getWidth(), getHeight()));
            prevSystem.setBoundary(
                new SystemBoundary(prevSystem, north, south));
        }
    }

    //----------------------------------//
    // createSymbolsControllerAndEditor //
    //----------------------------------//
    public void createSymbolsControllerAndEditor ()
    {
        SymbolsModel model = new SymbolsModel(this, getVerticalLag());
        symbolsController = new SymbolsController(model);

        if (Main.getGui() != null) {
            editor = new SymbolsEditor(this, symbolsController);
        }
    }

    //----------------------//
    // createSystemsBuilder //
    //----------------------//
    public void createSystemsBuilder ()
    {
        page.resetSystems();
        systemsBuilder = new SystemsBuilder(this);
    }

    //---------------------------//
    // createVerticalsController //
    //---------------------------//
    public void createVerticalsController ()
    {
        verticalsController = new VerticalsController(this);
    }

    //------//
    // done //
    //------//
    /**
     * Remember that the provided step has been done on the sheet
     * @param step the provided step
     */
    public final void done (Step step)
    {
        doneSteps.add(step);
    }

    //-----------------//
    // dumpSystemInfos //
    //-----------------//
    /**
     * Utility method, to dump all sheet systems
     */
    public void dumpSystemInfos ()
    {
        System.out.println("--- SystemInfos ---");

        int i = 0;

        for (SystemInfo system : getSystems()) {
            Main.dumping.dump(system, "#" + i++);
        }

        System.out.println("--- SystemInfos end ---");
    }

    //---------------//
    // hasHistoRatio //
    //---------------//
    /**
     * Check whether the parameter histoRatio has a value
     * @return true if so
     */
    public boolean hasHistoRatio ()
    {
        return histoRatio != null;
    }

    //------------------//
    // hasMaxForeground //
    //------------------//
    public boolean hasMaxForeground ()
    {
        return maxForeground != null;
    }

    //--------//
    // remove //
    //--------//
    /**
     * Remove this sheet from the containing score
     */
    public void remove ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Closing " + this);
        }

        // Close the related page
        getScore()
            .remove(page);

        // Close related UI assembly if any
        if (assembly != null) {
            assembly.close();
            SheetsController.getInstance()
                            .removeAssembly(this);
        }

        if (picture != null) {
            picture.close();
        }

        // If no sheet is left, close the score
        if (score.getPages()
                 .isEmpty()) {
            score.close();
        }
    }

    //----------------//
    // splitBarSticks //
    //----------------//
    /**
     * Split the bar sticks among systems
     *
     * @param barSticks the collection of all bar sticks
     * @return the set of modified systems
     */
    public Set<SystemInfo> splitBarSticks (Collection<?extends Glyph> barSticks)
    {
        Set<SystemInfo>                   modified = new LinkedHashSet<SystemInfo>();
        Map<SystemInfo, SortedSet<Glyph>> glyphs = new HashMap<SystemInfo, SortedSet<Glyph>>();

        for (SystemInfo system : systems) {
            glyphs.put(
                system,
                new ConcurrentSkipListSet<Glyph>(system.getGlyphs()));
            system.clearGlyphs();
        }

        // Assign the bar sticks to the proper system glyphs collection
        for (Glyph stick : barSticks) {
            if (stick.isActive()) {
                SystemInfo system = getSystemOf(stick);

                if (system != null) {
                    system.addGlyph(stick);
                }
            }
        }

        for (SystemInfo system : systems) {
            if (!(system.getGlyphs().equals(glyphs.get(system)))) {
                modified.add(system);
            }
        }

        return modified;
    }

    //------------------//
    // splitHorizontals //
    //------------------//
    /**
     * Split the various horizontals among systems
     *
     * @return the set of modified systems
     */
    public Set<SystemInfo> splitHorizontals ()
    {
        Set<SystemInfo>               modified = new LinkedHashSet<SystemInfo>();
        Map<SystemInfo, List<Ledger>> ledgers = new HashMap<SystemInfo, List<Ledger>>();
        Map<SystemInfo, List<Ending>> endings = new HashMap<SystemInfo, List<Ending>>();

        for (SystemInfo system : systems) {
            ledgers.put(system, new ArrayList<Ledger>(system.getLedgers()));
            system.getLedgers()
                  .clear();
            endings.put(system, new ArrayList<Ending>(system.getEndings()));
            system.getEndings()
                  .clear();
        }

        for (Ledger ledger : getHorizontals()
                                 .getLedgers()) {
            GlyphSection section = ledger.getStick()
                                         .getFirstSection();
            SystemInfo   system = getSystemOf(
                section.getGraph().switchRef(section.getCentroid(), null));

            if (system != null) {
                system.getLedgers()
                      .add(ledger);
            }
        }

        for (Ending ending : getHorizontals()
                                 .getEndings()) {
            GlyphSection section = ending.getStick()
                                         .getFirstSection();
            SystemInfo   system = getSystemOf(
                section.getGraph().switchRef(section.getCentroid(), null));

            if (system != null) {
                system.getEndings()
                      .add(ending);
            }
        }

        for (SystemInfo system : systems) {
            if (!(system.getLedgers().equals(ledgers.get(system)))) {
                modified.add(system);
            }

            if (!(system.getEndings().equals(endings.get(system)))) {
                modified.add(system);
            }
        }

        return modified;
    }

    //-----------------------//
    // splitVerticalSections //
    //-----------------------//
    /**
     * Split the various horizontal sections (Used by Glyphs).
     *
     * @return the set of modified systems
     */
    public Set<SystemInfo> splitVerticalSections ()
    {
        Set<SystemInfo>                           modified = new LinkedHashSet<SystemInfo>();
        Map<SystemInfo, Collection<GlyphSection>> sections = new HashMap<SystemInfo, Collection<GlyphSection>>();

        for (SystemInfo system : systems) {
            Collection<GlyphSection> systemSections = system.getMutableVerticalSections();
            sections.put(system, new ArrayList<GlyphSection>(systemSections));
            systemSections.clear();
        }

        for (GlyphSection section : getVerticalLag()
                                        .getSections()) {
            SystemInfo system = getSystemOf(
                section.getGraph().switchRef(section.getCentroid(), null));
            // Link section -> system
            section.setSystem(system);

            if (system != null) {
                // Link system <>-> section
                system.getMutableVerticalSections()
                      .add(section);
            }
        }

        for (SystemInfo system : systems) {
            if (!(system.getMutableVerticalSections().equals(
                sections.get(system)))) {
                modified.add(system);
            }
        }

        return modified;
    }

    //----------//
    // toString //
    //----------//
    /**
     * Report a simple readable identification of this sheet
     *
     * @return a string based on the related image file name
     */
    @Override
    public String toString ()
    {
        return "{Sheet " + page.getId() + "}";
    }

    //------------//
    // setPicture //
    //------------//
    /**
     * Set the picture of this sheet, that is the image to be processed.
     *
     * @param picture the related picture
     */
    private final void setPicture (Picture picture)
    {
        this.picture = picture;

        // Attach proper Selection objects
        // (reading from pixel location & writing to gray level)
        picture.setLevelService(selectionService);
        selectionService.subscribe(SheetLocationEvent.class, picture);

        // Display sheet picture if not batch mode
        if (Main.getGui() != null) {
            PictureView pictureView = new PictureView(Sheet.this);
            assembly.addViewTab(
                Step.PICTURE_TAB,
                pictureView,
                new BoardsPane(
                    Sheet.this,
                    pictureView.getView(),
                    new PixelBoard(page.getIndex() + ":Picture", Sheet.this)));
        }
    }

    //~ Inner Classes ----------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
        extends ConstantSet
    {
        //~ Instance fields ----------------------------------------------------

        Constant.Integer maxForegroundGrayLevel = new Constant.Integer(
            "ByteLevel",
            192,
            "Maximum gray level for a pixel to be considered as foreground (black)");

        /** Ratio of horizontal histogram to detect staves */
        Constant.Ratio defaultStaffThreshold = new Constant.Ratio(
            0.5d,
            "Ratio of horizontal histogram to detect staves");
    }
}
