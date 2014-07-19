package org.code_revue.dns.server.engine;

import org.code_revue.dns.message.*;
import org.code_revue.dns.server.DnsPayload;

/**
 * Moronic implementation of the {@link org.code_revue.dns.server.engine.DnsEngine} interface that always returns the
 * same response. I guess this is handy if you want to make one host the entire internet.
 *
 * @author Mike Fanning
 */
public class SingleTubeEngine implements DnsEngine {

    // If the internet is a series of tubes, this engine is a kink.

    /**
     * Answers every question with the same response.
     * @param payload Query
     * @return Response
     */
    @Override
    public DnsPayload processDnsPayload(DnsPayload payload) {
        DnsResponseBuilder builder = new DnsResponseBuilder(payload.getMessageData());
        builder.setAuthoritativeAnswer(true)
                .setRecursionAvailable(true)
                .setResponseCode(DnsResponseCode.NO_ERROR);

        DnsMessageOverlay overlay = new DnsMessageOverlay(payload.getMessageData());
        DnsQuestion[] questions = overlay.getQuestions();
        byte[] answer = new byte[] { 64, 65, 66, 67 };
        for (DnsQuestion question: questions) {
            builder.addAnswer(new DnsRecord(question.getQuestionName(), question.getQuestionType(),
                    question.getQuestionClass(), 120, answer));
        }
        payload.setMessageData(builder.build());
        return payload;
    }

}
