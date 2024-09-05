/**
 * Mailer.java
 *
 * @author
 * @version $Id$
 * $Author$
 * $Date$
 * $Id$
 * $Rev $
 * <p>
 * Release ID: $Name$
 */

package com.formsdirectinc.helpers.mail;

import com.formsdirectinc.environment.Environment;
import com.formsdirectinc.messageSource.KrailMessageSource;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.InputStream;
import java.util.*;

@Component
public class Mailer {
    private static Logger log = Logger.getLogger(Mailer.class);

    @Autowired
    private KrailMessageSource messageSource;

    @Autowired
    private Environment environment;

    /**
     * Sent a mail as text
     *
     * @param sender
     *            - mail sender
     * @param receiver
     *            - mail receiver
     * @param subject
     *            - mail subject
     * @param messageBody
     *            - mail content
     * @return int(0 - mail sucess and 1 - mail failure)
     *
     */
    public int sendMail(String sender, String receiver, String subject,
                               String messageBody) {
        Monitor mon = MonitorFactory
                .start("sendMail(sender,receiver,subject, messageBody)");
        try {

            Properties props = new Properties();
            props.put("mail.smtp.auth", environment.getProperty("mail.smtp.auth"));
            props.put("mail.host", environment.getProperty("mail.host"));
            props.put("mail.smtp.port", environment.getProperty("mail.smtp.port"));
            Boolean isAuthEnabled = Boolean.valueOf(props.getProperty("mail.smtp.auth"));
            Session mailConnection = Session.getInstance(props, isAuthEnabled ? mailAuthenticator() : null);

            final MimeMessage msg = new MimeMessage(mailConnection);

            Address from = new InternetAddress(sender);

            // infact the to address is declared as an array
            // to facilitate multiple receipents for a single mail

            Address[] to = InternetAddress.parse(receiver);

            msg.setFrom(from);
            msg.setRecipients(Message.RecipientType.TO, to);
            msg.setSubject(subject);
            msg.setContent(messageBody, "text/plain");

            // This can take a non-trivial amount of time so
            // spawn a thread to handle it.
            // This will be of advantage when sending bulk mail or in case
            // of mail server connectivity delays.

            Runnable r = new Runnable() {
                public void run() {
                    try {
                        Transport.send(msg);
                    } catch (Exception e) {
                        log.error("Error while trying to send message", e);
                    }
                }
            };
            Thread t = new Thread(r);
            t.start();
        } catch (Exception e) {
            log.error("Error creating message", e);
            return 1;
        }
        mon.stop();
        return 0;
    }

    /**
     * Sent a mail as HTML
     *
     * @param sender      - mail sender
     * @param receiver    - mail receiver
     * @param subject     - mail subject
     * @param messageBody - mail content
     * @return int(0 - mail sucess and 1 - mail failure)
     */
    public int sendMailHtml(String sender, String receiver,
                                   String subject, String messageBody) {
        return sendMailHtml(sender, receiver, null, null, null, subject,
                messageBody);
    }

    /**
     * Sent a mail as HTML with cc and bcc list
     *
     * @param sender
     *            - mail sender
     * @param receiver
     *            - mail receiver
     * @param ccList
     *            - mail ccList
     * @param bccList
     *            - mail bccList
     * @param subject
     *            - mail subject
     * @param messageBody
     *            - mail content
     * @return int(0 - mail sucess and 1 - mail failure)
     *
     */
    public int sendMailHtml(String sender, String receiver,
                                   ArrayList ccList, ArrayList bccList, String subject,
                                   String messageBody) {
        return sendMailHtml(sender, receiver, null, ccList, bccList, subject,
                messageBody);
    }

    /**
     * Sent a mail as HTML with replyTo address
     *
     * @param sender
     *            - mail sender
     * @param receiver
     *            - mail receiver
     * @param replyTo
     *            - mail replyTo
     * @param subject
     *            - mail subject
     * @param messageBody
     *            - mail content
     * @return int(0 - mail sucess and 1 - mail failure)
     *
     */
    public int sendMailHtml(String sender, String receiver,
                                   String replyTo, String subject, String messageBody) {
        return sendMailHtml(sender, receiver, replyTo, null, null, subject,
                messageBody);
    }

