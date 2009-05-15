//----------------------------------------------------------------------------//
//                                                                            //
//                                 S h e e t                                  //
//                                                                            //
//  Copyright (C) Herve Bitteur 2000-2009. All rights reserved.               //
//  This software is released under the GNU General Public License.           //
//  Please contact users@audiveris.dev.java.net to report bugs & suggestions. //
//----------------------------------------------------------------------------//
//
package omr.sheet;

import omr.Main;

import omr.glyph.Glyph;
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Glyphs;
import omr.glyph.Shape;
import omr.glyph.SymbolsModel;
import omr.glyph.ui.SymbolsController;
import omr.glyph.ui.SymbolsEditor;

import omr.lag.Section;
import omr.lag.Sections;

import omr.log.Logger;

import omr.score.Score;
import omr.score.common.PixelDimension;
import omr.score.common.PixelPoint;
import omr.score.entity.SystemNode;
import omr.score.ui.ScoreConstants;
import omr.score.visitor.ScoreColorizer;
import omr.score.visitor.ScoreVisitor;
import omr.score.visitor.SheetPainter;
import omr.score.visitor.Visitable;

import omr.script.Script;

import omr.selection.SelectionService;
import omr.selection.SheetLocationEvent;

import omr.sheet.picture.Picture;
import omr.sheet.picture.PictureView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetAssembly;

import omr.step.SheetSteps;
import omr.step.Step;
import static omr.step.Step.*;
import omr.step.StepException;

import omr.ui.BoardsPane;
import omr.ui.ErrorsEditor;
import omr.ui.MainGui;

import omr.util.BrokenLine;
import omr.util.Dumper;
import omr.util.FileUtil;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

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

    /** Link with related score. Set by SYSTEMS. */
    private Score score;

    /** Steps for this instance */
    private final SheetSteps sheetSteps;

    /** The script of user actions on this sheet */
    private Script script;

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
    private volatile LinesBuilder linesBuilder;

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

        // Insert in list of handled sheets
        SheetsManager.getInstance()
                     .insertInstance(this);

        // Update UI information if so needed
        displayAssembly();
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

                if (system != null) {
                    impacted.add(system);

                    Shape shape = glyph.getShape();

                    if (persistent ||
                        ((shape != null) && shape.isPersistent())) {
                        // Add the following systems
                        int index = systems.indexOf(system);
                        impacted.addAll(
                            systems.subList(index + 1, systems.size()));
                    }
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
        return getSheetSteps()
                   .getLatestStep() == SYMBOLS;
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
        picture.setLevelService(selectionService);
        selectionService.subscribe(SheetLocationEvent.class, picture);

        // Display sheet picture if not batch mode
        if (Main.getGui() != null) {
            PictureView pictureView = new PictureView(Sheet.this);
            displayAssembly();
            assembly.addViewTab(
                Step.LOAD,
                pictureView,
                new BoardsPane(
                    Sheet.this,
                    pictureView.getView(),
                    new PixelBoard(getRadix() + ":Picture", Sheet.this)));
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
        return picture;
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
     * @throws StepException
     */
    public void setScale (Scale scale)
        throws StepException
    {
        this.scale = scale;
        score.setScale(scale);

        // Remember current sheet dimensions in pixels
        width = getPicture()
                    .getWidth();
        height = getPicture()
                     .getHeight();

        score.setDimension(
            scale.toUnits(new PixelDimension(getWidth(), getHeight())));

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
        score.setSkewAngle(
            (int) Math.rint(getSkew().angle() * ScoreConstants.BASE));

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
            synchronized (this) {
                if (symbolsController == null) {
                    SymbolsModel model = new SymbolsModel(
                        this,
                        getVerticalLag());
                    symbolsController = new SymbolsController(model);
                    editor = new SymbolsEditor(this, symbolsController);
                }
            }
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
        getSymbolsController();

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
     * (the precise point is the glyph area center)
     * @param glyph the provided glyph
     * @return the containing system, or null
     */
    public SystemInfo getSystemOf (Glyph glyph)
    {
        return getSystemOf(glyph.getAreaCenter());
    }

    //-------------//
    // getSystemOf //
    //-------------//
    /**
     * Report the system, if any, which contains the provided vertical section
     * (the precise point is the glyph area center)
     * @param section the provided section
     * @return the containing system, or null
     */
    public SystemInfo getSystemOf (Section section)
    {
        return getSystemOf(section.getAreaCenter());
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
            SystemInfo glyphSystem = getSystemOf(glyph);

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
            SystemInfo sectionSystem = getSystemOf(section);

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
                            getSystemOf(section) + " and " + system + ") " +
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
        if (systemsBuilder == null) {
            synchronized (this) {
                if (systemsBuilder == null) {
                    systemsBuilder = new SystemsBuilder(this);
                }
            }
        }

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
     */
    public void setVerticalLag (GlyphLag vLag)
    {
        this.vLag = vLag;
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
        if (verticalsController == null) {
            synchronized (this) {
                if (verticalsController == null) {
                    verticalsController = new VerticalsController(this);
                }
            }
        }

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

        SheetsManager.getInstance()
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
        BrokenLine south = null;

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

    //-------------//
    // createScore //
    //-------------//
    /**
     * Simply create an empty Score instance and cross-link it with Sheet
     */
    public void createScore ()
    {
        if (logger.isFineEnabled()) {
            logger.fine("Allocating score");
        }

        score = new Score(getPath());

        // Mutual referencing
        score.setSheet(this);
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
            gui.sheetSetController.showSheet(this);
        }
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
            Dumper.dump(system, "#" + i++);
        }

        System.out.println("--- SystemInfos end ---");
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
    public Glyph lookupGlyph (PixelPoint source)
    {
        Glyph      glyph = null;
        SystemInfo system = getSystemOf(source);

        if (system != null) {
            return lookupSystemGlyph(system, source);
        }

        return null;
    }

    //-------------//
    // rebuildAfter //
    //-------------//
    public void rebuildAfter (Step                    step,
                              final Collection<Glyph> glyphs,
                              final Collection<Shape> shapes)
    {
        sheetSteps.rebuildAfter(step, glyphs, shapes, false); //Not imposed
    }

    //
    //    //-----------------------//
    //    // registerLagController //
    //    //-----------------------//
    //    /**
    //     * Register a lag controller for SheetLocation events
    //     *
    //     * @param lag the lag at hand
    //     */
    //    public void registerLagController (GlyphLagController controller)
    //    {
    //        // Input for location events
    //        eventService.subscribeStrongly(SheetLocationEvent.class, controller);
    //
    //        //        // Output for location events
    //        //        controller.setLocationService(eventService);
    //    }

    //----------------//
    // splitBarSticks //
    //----------------//
    /**
     * Split the bar sticks among systems
     *
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
            SystemInfo system = getSystemOf(ledger.getStick());

            if (system != null) {
                system.getLedgers()
                      .add(ledger);
            }
        }

        for (Ending ending : getHorizontals()
                                 .getEndings()) {
            SystemInfo system = getSystemOf(ending.getStick());

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

            if (system != null) {
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
        return "{Sheet " + getPath() + "}";
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
