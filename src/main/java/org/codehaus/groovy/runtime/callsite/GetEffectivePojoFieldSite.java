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
package org.codehaus.groovy.runtime.callsite;

import groovy.lang.MetaClassImpl;
import org.codehaus.groovy.reflection.CachedField;
import org.codehaus.groovy.runtime.GroovyCategorySupport;

@Deprecated
class GetEffectivePojoFieldSite extends AbstractCallSite {
    private final MetaClassImpl metaClass;
    private final CachedField effective;
    private final int metaClassVersion;

    public GetEffectivePojoFieldSite(final CallSite site, final MetaClassImpl metaClass, final CachedField effective) {
        super(site);
        this.metaClass = metaClass;
        this.effective = effective;
        this.metaClassVersion = metaClass.getVersion();
    }

    @Override
    public final CallSite acceptGetProperty(final Object receiver) {
        if (GroovyCategorySupport.hasCategoryInCurrentThread()
            || metaClass.getTheClass() != receiver.getClass()
            || metaClass.getVersion() != metaClassVersion) {

            return createGetPropertySite(receiver);
        }
        return this;
    }

    @Override
    public final Object getProperty(final Object receiver) {
        return effective.getProperty(receiver);
    }
}
