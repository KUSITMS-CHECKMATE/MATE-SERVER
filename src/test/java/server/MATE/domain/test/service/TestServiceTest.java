package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import server.MATE.domain.test.dto.request.TestUpdateRequest;
import server.MATE.domain.test.dto.response.TestLikeResponse;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.TestLike;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.storage.event.FileCleanupEvent;
import server.MATE.global.storage.event.FileDeleteEvent;

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
    private TestLikeRepository testLikeRepository;

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
        ReflectionTestUtils.setField(test, "id", TEST_ID);
        test.addCategories(List.of(Category.FOOD));
    }

    @Test
    void 이미지_교체_시_추가된_키는_CleanupEvent_제거된_키는_DeleteEvent_발행() {
        given(testRepository.findByIdAndDeletedAtIsNull(TEST_ID)).willReturn(Optional.of(test));

        // old: [old-key-1, old-key-2] → new: [old-key-1, new-key-1]
        TestUpdateRequest request = new TestUpdateRequest(null, null, null, null, null,
                List.of("old-key-1", "new-key-1"));
        testService.updateTest(TEST_ID, request, MAKER_ID);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publishEvent(captor.capture());

        List<Object> events = captor.getAllValues();
        FileCleanupEvent cleanupEvent = events.stream()
                .filter(e -> e instanceof FileCleanupEvent)
                .map(e -> (FileCleanupEvent) e)
                .findFirst().orElseThrow();
        FileDeleteEvent deleteEvent = events.stream()
                .filter(e -> e instanceof FileDeleteEvent)
                .map(e -> (FileDeleteEvent) e)
                .findFirst().orElseThrow();

        assertThat(cleanupEvent.fileKeys()).containsExactly("new-key-1");
        assertThat(deleteEvent.fileKeys()).containsExactly("old-key-2");
    }

    @Test
    void 이미지_필드_생략_시_이벤트_미발행() {
        given(testRepository.findByIdAndDeletedAtIsNull(TEST_ID)).willReturn(Optional.of(test));

        TestUpdateRequest request = new TestUpdateRequest("새 제목", null, null, null, null, null);
        testService.updateTest(TEST_ID, request, MAKER_ID);

        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void 테스트_찜_시_찜_정보를_저장하고_카운트를_증가시킨다() {
        given(testRepository.findByIdAndDeletedAtIsNullForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(testLikeRepository.existsByUserIdAndTestId(MAKER_ID, TEST_ID)).willReturn(false);

        TestLikeResponse response = testService.likeTest(TEST_ID, MAKER_ID);

        assertThat(response.testId()).isEqualTo(TEST_ID);
        assertThat(response.isLiked()).isTrue();
        assertThat(response.likeCount()).isEqualTo(1L);
        verify(testLikeRepository).save(any(TestLike.class));
    }

    @Test
    void 테스트_찜_취소_시_찜_정보를_삭제하고_카운트를_감소시킨다() {
        TestLike testLike = TestLike.builder()
                .userId(MAKER_ID)
                .testId(TEST_ID)
                .build();
        test.incrementLikeCount();

        given(testRepository.findByIdAndDeletedAtIsNullForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(testLikeRepository.findByUserIdAndTestId(MAKER_ID, TEST_ID)).willReturn(Optional.of(testLike));

        TestLikeResponse response = testService.unlikeTest(TEST_ID, MAKER_ID);

        assertThat(response.testId()).isEqualTo(TEST_ID);
        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isZero();
        verify(testLikeRepository).delete(testLike);
    }
}
