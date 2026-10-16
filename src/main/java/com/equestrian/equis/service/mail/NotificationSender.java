package com.equestrian.equis.service.mail;

// Delivers one email. Throws if it could not be sent.
public interface NotificationSender {
    void send(OutgoingEmail email);
}
