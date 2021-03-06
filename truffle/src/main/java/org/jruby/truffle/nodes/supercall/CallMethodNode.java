/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.nodes.supercall;

import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeChildren;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.source.SourceSection;
import org.jruby.truffle.nodes.RubyNode;
import org.jruby.truffle.nodes.dispatch.DispatchNode;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.methods.InternalMethod;

@NodeChildren({
        @NodeChild("method"),
        @NodeChild(value = "arguments", type = RubyNode[].class)
})
public abstract class CallMethodNode extends RubyNode {

    protected static int getCacheLimit() {
        return DispatchNode.DISPATCH_POLYMORPHIC_MAX;
    }

    public CallMethodNode(RubyContext context, SourceSection sourceSection) {
        super(context, sourceSection);
    }

    public abstract Object executeCallMethod(VirtualFrame frame, InternalMethod method, Object[] arguments);

    @Specialization(
            guards = "method == cachedMethod",
            // TODO(eregon, 12 June 2015) we should maybe check an Assumption here to remove the cache entry when the lookup changes (redefined method, hierarchy changes)
            limit = "getCacheLimit()")
    protected Object callMethodCached(VirtualFrame frame, InternalMethod method, Object[] arguments,
            @Cached("method") InternalMethod cachedMethod,
            @Cached("create(cachedMethod.getCallTarget())") DirectCallNode callNode) {
        return callNode.call(frame, arguments);
    }

    @Specialization
    protected Object callMethodUncached(VirtualFrame frame, InternalMethod method, Object[] arguments,
            @Cached("create()") IndirectCallNode indirectCallNode) {
        return indirectCallNode.call(frame, method.getCallTarget(), arguments);
    }

}
