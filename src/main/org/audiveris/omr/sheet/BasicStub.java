//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        B a s i c S t u b                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2018. All rights reserved.
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
package org.audiveris.omr.sheet;

import org.audiveris.omr.Main;
import org.audiveris.omr.OMR;
import static org.audiveris.omr.WellKnowns.LINE_SEPARATOR;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.sheet.Picture.TableKey;
import static org.audiveris.omr.sheet.Sheet.INTERNALS_RADIX;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.ProcessingCancellationException;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.step.ui.StepMonitoring;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Memory;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.OmrExecutors;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.ZipFileSystem;
import org.audiveris.omr.util.param.Param;
import org.audiveris.omr.util.param.StringParam;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.PostConstruct;
import javax.swing.SwingUtilities;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

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

    /** Indicate a sheet that contains no music. */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean invalid;

    /** Handling of binarization filter parameter. */
    @XmlElement(name = "binarization")
    @XmlJavaTypeAdapter(FilterParam.Adapter.class)
    private FilterParam binarizationFilter;

    /** Handling of dominant language(s) for this sheet. */
    @XmlElement(name = "ocr-languages")
    @XmlJavaTypeAdapter(StringParam.Adapter.class)
    private StringParam ocrLanguages;

    /** Handling of processing switches for this sheet. */
    @XmlElement(name = "processing")
    @XmlJavaTypeAdapter(ProcessingSwitches.Adapter.class)
    private ProcessingSwitches switches;

    /** All steps already performed on this sheet. */
    @XmlList
    @XmlElement(name = "steps")
    private final EnumSet<Step> doneSteps = EnumSet.noneOf(Step.class);

    /** Pages references. */
    @XmlElement(name = "page")
    private final List<PageRef> pageRefs = new ArrayList<PageRef>();

    // Transient data
    //---------------
    //
    /** Processing lock. */
    private final Lock lock = new ReentrantLock();

    /** Containing book. */
    @Navigable(false)
    private Book book;

    /** Full sheet material, if any. */
    private volatile Sheet sheet;

    /** The step being performed on the sheet. */
    private volatile Step currentStep;

    /** Has sheet been modified, WRT its book data. */
    private boolean modified = false;

    /** Related assembly instance, if any. */
    private SheetAssembly assembly;

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
    //------------//
    // addPageRef //
    //------------//
    @Override
    public void addPageRef (PageRef pageRef)
    {
        pageRefs.add(pageRef);
    }

    //---------------//
    // clearPageRefs //
    //---------------//
    @Override
    public void clearPageRefs ()
    {
        // Clear pageRefs in this stub
        pageRefs.clear();

        // Clear pageRefs in related book store if any
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

    //-----------------//
    // decideOnRemoval //
    //-----------------//
    @Override
    public void decideOnRemoval (String msg,
                                 boolean dummy)
            throws StepException
    {
        if (dummy) {
            invalidate();
            throw new StepException("Dummy decision");
        }

        logger.warn(msg.replaceAll(LINE_SEPARATOR, " "));

        if (OMR.gui != null) {
            StubsController.invokeSelect(this);
        }

        if ((OMR.gui == null)
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

    //-------------//
    // getAssembly //
    //-------------//
    @Override
    public SheetAssembly getAssembly ()
    {
        return assembly;
    }

    //-----------------------//
    // getBinarizationFilter //
    //-----------------------//
    @Override
    public FilterParam getBinarizationFilter ()
    {
        if (binarizationFilter == null) {
            binarizationFilter = new FilterParam();
            binarizationFilter.setParent(FilterDescriptor.defaultFilter);
        }

        return binarizationFilter;
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

    //-----------------//
    // getFirstPageRef //
    //-----------------//
    @Override
    public PageRef getFirstPageRef ()
    {
        if (pageRefs.isEmpty()) {
            return null;
        }

        return pageRefs.get(0);
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

    //----------------//
    // getLastPageRef //
    //----------------//
    @Override
    public PageRef getLastPageRef ()
    {
        if (pageRefs.isEmpty()) {
            return null;
        }

        return pageRefs.get(pageRefs.size() - 1);
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

    //-----------------//
    // getOcrLanguages //
    //-----------------//
    @Override
    public Param<String> getOcrLanguages ()
    {
        if (ocrLanguages == null) {
            ocrLanguages = new StringParam();
            ocrLanguages.setParent(book.getOcrLanguages());
        }

        return ocrLanguages;
    }

    //-------------//
    // getPageRefs //
    //-------------//
    /**
     * @return the pageRefs
     */
    @Override
    public List<PageRef> getPageRefs ()
    {
        return pageRefs;
    }

    //-----------------------//
    // getProcessingSwitches //
    //-----------------------//
    @Override
    public ProcessingSwitches getProcessingSwitches ()
    {
        if (switches == null) {
            switches = new ProcessingSwitches();
            switches.setParent(book.getProcessingSwitches());
        }

        return switches;
    }

    //----------//
    // getSheet //
    //----------//
    @Override
    public Sheet getSheet ()
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
                            sheet = new Sheet(this, (BufferedImage) null);
                        } catch (StepException ignored) {
                            logger.info("Could not load sheet for stub {}", this);
                        }
                    } else {
                        // LOAD already performed: load from book file
                        StopWatch watch = new StopWatch("Load Sheet " + this);

                        try {
                            Path sheetFile = null;
                            watch.start("unmarshal");

                            // Open the book file system
                            try {
                                book.getLock().lock();
                                sheetFile = book.openSheetFolder(number).resolve(Sheet.getSheetFileName(number));

                                InputStream is = Files.newInputStream(
                                        sheetFile,
                                        StandardOpenOption.READ);
                                sheet = Sheet.unmarshal(is);

                                // Close the stream as well as the book file system
                                is.close();
                                sheetFile.getFileSystem().close();
                            } finally {
                                book.getLock().unlock();
                            }

                            // Complete sheet reload
                            watch.start("afterReload");
                            sheet.afterReload(this);
                            logger.info("Loaded {}", sheetFile);
                        } catch (Exception ex) {
                            logger.warn("Error in loading sheet structure " + ex, ex);
                            logger.info("Trying to restart from binary");
                            resetToBinary();
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
        invalid = Boolean.TRUE;

        book.updateScores(this);

        pageRefs.clear();
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
        return !invalid;
    }

    //-----------//
    // reachStep //
    //-----------//
    /**
     * {@inheritDoc}
     * <p>
     * Each needed step is performed sequentially, guarded by a timeout.
     */
    @Override
    public boolean reachStep (Step target,
                              boolean force)
    {
        final StubsController ctrl = (OMR.gui != null) ? StubsController.getInstance() : null;
        final StopWatch watch = new StopWatch("reachStep " + target);
        EnumSet<Step> neededSteps = null;
        boolean ok = false;
        getLock().lock(); // Wait for completion of early processing if any
        logger.debug("reachStep got lock on {}", this);

        try {
            final Step latestStep = getLatestStep();

            if (force && (target.compareTo(latestStep) <= 0)) {
                resetToBinary();
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
        } catch (ExecutionException ex) {
            // A StepException may have been wrapped into an ExecutionException
            if (ex.getCause() instanceof StepException) {
                logger.info("StepException cause detected in " + neededSteps);
            } else {
                logger.warn("Error in performing {} {}", neededSteps, ex.toString(), ex);
            }
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
        doReset();
        logger.info("Sheet#{} reset as valid.", number);

        if (OMR.gui != null) {
            try {
                Runnable runnable = new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        StubsController.getInstance().reDisplay(BasicStub.this);
                    }
                };

                if (SwingUtilities.isEventDispatchThread()) {
                    runnable.run();
                } else {
                    SwingUtilities.invokeAndWait(runnable);
                }
            } catch (Throwable ex) {
                logger.warn("Could not reset {}", ex.toString(), ex);
            }
        }
    }

    //---------------//
    // resetToBinary //
    //---------------//
    @Override
    public void resetToBinary ()
    {
        try {
            // Avoid loading sheet just to reset to binary:
            // If sheet is available, use its picture.getTable()
            // Otherwise, load it directly from binary.xml on disk
            RunTable binaryTable = null;

            if (hasSheet()) {
                logger.debug("Getting BINARY from sheet");
                binaryTable = getSheet().getPicture().getTable(TableKey.BINARY);
            }

            if (binaryTable == null) {
                logger.debug("Loading BINARY from disk");
                binaryTable = new RunTableHolder(TableKey.BINARY).getData(this);
            }

            doReset();
            sheet = new Sheet(this, binaryTable);
            logger.info("Sheet#{} reset to BINARY.", number);
        } catch (Throwable ex) {
            logger.warn("Could not reset to BINARY {}", ex.toString(), ex);
            reset();
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
            book.setDirty(true);
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
            book.getLock().lock();

            Path bookPath = BookManager.getDefaultSavePath(book);

            try {
                Path root = ZipFileSystem.open(bookPath);
                book.storeBookInfo(root); // Book info (book.xml)

                Path sheetFolder = root.resolve(INTERNALS_RADIX + getNumber());
                sheet.store(sheetFolder, null);
                root.getFileSystem().close();
            } finally {
                book.getLock().unlock();
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
                Memory.gc(); // Trigger a garbage collection...
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
                            assembly.reset();
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

    //------------//
    // transcribe //
    //------------//
    @Override
    public boolean transcribe ()
    {
        return reachStep(Step.last(), false);
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

    //---------------//
    // beforeMarshal //
    //---------------//
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        if ((binarizationFilter != null) && !binarizationFilter.isSpecific()) {
            binarizationFilter = null;
        }

        if ((ocrLanguages != null) && !ocrLanguages.isSpecific()) {
            ocrLanguages = null;
        }

        if ((switches != null) && switches.isEmpty()) {
            switches = null;
        }
    }

    //-----------//
    // doOneStep //
    //-----------//
    /**
     * Do just one specified step, synchronously, with display of related UI if any.
     * <p>
     * Step duration is guarded by a timeout, so that processing cannot get blocked infinitely.
     *
     * @param step the step to perform
     * @throws Exception
     */
    private void doOneStep (final Step step)
            throws Exception
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
                        LogUtil.stopStub();
                    }

                    return null;
                }
            });

            future.get(timeout, TimeUnit.SECONDS);

            // At end of each step, save sheet to disk?
            if ((OMR.gui == null) && Main.getCli().isSave()) {
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
        } finally {
            setCurrentStep(null);
            StepMonitoring.notifyStep(this, step); // Stop monitoring
        }
    }

    //---------//
    // doReset //
    //---------//
    private void doReset ()
    {
        doneSteps.clear();
        pageRefs.clear();
        invalid = false;
        sheet = null;

        if (assembly != null) {
            assembly.reset();
        }

        setModified(true);
    }

    //----------------//
    // getNeededSteps //
    //----------------//
    private EnumSet<Step> getNeededSteps (Step target)
    {
        EnumSet<Step> neededSteps = EnumSet.noneOf(Step.class);

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

            if (binarizationFilter != null) {
                binarizationFilter.setParent(book.getBinarizationFilter());
            }

            if (ocrLanguages != null) {
                ocrLanguages.setParent(book.getOcrLanguages());
            }

            if (switches != null) {
                switches.setParent(book.getProcessingSwitches());
            }

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

    //-------------------//
    // OcrSheetLanguages //
    //-------------------//
    private static final class OcrSheetLanguages
            extends Param<String>
    {
        //~ Methods --------------------------------------------------------------------------------

        @Override
        public boolean setSpecific (String specific)
        {
            // Normalize
            if ((specific != null) && specific.isEmpty()) {
                specific = null;
            }

            return super.setSpecific(specific);
        }

        //~ Inner Classes --------------------------------------------------------------------------
        /**
         * JAXB adapter to mimic XmlValue.
         */
        public static class Adapter
                extends XmlAdapter<String, OcrSheetLanguages>
        {
            //~ Methods ----------------------------------------------------------------------------

            @Override
            public String marshal (OcrSheetLanguages val)
                    throws Exception
            {
                if (val == null) {
                    return null;
                }

                return val.getSpecific();
            }

            @Override
            public OcrSheetLanguages unmarshal (String str)
                    throws Exception
            {
                OcrSheetLanguages ol = new OcrSheetLanguages();
                ol.setSpecific(str);

                return ol;
            }
        }
    }
}
