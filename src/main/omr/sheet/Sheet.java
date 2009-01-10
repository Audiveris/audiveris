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
import omr.glyph.GlyphLag;
import omr.glyph.GlyphSection;
import omr.glyph.Shape;
import omr.glyph.SymbolsModel;
import omr.glyph.ui.SymbolsEditor;

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

import omr.selection.SheetLocationEvent;

import omr.sheet.picture.Picture;
import omr.sheet.picture.PictureView;
import omr.sheet.ui.PixelBoard;
import omr.sheet.ui.SheetAssembly;

import omr.step.SheetSteps;
import static omr.step.Step.*;
import omr.step.StepException;

import omr.stick.Stick;

import omr.ui.BoardsPane;
import omr.ui.ErrorsEditor;
import omr.ui.MainGui;

import omr.util.BrokenLine;
import omr.util.Dumper;
import omr.util.FileUtil;

import org.bushe.swing.event.EventService;
import org.bushe.swing.event.ThreadSafeEventService;

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

    /** Vertical lag (built by SYSTEMS/BarsBuilder) */
    private GlyphLag vLag;

    /** Sheet height in pixels */
    private int height = -1;

    /** Sheet width in pixels */
    private int width = -1;

    /** Retrieved systems. Set by SYSTEMS. */
    private List<SystemInfo> systems;

    /** Link with related score. Set by SYSTEMS. */
    private Score score;

    /** A bar line extractor for this sheet */
    private SystemsBuilder systemsBuilder;

    /** A measure extractor for this sheet */
    private MeasuresModel measuresModel;

    /** Horizontal lag (built by LINES/LinesBuilder) */
    private GlyphLag hLag;

    /** A staff line extractor for this sheet */
    private LinesBuilder linesBuilder;

    /** A ledger line extractor for this sheet */
    private HorizontalsBuilder horizontalsBuilder;

    /**
     * Non-lag related selections for this sheet
     * (SheetLocation, ScoreLocation and PixelLevel)
     */
    private EventService eventService = new ThreadSafeEventService();

    /** Dedicated skew builder */
    private SkewBuilder skewBuilder;

    /** Related errors editor */
    private volatile ErrorsEditor errorsEditor;

    /** Steps for this instance */
    private final SheetSteps sheetSteps;

    /** The script of user actions on this sheet */
    private Script script;

    /** Related assembly instance */
    private volatile SheetAssembly assembly;

    /** Specific builder dealing with glyphs */
    private volatile SymbolsModel symbolsBuilder;

    /** Related verticals model */
    private volatile VerticalsModel verticalsModel;

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
     * Meant for local (and XML binder ?) use only
     */
    private Sheet ()
    {
        sheetSteps = new SheetSteps(this);
        eventService.setDefaultCacheSizePerClassOrTopic(1);
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

    //-----------------//
    // getEventService //
    //-----------------//
    /**
     * Report the sheet event service
     * (which handles SheetLocationEvent, PixelLevelEvent, ScoreLocationEvent)
     * @return the sheet dedicated event service
     */
    public EventService getEventService ()
    {
        return eventService;
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
        eventService.subscribeStrongly(SheetLocationEvent.class, hLag);

        // Output
        hLag.setLocationService(eventService);
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

    //--------------------//
    // getMeasuresBuilder //
    //--------------------//
    /**
     * Give access to the builder in charge of measures computation
     *
     * @return the builder instance
     */
    public MeasuresModel getMeasuresModel ()
    {
        if (measuresModel == null) {
            synchronized (this) {
                if (measuresModel == null) {
                    measuresModel = new MeasuresModel(this);
                }
            }
        }

        return measuresModel;
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
        picture.setLevelService(eventService);
        eventService.subscribeStrongly(SheetLocationEvent.class, picture);

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

    //---------------//
    // getSheetSteps //
    //---------------//
    public SheetSteps getSheetSteps ()
    {
        return sheetSteps;
    }

    //------------------------//
    // getSheetVerticalsModel //
    //------------------------//
    public VerticalsModel getSheetVerticalsModel ()
    {
        if (verticalsModel == null) {
            synchronized (this) {
                if (verticalsModel == null) {
                    verticalsModel = new VerticalsModel(this);
                }
            }
        }

        return verticalsModel;
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
    // getSymbolsEditor //
    //------------------//
    /**
     * Give access to the UI dealing with symbol recognition
     *
     * @return the symbols editor
     */
    public SymbolsEditor getSymbolsEditor ()
    {
        return getSymbolsModel()
                   .getEditor();
    }

    //-----------------//
    // getSymbolsModel //
    //-----------------//
    /**
     * Give access to the module dealing with symbol management
     *
     * @return the symbols model
     */
    public SymbolsModel getSymbolsModel ()
    {
        if (symbolsBuilder == null) {
            synchronized (this) {
                if (symbolsBuilder == null) {
                    symbolsBuilder = new SymbolsModel(this);
                }
            }
        }

        return symbolsBuilder;
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
    public SystemInfo getSystemOf (Glyph glyph)
    {
        Point pt = glyph.getContourBox()
                        .getLocation();

        return getSystemOf(new PixelPoint(pt.x, pt.y));
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

        SystemInfo system = null;

        for (Glyph glyph : glyphs) {
            if (system == null) {
                system = getSystemOf(glyph);
            } else {
                // Make sure we are still in the same system
                if (getSystemOf(glyph) != system) {
                    throw new IllegalArgumentException(
                        "getSystemOf. Glyphs from different systems");
                }
            }
        }

        return system;
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

        // Input
        eventService.subscribeStrongly(SheetLocationEvent.class, vLag);

        // Output
        vLag.setLocationService(eventService);
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

        score = new Score(
            scale.toUnits(new PixelDimension(getWidth(), getHeight())),
            (int) Math.rint(getSkew().angle() * ScoreConstants.BASE),
            scale,
            getPath());

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
            // Prepare a assembly on this sheet, this uses the initial zoom
            // ratio
            int viewIndex = gui.sheetController.setSheetAssembly(this);
            gui.sheetController.showSheetView(viewIndex);
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

    //----------------//
    // splitBarSticks //
    //----------------//
    /**
     * Split the bar sticks among systems
     */
    public void splitBarSticks (Collection<Stick> barSticks)
    {
        for (SystemInfo system : systems) {
            system.clearGlyphs();
        }

        // Assign the bar sticks to the proper system glyphs collection
        for (Stick stick : barSticks) {
            getSystemOf(stick)
                .addGlyph(stick);
        }
    }

    //------------------//
    // splitHorizontals //
    //------------------//
    /**
     * Split the various horizontals among systems
     */
    public void splitHorizontals ()
    {
        for (SystemInfo system : systems) {
            system.getLedgers()
                  .clear();
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
    }

    //-----------------------//
    // splitVerticalSections //
    //-----------------------//
    /**
     * Split the various horizontal sections (Used by Glyphs).
     */
    public void splitVerticalSections ()
    {
        for (SystemInfo system : systems) {
            system.getMutableVerticalSections()
                  .clear();
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
    public void updateLastSteps (final Collection<Glyph> glyphs,
                                 final Collection<Shape> shapes)
    {
        sheetSteps.updateLastSteps(glyphs, shapes, false); //Not imposed
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
