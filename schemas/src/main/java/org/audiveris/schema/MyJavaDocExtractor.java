//------------------------------------------------------------------------------------------------//
//                                                                                                //
//                             M y J a v a D o c E x t r a c t o r                                //
//                                                                                                //
//------------------------------------------------------------------------------------------------//
package org.audiveris.schema;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
import com.thoughtworks.qdox.JavaProjectBuilder;
import com.thoughtworks.qdox.model.JavaAnnotatedElement;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.JavaDocData;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.SearchableDocumentation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.SortableLocation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.ClassLocation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.FieldLocation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.MethodLocation;
import org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.location.PackageLocation;
import org.codehaus.mojo.jaxb2.shared.FileSystemUtilities;
import org.codehaus.mojo.jaxb2.shared.Validate;

import com.thoughtworks.qdox.model.JavaAnnotation;
import com.thoughtworks.qdox.model.JavaClass;
import com.thoughtworks.qdox.model.JavaField;
import com.thoughtworks.qdox.model.JavaMethod;
import com.thoughtworks.qdox.model.JavaPackage;
import com.thoughtworks.qdox.model.JavaSource;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlEnumValue;
import javax.xml.bind.annotation.XmlType;

/**
 * The schemagen tool operates on compiled bytecode, where JavaDoc comments are not present.
 * However, the javadoc documentation present in java source files is required within the generated
 * XSD to increase usability and produce an XSD which does not loose out on important usage
 * information.</p>
 * <p>
 * The JavaDocExtractor is used as a post processor after creating the XSDs within the compilation
 * unit, and injects XSD annotations into the appropriate XSD elements or types.</p>
 * <p>
 * HB: This is a modified version of class
 * org.codehaus.mojo.jaxb2.schemageneration.postprocessing.javadoc.JavaDocExtractor
 * which processes all relevant classes, including the inner classes.
 *
 * @author <a href="mailto:lj@jguru.se">Lennart J&ouml;relid</a>, jGuru Europe AB
 * @since 2.0
 */
public class MyJavaDocExtractor
{
    //~ Static fields/initializers -----------------------------------------------------------------

    /**
     * The default value given as the return value from some annotation classes whenever
     * the attribute has not been supplied within the codebase.
     */
    private static final String DEFAULT_VALUE = "##default";

    //~ Instance fields ----------------------------------------------------------------------------
    private final JavaProjectBuilder builder;

    private final Log log;

    //~ Constructors -------------------------------------------------------------------------------
    /**
     * Creates a NestedJavaDocExtractor wrapping the supplied Maven Log.
     *
     * @param log A non-null Log.
     */
    public MyJavaDocExtractor (final Log log)
    {

        // Check sanity
        Validate.notNull(log, "log");

        // Create internal state
        this.log = log;
        this.builder = new JavaProjectBuilder();
    }

    //~ Methods ------------------------------------------------------------------------------------
    /**
     * Assigns the encoding of the underlying {@link JavaProjectBuilder}.
     *
     * @param encoding The non-empty encoding to be set into the underlying
     *                 {@link JavaProjectBuilder}.
     */
    public void setEncoding (final String encoding)
    {
        this.builder.setEncoding(encoding);
    }

    /**
     * Adds the supplied sourceCodeFiles for processing by this JavaDocExtractor.
     *
     * @param sourceCodeFiles The non-null List of source code files to add.
     * @return This MyJavaDocExtractor, for call chaining.
     * @throws IllegalArgumentException If any of the given sourceCodeFiles could not be read
     *                                  properly.
     */
    public MyJavaDocExtractor addSourceFiles (final List<File> sourceCodeFiles)
            throws IllegalArgumentException
    {

        // Check sanity
        Validate.notNull(sourceCodeFiles, "addSourceFiles");

        // Add the files.
        for (File current : sourceCodeFiles) {
            try {
                builder.addSource(current);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not add file ["
                                                           + FileSystemUtilities.getCanonicalPath(
                                current) + "]", e);
            }
        }

        // All done.
        return this;
    }

