# jackson-dataformat-xlsx-lite

[![CircleCI](https://circleci.com/gh/sett4/jackson-dataformat-xlsx-lite.svg?style=svg)](https://circleci.com/gh/sett4/jackson-dataformat-xlsx-lite)

Generates Xlsx files with Jackson

This project uses a lot of copy from jackson-dataformat-csv.

# Usage

```xml
	<repositories>
		<repository>
		    <id>jitpack.io</id>
		    <url>https://jitpack.io</url>
		</repository>
	</repositories>
```

```xml
	<dependency>
	    <groupId>com.github.sett4</groupId>
	    <artifactId>jackson-dataformat-xlsx-lite</artifactId>
	    <version>2.9.9</version>
	</dependency>
```


```java
package com.github.sett4.dataformat.xlsx.serialize;

import com.fasterxml.jackson.databind.SequenceWriter;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.github.sett4.dataformat.xlsx.XlsxMapper;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class UsageTest {

    public static void main(String... args) throws IOException {
        File file = File.createTempFile("test", ".xlsx");
        System.out.println(file.getCanonicalPath());
        XlsxMapper mapper = new XlsxMapper();
        CsvSchema schema = mapper.schemaFor(Person.class).withHeader();
        SequenceWriter writer = mapper.writer(schema).writeValues(file);

        Person[] persons = new Person[]{
                new Person("Foo", "Bar"),
                new Person("Piyo", "Fuga")
        };
        for (Person p : persons) {
            writer.write(p);
        }
        writer.close();
    }

    public static class Person implements Serializable {
        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        String firstName;
        String lastName;

        public Person(String firstName, String lastName) {
            this.firstName = firstName;
            this.lastName = lastName;
        }
    }

}


```
