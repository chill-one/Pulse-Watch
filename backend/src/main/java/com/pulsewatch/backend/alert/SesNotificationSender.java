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


    /** 
     * @param alert
     */
    @Override
    public void send(Alert alert) {

        Incident incident = alert.getIncident();
        Monitor monitor = incident.getMonitor();

        String subject = buildSubject(alert, monitor);
        String body = buildBody(alert, monitor, incident);

        SendEmailRequest request =
                SendEmailRequest.builder()

                        .fromEmailAddress(fromAddress)

                        .destination(
                                Destination.builder()
                                        .toAddresses(toAddress)
                                        .build()
                        )

                        .content(
                                EmailContent.builder()
                                        .simple(
                                                Message.builder()

                                                        .subject(
                                                                Content.builder()
                                                                        .data(subject)
                                                                        .build()
                                                        )

                                                        .body(
                                                                Body.builder()
                                                                        .text(
                                                                                Content.builder()
                                                                                        .data(body)
                                                                                        .build()
                                                                        )
                                                                        .build()
                                                        )

                                                        .build()
                                        )
                                        .build()
                        )

                        .build();

        sesClient.sendEmail(request);
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