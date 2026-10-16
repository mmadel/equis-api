package com.equestrian.equis.support;

import com.equestrian.equis.service.mail.NotificationSender;
import com.equestrian.equis.service.mail.OutgoingEmail;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Keeps every email instead of sending it, so a test can read the link out of it.
public class RecordingNotificationSender implements NotificationSender {
    private static final Pattern TOKEN = Pattern.compile("/invite/([A-Za-z0-9_-]+)");

    private final List<OutgoingEmail> sent = new ArrayList<>();
    private RuntimeException failure;

    @Override
    public synchronized void send(OutgoingEmail email) {
        if (failure != null) {
            throw failure;
        }
        sent.add(email);
    }

    public synchronized List<OutgoingEmail> all() {
        return List.copyOf(sent);
    }

    public synchronized OutgoingEmail last() {
        if (sent.isEmpty()) {
            throw new AssertionError("No email was sent");
        }
        return sent.getLast();
    }

    // Also stops failing.
    public synchronized void clear() {
        sent.clear();
        failure = null;
    }

    // Every send throws this until clear().
    public synchronized void failWith(RuntimeException failure) {
        this.failure = failure;
    }

    public static String tokenFrom(OutgoingEmail email) {
        Matcher m = TOKEN.matcher(email.text());
        if (!m.find()) {
            throw new AssertionError("No /invite/ link in " + email);
        }
        return m.group(1);
    }
}
