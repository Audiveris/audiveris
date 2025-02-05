//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h e e t S t u b                                       //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2025. All rights reserved.
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
import org.audiveris.omr.image.FilterDescriptor;
import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.image.ImageLoading;
import org.audiveris.omr.log.LogUtil;
import org.audiveris.omr.run.RunTable;
import org.audiveris.omr.score.Page;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.sheet.Params.SheetParams;
import org.audiveris.omr.sheet.Picture.ImageKey;
import org.audiveris.omr.sheet.Picture.TableKey;
import org.audiveris.omr.sheet.Profiles.InputQuality;
import static org.audiveris.omr.sheet.Profiles.InputQuality.Poor;
import static org.audiveris.omr.sheet.Profiles.InputQuality.Standard;
import static org.audiveris.omr.sheet.Profiles.InputQuality.Synthetic;
import static org.audiveris.omr.sheet.Sheet.INTERNALS_RADIX;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.sheet.ui.StubsController;
import org.audiveris.omr.step.OmrStep;
import org.audiveris.omr.step.ProcessingCancellationException;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.step.StepPause;
import org.audiveris.omr.step.ui.StepMonitoring;
import org.audiveris.omr.ui.Colors;
import org.audiveris.omr.ui.symbol.MusicFamily;
import org.audiveris.omr.ui.symbol.TextFamily;
import org.audiveris.omr.util.FileUtil;
import org.audiveris.omr.util.Jaxb;
import org.audiveris.omr.util.Memory;
import org.audiveris.omr.util.Navigable;
import org.audiveris.omr.util.OmrExecutors;
import org.audiveris.omr.util.StopWatch;
import org.audiveris.omr.util.Version;
import org.audiveris.omr.util.ZipFileSystem;
import org.audiveris.omr.util.param.IntegerParam;
import org.audiveris.omr.util.param.Param;
import org.audiveris.omr.util.param.StringParam;

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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Predicate;

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class <code>SheetStub</code> is a placeholder in a <code>Book</code> to
 * decouple the Book instance from the actual <code>Sheet</code> instance.
 * <p>
 * This avoids having to keep all sheets in memory, but rather load any needed sheet on demand.
 *
 * @author Hervé Bitteur
 */
