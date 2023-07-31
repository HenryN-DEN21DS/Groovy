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
package groovy.transform

import org.codehaus.groovy.ast.AnnotatedNode
import org.codehaus.groovy.ast.AnnotationNode
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AnnotationCollectorTransform
import org.junit.Test

import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

import static groovy.test.GroovyAssert.assertScript
import static groovy.test.GroovyAssert.shouldFail

final class AnnotationCollectorTest {

    static class MyProcessor extends AnnotationCollectorTransform {
        @Override
        List<AnnotationNode> visit(AnnotationNode collector, AnnotationNode aliasAnnotationUsage, AnnotatedNode aliasAnnotated, SourceUnit source) {
            def excludes = aliasAnnotationUsage.getMember('excludes')
            if (excludes) {
                addError('use myex instead of excludes', aliasAnnotationUsage, source)
                return []
            }
            def myex = aliasAnnotationUsage.getMembers().remove('myex')
            if (myex) aliasAnnotationUsage.addMember('excludes', myex)

            super.visit(collector, aliasAnnotationUsage, aliasAnnotated, source)
        }
    }

    @Test
    void testSimpleUsage() {
        def data = PreCompiledAlias.getAnnotation(AnnotationCollector).serializeClass().value()
        assert data instanceof Object[][]
        assert data.length == 0

        assertScript '''import groovy.transform.PreCompiledAlias
            @PreCompiledAlias
            class Foo {
                Integer a, b
            }

            assert Foo.class.annotations.size() == 0
            assert new Foo(a: 1, b: 2).toString() == "Foo(1, 2)"
            assert PreCompiledAlias.CollectorHelper.value().length == 0
            assert PreCompiledAlias.CollectorHelper.value() instanceof Object[][]
        '''

        assertScript '''import groovy.transform.*
            @AnnotationCollector([EqualsAndHashCode, ToString, Sortable])
            @interface NotPreCompiledAlias {}

            def data = NotPreCompiledAlias.getAnnotation(AnnotationCollector).serializeClass().value()
            assert data instanceof Object[][]
            assert data.length == 0

            @NotPreCompiledAlias
            class Foo {
                Integer a, b
            }

            assert Foo.class.annotations.size() == 0
            assert new Foo(a: 1, b: 2).toString() == "Foo(1, 2)"
        '''
    }

    @Test
    void testUsageWithArgument() {
        assertScript '''import groovy.transform.*
            @PreCompiledAlias(excludes=["a"])
            class Foo {
                Integer a, b
            }

            assert Foo.class.annotations.size() == 0
            assert new Foo(a: 1, b: 2).toString() == "Foo(2)"
        '''

        assertScript '''import groovy.transform.*
            @AnnotationCollector([ToString, EqualsAndHashCode, Sortable])
            @interface NotPreCompiledAlias {}

            def data = NotPreCompiledAlias.getAnnotation(AnnotationCollector).serializeClass().value()
            assert data instanceof Object[][]
            assert data.length == 0

            @NotPreCompiledAlias(excludes=["a"])
            class Foo {
                Integer a, b
            }

            assert Foo.class.annotations.size() == 0
            assert new Foo(a: 1, b: 2).toString() == "Foo(2)"
        '''
    }

    @Test
    void testClosureAnnotation() {
        assertScript '''import groovy.transform.*
            @AnnotationCollector([ConditionalInterrupt])
            @interface NotPreCompiledAlias {}

            def data = NotPreCompiledAlias.getAnnotation(AnnotationCollector).serializeClass().value()
            assert data instanceof Object[][]
            assert data.length == 0

            @NotPreCompiledAlias(applyToAllClasses=false, value={ counter++> 10})
            class X {
                def counter = 0
                def method() {
                  4.times {null}
                 }
            }

            def x = new X(counter:20)
            try {
                x.method()
                assert false
            } catch (InterruptedException ie)  {
                assert true
            }
        '''

        assertScript '''import groovy.transform.*
            @OtherPreCompiledAlias(applyToAllClasses=false, value={ counter++> 10})
            class X {
                def counter = 0
                def method() {
                  4.times {null}
                 }
            }

            def x = new X(counter:20)
            try {
                x.method()
                assert false
            } catch (InterruptedException ie)  {
                assert true
            }
        '''
    }

