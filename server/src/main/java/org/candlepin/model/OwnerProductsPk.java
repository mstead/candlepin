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

import java.io.Serializable;

public class OwnerProductsPk implements Serializable{
    private static final long serialVersionUID = 1L;
    private String ownerId;
    private String productUuid;

    public OwnerProductsPk(){
        
    }
    
    public OwnerProductsPk(String ownerId, String productUuid) {
        super();
        this.ownerId = ownerId;
        this.productUuid = productUuid;
    }
    
    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getProductId() {
        return productUuid;
    }

    public void setProductId(String productId) {
        this.productUuid = productId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((ownerId == null) ? 0 : ownerId.hashCode());
        result = prime * result + ((productUuid == null) ? 0 : productUuid.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OwnerProductsPk other = (OwnerProductsPk) obj;
        if (ownerId == null) {
            if (other.ownerId != null)
                return false;
        }
        else if (!ownerId.equals(other.ownerId))
            return false;
        if (productUuid == null) {
            if (other.productUuid != null)
                return false;
        }
        else if (!productUuid.equals(other.productUuid))
            return false;
        return true;
    }
    
    
}
