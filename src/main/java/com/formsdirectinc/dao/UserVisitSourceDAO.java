package com.formsdirectinc.dao;

import org.hibernate.Session;
import com.formsdirectinc.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Restrictions;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UserVisitSourceDAO {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserVisitSourceDAO.class);

    @Autowired
    private SessionFactory sessionFactory;

    public void save(UserVisitSource userVisitSource) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();

        try {
            transaction = session.beginTransaction();
            session.persist(userVisitSource);
            transaction.commit();
        } catch (Exception e) {
            LOGGER.error("Unable to save userVisitSource due to:", e);
            if (transaction != null && transaction.getStatus().isOneOf(TransactionStatus.ACTIVE)) {
                transaction.rollback();
            }
        } finally {
            if (session.isOpen()) {
                session.close();
            }
        }
    }

    public UserVisitSource find(UserVisitSource userVisitSource) {
        Session session = sessionFactory.openSession();
        UserVisitSource userVisitSourceInstance = null;
        try {
            userVisitSourceInstance = (UserVisitSource) session.createCriteria(userVisitSource.getClass())
                    .add(Restrictions.eq("userId", userVisitSource.getUserId()))
                    .add(Restrictions.eq("propertyKey", userVisitSource.getPropertyKey()))
                    .add(Restrictions.eq("propertyValue", userVisitSource.getPropertyValue()))
                    .uniqueResult();
        } catch (Exception e) {
            LOGGER.error("Unable to find userVisitSource due to:", e);
        } finally {
            session.close();
        }

        return userVisitSourceInstance;
    }
}
