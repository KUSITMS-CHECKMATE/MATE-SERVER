package server.MATE.global.image.event;

import java.util.List;

public record ImageDeleteEvent(List<String> imageKeys) {
}
