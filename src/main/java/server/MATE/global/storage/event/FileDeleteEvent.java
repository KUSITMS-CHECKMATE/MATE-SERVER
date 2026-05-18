package server.MATE.global.storage.event;

import java.util.List;

public record FileDeleteEvent(List<String> fileKeys) {
}
