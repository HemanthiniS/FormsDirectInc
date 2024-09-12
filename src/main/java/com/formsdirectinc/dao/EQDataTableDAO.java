package com.formsdirectinc.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Criteria;
import org.hibernate.Session;
import com.formsdirectinc.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class EQDataTableDAO {

    private static final Log log = LogFactory.getLog(EQDataTableDAO.class);

    @Autowired
    private SessionFactory sessionFactory;

    public List<EQDataTable> findByProductAndIds(String product, List<Long> eqIds) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(EQDataTable.class);
            criteria.add(Restrictions.eq("product", product));
            criteria.add(Restrictions.in("id", eqIds));
            criteria.addOrder(Order.desc("id"));
            return criteria.list();
        } catch (Exception e) {
            log.error(String.format("Error loading eq_data_t by ids = %s and product = %s", eqIds, product));
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

    public List<EQDataTable> findByIds(List<Long> eqIds) {
        Session session = sessionFactory.openSession();
        Transaction transaction = null;
        try {
            transaction = session.beginTransaction();
            Criteria criteria = session.createCriteria(EQDataTable.class);
            criteria.add(Restrictions.in("id", eqIds));
            criteria.addOrder(Order.desc("id"));
            return criteria.list();
        } catch (Exception e) {
            log.error(String.format("Error loading eq_data_t by ids = %s", eqIds));
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }

    public void saveOrUpdate(EQDataTable eqDataTable) {
        Transaction transaction = null;
        Session session = sessionFactory.openSession();
        try {
            transaction = session.beginTransaction();
            session.saveOrUpdate(eqDataTable);
            transaction.commit();
        } catch (Exception e) {
            log.error("Error saving EQData");
            if (transaction != null) {
                transaction.rollback();
            }
            throw e;
        } finally {
            session.close();
        }
    }
}