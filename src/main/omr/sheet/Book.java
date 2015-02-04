//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                             B o o k                                            //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Herve Bitteur and others 2000-2014. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;

import omr.image.FilterDescriptor;
import omr.image.ImageLoading;

import omr.score.Score;

import omr.script.Script;
import omr.script.ScriptActions;

import omr.sheet.ui.BookBrowser;
import omr.sheet.ui.SheetActions;
import omr.sheet.ui.SheetsController;

import omr.step.StepException;

import omr.text.Language;

import omr.util.FileUtil;
import omr.util.Param;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFrame;

/**
 * Class {@code Book} is the root class for handling a physical set of image input files,
 * resulting in one or several logical MusicXML scores.
 * <p>
 * A book instance generally corresponds to an input file containing one or several images, each
 * image resulting in a separate {@link Sheet} instance.
 * But a (super) book may also contain (sub) books to recursively gather a sequence of input files.
 * <p>
 * A sheet generally contains one or several systems. An indented system (prefixed by part names)
 * usually indicates the beginning of a movement. Such indented system may appear in the middle of a
 * sheet, thus (logical) movement frontiers do not always match (physical) sheet frontiers.
 * <p>
 * <img src="doc-files/Book.png" />
 *
 * @author Hervé Bitteur
 */
public class Book
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(Book.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Input path of the related image(s), if any. */
    private final Path imagePath;

    /** Dedicated image loader. */
    private ImageLoading.Loader loader;

    /** Sheet offset of imageFile with respect to full work. */
    private int offset;

    /** The related file radix (name w/o extension). */
    private final String radix;

    /** True if the book contains several sheets. */
    private boolean multiSheet;

    /** Sequence of sheets from image file. */
    private final List<Sheet> sheets = new ArrayList<Sheet>();

    /** Flag to indicate this book is being closed. */
    private volatile boolean closing;

    /** Logical scores for this book. */
    private final List<Score> scores = new ArrayList<Score>();

    /** Sub books, if any. */
    private final List<Book> subBooks;

    /** The script of user actions on this book. */
    private Script script;

    /** The recording of key processing data. */
    private final BookBench bench;

    /** Dominant text language in the book. */
    private String language;

    /** Browser on this book. */
    private BookBrowser bookBrowser;

    /** Abstract path (sans extension) where the MusicXML output is to be stored. */
    private Path exportPath;

    /** Where the script is to be stored. */
    private File scriptFile;

    /** Where the book PDF data is to be stored. */
    private Path printPath;

    /** Handling of binarization filter parameter. */
    private final Param<FilterDescriptor> filterParam = new Param<FilterDescriptor>(
            FilterDescriptor.defaultFilter);

    /** Handling of language parameter. */
    private final Param<String> textParam = new Param<String>(Language.defaultSpecification);

    //~ Constructors -------------------------------------------------------------------------------
    //------//
    // Book //
    //------//
    /**
     * Create a Book with a path to an input image path.
     *
     * @param imagePath the input image path (which may contain several images)
     */
    public Book (Path imagePath)
    {
        if (imagePath == null) {
            throw new IllegalArgumentException("Trying to create a Book with null imagePath");
        }

        this.imagePath = imagePath;

        radix = FileUtil.getNameSansExtension(imagePath);
        subBooks = null;

        bench = new BookBench(this);

        // Start a score
        addScore(new Score());

        // Register this book instance
        BookManager.getInstance().addInstance(this);
    }

    //------//
    // Book //
    //------//
    /**
     * Create a meta Book, to be later populated with sub-books.
     *
     * @param radix a radix name for this book
     */
    public Book (String radix)
    {
        if (radix == null) {
            throw new IllegalArgumentException("Trying to create a meta Book with null radix");
        }

        this.radix = radix;

        imagePath = null;
        subBooks = new ArrayList<Book>();

        // Related bench
        bench = new BookBench(this);

        // Register this book instance
        BookManager.getInstance().addInstance(this);
    }

    /**
     * Creates a new {@code Book} just for one sheet.
     *
     * @param sheet  the single sheet
     * @param offset sheet offset
     */
    public Book (Sheet sheet,
                 int offset)
    {
        this.imagePath = sheet.getBook().getImagePath();
        this.offset = offset;

        radix = sheet.getBook().getRadix() + "#" + (1 + offset);
        subBooks = null;
        bench = new BookBench(this);
    }

    //~ Methods ------------------------------------------------------------------------------------
    //----------//
    // addScore //
    //----------//
    public final void addScore (Score score)
    {
        scores.add(score);
        scores.get(scores.size() - 1).setId(scores.size());
    }

    //-------//
    // close //
    //-------//
    /**
     * Close this book instance, as well as its view if any.
     */
    public void close ()
    {
        logger.info("Closing {}", this);

        setClosing(true);

        // Check whether the book script has been saved (or user has declined)
        if ((Main.getGui() != null) && !ScriptActions.checkStored(getScript())) {
            return;
        }

        // Close contained sheets (and pages)
        for (Sheet sheet : new ArrayList<Sheet>(sheets)) {
            sheet.remove(true);
        }

        // Close browser if any
        if (bookBrowser != null) {
            bookBrowser.close();
        }

        // Complete and store all bench data
        BookManager.getInstance().storeBench(bench, true);

        // Remove from score instances
        BookManager.getInstance().removeInstance(this);
    }

    //--------------//
    // createSheets //
    //--------------//
    /**
     * Create as many sheets as there are images in the input image file.
     * A created sheet is more like a placeholder, the corresponding sheet image will have to be
     * loaded later.
     *
     * @param sheetIds set of sheet IDs (1-based) explicitly included.
     *                 if set is empty or null all sheets found in file are created.
     */
    public void createSheets (SortedSet<Integer> sheetIds)
    {
        loader = ImageLoading.getLoader(imagePath);

        if (loader != null) {
            Sheet firstSheet = null;
            multiSheet = loader.getImageCount() > 1; // Several images in the file

            if (sheetIds == null) {
                sheetIds = new TreeSet<Integer>();

                for (int i = 1; i <= loader.getImageCount(); i++) {
                    sheetIds.add(i);
                }
            }

            for (int id : sheetIds) {
                Sheet sheet = null;

                try {
                    sheet = new Sheet(this, id, null);
                    sheets.add(sheet);

                    if (firstSheet == null) {
                        firstSheet = sheet;

                        // Let the UI focus on first sheet
                        if (Main.getGui() != null) {
                            SheetsController.getInstance().showAssembly(firstSheet);
                        }
                    }
                } catch (StepException ex) {
                    // Remove sheet from book, if already included
                    if ((sheet != null) && sheets.remove(sheet)) {
                        logger.info("Sheet #{} removed", id);
                    }
                }
            }

            // Remember (even across runs) the parent directory
            BookManager.getInstance().setDefaultInputDirectory(imagePath.getParent().toString());

            // Insert in sheet history
            BookManager.getInstance().getHistory().add(getImagePath().toString());

            if (Main.getGui() != null) {
                SheetActions.HistoryMenu.getInstance().setEnabled(true);
            }
        }
    }

    //----------//
    // getBench //
    //----------//
    /**
     * Report the related sheet bench.
     *
     * @return the related bench
     */
    public BookBench getBench ()
    {
        return bench;
    }

    //-----------------//
    // getBrowserFrame //
    //-----------------//
    /**
     * Create a dedicated frame, where all book elements can be browsed interactively.
     *
     * @return the created frame
     */
    public JFrame getBrowserFrame ()
    {
        if (bookBrowser == null) {
            // Build the BookBrowser on the score
            bookBrowser = new BookBrowser(this);
        }

        return bookBrowser.getFrame();
    }

    //---------------//
    // getExportPath //
    //---------------//
    /**
     * Report the abstract path (sans extension) for export
     *
     * @return the target path name sans extension, or null
     */
    public Path getExportPath ()
    {
        return exportPath;
    }

    //----------------//
    // getFilterParam //
    //----------------//
    public Param<FilterDescriptor> getFilterParam ()
    {
        return filterParam;
    }

    //---------------//
    // getFirstSheet //
    //---------------//
    public Sheet getFirstSheet ()
    {
        if (sheets.isEmpty()) {
            return null;
        }

        return sheets.get(0);
    }

    //--------------//
    // getImagePath //
    //--------------//
    /**
     * Report the path name of the book image(s).
     *
     * @return the image path
     */
    public Path getImagePath ()
    {
        return imagePath;
    }

    //--------------//
    // getLastScore //
    //--------------//
    public Score getLastScore ()
    {
        if (scores.isEmpty()) {
            return null;
        }

        return scores.get(scores.size() - 1);
    }

    //--------------//
    // getLastSheet //
    //--------------//
    public Sheet getLastSheet ()
    {
        if (sheets.isEmpty()) {
            return null;
        }

        return sheets.get(sheets.size() - 1);
    }

    //--------------//
    // getLogPrefix //
    //--------------//
    /**
     * Report the proper prefix to use when logging a message
     *
     * @return the proper prefix
     */
    public String getLogPrefix ()
    {
        if (BookManager.isMultiBook()) {
            return "[" + radix + "] ";
        } else {
            return "";
        }
    }

    /**
     * @return the offset
     */
    public int getOffset ()
    {
        return offset;
    }

    //--------------//
    // getPrintPath //
    //--------------//
    /**
     * Report to which path, if any, the sheet PDF data is to be written.
     *
     * @return the sheet PDF path, or null
     */
    public Path getPrintPath ()
    {
        return printPath;
    }

    //----------//
    // getRadix //
    //----------//
    /**
     * Report the radix of the file that corresponds to the book.
     * It is based on the simple file name of the book, with no path and no extension.
     *
     * @return the book input file radix
     */
    public String getRadix ()
    {
        return radix;
    }

    //-----------//
    // getScores //
    //-----------//
    /**
     * @return the scores
     */
    public List<Score> getScores ()
    {
        return Collections.unmodifiableList(scores);
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
    // getScriptFile //
    //---------------//
    /**
     * Report the file, if any, where the script should be written.
     *
     * @return the related script file or null
     */
    public File getScriptFile ()
    {
        return scriptFile;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Report the sheet with provided sheet-index.
     *
     * @param sheetIndex the desired value for sheet index
     * @return the proper sheet, or null if not found
     */
    public Sheet getSheet (int sheetIndex)
    {
        return sheets.get(sheetIndex);
    }

    //-----------//
    // getSheets //
    //-----------//
    /**
     * @return the sheets
     */
    public List<Sheet> getSheets ()
    {
        return sheets;
    }

    //--------------//
    // getTextParam //
    //--------------//
    public Param<String> getTextParam ()
    {
        return textParam;
    }

    //-------------//
    // hasLanguage //
    //-------------//
    /**
     * Check whether a language has been defined for this book.
     *
     * @return true if a language is defined
     */
    public boolean hasLanguage ()
    {
        return language != null;
    }

    //-------------//
    // includeBook //
    //-------------//
    /**
     * Include a (sub) book into this one.
     *
     * @param book the sub book to include
     */
    public void includeBook (Book book)
    {
        subBooks.add(book);
    }

    /**
     * @return the closing
     */
    public boolean isClosing ()
    {
        return closing;
    }

    //--------//
    // isIdle //
    //--------//
    /**
     * Check whether this book is idle or not.
     * The book is busy when at least one of its sheets is under a step processing.
     *
     * @return true if idle, false if busy
     */
    public boolean isIdle ()
    {
        for (Sheet sheet : sheets) {
            if ((sheet != null) && (sheet.getCurrentStep() != null)) {
                return false;
            }
        }

        return true;
    }

    //--------------//
    // isMultiSheet //
    //--------------//
    /**
     * @return the multiSheet
     */
    public boolean isMultiSheet ()
    {
        return multiSheet;
    }

    //-----------//
    // readImage //
    //-----------//
    /**
     * Actually load the image that corresponds to the specified sheet id.
     *
     * @param id specified sheet id
     * @return the loaded sheet image
     */
    public BufferedImage readImage (int id)
    {
        try {
            BufferedImage img = loader.getImage(id);
            logger.info("{} loaded sheet#{} {}x{}", this, id, img.getWidth(), img.getHeight());

            if (loader.allImagesLoaded()) {
                loader.dispose();
                logger.debug("{} all images loaded, loader disposed.", this);
            }

            return img;
        } catch (IOException ex) {
            logger.warn("Error in book.readImage", ex);

            return null;
        }
    }

    //-------------//
    // removeSheet //
    //-------------//
    public boolean removeSheet (Sheet sheet)
    {
        return sheets.remove(sheet);
    }

    /**
     * @param closing the closing to set
     */
    public void setClosing (boolean closing)
    {
        this.closing = closing;
    }

    //---------------//
    // setExportPath //
    //---------------//
    /**
     * Remember where the book is to be exported.
     *
     * @param exportPath the abstract path (without .xml or .mxl extension)
     */
    public void setExportPath (Path exportPath)
    {
        this.exportPath = exportPath;
    }

    //-------------//
    // setLanguage //
    //-------------//
    /**
     * Set the score dominant language.
     *
     * @param language the dominant language
     */
    public void setLanguage (String language)
    {
        this.language = language;
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset (int offset)
    {
        this.offset = offset;

        if (script != null) {
            // Forward to related script
            script.setOffset(offset);
        }
    }

    //--------------//
    // setPrintPath //
    //--------------//
    /**
     * Remember to which path the sheet PDF data is to be exported.
     *
     * @param sheetPdfPath the sheet PDF path
     */
    public void setPrintPath (Path sheetPdfPath)
    {
        this.printPath = sheetPdfPath;
    }

    //---------------//
    // setScriptFile //
    //---------------//
    /**
     * Remember the file where the script is written.
     *
     * @param scriptFile the related script file
     */
    public void setScriptFile (File scriptFile)
    {
        this.scriptFile = scriptFile;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder("{Book");
        sb.append(" ").append(radix);

        if (offset > 0) {
            sb.append(" offset:").append(offset);
        }

        sb.append("}");

        return sb.toString();
    }
}
