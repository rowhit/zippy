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
package edu.uci.python.nodes.truffle;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.python.core.Py;
import org.python.core.PyArray;
import org.python.core.PyComplex;
import org.python.core.PyDictionary;
import org.python.core.PyFloat;
import org.python.core.PyInteger;
import org.python.core.PyList;
import org.python.core.PyLong;
import org.python.core.PyObject;
import org.python.core.PySet;
import org.python.core.PyString;
import org.python.core.PyTuple;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;

import edu.uci.python.runtime.array.PArray;
import edu.uci.python.runtime.array.PCharArray;
import edu.uci.python.runtime.array.PDoubleArray;
import edu.uci.python.runtime.array.PIntArray;
import edu.uci.python.runtime.datatype.PComplex;
import edu.uci.python.runtime.datatype.PDict;
import edu.uci.python.runtime.datatype.PFrozenSet;
import edu.uci.python.runtime.datatype.PNone;
import edu.uci.python.runtime.datatype.PRange;
import edu.uci.python.runtime.function.PFunction;
import edu.uci.python.runtime.object.PythonObject;
import edu.uci.python.runtime.sequence.PList;
import edu.uci.python.runtime.sequence.PSet;
import edu.uci.python.runtime.sequence.PTuple;

public class PythonTypesUtil {

    public static PyTuple createPyTuple(Object[] values) {
        PyObject[] adaptedValue = new PyObject[values.length];

        for (int i = 0; i < values.length; i++) {
            adaptedValue[i] = adaptToPyObject(values[i]);
        }

        return new PyTuple(adaptedValue);
    }

    @TruffleBoundary
    public static PyObject adaptToPyObject(Object value) {
        CompilerAsserts.neverPartOfCompilation();

        if (value instanceof PyObject) {
            return (PyObject) value;
        }

        if (value instanceof Boolean) {
            return Py.newBoolean((boolean) value);
        } else if (value instanceof Integer) {
            return Py.newInteger((int) value);
        } else if (value instanceof Long) {
            return Py.newLong((long) value);
        } else if (value instanceof BigInteger) {
            return Py.newLong((BigInteger) value);
        } else if (value instanceof Double) {
            return Py.newFloat((double) value);
        } else if (value instanceof PComplex) {
            PComplex complex = (PComplex) value;
            PyComplex pyComplex = new PyComplex(complex.getReal(), complex.getImag());
            return pyComplex;
        } else if (value instanceof String) {
            try {
                return Py.newString((String) value);
            } catch (Exception e) {
                return Py.newUnicode((String) value);
            }
        } else if (value instanceof PNone) {
            return Py.None;
        } else if (value instanceof PTuple) {
            PTuple tuple = (PTuple) value;
            return new PyTuple(adaptToPyObjects(tuple.getArray()));
        } else if (value instanceof PList) {
            PList list = (PList) value;
            PyObject[] pyObjs = new PyObject[list.len()];

            for (int i = 0; i < list.len(); i++) {
                pyObjs[i] = adaptToPyObject(list.getItem(i));
            }

            return new PyList(pyObjs);
        } else if (value instanceof PSet) {
            PSet set = (PSet) value;
            return new PySet(adaptToPyObjects(set.getSet().toArray()));
        } else if (value instanceof PFrozenSet) {
            PFrozenSet set = (PFrozenSet) value;
            return new PySet(adaptToPyObjects(set.getSet().toArray()));
        } else if (value instanceof PDict) {
            PDict dict = (PDict) value;
            ConcurrentHashMap<PyObject, PyObject> map = new ConcurrentHashMap<>();
            for (Object key : dict.keys()) {
                map.put(adaptToPyObject(key), adaptToPyObject(dict.getItem(key)));
            }
            return new PyDictionary(map);
        } else if (value instanceof PIntArray) {
            return new PyArray(int.class, ((PIntArray) value).getSequence());
        } else if (value instanceof PDoubleArray) {
            return new PyArray(double.class, ((PDoubleArray) value).getSequence());
        } else if (value instanceof PCharArray) {
            return new PyArray(char.class, ((PCharArray) value).getSequence());
        } else if (value instanceof PythonObject) {
            return adoptToPyClass((PythonObject) value);
        } else if (value instanceof PFunction) {
            /**
             * zwei: This is a fix to deal with string modulo operation. It only works if this
             * static function is invoked by the binary modulo node.
             */
            return new PyString(value.toString());
        }

        throw new RuntimeException("unexpected type! " + value.getClass());
    }

    // TODO: Future support
    public static PyObject adoptToPyClass(PythonObject object) {
        throw new RuntimeException("type not supported to adopt to Jython! " + object.toString() + "  class:" + object.getClass());
    }

    @TruffleBoundary
    public static PyObject[] adaptToPyObjects(Object[] values) {
        List<PyObject> converted = new ArrayList<>(values.length);

        for (Object value : values) {
            /**
             * The null check here is to deal with the case where the physical storage of a sequence
             * might have unused spaces.
             */
            if (value != null) {
                converted.add(adaptToPyObject(value));
            }
        }

        return converted.toArray(new PyObject[values.length]);
    }

    @TruffleBoundary
    public static PyObject jythonCall(PyObject callee, Object[] pyargs) {
        return callee.__call__(adaptToPyObjects(pyargs));
    }

    @TruffleBoundary
    public static Object unboxPyObject(PyObject value) {
        if (value instanceof PyInteger) {
            return ((PyInteger) value).getValue();
        } else if (value instanceof PyString) {
            return ((PyString) value).getString();
        } else if (value instanceof PyFloat) {
            return ((PyFloat) value).getValue();
        } else if (value instanceof PyLong) {
            return ((PyLong) value).getValue();
        } else if (value instanceof PyTuple) {
            PyTuple tuple = (PyTuple) value;
            return new PTuple(unboxPyObjects(tuple.getArray()));
        } else if (value instanceof PyArray) {
            // TODO Temporary fix
            PyList array = (PyList) ((PyArray) value).tolist();
            return unboxPyObject(array);
        }
        // fall back is just return as it is.
        return value;
    }

    public static Object[] unboxPyObjects(PyObject[] values) {
        Object[] unboxed = new Object[values.length];

        int i = 0;
        for (PyObject value : values) {
            unboxed[i++] = unboxPyObject(value);
        }

        return unboxed;
    }

    /**
     * TODO This method should be extended to support the remaining types.
     */
    public static String getPythonTypeName(Object object) {
        CompilerAsserts.neverPartOfCompilation();

        if (object instanceof Integer || object instanceof BigInteger) {
            return "int";
        } else if (object instanceof Double) {
            return "float";
        } else if (object instanceof PComplex) {
            return "complex";
        } else if (object instanceof String) {
            return "str";
        } else if (object instanceof PList) {
            return "list";
        } else if (object instanceof PTuple) {
            return "tuple";
        } else if (object instanceof PRange) {
            return "range";
        } else if (object instanceof PArray) {
            return "array";
        } else if (object instanceof PSet) {
            return "set";
        } else if (object instanceof PDict) {
            return "dict";
        } else if (object instanceof PNone) {
            return "None";
        }

        throw new RuntimeException("Unsupported type name " + object.getClass());
    }

}
