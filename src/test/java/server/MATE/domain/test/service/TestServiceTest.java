package server.MATE.domain.test.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import server.MATE.domain.participation.repository.ParticipationRepository;
import server.MATE.global.common.exception.BaseErrorCode;
import server.MATE.global.common.exception.BaseException;
import server.MATE.domain.test.dto.request.CompletedTestCursor;
import server.MATE.domain.test.dto.response.CompletedTestListResponse;
import server.MATE.domain.test.dto.response.LikedTestSummaryItem;
import server.MATE.domain.test.dto.response.LikedTestSummaryResponse;
import server.MATE.domain.test.dto.response.MyTestSummaryResponse;
import server.MATE.domain.test.dto.response.TestDetailResponse;
import server.MATE.domain.test.dto.response.TestLikeResponse;
import server.MATE.domain.test.dto.response.TestSummaryListResponse;
import server.MATE.domain.test.entity.Category;
import server.MATE.domain.test.entity.TestLike;
import server.MATE.domain.test.entity.TestStatus;
import server.MATE.domain.test.repository.TestLikeRepository;
import server.MATE.domain.test.repository.TestRepository;
import server.MATE.global.storage.service.FileStorageService;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private ParticipationRepository participationRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private TestCloseScheduler testCloseScheduler;

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
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(test, "id", TEST_ID);
        test.addCategories(List.of(Category.FOOD));
        lenient().when(fileStorageService.generateDownloadUrl(anyString())).thenReturn("https://example.com/url");
        lenient().when(clock.withZone(ZoneId.of("Asia/Seoul"))).thenReturn(Clock.system(ZoneId.of("Asia/Seoul")));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-05-30T00:00:00Z"));
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }

    @Test
    void 진행_중이거나_검수_중이고_마감일_이내인_테스트_목록을_페이지_단위로_조회한다() {
        server.MATE.domain.test.entity.Test inProgressTest = createListTest(10L, "진행 중 테스트", TestStatus.IN_PROGRESS);
        server.MATE.domain.test.entity.Test waitingTest = createListTest(11L, "검수 중 테스트", TestStatus.WAITING);

        given(testRepository.findAvailableTestsForUser(
                eq(List.of(TestStatus.IN_PROGRESS, TestStatus.WAITING)),
                any(),
                eq(MAKER_ID),
                eq(0),
                eq(3)
        )).willReturn(List.of(inProgressTest, waitingTest));
        given(testRepository.countAvailableTestsForUser(
                eq(List.of(TestStatus.IN_PROGRESS, TestStatus.WAITING)),
                any(),
                eq(MAKER_ID)
        )).willReturn(2L);
        given(testLikeRepository.findLikedTestIds(MAKER_ID, List.of(10L, 11L)))
                .willReturn(List.of());

        TestSummaryListResponse response = testService.listTests(MAKER_ID, 1, 3);

        assertThat(response.testCount()).isEqualTo(2L);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(3);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.tests()).extracting("title")
                .containsExactly("진행 중 테스트", "검수 중 테스트");
    }

    @Test
    void 전체_개수가_현재_페이지까지의_개수보다_많으면_hasNext가_true다() {
        List<server.MATE.domain.test.entity.Test> tests = List.of(
                createListTest(10L, "테스트4", TestStatus.IN_PROGRESS),
                createListTest(11L, "테스트5", TestStatus.IN_PROGRESS),
                createListTest(12L, "테스트6", TestStatus.IN_PROGRESS)
        );
        given(testRepository.findAvailableTestsForUser(any(), any(), eq(MAKER_ID), eq(3), eq(3)))
                .willReturn(tests);
        given(testRepository.countAvailableTestsForUser(any(), any(), eq(MAKER_ID))).willReturn(7L);
        given(testLikeRepository.findLikedTestIds(MAKER_ID, List.of(10L, 11L, 12L)))
                .willReturn(List.of());

        TestSummaryListResponse response = testService.listTests(MAKER_ID, 2, 3);

        assertThat(response.testCount()).isEqualTo(7L);
        assertThat(response.hasNext()).isTrue();
    }

    @Test
    void 페이지와_크기가_범위를_벗어나면_보정해서_조회한다() {
        given(testRepository.findAvailableTestsForUser(any(), any(), eq(MAKER_ID), eq(0), eq(100)))
                .willReturn(List.of());
        given(testRepository.countAvailableTestsForUser(any(), any(), eq(MAKER_ID))).willReturn(0L);

        TestSummaryListResponse response = testService.listTests(MAKER_ID, 0, 1000);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(100);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.tests()).isEmpty();
    }

    @Test
    void 종료_테스트_첫_페이지_조회_시_size보다_많으면_잘라서_반환하고_다음_커서를_준다() {
        server.MATE.domain.test.entity.Test newest = createCompletedListTest(10L, "최근 종료", LocalDateTime.of(2026, 10, 3, 12, 0));
        server.MATE.domain.test.entity.Test middle = createCompletedListTest(11L, "중간 종료", LocalDateTime.of(2026, 10, 2, 12, 0));
        server.MATE.domain.test.entity.Test oldest = createCompletedListTest(12L, "오래전 종료", LocalDateTime.of(2026, 10, 1, 12, 0));
        given(testRepository.findCompletedTests(null, null, 3)).willReturn(List.of(newest, middle, oldest));
        given(testLikeRepository.findLikedTestIds(MAKER_ID, List.of(10L, 11L))).willReturn(List.of());

        CompletedTestListResponse response = testService.listCompletedTests(MAKER_ID, null, 2);

        assertThat(response.hasNext()).isTrue();
        assertThat(response.tests()).extracting("title").containsExactly("최근 종료", "중간 종료");
        CompletedTestCursor nextCursor = CompletedTestCursor.decode(response.nextCursor());
        assertThat(nextCursor.completedAt()).isEqualTo(LocalDateTime.of(2026, 10, 2, 12, 0));
        assertThat(nextCursor.id()).isEqualTo(11L);
    }

    @Test
    void 종료_테스트_커서로_조회_시_마지막_페이지면_hasNext가_false이고_다음_커서가_없다() {
        LocalDateTime cursorCompletedAt = LocalDateTime.of(2026, 10, 2, 12, 0);
        String cursor = new CompletedTestCursor(cursorCompletedAt, 11L).encode();
        server.MATE.domain.test.entity.Test oldest = createCompletedListTest(12L, "오래전 종료", LocalDateTime.of(2026, 10, 1, 12, 0));
        given(testRepository.findCompletedTests(cursorCompletedAt, 11L, 3)).willReturn(List.of(oldest));
        given(testLikeRepository.findLikedTestIds(MAKER_ID, List.of(12L))).willReturn(List.of());

        CompletedTestListResponse response = testService.listCompletedTests(MAKER_ID, cursor, 2);

        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isNull();
        assertThat(response.tests()).extracting("title").containsExactly("오래전 종료");
    }

    @Test
    void 종료_테스트_조회_시_커서_형식이_잘못되면_COMMON_002_예외가_발생한다() {
        assertThatThrownBy(() -> testService.listCompletedTests(MAKER_ID, "invalid-cursor", 4))
                .isInstanceOf(BaseException.class)
                .extracting(e -> ((BaseException) e).getErrorCode())
                .isEqualTo(BaseErrorCode.COMMON_002);
        verify(testRepository, never()).findCompletedTests(any(), any(), anyInt());
    }

    @Test
    void 테스트_상세_조회_시_상태와_응답_여부를_반환한다() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.IN_PROGRESS);
        given(testRepository.findWithCategoriesById(TEST_ID)).willReturn(Optional.of(test));
        given(participationRepository.existsActiveParticipation(TEST_ID, MAKER_ID))
                .willReturn(true);

        TestDetailResponse response = testService.getTest(TEST_ID, MAKER_ID);

        assertThat(response.id()).isEqualTo(TEST_ID);
        assertThat(response.testStatus()).isEqualTo(TestStatus.IN_PROGRESS);
        assertThat(response.hasResponded()).isTrue();
    }

    @Test
    void 종료된_테스트_상세_조회_시_상태를_반환한다() {
        ReflectionTestUtils.setField(test, "testStatus", TestStatus.COMPLETED);
        given(testRepository.findWithCategoriesById(TEST_ID)).willReturn(Optional.of(test));
        given(participationRepository.existsActiveParticipation(TEST_ID, MAKER_ID))
                .willReturn(false);

        TestDetailResponse response = testService.getTest(TEST_ID, MAKER_ID);

        assertThat(response.testStatus()).isEqualTo(TestStatus.COMPLETED);
        assertThat(response.hasResponded()).isFalse();
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
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(myTest, "id", TEST_ID);

        given(testRepository.findByMakerId(MAKER_ID))
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
        given(testRepository.findLikedTests(
                MAKER_ID,
                List.of(TestStatus.IN_PROGRESS, TestStatus.WAITING, TestStatus.COMPLETED)
        )).willReturn(List.of(test));

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
        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
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

        given(testRepository.findByIdForUpdate(TEST_ID)).willReturn(Optional.of(test));
        given(testLikeRepository.findByUserIdAndTestId(MAKER_ID, TEST_ID)).willReturn(Optional.of(testLike));

        TestLikeResponse response = testService.unlikeTest(TEST_ID, MAKER_ID);

        assertThat(response.testId()).isEqualTo(TEST_ID);
        assertThat(response.isLiked()).isFalse();
        assertThat(response.likeCount()).isZero();
        verify(testLikeRepository).delete(testLike);
    }

    @Test
    void REJECTED_테스트는_상세_조회되지_않는다() {
        given(testRepository.findWithCategoriesById(TEST_ID)).willReturn(Optional.empty());

        BaseException exception = org.junit.jupiter.api.Assertions.assertThrows(BaseException.class,
                () -> testService.getTest(TEST_ID, MAKER_ID));

        assertThat(exception.getErrorCode()).isEqualTo(BaseErrorCode.TEST_004);
    }

    private server.MATE.domain.test.entity.Test createCompletedListTest(Long id, String title, LocalDateTime completedAt) {
        server.MATE.domain.test.entity.Test listTest = createListTest(id, title, TestStatus.IN_PROGRESS);
        listTest.complete(completedAt);
        return listTest;
    }

    private server.MATE.domain.test.entity.Test createListTest(Long id, String title, TestStatus testStatus) {
        server.MATE.domain.test.entity.Test listTest = server.MATE.domain.test.entity.Test.builder()
                .makerId(MAKER_ID)
                .title(title)
                .description("소개")
                .serviceName("서비스")
                .serviceDescription("서비스 소개")
                .imageKeys(List.of())
                .testStatus(testStatus)
                .closedAt(LocalDateTime.of(2099, 12, 31, 23, 59, 59))
                .build();
        ReflectionTestUtils.setField(listTest, "id", id);
        return listTest;
    }

}
