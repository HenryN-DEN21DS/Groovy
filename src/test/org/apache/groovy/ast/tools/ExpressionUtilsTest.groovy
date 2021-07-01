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
package org.apache.groovy.ast.tools

import groovy.test.GroovyTestCase
import groovy.test.NotYetImplemented
import groovy.transform.AutoFinal
import org.apache.groovy.parser.antlr4.TestUtils
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.control.CompilationUnit
import org.codehaus.groovy.control.Phases
import org.codehaus.groovy.syntax.Token
import org.codehaus.groovy.syntax.Types
import org.junit.Assert

import static org.apache.groovy.parser.antlr4.util.ASTComparatorCategory.LOCATION_IGNORE_LIST

/**
 * Testing expressions of ExpressionUtils.
 */
@AutoFinal
final class ExpressionUtilsTest extends GroovyTestCase {

    void 'test transformBinaryConstantExpression - null'() {
        try {
            ExpressionUtils.transformBinaryConstantExpression(null, null)
        } catch(NullPointerException npe) {
            // Pass
            return
        }
        Assert.fail('Should throw NullPointerException')
    }

    void 'test transformBinaryConstantExpression - string PLUS string'() {
        ConstantExpression left = new ConstantExpression('hello, ')
        ConstantExpression right = new ConstantExpression('world!')
        Token token = new Token(Types.PLUS, '+', 1, 1)
        BinaryExpression be = new BinaryExpression(left, token, right)
        ClassNode targetType = ClassHelper.make(String)
        ConstantExpression actual = ExpressionUtils.transformBinaryConstantExpression(be, targetType)
        assertEquals('hello, world!', actual.value)
    }

    void 'test transformBinaryConstantExpression - long PLUS long'() {
        ConstantExpression left = new ConstantExpression(11111111L)
        ConstantExpression right = new ConstantExpression(11111111L)
        Token token = new Token(Types.PLUS, '+', 1, 1)
        BinaryExpression be = new BinaryExpression(left, token, right)
        ClassNode targetType = ClassHelper.make(Long)
        ConstantExpression actual = ExpressionUtils.transformBinaryConstantExpression(be, targetType)
        assertEquals(22222222L, actual.value)
    }
}
