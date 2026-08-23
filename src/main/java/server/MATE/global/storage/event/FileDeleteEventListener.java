package server.MATE.global.storage.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.MATE.global.storage.service.FileStorageService;

@Component
@RequiredArgsConstructor
public class FileDeleteEventListener {

    private final FileStorageService fileStorageService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleFileDelete(FileDeleteEvent event) {
        if (event.fileKeys() != null && !event.fileKeys().isEmpty()) {
            fileStorageService.deleteFiles(event.fileKeys());
        }
    }
}
