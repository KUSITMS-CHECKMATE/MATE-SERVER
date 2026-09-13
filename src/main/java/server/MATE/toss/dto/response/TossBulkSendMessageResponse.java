package server.MATE.toss.dto.response;

public record TossBulkSendMessageResponse(
        int msgCount,
        int sentPushCount,
        int sentAlimtalkCount,
        int sentFriendtalkCount,
        int sentInboxCount,
        int sentSmsCount
) {
}
