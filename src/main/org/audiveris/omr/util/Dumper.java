//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                                          D u m p e r                                           //
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
package org.audiveris.omr.util;

import org.audiveris.omr.constant.Constant;
import org.audiveris.omr.constant.ConstantSet;
import org.audiveris.omr.math.LineUtil;
import org.audiveris.omr.ui.util.UIUtil;
import org.audiveris.omr.util.Dumping.Relevance;

import java.awt.geom.Line2D;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Class {@code Dumper} is a debugging utility that reports, in a brute
 * force manner, any internal data of a class instance.
 * <p>
 * When used on a class instance, all class internal fields which are considered as "relevant" are
 * printed using their toString() method, then we walk up the inheritance tree and repeat the same
 * actions, until there is no more superclass or until the superclass we have reached is considered
 * as non-relevant.
 * <p>
 * A (super)class is considered "relevant" if the static method {@code isClassRelevant(class)}
 * returns true. This method can be overridden in a subclass of Dumper to adapt to local needs.
 * <p>
 * A field is considered "relevant" if the following condition if the method
 * {@code isFieldRelevant(field)} returns true. Similarly, the behavior of this predicate can be
 * customized by subclassing the Dumper class.
 * <p>
 * There are several kinds of print outs available through subclassing. Each of them export two
 * public methods: {@code dump()} which prints the result on default output stream, and
 * {@code dumpOf()} which simply returns the generated dump string.
 * <ul>
 * <li><b>Column</b>: a dump with one line per field</li>
 * <li><b>Row</b>: a dump with all information on one row</li>
 * <li><b>Html</b>: an Html stream with fields arranged in tables</li>
 * </ul>
 * <p>
 * Here are some examples of use:
 *
 * <pre>
 * // Using the predefined static helper methods
 * Dumper.dump(myinstance);
 * Dumper.dump(myinstance, "My Title");
 * Dumper.dump(myinstance, "My Title", 2);
 * System.out.println(Dumper.dumpOf(myinstance));
 * System.out.println(Dumper.htmlDumpOf(myinstance));
 *
 * // Using directly the Dumper subclasses
 * new Dumper.Column(myinstance).print();
 * System.out.println(new Dumper.Row(myinstance).toString());
 * display(new Dumper.Html(myinstance).toString());
 * </pre>
 *
 * @author Hervé Bitteur
 */
public class Dumper
{
    //~ Static fields/initializers -----------------------------------------------------------------

    private static final Constants constants = new Constants();

    /** Maximum number of collection items printed */
    private static final int MAX_COLLECTION_INDEX = 9;

    //~ Instance fields ----------------------------------------------------------------------------
    /** To filter classes and fields */
    protected final Relevance relevance;

    /**
     * The object to be dumped
     */
    protected final Object object;

    /**
     * The string buffer used as output
     */
    protected final StringBuilder sb;

    /**
     * Can we use HTML directives?
     */
    protected final boolean useHtml;

    /**
     * Class (beware, this variable is updated as we walk up the inheritance tree)
     */
    protected Class<?> classe;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a new Dumper.
     *
     * @param relevance the relevance filter
     * @param object    the object instance to be dumped.
     * @param useHtml   ????
     */
    public Dumper (Relevance relevance,
                   Object object,
                   boolean useHtml)
    {
        this.relevance = relevance;

        // (re)Allocate the string buffer
        sb = new StringBuilder(1_024);

        // Cache the object & the related class
        this.object = object;
        this.useHtml = useHtml;
        classe = object.getClass();
    }

    //~ Methods ------------------------------------------------------------------------------------
    //-------//
    // print //
    //-------//
    /**
     * Print the dump string onto the standard output
     */
    public void print ()
    {
    }

    //----------//
    // toString //
    //----------//
    /**
     * Return the string buffer content
     *
     * @return the dump of the object as a string
     */
    @Override
    public String toString ()
    {
        // Do the processing
        processObject();

        // Return the final content of string buffer
        return sb.toString();
    }

    //------------------//
    // printClassEpilog //
    //------------------//
    /**
     * To be overridden so as to print the epilog of class data
     */
    protected void printClassEpilog ()
    {
    }

