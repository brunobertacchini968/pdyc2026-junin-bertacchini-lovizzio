package ar.edu.unnoba.pdyc2026.events.service;

import ar.edu.unnoba.pdyc2026.events.model.Event;

public interface NotificationService {

    void notifyEventChange(Event event, String message);
}
