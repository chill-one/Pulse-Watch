package com.pulsewatch.backend.alert;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.pulsewatch.common.domain.Alert;
import com.pulsewatch.common.domain.AlertType;
import com.pulsewatch.common.domain.Incident;
import com.pulsewatch.common.domain.Monitor;

import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.Body;
import software.amazon.awssdk.services.sesv2.model.Content;
import software.amazon.awssdk.services.sesv2.model.Destination;
import software.amazon.awssdk.services.sesv2.model.EmailContent;
import software.amazon.awssdk.services.sesv2.model.Message;
import software.amazon.awssdk.services.sesv2.model.SendEmailRequest;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

@Component
@Profile("ses-email")
public class SesNotificationSender
        implements NotificationSender {

    private final SesV2Client sesClient;
    private final String fromAddress;
    private final String toAddress;

    public SesNotificationSender(
            SesV2Client sesClient,
            @Value("${pulsewatch.email.from}") String fromAddress,
            @Value("${pulsewatch.email.to}") String toAddress) {

        this.sesClient = sesClient;
        this.fromAddress = fromAddress;
        this.toAddress = toAddress;
    }


        @Override
        public void send(Alert alert) {

                Incident incident = alert.getIncident();
                Monitor monitor = incident.getMonitor();

                String subject = buildSubject(alert, monitor);
                String body = buildBody(alert, monitor, incident);

                Destination destination =
                        Destination.builder()
                                .toAddresses(toAddress)
                                .build();

                Content subjectContent =
                        Content.builder()
                                .data(subject)
                                .build();

                Content bodyContent =
                        Content.builder()
                                .data(body)
                                .build();

                Body emailBody =
                        Body.builder()
                                .text(bodyContent)
                                .build();

                Message message =
                        Message.builder()
                                .subject(subjectContent)
                                .body(emailBody)
                                .build();

                EmailContent emailContent =
                        EmailContent.builder()
                                .simple(message)
                                .build();

                SendEmailRequest request =
                        SendEmailRequest.builder()
                                .fromEmailAddress(fromAddress)
                                .destination(destination)
                                .content(emailContent)
                                .build();

                SendEmailResponse response =
                        sesClient.sendEmail(request);

                System.out.println(
                        "SES email sent:"
                        + " alertId=" + alert.getId()
                        + " messageId=" + response.messageId()
                );
        }


    private String buildSubject(
            Alert alert,
            Monitor monitor) {

        if (alert.getType() == AlertType.OUTAGE) {
            return "[PulseWatch] DOWN: " + monitor.getName();
        }

        return "[PulseWatch] RECOVERED: " + monitor.getName();
    }

    private String buildBody(
        Alert alert,
        Monitor monitor,
        Incident incident) {

    if (alert.getType() == AlertType.OUTAGE) {

        return """
                PulseWatch detected an outage.

                Monitor: %s
                URL: %s
                Status: DOWN
                Outage started: %s
                """.formatted(
                        monitor.getName(),
                        monitor.getUrl(),
                        incident.getStartedAt()
                );
    }

    return """
            PulseWatch detected a recovery.

            Monitor: %s
            URL: %s
            Status: UP
            Outage started: %s
            Recovered at: %s
            """.formatted(
                    monitor.getName(),
                    monitor.getUrl(),
                    incident.getStartedAt(),
                    incident.getEndedAt()
            );
    }
}