    //------------------//
    // printClassProlog //
    //------------------//
    /**
     * To be overridden so as to print the prolog of class data
     */
    protected void printClassProlog ()
    {
    }

    //----------------------//
    // printCollectionValue //
    //----------------------//
    /**
     * Print the provided collection to the dump.
     *
     * @param collection the collection object to print
     */
    protected void printCollectionValue (Collection<?> collection)
    {
        sb.append("[");

        int i = 0;

        for (Object obj : collection) {
            if (i++ > 0) {
                sb.append(useHtml ? ",<br/>" : ",");
            }

            // Safeguard action when the object is a big collection
            if (i > MAX_COLLECTION_INDEX) {
                sb.append(" ... ").append(collection.size()).append(" items");

                break;
            } else {
                sb.append(obj);
            }
        }

        sb.append("]");
    }

    //------------//
    // printField //
    //------------//
    /**
     * Basic printing of field name and value.
     * The method can of course be overridden.
     *
     * @param name  the field name
     * @param value the field value, which may be null
     */
    protected void printField (String name,
                               Object value)
    {
        if (value == null) {
            sb.append("null");
        } else if (value instanceof Collection) {
            printCollectionValue((Collection) value);
        } else if (value instanceof Map) {
            printCollectionValue(((Map) value).entrySet());
        } else if (value instanceof boolean[]) {
            sb.append(Arrays.toString((boolean[]) value));
        } else if (value instanceof byte[]) {
            sb.append(Arrays.toString((byte[]) value));
        } else if (value instanceof short[]) {
            sb.append(Arrays.toString((short[]) value));
        } else if (value instanceof char[]) {
            sb.append(Arrays.toString((char[]) value));
        } else if (value instanceof int[]) {
            sb.append(Arrays.toString((int[]) value));
        } else if (value instanceof long[]) {
            sb.append(Arrays.toString((long[]) value));
        } else if (value instanceof float[]) {
            sb.append(Arrays.toString((float[]) value));
        } else if (value instanceof double[]) {
            sb.append(Arrays.toString((double[]) value));
        } else if (value instanceof Line2D) {
            sb.append(LineUtil.toString((Line2D) value));
        } else if (value.getClass().isArray()) {
            printArrayValue((Object[]) value);
        } else {
            sb.append(value.toString());
        }
    }

    private void printArrayValue (Object[] value)
    {
        sb.append("[");

        int i = 0;

        for (Object obj : value) {
            if (i++ > 0) {
                sb.append(useHtml ? ",<br/>" : ",");
            }

            // Safeguard action when the object is a big collection
            if (i > MAX_COLLECTION_INDEX) {
                sb.append(" ... ").append(value.length).append(" items");

                break;
            } else {
                sb.append(obj);
            }
        }

        sb.append("]");
    }

    //--------------//
    // processClass //
    //--------------//
    private void processClass ()
    {
        // Class Prolog
        printClassProlog();

        // Process the class Fields
        for (Field field : classe.getDeclaredFields()) {
            processField(field);
        }

        // Class Epilog
        printClassEpilog();
    }

    //--------------//
    // processField //
    //--------------//
    private void processField (Field field)
    {
        // Check that we are really interested in printing this field out
        if (relevance.isFieldRelevant(field)) {
            // Override any access limitation
            field.setAccessible(true);

            try {
                // Retrieve field value in the object instance
                Object value = field.get(object);

                // Print the field value as requested
                printField(field.getName(), value);
            } catch (IllegalAccessException ex) {
                // Cannot occur in fact, thanks to setAccessible
            }
        }
    }

