package com.jfl.appointment.n8n.controller;


import com.jfl.appointment.n8n.dto.ClinicWhatsappConfigDto;
import com.jfl.appointment.n8n.dto.IdentifyByTokenRequest;
import com.jfl.appointment.n8n.dto.IdentifyClinicResponse;
import com.jfl.appointment.n8n.dto.IdentifyPatientResponse;
import com.jfl.appointment.n8n.service.AppointmentService;
import com.jfl.appointment.n8n.service.AvailabilityService;
import com.jfl.appointment.n8n.service.ConversationSessionService;
import com.jfl.appointment.n8n.service.N8nSupportService;
import com.jfl.appointment.repository.DoctorRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Everything the QR/Twilio n8n flow calls, all under /api/n8n so it's
 * cleanly separated from any other integration hitting your API. Two
 * genuinely new pieces (clinic-by-whatsapp-number, identify-by-token) live
 * in N8nSupportService; everything else here is a thin delegate to the same
 * services your dashboard and WhatsApp-conversation flow already use - no
 * business logic is duplicated, just re-exposed under this path prefix.
 */
@RestController
@RequestMapping("/api/n8n")
@RequiredArgsConstructor
public class N8nApiController {

    private final N8nSupportService n8nSupportService;
    private final ConversationSessionService sessionService;
    private final DoctorRepository doctorRepository;
    private final AvailabilityService availabilityService;
    private final AppointmentService appointmentService;

    // --- New: clinic resolution by the WhatsApp number a message arrived on ---
    @GetMapping("/clinics/by-whatsapp-number")
    public ClinicWhatsappConfigDto getClinicByWhatsappNumber(@RequestParam String number) {
        return n8nSupportService.resolveClinicByWhatsappNumber(number);
    }

    // --- New: token-based patient identification from a scanned QR code ---
    @PostMapping("/patients/identify-by-token")
    public IdentifyPatientResponse identifyByToken(@Valid @RequestBody IdentifyByTokenRequest request) {
        return n8nSupportService.identifyByToken(request.clinicId(), request.token());
    }

    @PostMapping("/clinics/identify-by-token")
    public IdentifyClinicResponse clinicIdentifyByToken(@Valid @RequestBody IdentifyByTokenRequest request) {
        return n8nSupportService.clinicIdentifyByToken(request.clinicId(), request.token());
    }

    // --- Delegates: session lookup/update (same logic as SessionController) ---
//    @GetMapping("/clinics/{clinicId}/sessions/active")
//    public SessionResponse getActiveSession(
//            @PathVariable Long clinicId, @RequestParam String whatsappNumber) {
//        return sessionService.findOrCreateActiveSession(clinicId, whatsappNumber);
//    }

//    @PatchMapping("/sessions/{sessionId}")
//    public SessionResponse updateSession(
//            @PathVariable Long sessionId, @RequestBody UpdateSessionRequest request) {
//        return sessionService.updateSession(sessionId, request);
//    }

    // --- Delegate: doctor list, optionally filtered by service (same as DoctorController) ---
//    @GetMapping("/clinics/{clinicId}/doctors")
//    public List<DoctorDto> getDoctors(
//            @PathVariable Long clinicId, @RequestParam(required = false) Long serviceId) {
//        List<Doctor> doctors = serviceId != null
//                ? doctorRepository.findByClinicIdAndActiveTrueAndServices_Id(clinicId, serviceId)
//                : doctorRepository.findByClinicIdAndActiveTrue(clinicId);
//        return doctors.stream()
//                .map(d -> new DoctorDto(d.getId(), d.getName(), d.getSpecialization()))
//                .toList();
//    }

    // --- Delegate: availability (same as AvailabilityController) ---
//    @GetMapping("/clinics/{clinicId}/availability")
//    public AvailabilityResponse getAvailability(
//            @PathVariable Long clinicId,
//            @RequestParam Long doctorId,
//            @RequestParam Long serviceId,
//            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
//        return availabilityService.getAvailability(clinicId, doctorId, serviceId, date);
//    }

    // --- Delegate: appointment creation (same as AppointmentController, same
    // locking + idempotency guarantees, same notification scheduling) ---
//    @PostMapping("/appointments")
//    public AppointmentResponse createAppointment(@Valid @RequestBody CreateAppointmentRequest request) {
//        return appointmentService.createAppointment(request);
//    }
}