@XmlAccessorType(XmlAccessType.NONE)
public class SheetStub
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(SheetStub.class);

    /** Predicate that tests stub validity. */
    public static final Predicate<SheetStub> VALIDITY_CHECK = (SheetStub stub) -> stub.isValid();

    //~ Instance fields ----------------------------------------------------------------------------

    // Persistent data
    //----------------

    /**
     * This is the rank of sheet, counted from 1, within the book.
     */
    @XmlAttribute(name = "number")
    private final int number;

    /**
     * Audiveris version that last operated on this particular sheet.
     * <p>
     * This version may be older than the version recorded at book level.
     */
    @XmlAttribute(name = "version")
    private volatile String versionValue;

    /**
     * Information about original image input file.
     * <p>
     * If this element is not present, the sheet input file is the book input file.
     */
    @XmlElement(name = "input")
    private SheetInput sheetInput;

    /**
     * If true, this boolean signals that the sheet contains no valid music
     * from OMR point of view.
     * <p>
     * This occurs mainly at the beginning or the end of a book, where some sheets may be left
     * blank or contain just illustrations.
     * <p>
     * Sheet invalidity can generally be detected during the <code>SCALE</code> step.
     * <p>
     * By default, this boolean is false and not present in the project XML data.
     */
    @XmlAttribute
    @XmlJavaTypeAdapter(type = boolean.class, value = Jaxb.BooleanPositiveAdapter.class)
    private boolean invalid;

    /**
     * All SheetStub parameters, editable via the BookParameters dialog.
     * This structure replaces the deprecated individual Param instances.
     */
    @XmlElement(name = "parameters")
    private SheetParams parameters;

    /**
     * This is the sequence of steps already performed on this sheet.
     */
    @XmlList
    @XmlElement(name = "steps")
    private final EnumSet<OmrStep> doneSteps = EnumSet.noneOf(OmrStep.class);

    /**
     * List of logical references to the pages contained in the corresponding sheet.
     */
    @XmlElement(name = "page")
    private final List<PageRef> pageRefs = new ArrayList<>();

    // Transient data
    //---------------

    /** Processing lock. */
    private final Lock lock = new ReentrantLock();

    /** Containing book. */
    @Navigable(false)
    private Book book;

    /** Full sheet material, if any. */
    private volatile Sheet sheet;

    /** The step being performed on the sheet. */
    private volatile OmrStep currentStep;

    /** Has this sheet been modified, WRT its persisted data. */
    private volatile boolean modified = false;

    /** Has this sheet been upgraded, WRT its persisted data. */
    private volatile boolean upgraded = false;

    /** Related assembly instance, if any. */
    private SheetAssembly assembly;

    /** A trick to keep parameters intact, even when nullified at marshal time. */
    private SheetParams parametersMirror;

    // Deprecated persistent data
    //---------------------------

    /** Deprecated, replaced by SheetStub parameters structure. */
    @Deprecated
    @XmlElement(name = "music-font")
    private MusicFamily.MyParam old_musicFamily;

    /** Deprecated, replaced by SheetStub parameters structure. */
    @Deprecated
    @XmlElement(name = "text-font")
    private TextFamily.MyParam old_textFamily;

    /** Deprecated, replaced by SheetStub parameters structure. */
    @Deprecated
    @XmlElement(name = "input-quality")
    private volatile InputQualityParam old_inputQuality;

    /** Deprecated, replaced by SheetStub parameters structure. */
    @Deprecated
    @XmlElement(name = "beam-specification")
    private IntegerParam old_beamSpecification;

    /** Deprecated, replaced by SheetStub parameters structure. */
    @Deprecated
    @XmlElement(name = "ocr-languages")
    private StringParam old_ocrLanguages;

    /** Deprecated, replaced by SheetStub parameters structure. */
    @Deprecated
    @XmlElement(name = "processing")
    private ProcessingSwitches old_switches;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * No-argument constructor meant for JAXB.
     */
    private SheetStub ()
    {
        this.number = 0;
    }

    /**
     * Creates a new <code>SheetStub</code> object.
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
     * Creates a new <code>SheetStub</code> object by replicating an old stub.
     *
     * @param book    the (new) containing book
     * @param number  the (1-based) stub in the new book
     * @param oldStub the old stub to copy from
     */
    public SheetStub (Book book,
                      int number,
                      SheetStub oldStub)
    {
        this.number = number;

        initTransients(book);

        // Copy data from oldStub
        versionValue = oldStub.versionValue;
        sheetInput = new SheetInput(oldStub.getSheetInput());
        invalid = oldStub.invalid;
        doneSteps.addAll(oldStub.doneSteps);
        pageRefs.addAll(oldStub.pageRefs); /// ??? TODO: To be checked
    }

    //~ Methods ------------------------------------------------------------------------------------

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

    //--------------//
    // afterMarshal //
    //--------------//
    @SuppressWarnings("unused")
    private void afterMarshal (Marshaller m)
    {
        parameters = parametersMirror.duplicate();
    }

    //---------------//
    // beforeMarshal //
    //---------------//
    @SuppressWarnings("unused")
    private void beforeMarshal (Marshaller m)
    {
        if ((parameters != null) && parameters.prune()) {
            parameters = null;
        }
    }

    //--------------//
    // checkSystems //
    //--------------//
    /**
     * Make sure system information exists in SheetStub.
     * <p>
     * This is meant to cope with old OMR versions in which SheetStub did not keep info about
     * systems and parts.
     */
    public void checkSystems ()
    {
        for (PageRef pageRef : pageRefs) {
            if (pageRef.getSystems().isEmpty()) {
                sheet = getSheet(); // Loading...
                final Page page = sheet.getPages().get(pageRef.getIndex());

                for (SystemInfo system : page.getSystems()) {
                    pageRef.addSystem(system.buildRef());
                }
            }
        }
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

    //---------//
    // display //
    //---------//
    private void display ()
    {
        if (OMR.gui != null) {
            try {
                Runnable runnable = () -> StubsController.getInstance().display(
                        SheetStub.this,
                        false);

                if (SwingUtilities.isEventDispatchThread()) {
                    runnable.run();
                } else {
                    SwingUtilities.invokeAndWait(runnable);
                }
            } catch (InterruptedException | InvocationTargetException ex) {
                logger.warn("Could not reset {}", ex.toString(), ex);
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
    public final void done (OmrStep step)
    {
        doneSteps.add(step);
    }

    //-----------//
    // doOneStep //
    //-----------//
    /**
     * Do just one specified step, synchronously, with display of related UI if any.
     * <p>
     * OmrStep duration is guarded by a timeout, so that processing cannot get blocked infinitely.
     *
     * @param step the step to perform
     * @throws Exception
     */
    private void doOneStep (final OmrStep step)
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
            future = OmrExecutors.getCachedLowExecutor().submit( () -> {
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

    //------------------//
    // getBarlineHeight //
    //------------------//
    /**
     * Report the barline height defined at sheet level.
     *
     * @return the barline height
     */
    public BarlineHeight getBarlineHeight ()
    {
        return getBarlineHeightParam().getValue();
    }

    //-----------------------//
    // getBarlineHeightParam //
    //-----------------------//
    /**
     * Report the barline height parameter defined at sheet level.
     *
     * @return the barline height parameter
     */
    public BarlineHeight.MyParam getBarlineHeightParam ()
    {
        return parameters.barlineSpecification;
    }

    //----------------------//
    // getBeamSpecification //
    //----------------------//
    public Integer getBeamSpecification ()
    {
        return getBeamSpecificationParam().getValue();
    }

    //---------------------------//
    // getBeamSpecificationParam //
    //---------------------------//
    public IntegerParam getBeamSpecificationParam ()
    {
        return parameters.beamSpecification;
    }

    //-----------------------//
    // getBinarizationFilter //
    //-----------------------//
    public FilterDescriptor getBinarizationFilter ()
    {
        return getBinarizationFilterParam().getValue();
    }

    //----------------------------//
    // getBinarizationFilterParam //
    //----------------------------//
    public FilterParam getBinarizationFilterParam ()
    {
        return parameters.binarizationFilter;
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
    public OmrStep getCurrentStep ()
    {
        return currentStep;
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

    //-----------------//
    // getInputQuality //
    //-----------------//
    /**
     * Report the input quality defined at sheet level.
     *
     * @return the input quality
     */
    public InputQuality getInputQuality ()
    {
        return getInputQualityParam().getValue();
    }

    //----------------------//
    // getInputQualityParam //
    //----------------------//
    /**
     * Report the input quality param defined at sheet level.
     *
     * @return the input quality parameter
     */
    public InputQualityParam getInputQualityParam ()
    {
        return parameters.inputQuality;
    }

    //---------------------------//
    // getInterlineSpecification //
    //---------------------------//
    public Integer getInterlineSpecification ()
    {
        return getInterlineSpecificationParam().getValue();
    }

    //--------------------------------//
    // getInterlineSpecificationParam //
    //--------------------------------//
    public IntegerParam getInterlineSpecificationParam ()
    {
        return parameters.interlineSpecification;
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
    public OmrStep getLatestStep ()
    {
        OmrStep latest = null;

        for (OmrStep step : OmrStep.values()) {
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

    //----------------//
    // getMusicFamily //
    //----------------//
    /**
     * Report the music family defined at sheet level.
     *
     * @return the music font family
     */
    public MusicFamily getMusicFamily ()
    {
        return getMusicFamilyParam().getValue();
    }

    //---------------------//
    // getMusicFamilyParam //
    //---------------------//
    /**
     * Report the music family param defined at sheet level.
     *
     * @return the music family parameter
     */
    public MusicFamily.MyParam getMusicFamilyParam ()
    {
        return parameters.musicFamily;
    }

    //----------------//
    // getNeededSteps //
    //----------------//
    private EnumSet<OmrStep> getNeededSteps (OmrStep target)
    {
        EnumSet<OmrStep> neededSteps = EnumSet.noneOf(OmrStep.class);

        // Add all needed steps
        for (OmrStep step : EnumSet.range(OmrStep.first(), target)) {
            if (!isDone(step)) {
                neededSteps.add(step);
            }
        }

        return neededSteps;
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
     * @return the OCR language(s) specification
     */
    public String getOcrLanguages ()
    {
        return getOcrLanguagesParam().getValue();
    }

    //----------------------//
    // getOcrLanguagesParam //
    //----------------------//
    /**
     * Report the OCR language(s) specification parameter defined at sheet level if any.
     *
     * @return the OCR language(s) specification parameter
     */
    public Param<String> getOcrLanguagesParam ()
    {
        return parameters.ocrLanguages;
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
        return parameters.switches;
    }

    //------------//
    // getProfile //
    //------------//
    /**
     * Report the processing profile for this stub, based on declared input quality.
     *
     * @return STRICT, STANDARD or POOR
     */
    public int getProfile ()
    {
        final InputQuality quality = getInputQuality();

        return switch (quality) {
            case Synthetic -> Profiles.STRICT;
            case Standard -> Profiles.STANDARD;
            case Poor -> Profiles.POOR;
        };
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
        if (sheet != null) {
            return sheet;
        }

        synchronized (this) {
            // We have to recheck sheet, which may have just been allocated
            if (sheet != null) {
                return sheet;
            }

            // Actually load the sheet
            if (!isDone(OmrStep.LOAD)) {
                // LOAD not yet performed: load from book image file
                try {
                    return sheet = new Sheet(this, null, false);
                } catch (StepException ignored) {
                    logger.info("Could not load sheet for stub {}", this);
                    return null;
                }
            }

            // LOAD already performed: unmarshall from book file
            if (SwingUtilities.isEventDispatchThread()) {
                logger.warn("XXX Unmarshalling .omr file on EDT XXXX");
            }

            final StopWatch watch = new StopWatch("Load Sheet " + this);

            try {
                final Path sheetFile;
                watch.start("unmarshal");

                // Open the book file system
                try {
                    book.getLock().lock();
                    sheetFile = book.openSheetFolder(number).resolve(
                            Sheet.getSheetFileName(number));

                    try (InputStream is = Files.newInputStream(
                            sheetFile,
                            StandardOpenOption.READ)) {
                        sheet = Sheet.unmarshal(is);
                    }

                    sheetFile.getFileSystem().close();
                } finally {
                    book.getLock().unlock();
                }

                // Complete sheet reload
                watch.start("afterReload");
                sheet.afterReload(this);
                setVersionValue(WellKnowns.TOOL_REF); // Sheet is now OK WRT tool version

                if (OMR.gui != null) {
                    StubsController.getInstance().markTab(
                            this,
                            invalid ? Colors.SHEET_INVALID : Colors.SHEET_OK);
                }

                if (constants.printWatch.isSet()) {
                    watch.print();
                }

                logger.info("Loaded {}", sheetFile);
            } catch (IOException | JAXBException ex) {
                logger.warn("Error in loading sheet structure " + ex, ex);
                logger.info("Trying to restart from binary");
                resetToBinary();
            }

            return sheet;
        }
    }

    //---------------//
    // getSheetInput //
    //---------------//
    /**
     * Report the precise image input for this sheet.
     *
     * @return the sheetInput
     */
    public SheetInput getSheetInput ()
    {
        if (sheetInput == null) {
            sheetInput = new SheetInput(book.getInputPath(), number);
        }

        return sheetInput;
    }

    //---------------//
    // getTextFamily //
    //---------------//
    /**
     * Report the text family defined at sheet level.
     *
     * @return the text family
     */
    public TextFamily getTextFamily ()
    {
        return getTextFamilyParam().getValue();
    }

    //--------------------//
    // getTextFamilyParam //
    //--------------------//
    /**
     * Report the text family param defined at sheet level.
     *
     * @return the text family parameter
     */
    public TextFamily.MyParam getTextFamilyParam ()
    {
        return parameters.textFamily;
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
    // grabBinaryTable //
    //-----------------//
    private RunTable grabBinaryTable ()
    {
        // Avoid loading sheet just to reset to binary:
        // If sheet is available, use its picture.getVerticalTable()
        // Otherwise, load binary image from disk and convert to RunTable
        RunTable binaryTable = null;

        if (hasSheet()) {
            logger.debug("Sheet#{} getting BINARY from sheet", number);
            binaryTable = getSheet().getPicture().getVerticalTable(TableKey.BINARY);
        }

        if (binaryTable == null) {
            logger.debug("Sheet#{} loading BINARY image from disk", number);
            final BufferedImage binaryImg = new ImageHolder(ImageKey.BINARY).getData(this);

            if (binaryImg != null) {
                logger.debug("Sheet#{} getting BINARY table from image", number);
                binaryTable = Picture.verticalTableOf(binaryImg);
            }
        }

        return binaryTable;
    }

    /**
     * Report whether the stub has a sheet in memory
     *
     * @return true if sheet is present in memory
     */
    public boolean hasSheet ()
    {
        return sheet != null;
    }

    //----------------//
    // initParameters //
    //----------------//
    /**
     * Make sure the parameters are properly allocated, but their parents not yet set.
     */
    public void initParameters ()
    {
        // Migrate old Params, if any
        migrateOldParams();

        // At this point in time, parameters contains only the params with specific value
        if (parameters == null) {
            parameters = new SheetParams();
        }

        parameters.completeParams();
        parameters.setScope(this);
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
    final void initTransients (Book book)
    {
        try {
            LogUtil.start(book);

            this.book = book;

            setParamParents(book);

            if (!isValid()) {
                doneSteps.removeIf( (s) -> s.compareTo(OmrStep.BINARY) > 0); // Safer for old .omr
            }

            if (OMR.gui != null) {
                assembly = new SheetAssembly(this);
            }
        } finally {
            LogUtil.stopBook();
        }
    }

    //------------//
    // invalidate //
    //------------//
    /**
     * Flag a stub as invalid (containing no music).
     */
    public void invalidate ()
    {
        invalid = true;

        doneSteps.removeIf( (s) -> s.compareTo(OmrStep.BINARY) > 0);
        pageRefs.clear();
        book.updateScores(this);
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
    public boolean isDone (OmrStep step)
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

    //---------------//
    // loadGrayImage //
    //---------------//
    /**
     * Actually load the image that corresponds to this sheet stub.
     *
     * @return the loaded sheet image
     */
    public synchronized BufferedImage loadGrayImage ()
    {
        try {
            final SheetInput si = getSheetInput();

            if (!Files.exists(si.path)) {
                logger.info("Input {} not found", si.path);

                return null;
            }

            final ImageLoading.Loader loader = ImageLoading.getLoader(si.path);

            if (loader == null) {
                return null;
            }

            final BufferedImage img = loader.getImage(si.number);
            logger.info(
                    "Loaded image #{} {}x{} from {}",
                    si.number,
                    img.getWidth(),
                    img.getHeight(),
                    si.path);

            loader.dispose();

            return img;
        } catch (IOException ex) {
            logger.warn("Error in SheetStub.loadGrayImage", ex);

            return null;
        }
    }

    //------------------//
    // migrateOldParams //
    //------------------//
    /**
     * If an old param exists, it is put into the parameters structure.
     */
    private void migrateOldParams ()
    {
        if (old_musicFamily != null) {
            upgradeParameters().musicFamily = old_musicFamily;
            old_musicFamily = null;
        }

        if (old_textFamily != null) {
            upgradeParameters().textFamily = old_textFamily;
            old_textFamily = null;
        }

        if (old_inputQuality != null) {
            upgradeParameters().inputQuality = old_inputQuality;
            old_inputQuality = null;
        }

        if (old_beamSpecification != null) {
            upgradeParameters().beamSpecification = old_beamSpecification;
            old_beamSpecification = null;
        }

        if (old_ocrLanguages != null) {
            upgradeParameters().ocrLanguages = old_ocrLanguages;
            old_ocrLanguages = null;
        }

        if (old_switches != null) {
            upgradeParameters().switches = old_switches;
            old_switches = null;
        }
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
    public boolean reachStep (OmrStep target,
                              boolean force)
    {
        final StubsController ctrl = (OMR.gui != null) ? StubsController.getInstance() : null;
        final StopWatch watch = new StopWatch("reachStep " + target);
        EnumSet<OmrStep> neededSteps = null;
        boolean ok = false;
        getLock().lock(); // Wait for completion of early processing if any
        logger.debug("reachStep got lock on {}", this);

        try {
            final OmrStep latestStep = getLatestStep();

            if (force && (target.compareTo(latestStep) <= 0)) {
                if (target.compareTo(OmrStep.BINARY) > 0) {
                    resetToBinary();
                } else {
                    resetToGray();
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

            for (final OmrStep step : neededSteps) {
                if (book.isPauseRequired()) {
                    throw new StepPause("Pause required");
                }

                watch.start(step.name());
                StepMonitoring.notifyMsg(step.toString());
                logger.debug("reachStep {} towards {}", step, target);
                doOneStep(step);
            }

            ok = true;
        } catch (StepPause sp) {
            ok = false;
            logger.info("Processing stopped.");
            throw sp;
        } catch (ProcessingCancellationException pce) {
            ok = false;
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
            BufferedImage img = loadGrayImage();
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
            final RunTable binaryTable = grabBinaryTable();

            if (binaryTable != null) {
                doReset();
                sheet = new Sheet(this, binaryTable);
                logger.info("Sheet#{} reset to binary.", number);
                display();
            } else {
                logger.warn("No binary table available for sheet #{}", number);
            }
        } catch (Throwable ex) {
            logger.warn("Sheet#{} could not reset to binary {}", number, ex.toString(), ex);
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
            final BufferedImage img = loadGrayImage();

            if (img != null) {
                doReset();
                sheet = new Sheet(this, img, true);
                logger.info("Sheet#{} reset to gray.", number);
                display();
            } else {
                logger.warn("No gray image available for sheet #{}", number);
            }
        } catch (Throwable ex) {
            logger.warn("Sheet#{} could not reset to gray {}", number, ex.toString(), ex);
        }
    }

    //----------------//
    // setCurrentStep //
    //----------------//
    /**
     * Assign the step being performed.
     *
     * @param step the current step
     */
    public void setCurrentStep (OmrStep step)
    {
        currentStep = step;
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

    //-----------------//
    // setParamParents //
    //-----------------//
    /**
     * Connect every stub parameter to proper book parameter.
     */
    private void setParamParents (Book book)
    {
        // 1/ Make sure parameters are available
        initParameters();

        // 2/ set parents
        parameters.setParents(book);

        // 3/ set parametersMirror
        parametersMirror = parameters.duplicate();
    }

    //---------------//
    // setSheetInput //
    //---------------//
    /**
     * Assigns the precise image input for this sheet.
     *
     * @param sheetInput the sheetInput to set
     */
    public void setSheetInput (SheetInput sheetInput)
    {
        this.sheetInput = new SheetInput(sheetInput);
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
            SwingUtilities.invokeLater( () -> {
                final StubsController controller = StubsController.getInstance();
                final SheetStub stub = controller.getSelectedStub();

                if ((stub == SheetStub.this)) {
                    controller.refresh();
                }
            });
        }
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
                SwingUtilities.invokeLater( () -> {
                    // Gray out the related tab
                    StubsController ctrl = StubsController.getInstance();
                    ctrl.markTab(SheetStub.this, Colors.SHEET_NOT_LOADED);

                    // Close stub UI, if any
                    if (assembly != null) {
                        assembly.reset();
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
        return reachStep(OmrStep.last(), false);
    }

    //-------------------//
    // upgradeParameters //
    //-------------------//
    /**
     * Get/create the parameters structure for upgrading.
     *
     * @return the existing or created structure
     */
    private SheetParams upgradeParameters ()
    {
        setUpgraded(true);

        if (parameters == null) {
            parameters = new SheetParams();
        }

        return parameters;
    }

    //----------//
    // validate //
    //----------//
    /**
     * Flag a stub as valid (containing music).
     */
    public void validate ()
    {
        resetToBinary();

        book.updateScores(this);
        setModified(true);

        if (OMR.gui != null) {
            StubsController.getInstance().markTab(this, Colors.SHEET_OK);
        }

        logger.info("Sheet {} flagged as valid.", getId());
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.Boolean printWatch = new Constant.Boolean(
                false,
                "Should we print out the stop watch");
    }

    //------------//
    // SheetInput //
    //------------//
    /**
     * Class <code>SheetInput</code> records reference of input image file for a sheet.
     */
    @XmlAccessorType(XmlAccessType.NONE)
    public static class SheetInput
    {
        /**
         * This is the path to the image file this sheet was created from.
         */
        @XmlElement(name = "path")
        @XmlJavaTypeAdapter(Jaxb.PathAdapter.class)
        public final Path path;

        /**
         * This is the rank, counted from 1, of the sheet image within the input file.
         */
        @XmlElement(name = "number")
        public final int number;

        /** No-argument constructor needed for JAXB. */
        private SheetInput ()
        {
            path = null;
            number = 0;
        }

        public SheetInput (Path path,
                           int number)
        {
            this.path = path;
            this.number = number;
        }

        public SheetInput (SheetInput input)
        {
            this(input.path, input.number);
        }

        @Override
        public String toString ()
        {
            return FileUtil.getNameSansExtension(path) + "#" + number;
        }
    }
}
