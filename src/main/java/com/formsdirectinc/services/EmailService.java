package com.formsdirectinc.services;

import com.formsdirectinc.dao.CustomerSignup;
import com.formsdirectinc.environment.Environment;
import com.formsdirectinc.helpers.mail.Mailer;
import com.formsdirectinc.helpers.mail.VelocityHelper;
import com.formsdirectinc.messageSource.KrailMessageSource;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class EmailService {
    private static final Logger LOGGER = Logger.getLogger(EmailService.class);

    @Value("com/formsdirectinc/registration/resources/ResourceBundle")
    private String resourceBundleBaseName;

    @Autowired
    private KrailMessageSource messageSource;

    @Autowired
    private Environment environment;

    @Autowired
    private Mailer mailer;

    public void sendWelcomeEmail(CustomerSignup customerSignup, String application, String baseUrl) {
        String templateName = environment.getProperty(String.format("%s.welcome.email.template", application));
        if (templateName == null) {
            LOGGER.info("Welcome email template not configured");
            return;
        }

        LOGGER.info("Initializing the email template " + templateName);

        Locale locale = new Locale(customerSignup.getLanguage());
        Map<String, Object> contextParams = new HashMap<String, Object>();
        contextParams.put("baseUrl", baseUrl);
        contextParams.put("lang", customerSignup.getLanguage());
        contextParams.put("email", customerSignup.getEmailId());
        contextParams.put("firstName", customerSignup.getFirstName());
        contextParams.put("signupDate", new Date());

        String emailContent = new VelocityHelper().mergeTemplateToStringBuilder(templateName, "UTF-8",
                contextParams, resourceBundleBaseName).toString();

        HashMap<String, String> emailParameters = new HashMap<>();
        emailParameters.put("fromAddress", environment.getProperty(String.format("%s.welcome.email.sender", application)));
        emailParameters.put("fromName", environment.getProperty(String.format("%s.welcome.email.senderName", application)));
        emailParameters.put("toAddress", customerSignup.getEmailId());
        emailParameters.put("subject", messageSource.getMessage(String.format("%s.welcome.email.subject", application), new String[]{customerSignup.getFirstName()}, locale));
        emailParameters.put("emailContent", emailContent);

        LOGGER.info("Sending mail");
        sendMail(emailParameters, null);
    }

    /**
     * @param emailParameters  : Required parameters for sending an email
     * @param emailAttachments : Attachment if any
     * @throws Exception
     */
    private void sendMail(
            HashMap<String, String> emailParameters,
            HashMap<String, InputStream> emailAttachments) {

        if (emailAttachments != null && !emailAttachments.isEmpty()) {
            // Send mail with attachment
            mailer.sendMailPDF(
                    emailParameters.get("fromAddress"),
                    emailParameters.get("fromName"),
                    emailParameters.get("toAddress"),
                    emailParameters.get("replyToAddress"),
                    emailParameters.get("subject"),
                    emailParameters.get("emailContent"),
                    emailAttachments);
        } else {

            // Send plain mail without attachment
            mailer.sendMailHtml(
                    emailParameters.get("fromAddress"),
                    emailParameters.get("fromName"),
                    emailParameters.get("toAddress"),
                    emailParameters.get("replyToAddress"),
                    emailParameters.get("subject"),
                    emailParameters.get("emailContent"));
        }
    }
}
