/**
 * AccountDelegate.java
 * <p/>
 * <p/>
 * Created: Wed Jul 12 10:57:17 2006
 *
 * @author Selvakumar
 * @version $Id$
 * <p/>
 * Release ID: $Name$
 * <p/>
 * Last modified:
 * <p/>
 * AccountDelegate Manages the Customer Registraction process and the Application Process of the users.
 * createAccount, UpdateProfile, retrieveProfile, retrievePassword, authenticate and AccountSummary process are handled
 * by the AccountDelegate. For the retrievePassword() method we have used the Ostermiller RandPass util class to
 * generate
 * a random password and to send it to the user.
 */

package com.formsdirectinc.services.account;

import com.formsdirectinc.dao.*;
import com.formsdirectinc.helpers.mail.Mailer;
import com.formsdirectinc.messageSource.KrailMessageSource;
import com.formsdirectinc.security.passwords.DoubleEncryptionException;
import com.formsdirectinc.security.passwords.PasswordStorageServiceRegistry;
import com.formsdirectinc.tenant.TenantContextHolder;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

@Component
@Scope("singleton")
public class AccountDelegate {

    public static Logger log = Logger.getLogger(AccountDelegate.class.getName());
    public static boolean lastAppSavedStatus = true;
    boolean lastAppSaveStatus;
    Map sessionMap;
    @Autowired
    private com.formsdirectinc.SessionFactory sessionFactory;
    @Autowired
    private String passwordServiceName;
    @Autowired
    private CustomerSignupDAO customerSignupDAO;
    @Autowired
    private KrailMessageSource messageSource;

    @Autowired
    private Mailer mailer;

    @Autowired
    private CustomerConsentDAO customerConsentDAO;

    public AccountDelegate() {
    }

    public void setPasswordServiceName(String passwordServiceName) {
        this.passwordServiceName = passwordServiceName;
    }

    public void createAccount(CustomerSignup customerSignup) throws AccountCreationException {
        try {

            if(customerSignup.getEmailId() !=null) {
                customerSignup.setEmailId(customerSignup.getEmailId().toLowerCase());
            }

            if (customerSignup.getPassword() != null) {
                try {
                    String digestedPassword = PasswordStorageServiceRegistry.getInstance()
                            .getService(getPasswordServiceName()).protectPassword(customerSignup.getPassword());
                    customerSignup.setPassword(digestedPassword);
                } catch (DoubleEncryptionException de) {
                    log.error("Password already digested");
                }
            }
            customerSignupDAO.save(customerSignup);

        } catch (Exception e) {
            throw new AccountCreationException(e);
        }
    }

