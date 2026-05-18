package server.MATE.global.storage.event;

import java.util.List;

public record FileCleanupEvent(List<String> fileKeys) {
}
