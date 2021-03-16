//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h e e t S t u b                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2021. All rights reserved.
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
import org.audiveris.omr.WellKnowns;
import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.sheet.Picture.TableKey;
import static org.audiveris.omr.sheet.Sheet.INTERNALS_RADIX;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.StepPause;
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
import org.audiveris.omr.util.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
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

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBException;
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
 * Class {@code SheetStub} represents a placeholder in a {@link Book} to decouple the
 * Book instance from the actual {@link Sheet} instances and avoid loading all of them
 * in memory.
 * <p>
 * Methods are organized as follows:
 * <dl>
 * <dt>Administration</dt>
 * <dd>
 * <ul>
 * <li>{@link #getId}</li>
 * <li>{@link #getBook}</li>
 * <li>{@link #getNum}</li>
 * <li>{@link #getNumber}</li>
 * <li>{@link #hasSheet}</li>
 * <li>{@link #getSheet}</li>
 * <li>{@link #swapSheet}</li>
 * <li>{@link #decideOnRemoval}</li>
 * <li>{@link #setModified}</li>
 * <li>{@link #isModified}</li>
 * <li>{@link #close}</li>
 * <li>{@link #getLock}</li>
 * <li>{@link #storeSheet}</li>
 * </ul>
 * </dd>
 * <dt>Pages</dt>
 * <dd>
 * <ul>
 * <li>{@link #addPageRef}</li>
 * <li>{@link #clearPageRefs}</li>
 * <li>{@link #getFirstPageRef}</li>
 * <li>{@link #getLastPageRef}</li>
 * <li>{@link #getPageRefs}</li>
 * </ul>
 * </dd>
 * <dt>Parameters</dt>
 * <dd>
 * <ul>
 * <li>{@link #getBinarizationFilter}</li>
 * <li>{@link #getOcrLanguages}</li>
 * <li>{@link #getProcessingSwitches}</li>
 * </ul>
 * </dd>
 * <dt>Transcription</dt>
 * <dd>
 * <ul>
 * <li>{@link #reset}</li>
 * <li>{@link #resetToBinary}</li>
 * <li>{@link #reachStep}</li>
 * <li>{@link #getCurrentStep}</li>
 * <li>{@link #getLatestStep}</li>
 * <li>{@link #transcribe}</li>
 * <li>{@link #done}</li>
 * <li>{@link #isDone}</li>
 * <li>{@link #invalidate}</li>
 * <li>{@link #isValid}</li>
 * </ul>
 * </dd>
 * <dt>UI</dt>
 * <dd>
 * <ul>
 * <li>{@link #getAssembly}</li>
 * </ul>
 * </dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class SheetStub
{

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetStub.class);

    // Persistent data
    //----------------
    //
    /** Index of sheet, counted from 1, in the image file. */
    @XmlAttribute(name = "number")
    private final int number;

    /** Related Audiveris version that last operated on this sheet. */
    @XmlAttribute(name = "version")
    private volatile String versionValue;

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
    private final List<PageRef> pageRefs = new ArrayList<>();

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

    /** Has this sheet been modified, WRT its persisted data. */
    private volatile boolean modified = false;

    /** Has this sheet been upgraded, WRT its persisted data. */
    private volatile boolean upgraded = false;

    /** Related assembly instance, if any. */
    private SheetAssembly assembly;

    /**
     * Creates a new {@code SheetStub} object.
     *
     * @param book   the containing book instance
     * @param number the 1-based sheet number within the containing book
     */
    public SheetStub (Book book,
                      int number)
    {
        this.number = number;

        initTransients(book);
    }

    /**
     * No-arg constructor meant for JAXB.
     */
    private SheetStub ()
    {
        this.number = 0;
    }

    //------------//
    // addPageRef //
    //------------//
    /**
     * Add a page reference to this stub.
     *
     * @param pageRef the page reference
     */
    public void addPageRef (PageRef pageRef)
    {
        pageRefs.add(pageRef);
    }

    //------------//
    // addPageRef //
    //------------//
    /**
     * Add a page reference to this stub at the provided index.
     *
     * @param index   the provided index
     * @param pageRef the page reference
     */
    public void addPageRef (int index,
                            PageRef pageRef)
    {
        pageRefs.add(index, pageRef);
    }

    //---------------//
    // clearPageRefs //
    //---------------//
    /**
     * Empty the collection of page references.
     */
    public void clearPageRefs ()
    {
        // Clear pageRefs in this stub
        pageRefs.clear();

        // Clear pageRefs in related book store if any
    }

    //-------//
    // close //
    //-------//
    /**
     * Close this stub, and remove it from the containing book.
     */
    public void close ()
    {
        // If no stub is left, force book closing
        if (!book.isClosing()) {
            if (!book.getStubs().isEmpty()) {
                logger.info("Sheet closed");
            } else {
                book.close(null);
            }
        }
    }

    //-----------------//
    // decideOnRemoval //
    //-----------------//
    /**
     * An abnormal situation has been found, as detailed in provided message,
     * now how should we proceed?, depending on batch mode or user answer.
     *
     * @param msg   the problem description
     * @param dummy true for a dummy (positive) decision
     * @throws StepException thrown when processing must stop
     */
    public void decideOnRemoval (String msg,
                                 boolean dummy)
            throws StepException
    {
        if (dummy) {
            invalidate();
            throw new StepException("Dummy decision");
        }

        logger.warn(msg.replaceAll(WellKnowns.LINE_SEPARATOR, " "));

        if (OMR.gui != null) {
            StubsController.invokeSelect(this);
        }

        if ((OMR.gui == null) || (OMR.gui.displayConfirmation(
                msg + WellKnowns.LINE_SEPARATOR + "OK for discarding this sheet?"))) {
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
    /**
     * In non batch mode, report the related SheetAssembly for GUI
     *
     * @return the stub UI assembly, or null in batch mode
     */
    public SheetAssembly getAssembly ()
    {
        return assembly;
    }

    //-----------------------//
    // getBinarizationFilter //
    //-----------------------//
    /**
     * Report the binarization filter defined at sheet level.
     *
     * @return the filter parameter
     */
    public FilterParam getBinarizationFilter ()
    {
        if (binarizationFilter == null) {
            binarizationFilter = new FilterParam();
            binarizationFilter.setParent(book.getBinarizationFilter());
        }

        return binarizationFilter;
    }

    //---------//
    // getBook //
    //---------//
    /**
     * Report the containing book.
     *
     * @return containing book
     */
    public Book getBook ()
    {
        return book;
    }

    //----------------//
    // getCurrentStep //
    //----------------//
    /**
     * Report the step being processed, if any.
     *
     * @return the current step or null
     */
    public Step getCurrentStep ()
    {
        return currentStep;
    }

    //----------------//
    // setCurrentStep //
    //----------------//
    /**
     * Assign the step being performed.
     *
     * @param step the current step
     */
    public void setCurrentStep (Step step)
    {
        currentStep = step;
    }

    //-----------------//
    // getFirstPageRef //
    //-----------------//
    /**
     * Report the first page ref in stub
     *
     * @return first page ref or null
     */
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
    /**
     * Report the distinguished name for this sheet stub.
     *
     * @return sheet (stub) name
     */
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
    /**
     * Report the last page ref in stub
     *
     * @return last page ref or null
     */
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
    /**
     * Report the latest step done so far on this sheet.
     *
     * @return the latest step done, or null
     */
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
    /**
     * Report the lock that protects stub processing.
     *
     * @return stub processing lock
     */
    public Lock getLock ()
    {
        return lock;
    }

    //--------//
    // getNum //
    //--------//
    /**
     * Report the number string for this sheet in containing book
     *
     * @return "#n" for a multi-sheet book, "" otherwise
     */
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
    /**
     * Report the number for this sheet in containing book
     *
     * @return the sheet index number (1-based) in containing book
     */
    public int getNumber ()
    {
        return number;
    }

    //-----------------//
    // getOcrLanguages //
    //-----------------//
    /**
     * Report the OCR language(s) specification defined at sheet level if any.
     *
     * @return the OCR language(s) spec
     */
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
     * Report the stub sequence of page references.
     *
     * @return the page ref 's
     */
    public List<PageRef> getPageRefs ()
    {
        return pageRefs;
    }

    //-----------------------//
    // getProcessingSwitches //
    //-----------------------//
    /**
     * Report the processing switches defined at sheet level if any.
     *
     * @return sheet switches
     */
    public ProcessingSwitches getProcessingSwitches ()
    {
        if (switches == null) {
            switches = new ProcessingSwitches();
            switches.setParent(book.getProcessingSwitches());
        }

        return switches;
    }

    //------------//
    // getProfile //
    //------------//
    /**
     * Report the processing profile for this stub, based on poor switch.
     *
     * @return 1 (for poor), 0 (default)
     */
    public int getProfile ()
    {
        return getProcessingSwitches().getValue(ProcessingSwitches.Switch.poorInputMode) ? 1 : 0;
    }

    //----------//
    // getSheet //
    //----------//
    /**
     * Make sure the sheet material is in memory.
     *
     * @return the sheet ready to use
     */
    public Sheet getSheet ()
    {
        Sheet sh = this.sheet;

        if (sh == null) {
            synchronized (this) {
                sh = this.sheet;
                // We have to recheck sheet, which may have just been allocated
                if (sh == null) {
                    // Actually load the sheet
                    if (!isDone(Step.LOAD)) {
                        // LOAD not yet performed: load from book image file
                        try {
                            this.sheet = sh = new Sheet(this, (BufferedImage) null, false);
                        } catch (StepException ignored) {
                            logger.info("Could not load sheet for stub {}", this);
                        }
                    } else {
                        // LOAD already performed: load from book file
                        if (SwingUtilities.isEventDispatchThread()) {
                            logger.warn("XXX Loading .omr file on EDT XXXX");
                        }

                        StopWatch watch = new StopWatch("Load Sheet " + this);

                        try {
                            Path sheetFile = null;
                            watch.start("unmarshal");

                            // Open the book file system
                            try {
                                book.getLock().lock();
                                sheetFile = book.openSheetFolder(number).resolve(
                                        Sheet.getSheetFileName(number));

                                try (InputStream is = Files.newInputStream(
                                        sheetFile,
                                        StandardOpenOption.READ)) {
                                    this.sheet = sh = Sheet.unmarshal(is);
                                }

                                sheetFile.getFileSystem().close();
                            } finally {
                                book.getLock().unlock();
                            }

                            // Complete sheet reload
                            watch.start("afterReload");
                            sh.afterReload(this);
                            setVersionValue(WellKnowns.TOOL_REF); // Sheet is now OK WRT tool version
                            logger.info("Loaded {}", sheetFile);
                        } catch (IOException |
                                 JAXBException ex) {
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

        return sh;
    }

    //----------//
    // hasSheet //
    //----------//
    /**
     * Report whether the stub has a sheet in memory
     *
     * @return true if sheet is present in memory
     */
    public boolean hasSheet ()
    {
        return sheet != null;
    }

    //------------//
    // invalidate //
    //------------//
    /**
     * Flag a stub as invalid (containing no music).
     */
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
    /**
     * Report whether the specified step has been performed on this sheet
     *
     * @param step the step to check
     * @return true if already performed
     */
    public boolean isDone (Step step)
    {
        return doneSteps.contains(step);
    }

    //------------//
    // isModified //
    //------------//
    /**
     * Has the sheet been modified with respect to its persisted data?.
     *
     * @return true if modified
     */
    public boolean isModified ()
    {
        return modified;
    }

    //-------------//
    // setModified //
    //-------------//
    /**
     * Set the modified flag.
     *
     * @param modified the new flag value
     */
    public void setModified (boolean modified)
    {
        this.modified = modified;

        if (modified) {
            book.setModified(true);
            book.setDirty(true);
        }
    }

    //------------//
    // isUpgraded //
    //------------//
    /**
     * Has the sheet been upgraded with respect to its persisted data?.
     *
     * @return true if upgraded
     */
    public boolean isUpgraded ()
    {
        return upgraded;
    }

    //-------------//
    // setUpgraded //
    //-------------//
    /**
     * Set the upgraded flag.
     *
     * @param upgraded the new flag value
     */
    public void setUpgraded (boolean upgraded)
    {
        this.upgraded = upgraded;

        if (OMR.gui != null) {
            SwingUtilities.invokeLater(new Runnable()
            {
                @Override
                public void run ()
                {
                    final StubsController controller = StubsController.getInstance();
                    final SheetStub stub = controller.getSelectedStub();

                    if ((stub == SheetStub.this)) {
                        controller.refresh();
                    }
                }
            });
        }
    }

    //---------//
    // isValid //
    //---------//
    /**
     * Report whether this sheet is valid music.
     *
     * @return true if valid, false if invalid
     */
    public boolean isValid ()
    {
        return !invalid;
    }

    //------------//
    // getVersion //
    //------------//
    /**
     * Report the sheet version, specific if any, otherwise the book version
     *
     * @return the sheet version, specific or not.
     */
    public Version getVersion ()
    {
        if (versionValue == null) {
            return book.getVersion();
        }

        return new Version(versionValue);
    }

    //-----------------//
    // getVersionValue //
    //-----------------//
    /**
     * Report the specific sheet version, if any.
     *
     * @return the version, perhaps null
     */
    public String getVersionValue ()
    {
        return versionValue;
    }

    //-----------------//
    // setVersionValue //
    //-----------------//
    /**
     * Set a specific software version for this sheet.
     *
     * @param value the version value to set
     */
    public void setVersionValue (String value)
    {
        this.versionValue = value;
    }

    //-----------//
    // reachStep //
    //-----------//
    /**
     * Make sure the provided step has been reached on this sheet stub.
     * <p>
     * Each needed step is performed sequentially, guarded by a timeout.
     *
     * @param target the step to check
     * @param force  if true and step already reached, stub is reset and processed until step
     * @return true if OK, false if not OK (including when a step paused)
     */
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
                if (target.compareTo(Step.BINARY) > 0) {
                    resetToBinary();
                } else if (target == Step.BINARY) {
                    resetToGray();
                } else {
                    reset();
                }
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
            if (ex.getCause() instanceof StepPause) {
                logger.info("Processing stopped. Cause: {}", ex.getCause().getMessage());
                ok = false;
            } else if (ex.getCause() instanceof StepException) {
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

    //---------------//
    // removePageRef //
    //---------------//
    /**
     * Remove the provided PageRef.
     *
     * @param pageRef the page ref to remove
     */
    public void removePageRef (PageRef pageRef)
    {
        pageRefs.remove(pageRef);
    }

    //-------//
    // reset //
    //-------//
    /**
     * Reset this stub to its initial state (that is valid and non-processed).
     */
    public void reset ()
    {
        doReset();

        try {
            BufferedImage img = book.loadSheetImage(number);
            sheet = new Sheet(this, img, true);
            logger.info("Sheet#{} reset as valid.", number);
            display();
        } catch (Exception ex) {
            logger.warn("Error reloading image for sheet#{} {}", number, ex.toString(), ex);
        }
    }

    //---------------//
    // resetToBinary //
    //---------------//
    /**
     * Reset this stub to the binary image.
     */
    public void resetToBinary ()
    {
        try {
            final BufferedImage gray = sheet.getPicture().getGrayImage();
            final RunTable binaryTable = grabBinaryTable();

            doReset();
            sheet = new Sheet(this, binaryTable);

            if (gray != null) {
                sheet.setImage(gray, false);
            }

            logger.info("Sheet#{} reset to binary.", number);
            display();
        } catch (Throwable ex) {
            logger.warn("Could not reset to binary {}", ex.toString(), ex);
        }
    }

    //-------------//
    // resetToGray //
    //-------------//
    /**
     * Reset this stub to the gray image (result of LOAD step).
     */
    public void resetToGray ()
    {
        try {
            final BufferedImage gray = sheet.getPicture().getGrayImage();

            if (gray != null) {
                doReset();
                sheet = new Sheet(this, gray, false);
                logger.info("Sheet#{} reset to gray.", number);
                display();
            } else {
                logger.warn("No gray image available for sheet #{}", number);
            }
        } catch (Throwable ex) {
            logger.warn("Could not reset to gray {}", ex.toString(), ex);
        }
    }

    //---------//
    // display //
    //---------//
    private void display ()
    {
        if (OMR.gui != null) {
            try {
                Runnable runnable = new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        StubsController.getInstance().display(SheetStub.this, false);
                    }
                };

                if (SwingUtilities.isEventDispatchThread()) {
                    runnable.run();
                } else {
                    SwingUtilities.invokeAndWait(runnable);
                }
            } catch (InterruptedException |
                     InvocationTargetException ex) {
                logger.warn("Could not reset {}", ex.toString(), ex);
            }
        }
    }

    //------------//
    // storeSheet //
    //------------//
    /**
     * Store sheet material into book.
     *
     * @throws Exception if storing fails
     */
    public void storeSheet ()
            throws Exception
    {
        if (isModified() || isUpgraded()) {
            final Lock bookLock = book.getLock();
            bookLock.lock();

            try {
                Path bookPath = BookManager.getDefaultSavePath(book);
                Path root = ZipFileSystem.open(bookPath);
                book.storeBookInfo(root); // Book info (book.xml)

                Path sheetFolder = root.resolve(INTERNALS_RADIX + getNumber());
                sheet.store(sheetFolder, null);
                root.getFileSystem().close();
            } finally {
                bookLock.unlock();
            }
        }
    }

    //-----------//
    // swapSheet //
    //-----------//
    /**
     * Swap sheet material.
     * <p>
     * If modified or upgraded, sheet material will be stored before being disposed of.
     */
    public void swapSheet ()
    {
        try {
            if (isModified() || isUpgraded()) {
                logger.info("{} storing", this);
                storeSheet();
            }

            if (sheet != null) {
                logger.info("Disposed sheet{}", sheet.getStub().getNum());
                sheet = null;
                Memory.gc(); // Trigger a garbage collection...
            }

            if (OMR.gui != null) {
                SwingUtilities.invokeLater(new Runnable()
                {
                    @Override
                    public void run ()
                    {
                        // Gray out the related tab
                        StubsController ctrl = StubsController.getInstance();
                        ctrl.markTab(SheetStub.this, Colors.SHEET_NOT_LOADED);

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
    /**
     * Convenient method to reach last step on this stub.
     * Defined as reachStep(Step.last(), false);
     *
     * @return true if OK
     */
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
    @SuppressWarnings("unused")
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
            future = OmrExecutors.getCachedLowExecutor().submit(new Callable<Void>()
            {
                @Override
                public Void call ()
                        throws Exception
                {
                    LogUtil.start(SheetStub.this);

                    try {
                        setCurrentStep(step);
                        setModified(true); // At beginning of processing
                        sheet.reset(step); // Reset sheet relevant data

                        try {
                            step.doit(sheet); // Standard processing on an existing sheet
                            done(step); // Full completion
                            StepMonitoring.notifyStep(SheetStub.this, step);
                        } catch (StepPause sp) {
                            done(step);
                            StepMonitoring.notifyStep(SheetStub.this, step);
                            throw sp;
                        }
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

    //-----------------//
    // grabBinaryTable //
    //-----------------//
    private RunTable grabBinaryTable ()
    {
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

        return binaryTable;
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

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch for sheet loading");
    }

    //-------------------//
    // OcrSheetLanguages //
    //-------------------//
    private static class OcrSheetLanguages
            extends Param<String>
    {

        @Override
        public boolean setSpecific (String specific)
        {
            // Normalize
            if ((specific != null) && specific.isEmpty()) {
                specific = null;
            }

            return super.setSpecific(specific);
        }

        /**
         * JAXB adapter to mimic XmlValue.
         */
        public static class Adapter
                extends XmlAdapter<String, OcrSheetLanguages>
        {

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
