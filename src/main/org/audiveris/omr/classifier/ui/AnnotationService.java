//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                A n n o t a t i o n S e r v i c e                               //
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

import org.audiveris.omr.classifier.Annotation;
import org.audiveris.omr.ui.selection.EntityListEvent;
import org.audiveris.omr.ui.selection.EntityService;
import org.audiveris.omr.ui.selection.IdEvent;
import org.audiveris.omr.ui.selection.SelectionService;
import org.audiveris.omr.util.EntityIndex;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class {@code AnnotationService} is an EntityService for annotations.
 *
 * @author Hervé Bitteur
 */
public class AnnotationService
        extends EntityService<Annotation>
{

    private static final Logger logger = LoggerFactory.getLogger(AnnotationService.class);

    /** Events that can be published on an annotation service. */
    private static final Class<?>[] eventsAllowed = new Class<?>[]{
        IdEvent.class, EntityListEvent.class
    };

    /**
     * Creates a new {@code AnnotationService} object.
     *
     * @param index           underlying annotation index
     * @param locationService related service for location info
     */
    public AnnotationService (EntityIndex<Annotation> index,
                              SelectionService locationService)
    {
        super(index, locationService, eventsAllowed);
    }
}
