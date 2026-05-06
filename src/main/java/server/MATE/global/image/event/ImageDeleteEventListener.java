package server.MATE.global.image.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.global.image.ImageService;

@Component
@RequiredArgsConstructor
public class ImageDeleteEventListener {

    private final ImageService imageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleImageDelete(ImageDeleteEvent event) {
        if (event.imageKeys() != null && !event.imageKeys().isEmpty()) {
            imageService.deleteFiles(event.imageKeys());
        }
    }
}
