package com.formsdirectinc.dao;
// Generated Aug 2, 2006 9:46:48 AM by Hibernate Tools 3.1.0.beta5


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.*;
import org.hibernate.criterion.Example;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class ProductDAO {

    private static final Log log = LogFactory.getLog(ProductDAO.class);


    @Autowired
    private SessionFactory sessionFactory;
    
    public void persist(Product transientInstance) {
        log.debug("persisting Product instance");
	Transaction tx = null;
	Session s = sessionFactory.getCurrentSession();
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
    
    public void attachDirty(Product instance) {
        log.debug("attaching dirty Product instance");
	Transaction tx = null;
	Session s = sessionFactory.getCurrentSession();
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
    
    public void attachClean(Product instance) {
        log.debug("attaching clean Product instance");
	Transaction tx = null;
	Session s = sessionFactory.getCurrentSession();
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
    
    public void delete(Product persistentInstance) {
        log.debug("deleting Product instance");
	Transaction tx = null;
	Session s = sessionFactory.getCurrentSession();
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
    
    public Product merge(Product detachedInstance) {
        log.debug("merging Product instance");
	Transaction tx = null;
	Session s = sessionFactory.getCurrentSession();
        try {
	    tx = s.beginTransaction();
            Product result = (Product) sessionFactory.getCurrentSession()
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
    
    public Product findById(long id) {
        log.debug("getting Product instance with id: " + id);
	Transaction tx = null;
	Session s = sessionFactory.getCurrentSession();
        try {
	    tx = s.beginTransaction();
            Product instance = (Product) s
                    .get("com.formsdirectinc.dao.Product", id);
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
	    if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
		tx.rollback();
            throw re;
        }finally {
            s.close();
        }
    }
    
    public List findByExample(Product instance) {
        log.debug("finding Product instance by example");
	Transaction tx = null;
        Session s = sessionFactory.getCurrentSession();

        try {
	     tx = s.beginTransaction();
	
            List results = s
                    .createCriteria("com.formsdirectinc.dao.Product")
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

  public List findByProductId(Integer prodId) {
      Transaction tx = null;
      Session s = sessionFactory.getCurrentSession();
      try {	
        tx = s.beginTransaction();
        Query query = s.getNamedQuery("com.formsdirectinc.dao.Product.findByProductId");
        query.setParameter("prodId", prodId);
        return query.list();
       }catch(RuntimeException re) {
         if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	   tx.rollback();
         throw re;
       }finally{
         s.close();
       }
    }
}

