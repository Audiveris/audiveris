//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                    P a r t C o l l a t i o n                                   //
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
package org.audiveris.omr.score;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.sheet.SheetStub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Class <code>PartCollation</code> is in charge of collecting and combining parts across
 * systems and pages so that a part always represents the same instrument all along the score.
 * <p>
 * <p>
 * The strategy used to build LogicalPart's out of PartRef's is based on the following assumptions:
 * <ul>
 * <li>For a part of a system to be combined to a part of another system, they must exhibit the
 * same staves physical configuration:
 * <ol>
 * <li>Same count of staves
 * <li>Same count of lines in corresponding staves
 * <li>Same small attribute if any in corresponding staves
 * </ol>
 * <li>Parts cannot be swapped from one system to the other. In other words, we cannot have say
 * partA followed by partB in a system, and partB followed by partA in another system.</li>
 * <li>A part with 2 standard staves, likely to be the piano part if any, is used as a pivot to
 * align collations.</li>
 * <li>Otherwise, since additional parts appear at the top of a system, rather than at the bottom,
 * we process part collation bottom up.</li>
 * <li>When available, we use the part names (or abbreviations) to help the collation algorithm,
 * but this is questionable for lack of OCR reliability on part names and abbreviations.
 * </ul>
 *
 * @author Hervé Bitteur
 */
