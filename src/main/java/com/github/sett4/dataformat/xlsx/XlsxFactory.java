package com.github.sett4.dataformat.xlsx;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.io.IOContext;
import com.fasterxml.jackson.dataformat.csv.CsvGenerator;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.impl.CsvIOContext;

import java.io.*;
import java.net.URL;

public class XlsxFactory
        extends JsonFactory
        implements Serializable {
    /**
     * Name used to identify CSV format.
     * (and returned by {@link #getFormatName()}
     */
    public final static String FORMAT_NAME_XLSX = "XLSX";
    protected static final CsvSchema DEFAULT_SCHEMA = CsvSchema.emptySchema();

    /*
    /**********************************************************************
    /* Configuration
    /**********************************************************************
     */
    private static final long serialVersionUID = 1L;
//    protected final int _formatParserFeatures;

    /*
    protected char _cfgColumnSeparator = ',';

    protected char _cfgQuoteCharacter = '"';

    protected char[] _cfgLineSeparator = DEFAULT_LF;
    */
//    protected final int _formatGeneratorFeatures;

    protected CsvSchema _schema;

    /*
    /**********************************************************************
    /* Factory construction, configuration
    /**********************************************************************
     */

    /**
     * Default constructor used to create factory instances.
     * Creation of a factory instance is a light-weight operation,
     * but it is still a good idea to reuse limited number of
     * factory instances (and quite often just a single instance):
     * factories are used as context for storing some reused
     * processing objects (such as symbol tables parsers use)
     * and this reuse only works within context of a single
     * factory instance.
     */
    public XlsxFactory() {
        super();
//        _formatParserFeatures = -1;
//        _formatGeneratorFeatures = -1;
    }

    public XlsxFactory(ObjectCodec oc) {
        super(oc);
    }

    protected XlsxFactory(XlsxFactory src, ObjectCodec oc) {
        super(src, oc);
//        _formatParserFeatures = src._formatParserFeatures;
//        _formatGeneratorFeatures = src._formatGeneratorFeatures;
        this._schema = src._schema;

    }


    @Override
    public XlsxFactory copy() {
        _checkInvalidCopy(XlsxFactory.class);
        return new XlsxFactory(this, null);
    }


    /*
    /**********************************************************
    /* Serializable overrides
    /**********************************************************
     */

    /**
     * Method that we need to override to actually make restoration go
     * through constructors etc.
     * Also: must be overridden by sub-classes as well.
     */
    protected Object readResolve() {
        return new XlsxFactory(this, _objectCodec);
    }

    /*
    /**********************************************************
    /* Capability introspection
    /**********************************************************
     */

    @Override
    public Version version() {
        return com.github.sett4.xlsx.PackageVersion.VERSION;
    }

    // Yes; CSV is positional
    @Override
    public boolean requiresPropertyOrdering() {
        return true;
    }

    // No, we can't make use of char[] optimizations
    @Override
    public boolean canUseCharArrays() {
        return false;
    }

    @Override
    public boolean canParseAsync() {
        // 30-Sep-2017, tatu: No async parsing yet
        return false;
    }

    /*
    /**********************************************************
    /* Format support
    /**********************************************************
     */

    @Override
    public String getFormatName() {
        return FORMAT_NAME_XLSX;
    }

    @Override
    public boolean canUseSchema(FormatSchema schema) {
        return (schema instanceof CsvSchema);
    }

    @Override
    public Class<CsvParser.Feature> getFormatReadFeatureType() {
        return CsvParser.Feature.class;
    }

    @Override
    public Class<CsvGenerator.Feature> getFormatWriteFeatureType() {
        return CsvGenerator.Feature.class;
    }


    /*
    /**********************************************************
    /* Overridden parser factory methods, 2.1
    /**********************************************************
     */

    @Override
    public XlsxParser createParser(File f) throws IOException {
        IOContext ctxt = _createContext(f, true);
        return _createParser(_decorate(new FileInputStream(f), ctxt), ctxt);
    }

    @Override
    public XlsxParser createParser(URL url) throws IOException {
        IOContext ctxt = _createContext(url, true);
        return _createParser(_decorate(_optimizedStreamFromURL(url), ctxt), ctxt);
    }

    @Override
    public XlsxParser createParser(InputStream in) throws IOException {
        IOContext ctxt = _createContext(in, false);
        return _createParser(_decorate(in, ctxt), ctxt);
    }

    @Override
    public XlsxParser createParser(Reader r) throws IOException {
        IOContext ctxt = _createContext(r, false);
        return _createParser(_decorate(r, ctxt), ctxt);
    }

    @Override
    public XlsxParser createParser(String doc) throws IOException {
        return (XlsxParser) super.createParser(doc);
    }

    @Override
    public XlsxParser createParser(byte[] data) throws IOException {
        return (XlsxParser) super.createParser(data);
    }

    @Override
    public XlsxParser createParser(byte[] data, int offset, int len) throws IOException {
        return (XlsxParser) super.createParser(data, offset, len);
    }

    @Override
    public XlsxParser createParser(char[] data) throws IOException {
        return (XlsxParser) super.createParser(data);
    }

    @Override
    public CsvParser createParser(char[] data, int offset, int len) throws IOException {
        return (CsvParser) super.createParser(data, offset, len);
    }
    
    /*
    /******************************************************
    /* Factory methods: generators
    /******************************************************
     */


    /*
    /******************************************************
    /* Overridden internal factory methods
    /******************************************************
     */

    //protected IOContext _createContext(Object srcRef, boolean resourceManaged)

    /**
     * Overridable factory method that actually instantiates desired parser.
     */
    @Override
    protected XlsxParser _createParser(InputStream in, IOContext ctxt) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    protected XlsxParser _createParser(byte[] data, int offset, int len, IOContext ctxt) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    /**
     * Overridable factory method that actually instantiates desired parser.
     */
    @Override
    protected XlsxParser _createParser(Reader r, IOContext ctxt) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    protected XlsxParser _createParser(char[] data, int offset, int len, IOContext ctxt,
                                       boolean recyclable) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    protected XlsxGenerator _createGenerator(Writer out, IOContext ctxt) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    @Override
    protected XlsxGenerator _createUTF8Generator(OutputStream out, IOContext ctxt) throws IOException {
        return _createGenerator(ctxt, out);
    }

    @Override
    protected Writer _createWriter(OutputStream out, JsonEncoding enc, IOContext ctxt) throws IOException {
        throw new IllegalStateException("not implemented");
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */

    @Override
    protected IOContext _createContext(Object srcRef, boolean resourceManaged) {
        return new CsvIOContext(_getBufferRecycler(), srcRef, resourceManaged);
    }

    private XlsxGenerator _createGenerator(IOContext ctxt, OutputStream out) throws IOException {
        XlsxGenerator gen = new XlsxGenerator(ctxt, _generatorFeatures, -1,
                _objectCodec, out, _schema);
        // any other initializations? No?
        return gen;
    }
}
