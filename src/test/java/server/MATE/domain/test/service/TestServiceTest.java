package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import server.MATE.domain.test.dto.response.LikedTestSummaryItem;
import server.MATE.domain.test.dto.response.TestLikeResponse;
import server.MATE.domain.test.dto.response.LikedTestSummaryResponse;
import server.MATE.domain.test.dto.response.MyTestSummaryResponse;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.TestLike;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.storage.FileStorageService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TestServiceTest {

    @Mock
    private TestRepository testRepository;

    @Mock
    private TestLikeRepository testLikeRepository;

    @Mock
    private FileStorageService fileStorageService;

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
        lenient().when(fileStorageService.generateDownloadUrl(anyString())).thenReturn("https://example.com/url");
    }

    @Test
    void 내가_생성한_테스트_목록을_조회한다() {
        server.MATE.domain.test.entity.Test myTest = server.MATE.domain.test.entity.Test.builder()
                .makerId(MAKER_ID)
                .title("내 테스트")
                .description("소개")
                .serviceName("서비스")
                .serviceDescription("서비스 소개")
                .imageKeys(List.of())
                .build();
        ReflectionTestUtils.setField(myTest, "id", TEST_ID);

        given(testRepository.findAllByMakerIdAndDeletedAtIsNullOrderByCreatedAtDesc(MAKER_ID))
                .willReturn(List.of(myTest));

        MyTestSummaryResponse response = testService.listMyTests(MAKER_ID);

        assertThat(response.testCount()).isEqualTo(1);
        assertThat(response.tests()).hasSize(1);
        assertThat(response.tests().getFirst().id()).isEqualTo(TEST_ID);
        assertThat(response.tests().getFirst().title()).isEqualTo("내 테스트");
        assertThat(response.tests().getFirst().testStatus()).isEqualTo(TestStatus.WAITING);
        assertThat(response.tests().getFirst().pplCount()).isZero();
        assertThat(response.tests().getFirst().goalPpl()).isEqualTo(100);
    }

    @Test
    void 내가_찜한_테스트_목록을_조회한다() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.IN_PROGRESS);
        given(testRepository.findLikedTestsByUserId(MAKER_ID, TestStatus.IN_PROGRESS))
                .willReturn(List.of(test));

        LikedTestSummaryResponse response = testService.listLikedTests(MAKER_ID);

        assertThat(response.testCount()).isEqualTo(1);
        assertThat(response.tests()).hasSize(1);

        LikedTestSummaryItem item = response.tests().getFirst();
        assertThat(item.id()).isEqualTo(TEST_ID);
        assertThat(item.title()).isEqualTo("기존 제목");
        assertThat(item.description()).isEqualTo("기존 소개");
        assertThat(item.reward()).isEqualTo(300);
        assertThat(item.thumbnailUrl()).isEqualTo("https://example.com/url");
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
