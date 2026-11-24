package com.camido.backend.email;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "camido.email")
public class EmailProperties {

	/**
	 * {@code none} (solo desarrollo) o {@code resend}.
	 */
	private String provider = "none";

	private final Resend resend = new Resend();

	public String getProvider() {
		return provider;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public Resend getResend() {
		return resend;
	}

	public static class Resend {
		private String apiKey = "";
		private String from = "";
		private String apiUrl = "https://api.resend.com/emails";

		public String getApiKey() {
			return apiKey;
		}

		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}

		public String getFrom() {
			return from;
		}

		public void setFrom(String from) {
			this.from = from;
		}

		public String getApiUrl() {
			return apiUrl;
		}

		public void setApiUrl(String apiUrl) {
			this.apiUrl = apiUrl;
		}
	}
}
