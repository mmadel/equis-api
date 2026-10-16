package com.equestrian.equis.service.mail;

public record OutgoingEmail(String to, String subject, String text, String html) {
    // The bodies hold the link, so they are never printed.
    @Override
    public String toString() {
        return "OutgoingEmail[to=" + to + ", subject=" + subject + "]";
    }
}
