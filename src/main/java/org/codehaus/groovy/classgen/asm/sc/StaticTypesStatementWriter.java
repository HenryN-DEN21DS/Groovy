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
package org.codehaus.groovy.classgen.asm.sc;

import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.Parameter;
import org.codehaus.groovy.ast.expr.ArgumentListExpression;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCallExpression;
import org.codehaus.groovy.ast.stmt.BlockStatement;
import org.codehaus.groovy.ast.stmt.ForStatement;
import org.codehaus.groovy.classgen.AsmClassGenerator;
import org.codehaus.groovy.classgen.asm.BytecodeVariable;
import org.codehaus.groovy.classgen.asm.CompileStack;
import org.codehaus.groovy.classgen.asm.MethodCaller;
import org.codehaus.groovy.classgen.asm.OperandStack;
import org.codehaus.groovy.classgen.asm.StatementWriter;
import org.codehaus.groovy.classgen.asm.TypeChooser;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingSupport;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;

import java.util.Enumeration;

import static org.codehaus.groovy.ast.ClassHelper.isPrimitiveType;
import static org.codehaus.groovy.classgen.asm.util.TypeUtil.isPrimitiveBoolean;
import static org.codehaus.groovy.classgen.asm.util.TypeUtil.isPrimitiveByte;
import static org.codehaus.groovy.classgen.asm.util.TypeUtil.isPrimitiveChar;
import static org.codehaus.groovy.classgen.asm.util.TypeUtil.isPrimitiveDouble;
import static org.codehaus.groovy.classgen.asm.util.TypeUtil.isPrimitiveFloat;
import static org.codehaus.groovy.classgen.asm.util.TypeUtil.isPrimitiveInt;
import static org.codehaus.groovy.classgen.asm.util.TypeUtil.isPrimitiveLong;
import static org.codehaus.groovy.classgen.asm.util.TypeUtil.isPrimitiveShort;
import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARRAYLENGTH;
import static org.objectweb.asm.Opcodes.BALOAD;
import static org.objectweb.asm.Opcodes.CALOAD;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FALOAD;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.LALOAD;
import static org.objectweb.asm.Opcodes.SALOAD;

/**
 * A class to write out the optimized statements.
 */
public class StaticTypesStatementWriter extends StatementWriter {

    private static final ClassNode ITERABLE_CLASSNODE = ClassHelper.make(Iterable.class);
    private static final ClassNode ENUMERATION_CLASSNODE = ClassHelper.make(Enumeration.class);
    private static final MethodCaller ENUMERATION_NEXT_METHOD = MethodCaller.newInterface(Enumeration.class, "nextElement");
    private static final MethodCaller ENUMERATION_HASMORE_METHOD = MethodCaller.newInterface(Enumeration.class, "hasMoreElements");

    public StaticTypesStatementWriter(StaticTypesWriterController controller) {
        super(controller);
    }

    @Override
    public void writeBlockStatement(BlockStatement statement) {
        controller.switchToFastPath();
        super.writeBlockStatement(statement);
        controller.switchToSlowPath();
    }

    @Override
    protected void writeForInLoop(final ForStatement loop) {
        controller.getAcg().onLineNumber(loop,"visitForLoop");
        writeStatementLabel(loop);

        CompileStack compileStack = controller.getCompileStack();
        MethodVisitor mv = controller.getMethodVisitor();
        OperandStack operandStack = controller.getOperandStack();

        compileStack.pushLoop(loop.getVariableScope(), loop.getStatementLabels());

        // Identify type of collection
        TypeChooser typeChooser = controller.getTypeChooser();
        Expression collectionExpression = loop.getCollectionExpression();
        ClassNode collectionType = typeChooser.resolveType(collectionExpression, controller.getClassNode());
        Parameter loopVariable = loop.getVariable();
        int size = operandStack.getStackLength();
        if (collectionType.isArray() && loopVariable.getOriginType().equals(collectionType.getComponentType())) {
            writeOptimizedForEachLoop(compileStack, operandStack, mv, loop, collectionExpression, collectionType, loopVariable);
        } else if (ENUMERATION_CLASSNODE.equals(collectionType)) {
            writeEnumerationBasedForEachLoop(loop, collectionExpression, collectionType);
        } else {
            writeIteratorBasedForEachLoop(loop, collectionExpression, collectionType);
        }
        operandStack.popDownTo(size);
        compileStack.pop();
    }

