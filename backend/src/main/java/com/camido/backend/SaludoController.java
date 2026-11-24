
package com.camido.backend;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SaludoController {

    @GetMapping("/api/saludo")
    public String saludar() {
        return "Bienvenido al backend de Cami-Do";
    }
}