    /**
     * Update an already registered User Account
     *
     * @param cs
     *            CustomerSignup bean
     * @return true if updated false otherwise TODO: Detach and Attach are
     *         working, but find if this is the best way to do?
     */
    public boolean updateProfile(CustomerSignup cs) {
        Monitor mon = MonitorFactory.start("updateProfile(cs)");
        boolean result = false;
        Transaction tx = null;
        // Open the new Session
        Session hibernateSession = sessionFactory.openSession();

        log.debug("Entered UpdateProfile");
        //
        // Check for countryCode & countryName from country value and save in db
        //

        formatTelephone(cs);

        log.debug("CustomerSignup Updated:" + cs);

        try {
            // Create the updated session
            // Encrypt before updating to the Database
            log.debug("CS Email:" + cs.getEmailId());

            if(cs.getPassword() != null) {
                try {
                    String cipherText = PasswordStorageServiceRegistry.getInstance().getService(getPasswordServiceName())
                            .protectPassword(cs.getPassword());
                    cs.setPassword(cipherText);
                } catch (DoubleEncryptionException de) {
                    log.warn("Password already encrypted");
                }
            }
            tx = hibernateSession.beginTransaction();
            hibernateSession.update(cs);
            // After Updation Decrypt the Password to have it in session
            tx.commit();

            result = true;
        } catch (HibernateException hibernateException) {
            log.error("Error updating profile for customer with email id: " + cs.getEmailId(), hibernateException);
            if (tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
                tx.rollback();
            result = false;
        } catch (Exception e) {
            log.error("Error updating profile for customer with email id: " + cs.getEmailId(), e);
        } finally {
            if (hibernateSession != null) {
                hibernateSession.close();
            }
        }
        mon.stop();
        return result;
    }// updateAccount

    /**
     * @param emailId
     *            customer email
     * @return customer profile as a bean
     */
    public CustomerSignup retrieveProfile(String emailId) {
        Monitor mon = MonitorFactory.start("retrieveProfile(emailId)");
        CustomerSignup customer = null;
        log.debug("RetrieveProfile Email" + emailId);
        emailId = emailId.toLowerCase();
        log.debug("RetrieveProfile Email 1" + emailId);
        try {
            List csList = customerSignupDAO.findByEmail(emailId);
            Iterator csIter = csList.iterator();
            while (csIter.hasNext()) {
                customer = (CustomerSignup) csIter.next();
                log.debug("customer Email:" + customer.getEmailId());
            }// while
        } catch (HibernateException he) {
            log.error("Error retrieving customer with email ID: " + emailId, he);
        } catch (Exception e) {
            log.error("Error retrieving customer with email ID: " + emailId, e);
        }
        mon.stop();
        return customer;
    }// retrieveCustomer


    /**
     * @param emailId
     *            customer email
     * @return customer profile as a bean
     */
    public CustomerSignup retrieveCustomerSignup(String emailId) {
        CustomerSignup customer = null;
        emailId = emailId.toLowerCase();
        log.debug("Retrieve CustomerSignup with email" + emailId);
        List csList = customerSignupDAO.findByEmail(emailId);
        Iterator csIter = csList.iterator();
        while (csIter.hasNext()) {
            customer = (CustomerSignup) csIter.next();
            log.debug(String.format("CustomerId for email %s is %s:", customer.getEmailId(), customer.getId()));
        }// while

        return customer;
    }// retrieveCustomer


    /**
     * @param authId
     *            customer email
     * @return customer profile as a bean
     */
    public CustomerSignup retrieveProfileByAuthId(String authId) {
        Monitor mon = MonitorFactory.start("retrieveProfileByAuthId(authId)");
        CustomerSignup customer = null;
        log.debug("RetrieveProfile authId" + authId);
        try {
            List csList = customerSignupDAO.findByAuthId(authId);
            Iterator csIter = csList.iterator();
            while (csIter.hasNext()) {
                customer = (CustomerSignup) csIter.next();
                log.debug("customer AuthId:" + customer.getAuthId());
            }// while
        } catch (HibernateException he) {
            log.error("Error retrieving customer with auth ID: " + authId, he);
        } catch (Exception e) {
            log.error("Error retrieving customer with auth ID: " + authId, e);
        }
        mon.stop();
        return customer;
    }// retrieveCustomer

    /**
     * retrieves userId from applicationTable for the given applicationId
     *
     * @param applicationId
     *            appppicationId for which the userId need to retrieved
     * @return userId to which the application belogs
     */

    public long retrieveUserId(long applicationId) {
        Monitor mon = MonitorFactory.start("retrieveUserId(applicationId)");
        long userId = -1L;
        Session hibernateSession = sessionFactory.openSession();
        try {
            Query q = hibernateSession
                            .createQuery("select app.userId from ApplicationTable app where app.applicationId = :applicationId");
            q.setParameter("applicationId", applicationId);

            List localList = q.list();
            Iterator iter = localList.iterator();

            while (iter.hasNext()) {
                Object data = (Object) iter.next();
                userId = (Long) data;
            }
        } catch (Exception e) {
            log.error("Error retrieving user id for application ID:  " + applicationId, e);
        } finally {
            if (hibernateSession != null) {
                hibernateSession.close();
            }
        }
        mon.stop();
        return userId;
    }

    /**
     * function: retrieveEmail * @param userId
     *
     * @return emailId
     */

    public String retrieveEmail(long userId) {
        Monitor mon = MonitorFactory.start("retrieveEmail(userId)");
        String emailId = "";
        Session hibernateSession = sessionFactory.openSession();
        Transaction tx = null;
        try {
            tx = hibernateSession.beginTransaction();
            Query q = hibernateSession.createQuery("select cs.emailId from CustomerSignup cs where cs.id= " + userId
                            + "");

            List localList = q.list();
            Iterator iter = localList.iterator();

            while (iter.hasNext()) {
                Object data = (Object) iter.next();
                emailId = (String) data;
            }
            tx.commit();

        } catch (Exception e) {
            log.error("Error retrieving email for user ID: " + userId, e);
        } finally {
            if (hibernateSession != null) {
                hibernateSession.close();
            }
        }
        mon.stop();
        return emailId;
    }

    /**
     * function: retrieveCustomerSignup
     *
     * @param userId
     * @return CustomerSignup bean
     */
    public CustomerSignup retrieveCustomerSignup(long userId) {
        Monitor mon = MonitorFactory.start("retrieveEmail(userId)");
        String emailId = "";
        Session hibernateSession = sessionFactory.openSession();
        CustomerSignup cs = null;
        Transaction tx = null;
        try {
            Query q = hibernateSession.createQuery(" from CustomerSignup cs where cs.id= " + userId + "");

            List localList = q.list();
            Iterator iter = localList.iterator();

            while (iter.hasNext()) {
                cs = (CustomerSignup) iter.next();
            }

        } catch (Exception e) {
            log.error("Error retrieving customer with user ID: " + userId, e);
        } finally {
            if (hibernateSession != null) {
                hibernateSession.close();
            }
        }
        mon.stop();
        return cs;
    }

    /**
     *
     * @param emailId
     * @param password
     * @return HashMap<String, String> with login authentication response
     */
    public HashMap<String, String> authenticate(String emailId, String password) {

        CustomerSignup customer;

        HashMap<String, String> authenticationResponse = new HashMap<>();

        try {
            List csList = customerSignupDAO.findByEmail(emailId.toLowerCase());
            if (csList.size() == 0) {
                authenticationResponse.put("status", "ERROR");
                authenticationResponse.put("errorCode", "0001");
                authenticationResponse.put("field", "EMAIL");
                return authenticationResponse;
            }
            Iterator csIter = csList.iterator();
            while (csIter.hasNext()) {
                customer = (CustomerSignup) csIter.next();
                if(PasswordStorageServiceRegistry.getInstance().getService(getPasswordServiceName())
                        .comparePassword(password, customer.getPassword())){
                    authenticationResponse.put("status", "OK");
                    authenticationResponse.put("errorCode", "0");
                    return authenticationResponse;
                }
            }
        } catch (Exception e) {
            log.error("Error authenticating user with email ID: " + emailId, e);
        }

        authenticationResponse.put("status", "ERROR");
        authenticationResponse.put("errorCode", "0002");
        authenticationResponse.put("field", "PASSWORD");
        return authenticationResponse;
    }

    private String getPasswordServiceName() {
        return TenantContextHolder.getTenantId() + "_" + this.passwordServiceName;
    }

    /**
     * Authenticate a user given email address and hashed password
     *
     * @param emailAddress
     *            User's email address
     * @param hashedPassword
     *            Hashed password
     * @return true if the two hashed passwords match, false otherwise
     */

    public boolean authenticateWithHashedPassword(String emailAddress, String hashedPassword) {
        CustomerSignup customer = retrieveProfile(emailAddress);
        if (customer == null) {
            return false;
        }
        return (hashedPassword != null && hashedPassword.equals(customer.getPassword()));
    }

    /**
     * function : retrieveAccountSummaryUsingEmailID Retrieve AccountSummary
     * from the User Application.
     *
     * @param emailId
     *            return List of ApplicationTable
     */
    public List<ApplicationTable> retrieveAccountSummaryUsingEmailID(String emailId) {
        // Monitor
        // rp=MonitorFactory.startPrimary("AccountDelegateMonitor.retrieveAccountSummaryUsingEmailID");
        List atList = new ArrayList();
        List<ApplicationTable> appList = new ArrayList<ApplicationTable>();

        ApplicationTableHome applicationHome = new ApplicationTableHome();
        try {
            atList = applicationHome.findByEmailId(emailId.toLowerCase());
            Iterator atIter = atList.iterator();
            while (atIter.hasNext()) {
                ApplicationTable application = new ApplicationTable();
                Object[] twoData = (Object[]) atIter.next();
                application = (ApplicationTable) twoData[0];
                log.info("ApplicationTable Id:" + application.getApplicationId());
                log.info("ApplicationTable Type:" + application.getApplicationType());
                appList.add(application);
            }// while
        } catch (HibernateException he) {
            log.error("Error retrieving account summary for user with Email ID: " + emailId, he);
        } catch (Exception e) {
            log.error("Error retrieving account summary for user with Email ID: " + emailId, e);
        }
        // rp.stop();
        return appList;
    }// retrieveApplication

    /**
     * function : retrieveAccountSummary Retrieve AccountSummary from the User
     * Application.
     *
     * @param userId
     *            return List of ApplicationTable
     */
    public List retrieveAccountSummary(String userId) {
        Monitor mon = MonitorFactory.start("retrieveAccountSummary(userId)");
        List atList = new ArrayList();
        Session hibernateSession = sessionFactory.openSession();
        Transaction tx = null;
        // ApplicationTable application = new ApplicationTable();
        try {
            tx = hibernateSession.beginTransaction();
            Query q = hibernateSession
                            .createQuery(" from ApplicationTable as appTable where appTable.userId = :appTableUserId");
            q.setLong("appTableUserId", Long.parseLong(userId.trim()));
            atList = q.list();
            tx.commit();
        } catch (HibernateException he) {
            log.error("Error retrieving account summary for user ID: " + userId, he);
            if (tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
                tx.rollback();
        } finally {
            try {
                if (hibernateSession != null) {
                    hibernateSession.close();
                }
            } catch (Exception e) {
                log.error("Error closing hibernate session: ", e);
            }
        }
        mon.stop();
        return atList;
    }// retrieveApplication

    /**
     * function : retrieveAccount Retrieve AccountSummary from the User
     * Application.
     *
     * @param emailId
     * @param applicationId
     * @param applicationType
     *            return ApplicationTable
     */

    public ApplicationTable retrieveAccount(String emailId, long applicationId, int applicationType) {
        Monitor mon = MonitorFactory.start("retrieveAccount( emailId,applicationId,applicationType)");
        ApplicationTable application = new ApplicationTable();
        ApplicationTableHome applicationHome = new ApplicationTableHome();
        try {
            List atList = applicationHome.findByEmailIdAppIdAppType(emailId.toLowerCase(), applicationType,
                            applicationId);
            Iterator atIter = atList.iterator();
            while (atIter.hasNext()) {
                application = (ApplicationTable) atIter.next();
                log.debug("ApplicationTable Id:" + application.getApplicationId());
                log.debug("ApplicationTable Type:" + application.getApplicationType());
            }// while
        } catch (HibernateException he) {
            he.printStackTrace();
        }
        mon.stop();
        return application;
    }// retrieveApplication

    /**
     * function: mailUser()
     *
     * @param rbBaseName
     * @param senderMail
     * @param userMail
     * @param subject
     * @param messageBody
     * @param mailArguments
     * @param language
     */

    public void mailUser(String rbBaseName, String senderMail, String userMail, String subject, String messageBody,
                    List mailArguments, String language) {
        mailUser(rbBaseName, senderMail, userMail, null, null, subject, messageBody, mailArguments, language);
    }

    /**
     * function: mailUser()
     *
     * @param rbBaseName
     * @param senderMail
     * @param userMail
     * @param subject
     * @param messageBody
     * @param mailArguments
     */

    public void mailUser(String rbBaseName, String senderMail, String userMail, String subject, String messageBody,
                    List mailArguments) {
        mailUser(rbBaseName, senderMail, userMail, null, null, subject, messageBody, mailArguments, null);
    }

    /**
     * function: mailUser() - overloaded method to have cc and bcc fecility
     *
     * @param rbBaseName
     * @param senderMail
     * @param userMail
     * @param ccList
     * @param bccList
     * @param subject
     * @param messageBody
     * @param mailArguments
     * @param language
     */

    public void mailUser(String rbBaseName, String senderMail, String userMail, ArrayList ccList, ArrayList bccList,
                    String subject, String messageBody, List mailArguments, String language) {
        Monitor mon = MonitorFactory
                        .start("mailUser(rbBaseName,senderMail,userMail,ccList,bccList,subject,messageBody,mailArguments)");
        Mailer mailer = new Mailer();

        if (language == null) {
            language = "en";
        }
        Locale locale = new Locale(language);
        // getting contents from Resource Bundle for senderMail,subject and
        // messageBody
        senderMail = messageSource.getMessage(senderMail, null, locale);
        subject = messageSource.getMessage(subject, null, locale);
        messageBody = messageSource.getMessage(messageBody, null, locale);
        // Set the mail Arg to 2 since both SenderMail Id will be 0 and userMail
        // Id will be 1.
        // First Check how many arguments have been passed and create the Object
        // Array accordingly.

        int mailArg = 2;
        for (Iterator iter = mailArguments.listIterator(); iter.hasNext();) {
            mailArg++;
            log.debug("MailArg " + mailArg + ":" + (String) iter.next());
        }
        // Create Object Array.

        Object[] arguments = new Object[mailArg];

        // Set both the senderMail and
        // userMail
        arguments[0] = senderMail;
        arguments[1] = userMail;

        // Now set the passed mail arguments to the Object Array.
        int mailArg1 = 2;
        for (Iterator iter = mailArguments.listIterator(); iter.hasNext();) {
            arguments[mailArg1] = (String) iter.next();
            mailArg1++;
            log.debug("MailArg" + mailArg1);
        }

        subject = java.text.MessageFormat.format(subject, arguments);
        // Pass the messageBody and arguments
        messageBody = java.text.MessageFormat.format(messageBody, arguments);

        // Send the mail
        mailer.sendMailHtml(senderMail, userMail, ccList, bccList, subject, messageBody);
        mon.stop();
    }

    /**
     * function: mailUser() - overloaded method to send mail with pdf attachment
     *
     * @param template
     *            name which used to generate signed receipt
     * @param root
     *            path of the server
     * @param base
     *            name for the resource bundle
     * @param name
     *            of the pdf attachment
     * @param resource
     *            bundle key for the sender email address
     * @param user
     *            email address
     * @param resource
     *            bundle key for the subject
     * @param resource
     *            bundle key for the message body
     * @param language
     *            user chosen language
     * @param list
     *            of mail arguments which needs to be substituted in the message
     *            body
     */
    public void mailUser(String rbBaseName, String senderMail, String userMail, String subject, String messageBody,
                    List mailArguments, String attachmentName, InputStream is, String language) {
        Monitor mon = MonitorFactory
                        .start("mailUser(rbBaseName,senderMail,userMail,subject,messageBody,mailArguments,attachmentName,is)");

        if (language == null) {
            language = "en";
        }
        Locale locale = new Locale(language);
        // getting contents from Resource Bundle for senderMail,subject and
        // messageBody

        senderMail = messageSource.getMessage(senderMail, null, locale);
        subject = messageSource.getMessage(subject, null, locale);
        messageBody = messageSource.getMessage(messageBody, null, locale);
        // Set the mail Arg to 2 since both SenderMail Id will be 0 and userMail
        // Id will be 1.
        // First Check how many arguments have been passed and create the Object
        // Array accordingly.

        int mailArg = 2;
        for (Iterator iter = mailArguments.listIterator(); iter.hasNext();) {
            mailArg++;
            log.debug("MailArg " + mailArg + ":" + (String) iter.next());
        }
        // Create Object Array.
        Object[] arguments = new Object[mailArg];

        // Set both the senderMail and
        // userMail
        arguments[0] = senderMail;
        arguments[1] = userMail;

        // Now set the passed mail arguments to the Object Array.

        int mailArg1 = 2;
        for (Iterator iter = mailArguments.listIterator(); iter.hasNext();) {
            arguments[mailArg1] = (String) iter.next();
            mailArg1++;
            log.debug("MailArg" + mailArg1);
        }
        // Pass the messageBody and arguments
        messageBody = java.text.MessageFormat.format(messageBody, arguments);

        // used to put mail attachment source
        HashMap<String, InputStream> pdfFiles = new HashMap<String, InputStream>();
        pdfFiles.put(attachmentName, is);

        // Send the mail
        mailer.sendMailPDF(senderMail, userMail, subject, messageBody, pdfFiles);
        mon.stop();
    }

    /**
     * Get the value of sessionMap.
     *
     * @return value of sessionMap.
     */
    public Map getSessionMap() {
        return sessionMap;
    }

    /**
     * Set the value of sessionMap.
     *
     * @param v
     *            Value to assign to sessionMap.
     */
    public void setSessionMap(Map v) {
        this.sessionMap = v;
    }

    /**
     * function: deleteAlertSiteAccount() Function to detlete the
     * Performanceid\@dcis.net account which will be created for Alert Site
     *
     * @return int
     */

    public int deleteAlertSiteAccount() {
        Monitor mon = MonitorFactory.start("deleteAlertSiteAccount()");
        int result = 1;
        Transaction tx = null;
        CustomerSignup perCustomerSignup = retrieveProfile("performanceid@dcis.net");
        Session hibernateSession = sessionFactory.openSession();
        log.debug("HibernateSession:" + hibernateSession);
        try {
            tx = hibernateSession.beginTransaction();
            if (null != perCustomerSignup)
                deleteCustomerSignup(perCustomerSignup);
            log.info("Deleted performanceid@dcis.net email");
            result = 0;
            log.info("\nresult :" + result);
        } catch (HibernateException hibernateException) {
            result = 1;
            log.info("\nresult :" + result);
            log.error("Error deleting alertsite account", hibernateException);
            if (tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
                tx.rollback();
        } catch (Exception exception) {
            log.error("Error deleting alertsite account", exception);
            if (tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
                tx.rollback();
            result = 1;
            log.info("\nresult :" + result);
        } finally {
            if (hibernateSession != null) {
                hibernateSession.close();
            }
        }
        mon.stop();
        log.info("\nresult :" + result);
        return result;
    }// deleteAlertSiteAccount

    /**
     * function: deleteCustomerSignup() Delete Customer Signup from the
     * database. Currently we use only for Alert site.
     *
     * @param CustomerSignup
     */
    private void deleteCustomerSignup(CustomerSignup cs) {
        Transaction tx = null;
        Session session = sessionFactory.openSession();
        try {
            tx = session.beginTransaction();
            session.delete(cs);
            tx.commit();
        } catch (HibernateException e) {
            log.error("Error deleting customer signup with emailId: " + cs.getEmailId() + " and ID: " + cs.getId(), e);
            if (tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
                tx.rollback();
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }// deleteCustomerSignup

    /**
     * function: formatTelephone() Function to format and store telephone number
     * in data base
     * 
     * @param customerSignup
     */

    public void formatTelephone(CustomerSignup cs) {

        String areaCode = cs.getSigninPhone().getAreaCode();
        String phone1 = cs.getSigninPhone().getPhone1();
        String phone2 = cs.getSigninPhone().getPhone2();
        String phone3 = cs.getSigninPhone().getPhone3();
        String countryValue = cs.getSigninPhone().getCountry();

        if (StringUtils.isNotBlank(areaCode))
            areaCode = areaCode.replaceAll("\\D+", "");

        if (StringUtils.isNotBlank(phone1))
            phone1 = phone1.replaceAll("\\D+", "");

        if (StringUtils.isNotBlank(phone2))
            phone2 = phone2.replaceAll("\\D+", "");

        if (StringUtils.isNotBlank(phone3))
            phone3 = phone3.replaceAll("\\D+", "");

        if (phoneNumberNotProvided(countryValue, areaCode, phone1, phone2, phone3)) {
            log.warn("Phone number not provided");

        } else if (isValidTelephoneCountry(countryValue)) {
            String[] splitCountryValue = countryValue.split("_");
            cs.setTelephoneCountryCode(splitCountryValue[0].trim());
            cs.setTelephoneCountryName(splitCountryValue[1]);
            String telephone = splitCountryValue[0].equals("1") ? areaCode + phone1 + phone2 : phone3;
            cs.setTelephone(telephone);
            log.debug("Telephone updated");
        } else {
            String[] splitCountryValue = countryValue.split(":");
            cs.setTelephoneCountryName(splitCountryValue[0].trim());
            cs.setTelephoneCountryCode( splitCountryValue[1].replace("+","").trim());
            String telephone = countryValue.equals("1") ? areaCode + phone1 + phone2 : phone3;
            cs.setTelephone(telephone);
            log.warn(String.format("Invalid country value %s. Check country value in tag.", countryValue));
        }
    }

    public void createOrUpdateCustomerConsent(CustomerSignup customerSignup, Map<ConsentType, ConsentStatus> consentMap, String source) {
        List<CustomerConsent> consents = new ArrayList<>();
        consentMap.forEach((type, status) -> {
            CustomerConsent consent;
            consent = customerConsentDAO.findByCustomerSignupAndTypes(customerSignup, type);
            if (consent == null) {
                consent = new CustomerConsent();
            }
            consent.setCustomerId(customerSignup.getId());
            consent.setType(type.name());
            consent.setStatus(status.name());
            consent.setDate(new Date());
            ConsentHistory consentHistory = new ConsentHistory();
            consentHistory.setConsent(consent);
            consentHistory.setStatus(status.name());
            consentHistory.setSource(source);
            consentHistory.setDate(new Date());
            consent.setHistory(consentHistory);
            consents.add(consent);
        });
        customerConsentDAO.saveOrUpdate(consents);
    }

    /**
     * Check if Telephone number is optional based on country value
     * 
     * @return true if Country is not provided or Telephone number is empty
     *
     */
    private boolean phoneNumberNotProvided(String countryValue, String areaCode, String phone1, String phone2,
            String phone3) {
        // if country is US or Canada
        if (StringUtils.isNotBlank(countryValue) && (countryValue.split("_")[0].trim()).equals("1")) {
            return (StringUtils.isBlank(areaCode) || StringUtils.isBlank(phone1) || StringUtils.isBlank(phone2));
        } else if (StringUtils.isNotBlank(countryValue)) {
            return StringUtils.isBlank(phone3);
        } else {
            return true;
        }
    }

    private boolean isValidTelephoneCountry(String countryValue) {
        if (countryValue.contains("_") && countryValue.split("_").length == 2) {
            return true;
        }
        return false;
    }

    public void deleteCustomer(CustomerSignup customerSignup, Map<ConsentType, ConsentStatus> consents, Boolean markDeleted) {
        if (!markDeleted || consents.isEmpty()) {
            return;
        }
        consents.forEach(((consentType, consentStatus) -> {
            if (customerSignup.getEmailId() != null && consentType.equals(ConsentType.EMAIL)) {
                StringBuilder sb = new StringBuilder();
                sb.append(customerSignup.getEmailId());
                sb.insert(customerSignup.getEmailId().indexOf('@'), "-deleted");
                customerSignup.setEmailId(sb.toString());
            }
            if (customerSignup.getTelephone() != null && consentType.equals(ConsentType.SMS)) {
                customerSignup.setTelephoneCountryCode("");
                customerSignup.setTelephone("");
            }
        }));
        customerSignup.setUniversalOptOut(true);
        customerSignup.setOptOut(1);
        updateProfile(customerSignup);
    }

    public List<CustomerSignup> retrieveCustomerSignupByPhone(String phoneNumber) {
        Monitor mon = MonitorFactory.start("retrieveCustomerSignupByPhone(phoneNumber)");
        List<CustomerSignup> customers = null;
        log.debug("retrieveCustomerSignupByPhone telephone" + phoneNumber);
        try {
            customers = customerSignupDAO.findByPhoneNumber(phoneNumber);
        } catch (HibernateException he) {
            log.error("Error retrieving customer with telephone: " + phoneNumber, he);
        } catch (Exception e) {
            log.error("Error retrieving customer with telephone: " + phoneNumber, e);
        }
        mon.stop();
        return customers;
    }
}// AccountDelegate