    /**
     * Sent a mail as HTML with replyTo address
     *
     * @param sender      - mail sender
     * @param senderName  - mail sender name
     * @param receiver    - mail receiver
     * @param replyTo     - mail replyTo
     * @param subject     - mail subject
     * @param messageBody - mail content
     * @return int(0 - mail sucess and 1 - mail failure)
     */
    public int sendMailHtml(String sender, String senderName, String receiver,
                                   String replyTo, String subject, String messageBody) {
        return sendMailHtml(sender, senderName, receiver, replyTo, null, null, subject,
                messageBody);
    }

    /**
     * Sent a mail as HTML by getting the message dependencies from resource
     * bundle
     *
     * @param rbBaseName    - resource bundle name
     * @param sender        - mail sender
     * @param receiver      - mail receiver
     * @param subject       - mail subject
     * @param messageBody   - mail content
     * @param mailArguments - mail arguments as list
     * @return int(0 - mail sucess and 1 - mail failure)
     */
    public int sendMailHtml(String rbBaseName, String sender,
                                   String receiver, String subject, String messageBody,
                                   List mailArguments) {
        HashMap<String, String> mailContents = resolveContents(rbBaseName,
                sender, receiver, subject, messageBody, mailArguments);
        return sendMailHtml(mailContents.get("sender"), receiver, null, null,
                null, mailContents.get("subject"),
                mailContents.get("messageBody"));
    }

    /**
     * Sent a mail as HTML with cc and bcc list by getting the message
     * dependencies from resource bundle
     *
     * @param rbBaseName
     *            - resource bundle name
     * @param sender
     *            - mail sender
     * @param receiver
     *            - mail receiver
     * @param ccList
     *            - mail ccList
     * @param bccList
     *            - mail bccList
     * @param subject
     *            - mail subject
     * @param messageBody
     *            - mail content
     * @param mailArguments
     *            - mail arguments as list
     * @return int(0 - mail sucess and 1 - mail failure)
     *
     */

    public int sendMailHtml(String rbBaseName, String sender,
                                   String receiver, ArrayList ccList, ArrayList bccList,
                                   String subject, String messageBody, List mailArguments) {
        HashMap<String, String> mailContents = resolveContents(rbBaseName,
                sender, receiver, subject, messageBody, mailArguments);
        return sendMailHtml(mailContents.get("sender"), receiver, null, ccList,
                bccList, mailContents.get("subject"),
                mailContents.get("messageBody"));
    }

    public int sendMailHtml(String sender, String receiver,
                                   String replyTo, ArrayList ccList, ArrayList bccList,
                                   String subject, String messageBody) {
        return sendMailHtml(sender, null, receiver, replyTo, ccList, bccList, subject, messageBody);
    }

    /**
     * Overloaded method to sent a mail as HTML with cc and bcc list
     *
     * @param sender      - mail sender
     * @param receiver    - mail receiver
     * @param ccList      - mail ccList
     * @param bccList     - mail bccList
     * @param subject     - mail subject
     * @param messageBody - mail content
     * @return int(0 - mail sucess and 1 - mail failure)
     */

