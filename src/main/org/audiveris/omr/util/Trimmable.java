//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                        T r i m m a b l e                                       //
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
package org.audiveris.omr.util;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Class {@code Trimmable} allows to nullify some empty collection fields before being
 * marshalled and to reallocate proper empty collection after being marshalled.
 *
 * @author Hervé Bitteur
 */
public abstract class Trimmable
{

    //~ Static fields/initializers -----------------------------------------------------------------
    //~ Instance fields ----------------------------------------------------------------------------
    //~ Constructors -------------------------------------------------------------------------------
    private Trimmable ()
    {
    }

    //~ Methods ------------------------------------------------------------------------------------
    //---------------//
    // beforeMarshal //
    //---------------//
    /**
     * Nullify any empty collection annotated field before being marshalled.
     *
     * @param obj the object about to be marshalled
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static void beforeMarshal (Object obj)
            throws IllegalAccessException,
                   IllegalArgumentException,
                   InvocationTargetException,
                   NoSuchMethodException
    {
        // Nullify any empty collection field
        Class<?> classe = obj.getClass();

        for (Field field : classe.getDeclaredFields()) {
            if (field.isAnnotationPresent(Collection.class)) {
                Method isEmptyMethod = field.getType().getMethod("isEmpty");
                field.setAccessible(true);
                Object collection = field.get(obj);
                boolean isEmpty = (boolean) isEmptyMethod.invoke(collection);

                if (isEmpty) {
                    field.set(obj, null);
                }
            }
        }
    }

    //--------------//
    // afterMarshal //
    //--------------//
    /**
     * Re-allocate empty collections to null annotated fields after being marshalled.
     *
     * @param obj the object just marshalled
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws InvocationTargetException
     * @throws NoSuchMethodException
     */
    public static void afterMarshal (Object obj)
            throws IllegalAccessException,
                   IllegalArgumentException,
                   InstantiationException,
                   InvocationTargetException,
                   NoSuchMethodException
    {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Collection.class)) {
                field.setAccessible(true);
                Object collection = field.get(obj);

                if (collection == null) {
                    Constructor<?> cons = field.getType().getConstructor(new Class[0]);
                    field.set(obj, cons.newInstance());
                }
            }
        }
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    /**
     * Annotation {@code Collection} flags a collection field as trimmable.
     * <p>
     * NOTA: This can apply only on concrete collection class, not on collection interface.
     * For example ArrayList but not List.
     */
    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Collection
    {
    }
}
