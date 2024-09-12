package com.formsdirectinc.dao;
// Generated Aug 2, 2006 9:46:48 AM by Hibernate Tools 3.1.0.beta5


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.hibernate.criterion.Example;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import com.formsdirectinc.SessionFactory;

import java.util.List;

@Component
@Scope("prototype")
public class CustomerSignupDAO {

    private static final Log log = LogFactory.getLog(CustomerSignupDAO.class);

    @Autowired
    @Qualifier("sessionFactory")
    private SessionFactory sessionFactory;
    
    public void save(CustomerSignup customerSignup) {
        Transaction transaction = null;
	Session session = sessionFactory.openSession();

        try {
            transaction = session.beginTransaction();
            session.save(customerSignup);
            transaction.commit();
        } catch (Exception e) {
            if (transaction != null && transaction.getStatus().isOneOf(TransactionStatus.ACTIVE)) {
                transaction.rollback();
            }
            throw e;
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }
    
    public void persist(CustomerSignup transientInstance) {
        log.debug("persisting CustomerSignup instance");
	Transaction tx = null;
	Session s = sessionFactory.openSession();
        try {
	    tx = s.beginTransaction();
            s.persist(transientInstance);
	    tx.commit();
            log.debug("persist successful");
        }
        catch (RuntimeException re) {
            log.error("persist failed", re);
            if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	      tx.rollback();
            throw re;
        }finally {
         s.close();
        }
    }
    
    public void attachDirty(CustomerSignup instance) {
        log.debug("attaching dirty CustomerSignup instance");
	Transaction tx = null;
	Session s = sessionFactory.openSession();
        try {
	    tx = s.beginTransaction();
            s.saveOrUpdate(instance);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	      tx.rollback();
            throw re;
        }finally {
         s.close();
        }
    }
    
    public void attachClean(CustomerSignup instance) {
        log.debug("attaching clean CustomerSignup instance");
	Transaction tx = null;
	Session s = sessionFactory.openSession();
        try {
	    tx = s.beginTransaction();
            s.lock(instance, LockMode.NONE);
            log.debug("attach successful");
        }
        catch (RuntimeException re) {
            log.error("attach failed", re);
            if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	      tx.rollback();
            throw re;
        }finally {
         s.close();
        }
    }
    
    public void delete(CustomerSignup persistentInstance) {
        log.debug("deleting CustomerSignup instance");
	Transaction tx = null;
	Session s = sessionFactory.openSession();
        try {
	    tx = s.beginTransaction();
            s.delete(persistentInstance);
            log.debug("delete successful");
        }
        catch (RuntimeException re) {
            log.error("delete failed", re);
            if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	      tx.rollback();
            throw re;
        }finally {
         s.close();
        }
    }
    
    public CustomerSignup merge(CustomerSignup detachedInstance) {
        log.debug("merging CustomerSignup instance");
	Transaction tx = null;
	Session s = sessionFactory.openSession();
        try {
	    tx = s.beginTransaction();
            CustomerSignup result = (CustomerSignup) sessionFactory.getCurrentSession()
                    .merge(detachedInstance);
            log.debug("merge successful");
            return result;
        }
        catch (RuntimeException re) {
            log.error("merge failed", re);
            if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	      tx.rollback();
            throw re;
        }finally {
            s.close();
        }
    }
    
    public CustomerSignup findById( long id) {
        log.debug("getting CustomerSignup instance with id: " + id);
	Transaction tx = null;
	Session s = sessionFactory.openSession();
        try {
	      tx = s.beginTransaction();
            CustomerSignup instance = (CustomerSignup) s
                    .get("com.formsdirectinc.dao.CustomerSignup", id);
            if (instance==null) {
                log.debug("get successful, no instance found");
            }
            else {
                log.debug("get successful, instance found");
            }
            return instance;
        }
        catch (RuntimeException re) {
            log.error("get failed", re);

            throw re;
        }
        finally {
            s.close();
        }
    }
    
    public List findByExample(CustomerSignup instance) {
        log.debug("finding CustomerSignup instance by example");
	Transaction tx = null;
        Session s = sessionFactory.openSession();

        try {
	     tx = s.beginTransaction();
	
            List results = s
                    .createCriteria("com.formsdirectinc.dao.CustomerSignup")
                    .add(Example.create(instance))
            .list();
            log.debug("find by example successful, result size: " + results.size());
            return results;
        }
        catch (RuntimeException re) {
            log.error("find by example failed", re);
            if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	      tx.rollback();
            throw re;
        } finally {
            s.close();
      }
    } 
    public List findByEmailPassword(String emailId, String password) {
      Transaction tx = null;
      Session s = sessionFactory.openSession();
      try {	
        tx = s.beginTransaction();
        Query query = s.getNamedQuery("com.formsdirectinc.dao.CustomerSignup.findByEmailPasswordSite");
        query.setParameter("emailId", emailId);
        query.setParameter("password", password);
        return query.list();
       }catch(RuntimeException re) {
         if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	   tx.rollback();
         throw re;
       }finally{
         s.close();
       }
    }
    public List findByEmail(String emailId) {
      Transaction tx = null;
      Session s = sessionFactory.openSession();
      try {	
	  tx = s.beginTransaction();
        Query query = s.getNamedQuery("com.formsdirectinc.dao.CustomerSignup.findByEmail");
        query.setParameter("emailId", emailId);	
        return query.list();
       }catch(RuntimeException re) {
	   if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	   tx.rollback();
         throw re;
       }finally{
         s.close();
       }
    }

    public List findByAuthId(String authId) {
        Transaction tx = null;
        Session s = sessionFactory.openSession();
        try {
            tx = s.beginTransaction();
            Query query = s.getNamedQuery("com.formsdirectinc.dao.CustomerSignup.findByAuthId");
            query.setParameter("authId", authId);
            return query.list();
        }catch(RuntimeException re) {
            if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
                tx.rollback();
            throw re;
        }finally{
            s.close();
        }
    }
    public List findByEmailPasswordSite(String emailId, String password, String site ) {
      Transaction tx = null;
      Session s = sessionFactory.openSession();
      try {	
        tx = s.beginTransaction();
        Query query = s.getNamedQuery("com.formsdirectinc.dao.CustomerSignup.findByEmailPassword");
        query.setParameter("emailId", emailId);
        query.setParameter("password", password);
	query.setParameter("site", site);
        return query.list();
       }catch(RuntimeException re) {
         if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	   tx.rollback();
         throw re;
       }finally{
         s.close();
       }
    }

    public List findByPhoneNumber(String telephone) {
        Transaction tx = null;
        Session s = sessionFactory.openSession();
        try {
            tx = s.beginTransaction();
            Query query = s.getNamedQuery("com.formsdirectinc.dao.CustomerSignup.findByPhone");
            query.setParameter("telephone", telephone);
            return query.list();
        } catch (Exception ex) {
            if (tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE)) {
                tx.rollback();
            }
            throw ex;
        } finally {
            s.close();
        }
    }
}

