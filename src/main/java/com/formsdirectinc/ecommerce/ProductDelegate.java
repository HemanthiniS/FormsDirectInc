
/**
 * ProductDelegate.java
 *
 *
 * Created: Fri Sep  8 20:29:03 2006
 *
 * @author root
 * @version $Id$
 * 
 * Release ID: $Name$ 
 * 
 * Last modified: 
 * 
 */

package com.formsdirectinc.ecommerce;


import com.formsdirectinc.dao.Product;
import com.formsdirectinc.dao.ProductDAO;
import com.jamonapi.Monitor;
import com.jamonapi.MonitorFactory;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.resource.transaction.spi.TransactionStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

@Component
public class ProductDelegate {

  private static Logger log = Logger.getLogger(ProductDelegate.class.getName());
    
  @Autowired
  private com.formsdirectinc.SessionFactory sessionFactory;

  private static Map<String, Integer> applicationTypes = new HashMap<String, Integer>();
    /**
     *
     * @param productCatalogue product bean
     * @return boolean true or false
     */

  public boolean saveProduct(Product productCatalogue) {
   Monitor mon = MonitorFactory.start("saveProduct(productCatalogue)");
    boolean result = false;
    Transaction tx = null;
    Session hibernateSession = getHibernateSession();
    log.debug("HibernateSession:" +hibernateSession);
    try {
      tx = hibernateSession.beginTransaction();
      hibernateSession.saveOrUpdate(productCatalogue);
      tx.commit();
      result=true;
      hibernateSession.flush();
    } catch(HibernateException hibernateException) {
      hibernateException.printStackTrace();
      if(tx != null && tx.getStatus().isOneOf(TransactionStatus.ACTIVE))
	tx.rollback();
      result=false;
    } finally {
      hibernateSession.close();
    }
    mon.stop();
    return result;
  }

    public BigDecimal retrieveProductCostByName(String productName) {
	Session hibernateSession = null;
	BigDecimal cost = null;
	try {
	    hibernateSession = getHibernateSession();
	    cost = (BigDecimal) hibernateSession
		.createQuery(
			     "select price from Product where name =:productName and active = 1")
		.setParameter("productName", productName).uniqueResult();
	    hibernateSession.close();
	    return cost;
	} catch (HibernateException he) {
	    log.error("Error retrieving cost for product name: "
		      + productName, he);
	    if (hibernateSession != null) {
		hibernateSession.close();
	    }
	    throw he;
	} 
    }

  /**
   *@return List Product
   */

  public List retrieveProductSummary() {
          Monitor mon = MonitorFactory.start("retrieveProductSummary()");
    List pcList = new ArrayList(); 
    Product productCatalogue = new Product();
    ProductDAO productHome = new ProductDAO();
    try {
      pcList = productHome.findByExample(productCatalogue);
      Iterator pcIter = pcList.iterator();
      while(pcIter.hasNext()) {
	productCatalogue = (Product) pcIter.next();
	log.debug("Product Id:" +productCatalogue.getProductId());
	log.debug("Product Name:" +productCatalogue.getName());
	log.debug("Product Amount:" +productCatalogue.getPrice());
      }//while
    }catch(HibernateException he) {
      he.printStackTrace();
    }
    mon.stop();
    return pcList;
  }

  public BigDecimal getProductCost(int prodId){
  Monitor mon = MonitorFactory.start("getProductCost(prodId)"); 
   List pcList;
    Product productCatalogue;
    ProductDAO productHome = new ProductDAO();
    try {
      pcList = productHome.findByProductId(prodId);
      Iterator pcIter = pcList.iterator();
      while(pcIter.hasNext()) {
     	productCatalogue = (Product) pcIter.next();
     	return productCatalogue.getPrice();
      }
 mon.stop();
      return BigDecimal.ZERO;
    }catch(HibernateException he) {
      he.printStackTrace();
      return null;
    }
   
  }

  /**
   * function: getHibernateSession()
   * @return Session hibernateSession
   */
  
  private Session getHibernateSession() {
     Monitor mon = MonitorFactory.start("getHibernateSession()");
    Session hibernateSession = null;
    try {
        log.debug("HibernateSession:" +hibernateSession);
	hibernateSession = sessionFactory.openSession();
    }catch(Exception exception) {
      log.error("Exception in creating Session");
      exception.printStackTrace();
    }
    mon.stop();
    return hibernateSession;
  }//getHibernateSession()

  /**
   *
   *<p>Returns integer, application Type using the param product name from the product_t table.</p>
   * function: retrieveApplicationType()
   * 
   * @param productName
   * @return int from product_t table
   *
   */
  public int retrieveApplicationType(String productName) {
	if (applicationTypes.get(productName) != null) {
	    return applicationTypes.get(productName);
	}
	BigInteger appType;
	Session hibernateSession = sessionFactory.openSession();
	Transaction tx = null;
	try {
	    tx = hibernateSession.beginTransaction();
	    appType = (BigInteger) hibernateSession
		    .createSQLQuery(
			    "select id from product_t where name =:productName and active = 1")
		    .setParameter("productName", productName).uniqueResult();
	    tx.commit();
	    applicationTypes.put(productName, appType.intValue());
	} catch (HibernateException he) {
	    log.error("Error retrieving application type for product name: "
		    + productName, he);
	    throw he;
	} catch (RuntimeException e) {
	    log.error("Error retrieving application type for product name: "
		    + productName, e);
	    throw e;
	} finally {
	    if (hibernateSession != null) {
		log.info("HibernateSession :" + hibernateSession);
		hibernateSession.close();
	    }
	}
	return appType.intValue();
  }

  /**
   *
   *<p>Returns string, product name by using the param application type from the product_t table.</p>
   * function: retrieveProductName()
   * 
   * @param appType
   * @return productname from Product table
   *
   */
  public String retrieveProductName(int appType) {
	Monitor mon = MonitorFactory.start("retrieveProductName(appType)");
	String appName = "";
	Session hibernateSession = sessionFactory.openSession();
	Transaction tx = null;
	// ApplicationTable application = new ApplicationTable();
	try {
	    tx = hibernateSession.beginTransaction();
	    Query q = hibernateSession
		    .createQuery("select pt.name from Product pt where pt.id ="
			    + appType + "and active = 1");

	    List localList = q.list();
	    Iterator iter = localList.iterator();
	    while (iter.hasNext()) {
		appName = ((String) iter.next());
	    }

	    tx.commit();

	} catch (HibernateException he) {
	    log.error("Error retrieving product name for application type: "
		    + appType, he);
	} catch (Exception e) {
	    log.error("Error retrieving product name for application type: "
		    + appType, e);
	} finally {
	    if (hibernateSession != null) {
		log.info("HibernateSession :" + hibernateSession);
		hibernateSession.close();
	    }
	}
	mon.stop();
	return appName;
  }

  public Product getProduct(String productName) {
	  Product product = null;
	  Session hibernateSession = sessionFactory.openSession();
	  Transaction tx = null;
	  try {
		  tx = hibernateSession.beginTransaction();
		  product = (Product) hibernateSession
				  .createQuery("from Product where name =:productName and active = 1")
				  .setParameter("productName", productName)
				  .uniqueResult();
		  tx.commit();
		  hibernateSession.close();
		  return product;
	  } catch (HibernateException e) {
		  log.error("Error retrieving product by name "
				  + productName, e);
		  throw e;
	  }
  }

}//ProductDelegate


