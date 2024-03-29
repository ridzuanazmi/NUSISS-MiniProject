package nusiss.project.server.controllers;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;

import nusiss.project.server.models.MaintenanceCheckout;
import nusiss.project.server.services.EmailService;

@RestController
@RequestMapping(path = "/payment")
public class StripeController {

  private final EmailService emailSrvc;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private static final Logger logger = LoggerFactory.getLogger(StripeController.class);
  @Value("${stripe.api.key}")
  private String stripeApiKey;

  public StripeController(EmailService emailSrvc) {
    this.emailSrvc = emailSrvc;
  }

  @PostMapping("/maintenance")
  public ResponseEntity<String> maintenanceCheckoutPage(@RequestBody MaintenanceCheckout checkout) {

    // Log checkout object details
    logger.info(
        "Maintenance Checkout - Plot Number: {}, Block Number: {}, PriceId: {}, SuccessUrl: {}, CancelUrl: {}, fullName: {}, email: {}",
        checkout.getPlotNumber(), checkout.getBlockNumber(), checkout.getPriceId(), checkout.getSuccessUrl(),
        checkout.getCancelUrl(), checkout.getFullName(), checkout.getEmail());

    // We initialize stripe object with the api key
    Stripe.apiKey = stripeApiKey;

    // Adding metadata
    Map<String, String> metadata = new HashMap<>();
    metadata.put("plotNumber", checkout.getPlotNumber());
    metadata.put("blockNumber", checkout.getBlockNumber());

    SessionCreateParams params = new SessionCreateParams.Builder().setSuccessUrl(checkout.getSuccessUrl())
        .setCancelUrl(checkout.getCancelUrl()).addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION).addLineItem(new SessionCreateParams.LineItem.Builder()
            .setQuantity(1L).setPrice(checkout.getPriceId()).build())
        .putAllMetadata(metadata) // Setting metadata
        .build();

    try {
      // TODO: Correct the implementation using webhook
      // Create email template and send
      emailSrvc.sendMaintenanceEmailToTeam(checkout);
      emailSrvc.sendMaintenanceEmailToUser(checkout);

      Session session = Session.create(params);
      Map<String, Object> responseData = new HashMap<>();
      responseData.put("sessionId", session.getId());

      return ResponseEntity.ok(objectMapper.writeValueAsString(responseData));
    } catch (StripeException stripeException) {
      logger.error("Stripe error: ", stripeException);
      return createErrorResponse(stripeException.getMessage());
    } catch (JsonProcessingException jsonException) {
      logger.error("JSON Processing error: ", jsonException);
      return createErrorResponse(jsonException.getMessage());
    } catch (Exception e) {
      logger.error("Unexpected error: ", e);
      return createErrorResponse("An unexpected error occurred");
    }
  }

  private ResponseEntity<String> createErrorResponse(String errorMessage) {
    Map<String, Object> messageData = new HashMap<>();
    messageData.put("id", errorMessage);
    Map<String, Object> responseData = new HashMap<>();
    responseData.put("error", messageData);

    try {
      return ResponseEntity.badRequest().body(objectMapper.writeValueAsString(responseData));
    } catch (JsonProcessingException jsonException) {
      logger.error("Error while creating error response: ", jsonException);
      return ResponseEntity.badRequest().body("{\"message\":\"An error occurred\"}");
    }
  }

}
