package com.qiaben.ciyex.service;

import com.qiaben.ciyex.dto.StripeBillingCardDto;
import com.stripe.exception.InvalidRequestException;
import com.stripe.model.Customer;
import com.stripe.model.PaymentMethod;
import com.stripe.net.RequestOptions;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "stripe.backfill.enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class StripeCustomerBackfill implements CommandLineRunner {

    private final StripeBillingCardService cardService;
    private final OrgConfigService orgConfigService;

    @Override
    public void run(String... args) throws Exception {
        // 🔹 Replace with your orgId for testing or loop over all orgs
        Long orgId = 1L;

        String stripeKey = orgConfigService.getStripeSecretKey(orgId);
        RequestOptions opts = RequestOptions.builder().setApiKey(stripeKey).build();

        List<StripeBillingCardDto> cards = cardService.findAllWithoutCustomer(orgId);

        for (StripeBillingCardDto card : cards) {
            // 1. Create Customer
            Map<String, Object> custParams = new HashMap<>();
            custParams.put("description", "Org " + orgId + " user " + card.getUserId());
            custParams.put("metadata", Map.of("orgId", String.valueOf(orgId)));
            Customer customer = null;
            try {
                customer = Customer.create(custParams, opts);

                // 2. Attach existing PaymentMethod
                PaymentMethod pm = PaymentMethod.retrieve(card.getStripePaymentMethodId(), opts);
                pm.attach(Map.of("customer", customer.getId()), opts);

                // 3. Save in DB
                cardService.updateCustomerId(card.getId(), orgId, customer.getId());

                System.out.println("✅ Backfilled card " + card.getId() + " with customer " + customer.getId());
            } catch (InvalidRequestException e) {
                // This happens when the payment method can't be attached (e.g., previously used/detached)
                String pmId = card.getStripePaymentMethodId();
                System.err.println("⚠️ Skipping card " + card.getId() + " - failed to attach PaymentMethod " + pmId + ": " + e.getMessage());

                // If we created a customer but attach failed, delete the customer to avoid orphaned objects
                if (customer != null) {
                    try {
                        customer.delete(opts);
                        System.err.println("ℹ️ Deleted temporary customer " + customer.getId() + " after failed attach");
                    } catch (Exception ex) {
                        System.err.println("‼️ Failed to delete temporary customer " + (customer != null ? customer.getId() : "null") + ": " + ex.getMessage());
                    }
                }

                // continue to next card without failing the whole application
            }
        }
    }
}