    @Test
    void testAST() {
        assertScript '''import groovy.transform.*
            @AnnotationCollector([EqualsAndHashCode, ToString, Sortable])
            @interface Alias {}

            def data = Alias.getAnnotation(AnnotationCollector).serializeClass().value()
            assert data instanceof Object[][]
            assert data.length == 0

            @Alias(excludes=["a"])
            @ASTTest(phase=INSTRUCTION_SELECTION, value={
                def annotations = node.annotations
                assert annotations.size() == 4 // ASTTest + 3
                annotations.each {
                    assert it.lineNumber == 9 || it.classNode.name.endsWith('ASTTest')
                }
            })
            class Foo {
                Integer a, b
            }

            assert new Foo(a: 1, b: 2).toString() == "Foo(2)"
        '''
    }

    @Test
    void testConflictingAnnotations() {
        def err = shouldFail '''import groovy.transform.*
            @interface ConflictingA {String foo()}
            @interface ConflictingB {int foo()}

            @AnnotationCollector([ConflictingA, ConflictingB])
            @interface Alias {}

            @Alias(foo="1") class X{}
        '''
        assert err.message.contains("line 8, column 24")
        assert err.message.contains("Attribute 'foo' should have type 'java.lang.Integer'")
    }

    @Test
    void testCustomProcessor() {
        assertScript '''import groovy.transform.*
            @AnnotationCollector(value=[EqualsAndHashCode, ToString, Sortable], processor='groovy.transform.AnnotationCollectorTest$MyProcessor')
            @interface Alias {}

            @Alias(myex=["a"])
            class Foo {
                Integer a, b
            }

            assert new Foo(a: 1, b: 2).toString() == "Foo(2)"
        '''
    }

    @Test
    void testProcessorThrowingCustomMessage() {
        def err = shouldFail '''import groovy.transform.*
            @AnnotationCollector(value=[ToString, EqualsAndHashCode, Sortable], processor='groovy.transform.AnnotationCollectorTest$MyProcessor')
            @interface Alias {}

            @Alias(excludes=["a"])
            class Foo {
                Integer a, b
            }

            assert new Foo(a: 1, b: 2).toString() == "Foo(2)"
        '''
        assert err.message.contains("use myex instead of excludes @ line 5, column 13")
    }

    @Test
    void testWrongProcessorName() {
        def err = shouldFail '''import groovy.transform.*
            @AnnotationCollector(value=[ToString, EqualsAndHashCode, Sortable], processor='MyProcessor')
            @interface Alias {}

            @Alias(excludes=["a"])
            class Foo {
                Integer a, b
            }

            assert new Foo(a: 1, b: 2).toString() == "Foo(2)"
        '''
        assert err.message.contains('Could not find class for Transformation Processor MyProcessor declared by Alias')
    }

    // GROOVY-10570
    @Test
    void testCollectorOnJavaAnno1() {
        def err = shouldFail '''
            @groovy.transform.Groovy10570 // Java @interface with @AnnotationCollector
            class Foo {
                def bar
            }
        '''
        assert err.message.contains('Expecting static method `Object[][] value()` in groovy.transform.Groovy10570. Was it compiled from a Java source?')
    }

    // GROOVY-10570
    @Test
    void testCollectorOnJavaAnno2() {
        assertScript '''
            @groovy.transform.Groovy10570emu // Java @interface with @AnnotationCollector and value array
            class Foo {
                def bar
            }

            // test application of "@EqualsAndHashCode(canEqual=false)"
            groovy.test.GroovyAssert.shouldFail NoSuchMethodException,{
                Foo.class.getDeclaredMethod('canEqual', Object)
            }
        '''
    }

