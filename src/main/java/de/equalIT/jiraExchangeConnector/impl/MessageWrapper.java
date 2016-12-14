package de.equalIT.jiraExchangeConnector.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.MimeMultipart;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import com.google.common.base.Strings;
import com.google.common.collect.Maps;

/**
 * This is a helper class which aids in extracting body text and attachments from mails.
 * 
 * @author Volker Gronau
 *
 */
public class MessageWrapper {
	protected static final Logger logger = LogManager.getLogger("atlassian.plugin.jiraExchangeConnectorPlugin.MessageWrapper");
	protected Message message;

	public MessageWrapper(Message message) {
		super();
		this.message = message;
	}

	/**
	 * Returns the body text of a mail.
	 * 
	 */
	public String getBodyText() throws Exception {
		try {
			String result;
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
		} catch (Exception e) {
			throw new Exception("Could not get mailbody because: ", e);
		}
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

	/**
	 * Returns a map of all attachments. All files are written to a temporary directory and need to be deleted after
	 * use.
	 * 
	 * @throws MessagingException
	 * @throws IOException
	 */
	public Map<File, String> getAttachments() throws MessagingException, IOException {
		Map<File, String> result = Maps.newHashMap();

		if (message.isMimeType("multipart/*")) {
			Multipart multipart = (Multipart) message.getContent();

			for (int i = 0; i < multipart.getCount(); i++) {
				BodyPart bodyPart = multipart.getBodyPart(i);
				if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition()) && !Strings.isNullOrEmpty(bodyPart.getFileName())) {
					InputStream is = bodyPart.getInputStream();
					try {
						File tmpFile = File.createTempFile("jiraattachment", "jiraattachment");
						FileOutputStream fos = new FileOutputStream(tmpFile);
						try {
							byte[] buf = new byte[4096];
							int bytesRead;
							while ((bytesRead = is.read(buf)) != -1) {
								fos.write(buf, 0, bytesRead);
							}
						} finally {
							fos.close();
						}
						result.put(tmpFile, bodyPart.getFileName());
					} finally {
						is.close();
					}
				}

			}
		}

		return result;
	}
}
