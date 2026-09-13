package com.jfl.appointment.n8n.dto;

// patientId is null when the token doesn't match any patient for that clinic -
// the n8n flow checks for this rather than relying on an HTTP error code,
// so an invalid/expired QR scan gets a clean, specific reply instead of a
// generic failure.
public record IdentifyPatientResponse(Long patientId, String patientName) {}
