/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.vmplugin;

import org.codehaus.groovy.ast.AnnotationNode;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.CompileUnit;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

/**
 * Interface to access VM version based actions.
 * This interface is for internal use only!
 */
public interface VMPlugin {
    void setAdditionalClassInformation(ClassNode c);
    Class[] getPluginDefaultGroovyMethods();
    Class[] getPluginStaticGroovyMethods();
    void configureAnnotationNodeFromDefinition(AnnotationNode definition, AnnotationNode root);
    void configureAnnotation(AnnotationNode an);
    void configureClassNode(CompileUnit compileUnit, ClassNode classNode);
    void invalidateCallSites();
    /**
     * Returns a handle with bound receiver to invokeSpecial the given method.
     * This method will require at least Java 7, but since the source has to compile
     * on older Java versions as well it is not marked to return a MethodHandle and
     * uses Object instead
     * @return  null in case of jdk&lt;7, otherwise a handle that takes the method call
     *          arguments for the invokespecial call
     */
    Object getInvokeSpecialHandle(Method m, Object receiver);

    /**
     * Invokes a handle produced by #getInvokeSpecialdHandle
     * @param handle the handle
     * @param args arguments for the method call, can be empty but not null
     * @return the result of the method call
     */
    Object invokeHandle(Object handle, Object[] args) throws Throwable;

    /**
     * Gives the version the plugin is made for
     * @return 7 for jdk7, 8 for jdk8, 9 for jdk9 or higher
     */
    int getVersion();

    /**
     * Check whether invoking {@link AccessibleObject#setAccessible(boolean)} on the accessible object will be completed successfully
     *
     * @param accessibleObject the accessible object to check
     * @param caller the caller to invoke {@code setAccessible}
     * @return the check result
     */
    boolean checkCanSetAccessible(AccessibleObject accessibleObject, Class<?> caller);

    /**
     * Set the {@code accessible} flag for this reflected object to {@code true}
     * if possible.
     *
     * @param ao the accessible object
     * @return {@code true} if the {@code accessible} flag is set to {@code true};
     *         {@code false} if access cannot be enabled.
     * @throws SecurityException if the request is denied by the security manager
     */
    boolean trySetAccessible(AccessibleObject ao);
}
