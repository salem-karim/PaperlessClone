package at.technikum.Batch_Processor;

import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public record AccessLogXml(
        @JacksonXmlProperty(isAttribute = true) LocalDate date,
        @JacksonXmlProperty(isAttribute = true) String source,
        @JacksonXmlProperty(localName = "entry") @JacksonXmlElementWrapper(useWrapping = false) List<AccessEntryXml> entries) {
}
