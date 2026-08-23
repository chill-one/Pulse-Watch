package com.pulsewatch.backend.alert;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

@ExtendWith(MockitoExtension.class)
class SesNotificationSenderTest {

    @Mock
    private SesV2Client sesClient;

    @Test
    void buildsSesRequestFromNotificationMessage() {
        when(sesClient.sendEmail(any(SendEmailRequest.class)))
                .thenReturn(SendEmailResponse.builder().messageId("message-123").build());
        SesNotificationSender sender = new SesNotificationSender(
                sesClient,
                "from@example.test",
                "to@example.test"
        );
        NotificationMessage message = new NotificationMessage(
                UUID.randomUUID(),
                "[PulseWatch] DOWN : API",
                "The API is down."
        );

        sender.send(message);

        ArgumentCaptor<SendEmailRequest> request = ArgumentCaptor.forClass(SendEmailRequest.class);
        verify(sesClient).sendEmail(request.capture());
        assertThat(request.getValue().fromEmailAddress()).isEqualTo("from@example.test");
        assertThat(request.getValue().destination().toAddresses())
                .containsExactly("to@example.test");
        assertThat(request.getValue().content().simple().subject().data())
                .isEqualTo(message.subject());
        assertThat(request.getValue().content().simple().body().text().data())
                .isEqualTo(message.body());
    }

    @Test
    void propagatesSesFailure() {
        RuntimeException failure = new IllegalStateException("SES unavailable");
        when(sesClient.sendEmail(any(SendEmailRequest.class))).thenThrow(failure);
        SesNotificationSender sender = new SesNotificationSender(
                sesClient,
                "from@example.test",
                "to@example.test"
        );

        assertThatThrownBy(() -> sender.send(new NotificationMessage(UUID.randomUUID(), "subject", "body")))
                .isSameAs(failure);
    }
}