    @Test
    void testAnnotationOnAnnotation() {
        assertScript '''import groovy.transform.*
            @PreCompiledAlias3
            class Foo {
                Integer a, b
            }

            assert new Foo(a: 1, b: 2).toString() == "Foo(2)"

            def data = PreCompiledAlias3.CollectorHelper.value()
            assert data instanceof Object[][]
            assert data.length == 2
            assert data[0].length == 2
            assert data[0][0] == groovy.transform.Sortable
            assert data[0][1] instanceof Map
            assert data[0][1].size() == 0
            assert data[1][0] == groovy.transform.ToString
            assert data[1][1] instanceof Map
            assert data[1][1].size() == 1
            assert data[1][1].excludes instanceof Object[]
            assert data[1][1].excludes[0] == "a"
        '''

        assertScript '''import groovy.transform.*
            @Sortable
            @ToString(excludes=["a"])
            @AnnotationCollector
            class Alias {}

            @Alias
            class Foo {
                Integer a, b
            }

            assert new Foo(a: 1, b: 2).toString() == "Foo(2)"

            def data = Alias.getAnnotation(AnnotationCollector).serializeClass().value()
            assert data.length == 2
            assert data instanceof Object[][]
            assert data[0].length == 2
            assert data[0][0] == groovy.transform.Sortable
            assert data[0][1] instanceof Map
            assert data[0][1].size() == 0
            assert data[1][0] == groovy.transform.ToString
            assert data[1][1] instanceof Map
            assert data[1][1].size() == 1
            assert data[1][1].excludes instanceof Object[]
            assert data[1][1].excludes[0] == "a"
        '''
    }

    @Test
    void testIgnoreIncubating() {
        assertScript '''import groovy.transform.*
            @Sortable
            @org.apache.groovy.lang.annotation.Incubating
            @ToString(excludes=["a"])
            @AnnotationCollector
            class Alias {}

            def data = Alias.getAnnotation(AnnotationCollector).serializeClass().value()
            assert data instanceof Object[][]
            assert data.length == 2
            assert data[0].length == 2
            assert data[0][0] == groovy.transform.Sortable
            assert data[0][1] instanceof Map
            assert data[0][1].size() == 0
            assert data[1][0] == groovy.transform.ToString
            assert data[1][1] instanceof Map
            assert data[1][1].size() == 1
            assert data[1][1].excludes instanceof Object[]
            assert data[1][1].excludes[0] == "a"

            @Alias
            class Foo {
                Integer a, b
            }

            assert Foo.class.annotations.size() == 0
            assert new Foo(a: 1, b: 2).toString() == "Foo(2)"
        '''
    }

    @Test
    void testAnnotationTakingAnnotationParams() {
        assertScript '''import groovy.transform.*
            def data = TheSuperGroovyHeroes.CollectorHelper.value()
            assert data instanceof Object[][]
            assert data.length == 1
            assert data[0].length == 2
            assert data[0][0] == groovy.transform.GroovyCoreTeam
            assert data[0][1] instanceof Map
            assert data[0][1].size() == 1
            data = data[0][1].value
            assert data.length == 4
            assert data[0][0] == GroovyDeveloper
            assert data[0][1].value == "Paul"
            assert data[1][0] == GroovyDeveloper
            assert data[1][1].value == "Cedric"
            assert data[2][0] == GroovyDeveloper
            assert data[2][1].value == "Jochen"
            assert data[3][0] == GroovyDeveloper
            assert data[3][1].value == "Guillaume"

            @TheSuperGroovyHeroes
            class Team {}

            assert Team.class.annotations.size() == 1
            assert Team.class.annotations[0] instanceof GroovyCoreTeam
            assert Team.class.annotations[0].value().size() == 4
            assert Team.class.annotations[0].value().collect { it.value() } == ['Paul', 'Cedric', 'Jochen', 'Guillaume']
        '''

        assertScript '''import groovy.transform.*
            @GroovyCoreTeam([
                @GroovyDeveloper('Paul'),
                @GroovyDeveloper('Cedric'),
                @GroovyDeveloper('Jochen'),
                @GroovyDeveloper('Guillaume')
            ])
            @AnnotationCollector
            @interface SuperHeroes {}

            def data = SuperHeroes.getAnnotation(AnnotationCollector).serializeClass().value()
            assert data instanceof Object[][]
            assert data.length == 1
            assert data[0].length == 2
            assert data[0][0] == groovy.transform.GroovyCoreTeam
            assert data[0][1] instanceof Map
            assert data[0][1].size() == 1
            data = data[0][1].value
            assert data.length == 4
            assert data[0][0] == GroovyDeveloper
            assert data[0][1].value == "Paul"
            assert data[1][0] == GroovyDeveloper
            assert data[1][1].value == "Cedric"
            assert data[2][0] == GroovyDeveloper
            assert data[2][1].value == "Jochen"
            assert data[3][0] == GroovyDeveloper
            assert data[3][1].value == "Guillaume"

            @SuperHeroes
            class Team {}

            assert Team.class.annotations.size() == 1
            assert Team.class.annotations[0] instanceof GroovyCoreTeam
            assert Team.class.annotations[0].value().size() == 4
            assert Team.class.annotations[0].value().collect { it.value() } == ['Paul', 'Cedric', 'Jochen', 'Guillaume']
        '''
    }

