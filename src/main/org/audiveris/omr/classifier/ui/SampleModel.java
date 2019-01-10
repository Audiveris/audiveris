//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                      S a m p l e M o d e l                                     //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
// <editor-fold defaultstate="collapsed" desc="hdr">
//
//  Copyright © Audiveris 2019. All rights reserved.
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
package org.audiveris.omr.classifier.ui;

import org.audiveris.omr.classifier.Sample;
import org.audiveris.omr.classifier.SampleRepository;
import org.audiveris.omr.classifier.SampleSheet;
import org.audiveris.omr.glyph.GlyphsModel;
import org.audiveris.omr.ui.selection.EntityService;

/**
 * Class {@code SampleModel} is a very basic sample model, used to handle
 * addition/deletion of samples.
 *
 * @author Hervé Bitteur
 */
public class SampleModel
        extends GlyphsModel
{

    private final SampleRepository repository;

    /**
     * Creates a new {@code SampleModel} object.
     *
     * @param repository    the sample repository
     * @param sampleService the event service for samples
     */
    public SampleModel (SampleRepository repository,
                        EntityService<Sample> sampleService)
    {
        super(null, sampleService);
        this.repository = repository;
    }

    /**
     * Add a sample.
     *
     * @param sample      the sample to add
     * @param sampleSheet the containing sheet
     */
    public void addSample (Sample sample,
                           SampleSheet sampleSheet)
    {
        repository.addSample(sample, sampleSheet);
    }

    /**
     * Report the underlying repository
     *
     * @return the repository
     */
    public SampleRepository getRepository ()
    {
        return repository;
    }

    /**
     * Remove a sample.
     *
     * @param sample the sample to remove
     */
    public void removeSample (Sample sample)
    {
        repository.removeSample(sample);
    }
}
