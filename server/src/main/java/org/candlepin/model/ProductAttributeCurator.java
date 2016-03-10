/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.model;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.TypedQuery;

/**
 * AttributeCurator
 */
public class ProductAttributeCurator extends AbstractHibernateCurator<ProductAttribute> {

    public ProductAttributeCurator() {
        super(ProductAttribute.class);
    }
//
//    public Map<String, Map<String, ProductAttribute>> getProductWithAttributes(List<Pool> poolList) {
//        TypedQuery<Object[]> productAttributes = 
//                getEntityManager().createQuery(
//                          "select pool.id, a "
//                        + "FROM Pool pool, ProductAttribute a  "+ 
//                          "WHERE pool in :poolList and a.product = pool.product  "
//                        , Object[].class)
//                    .setParameter("poolList", poolList);
//        List<Object[]> queryResult = productAttributes.getResultList();
//        
//        return null;
////        return result;
//    }
    
   
//    public Map<String, Map<String, ProductAttribute>> findProductAttributesForPools(List<Pool> poolList) {
//        TypedQuery<PoolProductAttribute> productAttributes = 
//                getEntityManager().createQuery(
//                          "SELECT NEW org.candlepin.model.PoolProductAttribute(pool.id, attribute) "
//                        +" FROM Pool pool "
//                        + "INNER JOIN pool.product product "
//                        + "INNER JOIN product.attributes attribute "
//                        + "WHERE pool in :poolList "
//                        , PoolProductAttribute.class).setParameter("poolList", poolList);
//        
//        Map<String, Map<String, ProductAttribute>> result = new HashMap<String, Map<String, ProductAttribute>>();
//        List<PoolProductAttribute> queryResult = productAttributes.getResultList();
//        for (PoolProductAttribute ppa : queryResult){
//            if (!result.containsKey(ppa.getPoolId())){
//                result.put(ppa.getPoolId() , new HashMap<>());
//            }
//            
//            if (!result.get(ppa.getPoolId()).containsKey(ppa.getProdAttribute().getName())){
//                result.get(ppa.getPoolId()).put(ppa.getProdAttribute().getName(), ppa.getProdAttribute());
//            }
//        }
//        
//        return result;
//    }
    
    public Map<String, Map<String, String>> findProductAttributesForPools2(List<Pool> poolList) {
        TypedQuery<PoolProductAttribute> productAttributes = 
                getEntityManager().createQuery(
                          "SELECT NEW  org.candlepin.model.PoolProductAttribute(p.id, attribute.name, attribute.value) "
                        + "FROM Pool p, ProductAttribute attribute "
                        + "WHERE p in :poolList and attribute.product = p.product"
                        , PoolProductAttribute.class).setParameter("poolList", poolList);
        
        Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        List<PoolProductAttribute> queryResult = productAttributes.getResultList();
        for (PoolProductAttribute ppa : queryResult){
            if (!result.containsKey(ppa.getPoolId())){
                result.put(ppa.getPoolId() , new HashMap<String, String>());
            }
            
            if (!result.get(ppa.getPoolId()).containsKey(ppa.getName())){
                result.get(ppa.getPoolId()).put(ppa.getName(), ppa.getValue());
            }
        }
        
        return result;
    }

}