    @Test
    void testAnnotationCollectorModePreferCollector() {
        assertScript '''import groovy.transform.*
            @ToString(includeNames=true)
            @AnnotationCollector(mode=AnnotationCollectorMode.PREFER_COLLECTOR)
            @interface ToStringNames {}

            @ToString(excludes='prop1')
            @ToStringNames(excludes='prop2')
            class Dummy1 { String prop1, prop2 }

            @ToString(excludes='prop1')
            @ToStringNames
            class Dummy2 { String prop1, prop2 }

            assert new Dummy1(prop1: 'hello', prop2: 'goodbye').toString() == 'Dummy1(prop1:hello)'
            assert new Dummy2(prop1: 'hello', prop2: 'goodbye').toString() == 'Dummy2(prop1:hello, prop2:goodbye)'
        '''
    }

    @Test
    void testAnnotationCollectorModePreferCollectorMerged() {
        assertScript '''import groovy.transform.*
            @ToString(includeNames=true)
            @AnnotationCollector(mode=AnnotationCollectorMode.PREFER_COLLECTOR_MERGED)
            @interface ToStringNames {}

            @ToString(excludes='prop1')
            @ToStringNames(excludes='prop2')
            class Dummy1 { String prop1, prop2 }

            @ToString(excludes='prop1')
            @ToStringNames
            class Dummy2 { String prop1, prop2 }

            assert new Dummy1(prop1: 'hello', prop2: 'goodbye').toString() == 'Dummy1(prop1:hello)'
            assert new Dummy2(prop1: 'hello', prop2: 'goodbye').toString() == 'Dummy2(prop2:goodbye)'
        '''
    }

    @Test
    void testAnnotationCollectorModePreferCollectorExplicit() {
        assertScript '''import groovy.transform.*
            @ToString(includeNames=true)
            @AnnotationCollector(mode=AnnotationCollectorMode.PREFER_EXPLICIT)
            @interface ToStringNames {}

            @ToString(excludes='prop1')
            @ToStringNames(excludes='prop2')
            class Dummy1 { String prop1, prop2 }

            @ToString(excludes='prop1')
            @ToStringNames
            class Dummy2 { String prop1, prop2 }

            assert new Dummy1(prop1: 'hello', prop2: 'goodbye').toString() == 'Dummy1(goodbye)'
            assert new Dummy2(prop1: 'hello', prop2: 'goodbye').toString() == 'Dummy2(goodbye)'
        '''
    }

    @Test
    void testAnnotationCollectorModePreferCollectorExplicitMerged() {
        assertScript '''import groovy.transform.*
            @ToString(includeNames=true)
            @AnnotationCollector(mode=AnnotationCollectorMode.PREFER_EXPLICIT_MERGED)
            @interface ToStringNames {}

            @ToString(excludes='prop1')
            @ToStringNames(excludes='prop2')
            class Dummy1 { String prop1, prop2 }

            @ToString(excludes='prop1')
            @ToStringNames
            class Dummy2 { String prop1, prop2 }

            assert new Dummy1(prop1: 'hello', prop2: 'goodbye').toString() == 'Dummy1(prop2:goodbye)'
            assert new Dummy2(prop1: 'hello', prop2: 'goodbye').toString() == 'Dummy2(prop2:goodbye)'
        '''
    }
}

@AnnotationCollector([EqualsAndHashCode, ToString, Sortable])
@interface PreCompiledAlias {
}

@AnnotationCollector([ConditionalInterrupt])
@interface OtherPreCompiledAlias {
}

@Sortable
@ToString(excludes = ["a"])
@AnnotationCollector()
class PreCompiledAlias3 {
}

@Retention(RetentionPolicy.RUNTIME)
@interface GroovyCoreTeam {
    GroovyDeveloper[] value()
}

@Retention(RetentionPolicy.RUNTIME)
@interface GroovyDeveloper {
    String value() default "";
}

@GroovyCoreTeam([
    @GroovyDeveloper('Paul'),
    @GroovyDeveloper('Cedric'),
    @GroovyDeveloper('Jochen'),
    @GroovyDeveloper('Guillaume')
])
@AnnotationCollector
@interface TheSuperGroovyHeroes {
}