    public int sendMailHtml(String sender, String senderName, String receiver,
                                   String replyTo, ArrayList ccList, ArrayList bccList,
                                   String subject, String messageBody) {
        Monitor mon = MonitorFactory
                .start("sendMailHtml(sender,receiver,ccList,bccList,subject, messageBody)");
        try {

            Properties props = new Properties();
            System.out.println("<<<<<<<<>>>>>>>>>>>"+environment);
            System.out.println("<<<<<<<<>>>>>>>>>>>"+environment.getProperty("mail.smtp.auth"));
            props.put("mail.smtp.auth", environment.getProperty("mail.smtp.auth"));
            props.put("mail.host", environment.getProperty("mail.host"));
            props.put("mail.smtp.port", environment.getProperty("mail.smtp.port"));
            Boolean isAuthEnabled = Boolean.valueOf(props.getProperty("mail.smtp.auth"));
            Session mailConnection = Session.getInstance(props, isAuthEnabled ? mailAuthenticator() : null);

            final MimeMessage msg = new MimeMessage(mailConnection);

            Address from;
            if (senderName == null) {
                from = new InternetAddress(sender);
            } else {
                from = new InternetAddress(sender, senderName);
            }

            // infact the to address is declared as an array
            // to facilitate multiple receipents for a single mail

            Address[] to = InternetAddress.parse(receiver);

            msg.setFrom(from);
            msg.setRecipients(Message.RecipientType.TO, to);
            if (replyTo != null) {
                msg.setReplyTo(InternetAddress.parse(replyTo));
            }
            if (bccList != null) {
                Iterator bccListItr = bccList.iterator();
                while (bccListItr.hasNext()) {
                    msg.addRecipient(Message.RecipientType.BCC,
                            new InternetAddress((String) bccListItr.next()));
                }
            }

            if (ccList != null) {
                Iterator ccListItr = ccList.iterator();
                while (ccListItr.hasNext()) {
                    msg.addRecipient(Message.RecipientType.CC,
                            new InternetAddress((String) ccListItr.next()));
                }
            }
            msg.setSubject(subject);
            msg.setContent(messageBody, "text/html");

            // This can take a non-trivial amount of time so
            // spawn a thread to handle it.
            // This will be of advantage when sending bulk mail or in case
            // of mail server connectivity delays.
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        Transport.send(msg);
                    } catch (Exception e) {
                        log.error("Error occured while trying to send message",
                                e);
                    }
                }
            };
            Thread t = new Thread(r);
            t.start();
        } catch (Exception e) {
            log.error("Error occured while trying to send message", e);
            return 1;
        }

        mon.stop();
        return 0;
    }

    /**
     * Sent a mail as HTML with PDF attachment.
     *
     * @param sender
     *            - mail sender
     * @param receiver
     *            - mail receiver
     * @param subject
     *            - mail subject
     * @param messageBody
     *            - mail content
     * @param pdfFiles
     *            - mail attachments
     * @return int(0 - mail sucess and 1 - mail failure)
     *
     */

    public int sendMailPDF(String sender, String receiver,
                                  String subject, String messageBody,
                                  HashMap<String, InputStream> pdfFiles) {
        return sendMailPDF(sender, receiver, null, null, null, subject,
                messageBody, pdfFiles);
    }

    /**
     * Sent a mail as HTML with PDF attachment.(Has replyTo feature)
     *
     * @param sender
     *            - mail sender
     * @param receiver
     *            - mail receiver
     * @param replyTo
     *            - mail replyto address
     * @param subject
     *            - mail subject
     * @param messageBody
     *            - mail content
     * @param pdfFiles
     *            - mail attachments
     * @return int(0 - mail sucess and 1 - mail failure)
     *
     */

    public int sendMailPDF(String sender, String receiver,
                                  String replyTo, String subject, String messageBody,
                                  HashMap<String, InputStream> pdfFiles) {
        return sendMailPDF(sender, receiver, replyTo, null, null, subject,
                messageBody, pdfFiles);
    }

    /**
     * Sent a mail as HTML with PDF attachment.(Has replyTo feature)
     *
     * @param sender      - mail sender
     * @param senderName  - mail sender name
     * @param receiver    - mail receiver
     * @param replyTo     - mail replyto address
     * @param subject     - mail subject
     * @param messageBody - mail content
     * @param pdfFiles    - mail attachments
     * @return int(0 - mail sucess and 1 - mail failure)
     */

    public int sendMailPDF(String sender, String senderName, String receiver,
                                  String replyTo, String subject, String messageBody,
                                  HashMap<String, InputStream> pdfFiles) {
        return sendMailPDF(sender, senderName, receiver, replyTo, null, null, subject,
                messageBody, pdfFiles);
    }

    /**
     * Sent a mail with PDF attachment as HTML by getting the message
     * dependencies from resource bundle
     *
     * @param rbBaseName  - resource bundle name
     * @param sender      - mail sender
     * @param receiver    - mail receiver
     * @param subject     - mail subject
     * @param messageBody - mail content
     * @param pdfFiles    - mail attachments
     * @return int(0 - mail sucess and 1 - mail failure)
     */
    public int sendMailPDF(String rbBaseName, String sender,
                                  String receiver, String subject, String messageBody,
                                  List mailArguments, HashMap<String, InputStream> pdfFiles) {
        HashMap<String, String> mailContents = resolveContents(rbBaseName,
                sender, receiver, subject, messageBody, mailArguments);
        return sendMailPDF(mailContents.get("sender"), receiver, null, null,
                null, mailContents.get("subject"),
                mailContents.get("messageBody"), pdfFiles);
    }

    /**
     * Sent a mail with ccList, bccList and PDF attachment as HTML by getting
     * the message dependencies from resource bundle
     *
     * @param rbBaseName
     *            - resource bundle name
     * @param sender
     *            - mail sender
     * @param receiver
     *            - mail receiver
     * @param ccList
     *            - mail ccList
     * @param bccList
     *            - mail bccList
     * @param subject
     *            - mail subject
     * @param messageBody
     *            - mail content
     * @param mailArguments
     *            - mail arguments as list
     * @param pdfFiles
     *            - attachment PDF files
     * @return int(0 - mail sucess and 1 - mail failure)
     *
     */
    public int sendMailPDF(String rbBaseName, String sender,
                                  String receiver, ArrayList ccList, ArrayList bccList,
                                  String subject, String messageBody, List mailArguments,
                                  HashMap<String, InputStream> pdfFiles) {

        HashMap<String, String> mailContents = resolveContents(rbBaseName,
                sender, receiver, subject, messageBody, mailArguments);
        return sendMailPDF(mailContents.get("sender"), receiver, null, null,
                null, mailContents.get("subject"),
                mailContents.get("messageBody"), pdfFiles);
    }

    /**
     * Overloaded method to sent a mail with ccList, bccList and PDF attachment
     * as HTML by getting the message dependencies from resource bundle
     *
     * @param sender
     *            - mail sender
     * @param receiver
     *            - mail receiver
     * @param ccList
     *            - mail ccList
     * @param bccList
     *            - mail bccList
     * @param subject
     *            - mail subject
     * @param messageBody
     *            - mail content
     * @param pdfFiles
     *            - attachment PDF files
     * @return int(0 - mail sucess and 1 - mail failure)
     *
     */
    public int sendMailPDF(String sender, String receiver,
                                  String replyTo, ArrayList ccList, ArrayList bccList,
                                  String subject, String messageBody,
                                  HashMap<String, InputStream> pdfFiles) {
        return sendMailPDF(sender, null, receiver, replyTo, ccList, bccList, subject, messageBody, pdfFiles);
    }

    /**
     * Overloaded method to sent a mail with ccList, bccList and PDF attachment
     * as HTML by getting the message dependencies from resource bundle
     *
     * @param sender      - mail sender
     * @param senderName  - mail sender name
     * @param receiver    - mail receiver
     * @param ccList      - mail ccList
     * @param bccList     - mail bccList
     * @param subject     - mail subject
     * @param messageBody - mail content
     * @param pdfFiles    - attachment PDF files
     * @return int(0 - mail sucess and 1 - mail failure)
     */
    public int sendMailPDF(String sender, String senderName, String receiver,
                                  String replyTo, ArrayList ccList, ArrayList bccList,
                                  String subject, String messageBody,
                                  HashMap<String, InputStream> pdfFiles) {

        Monitor mon = MonitorFactory
                .start("sendMailPDF(sender,receiver,subject,messageBody,pdfFiles)");
        try {

            Properties props = new Properties();
            props.put("mail.smtp.auth", environment.getProperty("mail.smtp.auth"));
            props.put("mail.host", environment.getProperty("mail.host"));
            props.put("mail.smtp.port", environment.getProperty("mail.smtp.port"));
            Boolean isAuthEnabled = Boolean.valueOf(props.getProperty("mail.smtp.auth"));
            Session mailConnection = Session.getInstance(props, isAuthEnabled ? mailAuthenticator() : null);

            final MimeMessage msg = new MimeMessage(mailConnection);

            Address from;
            if (senderName == null) {
                from = new InternetAddress(sender);
            } else {
                from = new InternetAddress(sender, senderName);
            }
            // infact the to address is declared as an array
            // to facilitate multiple receipents for a single mail

            Address[] to = InternetAddress.parse(receiver);

            msg.setFrom(from);
            msg.setRecipients(Message.RecipientType.TO, to);
            if (bccList != null) {
                Iterator bccListItr = bccList.iterator();
                while (bccListItr.hasNext()) {
                    msg.addRecipient(Message.RecipientType.BCC,
                            new InternetAddress((String) bccListItr.next()));
                }
            }

            if (ccList != null) {
                Iterator ccListItr = ccList.iterator();
                while (ccListItr.hasNext()) {
                    msg.addRecipient(Message.RecipientType.CC,
                            new InternetAddress((String) ccListItr.next()));
                }
            }
            if (replyTo != null) {
                msg.setReplyTo(InternetAddress.parse(replyTo));
            }
            msg.setSubject(subject);
            MimeBodyPart mbp1 = new MimeBodyPart();
            mbp1.setText(messageBody, "UTF-8", "html");
            // javax.activation.MimetypesFileTypeMap mimeType = new
            // javax.activation.MimetypesFileTypeMap();
            // mimeType.addMimeTypes("application/pdf");
            Multipart mp = new MimeMultipart();
            mp.addBodyPart(mbp1);

            for (Object key : pdfFiles.keySet()) {

                MimeBodyPart mbp2 = new MimeBodyPart();
                mbp2.setHeader("Content-Type", "application/pdf");

                javax.activation.DataSource ds = new javax.mail.util.ByteArrayDataSource(
                        pdfFiles.get(key), "application/pdf");
                mbp2.setDataHandler(new javax.activation.DataHandler(ds));

                mbp2.setFileName(key.toString());

                mp.addBodyPart(mbp2);
            }
            msg.setContent(mp);
            msg.setSentDate(new Date());
            // This can take a non-trivial amount of time so
            // spawn a thread to handle it.
            // This will be of advantage when sending bulk mail or in case
            // of mail server connectivity delays.
            Runnable r = new Runnable() {
                public void run() {
                    try {
                        System.out.println("MESSAGE : " + msg);
                        Transport.send(msg);
                    } catch (Exception e) {
                        log.error("Error occurred while trying to send message",
                                e);
                    }
                }
            };
            Thread t = new Thread(r);
            t.start();
        } catch (Exception e) {
            log.error("Error occurred while trying to send message", e);
            return 1;
        }
        mon.stop();
        return 0;
    }

    /**
     * Method resolves the dependencies of the content from resource bundle
     *
     * @param rbBaseName    - resource bundle name
     * @param sender        - mail sender
     * @param receiver      - mail receiver
     * @param subject       - mail subject
     * @param messageBody   - mail content
     * @param mailArguments - mail arguments as list
     * @return map - mail related info's as hashmap
     */
    public HashMap<String, String> resolveContents(String rbBaseName,
                                                          String sender, String receiver, String subject, String messageBody,
                                                          List mailArguments) {
        HashMap<String, String> contents = new HashMap<String, String>();


        sender = messageSource.getMessage(sender, null, new Locale("en"));
        subject = messageSource.getMessage(subject, null, new Locale("en"));
        messageBody = messageSource.getMessage(messageBody, null, new Locale("en"));
        // Set the mail Arg to 2 since both SenderMail Id will be 0 and userMail
        // Id will be 1.
        // First Check how many arguments have been passed and create the Object
        // Array accordingly.

        // int mailArg = 2 + mailArguments.size();

        // Create Object Array.
        Object[] arguments = new Object[2 + mailArguments.size()];

        // Set both the senderMail and
        // userMail
        arguments[0] = sender;
        arguments[1] = receiver;

        // Now set the passed mail arguments to the Object Array.
        int mailArg = 2;
        for (Iterator iter = mailArguments.listIterator(); iter.hasNext(); ) {
            arguments[mailArg] = iter.next();
            mailArg++;
        }
        contents.put("sender", sender);
        contents.put("subject",
                java.text.MessageFormat.format(subject, arguments));
        // Pass the messageBody and arguments
        contents.put("messageBody",
                java.text.MessageFormat.format(messageBody, arguments));
        return contents;
    }

    // Mail authentication.
    private Authenticator mailAuthenticator() {
        String username = environment.getProperty("mail.smtp.user");
        String password = environment.getProperty("mail.smtp.password");
        return new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        };
    }
} // Mailer End's