    /**
     * Adds the supplied sourceCodeFiles for processing by this JavaDocExtractor.
     *
     * @param sourceCodeURLs The non-null List of source code URLs to add.
     * @return This MyJavaDocExtractor, for call chaining.
     * @throws IllegalArgumentException If any of the given sourceCodeURLs could not be read
     *                                  properly.
     */
    public MyJavaDocExtractor addSourceURLs (final List<URL> sourceCodeURLs)
            throws IllegalArgumentException
    {

        // Check sanity
        Validate.notNull(sourceCodeURLs, "sourceCodeURLs");

        // Add the URLs
        for (URL current : sourceCodeURLs) {
            try {
                builder.addSource(current);
            } catch (IOException e) {
                throw new IllegalArgumentException("Could not add URL [" + current.toString() + "]",
                                                   e);
            }
        }

        // All done
        return this;
    }

    /**
     * Processes all supplied Java source Files and URLs to extract JavaDocData for all
     * ClassLocations from which JavaDoc has been collected.
     *
     * @return A SearchableDocumentation relating SortableLocations and their paths to harvested
     *         JavaDocData.
     */
    public SearchableDocumentation process ()
    {

        // Start processing.
        final SortedMap<SortableLocation, JavaDocData> dataHolder = new TreeMap<>();
        final Collection<JavaSource> sources = builder.getSources();

        if (log.isInfoEnabled()) {
            log.info("Processing [" + sources.size() + "] java sources.");
        }

        for (JavaSource current : sources) {

            // Add the package-level JavaDoc
            final JavaPackage currentPackage = current.getPackage();
            final String packageName = currentPackage.getName();
            addEntry(dataHolder, new PackageLocation(packageName), currentPackage);

            if (log.isDebugEnabled()) {
                log.debug("Added package-level JavaDoc for [" + packageName + "]");
            }

            for (JavaClass currentClass : current.getClasses()) {
                processClass(currentClass, packageName, dataHolder);
            }
        }

        // All done.
        return new ReadOnlySearchableDocumentation(dataHolder);
    }

