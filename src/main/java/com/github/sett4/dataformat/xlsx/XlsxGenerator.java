package com.github.sett4.dataformat.xlsx;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.GeneratorBase;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.core.json.JsonWriteContext;
import com.fasterxml.jackson.core.json.JsonWriteFeature;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.PackageVersion;
import com.github.sett4.dataformat.xlsx.impl.XlsxWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.logging.Logger;

public class XlsxGenerator extends GeneratorBase {
    Logger log = Logger.getLogger(XlsxGenerator.class.getCanonicalName());

    //    private final IOContext _outputContext;
    private final int xlsxFeatures;
    private CsvSchema _schema;
    private final XlsxWriter _writer;

    /**
     * Flag that indicates that we need to write header line, if
     * one is needed. Used because schema may be specified after
     * instance is constructed.
     */
    protected boolean _handleFirstLine = true;

    /**
     * Index of column that we will be getting next, based on
     * field name call that was made.
     */
    protected int _nextColumnByName = -1;

    /**
     * Flag set when property to write is unknown, and the matching value
     * is to be skipped quietly.
     *
     * @since 2.5
     */
    protected boolean _skipValue;

    /**
     * Separator to use during writing of (simple) array value, to be encoded as a
     * single column value, if any.
     *
     * @since 2.5
     */
    protected String _arraySeparator = CsvSchema.NO_ARRAY_ELEMENT_SEPARATOR;

    /**
     * Accumulated contents of an array cell, if any
     */
    protected StringBuilder _arrayContents;

    /**
     * Additional counter that indicates number of value entries in the
     * array. Needed because `null` entries do not add content, but need
     * to be separated by array cell separator
     *
     * @since 2.7
     */
    protected int _arrayElements;

    /**
     * When skipping output (for "unknown" output), outermost write context
     * where skipping should occur
     */
    protected JsonWriteContext _skipWithin;

    public XlsxGenerator(IOContext ioCtxt,
                         int generatorFeatures, int xlsxFeatures,
                         ObjectCodec codec, OutputStream out, CsvSchema schema) {
        super(generatorFeatures, codec);
//        this._outputContext = ioCtxt;
        this.xlsxFeatures = xlsxFeatures;
        this._writer = new XlsxWriter(out);
        this._schema = schema;
    }

       /*
    /**********************************************************
    /* Versioned
    /**********************************************************
     */

    @Override
    public Version version() {
        return PackageVersion.VERSION;
    }

    /*
    /**********************************************************
    /* Overridden methods, configuration
    /**********************************************************
     */

    /**
     * No way (or need) to indent anything, so let's block any attempts.
     * (should we throw an exception instead?)
     */
    @Override
    public XlsxGenerator useDefaultPrettyPrinter() {
        return this;
    }

    /**
     * No way (or need) to indent anything, so let's block any attempts.
     * (should we throw an exception instead?)
     */
    @Override
    public XlsxGenerator setPrettyPrinter(PrettyPrinter pp) {
        return this;
    }

    @Override
    public Object getOutputTarget() {
        return _writer.getOutputTarget();
    }


    @Override
    public void setSchema(FormatSchema schema) {
        if (schema instanceof CsvSchema) {
            if (_schema != schema) {
                _schema = (CsvSchema) schema;
            }
        } else {
            super.setSchema(schema);
        }
    }

    @Override
    public int getFormatFeatures() {
        return xlsxFeatures;
    }

