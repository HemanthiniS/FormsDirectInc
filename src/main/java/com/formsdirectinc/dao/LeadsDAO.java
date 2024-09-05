package com.formsdirectinc.dao;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Query;
import org.hibernate.Session;
import com.formsdirectinc.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Scope("prototype")
public class LeadsDAO {
    private static final Log log = LogFactory.getLog(LeadsDAO.class);

    @Autowired
    private SessionFactory sessionFactory;

    public List findLeadByCustomerId(Long customerId) {
        Transaction tx = null;
        Session s = sessionFactory.openSession();
        try {
            tx = s.beginTransaction();
            Query query = s.getNamedQuery("com.formsdirectinc.dao.Leads.findLeadsByCustomerId");
            query.setParameter("customerId", customerId);
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
