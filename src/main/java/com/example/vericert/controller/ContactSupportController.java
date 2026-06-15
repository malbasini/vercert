package com.example.vericert.controller;


import com.example.vericert.dto.ContactFormDto;
import com.example.vericert.service.ContactEmailService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/contact")
public class ContactSupportController {

    private final ContactEmailService contactService;

    public ContactSupportController(ContactEmailService contactService) {
        this.contactService = contactService;
    }

    @GetMapping
    public String contactPage(Model model) {
        if (!model.containsAttribute("contactForm")) {
            model.addAttribute("contactForm", new ContactFormDto());
        }
        return "public/contact";
    }

    @PostMapping
    public String sendContact(@Valid @ModelAttribute("contactForm") ContactFormDto form,
                              BindingResult bindingResult,
                              Model model) {
        if (bindingResult.hasErrors()) {
            return "public/contact";
        }
        try {
            contactService.send(form);
            model.addAttribute("successMessage", "Messaggio inviato correttamente.");
            model.addAttribute("contactForm", new ContactFormDto());
            return "public/contact";
        }
        catch (Exception ex) {
            model.addAttribute("errorMessage", "Errore durante l'invio del messaggio. " + ex.getMessage());
            return "public/contact";
        }
    }
}
