//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h e e t                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2007. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Contact author at herve.bitteur@laposte.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;

import omr.glyph.Glyph;
import omr.glyph.GlyphInspector;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.GlyphsBuilder;
import omr.glyph.Shape;
import omr.glyph.SymbolsBuilder;
import omr.glyph.ui.SymbolsEditor;

import omr.score.Score;
import omr.score.ScoreBuilder;
import omr.score.entity.SystemNode;
import omr.score.visitor.ScoreColorizer;
import omr.score.visitor.ScoreVisitor;
import omr.score.visitor.SheetPainter;
import omr.score.visitor.Visitable;

import omr.script.Script;

import omr.selection.Selection;
import omr.selection.SelectionManager;
import omr.selection.SelectionTag;
import static omr.selection.SelectionTag.*;

import omr.step.SheetSteps;
import omr.step.Step;
import static omr.step.Step.*;
import omr.step.StepException;

import omr.ui.BoardsPane;
import omr.ui.ErrorsEditor;
import omr.ui.MainGui;
import omr.ui.PixelBoard;
import omr.ui.SheetAssembly;

import omr.util.FileUtil;
import omr.util.Logger;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Class <code>Sheet</code> encapsulates the original music image, as well as
 * pointers to all processings related to this image.
 *
 * @author Herv&eacute; Bitteur
 * @version $Id$
 */
