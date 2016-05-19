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

import org.hibernate.annotations.GenericGenerator;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotNull;


@Entity
@Table(name = "cp_mode")
public class CandlepinMode extends AbstractHibernateObject implements Serializable {
    private static final long serialVersionUID = -7059065874812188168L;

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(length = 37)
    @NotNull
    private String id;
    
    @NotNull
    @Enumerated(EnumType.STRING)
    private Mode mode;
    
    @NotNull
    private String reason;

    @NotNull
    private Date changeTime;
    
    public enum Mode {
        SUSPEND,
        NORMAL
    }

    /**
     * Default constructor
     */
    public CandlepinMode() {

    }
    
    public CandlepinMode(Date changeTime, Mode mode, String reason) {
        this.mode = mode;
        this.changeTime = changeTime;
        this.reason = reason;
    }
    
    public String getReason() {
        return reason;
    }
    
    @Override
    public Serializable getId() {
        return id;
    }


    public Mode getMode() {
        return mode;
    }

    public Date getChangeTime() {
        return changeTime;
    }


    public void setId(String id) {
        this.id = id;
    }
}