    /*
    /**********************************************************
    /* Public API, capability introspection methods
    /**********************************************************
     */

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return (schema instanceof CsvSchema);
    }

    @Override
    public boolean canOmitFields() {
        // Nope: CSV requires at least a placeholder
        return false;
    }

    @Override
    public boolean canWriteFormattedNumbers() {
        return true;
    }

    /*
    /**********************************************************
    /* Public API: low-level I/O
    /**********************************************************
     */

    @Override
    public void flush() throws IOException {
        log.fine("flush");
    }

    @Override
    public void close() throws IOException {
        log.fine("close");

        super.close();

        // Let's mark row as closed, if we had any...
        finishRow();

        // Write the header if necessary, occurs when no rows written
        if (_handleFirstLine) {
            _handleFirstLine();
        }
        _writer.close();
    }

    /*
    /**********************************************************
    /* Public API: structural output
    /**********************************************************
     */

    @Override
    public final void writeStartArray() throws IOException {
        _verifyValueWrite("start an array");
        // Ok to create root-level array to contain Objects/Arrays, but
        // can not nest arrays in objects
        if (_writeContext.inObject()) {
            if ((_skipWithin == null)
                    && _skipValue && isEnabled(JsonGenerator.Feature.IGNORE_UNKNOWN)) {
                _skipWithin = _writeContext;
            } else if (!_skipValue) {
                // First: column may have its own separator
                String sep;
                if (_nextColumnByName >= 0) {
                    CsvSchema.Column col = _schema.column(_nextColumnByName);
                    sep = col.isArray() ? col.getArrayElementSeparator() : CsvSchema.NO_ARRAY_ELEMENT_SEPARATOR;
                } else {
                    sep = CsvSchema.NO_ARRAY_ELEMENT_SEPARATOR;
                }
                if (sep.isEmpty()) {
                    if (!_schema.hasArrayElementSeparator()) {
                        _reportError("CSV generator does not support Array values for properties without setting 'arrayElementSeparator' in schema");
                    }
                    sep = _schema.getArrayElementSeparator();
                }
                _arraySeparator = sep;
                if (_arrayContents == null) {
                    _arrayContents = new StringBuilder();
                } else {
                    _arrayContents.setLength(0);
                }
                _arrayElements = 0;
            }
        } else {
            if (!_arraySeparator.isEmpty()) {
                // also: no nested arrays, yet
                _reportError("CSV generator does not support nested Array values");
            }
        }
        _writeContext = _writeContext.createChildArrayContext();
        // and that's about it, really
    }

    @Override
    public final void writeEndArray() throws IOException {
        if (!_writeContext.inArray()) {
            _reportError("Current context not Array but " + _writeContext.typeDesc());
        }
        _writeContext = _writeContext.getParent();
        // 14-Dec-2015, tatu: To complete skipping of ignored structured value, need this:
        if (_skipWithin != null) {
            if (_writeContext == _skipWithin) {
                _skipWithin = null;
            }
            return;
        }
        if (!_arraySeparator.isEmpty()) {
            _arraySeparator = CsvSchema.NO_ARRAY_ELEMENT_SEPARATOR;
            _writer.write(_columnIndex(), _arrayContents.toString());
        }
        // 20-Nov-2014, tatu: When doing "untyped"/"raw" output, this means that row
        //    is now done. But not if writing such an array field, so:
        if (!_writeContext.inObject()) {
            finishRow();
        }
    }


    @Override
    public final void writeStartObject() throws IOException {
        log.fine("writeStartObject");
        _verifyValueWrite("start an object");
        // No nesting for objects; can write Objects inside logical root-level arrays.
        // 14-Dec-2015, tatu: ... except, should be fine if we are ignoring the property
        if (_writeContext.inObject() ||
                // 07-Nov-2017, tatu: But we may actually be nested indirectly; so check
                (_writeContext.inArray() && !_writeContext.getParent().inRoot())) {
            if (_skipWithin == null) { // new in 2.7
                if (_skipValue && isEnabled(JsonGenerator.Feature.IGNORE_UNKNOWN)) {
                    _skipWithin = _writeContext;
                } else {
                    _reportMappingError("CSV generator does not support Object values for properties (nested Objects)");
                }
            }
        }
        _writeContext = _writeContext.createChildObjectContext();
    }

    @Override
    public final void writeEndObject() throws IOException {
        log.fine("writeEndObject");
        if (!_writeContext.inObject()) {
            _reportError("Current context not Object but " + _writeContext.typeDesc());
        }
        _writeContext = _writeContext.getParent();
        // 14-Dec-2015, tatu: To complete skipping of ignored structured value, need this:
        if (_skipWithin != null) {
            if (_writeContext == _skipWithin) {
                _skipWithin = null;
            }
            return;
        }
        // not 100% fool-proof, but chances are row should be done now
        finishRow();
    }


    /*
     * Method called when the current row is complete; typically
     * will flush possibly buffered column values, append linefeed
     * and reset state appropriately.
     */
    protected void finishRow() throws IOException {
        log.fine("finishRow");
        _writer.endRow();
        _nextColumnByName = -1;
    }

    @Override
    public void writeFieldName(String name) throws IOException {
        log.fine("writeFieldName: " + name);
        if (_writeContext.writeFieldName(name) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name);
    }

    @Override
    public final void writeFieldName(SerializableString name) throws IOException {
        log.fine("writeFieldName: " + name);
        // Object is a value, need to verify it's allowed
        if (_writeContext.writeFieldName(name.getValue()) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(name.getValue());
    }

    @Override
    public final void writeStringField(String fieldName, String value) throws IOException {
        log.fine("writeStringField");
        if (_writeContext.writeFieldName(fieldName) == JsonWriteContext.STATUS_EXPECT_VALUE) {
            _reportError("Can not write a field name, expecting a value");
        }
        _writeFieldName(fieldName);
        writeString(value);
    }

    private void _writeFieldName(String name) throws IOException {
        // just find the matching index -- must have schema for that
        if (_schema == null) {
            // not a low-level error, so:
            _reportMappingError("Unrecognized column '" + name + "', can not resolve without CsvSchema");
        }
        if (_skipWithin != null) { // new in 2.7
            _skipValue = true;
            _nextColumnByName = -1;
            return;
        }
        // note: we are likely to get next column name, so pass it as hint
        CsvSchema.Column col = _schema.column(name, _nextColumnByName + 1);
        if (col == null) {
            if (isEnabled(JsonGenerator.Feature.IGNORE_UNKNOWN)) {
                _skipValue = true;
                _nextColumnByName = -1;
                return;
            }
            // not a low-level error, so:
            _reportMappingError("Unrecognized column '" + name + "': known columns: " + _schema.getColumnDesc());
        }
        _skipValue = false;
        // and all we do is just note index to use for following value write
        _nextColumnByName = col.getIndex();
    }

    @Override
    public void writeString(String text) throws IOException {
        log.fine("writeString");

        if (text == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write String value");
        if (!_skipValue) {
            if (!_arraySeparator.isEmpty()) {
                _addToArray(text);
            } else {
                _writer.write(_columnIndex(), text);
            }
        }
    }

    @Override
    public void writeString(char[] chars, int i, int i1) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void writeRawUTF8String(byte[] bytes, int i, int i1) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void writeUTF8String(byte[] bytes, int i, int i1) throws IOException {
        throw new IllegalStateException("not implemented");
    }


    @Override
    public void writeRaw(String text) throws IOException {
        _writer.write(_columnIndex(), text);
    }

    @Override
    public void writeRaw(String text, int offset, int len) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void writeRaw(char[] text, int offset, int len) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    public void writeRaw(char c) throws IOException {
        _writer.write(_columnIndex(), c);
    }

    @Override
    public void writeRawValue(String text) throws IOException {
        _verifyValueWrite("write Raw value");
        if (!_skipValue) {
            // NOTE: ignore array stuff
            _writer.write(_columnIndex(), text);
        }
    }

    @Override
    public void writeRawValue(String text, int offset, int len) throws IOException {
        _verifyValueWrite("write Raw value");
        if (!_skipValue) {
            // NOTE: ignore array stuff
            _writer.write(_columnIndex(), text.substring(offset, offset + len));
        }
    }

    @Override
    public void writeRawValue(char[] text, int offset, int len) throws IOException {
        _verifyValueWrite("write Raw value");
        if (!_skipValue) {
            // NOTE: ignore array stuff
            _writer.write(_columnIndex(), new String(text, offset, len));
        }
    }

    @Override
    public void writeBinary(Base64Variant b64variant, byte[] data, int offset, int len) throws IOException {
        log.fine("writeRaw");
        if (data == null) {
            writeNull();
            return;
        }
        _verifyValueWrite("write Binary value");
        if (!_skipValue) {
            // ok, better just Base64 encode as a String...
            if (offset > 0 || (offset + len) != data.length) {
                data = Arrays.copyOfRange(data, offset, offset + len);
            }
            String encoded = b64variant.encode(data);

            if (!_arraySeparator.isEmpty()) {
                _addToArray(encoded);
            } else {
                _writer.write(_columnIndex(), encoded);
            }
        }
    }

    public void writeNumber(int v) throws IOException {
        this._verifyValueWrite("write number");
        if (!this._skipValue) {
            if (!this._arraySeparator.isEmpty()) {
                this._addToArray(String.valueOf(v));
            } else {
                this._writer.write(this._columnIndex(), v);
            }
        }

    }

    public void writeNumber(long v) throws IOException {
        if (v <= 2147483647L && v >= -2147483648L) {
            this.writeNumber((int) v);
        } else {
            this._verifyValueWrite("write number");
            if (!this._skipValue) {
                if (!this._arraySeparator.isEmpty()) {
                    this._addToArray(String.valueOf(v));
                } else {
                    this._writer.write(this._columnIndex(), v);
                }
            }

        }
    }

    public void writeNumber(BigInteger v) throws IOException {
        if (v == null) {
            this.writeNull();
        } else {
            this._verifyValueWrite("write number");
            if (!this._skipValue) {
                if (!this._arraySeparator.isEmpty()) {
                    this._addToArray(String.valueOf(v));
                } else {
                    this._writer.write(this._columnIndex(), v.doubleValue());
                }
            }

        }
    }

    public void writeNumber(double v) throws IOException {
        this._verifyValueWrite("write number");
        if (!this._skipValue) {
            if (!this._arraySeparator.isEmpty()) {
                this._addToArray(String.valueOf(v));
            } else {
                this._writer.write(this._columnIndex(), v);
            }
        }

    }

    public void writeNumber(float v) throws IOException {
        this._verifyValueWrite("write number");
        if (!this._skipValue) {
            if (!this._arraySeparator.isEmpty()) {
                this._addToArray(String.valueOf(v));
            } else {
                this._writer.write(this._columnIndex(), v);
            }
        }

    }

    public void writeNumber(BigDecimal v) throws IOException {
        if (v == null) {
            this.writeNull();
        } else {
            this._verifyValueWrite("write number");
            if (!this._skipValue) {
                if (isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)) {
                    String str = this.isEnabled(StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN)
                            ? v.toPlainString() : v.toString();
                    if (!this._arraySeparator.isEmpty()) {
                        this._addToArray(String.valueOf(v));
                    } else {
                        this._writer.write(this._columnIndex(), str);
                    }
                } else {
                    this._writer.write(this._columnIndex(), v.doubleValue());
                }
            }

        }
    }

    public void writeNumber(String encodedValue) throws IOException {
        if (encodedValue == null) {
            this.writeNull();
        } else {
            this._verifyValueWrite("write number");
            if (!this._skipValue) {
                if (!this._arraySeparator.isEmpty()) {
                    this._addToArray(encodedValue);
                } else {
                    this._writer.write(this._columnIndex(), encodedValue);
                }
            }

        }
    }

    public void writeBoolean(boolean state) throws IOException {
        this._verifyValueWrite("write boolean value");
        if (!this._skipValue) {
            if (!this._arraySeparator.isEmpty()) {
                this._addToArray(state ? "true" : "false");
            } else {
                this._writer.write(this._columnIndex(), state);
            }
        }

    }

    @Override
    public void writeNull() throws IOException {
        this._verifyValueWrite("write null value");
        if (!this._skipValue) {
            if (!this._arraySeparator.isEmpty()) {
                this._addToArray(this._schema.getNullValueOrEmpty());
            } else if (this._writeContext.inObject()) {
                this._writer.writeNull(this._columnIndex());
            } else if (this._writeContext.inArray() && !this._writeContext.getParent().inRoot()) {
                this._writer.writeNull(this._columnIndex());
            }
        }
    }

    @Override
    protected void _releaseBuffers() {

    }

    @Override
    protected void _verifyValueWrite(String typeMsg) throws IOException {
        int status = _writeContext.writeValue();
        if (status == JsonWriteContext.STATUS_EXPECT_NAME) {
            _reportError("Can not " + typeMsg + ", expecting field name");
        }
        if (_handleFirstLine) {
            _handleFirstLine();
        }
    }

    protected void _handleFirstLine() throws IOException {
        _handleFirstLine = false;
        if (_schema.usesHeader()) {
            int count = _schema.size();
            if (count == 0) {
                _reportMappingError("Schema specified that header line is to be written; but contains no column names");
            }
            for (CsvSchema.Column column : _schema) {
                _writer.writeColumnName(column.getName(), column.getIndex());
            }
            finishRow();
        }
    }

    protected void _reportMappingError(String msg) throws JsonProcessingException {
//        throw CsvMappingException.from((CsvGenerator)this, msg, _schema);
        throw new JsonGenerationException(msg, this);
    }

        /*
    /**********************************************************
    /* Internal methods, other
    /**********************************************************
     */

    protected final int _columnIndex() {
        int ix = _nextColumnByName;
        if (ix < 0) { // if we had one, remove now
            throw new IllegalStateException("ix<0");
        }
        return ix;
    }

    protected void _addToArray(String value) {
        if (_arrayElements > 0) {
            _arrayContents.append(_arraySeparator);
        }
        ++_arrayElements;
        _arrayContents.append(value);
    }

    protected void _addToArray(char[] value) {
        if (_arrayElements > 0) {
            _arrayContents.append(_arraySeparator);
        }
        ++_arrayElements;
        _arrayContents.append(value);
    }
}