    private void writeOptimizedForEachLoop(
            CompileStack compileStack,
            OperandStack operandStack,
            MethodVisitor mv,
            ForStatement loop,
            Expression collectionExpression,
            ClassNode collectionType,
            Parameter loopVariable) {
        BytecodeVariable variable = compileStack.defineVariable(loopVariable, false);

        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();

        AsmClassGenerator acg = controller.getAcg();

        // load array on stack
        collectionExpression.visit(acg);
        mv.visitInsn(DUP);
        int array = compileStack.defineTemporaryVariable("$arr", collectionType, true);
        mv.visitJumpInsn(IFNULL, breakLabel);

        // $len = array.length
        mv.visitVarInsn(ALOAD, array);
        mv.visitInsn(ARRAYLENGTH);
        operandStack.push(ClassHelper.int_TYPE);
        int arrayLen = compileStack.defineTemporaryVariable("$len", ClassHelper.int_TYPE, true);

        // $idx = 0
        mv.visitInsn(ICONST_0);
        operandStack.push(ClassHelper.int_TYPE);
        int loopIdx = compileStack.defineTemporaryVariable("$idx", ClassHelper.int_TYPE, true);

        mv.visitLabel(continueLabel);
        // $idx<$len?
        mv.visitVarInsn(ILOAD, loopIdx);
        mv.visitVarInsn(ILOAD, arrayLen);
        mv.visitJumpInsn(IF_ICMPGE, breakLabel);

        // get array element
        loadFromArray(mv, variable, array, loopIdx);

        // $idx++
        mv.visitIincInsn(loopIdx, 1);

        // loop body
        loop.getLoopBlock().visit(acg);

        mv.visitJumpInsn(GOTO, continueLabel);

        mv.visitLabel(breakLabel);

        compileStack.removeVar(loopIdx);
        compileStack.removeVar(arrayLen);
        compileStack.removeVar(array);
    }

    private void loadFromArray(MethodVisitor mv, BytecodeVariable variable, int array, int iteratorIdx) {
        OperandStack os = controller.getOperandStack();
        mv.visitVarInsn(ALOAD, array);
        mv.visitVarInsn(ILOAD, iteratorIdx);

        ClassNode varType = variable.getType();

        if (isPrimitiveType(varType)) {
            if (isPrimitiveInt(varType)) {
                mv.visitInsn(IALOAD);
            } else if (isPrimitiveLong(varType)) {
                mv.visitInsn(LALOAD);
            } else if (isPrimitiveByte(varType) || isPrimitiveBoolean(varType)) {
                mv.visitInsn(BALOAD);
            } else if (isPrimitiveChar(varType)) {
                mv.visitInsn(CALOAD);
            } else if (isPrimitiveShort(varType)) {
                mv.visitInsn(SALOAD);
            } else if (isPrimitiveFloat(varType)) {
                mv.visitInsn(FALOAD);
            } else if (isPrimitiveDouble(varType)) {
                mv.visitInsn(DALOAD);
            }
        } else {
            mv.visitInsn(AALOAD);
        }
        os.push(varType);
        os.storeVar(variable);
    }

    private void writeIteratorBasedForEachLoop(
            ForStatement loop,
            Expression collectionExpression,
            ClassNode collectionType) {

        if (StaticTypeCheckingSupport.implementsInterfaceOrIsSubclassOf(collectionType, ITERABLE_CLASSNODE)) {
            MethodCallExpression iterator = new MethodCallExpression(collectionExpression, "iterator", new ArgumentListExpression());
            iterator.setMethodTarget(collectionType.getMethod("iterator", Parameter.EMPTY_ARRAY));
            iterator.setImplicitThis(false);
            iterator.visit(controller.getAcg());
        } else {
            collectionExpression.visit(controller.getAcg());
            controller.getMethodVisitor().visitMethodInsn(INVOKESTATIC, "org/codehaus/groovy/runtime/DefaultGroovyMethods", "iterator", "(Ljava/lang/Object;)Ljava/util/Iterator;", false);
            controller.getOperandStack().replace(ClassHelper.Iterator_TYPE);
        }

        writeForInLoopControlAndBlock(loop);
    }

    private void writeEnumerationBasedForEachLoop(
            ForStatement loop,
            Expression collectionExpression,
            ClassNode collectionType) {

        CompileStack compileStack = controller.getCompileStack();
        MethodVisitor mv = controller.getMethodVisitor();
        OperandStack operandStack = controller.getOperandStack();

        // Declare the loop counter.
        BytecodeVariable variable = compileStack.defineVariable(loop.getVariable(), false);

        collectionExpression.visit(controller.getAcg());

        // Then get the iterator and generate the loop control

        int enumIdx = compileStack.defineTemporaryVariable("$enum", ENUMERATION_CLASSNODE, true);

        Label continueLabel = compileStack.getContinueLabel();
        Label breakLabel = compileStack.getBreakLabel();

        mv.visitLabel(continueLabel);
        mv.visitVarInsn(ALOAD, enumIdx);
        ENUMERATION_HASMORE_METHOD.call(mv);
        // note: ifeq tests for ==0, a boolean is 0 if it is false
        mv.visitJumpInsn(IFEQ, breakLabel);

        mv.visitVarInsn(ALOAD, enumIdx);
        ENUMERATION_NEXT_METHOD.call(mv);
        operandStack.push(ClassHelper.OBJECT_TYPE);
        operandStack.storeVar(variable);

        // Generate the loop body
        loop.getLoopBlock().visit(controller.getAcg());

        mv.visitJumpInsn(GOTO, continueLabel);
        mv.visitLabel(breakLabel);

    }

}
