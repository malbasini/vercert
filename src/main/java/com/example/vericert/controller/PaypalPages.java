package com.example.vericert.controller;

import com.example.vericert.domain.Payment;
import com.example.vericert.domain.User;
import com.example.vericert.dto.CurrentPlanView;
import com.example.vericert.repo.*;
import com.example.vericert.service.InvoiceService;
import com.example.vericert.service.MailService;
import com.example.vericert.service.PlanEnforcementService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Controller
@RequestMapping("/paypal")
public class PaypalPages {

    private final PaymentRepository payRepo;
    private final PlanEnforcementService planEnforcementService;
    private final TenantSettingsRepository tenantSettingsRepo;
    private final MailService mailService;
    private final UserRepository userRepo;
    private final MembershipRepository membershipRepo;
    private final InvoiceService invoiceService;
    private final TemplateRepository tempRepo;

    public PaypalPages(PaymentRepository payRepo,
                         PlanEnforcementService planEnforcementService,
                         TenantSettingsRepository tenantSettingsRepo,
                         MailService mailService,
                         UserRepository userRepo,
                         MembershipRepository membershipRepo,
                         InvoiceService invoiceService,
                         TemplateRepository tempRepo) {



        this.payRepo = payRepo;
        this.planEnforcementService = planEnforcementService;
        this.tenantSettingsRepo = tenantSettingsRepo;
        this.mailService = mailService;
        this.userRepo = userRepo;
        this.membershipRepo = membershipRepo;
        this.invoiceService = invoiceService;
        this.tempRepo = tempRepo;

    }


    @GetMapping("/success")
    public String order(@RequestParam("token") String orderId,
                          @RequestParam(value="PayerID", required=false) String payerId,
                          Model model) {
        model.addAttribute("orderId", orderId);
        model.addAttribute("payerId", payerId);
        return "paypal/order";
    }
    @GetMapping("/cancel")
    public String cancelPaypal() { return "paypal/cancel"; }

    @GetMapping("success/{orderId}")
    public String success(@PathVariable String orderId,Model model) {
        Payment p = payRepo.findByProviderIntentId(orderId).orElseThrow();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        User u = userRepo.findByUserName(username).orElseThrow();
        Long tenantId = membershipRepo.findByUser(u).get().getTenantId();
        // sicurezza: utente deve appartenere al tenant del pagamento
        if (!tenantId.equals(p.getTenantId())) {
            return "error/403";
        }
        Instant date = tenantSettingsRepo.findByTenantId(p.getTenantId()).get().getCurrentPeriodEnd();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                .withZone(ZoneId.systemDefault()); // o ZoneId.of("Europe/Rome")
        String formatted = fmt.format(date);
        CurrentPlanView current = planEnforcementService.buildCurrentPlanView(p.getTenantId());
        model.addAttribute("currentPlan", current);
        model.addAttribute("amountFormatted",formatCents(p.getAmountMinor()));
        model.addAttribute("provider", "PayPal"); // o "PayPal"
        model.addAttribute("nextRenewalDate", formatted);
        model.addAttribute("transactionId", orderId);// o l’id PayPal/Stripe reale
        model.addAttribute("title", "Pagamento riuscito");
        model.addAttribute("description", "Pagamento riuscito.");
        return "paypal/success";
    }

    public static String formatEuroIT(String importoStr) {
        BigDecimal value = new BigDecimal(importoStr.replace(',', '.')); // gestisce input "12.08" o "12,08"
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);
        return fmt.format(value); // "€ 12,08"
    }

    public static String formatEuroIT(BigDecimal value) {
        NumberFormat nf = NumberFormat.getCurrencyInstance(Locale.ITALY);
        nf.setCurrency(Currency.getInstance("EUR"));
        return nf.format(value);
    }
    public static String formatCents(long cents) {
        BigDecimal eur = BigDecimal.valueOf(cents).movePointLeft(2); // divide per 100 senza perdita
        NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.ITALY);
        return fmt.format(eur); // es. 2990 -> "€ 29,90"
    }
}