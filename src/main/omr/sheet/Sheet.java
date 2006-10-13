//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h e e t                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2006. All rights reserved.               //
//  This software is released under the terms of the GNU General Public       //
//  License. Please contact the author at herve.bitteur@laposte.net           //
//  to report bugs & suggestions.                                             //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;
import omr.ProcessingException;
import omr.Step;

import omr.glyph.Glyph;
import omr.glyph.GlyphBuilder;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.ui.SymbolsEditor;

import omr.score.Score;
import omr.score.ScoreBuilder;
import omr.score.ScoreManager;
import omr.score.visitor.CheckingVisitor;
import omr.score.visitor.ColorizingVisitor;
import omr.score.visitor.RenderingVisitor;
import omr.score.visitor.Visitable;
import omr.score.visitor.Visitor;

import omr.selection.Selection;
import omr.selection.SelectionManager;
import omr.selection.SelectionTag;
import static omr.selection.SelectionTag.*;

import omr.stick.Stick;

import omr.ui.BoardsPane;
import omr.ui.Jui;
import omr.ui.PixelBoard;
import omr.ui.SheetAssembly;

import omr.util.FileUtil;
import omr.util.Logger;

import java.awt.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;

/**
 * Class <code>Sheet</code> encapsulates the original music image, as well as
 * pointers to all processings related to this image.
 *
 * <p/>Here is the ordered list of the various steps that sheet processing can
 * go through :
 *
 * <p/> <ol>
 *
 * <li> {@link #LOAD} loads the image for the sheet, from a provided image
 * file. </li>
 *
 * <li> {@link #SCALE} determines the general scale of the sheet, based on the
 * mean distance between staff lines. </li>
 *
 * <li> {@link #SKEW} determines the average skew of the picture, and deskews it
 * if needed. </li>
 *
 * <li> {@link #LINES} retrieves the staff lines, erases their pixels and
 * creates crossing objects when needed. Pixels modifications are made in the
 * original (unblurred) image. </li>
 *
 * <li> {@link #HORIZONTALS} retrieves the horizontal dashes. </li>
 *
 * <li> {@link #BARS} retrieves the vertical bar lines, and so the systems and
 * measures. </li>
 *
 * <li> {@link #SYMBOLS} recognizes isolated symbols glyphs. </li>
 *
 * <li> {@link #SYMBOLS_COMPOUNDS} aggregates unknown symbols into compound
 * glyphs. </li>
 *
 * <li> {@link #VERTICALS} retrieves the vertical items such as stems. </li>
 *
 * <li> {@link #LEAVES} processes leaves, which are glyphs attached to
 * stems. </li>
 *
 * <li> {@link #LEAVES_COMPOUNDS} aggregates unknown leaves into compound
 * glyphs. </li>
 *
 * <li> {@link #CLEANUP} is a final cleanup step. </li>
 *
 * <li> {@link #SCORE} is the score recognition step. </li>
 *
 * </ol>
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Sheet
    implements java.io.Serializable, Visitable
{
    //~ Static fields/initializers ---------------------------------------------

    private static final Logger                logger = Logger.getLogger(
        Sheet.class);

    /** List of steps */
    private static List<Step> steps;

    //~ Instance fields --------------------------------------------------------

    // First: non-transient members

    /** Link with sheet original image file. Set by constructor. */
    private File imageFile;

    /** Vertical lag (built by BARS/BarsBuilder) */
    private GlyphLag vLag;

    /** Sheet height in pixels */
    private int height = -1;

    /** Sheet width in pixels */
    private int width = -1;

    /** Retrieved systems. Set by BARS. */
    private List<SystemInfo> systems;

    /** Link with related score. Set by BARS. */
    private Score score;

    /** Glyph id of the first symbol */
    private int firstSymbolId = -1;

    // Below: transient members

    /** A bar line extractor for this sheet */
    private transient BarsBuilder barsBuilder;

    /** A glyph extractor for this sheet */
    private transient GlyphBuilder glyphBuilder;

    /** A glyph inspector for this sheet */
    private transient GlyphInspector glyphInspector;

    /** Horizontal lag (built by LINES/LinesBuilder) */
    private transient GlyphLag hLag;

    /** A staff line extractor for this sheet */
    private transient LinesBuilder linesBuilder;

    /** All Current selections for this sheet */
    private transient SelectionManager selectionManager;

    /** Related assembly instance */
    private transient SheetAssembly assembly;

    /** Dedicated skew builder */
    private transient SkewBuilder skewBuilder;

    /** Specific pane dealing with glyphs */
    private transient SymbolsEditor symbolsEditor;

    /** To avoid concurrent modifications */
    private transient volatile boolean busy = false;

    // InstanceStep Definitions (in proper order) ------------------------------

    /** Step to initially load a sheet picture */
    public transient LoadStep LOAD;

    /**
     * Step to determine the main scale of the sheet. The scale is the mean
     * distance, in pixels, between two consecutive staff lines. This is based
     * on the population of vertical runs, since most frequent foreground runs
     * come from staff lines, and most frequent background runs come from inter
     * staff lines.
     */
    public final InstanceStep<Scale> SCALE = new InstanceStep<Scale>(
        "Compute the main Scale of the sheet") {
        public void doit ()
            throws ProcessingException
        {
            result = new Scale(Sheet.this);

            // Check we've got something usable
            int fore = getScale()
                           .mainFore();

            if (fore == 0) {
                logger.warning("Invalid scale mainFore value : " + fore);
                throw new ProcessingException();
            }
        }
    };

    /**
     * Step to determine the general slope of the sheet, still based on
     * pseudo-horizontal (staff) lines. If the absolute value of the computed
     * slope is above a maximum threshold, then the image as a whole is
     * "deskewed", since this significantly eases the subsequent processing.
     * The resulting (deskewed) image is stored on disk, and reloaded in place
     * of the original (skewed) image. From this step on, we'll play only with
     * the deskewed image.
     */
    public final InstanceStep<Skew> SKEW = new InstanceStep<Skew>(
        "Compute the global Skew, and rotate if needed") {
        public void doit ()
            throws ProcessingException
        {
            skewBuilder = new SkewBuilder(Sheet.this);
            result = skewBuilder.buildInfo();

            // Update displayed image if any
            if (getPicture()
                    .isRotated() && (Main.getJui() != null)) {
                assembly.getComponent()
                        .repaint();
            }

            // Remember final sheet dimensions in pixels
            width = getPicture()
                        .getWidth();
            height = getPicture()
                         .getHeight();
        }
    };

    /**
     * Step to retrieve all staff lines, and remove them from the picture
     */
    public final InstanceStep<List<StaffInfo>> LINES = new InstanceStep<List<StaffInfo>>(
        "Detect & remove all Staff Lines") {
        public void doit ()
            throws ProcessingException
        {
            linesBuilder = new LinesBuilder(Sheet.this);
            result = linesBuilder.getStaves();
        }
    };

    /**
     * Step to retrieve all horizontal dashes
     */
    public final InstanceStep<Horizontals> HORIZONTALS = new InstanceStep<Horizontals>(
        "Retrieve horizontal Dashes") {
        public void doit ()
            throws ProcessingException
        {
            HorizontalsBuilder builder = new HorizontalsBuilder(Sheet.this);
            result = builder.buildInfo();
        }
    };

    /**
     * Step to retrieve all bar lines. This allocates and links the sheet
     * related score.
     */
    public final InstanceStep<Boolean> BARS = new InstanceStep<Boolean>(
        "Detect vertical Bar lines") {
        public void doit ()
            throws ProcessingException
        {
            barsBuilder = new BarsBuilder(Sheet.this);
            barsBuilder.buildInfo();
            result = Boolean.valueOf(true);

            // Force score view creation if UI is present
            if (Main.getJui() != null) {
                Main.getJui().scoreController.setScoreView(score);
            }
        }

        //        public void displayUI ()
        //        {
        //            Main.getJui().scoreController.setScoreView(score);
        //        }
    };

    /**
     * Step to process all glyphs, built with connected sections from the
     * current collection of non-recognized sections.
     */
    public final InstanceStep<Boolean> SYMBOLS = new InstanceStep<Boolean>(
        "Recognize Symbols") {
        public void doit ()
            throws ProcessingException
        {
            // We need the glyphs that result from extraction
            if (firstSymbolId == -1) {
                firstSymbolId = getGlyphBuilder()
                                    .buildInfo();
            }

            result = Boolean.valueOf(true);

            // Accept consistent votes
            GlyphInspector inspector = getGlyphInspector();
            inspector.evaluateGlyphs(inspector.getSymbolMaxGrade());
        }

        public void displayUI ()
        {
            getSymbolsEditor()
                .refresh();
        }
    };

    /**
     * Step to aggregate unknown symbols to larger glyphs, named compounds, if
     * this leads to recognized compound symbols.
     */
    public final InstanceStep<Boolean> SYMBOLS_COMPOUNDS = new InstanceStep<Boolean>(
        "Aggregate symbol Compounds") {
        public void doit ()
            throws ProcessingException
        {
            SYMBOLS.getResult();

            GlyphInspector inspector = getGlyphInspector();
            inspector.processCompounds(inspector.getSymbolMaxGrade());
            result = Boolean.valueOf(true);
            inspector.evaluateGlyphs(inspector.getSymbolMaxGrade());
        }

        public void displayUI ()
        {
            getSymbolsEditor()
                .refresh();
        }
    };

    /**
     * Step to extract vertical stick as Stems (or vertical Endings), and
     * recognize newly segmented leaves, since sections belonging to stems are
     * properly assigned.
     */
    public final InstanceStep<Boolean> VERTICALS = new InstanceStep<Boolean>(
        "Extract verticals") {
        public void doit ()
            throws ProcessingException
        {
            SYMBOLS_COMPOUNDS.getResult();

            getGlyphInspector()
                .processVerticals();
            result = Boolean.valueOf(true);
        }

        public void displayUI ()
        {
            getSymbolsEditor()
                .refresh();
        }
    };

    /**
     * Step to extract newly segmented leaves, since sections belonging to stems
     * are properly assigned.
     */
    public final InstanceStep<Boolean> LEAVES = new InstanceStep<Boolean>(
        "Extract Leaves attached to stems") {
        public void doit ()
            throws ProcessingException
        {
            VERTICALS.getResult();

            getGlyphInspector()
                .processLeaves();
            result = Boolean.valueOf(true);
            getGlyphInspector()
                .evaluateGlyphs(GlyphInspector.getLeafMaxGrade());
        }

        public void displayUI ()
        {
            getSymbolsEditor()
                .refresh();
        }
    };

    /**
     * Step to aggregate unknown leaves to larger compound glyphs, if this leads
     * to recognized compound leaves.
     */
    public final InstanceStep<Boolean> LEAVES_COMPOUNDS = new InstanceStep<Boolean>(
        "Aggregate symbol Compounds") {
        public void doit ()
            throws ProcessingException
        {
            LEAVES.getResult();

            getGlyphInspector()
                .processCompounds(GlyphInspector.getLeafMaxGrade());
            result = Boolean.valueOf(true);
            getGlyphInspector()
                .evaluateGlyphs(GlyphInspector.getLeafMaxGrade());
        }

        public void displayUI ()
        {
            getSymbolsEditor()
                .refresh();
        }
    };

    /**
     * Step to clean up undue constructions, such as wrong stems..
     */
    public final InstanceStep<Boolean> CLEANUP = new InstanceStep<Boolean>(
        "Cleanup undue stems") {
        public void doit ()
            throws ProcessingException
        {
            LEAVES_COMPOUNDS.getResult();

            getGlyphInspector()
                .processUndueStems();
            result = Boolean.valueOf(true);
            getGlyphInspector()
                .evaluateGlyphs(GlyphInspector.getCleanupMaxGrade());
        }

        public void displayUI ()
        {
            getSymbolsEditor()
                .refresh();
        }
    };

    /**
     * Step to translate recognized glyphs into score items
     */
    public final InstanceStep<Boolean> SCORE = new InstanceStep<Boolean>(
        "Translate glyphs to score items") {
        public void doit ()
            throws ProcessingException
        {
            CLEANUP.getResult();

            // Perform the glyphs translation
            ScoreBuilder builder = new ScoreBuilder(score, Sheet.this);
            builder.buildInfo();

            // Perform global checks recursively
            score.accept(new CheckingVisitor());
        }

        public void displayUI ()
        {
            getSymbolsEditor()
                .refresh();
        }
    };


    //~ Constructors -----------------------------------------------------------

    //-------//
    // Sheet //
    //-------//
    /**
     * Create a new <code>Sheet</code> instance, based on a given image file.
     * Several files extensions are supported, including the most common ones.
     *
     * @param imageFile a <code>File</code> value to specify the image file.
     * @param force should we keep the sheet structure even if the image cannot
     *                be loaded for whatever reason
     * @throws ProcessingException raised if, while 'force' is false, image file
     *                  cannot be loaded
     */
    public Sheet (File    imageFile,
                  boolean force)
        throws ProcessingException
    {
        this();

        if (logger.isFineEnabled()) {
            logger.fine("creating Sheet form image " + imageFile);
        }

        try {
            // We make sure we have a canonical form for the file name
            this.imageFile = imageFile.getCanonicalFile();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        // Load this image picture
        try {
            LOAD.doit();
        } catch (ProcessingException ex) {
            if (!force) {
                throw ex;
            }
        }

        // Insert in sheet history
        SheetManager.getInstance()
                    .getHistory()
                    .add(getPath());

        // Insert in list of handled sheets
        SheetManager.getInstance()
                    .insertInstance(this);

        // Try to update links with score side
        ScoreManager.getInstance()
                    .linkAllScores();

        // Update UI information if so needed
        displayAssembly();
    }

    //-------//
    // Sheet //
    //-------//
    /**
     * Create a sheet as a score companion
     *
     * @param score the existing score
     */
    public Sheet (Score score)
        throws ProcessingException
    {
        this(new File(score.getImagePath()), /* force => */
             true);

        if (logger.isFineEnabled()) {
            logger.fine("Created Sheet from " + score);
        }
    }

    //-------//
    // Sheet //
    //-------//
    /**
     * Meant for local (and XML binder ?) use only
     */
    private Sheet ()
    {
        checkTransientSteps();
    }

    //~ Methods ----------------------------------------------------------------

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
        if (assembly == null) {
            setAssembly(new SheetAssembly(this));
        }

        return assembly;
    }

    //----------------//
    // getBarsBuilder //
    //----------------//
    /**
     * Give access to the builder in charge of bars computation
     *
     * @return the builder instance
     */
    public BarsBuilder getBarsBuilder ()
    {
        return barsBuilder;
    }

    //---------//
    // setBusy //
    //---------//
    /**
     * Flag the sheet processing state
     *
     * @param busy true if busy
     */
    public synchronized void setBusy (boolean busy)
    {
        this.busy = busy;
    }

    //--------//
    // isBusy //
    //--------//
    /**
     * Check whether the sheet is being processed
     *
     * @return true if busy
     */
    public synchronized boolean isBusy ()
    {
        return busy;
    }

    //------------------//
    // getClosestSystem //
    //------------------//
    /**
     * Report the closest system (apart from the provided one) in the direction
     * of provided ordinate
     *
     * @param system the current system
     * @param y the ordinate (of a point, a glyph, ...)
     * @return the next (or previous) system if any
     */
    public SystemInfo getClosestSystem (SystemInfo system,
                                        int        y)
    {
        int index = systems.indexOf(system);
        int middle = (system.getAreaTop() + system.getAreaBottom()) / 2;

        if (y > middle) {
            if (index < (systems.size() - 1)) {
                return systems.get(index + 1);
            }
        } else {
            if (index > 0) {
                return systems.get(index - 1);
            }
        }

        return null;
    }

    //-----------------//
    // getGlyphBuilder //
    //-----------------//
    /**
     * Give access to the glyph builder for this sheet
     *
     * @return the builder instance
     */
    public GlyphBuilder getGlyphBuilder ()
    {
        if (glyphBuilder == null) {
            glyphBuilder = new GlyphBuilder(this);
        }

        return glyphBuilder;
    }

    //-------------------//
    // getGlyphInspector //
    //-------------------//
    /**
     * Give access to the glyph inspector (in charge of all glyph recognition
     * actions) for this sheet
     *
     * @return the inspector instance
     */
    public GlyphInspector getGlyphInspector ()
    {
        if (glyphInspector == null) {
            glyphInspector = new GlyphInspector(this, getGlyphBuilder());
        }

        return glyphInspector;
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

        // Input
        getSelectionManager()
            .addObserver(
            hLag,
            PIXEL,
            HORIZONTAL_SECTION,
            HORIZONTAL_SECTION_ID,
            HORIZONTAL_GLYPH,
            HORIZONTAL_GLYPH_ID);

        // Output
        hLag.setLocationSelection(getSelection(PIXEL));
        hLag.setRunSelection(getSelection(HORIZONTAL_RUN));
        hLag.setSectionSelection(getSelection(HORIZONTAL_SECTION));
        hLag.setGlyphSelection(getSelection(HORIZONTAL_GLYPH));
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
        if (hLag == null) {
            try {
                // Brought by LinesBuilder, so...
                LINES.doit();
            } catch (ProcessingException ex) {
                logger.severe("Cannot retrieve HorizontalLag from LINES");
            }
        }

        return hLag;
    }

    //--------------//
    // getImageFile //
    //--------------//
    /**
     * Report the file used to load the image from.
     *
     * @return the File entity
     */
    public File getImageFile ()
    {
        return imageFile;
    }

    //-----------------//
    // getInstanceStep //
    //-----------------//
    /**
     * Report the sheet element [InstanceStep] linked to the given step
     *
     * @param step the driving step
     *
     * @return the InstanceStep that contains the result of the step on the
     *         sheet
     */
    public InstanceStep getInstanceStep (Step step)
    {
        try {
            return (InstanceStep) step.getField()
                                      .get(this);
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();

            return null;
        }
    }

    //----------------//
    // getLinesBuilder //
    //----------------//
    /**
     * Give access to the builder in charge of staff lines
     *
     * @return the builder instance
     */
    public LinesBuilder getLinesBuilder ()
    {
        return linesBuilder;
    }

    //-------------//
    // isOnSymbols //
    //-------------//
    /**
     * Check whether current step is SYMBOLS or SYMBOL_COMPOUND
     *
     * @return true if on SYMBOLS
     */
    public boolean isOnSymbols ()
    {
        InstanceStep iStep = getInstanceStep(currentStep());

        return (iStep == SYMBOLS) || (iStep == SYMBOLS_COMPOUNDS);
    }

    //     //--------------//
    //     // requestScore //
    //     //--------------//
    //     /**
    //      * Make sure to report the Score that gathers in a score the information
    //      * retrieved from this sheet.
    //      *
    //      * @return the related score
    //      */
    //     public Score requestScore ()
    //         throws ProcessingException
    //     {
    //         if (score == null) {
    //             try {
    //                 // Brought by BARS/BarsBuilder, so...
    //                 BARS.doit();
    //             } catch (ProcessingException ex) {
    //                 logger.error("Cannot retrieve Score from BARS");
    //                 throw new ProcessingException();
    //             }
    //         }

    //         return score;
    //     }

    //---------//
    // getPath //
    //---------//
    /**
     * Report the (canonical) expression of the image file name, to uniquely and
     * unambiguously identify this sheet.
     *
     * @return the normalized image file path
     */
    public String getPath ()
    {
        return imageFile.getPath();
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
        try {
            return LOAD.getResult();
        } catch (ProcessingException ex) {
            logger.severe("Picture not available");

            return null;
        }
    }

    //----------//
    // getRadix //
    //----------//
    /**
     * Report a short name for this sheet (no path, no extension). Useful for
     * tab labels for example.
     *
     * @return just the name of the image file
     */
    public String getRadix ()
    {
        return FileUtil.getNameSansExtension(imageFile);
    }

    //----------//
    // setScale //
    //----------//
    /**
     * Link scale information to this sheet
     *
     * @param scale the computed (or read from score file) scale
     */
    public void setScale (Scale scale)
    {
        SCALE.result = scale;
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
        try {
            return SCALE.getResult();
        } catch (ProcessingException ex) {
            logger.severe("Scale not available");

            return null;
        }
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

    //--------------//
    // getSelection //
    //--------------//
    /**
     * Report, within this sheet, the Selection related to the provided Tag
     *
     * @param tag specific selection (such as PIXEL, GLYPH, etc)
     * @return the selection object, that can be observed
     */
    public Selection getSelection (SelectionTag tag)
    {
        return getSelectionManager()
                   .getSelection(tag);
    }

    //---------------------//
    // getSelectionManager //
    //---------------------//
    /**
     * Report, the selection manager assigned to this sheet.
     * @return the selection manager
     */
    public SelectionManager getSelectionManager ()
    {
        if (selectionManager == null) {
            selectionManager = new SelectionManager(this);
        }

        return selectionManager;
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
        SKEW.result = skew;
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
        try {
            return SKEW.getResult();
        } catch (ProcessingException ex) {
            logger.severe("Skew not available");

            return null;
        }
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

    //----------//
    // getSteps //
    //----------//
    /**
     * Report the ordered list of steps defined in the Sheet class
     *
     * @return the comprehensive list of Steps
     */
    public static List<Step> getSteps ()
    {
        if (steps == null) {
            try {
                steps = new ArrayList<Step>();

                // Kludge to retrieve the related descriptions
                Sheet   sheet = new Sheet();

                Class   sheetClass = Sheet.class;
                Field[] fields = sheetClass.getDeclaredFields();

                for (int i = 0; i < fields.length; i++) {
                    Field field = fields[i];

                    if (InstanceStep.class.isAssignableFrom(field.getType())) {
                        InstanceStep is = (InstanceStep) field.get(sheet);
                        steps.add(new Step(field, is.getDescription()));
                    }
                }
            } catch (IllegalAccessException ex) {
                ex.printStackTrace(); // Should not happen
            }
        }

        return steps;
    }

    //------------------//
    // getFirstSymbolId //
    //------------------//
    /**
     * Report the id of the first symbol glyph
     *
     * @return the first symbol glyph id
     */
    public int getFirstSymbolId ()
    {
        return firstSymbolId;
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
        try {
            return HORIZONTALS.getResult();
        } catch (ProcessingException ex) {
            logger.severe("Horizontals not processed");

            return null;
        }
    }

    //----------//
    // setScore //
    //----------//
    /**
     * Link the score panel with the related score entity
     *
     * @param score the related score
     */
    public void setScore (Score score)
    {
        // If there was already a linked score, clean up everything
        if (this.score != null) {
            if (logger.isFineEnabled()) {
                logger.fine("Deconnecting " + this.score);
            }

            this.score.close();
        }

        this.score = score;
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
            new Integer(y),
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
    // getStaves //
    //-----------//
    /**
     * Report the list of staves found in the sheet
     *
     * @return the collection of staves found
     */
    public List<StaffInfo> getStaves ()
    {
        try {
            return LINES.getResult();
        } catch (ProcessingException ex) {
            logger.severe("Staves not available");

            return null;
        }
    }

    //------------------//
    // getSymbolsEditor //
    //------------------//
    /**
     * Give access to the module dealing with symbol recognition
     *
     * @return the instance of glyph pane
     */
    public SymbolsEditor getSymbolsEditor ()
    {
        if (symbolsEditor == null) {
            symbolsEditor = new SymbolsEditor(this);
        }

        return symbolsEditor;
    }

    //--------------//
    // getSystemAtY //
    //--------------//
    /**
     * Find out the proper system info, for a given ordinate, according to the
     * split in system areas
     *
     * @param y the point ordinate
     *
     * @return the containing system,
     */
    public SystemInfo getSystemAtY (int y)
    {
        for (SystemInfo info : getSystems()) {
            if (y <= info.getAreaBottom()) {
                return info;
            }
        }

        // Should not happen
        logger.severe("getSystemAtY y=" + y + " not in  any system");

        return null;
    }

    //------------//
    // setSystems //
    //------------//
    /**
     * Assign the retrieved systems (infos)
     *
     * @param systems the elaborated list of SystemInfo's
     */
    public void setSystems (List<SystemInfo> systems)
    {
        this.systems = systems;
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
        if (systems == null) {
            try {
                BARS.doit();
            } catch (ProcessingException ex) {
                logger.severe("Bars systems not available");

                return null;
            }
        }

        return systems;
    }

    //--------------//
    // getSystemsAt //
    //--------------//
    /**
     * Report the collection of systems that intersect a given rectangle
     *
     * @param rect the rectangle of interest
     * @return the collection of systems, maybe empty but not null
     */
    public List<SystemInfo> getSystemsAt (Rectangle rect)
    {
        List<SystemInfo> list = new ArrayList<SystemInfo>();

        if (rect != null) {
            for (SystemInfo info : getSystems()) {
                if ((rect.y <= info.getAreaBottom()) &&
                    ((rect.y + rect.height) >= info.getAreaTop())) {
                    list.add(info);
                }
            }
        }

        return list;
    }

    //----------------//
    // setVerticalLag //
    //----------------//
    /**
     * Assign the current vertical lag for the sheet
     *
     * @param vLag the current vertical lag
     */
    public void setVerticalLag (GlyphLag vLag)
    {
        this.vLag = vLag;

        // Input
        getSelectionManager()
            .addObserver(
            vLag,
            PIXEL,
            VERTICAL_SECTION,
            VERTICAL_SECTION_ID,
            VERTICAL_GLYPH,
            VERTICAL_GLYPH_ID);

        // Output
        vLag.setLocationSelection(getSelection(PIXEL));
        vLag.setRunSelection(getSelection(VERTICAL_RUN));
        vLag.setSectionSelection(getSelection(VERTICAL_SECTION));
        vLag.setGlyphSelection(getSelection(VERTICAL_GLYPH));
        vLag.setGlyphSetSelection(getSelection(GLYPH_SET));
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
        if (vLag == null) {
            try {
                BARS.doit();
            } catch (ProcessingException ex) {
                logger.severe("Cannot retrieve vLag from BARS");
            }
        }

        return vLag;
    }

    //--------------//
    // getVerticals //
    //--------------//
    /**
     * Retrieve verticals system by system
     */
    public Boolean getVerticals ()
    {
        try {
            return VERTICALS.getResult();
        } catch (ProcessingException ex) {
            logger.severe("Verticals not available");

            return null;
        }
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

    // Temporary kludge
    public boolean BarsAreDone ()
    {
        return BARS.isDone();
    }

    // Temporary kludge
    public boolean HorizontalsAreDone ()
    {
        return HORIZONTALS.isDone();
    }

    // Temporary kludge
    public boolean LinesAreDone ()
    {
        return LINES.isDone();
    }

    public boolean accept (Visitor visitor)
    {
        if (visitor instanceof RenderingVisitor) {
            ((RenderingVisitor) visitor).visit(this);
        }

        return true;
    }

    //-------------------//
    // checkScaleAndSkew //
    //-------------------//
    /**
     * Given a sheet, and its related score, this method checks if scale and
     * skew information are available from the sheet. Otherwise, these infos are
     * copied from the score instance to the sheet instance. This is useful when
     * playing with a score and a sheet, wihout launching the costly processing
     * of the sheet.
     *
     * @param score the related score instance
     */
    public void checkScaleAndSkew (Score score)
    {
        // Make sure that scale and skew info is available for the sheet
        if (!SCALE.isDone()) {
            setScale(new Scale(score.getSpacing()));
        }

        if (!SKEW.isDone()) {
            setSkew(new Skew(score.getSkewAngleDouble()));
        }
    }

    //---------------------//
    // checkTransientSteps //
    //---------------------//
    /**
     * Some transient steps (LOAD) have to be allocated after deserialization of
     * sheet backup
     */
    public void checkTransientSteps ()
    {
        if (LOAD == null) {
            LOAD = new LoadStep();
        }
    }

    //-------//
    // close //
    //-------//
    /**
     * Close this sheet, as well as its assembly if any.
     */
    public void close ()
    {
        SheetManager.getInstance()
                    .close(this);

        if (LOAD.isDone()) {
            getPicture()
                .close();
        }

        if (score != null) {
            score.setSheet(null);
        }

        // Close related assembly if any
        if (assembly != null) {
            assembly.close();
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
            score.accept(new ColorizingVisitor(lag, viewIndex, color));
        } else {
            // Nothing to colorize ? TBD
        }
    }

    //-------------//
    // currentStep //
    //-------------//
    /**
     * Report what was the last step performed on the sheet
     *
     * @return the last step on this sheet
     */
    public Step currentStep ()
    {
        // Reverse loop on step list
        for (ListIterator<Step> it = getSteps()
                                         .listIterator(getSteps().size() - 1);
             it.hasPrevious();) {
            Step         step = it.previous();
            InstanceStep iStep = getInstanceStep(step);

            if ((iStep != null) && iStep.isDone()) {
                return step;
            }
        }

        return null;
    }

    //-----------------//
    // displayAssembly //
    //-----------------//
    /**
     * Display the related sheet view in the tabbed pane
     */
    public void displayAssembly ()
    {
        Jui jui = Main.getJui();

        if (jui != null) {
            // Prepare a assembly on this sheet, this uses the initial zoom
            // ratio
            int viewIndex = jui.sheetController.setSheetAssembly(this);

            // if this is the current target, then show this sheet immediately
            //////if (jui.isTarget(getPath())) {
            jui.sheetController.showSheetView(viewIndex, true);

            /////}
        }
    }

    //----------//
    // fromStep //
    //----------//
    /**
     * Determine the needed starting step to get to the target step
     *
     * @param step the target step
     *
     * @return the first step not done before the target step, or the target
     *         step if there is no un-done step before it
     */
    public Step fromStep (Step step)
    {
        for (Step s : getSteps()) {
            if (s == step) {
                return step;
            }

            if (!getInstanceStep(s)
                     .isDone()) {
                return s;
            }
        }

        return null; // As a last resort, should never be reached
    }

    //-------------//
    // lookupGlyph //
    //-------------//
    /**
     * Look up for a glyph, knowing its coordinates
     *
     * @param source the coordinates of the point
     *
     * @return the found glyph, or null
     */
    public Glyph lookupGlyph (Point source)
    {
        Glyph      glyph = null;
        SystemInfo system = getSystemAtY(source.y);

        if (system != null) {
            glyph = lookupSystemGlyph(system, source);

            if (glyph != null) {
                return glyph;
            }

            // Not found?, let's have a look at next (or previous) closest
            // system, according to source ordinate
            SystemInfo closest = getClosestSystem(system, source.y);

            if (closest != null) {
                glyph = lookupSystemGlyph(closest, source);
            }

            return glyph;
        }

        return null;
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
        return "{Sheet " + getPath() + "}";
    }

    //-------------------//
    // lookupSystemGlyph //
    //-------------------//
    private Glyph lookupSystemGlyph (SystemInfo system,
                                     Point      source)
    {
        /// Check this loop is really needed TBD TBD
        for (Stick bar : system.getBars()) {
            for (GlyphSection section : bar.getMembers()) {
                // Swap of x & y, since this is a vertical lag
                if (section.contains(source.y, source.x)) {
                    return bar;
                }
            }
        }

        for (Glyph glyph : system.getGlyphs()) {
            for (GlyphSection section : glyph.getMembers()) {
                // Swap of x & y, since this is a vertical lag
                if (section.contains(source.y, source.x)) {
                    return glyph;
                }
            }
        }

        // Not found
        return null;
    }

    //~ Inner Classes ----------------------------------------------------------

    /**
     * Step to (re)load sheet picture. A brand new sheet is created with the
     * provided image file as parameter.
     *
     * <p>The result of this step (a Picture) is <b>transient</b>, thus not
     * saved nor restored, since a picture is too costly. If picture is indeed
     * needed, then it is explicitly reloaded from the image file through the
     * <b>getPicture</b> method.
     */
    class LoadStep
        extends InstanceStep<Picture>
    {
        LoadStep ()
        {
            super("[Re]load the sheet picture");
        }

        public void doit ()
            throws ProcessingException
        {
            try {
                result = new Picture(imageFile);

                // Attach proper Selection objects
                // (reading from pixel location & writing to grey level)
                result.setLevelSelection(getSelection(SelectionTag.LEVEL));
                getSelection(SelectionTag.PIXEL)
                    .addObserver(result);

                // Display sheet picture if not batch mode
                if (Main.getJui() != null) {
                    PictureView pictureView = new PictureView(Sheet.this);
                    displayAssembly();
                    assembly.addViewTab(
                        "Picture",
                        pictureView,
                        new BoardsPane(
                            Sheet.this,
                            pictureView.getView(),
                            new PixelBoard("Picture")));
                }
            } catch (FileNotFoundException ex) {
                logger.warning("Cannot find file " + imageFile);
                throw new ProcessingException();
            } catch (IOException ex) {
                logger.warning("Input error on file " + imageFile);
                throw new ProcessingException();
            } catch (ImageFormatException ex) {
                logger.warning("Unsupported image format in file " + imageFile);
                logger.warning(ex.getMessage());

                if (Main.getJui() != null) {
                    Main.getJui()
                        .displayWarning(
                        "<B>" + ex.getMessage() + "</B><BR>" +
                        "Please use grey scale with 256 values");
                }

                throw new ProcessingException();
            } catch (Exception ex) {
                ex.printStackTrace();
                logger.warning(ex.getMessage());
                throw new ProcessingException();
            }
        }
    }
}
