package com.pulsewatch.backend.alert;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

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
    public void send(NotificationMessage message) {

        Destination destination =
                Destination.builder()
                        .toAddresses(toAddress)
                        .build();

        Content subjectContent =
                Content.builder()
                        .data(message.subject())
                        .build();

        Content bodyContent =
                Content.builder()
                        .data(message.body())
                        .build();

        Message emailMessage =
                Message.builder()
                        .subject(subjectContent)
                        .body(
                                Body.builder()
                                        .text(bodyContent)
                                        .build()
                        )
                        .build();

        SendEmailRequest request =
                SendEmailRequest.builder()
                        .fromEmailAddress(fromAddress)
                        .destination(destination)
                        .content(
                                EmailContent.builder()
                                        .simple(emailMessage)
                                        .build()
                        )
                        .build();

        SendEmailResponse response =
                sesClient.sendEmail(request);

        System.out.println(
                "SES email sent:"
                + " alertId=" + message.alertId()
                + " messageId=" + response.messageId()
        );
    }
}