    //---------------//
    // processObject //
    //---------------//
    private void processObject ()
    {
        do {
            // Process the class at hand
            processClass();

            // Walk up the inheritance tree
            classe = classe.getSuperclass();
        } while (relevance.isClassRelevant(classe));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    //--------//
    // Column //
    //--------//
    /**
     * Class {@code Column} implements a Dumper where all fields are
     * presented in one column, each field on a separate line.
     * <p>
     * The column can be left indented, according to the specified indentation level.
     */
    public static class Column
            extends Dumper
    {

        private static final String MEMBER_GAP = "   ";

        private static final String INDENT_GAP = ".  ";

        private final String title;

        private final StringBuilder prefix;

        /**
         * Create a Column.
         *
         * @param relevance selection policy
         * @param object    the object to dump
         * @param title     title of the dump
         * @param level     initial indentation level
         */
        public Column (Relevance relevance,
                       Object object,
                       String title,
                       int level)
        {
            super(relevance, object, false);

            // Cache the title
            if (title != null) {
                this.title = title;
            } else {
                this.title = "";
            }

            // Prepare indent prefix
            prefix = new StringBuilder(level * INDENT_GAP.length());

            for (int i = level; i > 0; i--) {
                prefix.append(INDENT_GAP);
            }
        }

        @Override
        protected void printClassProlog ()
        {
            // We print the class name only for the lowest class in heritance hierarchy
            if (object.getClass() == classe) {
                sb.append("\n");
                sb.append(prefix).append(classe.getName());
                sb.append(" ").append(title).append(":");
            }
        }

        @Override
        protected void printField (String name,
                                   Object value)
        {
            sb.append("\n");
            sb.append(prefix).append(MEMBER_GAP);
            sb.append(name).append("=");
            super.printField(name, value);
        }
    }

    //------//
    // Html //
    //------//
    /**
     * Class {@code Html} implements a Dumper using HTML tags to present
     * fields in a table.
     */
    public static class Html
            extends Dumper
    {

        /**
         * Create an Html object.
         *
         * @param relevance selection policy
         * @param object    the object to dump
         */
        public Html (Relevance relevance,
                     Object object)
        {
            super(relevance, object, true);
        }

        @Override
        public String toString ()
        {
            // Style
            final String name = constants.fontName.getValue();
            final int size = UIUtil.adjustedSize(constants.fontSize.getValue());
            sb.append("<style> td {").append(" font-family: ").append(name).append(
                    ", Verdana, sans-serif;").append(" font-size: ").append(size).append("px;")
                    .append(
                            " font-style: normal;").append("} </style>");

            // Table begin
            sb.append("<table border=0 cellpadding=3>");

            // The object
            super.processObject();

            // Table end
            sb.append("</table>");

            // Return the final content of string buffer
            return sb.toString();
        }

        @Override
        protected void printClassProlog ()
        {
            // Class name
            sb.append("<tr><td colspan=2><font color='BLUE'>").append(classe.getName()).append(
                    "</font></td></tr>");
        }

        @Override
        protected void printField (String name,
                                   Object value)
        {
            // One table row per field
            sb.append("<tr>");

            // First the field name
            sb.append("<td align='right'><font color='RED'>").append(name).append("</font></td>");

            // Then the field value
            sb.append("<td>");
            super.printField(name, value);

            sb.append("</td>").append("</tr>");
        }
    }

    //-----//
    // Row //
    //-----//
    /**
     * Class {@code Row} implements a Dumper where all fields are
     * presented on the same line.
     */
    public static class Row
            extends Dumper
    {

        /**
         * Create a Row object.
         *
         * @param relevance selection policy
         * @param object    the object to dump
         */
        public Row (Relevance relevance,
                    Object object)
        {
            super(relevance, object, false);
        }

        @Override
        protected void printClassEpilog ()
        {
            sb.append("}");
        }

        @Override
        protected void printClassProlog ()
        {
            // Class name
            sb.append("{");

            // Special annotation for superclass
            if (object.getClass() != classe) {
                sb.append("from ");
            }

            sb.append(classe.getName()).append(":");
        }

        @Override
        protected void printField (String name,
                                   Object value)
        {
            sb.append(" ");
            sb.append(name).append("=");
            super.printField(name, value);
        }
    }

    //-----------//
    // Constants //
    //-----------//
    private static class Constants
            extends ConstantSet
    {

        private final Constant.Integer fontSize = new Constant.Integer(
                "Points",
                9,
                "Font size for HTML dump");

        private final Constant.String fontName = new Constant.String(
                "Lucida Console",
                "Font name for HTML dump");
    }
}