    private void processClass (JavaClass currentClass,
                               String packageName,
                               SortedMap<SortableLocation, JavaDocData> dataHolder)
    {
        // Add the class-level JavaDoc
        final String simpleClassName = currentClass.getName();
        final String classXmlName = getAnnotationAttributeValueFrom(
                XmlType.class, "name", currentClass.getAnnotations());

        final ClassLocation classLocation = new ClassLocation(
                packageName, simpleClassName, classXmlName);

        addEntry(dataHolder, classLocation, currentClass);

        if (log.isDebugEnabled()) {
            log.debug("Added class-level JavaDoc for [" + classLocation + "]");
        }

        // Fields
        for (JavaField currentField : currentClass.getFields()) {

            final List<JavaAnnotation> currentFieldAnnotations = currentField
                    .getAnnotations();
            String annotatedXmlName = null;

            //
            // Is this field a collection, annotated with @XmlElementWrapper?
            // If so, the documentation should pertain to the corresponding XML Sequence,
            // rather than the individual XML elements.
            //
            if (hasAnnotation(XmlElementWrapper.class, currentFieldAnnotations)) {

                // There are 2 cases here:
                //
                // 1: The XmlElementWrapper is named.
                // ==================================
                // @XmlElementWrapper(name = "foobar")
                // @XmlElement(name = "aString")
                // private List<String> strings;
                //
                // ==> annotatedXmlName == "foobar"
                //
                // 2: The XmlElementWrapper is not named.
                // ======================================
                // @XmlElementWrapper
                // @XmlElement(name = "anInteger")
                // private SortedSet<Integer> integerSet;
                //
                // ==> annotatedXmlName == "integerSet"
                //
                annotatedXmlName = getAnnotationAttributeValueFrom(
                        XmlElementWrapper.class,
                        "name",
                        currentFieldAnnotations);

                if (annotatedXmlName == null || annotatedXmlName.equals(DEFAULT_VALUE)) {
                    annotatedXmlName = currentField.getName();
                }
            }

            // Find the XML name if provided within an annotation.
            if (annotatedXmlName == null) {
                annotatedXmlName = getAnnotationAttributeValueFrom(
                        XmlElement.class,
                        "name",
                        currentFieldAnnotations);
            }

            if (annotatedXmlName == null) {
                annotatedXmlName = getAnnotationAttributeValueFrom(
                        XmlAttribute.class,
                        "name",
                        currentFieldAnnotations);
            }
            if (annotatedXmlName == null) {
                annotatedXmlName = getAnnotationAttributeValueFrom(
                        XmlEnumValue.class,
                        "value",
                        currentFieldAnnotations);
            }

            // Add the field-level JavaDoc
            final FieldLocation fieldLocation = new FieldLocation(
                    packageName,
                    simpleClassName,
                    classXmlName,
                    currentField.getName(),
                    annotatedXmlName);

            addEntry(dataHolder, fieldLocation, currentField);

            if (log.isDebugEnabled()) {
                log.debug("Added field-level JavaDoc for [" + fieldLocation + "]");
            }
        }

        // Methods
        for (JavaMethod currentMethod : currentClass.getMethods()) {

            final List<JavaAnnotation> currentMethodAnnotations = currentMethod
                    .getAnnotations();
            String annotatedXmlName = null;

            //
            // Is this field a collection, annotated with @XmlElementWrapper?
            // If so, the documentation should pertain to the corresponding XML Sequence,
            // rather than the individual XML elements.
            //
            if (hasAnnotation(XmlElementWrapper.class, currentMethodAnnotations)) {

                // There are 2 cases here:
                //
                // 1: The XmlElementWrapper is named.
                // ==================================
                // @XmlElementWrapper(name = "foobar")
                // @XmlElement(name = "aString")
                // public List<String> getStrings() { ... };
                //
                // ==> annotatedXmlName == "foobar"
                //
                // 2: The XmlElementWrapper is not named.
                // ======================================
                // @XmlElementWrapper
                // @XmlElement(name = "anInteger")
                // public SortedSet<Integer> getIntegerSet() { ... };
                //
                // ==> annotatedXmlName == "getIntegerSet"
                //
                annotatedXmlName = getAnnotationAttributeValueFrom(
                        XmlElementWrapper.class,
                        "name",
                        currentMethodAnnotations);

                if (annotatedXmlName == null || annotatedXmlName.equals(DEFAULT_VALUE)) {
                    annotatedXmlName = currentMethod.getName();
                }
            }

            // Find the XML name if provided within an annotation.
            if (annotatedXmlName == null) {
                annotatedXmlName = getAnnotationAttributeValueFrom(
                        XmlElement.class,
                        "name",
                        currentMethod.getAnnotations());
            }

            if (annotatedXmlName == null) {
                annotatedXmlName = getAnnotationAttributeValueFrom(
                        XmlAttribute.class,
                        "name",
                        currentMethod.getAnnotations());
            }

            // Add the method-level JavaDoc
            final MethodLocation location = new MethodLocation(packageName,
                                                               simpleClassName,
                                                               classXmlName,
                                                               currentMethod.getName(),
                                                               annotatedXmlName,
                                                               currentMethod.getParameters());
            addEntry(dataHolder, location, currentMethod);

            if (log.isDebugEnabled()) {
                log.debug("Added method-level JavaDoc for [" + location + "]");
            }
        }

        // HB: Recursion to inner classes if any
        for (JavaClass innerClass : currentClass.getNestedClasses()) {
            processClass(innerClass, packageName, dataHolder);
        }
    }

    /**
     * Finds the value of the attribute with the supplied name within the first matching
     * JavaAnnotation of
     * the given type encountered in the given annotations List. This is typically used for reading
     * values of
     * annotations such as {@link XmlElement}, {@link XmlAttribute} or {@link XmlEnumValue}.
     *
     * @param annotations    The list of JavaAnnotations to filter from.
     * @param annotationType The type of annotation to read attribute values from.
     * @param attributeName  The name of the attribute the value of which should be returned.
     * @return The first matching JavaAnnotation of type annotationType within the given annotations
     *         List, or {@code null} if none was found.
     * @since 2.2
     */
    private static String getAnnotationAttributeValueFrom (
            final Class<?> annotationType,
            final String attributeName,
            final List<JavaAnnotation> annotations)
    {

        // QDox uses the fully qualified class name of the annotation for comparison.
        // Extract it.
        final String fullyQualifiedClassName = annotationType.getName();

        JavaAnnotation annotation = null;
        String toReturn = null;

        if (annotations != null) {

            for (JavaAnnotation current : annotations) {
                if (current.getType().isA(fullyQualifiedClassName)) {
                    annotation = current;
                    break;
                }
            }

            if (annotation != null) {

                final Object nameValue = annotation.getNamedParameter(attributeName);

                if (nameValue != null && nameValue instanceof String) {

                    toReturn = ((String) nameValue).trim();

                    // Remove initial and trailing " chars, if present.
                    if (toReturn.startsWith("\"") && toReturn.endsWith("\"")) {
                        toReturn = (((String) nameValue).trim()).substring(1, toReturn.length() - 1);
                    }
                }
            }
        }

        // All Done.
        return toReturn;
    }

