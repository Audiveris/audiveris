//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c S t u b                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//  Copyright © Hervé Bitteur and others 2000-2016. All rights reserved.
//  This software is released under the GNU General Public License.
//  Goto http://kenai.com/projects/audiveris to report bugs or suggestions.
//------------------------------------------------------------------------------------------------//
// </editor-fold>
package omr.sheet;

import omr.Main;
import omr.OMR;
import static omr.WellKnowns.LINE_SEPARATOR;

import omr.constant.Constant;
import omr.constant.ConstantSet;

import omr.image.FilterDescriptor;

import omr.log.LogUtil;
import static omr.sheet.Sheet.INTERNALS_RADIX;
import omr.sheet.ui.SheetAssembly;
import omr.sheet.ui.StubsController;

import omr.step.ProcessingCancellationException;
import omr.step.Step;
import omr.step.StepException;
import omr.step.ui.StepMonitoring;

import omr.ui.Colors;

import omr.util.Jaxb;
import omr.util.LiveParam;
import omr.util.Navigable;
import omr.util.OmrExecutors;
import omr.util.StopWatch;
import omr.util.Zip;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.EnumSet;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.swing.SwingUtilities;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Class {@code BasicStub} is the implementation of SheetStub.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class BasicStub
        implements SheetStub
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(
            BasicStub.class);

    //~ Instance fields ----------------------------------------------------------------------------
    //
    // Persistent data
    //----------------
    //
    /** Index of sheet, counted from 1, in the image file. */
    @XmlAttribute(name = "number")
    private final int number;

    /** All steps already performed on this sheet. */
    @XmlList
    @XmlElement(name = "steps")
    private final EnumSet<Step> doneSteps = EnumSet.noneOf(Step.class);

    /** Indicate a sheet that contains no music. */
    @XmlElement
    private Jaxb.True invalid;

    // Transient data
    //---------------
    //
    /** Processing lock. */
    private final Lock lock = new ReentrantLock();

    /** Containing book. */
    @Navigable(false)
    private Book book;

    /** Full sheet material, if any. */
    private volatile BasicSheet sheet;

    /** The step being performed on the sheet. */
    private volatile Step currentStep;

    /** Has sheet been modified, WRT its book data. */
    private boolean modified = false;

    /** Related assembly instance, if any. */
    private SheetAssembly assembly;

    /** Param for pixel filter. */
    private LiveParam<FilterDescriptor> filterContext;

    /** Param for text language. */
    private LiveParam<String> textContext;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new {@code SheetStub} object.
     *
     * @param book   the containing book instance
     * @param number the 1-based sheet number within the containing book
     */
    public BasicStub (Book book,
                      int number)
    {
        this.number = number;

        initTransients(book);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private BasicStub ()
    {
        this.number = 0;
    }

    //~ Methods ------------------------------------------------------------------------------------
    //------//
    // done //
    //------//
    /**
     * Remember that the provided step has been completed on the sheet.
     *
     * @param step the provided step
     */
    public final void done (Step step)
    {
        doneSteps.add(step);
    }

    //-------//
    // close //
    //-------//
    @Override
    public void close ()
    {
        // If no stub is left, force book closing
        if (!book.isClosing()) {
            if (!book.getStubs().isEmpty()) {
                logger.info("Sheet closed");
            } else {
                book.close();
            }
        }
    }

    //-------------//
    // createSheet //
    //-------------//
    @Override
    public Sheet createSheet ()
    {
        try {
            sheet = new BasicSheet(this, null);
        } catch (StepException ex) {
            logger.warn("Error creating blank sheet", ex);
        }

        return sheet;
    }

    //-----------------//
    // decideOnRemoval //
    //-----------------//
    @Override
    public void decideOnRemoval (String msg,
                                 boolean dummy)
            throws StepException
    {
        if (dummy) {
            throw new StepException("Dummy decision");
        }

        logger.warn(msg.replaceAll(LINE_SEPARATOR, " "));

        if (OMR.gui != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    // Make sheet visible to the user
                    StubsController.getInstance().selectAssembly(BasicStub.this);
                }
            });
        }

        if ((OMR.gui == null) /// || (OMR.gui.displayModelessConfirm(msg + LINE_SEPARATOR + "OK for discarding this sheet?") == JOptionPane.OK_OPTION)) {
            || (OMR.gui.displayConfirmation(msg + LINE_SEPARATOR + "OK for discarding this sheet?"))) {
            invalidate();

            if (book.isMultiSheet()) {
                close();
                throw new StepException("Sheet removed");
            } else {
                throw new StepException("Sheet ignored");
            }
        }
    }

    //-------------//
    // getAssembly //
    //-------------//
    @Override
    public SheetAssembly getAssembly ()
    {
        return assembly;
    }

    //---------//
    // getBook //
    //---------//
    @Override
    public Book getBook ()
    {
        return book;
    }

    //----------------//
    // getCurrentStep //
    //----------------//
    @Override
    public Step getCurrentStep ()
    {
        return currentStep;
    }

    //----------------//
    // getFilterParam //
    //----------------//
    @Override
    public LiveParam<FilterDescriptor> getFilterParam ()
    {
        return filterContext;
    }

    //-------//
    // getId //
    //-------//
    @Override
    public String getId ()
    {
        if (book.isMultiSheet()) {
            return book.getRadix() + "#" + number;
        } else {
            return book.getRadix();
        }
    }

    //------------------//
    // getLanguageParam //
    //------------------//
    @Override
    public LiveParam<String> getLanguageParam ()
    {
        return textContext;
    }

    //---------------//
    // getLatestStep //
    //---------------//
    @Override
    public Step getLatestStep ()
    {
        Step latest = null;

        for (Step step : Step.values()) {
            if (isDone(step)) {
                latest = step;
            }
        }

        return latest;
    }

    //---------//
    // getLock //
    //---------//
    @Override
    public Lock getLock ()
    {
        return lock;
    }

    //--------//
    // getNum //
    //--------//
    @Override
    public String getNum ()
    {
        if (book.isMultiSheet()) {
            return "#" + number;
        }

        return "";
    }

    //-----------//
    // getNumber //
    //-----------//
    @Override
    public int getNumber ()
    {
        return number;
    }

    //----------//
    // getSheet //
    //----------//
    @Override
    public BasicSheet getSheet ()
    {
        if (sheet == null) {
            synchronized (this) {
                // We have to recheck sheet, which may have just been allocated
                if (sheet == null) {
                    if (SwingUtilities.isEventDispatchThread()) {
                        logger.warn("XXXX getSheet called on EDT XXXX");
                    }

                    // Actually load the sheet
                    if (!isDone(Step.LOAD)) {
                        // LOAD not yet performed: load from book image file
                        try {
                            sheet = new BasicSheet(this, null);
                        } catch (StepException ignored) {
                            logger.info("Could not load sheet for stub {}", this);
                        }
                    } else {
                        // LOAD already performed: load from book file
                        StopWatch watch = new StopWatch("Load Sheet " + this);

                        try {
                            watch.start("unmarshal");

                            // Open the book file system
                            Path sheetFile = book.openSheetFolder(number).resolve(
                                    BasicSheet.getSheetFileName(number));
                            InputStream is = Files.newInputStream(
                                    sheetFile,
                                    StandardOpenOption.READ);
                            sheet = BasicSheet.unmarshal(is);

                            // Close the stream as well as the book file system
                            is.close();
                            sheetFile.getFileSystem().close();

                            // Complete sheet reload
                            watch.start("afterReload");
                            sheet.afterReload(this);
                            LogUtil.start(this);
                            logger.info("Loaded {}", sheetFile);
                        } catch (Exception ex) {
                            logger.warn("Error in loading sheet structure " + ex, ex);
                        } finally {
                            if (constants.printWatch.isSet()) {
                                watch.print();
                            }
                        }
                    }
                }
            }
        }

        return sheet;
    }

    //----------//
    // hasSheet //
    //----------//
    @Override
    public boolean hasSheet ()
    {
        return sheet != null;
    }

    //------------//
    // invalidate //
    //------------//
    @Override
    public void invalidate ()
    {
        invalid = Jaxb.TRUE;
        setModified(true);

        if (OMR.gui != null) {
            StubsController.getInstance().markTab(this, Colors.SHEET_INVALID);
        }

        logger.info("Sheet {} flagged as invalid.", getId());
    }

    //--------//
    // isDone //
    //--------//
    @Override
    public boolean isDone (Step step)
    {
        return doneSteps.contains(step);
    }

    //------------//
    // isModified //
    //------------//
    @Override
    public boolean isModified ()
    {
        return modified;
    }

    //---------//
    // isValid //
    //---------//
    @Override
    public boolean isValid ()
    {
        return invalid == null;
    }

    //-----------//
    // reachStep //
    //-----------//
    @Override
    public boolean reachStep (Step target,
                              boolean force)
    {
        final StubsController ctrl = (OMR.gui != null) ? StubsController.getInstance() : null;
        final StopWatch watch = new StopWatch("reachStep " + target);
        SortedSet<Step> neededSteps = null;
        boolean ok = false;
        getLock().lock();
        logger.debug("reachStep got lock on {}", this);

        try {
            if (force && isDone(target)) {
                reset();
            }

            neededSteps = getNeededSteps(target);

            if (neededSteps.isEmpty()) {
                return true;
            }

            logger.debug("Sheet#{} scheduling {}", number, neededSteps);

            StepMonitoring.notifyStart();

            if (ctrl != null) {
                ctrl.markTab(this, Colors.SHEET_BUSY);
            }

            for (final Step step : neededSteps) {
                watch.start(step.name());
                StepMonitoring.notifyMsg(step.toString());
                logger.debug("reachStep {} towards {}", step, target);
                doOneStep(step);
            }

            ok = true;
        } catch (ProcessingCancellationException pce) {
            throw pce;
        } catch (StepException ignored) {
            logger.info("StepException detected in " + neededSteps);
        } catch (Exception ex) {
            logger.warn("Error in performing {} {}", neededSteps, ex.toString(), ex);
        } finally {
            StepMonitoring.notifyStop();

            if (constants.printWatch.isSet()) {
                watch.print();
            }

            logger.debug("reachStep releasing lock on {}", this);
            getLock().unlock();
        }

        if (ctrl != null) {
            ctrl.markTab(this, ok ? Colors.SHEET_OK : Colors.SHEET_NOT_OK);
        }

        return ok;
    }

    //-------//
    // reset //
    //-------//
    @Override
    public void reset ()
    {
        doneSteps.clear();
        invalid = null;
        sheet = null;

        if (assembly != null) {
            assembly.reset();
        }

        setModified(true);
        logger.info("Sheet#{} reset as valid.", number);

        if (OMR.gui != null) {
            SwingUtilities.invokeLater(
                    new Runnable()
            {
                @Override
                public void run ()
                {
                    StubsController.getInstance().stateChanged(null);
                }
            });
        }
    }

    //----------------//
    // setCurrentStep //
    //----------------//
    public void setCurrentStep (Step step)
    {
        currentStep = step;
    }

    //-------------//
    // setModified //
    //-------------//
    @Override
    public void setModified (boolean modified)
    {
        this.modified = modified;

        if (modified) {
            book.setModified(true);
        }
    }

    //------------//
    // storeSheet //
    //------------//
    @Override
    public void storeSheet ()
            throws Exception
    {
        if (modified) {
            Path bookPath = BookManager.getDefaultBookPath(book);

            synchronized (book) {
                Path root = Zip.openFileSystem(bookPath);
                book.storeBookInfo(root); // Book info (book.xml)

                Path sheetFolder = root.resolve(INTERNALS_RADIX + getNumber());
                sheet.store(sheetFolder, null);
                root.getFileSystem().close();
            }
        }
    }

    //-----------//
    // swapSheet //
    //-----------//
    @Override
    public void swapSheet ()
    {
        try {
            if (isModified()) {
                logger.info("{} storing", this);
                storeSheet();
            }

            if (sheet != null) {
                logger.info("{} disposed", sheet);
                sheet = null;
            }

            if (OMR.gui != null) {
                SwingUtilities.invokeLater(
                        new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        // Gray out the related tab
                        StubsController ctrl = StubsController.getInstance();
                        ctrl.markTab(BasicStub.this, Colors.SHEET_NOT_LOADED);

                        // Close stub UI, if any
                        if (assembly != null) {
                            ///ctrl.deleteAssembly(BasicStub.this);
                            assembly.reset();
                            assembly.close();
                        }
                    }
                });
            }
        } catch (Exception ex) {
            logger.warn("Error swapping sheet", ex);
        }
    }

    //----------//
    // toString //
    //----------//
    @Override
    public String toString ()
    {
        return "Stub#" + number;
    }

    //----------------//
    // afterUnmarshal //
    //----------------//
    /**
     * Called after all the properties (except IDREF) are unmarshalled
     * for this object, but before this object is set to the parent object.
     * All non-persistent members are null.
     */
    @PostConstruct // Don't remove this method, invoked by JAXB through reflection

    private void afterUnmarshal (Unmarshaller um,
                                 Object parent)
    {
        initTransients((Book) parent);
    }

    //-----------//
    // doOneStep //
    //-----------//
    /**
     * Do just one specified step, synchronously, with display of related UI if any
     * and recording of the step into the script.
     *
     * @param step the step to perform
     * @throws StepException
     */
    private void doOneStep (final Step step)
            throws StepException
    {
        final int timeout = Main.getSheetStepTimeOut();
        Future<Void> future = null;

        try {
            // Make sure sheet is available
            if (!hasSheet()) {
                getSheet();
            }

            // Implement a timeout for this step on the stub
            future = OmrExecutors.getCachedLowExecutor().submit(
                    new Callable<Void>()
            {
                @Override
                public Void call ()
                        throws Exception
                {
                    LogUtil.start(BasicStub.this);

                    try {
                        setCurrentStep(step);
                        StepMonitoring.notifyStep(BasicStub.this, step); // Start monitoring
                        setModified(true); // At beginning of processing
                        sheet.reset(step); // Reset sheet relevant data
                        step.doit(sheet); // Standard processing on an existing sheet
                        done(step); // Full completion
                    } finally {
                        LogUtil.stopBook();
                    }

                    return null;
                }
            });

            future.get(timeout, TimeUnit.SECONDS);

            // At end of each step, save sheet to disk?
            if ((OMR.gui == null) && Main.saveSheetOnEveryStep()) {
                logger.debug("calling storeSheet");
                storeSheet();
            }
        } catch (TimeoutException tex) {
            logger.warn("Timeout {} seconds for step {}", timeout, step, tex);

            // Signal the on-going step processing to stop (if possible)
            if (future != null) {
                future.cancel(true);
            }

            throw new ProcessingCancellationException(tex);
        } catch (Exception ex) {
            logger.warn("Error in {} {}", step, ex.toString(), ex);

            Throwable cause = ex.getCause();

            if (cause != null) {
                logger.info("Cause {}", cause.toString());

                if (cause instanceof StepException) {
                    throw (StepException) cause;
                }
            }

            throw new StepException(ex);
        } finally {
            setCurrentStep(null);
            StepMonitoring.notifyStep(this, step); // Stop monitoring
        }
    }

    //----------------//
    // getNeededSteps //
    //----------------//
    private SortedSet<Step> getNeededSteps (Step target)
    {
        SortedSet<Step> neededSteps = new TreeSet();

        // Add all needed steps
        for (Step step : EnumSet.range(Step.first(), target)) {
            if (!isDone(step)) {
                neededSteps.add(step);
            }
        }

        return neededSteps;
    }

    //----------------//
    // initTransients //
    //----------------//
    /**
     * Initialize needed transient members.
     * (which by definition have not been set by the unmarshalling).
     *
     * @param book the containing book
     */
    private void initTransients (Book book)
    {
        try {
            LogUtil.start(book);

            logger.trace("{} initTransients", this);
            this.book = book;

            filterContext = new LiveParam<FilterDescriptor>(book.getFilterParam());
            textContext = new LiveParam<String>(book.getLanguageParam());

            if (OMR.gui != null) {
                assembly = new SheetAssembly(this);
            }
        } finally {
            LogUtil.stopBook();
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //---------//
    // Adapter //
    //---------//
    /**
     * Meant for JAXB handling of SheetStub interface.
     */
    public static class Adapter
            extends XmlAdapter<BasicStub, SheetStub>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public BasicStub marshal (SheetStub s)
        {
            return (BasicStub) s;
        }

        @Override
        public SheetStub unmarshal (BasicStub s)
        {
            return s;
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static final class Constants
            extends ConstantSet
    {
        //~ Instance fields ------------------------------------------------------------------------

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch for sheet loading");
    }
}