public class PartCollation
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    private static final Logger logger = LoggerFactory.getLogger(PartCollation.class);

    public static final List<StaffConfig> PIANO_CONFIG = StaffConfig.decodeCsv(
            constants.pianoStaffConfig.getValue());

    //~ Instance fields ----------------------------------------------------------------------------

    /**
     * The sorted list of logical parts, each with its affiliated physical parts.
     */
    private final List<Record> records = new ArrayList<>();

    /**
     * All logical names.
     */
    final Set<String> logicalNames = new LinkedHashSet<>();

    /**
     * Are the score LogicalPart's locked?.
     */
    private boolean logicalsLocked;

    //~ Constructors -------------------------------------------------------------------------------

    /**
     * Creates a new <code>PartCollation</code> object.
     *
     * @param sequences the list of sequences of parts
     * @param logicals  the pre-populated list of LogicalPart's, or null if un-locked
     */
    public PartCollation (List<List<PartRef>> sequences,
                          List<LogicalPart> logicals)
    {
        if (logicals != null) {
            logicalsLocked = true;

            // Allocate the records
            for (LogicalPart logical : logicals) {
                records.add(new Record(logical));

                final String logicalName = logical.getName();
                if (logicalName != null) {
                    logicalNames.add(logicalName);
                }
            }
        }

        collate(sequences);
    }

    //~ Methods ------------------------------------------------------------------------------------

    //-----------//
    // addRecord //
    //-----------//
    /**
     * Build a new Record based on provided PartRef and insert it at last position,
     * according to provided direction, in the list of records.
     *
     * @param dir     -1 for up, +1 for down
     * @param partRef the provided candidate partRef
     * @param records (output) the records to be augmented
     */
    private void addRecord (int dir,
                            PartRef partRef,
                            List<Record> records)
    {
        // Create a brand new logical part for this candidate part
        final LogicalPart logical = new LogicalPart(
                0, // This id indicates a just-created logical
                partRef.getStaffCount(),
                partRef.getStaffConfigs());
        logical.setName(partRef.getName());
        logical.setAbbreviation(null);
        logical.setStaffConfigs(partRef.getStaffConfigs());
        logger.debug("Created {} from {}", logical, partRef);

        final Record record = new Record(logical);
        record.partRefs.add(partRef);

        if (logical.getName() != null) {
            logicalNames.add(logical.getName());
        }

        // Insert at proper end of records
        records.add(dir < 0 ? 0 : records.size(), record);
    }

    //---------//
    // biIndex //
    //---------//
    /**
     * Report the index in provided PartRef's sequence of the (first) 2-staff PartRef.
     *
     * @param sequence sequence of partRef's
     * @return index of bi-staff part in sequence, or -1 if not found
     */
    private int biIndex (List<PartRef> sequence)
    {
        for (PartRef partRef : sequence) {
            if (partRef.getStaffCount() == 2) {
                return sequence.indexOf(partRef);
            }
        }

        return -1;
    }

    //---------//
    // collate //
    //---------//
    /**
     * The heart of the part collation algorithm, when we collate parts across several pages.
     *
     * @param sequences a list of sequences of candidate parts (one sequence = one system)
     */
    private void collate (List<List<PartRef>> sequences)
    {
        // Piano-like part record found, if any
        Record biRecord = null;

        // Process each sequence of candidate parts in turn
        // One such sequence = one system
        for (int iSeq = 0; iSeq < sequences.size(); iSeq++) {
            final List<PartRef> sequence = sequences.get(iSeq);
            final int biIndex = biIndex(sequence);

            // Respect manual assignments if any
            final Set<Record> manuals = new LinkedHashSet<>();
            for (PartRef partRef : sequence) {
                if (partRef.isManual()) {
                    final Record record = getRecord(partRef.getLogicalId());
                    if (record != null) {
                        manuals.add(record);
                        record.partRefs.add(partRef);
                    }
                }
            }

            if (iSeq == 0) {
                dispatch(sequence, records, +1, manuals);
            } else {
                if ((biRecord != null) && (biIndex != -1)) {
                    // Align on the biRecord
                    biRecord.partRefs.add(sequence.get(biIndex));

                    // Process parts above biRecord
                    dispatch(
                            sequence.subList(0, biIndex),
                            records.subList(0, records.indexOf(biRecord)),
                            -1,
                            manuals);

                    // Process parts below biRecord
                    dispatch(
                            sequence.subList(biIndex + 1, sequence.size()),
                            records.subList(records.indexOf(biRecord) + 1, records.size()),
                            +1,
                            manuals);
                } else {
                    dispatch(sequence, records, -1, manuals);
                }
            }

            if (biRecord == null) {
                biRecord = getBiRecord();
            }
        }

        if (!logicalsLocked) {
            // Assign ids to logical parts
            renumberRecords();
        }
    }

    //----------//
    // dispatch //
    //----------//
    /**
     * Dispatch the list of candidate PartRef's (the parts of a system) to the current list
     * of records.
     *
     * @param sequence the (sub-system) candidate parts to dispatch
     * @param records  the [sub-]list of records available
     * @param dir      -1 or +1 for browsing up or down
     * @param manuals  records already used by manual assignment
     */
    private void dispatch (List<PartRef> sequence,
                           List<Record> records,
                           int dir,
                           Set<Record> manuals)
    {
        final int ic1 = (dir > 0) ? 0 : (sequence.size() - 1); // Starting sequence index value
        final int ic2 = (dir > 0) ? sequence.size() : (-1); // Breaking sequence index value
        int recordIndex = (dir > 0) ? (-1) : records.size(); // Current index in defined records

        CandidateLoop:
        for (int ic = ic1; ic != ic2; ic += dir) {
            final PartRef partRef = sequence.get(ic);
            final List<StaffConfig> staffConfigs = partRef.getStaffConfigs();
            logger.debug("\nCandidate {}", partRef.toQualifiedString());

            if (partRef.isManual()) {
                continue; // Candidate part already assigned manually
            }

            // Check against defined records
            recordIndex += dir;

            for (; ((dir > 0) && (recordIndex < records.size())) || ((dir < 0)
                    && (recordIndex >= 0)); recordIndex += dir) {
                final Record record = records.get(recordIndex);

                if (manuals.contains(record)) {
                    continue; // Candidate part must skip this record
                }

                final LogicalPart logical = record.logical;
                logger.debug("  Comparing with {}", logical);

                // Check parts are compatible in terms of staves counts
                if (logical.getStaffCount() != partRef.getStaffCount()) {
                    logger.debug("   Staff count incompatibility");
                    continue;
                }

                // Check parts are compatible in terms of line counts
                // For backward compatibility w/ old OMRs, check is made only if line counts exist
                if (!staffConfigs.isEmpty() //
                        && !logical.getStaffConfigs().isEmpty() //
                        && !Objects.deepEquals(logical.getStaffConfigs(), staffConfigs)) {
                    logger.debug("    Line counts incompatibility");
                    continue;
                }

                // Check candidate name
                final String name = partRef.getName();
                if ((name != null) //
                        && !name.equalsIgnoreCase(logical.getName()) //
                        && !name.equalsIgnoreCase(logical.getAbbreviation())) {
                    logger.debug("    Name incompatibility");
                    continue;
                }

                logger.debug("    Success");
                record.partRefs.add(partRef);

                // Use name of affiliate part to define abbreviation of logical?
                final String resAbbrev = logical.getAbbreviation();
                if (resAbbrev == null) {
                    final String resName = logical.getName();
                    final String affiName = partRef.getName();
                    if ((affiName != null) //
                            && !affiName.equals(resName) //
                            && (resName == null || affiName.length() < logical.getName().length()) //
                            && !logicalNames.contains(affiName)) {
                        logical.setAbbreviation(affiName);
                    }
                }

                continue CandidateLoop;
            }

            logger.debug("  No more records available");

            if (!logicalsLocked) {
                addRecord(dir, partRef, records); // Create a brand new record for this candidate
            } else {
                logger.info("  Cannot map {} to any logical", partRef.toQualifiedString());
            }
        }
    }

    //-------------//
    // dumpRecords //
    //-------------//
    /**
     * Dump content of collated records.
     */
    public void dumpRecords ()
    {
        StringBuilder sb = new StringBuilder();

        for (Record record : records) {
            sb.append("\n").append(record.logical);

            for (PartRef partRef : record.partRefs) {
                final SystemRef system = partRef.getSystem();
                final int rank = 1 + system.getParts().indexOf(partRef);
                final PageRef page = system.getPage();
                final SheetStub stub = page.getStub();

                sb.append("\n   ") //
                        .append(partRef) //
                        .append(" rank:").append(rank) //
                        .append(" in ").append(stub) //
                        .append(", Page#").append(page.getId()) //
                        .append(", System#").append(system.getId());
            }
        }

        logger.info("PartCollation records:{}", sb);
    }

    //-------------//
    // getBiRecord //
    //-------------//
    /**
     * Report the (first) record, if any, among the current sequence of records, which could
     * be the piano part.
     *
     * @return the 2-staff piano record found or null
     */
    private Record getBiRecord ()
    {
        for (Record record : records) {
            final List<StaffConfig> configs = record.logical.getStaffConfigs();

            if (Objects.deepEquals(configs, PIANO_CONFIG)) {
                return record;
            }
        }

        return null;
    }

    //-----------//
    // getRecord //
    //-----------//
    /**
     * Report the Record for the provided logical ID.
     *
     * @param logicalId the provided logical id
     * @return the Record found or null
     */
    private Record getRecord (int logicalId)
    {
        for (Record record : records) {
            if (record.logical.getId() == logicalId) {
                return record;
            }
        }

        logger.warn("Cannot find logical for id {}", logicalId);
        return null;
    }

    //------------//
    // getRecords //
    //------------//
    /**
     * Report the collation records.
     *
     * @return the collation records
     */
    public List<Record> getRecords ()
    {
        return records;
    }

    //-----------------//
    // renumberRecords //
    //-----------------//
    /**
     * Renumber the records.
     */
    private void renumberRecords ()
    {
        for (int i = 0; i < records.size(); i++) {
            final Record record = records.get(i);
            final int id = i + 1;
            record.logical.setId(id);
            logger.debug("Final {}", record.logical);
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {
        private final Constant.String pianoStaffConfig = new Constant.String(
                "5,5",
                "Typical staff configuration for the piano part");
    }

    //--------//
    // Record //
    //--------//
    /**
     * Records a LogicalPart with its affiliated (physical) PartRef's.
     */
    public static class Record
    {
        /** Recording logical part. */
        final LogicalPart logical;

        /** Affiliated candidate parts. */
        final List<PartRef> partRefs = new ArrayList<>();

        public Record (LogicalPart logical)
        {
            this.logical = logical;
        }

        @Override
        public String toString ()
        {
            return new StringBuilder(getClass().getSimpleName()).append('{') //
                    .append(logical) //
                    .append(" affs:").append(partRefs.size())//
                    .append('}').toString();
        }
    }
}
