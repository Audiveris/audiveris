//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        S h e e t S t u b                                       //
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

import org.audiveris.omr.image.FilterParam;
import org.audiveris.omr.score.PageRef;
import org.audiveris.omr.sheet.ui.SheetAssembly;
import org.audiveris.omr.step.Step;
import org.audiveris.omr.step.StepException;
import org.audiveris.omr.util.param.Param;

import java.util.List;
import java.util.concurrent.locks.Lock;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Class {@code SheetStub} represents a placeholder in a {@link Book} to decouple the
 * Book instance from the actual {@link Sheet} instances and avoid loading all of them
 * in memory.
 *
 * Methods are organized as follows:
 * <dl>
 * <dt>Administration</dt>
 * <dd><ul>
 * <li>{@link #getId}</li>
 * <li>{@link #getBook}</li>
 * <li>{@link #getNum}</li>
 * <li>{@link #getNumber}</li>
 * <li>{@link #hasSheet}</li>
 * <li>{@link #getSheet}</li>
 * <li>{@link #swapSheet}</li>
 * <li>{@link #decideOnRemoval}</li>
 * <li>{@link #isModified}</li>
 * <li>{@link #setModified}</li>
 * <li>{@link #close}</li>
 * <li>{@link #getLock}</li>
 * </ul></dd>
 *
 * <dt>Pages</dt>
 * <dd><ul>
 * <li>{@link #addPageRef}</li>
 * <li>{@link #clearPageRefs}</li>
 * <li>{@link #getFirstPageRef}</li>
 * <li>{@link #getLastPageRef}</li>
 * <li>{@link #getPageRefs}</li>
 * </ul></dd>
 *
 * <dt>Parameters</dt>
 * <dd><ul>
 * <li>{@link #getBinarizationFilter}</li>
 * <li>{@link #getOcrLanguages}</li>
 * <li>{@link #getProcessingSwitches}</li>
 * </ul></dd>
 *
 * <dt>Transcription</dt>
 * <dd><ul>
 * <li>{@link #reset}</li>
 * <li>{@link #resetToBinary}</li>
 * <li>{@link #reachStep}</li>
 * <li>{@link #getCurrentStep}</li>
 * <li>{@link #getLatestStep}</li>
 * <li>{@link #transcribe}</li>
 * <li>{@link #isDone}</li>
 * <li>{@link #isValid}</li>
 * <li>{@link #invalidate}</li>
 * </ul></dd>
 *
 * <dt>Artifacts</dt>
 * <dd><ul>
 * <li>{@link #storeSheet}</li>
 * </ul></dd>
 *
 * <dt>UI</dt>
 * <dd><ul>
 * <li>{@link #getAssembly}</li>
 * </ul></dd>
 * </dl>
 *
 * @author Hervé Bitteur
 */
@XmlJavaTypeAdapter(BasicStub.Adapter.class)
public interface SheetStub
{
    //~ Methods ------------------------------------------------------------------------------------

    /**
     * Add a page reference to this stub.
     *
     * @param pageRef the page reference
     */
    void addPageRef (PageRef pageRef);

    /**
     * Empty the collection of page references.
     */
    void clearPageRefs ();

    /**
     * Close this stub, and remove it from the containing book.
     */
    void close ();

    /**
     * An abnormal situation has been found, as detailed in provided message,
     * now how should we proceed?, depending on batch mode or user answer.
     *
     * @param msg   the problem description
     * @param dummy true for a dummy (positive) decision
     * @throws StepException thrown when processing must stop
     */
    void decideOnRemoval (String msg,
                          boolean dummy)
            throws StepException;

    /**
     * In non batch mode, report the related SheetAssembly for GUI
     *
     * @return the stub UI assembly, or null in batch mode
     */
    SheetAssembly getAssembly ();

    /**
     * Report the binarization filter defined at sheet level.
     *
     * @return the filter parameter
     */
    FilterParam getBinarizationFilter ();

    /**
     * Report the containing book.
     *
     * @return containing book
     */
    Book getBook ();

    /**
     * Report the step being processed, if any.
     *
     * @return the current step or null
     */
    Step getCurrentStep ();

    /**
     * Report the first page ref in stub
     *
     * @return first page ref or null
     */
    PageRef getFirstPageRef ();

    /**
     * Report the distinguished name for this sheet stub.
     *
     * @return sheet (stub) name
     */
    String getId ();

    /**
     * Report the last page ref in stub
     *
     * @return last page ref or null
     */
    PageRef getLastPageRef ();

    /**
     * Report the latest step done so far on this sheet.
     *
     * @return the latest step done, or null
     */
    Step getLatestStep ();

    /**
     * Report the lock that protects stub processing.
     *
     * @return stub processing lock
     */
    Lock getLock ();

    /**
     * Report the number string for this sheet in containing book
     *
     * @return "#n" for a multi-sheet book, "" otherwise
     */
    String getNum ();

    /**
     * Report the number for this sheet in containing book
     *
     * @return the sheet index number (1-based) in containing book
     */
    int getNumber ();

    /**
     * Report the OCR language(s) specification defined at sheet level if any.
     *
     * @return the OCR language(s) spec
     */
    Param<String> getOcrLanguages ();

    /**
     * Report the stub sequence of page references.
     *
     * @return the page ref 's
     */
    List<PageRef> getPageRefs ();

    /**
     * Report the processing switches defined at sheet level if any.
     *
     * @return sheet switches
     */
    ProcessingSwitches getProcessingSwitches ();

    /**
     * Make sure the sheet material is in memory.
     *
     * @return the sheet ready to use
     */
    Sheet getSheet ();

    /**
     * Report whether the stub has a sheet in memory
     *
     * @return true if sheet is present in memory
     */
    boolean hasSheet ();

    /**
     * Flag a stub as invalid (containing no music).
     */
    void invalidate ();

    /**
     * Report whether the specified step has been performed on this sheet
     *
     * @param step the step to check
     * @return true if already performed
     */
    boolean isDone (Step step);

    /**
     * Has the sheet been modified with respect to its book data?.
     *
     * @return true if modified
     */
    boolean isModified ();

    /**
     * Report whether this sheet is valid music.
     *
     * @return true if valid, false if invalid
     */
    boolean isValid ();

    /**
     * Make sure the provided step has been reached on this sheet stub.
     *
     * @param step  the step to check
     * @param force if true and step already reached, stub is reset and processed until step
     * @return true if OK
     */
    boolean reachStep (Step step,
                       boolean force);

    /**
     * Reset this stub to its initial state (that is valid and non-processed).
     */
    void reset ();

    /**
     * Reset this stub to end of its ANNOTATIONS step.
     */
    void resetToAnnotations ();

    /**
     * Reset this stub to end of its BINARY step.
     */
    void resetToBinary ();

    /**
     * Set the modified flag.
     *
     * @param modified the new flag value
     */
    void setModified (boolean modified);

    /**
     * Store sheet material into book.
     *
     * @throws Exception if storing fails
     */
    void storeSheet ()
            throws Exception;

    /**
     * Swap sheet material.
     * If modified and not discarded, sheet material will be stored before being disposed of.
     */
    void swapSheet ();

    /**
     * Convenient method to reach last step on this stub.
     * Defined as reachStep(Step.last(), false);
     *
     * @return true if OK
     */
    boolean transcribe ();
}
