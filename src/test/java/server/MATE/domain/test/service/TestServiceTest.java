package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Clock;
import server.MATE.domain.test.dto.request.TestUpdateRequest;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.image.event.ImageCleanupEvent;
import server.MATE.global.image.event.ImageDeleteEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private Clock clock;

    @InjectMocks
    private TestService testService;

    private server.MATE.domain.test.entity.Test test;
    private final Long MAKER_ID = 1L;
    private final Long TEST_ID = 10L;

    @BeforeEach
    void setUp() {
        test = server.MATE.domain.test.entity.Test.builder()
                .makerId(MAKER_ID)
                .title("기존 제목")
                .description("기존 소개")
                .serviceName("기존 서비스")
                .serviceDescription("기존 서비스 소개")
                .imageKeys(new ArrayList<>(List.of("old-key-1", "old-key-2")))
                .build();
        test.addCategories(List.of(Category.FOOD));
    }

    @Test
    void 이미지_교체_시_추가된_키는_CleanupEvent_제거된_키는_DeleteEvent_발행() {
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));

        // old: [old-key-1, old-key-2] → new: [old-key-1, new-key-1]
        TestUpdateRequest request = new TestUpdateRequest(null, null, null, null, null,
                List.of("old-key-1", "new-key-1"));
        testService.updateTest(TEST_ID, request, MAKER_ID);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<Object> events = captor.getAllValues();
        ImageCleanupEvent cleanupEvent = events.stream()
                .filter(e -> e instanceof ImageCleanupEvent)
                .map(e -> (ImageCleanupEvent) e)
                .findFirst().orElseThrow();
        ImageDeleteEvent deleteEvent = events.stream()
                .filter(e -> e instanceof ImageDeleteEvent)
                .map(e -> (ImageDeleteEvent) e)
                .findFirst().orElseThrow();

        assertThat(cleanupEvent.imageKeys()).containsExactly("new-key-1");
        assertThat(deleteEvent.imageKeys()).containsExactly("old-key-2");
    }

    @Test
    void 이미지_필드_생략_시_이벤트_미발행() {
        given(testRepository.findById(TEST_ID)).willReturn(Optional.of(test));

        TestUpdateRequest request = new TestUpdateRequest("새 제목", null, null, null, null, null);
        testService.updateTest(TEST_ID, request, MAKER_ID);

        verify(eventPublisher, never()).publishEvent(any());
    }
}
