package at.technikum.Batch_Processor;

import java.util.UUID;

public record AccessEntryXml(
        UUID documentId,
        int accessCount) {
}
