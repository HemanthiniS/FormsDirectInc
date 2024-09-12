package com.formsdirectinc.dao;

import com.formsdirectinc.services.account.ConsentType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import com.formsdirectinc.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class CustomerConsentDAO {

    private static final Log log = LogFactory.getLog(CustomerConsentDAO.class);

    @Autowired
    private SessionFactory sessionFactory;


    public void saveOrUpdate(CustomerConsent consent) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            session.saveOrUpdate(consent);
            transaction.commit();
        } catch (Exception e) {
            log.error("Error Saving Customer Consent");
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

    public void saveOrUpdate(List<CustomerConsent> consents) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            consents.forEach(session::saveOrUpdate);
            transaction.commit();
        } catch (Exception e) {
            log.error("Error Saving Customer Consents");
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

    public List<CustomerConsent> findByCustomerSignup(CustomerSignup customerSignup) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(CustomerConsent.class);
            criteria.add(Restrictions.eq("customerId", customerSignup.getId()));
            return criteria.list();
        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

    public CustomerConsent findByCustomerSignupAndTypes(CustomerSignup customerSignup, ConsentType type) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(CustomerConsent.class);
            criteria.add(Restrictions.eq("customerId", customerSignup.getId()));
            criteria.add(Restrictions.eq("type", type.name()));
            return (CustomerConsent) criteria.uniqueResult();
        } catch (Exception e) {

            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }


}
