package com.jfl.appointment.exception;

// Thrown when the second (authoritative) availability check fails right
// before booking - see design doc section 15: "availability must be
// checked twice". n8n/AI should catch this and re-offer fresh slots.
public class SlotUnavailableException extends RuntimeException {
    public SlotUnavailableException(String message) { super(message); }
}
