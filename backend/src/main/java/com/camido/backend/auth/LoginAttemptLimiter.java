package com.camido.backend.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Límite en memoria por IP + email. Vale para una sola instancia del MVP.
 */
@Component
public class LoginAttemptLimiter {

	static final int MAX_FALLOS = 5;
	static final Duration VENTANA = Duration.ofMinutes(15);

	private final ConcurrentHashMap<String, Deque<Instant>> intentos = new ConcurrentHashMap<>();

	public boolean bloqueado(String clave) {
		return fallosRecientes(clave) >= MAX_FALLOS;
	}

	public void registrarFallo(String clave) {
		Deque<Instant> cola = intentos.computeIfAbsent(clave, ignored -> new ArrayDeque<>());
		synchronized (cola) {
			podar(cola, Instant.now());
			cola.addLast(Instant.now());
		}
	}

	public void reiniciar(String clave) {
		intentos.remove(clave);
	}

	private int fallosRecientes(String clave) {
		Deque<Instant> cola = intentos.get(clave);
		if (cola == null) {
			return 0;
		}
		synchronized (cola) {
			podar(cola, Instant.now());
			return cola.size();
		}
	}

	private static void podar(Deque<Instant> cola, Instant ahora) {
		Instant limite = ahora.minus(VENTANA);
		while (!cola.isEmpty() && cola.peekFirst().isBefore(limite)) {
			cola.removeFirst();
		}
	}
}
