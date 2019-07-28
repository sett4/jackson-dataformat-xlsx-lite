package com.github.sett4.dataformat.xlsx;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;

public abstract class ModuleTestBase extends junit.framework.TestCase {
    protected ModuleTestBase() {
    }

    ;

    protected XlsxMapper mapperForXLsx() {
        return new XlsxMapper();
    }

    /*
    /**********************************************************************
    /* Helper methods, setup
    /**********************************************************************
     */

    protected void assertToken(JsonToken expToken, JsonToken actToken) {
        if (actToken != expToken) {
            fail("Expected token " + expToken + ", current token " + actToken);
        }
    }

    /*
    /**********************************************************
    /* Helper methods; low-level
    /**********************************************************
     */

    protected void assertToken(JsonToken expToken, JsonParser jp) {
        assertToken(expToken, jp.currentToken());
    }

    protected void assertType(Object ob, Class<?> expType) {
        if (ob == null) {
            fail("Expected an object of type " + expType.getName() + ", got null");
        }
        Class<?> cls = ob.getClass();
        if (!expType.isAssignableFrom(cls)) {
            fail("Expected type " + expType.getName() + ", got " + cls.getName());
        }
    }

    /**
     * Method that gets textual contents of the current token using
     * available methods, and ensures results are consistent, before
     * returning them
     */
    protected String getAndVerifyText(JsonParser jp)
            throws IOException, JsonParseException {
        // Ok, let's verify other accessors
        int actLen = jp.getTextLength();
        char[] ch = jp.getTextCharacters();
        String str2 = new String(ch, jp.getTextOffset(), actLen);
        String str = jp.getText();

        if (str.length() != actLen) {
            fail("Internal problem (jp.token == " + jp.currentToken() + "): jp.getText().length() ['" + str + "'] == " + str.length() + "; jp.getTextLength() == " + actLen);
        }
        assertEquals("String access via getText(), getTextXxx() must be the same", str, str2);

        return str;
    }

    protected void verifyFieldName(JsonParser p, String expName)
            throws IOException {
        assertEquals(expName, p.getText());
        assertEquals(expName, p.currentName());
    }

    protected void verifyIntValue(JsonParser jp, long expValue)
            throws IOException {
        // First, via textual
        assertEquals(String.valueOf(expValue), jp.getText());
    }

    protected void verifyException(Throwable e, String... matches) {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings (" + Arrays.asList(matches) + "): got one with message \"" + msg + "\"");
    }

    public enum Gender {MALE, FEMALE}

    /**
     * Slightly modified sample class from Jackson tutorial ("JacksonInFiveMinutes")
     */
    @JsonPropertyOrder({"firstName", "lastName", "gender", "verified", "userImage", "i", "bigDecimal"})
    protected static class FiveMinuteUser {

        public String firstName, lastName;
        private Gender _gender;
        private boolean _isVerified;
        private byte[] _userImage;
        private int i;
        private BigDecimal bigDecimal;

        public FiveMinuteUser() {
        }

        public FiveMinuteUser(String first, String last, boolean verified, Gender g, int i, byte[] data, BigDecimal bigDecimal) {
            firstName = first;
            lastName = last;
            _isVerified = verified;
            _gender = g;
            this.i = i;
            _userImage = data;
            this.bigDecimal = bigDecimal;
        }

        public boolean isVerified() {
            return _isVerified;
        }

        public void setVerified(boolean b) {
            _isVerified = b;
        }

        public Gender getGender() {
            return _gender;
        }

        public void setGender(Gender g) {
            _gender = g;
        }

        public void setI(int i) {
            this.i = i;
        }

        public int getI() {
            return i;
        }

        public byte[] getUserImage() {
            return _userImage;
        }

//        public void setUserImage(byte[] b) {
//            _userImage = b;
//        }

        @Override
        public boolean equals(Object o) {
            if (o == this) return true;
            if (o == null || o.getClass() != getClass()) return false;
            FiveMinuteUser other = (FiveMinuteUser) o;
            if (_isVerified != other._isVerified) return false;
            if (_gender != other._gender) return false;
            if (!firstName.equals(other.firstName)) return false;
            if (!lastName.equals(other.lastName)) return false;
            byte[] otherImage = other._userImage;
            if (otherImage.length != _userImage.length) return false;
            for (int i = 0, len = _userImage.length; i < len; ++i) {
                if (_userImage[i] != otherImage[i]) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public int hashCode() {
            // not really good but whatever:
            return firstName.hashCode();
        }

        public BigDecimal getBigDecimal() {
            return bigDecimal;
        }

        public void setBigDecimal(BigDecimal bigDecimal) {
            this.bigDecimal = bigDecimal;
        }
    }

    @JsonPropertyOrder({"id", "desc"})
    protected static class IdDesc {
        public String id, desc;

        protected IdDesc() {
        }

        public IdDesc(String id, String desc) {
            this.id = id;
            this.desc = desc;
        }
    }
}
