package de.equalIT.jiraExchangeConnector.impl;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class MessageWrapper {
	protected static final Logger logger = LogManager.getLogger("atlassian.plugin");
	protected Message message;

	public MessageWrapper(Message message) {
		super();
		this.message = message;
	}

	public String getBodyText() throws Exception {
		String result = "";
		if (message.isMimeType("text/plain")) {
			result = message.getContent().toString();
			logger.info("Mail is plain");
		} else if (message.isMimeType("multipart/*")) {
			MimeMultipart mimeMultipart = (MimeMultipart) message.getContent();
			result = getTextFromMimeMultipart(mimeMultipart);
			logger.info("Mail is multipart");
		} else {
			result = org.jsoup.Jsoup.parse(message.getContent().toString()).text();
			logger.info("Mail is probably html mail");
		}
		return result;
	}

	private String getTextFromMimeMultipart(MimeMultipart mimeMultipart) throws Exception {
		String result = "";
		int count = mimeMultipart.getCount();
		for (int i = 0; i < count; i++) {
			BodyPart bodyPart = mimeMultipart.getBodyPart(i);
			logger.info("Mail body part: " + bodyPart.getContentType());
			if (bodyPart.isMimeType("text/plain")) {
				result = result + "\n" + bodyPart.getContent();
				break; // without break same text appears twice in my tests
			} else if (bodyPart.isMimeType("text/html")) {
				String html = (String) bodyPart.getContent();
				result = result + "\n" + org.jsoup.Jsoup.parse(html).text();
			} else if (bodyPart.getContent() instanceof MimeMultipart) {
				result = result + getTextFromMimeMultipart((MimeMultipart) bodyPart.getContent());
			}
		}
		return result;
	}
}
