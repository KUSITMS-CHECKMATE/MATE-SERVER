package server.MATE.toss.dto.response;

public record TossMessengerSendResponse(
        int msgCount,
        int sentPushCount,
        int sentAlimtalkCount,
        int sentFriendtalkCount,
        int sentInboxCount,
        int sentSmsCount
) {
}