    private static boolean hasAnnotation (final Class<?> annotationType,
                                          final List<JavaAnnotation> annotations)
    {

        if (annotations != null && !annotations.isEmpty() && annotationType != null) {

            final String fullAnnotationClassName = annotationType.getName();

            for (JavaAnnotation current : annotations) {
                if (current.getType().isA(fullAnnotationClassName)) {
                    return true;
                }
            }
        }

        return false;
    }

    //
    // Private helpers
    //
    private void addEntry (final SortedMap<SortableLocation, JavaDocData> map,
                           final SortableLocation key,
                           final JavaAnnotatedElement value)
    {

        // Check sanity
        if (map.containsKey(key)) {

            // Get something to compare with
            final JavaDocData existing = map.get(key);

            // Is this an empty package-level documentation?
            if (key instanceof PackageLocation) {

                final boolean emptyExisting = existing.getComment() == null || existing.getComment()
                        .isEmpty();
                final boolean emptyGiven = value.getComment() == null || value.getComment()
                        .isEmpty();

                if (emptyGiven) {
                    if (log.isDebugEnabled()) {
                        log.debug("Skipping processing empty Package javadoc from [" + key + "]");
                    }
                    return;
                } else if (emptyExisting && log.isWarnEnabled()) {
                    log.warn("Overwriting empty Package javadoc from [" + key + "]");
                }
            } else {
                final String given = "[" + value.getClass().getName() + "]: " + value.getComment();
                throw new IllegalArgumentException("Not processing duplicate SortableLocation ["
                                                           + key + "]. "
                                                           + "\n Existing: " + existing
                                                           + ".\n Given: [" + given + "]");
            }
        }

        // Validate.isTrue(!map.containsKey(key), "Found duplicate SortableLocation [" + key + "] in map. "
        //         + "Current map keySet: " + map.keySet() + ". Got comment: [" + value.getComment() + "]");
        map.put(key, new JavaDocData(value.getComment(), value.getTags()));
    }

    //~ Inner Classes ------------------------------------------------------------------------------
    /**
     * Standard read-only SearchableDocumentation implementation.
     */
    private class ReadOnlySearchableDocumentation
            implements SearchableDocumentation
    {

        // Internal state
        private final TreeMap<String, SortableLocation> keyMap;

        private final SortedMap<? extends SortableLocation, JavaDocData> valueMap;

        ReadOnlySearchableDocumentation (final SortedMap<SortableLocation, JavaDocData> valueMap)
        {

            // Create internal state
            this.valueMap = valueMap;

            keyMap = new TreeMap<>();
            for (Map.Entry<SortableLocation, JavaDocData> current : valueMap.entrySet()) {

                final SortableLocation key = current.getKey();
                keyMap.put(key.getPath(), key);
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public SortedSet<String> getPaths ()
        {
            return Collections.unmodifiableSortedSet(keyMap.navigableKeySet());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public JavaDocData getJavaDoc (final String path)
        {

            // Check sanity
            Validate.notNull(path, "path");

            // All done.
            final SortableLocation location = getLocation(path);
            return (location == null) ? null : valueMap.get(location);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T extends SortableLocation> T getLocation (final String path)
        {

            // Check sanity
            Validate.notNull(path, "path");

            // All done
            return (T) keyMap.get(path);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public SortedMap<SortableLocation, JavaDocData> getAll ()
        {
            return (SortedMap<SortableLocation, JavaDocData>) Collections.unmodifiableSortedMap(
                    valueMap);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        @SuppressWarnings("unchecked")
        public <T extends SortableLocation> SortedMap<T, JavaDocData> getAll (final Class<T> type)
        {

            // Check sanity
            Validate.notNull(type, "type");

            // Filter the valueMap.
            final SortedMap<T, JavaDocData> toReturn = new TreeMap<>();
            for (Map.Entry<? extends SortableLocation, JavaDocData> current : valueMap.entrySet()) {
                if (type == current.getKey().getClass()) {
                    toReturn.put((T) current.getKey(), current.getValue());
                }
            }

            // All done.
            return toReturn;
        }
    }
}
