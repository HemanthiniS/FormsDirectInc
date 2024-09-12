package com.formsdirectinc.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import com.formsdirectinc.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class BrowserDataDAO {

	private static final Log log = LogFactory.getLog(BrowserDataDAO.class);

	@Autowired
	private SessionFactory sessionFactory;

	public void save(BrowserData browserData) {
		log.debug("Saving user's browser information");
		Transaction transaction = null;
		Session session = sessionFactory.openSession();
		try {
			transaction = session.beginTransaction();
			session.persist(browserData);
			transaction.commit();
			log.debug("browserData saved successfully");
		} catch (HibernateException exception) {
		    log.error("Save failed ::", exception);
		    if (transaction != null && transaction.getStatus().isOneOf(TransactionStatus.ACTIVE)){
			transaction.rollback();
		    }
		    throw exception;
		} finally {
		    session.close();
		}
	}
    
}
