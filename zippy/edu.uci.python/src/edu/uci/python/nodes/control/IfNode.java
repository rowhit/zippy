/*
 * Copyright (c) 2013, Regents of the University of California
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.uci.python.nodes.control;

import com.oracle.truffle.api.frame.VirtualFrame;

import edu.uci.python.ast.VisitorIF;
import edu.uci.python.nodes.EmptyNode;
import edu.uci.python.nodes.PNode;
import edu.uci.python.nodes.expression.CastToBooleanNode;
import edu.uci.python.nodes.statement.StatementNode;
import edu.uci.python.runtime.datatype.PNone;

public class IfNode extends StatementNode {

    @Child protected CastToBooleanNode condition;
    @Child protected PNode then;
    @Child protected PNode orelse;

    public IfNode(CastToBooleanNode condition, PNode then, PNode orelse) {
        this.condition = condition;
        this.then = then;
        this.orelse = orelse;
    }

    public static IfNode create(CastToBooleanNode condition, PNode then, PNode orelse) {
        if (!EmptyNode.isEmpty(orelse)) {
            return new IfNode(condition, then, orelse);
        } else {
            return new IfWithoutElseNode(condition, then);
        }
    }

    public CastToBooleanNode getCondition() {
        return condition;
    }

    public PNode getThen() {
        return then;
    }

    public PNode getElse() {
        return orelse;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        if (condition.executeBoolean(frame)) {
            return then.execute(frame);
        } else {
            return orelse.execute(frame);
        }
    }

    public static final class IfWithoutElseNode extends IfNode {

        public IfWithoutElseNode(CastToBooleanNode condition, PNode then) {
            super(condition, then, EmptyNode.create());
        }

        @Override
        public Object execute(VirtualFrame frame) {
            if (condition.executeBoolean(frame)) {
                return then.execute(frame);
            }

            return PNone.NONE;
        }
    }

    @Override
    public <R> R accept(VisitorIF<R> visitor) throws Exception {
        return visitor.visitIfNode(this);
    }
}
