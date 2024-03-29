package nusiss.project.server.controllers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import nusiss.project.server.models.Contact;
import nusiss.project.server.models.ContactDto;
import nusiss.project.server.services.ContactService;
import nusiss.project.server.services.EmailService;

@RestController
@RequestMapping(path = "/contact-us")
public class ContactUsController {

    private final ContactService contactSrvc;
    private final EmailService emailSrvc;
    private static final Logger LOGGER = LoggerFactory.getLogger(ContactUsController.class);

    public ContactUsController(
            ContactService contactSrvc,
            EmailService emailSrvc) {
        this.contactSrvc = contactSrvc;
        this.emailSrvc = emailSrvc;
    }

    @PostMapping
    public ResponseEntity<String> saveContactInquiryDetails(@RequestBody ContactDto contactDto) {
        LOGGER.info("POST /contact - Start - ContactDto: {}", contactDto);

        try {
            // Create new Contact object from the ContactDto
            Contact contact = contactSrvc.createContact(contactDto);
            LOGGER.info("POST /contact - try block - Contact: {}", contact);

            // Create and send email to the DD team
            emailSrvc.sendContactEmail(contact);
            LOGGER.info("POST /contact - Email sent to DD team");
            contactSrvc.saveContact(contact);

            Map<String, String> responseBody = new HashMap<>();
            responseBody.put("contactId", contact.getContactId());

            String jsonResponseBody = new ObjectMapper().writeValueAsString(responseBody);
            LOGGER.info("POST /contact - Contact saved successfully - ContactID: {}", contact.getContactId());
            return ResponseEntity.status(HttpStatus.CREATED).body(jsonResponseBody);

        } catch (Exception e) {

            System.out.println("Error occurred while saving the contact: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"message\":\"Error occurred while saving the contact. Please try again later.\"}");

        } finally {
            LOGGER.info("POST /contact - End");
        }
    }
}