public class Sheet
    implements Visitable
{
    //~ Static fields/initializers ---------------------------------------------

    /** Usual logger utility */
    private static final Logger logger = Logger.getLogger(Sheet.class);

    //~ Instance fields --------------------------------------------------------

    /** Link with sheet original image file. Set by constructor. */
    private File imageFile;

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

    /** A bar line extractor for this sheet */
    private BarsBuilder barsBuilder;

    /** A glyph extractor for this sheet */
    private GlyphsBuilder glyphBuilder;

    /** A glyph inspector for this sheet */
    private GlyphInspector glyphInspector;

    /** Horizontal lag (built by LINES/LinesBuilder) */
    private GlyphLag hLag;

    /** A staff line extractor for this sheet */
    private LinesBuilder linesBuilder;

    /** A ledger line extractor for this sheet */
    private HorizontalsBuilder horizontalsBuilder;

    /** All Current selections for this sheet */
    private SelectionManager selectionManager;

    /** Dedicated skew builder */
    private SkewBuilder skewBuilder;

    /** Score builder */
    private volatile ScoreBuilder scoreBuilder;

    /** To avoid concurrent modifications */
    private volatile boolean busy = false;

    /** Related errors editor */
    private volatile ErrorsEditor errorsEditor;

    /** Steps for this instance */
    private final SheetSteps sheetSteps;

    /** The script of user actions on this sheet */
    private Script script;

    /** Related assembly instance */
    private volatile SheetAssembly assembly;

    /** Specific builder dealing with glyphs */
    private volatile SymbolsBuilder symbolsBuilder;

    /** Related verticals builder */
    private volatile VerticalsBuilder verticalsBuilder;

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
     * @throws StepException raised if, while 'force' is false, image file
     *                  cannot be loaded
     */
    public Sheet (File    imageFile,
                  boolean force)
        throws StepException
    {
        this();

        if (logger.isFineEnabled()) {
            logger.fine("creating Sheet from image " + imageFile);
        }

        try {
            // We make sure we have a canonical form for the file name
            this.imageFile = imageFile.getCanonicalFile();
        } catch (IOException ex) {
            logger.warning(ex.toString(), ex);
        }

        // Load this image picture
        try {
            sheetSteps.doit(LOAD);
        } catch (StepException ex) {
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
     * @throws StepException
     */
    public Sheet (Score score)
        throws StepException
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
        sheetSteps = new SheetSteps(this);
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
        if (assembly == null) {
            synchronized (this) {
                if (assembly == null) {
                    setAssembly(new SheetAssembly(this));
                }
            }
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
        if (barsBuilder == null) {
            synchronized (this) {
                if (barsBuilder == null) {
                    barsBuilder = new BarsBuilder(this);
                }
            }
        }

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
    public void setBusy (boolean busy)
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
    public boolean isBusy ()
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
    // getErrorsEditor //
    //-----------------//
    public ErrorsEditor getErrorsEditor ()
    {
        if (errorsEditor == null) {
            synchronized (this) {
                if (errorsEditor == null) {
                    errorsEditor = new ErrorsEditor(this);
                }
            }
        }

        return errorsEditor;
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
            synchronized (this) {
                if (glyphInspector == null) {
                    glyphInspector = new GlyphInspector(
                        this,
                        getGlyphsBuilder());
                }
            }
        }

        return glyphInspector;
    }

    //------------------//
    // getGlyphsBuilder //
    //------------------//
    /**
     * Give access to the glyphs builder for this sheet
     *
     * @return the builder instance
     */
    public GlyphsBuilder getGlyphsBuilder ()
    {
        if (glyphBuilder == null) {
            synchronized (this) {
                if (glyphBuilder == null) {
                    glyphBuilder = new GlyphsBuilder(this);
                }
            }
        }

        return glyphBuilder;
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
            SHEET_RECTANGLE,
            HORIZONTAL_SECTION,
            HORIZONTAL_SECTION_ID,
            HORIZONTAL_GLYPH,
            HORIZONTAL_GLYPH_ID);

        // Output
        hLag.setLocationSelection(getSelection(SHEET_RECTANGLE));
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
            synchronized (this) {
                if (hLag == null) {
                    try {
                        // Brought by LinesBuilder, so...
                        sheetSteps.getResult(LINES);
                    } catch (StepException ex) {
                        logger.severe(
                            "Cannot retrieve HorizontalLag from LINES");
                    }
                }
            }
        }

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
        try {
            sheetSteps.getResult(HORIZONTALS);

            return horizontals;
        } catch (StepException ex) {
            logger.severe("Horizontals not processed");

            return null;
        }
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

    //--------------------//
    // getImpactedSystems //
    //--------------------//
    /**
     * Report the collection of systems that are impacted by a shape
     * modification in the provided glyphs
     *
     * @param glyphs the glyphs for which we look for impacted systems
     * @param shapes the collection of initial shapes of these glyphs
     * @return the ordered collection of systems
     */
    public SortedSet<SystemInfo> getImpactedSystems (Collection<Glyph> glyphs,
                                                     Collection<Shape> shapes)
    {
        // Flag to indicate that the impact may persist on the following systems
        boolean persistent = false;

        if (shapes != null) {
            for (Shape shape : shapes) {
                if (shape.isPersistent()) {
                    persistent = true;

                    break;
                }
            }
        } else {
            persistent = true; // More expensive, but safer
        }

        SortedSet<SystemInfo> impacted = new TreeSet<SystemInfo>();

        if (glyphs != null) {
            for (Glyph glyph : glyphs) {
                SystemInfo system = getSystemOf(glyph);
                impacted.add(system);

                Shape shape = glyph.getShape();

                if (persistent || ((shape != null) && shape.isPersistent())) {
                    // Add the following systems
                    int index = systems.indexOf(system);
                    impacted.addAll(systems.subList(index + 1, systems.size()));
                }
            }
        }

        return impacted;
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

    //-----------------//
    // setLinesBuilder //
    //-----------------//
    /**
     * Set the builder in charge of staff lines
     *
     * @param linesBuilder the builder instance
     */
    public void setLinesBuilder (LinesBuilder linesBuilder)
    {
        this.linesBuilder = linesBuilder;
    }

    //-----------------//
    // getLinesBuilder //
    //-----------------//
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
     * Check whether current step is SYMBOLS
     *
     * @return true if on SYMBOLS
     */
    public boolean isOnSymbols ()
    {
        return currentStep() == SYMBOLS;
    }

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
    // setPicture //
    //------------//
    /**
     * Set the picture of this sheet, that is the image to be processed.
     *
     * @param picture the related picture
     */
    public void setPicture (Picture picture)
    {
        this.picture = picture;

        // Attach proper Selection objects
        // (reading from pixel location & writing to grey level)
        picture.setLevelSelection(getSelection(SelectionTag.PIXEL_LEVEL));
        getSelection(SelectionTag.SHEET_RECTANGLE)
            .addObserver(picture);

        // Display sheet picture if not batch mode
        if (Main.getGui() != null) {
            PictureView pictureView = new PictureView(Sheet.this);
            displayAssembly();
            assembly.addViewTab(
                "Picture",
                pictureView,
                new BoardsPane(
                    Sheet.this,
                    pictureView.getView(),
                    new PixelBoard(getRadix() + ":Picture")));
        }
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
            sheetSteps.getResult(LOAD);

            return picture;
        } catch (StepException ex) {
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
        throws StepException
    {
        this.scale = scale;

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
        try {
            sheetSteps.getResult(SCALE);

            return scale;
        } catch (StepException ex) {
            throw new RuntimeException(ex);
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

    //-----------------//
    // getScoreBuilder //
    //-----------------//
    public ScoreBuilder getScoreBuilder ()
    {
        if (scoreBuilder == null) {
            synchronized (this) {
                if (scoreBuilder == null) {
                    scoreBuilder = new ScoreBuilder(score, this);
                }
            }
        }

        return scoreBuilder;
    }

    //-----------//
    // getScript //
    //-----------//
    public Script getScript ()
    {
        if (script == null) {
            script = new Script(this);
        }

        return script;
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

    //---------------//
    // getSheetSteps //
    //---------------//
    public SheetSteps getSheetSteps ()
    {
        return sheetSteps;
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
        try {
            sheetSteps.getResult(Step.SKEW);

            return skew;
        } catch (StepException ex) {
            logger.severe("Skew not available");

            return null;
        }
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
        try {
            sheetSteps.getResult(LINES);

            return staves;
        } catch (StepException ex) {
            logger.severe("Staves not available");

            return null;
        }
    }

    //-------------------//
    // getSymbolsBuilder //
    //-------------------//
    /**
     * Give access to the module dealing with symbol recognition
     *
     * @return the symbols builder
     */
    public SymbolsBuilder getSymbolsBuilder ()
    {
        if (symbolsBuilder == null) {
            synchronized (this) {
                if (symbolsBuilder == null) {
                    symbolsBuilder = new SymbolsBuilder(this);
                }
            }
        }

        return symbolsBuilder;
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
        return getSymbolsBuilder()
                   .getEditor();
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

    //-------------//
    // getSystemOf //
    //-------------//
    public SystemInfo getSystemOf (Glyph glyph)
    {
        return getSystemAtY(glyph.getContourBox().y);
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
                sheetSteps.getResult(BARS);
            } catch (StepException ex) {
                throw new RuntimeException(ex);
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
            SHEET_RECTANGLE,
            VERTICAL_SECTION,
            VERTICAL_SECTION_ID,
            VERTICAL_GLYPH,
            VERTICAL_GLYPH_ID);

        // Output
        vLag.setLocationSelection(getSelection(SHEET_RECTANGLE));
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
            synchronized (this) {
                if (vLag == null) {
                    try {
                        sheetSteps.getResult(BARS);
                    } catch (StepException ex) {
                        logger.severe("Cannot retrieve vLag from BARS");
                    }
                }
            }
        }

        return vLag;
    }

    //---------------------//
    // getVerticalsBuilder //
    //---------------------//
    public VerticalsBuilder getVerticalsBuilder ()
    {
        if (verticalsBuilder == null) {
            synchronized (this) {
                if (verticalsBuilder == null) {
                    verticalsBuilder = new VerticalsBuilder(this);
                }
            }
        }

        return verticalsBuilder;
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

    //--------//
    // accept //
    //--------//
    public boolean accept (ScoreVisitor visitor)
    {
        if (visitor instanceof SheetPainter) {
            ((SheetPainter) visitor).visit(this);
        }

        return true;
    }

    //----------//
    // addError //
    //----------//
    public void addError (SystemNode container,
                          Glyph      glyph,
                          String     text)
    {
        getErrorsEditor()
            .addError(container, glyph, text);
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
        if (!sheetSteps.isDone(Step.SCALE)) {
            try {
                setScale(score.getScale());
            } catch (StepException ex) {
                logger.warning("Step aborted", ex);
            }
        }

        if (!sheetSteps.isDone(Step.SKEW)) {
            setSkew(new Skew(score.getSkewAngleDouble()));
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
        // Close related UI assembly if any
        if (assembly != null) {
            assembly.close();
        }

        SheetManager.getInstance()
                    .close(this);

        if (picture != null) {
            picture.close();
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
        Step prev = null;

        for (Step step : Step.values()) {
            if (!sheetSteps.isDone(step)) {
                break;
            }

            prev = step;
        }

        return prev;
    }

    //-----------------//
    // displayAssembly //
    //-----------------//
    /**
     * Display the related sheet view in the tabbed pane
     */
    public void displayAssembly ()
    {
        MainGui gui = Main.getGui();

        if (gui != null) {
            // Prepare a assembly on this sheet, this uses the initial zoom
            // ratio
            int viewIndex = gui.sheetController.setSheetAssembly(this);
            gui.sheetController.showSheetView(viewIndex);
        }
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

    //-----------------//
    // updateLastSteps //
    //-----------------//
    public void updateLastSteps (Collection<Glyph> glyphs,
                                 Collection<Shape> shapes)
    {
        sheetSteps.updateLastSteps(glyphs, shapes, /* imposed => */
                                   false);
    }

    //-------------------//
    // lookupSystemGlyph //
    //-------------------//
    private Glyph lookupSystemGlyph (SystemInfo system,
                                     Point      source)
    {
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
}
