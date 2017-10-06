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
package org.codehaus.groovy.transform.impl.autofinal;

import groovy.transform.AutoFinal;
import groovy.transform.impl.autofinal.AutoFinalClosure;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ClosureExpression;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.ASTTransformation;
import org.codehaus.groovy.transform.GroovyASTTransformation;

import java.lang.reflect.Modifier;

import static org.codehaus.groovy.ast.ClassHelper.make;

/**
 * Handles {@link AutoFinal} annotation code generation for arguments of closures.
 */
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class AutoFinalClosureASTTransformation implements ASTTransformation {

    private static final Class MY_CLASS = AutoFinalClosure.class;
    private static final ClassNode MY_TYPE = make(MY_CLASS);
    private static final String MY_TYPE_NAME = "@" + MY_TYPE.getNameWithoutPackage();

    public void visit(ASTNode[] nodes, SourceUnit unit) {
        ClassNode annotatedClass = (ClassNode) nodes[1];

        // TODO:Test
        final ClassCodeVisitorSupport visitor = new ClassCodeVisitorSupport() {
            @Override
            public void visitClosureExpression(ClosureExpression expression) {
                if(expression.isSynthetic()) { return; }
                Parameter[] origParams = expression.getParameters();
                for (Parameter p : origParams) {
                    p.setModifiers(p.getModifiers() | Modifier.FINAL);
                }
                super.visitClosureExpression(expression);
            }

            protected SourceUnit getSourceUnit() {
                return unit;
            }
        };
        
        visitor.visitClass(annotatedClass);
    }
}