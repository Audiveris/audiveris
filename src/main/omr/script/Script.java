//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          S c r i p t                                           //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.script;

import omr.OMR;

import omr.sheet.Book;
import omr.sheet.SheetStub;

import omr.step.ProcessingCancellationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Class {@code Script} handles a complete script applied to a book.
 * <p>
 * A script is a sequence of {@link ScriptTask} instances that are recorded as the user interacts
 * with the book data.
 * It can also be created manually from scratch.
 * <p>
 * A script can be stored and reloaded/replayed.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "script")
public class Script
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Logger logger = LoggerFactory.getLogger(
            Script.class);

    //~ Instance fields ----------------------------------------------------------------------------
    /** Book to which the script is applied. */
    private Book book;

    /** Full path to the Book image file. */
    @XmlAttribute(name = "input")
    private final Path inputPath;

    /** Full path to the Book project file. */
    @XmlAttribute(name = "project")
    private final Path projectPath;

    /** Sheet offset of the book WRT full work. */
    @XmlAttribute(name = "offset")
    private Integer offset;

    /** Collection of 1-based sheet IDs explicitly included, if any. */
    @XmlList
    @XmlElement(name = "sheets")
    private SortedSet<Integer> sheetIds;

    /** Sequence of tasks that compose the script. */
    @XmlElements({
        @XmlElement(name = "assign", type = AssignTask.class),
        @XmlElement(name = "barline", type = BarlineTask.class),
        @XmlElement(name = "delete", type = DeleteTask.class),
        @XmlElement(name = "export", type = ExportTask.class),
        @XmlElement(name = "save", type = SaveTask.class),
        @XmlElement(name = "score", type = ScoreTask.class),
        @XmlElement(name = "insert", type = InsertTask.class),
        @XmlElement(name = "invalidate", type = InvalidateTask.class),
        @XmlElement(name = "parameters", type = ParametersTask.class),
        @XmlElement(name = "print", type = PrintTask.class),
        @XmlElement(name = "rational", type = RationalTask.class),
        @XmlElement(name = "reset", type = ResetTask.class),
        @XmlElement(name = "segment", type = SegmentTask.class),
        @XmlElement(name = "slur", type = SlurTask.class),
        @XmlElement(name = "book-step", type = BookStepTask.class),
        @XmlElement(name = "sheet-step", type = SheetStepTask.class),
        @XmlElement(name = "text", type = TextTask.class)
    })
    private final List<ScriptTask> tasks = new ArrayList<ScriptTask>();

    /** Flag a script that needs to be stored. */
    private boolean modified;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Create a script.
     *
     * @param book the related book
     */
    public Script (Book book)
    {
        this.book = book;

        inputPath = book.getInputPath();
        projectPath = book.getProjectPath();
        offset = book.getOffset();

        // Store sheet ids
        sheetIds = new TreeSet<Integer>();

        for (SheetStub stub : book.getStubs()) {
            sheetIds.add(stub.getNumber());
        }
    }

    /** No-arg constructor meant for JAXB. */
    private Script ()
    {
        inputPath = null;
        projectPath = null;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------//
    // addTask //
    //---------//
    /**
     * Add a task to the script.
     *
     * @param task the task to add at the end of the current sequence
     */
    public synchronized void addTask (ScriptTask task)
    {
        tasks.add(task);
        setModified(true);
        logger.debug("Script: added {}", task);
    }

    //------//
    // dump //
    //------//
    /**
     * Meant for debug.
     */
    public void dump ()
    {
        logger.info(toString());

        if ((sheetIds != null) && !sheetIds.isEmpty()) {
            logger.info("Included sheets: {}", sheetIds);
        }

        for (ScriptTask task : tasks) {
            logger.info(task.toString());
        }
    }

    //---------//
    // getBook //
    //---------//
    /**
     * Report the book this script is linked to.
     *
     * @return the book concerned
     */
    public Book getBook ()
    {
        return book;
    }

    /**
     * @return the offset
     */
    public Integer getOffset ()
    {
        return offset;
    }

    //------------//
    // isModified //
    //------------//
    /**
     * Has the script been modified (WRT its backup on disk)?
     *
     * @return the modified
     */
    public boolean isModified ()
    {
        return modified;
    }

    //-----//
    // run //
    //-----//
    /**
     * This methods runs sequentially and synchronously the various tasks of the script.
     * It is up to the caller to run this method in a separate thread if so desired.
     */
    public void run ()
    {
        logger.debug("Running {}{}", this, (book != null) ? (" on book " + book.getRadix()) : "");

        // Make book concrete (with its sheets)
        if (book == null) {
            if (inputPath != null) {
                book = OMR.getEngine().loadInput(inputPath);
                book.setOffset(offset);
                book.createStubs(sheetIds); // This loads all specified sheets indices
            } else if (projectPath != null) {
                book = OMR.getEngine().loadProject(projectPath);
            } else {
                logger.warn("No book defined in script");

                return;
            }
        }

        // Run the tasks in sequence
        try {
            for (ScriptTask task : tasks) {
                final SheetStub stub;

                if (task instanceof SheetTask) {
                    Integer sheetIndex = ((SheetTask) task).getSheetIndex();
                    stub = book.getStub(sheetIndex);

                    if (stub == null) {
                        logger.warn("Script error. No sheet for index {}", sheetIndex);

                        continue;
                    }
                } else {
                    stub = book.getFirstValidStub();
                }

                logger.debug("Running {} on {}", task, stub);

                try {
                    // Run the task synchronously (prolog/core/epilog)
                    task.run(stub.getSheet());
                } catch (ProcessingCancellationException pce) {
                    throw pce;
                } catch (Exception ex) {
                    logger.warn("Error running " + task, ex);
                    throw new RuntimeException(task.toString());
                }
            }

            logger.debug("All tasks run on {}", book);
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (Exception ex) {
            logger.warn("Script aborted", ex);
        } finally {
            // Flag the (active) script as up-to-date
            book.getScript().setModified(false);
        }
    }

    /**
     * @param offset the offset to set
     */
    public void setOffset (Integer offset)
    {
        this.offset = offset;
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("{Script");

        if (modified) {
            sb.append(" modified");
        }

        if (inputPath != null) {
            sb.append(" ").append(inputPath);
        } else if (projectPath != null) {
            sb.append(" ").append(projectPath);
        } else if (book != null) {
            sb.append(" ").append(book.getRadix());
        }

        if ((sheetIds != null) && !sheetIds.isEmpty()) {
            sb.append(" pages:").append(sheetIds);
        }

        if (tasks != null) {
            sb.append(" tasks:").append(tasks.size());
        }

        sb.append("}");

        return sb.toString();
    }

    //-------------//
    // setModified //
    //-------------//
    /**
     * Flag the script as modified (WRT disk)
     *
     * @param modified the modified to set
     */
    void setModified (boolean modified)
    {
        this.modified = modified;
    }
}
