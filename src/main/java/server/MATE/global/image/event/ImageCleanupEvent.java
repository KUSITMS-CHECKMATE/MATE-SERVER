package server.MATE.global.image.event;

import java.util.List;

public record ImageCleanupEvent(List<String> imageKeys) {
}
