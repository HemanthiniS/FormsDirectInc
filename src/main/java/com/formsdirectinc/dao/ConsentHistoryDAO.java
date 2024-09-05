package com.formsdirectinc.dao;


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
public class ConsentHistoryDAO {

    @Autowired
    private SessionFactory sessionFactory;

    private static final Log log = LogFactory.getLog(CustomerConsentDAO.class);


    public void saveOrUpdate(ConsentHistory consentHistory) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            session.saveOrUpdate(consentHistory);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Error Saving Consent History");
            throw e;
        } finally {
            session.close();
        }
    }

    public List<ConsentHistory> findByCustomerConsent(CustomerConsent consent) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(ConsentHistory.class);
            criteria.add(Restrictions.eq("consent", consent));
            return criteria.list();
        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            log.error("Error finding Consent History");
            throw e;
        } finally {
            session.close();
        }
    }

}
